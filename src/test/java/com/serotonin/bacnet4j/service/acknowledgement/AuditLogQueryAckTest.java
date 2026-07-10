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

package com.serotonin.bacnet4j.service.acknowledgement;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.AuditLogRecord;
import com.serotonin.bacnet4j.type.constructed.AuditLogRecordResult;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.LogStatus;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.Unsigned64;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditLogQueryAckTest {
    @Test
    public void roundTrip() throws BACnetException {
        AuditLogRecordResult r1 = new AuditLogRecordResult(new Unsigned64(1),
                new AuditLogRecord(
                        new DateTime(new Date(2026, Month.JUNE, 12, DayOfWeek.FRIDAY), new Time(9, 15, 30, 0)),
                        new LogStatus(false, false, false)));
        AuditLogRecordResult r2 = new AuditLogRecordResult(new Unsigned64(2),
                new AuditLogRecord(
                        new DateTime(new Date(2026, Month.JUNE, 12, DayOfWeek.FRIDAY), new Time(9, 16, 0, 0)),
                        new LogStatus(true, false, false)));

        AuditLogQueryAck ack = new AuditLogQueryAck(
                new ObjectIdentifier(ObjectType.auditLog, 1),
                new SequenceOf<>(r1, r2),
                Boolean.TRUE);

        ByteQueue queue = new ByteQueue();
        ack.write(queue);

        assertEquals(ack, new AuditLogQueryAck(queue));
    }

    @Test
    public void emptyRecords() throws BACnetException {
        AuditLogQueryAck ack = new AuditLogQueryAck(
                new ObjectIdentifier(ObjectType.auditLog, 1),
                new SequenceOf<>(),
                Boolean.FALSE);

        ByteQueue queue = new ByteQueue();
        ack.write(queue);

        assertEquals(ack, new AuditLogQueryAck(queue));
    }
}
