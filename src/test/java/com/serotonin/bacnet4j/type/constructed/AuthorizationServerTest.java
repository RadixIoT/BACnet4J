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
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthorizationServerTest {
    @Test
    public void allFields() throws BACnetException {
        final AuthorizationServer server = new AuthorizationServer(
                new Unsigned32(101),
                new OctetString(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}),
                new OctetString(new byte[] {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd}));

        final ByteQueue queue = new ByteQueue();
        server.write(queue);

        assertEquals(server, new AuthorizationServer(queue));
    }

    @Test
    public void optionalsAbsent() throws BACnetException {
        final AuthorizationServer server = new AuthorizationServer(new Unsigned32(0x3FFFFF), null, null);

        final ByteQueue queue = new ByteQueue();
        server.write(queue);

        assertEquals(server, new AuthorizationServer(queue));
    }
}
