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
