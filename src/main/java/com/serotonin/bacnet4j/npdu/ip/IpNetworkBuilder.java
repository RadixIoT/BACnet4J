/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Infinite Automation Software,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.infiniteautomation.com for commercial license options.
 *
 * @author Matthew Lohbihler
 */
package com.serotonin.bacnet4j.npdu.ip;

import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.util.BACnetUtils;
import org.apache.commons.lang3.SystemUtils;

import static com.serotonin.bacnet4j.npdu.ip.IpNetworkUtils.toIpAddrString;

public class IpNetworkBuilder {
    private String localBindAddress = IpNetwork.DEFAULT_BIND_IP;
    private String broadcastAddress;
    private String subnetMask;
    private int port = IpNetwork.DEFAULT_PORT;
    private int localNetworkNumber = Address.LOCAL_NETWORK;
    private static final boolean reuseAddress = !(SystemUtils.IS_OS_WINDOWS);

    public IpNetworkBuilder withLocalBindAddress(final String localBindAddress) {
        this.localBindAddress = localBindAddress;
        if (this.broadcastAddress == null)
            this.broadcastAddress = IpNetworkUtils.getLocalBroadcastAddressString(localBindAddress);
        if (this.subnetMask == null)
            this.subnetMask = IpNetworkUtils.getSubnetMask(localBindAddress);

        return this;
    }

    /**
     * Either this method or withSubnet must be called.
     *
     * @param broadcastAddress
     *            the broadcast address for the network
     * @param networkPrefixLength
     *            the number of bits in the local subnet.
     * @return this
     */
    public IpNetworkBuilder withBroadcast(final String broadcastAddress, final int networkPrefixLength) {
        this.broadcastAddress = broadcastAddress;
        this.subnetMask = toIpAddrString(IpNetworkUtils.createMask(networkPrefixLength));
        if (this.localBindAddress == null || 
            this.localBindAddress.equals(IpNetwork.DEFAULT_BIND_IP))
                this.localBindAddress = IpNetworkUtils.getIPAddressString(this.broadcastAddress);
        return this;
    }

    /**
     * Either this method or withBroadcast must be called.
     *
     * @param subnetAddress
     *            the address of the local subnet, NOT the subnet mask., e.g. 192.168.0.0. The subnet address is
     *            required because the given local bind address could be the wildcard address, i.e. 0.0.0.0, from
     *            which the broadcast address cannot be calculated.
     * @param networkPrefixLength
     *            the number of bits in the local subnet.
     * @return this
     */
    public IpNetworkBuilder withSubnet(final String subnetAddress, final int networkPrefixLength) {
        final long subnetMask = IpNetworkUtils.createMask(networkPrefixLength);
        this.subnetMask = toIpAddrString(subnetMask);

        final long negMask = ~subnetMask & 0xFFFFFFFFL;
        final long subnet = IpNetworkUtils.bytesToLong(BACnetUtils.dottedStringToBytes(subnetAddress));
        if (this.broadcastAddress == null)
            this.broadcastAddress = toIpAddrString(subnet | negMask);
        if (this.localBindAddress == null || 
            this.localBindAddress.equals(IpNetwork.DEFAULT_BIND_IP))
            this.localBindAddress = IpNetworkUtils.getIPAddressString(this.broadcastAddress);
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

       public IpNetworkBuilder withInterfaceName(final String ifaceName ){
        this.localBindAddress = IpNetworkUtils.getIPAddressString(ifaceName);
        this.broadcastAddress = IpNetworkUtils.getLocalBroadcastAddressString(ifaceName);
        this.subnetMask = IpNetworkUtils.getSubnetMask(ifaceName);
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
