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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.OctetString;

/**
 * A file access for files whose records are CRLF-delimited. Actually, because a BufferedReader is used, any line
 * delimiter is sufficient for this class to work. This is an example of how to create a record-based file access. This
 * does not necessarily use the most performant approaches, but should be reasonably illustrative of how to implement.
 *
 * @author Matthew
 */
public class CrlfDelimitedFileAccess implements RecordAccess {
    static final Logger LOG = LoggerFactory.getLogger(CrlfDelimitedFileAccess.class);

    private final File file;
    /**
     * Records are maintained in memory.
     */
    private final List<String> records = new ArrayList<>();

    public CrlfDelimitedFileAccess(final File file) throws IOException {
        this.file = file;
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                records.add(line);
            }
        }
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
    public long recordCount() {
        return records.size();
    }

    @Override
    public boolean delete() {
        return file.delete();
    }

    @Override
    public void validateFileSizeWrite(final long fileSize) throws BACnetServiceException {
        // Overridden to allow the writing of the file size.
    }

    @Override
    public void writeFileSize(final long fileSize) {
        long position = 0;
        for (int i = 0; i < records.size(); i++) {
            String record = records.get(i);
            final int len = record.length() + System.lineSeparator().length();

            if (position + len >= fileSize) {
                // Found the truncation point.
                final int clip = (int) (position + len - fileSize);
                int removeFrom;
                if (clip > record.length()) {
                    // Remove starting at the current record.
                    removeFrom = i;
                } else {
                    // Keep at least part of the record, maybe only the delimiter.
                    if (clip > 0) {
                        record = record.substring(0, record.length() - clip);
                        records.set(i, record);
                    }
                    position += record.length() + System.lineSeparator().length();
                    removeFrom = i + 1;
                }
                while (records.size() > removeFrom)
                    records.remove(records.size() - 1);
                break;
            }
            position += len;
        }

        while (position < fileSize) {
            // Pad the file until the size is >= fileSize. The record delimiter may have a length > 1, so we may go
            records.add("");
            position += System.lineSeparator().length();
        }

        writeFileLogException();
    }

    @Override
    public void validateRecordCountWrite(final long recordCount) throws BACnetServiceException {
        // Overridden to allow the writing of the record count.
    }

    @Override
    public void writeRecordCount(final long recordCount) {
        if (records.size() < recordCount) {
            // Pad with empty records.
            while (records.size() < recordCount) {
                records.add("");
            }
        } else if (records.size() > recordCount) {
            while (records.size() > recordCount)
                records.remove(records.size() - 1);
        }

        writeFileLogException();
    }

    @Override
    public SequenceOf<OctetString> readRecords(final long start, final long count)
            throws IOException, BACnetServiceException {
        final SequenceOf<OctetString> result = new SequenceOf<>();
        int position = (int) start;
        while (position < records.size() && result.size() < count) {
            result.add(new OctetString(records.get(position++).getBytes()));
        }
        return result;
    }

    @Override
    public long writeRecords(final long start, final SequenceOf<OctetString> data)
            throws IOException, BACnetServiceException {
        long result;
        if (start == -1) {
            result = records.size();
            // Append the records.
            for (final OctetString o : data) {
                records.add(new String(o.getBytes()));
            }
        } else if (start >= records.size()) {
            result = start;
            // Pad with empty records.
            final int count = (int) start - records.size();
            for (int i = 0; i < count; i++)
                records.add("");
            // Append the records.
            for (final OctetString o : data) {
                records.add(new String(o.getBytes()));
            }
        } else {
            result = start;
            // Set the records at the given position
            int pos = (int) start;
            for (final OctetString o : data) {
                if (pos >= records.size()) {
                    records.add(new String(o.getBytes()));
                } else {
                    records.set(pos, new String(o.getBytes()));
                }
                pos++;
            }
        }

        writeFileLogException();

        return result;
    }

    private void writeFileLogException() {
        try {
            writeFile();
        } catch (final IOException e) {
            LOG.error("Failed to write file data", e);
        }
    }

    private void writeFile() throws IOException {
        try (PrintWriter out = new PrintWriter(file)) {
            for (final String record : records) {
                out.println(record);
            }
        }
    }
}
