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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class IpNetworkDistributeBroadcastToNetworkTest {
    // Note that this is not the default port. This is to ensure the default isn't assumed when it shouldn't be.
    int port = 47806;
    LocalDevice localDevice;
    IpNetwork network;

    void setUp(boolean wildcard) throws Exception {
        var bindAddress = wildcard ? "0.0.0.0" : "1.2.3.4";

        network = spy(new IpNetworkBuilder()
                .withLocalBindAddress(bindAddress)
                .withBroadcast("1.2.3.255", 24)
                .withPort(port)
                .build());

        doNothing().when(network).listen(any());
        doReturn(null).when(network).createSocket(any());
        doReturn(null).when(network).parseNpduData(any(), any());
        doNothing().when(network).sendPacket(any(), any());
        doNothing().when(network).sendToBDT(any(), any());
        doReturn(List.of(
                new Address(IpNetworkUtils.toOctetString("0.0.0.0", port)),
                new Address(IpNetworkUtils.toOctetString("1.2.3.4", port))
        )).when(network).getLocalAddressList();

        localDevice = mock(LocalDevice.class);
        Transport transport = mock(Transport.class);
        doReturn(localDevice).when(transport).getLocalDevice();
        network.initialize(transport);
        network.enableBBMD();
    }

    byte[] getResponse(boolean nak) {
        final ByteQueue response = new ByteQueue();
        response.push(IpNetwork.BVLC_TYPE);
        response.push(0); // Response type
        response.pushU2B(6); // Length
        response.pushU2B(nak ? 0x60 : 0x0);
        return response.popAll();
    }

    @Test
    @Parameters({"false", "true"})
    public void unknownForeignDevice(boolean wildcard) throws Exception {
        setUp(wildcard);
        ByteQueue queue = new ByteQueue();
        queue.push(IpNetwork.BVLC_TYPE);
        queue.push(0x9); // Distribute-Broadcast-To-Network
        queue.pushShort((short) 4); // Length

        OctetString linkService = IpNetworkUtils.toOctetString("2.3.4.5", port);
        network.handleIncomingDataImpl(queue, linkService);

        verify(network, times(1)).sendPacket(any(), any());
        verify(network).sendPacket(IpNetworkUtils.getInetSocketAddress(linkService), getResponse(true));
        verify(network, never()).parseNpduData(any(), any());
    }

    @Test
    @Parameters({"false", "true"})
    public void bdtAndFdtDistribution(boolean wildcard) throws Exception {
        setUp(wildcard);
        doReturn(Clock.systemUTC()).when(localDevice).getClock();

        network.writeFDT(List.of(
                network.new FDTEntry(new InetSocketAddress("2.3.4.5", port), 1000),
                network.new FDTEntry(new InetSocketAddress("3.4.5.6", port), 1200)
        ));

        var otherBdtEntry = new IpNetwork.BDTEntry("1.2.4.4", port);
        network.writeBDT(List.of(new IpNetwork.BDTEntry("1.2.3.4", port), otherBdtEntry));

        ByteQueue queue = new ByteQueue();
        queue.push(IpNetwork.BVLC_TYPE);
        queue.push(0x9); // Distribute-Broadcast-To-Network
        queue.pushShort((short) 4); // Length

        OctetString linkService = IpNetworkUtils.toOctetString("2.3.4.5", port);
        network.handleIncomingDataImpl((ByteQueue) queue.clone(), linkService);

        ByteQueue fwd = new ByteQueue();
        fwd.push(IpNetwork.BVLC_TYPE);
        fwd.push(4); // Forward
        fwd.pushU2B(10); // Length
        fwd.push(linkService.getBytes()); // Origin
        var data = fwd.popAll();

        verify(network, times(3)).sendPacket(any(), any());
        verify(network).sendPacket(InetAddrCache.get("1.2.3.255", port), data);
        verify(network, times(1)).sendToBDT(any(), any());
        verify(network).sendToBDT(otherBdtEntry, data);
        verify(network).sendPacket(InetAddrCache.get("3.4.5.6", port), data);
        verify(network).sendPacket(IpNetworkUtils.getInetSocketAddress(linkService), getResponse(false));
        verify(network).parseNpduData(any(), any());
    }
}
