package com.serotonin.bacnet4j.transport;

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
import com.serotonin.bacnet4j.enums.MaxSegments;
import com.serotonin.bacnet4j.exception.BACnetAbortException;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRejectException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.exception.CommunicationDisabledException;
import com.serotonin.bacnet4j.exception.NotImplementedException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.NetworkIdentifier;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest;
import com.serotonin.bacnet4j.service.unconfirmed.IAmRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.AbortReason;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.RejectReason;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transport abstraction
 *
 * @author Terry Packer
 */
public abstract class AbstractTransport implements Transport {
    static final Logger LOG = LoggerFactory.getLogger(AbstractTransport.class);
    static final MaxSegments MAX_SEGMENTS = MaxSegments.MORE_THAN_64;

    final Map<Integer, OctetString> networkRouters = new ConcurrentHashMap<>();

    // Processing
    final UnackedMessages unackedMessages = new UnackedMessages();

    // Configuration
    protected LocalDevice localDevice;
    final Network network;
    int timeout = DEFAULT_TIMEOUT;
    int retries = DEFAULT_RETRIES;
    int segTimeout = DEFAULT_SEG_TIMEOUT;
    int segWindow = DEFAULT_SEG_WINDOW;
    ServicesSupported servicesSupported;

    protected AbstractTransport(Network network) {
        this.network = network;
    }

    @Override
    public void initialize() throws Exception {
        servicesSupported = localDevice.getServicesSupported();
        network.initialize(this);
        // Send a WhoIsRouter message.
        LOG.debug("Broadcasting WhoIsRouter to local network");
        network.sendNetworkMessage(getLocalBroadcastAddress(), null, 0, null, true, false);
    }

    @Override
    public void terminate() {
        network.terminate();
    }

    @Override
    public void send(final Address address, final UnconfirmedRequestService service) {
        final boolean broadcast = address.equals(getLocalBroadcastAddress()) || address.equals(Address.GLOBAL);

        // 16.1.2
        boolean allowSend = true;
        if (!DeviceCommunicationControlRequest.EnableDisable.enable.equals(localDevice.getCommunicationControlState())) {
            allowSend = false;

            // Check if this is an IAm.
            if (service instanceof IAmRequest && ((IAmRequest) service).isResponseToWhoIs()) {
                    allowSend = true;
                }
        }

        if (allowSend) {
            sendUnconfirmedImpl(address, service, broadcast);
        }
    }

    @Override
    public ServiceFuture send(final Address address, final int maxAPDULengthAccepted,
                              final Segmentation segmentationSupported, final ConfirmedRequestService service) {
        final ServiceFutureImpl future = getFuture();
        send(address, maxAPDULengthAccepted, segmentationSupported, service, future);
        return future;
    }

    @Override
    public void send(final Address address, final int maxAPDULengthAccepted, final Segmentation segmentationSupported,
                     final ConfirmedRequestService service, final ResponseConsumer consumer) {
        // 16.1.2
        if (DeviceCommunicationControlRequest.EnableDisable.enable.equals(localDevice.getCommunicationControlState())) {
            sendConfirmedImpl(address, maxAPDULengthAccepted, segmentationSupported, service, consumer);
        } else {
            // Communication has been disabled as the result of a DeviceCommunicationControlRequest. The consumer
            // is informed with an exception.
            consumer.ex(new CommunicationDisabledException());
        }
    }

    /**
     * Send a confirmed request
     * @param address
     * @param maxAPDULengthAccepted
     * @param segmentationSupported
     * @param service
     */
    protected void sendConfirmedImpl(final Address address, final int maxAPDULengthAccepted,
                                              final Segmentation segmentationSupported, final
                                              ConfirmedRequestService service, final ResponseConsumer consumer) {
        sendOutgoingConfirmed(new OutgoingConfirmed(this, address, maxAPDULengthAccepted, segmentationSupported, service, consumer,
                new Exception()));
    }

    /**
     * Send unconfirmed request
     * @param address
     * @param service
     * @param broadcast
     */
    protected void sendUnconfirmedImpl(final Address address, final UnconfirmedRequestService service,
                                       final boolean broadcast) {
        sendOutgoingUnconfirmed(new OutgoingUnconfirmed(this, address, service, broadcast, new Exception()));
    }

    /**
     * Allow overriding our
     * @return
     */
    protected ServiceFutureImpl getFuture() {
        return new ServiceFutureImpl();
    }

    /**
     * Send a delayed outgoing message, this will happen if a message send fails
     * and can be re-tried
     * @param message
     */
    protected abstract void sendDelayedOutgoing(DelayedOutgoing message);

    /**
     * Send an unconfirmed message
     * @param message
     */
    protected abstract void sendOutgoingUnconfirmed(OutgoingUnconfirmed message);

    /**
     * Send a confirmed message
     * @param message
     */
    protected abstract void sendOutgoingConfirmed(OutgoingConfirmed message);

    protected void receiveImpl(final NPDU in) {
        if (in.isNetworkMessage()) {
            switch (in.getNetworkMessageType()) {
                case 0x1: // I-Am-Router-To-Network
                case 0x2: // I-Could-Be-Router-To-Network
                    final ByteQueue data = in.getNetworkMessageData();
                    while (data.size() > 1) {
                        final int nn = data.popU2B();
                        LOG.debug("Adding network router {} for network {}", in.getFrom().getMacAddress(), nn);
                        networkRouters.put(nn, in.getFrom().getMacAddress());
                    }
                    break;
                case 0x3: // Reject-Message-To-Network
                    String reason;
                    final int reasonCode = in.getNetworkMessageData().popU1B();
                    if (reasonCode == 0)
                        reason = "Other error";
                    else if (reasonCode == 1)
                        reason = "The router is not directly connected to DNET and cannot find a router to DNET on any " //
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
                        reason = "The source message was rejected due to a BACnet security error and that error cannot " //
                                + " be forwarded to the source device. See Clause 24.12.1.1 for more details on the " //
                                + "generation of Reject-Message-To-Network messages indicating this reason.";
                    else if (reasonCode == 6)
                        reason = "The source message was rejected due to errors in the addressing. The length of the " //
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

    protected void receiveAPDU(final NPDU npdu) {
        final Address from = npdu.getFrom();
        final OctetString linkService = npdu.getLinkService();
        APDU apdu;

        try {
            apdu = npdu.getAPDU(servicesSupported);
        } catch (final BACnetException e) {
            // Error parsing the APDU. Drop the request.
            LOG.debug("Error parsing APDU", e);
            return;
        }

        if (apdu instanceof ConfirmedRequest) {
            // Received a request that must be handled and responded to.
            final ConfirmedRequest confAPDU = (ConfirmedRequest) apdu;
            final byte invokeId = confAPDU.getInvokeId();

            try {
                ConfirmedRequestService.checkConfirmedRequestService(servicesSupported, confAPDU.getServiceChoice());
            } catch (final BACnetRejectException e) {
                try {
                    network.sendAPDU(from, linkService, new Reject(confAPDU.getInvokeId(), e.getRejectReason()), false);
                } catch (final BACnetException e1) {
                    LOG.warn("Error sending error response", e1);
                }
                LOG.warn("Receiving a confirmed service request that ist not supported or available. TYPE_ID '{}'", confAPDU.getServiceChoice());
                return;
            }

            if (confAPDU.isSegmentedMessage()) {
                final UnackedMessageKey key = new UnackedMessageKey(from, linkService, invokeId, false);
                UnackedMessageContext ctx;
                if (confAPDU.getSequenceNumber() == 0)
                    // This is the first segment
                    ctx = new UnackedMessageContext(localDevice.getClock(), timeout, retries, null, null);
                else {
                    ctx = unackedMessages.remove(key);
                    if (ctx == null)
                        LOG.warn("Received a request segment for an unknown request: {}", confAPDU);
                }

                try {
                    segmentedIncoming(key, confAPDU, ctx);
                } catch (final BACnetException e) {
                    LOG.warn("Error handling incoming request", e);
                    final com.serotonin.bacnet4j.apdu.Error error = new com.serotonin.bacnet4j.apdu.Error(
                            confAPDU.getInvokeId(), 127,
                            new ErrorClassAndCode(ErrorClass.services, ErrorCode.operationalProblem));
                    try {
                        network.sendAPDU(from, linkService, error, false);
                    } catch (final BACnetException e1) {
                        LOG.warn("Error sending error response", e1);
                    }
                    localDevice.getExceptionDispatcher().fireReceivedException(e);
                }
            } else
                // Just handle the message.
                incomingConfirmedRequest(confAPDU, from, linkService, invokeId);
        } else if (apdu instanceof UnconfirmedRequest) {
            // Received a request that must be handled with no response.
            final UnconfirmedRequest ur = (UnconfirmedRequest) apdu;

            try {
                ur.parseServiceData();
                localDevice.getEventHandler().requestReceived(from, ur.getService());
                ur.getService().handle(localDevice, from);
            } catch (@SuppressWarnings("unused") final BACnetRejectException e) {
                // Ignore
            } catch (final BACnetException e) {
                localDevice.getExceptionDispatcher().fireReceivedException(e);
            }
        } else {
            // Must be an acknowledgement
            LOG.debug("incomingApdu: recieved an acknowledgement from {}", from);

            final AckAPDU ack = (AckAPDU) apdu;
            final UnackedMessageKey key = new UnackedMessageKey(from, linkService, ack.getOriginalInvokeId(),
                    ack.isServer());
            final UnackedMessageContext ctx = unackedMessages.remove(key);

            if (ctx == null) {
                // This can legitimately happen when requests are sent for which the sender did not need the response,
                // such as COV unsubscribes.
                LOG.debug("Received an acknowledgement from {} for an unknown request: {}", from, ack);
            } else if (ack instanceof SegmentACK)
                segmentedOutgoing(key, ctx, (SegmentACK) ack);
            else if (ctx.getConsumer() != null) {
                final ResponseConsumer consumer = ctx.getConsumer();
                if (ack instanceof SimpleACK) {
                    consumer.success(null);
                } else if (ack instanceof ComplexACK) {
                    final ComplexACK cack = (ComplexACK) ack;
                    if (cack.isSegmentedMessage()) {
                        try {
                            segmentedIncoming(key, cack, ctx);
                        } catch (final BACnetException e) {
                            consumer.ex(e);
                        }
                    } else
                        completeComplexAckResponse(cack, consumer);
                } else if (ack instanceof com.serotonin.bacnet4j.apdu.Error)
                    consumer.fail(ack);
                else if (ack instanceof Reject)
                    consumer.fail(ack);
                else if (ack instanceof Abort)
                    consumer.fail(ack);
                else
                    LOG.error("Unexpected ack from {}, APDU: {}", from, ack);
            }
        }
    }

    protected void segmentedIncoming(final UnackedMessageKey key, final Segmentable msg, final UnackedMessageContext ctx)
            throws BACnetException {
        final int windowSize = msg.getProposedWindowSize();
        final int currentSeq = msg.getSequenceNumber() & 0xff;
        boolean complete = false;

        if (ctx.getSegmentWindow() == null) {
            LOG.debug("Received first segment {} for {}", currentSeq, key);

            // This is the first segment.
            ctx.setSegmentWindow(new SegmentWindow(windowSize, currentSeq + 1));
            ctx.setSegmentedMessage(msg);

            // Send a segment acknowledgement going with the proposed window size.
            network.sendAPDU(key.getAddress(), key.getLinkService(),
                    new SegmentACK(false, !key.isFromServer(), msg.getInvokeId(), currentSeq, windowSize, true), false);
        } else {
            final SegmentWindow segmentWindow = ctx.getSegmentWindow();

            LOG.debug("Received segment {}, first={}, window size={}, for {}", currentSeq,
                    segmentWindow.getFirstSequenceId(), segmentWindow.getWindowSize(), key);

            if (segmentWindow.fitsInWindow(msg)) {
                segmentWindow.setSegment(msg);

                // Do we need to send an ack?
                complete = segmentWindow.isMessageComplete();
                if (complete || segmentWindow.isFull()) {
                    final int lastSeq = segmentWindow.getLatestSequenceId();

                    LOG.debug("Sending ack for segment {}, key={}", lastSeq, key);

                    // Send an acknowledgement
                    network.sendAPDU(key.getAddress(), key.getLinkService(), new SegmentACK(false, !key.isFromServer(),
                            msg.getInvokeId(), lastSeq, windowSize, !segmentWindow.isMessageComplete()), false);

                    // Append the window onto the original response.
                    for (final Segmentable segment : segmentWindow.getSegments()) {
                        ctx.getSegmentedMessage().appendServiceData(segment.getServiceData());
                        if (!segment.isMoreFollows())
                            break;
                    }
                    segmentWindow.clear(lastSeq + 1);
                }
            } else {
                LOG.warn("Segment did not fit in segment window: segment={}, first={}, windowSize={}, key={}",
                        currentSeq, segmentWindow.getFirstSequenceId(), segmentWindow.getWindowSize(), key);
            }
        }

        if (!complete) {
            // More segments to come. Put the value back in the pending requests.
            ctx.reset(segTimeout * 4, 0);
            unackedMessages.add(key, ctx);
        } else if (msg instanceof ComplexACK)
            // We're done receiving the segmented response.
            completeComplexAckResponse((ComplexACK) ctx.getSegmentedMessage(), ctx.getConsumer());
        else
            // We're done receiving the segmented request.
            incomingConfirmedRequest((ConfirmedRequest) ctx.getSegmentedMessage(), key.getAddress(),
                    key.getLinkService(), msg.getInvokeId());
    }

    protected static void completeComplexAckResponse(final ComplexACK cack, final ResponseConsumer consumer) {
        try {
            cack.parseServiceData();
            if (consumer != null) {
                consumer.success(cack.getService());
            }
        } catch (final BACnetException e) {
            if (consumer != null) {
                consumer.ex(e);
            }
        }
    }

    /**
     * The first part of the segmented message has already been sent. This is called each time a segment ack is
     * received.
     *
     * This method handles outgoing segmented requests and responses.
     */
    protected void segmentedOutgoing(final UnackedMessageKey key, final UnackedMessageContext ctx, final SegmentACK ack) {
        // TODO handle NAK

        LOG.debug("Received segment ack {} for {}", ack.getSequenceNumber(), key);

        if (ctx.getServiceData().size() == 0) {
            // There any no more segments to send. If this is a request, expect the response.
            if (ctx.getOriginalApdu() instanceof ConfirmedRequest)
                unackedMessages.add(key, ctx);
            // However, if this is a response, there is nothing left to do.
            LOG.debug("Done sending segmented response");
            return;
        }

        // This may be a segment ack for an inter-window segment. We ignore all segment acks except for the
        // one for the last segment that was sent.
        if (ack.getSequenceNumber() < ctx.getLastIdSent())
            return;

        int remaining = ack.getActualWindowSize();

        // Send the next window of messages.
        int sequenceNumber = ctx.getLastIdSent();
        while (remaining > 0 && ctx.getServiceData().size() > 0) {
            final ByteQueue segData = ctx.getNextSegment();
            final APDU segment = ctx.getSegmentTemplate().clone(ctx.getServiceData().size() > 0, ++sequenceNumber,
                    ack.getActualWindowSize(), segData);

            LOG.debug("Sending segment {} for {}", sequenceNumber, key);
            try {
                network.sendAPDU(key.getAddress(), key.getLinkService(), segment, false);
            } catch (final BACnetException e) {
                ctx.useConsumer(consumer -> consumer.ex(e));
                return;
            }

            remaining--;
        }
        ctx.setLastIdSent(sequenceNumber);
        ctx.reset(segTimeout, retries);

        // Expect the segment ack.
        unackedMessages.add(key, ctx);
    }

    protected void incomingConfirmedRequest(final ConfirmedRequest confAPDU, final Address address,
                                            final OctetString linkService, final byte invokeId) {
        try {
            try {
                confAPDU.parseServiceData();
                final AcknowledgementService ackService = handleConfirmedRequest(address, invokeId,
                        confAPDU.getServiceRequest());

                // 16.1.2: Check if communication is currently disabled. If so, only certain requests are responded.
                boolean allowResponse = true;
                if (DeviceCommunicationControlRequest.EnableDisable.disable.equals(localDevice.getCommunicationControlState())) {
                    // Communication is disabled. Check if the response should be allowed anyway. This includes at
                    // least communication control and reinitialize device.
                    if (!confAPDU.getServiceRequest().isCommunicationControlOverride()) {
                        allowResponse = false;
                    }
                }

                if (allowResponse) {
                    // Send the response.
                    sendConfirmedResponse(address, linkService, confAPDU, ackService);
                } else {
                    LOG.info("Response suppressed because communication has been disabled.");
                }
            } catch (final BACnetErrorException e) {
                network.sendAPDU(address, linkService,
                        new com.serotonin.bacnet4j.apdu.Error(invokeId, e.getBacnetError()), false);
            } catch (final BACnetRejectException e) {
                network.sendAPDU(address, linkService, new Reject(invokeId, e.getRejectReason()), false);
            } catch (final BACnetAbortException e) {
                network.sendAPDU(address, linkService, new Abort(true, invokeId, e.getAbortReason()), false);
            } catch (final BACnetException e) {
                LOG.warn("Error handling incoming request", e);
                final com.serotonin.bacnet4j.apdu.Error error = new com.serotonin.bacnet4j.apdu.Error(
                        confAPDU.getInvokeId(), 127,
                        new ErrorClassAndCode(ErrorClass.services, ErrorCode.operationalProblem));
                network.sendAPDU(address, linkService, error, false);
                localDevice.getExceptionDispatcher().fireReceivedException(e);
            }
        } catch (final BACnetException e) {
            localDevice.getExceptionDispatcher().fireReceivedException(e);
        }
    }

    protected AcknowledgementService handleConfirmedRequest(final Address from, final byte invokeId,
                                                            final ConfirmedRequestService service) throws BACnetException {
        try {
            localDevice.getEventHandler().requestReceived(from, service);
            return service.handle(localDevice, from);
        } catch (@SuppressWarnings("unused") final NotImplementedException e) {
            LOG.warn("Unsupported confirmed request: invokeId=" + invokeId + ", from=" + from + ", request="
                    + service.getClass().getName());
            throw new BACnetRejectException(RejectReason.unrecognizedService, e);
        } catch (final BACnetErrorException e) {
            throw e;
        } catch (final Exception e) {
            LOG.warn("Error while handling confirmed request", e);
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.operationalProblem);
        }
    }

    protected void sendConfirmedResponse(final Address address, final OctetString linkService,
                                         final ConfirmedRequest request, final AcknowledgementService response) throws BACnetException {
        if (response == null)
            network.sendAPDU(address, linkService,
                    new SimpleACK(request.getInvokeId(), request.getServiceRequest().getChoiceId()), false);
        else {
            // A complex ack response. Serialize the data.
            final ByteQueue serviceData = new ByteQueue();
            response.write(serviceData);

            // Check if we need to segment the message.
            if (serviceData.size() > request.getMaxApduLengthAccepted().getMaxLengthInt()
                    - ComplexACK.getHeaderSize(false)) {
                final int maxServiceData = request.getMaxApduLengthAccepted().getMaxLengthInt()
                        - ComplexACK.getHeaderSize(true);
                // Check if the device can accept what we want to send.
                if (!request.isSegmentedResponseAccepted()) {
                    LOG.warn("Response too big to send to device without segmentation");
                    throw new BACnetAbortException(AbortReason.bufferOverflow);
                }
                final int segmentsRequired = serviceData.size() / maxServiceData + 1;
                if (segmentsRequired > request.getMaxSegmentsAccepted().getMaxSegments() || segmentsRequired > 255) {
                    LOG.warn("Response too big to send to device; too many segments required");
                    throw new BACnetAbortException(AbortReason.bufferOverflow);
                }
                LOG.debug("Sending confirmed response as segmented with {} segments", segmentsRequired);
                // Prepare the segmenting session.
                final UnackedMessageContext ctx = new UnackedMessageContext(localDevice.getClock(), timeout, retries,
                        null, null);
                final UnackedMessageKey key = unackedMessages.addServer(address, linkService, request.getInvokeId(),
                        ctx);

                ctx.setSegmentTemplate(
                        new ComplexACK(true, true, request.getInvokeId(), 0, segWindow, response.getChoiceId(), null));
                ctx.setServiceData(serviceData);
                ctx.setSegBuf(new byte[maxServiceData]);

                // Send an initial message to negotiate communication terms.
                final APDU apdu = ctx.getSegmentTemplate().clone(true, 0, segWindow, ctx.getNextSegment());

                ctx.setOriginalApdu(apdu);
                sendForResponse(key, ctx);
            } else {
                // We can send the whole APDU in one shot.
                network.sendAPDU(address, linkService,
                        new ComplexACK(false, false, request.getInvokeId(), 0, 0, response), false);
            }
        }
    }

    protected void sendForResponse(final UnackedMessageKey key, final UnackedMessageContext ctx) {
        try {
            network.sendAPDU(key.getAddress(), key.getLinkService(), ctx.getOriginalApdu(), false);
        } catch (final BACnetException e) {
            unackedMessages.remove(key);
            ctx.useConsumer(consumer -> consumer.ex(e));
        }
    }

    /**
     * Confirm the message is to be sent to a specific remote network
     * @param address
     * @return - the address or if not found throws exception
     * @throws BACnetException
     */
    protected OctetString checkLinkService(Address address) throws BACnetException {
        OctetString linkService = null;
        // Check if the message is to be sent to a specific remote network.
        final int targetNetworkNumber = address.getNetworkNumber().intValue();
        if (targetNetworkNumber != Address.LOCAL_NETWORK && targetNetworkNumber != Address.ALL_NETWORKS
                && targetNetworkNumber != network.getLocalNetworkNumber()) {
            // Going to a specific remote network. Check if we know the router for it.
            linkService = networkRouters.get(targetNetworkNumber);
            if (linkService == null) {
                throw new BACnetException(
                        "Unable to find router to network " + address.getNetworkNumber().intValue());

            }
        }
        return linkService;
    }

    protected boolean expire() {
        boolean didSomething = false;

        final long now = localDevice.getClock().millis();
        final Iterator<Map.Entry<UnackedMessageKey, UnackedMessageContext>> umIter = unackedMessages.getRequests()
                .entrySet().iterator();

        // Check for expired unacked messages
        while (umIter.hasNext()) {
            final Map.Entry<UnackedMessageKey, UnackedMessageContext> e = umIter.next();
            final UnackedMessageKey key = e.getKey();
            final UnackedMessageContext ctx = e.getValue();
            if (ctx.isExpired(now)) {
                if (ctx.hasMoreAttempts()) {
                    // Resend
                    ctx.retry(timeout);
                    sendForResponse(key, ctx);
                } else {
                    LOG.debug("Timeout on key {}", key);

                    // Timeout
                    umIter.remove();
                    if (ctx.getSegmentWindow() == null) {
                        // Not a segmented message, at least as far as we know.
                        ctx.useConsumer(consumer -> consumer.ex(new BACnetTimeoutException()));
                    } else {
                        // A segmented message.
                        if (ctx.getSegmentWindow().isEmpty()) {
                            // No segments received. Return a timeout.
                            ctx.useConsumer(consumer -> consumer.ex(new BACnetTimeoutException(
                                    "Timeout while waiting for segment part: invokeId=" + key.getInvokeId()
                                            + ", sequenceId=" + ctx.getSegmentWindow().getFirstSequenceId())));
                        } else if (ctx.getSegmentWindow().isEmpty())
                            LOG.warn("No segments received for message " + ctx.getOriginalApdu());
                        else {
                            // Return a NAK with the last sequence id received in order and start over.
                            try {
                                network.sendAPDU(key.getAddress(), key.getLinkService(),
                                        new SegmentACK(true, key.isFromServer(), key.getInvokeId(),
                                                ctx.getSegmentWindow().getLatestSequenceId(),
                                                ctx.getSegmentWindow().getWindowSize(), true),
                                        false);
                            } catch (final BACnetException ex) {
                                ctx.useConsumer(consumer -> consumer.ex(ex));
                            }
                        }
                    }
                }

                didSomething = true;
            }
        }

        return !didSomething;
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
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setSegTimeout(final int segTimeout) {
        this.segTimeout = segTimeout;
    }

    @Override
    public int getSegTimeout() {
        return segTimeout;
    }

    @Override
    public void setRetries(final int retries) {
        this.retries = retries;
    }

    @Override
    public int getRetries() {
        return retries;
    }

    @Override
    public void setSegWindow(final int segWindow) {
        this.segWindow = segWindow;
    }

    @Override
    public int getSegWindow() {
        return segWindow;
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
    public void setLocalDevice(final LocalDevice localDevice) {
        this.localDevice = localDevice;
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
    public void addNetworkRouter(final int networkNumber, final OctetString mac) {
        networkRouters.put(networkNumber, mac);
    }

    @Override
    public Map<Integer, OctetString> getNetworkRouters() {
        return networkRouters;
    }

}
