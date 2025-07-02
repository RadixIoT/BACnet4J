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

package com.serotonin.bacnet4j.service.unconfirmed;

import static com.serotonin.bacnet4j.TestUtils.awaitTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;

public class AutoDiscoveryTest {
    /**
     * Test to ensure that when an IHave is received from an unknown device that a properly configured RemoteDevice
     * object gets cached.
     *
     * @throws Exception
     */
    @Test
    public void iHaveToWhoIs() throws Exception {
        final TestNetworkMap map = new TestNetworkMap();
        try (final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
                final LocalDevice d2 = new LocalDevice(2,
                        new DefaultTransport(new TestNetwork(map, 2, 0))).initialize()) {
            d1.getEventHandler().addListener(new DeviceEventAdapter() {
                @Override
                public void iHaveReceived(final RemoteDevice d, final RemoteObject o) {
                    assertEquals(Segmentation.segmentedBoth,
                            d.getDeviceProperty(PropertyIdentifier.segmentationSupported));
                }
            });

            // Send an IHave from d2
            d2.sendGlobalBroadcast(new IHaveRequest(d2.getId(), d2.getId(), d2.get(PropertyIdentifier.objectName)));

            // Wait while d1 receives the IHave, sends a WhoIs to d2, and then receives an IAm from d2 and creates
            // a remote device from the content.
            awaitTrue(() -> d1.getCachedRemoteDevice(2) != null);

            // Check a property that is not in the IHave, but is in the IAm
            assertEquals(Segmentation.segmentedBoth,
                    d1.getCachedRemoteDevice(2).getDeviceProperty(PropertyIdentifier.segmentationSupported));
        }
    }
}
