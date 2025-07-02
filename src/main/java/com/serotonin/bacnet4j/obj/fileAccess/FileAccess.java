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
 * Despite the name, implementations do not have to actually rely upon a file. For example, data can be kept in memory,
 * or record accesses could be delegated to a database. This is why this base interface does not refer to the
 * java.io.File class.
 *
 * @author Matthew
 */
public interface FileAccess {
    String getName();

    boolean exists();

    boolean delete();

    boolean isDirectory();

    FileAccessMethod getAccessMethod();

    long length();

    long lastModified();

    boolean canWrite();

    long recordCount();

    /**
     * Allows the FileAccess implementation to determine whether to allow the given file size write.
     *
     * @param fileSize the file size to write. If an exception is not thrown and other validations succeed, the
     *                 writeFileSize
     *                 method will be called.
     * @throws BACnetServiceException
     */
    default void validateFileSizeWrite(final long fileSize) throws BACnetServiceException {
        throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
    }

    void writeFileSize(long fileSize);

    /**
     * Allows the FileAccess implementation to determine whether to allow the given record count write. Stream access
     * implementations will not normally override the default behaviour.
     *
     * @param recordCount the record count to write. If and exception is not thrown and other validations succeed, the
     *                    writeRecordCount method will be called.
     * @throws BACnetServiceException
     */
    default void validateRecordCountWrite(final long recordCount) throws BACnetServiceException {
        throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
    }

    void writeRecordCount(long recordCount);

    boolean supportsStreamAccess();

    OctetString readData(long start, long length) throws IOException, BACnetServiceException;

    /**
     * In the case of a start at -1, i.e. an append, this method returns the actual location where data was written.
     *
     * @param start
     * @param data
     * @return
     * @throws IOException
     * @throws BACnetServiceException
     */
    long writeData(long start, OctetString data) throws IOException, BACnetServiceException;

    boolean supportsRecordAccess();

    SequenceOf<OctetString> readRecords(long start, long count) throws IOException, BACnetServiceException;

    /**
     * In the case of a start at -1, i.e. an append, this method returns the actual record where data was written.
     *
     * @param start
     * @param data
     * @return
     * @throws IOException
     * @throws BACnetServiceException
     */
    long writeRecords(long start, SequenceOf<OctetString> records) throws IOException, BACnetServiceException;
}
