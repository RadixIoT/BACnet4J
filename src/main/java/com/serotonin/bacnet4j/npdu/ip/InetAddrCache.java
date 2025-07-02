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
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class InetAddrCache {
    private static final Map<InetAddress, Map<Integer, InetSocketAddress>> socketCache = new HashMap<>();

    public static InetSocketAddress get(final String host, final int port) {
        try {
            return get(InetAddress.getByName(host), port);
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * InetSocketAddress cache, because instantiation can take up to 10 seconds on Android.
     * ??? Should there be a means of purging this?
     *
     * @param addr
     * @param port
     * @return
     */
    public static InetSocketAddress get(final InetAddress addr, final int port) {
        Map<Integer, InetSocketAddress> ports = socketCache.get(addr);
        if (ports == null) {
            synchronized (socketCache) {
                ports = socketCache.get(addr);
                if (ports == null) {
                    ports = new HashMap<>();
                    socketCache.put(addr, ports);
                }
            }
        }

        InetSocketAddress socket = ports.get(port);
        if (socket == null) {
            synchronized (ports) {
                socket = ports.get(port);
                if (socket == null) {
                    socket = new InetSocketAddress(addr, port);
                    ports.put(port, socket);
                }
            }
        }

        return socket;
    }
}
