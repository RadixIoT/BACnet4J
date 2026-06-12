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
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AccessToken extends BaseType {
    private final Unsigned32 issuer;
    private final DateTime issued;
    private final SequenceOf<SignedInteger> audience;
    private final DateTime notBefore;
    private final DateTime notAfter;
    private final Unsigned32 client;
    private final AuthorizationConstraint constraint;
    private final AuthorizationScope scope;
    private final Unsigned8 keyId;
    private final OctetString signature;

    public AccessToken(
            Unsigned32 issuer,
            DateTime issued,
            SequenceOf<SignedInteger> audience,
            DateTime notBefore,
            DateTime notAfter,
            Unsigned32 client,
            AuthorizationConstraint constraint,
            AuthorizationScope scope,
            Unsigned8 keyId,
            OctetString signature) {
        this.issuer = issuer;
        this.issued = issued;
        this.audience = audience;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.client = client;
        this.constraint = constraint;
        this.scope = scope;
        this.keyId = keyId;
        this.signature = signature;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, issuer, 0);
        write(queue, issued, 1);
        write(queue, audience, 2);
        writeOptional(queue, notBefore, 3);
        writeOptional(queue, notAfter, 4);
        write(queue, client, 5);
        write(queue, constraint, 6);
        write(queue, scope, 7);
        write(queue, keyId, 8);
        write(queue, signature, 9);
    }

    @Override
    public String toString() {
        return "AccessToken [" +
                "issuer=" + issuer +
                ", issued=" + issued +
                ", audience=" + audience +
                ", notBefore=" + notBefore +
                ", notAfter=" + notAfter +
                ", client=" + client +
                ", constraint=" + constraint +
                ", scope=" + scope +
                ", keyId=" + keyId +
                ", signature=" + signature +
                ']';
    }

    public Unsigned32 getIssuer() {
        return issuer;
    }

    public DateTime getIssued() {
        return issued;
    }

    public SequenceOf<SignedInteger> getAudience() {
        return audience;
    }

    public DateTime getNotBefore() {
        return notBefore;
    }

    public DateTime getNotAfter() {
        return notAfter;
    }

    public Unsigned32 getClient() {
        return client;
    }

    public AuthorizationConstraint getConstraint() {
        return constraint;
    }

    public AuthorizationScope getScope() {
        return scope;
    }

    public Unsigned8 getKeyId() {
        return keyId;
    }

    public OctetString getSignature() {
        return signature;
    }

    public AccessToken(final ByteQueue queue) throws BACnetException {
        issuer = read(queue, Unsigned32.class, 0);
        issued = read(queue, DateTime.class, 1);
        audience = readSequenceOf(queue, SignedInteger.class, 2);
        notBefore = readOptional(queue, DateTime.class, 3);
        notAfter = readOptional(queue, DateTime.class, 4);
        client = read(queue, Unsigned32.class, 5);
        constraint = read(queue, AuthorizationConstraint.class, 6);
        scope = read(queue, AuthorizationScope.class, 7);
        keyId = read(queue, Unsigned8.class, 8);
        signature = read(queue, OctetString.class, 9);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AccessToken that = (AccessToken) o;
        return Objects.equals(issuer, that.issuer) && Objects.equals(issued,
                that.issued) && Objects.equals(audience, that.audience) && Objects.equals(notBefore,
                that.notBefore) && Objects.equals(notAfter, that.notAfter) && Objects.equals(client,
                that.client) && Objects.equals(constraint, that.constraint) && Objects.equals(scope,
                that.scope) && Objects.equals(keyId, that.keyId) && Objects.equals(signature,
                that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issuer, issued, audience, notBefore, notAfter, client, constraint, scope, keyId, signature);
    }
}
