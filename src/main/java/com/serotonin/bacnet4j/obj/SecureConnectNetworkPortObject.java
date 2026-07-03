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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.sc.SCNetwork;
import com.serotonin.bacnet4j.npdu.sc.SCNetworkUtils;
import com.serotonin.bacnet4j.obj.fileAccess.StreamAccess;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Health;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyValue;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Network Port object for a BACnet/SC port (Clauses 12.56 and Annex AB).
 *
 * <p>The properties in Table 12-71.8 not initialized here are either optional (and left to product builders) or
 * dynamic state owned by the BACnet/SC stack (e.g., {@code SC_Hub_Connector_State}, hub connection status lists,
 * failed connection requests).
 */
public class SecureConnectNetworkPortObject extends NetworkPortObject {
    static final Logger LOG = LoggerFactory.getLogger(SecureConnectNetworkPortObject.class);

    private final SCNetwork network;

    public SecureConnectNetworkPortObject(LocalDevice localDevice, SCNetwork network, int instanceNumber) {
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
            throw new IllegalStateException("Network port object already initialized");
        }
        this.network = network;

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
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
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
     * commands fall through to the base class handling.
     */
    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);
        if (pid.equals(PropertyIdentifier.command) && newValue == NetworkPortCommand.generateCsrFile) {
            executeCommand(this::generateCsrFile);
        }
    }

    /**
     * Extension point for CSR generation. Called on the local device's executor when the client
     * writes GENERATE_CSR_FILE to the Command property. The default implementation does nothing;
     * product developers subclass this class and override this method to:
     * <ol>
     *   <li>Generate and save a new private/public key pair if necessary.</li>
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

    protected void validateUnsignedRange(UnsignedInteger value, int min, int max) throws BACnetServiceException {
        var i = value.intValue();
        if (i < min || i > max) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        }
    }

    @Override
    protected void initializeImpl() {
        super.initializeImpl();

        getLocalDevice().getEventHandler().addListener(new DeviceEventAdapter() {
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
             *   extension. We also add an entry to the pending changes map, using the propery id of the cert. But,
             *   because the issuer certs is an array, we need to track each cert separately by using an array as the
             *   value. The array values are BACnet Booleans. For convenience, we also use an array for the operational
             *   cert even though there is only one.
             * - the changes are called "pending", but this is only from the perspective of the network port object.
             *   we allow requests to continue as normal because the changes they make need to be reflected in, e.g.,
             *   subsequent read requests.
             * - subseqent write file requests may change the file back to its original content. If this happens we
             *   remove the backup file and set the appropriate array value to false, and if the array only has false
             *   values, remove the entry from the pending changes map.
             * - note that the code does not attempt to detect reversions of file size, e.g. changing the file size to
             *   some other value and then changing it back again. If a file has been marked as changed, no change to
             *   its file size property will not remove that mark.
             * - if pending changes are discarded, we restore the backup file to be the current file and clear the
             *   pending changes markers.
             * - if the changes are to be applied with the suitable "reinitialize device" request, the backup files are
             *   deleted.
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
        });

        // Check if there are backup versions of the cert files, and if so make them the current files.
        reinstateBackupFiles();
    }

    protected static final String BACKUP_EXTENSION = ".pendingChangesBackup";

    @Override
    protected void discardChanges() {
        super.discardChanges();
        // No need to remove markers because the super class clears the pending changes. Just reinstate the backup files.
        reinstateBackupFiles();
    }

    private byte[] fileContent(ObjectIdentifier fileId) throws IOException {
        FileObject fileObject = getLocalDevice().getObject(fileId);
        var fileAccess = (StreamAccess) fileObject.getFileAccess();
        var filePath = fileAccess.getFile().toPath();
        return Files.readAllBytes(filePath);
    }

    private void reinstateBackupFiles() {
        reinstateBackupFile(network.getOperationalCertificateFileId());
        reinstateBackupFile(network.getIssuerCertificateFileIds().get(0));
        reinstateBackupFile(network.getIssuerCertificateFileIds().get(1));
    }

    private void reinstateBackupFile(ObjectIdentifier fileId) {
        FileObject fileObject = getLocalDevice().getObject(fileId);
        var fileAccess = (StreamAccess) fileObject.getFileAccess();
        var filePath = fileAccess.getFile().toPath();
        var backupPath = getBackupPath(filePath);
        try {
            Files.move(backupPath, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (NoSuchFileException e) {
            LOG.debug("No cert backup file to delete at {}", filePath, e);
        } catch (IOException e) {
            LOG.error("Failed to reinstate backup file at {}", filePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleFileWrite(AtomicWriteFileRequest awf) {
        try {
            // Check if a backup already exists.
            var ref = filePropertyReference(awf.getFileIdentifier());
            var propertyIndex = ref.getPropertyArrayIndex().intValue();
            var backupFileFlags = (BACnetArray<Boolean>) getPendingChanges().get(ref.getPropertyIdentifier());
            Boolean hasBackupFile = backupFileFlags == null ? Boolean.FALSE : backupFileFlags.get(propertyIndex);

            FileObject fileObject = getLocalDevice().getObject(awf.getFileIdentifier());
            var fileAccess = (StreamAccess) fileObject.getFileAccess();
            var filePath = fileAccess.getFile().toPath();
            var backupPath = getBackupPath(filePath);
            var requestStream = awf.getStreamAccess();

            if (Boolean.TRUE.equals(hasBackupFile)) {
                // Check if the new content is the same as the backup, but only if the start index is zero.
                if (requestStream.getFileStartPosition().intValue() == 0
                        && Arrays.equals(Files.readAllBytes(backupPath), requestStream.getFileData().getBytes())) {
                    // The original content is being reverted. Remove the backup file and the flag.
                    Files.delete(backupPath);
                    Objects.requireNonNull(backupFileFlags).set(propertyIndex, Boolean.FALSE);

                    // If all the flags are false, remove the property
                    if (backupFileFlags.stream().allMatch(Boolean.FALSE::equals)) {
                        pendingChanges().remove(ref.getPropertyIdentifier());
                    }
                } // Otherwise there is nothing to do. The file is just being changed again.
            } else {
                // Check if the content is actually changing, but only if the start index is zero.
                if (requestStream.getFileStartPosition().intValue() != 0
                        || !Arrays.equals(Files.readAllBytes(filePath), requestStream.getFileData().getBytes())) {
                    createBackupFile(filePath, backupPath, backupFileFlags, ref);
                } // Otherwise ignore because the file is not actually changing.
            }
        } catch (IOException e) {
            LOG.error("Failed to handle cert file write", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleFileSizeChange(ObjectIdentifier fileId) {
        try {
            var ref = filePropertyReference(fileId);
            var propertyIndex = ref.getPropertyArrayIndex().intValue();
            var backupFileFlags = (BACnetArray<Boolean>) getPendingChanges().get(ref.getPropertyIdentifier());
            Boolean hasBackupFile = backupFileFlags == null ? Boolean.FALSE : backupFileFlags.get(propertyIndex);

            // If there already is a backup file, ignore because there is no way with a fileSize write to reverse a change.
            if (Boolean.FALSE.equals(hasBackupFile)) {
                // Create a backup file
                FileObject fileObject = getLocalDevice().getObject(fileId);
                var fileAccess = (StreamAccess) fileObject.getFileAccess();
                var filePath = fileAccess.getFile().toPath();
                var backupPath = getBackupPath(filePath);
                createBackupFile(filePath, backupPath, backupFileFlags, ref);
            }
        } catch (IOException e) {
            LOG.error("Failed to handle cert file change", e);
        }
    }

    protected void createBackupFile(Path filePath, Path backupPath, BACnetArray<Boolean> backupFileFlags,
            PropertyReference ref) throws IOException {
        // Create the backup, and add a pending change marker.
        if (Files.exists(filePath)) {
            Files.copy(filePath, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
        } else {
            // The file may not actually exist, and so we create a 0-length marker file.
            Files.write(backupPath, new byte[0], StandardOpenOption.CREATE_NEW);
        }
        // Set a marker in the pending changes
        if (backupFileFlags == null) {
            backupFileFlags = new BACnetArray<>(arrayLength(ref.getPropertyIdentifier()), Boolean.FALSE);
        }
        backupFileFlags.set(ref.getPropertyArrayIndex().intValue(), Boolean.TRUE);
        pendingChanges().put(ref.getPropertyIdentifier(), backupFileFlags);
    }

    protected Path getBackupPath(Path filePath) {
        return filePath.resolveSibling(filePath.getFileName() + BACKUP_EXTENSION);
    }

    protected PropertyReference filePropertyReference(ObjectIdentifier fileId) {
        if (fileId.equals(network.getOperationalCertificateFileId())) {
            return new PropertyReference(PropertyIdentifier.operationalCertificateFile, UnsignedInteger.ZERO);
        }
        var issuerCerts = network.getIssuerCertificateFileIds();
        if (fileId.equals(issuerCerts.get(0))) {
            return new PropertyReference(PropertyIdentifier.issuerCertificateFiles, UnsignedInteger.ZERO);
        } else if (fileId.equals(issuerCerts.get(1))) {
            return new PropertyReference(PropertyIdentifier.issuerCertificateFiles, new UnsignedInteger(1));
        }
        throw new IllegalStateException("Unknown fileId: " + fileId);
    }

    protected int arrayLength(PropertyIdentifier pid) {
        return PropertyIdentifier.operationalCertificateFile.equals(pid) ? 1 : 2;
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

    @SuppressWarnings("unchecked")
    protected void validateCerts() throws CertException { // 12.56.100
        CertificateFactory cf = loadCertificateFactory();

        var opFileId = network.getOperationalCertificateFileId();
        var issuer1FileId = network.getIssuerCertificateFileIds().get(0);
        var issuer2FileId = network.getIssuerCertificateFileIds().get(1);

        // Load the certs. Some validation is done here regardless of whether the file has changed or not.
        var opCert = loadCertificate(cf, opFileId, PropertyIdentifier.operationalCertificateFile);
        var caCerts = new X509Certificate[] {
                loadCertificate(cf, issuer1FileId, PropertyIdentifier.issuerCertificateFiles),
                loadCertificate(cf, issuer2FileId, PropertyIdentifier.issuerCertificateFiles),
        };

        if (getPendingChanges().containsKey(PropertyIdentifier.operationalCertificateFile)) {
            // Validate the operational certificate.
            if (opCert == null) {
                throw new CertException(PropertyIdentifier.operationalCertificateFile, ErrorCode.certificateMalformed,
                        "Certificate file is empty");
            }
            checkCertValidity(opCert, opFileId, PropertyIdentifier.operationalCertificateFile);

            if (!network.matchesPublicKey(opCert)) {
                throw new CertException(PropertyIdentifier.operationalCertificateFile, ErrorCode.unknownCertificateKey,
                        "Operational certificate does not match the internal private key");
            }


            if (!verifyAgainstAnyIssuer(opCert, caCerts)) {
                throw new CertException(PropertyIdentifier.operationalCertificateFile, ErrorCode.certificateInvalid,
                        "Operational certificate cannot be validated by any issuer certificate");
            }
        }

        var flags = (BACnetArray<Boolean>) getPendingChanges().get(PropertyIdentifier.issuerCertificateFiles);
        if (flags != null) {
            // Validate the issuer certificates.
            if (Boolean.TRUE.equals(flags.get(0)) && caCerts[0] != null) {
                checkCertValidity(caCerts[0], issuer1FileId, PropertyIdentifier.issuerCertificateFiles);
            }

            if (Boolean.TRUE.equals(flags.get(1)) && caCerts[1] != null) {
                checkCertValidity(caCerts[1], issuer2FileId, PropertyIdentifier.issuerCertificateFiles);
            }
        }
    }

    private void checkCertValidity(X509Certificate cert, ObjectIdentifier fileId, PropertyIdentifier pid)
            throws CertException {
        try {
            cert.checkValidity();
        } catch (CertificateExpiredException e) {
            throw new CertException(pid, ErrorCode.certificateExpired,
                    "Certificate at " + fileId + " expired on " + cert.getNotAfter());
        } catch (CertificateNotYetValidException e) {
            throw new CertException(pid, ErrorCode.certificateInvalid,
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
     * Loads and parses an issuer certificate for use in operational-certificate chain validation.
     * Returns null on any error — the caller treats a null issuer the same as an issuer that
     * failed to verify, so unusable issuer files fall through to a CERTIFICATE_INVALID result on
     * the operational certificate.
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
