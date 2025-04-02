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
package com.serotonin.bacnet4j.transport;

import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Matthew
 */
public class DefaultTransport extends AbstractTransport implements Runnable {
    static final Logger LOG = LoggerFactory.getLogger(DefaultTransport.class);

    // Message queues
    protected final Queue<Outgoing> outgoing = new ConcurrentLinkedQueue<>();
    protected final Queue<NPDU> incoming = new ConcurrentLinkedQueue<>();
    protected final Queue<DelayedOutgoing> delayedOutgoing = new LinkedList<>();

    private Thread thread;
    private volatile boolean running = true;
    private final Object pauseLock = new Object();

    public DefaultTransport(final Network network) {
        super(network);
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        running = true;
        thread = new Thread(this, "BACnet4J transport for device " + localDevice.getInstanceNumber());
        thread.start();
    }

    @Override
    public void terminate() {
        // Stop the processing thread.
        running = false;
        ThreadUtils.notifySync(pauseLock);
        if (thread != null)
            ThreadUtils.join(thread);

        // Cancel any queued outgoing messages.
        for (final Outgoing og : outgoing) {
            if (og instanceof OutgoingConfirmed) {
                final OutgoingConfirmed ogc = (OutgoingConfirmed) og;
                if (ogc.getConsumer() != null) {
                    ogc.getConsumer().ex(new BACnetException("Cancelled due to transport shutdown"));
                }
            }
        }

        // Cancel any unacked messages
        for (final UnackedMessageContext ctx : unackedMessages.getRequests().values()) {
            if (ctx.getConsumer() != null) {
                ctx.getConsumer().ex(new BACnetException("Cancelled due to transport shutdown"));
            }
        }
        super.terminate();
    }

    //
    //
    // Adding new requests and responses.
    //


    @Override
    protected void sendOutgoingUnconfirmed(OutgoingUnconfirmed message) {
        outgoing.add(message);
        ThreadUtils.notifySync(pauseLock);
    }

    @Override
    protected void sendDelayedOutgoing(DelayedOutgoing message) {
        delayedOutgoing.add(message);
    }

    @Override
    protected void sendOutgoingConfirmed(OutgoingConfirmed message) {
        outgoing.add(message);
        ThreadUtils.notifySync(pauseLock);
    }

    @Override
    public ServiceFuture send(final Address address, final int maxAPDULengthAccepted,
            final Segmentation segmentationSupported, final ConfirmedRequestService service) {
        if (Thread.currentThread() == thread)
            throw new IllegalStateException("Cannot send future request in the transport thread. Use a callback " //
                    + "call instead, or make this call in a new thread.");
        final ServiceFutureImpl future = new ServiceFutureImpl();
        send(address, maxAPDULengthAccepted, segmentationSupported, service, future);
        return future;
    }

    @Override
    public void incoming(NPDU npdu) {
        incoming.add(npdu);
        ThreadUtils.notifySync(pauseLock);
    }

    //
    //
    // Processing
    //
    @Override
    public void run() {
        Outgoing out;
        NPDU in;
        boolean pause;

        while (running) {
            pause = true;

            // Send an outgoing message.
            out = outgoing.poll();
            if (out != null) {
                try {
                    out.send();
                } catch (final Exception e) {
                    LOG.error("Error during send: {}", out, e);
                    LOG.error("Original send stack", out.stack);
                }
                pause = false;
            }

            // Receive an incoming message.
            in = incoming.poll();
            if (in != null) {
                try {
                    receiveImpl(in);
                } catch (final Exception e) {
                    LOG.error("Error during receive: {}", in, e);
                }
                pause = false;
            }

            // Find delayed outgoings to retry.
            if (!delayedOutgoing.isEmpty()) {
                final Iterator<DelayedOutgoing> iter = delayedOutgoing.iterator();
                while (iter.hasNext()) {
                    final DelayedOutgoing delayedOutgoing = iter.next();
                    if (delayedOutgoing.isReady()) {
                        iter.remove();
                        outgoing.add(delayedOutgoing.outgoing);
                        LOG.info("Retrying delayed outgoing {}", delayedOutgoing.outgoing);
                        pause = false;
                    } else {
                        // No other entries in the list should be ready either
                        // since they were added chronologically.
                        break;
                    }
                }
            }

            if (pause && running) {
                try {
                    pause = expire();
                } catch (final Exception e) {
                    LOG.error("Error during expire messages: ", e);
                }
            }

            if (pause && running)
                ThreadUtils.waitSync(pauseLock, 50);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (network == null ? 0 : network.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DefaultTransport other = (DefaultTransport) obj;
        if (network == null) {
            if (other.network != null)
                return false;
        } else if (!network.equals(other.network))
            return false;
        return true;
    }

}
