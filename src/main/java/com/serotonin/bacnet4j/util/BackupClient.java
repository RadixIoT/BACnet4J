/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.apdu.ComplexACK;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.ErrorAPDUException;
import com.serotonin.bacnet4j.service.acknowledgement.AtomicReadFileAck;
import com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest;
import com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess;
import com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.BackupState;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.FileAccessMethod;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ArrayUtils;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

/**
 * Utility class that implements Device A - the device performing the backup procedure - from 19.1.2.
 *
 * @author Matthew
 */
public class BackupClient {
    static final Logger LOG = LoggerFactory.getLogger(BackupClient.class);

    private final LocalDevice localDevice;
    private final int targetDeviceId;
    private final CharacterString password;
    private final int recordsPerRequest;
    private final int maxOctetsPerRequest;

    public BackupClient(final LocalDevice localDevice, final int targetDeviceId, final String password) {
        this.localDevice = localDevice;
        this.targetDeviceId = targetDeviceId;
        this.password = password == null ? null : new CharacterString(password);
        this.recordsPerRequest = 5;
        this.maxOctetsPerRequest = 128;
    }

    /**
     * Constructor with communication parameters
     *
     * @param localDevice
     * @param targetDeviceId
     * @param password
     * @param recordsPerRequest   only for record-access relevant.
     *                            Record access files are written out as CRLF-delimited hex representations.
     *                            The choice of the number of records per request is arbitrary
     *                            because we don't know how big a record will be.
     * @param maxOctetsPerRequest only for stram-access relevant.
     *                            Defines the maximum APDU size that is used.
     *                            The choice of the number of bytes per request
     *                            is based upon the max APDU size of the target.
     */
    public BackupClient(final LocalDevice localDevice, final int targetDeviceId, final String password,
            int recordsPerRequest, int maxOctetsPerRequest) {
        this.localDevice = localDevice;
        this.targetDeviceId = targetDeviceId;
        this.password = password == null ? null : new CharacterString(password);
        this.recordsPerRequest = recordsPerRequest;
        this.maxOctetsPerRequest = maxOctetsPerRequest;
    }

    /**
     * Start the backup procedure with a default timeout of 30s.
     */
    public List<File> begin(final File saveDir) throws BACnetException, IOException {
        return begin(saveDir, 30000);
    }

    /**
     * Start the backup procedure.
     *
     * @param saveDir                  the directory in which backups should be copied.
     * @param backupStateChangeTimeout the maximum amount of milliseconds to wait for the backup state of the target
     *                                 to change to an actionable value.
     * @return the files that were written as a result of the backup.
     * @throws BACnetException
     * @throws IOException
     */
    public List<File> begin(final File saveDir, final long backupStateChangeTimeout)
            throws BACnetException, IOException {
        // Get a remote device handle for the device.
        LOG.info("Beginning backup procedure");
        LOG.info("Getting remote device...");
        final RemoteDevice rd = localDevice.getRemoteDeviceBlocking(targetDeviceId);

        // Get the backup preparation time.
        LOG.info("Getting backup preparation time...");
        int backupPreparationTimeSeconds = 0;
        try {
            final UnsignedInteger backupPreparationTime = RequestUtils.getProperty(localDevice, rd,
                    PropertyIdentifier.backupPreparationTime);
            backupPreparationTimeSeconds = backupPreparationTime.intValue();
        } catch (final BACnetErrorException e) {
            // Ignore unknown property to support old devices.
            final ErrorClassAndCode ecac = e.getBacnetError().getError().getErrorClassAndCode();
            if (!ecac.equals(ErrorClass.property, ErrorCode.unknownProperty)) {
                throw e;
            }
        }

        try {
            // 19.1.2.1 - send a start backup request to the target device.
            LOG.info("Sending start backup request...");
            localDevice.send(rd, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.startBackup, password)).get();

            // 19.1.2.2
            if (backupPreparationTimeSeconds > 0) {
                // Sleep for the given amount of seconds.
                LOG.info("Waiting for target device to complete backup preparation...");
                ThreadUtils.sleep(backupPreparationTimeSeconds * 1000);
            }

            // Poll the backup state of the target, waiting for it to change to something actionable.
            // It should already be preparingForBackup, and then change to either performingABackup or backupFailure.
            final long deadline = localDevice.getClock().millis() + backupStateChangeTimeout;
            try {
                while (true) {
                    LOG.info("Getting backup state...");
                    final BackupState backupState = RequestUtils.getProperty(localDevice, rd,
                            PropertyIdentifier.backupAndRestoreState);

                    if (backupState.equals(BackupState.preparingForBackup)) {
                        LOG.info("Target is still preparing for backup...");
                        // Waiting for this to change. Pause for a moment.
                        ThreadUtils.sleep(1000);
                    } else if (backupState.isOneOf(BackupState.performingABackup)) {
                        // Ready to perform the backup
                        LOG.info("Target is ready for backup...");
                        break;
                    } else if (backupState.isOneOf(BackupState.backupFailure)) {
                        throw new BACnetException("Target device reported a backup failure. Aborting backup.");
                    } else {
                        throw new BACnetException("Unexpected backup state reported by target device: " + backupState);
                    }

                    if (deadline < localDevice.getClock().millis()) {
                        LOG.info("Timeout waiting for backup state of target to change.");
                        throw new BACnetException("Timeout waiting for backup state of target to change.");
                    }
                }
            } catch (final BACnetErrorException e) {
                // Ignore unknown property to support old devices.
                final ErrorClassAndCode ecac = e.getBacnetError().getError().getErrorClassAndCode();
                if (!ecac.equals(ErrorClass.property, ErrorCode.unknownProperty)) {
                    throw e;
                }
            }

            return copyFiles(saveDir, rd);
        } finally {
            // 19.1.2.5
            localDevice.send(rd, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.endBackup, password));
        }
    }

    private List<File> copyFiles(final File saveDir, final RemoteDevice rd) throws BACnetException, IOException {
        // 19.1.2.3, 19.1.2.4
        // The property is of type BACnetArray, but encoding loses that information, and returns it as a SequenceOf.
        LOG.info("Getting configuration file list...");
        final SequenceOf<ObjectIdentifier> configurationFiles = RequestUtils.getProperty(localDevice, rd,
                PropertyIdentifier.configurationFiles);

        LOG.info("Target reported {} configuration files", configurationFiles.size());

        // The choice of the number of bytes per request is based upon the max APDU size of the target. 
        // This parameter is only relevant for streamAccess.
        int octetsPerRequest = rd.getMaxAPDULengthAccepted() - ComplexACK.getHeaderSize(
                true) - AtomicReadFileAck.getHeaderSize() - AtomicReadFileAck.StreamAccessAck.getHeaderSize();
        if (octetsPerRequest > maxOctetsPerRequest) {
            octetsPerRequest = maxOctetsPerRequest;
        }
        LOG.debug("With streamAccess, {} octets per Request can be transmitted", octetsPerRequest);

        final List<File> result = new ArrayList<>(configurationFiles.size());
        for (final ObjectIdentifier fileOid : configurationFiles) {
            LOG.info("Retrieving configuration file access method for {}", fileOid);
            final FileAccessMethod fileAccessMethod = RequestUtils.getProperty(localDevice, rd, fileOid,
                    PropertyIdentifier.fileAccessMethod);

            final File localFile = createLocalFile(saveDir, rd, fileOid);
            result.add(localFile);

            LOG.info("Retrieving configuration file contents for {}, and saving to {}", fileOid, localFile);
            int reqCount = 0;
            try {
                if (fileAccessMethod.equals(FileAccessMethod.recordAccess)) {
                    // Record access files are written out as CRLF-delimited hex representations. The choice of the number
                    // of records per request is arbitrary because we don't know how big a record will be.
                    int currentRecord = 0;
                    try (PrintWriter out = new PrintWriter(localFile)) {
                        while (true) {
                            reqCount++;
                            final AtomicReadFileRequest req = new AtomicReadFileRequest(fileOid,
                                    new RecordAccess(new SignedInteger(currentRecord),
                                            new UnsignedInteger(recordsPerRequest)));
                            final AtomicReadFileAck ack = localDevice.send(rd, req).get();
                            boolean firstLine = true;
                            for (final OctetString record : ack.getRecordAccess().getFileRecordData()) {
                                out.println(ArrayUtils.toPlainHexString(record.getBytes()));
                            }

                            if (ack.getEndOfFile().booleanValue()) {
                                break;
                            }
                            currentRecord += ack.getRecordAccess().getReturnedRecordCount().intValue();
                        }
                    }
                } else {
                    int currentPosition = 0;
                    try (FileOutputStream out = new FileOutputStream(localFile)) {
                        while (true) {
                            reqCount++;
                            final AtomicReadFileRequest req = new AtomicReadFileRequest(fileOid,
                                    new StreamAccess(new SignedInteger(currentPosition),
                                            new UnsignedInteger(octetsPerRequest)));
                            final AtomicReadFileAck ack = localDevice.send(rd, req).get();

                            out.write(ack.getStreamAccess().getFileData().getBytes());

                            if (ack.getEndOfFile().booleanValue()) {
                                break;
                            }
                            currentPosition += ack.getStreamAccess().getFileData().getLength();
                        }
                    }
                }
            } catch (ErrorAPDUException ex) {
                if (reqCount == 1 && ex.getError().equals(ErrorClass.services, ErrorCode.invalidFileStartPosition)) {
                    //Some devices send an error when trying to read an empty file. Continue with the next file.
                    LOG.info("File '{}' seems to be an empty file.", fileOid.getInstanceNumber());
                } else {
                    throw ex;
                }
            }
            LOG.info("Configuration file retrieved using {} requests", reqCount);
        }

        return result;
    }

    /**
     * Override this method to provide customized backup file names.
     *
     * @param saveDir
     * @param rd
     * @param fileOid
     * @return
     */
    protected File createLocalFile(final File saveDir, final RemoteDevice rd, final ObjectIdentifier fileOid) {
        return new File(saveDir, "device-" + targetDeviceId + "-file-" + fileOid.getInstanceNumber());
    }
}
