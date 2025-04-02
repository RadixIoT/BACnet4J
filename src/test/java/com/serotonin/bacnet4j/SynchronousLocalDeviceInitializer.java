package com.serotonin.bacnet4j;

import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.test.AbstractTestNetwork;
import com.serotonin.bacnet4j.npdu.test.SynchronousTestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.AbstractTransport;
import com.serotonin.bacnet4j.transport.SynchronousTransport;

/**
 * Helper interface for working with Local Devices to use synchronous messaging
 * @author Terry Packer
 */
public interface SynchronousLocalDeviceInitializer extends LocalDeviceInitializer {

    @Override
    default AbstractTransport createTransport(Network network) {
        return new SynchronousTransport(network);
    }

    @Override
    default AbstractTestNetwork createTestNetwork(TestNetworkMap map, int address, int sendDelay) {
        return new SynchronousTestNetwork(map, address, sendDelay);
    }
}
