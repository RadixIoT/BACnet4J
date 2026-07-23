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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Set;
import java.time.Instant;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.sc.InMemoryKeyPairHandler;
import com.serotonin.bacnet4j.npdu.sc.SCNetwork;
import com.serotonin.bacnet4j.npdu.sc.SCNetworkBuilder;
import com.serotonin.bacnet4j.obj.fileAccess.FileStreamAccess;
import com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Health;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SCHubConnection;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.constructed.WriteAccessSpecification;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class SecureConnectNetworkPortObjectTest {
    private static final String UUID = "46663baa-98cc-4cf7-ad19-503f4705b130";
    private static final OctetString VMAC = OctetString.fromHex("010203040506");
    private static final String PRIMARY_HUB = "wss://hub-primary.example.com:4443";
    private static final String FAILOVER_HUB = "wss://hub-failover.example.com:4443";
    private static final int OPERATIONAL_CERT = 1;
    private static final int ISSUER_CERT_1 = 2;
    private static final int ISSUER_CERT_2 = 3;
    private static final int CSR_FILE = 4;
    private static final int ANALOG_VALUE_INSTANCE = 22;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    SecureConnectNetworkPortObject npo;

    /**
     * Register BouncyCastle as a JCA provider so it can supply a KeyFactory for the EC OID
     * (1.2.840.10045.2.1) during cert generation and conversion. Some JVMs do not register
     * an EC KeyFactory under that OID name in the default provider set.
     */
    @BeforeClass
    public static void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    public void ensureProperties() throws Exception {
        withLocalDevice(localDevice -> {
            // Common Network Port properties (Table 12-71)
            assertEquals(ObjectType.networkPort, npo.readProperty(PropertyIdentifier.objectType));
            assertEquals(new ObjectIdentifier(ObjectType.networkPort, 12),
                    npo.readProperty(PropertyIdentifier.objectIdentifier));
            assertEquals(new CharacterString("46663baa98cc4cf7ad19503f4705b130"),
                    npo.readProperty(PropertyIdentifier.objectName));
            assertEquals(NetworkType.secureConnect, npo.readProperty(PropertyIdentifier.networkType));
            assertEquals(ProtocolLevel.bacnetApplication, npo.readProperty(PropertyIdentifier.protocolLevel));
            assertEquals(NetworkNumberQuality.unknown, npo.readProperty(PropertyIdentifier.networkNumberQuality));
            assertEquals(new UnsignedInteger(61327), npo.readProperty(PropertyIdentifier.apduLength));
            assertEquals(VMAC, npo.readProperty(PropertyIdentifier.macAddress));

            // BVLC / NPDU length per Table 6-1 for BACnet/SC
            assertEquals(new UnsignedInteger(1600), npo.readProperty(PropertyIdentifier.maxBvlcLengthAccepted));
            assertEquals(new UnsignedInteger(1497), npo.readProperty(PropertyIdentifier.maxNpduLengthAccepted));

            // SC properties from Table 12-71.8
            assertEquals(new CharacterString(PRIMARY_HUB), npo.readProperty(PropertyIdentifier.scPrimaryHubUri));
            assertEquals(new CharacterString(FAILOVER_HUB), npo.readProperty(PropertyIdentifier.scFailoverHubUri));
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

            assertEquals(new ObjectIdentifier(ObjectType.file, OPERATIONAL_CERT),
                    npo.readProperty(PropertyIdentifier.operationalCertificateFile));
            assertEquals(new BACnetArray<>(
                    new ObjectIdentifier(ObjectType.file, ISSUER_CERT_1),
                    new ObjectIdentifier(ObjectType.file, ISSUER_CERT_2)
            ), npo.readProperty(PropertyIdentifier.issuerCertificateFiles));
            assertEquals(new ObjectIdentifier(ObjectType.file, CSR_FILE),
                    npo.readProperty(PropertyIdentifier.certificateSigningRequestFile));
        });
    }

    @Test
    public void uriValidation() throws Exception {
        withLocalDevice(localDevice -> {
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

    /**
     * Resolves cert file paths in the temp folder, creating an empty file on first use so
     * FileObject's existence check passes. Safe to call across multiple LocalDevice sessions:
     * the file (with whatever content the test wrote to it) persists between sessions.
     */
    File certFile(String name) {
        var file = new File(tempFolder.getRoot(), name);
        if (!file.exists()) {
            try {
                Files.write(file.toPath(), new byte[0]);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }

    InMemoryKeyPairHandler keyPairHandler;

    void withLocalDevice(TestUtils.LocalDeviceConsumer work) throws Exception {
        withLocalDevice(mock(KeyPair.class), work);
    }

    void withLocalDevice(KeyPair keyPair, TestUtils.LocalDeviceConsumer work) throws Exception {
        keyPairHandler = new InMemoryKeyPairHandler(keyPair);
        var network = new SCNetworkBuilder()
                .vmac(VMAC)
                .uuid(UUID)
                .localNetworkNumber(123)
                .primaryHubUri(PRIMARY_HUB)
                .failoverHubUri(FAILOVER_HUB)
                .keyPairHandler(keyPairHandler)
                .operationalCertificateFileId(OPERATIONAL_CERT)
                .issuerCertificateFile1Id(ISSUER_CERT_1)
                .issuerCertificateFile2Id(ISSUER_CERT_2)
                .certificateSigningRequestFileId(CSR_FILE)
                .build();
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network))) {
            localDevice.addObject(new FileObject(localDevice, OPERATIONAL_CERT, "pem",
                    new FileStreamAccess(certFile("operational.pem"))));
            localDevice.addObject(new FileObject(localDevice, ISSUER_CERT_1, "pem",
                    new FileStreamAccess(certFile("issuer1.pem"))));
            localDevice.addObject(new FileObject(localDevice, ISSUER_CERT_2, "pem",
                    new FileStreamAccess(certFile("issuer2.pem"))));
            localDevice.addObject(new AnalogValueObject(localDevice, ANALOG_VALUE_INSTANCE, "av", 22.2F,
                    EngineeringUnits.noUnits, true));
            npo = localDevice.addObject(new SecureConnectNetworkPortObject(localDevice,
                    (SCNetwork) localDevice.getNetwork(), keyPairHandler, 12));
            localDevice.initialize();
            work.accept(localDevice);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Cert change tracking helpers
    // ---------------------------------------------------------------------------------------

    private static final ObjectIdentifier OP_CERT_ID = new ObjectIdentifier(ObjectType.file, OPERATIONAL_CERT);
    private static final ObjectIdentifier ISSUER_1_ID = new ObjectIdentifier(ObjectType.file, ISSUER_CERT_1);
    private static final ObjectIdentifier ISSUER_2_ID = new ObjectIdentifier(ObjectType.file, ISSUER_CERT_2);

    /** Path where the cert file lives on disk for a given file id. */
    private Path pathFor(ObjectIdentifier fileId) {
        if (fileId.equals(OP_CERT_ID))
            return certFile("operational.pem").toPath();
        if (fileId.equals(ISSUER_1_ID))
            return certFile("issuer1.pem").toPath();
        if (fileId.equals(ISSUER_2_ID))
            return certFile("issuer2.pem").toPath();
        throw new IllegalArgumentException("Unknown cert file id: " + fileId);
    }

    /** Backup path that the port object will write when tracking a pending change. */
    private Path backupPathFor(ObjectIdentifier fileId) {
        Path p = pathFor(fileId);
        return p.resolveSibling(p.getFileName() + SecureConnectNetworkPortObject.BACKUP_EXTENSION);
    }

    /**
     * Simulates the code at DefaultTransport#handleConfirmedRequest.
     */
    private void handleConfirmedRequest(LocalDevice localDevice, ConfirmedRequestService request)
            throws BACnetException {
        localDevice.getEventHandler().requestReceived(mock(Address.class), request);
        request.handle(localDevice, null);
    }

    /**
     * Simulates an AtomicWriteFileRequest arriving over the network: fires the port object's
     * event listener (which is what watches for cert changes and creates the backup) then
     * dispatches the request to actually write the file. Order matches production
     * (DefaultTransport calls requestReceived before handling).
     */
    private void atomicWriteFile(LocalDevice localDevice, ObjectIdentifier fileId, byte[] data) throws Exception {
        handleConfirmedRequest(localDevice, new AtomicWriteFileRequest(fileId,
                new AtomicWriteFileRequest.StreamAccess(new SignedInteger(0), new OctetString(data))));
    }

    /** Reads the current on-disk cert file content. */
    private byte[] readCertFile(ObjectIdentifier fileId) throws Exception {
        return Files.readAllBytes(pathFor(fileId));
    }

    /** Reads the backup file content, or throws if it does not exist. */
    private byte[] readBackupFile(ObjectIdentifier fileId) throws Exception {
        return Files.readAllBytes(backupPathFor(fileId));
    }

    /**
     * Returns whether the given certificate file has a pending (unactivated) content change.
     */
    private boolean certPending(ObjectIdentifier fileId) {
        return npo.getChangedCertificateFiles().contains(fileId);
    }

    // ---------------------------------------------------------------------------------------
    // Change detection tests
    // ---------------------------------------------------------------------------------------

    /**
     * Writing new bytes to the operational cert file creates a backup of the original content,
     * flips changesPending to TRUE, and adds a marker in the pending-changes map at
     * operationalCertificateFile → array[0]=TRUE.
     */
    @Test
    public void opCertWrite_createsBackupAndSetsPending() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "old-op-cert".getBytes());

            atomicWriteFile(localDevice, OP_CERT_ID, "new-op-cert".getBytes());

            // File on disk was updated by the AtomicWriteFileRequest handler.
            assertArrayEquals("new-op-cert".getBytes(), readCertFile(OP_CERT_ID));
            // Backup file preserves the original content.
            assertArrayEquals("old-op-cert".getBytes(), readBackupFile(OP_CERT_ID));
            // Pending change is registered on the operational cert property.
            assertTrue(certPending(OP_CERT_ID));
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    /**
     * While a certificate file change is pending, reads of the certificate file reference properties must
     * return the real ObjectIdentifier values, and getPendingChanges() must not contain marker entries.
     * Regression test: the markers were previously stored in the base class's pending-changes map under
     * the real property ids, so get() substituted a BACnetArray of Booleans for the OID on the wire, and
     * product persistence code iterating getPendingChanges() would have persisted the markers as values.
     */
    @Test
    public void certFilePropertiesReadCorrectlyWhileChangePending() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "old-op-cert".getBytes());
            Files.write(pathFor(ISSUER_1_ID), "old-issuer-1".getBytes());

            atomicWriteFile(localDevice, OP_CERT_ID, "new-op-cert".getBytes());
            atomicWriteFile(localDevice, ISSUER_1_ID, "new-issuer-1".getBytes());
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));

            // The reference properties still read as the actual object identifiers.
            assertEquals(OP_CERT_ID, npo.readProperty(PropertyIdentifier.operationalCertificateFile));
            assertEquals(new BACnetArray<>(ISSUER_1_ID, ISSUER_2_ID),
                    npo.readProperty(PropertyIdentifier.issuerCertificateFiles));
            assertEquals(OP_CERT_ID, npo.get(PropertyIdentifier.operationalCertificateFile));

            // The markers are not exposed through the pending property values.
            assertTrue(npo.getPendingChanges().isEmpty());
            assertEquals(Set.of(OP_CERT_ID, ISSUER_1_ID), npo.getChangedCertificateFiles());
        });
    }

    /**
     * Changing the size of the operational cert file creates a backup of the original content,
     * flips changesPending to TRUE, and adds a marker in the pending-changes map at
     * operationalCertificateFile → array[0]=TRUE.
     */
    @Test
    public void opCertWriteProp_createsBackupAndSetsPending() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "old-op-cert".getBytes());

            handleConfirmedRequest(localDevice, new WritePropertyRequest(OP_CERT_ID, PropertyIdentifier.fileSize,
                    null, UnsignedInteger.ZERO, null));

            // File on disk was updated by the WriteProperty handler.
            assertArrayEquals("".getBytes(), readCertFile(OP_CERT_ID));
            // Backup file preserves the original content.
            assertArrayEquals("old-op-cert".getBytes(), readBackupFile(OP_CERT_ID));
            // Pending change is registered on the operational cert property.
            assertTrue(certPending(OP_CERT_ID));
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    /**
     * Changing the size of the operational cert file creates a backup of the original content,
     * flips changesPending to TRUE, and adds a marker in the pending-changes map at
     * operationalCertificateFile → array[0]=TRUE.
     */
    @Test
    public void opCertWritePropMult_createsBackupAndSetsPending() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), "og-cert-1-data".getBytes());
            Files.write(pathFor(ISSUER_2_ID), "og-cert-2-data".getBytes());

            handleConfirmedRequest(localDevice, new WritePropertyMultipleRequest(new SequenceOf<>(
                    // Truncate the first issuer cert to 0 bytes.
                    new WriteAccessSpecification(ISSUER_1_ID, new SequenceOf<>(
                            new PropertyValue(PropertyIdentifier.fileSize, UnsignedInteger.ZERO)
                    )),
                    // Truncate the second issuer cert to 11 bytes.
                    new WriteAccessSpecification(ISSUER_2_ID, new SequenceOf<>(
                            new PropertyValue(PropertyIdentifier.fileSize, new UnsignedInteger(11))
                    )),
                    // Extend the first cert to 2 bytes. Done in the same request.
                    new WriteAccessSpecification(ISSUER_1_ID, new SequenceOf<>(
                            new PropertyValue(PropertyIdentifier.fileSize, new UnsignedInteger(2))
                    )),
                    // Show that writes to other objects still are ok to write to at the same time.
                    new WriteAccessSpecification(new ObjectIdentifier(ObjectType.analogValue, ANALOG_VALUE_INSTANCE),
                            new SequenceOf<>(
                                    new PropertyValue(PropertyIdentifier.presentValue, new Real(33.3F))
                            )
                    )
            )));

            // Files on disk were updated by the WritePropertyMultiple handler.
            assertArrayEquals(StreamUtils.fromHex("0000"), readCertFile(ISSUER_1_ID));
            assertArrayEquals("og-cert-2-d".getBytes(), readCertFile(ISSUER_2_ID));
            // Backup files preserve the original content.
            assertArrayEquals("og-cert-1-data".getBytes(), readBackupFile(ISSUER_1_ID));
            assertArrayEquals("og-cert-2-data".getBytes(), readBackupFile(ISSUER_2_ID));
            // Pending change is registered on the operational cert property.
            assertFalse(certPending(OP_CERT_ID));
            assertTrue(certPending(ISSUER_1_ID));
            assertTrue(certPending(ISSUER_2_ID));
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));
            assertEquals(new Real(33.3F),
                    localDevice.getObject(new ObjectIdentifier(ObjectType.analogValue, ANALOG_VALUE_INSTANCE))
                            .readProperty(PropertyIdentifier.presentValue));
        });
    }

    /**
     * Writing new bytes to issuer cert slot 0 sets array[0]=TRUE on the issuerCertificateFiles
     * pending change, leaves slot 1 alone, and backs up only slot 0.
     */
    @Test
    public void issuerCert1Write_createsBackupAndSetsPendingIndex0() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), "old-issuer-1".getBytes());
            Files.write(pathFor(ISSUER_2_ID), "issuer-2".getBytes());

            atomicWriteFile(localDevice, ISSUER_1_ID, "new-issuer-1".getBytes());

            assertArrayEquals("new-issuer-1".getBytes(), readCertFile(ISSUER_1_ID));
            assertArrayEquals("old-issuer-1".getBytes(), readBackupFile(ISSUER_1_ID));

            assertTrue(certPending(ISSUER_1_ID));
            assertFalse(certPending(ISSUER_2_ID));

            // Issuer 2's backup file should not exist since it was not touched.
            assertFalse(Files.exists(backupPathFor(ISSUER_2_ID)));
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    /**
     * Writing new bytes to issuer cert slot 1 sets array[1]=TRUE while leaving slot 0
     * unchanged. Confirms the array-indexed tracking works for both positions.
     */
    @Test
    public void issuerCert2Write_createsBackupAndSetsPendingIndex1() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), "issuer-1".getBytes());
            Files.write(pathFor(ISSUER_2_ID), "old-issuer-2".getBytes());

            atomicWriteFile(localDevice, ISSUER_2_ID, "new-issuer-2".getBytes());

            assertArrayEquals("new-issuer-2".getBytes(), readCertFile(ISSUER_2_ID));
            assertArrayEquals("old-issuer-2".getBytes(), readBackupFile(ISSUER_2_ID));

            assertFalse(certPending(ISSUER_1_ID));
            assertTrue(certPending(ISSUER_2_ID));
        });
    }

    /**
     * Writing both issuer cert slots produces one pending-changes entry with both array flags
     * set to TRUE, and two backup files.
     */
    @Test
    public void bothIssuerCertsWrite_bothFlagsSet() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), "old-1".getBytes());
            Files.write(pathFor(ISSUER_2_ID), "old-2".getBytes());

            atomicWriteFile(localDevice, ISSUER_1_ID, "new-1".getBytes());
            atomicWriteFile(localDevice, ISSUER_2_ID, "new-2".getBytes());

            assertTrue(certPending(ISSUER_1_ID));
            assertTrue(certPending(ISSUER_2_ID));
            assertArrayEquals("old-1".getBytes(), readBackupFile(ISSUER_1_ID));
            assertArrayEquals("old-2".getBytes(), readBackupFile(ISSUER_2_ID));
        });
    }

    /**
     * Writing the same content that is already on disk does NOT create a backup or flag a
     * pending change. This avoids false positives when a client rewrites unchanged data.
     */
    @Test
    public void opCertWriteSameContent_noPendingChange() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "same-content".getBytes());

            atomicWriteFile(localDevice, OP_CERT_ID, "same-content".getBytes());

            assertFalse(Files.exists(backupPathFor(OP_CERT_ID)));
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    // ---------------------------------------------------------------------------------------
    // Reversion detection tests
    // ---------------------------------------------------------------------------------------

    /**
     * After changing the op cert, writing back the original content should remove the backup
     * file and clear the pending change entirely (because the op cert array only has one slot,
     * so there are no other TRUE flags to keep the property in pending changes).
     */
    @Test
    public void opCertRevertToOriginal_clearsBackupAndPending() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "original".getBytes());

            // Note: the content actually becomes "changedl". Would need to write a file size change too.
            atomicWriteFile(localDevice, OP_CERT_ID, "changed".getBytes());
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));

            // Write the original bytes back.
            atomicWriteFile(localDevice, OP_CERT_ID, "original".getBytes());

            // Backup gone, pending change cleared, file has original content.
            assertFalse(Files.exists(backupPathFor(OP_CERT_ID)));
            assertFalse(certPending(OP_CERT_ID));
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
            assertArrayEquals("original".getBytes(), readCertFile(OP_CERT_ID));
        });
    }

    /**
     * With both issuer certs changed, reverting one should leave the property in pending
     * changes with only the still-changed slot flagged TRUE. The other slot's backup file is
     * deleted.
     */
    @Test
    public void issuerCertRevertOneSlot_keepsPropertyPendingForOther() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), "orig-1".getBytes());
            Files.write(pathFor(ISSUER_2_ID), "orig-2".getBytes());

            atomicWriteFile(localDevice, ISSUER_1_ID, "changed-1".getBytes());
            atomicWriteFile(localDevice, ISSUER_2_ID, "changed-2".getBytes());

            // Revert slot 0 only.
            atomicWriteFile(localDevice, ISSUER_1_ID, "orig-1".getBytes());

            assertFalse(Files.exists(backupPathFor(ISSUER_1_ID)));
            assertTrue(Files.exists(backupPathFor(ISSUER_2_ID)));
            assertFalse(certPending(ISSUER_1_ID));
            assertTrue(certPending(ISSUER_2_ID));
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    /**
     * With both issuer slots changed then both reverted, the pending-changes property is
     * removed entirely (no TRUE flags left in the array).
     */
    @Test
    public void issuerCertRevertBothSlots_removesPendingProperty() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), "orig-1".getBytes());
            Files.write(pathFor(ISSUER_2_ID), "orig-2".getBytes());

            atomicWriteFile(localDevice, ISSUER_1_ID, "changed-1".getBytes());
            atomicWriteFile(localDevice, ISSUER_2_ID, "changed-2".getBytes());

            atomicWriteFile(localDevice, ISSUER_1_ID, "orig-1".getBytes());
            atomicWriteFile(localDevice, ISSUER_2_ID, "orig-2".getBytes());

            assertFalse(certPending(ISSUER_1_ID));
            assertFalse(certPending(ISSUER_2_ID));
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    // ---------------------------------------------------------------------------------------
    // Discard command tests
    // ---------------------------------------------------------------------------------------

    /**
     * Writing DISCARD_CHANGES to the Command property must restore the backup files to their
     * original paths and clear the pending-changes map.
     */
    @Test
    public void discardChanges_restoresBackupsAndClearsPending() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "op-original".getBytes());
            Files.write(pathFor(ISSUER_1_ID), "issuer1-original".getBytes());

            atomicWriteFile(localDevice, OP_CERT_ID, "op-changed".getBytes());
            atomicWriteFile(localDevice, ISSUER_1_ID, "issuer1-changed".getBytes());
            assertEquals(Boolean.TRUE, npo.readProperty(PropertyIdentifier.changesPending));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.discardChanges);

            // Command executes on the local device's executor — wait for it to finish.
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            // Files restored, backups gone, pending cleared.
            assertArrayEquals("op-original".getBytes(), readCertFile(OP_CERT_ID));
            assertArrayEquals("issuer1-original".getBytes(), readCertFile(ISSUER_1_ID));
            assertFalse(Files.exists(backupPathFor(OP_CERT_ID)));
            assertFalse(Files.exists(backupPathFor(ISSUER_1_ID)));
            assertTrue(npo.getPendingChanges().isEmpty());
            assertTrue(npo.getChangedCertificateFiles().isEmpty());
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    // ---------------------------------------------------------------------------------------
    // Unintended restart tests
    // ---------------------------------------------------------------------------------------

    /**
     * If the device restarts (e.g., a crash or power cycle) while pending changes exist,
     * the backup files remain on disk. On the next init, the port object's initializeImpl
     * must move each backup back over its cert file, effectively discarding the pending
     * changes. This is the safety property: unactivated changes never take effect on a
     * cold start.
     */
    @Test
    public void unintendedRestart_restoresBackupsOnInit() throws Exception {
        // Simulate the state left by a crash mid-pending: cert file has NEW bytes, backup
        // file has ORIGINAL bytes. Both files sit in the temp folder; there is no first-pass
        // LocalDevice — we go straight to the "restart" LocalDevice that should reinstate.
        Files.write(certFile("operational.pem").toPath(), "post-crash-new".getBytes());
        Files.write(certFile("operational.pem.pendingChangesBackup").toPath(), "pre-crash-original".getBytes());
        Files.write(certFile("issuer1.pem").toPath(), "issuer1-new".getBytes());
        Files.write(certFile("issuer1.pem.pendingChangesBackup").toPath(), "issuer1-original".getBytes());

        withLocalDevice(localDevice -> {
            // On init, backups were reinstated: cert files now hold the pre-crash content
            // and the backup files are gone.
            assertArrayEquals("pre-crash-original".getBytes(), readCertFile(OP_CERT_ID));
            assertArrayEquals("issuer1-original".getBytes(), readCertFile(ISSUER_1_ID));
            assertFalse(Files.exists(backupPathFor(OP_CERT_ID)));
            assertFalse(Files.exists(backupPathFor(ISSUER_1_ID)));
            // No pending changes because they never activated and the state is now the
            // pre-crash steady state.
            assertTrue(npo.getPendingChanges().isEmpty());
            assertTrue(npo.getChangedCertificateFiles().isEmpty());
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    // ---------------------------------------------------------------------------------------
    // Apply pending changes tests
    // ---------------------------------------------------------------------------------------

    /**
     * applyPendingChanges commits pending cert file changes: the new content stays in place,
     * the backup files are deleted, and the pending-changes map is cleared so Changes_Pending
     * reads FALSE.
     */
    @Test
    public void applyPendingChanges_deletesBackupsAndClearsPending() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "old-op-cert".getBytes());
            Files.write(pathFor(ISSUER_1_ID), "old-issuer-1".getBytes());

            atomicWriteFile(localDevice, OP_CERT_ID, "new-op-cert".getBytes());
            atomicWriteFile(localDevice, ISSUER_1_ID, "new-issuer-1".getBytes());
            assertTrue(Files.exists(backupPathFor(OP_CERT_ID)));
            assertTrue(Files.exists(backupPathFor(ISSUER_1_ID)));

            npo.applyPendingChanges();

            assertArrayEquals("new-op-cert".getBytes(), readCertFile(OP_CERT_ID));
            assertArrayEquals("new-issuer-1".getBytes(), readCertFile(ISSUER_1_ID));
            assertFalse(Files.exists(backupPathFor(OP_CERT_ID)));
            assertFalse(Files.exists(backupPathFor(ISSUER_1_ID)));
            assertTrue(npo.getPendingChanges().isEmpty());
            assertTrue(npo.getChangedCertificateFiles().isEmpty());
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    /**
     * After applyPendingChanges, a restart must NOT revert the cert files: the backups are
     * gone, so the next initialization leaves the new content in place. This is the activation
     * counterpart to unintendedRestart_restoresBackupsOnInit.
     */
    @Test
    public void applyPendingChanges_thenRestart_keepsNewContent() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "original-cert".getBytes());
            atomicWriteFile(localDevice, OP_CERT_ID, "activated-cert".getBytes());
            npo.applyPendingChanges();
        });

        // Simulated restart: a new LocalDevice session over the same files.
        withLocalDevice(localDevice -> {
            assertArrayEquals("activated-cert".getBytes(), readCertFile(OP_CERT_ID));
            assertFalse(Files.exists(backupPathFor(OP_CERT_ID)));
            assertEquals(Boolean.FALSE, npo.readProperty(PropertyIdentifier.changesPending));
        });
    }

    // ---------------------------------------------------------------------------------------
    // Validate command tests
    // ---------------------------------------------------------------------------------------

    /**
     * Writing VALIDATE_CHANGES to the Command property with no pending certificate changes
     * must produce a success Health in Command_Validation_Result (delegated to the base
     * class's success path). Confirms the validate command dispatch works end-to-end.
     */
    @Test
    public void validateChanges_noCertChanges_reportsSuccess() throws Exception {
        withLocalDevice(localDevice -> {
            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertNotNull(result);
            assertEquals(ErrorClass.object, result.getResult().getErrorClass());
            assertEquals(ErrorCode.success, result.getResult().getErrorCode());
        });
    }

    /**
     * Erasing the operational certificate is only acceptable as part of a full credential
     * erasure. If the operational cert file is truncated to 0 bytes while an issuer certificate
     * remains configured, VALIDATE_CHANGES must report CERTIFICATE_INVALID on
     * operationalCertificateFile with error class SECURITY.
     */
    @Test
    public void validateChanges_opCertErasedWithIssuerConfigured_reportsInvalid() throws Exception {
        var now = Instant.now();
        var issuerA = makeCert(issuerAKey(), issuerAKey(), "issuer-a",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "old-op-cert".getBytes());
            Files.write(pathFor(ISSUER_1_ID), der(issuerA));

            // Truncate the op cert file to 0 bytes, creating a pending change on it.
            handleConfirmedRequest(localDevice, new WritePropertyRequest(OP_CERT_ID, PropertyIdentifier.fileSize,
                    null, UnsignedInteger.ZERO, null));
            assertTrue(certPending(OP_CERT_ID));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.security, result.getResult().getErrorClass());
            assertEquals(ErrorCode.certificateInvalid, result.getResult().getErrorCode());
            assertEquals(PropertyIdentifier.operationalCertificateFile, result.getProperty());
        });
    }

    /**
     * A full credential erasure — the operational certificate and both issuer certificates all
     * truncated to 0 bytes — must validate successfully. An empty credential set is a
     * legitimate state (e.g. decommissioning; see AB.7.4.2).
     */
    @Test
    public void validateChanges_fullCredentialErasure_reportsSuccess() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "old-op-cert".getBytes());
            Files.write(pathFor(ISSUER_1_ID), "old-issuer-1".getBytes());
            Files.write(pathFor(ISSUER_2_ID), "old-issuer-2".getBytes());

            for (var fileId : new ObjectIdentifier[] {OP_CERT_ID, ISSUER_1_ID, ISSUER_2_ID}) {
                handleConfirmedRequest(localDevice, new WritePropertyRequest(fileId, PropertyIdentifier.fileSize,
                        null, UnsignedInteger.ZERO, null));
            }
            assertTrue(certPending(OP_CERT_ID));
            assertTrue(certPending(ISSUER_1_ID));
            assertTrue(certPending(ISSUER_2_ID));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.object, result.getResult().getErrorClass());
            assertEquals(ErrorCode.success, result.getResult().getErrorCode());
        });
    }

    /**
     * If the operational cert is pending and its file contains garbage (not a valid X.509
     * PEM), VALIDATE_CHANGES must report CERTIFICATE_MALFORMED with error class SECURITY.
     * This exercises the CertificateException path in loadCertificate.
     */
    @Test
    public void validateChanges_pendingOpCertGarbage_reportsMalformed() throws Exception {
        withLocalDevice(localDevice -> {
            Files.write(pathFor(OP_CERT_ID), "old".getBytes());

            atomicWriteFile(localDevice, OP_CERT_ID, "not a real PEM certificate".getBytes());

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.security, result.getResult().getErrorClass());
            assertEquals(ErrorCode.certificateMalformed, result.getResult().getErrorCode());
            assertEquals(PropertyIdentifier.operationalCertificateFile, result.getProperty());
        });
    }

    /**
     * If an issuer cert on disk has garbage bytes, VALIDATE_CHANGES surfaces the malformed
     * error against the issuer property. Load-time parsing is unconditional (all three cert
     * files are read before any pending-changes gating), so an already-broken issuer cert
     * becomes visible whenever validate is run.
     * <p>
     * The op cert is left empty (0 bytes) so its load returns null without throwing, letting
     * the failure surface on issuer 1 which is the file we deliberately corrupted.
     */
    @Test
    public void validateChanges_issuerCertGarbageOnDisk_reportsMalformed() throws Exception {
        withLocalDevice(localDevice -> {
            // Op cert empty (loads to null with no exception).
            Files.write(pathFor(OP_CERT_ID), new byte[0]);
            // Issuer 1 has non-PEM bytes — its load throws CertificateException.
            Files.write(pathFor(ISSUER_1_ID), "invalid issuer bytes".getBytes());
            // Issuer 2 change is what triggered VALIDATE_CHANGES to run.
            atomicWriteFile(localDevice, ISSUER_2_ID, "some-issuer-2-content".getBytes());

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.security, result.getResult().getErrorClass());
            assertEquals(ErrorCode.certificateMalformed, result.getResult().getErrorCode());
            assertEquals(PropertyIdentifier.issuerCertificateFiles, result.getProperty());
        });
    }

    // ---------------------------------------------------------------------------------------
    // Crypto helper — generates real X.509 certs for the validation-path tests below
    // ---------------------------------------------------------------------------------------

    /** Cached to avoid repeated 100+ ms key generation across tests. */
    private static KeyPair cachedDeviceKey;
    private static KeyPair cachedOtherKey;
    private static KeyPair cachedIssuerAKey;
    private static KeyPair cachedIssuerBKey;

    private static KeyPair deviceKey() throws Exception {
        if (cachedDeviceKey == null)
            cachedDeviceKey = generateEcKeyPair();
        return cachedDeviceKey;
    }

    private static KeyPair otherKey() throws Exception {
        if (cachedOtherKey == null)
            cachedOtherKey = generateEcKeyPair();
        return cachedOtherKey;
    }

    private static KeyPair issuerAKey() throws Exception {
        if (cachedIssuerAKey == null)
            cachedIssuerAKey = generateEcKeyPair();
        return cachedIssuerAKey;
    }

    private static KeyPair issuerBKey() throws Exception {
        if (cachedIssuerBKey == null)
            cachedIssuerBKey = generateEcKeyPair();
        return cachedIssuerBKey;
    }

    private static KeyPair generateEcKeyPair() throws Exception {
        var kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        return kpg.generateKeyPair();
    }

    /**
     * Builds an X.509 certificate with the given subject key, signed by the given issuer key.
     * Set issuer == subject for a self-signed cert (used for issuer certs in these tests).
     */
    private static X509Certificate makeCert(KeyPair subject, KeyPair issuer, String cn, Instant notBefore,
            Instant notAfter) throws Exception {
        var issuerName = new X500Name("CN=" + (issuer == subject ? cn : cn + "-signer"));
        var subjectName = new X500Name("CN=" + cn);
        var serial = new BigInteger(64, new SecureRandom());
        var signer = new JcaContentSignerBuilder("SHA256withECDSA").build(issuer.getPrivate());
        var builder = new JcaX509v3CertificateBuilder(issuerName, serial, Date.from(notBefore), Date.from(notAfter),
                subjectName, subject.getPublic());
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder.build(signer));
    }

    /** Convenience: DER-encoded bytes of a cert, which the port object's loader accepts. */
    private static byte[] der(X509Certificate cert) throws Exception {
        return cert.getEncoded();
    }

    // ---------------------------------------------------------------------------------------
    // validateCerts happy-path and remaining error paths
    // ---------------------------------------------------------------------------------------

    /**
     * Full happy path: operational cert has current validity, matches the device's active key
     * pair, and is signed by one of the two issuer certs. VALIDATE_CHANGES returns success.
     * This exercises the code path all the way through the key pair handler's matches check
     * and verifyAgainstAnyIssuer without an intervening failure.
     */
    @Test
    public void validateChanges_pendingOpCertValid_reportsSuccess() throws Exception {
        var now = Instant.now();
        var opCert = makeCert(deviceKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var issuerA = makeCert(issuerAKey(), issuerAKey(), "issuer-a",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            // Both issuer slots must be present before op cert is validated (validateCerts loads
            // all three cert files unconditionally). Slot 0 is the real signer; slot 1 is empty.
            Files.write(pathFor(ISSUER_1_ID), der(issuerA));
            Files.write(pathFor(OP_CERT_ID), new byte[0]);
            atomicWriteFile(localDevice, OP_CERT_ID, der(opCert));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.object, result.getResult().getErrorClass());
            assertEquals(ErrorCode.success, result.getResult().getErrorCode());
        });
    }

    /**
     * An operational cert whose notAfter is in the past must be reported as CERTIFICATE_EXPIRED
     * under the operationalCertificateFile property, per 12.56.100.
     */
    @Test
    public void validateChanges_pendingOpCertExpired_reportsExpired() throws Exception {
        var now = Instant.now();
        // notBefore two hours ago, notAfter one hour ago.
        var opCert = makeCert(deviceKey(), issuerAKey(), "device",
                now.minusSeconds(7200), now.minusSeconds(3600));
        var issuerA = makeCert(issuerAKey(), issuerAKey(), "issuer-a",
                now.minusSeconds(7200), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), der(issuerA));
            Files.write(pathFor(OP_CERT_ID), new byte[0]);
            atomicWriteFile(localDevice, OP_CERT_ID, der(opCert));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.security, result.getResult().getErrorClass());
            assertEquals(ErrorCode.certificateExpired, result.getResult().getErrorCode());
            assertEquals(PropertyIdentifier.operationalCertificateFile, result.getProperty());
        });
    }

    /**
     * An operational cert whose notBefore is in the future is reported as CERTIFICATE_INVALID.
     * 12.56.100 does not enumerate a specific code for "not yet valid", so the current
     * implementation maps it to certificateInvalid (as noted in the CertException handler).
     */
    @Test
    public void validateChanges_pendingOpCertNotYetValid_reportsInvalid() throws Exception {
        var now = Instant.now();
        // notBefore one hour in the future.
        var opCert = makeCert(deviceKey(), issuerAKey(), "device",
                now.plusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var issuerA = makeCert(issuerAKey(), issuerAKey(), "issuer-a",
                now.minusSeconds(7200), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), der(issuerA));
            Files.write(pathFor(OP_CERT_ID), new byte[0]);
            atomicWriteFile(localDevice, OP_CERT_ID, der(opCert));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.security, result.getResult().getErrorClass());
            assertEquals(ErrorCode.certificateInvalid, result.getResult().getErrorCode());
            assertEquals(PropertyIdentifier.operationalCertificateFile, result.getProperty());
        });
    }

    /**
     * A valid operational cert whose public key matches neither the active nor the pending key
     * pair must be reported as unknownCertificateKey (the current replacement for the
     * deprecated UNKNOWN_KEY error code from clause 12.56.100).
     */
    @Test
    public void validateChanges_pendingOpCertWrongKey_reportsUnknownKey() throws Exception {
        var now = Instant.now();
        // Cert is bound to otherKey, but the handler only holds deviceKey (no pending pair).
        var opCert = makeCert(otherKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var issuerA = makeCert(issuerAKey(), issuerAKey(), "issuer-a",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), der(issuerA));
            Files.write(pathFor(OP_CERT_ID), new byte[0]);
            atomicWriteFile(localDevice, OP_CERT_ID, der(opCert));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.security, result.getResult().getErrorClass());
            assertEquals(ErrorCode.unknownCertificateKey, result.getResult().getErrorCode());
            assertEquals(PropertyIdentifier.operationalCertificateFile, result.getProperty());
        });
    }

    /**
     * A valid operational cert that matches the device's private key but is not signed by any
     * of the loaded issuer certs must be reported as CERTIFICATE_INVALID under the
     * operationalCertificateFile property (per clause 12.56.100's "cannot be validated by one
     * of the issuer certificates").
     */
    @Test
    public void validateChanges_pendingOpCertNotSignedByAnyIssuer_reportsInvalid() throws Exception {
        var now = Instant.now();
        // Op cert is signed by issuerA. The issuer certs on disk are B and C — neither
        // signed the op cert.
        var opCert = makeCert(deviceKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var issuerB = makeCert(issuerBKey(), issuerBKey(), "issuer-b",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            Files.write(pathFor(ISSUER_1_ID), der(issuerB));
            // Slot 2 empty — loadCertificate returns null and is skipped by verifyAgainstAnyIssuer.
            Files.write(pathFor(OP_CERT_ID), new byte[0]);
            atomicWriteFile(localDevice, OP_CERT_ID, der(opCert));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.security, result.getResult().getErrorClass());
            assertEquals(ErrorCode.certificateInvalid, result.getResult().getErrorCode());
            assertEquals(PropertyIdentifier.operationalCertificateFile, result.getProperty());
        });
    }

    /**
     * An expired issuer certificate is not itself a validation failure: 12.56.101 defines no
     * required validations for issuer files, and AB.7.4 does not check issuer expiry when
     * establishing connections either. As long as the operational certificate chain still
     * verifies, VALIDATE_CHANGES reports success.
     */
    @Test
    public void validateChanges_expiredIssuerCert_reportsSuccess() throws Exception {
        var now = Instant.now();
        var opCert = makeCert(deviceKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var expiredIssuer = makeCert(issuerAKey(), issuerAKey(), "issuer-a",
                now.minusSeconds(7200), now.minusSeconds(3600));

        withLocalDevice(deviceKey(), localDevice -> {
            Files.write(pathFor(OP_CERT_ID), der(opCert));
            atomicWriteFile(localDevice, ISSUER_1_ID, der(expiredIssuer));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.object, result.getResult().getErrorClass());
            assertEquals(ErrorCode.success, result.getResult().getErrorCode());
        });
    }

    /**
     * Changing an issuer certificate re-validates the operational certificate chain even when
     * the operational certificate itself has no pending change. Replacing the signing issuer
     * with an unrelated one must be reported as CERTIFICATE_INVALID on
     * operationalCertificateFile — otherwise activating the change would leave the device with
     * an unverifiable operational certificate.
     */
    @Test
    public void validateChanges_issuerChangeBreaksOpCertChain_reportsInvalid() throws Exception {
        var now = Instant.now();
        var opCert = makeCert(deviceKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var issuerA = makeCert(issuerAKey(), issuerAKey(), "issuer-a",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var issuerB = makeCert(issuerBKey(), issuerBKey(), "issuer-b",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            // Established configuration: op cert signed by issuer A, no pending changes.
            Files.write(pathFor(OP_CERT_ID), der(opCert));
            Files.write(pathFor(ISSUER_1_ID), der(issuerA));

            // Replace the signer with an unrelated issuer. Only the issuer file is pending.
            atomicWriteFile(localDevice, ISSUER_1_ID, der(issuerB));
            assertFalse(certPending(OP_CERT_ID));
            assertTrue(certPending(ISSUER_1_ID));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.security, result.getResult().getErrorClass());
            assertEquals(ErrorCode.certificateInvalid, result.getResult().getErrorCode());
            assertEquals(PropertyIdentifier.operationalCertificateFile, result.getProperty());
        });
    }

    /**
     * An issuer certificate change that leaves the operational certificate chain intact
     * validates successfully: writing an unrelated issuer into the empty second slot does not
     * disturb the slot-0 issuer that signed the operational certificate.
     */
    @Test
    public void validateChanges_issuerChangeKeepsOpCertChain_reportsSuccess() throws Exception {
        var now = Instant.now();
        var opCert = makeCert(deviceKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var issuerA = makeCert(issuerAKey(), issuerAKey(), "issuer-a",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var issuerB = makeCert(issuerBKey(), issuerBKey(), "issuer-b",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            Files.write(pathFor(OP_CERT_ID), der(opCert));
            Files.write(pathFor(ISSUER_1_ID), der(issuerA));

            atomicWriteFile(localDevice, ISSUER_2_ID, der(issuerB));
            assertTrue(certPending(ISSUER_2_ID));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.object, result.getResult().getErrorClass());
            assertEquals(ErrorCode.success, result.getResult().getErrorCode());
        });
    }

    // ---------------------------------------------------------------------------------------
    // Command dispatch — GENERATE_CSR_FILE is dispatched to the generateCsrFile() hook.
    // ---------------------------------------------------------------------------------------

    /**
     * GENERATE_CSR_FILE with the default (no-op) generateCsrFile() implementation is accepted
     * without exception. The executor runs the hook and returns Command to idle. Proves the
     * dispatch path in afterWriteProperty and the executor lifecycle.
     */
    @Test
    public void command_generateCsrFile_acceptedByScPort() throws Exception {
        withLocalDevice(localDevice -> {
            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.generateCsrFile);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));
            // No assertion beyond the await — the default hook is a no-op. Failure would
            // surface as an exception during writeProperty or a hung await.
        });
    }

    /**
     * A pending change that writes a new operational certificate while every issuer
     * certificate file is empty (removed) must be rejected by VALIDATE_CHANGES with
     * CERTIFICATE_INVALID on the operational-certificate property (12.56.100 required
     * validation: "cannot be validated by one of the issuer certificates").
     * <p>
     * The test starts with everything empty on disk (as the withLocalDevice helper leaves
     * things) — this models the "issuer certs have been removed" state. Then a new op cert
     * is atomically written, setting the op-cert pending flag. When validation runs, the op
     * cert loads and matches the private key, but verifyAgainstAnyIssuer sees both slots as
     * null and returns false, producing the expected error.
     * <p>
     * Note: attempting to "remove" a populated issuer file via atomic-write of 0 bytes does
     * NOT truncate the underlying file (StreamAccess.writeData seeks and writes, without
     * truncation). Real removal happens via a WriteProperty on File_Size, which is a
     * separate code path. This test focuses on the validation outcome, not the removal
     * mechanism — the end state at validation time is what matters.
     */
    @Test
    public void validateChanges_opCertChangedWithAllIssuersMissing_reportsInvalid() throws Exception {
        var now = Instant.now();
        var opCert = makeCert(deviceKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            // All cert files start empty on disk (from the withLocalDevice helper). Write the
            // new op cert to trigger its pending flag.
            atomicWriteFile(localDevice, OP_CERT_ID, der(opCert));
            assertTrue(certPending(OP_CERT_ID));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            // The op cert loaded fine, matches the private key, but no non-null issuer is
            // available to verify the signature — result is CERTIFICATE_INVALID on the op cert
            // property. This is the critical safeguard against activating a change that would
            // leave the device with an unverifiable operational certificate.
            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.security, result.getResult().getErrorClass());
            assertEquals(ErrorCode.certificateInvalid, result.getResult().getErrorCode());
            assertEquals(PropertyIdentifier.operationalCertificateFile, result.getProperty());
        });
    }

    // ---------------------------------------------------------------------------------------
    // Key pair lifecycle — validation against the pending pair, promotion at activation, and
    // retention through discard.
    // ---------------------------------------------------------------------------------------

    /**
     * An operational cert bound to the pending key pair (the normal key rotation case: the cert
     * was created from a CSR generated by GENERATE_CSR_FILE) must validate successfully even
     * though it does not match the active pair. This is the "an internal private key" wording
     * of 12.56.100: either the active or the pending pair is a legitimate match.
     */
    @Test
    public void validateChanges_pendingOpCertMatchesPendingKey_reportsSuccess() throws Exception {
        var now = Instant.now();
        // Cert is bound to otherKey, which the handler holds as the pending pair.
        var opCert = makeCert(otherKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        var issuerA = makeCert(issuerAKey(), issuerAKey(), "issuer-a",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            keyPairHandler.setPendingKeyPair(otherKey());

            Files.write(pathFor(ISSUER_1_ID), der(issuerA));
            Files.write(pathFor(OP_CERT_ID), new byte[0]);
            atomicWriteFile(localDevice, OP_CERT_ID, der(opCert));

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.validateChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            Health result = npo.readProperty(PropertyIdentifier.commandValidationResult);
            assertEquals(ErrorClass.object, result.getResult().getErrorClass());
            assertEquals(ErrorCode.success, result.getResult().getErrorCode());
        });
    }

    /**
     * Activating an operational cert bound to the pending pair promotes that pair to active in
     * the key pair handler and clears the pending pair. This is the key rotation completing.
     */
    @Test
    public void applyPendingChanges_certMatchesPendingKey_promotesPendingPair() throws Exception {
        var now = Instant.now();
        var opCert = makeCert(otherKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            keyPairHandler.setPendingKeyPair(otherKey());

            atomicWriteFile(localDevice, OP_CERT_ID, der(opCert));
            npo.applyPendingChanges();

            assertEquals(otherKey(), keyPairHandler.getActiveKeyPair());
            assertNull(keyPairHandler.getPendingKeyPair());
        });
    }

    /**
     * Activating an operational cert bound to the active pair (a renewal without key rotation)
     * leaves the handler's pairs untouched: the active pair stays active and an outstanding
     * pending pair is retained, because its CSR may still be at the signing CA.
     */
    @Test
    public void applyPendingChanges_certMatchesActiveKey_keepsPendingPair() throws Exception {
        var now = Instant.now();
        var opCert = makeCert(deviceKey(), issuerAKey(), "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        withLocalDevice(deviceKey(), localDevice -> {
            keyPairHandler.setPendingKeyPair(otherKey());

            atomicWriteFile(localDevice, OP_CERT_ID, der(opCert));
            npo.applyPendingChanges();

            assertEquals(deviceKey(), keyPairHandler.getActiveKeyPair());
            assertEquals(otherKey(), keyPairHandler.getPendingKeyPair());
        });
    }

    // ---------------------------------------------------------------------------------------
    // Current_Health and Reliability evaluation. In this test environment the operational cert
    // file is empty, so network initialization fails, which the port must surface on read.
    // ---------------------------------------------------------------------------------------

    /**
     * Current_Health (12.56.17) is evaluated on read and reports the network initialization error.
     * The withLocalDevice harness leaves the operational certificate file empty, so TLS
     * initialization fails with valueOutOfRange on the property class.
     */
    @Test
    public void currentHealth_reportsInitializationError() throws Exception {
        withLocalDevice(localDevice -> {
            Health health = npo.readProperty(PropertyIdentifier.currentHealth);
            assertEquals(ErrorClass.property, health.getResult().getErrorClass());
            assertEquals(ErrorCode.valueOutOfRange, health.getResult().getErrorCode());
        });
    }

    /**
     * Reliability is evaluated on read; a network initialization failure is reported as
     * CONFIGURATION_ERROR, which also raises the FAULT status flag.
     */
    @Test
    public void reliability_reportsConfigurationErrorOnInitializationFailure() throws Exception {
        withLocalDevice(localDevice -> {
            assertEquals(Reliability.configurationError, npo.readProperty(PropertyIdentifier.reliability));
        });
    }

    /**
     * DISCARD_CHANGES reverts pending certificate file changes but must not clear the pending
     * key pair: GENERATE_CSR_FILE does not affect Changes_Pending (12.56.16), so the pending
     * pair's lifecycle is independent of the pending changes framework.
     */
    @Test
    public void discardChanges_keepsPendingKeyPair() throws Exception {
        withLocalDevice(deviceKey(), localDevice -> {
            keyPairHandler.setPendingKeyPair(otherKey());

            Files.write(pathFor(OP_CERT_ID), "original".getBytes());
            atomicWriteFile(localDevice, OP_CERT_ID, "changed".getBytes());

            npo.writeProperty(new ValueSource(), PropertyIdentifier.command,
                    NetworkPortCommand.discardChanges);
            TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

            assertArrayEquals("original".getBytes(), readCertFile(OP_CERT_ID));
            assertEquals(otherKey(), keyPairHandler.getPendingKeyPair());
        });
    }
}
