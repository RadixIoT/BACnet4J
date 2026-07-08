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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.docker.DockerRemoteDevice;
import com.serotonin.bacnet4j.event.DefaultDeviceEventListener;
import com.serotonin.bacnet4j.event.DeviceEventListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.util.sero.IpAddressUtils;

/**
 * These tests assume the existence of the remote devices created by the docker compose, and so only runs if being
 * executed in a docker environment. See {@link DockerRemoteDevice}, which sends both broadcast and unicast
 * messages every second.
 */
public class IpNetworkTest extends AbstractTest {
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

    @Test
    public void getLocalAddressWithWildcardBindAddress() throws Exception {
        var port = 48706;
        IpNetwork network = spy(new IpNetworkBuilder()
                .withBroadcast("1.2.3.255", 24)
                .build());
        doReturn(List.of(
                new Address(IpNetworkUtils.toOctetString("0.0.0.0", port)),
                new Address(IpNetworkUtils.toOctetString("2.3.4.5", port)),
                new Address(IpNetworkUtils.toOctetString("3.4.4.5", port))
        )).when(network).getLocalAddressList();

        doNothing().when(network).listen(any());
        doReturn(null).when(network).createSocket(any());

        LocalDevice localDevice = mock(LocalDevice.class);
        Transport transport = mock(Transport.class);
        doReturn(localDevice).when(transport).getLocalDevice();
        network.initialize(transport);

        InetSocketAddress addr = network.getLocalAddress();
        assertArrayEquals(IpAddressUtils.toIpAddress("2.3.4.5"), addr.getAddress().getAddress());
        assertEquals(port, addr.getPort());

    }

    @Test
    public void getLocalAddressWithASpecificBindAddress() throws Exception {
        var port = 48706;
        IpNetwork network = spy(new IpNetworkBuilder()
                .withLocalBindAddress("1.2.3.4")
                .withPort(port)
                .withBroadcast("1.2.3.255", 24)
                .build());
        doReturn(List.of(
                new Address(IpNetworkUtils.toOctetString("0.0.0.0", port)),
                new Address(IpNetworkUtils.toOctetString("2.3.4.5", port)),
                new Address(IpNetworkUtils.toOctetString("3.4.4.5", port))
        )).when(network).getLocalAddressList();

        doNothing().when(network).listen(any());
        doReturn(null).when(network).createSocket(any());

        LocalDevice localDevice = mock(LocalDevice.class);
        Transport transport = mock(Transport.class);
        doReturn(localDevice).when(transport).getLocalDevice();
        network.initialize(transport);

        InetSocketAddress addr = network.getLocalAddress();
        assertArrayEquals(IpAddressUtils.toIpAddress("1.2.3.4"), addr.getAddress().getAddress());
        assertEquals(port, addr.getPort());
    }

    @Test
    public void registerAsForeignDevice() throws Exception {
        IpNetwork network = spy(new IpNetworkBuilder().withBroadcast("1.2.3.255", 24).build());

        var ttl = Duration.ofHours(12);
        var renewal = Duration.ofHours(6);

        var totalSuccesses = new AtomicInteger(0);
        var totalFailures = new AtomicInteger(0);
        var retryDelayProvider = new IpNetwork.ForeignDeviceRegistrationRetryDelayPolicy() {
            int failures = 0;

            @Override
            public void registrationSucceeded() {
                totalSuccesses.incrementAndGet();
                failures = 0;
            }

            @Override
            public Duration registrationFailed(BACnetException e) {
                totalFailures.incrementAndGet();
                failures++;

                if (failures <= 1) {
                    return Duration.ofSeconds(10);
                }
                return Duration.ofSeconds(30);
            }

            @Override
            public Duration renewalMargin(Duration timeToLive) {
                // Attempt re-registration after half of the lease time has expired.
                return Duration.ofSeconds(timeToLive.getSeconds() / 2);
            }
        };

        assertEquals(Duration.ofSeconds(60 * 60 * 6), Duration.ofHours(6));

        var future = mock(ScheduledFuture.class);
        doReturn(false).when(future).isCancelled();

        var transport = mock(Transport.class);
        var localDevice = mock(LocalDevice.class);
        doReturn(localDevice).when(transport).getLocalDevice();
        var task = new AtomicReference<Runnable>();
        when(localDevice.schedule(any(), anyLong(), any())).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            task.set(r);
            return future;
        });

        network.initialize(transport);

        doThrow(new BACnetException()) // Fail the initial registration...
                .doNothing() // ... but succeed the second try.
                .doThrow(new BACnetException()) // Fail once...
                .doThrow(new BACnetException()) // ... and twice on re-registration
                .doNothing() // ... and then succeed.
                .when(network).sendForeignDeviceRegistration();

        network.registerAsForeignDevice(InetSocketAddress.createUnresolved("1.2.3.4", IpNetwork.DEFAULT_PORT),
                ttl, retryDelayProvider);
        assertEquals(0, totalSuccesses.get());
        assertEquals(0, totalFailures.get());
        assertFalse(network.isFdRegistered());
        verify(localDevice, times(1)).schedule(any(), eq(0L), eq(TimeUnit.SECONDS));
        clearInvocations(localDevice);

        task.get().run();
        assertEquals(0, totalSuccesses.get());
        assertEquals(1, totalFailures.get());
        assertFalse(network.isFdRegistered());
        verify(localDevice, times(1)).schedule(any(), eq(10L), eq(TimeUnit.SECONDS));
        clearInvocations(localDevice);

        task.get().run();
        assertEquals(1, totalSuccesses.get());
        assertEquals(1, totalFailures.get());
        assertTrue(network.isFdRegistered());
        verify(localDevice, times(1)).schedule(any(), eq(renewal.getSeconds()), eq(TimeUnit.SECONDS));
        clearInvocations(localDevice);

        task.get().run();
        assertEquals(1, totalSuccesses.get());
        assertEquals(2, totalFailures.get());
        assertFalse(network.isFdRegistered());
        verify(localDevice, times(1)).schedule(any(), eq(10L), eq(TimeUnit.SECONDS));
        clearInvocations(localDevice);

        task.get().run();
        assertEquals(1, totalSuccesses.get());
        assertEquals(3, totalFailures.get());
        assertFalse(network.isFdRegistered());
        verify(localDevice, times(1)).schedule(any(), eq(30L), eq(TimeUnit.SECONDS));
        clearInvocations(localDevice);

        task.get().run();
        assertEquals(2, totalSuccesses.get());
        assertEquals(3, totalFailures.get());
        assertTrue(network.isFdRegistered());
        verify(localDevice, times(1)).schedule(any(), eq(renewal.getSeconds()), eq(TimeUnit.SECONDS));
        clearInvocations(localDevice);

        network.unregisterAsForeignDevice();

        task.get().run();
        assertEquals(2, totalSuccesses.get());
        assertEquals(3, totalFailures.get());
        assertFalse(network.isFdRegistered());
        verify(localDevice, never()).schedule(any(), anyLong(), any());
        verify(network, times(5)).sendForeignDeviceRegistration();

        network.terminate();
    }
}
