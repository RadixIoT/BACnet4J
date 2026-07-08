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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SCNetworkUtilsTest {

    // ---- loadPEM ----

    /**
     * A minimal, well-formed PEM block round-trips to the raw base64-decoded bytes.
     */
    @Test
    public void loadPEM_wellFormed_returnsDecodedBytes() {
        String pem = """
                -----BEGIN CERTIFICATE-----
                SGVsbG8sIFdvcmxkIQ==
                -----END CERTIFICATE-----
                """;
        byte[] result = SCNetworkUtils.loadPEM(pem);
        assertArrayEquals("Hello, World!".getBytes(), result);
    }

    /**
     * A multi-line base64 payload decodes across line breaks. MimeDecoder tolerates the
     * embedded line breaks that real PEM files always include.
     */
    @Test
    public void loadPEM_multiLineBase64_decodes() {
        String pem = """
                -----BEGIN CERTIFICATE-----
                SGVsbG8s
                IFdvcmxk
                IQ==
                -----END CERTIFICATE-----
                """;
        byte[] result = SCNetworkUtils.loadPEM(pem);
        assertArrayEquals("Hello, World!".getBytes(), result);
    }

    /**
     * Input that does not start with "---" (the beginning of a PEM header) is rejected. This
     * catches accidental inclusion of leading text (e.g., "openssl -text" output that
     * prepends a decoded description above the base64 block).
     */
    @Test
    public void loadPEM_missingHeaderPrefix_throws() {
        String garbage = "not a PEM block";
        assertThrows(IllegalArgumentException.class, () -> SCNetworkUtils.loadPEM(garbage));
    }

    // ---- isBroadcast ----

    @Test
    public void isBroadcast_broadcastVmac_returnsTrue() {
        var bcast =
                new SCVmac(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        assertTrue(SCNetworkUtils.isBroadcast(bcast));
    }

    @Test
    public void isBroadcast_regularVmac_returnsFalse() {
        var normal = new SCVmac(new byte[] {0x02, 0x11, 0x22, 0x33, 0x44, 0x55});
        assertFalse(SCNetworkUtils.isBroadcast(normal));
    }

    // ---- generateCertificate empty-bytes path ----

    /**
     * Empty file bytes return null rather than throwing — allows callers to treat an unset
     * cert file as "no cert" without special-casing.
     */
    @Test
    public void generateCertificate_emptyBytes_returnsNull() throws Exception {
        var cf = java.security.cert.CertificateFactory.getInstance(SCNetworkUtils.DEFAULT_CERTIFICATE_TYPE);
        assertNull(SCNetworkUtils.generateCertificate(cf, new byte[0]));
    }
}
