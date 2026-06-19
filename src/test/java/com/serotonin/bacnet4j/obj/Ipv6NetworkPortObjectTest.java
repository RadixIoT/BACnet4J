/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.npdu.ipv6.Ipv6NetworkBuilder;
import com.serotonin.bacnet4j.npdu.ipv6.Ipv6NetworkUtils;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.enumerated.IPMode;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;

public class Ipv6NetworkPortObjectTest {
    private static final String MULTICAST_ADDRESS = "FF05::BAC0";
    private static final String LOCAL_BIND_ADDRESS = "::1";
    private static final int PORT = 47811;

    @Before
    public void before() throws Exception {
        boolean canRun = true;
        try (MulticastSocket socket = new MulticastSocket(new InetSocketAddress(LOCAL_BIND_ADDRESS, 0))) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            NetworkInterface netIf = NetworkInterface.getByInetAddress(group);   // the interface to join on

            InetSocketAddress groupSockAddr = new InetSocketAddress(group, PORT);
            socket.joinGroup(groupSockAddr, netIf);
        } catch (Exception e) {
            canRun = false;
        }
        Assume.assumeTrue(canRun);
    }

    @Test
    public void ensureProperties() throws Exception {
        var network = new Ipv6NetworkBuilder(MULTICAST_ADDRESS)
                .port(PORT)
                .localBindAddress(LOCAL_BIND_ADDRESS)
                .localNetworkNumber(123)
                .build();
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network))) {
            localDevice.initialize();
            var npo = localDevice.addObject(new Ipv6NetworkPortObject(network, 12, "Ipv6Network"));

            assertEquals(ObjectType.networkPort, npo.readProperty(PropertyIdentifier.objectType));
            assertEquals(new ObjectIdentifier(ObjectType.networkPort, 12),
                    npo.readProperty(PropertyIdentifier.objectIdentifier));
            assertEquals(new CharacterString("Ipv6Network"), npo.readProperty(PropertyIdentifier.objectName));
            assertEquals(new Unsigned16(123), npo.readProperty(PropertyIdentifier.networkNumber));
            assertEquals(NetworkNumberQuality.unknown, npo.readProperty(PropertyIdentifier.networkNumberQuality));
            assertEquals(MaxApduLength.UP_TO_1476.getMaxLength(), npo.readProperty(PropertyIdentifier.apduLength));
            assertEquals(new OctetString(new byte[] {0, 0, 1}), npo.readProperty(PropertyIdentifier.macAddress));
            assertEquals(IPMode.normal, npo.readProperty(PropertyIdentifier.bacnetIpv6Mode));
            assertEquals(new Unsigned16(PORT), npo.readProperty(PropertyIdentifier.bacnetIpv6UdpPort));
            assertEquals(Ipv6NetworkUtils.toOctetString(InetAddress.getByName(MULTICAST_ADDRESS).getAddress(), PORT),
                    npo.readProperty(PropertyIdentifier.bacnetIpv6MulticastAddress));
        }
    }
}