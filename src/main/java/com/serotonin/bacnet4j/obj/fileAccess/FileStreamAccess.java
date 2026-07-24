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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

/**
 * Stream access backed by a file on disk.
 */
public class FileStreamAccess extends StreamAccess {
    static final Logger LOG = LoggerFactory.getLogger(FileStreamAccess.class);

    private final File file;

    public FileStreamAccess(final File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public boolean canWrite() {
        return file.canWrite();
    }

    @Override
    public boolean delete() {
        try {
            Files.delete(file.toPath());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void writeFileSize(final long fileSize) {
        try {
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                raf.setLength(fileSize);
            }
        } catch (final IOException e) {
            LOG.error("Failed to write file size {}", fileSize, e);
        }
    }

    @Override
    public OctetString readData(final long start, final long length) throws IOException {
        long skip = start;
        try (FileInputStream in = new FileInputStream(file)) {
            while (skip > 0) {
                final long result = in.skip(skip);
                if (result == -1)
                    // EOF
                    break;
                skip -= result;
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            StreamUtils.transfer(in, out, length);
            return new OctetString(out.toByteArray());
        }
    }

    @Override
    public long writeData(final long start, final OctetString data) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            final long actualStart = start == -1 ? raf.length() : start;

            if (actualStart > raf.length()) {
                raf.setLength(actualStart);
            }
            raf.seek(actualStart);

            final byte[] bytes = data.getBytes();
            raf.write(bytes);

            return actualStart;
        }
    }
}
