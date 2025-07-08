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

package com.serotonin.bacnet4j.service.unconfirmed;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.event.PrivateTransferHandler;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.EncodedValue;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class UnconfirmedPrivateTransferRequestTest extends AbstractTest {
    @Test
    public void noHandler() {
        new UnconfirmedPrivateTransferRequest(236, 11, new Real(12.3F)).handle(d1, null);
    }

    @Test
    public void noParameters() throws Exception {
        final AtomicBoolean handled = new AtomicBoolean();
        d1.addPrivateTransferHandler(236, 12, new PrivateTransferHandler() {
            @Override
            public Encodable handle(final LocalDevice localDevice, final Address from, final UnsignedInteger vendorId,
                    final UnsignedInteger serviceNumber, final EncodedValue serviceParameters, final boolean confirmed)
                    throws BACnetErrorException {
                assertEquals(d1, localDevice);
                assertEquals(TestNetworkUtils.toSourceAddress(2), from);
                assertEquals(new UnsignedInteger(236), vendorId);
                assertEquals(new UnsignedInteger(12), serviceNumber);
                assertEquals(null, serviceParameters);
                assertEquals(false, confirmed);
                synchronized (handled) {
                    handled.set(true);
                    handled.notify();
                }
                return null;
            }
        });

        synchronized (handled) {
            d2.send(rd1, new UnconfirmedPrivateTransferRequest(236, 12, null));
            handled.wait();
        }
        assertEquals(true, handled.get());
    }

    @Test
    public void withParameters() throws Exception {
        final EncodedValue parameters = new EncodedValue(Boolean.TRUE, Boolean.FALSE, new CharacterString("xyz"));

        final AtomicBoolean handled = new AtomicBoolean();
        d1.addPrivateTransferHandler(236, 13, new PrivateTransferHandler() {
            @Override
            public Encodable handle(final LocalDevice localDevice, final Address from, final UnsignedInteger vendorId,
                    final UnsignedInteger serviceNumber, final EncodedValue serviceParameters, final boolean confirmed)
                    throws BACnetErrorException {
                assertEquals(d1, localDevice);
                assertEquals(TestNetworkUtils.toSourceAddress(2), from);
                assertEquals(new UnsignedInteger(236), vendorId);
                assertEquals(new UnsignedInteger(13), serviceNumber);
                assertEquals(parameters, serviceParameters);
                assertEquals(false, confirmed);
                synchronized (handled) {
                    handled.set(true);
                    handled.notify();
                }
                return null;
            }
        });

        synchronized (handled) {
            d2.send(rd1, new UnconfirmedPrivateTransferRequest(236, 13, parameters));
            handled.wait();
        }
        assertEquals(true, handled.get());
    }
}
