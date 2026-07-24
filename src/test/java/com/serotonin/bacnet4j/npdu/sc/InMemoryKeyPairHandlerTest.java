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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers the {@link SCKeyPairHandler} default methods and the promotion behavior of
 * {@link InMemoryKeyPairHandler}.
 */
public class InMemoryKeyPairHandlerTest {
    private static KeyPair activePair;
    private static KeyPair pendingPair;
    private static X509Certificate activeCert;
    private static X509Certificate pendingCert;
    private static X509Certificate unrelatedCert;

    @BeforeClass
    public static void setup() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        activePair = generateEcKeyPair();
        pendingPair = generateEcKeyPair();
        KeyPair unrelatedPair = generateEcKeyPair();
        activeCert = makeSelfSignedCert(activePair, "active");
        pendingCert = makeSelfSignedCert(pendingPair, "pending");
        unrelatedCert = makeSelfSignedCert(unrelatedPair, "unrelated");
    }

    @Test
    public void matchesEitherPair() {
        var handler = new InMemoryKeyPairHandler(activePair, pendingPair);
        assertTrue(handler.matches(activeCert));
        assertTrue(handler.matches(pendingCert));
        assertFalse(handler.matches(unrelatedCert));
    }

    @Test
    public void matchesWithoutPendingPair() {
        var handler = new InMemoryKeyPairHandler(activePair);
        assertTrue(handler.matches(activeCert));
        assertFalse(handler.matches(pendingCert));
    }

    @Test
    public void privateKeyForSelectsMatchingPair() {
        var handler = new InMemoryKeyPairHandler(activePair, pendingPair);
        assertEquals(activePair.getPrivate(), handler.privateKeyFor(activeCert));
        assertEquals(pendingPair.getPrivate(), handler.privateKeyFor(pendingCert));
        assertNull(handler.privateKeyFor(unrelatedCert));
    }

    /**
     * A mock KeyPair has null public and private keys, which the default methods must tolerate: it models a
     * handler that was constructed before real keys were configured.
     */
    @Test
    public void nullSafeWithMockActivePair() {
        var handler = new InMemoryKeyPairHandler(mock(KeyPair.class));
        assertFalse(handler.matches(activeCert));
        assertNull(handler.privateKeyFor(activeCert));
    }

    @Test
    public void certificateActivatedPromotesMatchingPendingPair() {
        var handler = new InMemoryKeyPairHandler(activePair, pendingPair);
        handler.certificateActivated(pendingCert);
        assertEquals(pendingPair, handler.getActiveKeyPair());
        assertNull(handler.getPendingKeyPair());
    }

    @Test
    public void certificateActivatedKeepsPendingPairOnActiveMatch() {
        var handler = new InMemoryKeyPairHandler(activePair, pendingPair);
        handler.certificateActivated(activeCert);
        assertEquals(activePair, handler.getActiveKeyPair());
        assertEquals(pendingPair, handler.getPendingKeyPair());
    }

    @Test
    public void certificateActivatedIgnoresUnrelatedCertificate() {
        var handler = new InMemoryKeyPairHandler(activePair, pendingPair);
        handler.certificateActivated(unrelatedCert);
        assertEquals(activePair, handler.getActiveKeyPair());
        assertEquals(pendingPair, handler.getPendingKeyPair());
    }

    private static KeyPair generateEcKeyPair() throws Exception {
        var kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        return kpg.generateKeyPair();
    }

    private static X509Certificate makeSelfSignedCert(KeyPair keyPair, String cn) throws Exception {
        var name = new X500Name("CN=" + cn);
        var serial = new BigInteger(64, new SecureRandom());
        var now = Instant.now();
        var signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate());
        var builder = new JcaX509v3CertificateBuilder(name, serial, Date.from(now.minusSeconds(3600)),
                Date.from(now.plusSeconds(3600)), name, keyPair.getPublic());
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder.build(signer));
    }
}
