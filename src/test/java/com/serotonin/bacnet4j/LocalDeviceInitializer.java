package com.serotonin.bacnet4j;

import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.test.AbstractTestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.AbstractTransport;
import com.serotonin.warp.WarpClock;
import com.serotonin.warp.WarpScheduledExecutorService;

import java.time.ZoneOffset;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Helper interface for working with Local Devices
 * @author Terry Packer
 */
public interface LocalDeviceInitializer {

    default WarpClock getClock() {
        return new WarpClock(ZoneOffset.UTC);
    }

    default ScheduledExecutorService createExecutorService(WarpClock clock) {
        return new WarpScheduledExecutorService(clock);
    }

    /**
     * Create the desired transport
     * @param network - the network for the transport to use
     * @return - transport for the network
     */
    AbstractTransport createTransport(Network network);

    default AbstractTestNetwork createTestNetwork(TestNetworkMap<AbstractTestNetwork> map, int address, int sendDelay, int timeout) {
        return this.createTestNetwork(map, address, sendDelay).withTimeout(timeout);
    }

    /**
     * Create the test network
     * @param map - map of network
     * @param address - address in the network
     * @param sendDelay - delay before sending messages
     * @return the test network
     */
    AbstractTestNetwork createTestNetwork(TestNetworkMap<AbstractTestNetwork> map, int address, int sendDelay);
}
