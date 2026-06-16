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
import com.serotonin.bacnet4j.type.enumerated.AuthorizationDecision;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthorizationEvent extends BaseType {
    private final DateTime timestamp;
    private final Address address;
    private final AuthenticationClient client;
    private final AccessToken token;
    private final AuthorizationDecision decision;
    private final CharacterString decisionDetails;

    public AuthorizationEvent(
            DateTime timestamp,
            Address address,
            AuthenticationClient client,
            AccessToken token,
            AuthorizationDecision decision,
            CharacterString decisionDetails) {
        this.timestamp = timestamp;
        this.address = address;
        this.client = client;
        this.token = token;
        this.decision = decision;
        this.decisionDetails = decisionDetails;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, address, 1);
        writeOptional(queue, client, 2);
        writeOptional(queue, token, 3);
        write(queue, decision, 4);
        writeOptional(queue, decisionDetails, 5);
    }

    @Override
    public String toString() {
        return "AuthorizationEvent [" +
                "timestamp=" + timestamp +
                ", address=" + address +
                ", client=" + client +
                ", token=" + token +
                ", decision=" + decision +
                ", decisionDetails=" + decisionDetails +
                ']';
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public Address getAddress() {
        return address;
    }

    public AuthenticationClient getClient() {
        return client;
    }

    public AccessToken getToken() {
        return token;
    }

    public AuthorizationDecision getDecision() {
        return decision;
    }

    public CharacterString getDecisionDetails() {
        return decisionDetails;
    }

    public AuthorizationEvent(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        address = read(queue, Address.class, 1);
        client = readOptional(queue, AuthenticationClient.class, 2);
        token = readOptional(queue, AccessToken.class, 3);
        decision = read(queue, AuthorizationDecision.class, 4);
        decisionDetails = readOptional(queue, CharacterString.class, 5);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthorizationEvent that = (AuthorizationEvent) o;
        return Objects.equals(timestamp, that.timestamp) && Objects.equals(address,
                that.address) && Objects.equals(client, that.client) && Objects.equals(token,
                that.token) && Objects.equals(decision, that.decision) && Objects.equals(
                decisionDetails, that.decisionDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, address, client, token, decision, decisionDetails);
    }
}
