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
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.SCHubConnection;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class SecureConnectNetworkPortObjectTest {
    private static final OctetString VMAC = new OctetString(new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06});
    private static final CharacterString PRIMARY_HUB = new CharacterString("wss://hub-primary.example.com:4443");
    private static final CharacterString FAILOVER_HUB = new CharacterString("wss://hub-failover.example.com:4443");
    private static final ObjectIdentifier OPERATIONAL_CERT = new ObjectIdentifier(ObjectType.file, 1);
    private static final BACnetArray<ObjectIdentifier> ISSUER_CERTS = new BACnetArray<>(
            new ObjectIdentifier(ObjectType.file, 2),
            new ObjectIdentifier(ObjectType.file, 3));
    private static final ObjectIdentifier CSR_FILE = new ObjectIdentifier(ObjectType.file, 4);

    @Test
    public void ensureProperties() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new SecureConnectNetworkPortObject(
                    localDevice, 12, "ScNetwork", VMAC, PRIMARY_HUB,
                    FAILOVER_HUB, OPERATIONAL_CERT, ISSUER_CERTS, CSR_FILE));

            // Common Network Port properties (Table 12-71)
            assertEquals(ObjectType.networkPort, npo.readProperty(PropertyIdentifier.objectType));
            assertEquals(new ObjectIdentifier(ObjectType.networkPort, 12),
                    npo.readProperty(PropertyIdentifier.objectIdentifier));
            assertEquals(new CharacterString("ScNetwork"), npo.readProperty(PropertyIdentifier.objectName));
            assertEquals(NetworkType.secureConnect, npo.readProperty(PropertyIdentifier.networkType));
            assertEquals(ProtocolLevel.bacnetApplication, npo.readProperty(PropertyIdentifier.protocolLevel));
            assertEquals(NetworkNumberQuality.unknown, npo.readProperty(PropertyIdentifier.networkNumberQuality));
            assertEquals(MaxApduLength.UP_TO_1476.getMaxLength(), npo.readProperty(PropertyIdentifier.apduLength));
            assertEquals(VMAC, npo.readProperty(PropertyIdentifier.macAddress));

            // BVLC / NPDU length per Table 6-1 for BACnet/SC
            assertEquals(new UnsignedInteger(1497), npo.readProperty(PropertyIdentifier.maxBvlcLengthAccepted));
            assertEquals(new UnsignedInteger(61327), npo.readProperty(PropertyIdentifier.maxNpduLengthAccepted));

            // SC properties from Table 12-71.8
            assertEquals(PRIMARY_HUB, npo.readProperty(PropertyIdentifier.scPrimaryHubUri));
            assertEquals(FAILOVER_HUB, npo.readProperty(PropertyIdentifier.scFailoverHubUri));
            assertEquals(new UnsignedInteger(2), npo.readProperty(PropertyIdentifier.scMinimumReconnectTime));
            assertEquals(new UnsignedInteger(30), npo.readProperty(PropertyIdentifier.scMaximumReconnectTime));
            assertEquals(new UnsignedInteger(10), npo.readProperty(PropertyIdentifier.scConnectWaitTimeout));
            assertEquals(new UnsignedInteger(10), npo.readProperty(PropertyIdentifier.scDisconnectWaitTimeout));
            assertEquals(new UnsignedInteger(300), npo.readProperty(PropertyIdentifier.scHeartbeatTimeout));

            assertEquals(SCHubConnectorState.noHubConnection,
                    npo.readProperty(PropertyIdentifier.scHubConnectorState));
            assertEquals(
                    new SCHubConnection(SCConnectionState.notConnected, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED),
                    npo.readProperty(PropertyIdentifier.scPrimaryHubConnectionStatus));
            assertEquals(
                    new SCHubConnection(SCConnectionState.notConnected, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED),
                    npo.readProperty(PropertyIdentifier.scFailoverHubConnectionStatus));

            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.scHubFunctionEnable));
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.scDirectConnectInitiateEnable));
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.scDirectConnectAcceptEnable));

            assertEquals(OPERATIONAL_CERT, npo.readProperty(PropertyIdentifier.operationalCertificateFile));
            assertEquals(ISSUER_CERTS, npo.readProperty(PropertyIdentifier.issuerCertificateFiles));
            assertEquals(CSR_FILE, npo.readProperty(PropertyIdentifier.certificateSigningRequestFile));
        });
    }

    @Test
    public void uriValidation() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new SecureConnectNetworkPortObject(
                    localDevice, 12, "ScNetwork", VMAC, PRIMARY_HUB,
                    FAILOVER_HUB, OPERATIONAL_CERT, ISSUER_CERTS, CSR_FILE));

            // Can't set a bad uri
            var thrown = assertThrows(BACnetServiceException.class, () -> {
                npo.writeProperty(new ValueSource(), PropertyIdentifier.scPrimaryHubUri,
                        new CharacterString("bad$://test"));
            });
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.valueOutOfRange, thrown.getErrorCode());

            npo.writeProperty(new ValueSource(), PropertyIdentifier.scPrimaryHubUri, new CharacterString("wss://test"));
            assertEquals(new CharacterString("wss://test"), npo.readProperty(PropertyIdentifier.scPrimaryHubUri));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.scPrimaryHubUri, new CharacterString("test"));
            assertEquals(new CharacterString("test"), npo.readProperty(PropertyIdentifier.scPrimaryHubUri));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.scPrimaryHubUri, new CharacterString(""));
            assertEquals(new CharacterString(""), npo.readProperty(PropertyIdentifier.scPrimaryHubUri));
        });
    }

    @Test
    public void rangeValidation() throws Exception {
        withLocalDevice(localDevice -> {
            var npo = localDevice.addObject(new SecureConnectNetworkPortObject(
                    localDevice, 12, "ScNetwork", VMAC, PRIMARY_HUB,
                    FAILOVER_HUB, OPERATIONAL_CERT, ISSUER_CERTS, CSR_FILE));

            var thrown = assertThrows(BACnetServiceException.class, () -> {
                npo.writeProperty(new ValueSource(), PropertyIdentifier.scMinimumReconnectTime, UnsignedInteger.ZERO);
            });
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.valueOutOfRange, thrown.getErrorCode());

            thrown = assertThrows(BACnetServiceException.class, () -> {
                npo.writeProperty(new ValueSource(), PropertyIdentifier.scMinimumReconnectTime,
                        new UnsignedInteger(1000));
            });
            assertEquals(ErrorClass.property, thrown.getErrorClass());
            assertEquals(ErrorCode.valueOutOfRange, thrown.getErrorCode());

            npo.writeProperty(new ValueSource(), PropertyIdentifier.scMinimumReconnectTime, new UnsignedInteger(100));
            assertEquals(new UnsignedInteger(100), npo.readProperty(PropertyIdentifier.scMinimumReconnectTime));
        });
    }

    void withLocalDevice(TestUtils.LocalDeviceConsumer work) throws Exception {
        var network = new TestNetwork(new TestNetworkMap(), 1, 0);
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network))) {
            localDevice.initialize();
            work.accept(localDevice);
        }
    }
}
