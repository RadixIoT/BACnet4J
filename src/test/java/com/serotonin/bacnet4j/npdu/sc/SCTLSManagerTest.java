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

package com.serotonin.bacnet4j.npdu.sc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import javax.net.ssl.SSLContext;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import com.serotonin.bacnet4j.npdu.sc.SCTLSManager.TLSException;

/**
 * Verifies compliance with 135-2020cd-1 Clause AB.7.4, the TLS V1.3 Cipher Suite Application
 * Profile for BACnet/SC. The profile mandates support for:
 * <ul>
 *   <li>TLS 1.3</li>
 *   <li>Cipher suite TLS_AES_128_GCM_SHA256</li>
 *   <li>Signature algorithm ecdsa_secp256r1_sha256</li>
 *   <li>Key exchange group secp256r1</li>
 * </ul>
 */
public class SCTLSManagerTest {
    @BeforeClass
    public static void addBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    public void tls13IsAvailableInJvm() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        assertNotNull(ctx);
        assertEquals("TLSv1.3", ctx.getProtocol());
    }

    @Test
    public void requiredCipherSuiteIsAvailableInJvm() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        ctx.init(null, null, null);
        String[] supported = ctx.getSupportedSSLParameters().getCipherSuites();
        assertTrue("JVM must support the BACnet/SC required cipher suite "
                        + SCTLSManager.REQUIRED_CIPHER_SUITE + " but supports " + Arrays.toString(supported),
                Arrays.asList(supported).contains(SCTLSManager.REQUIRED_CIPHER_SUITE));
    }

    @Test
    public void assertProfileSupported_passesWhenCipherAvailable() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        ctx.init(null, null, null);
        SCTLSManager.assertProfileSupported(ctx);
    }

    @Test
    public void constructingSCTLSManager_exposesTls13ContextWithRequiredCipher() throws Exception {
        KeyPair deviceKey = generateEcKeyPair();
        KeyPair issuerKey = generateEcKeyPair();
        Instant now = Instant.now();
        X509Certificate opCert = makeCert(deviceKey, issuerKey, "device",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));
        X509Certificate issuerCert = makeCert(issuerKey, issuerKey, "issuer",
                now.minusSeconds(3600), now.plusSeconds(365 * 24 * 3600L));

        SCTLSManager tls = new SCTLSManager(deviceKey.getPrivate(), opCert.getEncoded(),
                issuerCert.getEncoded(), new byte[0]);

        SSLContext ctx = tls.getSSLContext();
        assertEquals("TLSv1.3", ctx.getProtocol());
        String[] supported = ctx.getSupportedSSLParameters().getCipherSuites();
        assertTrue(Arrays.asList(supported).contains(SCTLSManager.REQUIRED_CIPHER_SUITE));
        assertArrayEquals(opCert.getEncoded(), tls.getOperationalCertificate().getEncoded());
    }

    @Test
    public void tlsExceptionWithNullCausePreservesMessage() {
        TLSException ex = assertThrows(TLSException.class, () -> {
            throw new TLSException(
                    com.serotonin.bacnet4j.type.enumerated.ErrorClass.device,
                    com.serotonin.bacnet4j.type.enumerated.ErrorCode.internalError,
                    "test message", null);
        });
        assertEquals("test message", ex.getMessage());
        assertEquals(com.serotonin.bacnet4j.type.enumerated.ErrorClass.device, ex.getErrorClass());
        assertEquals(com.serotonin.bacnet4j.type.enumerated.ErrorCode.internalError, ex.getErrorCode());
    }

    private static KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        return kpg.generateKeyPair();
    }

    private static X509Certificate makeCert(KeyPair subject, KeyPair issuer, String cn, Instant notBefore,
            Instant notAfter) throws Exception {
        X500Name issuerName = new X500Name("CN=" + (issuer == subject ? cn : cn + "-signer"));
        X500Name subjectName = new X500Name("CN=" + cn);
        BigInteger serial = new BigInteger(64, new SecureRandom());
        var signer = new JcaContentSignerBuilder("SHA256withECDSA").build(issuer.getPrivate());
        var builder = new JcaX509v3CertificateBuilder(issuerName, serial, Date.from(notBefore), Date.from(notAfter),
                subjectName, subject.getPublic());
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder.build(signer));
    }
}
