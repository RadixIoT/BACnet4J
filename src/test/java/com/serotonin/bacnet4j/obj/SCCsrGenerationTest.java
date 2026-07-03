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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.npdu.sc.SCNetwork;
import com.serotonin.bacnet4j.npdu.sc.SCNetworkBuilder;
import com.serotonin.bacnet4j.obj.fileAccess.StreamAccess;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;

/**
 * Integration test that illustrates how a product developer would implement CSR generation
 * on a BACnet/SC network port, per clause 12.56.102 (GENERATE_CSR_FILE command).
 * <p>
 * The BACnet4J library provides the extension point via
 * {@link SecureConnectNetworkPortObject#generateCsrFile()} but does not embed a CSR builder,
 * because doing so would require a mandatory dependency on BouncyCastle. A real product will
 * subclass {@code SecureConnectNetworkPortObject}, override {@code generateCsrFile()}, and
 * plug in its preferred crypto library. This test demonstrates the pattern using BouncyCastle.
 */
public class SCCsrGenerationTest {
    private static final String UUID = "46663baa-98cc-4cf7-ad19-503f4705b130";
    private static final OctetString VMAC = OctetString.fromHex("010203040506");
    private static final int OPERATIONAL_CERT = 1;
    private static final int ISSUER_CERT_1 = 2;
    private static final int ISSUER_CERT_2 = 3;
    private static final int CSR_FILE = 4;
    private static final int NETWORK_PORT_INSTANCE = 12;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    LocalDevice localDevice;
    CsrGeneratingSCNetworkPortObject npo;

    /**
     * Register BouncyCastle as a JCA provider so it can supply a KeyFactory for the EC OID
     * (1.2.840.10045.2.1) when the CSR verifier reconstructs the public key from the encoded
     * SubjectPublicKeyInfo. Some JVMs (minimal builds, certain corporate distributions) do
     * not register an EC KeyFactory under that OID name in the default provider set.
     */
    @BeforeClass
    public static void registerBouncyCastle() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @After
    public void after() {
        if (localDevice != null) {
            localDevice.terminate();
        }
    }

    @Before
    public void before() throws Exception {
        // Empty files for FileObject's existence check.
        for (var name : new String[] {"operational.pem", "issuer1.pem", "issuer2.pem", "csr.pem"}) {
            Files.write(new File(tempFolder.getRoot(), name).toPath(), new byte[0]);
        }

        var network = new SCNetworkBuilder()
                .vmac(VMAC)
                .uuid(UUID)
                .localNetworkNumber(123)
                .primaryHubUri("wss://hub.example.com:4443")
                .keyPair(mock(KeyPair.class))  // Real key not needed; CSR flow generates its own.
                .operationalCertificateFileId(OPERATIONAL_CERT)
                .issuerCertificateFile1Id(ISSUER_CERT_1)
                .issuerCertificateFile2Id(ISSUER_CERT_2)
                .certificateSigningRequestFileId(CSR_FILE)
                .build();

        localDevice = new LocalDevice(1, new DefaultTransport(network));
        localDevice.addObject(new FileObject(localDevice, OPERATIONAL_CERT, "pem",
                new StreamAccess(new File(tempFolder.getRoot(), "operational.pem"))));
        localDevice.addObject(new FileObject(localDevice, ISSUER_CERT_1, "pem",
                new StreamAccess(new File(tempFolder.getRoot(), "issuer1.pem"))));
        localDevice.addObject(new FileObject(localDevice, ISSUER_CERT_2, "pem",
                new StreamAccess(new File(tempFolder.getRoot(), "issuer2.pem"))));
        localDevice.addObject(new FileObject(localDevice, CSR_FILE, "pem",
                new StreamAccess(new File(tempFolder.getRoot(), "csr.pem"))));
        npo = localDevice.addObject(new CsrGeneratingSCNetworkPortObject(
                localDevice, (SCNetwork) localDevice.getNetwork(), NETWORK_PORT_INSTANCE));
        localDevice.initialize();
    }

    /**
     * Client writes GENERATE_CSR_FILE to the Command property. The product's CSR generation
     * hook runs, produces a new key pair, builds a PKCS #10 CSR signed by the new private key,
     * and writes the PEM-encoded CSR into the file referenced by Certificate_Signing_Request_File.
     * <p>
     * The Command property returns to IDLE when the work completes. The new private key is
     * retained by the product implementation until a matching operational certificate is
     * written and activated via ReinitializeDevice.
     */
    @Test
    public void generateCsrFile_producesValidPkcs10() throws Exception {
        // Client requests CSR generation.
        npo.writeProperty(new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.generateCsrFile);

        // Command executes asynchronously on the local device's executor; wait for completion.
        TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));

        // The CSR file now contains a PEM-encoded PKCS #10 CSR.
        var csrPem = Files.readString(new File(tempFolder.getRoot(), "csr.pem").toPath());
        assertTrue("CSR should be PEM-encoded", csrPem.contains("-----BEGIN CERTIFICATE REQUEST-----"));
        assertTrue("CSR should be PEM-terminated", csrPem.contains("-----END CERTIFICATE REQUEST-----"));

        // Parse the CSR and verify the signature against the embedded public key. A valid
        // PKCS #10 CSR must be self-signed by the private key that will hold the eventual
        // operational certificate.
        var csr = readPemCsr(csrPem);
        assertNotNull(csr);
        assertTrue("CSR signature must verify against its own public key",
                csr.isSignatureValid(new JcaContentVerifierProviderBuilder()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(csr.getSubjectPublicKeyInfo())));

        // Product implementation retained the new key pair for later activation.
        assertNotNull("Product should retain the new private key", npo.pendingPrivateKey);
        assertNotNull("Product should retain the new public key", npo.pendingPublicKey);
        assertEquals("CSR public key must match the retained pending public key",
                csr.getSubjectPublicKeyInfo(),
                SubjectPublicKeyInfo.getInstance(npo.pendingPublicKey.getEncoded()));
    }

    /**
     * The base class rejects GENERATE_CSR_FILE for non-SC ports as optionalFunctionalityNotSupported.
     * The SC subclass opts in by overriding validateCommandInternal; this test just confirms the
     * command is accepted (no exception) on the SC network port.
     */
    @Test
    public void generateCsrFile_isAcceptedBySCPort() throws Exception {
        npo.writeProperty(new ValueSource(), PropertyIdentifier.command, NetworkPortCommand.generateCsrFile);
        TestUtils.await(() -> npo.readProperty(PropertyIdentifier.command).equals(NetworkPortCommand.idle));
        // Absence of exception is the assertion; the previous await guarantees the command handler ran.
    }

    // ---------------------------------------------------------------------------------------
    // Product-side implementation — this is the pattern a real BACnet/SC product would follow
    // ---------------------------------------------------------------------------------------


    /**
     * Example product subclass that plugs in a real CSR builder using BouncyCastle. A shipping
     * product would additionally:
     * <ul>
     *   <li>Persist the new private key durably (encrypted at rest).</li>
     *   <li>Restore it on device restart until the matching operational certificate is activated.</li>
     *   <li>Wire the new key pair into the SC network at ReinitializeDevice(ACTIVATE_CHANGES) time.</li>
     * </ul>
     */
    static class CsrGeneratingSCNetworkPortObject extends SecureConnectNetworkPortObject {
        /** Retained until ACTIVATE_CHANGES swaps this pair into the running SC network. */
        PrivateKey pendingPrivateKey;
        PublicKey pendingPublicKey;

        CsrGeneratingSCNetworkPortObject(LocalDevice localDevice, SCNetwork network, int instanceNumber) {
            super(localDevice, network, instanceNumber);
        }

        @Override
        protected void generateCsrFile() {
            try {
                // 1. Generate a fresh key pair.
                var kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(new ECGenParameterSpec("secp256r1"));
                var kp = kpg.generateKeyPair();
                pendingPrivateKey = kp.getPrivate();
                pendingPublicKey = kp.getPublic();

                // 2. Build a PKCS #10 CSR. The subject name identifies this device; a real
                //    product would include vendor-specific fields, device UUID, etc.
                X500Name subject = new X500Name("CN=BACnetDevice");
                var builder = new JcaPKCS10CertificationRequestBuilder(subject, kp.getPublic());
                ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(kp.getPrivate());
                PKCS10CertificationRequest csr = builder.build(signer);

                // 3. PEM-encode and write to the CSR file. The file object's data is what the
                //    client reads via AtomicReadFile after receiving Command_Validation_Result.
                var sw = new StringWriter();
                try (var pemWriter = new JcaPEMWriter(sw)) {
                    pemWriter.writeObject(csr);
                }
                var csrFileId = new ObjectIdentifier(ObjectType.file,
                        4);  // CSR_FILE — hard-coded here for clarity
                var csrFile = (FileObject) getLocalDevice().getObject(csrFileId);
                var access = (StreamAccess) csrFile.getFileAccess();
                access.writeData(0, new OctetString(sw.toString().getBytes()));
            } catch (Exception e) {
                throw new RuntimeException("CSR generation failed", e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // PEM parsing helper
    // ---------------------------------------------------------------------------------------

    private static PKCS10CertificationRequest readPemCsr(String pem) throws Exception {
        try (var reader = new PEMParser(new StringReader(pem))) {
            return (PKCS10CertificationRequest) reader.readObject();
        }
    }
}
