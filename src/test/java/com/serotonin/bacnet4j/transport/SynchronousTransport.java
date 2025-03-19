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

import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;

/**
 * Attempt to make test messaging synchronous and still test the default transport layer
 * @author Terry Packer
 */
public class SynchronousTransport extends AbstractTransport {

    public SynchronousTransport(Network network) {
        super(network);
    }

    @Override
    protected ServiceFutureImpl getFuture() {
        return new SynchronousTransportFuture(this.localDevice.getClock(), timeout);
    }

    @Override
    public void incoming(NPDU npdu) {
        receiveImpl(npdu);
    }

    @Override
    protected void sendDelayedOutgoing(DelayedOutgoing message) {
        //Error occurred and we aren't going to re-send this, should not happen on a
        // perfect network
        throw new UnsupportedOperationException();
    }

    @Override
    protected void sendOutgoingUnconfirmed(OutgoingUnconfirmed message) {
        message.send();
    }

    @Override
    protected void sendOutgoingConfirmed(OutgoingConfirmed message) {
        message.send();
    }
}
