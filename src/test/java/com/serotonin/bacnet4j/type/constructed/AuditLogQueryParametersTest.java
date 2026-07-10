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

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.SuccessFilter;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditLogQueryParametersTest {
    @Test
    public void byTargetAllFields() throws BACnetException {
        AuditLogQueryParameters p = new AuditLogQueryParameters(new AuditLogQueryParameters.ByTarget(
                new ObjectIdentifier(ObjectType.device, 100),
                new Address(new byte[] {1, 2, 3}),
                new ObjectIdentifier(ObjectType.analogInput, 1),
                PropertyIdentifier.presentValue,
                new UnsignedInteger(3),
                new UnsignedInteger(8),
                new AuditOperationFlags(true, false, false, false, false, false, false, false,
                        false, false, false, false, false, false, false, false),
                SuccessFilter.successesOnly));

        ByteQueue queue = new ByteQueue();
        p.write(queue);

        assertEquals(p, new AuditLogQueryParameters(queue));
    }

    /**
     * Per spec, successful-actions-only is a required field (no OPTIONAL keyword in the
     * ASN.1 for BACnetAuditLogQueryParameters). All other fields except the device identifier
     * are OPTIONAL and are omitted here.
     */
    @Test
    public void byTargetMinimal() throws BACnetException {
        AuditLogQueryParameters p = new AuditLogQueryParameters(new AuditLogQueryParameters.ByTarget(
                new ObjectIdentifier(ObjectType.device, 100),
                null, null, null, null, null, null,
                SuccessFilter.all));

        ByteQueue queue = new ByteQueue();
        p.write(queue);

        assertEquals(p, new AuditLogQueryParameters(queue));
    }

    @Test
    public void bySourceAllFields() throws BACnetException {
        AuditLogQueryParameters p = new AuditLogQueryParameters(new AuditLogQueryParameters.BySource(
                new ObjectIdentifier(ObjectType.device, 200),
                new Address(new byte[] {1, 2, 3}),
                new ObjectIdentifier(ObjectType.analogInput, 2),
                new AuditOperationFlags(false, true, false, false, false, false, false, false,
                        false, false, false, false, false, false, false, false),
                SuccessFilter.failuresOnly));

        ByteQueue queue = new ByteQueue();
        p.write(queue);

        assertEquals(p, new AuditLogQueryParameters(queue));
    }

    @Test
    public void bySourceMinimal() throws BACnetException {
        AuditLogQueryParameters p = new AuditLogQueryParameters(new AuditLogQueryParameters.BySource(
                new ObjectIdentifier(ObjectType.device, 200),
                null, null, null,
                SuccessFilter.all));

        ByteQueue queue = new ByteQueue();
        p.write(queue);

        assertEquals(p, new AuditLogQueryParameters(queue));
    }

}
