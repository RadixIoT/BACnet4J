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

package com.serotonin.bacnet4j.npdu.ip;

import static com.serotonin.bacnet4j.npdu.ip.IpNetworkUtils.toIpAddrString;

import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.util.BACnetUtils;

public class IpNetworkBuilder {
    private String localBindAddress = IpNetwork.DEFAULT_BIND_IP;
    private String broadcastAddress;
    private String subnetMask;
    private int port = IpNetwork.DEFAULT_PORT;
    private int localNetworkNumber = Address.LOCAL_NETWORK;
    private boolean reuseAddress = false;

    public IpNetworkBuilder withLocalBindAddress(final String localBindAddress) {
        this.localBindAddress = localBindAddress;
        return this;
    }

    /**
     * Either this method or withSubnet must be called.
     *
     * @param broadcastAddress    the broadcast address for the network
     * @param networkPrefixLength the number of bits in the local subnet.
     * @return this
     */
    public IpNetworkBuilder withBroadcast(final String broadcastAddress, final int networkPrefixLength) {
        this.broadcastAddress = broadcastAddress;
        this.subnetMask = toIpAddrString(IpNetworkUtils.createMask(networkPrefixLength));

        return this;
    }

    /**
     * Either this method or withBroadcast must be called.
     *
     * @param subnetAddress       the address of the local subnet, NOT the subnet mask., e.g. 192.168.0.0. The subnet
     *                            address is required because the given local bind address could be the wildcard
     *                            address, i.e. 0.0.0.0, from which the broadcast address cannot be calculated.
     * @param networkPrefixLength the number of bits in the local subnet.
     * @return this
     */
    public IpNetworkBuilder withSubnet(final String subnetAddress, final int networkPrefixLength) {
        final long subnetMask = IpNetworkUtils.createMask(networkPrefixLength);
        this.subnetMask = toIpAddrString(subnetMask);

        final long negMask = ~subnetMask & 0xFFFFFFFFL;
        final long subnet = IpNetworkUtils.bytesToLong(BACnetUtils.dottedStringToBytes(subnetAddress));

        this.broadcastAddress = toIpAddrString(subnet | negMask);

        return this;
    }

    public IpNetworkBuilder withPort(final int port) {
        this.port = port;
        return this;
    }

    public IpNetworkBuilder withLocalNetworkNumber(final int localNetworkNumber) {
        this.localNetworkNumber = localNetworkNumber;
        return this;
    }

    public IpNetworkBuilder withReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
        return this;
    }

    public String getLocalBindAddress() {
        return localBindAddress;
    }

    public String getBroadcastAddress() {
        return broadcastAddress;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public int getPort() {
        return port;
    }

    public int getLocalNetworkNumber() {
        return localNetworkNumber;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public IpNetwork build() {
        if (broadcastAddress == null || subnetMask == null) {
            throw new IllegalArgumentException("Either withBroadcast or withSubnet must be called.");
        }
        return new IpNetwork(port, localBindAddress, broadcastAddress, subnetMask, localNetworkNumber, reuseAddress);
    }
}
