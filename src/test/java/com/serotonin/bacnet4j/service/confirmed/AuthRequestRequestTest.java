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
import com.serotonin.bacnet4j.service.confirmed.AuthRequestRequest.TokenRequest;
import com.serotonin.bacnet4j.type.constructed.AuthorizationScope;
import com.serotonin.bacnet4j.type.constructed.AuthorizationScope.Standard;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthRequestRequestTest {
    @Test
    public void tokenRequestRoundTrip() throws BACnetException {
        AuthRequestRequest req = new AuthRequestRequest(new TokenRequest(
                new Unsigned32(101),
                new SequenceOf<>(new SignedInteger(1), new SignedInteger(2), new SignedInteger(-1)),
                new AuthorizationScope(
                        new Standard(true, true, false, false, false, false, false, false, false,
                                false, false, false, false, false, false, false,
                                false, false, false, false, false, false, false, false),
                        new SequenceOf<>(new CharacterString("scope-a")))));

        ByteQueue queue = new ByteQueue();
        req.write(queue);

        assertEquals(req, new AuthRequestRequest(queue));
    }
}
