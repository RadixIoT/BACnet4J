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
package com.serotonin.bacnet4j.npdu.test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.MessageValidationException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.NetworkSourceAddress;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A network that is useful for unit tests as it simulates a BACnet network within
 * a single process. The static <code>instances</code> field keeps track of all of
 * the currently available networks,
 */
public class TestNetwork extends AbstractTestNetwork<TestNetwork> implements Runnable {
    static final Logger LOG = LoggerFactory.getLogger(TestNetwork.class);

    private volatile boolean running = true;
    private Thread thread;

    /**
     * This is the list of outgoing messages queued up for sending.
     */
    private final Queue<SendData> queue = new ConcurrentLinkedQueue<>();
    private long bytesOut;
    private long bytesIn;

    public TestNetwork(final TestNetworkMap map, final int address, final int sendDelay) {
        this(map, new NetworkSourceAddress(Address.LOCAL_NETWORK, new byte[] { (byte) address }), sendDelay);
    }

    public TestNetwork(final TestNetworkMap map, final Address address, final int sendDelay) {
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
    public void initialize(final Transport transport) throws Exception {
        super.initialize(transport);
        running = true;
        thread = new Thread(this,
                "BACnet4J test network for address " + (address.getMacAddress().getBytes()[0] & 0xff));
        thread.start();
    }

    @Override
    public void terminate() {
        super.terminate();
        running = false;
        ThreadUtils.notifySync(queue);
        if (thread != null)
            ThreadUtils.join(thread);
    }

    @Override
    public void sendNPDU(final Address recipient, final OctetString router, final ByteQueue npdu,
            final boolean broadcast, final boolean expectsReply) throws BACnetException {
        final SendData d = new SendData();
        d.recipient = recipient;
        d.data = npdu.popAll();

        queue.add(d);
        ThreadUtils.notifySync(queue);
    }

    @Override
    public void run() {
        while (running) {
            // Check for a message to send.
            final SendData d = queue.poll();

            if (d == null)
                ThreadUtils.waitSync(queue, 2);
            else {
                // Pause before handing off the message.
                ThreadUtils.sleep(sendDelay);

                if (d.recipient.equals(getLocalBroadcastAddress()) || d.recipient.equals(Address.GLOBAL)) {
                    // A broadcast. Send to everyone.
                    for (final TestNetwork network : networkMap)
                        receive(network, d.data);
                } else {
                    // A directed message. Find the network to pass it to.
                    final TestNetwork network = networkMap.get(d.recipient);
                    if (network != null)
                        receive(network, d.data);
                }
            }
        }
    }

    @Override
    protected NPDU handleIncomingDataImpl(final ByteQueue queue, final OctetString linkService)
            throws MessageValidationException {
        return parseNpduData(queue, linkService);
    }

    static class SendData {
        Address recipient;
        byte[] data;
    }
}
