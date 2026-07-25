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

package com.serotonin.bacnet4j.service.confirmed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.AnalogInputObject;
import com.serotonin.bacnet4j.obj.FileObject;
import com.serotonin.bacnet4j.obj.fileAccess.CrlfDelimitedFileAccess;
import com.serotonin.bacnet4j.obj.fileAccess.FileStreamAccess;
import com.serotonin.bacnet4j.service.acknowledgement.AtomicReadFileAck;
import com.serotonin.bacnet4j.service.acknowledgement.AtomicWriteFileAck;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

public class AtomicWriteFileRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final String filename = "fileObjectTest.txt";
    private final String path = Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).getPath();

    private LocalDevice d1;
    private AnalogInputObject ai;

    @Before
    public void before() throws Exception {
        d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        ai = d1.addObject(new AnalogInputObject(d1, 0, "ai", 0, EngineeringUnits.noUnits, false));
    }

    @After
    public void after() {
        d1.terminate();
    }

    @Test
    public void errors() {
        // Use an oid what doesn't exist.
        TestUtils.assertRequestHandleException(() ->
                        new AtomicWriteFileRequest(new ObjectIdentifier(ObjectType.file, 0),
                                new AtomicWriteFileRequest.StreamAccess(
                                        new SignedInteger(2), new OctetString(new byte[0]))).handle(d1, null)
                , ErrorClass.object, ErrorCode.unknownObject);

        // Use an oid what is not a file object.
        TestUtils.assertRequestHandleException(() -> new AtomicWriteFileRequest(ai.getId(),
                new AtomicWriteFileRequest.StreamAccess(new SignedInteger(2), new OctetString(new byte[0]))
        ).handle(d1, null), ErrorClass.services, ErrorCode.inconsistentObjectType);
    }

    @Test
    public void streamAccess() throws Exception {
        // Create the file object to use.
        FileObject f = d1.addObject(new FileObject(d1, 0, "test", new FileStreamAccess(new File(path))));

        // Write starting at -2.
        TestUtils.assertRequestHandleException(() ->
                        new AtomicWriteFileRequest(f.getId(),
                                new AtomicWriteFileRequest.StreamAccess(
                                        new SignedInteger(-2), new OctetString(new byte[0]))).handle(d1, null)
                , ErrorClass.object, ErrorCode.invalidFileStartPosition);

        // Try to write records.
        TestUtils.assertRequestHandleException(() ->
                        new AtomicWriteFileRequest(f.getId(),
                                new AtomicWriteFileRequest.RecordAccess(
                                        new SignedInteger(0), new UnsignedInteger(10), new SequenceOf<>())).handle(d1, null)
                , ErrorClass.services, ErrorCode.invalidFileAccessMethod);

        // Do a legitimate write into an existing range.
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new FileStreamAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(600), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertFalse(wack.isRecordAccess());
            assertEquals(new SignedInteger(600), wack.getFileStart());

            // Do a read to confirm the change.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(599), new UnsignedInteger(7))).handle(d1, null);
            assertEquals(new OctetString("B!@#$%H".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write to exactly the end of the file.
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new FileStreamAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(917), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertFalse(wack.isRecordAccess());
            assertEquals(new SignedInteger(917), wack.getFileStart());

            // Do a read to confirm the change.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(916), new UnsignedInteger(7))).handle(d1, null);
            assertEquals(new OctetString("w!@#$%".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write a bit beyond the end of the file.
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new FileStreamAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(919), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertFalse(wack.isRecordAccess());
            assertEquals(new SignedInteger(919), wack.getFileStart());

            // Do a read to confirm the change.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(918), new UnsignedInteger(7))).handle(d1, null);
            assertEquals(new OctetString("y!@#$%".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write starting beyond the end of the file.
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new FileStreamAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(930), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertFalse(wack.isRecordAccess());
            assertEquals(new SignedInteger(930), wack.getFileStart());

            // Do a read to confirm the change.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(919), new UnsignedInteger(16))).handle(d1, null);
            assertEquals(new OctetString(new byte[] {'z', '\r', '\n', 0, 0, 0, 0, 0, 0, 0, 0, '!', '@', '#', '$', '%'}),
                    rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write append.
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new FileStreamAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(-1), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertFalse(wack.isRecordAccess());
            assertEquals(new SignedInteger(922), wack.getFileStart());

            // Do a read to confirm the change.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(919), new UnsignedInteger(16))).handle(d1, null);
            assertEquals(new OctetString("z\r\n!@#$%".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });
    }

    /**
     * A stream-access write at start=0 with a payload shorter than the current file must
     * overlay the leading bytes and leave the tail untouched. The BACnet spec (clause 14.2.5)
     * is silent on this case; the deliberate behavior for BACnet4J is to preserve the tail —
     * clients that want to shrink the file use WriteProperty(File_Size, ...) instead. This
     * test pins that choice against silent regression.
     */
    @Test
    public void streamAccess_startZeroShorterPreservesTail() throws Exception {
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new FileStreamAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.StreamAccess(new SignedInteger(0), new OctetString("hello".getBytes())))
                    .handle(d1, null);
            assertFalse(wack.isRecordAccess());
            assertEquals(new SignedInteger(0), wack.getFileStart());

            // File length is unchanged.
            assertEquals(new UnsignedInteger(922), f1.readProperty(PropertyIdentifier.fileSize, null));

            // The overlay covers bytes 0-4; bytes 5-9 are the original "56789" tail.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(0), new UnsignedInteger(10))).handle(d1, null);
            assertEquals(new OctetString("hello56789".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });
    }

    /**
     * A stream-access write at start=0 with an empty payload must be a no-op — file length
     * and content unchanged. Companion to {@link #streamAccess_startZeroShorterPreservesTail};
     * pins that a zero-length AtomicWriteFile does not clear the file.
     */
    @Test
    public void streamAccess_startZeroEmptyIsNoOp() throws Exception {
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new FileStreamAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.StreamAccess(new SignedInteger(0), new OctetString(new byte[0])))
                    .handle(d1, null);
            assertFalse(wack.isRecordAccess());
            assertEquals(new SignedInteger(0), wack.getFileStart());

            // File length and content unchanged.
            assertEquals(new UnsignedInteger(922), f1.readProperty(PropertyIdentifier.fileSize, null));
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(0), new UnsignedInteger(10))).handle(d1, null);
            assertEquals(new OctetString("0123456789".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });
    }

    @Test
    public void recordAccess() throws Exception {
        // Create the file object to use.
        FileObject f = d1.addObject(new FileObject(d1, 0, "test", new CrlfDelimitedFileAccess(new File(path))));

        // Write starting at -2.
        TestUtils.assertRequestHandleException(() ->
                        new AtomicWriteFileRequest(f.getId(),
                                new AtomicWriteFileRequest.RecordAccess(
                                        new SignedInteger(-2), UnsignedInteger.ZERO,
                                        new SequenceOf<>(new OctetString(new byte[0])))).handle(d1, null)
                , ErrorClass.object, ErrorCode.invalidFileStartPosition);

        // Try to write data.
        TestUtils.assertRequestHandleException(() ->
                        new AtomicWriteFileRequest(f.getId(),
                                new AtomicWriteFileRequest.StreamAccess(
                                        new SignedInteger(0), new OctetString(new byte[0]))).handle(d1, null)
                , ErrorClass.services, ErrorCode.invalidFileAccessMethod);

        // Do a legitimate write of an existing range.
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new CrlfDelimitedFileAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(0), new UnsignedInteger(3), new SequenceOf<>( //
                            new OctetString("Write 1".getBytes()), //
                            new OctetString("Write 2".getBytes()), //
                            new OctetString("Write 3".getBytes())))).handle(d1, null);
            assertTrue(wack.isRecordAccess());
            assertEquals(new SignedInteger(0), wack.getFileStart());

            // Do a read to confirm the change.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.RecordAccess(
                            new SignedInteger(0), new UnsignedInteger(4))).handle(d1, null);
            assertEquals(new SequenceOf<>( //
                            new OctetString("Write 1".getBytes()), //
                            new OctetString("Write 2".getBytes()), //
                            new OctetString("Write 3".getBytes()), //
                            new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzX".getBytes())),
                    rack.getRecordAccess().getFileRecordData());
            assertEquals(new UnsignedInteger(14), f1.readProperty(PropertyIdentifier.recordCount, null));

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write past the end of the file
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new CrlfDelimitedFileAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(12), new UnsignedInteger(3), new SequenceOf<>( //
                            new OctetString("Write 1".getBytes()), //
                            new OctetString("Write 2".getBytes()), //
                            new OctetString("Write 3".getBytes())))).handle(d1, null);
            assertTrue(wack.isRecordAccess());
            assertEquals(new SignedInteger(12), wack.getFileStart());

            // Do a read to confirm the change.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.RecordAccess(
                            new SignedInteger(11), new UnsignedInteger(6))).handle(d1, null);
            assertEquals(new SequenceOf<>( //
                    new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                    new OctetString("Write 1".getBytes()), //
                    new OctetString("Write 2".getBytes()), //
                    new OctetString("Write 3".getBytes())), rack.getRecordAccess().getFileRecordData());
            assertEquals(new UnsignedInteger(15), f1.readProperty(PropertyIdentifier.recordCount, null));

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write starting past the end of the file
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new CrlfDelimitedFileAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(16), new UnsignedInteger(3), new SequenceOf<>( //
                            new OctetString("Write 1".getBytes()), //
                            new OctetString("Write 2".getBytes()), //
                            new OctetString("Write 3".getBytes())))).handle(d1, null);
            assertTrue(wack.isRecordAccess());
            assertEquals(new SignedInteger(16), wack.getFileStart());

            // Do a read to confirm the change.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.RecordAccess(
                            new SignedInteger(12), new UnsignedInteger(10))).handle(d1, null);
            assertEquals(new SequenceOf<>( //
                    new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzX".getBytes()),
                    new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                    new OctetString(new byte[0]), //
                    new OctetString(new byte[0]), //
                    new OctetString("Write 1".getBytes()), //
                    new OctetString("Write 2".getBytes()), //
                    new OctetString("Write 3".getBytes())), rack.getRecordAccess().getFileRecordData());
            assertEquals(new UnsignedInteger(19), f1.readProperty(PropertyIdentifier.recordCount, null));

            d1.removeObject(f1.getId());
        });


        // Do a legitimate write appending
        doInCopy(file -> {
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new CrlfDelimitedFileAccess(file)));
            AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(-1), new UnsignedInteger(3), new SequenceOf<>( //
                            new OctetString("Write 1".getBytes()), //
                            new OctetString("Write 2".getBytes()), //
                            new OctetString("Write 3".getBytes())))).handle(d1, null);
            assertTrue(wack.isRecordAccess());
            assertEquals(new SignedInteger(14), wack.getFileStart());

            // Do a read to confirm the change.
            AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new AtomicReadFileRequest.RecordAccess(
                            new SignedInteger(12), new UnsignedInteger(10))).handle(d1, null);
            assertEquals(new SequenceOf<>( //
                    new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzX".getBytes()),
                    new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                    new OctetString("Write 1".getBytes()), //
                    new OctetString("Write 2".getBytes()), //
                    new OctetString("Write 3".getBytes())), rack.getRecordAccess().getFileRecordData());
            assertEquals(new UnsignedInteger(17), f1.readProperty(PropertyIdentifier.recordCount, null));

            d1.removeObject(f1.getId());
        });
    }

    @Test
    public void modificationDate() throws Exception {
        doInCopy(file -> {
            long originalTime = file.lastModified();
            ThreadUtils.sleep(5); // Ensures that the time changes from the original
            FileObject f1 = d1.addObject(new FileObject(d1, 1, "test", new FileStreamAccess(file)));
            new AtomicWriteFileRequest(f1.getId(),
                    new AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(600), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            long changedTime = file.lastModified();
            assertTrue(originalTime <= changedTime);
        });
    }

    @FunctionalInterface
    interface InCopyCommand {
        void doInCopy(File file) throws Exception;
    }

    private void doInCopy(InCopyCommand command) throws Exception {
        File source = new File(Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).toURI());
        File target = new File(source.getParentFile(), source.getName() + ".tmp");
        if (Files.exists(target.toPath()))
            Files.delete(target.toPath());
        Files.copy(source.toPath(), target.toPath());
        command.doInCopy(target);
        Files.delete(target.toPath());
    }
}
