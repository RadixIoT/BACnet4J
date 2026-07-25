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

package com.serotonin.bacnet4j.obj.fileAccess;

import java.util.Arrays;

import com.serotonin.bacnet4j.type.primitive.OctetString;

/**
 * Stream access backed by a byte array in memory. The content does not survive a restart; this is
 * intended for tests and for transient files that do not warrant disk storage.
 */
public class InMemoryStreamAccess extends StreamAccess {
    private final String name;
    private byte[] data;
    private boolean exists = true;
    private long lastModified;

    public InMemoryStreamAccess(final String name) {
        this(name, new byte[0]);
    }

    public InMemoryStreamAccess(final String name, final byte[] initialContent) {
        this.name = name;
        this.data = initialContent.clone();
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * Returns a copy of the full current content.
     */
    public synchronized byte[] getData() {
        return data.clone();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public synchronized boolean exists() {
        return exists;
    }

    @Override
    public synchronized long length() {
        return data.length;
    }

    @Override
    public synchronized long lastModified() {
        return lastModified;
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public synchronized boolean delete() {
        data = new byte[0];
        exists = false;
        return true;
    }

    @Override
    public synchronized void writeFileSize(final long fileSize) {
        data = Arrays.copyOf(data, (int) fileSize);
        modified();
    }

    @Override
    public synchronized OctetString readData(final long start, final long length) {
        if (start >= data.length) {
            return new OctetString(new byte[0]);
        }
        int from = (int) start;
        int to = (int) Math.min(data.length, start + length);
        return new OctetString(Arrays.copyOfRange(data, from, to));
    }

    @Override
    public synchronized long writeData(final long start, final OctetString octets) {
        final long actualStart = start == -1 ? data.length : start;
        final byte[] bytes = octets.getBytes();
        final int end = (int) actualStart + bytes.length;
        if (end > data.length) {
            data = Arrays.copyOf(data, end);
        }
        System.arraycopy(bytes, 0, data, (int) actualStart, bytes.length);
        modified();
        return actualStart;
    }

    private void modified() {
        exists = true;
        lastModified = System.currentTimeMillis();
    }
}
