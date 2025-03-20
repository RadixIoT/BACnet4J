package com.serotonin.bacnet4j;

import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.test.AbstractTestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.AbstractTransport;
import com.serotonin.bacnet4j.transport.DefaultTransport;

/**
 * Helper interface for working with Local Devices to use asynchronous messaging
 * @author Terry Packer
 */
public interface DefaultLocalDeviceInitializer extends LocalDeviceInitializer {

    @Override
    default AbstractTransport createTransport(Network network) {
        return new DefaultTransport(network);
    }

    @Override
    default AbstractTestNetwork createTestNetwork(TestNetworkMap<AbstractTestNetwork> map, int address, int sendDelay) {
        return new TestNetwork(map, address, sendDelay);
    }
}
