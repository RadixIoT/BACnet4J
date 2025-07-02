/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 Radix IoT LLC. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Radix IoT LLC,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.radixiot.com for commercial license options.
 */

package com.serotonin.bacnet4j.service.confirmed;

import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.awaitTrue;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

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
        d1.send(rd2,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description,
                        null, new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d2.get(PropertyIdentifier.description));

        // Receive a request.
        assertNull(d1.get(PropertyIdentifier.description));
        d2.send(rd1,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                        null, new CharacterString("a"), null)).get();
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
            d1.send(rd2,
                    new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description,
                            null, new CharacterString("a"), null)).get();
            fail("BACnetException should have been thrown");
        } catch (final BACnetException e) {
            // Inner exception must be a BACCommunicationDisabledException
            if (!(e.getCause() instanceof CommunicationDisabledException)) {
                fail("CommunicationDisabledException should have been thrown");
            }
        }

        // Receive a request
        assertNull(d1.get(PropertyIdentifier.description));
        d2.send(rd1,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                        null, new CharacterString("a"), null)).get();
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
        // Wait a bit to ensure nothing changes.
        quiesce();
        assertEquals(0, iamCount.get());

        // But should still respond to a WhoIs
        d2.send(rd1, new WhoIsRequest(1, 1));
        awaitEquals(1, iamCount::get);

        // Re-enable
        d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.enable, null)).get();

        // Send a request. This time it succeeds.
        assertNull(d2.get(PropertyIdentifier.description));
        d1.send(rd2,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description,
                        null, new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d2.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that only DCCR and reinitialize are handled when disabled
     */
    @Test
    public void disable() throws Exception {
        // Disable
        d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.disable, null)).get();

        // Fail to send a request.
        try {
            d1.send(rd2,
                    new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description,
                            null, new CharacterString("a"), null)).get();
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
            awaitTrue(() -> future.getState() == ServiceFuture.State.SENT);
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
        d1.send(rd2,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description,
                        null, new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d2.get(PropertyIdentifier.description));

        // Receive a request. This time it too succeeds. Note that the value is already "a", because requests are
        // still processed, just not responded.
        assertEquals(new CharacterString("a"), d1.get(PropertyIdentifier.description));
        d2.send(rd1,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                        null, new CharacterString("b"), null)).get();
        assertEquals(new CharacterString("b"), d1.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that the timer works.
     */
    @Test
    public void timer() throws Exception {
        // Disable for 5 minutes.
        d2.send(rd1, new DeviceCommunicationControlRequest(new UnsignedInteger(5), EnableDisable.disable, null)).get();
        awaitEquals(EnableDisable.disable, d1::getCommunicationControlState);

        // Fail to receive a request
        try {
            final ServiceFuture future = d2.send(rd1,
                    new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                            null, new CharacterString("a"), null));

            // We need to advance the clock because otherwise the request will never time out.
            // First give the transport a chance to send the request.
            awaitTrue(() -> future.getState() == ServiceFuture.State.SENT);

            // Then advance past the timeout.
            clock.plusMillis(TIMEOUT + 1);

            future.get();
            fail("BACnetTimeoutException should have been thrown");
        } catch (@SuppressWarnings("unused") final BACnetTimeoutException e) {
            // Expected
        }

        // Let the 5 minutes elapse.
        clock.plusMinutes(6);
        awaitEquals(EnableDisable.enable, d1::getCommunicationControlState);

        // Receive a request. This time it too succeeds. Note that the value is already "a", because requests are
        // still processed, just not responded.
        assertEquals(new CharacterString("a"), d1.get(PropertyIdentifier.description));
        d2.send(rd1,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                        null, new CharacterString("b"), null)).get();
        assertEquals(new CharacterString("b"), d1.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that the timer gets cancelled.
     */
    @Test
    public void timerCancel() throws Exception {
        // Disable for 5 minutes.
        d2.send(rd1, new DeviceCommunicationControlRequest(new UnsignedInteger(5), EnableDisable.disable, null)).get();

        // Fail to receive a request
        try {
            final ServiceFuture future = d2.send(rd1,
                    new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                            null, new CharacterString("a"), null));

            // We need to advance the clock because otherwise the request will never time out.
            // First give the transport a chance to send the request.
            awaitTrue(() -> future.getState() == ServiceFuture.State.SENT);
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
        d2.send(rd1,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                        null, new CharacterString("b"), null)).get();
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
