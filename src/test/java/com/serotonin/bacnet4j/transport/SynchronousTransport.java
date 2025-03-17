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

/**
 * Attempt to make test messaging synchronous and still test the default transport layer
 * @author Terry Packer
 */
public class SynchronousTransport extends AbstractTransport {

    public SynchronousTransport(Network network) {
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
