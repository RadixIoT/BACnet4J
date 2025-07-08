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

import java.io.IOException;

import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.FileAccessMethod;
import com.serotonin.bacnet4j.type.primitive.OctetString;

/**
 * Placeholder file access instance that does nothing.
 *
 * @author Matthew
 */
public class NullFileAccess implements FileAccess {
    @Override
    public String getName() {
        return "Null";
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public FileAccessMethod getAccessMethod() {
        return FileAccessMethod.streamAccess;
    }

    @Override
    public long length() {
        return 0;
    }

    @Override
    public long lastModified() {
        return 0;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public long recordCount() {
        return 0;
    }

    @Override
    public void writeFileSize(final long fileSize) {
        // no op
    }

    @Override
    public void writeRecordCount(final long recordCount) {
        // no op
    }

    @Override
    public boolean supportsStreamAccess() {
        return true;
    }

    @Override
    public OctetString readData(final long start, final long length) throws IOException, BACnetServiceException {
        return new OctetString(new byte[0]);
    }

    @Override
    public long writeData(final long start, final OctetString data) throws IOException, BACnetServiceException {
        return 0;
    }

    @Override
    public boolean supportsRecordAccess() {
        return false;
    }

    @Override
    public SequenceOf<OctetString> readRecords(final long start, final long count)
            throws IOException, BACnetServiceException {
        throw new BACnetServiceException(ErrorClass.services, ErrorCode.invalidFileAccessMethod);
    }

    @Override
    public long writeRecords(final long start, final SequenceOf<OctetString> records)
            throws IOException, BACnetServiceException {
        throw new BACnetServiceException(ErrorClass.services, ErrorCode.invalidFileAccessMethod);
    }
}
