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

import java.io.File;

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
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class AtomicReadFileRequestTest {
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
            new AtomicReadFileRequest(new ObjectIdentifier(ObjectType.file, 0),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(2), new UnsignedInteger(10))).handle(d1, null);
        }, ErrorClass.object, ErrorCode.unknownObject);

        // Use an oid what is not a file object.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicReadFileRequest(ai.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(2), new UnsignedInteger(10))).handle(d1, null);
        }, ErrorClass.services, ErrorCode.inconsistentObjectType);
    }

    @Test
    public void stream() throws Exception {
        // Create the file object to use.
        final FileObject f = new FileObject(d1, 0, "test", new StreamAccess(new File(path)));

        // Read starting at -1.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicReadFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(-1), new UnsignedInteger(10))).handle(d1, null);
        }, ErrorClass.object, ErrorCode.invalidFileStartPosition);

        // Read starting at > file size
        TestUtils.assertRequestHandleException(() -> {
            new AtomicReadFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(10000), new UnsignedInteger(10))).handle(d1, null);
        }, ErrorClass.object, ErrorCode.invalidFileStartPosition);

        // Try to read records.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicReadFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(
                            new SignedInteger(0), new UnsignedInteger(10))).handle(d1, null);
        }, ErrorClass.services, ErrorCode.invalidFileAccessMethod);

        // Do a legitimate read of an existing range.
        AtomicReadFileAck ack = (AtomicReadFileAck) new AtomicReadFileRequest(f.getId(),
                new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(new SignedInteger(600),
                        new UnsignedInteger(20))).handle(d1, null);
        assertEquals(Boolean.FALSE, ack.getEndOfFile());
        assertEquals(new SignedInteger(600), ack.getStreamAccess().getFileStartPosition());
        assertEquals(new OctetString("CDEFGHIJKLMNOPQRSTUV".getBytes()), ack.getStreamAccess().getFileData());

        // Do a legitimate read exactly to one before the end of the file.
        ack = (AtomicReadFileAck) new AtomicReadFileRequest(f.getId(),
                new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(new SignedInteger(900),
                        new UnsignedInteger(21))).handle(d1, null);
        assertEquals(Boolean.FALSE, ack.getEndOfFile());
        assertEquals(new SignedInteger(900), ack.getStreamAccess().getFileStartPosition());
        assertEquals(new OctetString("ghijklmnopqrstuvwxyz\r".getBytes()), ack.getStreamAccess().getFileData());

        // Do a legitimate read exactly to the end of the file.
        ack = (AtomicReadFileAck) new AtomicReadFileRequest(f.getId(),
                new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(new SignedInteger(900),
                        new UnsignedInteger(22))).handle(d1, null);
        assertEquals(Boolean.TRUE, ack.getEndOfFile());
        assertEquals(new SignedInteger(900), ack.getStreamAccess().getFileStartPosition());
        assertEquals(new OctetString("ghijklmnopqrstuvwxyz\r\n".getBytes()), ack.getStreamAccess().getFileData());

        // Do a legitimate read past the existing range.
        ack = (AtomicReadFileAck) new AtomicReadFileRequest(f.getId(),
                new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(new SignedInteger(900),
                        new UnsignedInteger(30))).handle(d1, null);
        assertEquals(Boolean.TRUE, ack.getEndOfFile());
        assertEquals(new SignedInteger(900), ack.getStreamAccess().getFileStartPosition());
        assertEquals(new OctetString("ghijklmnopqrstuvwxyz\r\n".getBytes()), ack.getStreamAccess().getFileData());
    }

    @Test
    public void record() throws Exception {
        // Create the file object to use.
        final FileObject f = new FileObject(d1, 0, "test", new CrlfDelimitedFileAccess(new File(path)));

        // Read starting at -1.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicReadFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(
                            new SignedInteger(-1), new UnsignedInteger(10))).handle(d1, null);
        }, ErrorClass.object, ErrorCode.invalidFileStartPosition);

        // Read starting at > record count
        TestUtils.assertRequestHandleException(() -> {
            new AtomicReadFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(
                            new SignedInteger(10000), new UnsignedInteger(10))).handle(d1, null);
        }, ErrorClass.object, ErrorCode.invalidFileStartPosition);

        // Try to read data.
        TestUtils.assertRequestHandleException(() -> {
            new AtomicReadFileRequest(f.getId(),
                    new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.StreamAccess(
                            new SignedInteger(0), new UnsignedInteger(10))).handle(d1, null);
        }, ErrorClass.services, ErrorCode.invalidFileAccessMethod);

        // Do a legitimate read of an existing range.
        AtomicReadFileAck ack = (AtomicReadFileAck) new AtomicReadFileRequest(f.getId(),
                new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(new SignedInteger(10),
                        new UnsignedInteger(3))).handle(d1, null);
        assertEquals(Boolean.FALSE, ack.getEndOfFile());
        assertEquals(new SignedInteger(10), ack.getRecordAccess().getFileStartRecord());
        assertEquals(new UnsignedInteger(3), ack.getRecordAccess().getReturnedRecordCount());
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(0));
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(1));
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzX".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(2));

        // Do a legitimate read exactly to the end of the file.
        ack = (AtomicReadFileAck) new AtomicReadFileRequest(f.getId(),
                new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(new SignedInteger(10),
                        new UnsignedInteger(4))).handle(d1, null);
        assertEquals(Boolean.TRUE, ack.getEndOfFile());
        assertEquals(new SignedInteger(10), ack.getRecordAccess().getFileStartRecord());
        assertEquals(new UnsignedInteger(4), ack.getRecordAccess().getReturnedRecordCount());
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(0));
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(1));
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzX".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(2));
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(3));

        // Do a legitimate read past the existing range.
        ack = (AtomicReadFileAck) new AtomicReadFileRequest(f.getId(),
                new com.serotonin.bacnet4j.service.confirmed.AtomicReadFileRequest.RecordAccess(new SignedInteger(10),
                        new UnsignedInteger(30))).handle(d1, null);
        assertEquals(Boolean.TRUE, ack.getEndOfFile());
        assertEquals(new SignedInteger(10), ack.getRecordAccess().getFileStartRecord());
        assertEquals(new UnsignedInteger(4), ack.getRecordAccess().getReturnedRecordCount());
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(0));
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(1));
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzX".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(2));
        assertEquals(new OctetString("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".getBytes()),
                ack.getRecordAccess().getFileRecordData().get(3));
    }
}
