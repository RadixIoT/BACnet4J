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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

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
import com.serotonin.bacnet4j.obj.fileAccess.StreamAccess;
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

public class AtomicWriteFileRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final String filename = "fileObjectTest.txt";
    private final String path = getClass().getClassLoader().getResource(filename).getPath();

    private LocalDevice d1;
    private AnalogInputObject ai;

    @Before
    public void before() throws Exception {
        d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        ai = new AnalogInputObject(d1, 0, "ai", 0, EngineeringUnits.noUnits, false);
    }

    @After
    public void after() {
        d1.terminate();
    }

    @Test
    public void errors() throws Exception {
        // Use an oid what doesn't exist.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicWriteFileRequest(new ObjectIdentifier(ObjectType.file, 0),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(2), new OctetString(new byte[0]))).handle(d1, null);
        }, ErrorClass.object, ErrorCode.unknownObject);

        // Use an oid what is not a file object.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicWriteFileRequest(ai.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(2), new OctetString(new byte[0]))).handle(d1, null);
        }, ErrorClass.services, ErrorCode.inconsistentObjectType);
    }

    @Test
    public void stream() throws Exception {
        // Create the file object to use.
        final FileObject f = new FileObject(d1, 0, "test", new StreamAccess(new File(path)));

        // Write starting at -2.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicWriteFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(-2), new OctetString(new byte[0]))).handle(d1, null);
        }, ErrorClass.object, ErrorCode.invalidFileStartPosition);

        // Try to write records.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicWriteFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(0), new UnsignedInteger(10), new SequenceOf<>())).handle(d1, null);
        }, ErrorClass.services, ErrorCode.invalidFileAccessMethod);

        // Do a legitimate write into an existing range.
        doInCopy((file) -> {
            final FileObject f1 = new FileObject(d1, 1, "test", new StreamAccess(file));
            final AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(600), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertEquals(false, wack.isRecordAccess());
            assertEquals(new SignedInteger(600), wack.getFileStart());

            // Do a read to confirm the change.
            final AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(599), new UnsignedInteger(7))).handle(d1, null);
            assertEquals(new OctetString("B!@#$%H".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write to exactly the end of the file.
        doInCopy((file) -> {
            final FileObject f1 = new FileObject(d1, 1, "test", new StreamAccess(file));
            final AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(917), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertEquals(false, wack.isRecordAccess());
            assertEquals(new SignedInteger(917), wack.getFileStart());

            // Do a read to confirm the change.
            final AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(916), new UnsignedInteger(7))).handle(d1, null);
            assertEquals(new OctetString("w!@#$%".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write a bit beyond the end of the file.
        doInCopy((file) -> {
            final FileObject f1 = new FileObject(d1, 1, "test", new StreamAccess(file));
            final AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(919), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertEquals(false, wack.isRecordAccess());
            assertEquals(new SignedInteger(919), wack.getFileStart());

            // Do a read to confirm the change.
            final AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(918), new UnsignedInteger(7))).handle(d1, null);
            assertEquals(new OctetString("y!@#$%".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write starting beyond the end of the file.
        doInCopy((file) -> {
            final FileObject f1 = new FileObject(d1, 1, "test", new StreamAccess(file));
            final AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(930), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertEquals(false, wack.isRecordAccess());
            assertEquals(new SignedInteger(930), wack.getFileStart());

            // Do a read to confirm the change.
            final AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(919), new UnsignedInteger(16))).handle(d1, null);
            assertEquals(new OctetString(new byte[] {'z', '\r', '\n', 0, 0, 0, 0, 0, 0, 0, 0, '!', '@', '#', '$', '%'}),
                    rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });

        // Do a legitimate write append.
        doInCopy((file) -> {
            final FileObject f1 = new FileObject(d1, 1, "test", new StreamAccess(file));
            final AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(-1), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            assertEquals(false, wack.isRecordAccess());
            assertEquals(new SignedInteger(922), wack.getFileStart());

            // Do a read to confirm the change.
            final AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(919), new UnsignedInteger(16))).handle(d1, null);
            assertEquals(new OctetString("z\r\n!@#$%".getBytes()), rack.getStreamAccess().getFileData());

            d1.removeObject(f1.getId());
        });
    }

    @Test
    public void record() throws Exception {
        // Create the file object to use.
        final FileObject f = new FileObject(d1, 0, "test", new CrlfDelimitedFileAccess(new File(path)));

        // Write starting at -2.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicWriteFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(-2), UnsignedInteger.ZERO,
                            new SequenceOf<>(new OctetString(new byte[0])))).handle(d1, null);
        }, ErrorClass.object, ErrorCode.invalidFileStartPosition);

        // Try to write data.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicWriteFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(0), new OctetString(new byte[0]))).handle(d1, null);
        }, ErrorClass.services, ErrorCode.invalidFileAccessMethod);

        // Do a legitimate write of an existing range.
        doInCopy((file) -> {
            final FileObject f1 = new FileObject(d1, 1, "test", new CrlfDelimitedFileAccess(file));
            final AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(0), new UnsignedInteger(3), new SequenceOf<>( //
                            new OctetString("Write 1".getBytes()), //
                            new OctetString("Write 2".getBytes()), //
                            new OctetString("Write 3".getBytes())))).handle(d1, null);
            assertEquals(true, wack.isRecordAccess());
            assertEquals(new SignedInteger(0), wack.getFileStart());

            // Do a read to confirm the change.
            final AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(
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
        doInCopy((file) -> {
            final FileObject f1 = new FileObject(d1, 1, "test", new CrlfDelimitedFileAccess(file));
            final AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(12), new UnsignedInteger(3), new SequenceOf<>( //
                            new OctetString("Write 1".getBytes()), //
                            new OctetString("Write 2".getBytes()), //
                            new OctetString("Write 3".getBytes())))).handle(d1, null);
            assertEquals(true, wack.isRecordAccess());
            assertEquals(new SignedInteger(12), wack.getFileStart());

            // Do a read to confirm the change.
            final AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(
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
        doInCopy((file) -> {
            final FileObject f1 = new FileObject(d1, 1, "test", new CrlfDelimitedFileAccess(file));
            final AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(16), new UnsignedInteger(3), new SequenceOf<>( //
                            new OctetString("Write 1".getBytes()), //
                            new OctetString("Write 2".getBytes()), //
                            new OctetString("Write 3".getBytes())))).handle(d1, null);
            assertEquals(true, wack.isRecordAccess());
            assertEquals(new SignedInteger(16), wack.getFileStart());

            // Do a read to confirm the change.
            final AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(
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
        doInCopy((file) -> {
            final FileObject f1 = new FileObject(d1, 1, "test", new CrlfDelimitedFileAccess(file));
            final AtomicWriteFileAck wack = (AtomicWriteFileAck) new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.RecordAccess(
                            new SignedInteger(-1), new UnsignedInteger(3), new SequenceOf<>( //
                            new OctetString("Write 1".getBytes()), //
                            new OctetString("Write 2".getBytes()), //
                            new OctetString("Write 3".getBytes())))).handle(d1, null);
            assertEquals(true, wack.isRecordAccess());
            assertEquals(new SignedInteger(14), wack.getFileStart());

            // Do a read to confirm the change.
            final AtomicReadFileAck rack = (AtomicReadFileAck) new AtomicReadFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(
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
        doInCopy((file) -> {
            final long originalTime = file.lastModified();
            Thread.sleep(1); // Ensures that the time changes from  the original
            final FileObject f1 = new FileObject(d1, 1, "test", new StreamAccess(file));
            new AtomicWriteFileRequest(f1.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicWriteFileRequest.StreamAccess(
                            new SignedInteger(600), new OctetString("!@#$%".getBytes()))).handle(d1, null);
            final long changedTime = file.lastModified();
            assertTrue(originalTime <= changedTime);
        });
    }

    @FunctionalInterface
    static interface InCopyCommand {
        void doInCopy(File file) throws Exception;
    }

    private void doInCopy(final InCopyCommand command) throws Exception {
        final File source = new File(getClass().getClassLoader().getResource(filename).toURI());
        final File target = new File(source.getParentFile(), source.getName() + ".tmp");
        if (target.exists())
            target.delete();
        Files.copy(source.toPath(), target.toPath());
        command.doInCopy(target);
        target.delete();
    }
}
