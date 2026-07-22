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
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(SCNetwork.class);

    // Configuration
    private OctetString vmac;
    private final OctetString uuid;
    private final int apduLength;
    private final int maxBvlcLengthAccepted;
    private final int maxNpduLengthAccepted;
    private final String primaryHubUri;
    private final String failoverHubUri;
    // The SC timing properties are configurable at runtime (12.56): they are read from here at
    // each use, so writes to the network port object take effect immediately.
    private volatile int minimumReconnectTime;
    private volatile int maximumReconnectTime;
    private volatile int connectWaitTimeout;
    private volatile int disconnectWaitTimeout;
    private volatile int heartbeatTimeout;
    private final int heartbeatAckTimeout;
    private final SCKeyPairHandler keyPairHandler;
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
    private final CopyOnWriteArrayList<SCHubConnectionListener> hubConnectionListeners = new CopyOnWriteArrayList<>();

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
            SCKeyPairHandler keyPairHandler,
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
        this.keyPairHandler = keyPairHandler;
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
        return new SCNetworkIdentifier(uuid);
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

    public void setMinimumReconnectTime(int minimumReconnectTime) {
        this.minimumReconnectTime = minimumReconnectTime;
        backoffPolicy.configure(minimumReconnectTime, maximumReconnectTime);
    }

    public UnsignedInteger getMaximumReconnectTime() {
        return new UnsignedInteger(maximumReconnectTime);
    }

    public void setMaximumReconnectTime(int maximumReconnectTime) {
        this.maximumReconnectTime = maximumReconnectTime;
        backoffPolicy.configure(minimumReconnectTime, maximumReconnectTime);
    }

    public UnsignedInteger getConnectWaitTimeout() {
        return new UnsignedInteger(connectWaitTimeout);
    }

    public void setConnectWaitTimeout(int connectWaitTimeout) {
        this.connectWaitTimeout = connectWaitTimeout;
    }

    public UnsignedInteger getDisconnectWaitTimeout() {
        return new UnsignedInteger(disconnectWaitTimeout);
    }

    public void setDisconnectWaitTimeout(int disconnectWaitTimeout) {
        this.disconnectWaitTimeout = disconnectWaitTimeout;
    }

    public UnsignedInteger getHeartbeatTimeout() {
        return new UnsignedInteger(heartbeatTimeout);
    }

    public void setHeartbeatTimeout(int heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
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
     * Adds a listener that is notified when the hub connector state changes. Unlike other network types,
     * an SC network is not usable when {@code LocalDevice.initialize()} returns: the hub connection is
     * established asynchronously, and until then outgoing messages are dropped. See
     * {@link SCHubConnectionListener} for the threading contract.
     */
    public void addHubConnectionListener(SCHubConnectionListener listener) {
        hubConnectionListeners.add(listener);
    }

    public void removeHubConnectionListener(SCHubConnectionListener listener) {
        hubConnectionListeners.remove(listener);
    }

    /**
     * Returns true if the hub connector is connected to the primary or the failover hub, i.e. the network
     * can currently send and receive messages.
     */
    public boolean isHubConnected() {
        return getHubConnectorState() != SCHubConnectorState.noHubConnection;
    }

    /**
     * Returns a future that completes with the hub connector state when a hub connection is established,
     * or immediately if one already is. Intended for startup sequencing: initialize the local device, then
     * wait on this future before starting operations that require the network.
     * <p>
     * The future completes on the local device's executor thread; chain substantial work with the async
     * continuation methods. It never completes exceptionally and does not time out by itself — callers that
     * cannot wait indefinitely (e.g. because the hub may be unreachable) should apply
     * {@link CompletableFuture#orTimeout}. Termination of the network does not complete the future.
     */
    public CompletableFuture<SCHubConnectorState> whenHubConnected() {
        CompletableFuture<SCHubConnectorState> future = new CompletableFuture<>();
        SCHubConnectionListener listener = (oldState, newState) -> {
            if (newState != SCHubConnectorState.noHubConnection) {
                future.complete(newState);
            }
        };
        addHubConnectionListener(listener);
        // Check the current state after registering the listener so that a connection established between
        // the check and the registration cannot be missed.
        SCHubConnectorState state = getHubConnectorState();
        if (state != SCHubConnectorState.noHubConnection) {
            future.complete(state);
        }
        future.whenComplete((s, e) -> removeHubConnectionListener(listener));
        return future;
    }

    /**
     * Called by the hub connector when its state changes. Listener exceptions are logged and do not
     * affect other listeners or the connector.
     */
    void fireHubConnectionStateChanged(SCHubConnectorState oldState, SCHubConnectorState newState) {
        for (SCHubConnectionListener listener : hubConnectionListeners) {
            try {
                listener.hubConnectionStateChanged(oldState, newState);
            } catch (Exception e) {
                LOG.error("Error in hub connection listener", e);
            }
        }
    }

    /**
     * Returns the connection status describing why this network failed to initialize, or null if it initialized
     * successfully. Initialization failures - e.g. TLS configuration problems or invalid hub URIs - are recorded
     * here rather than thrown from {@link #initialize(Transport)}. Only meaningful after initialization.
     */
    public SCHubConnection getInitializationError() {
        return node == null ? initializationError : null;
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

        // Select the internal private key that matches the operational certificate. The handler may hold a
        // pending pair from an outstanding CSR in addition to the active pair; the certificate determines
        // which pair is effective for the port.
        X509Certificate opCert;
        try {
            CertificateFactory cf = CertificateFactory.getInstance(SCNetworkUtils.DEFAULT_CERTIFICATE_TYPE);
            opCert = SCNetworkUtils.generateCertificate(cf, operationalCertificate);
        } catch (CertificateException e) {
            throw new SCTLSManager.TLSException(ErrorClass.security, ErrorCode.certificateMalformed,
                    "Operational certificate encoding error: %s", e);
        }
        PrivateKey privateKey = keyPairHandler.privateKeyFor(opCert);
        if (privateKey == null) {
            throw new SCTLSManager.TLSException(ErrorClass.security, ErrorCode.unknownCertificateKey,
                    "Operational certificate does not match a private key", null);
        }

        var tls = new SCTLSManager(privateKey, operationalCertificate, issuerCertificate1, issuerCertificate2);
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
