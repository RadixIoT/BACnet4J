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

import com.serotonin.bacnet4j.ResponseConsumer;
import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.ConfirmedRequest;
import com.serotonin.bacnet4j.apdu.UnconfirmedRequest;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRecoverableException;
import com.serotonin.bacnet4j.exception.ServiceTooBigException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
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
    protected final Queue<DefaultTransport.DelayedOutgoing> delayedOutgoing = new LinkedList<>();

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
        for (final DefaultTransport.Outgoing og : outgoing) {
            if (og instanceof DefaultTransport.OutgoingConfirmed) {
                final DefaultTransport.OutgoingConfirmed ogc = (DefaultTransport.OutgoingConfirmed) og;
                if (ogc.consumer != null) {
                    ogc.consumer.ex(new BACnetException("Cancelled due to transport shutdown"));
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
    protected void sendUnconfirmedImpl(final Address address, final UnconfirmedRequestService service,
                                       final boolean broadcast) {
        outgoing.add(new OutgoingUnconfirmed(address, service, broadcast, new Exception()));
        ThreadUtils.notifySync(pauseLock);
    }

    @Override
    protected void sendConfirmedImpl(final Address address, final int maxAPDULengthAccepted,
                                     final Segmentation segmentationSupported,
                                     final ConfirmedRequestService service, final ResponseConsumer consumer) {
        outgoing.add(new OutgoingConfirmed(address, maxAPDULengthAccepted, segmentationSupported, service, consumer,
                new Exception()));
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
    protected void incomingImpl(NPDU npdu) {
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

    abstract class Outgoing {
        protected final Address address;
        protected OctetString linkService;
        // TODO remove this when it is no longer needed.
        protected final Exception stack;

        public Outgoing(final Address address, final Exception stack) {
            if (address == null)
                throw new IllegalArgumentException("address cannot be null");
            this.address = address;
            this.stack = stack;
        }

        void send() {
            // Check if the message is to be sent to a specific remote network.
            final int targetNetworkNumber = address.getNetworkNumber().intValue();
            if (targetNetworkNumber != Address.LOCAL_NETWORK && targetNetworkNumber != Address.ALL_NETWORKS
                    && targetNetworkNumber != network.getLocalNetworkNumber()) {
                // Going to a specific remote network. Check if we know the router for it.
                linkService = networkRouters.get(targetNetworkNumber);
                if (linkService == null) {
                    handleException(new BACnetException(
                            "Unable to find router to network " + address.getNetworkNumber().intValue()));
                    return;
                }
            }

            try {
                sendImpl();
            } catch (final BACnetRecoverableException e) {
                LOG.info("Send delayed due to recoverable error: {}", e.getMessage());
                delayedOutgoing.add(new DelayedOutgoing(this));
            } catch (final BACnetException e) {
                handleException(e);
            }
        }

        abstract protected void sendImpl() throws BACnetException;

        abstract protected void handleException(BACnetException e);
    }

    class OutgoingConfirmed extends Outgoing {
        private final int maxAPDULengthAccepted;
        private final Segmentation segmentationSupported;
        private final ConfirmedRequestService service;
        private final ResponseConsumer consumer;

        public OutgoingConfirmed(final Address address, final int maxAPDULengthAccepted,
                                 final Segmentation segmentationSupported, final ConfirmedRequestService service,
                                 final ResponseConsumer consumer, final Exception stack) {
            super(address, stack);
            this.maxAPDULengthAccepted = maxAPDULengthAccepted;
            this.segmentationSupported = segmentationSupported;
            this.service = service;
            this.consumer = consumer;
        }

        @Override
        protected void sendImpl() throws BACnetException {
            final ByteQueue serviceData = new ByteQueue();
            service.write(serviceData);

            final UnackedMessageContext ctx = new UnackedMessageContext(localDevice.getClock(), timeout, retries,
                    consumer, service);
            final UnackedMessageKey key;
            APDU apdu;

            // Check if we need to segment the message.
            if (serviceData.size() > maxAPDULengthAccepted - ConfirmedRequest.getHeaderSize(false)) {
                final int maxServiceData = maxAPDULengthAccepted - ConfirmedRequest.getHeaderSize(true);
                // Check if the device can accept what we want to send.
                if (segmentationSupported.intValue() == Segmentation.noSegmentation.intValue()
                        || segmentationSupported.intValue() == Segmentation.segmentedTransmit.intValue())
                    throw new ServiceTooBigException("Request too big to send to device without segmentation");
                final int segmentsRequired = serviceData.size() / maxServiceData + 1;
                if (segmentsRequired > 255)
                    throw new ServiceTooBigException("Request too big to send to device; too many segments required");

                key = unackedMessages.addClient(address, linkService, ctx);
                // Prepare the segmenting session.
                ctx.setSegmentTemplate(new ConfirmedRequest(true, true, true, MAX_SEGMENTS, network.getMaxApduLength(),
                        key.getInvokeId(), 0, segWindow, service.getChoiceId(), null, service.getNetworkPriority()));
                ctx.setServiceData(serviceData);
                ctx.setSegBuf(new byte[maxServiceData]);

                // Send an initial message to negotiate communication terms.
                apdu = ctx.getSegmentTemplate().clone(true, 0, segWindow, ctx.getNextSegment());
            } else {
                key = unackedMessages.addClient(address, linkService, ctx);
                // We can send the whole APDU in one shot.
                apdu = new ConfirmedRequest(false, false, true, MAX_SEGMENTS, network.getMaxApduLength(),
                        key.getInvokeId(), (byte) 0, 0, service.getChoiceId(), serviceData,
                        service.getNetworkPriority());
            }

            ctx.setOriginalApdu(apdu);
            sendForResponse(key, ctx);
        }

        @Override
        protected void handleException(final BACnetException e) {
            if (consumer == null) {
                LOG.warn("Error during send", e);
                LOG.warn("Original stack", stack);
            } else
                consumer.ex(e);
        }

        @Override
        public String toString() {
            return "OutgoingConfirmed [maxAPDULengthAccepted=" + maxAPDULengthAccepted + ", segmentationSupported="
                    + segmentationSupported + ", service=" + service + ", consumer=" + consumer + ", address=" + address
                    + ", linkService=" + linkService + "]";
        }
    }

    class OutgoingUnconfirmed extends Outgoing {
        private final UnconfirmedRequestService service;
        private final boolean broadcast;

        public OutgoingUnconfirmed(final Address address, final UnconfirmedRequestService service,
                                   final boolean broadcast, final Exception stack) {
            super(address, stack);
            this.service = service;
            this.broadcast = broadcast;
        }

        @Override
        protected void sendImpl() throws BACnetException {
            network.sendAPDU(address, linkService, new UnconfirmedRequest(service), broadcast);
        }

        @Override
        protected void handleException(final BACnetException e) {
            LOG.error("Error during send", e);
        }

        @Override
        public String toString() {
            return "OutgoingUnconfirmed [service=" + service + ", broadcast=" + broadcast + ", address=" + address
                    + ", linkService=" + linkService + "]";
        }
    }

    class DelayedOutgoing {
        final Outgoing outgoing;
        final long retryTime;

        public DelayedOutgoing(final Outgoing outgoing) {
            super();
            this.outgoing = outgoing;
            // Retry in 1 second.
            retryTime = localDevice.getClock().millis() + 1000;
        }

        boolean isReady() {
            return retryTime <= localDevice.getClock().millis();
        }
    }
}
