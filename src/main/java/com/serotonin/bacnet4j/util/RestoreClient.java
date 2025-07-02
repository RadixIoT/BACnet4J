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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.apdu.ConfirmedRequest;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.CreateObjectAck;
import com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest;
import com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.RecordAccess;
import com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess;
import com.serotonin.bacnet4j.service.confirmed.CreateObjectRequest;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.BackupState;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.FileAccessMethod;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
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
 * Utility class that implements Device A - the device performing the restore procedure - from 19.1.3.
 *
 * @author Matthew
 */
public class RestoreClient {
    static final Logger LOG = LoggerFactory.getLogger(RestoreClient.class);

    private final LocalDevice localDevice;
    private final int targetDeviceId;
    private final CharacterString password;
    private final int recordsPerRequest;
    private final int maxOctetsPerRequest;

    public RestoreClient(final LocalDevice localDevice, final int targetDeviceId, final String password) {
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
     * @param recordsPerRequest   only for record-access relevant. Record access files are written out as CRLF-delimited
     *                            hex representations. The choice of the number of records per request is arbitrary
     *                            because we don't know how big a record will be.
     * @param maxOctetsPerRequest only for stream-access relevant. Defines the maximum APDU size that is used. The
     *                            choice of the number of bytes per request is based upon the max APDU size of the
     *                            target.
     */
    public RestoreClient(final LocalDevice localDevice, final int targetDeviceId, final String password,
            int recordsPerRequest, int maxOctetsPerRequest) {
        this.localDevice = localDevice;
        this.targetDeviceId = targetDeviceId;
        this.password = password == null ? null : new CharacterString(password);
        this.recordsPerRequest = recordsPerRequest;
        this.maxOctetsPerRequest = maxOctetsPerRequest;
    }

    /**
     * Start the restore procedure with a default timeout of 30s.
     */
    public void begin(final List<File> files) throws BACnetException, IOException {
        begin(files, 30000);
    }

    /**
     * Start the restore procedure.
     *
     * @param files                     the files to restore to the target device. The file names are expected to look
     *                                  like those produced by the BackupClient, and will be restore to file objects on
     *                                  the target device with the same object identifiers.
     * @param restoreStateChangeTimeout the maximum amount of milliseconds to wait for the backup state of the target
     *                                  to change to an actionable value.
     * @throws BACnetException
     * @throws IOException
     */
    public void begin(final List<File> files, final long restoreStateChangeTimeout)
            throws BACnetException, IOException {
        // Get a remote device handle for the device.
        LOG.info("Beginning restore procedure");
        LOG.info("Getting remote device...");
        final RemoteDevice rd = localDevice.getRemoteDeviceBlocking(targetDeviceId);

        // Get the restore preparation time.
        LOG.info("Getting restore preparation time...");
        int restorePreparationTimeSeconds = 0;
        try {
            final UnsignedInteger restorePreparationTime = RequestUtils.getProperty(localDevice, rd,
                    PropertyIdentifier.restorePreparationTime);
            restorePreparationTimeSeconds = restorePreparationTime.intValue();
        } catch (final BACnetErrorException e) {
            // Ignore unknown property to support old devices.
            final ErrorClassAndCode ecac = e.getBacnetError().getError().getErrorClassAndCode();
            if (!ecac.equals(ErrorClass.property, ErrorCode.unknownProperty)) {
                throw e;
            }
        }

        try {
            // 19.1.3.1 - send a start restore request to the target device.
            LOG.info("Sending start restore request...");
            localDevice.send(rd, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.startRestore, password))
                    .get();

            // 19.1.3.2
            if (restorePreparationTimeSeconds > 0) {
                // Sleep for the given amount of seconds.
                LOG.info("Waiting for target device to complete restore preparation...");
                ThreadUtils.sleep(restorePreparationTimeSeconds * 1000);
            }

            // Poll the backup state of the target, waiting for it to change to something actionable.
            // It should already be preparingForRestore, and then change to either performingARestore or restoreFailure.
            final long deadline = localDevice.getClock().millis() + restoreStateChangeTimeout;
            try {
                while (true) {
                    LOG.info("Getting restore state...");
                    final BackupState backupState = RequestUtils.getProperty(localDevice, rd,
                            PropertyIdentifier.backupAndRestoreState);

                    if (backupState.equals(BackupState.preparingForRestore)) {
                        LOG.info("Target is still preparing for restore...");
                        // Waiting for this to change. Pause for a moment.
                        ThreadUtils.sleep(1000);
                    } else if (backupState.equals(BackupState.performingARestore)) {
                        // Ready to perform the restore
                        LOG.info("Target is ready for restore...");
                        break;
                    } else if (backupState.equals(BackupState.restoreFailure)) {
                        throw new BACnetException("Target device reported a restore failure. Aborting restore.");
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

            copyFiles(files, rd);

            // 19.1.3.4
            localDevice.send(rd, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.endRestore, password));
        } catch (BACnetException | IOException | ThreadUtils.UncheckedInterruptedException e) {
            // 19.1.3.4
            localDevice.send(rd, new ReinitializeDeviceRequest(ReinitializedStateOfDevice.abortRestore, password));
            throw e;
        }
    }

    private void copyFiles(final List<File> files, final RemoteDevice rd) throws BACnetException, IOException {
        // 19.1.3.3
        // The property is of type BACnetArray, but encoding loses that information, and returns it as a SequenceOf.
        LOG.info("Getting configuration file list...");
        final SequenceOf<ObjectIdentifier> configurationFiles = RequestUtils.getProperty(localDevice, rd,
                PropertyIdentifier.configurationFiles);

        LOG.info("Target reported {} configuration files", configurationFiles.size());

        // The choice of the number of bytes per request is based upon the max APDU size of the target. 
        // This parameter is only relevant for streamAccess.
        int octetsPerRequest = rd.getMaxAPDULengthAccepted() - ConfirmedRequest.getHeaderSize(
                true) - AtomicWriteFileRequest.getHeaderSize() - AtomicWriteFileRequest.StreamAccess.getHeaderSize();
        if (octetsPerRequest > maxOctetsPerRequest) {
            octetsPerRequest = maxOctetsPerRequest;
        }
        LOG.debug("With streamAccess, {} octets per Request can be transmitted", octetsPerRequest);

        for (final File file : files) {
            // Find a matching file object.
            final int fileNumber = toFileInstanceNumber(file);
            ObjectIdentifier fileOid = null;
            for (final ObjectIdentifier oid : configurationFiles) {
                if (oid.getInstanceNumber() == fileNumber) {
                    fileOid = oid;
                    break;
                }
            }

            if (fileOid == null) {
                // Didn't find a matching file. Create a new file on the target.
                final CreateObjectAck ack = localDevice.send(rd,
                                new CreateObjectRequest(ObjectType.file, new SequenceOf<>(
                                        new PropertyValue(PropertyIdentifier.objectName, new CharacterString(file.getName())),
                                        new PropertyValue(PropertyIdentifier.fileType,
                                                new CharacterString("configurationFile")),
                                        new PropertyValue(PropertyIdentifier.fileAccessMethod, FileAccessMethod.streamAccess))))
                        .get();
                fileOid = ack.getObjectIdentifier();
            }

            LOG.info("Retrieving configuration file access method for {}", fileOid);
            final FileAccessMethod fileAccessMethod = RequestUtils.getProperty(localDevice, rd, fileOid,
                    PropertyIdentifier.fileAccessMethod);

            LOG.info("Writing configuration file contents for {}", file);
            int reqCount = 0;
            if (fileAccessMethod.equals(FileAccessMethod.recordAccess)) {
                // Empty the existing file by writing a record count of 0.
                RequestUtils.writeProperty(localDevice, rd, fileOid, PropertyIdentifier.recordCount,
                        UnsignedInteger.ZERO);

                // Record access files are expected to be CRLF-delimited hex representations, as written by the
                // BackupClient.
                int currentRecord = 0;
                try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                    boolean done = false;
                    while (!done) {
                        final SequenceOf<OctetString> records = new SequenceOf<>();
                        while (records.size() < recordsPerRequest) {
                            final String line = in.readLine();
                            if (line == null) {
                                // EOF
                                done = true;
                                break;
                            }
                            records.add(new OctetString(ArrayUtils.fromPlainHexString(line)));
                        }

                        if (records.size() > 0) {
                            reqCount++;
                            final AtomicWriteFileRequest req = new AtomicWriteFileRequest(fileOid, new RecordAccess(
                                    new SignedInteger(currentRecord), new UnsignedInteger(records.size()), records));
                            localDevice.send(rd, req).get();

                            currentRecord += records.size();
                        }
                    }
                }
            } else {
                // Empty the existing file by writing a file size of 0.
                RequestUtils.writeProperty(localDevice, rd, fileOid, PropertyIdentifier.fileSize, UnsignedInteger.ZERO);

                final byte[] buffer = new byte[octetsPerRequest];
                int currentPosition = 0;
                try (FileInputStream in = new FileInputStream(file)) {
                    while (true) {
                        final int readCount = in.read(buffer);
                        if (readCount == -1)
                            break;

                        OctetString data;
                        if (readCount < buffer.length) {
                            final byte[] b = new byte[readCount];
                            System.arraycopy(buffer, 0, b, 0, readCount);
                            data = new OctetString(b);
                        } else {
                            data = new OctetString(buffer);
                        }

                        reqCount++;
                        final AtomicWriteFileRequest req = new AtomicWriteFileRequest(fileOid,
                                new StreamAccess(new SignedInteger(currentPosition), data));
                        localDevice.send(rd, req).get();

                        currentPosition += readCount;
                    }
                }
            }
            LOG.info("Configuration file written using {} requests", reqCount);
        }
    }

    /**
     * Override this method to provide customized file name to object identifier mapping.
     *
     * @param saveDir
     * @param rd
     * @param fileOid
     * @return
     */
    protected int toFileInstanceNumber(final File file) {
        final Matcher matcher = FILE_NAME_PATTERN.matcher(file.getName());
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("device-\\d+-file-(\\d+)");
}
