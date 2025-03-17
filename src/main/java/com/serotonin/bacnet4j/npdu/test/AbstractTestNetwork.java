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
package com.serotonin.bacnet4j.npdu.test;

import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.NetworkIdentifier;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.NetworkSourceAddress;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Terry Packer
 */
public abstract class AbstractTestNetwork<T extends AbstractTestNetwork> extends Network {
    static final Logger LOG = LoggerFactory.getLogger(AbstractTestNetwork.class);

    public static final OctetString BROADCAST = new OctetString(new byte[0]);

    protected final TestNetworkMap<T> networkMap;
    protected final Address address;
    protected final int sendDelay;

    protected int timeout = 6000;
    protected int segTimeout = 1000;

    public AbstractTestNetwork(final TestNetworkMap map, final int address, final int sendDelay) {
        this(map, new NetworkSourceAddress(Address.LOCAL_NETWORK, new byte[] { (byte) address }), sendDelay);
    }

    public AbstractTestNetwork(final TestNetworkMap map, final Address address, final int sendDelay) {
        super(0);
        this.networkMap = map;
        this.address = address;
        this.sendDelay = sendDelay;
    }

    public T withTimeout(final int timeout) {
        this.timeout = timeout;
        return (T)this;
    }

    public T withSegTimeout(final int segTimeout) {
        this.segTimeout = segTimeout;
        return (T)this;
    }

    /**
     * Passes the the data over to the given network instance.
     *
     * @param recipient
     * @param data
     */
    protected void receive(final AbstractTestNetwork<T> recipient, final byte[] data) {
        LOG.debug("Sending data from {} to {}", address, recipient.address);
        recipient.handleIncomingData(new ByteQueue(data), address.getMacAddress());
    }

    @Override
    public void initialize(final Transport transport) throws Exception {
        super.initialize(transport);
        transport.setTimeout(timeout);
        transport.setRetries(0); // no retries, there's no network here after all
        transport.setSegTimeout(segTimeout);
        networkMap.add(address, (T)this);
    }

    @Override
    public void terminate() {
        networkMap.remove(address);
    }

    @Override
    public NetworkIdentifier getNetworkIdentifier() {
        return new TestNetworkIdentifier();
    }

    @Override
    public MaxApduLength getMaxApduLength() {
        return MaxApduLength.UP_TO_1476;
    }

    @Override
    protected OctetString getBroadcastMAC() {
        return BROADCAST;
    }

    @Override
    public Address[] getAllLocalAddresses() {
        return new Address[] { address };
    }

    @Override
    public Address getLoopbackAddress() {
        return address;
    }

    @Override
    public Address getSourceAddress(final APDU apdu) {
        return address;
    }
}
