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
import com.serotonin.bacnet4j.type.enumerated.AuditOperation;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditNotificationTest {
    private static TimeStamp ts(int h, int m) {
        return new TimeStamp(new DateTime(new Date(2026, Month.JUNE, 12, DayOfWeek.FRIDAY), new Time(h, m, 0, 0)));
    }

    @Test
    public void allFields() throws BACnetException {
        // Note: targetValue and currentValue (ANY-typed) are left null because the readOptionalANY
        // round-trip path requires server-side property-type resolution that the unit test environment
        // does not provide.
        final AuditNotification n = new AuditNotification(
                ts(10, 0),
                ts(10, 0),
                new Recipient(new ObjectIdentifier(ObjectType.device, 100)),
                new ObjectIdentifier(ObjectType.analogInput, 1),
                AuditOperation.write,
                new CharacterString("src-comment"),
                new CharacterString("tgt-comment"),
                new Unsigned8(7),
                new Unsigned16(42),
                new Unsigned8(3),
                new Recipient(new ObjectIdentifier(ObjectType.device, 200)),
                new ObjectIdentifier(ObjectType.analogInput, 1),
                new PropertyReference(PropertyIdentifier.presentValue),
                new UnsignedInteger(8),
                null,
                null,
                new ErrorClassAndCode(ErrorClass.object, ErrorCode.success));

        final ByteQueue queue = new ByteQueue();
        n.write(queue);

        assertEquals(n, new AuditNotification(queue));
    }

    @Test
    public void minimalFields() throws BACnetException {
        final AuditNotification n = new AuditNotification(
                null, null,
                new Recipient(new ObjectIdentifier(ObjectType.device, 100)),
                null,
                AuditOperation.deviceReset,
                null, null, null, null, null,
                new Recipient(new ObjectIdentifier(ObjectType.device, 200)),
                null, null, null, null, null, null);

        final ByteQueue queue = new ByteQueue();
        n.write(queue);

        assertEquals(n, new AuditNotification(queue));
    }
}
