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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.sc.SCHubConnectionListener;
import com.serotonin.bacnet4j.npdu.sc.SCKeyPairHandler;
import com.serotonin.bacnet4j.npdu.sc.SCNetwork;
import com.serotonin.bacnet4j.npdu.sc.SCNetworkUtils;
import com.serotonin.bacnet4j.obj.fileAccess.FileStreamAccess;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Health;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyValue;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Network Port object for a BACnet/SC port (Clauses 12.56 and Annex AB).
 *
 * <p>The properties in Table 12-71.8 not initialized here are either optional (and left to product builders) or
 * dynamic state owned by the BACnet/SC stack (e.g., {@code SC_Hub_Connector_State}, hub connection status lists,
 * failed connection requests).
 *
 * <p>The certificate File objects referenced by this port must use {@code FileStreamAccess}: the pending-changes
 * mechanism keeps backup copies of the certificate files on disk beside the originals so that unactivated changes
 * can be discarded and do not survive a restart.
 */
public class SecureConnectNetworkPortObject extends NetworkPortObject {
    static final Logger LOG = LoggerFactory.getLogger(SecureConnectNetworkPortObject.class);

    private final SCNetwork network;
    private final SCKeyPairHandler keyPairHandler;

    private SCHubConnectionListener hubConnectionListener;

    public SecureConnectNetworkPortObject(LocalDevice localDevice, SCNetwork network,
            SCKeyPairHandler keyPairHandler, int instanceNumber) {
        super(localDevice, instanceNumber, network.getNetworkIdentifier().getIdString(),
                false, NetworkType.secureConnect, ProtocolLevel.bacnetApplication, Set.of(
                        PropertyIdentifier.scHubFunctionEnable,
                        PropertyIdentifier.scHubFunctionAcceptUris,
                        PropertyIdentifier.scHubFunctionBinding,
                        PropertyIdentifier.scDirectConnectInitiateEnable,
                        PropertyIdentifier.scDirectConnectAcceptEnable,
                        PropertyIdentifier.scDirectConnectAcceptUris,
                        PropertyIdentifier.scDirectConnectBinding
                ));

        // There are things this port may need to do before the network initializes, so don't allow this state.
        if (network.isInitialized()) {
            throw new IllegalStateException("Network already initialized");
        }
        this.network = network;
        this.keyPairHandler = Objects.requireNonNull(keyPairHandler, "keyPairHandler is required");

        writePropertyInternal(PropertyIdentifier.apduLength, network.getApduLength());
        writePropertyInternal(PropertyIdentifier.macAddress, network.getVmac());
        writePropertyInternal(PropertyIdentifier.maxBvlcLengthAccepted, network.getMaxBvlcLengthAccepted());
        writePropertyInternal(PropertyIdentifier.maxNpduLengthAccepted, network.getMaxNpduLengthAccepted());

        writePropertyInternal(PropertyIdentifier.scPrimaryHubUri, network.getPrimaryHubUri());
        writePropertyInternal(PropertyIdentifier.scFailoverHubUri, network.getFailoverHubUri());
        writePropertyInternal(PropertyIdentifier.scMinimumReconnectTime, network.getMinimumReconnectTime());
        writePropertyInternal(PropertyIdentifier.scMaximumReconnectTime, network.getMaximumReconnectTime());
        writePropertyInternal(PropertyIdentifier.scConnectWaitTimeout, network.getConnectWaitTimeout());
        writePropertyInternal(PropertyIdentifier.scDisconnectWaitTimeout, network.getDisconnectWaitTimeout());
        writePropertyInternal(PropertyIdentifier.scHeartbeatTimeout, network.getHeartbeatTimeout());

        writePropertyInternal(PropertyIdentifier.scHubConnectorState, network.getHubConnectorState());
        writePropertyInternal(PropertyIdentifier.scPrimaryHubConnectionStatus, network.getPrimaryHubConnectionStatus());
        writePropertyInternal(PropertyIdentifier.scFailoverHubConnectionStatus,
                network.getFailoverHubConnectionStatus());
        writePropertyInternal(PropertyIdentifier.scHubFunctionEnable, Boolean.FALSE);
        writePropertyInternal(PropertyIdentifier.scDirectConnectInitiateEnable, Boolean.FALSE);
        writePropertyInternal(PropertyIdentifier.scDirectConnectAcceptEnable, Boolean.FALSE);

        writePropertyInternal(PropertyIdentifier.operationalCertificateFile, network.getOperationalCertificateFileId());
        writePropertyInternal(PropertyIdentifier.issuerCertificateFiles, network.getIssuerCertificateFileIds());
        writePropertyInternal(PropertyIdentifier.certificateSigningRequestFile,
                network.getCertificateSigningRequestFileId());
    }

    @Override
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
        PropertyIdentifier pid = value.getPropertyIdentifier();
        if (pid.equals(PropertyIdentifier.scPrimaryHubUri) || pid.equals(PropertyIdentifier.scFailoverHubUri)) {
            CharacterString cs = value.getValue();
            String s = cs.getValue();
            if (!s.isEmpty()) {
                try {
                    new URI(s);
                } catch (URISyntaxException e) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
                }
            }
        } else if (pid.equals(PropertyIdentifier.scMinimumReconnectTime)) {
            validateUnsignedRange(value.getValue(), 2, 300);
        } else if (pid.equals(PropertyIdentifier.scMaximumReconnectTime)) {
            validateUnsignedRange(value.getValue(), 2, 600);
        } else if (pid.equals(PropertyIdentifier.scConnectWaitTimeout)) {
            validateUnsignedRange(value.getValue(), 5, 300);
        } else if (pid.equals(PropertyIdentifier.scDisconnectWaitTimeout)) {
            validateUnsignedRange(value.getValue(), 5, 300);
        } else if (pid.equals(PropertyIdentifier.scHeartbeatTimeout)) {
            validateUnsignedRange(value.getValue(), 3, 300);
        }
        return false;
    }

    @Override
    protected void beforeReadProperty(PropertyIdentifier pid) throws BACnetServiceException {
        super.beforeReadProperty(pid);
        if (pid.equals(PropertyIdentifier.macAddress)) {
            // The node re-randomizes its VMAC when the hub reports a duplicate (AB.6.2.2), so the property
            // is refreshed from the network on read.
            OctetString vmac = network.getVmac();
            if (!vmac.equals(get(PropertyIdentifier.macAddress))) {
                writePropertyInternal(PropertyIdentifier.macAddress, vmac);
            }
        } else if (pid.equals(PropertyIdentifier.currentHealth)) {
            writePropertyInternal(PropertyIdentifier.currentHealth, evaluateHealth());
        }
    }

    /**
     * Evaluates Current_Health per 12.56.17: the most recent error for this port. A network initialization
     * error (e.g. an invalid hub URI or a TLS configuration problem) takes precedence. Otherwise, when there
     * is no hub connection, the most recent connection error is reported: the primary connection's error
     * first since the connector always retries the primary before the failover, then the failover's.
     * Otherwise, the port is healthy.
     */
    protected Health evaluateHealth() {
        var initError = network.getInitializationError();
        if (initError != null) {
            return new Health(new DateTime(getLocalDevice()), initError.getError(), null,
                    initError.getErrorDetails());
        }
        if (!network.isHubConnected()) {
            for (var status : List.of(network.getPrimaryHubConnectionStatus(),
                    network.getFailoverHubConnectionStatus())) {
                if (status.getError() != null) {
                    return new Health(new DateTime(getLocalDevice()), status.getError(), null,
                            status.getErrorDetails());
                }
            }
        }
        return new Health(new DateTime(getLocalDevice()),
                new ErrorClassAndCode(ErrorClass.object, ErrorCode.success), null, null);
    }

    /**
     * A network initialization failure means the port configuration could not be applied, which is a fault.
     * Connection-level failures are not faults: they are transient, the connector retries them, and they are
     * visible in Current_Health and the connection status properties.
     */
    @Override
    protected Reliability evaluateReliability() {
        return network.getInitializationError() == null ? Reliability.noFaultDetected
                : Reliability.configurationError;
    }

    /**
     * SC ports support the GENERATE_CSR_FILE command per clause 12.56.102. The base class marks
     * this command as unsupported by default; subclasses that provide a real
     * {@link #generateCsrFile()} implementation opt in here.
     */
    @Override
    protected void validateCommandInternal(NetworkPortCommand command) throws BACnetServiceException {
        if (command == NetworkPortCommand.generateCsrFile) {
            return;
        }
        super.validateCommandInternal(command);
    }

    /**
     * Dispatches the GENERATE_CSR_FILE command to the {@link #generateCsrFile()} hook. Other
     * commands fall through to the base class handling. Writes to the SC timing properties are
     * propagated to the running network: they are configurable per 12.56 but are not part of the
     * pending changes mechanism, so they take effect immediately.
     */
    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);
        if (pid.equals(PropertyIdentifier.command) && newValue == NetworkPortCommand.generateCsrFile) {
            executeCommand(this::generateCsrFile);
        } else if (pid.equals(PropertyIdentifier.scMinimumReconnectTime)) {
            network.setMinimumReconnectTime(((UnsignedInteger) newValue).intValue());
        } else if (pid.equals(PropertyIdentifier.scMaximumReconnectTime)) {
            network.setMaximumReconnectTime(((UnsignedInteger) newValue).intValue());
        } else if (pid.equals(PropertyIdentifier.scConnectWaitTimeout)) {
            network.setConnectWaitTimeout(((UnsignedInteger) newValue).intValue());
        } else if (pid.equals(PropertyIdentifier.scDisconnectWaitTimeout)) {
            network.setDisconnectWaitTimeout(((UnsignedInteger) newValue).intValue());
        } else if (pid.equals(PropertyIdentifier.scHeartbeatTimeout)) {
            network.setHeartbeatTimeout(((UnsignedInteger) newValue).intValue());
        }
    }

    /**
     * Extension point for CSR generation. Called on the local device's executor when the client
     * writes GENERATE_CSR_FILE to the Command property. The default implementation does nothing;
     * product developers subclass this class and override this method to:
     * <ol>
     *   <li>Generate a new private/public key pair and register it with
     *       {@code getKeyPairHandler().setPendingKeyPair(keyPair)}. The handler retains it (alongside the
     *       active pair, which stays in use) until the operational certificate created from the CSR is
     *       written and activated, at which point {@link #applyPendingChanges()} notifies the handler and
     *       the pending pair is promoted to active.</li>
     *   <li>Build a PKCS #10 certificate signing request (RFC 5967) signed by the new private key.</li>
     *   <li>Write the PEM-encoded CSR into the File object referenced by
     *       {@code Certificate_Signing_Request_File}.</li>
     * </ol>
     * <p>
     * BACnet4J does not embed a CSR builder because that would require a mandatory dependency on
     * BouncyCastle (or equivalent). See {@code SCCsrGenerationTest} for a reference implementation.
     */
    protected void generateCsrFile() {
        // Default no-op — subclasses override with their CSR generation logic.
    }

    /**
     * The handler that manages this port's active and pending key pairs. Subclasses use this in their
     * {@link #generateCsrFile()} implementation to register the newly generated pending pair.
     */
    protected SCKeyPairHandler getKeyPairHandler() {
        return keyPairHandler;
    }

    protected void validateUnsignedRange(UnsignedInteger value, int min, int max) throws BACnetServiceException {
        var i = value.intValue();
        if (i < min || i > max) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        }
    }

    private DeviceEventAdapter certFileChangeListener;

    @Override
    protected void initializeImpl() {
        super.initializeImpl();

        certFileChangeListener = new DeviceEventAdapter() {
            final List<ObjectIdentifier> certIds;

            {
                var issuerCerts = network.getIssuerCertificateFileIds();
                certIds = List.of(network.getOperationalCertificateFileId(),
                        issuerCerts.get(0), issuerCerts.get(1));
            }

            /**
             * When the contents of a cert file used by this port are changed, we need to indicate this by tracking the
             * pending change. See 12.56.100 and 12.56.101. This is a significant PITA, because these files don't neatly
             * fit into the pending changes framework used by the base class. The requirement for pending changes to be
             * discardable means that we need to keep a backup of the original file contents too, which is something
             * that FileObject does not handle for us.
             * <p>
             * So, this is how it works:
             * - we detect atomic write file requests, and write property [multiple] requests that change the file size
             *   property, when these requests are destined for the ids of the 3 cert files.
             * - if a change is made, we make a copy of the original file using ".pendingChangesBackup" for the
             *   extension. We also add the file's object id to the changedCertFiles set. The set is deliberately
             *   separate from the base class's pending-changes map: that map holds pending property *values*
             *   which get() substitutes on read and which products persist, whereas these are just markers - the
             *   pending data is the file content itself. isChanged() is overridden to include the set so that
             *   Changes_Pending and the command gating still see cert file changes.
             * - the changes are called "pending", but this is only from the perspective of the network port object.
             *   we allow requests to continue as normal because the changes they make need to be reflected in, e.g.,
             *   subsequent read requests.
             * - subseqent write file requests may change the file back to its original content. If this happens we
             *   remove the backup file and the file's entry in the set.
             * - note that the code does not attempt to detect reversions of file size, e.g. changing the file size to
             *   some other value and then changing it back again. If a file has been marked as changed, no change to
             *   its file size property will not remove that mark.
             * - if pending changes are discarded, we restore the backup file to be the current file and clear the
             *   pending changes markers.
             * - if the changes are to be applied with the suitable "reinitialize device" request, the product's
             *   reinitialize device handling calls applyPendingChanges(), which deletes the backup files.
             * - if the device is restarted while the changes are still pending, the initialization needs to restore
             *   the backup files to be the current files. This is why the backup files can't just be temp files; we
             *   need to be able to find them at initialization.
             * - actual files may not exist, in which case the file object reports them as having a length of 0. For the
             *   purpose of backup files, we still create a 0-length file in any case.
             * - file references may actually be directories, or reference directories that don't exist. This is
             *   considered a configuration problem and not handled here.
             * <p>
             *  Hopefully this explains the code below, which is distressingly complicated for the seemingly simple
             *  problem is it solving.
             * <p>
             *  Note: AB.7.4.1.3 says "If the effective operational certificate of an active connection is changed, the
             *  connection shall be re-established." If the operational certificate changes using the mechanisms below,
             *  the pending changes process requires a restart of the device anyway, and so all connections will be
             *  recreated, satisfying the requirement. If the certificate changes by any other means (e.g. a
             *  configuration tool), it is the responsibility of the product developer to ensure that the above
             *  requirement is satisfied.
             *
             * @param from    receiver of service
             * @param service the service received
             */
            @Override
            public void requestReceived(Address from, Service service) {
                // Watch for file writes target the operational or issuer certs.
                if (service instanceof AtomicWriteFileRequest awf && certIds.contains(awf.getFileIdentifier())) {
                    handleFileWrite(awf);
                }

                // And also watch for property writes. Flatten requests into a list of object property values.
                List<ObjectPropertyValue> opvs = null;
                if (service instanceof WritePropertyRequest wp) {
                    opvs = List.of(new ObjectPropertyValue(
                            wp.getObjectIdentifier(),
                            wp.getPropertyIdentifier(),
                            wp.getPropertyArrayIndex(),
                            wp.getPropertyValue(),
                            wp.getPriority()));
                } else if (service instanceof WritePropertyMultipleRequest wpm) {
                    opvs = wpm.getListOfWriteAccessSpecifications().stream()
                            .flatMap(was -> was.getListOfProperties().stream()
                                    .map(pv -> new ObjectPropertyValue(
                                            was.getObjectIdentifier(),
                                            pv.getPropertyIdentifier(),
                                            pv.getPropertyArrayIndex(),
                                            pv.getValue(),
                                            pv.getPriority())))
                            .toList();
                }
                if (opvs != null) {
                    opvs.stream().filter(opv -> certIds.contains(opv.getObjectIdentifier())
                            && PropertyIdentifier.fileSize.equals(opv.getPropertyIdentifier())
                    ).forEach(opv -> handleFileSizeChange(opv.getObjectIdentifier()));
                }
            }
        };
        getLocalDevice().getEventHandler().addListener(certFileChangeListener);

        // Keep SC_Hub_Connector_State and the hub connection status properties current as the hub connector
        // transitions, so that reads and COV-style monitoring of this object see live values.
        hubConnectionListener = (oldState, newState) -> {
            writePropertyInternal(PropertyIdentifier.scHubConnectorState, newState);
            writePropertyInternal(PropertyIdentifier.scPrimaryHubConnectionStatus,
                    network.getPrimaryHubConnectionStatus());
            writePropertyInternal(PropertyIdentifier.scFailoverHubConnectionStatus,
                    network.getFailoverHubConnectionStatus());
        };
        network.addHubConnectionListener(hubConnectionListener);

        // Check if there are backup versions of the cert files, and if so make them the current files.
        reinstateBackupFiles();
    }

    @Override
    protected void terminateImpl() {
        if (hubConnectionListener != null) {
            network.removeHubConnectionListener(hubConnectionListener);
        }
        if (certFileChangeListener != null) {
            getLocalDevice().getEventHandler().removeListener(certFileChangeListener);
        }
        super.terminateImpl();
    }

    protected static final String BACKUP_EXTENSION = ".pendingChangesBackup";

    /**
     * The ids of the certificate File objects that have pending (unactivated) content changes, i.e. that have
     * a backup of their original content on disk. Deliberately not kept in the base class's pending-changes
     * map: these are markers, not property values — the pending data is the file content itself. Storing them
     * under the real property ids would make {@link #get} return the marker for reads of the certificate file
     * properties, and would expose them to product persistence code iterating {@link #getPendingChanges()}.
     */
    private final Set<ObjectIdentifier> changedCertFiles = ConcurrentHashMap.newKeySet();

    /**
     * Returns the ids of the certificate File objects that currently have pending content changes.
     */
    public Set<ObjectIdentifier> getChangedCertificateFiles() {
        return Set.copyOf(changedCertFiles);
    }

    /**
     * Certificate file content changes count as pending changes (12.56.100/101) even though they are tracked
     * outside the base class's pending-changes map.
     */
    @Override
    public boolean isChanged() {
        return super.isChanged() || !changedCertFiles.isEmpty();
    }

    @Override
    protected void discardChanges() {
        super.discardChanges();
        // Reinstate the backup files and clear the markers; the super class only clears its own map.
        // The pending key pair in the key pair handler is deliberately left alone: GENERATE_CSR_FILE does not
        // affect Changes_Pending (12.56.16), and the CSR created with that pair may still be at the signing CA.
        reinstateBackupFiles();
        changedCertFiles.clear();
    }

    /**
     * Commits pending certificate file changes by deleting the backup files, so that the initialization after the
     * coming restart does not revert the certificate files to their original content. The new certificate content
     * is already in the files referenced by the File objects, so unlike ordinary pending property values it does
     * not need to be copied to the product's configuration store. The note on the base method about storing the
     * other pending property values still applies.
     */
    @Override
    public void applyPendingChanges() {
        super.applyPendingChanges();
        deleteBackupFile(network.getOperationalCertificateFileId());
        deleteBackupFile(network.getIssuerCertificateFileIds().get(0));
        deleteBackupFile(network.getIssuerCertificateFileIds().get(1));
        changedCertFiles.clear();
        notifyCertificateActivated();
    }

    /**
     * Notifies the key pair handler of the operational certificate that is now effective, so that it can
     * promote the pending key pair when the certificate matches it. Skipped when the operational certificate
     * file is empty (credential erasure) or unparseable.
     */
    private void notifyCertificateActivated() {
        try {
            byte[] data = fileContent(network.getOperationalCertificateFileId());
            if (data.length == 0) {
                return;
            }
            CertificateFactory cf = CertificateFactory.getInstance(SCNetworkUtils.DEFAULT_CERTIFICATE_TYPE);
            keyPairHandler.certificateActivated(SCNetworkUtils.generateCertificate(cf, data));
        } catch (IOException | CertificateException e) {
            LOG.warn("Failed to notify the key pair handler of the activated operational certificate", e);
        }
    }

    private void deleteBackupFile(ObjectIdentifier fileId) {
        FileObject fileObject = Objects.requireNonNull(getLocalDevice().getObject(fileId));
        var fileAccess = (FileStreamAccess) fileObject.getFileAccess();
        var filePath = fileAccess.getFile().toPath();
        fileObject.getLock().lock();
        try {
            Files.deleteIfExists(getBackupPath(filePath));
        } catch (IOException e) {
            LOG.error("Failed to delete cert backup file for {}", fileId, e);
        } finally {
            fileObject.getLock().unlock();
        }
    }

    /**
     * Reads the file's full content under the file object's lock, so that a concurrent AtomicWriteFile
     * cannot produce a torn read.
     */
    private byte[] fileContent(ObjectIdentifier fileId) throws IOException {
        FileObject fileObject = Objects.requireNonNull(getLocalDevice().getObject(fileId));
        var fileAccess = (FileStreamAccess) fileObject.getFileAccess();
        var filePath = fileAccess.getFile().toPath();
        fileObject.getLock().lock();
        try {
            return Files.readAllBytes(filePath);
        } finally {
            fileObject.getLock().unlock();
        }
    }

    private void reinstateBackupFiles() {
        reinstateBackupFile(network.getOperationalCertificateFileId());
        reinstateBackupFile(network.getIssuerCertificateFileIds().get(0));
        reinstateBackupFile(network.getIssuerCertificateFileIds().get(1));
    }

    private void reinstateBackupFile(ObjectIdentifier fileId) {
        FileObject fileObject = Objects.requireNonNull(getLocalDevice().getObject(fileId));
        var fileAccess = (FileStreamAccess) fileObject.getFileAccess();
        var filePath = fileAccess.getFile().toPath();
        var backupPath = getBackupPath(filePath);
        fileObject.getLock().lock();
        try {
            // Remove any partial backup left by a crash mid-creation; it was never moved into place, so it
            // must not be reinstated.
            Files.deleteIfExists(getBackupTempPath(filePath));
            atomicMove(backupPath, filePath);
        } catch (NoSuchFileException e) {
            LOG.debug("No cert backup file to reinstate at {}", filePath, e);
        } catch (IOException e) {
            LOG.error("Failed to reinstate backup file at {}", filePath, e);
        } finally {
            fileObject.getLock().unlock();
        }
    }

    protected void handleFileWrite(AtomicWriteFileRequest awf) {
        var fileId = awf.getFileIdentifier();
        FileObject fileObject = Objects.requireNonNull(getLocalDevice().getObject(fileId));
        var fileAccess = (FileStreamAccess) fileObject.getFileAccess();
        var filePath = fileAccess.getFile().toPath();
        var backupPath = getBackupPath(filePath);
        var requestStream = awf.getStreamAccess();

        fileObject.getLock().lock();
        try {
            if (changedCertFiles.contains(fileId)) {
                // Check if the new content is the same as the backup, but only if the start index is zero.
                if (requestStream.getFileStartPosition().intValue() == 0
                        && Arrays.equals(Files.readAllBytes(backupPath), requestStream.getFileData().getBytes())) {
                    // The original content is being reverted. Remove the backup file and the marker.
                    Files.delete(backupPath);
                    changedCertFiles.remove(fileId);
                } // Otherwise there is nothing to do. The file is just being changed again.
            } else {
                // Check if the content is actually changing, but only if the start index is zero.
                if (requestStream.getFileStartPosition().intValue() != 0
                        || !Arrays.equals(Files.readAllBytes(filePath), requestStream.getFileData().getBytes())) {
                    createBackupFile(fileId, filePath, backupPath);
                } // Otherwise ignore because the file is not actually changing.
            }
        } catch (IOException e) {
            LOG.error("Failed to handle cert file write", e);
        } finally {
            fileObject.getLock().unlock();
        }
    }

    protected void handleFileSizeChange(ObjectIdentifier fileId) {
        // If there already is a backup file, ignore because there is no way with a fileSize write to reverse a change.
        if (!changedCertFiles.contains(fileId)) {
            // Create a backup file
            FileObject fileObject = Objects.requireNonNull(getLocalDevice().getObject(fileId));
            var fileAccess = (FileStreamAccess) fileObject.getFileAccess();
            var filePath = fileAccess.getFile().toPath();
            var backupPath = getBackupPath(filePath);
            fileObject.getLock().lock();
            try {
                createBackupFile(fileId, filePath, backupPath);
            } catch (IOException e) {
                LOG.error("Failed to handle cert file change", e);
            } finally {
                fileObject.getLock().unlock();
            }
        }
    }

    protected void createBackupFile(ObjectIdentifier fileId, Path filePath, Path backupPath) throws IOException {
        // The backup is written to a temp file and atomically moved into place. A crash mid-copy must not
        // leave a partial file at the backup path, because initialization after a restart reinstates
        // backups over the certificate files.
        var tempPath = getBackupTempPath(filePath);
        if (Files.exists(filePath)) {
            Files.copy(filePath, tempPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        } else {
            // The file may not actually exist, and so we create a 0-length marker file.
            Files.write(tempPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        atomicMove(tempPath, backupPath);
        changedCertFiles.add(fileId);
    }

    protected Path getBackupPath(Path filePath) {
        return filePath.resolveSibling(filePath.getFileName() + BACKUP_EXTENSION);
    }

    protected Path getBackupTempPath(Path filePath) {
        return filePath.resolveSibling(filePath.getFileName() + BACKUP_EXTENSION + ".tmp");
    }

    /**
     * Moves atomically where the file system supports it (same-directory renames on common file systems
     * do), so that a crash cannot leave a partial file at the target.
     */
    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public Health validateChanges() {
        try {
            validateCerts();
        } catch (CertException e) {
            return e.toHealth();
        }

        return super.validateChanges();
    }

    protected class CertException extends Exception {
        private final transient PropertyIdentifier property;
        private final transient ErrorCode code;

        public CertException(PropertyIdentifier property, ErrorCode code, String details) {
            super(details);
            this.property = property;
            this.code = code;
        }

        Health toHealth() {
            return new Health(new DateTime(getLocalDevice()),
                    new ErrorClassAndCode(ErrorClass.security, code),
                    property,
                    new CharacterString(getMessage()));
        }
    }

    protected void validateCerts() throws CertException { // 12.56.100
        CertificateFactory cf = loadCertificateFactory();

        var opFileId = network.getOperationalCertificateFileId();
        var issuer1FileId = network.getIssuerCertificateFileIds().get(0);
        var issuer2FileId = network.getIssuerCertificateFileIds().get(1);

        // Load the certs. Malformed content is detected here regardless of whether the file has changed or not.
        var opCert = loadCertificate(cf, opFileId, PropertyIdentifier.operationalCertificateFile);
        var caCerts = new X509Certificate[] {
                loadCertificate(cf, issuer1FileId, PropertyIdentifier.issuerCertificateFiles),
                loadCertificate(cf, issuer2FileId, PropertyIdentifier.issuerCertificateFiles),
        };

        // The operational certificate is validated whenever any certificate file has a pending change, because a
        // change to an issuer certificate can invalidate the operational certificate's chain. The issuer
        // certificates themselves are not validated beyond the parsing above: 12.56.101 defines no required
        // validations for them, and AB.7.4 does not check issuer expiry at connection time either.
        if (changedCertFiles.isEmpty()) {
            return;
        }

        if (opCert == null) {
            // An empty operational certificate file is acceptable only as part of a full credential erasure,
            // i.e. when both issuer certificate files are also empty.
            if (caCerts[0] != null || caCerts[1] != null) {
                throw new CertException(PropertyIdentifier.operationalCertificateFile, ErrorCode.certificateInvalid,
                        "Operational certificate file is empty while issuer certificates are configured");
            }
            return;
        }

        checkCertValidity(opCert, opFileId);

        // 12.56.100 requires the certificate to validate against "an internal private key", i.e. either the
        // active pair or the pending pair from an outstanding GENERATE_CSR_FILE.
        if (!keyPairHandler.matches(opCert)) {
            throw new CertException(PropertyIdentifier.operationalCertificateFile, ErrorCode.unknownCertificateKey,
                    "Operational certificate does not match an internal private key");
        }

        if (!verifyAgainstAnyIssuer(opCert, caCerts)) {
            throw new CertException(PropertyIdentifier.operationalCertificateFile, ErrorCode.certificateInvalid,
                    "Operational certificate cannot be validated by any issuer certificate");
        }
    }

    private void checkCertValidity(X509Certificate cert, ObjectIdentifier fileId)
            throws CertException {
        try {
            cert.checkValidity();
        } catch (CertificateExpiredException e) {
            throw new CertException(PropertyIdentifier.operationalCertificateFile, ErrorCode.certificateExpired,
                    "Certificate at " + fileId + " expired on " + cert.getNotAfter());
        } catch (CertificateNotYetValidException e) {
            throw new CertException(PropertyIdentifier.operationalCertificateFile, ErrorCode.certificateInvalid,
                    "Certificate " + fileId + " not yet valid until " + cert.getNotBefore());
        }
    }

    private boolean verifyAgainstAnyIssuer(X509Certificate operational, X509Certificate[] issuers) {
        for (X509Certificate issuer : issuers) {
            if (issuer == null) {
                continue;
            }
            try {
                operational.verify(issuer.getPublicKey());
                return true;
            } catch (Exception ignored) {
                // Try the next issuer.
            }
        }
        return false;
    }

    private CertificateFactory loadCertificateFactory() throws CertException {
        try {
            return CertificateFactory.getInstance(SCNetworkUtils.DEFAULT_CERTIFICATE_TYPE);
        } catch (CertificateException e) {
            throw new CertException(null, ErrorCode.other, "Unable to create certificate factory: " + e.getMessage());
        }
    }

    /**
     * Loads and parses a certificate file. Returns null when the file is empty, which per 12.56.100/101
     * means no certificate is configured. Throws with CERTIFICATE_MALFORMED when the content cannot be
     * read or parsed.
     */
    private X509Certificate loadCertificate(CertificateFactory cf, ObjectIdentifier fileId,
            PropertyIdentifier pid) throws CertException {
        try {
            byte[] data = fileContent(fileId);
            if (data.length == 0) {
                return null;
            }
            return SCNetworkUtils.generateCertificate(cf, data);
        } catch (IOException e) {
            throw new CertException(pid, ErrorCode.certificateMalformed,
                    "Unable to load certificate data in file " + fileId + ": " + e.getMessage());
        } catch (CertificateException e) {
            throw new CertException(pid, ErrorCode.certificateMalformed,
                    "Certificate encoding error in file " + fileId + ": " + e.getMessage());
        }
    }
}
