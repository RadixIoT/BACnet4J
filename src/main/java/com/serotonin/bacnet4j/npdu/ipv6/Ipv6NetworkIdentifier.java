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

package com.serotonin.bacnet4j.npdu.ipv6;

import com.serotonin.bacnet4j.npdu.NetworkIdentifier;

public class Ipv6NetworkIdentifier extends NetworkIdentifier {
    private final String multicastAddress;
    private final int port;
    private final String localBindAddress;

    public Ipv6NetworkIdentifier(String multicastAddress, int port, String localBindAddress) {
        this.multicastAddress = multicastAddress;
        this.port = port;
        this.localBindAddress = localBindAddress;
    }

    public String getMulticastAddress() {
        return multicastAddress;
    }

    public int getPort() {
        return port;
    }

    public String getLocalBindAddress() {
        return localBindAddress;
    }

    @Override
    public String getIdString() {
        return "[" + multicastAddress + "]:" + port + " @ " + localBindAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((localBindAddress == null) ? 0 : localBindAddress.hashCode());
        result = prime * result + ((multicastAddress == null) ? 0 : multicastAddress.hashCode());
        result = prime * result + port;
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
        Ipv6NetworkIdentifier other = (Ipv6NetworkIdentifier) obj;
        if (localBindAddress == null) {
            if (other.localBindAddress != null)
                return false;
        } else if (!localBindAddress.equals(other.localBindAddress))
            return false;
        if (multicastAddress == null) {
            if (other.multicastAddress != null)
                return false;
        } else if (!multicastAddress.equals(other.multicastAddress))
            return false;
        if (port != other.port)
            return false;
        return true;
    }
}
