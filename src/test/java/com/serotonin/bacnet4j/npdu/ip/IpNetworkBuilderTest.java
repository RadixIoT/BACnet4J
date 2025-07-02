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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IpNetworkBuilderTest {
    @Test
    public void withSubnet16() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withSubnet("192.168.0.0", 16);
        assertEquals("192.168.255.255", builder.getBroadcastAddress());
        assertEquals("255.255.0.0", builder.getSubnetMask());
    }

    @Test
    public void withBroadcast16() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast("192.168.255.255", 16);
        assertEquals("192.168.255.255", builder.getBroadcastAddress());
        assertEquals("255.255.0.0", builder.getSubnetMask());
    }

    @Test
    public void withSubnet24() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withSubnet("192.168.2.0", 24);
        assertEquals("192.168.2.255", builder.getBroadcastAddress());
        assertEquals("255.255.255.0", builder.getSubnetMask());
    }

    @Test
    public void withBroadcast24() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast("192.168.4.255", 24);
        assertEquals("192.168.4.255", builder.getBroadcastAddress());
        assertEquals("255.255.255.0", builder.getSubnetMask());
    }

    @Test
    public void withSubnet19() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withSubnet("192.168.192.0", 19);
        assertEquals("192.168.223.255", builder.getBroadcastAddress());
        assertEquals("255.255.224.0", builder.getSubnetMask());
    }

    @Test
    public void withBroadcast19() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast("192.168.223.255", 19);
        assertEquals("192.168.223.255", builder.getBroadcastAddress());
        assertEquals("255.255.224.0", builder.getSubnetMask());
    }
}
