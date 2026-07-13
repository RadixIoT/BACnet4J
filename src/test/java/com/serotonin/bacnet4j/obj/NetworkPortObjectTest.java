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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class NetworkPortObjectTest {
    UnsignedInteger originalValue = new UnsignedInteger(0x3FFFFF);
    UnsignedInteger updatedValue = new UnsignedInteger(234);

    // On macOS, run the following:
    // sudo ifconfig lo0 alias 127.0.0.10
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
    public void propertyInitialization() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = new NetworkPortObject(localDevice, 12, "NetworkPort", false, NetworkType.virtual,
                    ProtocolLevel.bacnetApplication, Set.of());
            BACnetArray<PropertyIdentifier> props = npo.readProperty(PropertyIdentifier.propertyList);
            assertEquals(Set.of(
                    PropertyIdentifier.changesPending,
                    PropertyIdentifier.command,
                    PropertyIdentifier.protocolLevel,
                    PropertyIdentifier.referencePort,
                    PropertyIdentifier.currentHealth,
                    PropertyIdentifier.reliability,
                    PropertyIdentifier.reliabilityEvaluationInhibit,
                    PropertyIdentifier.networkType,
                    PropertyIdentifier.statusFlags,
                    PropertyIdentifier.commandValidationResult,
                    PropertyIdentifier.outOfService,
                    PropertyIdentifier.networkNumber,
                    PropertyIdentifier.networkNumberQuality
            ), new HashSet<>(props.getValues()));

            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    @Test
    public void pendingChanges() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));

            var thrown = assertThrows(BACnetServiceException.class,
                    () -> npo.writeProperty(new ValueSource(), PropertyIdentifier.changesPending, Boolean.TRUE));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.writeAccessDenied, thrown.getErrorCode());

            // Ensure the default value
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
            assertEquals(originalValue, npo.readProperty(PropertyIdentifier.referencePort));

            // Change the value and ensure pending changes.
            npo.writeProperty(new ValueSource(), PropertyIdentifier.referencePort, updatedValue);
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));
            assertTrue(npo.isChanged());
            assertEquals(Map.of(PropertyIdentifier.referencePort, updatedValue), npo.getPendingChanges());
            assertEquals(originalValue, npo.properties.get(PropertyIdentifier.referencePort));
            assertEquals(updatedValue, npo.readProperty(PropertyIdentifier.referencePort));

            // Change the value back to the original.
            npo.writeProperty(new ValueSource(), PropertyIdentifier.referencePort, originalValue);
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
            assertFalse(npo.isChanged());
            assertEquals(0, npo.getPendingChanges().size());
            assertEquals(originalValue, npo.properties.get(PropertyIdentifier.referencePort));
            assertEquals(originalValue, npo.readProperty(PropertyIdentifier.referencePort));
        });
    }

    @Test
    public void outOfService() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()) {
                @Override
                protected Reliability evaluateReliability() {
                    // Change the evaluated reliabilty to be different from the reliabilityEvaluationInhibit value.
                    return Reliability.underRange;
                }
            });

            assertEquals(Reliability.underRange, npo.readProperty(PropertyIdentifier.reliability));

            // Reliability is not writable when in service.
            var thrown = assertThrows(BACnetServiceException.class,
                    () -> npo.writeProperty(new ValueSource(), PropertyIdentifier.reliability, Reliability.noOutput));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.writeAccessDenied, thrown.getErrorCode());

            // Becomes writable when out of service.
            npo.writeProperty(new ValueSource(), PropertyIdentifier.outOfService, Boolean.TRUE);
            npo.writeProperty(new ValueSource(), PropertyIdentifier.reliability, Reliability.noOutput);
            assertEquals(Reliability.noOutput, npo.readProperty(PropertyIdentifier.reliability));

            // When back in service the value becomes dynamic again.
            npo.writeProperty(new ValueSource(), PropertyIdentifier.outOfService, Boolean.FALSE);
            assertEquals(Reliability.underRange, npo.readProperty(PropertyIdentifier.reliability));

            // Turn on reliabilityEvaluationInhibit
            npo.writeProperty(new ValueSource(), PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.TRUE);
            assertEquals(Reliability.noFaultDetected, npo.readProperty(PropertyIdentifier.reliability));

            assertFalse(npo.isChanged());
        });
    }

    @Test
    public void networkNumberQuality() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()) {
                {
                    writePropertyInternal(PropertyIdentifier.networkNumber, new Unsigned16(10));
                    writePropertyInternal(PropertyIdentifier.networkNumberQuality, NetworkNumberQuality.learned);
                }
            });

            // Ensure the default value
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
            assertEquals(new Unsigned16(10), npo.readProperty(PropertyIdentifier.networkNumber));
            assertEquals(NetworkNumberQuality.learned, npo.readProperty(PropertyIdentifier.networkNumberQuality));

            // Change to unknown
            npo.writeProperty(new ValueSource(), PropertyIdentifier.networkNumber, Unsigned16.ZERO);
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));
            assertEquals(Unsigned16.ZERO, npo.readProperty(PropertyIdentifier.networkNumber));
            assertEquals(NetworkNumberQuality.unknown, npo.readProperty(PropertyIdentifier.networkNumberQuality));

            // Change to known value different from the original
            npo.writeProperty(new ValueSource(), PropertyIdentifier.networkNumber, new Unsigned16(11));
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));
            assertEquals(new Unsigned16(11), npo.readProperty(PropertyIdentifier.networkNumber));
            assertEquals(NetworkNumberQuality.configured, npo.readProperty(PropertyIdentifier.networkNumberQuality));

            // Change back to the original
            npo.writeProperty(new ValueSource(), PropertyIdentifier.networkNumber, new Unsigned16(10));
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
            assertEquals(new Unsigned16(10), npo.readProperty(PropertyIdentifier.networkNumber));
            assertEquals(NetworkNumberQuality.learned, npo.readProperty(PropertyIdentifier.networkNumberQuality));
        });
    }

    @Test
    public void commands_cantWriteIdle() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));

            // Can't write idle
            var thrown = assertThrows(BACnetServiceException.class,
                    () -> npo.writeProperty(new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.idle));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.valueOutOfRange, thrown.getErrorCode());
        });
    }

    @Test
    public void commands_notIdle() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));

            // Can't write a command while not idle
            npo.properties.put(PropertyIdentifier.command, NetworkPortCommand.disconnect);
            var thrown = assertThrows(BACnetServiceException.class, () -> npo.writeProperty(
                    new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.discardChanges));
            assertEquals(ErrorClass.object, thrown.getErrorClass());
            assertEquals(ErrorCode.busy, thrown.getErrorCode());
            npo.properties.put(PropertyIdentifier.command, NetworkPortCommand.idle);
        });
    }

    @Test
    public void commands_outOfService() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));

            // Can't write certain commands while out of service.
            npo.writeProperty(new ValueSource(), PropertyIdentifier.outOfService, Boolean.TRUE);
            var thrown = assertThrows(BACnetServiceException.class, () -> npo.writeProperty(
                    new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.validateChanges));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.valueOutOfRange, thrown.getErrorCode());
        });
    }

    @Test
    public void commands_pendingChanges() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));

            // Can't write certain commands while changes are pending.
            npo.writeProperty(new ValueSource(), PropertyIdentifier.referencePort, updatedValue);
            var thrown = assertThrows(BACnetServiceException.class, () -> npo.writeProperty(
                    new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.disconnect));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.invalidValueInThisState, thrown.getErrorCode());
        });
    }

    @Test
    public void commands_renewDhcp() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));

            // Can't renew DHCP when the network type is not ip4/ip6
            var thrown = assertThrows(BACnetServiceException.class, () -> npo.writeProperty(
                    new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.renewDhcp));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.valueOutOfRange, thrown.getErrorCode());

            // ... but when it is the function is not supported by default.
            npo.writeProperty(new ValueSource(), PropertyIdentifier.networkType, NetworkType.ipv6);
            thrown = assertThrows(BACnetServiceException.class, () -> npo.writeProperty(
                    new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.renewDhcp));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.optionalFunctionalityNotSupported, thrown.getErrorCode());
        });
    }

    @Test
    public void commands_restartSubordinateDiscovery() throws Exception {
        withLocalDevice(localDevice -> {
            // A non-MS/TP port rejects the command as out of range.
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));
            var thrown = assertThrows(BACnetServiceException.class, () -> npo.writeProperty(
                    new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.restartSubordinateDiscovery));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.valueOutOfRange, thrown.getErrorCode());

            // An MS/TP port without subordinate proxy support rejects it as unsupported functionality.
            var mstp = localDevice.addObject(new NetworkPortObject(localDevice, 13, "NetworkPortMstp",
                    false, NetworkType.mstp, ProtocolLevel.bacnetApplication, Set.of()));
            thrown = assertThrows(BACnetServiceException.class, () -> mstp.writeProperty(
                    new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.restartSubordinateDiscovery));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.optionalFunctionalityNotSupported, thrown.getErrorCode());
        });
    }

    @Test
    public void commands_restartDeviceDiscovery() throws Exception {
        withLocalDevice(localDevice -> {
            // A port that does not support device address proxying rejects the command as unsupported
            // functionality per 12.56.14.
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));
            var thrown = assertThrows(BACnetServiceException.class, () -> npo.writeProperty(
                    new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.restartDeviceDiscovery));
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.optionalFunctionalityNotSupported, thrown.getErrorCode());
        });
    }

    @Test
    public void commands() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.referencePort, updatedValue);
            assertTrue(npo.isChanged());

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.discardChanges);

            awaitEquals(NetworkPortCommand.idle, () -> npo.readProperty(PropertyIdentifier.command));
            assertFalse(npo.isChanged());
            assertEquals(0, npo.getPendingChanges().size());
            assertEquals(originalValue, npo.properties.get(PropertyIdentifier.referencePort));
            assertEquals(originalValue, npo.readProperty(PropertyIdentifier.referencePort));
        });
    }

    void withLocalDevice(TestUtils.LocalDeviceConsumer work) throws Exception {
        var network = new IpNetworkBuilder()
                .withLocalNetworkNumber(123)
                .withLocalBindAddress("127.0.0.10")
                .withPort(12345)
                .withBroadcast("127.0.0.255", 24)
                .build();
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network))) {
            localDevice.initialize();
            work.accept(localDevice);
        }
    }
}
