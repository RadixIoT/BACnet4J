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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.AuditLogQueryParameters;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.SuccessFilter;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.Unsigned64;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditLogQueryRequestTest {
    @Test
    public void roundTripWithSequenceNumber() throws BACnetException {
        AuditLogQueryRequest req = new AuditLogQueryRequest(
                new ObjectIdentifier(ObjectType.auditLog, 1),
                new AuditLogQueryParameters(new AuditLogQueryParameters.BySource(
                        new ObjectIdentifier(ObjectType.device, 100),
                        null, null, null,
                        SuccessFilter.all)),
                new Unsigned64(500),
                new Unsigned16(50));

        ByteQueue queue = new ByteQueue();
        req.write(queue);

        assertEquals(req, new AuditLogQueryRequest(queue));
    }

    @Test
    public void roundTripWithoutSequenceNumber() throws BACnetException {
        AuditLogQueryRequest req = new AuditLogQueryRequest(
                new ObjectIdentifier(ObjectType.auditLog, 1),
                new AuditLogQueryParameters(new AuditLogQueryParameters.BySource(
                        new ObjectIdentifier(ObjectType.device, 100),
                        null, null, null,
                        SuccessFilter.all)),
                null,
                new Unsigned16(50));

        ByteQueue queue = new ByteQueue();
        req.write(queue);

        assertEquals(req, new AuditLogQueryRequest(queue));
    }
}
