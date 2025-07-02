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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;

import lohbihler.warp.WarpClock;

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
            lds.add(new LocalDevice(i, new DefaultTransport(new TestNetwork(map, i, 0).withTimeout(timeout)))
                    .withClock(clock));
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
