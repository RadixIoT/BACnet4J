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
import com.serotonin.bacnet4j.npdu.test.AbstractTestNetwork;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Attempt to make test messaging synchronous and still test the default transport layer
 * @author Terry Packer
 */
public class SynchronousTransport extends AbstractTransport {

    public SynchronousTransport(AbstractTestNetwork network) {
        super(network);
    }

    @Override
    public ServiceFuture send(final Address address, final int maxAPDULengthAccepted,
                              final Segmentation segmentationSupported, final ConfirmedRequestService service) {
        //Since there is no thread to track timeouts we do it here
        //TODO We need to handle this.unackedMessages messages from the transport in this future
        final SynchronousTransportFuture future = new SynchronousTransportFuture(this.localDevice.getClock(), timeout);
        send(address, maxAPDULengthAccepted, segmentationSupported, service, future);

        //Action to find the response
        return future;
    }

    @Override
    protected void incomingImpl(NPDU npdu) {
        receiveImpl(npdu);
    }

    @Override
    protected void sendConfirmedImpl(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported, ConfirmedRequestService service, ResponseConsumer consumer) {
        try {
            OctetString linkService = checkLinkService(address);
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
        }catch(BACnetException e) {
            LOG.error("Error sending confirmed message", e);
            if(consumer != null) {
                consumer.ex(e);
            }
        }
    }

    @Override
    protected void sendUnconfirmedImpl(Address address, UnconfirmedRequestService service, boolean broadcast) {
        try {
            OctetString linkService = checkLinkService(address);
            network.sendAPDU(address, linkService, new UnconfirmedRequest(service), broadcast);
        } catch (final BACnetRecoverableException e) {
            //TODO Could add to delayed messages
            LOG.info("Send delayed due to recoverable error: {}", e.getMessage());
        } catch (BACnetException e) {
            LOG.error("Error during send", e);
        }
    }

    protected OctetString checkLinkService(Address address) throws BACnetException {
        OctetString linkService = null;
        // Check if the message is to be sent to a specific remote network.
        final int targetNetworkNumber = address.getNetworkNumber().intValue();
        if (targetNetworkNumber != Address.LOCAL_NETWORK && targetNetworkNumber != Address.ALL_NETWORKS
                && targetNetworkNumber != network.getLocalNetworkNumber()) {
            // Going to a specific remote network. Check if we know the router for it.
            linkService = networkRouters.get(targetNetworkNumber);
            if (linkService == null) {
                BACnetException exception = new BACnetException(
                        "Unable to find router to network " + address.getNetworkNumber().intValue());
            }
        }
        return linkService;
    }
}
