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

package com.serotonin.bacnet4j.transport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.ResponseConsumer;
import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.Abort;
import com.serotonin.bacnet4j.apdu.AckAPDU;
import com.serotonin.bacnet4j.apdu.ComplexACK;
import com.serotonin.bacnet4j.apdu.ConfirmedRequest;
import com.serotonin.bacnet4j.apdu.Reject;
import com.serotonin.bacnet4j.apdu.SegmentACK;
import com.serotonin.bacnet4j.apdu.Segmentable;
import com.serotonin.bacnet4j.apdu.SimpleACK;
import com.serotonin.bacnet4j.apdu.UnconfirmedRequest;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.enums.MaxSegments;
import com.serotonin.bacnet4j.exception.BACnetAbortException;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRecoverableException;
import com.serotonin.bacnet4j.exception.BACnetRejectException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.exception.CommunicationDisabledException;
import com.serotonin.bacnet4j.exception.NotImplementedException;
import com.serotonin.bacnet4j.exception.ServiceTooBigException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.NetworkIdentifier;
import com.serotonin.bacnet4j.obj.DeviceObject;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable;
import com.serotonin.bacnet4j.service.unconfirmed.IAmRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.service.unconfirmed.YouAreRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.AbortReason;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.RejectReason;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

public class DefaultTransport implements Transport, Runnable {
    static final Logger LOG = LoggerFactory.getLogger(DefaultTransport.class);

    final Map<Integer, OctetString> networkRouters = new ConcurrentHashMap<>();

    // Configuration
    private LocalDevice localDevice;
    final Network network;
    int timeout = DEFAULT_TIMEOUT;
    int retries = DEFAULT_RETRIES;
    int segTimeout = DEFAULT_SEG_TIMEOUT;
    int segWindow = DEFAULT_SEG_WINDOW;
    ServicesSupported servicesSupported;

    // Message queues
    private final Queue<Outgoing> outgoing = new ConcurrentLinkedQueue<>();
    private final Queue<NPDU> incoming = new ConcurrentLinkedQueue<>();
    private final Queue<DelayedOutgoing> delayedOutgoing = new ConcurrentLinkedQueue<>();

    // Processing
    final UnackedMessages unackedMessages = new UnackedMessages();
    private Thread thread;
    private volatile boolean running = true;
    private final Object runLock = new Object();
    private final Object pauseLock = new Object();

    public DefaultTransport(Network network) {
        this.network = network;
    }

    //
    //
    // Configuration
    //
    @Override
    public NetworkIdentifier getNetworkIdentifier() {
        return network.getNetworkIdentifier();
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setSegTimeout(int segTimeout) {
        this.segTimeout = segTimeout;
    }

    @Override
    public int getSegTimeout() {
        return segTimeout;
    }

    @Override
    public void setRetries(int retries) {
        this.retries = retries;
    }

    @Override
    public int getRetries() {
        return retries;
    }

    @Override
    public void setSegWindow(int segWindow) {
        // The proposed window size is encoded in a single octet and clause 20.1.2 restricts it to 1 to 127. A value
        // outside that range would be rejected by the peer with a windowSizeOutOfRange abort, so it is clamped here
        // rather than allowed to make segmentation fail at run time.
        if (SegmentSequence.isValidWindowSize(segWindow)) {
            this.segWindow = segWindow;
        } else {
            this.segWindow = segWindow < SegmentSequence.MIN_WINDOW_SIZE ? SegmentSequence.MIN_WINDOW_SIZE
                    : SegmentSequence.MAX_WINDOW_SIZE;
            LOG.warn("Segment window of {} is out of the range {} to {}; using {}", segWindow,
                    SegmentSequence.MIN_WINDOW_SIZE, SegmentSequence.MAX_WINDOW_SIZE, this.segWindow);
        }
    }

    @Override
    public int getSegWindow() {
        return segWindow;
    }

    /**
     * The greatest number of segments this device will assemble from one message, which is the meaning clause
     * 12.11.20 gives to Max_Segments_Accepted. It does not bound what this device sends.
     * <p>
     * This is read from the local device's Max_Segments_Accepted property on each use rather than cached, because
     * that property is what remote devices read to learn the limit, and client code may write to it. A cached copy
     * could disagree with what this device advertises.
     *
     * @return the segment limit, never less than two
     */
    int getMaxSegments() {
        Encodable value = localDevice == null ? null : localDevice.get(PropertyIdentifier.maxSegmentsAccepted);
        if (value instanceof UnsignedInteger u && u.intValue() >= 2)
            return u.intValue();

        // The property is absent, of the wrong type, or below the minimum that clause 12.11.20 allows for a device
        // that receives segmented messages. Fall back rather than leave the message unbounded.
        return DeviceObject.DEFAULT_MAX_SEGMENTS_ACCEPTED;
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public LocalDevice getLocalDevice() {
        return localDevice;
    }

    @Override
    public void setLocalDevice(LocalDevice localDevice) {
        this.localDevice = localDevice;
    }

    @Override
    public void initialize() throws BACnetException {
        servicesSupported = localDevice.getServicesSupported();

        synchronized (runLock) {
            running = true;
        }
        network.initialize(this);
        thread = new Thread(this, "BACnet4J transport for device " + localDevice.getInstanceNumber());
        thread.start();

        // Send a WhoIsRouter message.
        LOG.debug("Broadcasting WhoIsRouter to local network");
        network.sendNetworkMessage(getLocalBroadcastAddress(), null, 0, null, true, false);
    }

    @Override
    public void terminate() {
        // Stop the processing thread.
        LOG.debug("Terminating transport");
        synchronized (runLock) {
            running = false;
        }
        ThreadUtils.notifySync(pauseLock);
        if (thread != null)
            ThreadUtils.join(thread);

        // Cancel any queued outgoing messages.
        for (Outgoing og : outgoing) {
            if (og instanceof OutgoingConfirmed ogc && ogc.consumer != null) {
                ogc.consumer.ex(new BACnetException("Outgoing cancelled due to transport shutdown"));
            }
        }

        // cancel any delayed outgoing messages.
        for (DelayedOutgoing delayed : delayedOutgoing) {
            if (delayed.outgoing instanceof OutgoingConfirmed ogc && ogc.consumer != null) {
                ogc.consumer.ex(new BACnetException("Delayed outgoing cancelled due to transport shutdown"));
            }
        }

        // Cancel any unacked messages
        for (UnackedMessageContext ctx : unackedMessages.getRequests().values()) {
            if (ctx.getConsumer() != null) {
                ctx.getConsumer().ex(new BACnetException("Unacked cancelled due to transport shutdown"));
            }
        }

        network.terminate();
    }

    @Override
    public long getBytesOut() {
        return network.getBytesOut();
    }

    @Override
    public long getBytesIn() {
        return network.getBytesIn();
    }

    @Override
    public Address getLocalBroadcastAddress() {
        return network.getLocalBroadcastAddress();
    }

    @Override
    public void addNetworkRouter(int networkNumber, OctetString mac) {
        networkRouters.put(networkNumber, mac);
    }

    @Override
    public Map<Integer, OctetString> getNetworkRouters() {
        return networkRouters;
    }

    public int getDelayedOutgoingCount() {
        return delayedOutgoing.size();
    }

    //
    //
    // Adding new requests and responses.
    //
    @Override
    public void send(Address address, UnconfirmedRequestService service) {
        boolean broadcast = address.equals(getLocalBroadcastAddress()) || address.equals(Address.GLOBAL);

        // 16.1.2
        boolean allowSend = true;
        if (!EnableDisable.enable.equals(localDevice.getCommunicationControlState())) {
            // Check if this is an IAm.
            // IAms are allowed to be sent if they are issued in accordance with the WhoIs procedure.
            allowSend = service instanceof IAmRequest iamRequest && iamRequest.isResponseToWhoIs();
        }

        if (allowSend) {
            var out = new OutgoingUnconfirmed(address, service, broadcast);
            synchronized (runLock) {
                if (running) {
                    outgoing.add(out);
                } else {
                    LOG.debug("Transport is not running, will not queue outgoing {}", out);
                }
            }

            ThreadUtils.notifySync(pauseLock);
        }
    }

    @Override
    public ServiceFuture send(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported,
            ConfirmedRequestService service) {
        return send(address, maxAPDULengthAccepted, segmentationSupported, null, service);
    }

    @Override
    public ServiceFuture send(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported,
            Integer maxSegmentsAccepted, ConfirmedRequestService service) {
        if (Thread.currentThread() == thread)
            throw new IllegalStateException("Cannot send future request in the transport thread. Use a callback " //
                    + "call instead, or make this call in a new thread.");
        ServiceFutureImpl future = new ServiceFutureImpl();
        send(address, maxAPDULengthAccepted, segmentationSupported, maxSegmentsAccepted, service, future);
        return future;
    }

    @Override
    public void send(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported,
            ConfirmedRequestService service, ResponseConsumer consumer) {
        send(address, maxAPDULengthAccepted, segmentationSupported, null, service, consumer);
    }

    @Override
    public void send(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported,
            Integer maxSegmentsAccepted, ConfirmedRequestService service, ResponseConsumer consumer) {
        if (address == null) {
            throw new IllegalArgumentException("address cannot be null");
        }
        if (maxAPDULengthAccepted < MaxApduLength.UP_TO_50.getMaxLengthInt()) {
            throw new IllegalArgumentException("invalid maxAPDULengthAccepted: " + maxAPDULengthAccepted);
        }
        if (segmentationSupported == null) {
            throw new IllegalArgumentException("segmentation supported cannot be null");
        }
        if (service == null) {
            throw new IllegalArgumentException("service cannot be null");
        }

        // 16.1.2
        if (EnableDisable.enable.equals(localDevice.getCommunicationControlState())) {
            var out = new OutgoingConfirmed(
                    address,
                    maxAPDULengthAccepted,
                    segmentationSupported,
                    maxSegmentsAccepted,
                    service,
                    consumer
            );

            boolean messageQueued = false;
            synchronized (runLock) {
                if (running) {
                    messageQueued = outgoing.add(out);
                }
            }

            if (messageQueued) {
                if (consumer != null) {
                    consumer.queued();
                }
                ThreadUtils.notifySync(pauseLock);
            } else {
                LOG.debug("Transport is not running, will not queue outgoing {}", out);
                if (consumer != null) {
                    consumer.ex(new BACnetException("Transport is not running"));
                }
            }
        } else if (consumer != null) {
            // Communication has been disabled as the result of a DeviceCommunicationControlRequest. The consumer
            // is informed with an exception.
            consumer.ex(new CommunicationDisabledException());
        }
    }

    @Override
    public void incoming(NPDU npdu) {
        incoming.add(npdu);
        ThreadUtils.notifySync(pauseLock);
    }

    abstract class Outgoing {
        protected final Address address;
        protected OctetString linkService;

        protected Outgoing(Address address) {
            if (address == null)
                throw new IllegalArgumentException("address cannot be null");
            this.address = address;
        }

        void send() {
            // Check if the message is to be sent to a specific remote network.
            int targetNetworkNumber = address.getNetworkNumber().intValue();
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
            } catch (BACnetRecoverableException e) {
                synchronized (runLock) {
                    if (running) {
                        LOG.info("Send delayed due to recoverable error: {}", e.getMessage());
                        delayedOutgoing.add(new DelayedOutgoing(this));
                    } else {
                        handleException(e);
                    }
                }
            } catch (BACnetException e) {
                handleException(e);
            }
        }

        protected abstract void sendImpl() throws BACnetException;

        protected abstract void handleException(BACnetException e);
    }


    class OutgoingConfirmed extends Outgoing {
        private final int maxAPDULengthAccepted;
        private final Segmentation segmentationSupported;
        private final Integer maxSegmentsAccepted;
        private final ConfirmedRequestService service;
        private final ResponseConsumer consumer;

        public OutgoingConfirmed(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported,
                Integer maxSegmentsAccepted, ConfirmedRequestService service, ResponseConsumer consumer) {
            super(address);
            this.maxAPDULengthAccepted = maxAPDULengthAccepted;
            this.segmentationSupported = segmentationSupported;
            this.maxSegmentsAccepted = maxSegmentsAccepted;
            this.service = service;
            this.consumer = consumer;
        }

        @Override
        protected void sendImpl() throws BACnetException {
            ByteQueue serviceData = new ByteQueue();
            service.write(serviceData);

            UnackedMessageContext ctx =
                    new UnackedMessageContext(localDevice.getClock(), timeout, retries, consumer, service);

            // Clause 5.4.4.1 SendConfirmedUnsegmented.
            if (serviceData.size() <= maxAPDULengthAccepted - ConfirmedRequest.getHeaderSize(false)) {
                UnackedMessageKey key = unackedMessages.addClient(address, linkService, ctx);
                ctx.setState(TsmState.AWAIT_CONFIRMATION);
                ctx.setSentAllSegments(true);
                // We can send the whole APDU in one shot.
                ctx.setOriginalApdu(new ConfirmedRequest(false, false, true, MaxSegments.forCount(getMaxSegments()),
                        network.getMaxApduLength(), key.getInvokeId(), (byte) 0, 0, service.getChoiceId(), serviceData,
                        service.getNetworkPriority()));
                sendForResponse(key, ctx);
                if (consumer != null) {
                    consumer.sent();
                }
                return;
            }

            int maxServiceData = maxAPDULengthAccepted - ConfirmedRequest.getHeaderSize(true);

            // The peer must be able to receive segmented messages.
            if (segmentationSupported.intValue() == Segmentation.noSegmentation.intValue()
                    || segmentationSupported.intValue() == Segmentation.segmentedTransmit.intValue()) {
                throw new ServiceTooBigException("Request too big to send to device without segmentation");
            }

            int segmentsRequired = UnackedMessageContext.segmentCount(serviceData.size(), maxServiceData);

            // Clause 5.4.4.1 CannotSend. Only checked when the peer's Max_Segments_Accepted is known. There is no
            // corresponding limit of this device's own: Max_Segments_Accepted is what this device will accept, and
            // clause 12.11.20 says nothing about how many segments it will send.
            if (maxSegmentsAccepted != null && segmentsRequired > maxSegmentsAccepted) {
                throw new ServiceTooBigException("Request requires " + segmentsRequired
                        + " segments but the device accepts at most " + maxSegmentsAccepted);
            }

            // Clause 5.4.4.1 SendConfirmedSegmented.
            UnackedMessageKey key = unackedMessages.addClient(address, linkService, ctx);
            ctx.setState(TsmState.SEGMENTED_REQUEST_CLIENT);
            ctx.setSegmentTemplate(new ConfirmedRequest(true, true, true, MaxSegments.forCount(getMaxSegments()),
                    network.getMaxApduLength(), key.getInvokeId(), 0, segWindow, service.getChoiceId(), null,
                    service.getNetworkPriority()));
            ctx.setSegmentData(serviceData, maxServiceData);

            beginSendingSegments(key, ctx);
            if (consumer != null) {
                consumer.sent();
            }
        }

        @Override
        protected void handleException(BACnetException e) {
            if (consumer == null) {
                LOG.warn("Error during send", e);
            } else {
                consumer.ex(e);
            }
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

        public OutgoingUnconfirmed(Address address, UnconfirmedRequestService service, boolean broadcast) {
            super(address);
            this.service = service;
            this.broadcast = broadcast;
        }

        @Override
        protected void sendImpl() throws BACnetException {
            network.sendAPDU(address, linkService, new UnconfirmedRequest(service), broadcast);
        }

        @Override
        protected void handleException(BACnetException e) {
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

        public DelayedOutgoing(Outgoing outgoing) {
            super();
            this.outgoing = outgoing;
            // Retry in 1 second.
            retryTime = localDevice.getClock().millis() + 1000;
        }

        boolean isReady() {
            return retryTime <= localDevice.getClock().millis();
        }
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
                } catch (Exception e) {
                    LOG.error("Error during send: {}", out, e);
                    out.handleException(new BACnetException("Error during send", e));
                }
                pause = false;
            }

            // Receive an incoming message.
            in = incoming.poll();
            if (in != null) {
                try {
                    receiveImpl(in);
                } catch (Exception e) {
                    LOG.error("Error during receive: {}", in, e);
                }
                pause = false;
            }

            // Find delayed outgoings to retry.
            if (!delayedOutgoing.isEmpty()) {
                Iterator<DelayedOutgoing> iter = delayedOutgoing.iterator();
                while (iter.hasNext()) {
                    DelayedOutgoing delayedOutgoingItem = iter.next();
                    if (delayedOutgoingItem.isReady()) {
                        iter.remove();
                        outgoing.add(delayedOutgoingItem.outgoing);
                        LOG.info("Retrying delayed outgoing {}", delayedOutgoingItem.outgoing);
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
                } catch (Exception e) {
                    LOG.error("Error during expire messages: ", e);
                }
            }

            if (pause && running)
                ThreadUtils.waitSync(pauseLock, 50);
        }
    }

    private void receiveImpl(NPDU in) {
        if (in.isNetworkMessage()) {
            switch (in.getNetworkMessageType()) {
                case 0x1, 0x2: // I-Am-Router-To-Network, I-Could-Be-Router-To-Network
                    ByteQueue data = in.getNetworkMessageData();
                    while (data.size() > 1) {
                        int nn = data.popU2B();
                        LOG.debug("Adding network router {} for network {}", in.getFrom().getMacAddress(), nn);
                        networkRouters.put(nn, in.getFrom().getMacAddress());
                    }
                    break;
                case 0x3: // Reject-Message-To-Network
                    String reason;
                    int reasonCode = in.getNetworkMessageData().popU1B();
                    if (reasonCode == 0)
                        reason = "Other error";
                    else if (reasonCode == 1)
                        reason = "The router is not directly connected to DNET and cannot find a router to DNET on any "
                                //
                                + "directly connected network using Who-Is-Router-To-Network messages.";
                    else if (reasonCode == 2)
                        reason = "The router is busy and unable to accept messages for the specified DNET at the " //
                                + "present time.";
                    else if (reasonCode == 3)
                        reason = "It is an unknown network layer message type. The DNET returned in this case is a " //
                                + "local matter.";
                    else if (reasonCode == 4)
                        reason = "The message is too long to be routed to this DNET.";
                    else if (reasonCode == 5)
                        reason = "The source message was rejected due to a BACnet security error and that error cannot "
                                //
                                + " be forwarded to the source device. See Clause 24.12.1.1 for more details on the " //
                                + "generation of Reject-Message-To-Network messages indicating this reason.";
                    else if (reasonCode == 6)
                        reason =
                                "The source message was rejected due to errors in the addressing. The length of the " //
                                        + "DADR or SADR was determined to be invalid.";
                    else
                        reason = "Unknown reason code";
                    LOG.warn("Received Reject-Message-To-Network with reason '{}': {}", reasonCode, reason);
                    break;
                default:
            }
        } else {
            receiveAPDU(in);
        }
    }

    private void receiveAPDU(NPDU npdu) {
        Address from = npdu.getFrom();
        OctetString linkService = npdu.getLinkService();
        APDU apdu;

        try {
            apdu = npdu.getAPDU(servicesSupported);
        } catch (BACnetException e) {
            // Error parsing the APDU. Drop the request.
            LOG.debug("Error parsing APDU", e);
            return;
        }

        if (apdu instanceof ConfirmedRequest confAPDU) {
            incomingConfirmedRequestApdu(confAPDU, from, linkService, npdu.isBroadcast());
        } else if (apdu instanceof UnconfirmedRequest ur) {
            // Received a request that must be handled with no response.
            try {
                ur.parseServiceData();
                var service = ur.getService();

                if (localDevice.isUnconfigured()
                        && !(service instanceof WhoIsRequest || service instanceof YouAreRequest)) {
                    // Per clause 19.7, the only unconfirmed services permitted while unconfigured are WhoIs and YouAre.
                    // Silently drop everything else. Unconfirmed services have no reply mechanism.
                    LOG.debug("Unconfigured device dropping unconfirmed choice {}", service.getClass());
                } else {
                    localDevice.getEventHandler().requestReceived(from, service);
                    ur.getService().handle(localDevice, from);
                }
            } catch (@SuppressWarnings("unused") BACnetRejectException e) {
                // Ignore
            } catch (BACnetException e) {
                localDevice.getExceptionDispatcher().fireReceivedException(e);
            }
        } else {
            // Must be an acknowledgement
            LOG.debug("incomingApdu: received an acknowledgement from {}", from);
            incomingAckApdu((AckAPDU) apdu, from, linkService);
        }
    }

    //
    //
    // Clause 5.4.5, the responding BACnet user (server) state machine.
    //

    /**
     * Dispatches a received confirmed request PDU to the transition appropriate for the state of its transaction.
     */
    private void incomingConfirmedRequestApdu(ConfirmedRequest confAPDU, Address from, OctetString linkService,
            boolean broadcast) {
        byte invokeId = confAPDU.getInvokeId();

        // 5.4.5.1 ConfirmedBroadcastReceived. A confirmed request addressed to a broadcast or multicast address is
        // discarded without response.
        if (broadcast) {
            LOG.debug("Discarding confirmed request from {} that was received at a broadcast address", from);
            return;
        }

        if (localDevice.isUnconfigured()) {
            // Per clause 19.7, no confirmed service is permitted while unconfigured. Reject with
            // unrecognizedService so the sender learns the request can't be handled here (rather than timing out).
            try {
                network.sendAPDU(from, linkService, new Reject(invokeId, RejectReason.unrecognizedService), false);
            } catch (BACnetException e) {
                LOG.warn("Error sending reject from unconfigured device", e);
            }
            return;
        }

        try {
            ConfirmedRequestService.checkConfirmedRequestService(servicesSupported, confAPDU.getServiceChoice());
        } catch (BACnetRejectException e) {
            try {
                network.sendAPDU(from, linkService, new Reject(invokeId, e.getRejectReason()), false);
            } catch (BACnetException e1) {
                LOG.warn("Error sending error response", e1);
            }
            LOG.warn("Receiving a confirmed service request that ist not supported or available. TYPE_ID '{}'",
                    confAPDU.getServiceChoice());
            return;
        }

        UnackedMessageKey key = new UnackedMessageKey(from, linkService, invokeId, false);
        UnackedMessageContext ctx = unackedMessages.remove(key);

        if (ctx == null) {
            idleConfirmedRequestReceived(key, confAPDU);
            return;
        }

        switch (ctx.getState()) {
            case SEGMENTED_REQUEST_SERVER -> {
                if (confAPDU.isSegmentedMessage())
                    segmentedIncoming(key, confAPDU, ctx);
                else {
                    // 5.4.5.2 UnexpectedPDU_Received. An unsegmented request carries no sequence number, so it must
                    // not be fed to the assembler: sequence number zero would either be negatively acknowledged or,
                    // if the message being assembled happens to be at sequence number 255, spliced onto it.
                    LOG.warn("Received an unsegmented request from {} while assembling a segmented one", from);
                    sendAbort(key, AbortReason.invalidApduInThisState);
                }
            }
            case AWAIT_RESPONSE -> awaitResponseConfirmedRequestReceived(key, confAPDU, ctx);
            case SEGMENTED_RESPONSE -> {
                // 5.4.5.4 UnexpectedPDU_Received. The peer has given up on the response being sent and reissued its
                // request, so the standard abandons the transaction rather than continuing to transmit.
                LOG.warn("Received a confirmed request from {} while sending a segmented response to it", from);
                sendAbort(key, AbortReason.invalidApduInThisState);
            }
            default -> {
                LOG.warn("Received an unexpected confirmed request from {} in state {}", from, ctx.getState());
                sendAbort(key, AbortReason.invalidApduInThisState);
            }
        }
    }

    /**
     * Clause 5.4.5.1, the server IDLE state, for a confirmed request that belongs to no existing transaction.
     */
    private void idleConfirmedRequestReceived(UnackedMessageKey key, ConfirmedRequest confAPDU) {
        UnackedMessageContext ctx = new UnackedMessageContext(localDevice.getClock(), timeout, retries, null, null);

        if (!confAPDU.isSegmentedMessage()) {
            // ConfirmedUnsegmentedReceived.
            ctx.setSegmentedMessage(confAPDU);
            confServIndication(key, ctx, confAPDU);
            return;
        }

        if ((confAPDU.getSequenceNumber() & 0xff) != 0) {
            // UnexpectedPDU_Received. A segment other than the first, for a transaction that does not exist.
            LOG.warn("Received a request segment for an unknown request: {}", confAPDU);
            sendAbort(key, AbortReason.invalidApduInThisState);
            return;
        }

        if (!SegmentSequence.isValidWindowSize(confAPDU.getProposedWindowSize())) {
            // ConfirmedSegmentedReceivedWindowSizeOutOfRange.
            LOG.warn("Received a segmented request from {} with an out of range proposed window size of {}",
                    key.getAddress(), confAPDU.getProposedWindowSize());
            sendAbort(key, AbortReason.windowSizeOutOfRange);
            return;
        }

        // ConfirmedSegmentedReceived.
        ctx.setState(TsmState.SEGMENTED_REQUEST_SERVER);
        beginReceivingSegments(key, confAPDU, ctx);
    }

    /**
     * Clause 5.4.5.3 AWAIT_RESPONSE, for a confirmed request received while the application is formulating a
     * response.
     * <p>
     * Note that confirmed requests are currently handled synchronously on the transport thread, so this state is
     * left before any further PDU can be processed and these transitions do not fire in practice. They become
     * effective if request handling is moved off the transport thread.
     */
    private void awaitResponseConfirmedRequestReceived(UnackedMessageKey key, ConfirmedRequest confAPDU,
            UnackedMessageContext ctx) {
        if (confAPDU.isSegmentedMessage()) {
            // DuplicateSegmentReceived. Discard the segment but re-acknowledge, in case the acknowledgement of the
            // final segment was lost.
            LOG.debug("Discarding duplicate segment for {} while awaiting the application's response", key);
            sendSegmentAck(key, false, ctx.getLastSequenceNumber(), ctx.getActualWindowSize());
        } else {
            // DuplicateRequestReceived.
            LOG.debug("Discarding duplicate request for {} while awaiting the application's response", key);
        }
        unackedMessages.add(key, ctx);
    }

    //
    //
    // Clause 5.4.4, the requesting BACnet user (client) state machine.
    //

    /**
     * Dispatches a received acknowledgement PDU to the transition appropriate for the state of its transaction.
     */
    private void incomingAckApdu(AckAPDU ack, Address from, OctetString linkService) {
        UnackedMessageKey key = new UnackedMessageKey(from, linkService, ack.getOriginalInvokeId(), ack.isServer());
        UnackedMessageContext ctx = unackedMessages.remove(key);

        if (ctx == null) {
            // 5.4.4.1 UnexpectedSegmentInfoReceived and 5.4.5.1 UnexpectedPDU_Received. A PDU that indicates the
            // peer still has an active state machine must be aborted, or it will go on retransmitting until its own
            // timers expire. Everything else is discarded, which legitimately happens for requests whose response
            // the sender did not need, such as COV unsubscribes.
            if (ack instanceof SegmentACK || ack instanceof ComplexACK cack && cack.isSegmentedMessage()) {
                LOG.debug("Aborting a segmentation PDU from {} for an unknown request: {}", from, ack);
                sendAbort(key, AbortReason.invalidApduInThisState);
            } else {
                LOG.debug("Received an acknowledgement from {} for an unknown request: {}", from, ack);
            }
            return;
        }

        if (ack instanceof SegmentACK sack) {
            if (ctx.getState().isSendingSegments())
                // 5.4.4.2 SEGMENTED_REQUEST and 5.4.5.4 SEGMENTED_RESPONSE.
                segmentedOutgoing(key, ctx, sack);
            else if (ctx.getState() == TsmState.AWAIT_CONFIRMATION) {
                // 5.4.4.3 SegmentACK_Received. Discard the PDU as a duplicate and remain in this state.
                LOG.debug("Discarding duplicate segment ack for {}", key);
                unackedMessages.add(key, ctx);
            } else {
                // 5.4.4.4 UnexpectedPDU_Received.
                LOG.warn("Received an unexpected segment ack from {} in state {}", from, ctx.getState());
                abortTransaction(key, ctx, AbortReason.invalidApduInThisState);
            }
            return;
        }

        if (ack instanceof ComplexACK cack && cack.isSegmentedMessage()) {
            segmentedComplexAckReceived(key, cack, ctx);
            return;
        }

        if (ctx.getState() == TsmState.SEGMENTED_CONF && !(ack instanceof Abort)) {
            // 5.4.4.4 UnexpectedPDU_Received. Anything other than a segment of the response being assembled, except
            // an abort, which has its own transition and is reported to the application below.
            LOG.warn("Received an unexpected {} from {} while assembling a segmented response",
                    ack.getClass().getSimpleName(), from);
            abortTransaction(key, ctx, AbortReason.invalidApduInThisState);
            return;
        }

        if (ctx.getState() == TsmState.SEGMENTED_REQUEST_CLIENT && !ctx.isSentAllSegments()
                && !(ack instanceof Abort) && !(ack instanceof Reject)) {
            // 5.4.4.2 UnexpectedPDU_Received. A response arrived before the request was completely sent.
            LOG.warn("Received a {} from {} before the segmented request was completely sent",
                    ack.getClass().getSimpleName(), from);
            abortTransaction(key, ctx, AbortReason.invalidApduInThisState);
            return;
        }

        ResponseConsumer consumer = ctx.getConsumer();
        if (consumer == null)
            return;

        if (ack instanceof SimpleACK)
            consumer.success(null);
        else if (ack instanceof ComplexACK cack)
            completeComplexAckResponse(cack, consumer);
        else if (ack instanceof com.serotonin.bacnet4j.apdu.Error || ack instanceof Reject || ack instanceof Abort)
            consumer.fail(ack);
        else
            LOG.error("Unexpected ack from {}, APDU: {}", from, ack);
    }

    /**
     * Clause 5.4.4.2 and 5.4.4.3 SegmentedComplexACK_Received, and clause 5.4.4.4, for the segments that follow.
     */
    private void segmentedComplexAckReceived(UnackedMessageKey key, ComplexACK cack, UnackedMessageContext ctx) {
        if (ctx.getState() == TsmState.SEGMENTED_CONF) {
            segmentedIncoming(key, cack, ctx);
            return;
        }

        boolean awaitingResponse = ctx.getState() == TsmState.AWAIT_CONFIRMATION
                || ctx.getState() == TsmState.SEGMENTED_REQUEST_CLIENT && ctx.isSentAllSegments();
        if (!awaitingResponse || (cack.getSequenceNumber() & 0xff) != 0) {
            // UnexpectedPDU_Received. The first segment of a response must have a sequence number of zero.
            LOG.warn("Received an unexpected segmented complex ack from {} in state {}, sequence number {}",
                    key.getAddress(), ctx.getState(), cack.getSequenceNumber());
            abortTransaction(key, ctx, AbortReason.invalidApduInThisState);
            return;
        }

        if (!SegmentSequence.isValidWindowSize(cack.getProposedWindowSize())) {
            LOG.warn("Received a segmented response from {} with an out of range proposed window size of {}",
                    key.getAddress(), cack.getProposedWindowSize());
            abortTransaction(key, ctx, AbortReason.windowSizeOutOfRange);
            return;
        }

        ctx.setState(TsmState.SEGMENTED_CONF);
        beginReceivingSegments(key, cack, ctx);
    }

    //
    //
    // Clause 5.4.4.4 SEGMENTED_CONF and clause 5.4.5.2 SEGMENTED_REQUEST. The two state machines are identical apart
    // from the 'server' parameter of the PDUs they send, which follows from the transaction key, and from what
    // happens once the message is complete. They are therefore implemented together.
    //

    /**
     * The common actions of 5.4.4.3 SegmentedComplexACK_Received and 5.4.5.1 ConfirmedSegmentedReceived: save the
     * first segment and its data attributes, determine the window size, acknowledge, and await the remainder.
     */
    private void beginReceivingSegments(UnackedMessageKey key, Segmentable msg, UnackedMessageContext ctx) {
        // The window size is a local matter, except that it must not exceed the proposed size and must be in the
        // range 1 to 127.
        int actualWindowSize = Math.min(msg.getProposedWindowSize(),
                Math.max(segWindow, SegmentSequence.MIN_WINDOW_SIZE));

        LOG.debug("Received first segment for {}, proposed window size={}, actual window size={}", key,
                msg.getProposedWindowSize(), actualWindowSize);

        ctx.setSegmentedMessage(msg);
        // Captured once here rather than read per segment, so that the limit cannot change part way through.
        ctx.setMaxSegments(getMaxSegments());
        ctx.setActualWindowSize(actualWindowSize);
        ctx.setLastSequenceNumber(0);
        ctx.setInitialSequenceNumber(0);
        ctx.setDuplicateCount(0);

        sendSegmentAck(key, false, 0, actualWindowSize);
        awaitNextSegment(key, ctx);
    }

    private void segmentedIncoming(UnackedMessageKey key, Segmentable msg, UnackedMessageContext ctx) {
        int seq = msg.getSequenceNumber() & 0xff;

        if (seq == SegmentSequence.next(ctx.getLastSequenceNumber())) {
            // 5.4.4.4 NewSegmentReceived_NoSpace. The message is longer than this device is prepared to assemble.
            // Without this the peer could go on sending segments until the heap was exhausted.
            if (ctx.getSegmentsReceived() >= ctx.getMaxSegments()) {
                LOG.warn("Aborting a segmented message from {} that exceeds the limit of {} segments",
                        key.getAddress(), ctx.getMaxSegments());
                abortTransaction(key, ctx, AbortReason.bufferOverflow);
                return;
            }

            // The next segment in order.
            ctx.appendSegment(msg);
            ctx.setLastSequenceNumber(seq);

            if (!msg.isMoreFollows())
                lastSegmentOfMessageReceived(key, ctx);
            else if (seq == SegmentSequence.plus(ctx.getInitialSequenceNumber(), ctx.getActualWindowSize()))
                lastSegmentOfGroupReceived(key, ctx);
            else
                newSegmentReceived(key, ctx);
            return;
        }

        // Not the next segment in order. Per addendum ch-1, the window is measured from the sequence number
        // following InitialSequenceNumber.
        int firstSeqNumber = SegmentSequence.next(ctx.getInitialSequenceNumber());
        if (SegmentSequence.duplicateInWindow(seq, firstSeqNumber, ctx.getLastSequenceNumber(),
                ctx.getActualWindowSize())) {
            if (ctx.getDuplicateCount() < ctx.getNdup())
                duplicateSegmentReceived(key, ctx, seq);
            else
                tooManyDuplicateSegmentsReceived(key, ctx, seq);
        } else {
            segmentReceivedOutOfOrder(key, ctx, seq);
        }
    }

    /**
     * NewSegmentReceived. A segment that is neither the last of the window nor the last of the message.
     */
    private void newSegmentReceived(UnackedMessageKey key, UnackedMessageContext ctx) {
        LOG.debug("Received segment {} for {}", ctx.getLastSequenceNumber(), key);
        awaitNextSegment(key, ctx);
    }

    /**
     * LastSegmentOfGroupReceived. The window is full, so acknowledge it and open the next one.
     */
    private void lastSegmentOfGroupReceived(UnackedMessageKey key, UnackedMessageContext ctx) {
        LOG.debug("Received segment {}, the last of its window, for {}", ctx.getLastSequenceNumber(), key);

        sendSegmentAck(key, false, ctx.getLastSequenceNumber(), ctx.getActualWindowSize());
        ctx.setInitialSequenceNumber(ctx.getLastSequenceNumber());
        ctx.setDuplicateCount(0);
        awaitNextSegment(key, ctx);
    }

    /**
     * LastSegmentOfMessageReceived (5.4.5.2) and LastSegmentOfComplexACK_Received (5.4.4.4). The message is complete.
     */
    private void lastSegmentOfMessageReceived(UnackedMessageKey key, UnackedMessageContext ctx) {
        LOG.debug("Received final segment {} for {}, {} segments in total", ctx.getLastSequenceNumber(), key,
                ctx.getSegmentsReceived());

        sendSegmentAck(key, false, ctx.getLastSequenceNumber(), ctx.getActualWindowSize());
        ctx.setInitialSequenceNumber(ctx.getLastSequenceNumber());

        if (ctx.getState() == TsmState.SEGMENTED_CONF)
            // The transaction is complete.
            completeComplexAckResponse((ComplexACK) ctx.getSegmentedMessage(), ctx.getConsumer());
        else
            // Hand the assembled request to the application and await its response.
            confServIndication(key, ctx, (ConfirmedRequest) ctx.getSegmentedMessage());
    }

    /**
     * DuplicateSegmentReceived. A segment already received in the current window; silently discarded.
     */
    private void duplicateSegmentReceived(UnackedMessageKey key, UnackedMessageContext ctx, int seq) {
        LOG.debug("Discarding duplicate segment {} for {}, duplicate count {}", seq, key, ctx.getDuplicateCount());
        ctx.incrementDuplicateCount();
        awaitNextSegment(key, ctx);
    }

    /**
     * TooManyDuplicateSegmentsReceived. Ndup duplicates have now been dropped, so tell the sender where the message
     * actually stands.
     */
    private void tooManyDuplicateSegmentsReceived(UnackedMessageKey key, UnackedMessageContext ctx, int seq) {
        LOG.debug("Discarding duplicate segment {} for {} and negatively acknowledging segment {}", seq, key,
                ctx.getLastSequenceNumber());

        sendSegmentAck(key, true, ctx.getLastSequenceNumber(), ctx.getActualWindowSize());
        if (ctx.getState() == TsmState.SEGMENTED_REQUEST_SERVER)
            // The server state machine resets the window here but the client one does not. This asymmetry is present
            // in the standard itself; see 5.4.4.4 and 5.4.5.2 as amended by addendum ch-1.
            ctx.setInitialSequenceNumber(ctx.getLastSequenceNumber());
        ctx.setDuplicateCount(0);
        awaitNextSegment(key, ctx);
    }

    /**
     * SegmentReceivedOutOfOrder. Negatively acknowledge so that the sender resumes from the last segment that was
     * received in order.
     */
    private void segmentReceivedOutOfOrder(UnackedMessageKey key, UnackedMessageContext ctx, int seq) {
        LOG.debug("Discarding out of order segment {} for {} and negatively acknowledging segment {}", seq, key,
                ctx.getLastSequenceNumber());

        sendSegmentAck(key, true, ctx.getLastSequenceNumber(), ctx.getActualWindowSize());
        ctx.setInitialSequenceNumber(ctx.getLastSequenceNumber());
        ctx.setDuplicateCount(0);
        awaitNextSegment(key, ctx);
    }

    /**
     * Restarts the segment timer and retains the transaction. The receiver's timer runs for Twait_for_seg, which
     * clause 5.4.1 defines as four times the segment timeout.
     */
    private void awaitNextSegment(UnackedMessageKey key, UnackedMessageContext ctx) {
        ctx.reset(segTimeout * 4, 0);
        unackedMessages.add(key, ctx);
    }

    private void sendSegmentAck(UnackedMessageKey key, boolean negativeAck, int sequenceNumber, int windowSize) {
        // We are the server of this transaction when the message being acknowledged did not come from a server.
        boolean server = !key.isFromServer();
        try {
            // Clause 5.4 issues every segment acknowledgement with 'data_expecting_reply' = FALSE. The segments that
            // follow are a new exchange, not a reply to the acknowledgement.
            network.sendAPDU(key.getAddress(), key.getLinkService(),
                    new SegmentACK(negativeAck, server, key.getInvokeId(), sequenceNumber, windowSize, false), false);
        } catch (BACnetException e) {
            LOG.warn("Error sending segment ack for {}", key, e);
            localDevice.getExceptionDispatcher().fireReceivedException(e);
        }
    }

    private Abort sendAbort(UnackedMessageKey key, AbortReason abortReason) {
        // We are the server of this transaction when the message being aborted did not come from a server.
        Abort abort = new Abort(!key.isFromServer(), key.getInvokeId(), abortReason);
        try {
            network.sendAPDU(key.getAddress(), key.getLinkService(), abort, false);
        } catch (BACnetException e) {
            LOG.warn("Error sending abort for {}", key, e);
            localDevice.getExceptionDispatcher().fireReceivedException(e);
        }
        return abort;
    }

    /**
     * Sends an abort, ends the transaction, and informs the consumer if there is one.
     */
    private void abortTransaction(UnackedMessageKey key, UnackedMessageContext ctx, AbortReason abortReason) {
        Abort abort = sendAbort(key, abortReason);
        ctx.useConsumer(consumer -> consumer.fail(abort));
    }

    private static void completeComplexAckResponse(ComplexACK cack, ResponseConsumer consumer) {
        try {
            cack.parseServiceData();
            if (consumer != null) {
                consumer.success(cack.getService());
            }
        } catch (BACnetException e) {
            if (consumer != null) {
                consumer.ex(e);
            }
        }
    }

    //
    //
    // Clause 5.4.4.2 SEGMENTED_REQUEST and clause 5.4.5.4 SEGMENTED_RESPONSE. As with the receiving states, the two
    // state machines are identical apart from the 'server' parameter of the PDUs they send and from what happens once
    // all segments have been acknowledged, so they are implemented together.
    //

    /**
     * Clause 5.4.3, function FillWindow. Transmits segments either until the window is full or until the last segment
     * of the message has been sent. The spec's sequenceNumber argument is InitialSequenceNumber at both of its call
     * sites, and corresponds to the segment at the context's window start index, so it is read from the context here
     * rather than passed in.
     *
     * @return false if a segment could not be sent, in which case the transaction has been abandoned
     */
    private boolean fillWindow(UnackedMessageKey key, UnackedMessageContext ctx) {
        for (int ix = 0; ix < ctx.getActualWindowSize(); ix++) {
            int index = ctx.getWindowStartIndex() + ix;
            if (index >= ctx.getSegmentCount())
                break;

            boolean finalSegment = ctx.isFinalSegment(index);
            int seq = SegmentSequence.plus(ctx.getInitialSequenceNumber(), ix);
            // Every segment carries this device's proposed window size, not the negotiated one. The number of
            // segments sent per window is what ActualWindowSize governs, i.e. the bound of this loop.
            APDU segment = ctx.getSegmentTemplate()
                    .clone(!finalSegment, seq, ctx.getProposedWindowSize(), ctx.getSegment(index));

            LOG.debug("Sending segment {} of {} with sequence number {} for {}", index + 1, ctx.getSegmentCount(), seq,
                    key);
            try {
                network.sendAPDU(key.getAddress(), key.getLinkService(), segment, false);
            } catch (BACnetException e) {
                LOG.warn("Error sending segment for {}", key, e);
                ctx.useConsumer(consumer -> consumer.ex(e));
                return false;
            }

            if (finalSegment) {
                // All segments have been transmitted at least once.
                ctx.setSentAllSegments(true);
                break;
            }
        }
        return true;
    }

    /**
     * The window size a segment acknowledgement asks for, constrained to the legal range. Clause 5.4 does not define
     * a transition for a value outside it, and a value of zero would stall the transmission until it timed out, so
     * the value is clamped rather than rejected.
     */
    private static int actualWindowSize(SegmentACK ack) {
        int windowSize = ack.getActualWindowSize();
        if (SegmentSequence.isValidWindowSize(windowSize))
            return windowSize;

        LOG.warn("Received a segment ack with an out of range actual window size of {}", windowSize);
        return windowSize < SegmentSequence.MIN_WINDOW_SIZE ? SegmentSequence.MIN_WINDOW_SIZE
                : SegmentSequence.MAX_WINDOW_SIZE;
    }

    /**
     * Called each time a segment acknowledgement is received for a message that this device is sending.
     */
    private void segmentedOutgoing(UnackedMessageKey key, UnackedMessageContext ctx, SegmentACK ack) {
        int ackSeq = ack.getSequenceNumber() & 0xff;

        LOG.debug("Received {}segment ack {} for {}", ack.isNegativeAck() ? "negative " : "", ackSeq, key);

        if (!SegmentSequence.inWindow(ackSeq, ctx.getInitialSequenceNumber(), ctx.getActualWindowSize())) {
            // DuplicateACK_Received. Restart the segment timer and continue to wait.
            LOG.debug("Segment ack {} is not in the window starting at {} for {}", ackSeq,
                    ctx.getInitialSequenceNumber(), key);
            ctx.resetTimer(segTimeout);
            unackedMessages.add(key, ctx);
            return;
        }

        // The index of the segment that was acknowledged, and so of the first one still to send.
        int nextIndex = ctx.getWindowStartIndex() + SegmentSequence.diff(ackSeq, ctx.getInitialSequenceNumber()) + 1;

        if (nextIndex >= ctx.getSegmentCount()) {
            // FinalACK_Received.
            finalAckReceived(key, ctx);
            return;
        }

        // NewACK_Received.
        ctx.setInitialSequenceNumber(SegmentSequence.next(ackSeq));
        ctx.setWindowStartIndex(nextIndex);
        ctx.setActualWindowSize(actualWindowSize(ack));
        ctx.setSegmentRetryCount(0);

        if (!fillWindow(key, ctx))
            return;

        ctx.reset(segTimeout, retries);
        unackedMessages.add(key, ctx);
    }

    /**
     * Passes a complete confirmed request to the application program and conveys its response, i.e. the transition
     * into clause 5.4.5.3 AWAIT_RESPONSE and out of it again.
     * <p>
     * The application program is invoked synchronously on the transport thread, so this state is entered and left
     * within this call. The transaction is nevertheless registered for the duration, so that the AWAIT_RESPONSE
     * transitions are correct if request handling is later moved off the transport thread.
     */
    private void confServIndication(UnackedMessageKey key, UnackedMessageContext ctx, ConfirmedRequest confAPDU) {
        Address address = key.getAddress();
        OctetString linkService = key.getLinkService();
        byte invokeId = key.getInvokeId();

        ctx.setState(TsmState.AWAIT_RESPONSE);
        ctx.reset(timeout, 0);
        unackedMessages.add(key, ctx);

        try {
            try {
                confAPDU.parseServiceData();
                AcknowledgementService ackService = handleConfirmedRequest(address, invokeId,
                        confAPDU.getServiceRequest());

                // Per addendum 135-2016bi-2 (Protocol Revision 20): DISABLE_INITIATION means the
                // device stops initiating BACnet messages but continues responding to incoming
                // requests normally. The old DISABLE option (which suppressed responses) is
                // deprecated and rejected at the service handler, so no response suppression is
                // required here.
                sendConfirmedResponse(key, ctx, confAPDU, ackService);
            } catch (BACnetErrorException e) {
                network.sendAPDU(address, linkService,
                        new com.serotonin.bacnet4j.apdu.Error(invokeId, e.getBacnetError()), false);
            } catch (BACnetRejectException e) {
                network.sendAPDU(address, linkService, new Reject(invokeId, e.getRejectReason()), false);
            } catch (BACnetAbortException e) {
                network.sendAPDU(address, linkService, new Abort(true, invokeId, e.getAbortReason()), false);
            } catch (BACnetException e) {
                LOG.warn("Error handling incoming request", e);
                com.serotonin.bacnet4j.apdu.Error error = new com.serotonin.bacnet4j.apdu.Error(
                        confAPDU.getInvokeId(), 127,
                        new ErrorClassAndCode(ErrorClass.services, ErrorCode.operationalProblem));
                network.sendAPDU(address, linkService, error, false);
                localDevice.getExceptionDispatcher().fireReceivedException(e);
            }
        } catch (BACnetException e) {
            localDevice.getExceptionDispatcher().fireReceivedException(e);
        } finally {
            // The transaction ends here unless a segmented response is now in progress.
            if (ctx.getState() == TsmState.AWAIT_RESPONSE)
                unackedMessages.remove(key);
        }
    }

    private AcknowledgementService handleConfirmedRequest(Address from, byte invokeId, ConfirmedRequestService service)
            throws BACnetException {
        try {
            localDevice.getEventHandler().requestReceived(from, service);
            return service.handle(localDevice, from);
        } catch (@SuppressWarnings("unused") NotImplementedException e) {
            LOG.warn("Unsupported confirmed request: invokeId={}, from={}, request={}", invokeId, from,
                    service.getClass().getName());
            throw new BACnetRejectException(RejectReason.unrecognizedService, e);
        } catch (BACnetErrorException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Error while handling confirmed request", e);
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.operationalProblem);
        }
    }

    /**
     * Clause 5.4.5.3, the transitions out of AWAIT_RESPONSE that convey the application program's response.
     */
    private void sendConfirmedResponse(UnackedMessageKey key, UnackedMessageContext ctx, ConfirmedRequest request,
            AcknowledgementService response) throws BACnetException {
        Address address = key.getAddress();
        OctetString linkService = key.getLinkService();

        if (response == null) {
            // SendSimpleACK.
            network.sendAPDU(address, linkService,
                    new SimpleACK(request.getInvokeId(), request.getServiceRequest().getChoiceId()), false);
            return;
        }

        // A complex ack response. Serialize the data.
        ByteQueue serviceData = new ByteQueue();
        response.write(serviceData);

        if (serviceData.size() <= request.getMaxApduLengthAccepted().getMaxLengthInt()
                - ComplexACK.getHeaderSize(false)) {
            // SendUnsegmentedComplexACK.
            network.sendAPDU(address, linkService,
                    new ComplexACK(false, false, request.getInvokeId(), 0, 0, response), false);
            return;
        }

        int maxServiceData = request.getMaxApduLengthAccepted().getMaxLengthInt() - ComplexACK.getHeaderSize(true);

        // CannotSendSegmentedComplexACK, cases (a) and (b). This device always supports segmented transmission, so
        // only case (b) can arise here.
        if (!request.isSegmentedResponseAccepted()) {
            LOG.warn("Response too big to send to device without segmentation");
            throw new BACnetAbortException(AbortReason.segmentationNotSupported);
        }

        int segmentsRequired = UnackedMessageContext.segmentCount(serviceData.size(), maxServiceData);

        // CannotSendSegmentedComplexACK, case (c), the client's limit. Case (d), the number of segments this device
        // can transmit, is not implemented: it is a separate local quantity from Max_Segments_Accepted, which clause
        // 12.11.20 defines only as what this device will accept.
        if (segmentsRequired > request.getMaxSegmentsAccepted().getMaxSegments()) {
            LOG.warn("Response requires {} segments but the client accepts at most {}", segmentsRequired,
                    request.getMaxSegmentsAccepted().getMaxSegments());
            throw new BACnetAbortException(AbortReason.bufferOverflow);
        }

        // SendSegmentedComplexACK.
        LOG.debug("Sending confirmed response as segmented with {} segments", segmentsRequired);
        ctx.setState(TsmState.SEGMENTED_RESPONSE);
        ctx.setSegmentTemplate(
                new ComplexACK(true, true, request.getInvokeId(), 0, segWindow, response.getChoiceId(), null));
        ctx.setSegmentData(serviceData, maxServiceData);

        beginSendingSegments(key, ctx);
    }

    /**
     * The common actions of 5.4.4.1 SendConfirmedSegmented and 5.4.5.3 SendSegmentedComplexACK: send the first
     * segment with a window size of one, and await its acknowledgement.
     */
    private void beginSendingSegments(UnackedMessageKey key, UnackedMessageContext ctx) {
        resetSegmentedSend(ctx);
        // Start the segment timer, not the request timer: an acknowledgement of this segment is what is awaited.
        ctx.reset(segTimeout, retries);

        // Send an initial message to negotiate communication terms.
        APDU apdu = ctx.getSegmentTemplate().clone(true, 0, ctx.getProposedWindowSize(), ctx.getSegment(0));
        ctx.setOriginalApdu(apdu);
        sendForResponse(key, ctx);
    }

    /**
     * Positions a segmented transmission at its first segment. Shared by the transitions that start one
     * (5.4.4.1 SendConfirmedSegmented, 5.4.5.3 SendSegmentedComplexACK) and the one that restarts it
     * (5.4.4.3 TimeoutSegmented). The request timer and retry count are deliberately left alone, because the
     * restarting transition must preserve them.
     */
    private void resetSegmentedSend(UnackedMessageContext ctx) {
        ctx.setSegmentRetryCount(0);
        ctx.setInitialSequenceNumber(0);
        // Until the peer replies, only the segment being sent now is outstanding.
        ctx.setActualWindowSize(1);
        ctx.setProposedWindowSize(segWindow);
        ctx.setWindowStartIndex(0);
        ctx.setSentAllSegments(false);
    }

    /**
     * FinalACK_Received, for both 5.4.4.2 SEGMENTED_REQUEST and 5.4.5.4 SEGMENTED_RESPONSE.
     */
    private void finalAckReceived(UnackedMessageKey key, UnackedMessageContext ctx) {
        if (ctx.getState() == TsmState.SEGMENTED_REQUEST_CLIENT) {
            // The request has been sent in full. Await the response to it.
            LOG.debug("Done sending segmented request for {}", key);
            ctx.setState(TsmState.AWAIT_CONFIRMATION);
            ctx.reset(timeout, retries);
            unackedMessages.add(key, ctx);
        } else {
            // The response has been sent in full, so the transaction is complete.
            LOG.debug("Done sending segmented response for {}", key);
        }
    }

    private boolean expire() {
        boolean didSomething = false;

        long now = localDevice.getClock().millis();
        Iterator<Map.Entry<UnackedMessageKey, UnackedMessageContext>> umIter = unackedMessages.getRequests()
                .entrySet().iterator();

        // Check for expired unacked messages
        var toSendForResponse = new HashMap<UnackedMessageKey, UnackedMessageContext>();
        var toFillWindow = new HashMap<UnackedMessageKey, UnackedMessageContext>();
        while (umIter.hasNext()) {
            Map.Entry<UnackedMessageKey, UnackedMessageContext> e = umIter.next();
            UnackedMessageKey key = e.getKey();
            UnackedMessageContext ctx = e.getValue();
            if (!ctx.isExpired(now))
                continue;

            didSomething = true;
            LOG.debug("Timeout on key {} in state {}", key, ctx.getState());

            if (ctx.getState() != null && ctx.getState().isSendingSegments()) {
                // 5.4.4.2 and 5.4.5.4 Timeout: retransmit the segments of the current window.
                if (ctx.getSegmentRetryCount() < retries) {
                    ctx.incrementSegmentRetryCount();
                    ctx.resetTimer(segTimeout);
                    toFillWindow.put(key, ctx);
                } else {
                    // FinalTimeout.
                    umIter.remove();
                    segmentedSendTimeout(key, ctx);
                }
            } else if (ctx.getState() != null && ctx.getState().isReceivingSegments()) {
                // 5.4.4.4 and 5.4.5.2 Timeout. There are no retries; the sender is responsible for those.
                umIter.remove();
                segmentedReceiveTimeout(key, ctx);
            } else if (ctx.getState() == TsmState.AWAIT_RESPONSE) {
                // 5.4.5.3 Timeout.
                umIter.remove();
                LOG.warn("The application program exceeded the reply time for {}", key);
                sendAbort(key, AbortReason.applicationExceededReplyTime);
            } else if (ctx.hasMoreAttempts()) {
                // 5.4.4.3 TimeoutUnsegmented and TimeoutSegmented.
                ctx.retry(timeout);
                if (ctx.getSegmentCount() > 1) {
                    // Restart the segmented transmission from its first segment.
                    ctx.setState(TsmState.SEGMENTED_REQUEST_CLIENT);
                    resetSegmentedSend(ctx);
                    ctx.resetTimer(segTimeout);
                }
                toSendForResponse.put(key, ctx);
            } else {
                // 5.4.4.3 FinalTimeout.
                umIter.remove();
                ctx.useConsumer(consumer -> consumer.ex(new BACnetTimeoutException()));
            }
        }
        toSendForResponse.forEach(this::sendForResponse);
        toFillWindow.forEach((key, ctx) -> {
            // fillWindow has already informed the consumer if it could not send, so the transaction is over.
            if (!fillWindow(key, ctx))
                unackedMessages.remove(key);
        });

        return !didSomething;
    }

    /**
     * 5.4.4.2 and 5.4.5.4 FinalTimeout. The segments of a message being sent were not acknowledged.
     */
    private void segmentedSendTimeout(UnackedMessageKey key, UnackedMessageContext ctx) {
        var timeoutEx = new BACnetTimeoutException(
                "Timeout while sending segment part: key=%s, initialSequenceNumber=%s".formatted(key,
                        ctx.getInitialSequenceNumber()));
        ctx.useConsumer(consumer -> consumer.ex(timeoutEx));
        if (ctx.getConsumer() == null) {
            LOG.warn("Timeout sending segment(s)", timeoutEx);
        }
    }

    /**
     * 5.4.4.4 and 5.4.5.2 Timeout. The remaining segments of a message being received did not arrive.
     */
    private void segmentedReceiveTimeout(UnackedMessageKey key, UnackedMessageContext ctx) {
        var timeoutEx = new BACnetTimeoutException(
                "Timeout while waiting for segment part: key=%s, lastSequenceNumber=%s, apdu=%s".formatted(key,
                        ctx.getLastSequenceNumber(), ctx.getOriginalApdu()));
        ctx.useConsumer(consumer -> consumer.ex(timeoutEx));
        if (ctx.getConsumer() == null) {
            LOG.warn("Timeout waiting for segment(s)", timeoutEx);
        }
    }

    void sendForResponse(UnackedMessageKey key, UnackedMessageContext ctx) {
        try {
            network.sendAPDU(key.getAddress(), key.getLinkService(), ctx.getOriginalApdu(), false);
        } catch (BACnetException e) {
            unackedMessages.remove(key);
            ctx.useConsumer(consumer -> consumer.ex(e));
        }
    }
}
