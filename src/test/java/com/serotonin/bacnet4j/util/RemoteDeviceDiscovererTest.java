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

package com.serotonin.bacnet4j.util;

import static com.serotonin.bacnet4j.TestUtils.assertListEqualsIgnoreOrder;
import static com.serotonin.bacnet4j.TestUtils.await;
import static com.serotonin.bacnet4j.TestUtils.indexOf;
import static com.serotonin.bacnet4j.TestUtils.toList;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.service.unconfirmed.IHaveRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;

public class RemoteDeviceDiscovererTest {
    private final TestNetworkMap map = new TestNetworkMap();

    @Test
    public void noCallback() throws Exception {
        final BiPredicate<Integer, RemoteDevice> predicate = (i, d) -> d.getInstanceNumber() == i;

        final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 1))).initialize();
        final LocalDevice d2 = new LocalDevice(12, new DefaultTransport(new TestNetwork(map, 112, 1))).initialize();
        final LocalDevice d3 = new LocalDevice(13, new DefaultTransport(new TestNetwork(map, 113, 1))).initialize();
        final LocalDevice d4 = new LocalDevice(14, new DefaultTransport(new TestNetwork(map, 114, 1))).initialize();
        final LocalDevice d5 = new LocalDevice(15, new DefaultTransport(new TestNetwork(map, 115, 1))).initialize();
        final LocalDevice d6 = new LocalDevice(16, new DefaultTransport(new TestNetwork(map, 116, 1))).initialize();
        final LocalDevice d7 = new LocalDevice(17, new DefaultTransport(new TestNetwork(map, 117, 1))).initialize();

        final RemoteDeviceDiscoverer discoverer = new RemoteDeviceDiscoverer(d1);
        discoverer.start();
        Thread.sleep(300);

        assertListEqualsIgnoreOrder(toList(12, 13, 14, 15, 16, 17), discoverer.getRemoteDevices(), predicate);
        assertListEqualsIgnoreOrder(toList(12, 13, 14, 15, 16, 17), discoverer.getLatestRemoteDevices(), predicate);

        //
        // Add some more devices
        final LocalDevice d8 = new LocalDevice(18, new DefaultTransport(new TestNetwork(map, 118, 1))).initialize();
        d8.sendGlobalBroadcast(d8.getIAm());
        Thread.sleep(300);

        assertListEqualsIgnoreOrder(toList(12, 13, 14, 15, 16, 17, 18), discoverer.getRemoteDevices(), predicate);
        assertListEqualsIgnoreOrder(toList(18), discoverer.getLatestRemoteDevices(), predicate);

        //
        // Add some more devices
        d2.sendGlobalBroadcast(d2.getIAm());
        d3.sendGlobalBroadcast(d3.getIAm());
        final LocalDevice d9 = new LocalDevice(19, new DefaultTransport(new TestNetwork(map, 119, 1))).initialize();
        d9.sendGlobalBroadcast(d9.getIAm());
        final LocalDevice d10 = new LocalDevice(20, new DefaultTransport(new TestNetwork(map, 120, 1))).initialize();
        d10.sendGlobalBroadcast(d10.getIAm());
        Thread.sleep(300);

        assertListEqualsIgnoreOrder(toList(12, 13, 14, 15, 16, 17, 18, 19, 20), discoverer.getRemoteDevices(),
                predicate);
        assertListEqualsIgnoreOrder(toList(19, 20), discoverer.getLatestRemoteDevices(), predicate);

        // Stop and add more devices to make sure they are not discovered.
        discoverer.stop();
        final LocalDevice d11 = new LocalDevice(21, new DefaultTransport(new TestNetwork(map, 121, 1))).initialize();
        d11.sendGlobalBroadcast(d11.getIAm());
        final LocalDevice d12 = new LocalDevice(22, new DefaultTransport(new TestNetwork(map, 122, 1))).initialize();
        d12.sendGlobalBroadcast(d12.getIAm());
        Thread.sleep(300);

        assertListEqualsIgnoreOrder(toList(12, 13, 14, 15, 16, 17, 18, 19, 20), discoverer.getRemoteDevices(),
                predicate);
        assertListEqualsIgnoreOrder(new ArrayList<Integer>(), discoverer.getLatestRemoteDevices(), predicate);

        // Cleanup
        d1.terminate();
        d2.terminate();
        d3.terminate();
        d4.terminate();
        d5.terminate();
        d6.terminate();
        d7.terminate();
        d8.terminate();
        d9.terminate();
        d10.terminate();
        d11.terminate();
        d12.terminate();
    }

    @Test
    public void withCallback() throws Exception {
        final BiPredicate<RemoteDevice, Integer> predicate = (d, i) -> d.getInstanceNumber() == i;

        final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 1))).initialize();
        final LocalDevice d2 = new LocalDevice(12, new DefaultTransport(new TestNetwork(map, 112, 1))).initialize();
        final LocalDevice d3 = new LocalDevice(13, new DefaultTransport(new TestNetwork(map, 113, 1))).initialize();
        final LocalDevice d4 = new LocalDevice(14, new DefaultTransport(new TestNetwork(map, 114, 1))).initialize();
        final LocalDevice d5 = new LocalDevice(15, new DefaultTransport(new TestNetwork(map, 115, 1))).initialize();
        final LocalDevice d6 = new LocalDevice(16, new DefaultTransport(new TestNetwork(map, 116, 1))).initialize();
        final LocalDevice d7 = new LocalDevice(17, new DefaultTransport(new TestNetwork(map, 117, 1))).initialize();

        final List<Integer> expected = toList(12, 13, 14, 15, 16, 17);
        final RemoteDeviceDiscoverer discoverer = new RemoteDeviceDiscoverer(d1, (d) -> {
            synchronized (expected) {
                final int index = indexOf(expected, d, predicate);
                if (index == -1)
                    Assert.fail("RemoteDevice " + d.getInstanceNumber() + " not found in expected list");
                expected.remove(index);
            }
        });

        discoverer.start();
        Thread.sleep(300);

        //
        // Add some more devices
        expected.add(18);
        final LocalDevice d8 = new LocalDevice(18, new DefaultTransport(new TestNetwork(map, 118, 1))).initialize();
        d8.sendGlobalBroadcast(d8.getIAm());
        Thread.sleep(300);

        //
        // Send some duplicate IAms
        d2.sendGlobalBroadcast(d2.getIAm());
        d3.sendGlobalBroadcast(d3.getIAm());

        //
        // Add some more devices
        expected.add(19);
        expected.add(20);
        final LocalDevice d9 = new LocalDevice(19, new DefaultTransport(new TestNetwork(map, 119, 1))).initialize();
        d9.sendGlobalBroadcast(d9.getIAm());
        final LocalDevice d10 = new LocalDevice(20, new DefaultTransport(new TestNetwork(map, 120, 1))).initialize();
        d10.sendGlobalBroadcast(d10.getIAm());
        Thread.sleep(300);

        // Stop and add more devices to make sure they are not discovered.
        discoverer.stop();
        final LocalDevice d11 = new LocalDevice(21, new DefaultTransport(new TestNetwork(map, 121, 1))).initialize();
        d11.sendGlobalBroadcast(d11.getIAm());
        final LocalDevice d12 = new LocalDevice(22, new DefaultTransport(new TestNetwork(map, 122, 1))).initialize();
        d12.sendGlobalBroadcast(d12.getIAm());
        Thread.sleep(300);

        // Cleanup
        d1.terminate();
        d2.terminate();
        d3.terminate();
        d4.terminate();
        d5.terminate();
        d6.terminate();
        d7.terminate();
        d8.terminate();
        d9.terminate();
        d10.terminate();
        d11.terminate();
        d12.terminate();
    }

    @Test
    public void expirationCheck() throws Exception {
        BlockingQueue<RemoteDevice> results = new ArrayBlockingQueue<>(1);

        try (LocalDevice d1 = createLocalDevice(1); LocalDevice d2 = createLocalDevice(2)) {
            d1.initialize();
            d2.initialize();

            try (RemoteDeviceDiscoverer discoverer = new RemoteDeviceDiscoverer(d1, results::add, d -> true)) {
                discoverer.start();

                RemoteDevice firstResponse = results.poll(10, TimeUnit.SECONDS);
                Assert.assertNotNull(firstResponse);
                Assert.assertEquals(2, firstResponse.getInstanceNumber());

                d2.sendGlobalBroadcast(d2.getIAm());
                RemoteDevice secondResponse = results.poll(10, TimeUnit.SECONDS);
                Assert.assertNotNull(secondResponse);
                Assert.assertEquals(2, secondResponse.getInstanceNumber());
            }
        }

        assertTrue(results.isEmpty());
    }

    private LocalDevice createLocalDevice(int address) {
        return new LocalDevice(address, new DefaultTransport(new TestNetwork(map, address, 1)));
    }

    @Test
    public void alternateMaxApduLength() throws Exception {
        try (LocalDevice d1 = createLocalDevice(1); LocalDevice d2 = createLocalDevice(2)) {
            d2.getDeviceObject().writePropertyInternal(PropertyIdentifier.maxApduLengthAccepted,
                    MaxApduLength.UP_TO_50.getMaxLength());
            d2.getDeviceObject()
                    .writePropertyInternal(PropertyIdentifier.segmentationSupported, Segmentation.noSegmentation);

            d1.initialize();
            d2.initialize();

            IHaveRequest iHave = new IHaveRequest(d2.getId(), d2.getDeviceObject().getId(),
                    d2.getDeviceObject().get(PropertyIdentifier.objectName));
            d2.sendGlobalBroadcast(iHave);

            AtomicBoolean received = new AtomicBoolean();
            d1.getEventHandler().addListener(new DeviceEventAdapter() {
                @Override
                public void iHaveReceived(final RemoteDevice d, final RemoteObject o) {
                    System.out.println("IHave received");
                    try {
                        DiscoveryUtils.getExtendedDeviceInformation(d1, d);
                        System.out.println(
                                "ExtendedDeviceInformation received: model name=" + d.getModelName() + ", maxApduLength=" + d.getMaxAPDULengthAccepted());
                    } catch (BACnetException e) {
                        System.out.println("Exception while getting extended device information: " + e.getMessage());
                    }
                    received.set(true);
                }
            });

            await(received::get);
            assertTrue(received.get());
        }
    }
}
