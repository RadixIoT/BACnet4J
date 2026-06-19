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
import com.serotonin.bacnet4j.type.constructed.AuthorizationConstraint.Authentication;
import com.serotonin.bacnet4j.type.constructed.AuthorizationConstraint.Origin;
import com.serotonin.bacnet4j.type.constructed.AuthorizationScope.Standard;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AccessTokenTest {
    @Test
    public void allFields() throws BACnetException {
        final AccessToken accessToken = new AccessToken(
                new Unsigned32(4321),
                new DateTime(new Date(2026, Month.JUNE, 12, DayOfWeek.FRIDAY), new Time(9, 15, 30, 0)),
                new SequenceOf<>(new SignedInteger(1234), new SignedInteger(-5), new SignedInteger(-1)),
                new DateTime(new Date(2026, Month.JUNE, 12, DayOfWeek.FRIDAY), new Time(0, 0, 0, 0)),
                new DateTime(new Date(2026, Month.DECEMBER, 31, DayOfWeek.THURSDAY), new Time(23, 59, 59, 99)),
                new Unsigned32(987),
                new AuthorizationConstraint(Origin.sameNetwork, Authentication.certified),
                new AuthorizationScope(
                        new Standard(
                                true, true, true, false, false, false, false, false, false,
                                false, false, false, false, false, false, false,
                                false, false, false, false, false, false, false, false),
                        new SequenceOf<>(new CharacterString("ext-scope-1"), new CharacterString("ext-scope-2"))),
                new Unsigned8(1),
                new OctetString(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));

        final ByteQueue queue = new ByteQueue();
        accessToken.write(queue);

        assertEquals(accessToken, new AccessToken(queue));
    }

    @Test
    public void optionalsAbsent() throws BACnetException {
        final AccessToken accessToken = new AccessToken(
                new Unsigned32(1),
                new DateTime(new Date(2026, Month.JANUARY, 1, DayOfWeek.THURSDAY), new Time(12, 0, 0, 0)),
                new SequenceOf<>(new SignedInteger(-1)),
                null,
                null,
                new Unsigned32(2),
                new AuthorizationConstraint(Origin.anyNetwork, Authentication.anyMethod),
                new AuthorizationScope(new Standard(), null),
                new Unsigned8(2),
                new OctetString(new byte[] {(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef}));

        final ByteQueue queue = new ByteQueue();
        accessToken.write(queue);

        assertEquals(accessToken, new AccessToken(queue));
    }
}
