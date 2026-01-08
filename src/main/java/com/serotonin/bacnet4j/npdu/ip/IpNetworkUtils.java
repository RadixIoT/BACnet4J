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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.BACnetUtils;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.IpAddressUtils;

public class IpNetworkUtils {
    public static OctetString toOctetString(final String dottedString) {
        final String s = dottedString.trim();
        final int colon = s.indexOf(":");
        if (colon == -1)
            throw new IllegalArgumentException("Dotted string missing port number. Expected x.x.x.x:port");

        final byte[] ip = BACnetUtils.dottedStringToBytes(s.substring(0, colon));
        final int port = Integer.parseInt(s.substring(colon + 1));
        return toOctetString(ip, port);
    }

    public static OctetString toOctetString(final byte[] ipAddress, final int port) {
        return new OctetString(toBytes(ipAddress, port));
    }

    public static OctetString toOctetString(final String dotted, final int port) {
        return toOctetString(BACnetUtils.dottedStringToBytes(dotted), port);
    }

    public static OctetString toOctetString(final ByteQueue queue) {
        final byte[] b = new byte[6];
        queue.pop(b);
        return new OctetString(b);
    }

    public static OctetString toOctetString(final InetSocketAddress addr) {
        return toOctetString(addr.getAddress().getAddress(), addr.getPort());
    }

    private static byte[] toBytes(final byte[] ipAddress, final int port) {
        if (ipAddress.length != 4)
            throw new IllegalArgumentException("IP address must have 4 parts, not " + ipAddress.length);

        final byte[] b = new byte[6];
        System.arraycopy(ipAddress, 0, b, 0, ipAddress.length);
        b[ipAddress.length] = (byte) (port >> 8);
        b[ipAddress.length + 1] = (byte) port;
        return b;
    }

    public static InetAddress getInetAddress(final OctetString mac) {
        try {
            return InetAddress.getByAddress(getIpBytes(mac));
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static InetSocketAddress getInetSocketAddress(final OctetString mac) {
        return InetAddrCache.get(getInetAddress(mac), getPort(mac));
    }

    public static int getPort(final OctetString mac) {
        if (mac.getLength() != 6)
            throw new IllegalArgumentException("Not an I/P mac");
        return (mac.getBytes()[4] & 0xff) << 8 | mac.getBytes()[5] & 0xff;
    }

    public static String toIpString(final OctetString mac) {
        return IpAddressUtils.toIpString(getIpBytes(mac));
    }

    public static String toIpPortString(final OctetString mac) {
        return toIpString(mac) + ":" + getPort(mac);
    }

    public static byte[] getIpBytes(final OctetString mac) {
        if (mac.getLength() != 6)
            throw new IllegalArgumentException("Not an I/P mac");
        final byte[] b = new byte[4];
        System.arraycopy(mac.getBytes(), 0, b, 0, 4);
        return b;
    }

    public static String toString(final OctetString mac) {
        return toIpPortString(mac);
    }

    public static Address toAddress(final byte[] ipAddress, final int port) {
        return toAddress(Address.LOCAL_NETWORK, ipAddress, port);
    }

    /**
     * Convenience constructor for IP addresses remote to this network.
     *
     * @param networkNumber
     * @param ipAddress
     * @param port
     * @return
     */
    public static Address toAddress(final int networkNumber, final byte[] ipAddress, final int port) {
        final byte[] ipMacAddress = new byte[ipAddress.length + 2];
        System.arraycopy(ipAddress, 0, ipMacAddress, 0, ipAddress.length);
        ipMacAddress[ipAddress.length] = (byte) (port >> 8);
        ipMacAddress[ipAddress.length + 1] = (byte) port;
        return new Address(networkNumber, new OctetString(ipMacAddress));
    }

    public static Address toAddress(final String host, final int port) {
        return toAddress(Address.LOCAL_NETWORK, host, port);
    }

    public static Address toAddress(final int networkNumber, final String host, final int port) {
        return toAddress(networkNumber, InetAddrCache.get(host, port));
    }

    public static Address toAddress(final InetSocketAddress addr) {
        return toAddress(Address.LOCAL_NETWORK, addr.getAddress().getAddress(), addr.getPort());
    }

    public static Address toAddress(final int networkNumber, final InetSocketAddress addr) {
        return toAddress(networkNumber, addr.getAddress().getAddress(), addr.getPort());
    }

    public static List<InterfaceAddress> getLocalInterfaceAddresses() {
        try {
            final List<InterfaceAddress> result = new ArrayList<>();
            for (final NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (final InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    if (!addr.getAddress().isLoopbackAddress())
                        result.add(addr);
                }
            }
            return result;
        } catch (final Exception e) {
            // Should never happen, so just wrap in a RuntimeException
            throw new RuntimeException(e);
        }
    }

    public static long bytesToLong(final byte[] ba) {
        return (ba[0] & 0xffL) << 24 | (ba[1] & 0xffL) << 16 | (ba[2] & 0xffL) << 8 | ba[3] & 0xffL;
    }

    public static long createMask(final int length) {
        long l = 0;
        int shift = 31;
        for (int i = 0; i < length; i++) {
            l |= 1L << shift--;
        }
        return l;
    }

    public static long toBroadcast(final long ipaddr, final long subnet) {
        return 0xFFFFFFFFL ^ subnet | ipaddr;
    }

    public static String toIpAddrString(final long addr) {
        final StringBuilder sb = new StringBuilder();
        sb.append(addr >> 24 & 0xFF).append('.');
        sb.append(addr >> 16 & 0xFF).append('.');
        sb.append(addr >> 8 & 0xFF).append('.');
        sb.append(addr & 0xFF);
        return sb.toString();
    }

    public static boolean matchWithMask(String address1, String address2, String mask) {
        return matchWithMask(IpAddressUtils.toIpAddress(address1), IpAddressUtils.toIpAddress(address2),
                IpAddressUtils.toIpAddress(mask));
    }

    /**
     * Determines if the given addresses are equal when applying the given mask. All arrays must have at least 4 bytes,
     * and only those 4 bytes are considered.
     *
     * @param address1 the first address to match
     * @param address2 the second address to match
     * @param mask     the mask to apply
     * @return true if the addresses match
     */
    public static boolean matchWithMask(byte[] address1, byte[] address2, byte[] mask) {
        for (int i = 0; i < 4; i++) {
            final int b1 = address1[i] & mask[i];
            final int b2 = address2[i] & mask[i];
            if (b1 != b2) {
                return false;
            }
        }
        return true;
    }
}
