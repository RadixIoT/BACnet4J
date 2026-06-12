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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class NetworkPortObjectTest {
    UnsignedInteger originalValue = new UnsignedInteger(0x3FFFFF);
    UnsignedInteger updatedValue = new UnsignedInteger(234);

    @Test
    public void propertyInitialization() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = new NetworkPortObject(localDevice, 12, "NetworkPort", false, NetworkType.virtual,
                    ProtocolLevel.bacnetApplication);
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
                    PropertyIdentifier.outOfService
            ), new HashSet<>(props.getValues()));

            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    @Test
    public void pendingChanges() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication));

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
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication) {
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
    public void commands_cantWriteIdle() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication));

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
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication));

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
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication));

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
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication));

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
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication));

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
    public void commands() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new NetworkPortObject(localDevice, 12, "NetworkPort",
                    false, NetworkType.virtual, ProtocolLevel.bacnetApplication));

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
