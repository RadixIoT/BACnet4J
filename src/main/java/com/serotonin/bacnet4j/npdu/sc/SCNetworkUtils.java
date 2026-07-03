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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.regex.Pattern;

import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class SCNetworkUtils {
    public static final SCVmac LOCAL_BROADCAST_VMAC = new SCVmac(StreamUtils.fromHex("FFFFFFFFFFFF"));
    public static final String DEFAULT_CERTIFICATE_TYPE = "X.509";

    private SCNetworkUtils() {
    }

    public static byte[] loadPEM(String pem) {
        if (!pem.startsWith("---")) {
            throw new IllegalArgumentException("PEM data contains extra starting content. Use openssl -notext");
        }
        Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
        String encoded = parse.matcher(pem).replaceFirst("$1");
        return Base64.getMimeDecoder().decode(encoded);
    }

    public static boolean isBroadcast(SCVmac vmac) {
        return LOCAL_BROADCAST_VMAC.equals(vmac);
    }

    public static X509Certificate generateCertificate(CertificateFactory cf, byte[] bytes) throws CertificateException {
        if (bytes.length == 0) {
            return null;
        }
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
    }
}
