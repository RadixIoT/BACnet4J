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
import com.serotonin.bacnet4j.type.enumerated.AuthenticationDecision;
import com.serotonin.bacnet4j.type.enumerated.AuthorizationDecision;
import com.serotonin.bacnet4j.type.enumerated.AuthorizationPosture;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthorizationStatusTest {
    private static DateTime ts() {
        return new DateTime(new Date(2026, Month.JUNE, 12, DayOfWeek.FRIDAY), new Time(12, 0, 0, 0));
    }

    private static AuthenticationEvent authEvent(AuthenticationDecision decision) {
        return new AuthenticationEvent(
                ts(),
                new AuthenticationPeer(
                        new HostNPort(new HostAddress(new OctetString(new byte[] {10, 0, 0, 1})),
                                new Unsigned16(47808)),
                        new Unsigned32(7777),
                        Boolean.TRUE, Boolean.FALSE, Boolean.TRUE),
                new AuthenticationClient(Boolean.TRUE, new Unsigned32(12345)),
                decision,
                null);
    }

    private static AuthorizationEvent authzEvent(AuthorizationDecision decision) {
        return new AuthorizationEvent(
                ts(),
                new Address(123, new OctetString(new byte[] {0, 0, 1})),
                null, null,
                decision,
                null);
    }

    @Test
    public void allFields() throws BACnetException {
        final AuthorizationStatus s = new AuthorizationStatus(
                AuthorizationPosture.configured,
                new ErrorClassAndCode(ErrorClass.security, ErrorCode.badSignature),
                new ObjectPropertyReference(new ObjectIdentifier(ObjectType.device, 1),
                        PropertyIdentifier.authorizationServer),
                new CharacterString("signing key not found"),
                new SequenceOf<>(authEvent(AuthenticationDecision.allowMatch)),
                new SequenceOf<>(authEvent(AuthenticationDecision.denyMismatch)),
                new SequenceOf<>(authzEvent(AuthorizationDecision.allowByLocalPolicy)),
                new SequenceOf<>(authzEvent(AuthorizationDecision.denyIssuer)));

        final ByteQueue queue = new ByteQueue();
        s.write(queue);

        assertEquals(s, new AuthorizationStatus(queue));
    }

    @Test
    public void minimalFields() throws BACnetException {
        final AuthorizationStatus s = new AuthorizationStatus(
                AuthorizationPosture.open, null, null, null, null, null, null, null);

        final ByteQueue queue = new ByteQueue();
        s.write(queue);

        assertEquals(s, new AuthorizationStatus(queue));
    }
}
