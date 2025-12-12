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

package com.serotonin.bacnet4j.npdu;

import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class NPDU {
    private final Address from;
    private final OctetString linkService;
    private final boolean networkMessage;
    private final int networkMessageType;
    private final ByteQueue queue;

    /**
     * Constructor for APDU messages.
     */
    public NPDU(Address from, OctetString linkService, ByteQueue queue) {
        this.from = from;
        this.linkService = linkService;
        this.networkMessage = false;
        this.networkMessageType = -1;
        this.queue = queue;
    }

    /**
     * Constructor for network messages.
     */
    public NPDU(Address from, OctetString linkService, int networkMessageType, ByteQueue queue) {
        this.from = from;
        this.linkService = linkService;
        this.networkMessage = true;
        this.networkMessageType = networkMessageType;
        this.queue = queue;
    }

    public Address getFrom() {
        return from;
    }

    public OctetString getLinkService() {
        return linkService;
    }

    public boolean isNetworkMessage() {
        return networkMessage;
    }

    public int getNetworkMessageType() {
        return networkMessageType;
    }

    public ByteQueue getNetworkMessageData() {
        return queue;
    }

    public APDU getAPDU(ServicesSupported servicesSupported) throws BACnetException {
        try {
            return APDU.createAPDU(servicesSupported, queue);
        } catch (BACnetException e) {
            // If it's already a BACnetException, don't bother wrapping it.
            throw e;
        } catch (Exception e) {
            throw new BACnetException("Error while creating APDU: ", e);
        }
    }

    @Override
    public String toString() {
        if (networkMessage)
            return "NPDU [from=" + from + ", linkService=" + linkService + ", networkMessageType=" + networkMessageType
                    + "]";
        return "NPDU [from=" + from + ", linkService=" + linkService + ", queue=" + queue + "]";
    }
}
