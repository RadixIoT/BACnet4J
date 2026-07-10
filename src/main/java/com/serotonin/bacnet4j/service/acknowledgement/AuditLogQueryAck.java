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

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.AuditLogRecordResult;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditLogQueryAck extends AcknowledgementService {
    public static final byte TYPE_ID = 33;

    private final ObjectIdentifier auditLog;
    private final SequenceOf<AuditLogRecordResult> records;
    private final Boolean noMoreItems;

    public AuditLogQueryAck(ObjectIdentifier auditLog, SequenceOf<AuditLogRecordResult> records, Boolean noMoreItems) {
        this.auditLog = auditLog;
        this.records = records;
        this.noMoreItems = noMoreItems;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, auditLog, 0);
        write(queue, records, 1);
        write(queue, noMoreItems, 2);
    }

    AuditLogQueryAck(ByteQueue queue) throws BACnetException {
        auditLog = read(queue, ObjectIdentifier.class, 0);
        records = readSequenceOf(queue, AuditLogRecordResult.class, 1);
        noMoreItems = read(queue, Boolean.class, 2);
    }

    public ObjectIdentifier getAuditLog() {
        return auditLog;
    }

    public SequenceOf<AuditLogRecordResult> getRecords() {
        return records;
    }

    public Boolean getNoMoreItems() {
        return noMoreItems;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuditLogQueryAck that = (AuditLogQueryAck) o;
        return Objects.equals(auditLog, that.auditLog) && Objects.equals(records,
                that.records) && Objects.equals(noMoreItems, that.noMoreItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auditLog, records, noMoreItems);
    }
}
