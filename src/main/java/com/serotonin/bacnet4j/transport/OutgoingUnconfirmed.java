package com.serotonin.bacnet4j.transport;

import com.serotonin.bacnet4j.apdu.UnconfirmedRequest;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;

/**
 * @author Terry Packer
 */
class OutgoingUnconfirmed extends Outgoing {
    private final UnconfirmedRequestService service;
    private final boolean broadcast;

    public OutgoingUnconfirmed(AbstractTransport transport, final Address address, final UnconfirmedRequestService service,
                               final boolean broadcast, final Exception stack) {
        super(transport, address, stack);
        this.service = service;
        this.broadcast = broadcast;
    }

    @Override
    protected void sendImpl() throws BACnetException {
        transport.network.sendAPDU(address, linkService, new UnconfirmedRequest(service), broadcast);
    }

    @Override
    protected void handleException(final BACnetException e) {
        DefaultTransport.LOG.error("Error during send", e);
    }

    @Override
    public String toString() {
        return "OutgoingUnconfirmed [service=" + service + ", broadcast=" + broadcast + ", address=" + address
                + ", linkService=" + linkService + "]";
    }
}
