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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.MessageValidationException;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * A BVLC header is four octets: type, function, and a two octet length. They used to be read before anything
 * checked that they were present, so a datagram shorter than the header ran the queue empty and failed with an
 * ArrayIndexOutOfBoundsException naming an array index, rather than as the malformed message it is.
 */
public class IpNetworkBvlcHeaderTest {
    private static final int PORT = 47806;

    private IpNetwork network;
    private OctetString linkService;

    @Before
    public void before() throws Exception {
        network = spy(new IpNetworkBuilder() //
                .withLocalBindAddress("1.2.3.4") //
                .withBroadcast("1.2.3.255", 24) //
                .withPort(PORT) //
                .build());

        doNothing().when(network).listen(any());
        doReturn(null).when(network).createSocket(any());
        doReturn(null).when(network).parseNpduData(any(), any());

        Transport transport = mock(Transport.class);
        doReturn(mock(LocalDevice.class)).when(transport).getLocalDevice();
        network.initialize(transport);

        linkService = IpNetworkUtils.toOctetString("1.2.3.9", PORT);
    }

    @Test
    public void datagramShorterThanTheBvlcHeader() {
        assertTruncated(new byte[] {});
        assertTruncated(new byte[] {IpNetwork.BVLC_TYPE});
        assertTruncated(new byte[] {IpNetwork.BVLC_TYPE, 0x4});
        assertTruncated(new byte[] {IpNetwork.BVLC_TYPE, 0x4, 0x0});
    }

    /**
     * A complete header is still accepted, so the check is not simply rejecting everything. Original-Unicast-NPDU
     * carries nothing between the header and the NPDU itself, so a bare header is a complete message for it.
     */
    @Test
    public void completeBvlcHeaderIsAccepted() throws Exception {
        ByteQueue queue = new ByteQueue();
        queue.push(IpNetwork.BVLC_TYPE);
        queue.push(0xa); // Original-Unicast-NPDU
        queue.pushShort((short) 4); // Length, counting the header itself

        network.handleIncomingDataImpl(queue, linkService);
    }

    private void assertTruncated(byte[] data) {
        ByteQueue queue = new ByteQueue(data);
        MessageValidationException e = assertThrows(MessageValidationException.class,
                () -> network.handleIncomingDataImpl(queue, linkService));
        assertTrue(e.getMessage(), e.getMessage().startsWith("Truncated BVLC header"));
    }
}
