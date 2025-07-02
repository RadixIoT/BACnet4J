/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.transport;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.exception.BACnetRecoverableException;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;

/**
 * This is a non-thread safe class for maintaining the list of pending requests at a local device. Access to this is
 * exclusively from Transport, which uses a single management thread.
 *
 * @author Matthew
 */
public class UnackedMessages {
    static final Logger LOG = LoggerFactory.getLogger(UnackedMessages.class);

    private final Map<UnackedMessageKey, UnackedMessageContext> requests = new HashMap<>();
    private byte nextInvokeId;

    /**
     * Add a new client-based request to the list of pending requests.
     */
    public UnackedMessageKey addClient(final Address address, final OctetString linkService,
            final UnackedMessageContext ctx) throws BACnetRecoverableException {
        UnackedMessageKey key;

        // Loop until we find a key that is available.
        int attempts = 256;
        while (true) {
            // We set the server value in the key to true so that it matches with the message from the server.
            key = new UnackedMessageKey(address, linkService, nextInvokeId++, true);

            if (requests.containsKey(key)) {
                // Key collision. Try again unless we've tried too many times.
                if (--attempts > 0)
                    continue;
                throw new BACnetRecoverableException(
                        "Cannot enter a client into the un-acked messages list. key=" + key);
            }

            // Found a good id. Use it and exit.
            requests.put(key, ctx);
            break;
        }

        return key;
    }

    /**
     * Add a new server-based request to the list of pending requests. This is used for segmented responses.
     */
    public UnackedMessageKey addServer(final Address address, final OctetString linkService, final byte id,
            final UnackedMessageContext ctx) throws BACnetRecoverableException {
        // We set the server value in the key to false so that it matches with the message from the client.
        final UnackedMessageKey key = new UnackedMessageKey(address, linkService, id, false);

        if (requests.containsKey(key))
            throw new BACnetRecoverableException("Cannot enter a server into the un-acked messages list. key=" + key);
        requests.put(key, ctx);

        return key;
    }

    public void add(final UnackedMessageKey key, final UnackedMessageContext value) {
        requests.put(key, value);
    }

    public UnackedMessageContext remove(final UnackedMessageKey key) {
        return requests.remove(key);
    }

    public Map<UnackedMessageKey, UnackedMessageContext> getRequests() {
        return requests;
    }

    @Override
    public String toString() {
        return "UnackedMessages [requests=" + requests + ", nextInvokeId=" + nextInvokeId + "]";
    }
}
