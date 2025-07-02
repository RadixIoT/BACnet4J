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

import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;

public class UnackedMessageKey {
    private final Address address;
    private final OctetString linkService;
    private final byte invokeId;
    private final boolean fromServer;

    public UnackedMessageKey(Address address, OctetString linkService, byte invokeId, boolean fromServer) {
        this.address = address;
        this.linkService = linkService;
        this.invokeId = invokeId;
        this.fromServer = fromServer;
    }

    public Address getAddress() {
        return address;
    }

    public OctetString getLinkService() {
        return linkService;
    }

    public byte getInvokeId() {
        return invokeId;
    }

    public boolean isFromServer() {
        return fromServer;
    }

    @Override
    public String toString() {
        return "Key(address=" + address + ", linkService=" + linkService + ", invokeId=" + invokeId + ", fromServer="
                + fromServer + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + (fromServer ? 1231 : 1237);
        result = prime * result + invokeId;
        result = prime * result + ((linkService == null) ? 0 : linkService.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UnackedMessageKey other = (UnackedMessageKey) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (fromServer != other.fromServer)
            return false;
        if (invokeId != other.invokeId)
            return false;
        if (linkService == null) {
            if (other.linkService != null)
                return false;
        } else if (!linkService.equals(other.linkService))
            return false;
        return true;
    }
}
