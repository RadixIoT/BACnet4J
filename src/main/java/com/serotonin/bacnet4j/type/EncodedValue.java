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

package com.serotonin.bacnet4j.type;

import java.util.Arrays;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class EncodedValue extends Encodable {
    private final byte[] data;

    public EncodedValue(final byte[] data) {
        this.data = data;
    }

    public EncodedValue(final Encodable... sequence) {
        final ByteQueue queue = new ByteQueue();
        for (final Encodable e : sequence) {
            e.write(queue);
        }
        data = queue.popAll();
    }

    public EncodedValue(final ByteQueue queue, final int contextId) throws BACnetException {
        popStart(queue, contextId);

        final TagData tagData = new TagData();
        final ByteQueue data = new ByteQueue();
        while (true) {
            peekTagData(queue, tagData);
            if (tagData.isEndTag(contextId))
                break;
            readData(queue, tagData, data);
        }
        this.data = data.popAll();

        popEnd(queue, contextId);
    }

    @Override
    public void write(final ByteQueue queue, final int contextId) {
        writeContextTag(queue, contextId, true);
        queue.push(data);
        writeContextTag(queue, contextId, false);
    }

    @Override
    public void write(final ByteQueue queue) {
        queue.push(data);
    }

    public byte[] getData() {
        return data;
    }

    private void readData(final ByteQueue queue, final TagData tagData, final ByteQueue data) {
        if (!tagData.contextSpecific) {
            // Application class.
            if (tagData.tagNumber == Boolean.TYPE_ID)
                copyData(queue, 1, data);
            else
                copyData(queue, tagData.getTotalLength(), data);
        } else {
            // Context specific class.
            if (tagData.isStartTag()) {
                // Copy the start tag
                copyData(queue, 1, data);

                // Remember the context id
                final int contextId = tagData.tagNumber;

                // Read ambiguous data until we find the end tag.
                while (true) {
                    peekTagData(queue, tagData);
                    if (tagData.isEndTag(contextId))
                        break;
                    readData(queue, tagData, data);
                }

                // Copy the end tag
                copyData(queue, 1, data);
            } else {
                copyData(queue, tagData.getTotalLength(), data);
            }
        }
    }

    @Override
    public String toString() {
        return "Encoded(" + StreamUtils.dumpArrayHex(data) + ")";
    }

    private static void copyData(final ByteQueue queue, final int length, final ByteQueue data) {
        if (length <= 0) {
            // Guard for https://github.com/RadixIoT/BACnet4J/issues/66
            throw new BACnetRuntimeException("Illegal copy length: " + length + ", encoded data may be corrupt");
        }
        int len = length;
        while (len-- > 0)
            data.push(queue.pop());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final EncodedValue other = (EncodedValue) obj;
        if (!Arrays.equals(data, other.data))
            return false;
        return true;
    }

    @Override
    public void validate() throws BACnetServiceException {
        //Not necessary
    }
}
