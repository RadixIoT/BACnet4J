package com.serotonin.bacnet4j.service.confirmed;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.event.DefaultReinitializeDeviceHandler;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.exception.CommunicationDisabledException;
import com.serotonin.bacnet4j.exception.ErrorAPDUException;
import com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * All tests modify the communication control in device d1.
 */
public class DeviceCommunicationControlRequestTest extends AbstractTest {
    /**
     * Ensure that requests can be sent and responded when enabled by default
     */
    @Test
    public void communicationEnabled() throws BACnetException {
        // Send a request.
        assertNull(d2.get(PropertyIdentifier.description));
        d1.send(rd2, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2),
                PropertyIdentifier.description, null, new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d2.get(PropertyIdentifier.description));

        // Receive a request.
        assertNull(d1.get(PropertyIdentifier.description));
        d2.send(rd1, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.description, null, new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d1.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that requests cannot be sent - except IAm, DCCR and reinitialize - when disable initiation, and that
     * responses can still be received and responded.
     */
    @Test
    public void disableInitiation() throws Exception {
        // Disable initiation
        d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.disableInitiation, null)).get();

        // Fail to send a request.
        try {
            d1.send(rd2, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2),
                    PropertyIdentifier.description, null, new CharacterString("a"), null)).get();
            fail("BACnetException should have been thrown");
        } catch (final BACnetException e) {
            // Inner exception must be a BACCommunicationDisabledException
            if (!(e.getCause() instanceof CommunicationDisabledException)) {
                fail("CommunicationDisabledException should have been thrown");
            }
        }

        // Receive a request
        assertNull(d1.get(PropertyIdentifier.description));
        d2.send(rd1, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.description, null, new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d1.get(PropertyIdentifier.description));

        // Sending of IAms...
        final AtomicInteger iamCount = new AtomicInteger(0);
        d2.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void iAmReceived(final RemoteDevice d) {
                iamCount.incrementAndGet();
            }
        });

        // Should also fail to send an IAm
        d1.send(rd2, d1.getIAm());
        assertEquals(0, iamCount.get());

        // But should still respond to a WhoIs
        d2.send(rd1, new WhoIsRequest(1, 1));
        assertEquals(1, iamCount.get());

        // Re-enable
        d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.enable, null)).get();

        // Send a request. This time it succeeds.
        assertNull(d2.get(PropertyIdentifier.description));
        d1.send(rd2, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2),
                PropertyIdentifier.description, null, new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d2.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that only DCCR and reinitialize are handled when disabled
     */
    @Test
    public void disable() throws BACnetException {
        // Disable
        d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.disable, null)).get();

        // Fail to send a request.
        try {
            d1.send(rd2, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2),
                    PropertyIdentifier.description, null, new CharacterString("a"), null)).get();
            fail("BACnetException should have been thrown");
        } catch (final BACnetException e) {
            // Inner exception must be a BACCommunicationDisabledException
            if (!(e.getCause() instanceof CommunicationDisabledException)) {
                fail("CommunicationDisabledException should have been thrown");
            }
        }

        // Fail to receive a request
        try {
            final ServiceFuture future = d2.send(rd1,
                    new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                            null, new CharacterString("a"), null));

            // We need to advance the clock because otherwise the request will never time out.
            // First give the transport a chance to send the request.
            ThreadUtils.sleep(5);
            // Then advance past the timeout.
            clock.plusMillis(TIMEOUT + 1);

            future.get();
            fail("BACnetTimeoutException should have been thrown");
        } catch (@SuppressWarnings("unused") final BACnetTimeoutException e) {
            // Expected
        }

        // Start backup "works" by returning a communication disabled error.
        TestUtils.assertErrorAPDUException(() -> {
            d2.send(rd1, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.startBackup, null)).get();
        }, ErrorClass.services, ErrorCode.communicationDisabled);

        // Reinitialize "works", or at least doesn't return an error after the proper method is overridden.
        d1.setReinitializeDeviceHandler(new DefaultReinitializeDeviceHandler() {
            @Override
            protected void activateChanges(final LocalDevice localDevice, final Address from)
                    throws BACnetErrorException {
                // no op
            }
        });
        d2.send(rd1, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.activateChanges, null)).get();

        // Re-enable
        d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.enable, null)).get();

        // Send a request. This time it succeeds.
        assertNull(d2.get(PropertyIdentifier.description));
        d1.send(rd2, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2),
                PropertyIdentifier.description, null, new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d2.get(PropertyIdentifier.description));

        // Receive a request. This time it too succeeds. Note that the value is already "a", because requests are
        // still processed, just not responded.
        assertEquals(new CharacterString("a"), d1.get(PropertyIdentifier.description));
        d2.send(rd1, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.description, null, new CharacterString("b"), null)).get();
        assertEquals(new CharacterString("b"), d1.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that the timer works.
     */
    @Test
    public void timer() throws BACnetException {
        // Disable for 5 minutes.
        d2.send(rd1, new DeviceCommunicationControlRequest(new UnsignedInteger(5), EnableDisable.disable, null)).get();

        // Fail to receive a request
        AtomicReference<BACnetTimeoutException> timeoutException = new AtomicReference<>();
        try {
            final ServiceFuture future = d2.send(rd1,
                    new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                            null, new CharacterString("a"), null));

            // We need to advance the clock because otherwise the request will never time out.
            // First give the transport a chance to send the request.
            ThreadUtils.sleep(5);
            // Then advance past the timeout.
            clock.plusMillis(TIMEOUT + 1);

            future.get();
            fail("BACnetTimeoutException should have been thrown");
        } catch (final BACnetTimeoutException e) {
            // Expected
            timeoutException.set(e);
        }

        //Assert that the exception was caught
        assertNotNull(timeoutException.get());

        // Let the 5 minutes elapse.
        clock.plusMinutes(6);

        try {
            this.d1Executor.waitForExecutorTasks(50, TimeUnit.MILLISECONDS, 10,
                    true, true, true);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // Receive a request. This time it too succeeds. Note that the value is already "a", because requests are
        // still processed, just not responded.
        assertEquals(new CharacterString("a"), d1.get(PropertyIdentifier.description));
        d2.send(rd1, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.description, null, new CharacterString("b"), null)).get();
        assertEquals(new CharacterString("b"), d1.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that the timer gets cancelled.
     */
    @Test
    public void timerCancel() throws BACnetException {
        // Disable for 5 minutes.
        d2.send(rd1, new DeviceCommunicationControlRequest(new UnsignedInteger(5), EnableDisable.disable, null)).get();

        // Fail to receive a request
        try {
            final ServiceFuture future = d2.send(rd1,
                    new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                            null, new CharacterString("a"), null));

            // We need to advance the clock because otherwise the request will never time out.
            // First give the transport a chance to send the request.
            ThreadUtils.sleep(5);
            // Then advance past the timeout.
            clock.plusMillis(TIMEOUT + 1);

            future.get();
            fail("BACnetTimeoutException should have been thrown");
        } catch (@SuppressWarnings("unused") final BACnetTimeoutException e) {
            // Expected
        }

        // Let 1 minute go by.
        clock.plusMinutes(1);

        // Re-enable. Yes, the timeout is still there, which shouldn't matter.
        d2.send(rd1, new DeviceCommunicationControlRequest(new UnsignedInteger(5), EnableDisable.enable, null)).get();

        // Receive a request. This time it too succeeds. Note that the value is already "a", because requests are
        // still processed, just not responded.
        assertEquals(new CharacterString("a"), d1.get(PropertyIdentifier.description));
        d2.send(rd1, new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.description, null, new CharacterString("b"), null)).get();
        assertEquals(new CharacterString("b"), d1.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that the password functionality works.
     */
    @Test
    public void password() throws BACnetException {
        d1.withPassword("asdf");

        // Try to disable with null
        try {
            d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.disable, null)).get();
            fail("ErrorAPDUException should have been thrown");
        } catch (final ErrorAPDUException e) {
            TestUtils.assertErrorClassAndCode(e.getError(), ErrorClass.security, ErrorCode.passwordFailure);
        }

        // Try to disable with incorrect password
        try {
            d2.send(rd1,
                    new DeviceCommunicationControlRequest(null, EnableDisable.disable, new CharacterString("qwer")))
                    .get();
            fail("ErrorAPDUException should have been thrown");
        } catch (final ErrorAPDUException e) {
            TestUtils.assertErrorClassAndCode(e.getError(), ErrorClass.security, ErrorCode.passwordFailure);
        }

        // Try to disable with correct password
        d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.disable, new CharacterString("asdf")))
                .get();
    }
}
