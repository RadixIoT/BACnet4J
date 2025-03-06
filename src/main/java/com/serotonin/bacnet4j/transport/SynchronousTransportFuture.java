package com.serotonin.bacnet4j.transport;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;

import java.time.Clock;

/**
 * @author Terry Packer
 */
public class SynchronousTransportFuture extends ServiceFutureImpl {
    private final Clock clock;
    private final long timeout;
    public SynchronousTransportFuture(Clock clock, int timeoutMs) {
        this.clock = clock;
        this.timeout = clock.instant().toEpochMilli() + timeoutMs;
    }

    @Override
    public synchronized <T extends AcknowledgementService> T get() throws BACnetException {
        if (done) {
            return result();
        }else if(clock.instant().toEpochMilli() > timeout) {
            ex(new BACnetTimeoutException());
            return result();
        }else {
            throw new BACnetException("Something wrong with future, TODO Fix me");
        }
    }

}
