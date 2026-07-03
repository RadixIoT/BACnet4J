/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.event.DefaultReinitializeDeviceHandler;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.npdu.ipv6.Ipv6Network;
import com.serotonin.bacnet4j.npdu.ipv6.Ipv6NetworkBuilder;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;

/**
 * Integration test that illustrates how to recreate a network to incorporate changes made to a network port object.
 */
public class NetworkAlteringTest {
    private static final String MULTICAST_ADDRESS = "FF05::BAC0";
    private static final ObjectIdentifier NETWORK_PORT_ID = new ObjectIdentifier(ObjectType.networkPort, 2);
    LocalDevice localDevice;
    int networkNumber = 100;

    @After
    public void after() {
        if (localDevice != null) {
            localDevice.terminate();
        }
    }

    /**
     * Tests setting pending changes in a network port object, and then requesting a device reinitialization that
     * persists those changes, and restarts the network.
     */
    @Test
    public void alterNetworkSettingAndReinitialize() throws Exception {
        // Initialize the original network and local device.
        var network = createNetwork();
        localDevice = new LocalDevice(1, new DefaultTransport(network));
        var npo = localDevice.addObject(createNetworkPortObject(network));

        localDevice.initialize();

        // Set up the handler to recreate the network.
        localDevice.setReinitializeDeviceHandler(new DefaultReinitializeDeviceHandler() {
            @Override
            protected void warmstart(LocalDevice localDevice, Address from) throws BACnetErrorException {
                // Both WARMSTART and ACTIVATE_CHANGES should restart the network, although otherwise they perform
                // other functions. For this test we override both and have them only restart the network.
                activateChanges(localDevice, from);
            }

            @Override
            protected void activateChanges(LocalDevice localDevice, Address from) throws BACnetErrorException {
                // This test only handles changes to the network number, but implementations will need to handle all
                // properties that qualify as "pending changes".
                var networkNumberChanged = new AtomicBoolean(false);
                localDevice.getLocalObjects().forEach(obj -> {
                    // Find network port objects that have pending changes
                    if (obj instanceof Ipv6NetworkPortObject npo && npo.isChanged()) {
                        // "Persist" the object's pending changes. Real implementation will rewrite networking
                        // files or otherwise store values. Persistence here means setting the network number to the
                        // new value.
                        var n = (Unsigned16) npo.getPendingChanges().get(PropertyIdentifier.networkNumber);
                        networkNumber = n.intValue();
                        networkNumberChanged.set(true);
                    }
                });
                if (networkNumberChanged.get()) {
                    try {
                        // Recreate the network with the new network number, and replace it in the local device, and
                        // create a new network port object with the new network reference.
                        var network = createNetwork();
                        var networkPort = createNetworkPortObject(network);
                        localDevice.replaceTransport(new DefaultTransport(network));
                        localDevice.removeObject(NETWORK_PORT_ID);
                        localDevice.addObject(networkPort);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Assert.fail("No pending changes detected in any network port object");
                }
            }
        });

        // Write a change to the network port so it has pending changes.
        npo.writeProperty(new ValueSource(), PropertyIdentifier.networkNumber, new Unsigned16(101));
        assertTrue(npo.isChanged());

        // Now simulate sending a warm start to the device. This will cause activateChanges above to be called in
        // the current thread because we are calling `handle` directly.
        new ReinitializeDeviceRequest(ReinitializeDeviceRequest.ReinitializedStateOfDevice.warmstart)
                .handle(localDevice, null);

        // Ensure that the current network port object reports the correct network number.
        npo = localDevice.getObject(NETWORK_PORT_ID);
        assertFalse(npo.isChanged());
        assertEquals(new Unsigned16(101), npo.readProperty(PropertyIdentifier.networkNumber));
    }

    Ipv6Network createNetwork() {
        return new Ipv6NetworkBuilder(MULTICAST_ADDRESS)
                .localBindAddress("::1")
                .localNetworkNumber(networkNumber)
                .build();
    }

    Ipv6NetworkPortObject createNetworkPortObject(Ipv6Network network) {
        return new Ipv6NetworkPortObject(localDevice, network, NETWORK_PORT_ID.getInstanceNumber());
    }
}
