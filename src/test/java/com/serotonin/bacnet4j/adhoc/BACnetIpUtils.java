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

package com.serotonin.bacnet4j.adhoc;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Terry Packer
 */
public class BACnetIpUtils {

    private BACnetIpUtils() {
    }

    public static void main(final String[] args) throws Exception {
        List<InterfaceAddress> usable = listUsableBACnetInterfaces();
        for (InterfaceAddress ifAddr : usable) {
            System.out.println("Address: " + ifAddr.getAddress());
            System.out.println("Broadcast: " + ifAddr.getBroadcast());
        }
    }

    /**
     * List all usable Interface addresses on the local machine.
     *
     * Usable: is not loopback, is up, has broadcast address
     *
     * @return
     * @throws SocketException
     */
    public static List<InterfaceAddress> listUsableBACnetInterfaces() throws SocketException {
        List<InterfaceAddress> usable = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }
            for (InterfaceAddress add : networkInterface.getInterfaceAddresses()) {
                if (add.getBroadcast() != null) {
                    usable.add(add);
                }

            }
        }
        return usable;
    }

}
