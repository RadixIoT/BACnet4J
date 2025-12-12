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

package com.serotonin.bacnet4j.event;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.DeviceObject;
import com.serotonin.bacnet4j.obj.FileObject;
import com.serotonin.bacnet4j.obj.fileAccess.StreamAccess;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest;
import com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest;
import com.serotonin.bacnet4j.service.confirmed.CreateObjectRequest;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.service.confirmed.ReinitializeDeviceRequest.ReinitializedStateOfDevice;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.BackupState;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * This class is provided mostly as an example of how such functionality might be implemented. It was written with the
 * specification at hand, but is not necessarily the most appropriate implementation. Serious application developers are
 * encouraged to write their own implementation.
 *
 * @author Matthew
 */
public class DefaultReinitializeDeviceHandler implements ReinitializeDeviceHandler {
    static final Logger LOG = LoggerFactory.getLogger(DefaultReinitializeDeviceHandler.class);

    private BackupStateMonitor backupStateMonitor;

    @Override
    public synchronized void handle(final LocalDevice localDevice, final Address from,
            final ReinitializedStateOfDevice reinitializedStateOfDevice) throws BACnetErrorException {
        if (reinitializedStateOfDevice.equals(ReinitializedStateOfDevice.coldstart)) {
            coldstart(localDevice, from);
        } else if (reinitializedStateOfDevice.equals(ReinitializedStateOfDevice.warmstart)) {
            warmstart(localDevice, from);
        } else if (reinitializedStateOfDevice.equals(ReinitializedStateOfDevice.startBackup)) {
            startBackup(localDevice, from);
        } else if (reinitializedStateOfDevice.equals(ReinitializedStateOfDevice.endBackup)) {
            endBackup(localDevice, from);
        } else if (reinitializedStateOfDevice.equals(ReinitializedStateOfDevice.startRestore)) {
            startRestore(localDevice, from);
        } else if (reinitializedStateOfDevice.equals(ReinitializedStateOfDevice.endRestore)) {
            endRestore(localDevice, from);
        } else if (reinitializedStateOfDevice.equals(ReinitializedStateOfDevice.abortRestore)) {
            abortRestore(localDevice, from);
        } else if (reinitializedStateOfDevice.equals(ReinitializedStateOfDevice.activateChanges)) {
            activateChanges(localDevice, from);
        } else {
            LOG.error("Unhandled ReinitializedStateOfDevice: {}", reinitializedStateOfDevice);
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.other);
        }
    }

    /**
     * Override as required.
     *
     * @param localDevice
     * @param from
     * @throws BACnetErrorException
     */
    protected void coldstart(final LocalDevice localDevice, final Address from) throws BACnetErrorException {
        throw new BACnetErrorException(ErrorClass.device, ErrorCode.notConfigured);
    }

    /**
     * Override as required.
     *
     * @param localDevice
     * @param from
     * @throws BACnetErrorException
     */
    protected void warmstart(final LocalDevice localDevice, final Address from) throws BACnetErrorException {
        throw new BACnetErrorException(ErrorClass.device, ErrorCode.notConfigured);
    }

    /**
     * Override as required.
     *
     * @param localDevice
     * @param from
     * @throws BACnetErrorException
     */
    protected void startBackup(final LocalDevice localDevice, final Address from) throws BACnetErrorException {
        LOG.info("Starting backup");
        final DeviceObject dev = localDevice.getDeviceObject();

        final BackupState backupState = dev.get(PropertyIdentifier.backupAndRestoreState);
        if (backupState.isOneOf( //
                BackupState.preparingForBackup, //
                BackupState.preparingForRestore, //
                BackupState.performingABackup, //
                BackupState.performingARestore)) {
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.configurationInProgress);
        }

        // Indicate preparing for backup.
        dev.writePropertyInternal(PropertyIdentifier.backupAndRestoreState, BackupState.preparingForBackup);

        // Do the remainder in a separate thread so that the response can be sent immediately.
        localDevice.execute(() -> {
            try {
                createConfigurationFiles(localDevice);

                // Maybe monitor the backup?
                maybeCreateMonitor(localDevice, from);

                // Indicate ready for backup.
                dev.writePropertyInternal(PropertyIdentifier.backupAndRestoreState, BackupState.performingABackup);
            } catch (final BACnetServiceException | IOException e) {
                LOG.error("Error during backup", e);
                dev.writePropertyInternal(PropertyIdentifier.backupAndRestoreState, BackupState.backupFailure);
            }
        });
    }

    protected void createConfigurationFiles(final LocalDevice localDevice) throws BACnetServiceException, IOException {
        // Ask persistence for its files. Create copies and then file object for the copies.
        final File[] files = localDevice.getPersistence().getFiles();
        BACnetArray<ObjectIdentifier> configurationFiles;
        if (files != null) {
            final List<ObjectIdentifier> fileOids = new ArrayList<>(files.length);
            for (final File file : files) {
                final File copy = new File(file.getParentFile(), file.getName() + ".backup");
                if (copy.exists())
                    copy.delete();
                Files.copy(file.toPath(), copy.toPath());

                final int instanceNumber = localDevice.getNextInstanceObjectNumber(ObjectType.file);
                final FileObject fo = new FileObject(localDevice, instanceNumber, "configurationFile",
                        new StreamAccess(copy));
                fileOids.add(fo.getId());
            }
            configurationFiles = new BACnetArray<>(fileOids);
        } else {
            configurationFiles = new BACnetArray<>();
        }
        localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.configurationFiles, configurationFiles);
    }

    /**
     * Override as required.
     *
     * @param localDevice
     * @param from
     * @throws BACnetErrorException
     */
    protected void endBackup(final LocalDevice localDevice, final Address from) throws BACnetErrorException {
        LOG.info("Ending backup");
        final BackupState backupState = localDevice.getDeviceObject().get(PropertyIdentifier.backupAndRestoreState);
        if (!backupState.isOneOf( //
                BackupState.performingABackup, //
                BackupState.backupFailure)) {
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.configurationInProgress);
        }

        cleanupBackupRestore(localDevice);
    }

    protected void maybeCreateMonitor(final LocalDevice localDevice, final Address from) {
        // Backup failure timeout
        final UnsignedInteger backupFailureTimeout = localDevice.getDeviceObject()
                .get(PropertyIdentifier.backupFailureTimeout);
        if (backupFailureTimeout.intValue() > 0) {
            backupStateMonitor = new BackupStateMonitor(localDevice, from, backupFailureTimeout.intValue() * 1000);
        }
    }

    // 19.1.2.5, 19.1.3.4
    // If device B does not receive any messages related to the backup procedure from device A for the number of
    // seconds specified in the Backup_Failure_Timeout property of its Device object, device B should assume that the
    // backup procedure has been aborted, and device B should exit backup mode. A message related to the backup
    // procedure is defined to be any ReadProperty, ReadPropertyMultiple, WriteProperty, WritePropertyMultiple,
    // CreateObject, or AtomicReadFile request that directly accesses a configuration File object.
    private static final AtomicInteger nextId = new AtomicInteger(15);


    class BackupStateMonitor {
        private final int id = nextId.getAndIncrement();
        private final LocalDevice localDevice;
        private final DeviceEventAdapter listener;

        private volatile long deadline;
        private final ScheduledFuture<?> future;

        BackupStateMonitor(final LocalDevice localDevice, final Address remoteAddress, final long timeout) {
            LOG.info("Created timeout monitor {}", id);
            this.localDevice = localDevice;

            deadline = localDevice.getClock().millis() + timeout;

            // Register a listener to monitor the requests that are arriving.
            listener = new DeviceEventAdapter() {
                @Override
                public void requestReceived(final Address from, final Service service) {
                    if (remoteAddress.equals(from)) {
                        // Request is from the device that started the backup.
                        if (service instanceof ReadPropertyRequest //
                                || service instanceof ReadPropertyMultipleRequest //
                                || service instanceof WritePropertyRequest //
                                || service instanceof WritePropertyMultipleRequest //
                                || service instanceof CreateObjectRequest //
                                || service instanceof AtomicReadFileRequest //
                                || service instanceof AtomicWriteFileRequest) {
                            // The request is of a type as described above. We will assume it is to do with backup
                            // instead of actually check that it is directed toward one of the configuration files.

                            // Refresh the deadline
                            LOG.info("Timeout deadline refreshed in monitor {}", id);
                            deadline = localDevice.getClock().millis() + timeout;
                        }
                    }
                }
            };
            localDevice.getEventHandler().addListener(listener);

            // Run a task every second to check if the deadline has been past. This is simpler than canceling a task
            // and creating a new one on every qualifying request.
            future = localDevice.scheduleWithFixedDelay(() -> {
                LOG.info("Checking for timeout in monitor {}", id);
                if (deadline < localDevice.getClock().millis()) {
                    // Timeout
                    synchronized (DefaultReinitializeDeviceHandler.this) {
                        final BackupState backupState = localDevice.getDeviceObject()
                                .get(PropertyIdentifier.backupAndRestoreState);
                        LOG.warn("Backup/restore timeout occurred when backup state is {}", backupState);
                        if (backupState.isOneOf(BackupState.performingABackup, BackupState.performingARestore)) {
                            cleanupBackupRestore(localDevice);
                        } else {
                            LOG.warn(
                                    "Timeout occurred, but the backup state is not performingABackup or performingARestore: {}",
                                    backupState);
                        }
                    }
                }
            }, 1, 1, TimeUnit.SECONDS);
        }

        void cancel() {
            LOG.info("Canceling timeout monitor {}", id);
            if (!future.cancel(false)) {
                LOG.warn("Failed to cancel timeout monitor {}", id, new Exception());
            }
            localDevice.getEventHandler().removeListener(listener);
            backupStateMonitor = null;
        }
    }

    protected void cleanupBackupRestore(final LocalDevice localDevice) {
        LOG.info("Running backup/restore cleanup");
        if (backupStateMonitor != null) {
            backupStateMonitor.cancel();
        }

        final DeviceObject dev = localDevice.getDeviceObject();

        // Remove the configuration files.
        final BACnetArray<ObjectIdentifier> configurationFiles = dev.get(PropertyIdentifier.configurationFiles);
        dev.writePropertyInternal(PropertyIdentifier.configurationFiles, new BACnetArray<>());

        for (final ObjectIdentifier fileOid : configurationFiles) {
            try {
                final FileObject fo = (FileObject) localDevice.removeObject(fileOid);
                if (!fo.getFileAccess().delete()) {
                    LOG.warn("Failed to delete configuration file " + fo.getFileAccess().getName());
                }
            } catch (final BACnetServiceException e) {
                LOG.error("Error while trying to remove configuration file", e);
            }
        }

        // Reset the backup state
        dev.writePropertyInternal(PropertyIdentifier.backupAndRestoreState, BackupState.idle);
    }

    /**
     * Override as required.
     *
     * @param localDevice
     * @param from
     * @throws BACnetErrorException
     */
    protected void startRestore(final LocalDevice localDevice, final Address from) throws BACnetErrorException {
        LOG.info("Starting restore");
        final DeviceObject dev = localDevice.getDeviceObject();

        final BackupState backupState = dev.get(PropertyIdentifier.backupAndRestoreState);
        if (backupState.isOneOf( //
                BackupState.preparingForBackup, //
                BackupState.preparingForRestore, //
                BackupState.performingABackup, //
                BackupState.performingARestore)) {
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.configurationInProgress);
        }

        // Indicate preparing for restore.
        dev.writePropertyInternal(PropertyIdentifier.backupAndRestoreState, BackupState.preparingForRestore);

        // Do the remainder in a separate thread so that the response can be sent immediately.
        localDevice.execute(() -> {
            try {
                createConfigurationFiles(localDevice);

                // Maybe monitor the backup?
                maybeCreateMonitor(localDevice, from);

                // Indicate ready for restore.
                dev.writePropertyInternal(PropertyIdentifier.backupAndRestoreState, BackupState.performingARestore);
            } catch (final BACnetServiceException | IOException e) {
                LOG.error("Error during backup", e);
                dev.writePropertyInternal(PropertyIdentifier.backupAndRestoreState, BackupState.restoreFailure);
            }
        });
    }

    /**
     * Override as required.
     *
     * @param localDevice
     * @param from
     * @throws BACnetErrorException
     */
    protected void endRestore(final LocalDevice localDevice, final Address from) throws BACnetErrorException {
        LOG.info("Ending restore");
        final DeviceObject dev = localDevice.getDeviceObject();

        final BackupState backupState = dev.get(PropertyIdentifier.backupAndRestoreState);
        if (!backupState.isOneOf( //
                BackupState.performingARestore, //
                BackupState.restoreFailure)) {
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.configurationInProgress);
        }

        finishRestore(localDevice);

        localDevice.incrementDatabaseRevision();
        dev.writePropertyInternal(PropertyIdentifier.lastRestoreTime, new TimeStamp(new DateTime(localDevice)));

        cleanupBackupRestore(localDevice);
    }

    /**
     * This method should be overridden to properly validate and implement the files that were restored. Since not all
     * of the configuration files were necessarily written, and since new file objects may have been created for new
     * files, the list of files should be derived from the existing file objects. Also, file objects that are detected
     * as being new should be removed as required, since this handler will only remove file objects that it created, as
     * given by the configurationFiles property.
     *
     * @param localDevice
     */
    protected void finishRestore(final LocalDevice localDevice) {
        // Override as required
    }

    /**
     * Override as required.
     *
     * @param localDevice
     * @param from
     * @throws BACnetErrorException
     */
    protected void abortRestore(final LocalDevice localDevice, final Address from) throws BACnetErrorException {
        LOG.info("Aborting restore");
        final BackupState backupState = localDevice.getDeviceObject().get(PropertyIdentifier.backupAndRestoreState);
        if (!backupState.equals(BackupState.performingARestore)) {
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.configurationInProgress);
        }

        cleanupBackupRestore(localDevice);
    }

    /**
     * Override as required.
     *
     * @param localDevice
     * @param from
     * @throws BACnetErrorException
     */
    protected void activateChanges(final LocalDevice localDevice, final Address from) throws BACnetErrorException {
        throw new BACnetErrorException(ErrorClass.device, ErrorCode.notConfigured);
    }
}
