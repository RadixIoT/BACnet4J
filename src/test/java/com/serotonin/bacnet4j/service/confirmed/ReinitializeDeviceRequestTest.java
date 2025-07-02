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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.event.DefaultReinitializeDeviceHandler;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.ErrorAPDUException;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;

public class ReinitializeDeviceRequestTest extends AbstractTest {
    @Test
    public void noPassword() throws Exception {
        // Create the listener in device 2
        final AtomicReference<Address> receivedAddress = new AtomicReference<>(null);
        d2.setReinitializeDeviceHandler(new DefaultReinitializeDeviceHandler() {
            @Override
            protected void activateChanges(final LocalDevice localDevice, final Address from)
                    throws BACnetErrorException {
                receivedAddress.set(from);
            }
        });

        d1.send(rd2, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.activateChanges, null)).get();

        assertEquals(d1.getAllLocalAddresses()[0], receivedAddress.get());
    }

    @Test
    public void badPassword() throws Exception {
        d2.withPassword("testPassword");

        try {
            d1.send(rd2, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.abortRestore,
                    new CharacterString("wrongPassword"))).get();
            fail("Should have gotten an error response");
        } catch (final ErrorAPDUException e) {
            TestUtils.assertErrorClassAndCode(e.getError(), ErrorClass.security, ErrorCode.passwordFailure);
        }
    }

    @Test
    public void password() throws Exception {
        d2.withPassword("testPassword");

        // Create the listener in device 2
        final AtomicReference<Address> receivedAddress = new AtomicReference<>(null);
        d2.setReinitializeDeviceHandler(new DefaultReinitializeDeviceHandler() {
            @Override
            protected void activateChanges(final LocalDevice localDevice, final Address from)
                    throws BACnetErrorException {
                receivedAddress.set(from);
            }
        });

        d1.send(rd2, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.activateChanges,
                new CharacterString("testPassword"))).get();

        assertEquals(d1.getAllLocalAddresses()[0], receivedAddress.get());
    }
}
