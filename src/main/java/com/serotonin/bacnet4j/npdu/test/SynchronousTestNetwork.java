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
 * See www.radixiot.com for commercial license options.
 *
 */
package com.serotonin.bacnet4j.npdu.test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

import java.util.Objects;

/**
 * @author Terry Packer
 */
public class SynchronousTestNetwork extends AbstractTestNetwork {

    private long bytesOut;
    private long bytesIn;

    public SynchronousTestNetwork(TestNetworkMap<AbstractTestNetwork> map, int address, int sendDelay) {
        super(map, address, sendDelay);
    }


    @Override
    public long getBytesOut() {
        return bytesOut;
    }

    @Override
    public long getBytesIn() {
        return bytesIn;
    }

    @Override
    public void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast, boolean expectsReply) throws BACnetException {
        final TestNetwork.SendData d = new TestNetwork.SendData();
        d.recipient = recipient;
        d.data = npdu.popAll();
        bytesOut += d.data.length;

        ThreadUtils.sleep(sendDelay);

        if (d.recipient.equals(getLocalBroadcastAddress()) || d.recipient.equals(Address.GLOBAL)) {
            // A broadcast. Send to everyone.
            for (final AbstractTestNetwork network : networkMap)
                receive(network, d.data);
        } else {
            // A directed message. Find the network to pass it to.
            final AbstractTestNetwork network = networkMap.get(d.recipient);
            if (network != null)
                receive(network, d.data);
        }
    }

    @Override
    protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws Exception {
        return parseNpduData(queue, linkService);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SynchronousTestNetwork that = (SynchronousTestNetwork) o;
        return bytesOut == that.bytesOut && bytesIn == that.bytesIn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bytesOut, bytesIn);
    }
}
