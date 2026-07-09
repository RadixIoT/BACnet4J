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
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthorizationPolicy extends BaseType {
    private final DateTime notBefore;
    private final DateTime notAfter;
    private final SequenceOf<Unsigned32> clients;
    private final AuthorizationConstraint constraint;
    private final AuthorizationScope scope;

    public AuthorizationPolicy(
            DateTime notBefore,
            DateTime notAfter,
            SequenceOf<Unsigned32> clients,
            AuthorizationConstraint constraint,
            AuthorizationScope scope) {
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.clients = clients;
        this.constraint = constraint;
        this.scope = scope;
    }

    @Override
    public void write(ByteQueue queue) {
        writeOptional(queue, notBefore, 0);
        writeOptional(queue, notAfter, 1);
        write(queue, clients, 2);
        write(queue, constraint, 3);
        write(queue, scope, 4);
    }

    @Override
    public String toString() {
        return "AuthorizationPolicy [" +
                "notBefore=" + notBefore +
                ", notAfter=" + notAfter +
                ", clients=" + clients +
                ", constraint=" + constraint +
                ", scope=" + scope +
                ']';
    }

    public DateTime getNotBefore() {
        return notBefore;
    }

    public DateTime getNotAfter() {
        return notAfter;
    }

    public SequenceOf<Unsigned32> getClients() {
        return clients;
    }

    public AuthorizationConstraint getConstraint() {
        return constraint;
    }

    public AuthorizationScope getScope() {
        return scope;
    }

    public AuthorizationPolicy(ByteQueue queue) throws BACnetException {
        notBefore = readOptional(queue, DateTime.class, 0);
        notAfter = readOptional(queue, DateTime.class, 1);
        clients = readSequenceOf(queue, Unsigned32.class, 2);
        constraint = read(queue, AuthorizationConstraint.class, 3);
        scope = read(queue, AuthorizationScope.class, 4);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthorizationPolicy that = (AuthorizationPolicy) o;
        return Objects.equals(notBefore, that.notBefore) && Objects.equals(notAfter,
                that.notAfter) && Objects.equals(clients, that.clients) && Objects.equals(constraint,
                that.constraint) && Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notBefore, notAfter, clients, constraint, scope);
    }
}
