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

package com.serotonin.bacnet4j.npdu.ip;

import static com.serotonin.bacnet4j.TestUtils.awaitTrue;
import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.docker.DockerRemoteDevice;
import com.serotonin.bacnet4j.event.DefaultDeviceEventListener;
import com.serotonin.bacnet4j.event.DeviceEventListener;
import com.serotonin.bacnet4j.transport.DefaultTransport;

/**
 * These tests assume the existence of the remote devices created by the docker compose, and so only runs if being
 * executed in a docker environment. See @{@link DockerRemoteDevice}, which sends both broadcast and unicast
 * messages every second.
 */
public class IpNetworkTest {
    @Test
    public void receiveUnicastAndBroadcast() throws Exception {
        assumeTrue(TestUtils.isDockerEnv());

        // The values here match those given in the docker-compose.yml file.
        IpNetwork network = new IpNetworkBuilder()
                .withLocalBindAddress("192.168.1.11")
                .withBroadcast("192.168.1.255", 24)
                .build();
        try (LocalDevice localDevice = new LocalDevice(11, new DefaultTransport(network))) {
            localDevice.initialize();

            CopyOnWriteArraySet<Integer> iHaves = new CopyOnWriteArraySet<>();
            CopyOnWriteArraySet<Integer> iAms = new CopyOnWriteArraySet<>();

            DeviceEventListener listener = new DefaultDeviceEventListener() {
                @Override
                public void iHaveReceived(RemoteDevice d, RemoteObject o) {
                    iHaves.add(d.getInstanceNumber());
                }

                @Override
                public void iAmReceived(RemoteDevice d) {
                    iAms.add(d.getInstanceNumber());
                }
            };
            localDevice.getEventHandler().addListener(listener);

            awaitTrue(() -> iAms.contains(22));
            awaitTrue(() -> iAms.contains(33));
            awaitTrue(() -> iHaves.contains(22));
            awaitTrue(() -> iHaves.contains(33));
        }
    }

    @Test
    public void wildcardReceiveUnicastAndBroadcast() throws Exception {
        assumeTrue(TestUtils.isDockerEnv());

        // The values here match those given in the docker-compose.yml file.
        IpNetwork network = new IpNetworkBuilder()
                .withLocalBindAddress("0.0.0.0")
                .withBroadcast("192.168.1.255", 24)
                .build();
        try (LocalDevice localDevice = new LocalDevice(11, new DefaultTransport(network))) {
            localDevice.initialize();

            CopyOnWriteArraySet<Integer> iHaves = new CopyOnWriteArraySet<>();
            CopyOnWriteArraySet<Integer> iAms = new CopyOnWriteArraySet<>();

            DeviceEventListener listener = new DefaultDeviceEventListener() {
                @Override
                public void iHaveReceived(RemoteDevice d, RemoteObject o) {
                    iHaves.add(d.getInstanceNumber());
                }

                @Override
                public void iAmReceived(RemoteDevice d) {
                    iAms.add(d.getInstanceNumber());
                }
            };
            localDevice.getEventHandler().addListener(listener);

            awaitTrue(() -> iAms.contains(22));
            awaitTrue(() -> iAms.contains(33));
            awaitTrue(() -> iHaves.contains(22));
            awaitTrue(() -> iHaves.contains(33));
        }
    }

}
