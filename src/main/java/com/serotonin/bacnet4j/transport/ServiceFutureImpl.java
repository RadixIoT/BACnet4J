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
 * See www.infiniteautomation.com for commercial license options.
 *
 * @author Matthew Lohbihler
 */
package com.serotonin.bacnet4j.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.ResponseConsumer;
import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.apdu.Abort;
import com.serotonin.bacnet4j.apdu.AckAPDU;
import com.serotonin.bacnet4j.apdu.Reject;
import com.serotonin.bacnet4j.exception.AbortAPDUException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.exception.ErrorAPDUException;
import com.serotonin.bacnet4j.exception.RejectAPDUException;
import com.serotonin.bacnet4j.exception.ServiceTooBigException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

public class ServiceFutureImpl implements ServiceFuture, ResponseConsumer {
    static final Logger LOG = LoggerFactory.getLogger(ServiceFutureImpl.class);

    private AcknowledgementService ack;
    private AckAPDU fail;
    private BACnetException ex;
    private volatile boolean done;
    private volatile State state = State.NEW;

    @Override
    public synchronized <T extends AcknowledgementService> T get() throws BACnetException {
        if (done) {
            return result();
        }

        ThreadUtils.wait(this);

        return result();
    }

    @SuppressWarnings("unchecked")
    private <T extends AcknowledgementService> T result() throws BACnetException {
        if (ex != null) {
            // We want to preserve the original type of the exception, but not have
            // to have a big if/then/else chain to handle all the exception types.
            // Timeout is probably the only one most clients really care to handle,
            // so only that one is currently handled.
            if (ex instanceof BACnetTimeoutException) {
                throw new BACnetTimeoutException(ex.getMessage(), ex);
            } else if (ex instanceof ServiceTooBigException) {
                throw new ServiceTooBigException(ex.getMessage());
            }
            throw new BACnetException(ex.getMessage(), ex);
        }
        if (fail != null) {
            if (fail instanceof com.serotonin.bacnet4j.apdu.Error error)
                throw new ErrorAPDUException(error);
            else if (fail instanceof Reject reject)
                throw new RejectAPDUException(reject);
            else if (fail instanceof Abort abort)
                throw new AbortAPDUException(abort);
        }
        return (T) ack;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void queued() {
        state = State.QUEUED;
    }

    @Override
    public void sent() {
        state = State.SENT;
    }

    @Override
    public synchronized void success(final AcknowledgementService ack) {
        this.ack = ack;
        state = State.DONE;
        complete();
    }

    @Override
    public synchronized void fail(final AckAPDU ack) {
        if (ack == null) {
            LOG.warn("ServiceFuture fail called with null argument", new Exception());
        }
        fail = ack;
        state = State.FAILED;
        complete();
    }

    @Override
    public synchronized void ex(final BACnetException e) {
        if (e == null) {
            LOG.warn("ServiceFuture ex called with null argument", new Exception());
        }
        ex = e;
        state = State.EXCEPTION;
        complete();
    }

    private void complete() {
        done = true;
        notify();
    }
}
