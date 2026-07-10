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

package com.serotonin.bacnet4j.service.confirmed;

import java.util.Objects;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.NotImplementedException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.AuditLogQueryParameters;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.Unsigned64;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditLogQueryRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 33;

    private final ObjectIdentifier auditLog;
    private final AuditLogQueryParameters queryParameters;
    private final Unsigned64 startAtSequenceNumber;
    private final Unsigned16 requestedCount;

    public AuditLogQueryRequest(ObjectIdentifier auditLog, AuditLogQueryParameters queryParameters,
            Unsigned64 startAtSequenceNumber, Unsigned16 requestedCount) {
        this.auditLog = auditLog;
        this.queryParameters = queryParameters;
        this.startAtSequenceNumber = startAtSequenceNumber;
        this.requestedCount = requestedCount;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, auditLog, 0);
        write(queue, queryParameters, 1);
        writeOptional(queue, startAtSequenceNumber, 2);
        write(queue, requestedCount, 3);
    }

    AuditLogQueryRequest(ByteQueue queue) throws BACnetException {
        auditLog = read(queue, ObjectIdentifier.class, 0);
        queryParameters = read(queue, AuditLogQueryParameters.class, 1);
        startAtSequenceNumber = readOptional(queue, Unsigned64.class, 2);
        requestedCount = read(queue, Unsigned16.class, 3);
    }

    @Override
    public AcknowledgementService handle(LocalDevice localDevice, Address from) throws BACnetException {
        throw new NotImplementedException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuditLogQueryRequest that = (AuditLogQueryRequest) o;
        return Objects.equals(auditLog, that.auditLog) && Objects.equals(queryParameters,
                that.queryParameters) && Objects.equals(startAtSequenceNumber,
                that.startAtSequenceNumber) && Objects.equals(requestedCount, that.requestedCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auditLog, queryParameters, startAtSequenceNumber, requestedCount);
    }
}
