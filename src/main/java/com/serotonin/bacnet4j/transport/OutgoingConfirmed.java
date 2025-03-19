package com.serotonin.bacnet4j.transport;

import com.serotonin.bacnet4j.ResponseConsumer;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.ConfirmedRequest;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.ServiceTooBigException;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * @author Terry Packer
 */
class OutgoingConfirmed extends Outgoing {
    private final int maxAPDULengthAccepted;
    private final Segmentation segmentationSupported;
    private final ConfirmedRequestService service;
    private final ResponseConsumer consumer;

    public OutgoingConfirmed(AbstractTransport transport, final Address address, final int maxAPDULengthAccepted,
                             final Segmentation segmentationSupported, final ConfirmedRequestService service,
                             final ResponseConsumer consumer, final Exception stack) {
        super(transport, address, stack);
        this.maxAPDULengthAccepted = maxAPDULengthAccepted;
        this.segmentationSupported = segmentationSupported;
        this.service = service;
        this.consumer = consumer;
    }

    @Override
    protected void sendImpl() throws BACnetException {
        final ByteQueue serviceData = new ByteQueue();
        service.write(serviceData);

        final UnackedMessageContext ctx = new UnackedMessageContext(transport.localDevice.getClock(), transport.timeout, transport.retries,
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

            key = transport.unackedMessages.addClient(address, linkService, ctx);
            // Prepare the segmenting session.
            ctx.setSegmentTemplate(new ConfirmedRequest(true, true, true, AbstractTransport.MAX_SEGMENTS, transport.network.getMaxApduLength(),
                    key.getInvokeId(), 0, transport.segWindow, service.getChoiceId(), null, service.getNetworkPriority()));
            ctx.setServiceData(serviceData);
            ctx.setSegBuf(new byte[maxServiceData]);

            // Send an initial message to negotiate communication terms.
            apdu = ctx.getSegmentTemplate().clone(true, 0, transport.segWindow, ctx.getNextSegment());
        } else {
            key = transport.unackedMessages.addClient(address, linkService, ctx);
            // We can send the whole APDU in one shot.
            apdu = new ConfirmedRequest(false, false, true, AbstractTransport.MAX_SEGMENTS, transport.network.getMaxApduLength(),
                    key.getInvokeId(), (byte) 0, 0, service.getChoiceId(), serviceData,
                    service.getNetworkPriority());
        }

        ctx.setOriginalApdu(apdu);
        transport.sendForResponse(key, ctx);
    }

    public ResponseConsumer getConsumer() {
        return consumer;
    }

    @Override
    protected void handleException(final BACnetException e) {
        if (consumer == null) {
            DefaultTransport.LOG.warn("Error during send", e);
            DefaultTransport.LOG.warn("Original stack", stack);
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
