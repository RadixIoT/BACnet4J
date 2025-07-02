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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.DateTime;

public class TimeSynchronizationRequestTest extends AbstractTest {
    @Test
    public void timeSync() throws Exception {
        final DateTime dateTime = new DateTime(d1);

        // Create the listener in device 2
        final AtomicReference<Address> receivedAddress = new AtomicReference<>(null);
        final AtomicReference<DateTime> receivedDateTime = new AtomicReference<>(null);
        final AtomicBoolean receivedUtc = new AtomicBoolean(false);
        d2.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void synchronizeTime(final Address from, final DateTime dateTime, final boolean utc) {
                receivedAddress.set(from);
                receivedDateTime.set(dateTime);
                receivedUtc.set(utc);
                synchronized (dateTime) {
                    dateTime.notify();
                }
            }
        });

        final RemoteDevice rd2 = d1.getRemoteDevice(2).get();

        synchronized (dateTime) {
            d1.send(rd2, new TimeSynchronizationRequest(dateTime));
            dateTime.wait(1000);
        }

        assertEquals(d1.getAllLocalAddresses()[0], receivedAddress.get());
        assertEquals(dateTime, receivedDateTime.get());
        assertEquals(false, receivedUtc.get());
    }

    @Test
    public void disabledTimeSync() throws Exception {
        d2.getServicesSupported().setTimeSynchronization(false);

        final DateTime dateTime = new DateTime(d1);

        // Create the listener in device 2
        final AtomicReference<Address> receivedAddress = new AtomicReference<>(null);
        final AtomicReference<DateTime> receivedDateTime = new AtomicReference<>(null);
        final AtomicBoolean receivedUtc = new AtomicBoolean(false);
        d2.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void synchronizeTime(final Address from, final DateTime dateTime, final boolean utc) {
                receivedAddress.set(from);
                receivedDateTime.set(dateTime);
                receivedUtc.set(utc);
                synchronized (dateTime) {
                    dateTime.notify();
                }
            }
        });

        final RemoteDevice rd2 = d1.getRemoteDevice(2).get();

        synchronized (dateTime) {
            d1.send(rd2, new TimeSynchronizationRequest(dateTime));
            dateTime.wait(1000);
        }

        assertNull(receivedAddress.get());
        assertNull(receivedDateTime.get());
        assertFalse(receivedUtc.get());
    }
}
