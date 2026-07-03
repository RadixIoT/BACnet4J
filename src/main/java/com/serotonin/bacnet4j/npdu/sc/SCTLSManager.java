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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;

public class SCTLSManager {
    private static final Logger LOG = LoggerFactory.getLogger(SCTLSManager.class);
    private static final String TLS_VERSION = "TLSv1.3";

    protected final PrivateKey privateKey;
    protected final byte[] operationalCertificateBytes;
    protected final byte[] issuerCertificate1Bytes;
    protected final byte[] issuerCertificate2Bytes;

    protected SSLContext sslContext;
    protected X509Certificate operationalCertificate;

    public SCTLSManager(
            PrivateKey privateKey,
            byte[] operationalCertificateBytes,
            byte[] issuerCertificate1Bytes,
            byte[] issuerCertificate2Bytes)
            throws TLSException {
        this.privateKey = privateKey;
        this.operationalCertificateBytes = operationalCertificateBytes;
        this.issuerCertificate1Bytes = issuerCertificate1Bytes;
        this.issuerCertificate2Bytes = issuerCertificate2Bytes;

        this.sslContext = makeSSLContext();
    }

    public SSLContext getSSLContext() {
        return sslContext;
    }

    public X509Certificate getOperationalCertificate() {
        return operationalCertificate;
    }

    protected SSLContext makeSSLContext() throws TLSException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance(SCNetworkUtils.DEFAULT_CERTIFICATE_TYPE);
            operationalCertificate = SCNetworkUtils.generateCertificate(cf, operationalCertificateBytes);
            var caCerts = Stream.of(
                    SCNetworkUtils.generateCertificate(cf, issuerCertificate1Bytes),
                    SCNetworkUtils.generateCertificate(cf, issuerCertificate2Bytes)
            ).filter(Objects::nonNull).toArray(X509Certificate[]::new);

            KeyStore keystore = KeyStore.getInstance("JKS"); // Use PKCS12 instead?
            try {
                keystore.load(null);
            } catch (IOException e) {
                // Won't happen with null
            }
            keystore.setCertificateEntry("dev-cert", operationalCertificate);
            keystore.setKeyEntry("dev-key", privateKey, "".toCharArray(), new Certificate[] {operationalCertificate});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, "".toCharArray());
            KeyManager[] keyManagers = kmf.getKeyManagers();

            TrustManager[] trustManagers = new TrustManager[] {makeTrustManager(caCerts)};

            SSLContext ctx = SSLContext.getInstance(TLS_VERSION);
            ctx.init(keyManagers, trustManagers, null);
            return ctx;
        } catch (KeyStoreException e) {
            throw new TLSException(ErrorClass.device, ErrorCode.internalError, "Keystore error: %s", e);
        } catch (UnrecoverableKeyException e) {
            throw new TLSException(ErrorClass.device, ErrorCode.internalError, "KeyManagerFactory error: %s", e);
        } catch (KeyManagementException e) {
            throw new TLSException(ErrorClass.device, ErrorCode.internalError, "SSLContext.init error: %s", e);
        } catch (CertificateException e) {
            throw new TLSException(ErrorClass.property, ErrorCode.invalidConfigurationData, "Bad certificate data: %s",
                    e);
        } catch (NoSuchAlgorithmException e) {
            throw new TLSException(ErrorClass.device, ErrorCode.internalError, "Algorithm not available: %s", e);
        }
    }

    private TrustManager makeTrustManager(X509Certificate[] caCerts) {
        return new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return caCerts;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                var principal = certs[0].getSubjectX500Principal().getName();
                LOG.info("TrustManager is validating client {}, signed with {}", principal, certs[0].getSigAlgName());
                validateAgainstCAs(certs[0], caCerts);
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                var principal = certs[0].getSubjectX500Principal();
                LOG.info("TrustManager is validating server {}, signed with {}", principal, certs[0].getSigAlgName());
                validateAgainstCAs(certs[0], caCerts);
            }
        };
    }

    private void validateAgainstCAs(X509Certificate cert, X509Certificate[] caCerts) throws CertificateException {
        List<String> errors = new ArrayList<>();
        for (X509Certificate caCert : caCerts) {
            try {
                cert.checkValidity();
                cert.verify(caCert.getPublicKey());
                LOG.info("TrustManager approves of signature by {}, signed with  {}",
                        caCert.getSubjectX500Principal().getName(), caCert.getSigAlgName());
                return;
            } catch (Exception e) {
                errors.add("{" + cert.getSubjectX500Principal().getName() + "}: " + e.getLocalizedMessage());
            }
        }
        if (LOG.isErrorEnabled()) {
            LOG.error("Certificate validation error: {}", String.join(", ", errors));
        }
        throw new CertificateException("Certificate not accepted");
    }


    public static class TLSException extends Exception {
        private final transient ErrorClass errorClass;
        private final transient ErrorCode errorCode;

        public TLSException(ErrorClass errorClass, ErrorCode errorCode, String message, Exception cause) {
            super(message.formatted(cause.getMessage()), cause);
            this.errorClass = errorClass;
            this.errorCode = errorCode;
        }

        public ErrorClass getErrorClass() {
            return errorClass;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }
    }
}
