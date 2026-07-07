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
import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class AmbiguousValue extends Encodable {
    @SuppressWarnings("unchecked")
    public static <T extends Encodable> T convertTo(Encodable value, Class<T> clazz) throws BACnetException {
        if (value instanceof AmbiguousValue amb) {
            return amb.convertTo(clazz);
        }
        return (T) value;
    }

    private byte[] data;

    public AmbiguousValue(byte[] data) {
        this.data = data;
    }

    public AmbiguousValue(Encodable... sequence) {
        ByteQueue queue = new ByteQueue();
        for (Encodable e : sequence) {
            e.write(queue);
        }
        data = queue.popAll();
    }

    public AmbiguousValue(ByteQueue queue) {
        TagData tagData = new TagData();
        peekTagData(queue, tagData);
        readAmbiguousData(queue, tagData);
    }

    public AmbiguousValue(ByteQueue queue, int contextId) throws BACnetException {
        popStart(queue, contextId);

        TagData tagData = new TagData();
        while (true) {
            peekTagData(queue, tagData);
            if (tagData.isEndTag(contextId))
                break;
            readAmbiguousData(queue, tagData);
        }

        popEnd(queue, contextId);
    }

    @Override
    public void write(ByteQueue queue, int contextId) {
        writeContextTag(queue, contextId, true);
        queue.push(data);
        writeContextTag(queue, contextId, false);
    }

    @Override
    public void write(ByteQueue queue) {
        queue.push(data);
    }

    private void readAmbiguousData(ByteQueue queue, TagData tagData) {
        ByteQueue tmp = new ByteQueue();
        readAmbiguousData(queue, tagData, tmp);
        byte[] element = tmp.popAll();
        //concatenate data
        byte[] newData;
        if (this.data != null) {
            newData = new byte[this.data.length + element.length];
            System.arraycopy(this.data, 0, newData, 0, this.data.length);
            System.arraycopy(element, 0, newData, this.data.length, element.length);
        } else {
            newData = element;
        }
        this.data = newData;
    }

    private void readAmbiguousData(ByteQueue queue, TagData tagData, ByteQueue data) {
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
                int contextId = tagData.tagNumber;

                // Read ambiguous data until we find the end tag.
                while (true) {
                    peekTagData(queue, tagData);
                    if (tagData.isEndTag(contextId))
                        break;
                    readAmbiguousData(queue, tagData);
                }

                // Copy the end tag
                copyData(queue, 1, data);
            } else
                copyData(queue, tagData.getTotalLength(), data);
        }
    }

    @Override
    public String toString() {
        if (data != null) {
            return "Ambiguous " + StreamUtils.dumpArrayHex(data);
        } else {
            return "Ambiguous []";
        }
    }

    public String toPrimitiveString() {
        if (data != null) {
            String s;
            if (Primitive.isPrimitive(data[0])) {
                try {
                    s = convertTo(Primitive.class).toString();
                } catch (BACnetException e) {
                    throw new RuntimeException(e);
                }
                return s;
            } else {
                return toString();
            }
        } else {
            return "Ambiguous []";
        }
    }

    private static void copyData(ByteQueue queue, int length, ByteQueue data) {
        int len = length;
        while (len-- > 0)
            data.push(queue.pop());
    }

    public boolean isNull() {
        return data == null || data.length == 0 // Non-spec acceptance of empty content as Null
                || (data.length == 1 && data[0] == 0);
    }

    public <T extends Encodable> T convertTo(Class<T> clazz) throws BACnetException {
        return read(new ByteQueue(data), clazz);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AmbiguousValue that))
            return false;
        return Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public void validate() throws BACnetServiceException {
        //No way to validate such a thing
    }
}
