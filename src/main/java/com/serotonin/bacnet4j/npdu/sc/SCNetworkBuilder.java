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

import java.security.KeyPair;
import java.util.UUID;

import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;

public class SCNetworkBuilder {
    private OctetString vmac;
    private OctetString uuid;
    private int localNetworkNumber = Address.LOCAL_NETWORK;
    private int apduLength = 61327; // Table 6-1
    private int maxBvlcLengthAccepted = 1600; // NPDU + 16 byte header + options budget
    private int maxNpduLengthAccepted = 1497;
    private String primaryHubUri = "";
    private String failoverHubUri = "";
    // Defaults per spec; SC_Minimum/Maximum_Reconnect_Time have ranges of 2..300 and 2..600 respectively, but
    // the spec specifies no defaults. The values below are reasonable starting points.
    private int minimumReconnectTime = 2;
    private int maximumReconnectTime = 30;
    // Recommended defaults per 12.56.84, 12.56.85, 12.56.86.
    private int connectWaitTimeout = 10;
    private int disconnectWaitTimeout = 10;
    private int heartbeatTimeout = 300;
    // The spec says that in the event that a heartbeat request is not ack'ed, the connection should be closed, but it
    // does not say how long the connection should wait for the ack. The wording suggests that the heartbeat timeout -
    // with recommended value of 300 seconds - should be used. But this means that it could take up to 10 minutes for
    // a dead connection to be closed. The value below defaults to the heartbeat timeout, but can be overridden as
    // needed to provide greater responsiveness.
    private int heartbeatAckTimeout = heartbeatTimeout;
    private KeyPair keyPair;
    private Integer operationalCertificateFileId;
    private Integer issuerCertificateFile1Id;
    private Integer issuerCertificateFile2Id;
    private Integer certificateSigningRequestFileId;
    private BackoffPolicy backoffPolicy = new ExponentialBackoff(1.5);

    public SCNetworkBuilder vmac(OctetString vmac) {
        this.vmac = vmac;
        return this;
    }

    public SCNetworkBuilder vmac(String vmac) {
        return vmac(OctetString.fromHex(vmac));
    }

    public SCNetworkBuilder uuid(UUID uuid) {
        return uuid(uuid.toString());
    }

    public SCNetworkBuilder uuid(String uuid) {
        return uuid(OctetString.fromHex(uuid.replace("-", "")));
    }

    public SCNetworkBuilder uuid(OctetString uuid) {
        this.uuid = uuid;
        return this;
    }

    public SCNetworkBuilder localNetworkNumber(int localNetworkNumber) {
        this.localNetworkNumber = localNetworkNumber;
        return this;
    }

    public SCNetworkBuilder apduLength(int apduLength) {
        this.apduLength = apduLength;
        return this;
    }

    public SCNetworkBuilder maxBvlcLengthAccepted(int maxBvlcLengthAccepted) {
        this.maxBvlcLengthAccepted = maxBvlcLengthAccepted;
        return this;
    }

    public SCNetworkBuilder maxNpduLengthAccepted(int maxNpduLengthAccepted) {
        this.maxNpduLengthAccepted = maxNpduLengthAccepted;
        return this;
    }

    public SCNetworkBuilder primaryHubUri(String primaryHubUri) {
        this.primaryHubUri = primaryHubUri;
        return this;
    }

    public SCNetworkBuilder failoverHubUri(String failoverHubUri) {
        this.failoverHubUri = failoverHubUri;
        return this;
    }

    public SCNetworkBuilder minimumReconnectTime(int minimumReconnectTime) {
        this.minimumReconnectTime = minimumReconnectTime;
        return this;
    }

    public SCNetworkBuilder maximumReconnectTime(int maximumReconnectTime) {
        this.maximumReconnectTime = maximumReconnectTime;
        return this;
    }

    public SCNetworkBuilder connectWaitTimeout(int connectWaitTimeout) {
        this.connectWaitTimeout = connectWaitTimeout;
        return this;
    }

    public SCNetworkBuilder disconnectWaitTimeout(int disconnectWaitTimeout) {
        this.disconnectWaitTimeout = disconnectWaitTimeout;
        return this;
    }

    public SCNetworkBuilder heartbeatTimeout(int heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
        return this;
    }

    public SCNetworkBuilder heartbeatAckTimeout(int heartbeatAckTimeout) {
        this.heartbeatAckTimeout = heartbeatAckTimeout;
        return this;
    }

    public SCNetworkBuilder keyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
        return this;
    }

    /**
     * @param operationalCertificateFileId the instance ID of the file object containing the content.
     */
    public SCNetworkBuilder operationalCertificateFileId(int operationalCertificateFileId) {
        this.operationalCertificateFileId = operationalCertificateFileId;
        return this;
    }

    /**
     * @param issuerCertificateFile1Id the instance ID of the file object containing the content.
     */
    public SCNetworkBuilder issuerCertificateFile1Id(int issuerCertificateFile1Id) {
        this.issuerCertificateFile1Id = issuerCertificateFile1Id;
        return this;
    }

    /**
     * @param issuerCertificateFile2Id the instance ID of the file object containing the content.
     */
    public SCNetworkBuilder issuerCertificateFile2Id(int issuerCertificateFile2Id) {
        this.issuerCertificateFile2Id = issuerCertificateFile2Id;
        return this;
    }

    /**
     * @param certificateSigningRequestFileId the instance ID of the file object containing the content.
     */
    public SCNetworkBuilder certificateSigningRequestFileId(int certificateSigningRequestFileId) {
        this.certificateSigningRequestFileId = certificateSigningRequestFileId;
        return this;
    }

    public SCNetworkBuilder backoffPolicy(BackoffPolicy backoffPolicy) {
        this.backoffPolicy = backoffPolicy;
        return this;
    }

    public SCNetwork build() {
        if (uuid == null) {
            throw new IllegalArgumentException("Device UUID is required for Secure Connect");
        } else if (uuid.getLength() != 16) {
            throw new IllegalArgumentException("Device UUID is required to have length 16 bytes");
        }

        if (vmac == null) {
            vmac = new OctetString(SCVmac.makeRandom().getBytes());
        } else if (vmac.equals(OctetString.fromHex("000000000000"))
                || vmac.equals(SCNetworkUtils.LOCAL_BROADCAST_VMAC.getOctetString())) {
            throw new IllegalArgumentException("invalid vmac");
        }

        if (minimumReconnectTime < 2 || minimumReconnectTime > 300) {
            throw new IllegalArgumentException("invalid minimum reconnect time");
        }
        if (maximumReconnectTime < 2 || maximumReconnectTime > 600) {
            throw new IllegalArgumentException("invalid maximum reconnect time");
        }
        if (connectWaitTimeout < 5 || connectWaitTimeout > 300) {
            throw new IllegalArgumentException("invalid connect wait timeout");
        }
        if (disconnectWaitTimeout < 5 || disconnectWaitTimeout > 300) {
            throw new IllegalArgumentException("invalid disconnect wait timeout");
        }
        if (heartbeatTimeout < 3 || heartbeatTimeout > 300) {
            throw new IllegalArgumentException("invalid disconnect wait timeout");
        }

        if (keyPair == null) {
            throw new IllegalArgumentException("keyPair is required");
        }
        if (operationalCertificateFileId == null) {
            throw new IllegalArgumentException("operationalCertificateFileId is required");
        }
        if (issuerCertificateFile1Id == null) {
            throw new IllegalArgumentException("issuerCertificateFile1Id is required");
        }
        if (issuerCertificateFile2Id == null) {
            throw new IllegalArgumentException("issuerCertificateFile2Id is required");
        }
        if (certificateSigningRequestFileId == null) {
            throw new IllegalArgumentException("certificateSigningRequestFileId is required");
        }
        return new SCNetwork(localNetworkNumber, vmac, uuid, apduLength, maxBvlcLengthAccepted, maxNpduLengthAccepted,
                primaryHubUri, failoverHubUri, minimumReconnectTime, maximumReconnectTime, connectWaitTimeout,
                disconnectWaitTimeout, heartbeatTimeout, heartbeatAckTimeout, keyPair, operationalCertificateFileId,
                issuerCertificateFile1Id, issuerCertificateFile2Id, certificateSigningRequestFileId, backoffPolicy);
    }
}
