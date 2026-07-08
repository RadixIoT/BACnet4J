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

package com.serotonin.bacnet4j.service.unconfirmed;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class YouAreRequestTest {
    @Test
    public void allFields() throws BACnetException {
        YouAreRequest req = new YouAreRequest(
                new Unsigned16(42),
                new CharacterString("Model-X"),
                new CharacterString("SN-000123"),
                new ObjectIdentifier(ObjectType.device, 17),
                new OctetString(new byte[] {(byte) 0xAB, (byte) 0xCD, (byte) 0xEF}));

        ByteQueue queue = new ByteQueue();
        req.write(queue);

        assertEquals(req, new YouAreRequest(queue));
    }

    /**
     * Per Clause 16.11.3, device-identifier and device-mac-address are OPTIONAL. The write path
     * uses writeOptional; the read path must use readOptional for symmetric round-trip when the
     * fields are absent.
     */
    @Test
    public void optionalsAbsent() throws BACnetException {
        YouAreRequest req = new YouAreRequest(
                new Unsigned16(42),
                new CharacterString("Model-X"),
                new CharacterString("SN-000123"),
                null,
                null);

        ByteQueue queue = new ByteQueue();
        req.write(queue);

        assertEquals(req, new YouAreRequest(queue));
    }

    @Test
    public void deviceIdentifierOnly() throws BACnetException {
        YouAreRequest req = new YouAreRequest(
                new Unsigned16(1),
                new CharacterString("M"),
                new CharacterString("S"),
                new ObjectIdentifier(ObjectType.device, 99),
                null);

        ByteQueue queue = new ByteQueue();
        req.write(queue);

        assertEquals(req, new YouAreRequest(queue));
    }

    @Test
    public void macOnly() throws BACnetException {
        YouAreRequest req = new YouAreRequest(
                new Unsigned16(1),
                new CharacterString("M"),
                new CharacterString("S"),
                null,
                new OctetString(new byte[] {1, 2, 3}));

        ByteQueue queue = new ByteQueue();
        req.write(queue);

        assertEquals(req, new YouAreRequest(queue));
    }
}
