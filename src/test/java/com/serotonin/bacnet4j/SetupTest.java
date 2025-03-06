package com.serotonin.bacnet4j;

import com.serotonin.bacnet4j.npdu.test.SynchronousTestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.SynchronousTransport;
import com.serotonin.warp.WarpClock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SetupTest {
    static final Logger LOG = LoggerFactory.getLogger(SetupTest.class);

    private final int timeout = 200;

    @Test
    public void setup() throws Exception {
        final int count = 20;
        final TestNetworkMap map = new TestNetworkMap();
        final WarpClock clock = new WarpClock();

        final List<LocalDevice> lds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lds.add(new LocalDevice(i, new SynchronousTransport(new SynchronousTestNetwork(map, i, 0).withTimeout(timeout)), clock));
        }

        for (int i = 0; i < count; i++) {
            lds.get(i).initialize();
        }

        for (int i = 0; i < count; i++) {
            final LocalDevice d = lds.get(i);
            for (int j = 0; j < count; j++) {
                if (i != j) {
                    if ((i + j) % 2 == 0) {
                        d.getRemoteDevice(j).get();
                    } else {
                        d.getRemoteDeviceBlocking(j);
                    }
                }
            }
        }

        for (int i = 0; i < count; i++) {
            lds.get(i).terminate();
        }
    }
}
