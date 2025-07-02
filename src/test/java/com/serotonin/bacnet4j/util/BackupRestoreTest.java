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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.event.DefaultReinitializeDeviceHandler;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.FileObject;
import com.serotonin.bacnet4j.obj.fileAccess.CrlfDelimitedFileAccess;
import com.serotonin.bacnet4j.obj.fileAccess.StreamAccess;
import com.serotonin.bacnet4j.persistence.FilePersistence;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

/**
 * Testing of backup and restore procedures as described in 19.1.
 *
 * @author Matthew
 */
public class BackupRestoreTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final LocalDevice a = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
    private final LocalDevice b = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0)));

    private File file;

    @Before
    public void before() throws Exception {
        final String filename = "backupTest.txt";
        final String path = getClass().getClassLoader().getResource(filename).getPath();
        file = new File(path);

        b.setPersistence(new FilePersistence(file));

        a.initialize();
        b.initialize();
    }

    @After
    public void after() {
        a.terminate();
        b.terminate();
    }

    /**
     * Perform a "real" backup and restore.
     *
     * @throws Exception
     */
    @Test
    public void backupAndRestore() throws Exception {
        // Make sure the test file exists.
        assertEquals(true, file.exists());

        // Create a custom reinitialization handler
        final List<File> files = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean restoreSuccess = new AtomicBoolean(false);
        b.setReinitializeDeviceHandler(new DefaultReinitializeDeviceHandler() {
            // Override createConfigurationFiles to create file objects of both stream and record access.
            @Override
            protected void createConfigurationFiles(final LocalDevice localDevice)
                    throws BACnetServiceException, IOException {
                final File streamFile = copy(file, "stream");
                final File recordFile = copy(file, "record");

                final FileObject stream = new FileObject(localDevice,
                        localDevice.getNextInstanceObjectNumber(ObjectType.file), "configurationFile",
                        new StreamAccess(streamFile));
                final FileObject record = new FileObject(localDevice,
                        localDevice.getNextInstanceObjectNumber(ObjectType.file), "configurationFile",
                        new CrlfDelimitedFileAccess(recordFile));
                localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.configurationFiles,
                        new BACnetArray<>(stream.getId(), record.getId()));
            }

            // Override finishRestore to check the copied files and ensure that they match the originals.
            @Override
            protected void finishRestore(final LocalDevice localDevice) {
                try {
                    // NOTE: The configuration files property is used here, but for production purposes the file
                    // objects should be used as the definitive source of configuration files.
                    final BACnetArray<ObjectIdentifier> configurationFiles = localDevice.getDeviceObject()
                            .get(PropertyIdentifier.configurationFiles);
                    assertEquals(2, configurationFiles.size());

                    // Verify the file copy here because the file will be deleted in the clean up.
                    final FileObject stream = (FileObject) localDevice.getObject(configurationFiles.get(0));
                    final File restoredStream = ((StreamAccess) stream.getFileAccess()).getFile();

                    final FileObject record = (FileObject) localDevice.getObject(configurationFiles.get(1));
                    final File restoredRecord = ((CrlfDelimitedFileAccess) record.getFileAccess()).getFile();

                    try {
                        TestUtils.assertFileContentEquals(restoredStream, file);
                        TestUtils.assertFileContentEquals(restoredRecord, file);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }

                    restoreSuccess.set(true);
                } finally {
                    latch.countDown();
                }
            }
        });

        try {
            //
            // Backup
            final BackupClient backupClient = new BackupClient(a, 2, null);
            files.addAll(backupClient.begin(file.getParentFile()));

            assertEquals(2, files.size());
            // Only check the stream file contents, because the record file will be different. The restored record file
            // can be verified through.
            TestUtils.assertFileContentEquals(file, files.get(0));

            //
            // Restore
            final RestoreClient restoreClient = new RestoreClient(a, 2, null);
            restoreClient.begin(files);

            latch.await();
            assertEquals(true, restoreSuccess.get());
        } finally {
            for (final File f : files) {
                f.delete();
            }
        }
    }

    private static File copy(final File from, final String extension) throws IOException {
        final File copy = new File(from.getParentFile(), from.getName() + "." + extension);
        if (copy.exists())
            copy.delete();
        Files.copy(from.toPath(), copy.toPath());
        return copy;
    }
}
