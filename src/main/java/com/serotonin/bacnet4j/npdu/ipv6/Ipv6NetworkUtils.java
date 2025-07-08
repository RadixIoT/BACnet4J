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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.serotonin.bacnet4j.npdu.ip.InetAddrCache;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;

public class Ipv6NetworkUtils {
    public static OctetString toOctetString(byte[] addr, int port) {
        if (addr.length != 16)
            throw new IllegalArgumentException("Address must have 16 parts, not " + addr.length);

        byte[] b = new byte[18];
        System.arraycopy(addr, 0, b, 0, 16);
        b[16] = (byte) (port >> 8);
        b[17] = (byte) port;

        return new OctetString(b);
    }

    public static Address toAddress(int networkNumber, byte[] addr, int port) {
        return new Address(networkNumber, toOctetString(addr, port));
    }

    public static InetSocketAddress getInetSocketAddress(OctetString mac) {
        return InetAddrCache.get(getInetAddress(mac), getPort(mac));
    }

    public static InetAddress getInetAddress(OctetString mac) {
        try {
            return InetAddress.getByAddress(getIpBytes(mac));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getPort(OctetString mac) {
        if (mac.getLength() != 18)
            throw new IllegalArgumentException("Not an I/Pv6 mac");
        return ((mac.getBytes()[16] & 0xff) << 8) | (mac.getBytes()[17] & 0xff);
    }

    public static byte[] getIpBytes(OctetString mac) {
        if (mac.getLength() != 18)
            throw new IllegalArgumentException("Not an I/Pv6 mac");
        byte[] b = new byte[16];
        System.arraycopy(mac.getBytes(), 0, b, 0, 16);
        return b;
    }

    public static String toString(OctetString mac) {
        return toIpPortString(mac);
    }

    public static String toIpPortString(OctetString mac) {
        return "[" + toIpString(mac) + "]:" + getPort(mac);
    }

    public static String toIpString(OctetString mac) {
        byte[] ipBytes = getIpBytes(mac);
        try {
            return InetAddress.getByAddress(ipBytes).getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
