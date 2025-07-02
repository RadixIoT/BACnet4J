/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 Radix IoT LLC. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Radix IoT LLC,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.radixiot.com for commercial license options.
 */

package com.serotonin.bacnet4j;

import org.junit.After;
import org.junit.Before;

import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.util.DiscoveryUtils;

import lohbihler.warp.WarpClock;

/**
 * Common base class for tests that use real local devices and a warp clock.
 *
 * @author Matthew
 */
abstract public class AbstractTest {
    protected static final int TIMEOUT = 500;

    private final TestNetworkMap map = new TestNetworkMap();
    protected final WarpClock clock = new WarpClock();
    protected final LocalDevice d1 = new LocalDevice(1,
            new DefaultTransport(new TestNetwork(map, 1, 0).withTimeout(TIMEOUT))).withClock(clock);
    protected final LocalDevice d2 = new LocalDevice(2,
            new DefaultTransport(new TestNetwork(map, 2, 0).withTimeout(TIMEOUT))).withClock(clock);
    protected final LocalDevice d3 = new LocalDevice(3, new DefaultTransport(new TestNetwork(map, 3, 0)))
            .withClock(clock);
    protected final LocalDevice d4 = new LocalDevice(4, new DefaultTransport(new TestNetwork(map, 4, 0)))
            .withClock(clock);
    protected RemoteDevice rd1;
    protected RemoteDevice rd2;
    protected RemoteDevice rd3;

    @Before
    public void abstractBefore() throws Exception {
        beforeInit();

        d1.initialize();
        d2.initialize();
        d3.initialize();
        d4.initialize();

        // Get d1 as a remote object.
        rd1 = d2.getRemoteDevice(1).get();
        rd2 = d1.getRemoteDevice(2).get();
        rd3 = d1.getRemoteDevice(3).get();

        DiscoveryUtils.getExtendedDeviceInformation(d1, rd1);
        DiscoveryUtils.getExtendedDeviceInformation(d1, rd2);
        DiscoveryUtils.getExtendedDeviceInformation(d1, rd3);

        DiscoveryUtils.getExtendedDeviceInformation(d2, rd1);
        DiscoveryUtils.getExtendedDeviceInformation(d2, rd2);
        DiscoveryUtils.getExtendedDeviceInformation(d2, rd3);

        DiscoveryUtils.getExtendedDeviceInformation(d3, rd1);
        DiscoveryUtils.getExtendedDeviceInformation(d3, rd2);
        DiscoveryUtils.getExtendedDeviceInformation(d3, rd3);

        DiscoveryUtils.getExtendedDeviceInformation(d4, rd1);
        DiscoveryUtils.getExtendedDeviceInformation(d4, rd2);
        DiscoveryUtils.getExtendedDeviceInformation(d4, rd3);

        afterInit();
    }

    public void beforeInit() throws Exception {
        // Override as required
    }

    public void afterInit() throws Exception {
        // Override as required
    }

    @After
    public void abstractAfter() {
        // Shut down
        d1.terminate();
        d2.terminate();
        d3.terminate();
        d4.terminate();
    }
}
