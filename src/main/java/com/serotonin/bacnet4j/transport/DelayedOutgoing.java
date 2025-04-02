package com.serotonin.bacnet4j.transport;

/**
 * @author Terry Packer
 */
class DelayedOutgoing {
    private final AbstractTransport defaultTransport;
    final Outgoing outgoing;
    final long retryTime;

    public DelayedOutgoing(AbstractTransport defaultTransport, final Outgoing outgoing) {
        super();
        this.defaultTransport = defaultTransport;
        this.outgoing = outgoing;
        // Retry in 1 second.
        retryTime = defaultTransport.localDevice.getClock().millis() + 1000;
    }

    boolean isReady() {
        return retryTime <= defaultTransport.localDevice.getClock().millis();
    }
}
