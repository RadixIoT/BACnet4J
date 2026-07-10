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
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.NetworkIdentifier;
import com.serotonin.bacnet4j.npdu.sc.msg.SCBVLC;
import com.serotonin.bacnet4j.obj.FileObject;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.SCHubConnection;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * How to use:
 * 1) Provide a mechanism for reinitializing this network when pending changes are saved. See NetworkAlteringTest.
 * 2) Create {@link FileObject} instances to store the certificate and CSR data.
 * 3) Provide a mechanism for generating a CSR. See SCCsrGenerationTest.
 */
public class SCNetwork extends Network {
    // Configuration
    private OctetString vmac;
    private final OctetString uuid;
    private final int apduLength;
    private final int maxBvlcLengthAccepted;
    private final int maxNpduLengthAccepted;
    private final String primaryHubUri;
    private final String failoverHubUri;
    private final int minimumReconnectTime;
    private final int maximumReconnectTime;
    private final int connectWaitTimeout;
    private final int disconnectWaitTimeout;
    private final int heartbeatTimeout;
    private final int heartbeatAckTimeout;
    private final KeyPair keyPair;
    private final ObjectIdentifier operationalCertificateFileId;
    private final ObjectIdentifier issuerCertificateFile1Id;
    private final ObjectIdentifier issuerCertificateFile2Id;
    private final ObjectIdentifier certificateSigningRequestFileId;
    private final BackoffPolicy backoffPolicy;

    // Evaluated
    private SSLContext sslContext;
    private URI primaryHub;
    private URI failoverHub;
    private SCHubConnection initializationError =
            new SCHubConnection(SCConnectionState.notConnected, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED);
    private SCNode node;

    private long bytesOut;
    private long bytesIn;

    SCNetwork(
            int localNetworkNumber,
            OctetString vmac,
            OctetString uuid,
            int apduLength,
            int maxBvlcLengthAccepted,
            int maxNpduLengthAccepted,
            String primaryHubUri,
            String failoverHubUri,
            int minimumReconnectTime,
            int maximumReconnectTime,
            int connectWaitTimeout,
            int disconnectWaitTimeout,
            int heartbeatTimeout,
            int heartbeatAckTimeout,
            KeyPair keyPair,
            Integer operationalCertificateFileId,
            Integer issuerCertificateFile1Id,
            Integer issuerCertificateFile2Id,
            Integer certificateSigningRequestFileId,
            BackoffPolicy backoffPolicy) {
        super(localNetworkNumber);

        this.uuid = uuid;
        this.vmac = vmac;
        this.apduLength = apduLength;
        this.maxBvlcLengthAccepted = maxBvlcLengthAccepted;
        this.maxNpduLengthAccepted = maxNpduLengthAccepted;
        this.primaryHubUri = primaryHubUri;
        this.failoverHubUri = failoverHubUri;
        this.minimumReconnectTime = minimumReconnectTime;
        this.maximumReconnectTime = maximumReconnectTime;
        this.connectWaitTimeout = connectWaitTimeout;
        this.disconnectWaitTimeout = disconnectWaitTimeout;
        this.heartbeatTimeout = heartbeatTimeout;
        this.heartbeatAckTimeout = heartbeatAckTimeout;
        this.keyPair = keyPair;
        this.operationalCertificateFileId = new ObjectIdentifier(ObjectType.file, operationalCertificateFileId);
        this.issuerCertificateFile1Id = new ObjectIdentifier(ObjectType.file, issuerCertificateFile1Id);
        this.issuerCertificateFile2Id = new ObjectIdentifier(ObjectType.file, issuerCertificateFile2Id);
        this.certificateSigningRequestFileId = new ObjectIdentifier(ObjectType.file, certificateSigningRequestFileId);
        this.backoffPolicy = backoffPolicy;

        backoffPolicy.configure(minimumReconnectTime, maximumReconnectTime);
    }

    @Override
    public long getBytesOut() {
        return bytesOut;
    }

    @Override
    public long getBytesIn() {
        return bytesIn;
    }

    @Override
    public NetworkIdentifier getNetworkIdentifier() {
        return new SCNetworkIdentifier(vmac);
    }

    @Override
    public MaxApduLength getMaxApduLength() {
        return MaxApduLength.UP_TO_1476;
    }

    @Override
    protected OctetString getBroadcastMAC() {
        return new OctetString(SCNetworkUtils.LOCAL_BROADCAST_VMAC.getBytes());
    }

    @Override
    public Address[] getAllLocalAddresses() {
        return new Address[0];
    }

    @Override
    public Address getLoopbackAddress() {
        return null;
    }

    public OctetString getDeviceUUID() {
        return uuid;
    }

    public OctetString getVmac() {
        return vmac;
    }

    void setVmac(OctetString vmac) {
        this.vmac = vmac;
    }

    public UnsignedInteger getApduLength() {
        return new UnsignedInteger(apduLength);
    }

    public UnsignedInteger getMaxBvlcLengthAccepted() {
        return new UnsignedInteger(maxBvlcLengthAccepted);
    }

    public UnsignedInteger getMaxNpduLengthAccepted() {
        return new UnsignedInteger(maxNpduLengthAccepted);
    }

    public CharacterString getPrimaryHubUri() {
        return new CharacterString(primaryHubUri);
    }

    public CharacterString getFailoverHubUri() {
        return new CharacterString(failoverHubUri);
    }

    public UnsignedInteger getMinimumReconnectTime() {
        return new UnsignedInteger(minimumReconnectTime);
    }

    public UnsignedInteger getMaximumReconnectTime() {
        return new UnsignedInteger(maximumReconnectTime);
    }

    public UnsignedInteger getConnectWaitTimeout() {
        return new UnsignedInteger(connectWaitTimeout);
    }

    public UnsignedInteger getDisconnectWaitTimeout() {
        return new UnsignedInteger(disconnectWaitTimeout);
    }

    public UnsignedInteger getHeartbeatTimeout() {
        return new UnsignedInteger(heartbeatTimeout);
    }

    // Not a spec value; does not appear in the network port object.
    int getHeartbeatAckTimeout() {
        return heartbeatAckTimeout;
    }

    public ObjectIdentifier getOperationalCertificateFileId() {
        return operationalCertificateFileId;
    }

    public BACnetArray<ObjectIdentifier> getIssuerCertificateFileIds() {
        return new BACnetArray<>(issuerCertificateFile1Id, issuerCertificateFile2Id);
    }

    public ObjectIdentifier getCertificateSigningRequestFileId() {
        return certificateSigningRequestFileId;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public URI getPrimaryHub() {
        return primaryHub;
    }

    public URI getFailoverHub() {
        return failoverHub;
    }

    public BackoffPolicy getBackoffPolicy() {
        return backoffPolicy;
    }

    public SCHubConnectorState getHubConnectorState() {
        return node == null ? SCHubConnectorState.noHubConnection : node.getHubConnectorState();
    }

    public SCHubConnection getPrimaryHubConnectionStatus() {
        return node == null ? initializationError : node.getPrimaryHubConnectionStatus();
    }

    public SCHubConnection getFailoverHubConnectionStatus() {
        return node == null ? initializationError : node.getFailoverHubConnectionStatus();
    }

    /**
     * Returns true if the public key in the given certificate equals that in the key pair held by this network. Used
     * for clause 12.56.100's "operational certificate failed to validate against an internal private key" check.
     */
    public boolean matchesPublicKey(X509Certificate cert) {
        return Arrays.equals(cert.getPublicKey().getEncoded(), keyPair.getPublic().getEncoded());
    }

    @Override
    public void initialize(Transport transport) throws BACnetException {
        super.initialize(transport);

        initializationError =
                new SCHubConnection(SCConnectionState.notConnected, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED);

        LocalDevice localDevice = transport.getLocalDevice();
        localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.deviceUuid, uuid);

        // Ensure the URIs start with "wss" and are valid URIs
        try {
            primaryHub = validateURI(primaryHubUri, "primaryHubUri");
            failoverHub = validateURI(failoverHubUri, "failoverHubUri");
        } catch (IllegalArgumentException e) {
            initializationError(ErrorClass.property, ErrorCode.valueOutOfRange, e.getMessage());
            return;
        }

        try {
            initializeTLS(localDevice);
            node = new SCNode(this);
            node.configure(getTransport());
            node.initialize();
        } catch (BACnetServiceException | IOException e) {
            initializationError(ErrorClass.device, ErrorCode.internalError, e.getMessage());
        } catch (IllegalArgumentException e) {
            initializationError(ErrorClass.property, ErrorCode.valueOutOfRange, e.getMessage());
        } catch (SCTLSManager.TLSException e) {
            initializationError(e.getErrorClass(), e.getErrorCode(), e.getMessage());
        }
    }

    private URI validateURI(String uri, String type) {
        if (!StringUtils.isBlank(uri)) {
            try {
                return URI.create(uri);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(type + " is not a valid URI");
            }
        }
        return null;
    }

    protected void initializationError(ErrorClass errorClass, ErrorCode errorCode, String errorMessage) {
        initializationError = new SCHubConnection(
                SCConnectionState.failedToConnect,
                new DateTime(getTransport().getLocalDevice()),
                DateTime.UNSPECIFIED,
                new ErrorClassAndCode(errorClass, errorCode),
                new CharacterString(errorMessage));
    }

    protected void initializeTLS(LocalDevice localDevice)
            throws BACnetServiceException, IOException, SCTLSManager.TLSException {
        // Ensure references to the files exist.
        var operationalCertificate =
                getFileContent(localDevice, operationalCertificateFileId, "operationalCertificateFile");
        if (operationalCertificate.length == 0) {
            throw new IllegalArgumentException("Operational certificate file object is empty");
        }

        var issuerCertificate1 = getFileContent(localDevice, issuerCertificateFile1Id, "issuerCertificateFile1");
        var issuerCertificate2 = getFileContent(localDevice, issuerCertificateFile2Id, "issuerCertificateFile2");
        if (issuerCertificate1.length == 0 && issuerCertificate2.length == 0) {
            throw new IllegalArgumentException("Both issuer certificates file objects cannot be empty");
        }

        var tls = new SCTLSManager(keyPair.getPrivate(), operationalCertificate, issuerCertificate1,
                issuerCertificate2);
        sslContext = tls.getSSLContext();
    }

    private byte[] getFileContent(LocalDevice localDevice, ObjectIdentifier fileId, String propName)
            throws BACnetServiceException, IOException {
        var file = localDevice.getObject(fileId);
        if (file == null) {
            throw new IllegalArgumentException(propName + " file object not found in local device");
        }
        if (file instanceof FileObject fo) {
            var access = fo.getFileAccess();
            if (!access.supportsStreamAccess()) {
                throw new IllegalArgumentException(propName + " file object must have stream access");
            }
            if (!new CharacterString("pem").equals(fo.get(PropertyIdentifier.fileType))) {
                throw new IllegalArgumentException(
                        propName + " file object has unsupported file type: " + fo.get(PropertyIdentifier.fileType));
            }
            return access.readData(0, access.length()).getBytes();
        }
        throw new IllegalArgumentException(propName + " does not reference a file object");
    }

    @Override
    public void terminate() {
        if (node != null) {
            node.terminate();
        }
    }

    @Override
    public void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast, boolean expectsReply)
            throws BACnetException {
        if (node != null) {
            SCVmac dest = broadcast ? SCNetworkUtils.LOCAL_BROADCAST_VMAC
                    : new SCVmac(recipient.getMacAddress().getBytes());
            var message = new SCBVLC(null, dest, SCBVLC.ENCAPSULATED_NPDU, npdu.popAll());
            node.sendMessage(message);
        }
    }

    void onIncoming(SCBVLC message) {
        var sender = message.getOriginating();
        handleIncomingData(new ByteQueue(message.getPayload()),
                new OctetString(sender == null ? new byte[0] : sender.getBytes()));
    }

    @Override
    protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws BACnetException {
        return parseNpduData(queue, linkService);
    }
}
