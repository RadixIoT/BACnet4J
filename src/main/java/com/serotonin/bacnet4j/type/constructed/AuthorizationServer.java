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

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthorizationServer extends BaseType {
    private final Unsigned32 authServer;
    private final OctetString signingKey1;
    private final OctetString signingKey2;

    public AuthorizationServer(Unsigned32 authServer, OctetString signingKey1, OctetString signingKey2) {
        this.authServer = authServer;
        this.signingKey1 = signingKey1;
        this.signingKey2 = signingKey2;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, authServer, 0);
        writeOptional(queue, signingKey1, 1);
        writeOptional(queue, signingKey2, 2);
    }

    @Override
    public String toString() {
        return "AuthorizationServer [" +
                "authServer=" + authServer +
                ", signingKey1=" + signingKey1 +
                ", signingKey2=" + signingKey2 +
                ']';
    }

    public Unsigned32 getAuthServer() {
        return authServer;
    }

    public OctetString getSigningKey1() {
        return signingKey1;
    }

    public OctetString getSigningKey2() {
        return signingKey2;
    }

    public AuthorizationServer(final ByteQueue queue) throws BACnetException {
        authServer = read(queue, Unsigned32.class, 0);
        signingKey1 = readOptional(queue, OctetString.class, 1);
        signingKey2 = readOptional(queue, OctetString.class, 2);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthorizationServer that = (AuthorizationServer) o;
        return Objects.equals(authServer, that.authServer) && Objects.equals(signingKey1,
                that.signingKey1) && Objects.equals(signingKey2, that.signingKey2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authServer, signingKey1, signingKey2);
    }
}
