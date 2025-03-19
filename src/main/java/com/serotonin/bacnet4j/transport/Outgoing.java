package com.serotonin.bacnet4j.transport;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRecoverableException;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;

/**
 * @author Terry Packer
 */
abstract class Outgoing {
    protected final AbstractTransport transport;
    protected final Address address;
    protected OctetString linkService;
    // TODO remove this when it is no longer needed.
    protected final Exception stack;

    protected Outgoing(AbstractTransport transport, final Address address, final Exception stack) {
        this.transport = transport;
        if (address == null)
            throw new IllegalArgumentException("address cannot be null");
        this.address = address;
        this.stack = stack;
    }

    void send() {
        // Check if the message is to be sent to a specific remote network.
        try {
            linkService = transport.checkLinkService(address);
        } catch (BACnetException e) {
            handleException(e);
            return;
        }

        try {
            sendImpl();
        } catch (final BACnetRecoverableException e) {
            DefaultTransport.LOG.info("Send delayed due to recoverable error: {}", e.getMessage());
            transport.sendDelayedOutgoing(new DelayedOutgoing(transport, this));
        } catch (final BACnetException e) {
            handleException(e);
        }
    }

    protected abstract void sendImpl() throws BACnetException;

    protected abstract void handleException(BACnetException e);
}
