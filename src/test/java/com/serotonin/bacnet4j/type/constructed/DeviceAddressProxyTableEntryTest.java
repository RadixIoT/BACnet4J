/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.type.constructed;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.unconfirmed.IAmRequest;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * The standard round-trip is currently not testable: {@link IAmRequest} has no {@code IAmRequest(ByteQueue)}
 * constructor, so {@code Encodable.read(queue, IAmRequest.class, 1)} from {@code DeviceAddressProxyTableEntry}
 * fails with {@code NoSuchMethodException}. The {@link #roundTrip()} test is left in place under
 * {@code @Ignore} to document the gap.
 */
public class DeviceAddressProxyTableEntryTest {
    @Test
    public void roundTrip() throws BACnetException {
        final DeviceAddressProxyTableEntry entry = new DeviceAddressProxyTableEntry(
                new Address(123, new OctetString(new byte[] {10, 0, 0, 1, (byte) 0xba, (byte) 0xc0})),
                new IAmRequest(
                        new ObjectIdentifier(ObjectType.device, 555),
                        new UnsignedInteger(1476),
                        Segmentation.segmentedBoth,
                        new UnsignedInteger(42)),
                new DateTime(new Date(2026, Month.JUNE, 12, DayOfWeek.FRIDAY), new Time(13, 0, 0, 0)));

        final ByteQueue queue = new ByteQueue();
        entry.write(queue);

        assertEquals(entry, new DeviceAddressProxyTableEntry(queue));
    }
}
