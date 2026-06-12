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
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditLogRecordTest {
    private static DateTime ts() {
        return new DateTime(new Date(2026, Month.JUNE, 12, DayOfWeek.FRIDAY), new Time(10, 0, 0, 0));
    }

    @Test
    public void logStatus() throws BACnetException {
        final AuditLogRecord rec = new AuditLogRecord(ts(), new LogStatus(true, false, true));

        final ByteQueue queue = new ByteQueue();
        rec.write(queue);

        assertEquals(rec, new AuditLogRecord(queue));
    }

    @Test
    public void auditNotification() throws BACnetException {
        final AuditNotification notification = new AuditNotification(
                null, null,
                new Recipient(new ObjectIdentifier(ObjectType.device, 1)),
                null,
                AuditOperation.write,
                null, null, null, null, null,
                new Recipient(new ObjectIdentifier(ObjectType.device, 2)),
                null, null, null, null, null,
                new ErrorClassAndCode(ErrorClass.object, ErrorCode.success));

        final AuditLogRecord rec = new AuditLogRecord(ts(), notification);

        final ByteQueue queue = new ByteQueue();
        rec.write(queue);

        assertEquals(rec, new AuditLogRecord(queue));
    }

    @Test
    public void timeChange() throws BACnetException {
        final AuditLogRecord rec = new AuditLogRecord(ts(), new Real(3600.0f));

        final ByteQueue queue = new ByteQueue();
        rec.write(queue);

        assertEquals(rec, new AuditLogRecord(queue));
    }
}
