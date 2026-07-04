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
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.event.DefaultReinitializeDeviceHandler;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetException;
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
import com.serotonin.bacnet4j.type.primitive.Unsigned16;

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
        } catch (BACnetException e) {
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
        AtomicInteger iamCount = new AtomicInteger(0);
        d2.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void iAmReceived(RemoteDevice d) {
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
     * Per addendum 135-2016bi-2 (Protocol Revision 20), the DISABLE option is deprecated.
     * A valid request carrying the deprecated value must be rejected with SERVICES /
     * SERVICE_REQUEST_DENIED, and the receiver's communication state must not change.
     */
    @Test
    public void disable_deprecatedValueRejected() throws Exception {
        assertEquals(EnableDisable.enable, d1.getCommunicationControlState());

        TestUtils.assertErrorAPDUException(() -> {
            d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.disable, null)).get();
        }, ErrorClass.services, ErrorCode.serviceRequestDenied);

        // State must not have changed.
        assertEquals(EnableDisable.enable, d1.getCommunicationControlState());

        // Regular traffic still works — d1 can send, and d1 can receive.
        d1.send(rd2,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description,
                        null, new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d2.get(PropertyIdentifier.description));

        d2.send(rd1,
                new WritePropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description,
                        null, new CharacterString("b"), null)).get();
        assertEquals(new CharacterString("b"), d1.get(PropertyIdentifier.description));
    }

    /**
     * Per addendum 135-2016bi-2: LocalDevice.setCommunicationControl(disable, ...) must reject
     * the deprecated value. This is a defence-in-depth check for callers that bypass the
     * service handler.
     */
    @Test
    public void disable_setCommunicationControlThrows() {
        try {
            d1.setCommunicationControl(EnableDisable.disable, 0);
            fail("IllegalArgumentException should have been thrown");
        } catch (@SuppressWarnings("unused") IllegalArgumentException expected) {
            // Expected.
        }
    }

    /**
     * Under DISABLE_INITIATION, incoming ReinitializeDevice(startBackup) is not treated
     * specially by the DCC layer — it proceeds through the normal handler path. Prior to
     * bi-2, the service handler returned COMMUNICATION_DISABLED for backup states when the
     * device was in DISABLE. That code path is removed because DISABLE is deprecated and
     * COMMUNICATION_DISABLED is no longer intended as a service-response error code.
     */
    @Test
    public void disableInitiation_reinitializeStartBackupProceedsToHandler() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        d1.setReinitializeDeviceHandler(new DefaultReinitializeDeviceHandler() {
            @Override
            protected void startBackup(LocalDevice localDevice, Address from) {
                called.set(true);
            }
        });

        d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.disableInitiation, null)).get();
        d2.send(rd1, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.startBackup, null)).get();

        assertTrue(called.get());
    }

    /**
     * Ensure that the timer works. Under DISABLE_INITIATION, d1 cannot initiate BACnet
     * services for the duration; incoming requests are still responded to normally. When
     * the timer expires, d1 returns to ENABLE and can initiate again.
     */
    @Test
    public void timer() throws Exception {
        // Disable-initiation on d1 for 5 minutes.
        d2.send(rd1, new DeviceCommunicationControlRequest(
                new Unsigned16(5), EnableDisable.disableInitiation, null)).get();
        awaitEquals(EnableDisable.disableInitiation, d1::getCommunicationControlState);

        // d1 cannot initiate a request during the disable window.
        try {
            d1.send(rd2, new WritePropertyRequest(
                    new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description, null,
                    new CharacterString("a"), null)).get();
            fail("BACnetException should have been thrown");
        } catch (BACnetException e) {
            if (!(e.getCause() instanceof CommunicationDisabledException)) {
                fail("CommunicationDisabledException should have been thrown");
            }
        }

        // But d1 still responds to incoming requests.
        d2.send(rd1, new WritePropertyRequest(
                new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.description, null,
                new CharacterString("a"), null)).get();
        assertEquals(new CharacterString("a"), d1.get(PropertyIdentifier.description));

        // Let the 5 minutes elapse.
        clock.plusMinutes(6);
        awaitEquals(EnableDisable.enable, d1::getCommunicationControlState);

        // d1 can initiate again.
        assertNull(d2.get(PropertyIdentifier.description));
        d1.send(rd2, new WritePropertyRequest(
                new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description, null,
                new CharacterString("b"), null)).get();
        assertEquals(new CharacterString("b"), d2.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that an explicit re-enable cancels the pending timer, so no spurious re-enable
     * fires later when the timer would have expired.
     */
    @Test
    public void timerCancel() throws Exception {
        // Disable-initiation on d1 for 5 minutes.
        d2.send(rd1, new DeviceCommunicationControlRequest(
                new Unsigned16(5), EnableDisable.disableInitiation, null)).get();
        awaitEquals(EnableDisable.disableInitiation, d1::getCommunicationControlState);

        // Confirm d1 cannot initiate during the disable window.
        try {
            d1.send(rd2, new WritePropertyRequest(
                    new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description, null,
                    new CharacterString("a"), null)).get();
            fail("BACnetException should have been thrown");
        } catch (BACnetException e) {
            if (!(e.getCause() instanceof CommunicationDisabledException)) {
                fail("CommunicationDisabledException should have been thrown");
            }
        }

        // Let 1 minute go by.
        clock.plusMinutes(1);

        // Re-enable explicitly. The 5-minute timer should be cancelled so it can't fire later.
        d2.send(rd1, new DeviceCommunicationControlRequest(new Unsigned16(5), EnableDisable.enable, null)).get();
        awaitEquals(EnableDisable.enable, d1::getCommunicationControlState);

        // d1 can initiate now.
        assertNull(d2.get(PropertyIdentifier.description));
        d1.send(rd2, new WritePropertyRequest(
                new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.description, null,
                new CharacterString("b"), null)).get();
        assertEquals(new CharacterString("b"), d2.get(PropertyIdentifier.description));
    }

    /**
     * Ensure that the password functionality works. Password validation must precede the
     * bi-2 deprecated-value check, so a null or wrong password on a request carrying the
     * deprecated DISABLE value still surfaces PASSWORD_FAILURE — not SERVICE_REQUEST_DENIED.
     * A request with the correct password and the current DISABLE_INITIATION value succeeds.
     */
    @Test
    public void password() throws BACnetException {
        d1.withPassword("asdf");

        // Null password against a request that would otherwise be deprecated-value-rejected:
        // password failure takes precedence.
        try {
            d2.send(rd1, new DeviceCommunicationControlRequest(null, EnableDisable.disable, null)).get();
            fail("ErrorAPDUException should have been thrown");
        } catch (ErrorAPDUException e) {
            TestUtils.assertErrorClassAndCode(e.getError(), ErrorClass.security, ErrorCode.passwordFailure);
        }

        // Wrong password: also PASSWORD_FAILURE, not SERVICE_REQUEST_DENIED.
        try {
            d2.send(rd1, new DeviceCommunicationControlRequest(
                    null, EnableDisable.disable, new CharacterString("qwer"))).get();
            fail("ErrorAPDUException should have been thrown");
        } catch (ErrorAPDUException e) {
            TestUtils.assertErrorClassAndCode(e.getError(), ErrorClass.security, ErrorCode.passwordFailure);
        }

        // Correct password + deprecated DISABLE value: the deprecated-value check fires
        // after the password check, so this now surfaces SERVICE_REQUEST_DENIED.
        TestUtils.assertErrorAPDUException(() -> {
            d2.send(rd1, new DeviceCommunicationControlRequest(
                    null, EnableDisable.disable, new CharacterString("asdf"))).get();
        }, ErrorClass.services, ErrorCode.serviceRequestDenied);

        // Correct password + valid DISABLE_INITIATION value: request succeeds.
        d2.send(rd1, new DeviceCommunicationControlRequest(
                null, EnableDisable.disableInitiation, new CharacterString("asdf"))).get();
    }
}
