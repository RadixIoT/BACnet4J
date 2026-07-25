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

package com.serotonin.bacnet4j.obj.fileAccess;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.serotonin.bacnet4j.type.enumerated.FileAccessMethod;
import com.serotonin.bacnet4j.type.primitive.OctetString;

/**
 * Behavior of the in-memory stream access, mirroring the semantics of the file-backed implementation:
 * position-based reads and writes, extension on write past the end, append with start -1, and truncation
 * or extension via writeFileSize.
 */
public class InMemoryStreamAccessTest {
    @Test
    public void initialState() {
        var access = new InMemoryStreamAccess("test.pem");
        assertEquals("test.pem", access.getName());
        assertEquals(FileAccessMethod.streamAccess, access.getAccessMethod());
        assertTrue(access.exists());
        assertFalse(access.isDirectory());
        assertTrue(access.canWrite());
        assertTrue(access.supportsStreamAccess());
        assertFalse(access.supportsRecordAccess());
        assertEquals(0, access.length());
    }

    @Test
    public void initialContentIsCopied() {
        byte[] content = "hello".getBytes();
        var access = new InMemoryStreamAccess("test", content);
        content[0] = 'X';
        assertArrayEquals("hello".getBytes(), access.getData());
    }

    @Test
    public void readSlice() {
        var access = new InMemoryStreamAccess("test", "abcdefgh".getBytes());
        assertArrayEquals("cde".getBytes(), access.readData(2, 3).getBytes());
        // Read past the end is truncated.
        assertArrayEquals("gh".getBytes(), access.readData(6, 10).getBytes());
        // Read entirely beyond the end is empty.
        assertArrayEquals(new byte[0], access.readData(100, 10).getBytes());
    }

    @Test
    public void writeAtPosition() {
        var access = new InMemoryStreamAccess("test", "abcdefgh".getBytes());
        long start = access.writeData(2, new OctetString("XY".getBytes()));
        assertEquals(2, start);
        assertArrayEquals("abXYefgh".getBytes(), access.getData());
    }

    @Test
    public void writePastEndExtends() {
        var access = new InMemoryStreamAccess("test", "abc".getBytes());
        access.writeData(5, new OctetString("XY".getBytes()));
        assertArrayEquals(new byte[] {'a', 'b', 'c', 0, 0, 'X', 'Y'}, access.getData());
    }

    @Test
    public void writeAtMinusOneAppends() {
        var access = new InMemoryStreamAccess("test", "abc".getBytes());
        long start = access.writeData(-1, new OctetString("de".getBytes()));
        assertEquals(3, start);
        assertArrayEquals("abcde".getBytes(), access.getData());
    }

    @Test
    public void writeFileSizeTruncatesAndExtends() {
        var access = new InMemoryStreamAccess("test", "abcdefgh".getBytes());
        access.writeFileSize(3);
        assertArrayEquals("abc".getBytes(), access.getData());
        access.writeFileSize(5);
        assertArrayEquals(new byte[] {'a', 'b', 'c', 0, 0}, access.getData());
    }

    @Test
    public void deleteClearsContentAndExistence() {
        var access = new InMemoryStreamAccess("test", "abc".getBytes());
        assertTrue(access.delete());
        assertFalse(access.exists());
        assertEquals(0, access.length());

        // A write brings it back into existence.
        access.writeData(0, new OctetString("new".getBytes()));
        assertTrue(access.exists());
        assertArrayEquals("new".getBytes(), access.getData());
    }
}
