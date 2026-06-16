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

import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkUtils;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.BDTEntry;
import com.serotonin.bacnet4j.type.constructed.FDTEntry;
import com.serotonin.bacnet4j.type.constructed.HostAddress;
import com.serotonin.bacnet4j.type.constructed.HostNPort;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.IPMode;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

import lohbihler.warp.WarpClock;

public class IpNetworkPortObjectTest {
    private final WarpClock clock = new WarpClock();

    // On macOS, run the following:
    // sudo ifconfig lo0 alias 127.0.0.10
    // sudo ifconfig lo0 alias 127.0.0.11
    // sudo ifconfig lo0 alias 127.0.0.255

    @Before
    public void before() throws Exception {
        boolean canRun = true;
        if (!SystemUtils.IS_OS_LINUX) {
            try {
                InetSocketAddress addr = new InetSocketAddress("127.0.1.1", 47808);
                try (DatagramSocket ignored = new DatagramSocket(addr)) {
                    // no op
                }
            } catch (SocketException e) {
                canRun = false;
            }
        }
        Assume.assumeTrue(canRun);
    }

    @Test
    public void ensureProperties() throws Exception {
        var network = new IpNetworkBuilder()
                .withLocalNetworkNumber(123)
                .withLocalBindAddress("127.0.0.10")
                .withPort(12345)
                .withBroadcast("127.0.0.255", 24)
                .build();
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network)).withClock(clock)) {
            localDevice.initialize();
            var npo = localDevice.addObject(new IpNetworkPortObject(network, 12, "IpNetwork", false));

            assertEquals(ObjectType.networkPort, npo.readProperty(PropertyIdentifier.objectType));
            assertEquals(new ObjectIdentifier(ObjectType.networkPort, 12),
                    npo.readProperty(PropertyIdentifier.objectIdentifier));
            assertEquals(new CharacterString("IpNetwork"), npo.readProperty(PropertyIdentifier.objectName));
            assertEquals(new UnsignedInteger(123), npo.readProperty(PropertyIdentifier.networkNumber));
            assertEquals(NetworkNumberQuality.unknown, npo.readProperty(PropertyIdentifier.networkNumberQuality));
            assertEquals(MaxApduLength.UP_TO_1476.getMaxLength(), npo.readProperty(PropertyIdentifier.apduLength));
            assertEquals(IpNetworkUtils.toOctetString("127.0.0.10", 12345),
                    npo.readProperty(PropertyIdentifier.macAddress));
            assertEquals(IPMode.normal, npo.readProperty(PropertyIdentifier.bacnetIpMode));
            assertEquals(new Unsigned16(12345), npo.readProperty(PropertyIdentifier.bacnetIpUdpPort));
            assertEquals(new OctetString(new byte[] {127, 0, 0, 10}), npo.readProperty(PropertyIdentifier.ipAddress));
            assertEquals(new OctetString(new byte[] {(byte) 255, (byte) 255, (byte) 255, 0}),
                    npo.readProperty(PropertyIdentifier.ipSubnetMask));

            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
        }
    }

    @Test
    public void bbmdProperties() throws Exception {
        var network = new IpNetworkBuilder()
                .withLocalNetworkNumber(123)
                .withLocalBindAddress("127.0.0.10")
                .withPort(12345)
                .withBroadcast("127.0.0.255", 24)
                .build();
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network)).withClock(clock)) {
            network.enableBBMD();

            localDevice.initialize();
            var npo = localDevice.addObject(new IpNetworkPortObject(network, 12, "IpNetwork", false));

            network.writeBDT(List.of(
                    new IpNetwork.BDTEntry("1.2.4.4", 47800, "255.255.255.255"),
                    new IpNetwork.BDTEntry("1.2.5.4", 47801, "255.255.255.250")
            ));
            network.writeFDT(List.of(
                    network.new FDTEntry("2.3.4.5", 47802, 600),
                    network.new FDTEntry("3.4.5.6", 47803, 700)
            ));

            assertEquals(
                    new BACnetArray<>(
                            new BDTEntry(
                                    new HostNPort(new HostAddress(new OctetString(new byte[] {1, 2, 4, 4})),
                                            new Unsigned16(47800)),
                                    new OctetString(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255})
                            ),
                            new BDTEntry(
                                    new HostNPort(new HostAddress(new OctetString(new byte[] {1, 2, 5, 4})),
                                            new Unsigned16(47801)),
                                    new OctetString(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 250})
                            )
                    ),
                    npo.readProperty(PropertyIdentifier.bbmdBroadcastDistributionTable));
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.bbmdAcceptFdRegistrations));
            assertEquals(
                    new BACnetArray<>(
                            new FDTEntry(
                                    new OctetString(new byte[] {2, 3, 4, 5, (byte) 0xba, (byte) 0xba}),
                                    new Unsigned16(600),
                                    new Unsigned16(630)
                            ),
                            new FDTEntry(
                                    new OctetString(new byte[] {3, 4, 5, 6, (byte) 0xba, (byte) 0xbb}),
                                    new Unsigned16(700),
                                    new Unsigned16(730)
                            )
                    ),
                    npo.readProperty(PropertyIdentifier.bbmdForeignDeviceTable));

            clock.plusMinutes(2);

            assertEquals(
                    new BACnetArray<>(
                            new FDTEntry(
                                    new OctetString(new byte[] {2, 3, 4, 5, (byte) 0xba, (byte) 0xba}),
                                    new Unsigned16(600),
                                    new Unsigned16(510)
                            ),
                            new FDTEntry(
                                    new OctetString(new byte[] {3, 4, 5, 6, (byte) 0xba, (byte) 0xbb}),
                                    new Unsigned16(700),
                                    new Unsigned16(610)
                            )
                    ),
                    npo.readProperty(PropertyIdentifier.bbmdForeignDeviceTable));
        }
    }

    @Test
    public void foreignDeviceProperties() throws Exception {
        var originalValue = new Unsigned16(600);
        var updatedValue = new Unsigned16(200);

        var network = new IpNetworkBuilder()
                .withLocalNetworkNumber(123)
                .withLocalBindAddress("127.0.0.10")
                .withPort(12345)
                .withBroadcast("127.0.0.255", 24)
                .build();
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network)).withClock(clock)) {
            localDevice.initialize();
            var npo = localDevice.addObject(new IpNetworkPortObject(network, 12, "IpNetwork", false));

            // Ensure the properties start as unset.
            assertNull(npo.readProperty(PropertyIdentifier.fdBbmdAddress));
            assertNull(npo.readProperty(PropertyIdentifier.fdSubscriptionLifetime));

            // Tell the network to act as a foreign device.
            CompletableFuture<Void> future = new CompletableFuture<>();
            network.registerAsForeignDevice(
                    InetSocketAddress.createUnresolved("127.0.0.11", 12345),
                    Duration.ofMinutes(10),
                    new IpNetwork.ForeignDeviceRegistrationRetryDelayPolicy() {
                        // Registration will fail because no BACnet device is listening at the address.
                        @Override
                        public Duration registrationFailed(BACnetException e) {
                            future.complete(null);
                            return Duration.ZERO;
                        }
                    });
            future.get(10, TimeUnit.SECONDS);

            // Ensure the properties are set.
            assertEquals(new HostNPort(new HostAddress(new OctetString(new byte[] {0x7f, 0, 0, 0xb})),
                    new Unsigned16(12345)), npo.readProperty(PropertyIdentifier.fdBbmdAddress));
            assertEquals(originalValue, npo.readProperty(PropertyIdentifier.fdSubscriptionLifetime));

            // Change a value and ensure pending changes.
            npo.writeProperty(new ValueSource(), PropertyIdentifier.fdSubscriptionLifetime, updatedValue);
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));
            assertTrue(npo.isChanged());
            assertEquals(Map.of(PropertyIdentifier.fdSubscriptionLifetime, updatedValue), npo.getPendingChanges());
            assertEquals(originalValue, npo.properties.get(PropertyIdentifier.fdSubscriptionLifetime));
            assertEquals(updatedValue, npo.readProperty(PropertyIdentifier.fdSubscriptionLifetime));

            // Change the value back to the original.
            npo.writeProperty(new ValueSource(), PropertyIdentifier.fdSubscriptionLifetime, originalValue);
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
            assertFalse(npo.isChanged());
            assertEquals(0, npo.getPendingChanges().size());
            assertEquals(originalValue, npo.properties.get(PropertyIdentifier.fdSubscriptionLifetime));
            assertEquals(originalValue, npo.readProperty(PropertyIdentifier.fdSubscriptionLifetime));
        }
    }

    @Test
    public void renewFdRegistration() throws Exception {
        var network = spy(new IpNetworkBuilder()
                .withLocalNetworkNumber(123)
                .withLocalBindAddress("127.0.0.10")
                .withPort(12345)
                .withBroadcast("127.0.0.255", 24)
                .build());
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network)).withClock(clock)) {
            localDevice.initialize();
            var npo = localDevice.addObject(new IpNetworkPortObject(network, 12, "IpNetwork", false));

            // Tell the network to act as a foreign device.
            CompletableFuture<Void> future = new CompletableFuture<>();
            network.registerAsForeignDevice(
                    InetSocketAddress.createUnresolved("127.0.0.11", 12345),
                    Duration.ofMinutes(10),
                    new IpNetwork.ForeignDeviceRegistrationRetryDelayPolicy() {
                        // Registration will fail because no BACnet device is listening at the address.
                        @Override
                        public Duration registrationFailed(BACnetException e) {
                            future.complete(null);
                            return Duration.ZERO;
                        }
                    });
            future.get(10, TimeUnit.SECONDS);

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.renewFdRegistration);
            awaitEquals(NetworkPortCommand.idle, () -> npo.readProperty(PropertyIdentifier.command));
            verify(network).tryFdRegister();
            assertEquals(NetworkPortCommand.idle, npo.readProperty(PropertyIdentifier.command));
        }
    }
}
