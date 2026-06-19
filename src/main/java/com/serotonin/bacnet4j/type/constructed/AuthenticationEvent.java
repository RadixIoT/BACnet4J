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
import com.serotonin.bacnet4j.type.enumerated.AuthenticationDecision;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthenticationEvent extends BaseType {
    private final DateTime timestamp;
    private final AuthenticationPeer peer;
    private final AuthenticationClient client;
    private final AuthenticationDecision decision;
    private final CharacterString decisionDetails;

    public AuthenticationEvent(
            DateTime timestamp,
            AuthenticationPeer peer,
            AuthenticationClient client,
            AuthenticationDecision decision,
            CharacterString decisionDetails) {
        this.timestamp = timestamp;
        this.peer = peer;
        this.client = client;
        this.decision = decision;
        this.decisionDetails = decisionDetails;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, peer, 1);
        write(queue, client, 2);
        write(queue, decision, 3);
        writeOptional(queue, decisionDetails, 4);
    }

    @Override
    public String toString() {
        return "AuthenticationEvent [" +
                "timestamp=" + timestamp +
                ", peer=" + peer +
                ", client=" + client +
                ", decision=" + decision +
                ", decisionDetails=" + decisionDetails +
                ']';
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public AuthenticationPeer getPeer() {
        return peer;
    }

    public AuthenticationClient getClient() {
        return client;
    }

    public AuthenticationDecision getDecision() {
        return decision;
    }

    public CharacterString getDecisionDetails() {
        return decisionDetails;
    }

    public AuthenticationEvent(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        peer = read(queue, AuthenticationPeer.class, 1);
        client = read(queue, AuthenticationClient.class, 2);
        decision = read(queue, AuthenticationDecision.class, 3);
        decisionDetails = readOptional(queue, CharacterString.class, 4);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthenticationEvent that = (AuthenticationEvent) o;
        return Objects.equals(timestamp, that.timestamp) && Objects.equals(peer,
                that.peer) && Objects.equals(client, that.client) && Objects.equals(decision,
                that.decision) && Objects.equals(decisionDetails, that.decisionDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, peer, client, decision, decisionDetails);
    }
}
