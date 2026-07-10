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

package com.serotonin.bacnet4j.npdu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.NetworkSourceAddress;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public abstract class Network {
    static final Logger LOG = LoggerFactory.getLogger(Network.class);

    private final int localNetworkNumber;
    private Transport transport;

    protected Network() {
        this(0);
    }

    protected Network(int localNetworkNumber) {
        this.localNetworkNumber = localNetworkNumber;
    }

    public int getLocalNetworkNumber() {
        return localNetworkNumber;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    public abstract long getBytesOut();

    public abstract long getBytesIn();

    public abstract NetworkIdentifier getNetworkIdentifier();

    public abstract MaxApduLength getMaxApduLength();

    /**
     * Override as desired if you want to set the Source Address in outgoing messages
     * in the NPDU
     *
     * @param apdu the APDU from which to derive the source address.
     */
    public Address getSourceAddress(APDU apdu) {
        return null;
    }

    public void initialize(Transport transport) throws BACnetException {
        this.transport = transport;
    }

    public boolean isInitialized() {
        return transport != null;
    }

    public abstract void terminate();

    public final Address getLocalBroadcastAddress() {
        return new Address(localNetworkNumber, getBroadcastMAC());
    }

    protected abstract OctetString getBroadcastMAC();

    public abstract Address[] getAllLocalAddresses();

    public abstract Address getLoopbackAddress();

    public final void sendAPDU(Address recipient, OctetString router, APDU apdu, boolean broadcast)
            throws BACnetException {
        ByteQueue npdu = new ByteQueue();

        NPCI npci;
        if (recipient.isGlobal())
            npci = new NPCI(getSourceAddress(apdu));
        else if (isThisNetwork(recipient)) {
            if (router != null)
                throw new BACnetRuntimeException(
                        "Invalid arguments: router address provided for local recipient " + recipient);
            npci = new NPCI(null, getSourceAddress(apdu), apdu.expectsReply());
        } else {
            if (router == null)
                throw new BACnetRuntimeException(
                        "Invalid arguments: router address not provided for remote recipient " + recipient);
            npci = new NPCI(recipient, getSourceAddress(apdu), apdu.expectsReply());
        }

        if (apdu.getNetworkPriority() != null)
            npci.priority(apdu.getNetworkPriority());

        npci.write(npdu);

        apdu.write(npdu);

        sendNPDU(recipient, router, npdu, broadcast, apdu.expectsReply());
    }

    public final void sendNetworkMessage(Address recipient, OctetString router, int messageType, byte[] msg,
            boolean broadcast, boolean expectsReply) throws BACnetException {
        ByteQueue npdu = new ByteQueue();

        NPCI npci;
        if (recipient.isGlobal())
            npci = new NPCI(null, null, expectsReply, messageType, 0);
        else if (isThisNetwork(recipient)) {
            if (router != null)
                throw new BACnetRuntimeException("Invalid arguments: router address provided for a local recipient");
            npci = new NPCI(null, null, expectsReply, messageType, 0);
        } else {
            if (router == null)
                throw new BACnetRuntimeException(
                        "Invalid arguments: router address not provided for a remote recipient");
            npci = new NPCI(recipient, null, expectsReply, messageType, 0);
        }
        npci.write(npdu);

        // Network message
        if (msg != null)
            npdu.push(msg);

        sendNPDU(recipient, router, npdu, broadcast, expectsReply);
    }

    public abstract void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast,
            boolean expectsReply) throws BACnetException;

    protected OctetString getDestination(Address recipient, OctetString link) {
        if (recipient.isGlobal())
            return getLocalBroadcastAddress().getMacAddress();
        if (link != null)
            return link;
        return recipient.getMacAddress();
    }

    public boolean isThisNetwork(Address address) {
        int nn = address.getNetworkNumber().intValue();
        return nn == Address.LOCAL_NETWORK || nn == localNetworkNumber;
    }

    protected synchronized void handleIncomingData(ByteQueue queue, OctetString linkService) {
        try {
            NPDU npdu = handleIncomingDataImpl(queue, linkService);
            if (npdu != null) {
                LOG.debug("Received NPDU from {}: {}", linkService, npdu);
                getTransport().incoming(npdu);
            }
        } catch (Exception e) {
            transport.getLocalDevice().getExceptionDispatcher().fireReceivedException(e);
        }
    }

    protected abstract NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws BACnetException;

    public NPDU parseNpduData(ByteQueue queue, OctetString linkService) throws MessageValidationException {
        // Network layer protocol control information. See 6.2.2
        NPCI npci = new NPCI(queue);
        if (npci.getVersion() != 1)
            throw new MessageValidationException("Invalid protocol version: " + npci.getVersion());

        // Check the destination network number and ignore foreign networks requests
        if (npci.hasDestinationInfo()) {
            int destNet = npci.getDestinationNetwork();
            if (destNet > 0 && destNet != 0xffff && getLocalNetworkNumber() > 0 && getLocalNetworkNumber() != destNet)
                return null;
        }

        Address from;
        if (npci.hasSourceInfo()) {
            LOG.debug("Received source information in message network={}, address={}", npci.getSourceNetwork(),
                    npci.getSourceAddress());
            from = new NetworkSourceAddress(npci.getSourceNetwork(), npci.getSourceAddress());
        } else {
            from = new Address(linkService);
        }

        OctetString ls = linkService;
        if (isThisNetwork(from)) {
            LOG.debug("Received NPDU from local network. From={}, local={}", from, localNetworkNumber);
            ls = null;
        } else {
            // Remember the network router in case we haven't heard from it before. This may happen if the router did
            // not respond to a WhoIsRouterToNetwork request.
            int nn = from.getNetworkNumber().intValue();
            if (!transport.getNetworkRouters().containsKey(nn)) {
                LOG.debug("Network router {} to {} is not currently known. Adding to transport's list", linkService,
                        nn);
                transport.addNetworkRouter(nn, linkService);
            }
            LOG.debug("Received NPDU from remote network. From={}, local={}", from, localNetworkNumber);
        }

        if (npci.isNetworkMessage())
            // Network message
            return new NPDU(from, ls, npci.getMessageType(), queue);

        // APDU message
        return new NPDU(from, ls, queue);
    }
}
