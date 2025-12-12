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
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class AmbiguousValue extends Encodable {
    @SuppressWarnings("unchecked")
    public static <T extends Encodable> T convertTo(final Encodable value, final Class<T> clazz)
            throws BACnetException {
        if (value instanceof AmbiguousValue) {
            return ((AmbiguousValue) value).convertTo(clazz);
        }
        return (T) value;
    }

    private byte[] data;

    public AmbiguousValue(final byte[] data) {
        this.data = data;
    }

    public AmbiguousValue(final Encodable... sequence) {
        final ByteQueue queue = new ByteQueue();
        for (final Encodable e : sequence) {
            e.write(queue);
        }
        data = queue.popAll();
    }

    public AmbiguousValue(final ByteQueue queue) {
        final TagData tagData = new TagData();
        peekTagData(queue, tagData);
        readAmbiguousData(queue, tagData);
    }

    public AmbiguousValue(final ByteQueue queue, final int contextId) throws BACnetException {
        popStart(queue, contextId);

        final TagData tagData = new TagData();
        while (true) {
            peekTagData(queue, tagData);
            if (tagData.isEndTag(contextId))
                break;
            readAmbiguousData(queue, tagData);
        }

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

    private void readAmbiguousData(final ByteQueue queue, final TagData tagData) {
        final ByteQueue data = new ByteQueue();
        readAmbiguousData(queue, tagData, data);
        byte[] element = data.popAll();
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

    private void readAmbiguousData(final ByteQueue queue, final TagData tagData, final ByteQueue data) {
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
                } catch (final BACnetException e) {
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

    private static void copyData(final ByteQueue queue, final int length, final ByteQueue data) {
        int len = length;
        while (len-- > 0)
            data.push(queue.pop());
    }

    public boolean isNull() {
        return data.length == 1 && data[0] == 0;
    }

    public <T extends Encodable> T convertTo(final Class<T> clazz) throws BACnetException {
        return read(new ByteQueue(data), clazz);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (data == null ? 0 : data.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (obj instanceof AmbiguousValue) {
            final AmbiguousValue that = (AmbiguousValue) obj;
            return Arrays.equals(data, that.data);
        }

        if (!(obj instanceof Encodable))
            return false;
        final Encodable eobj = (Encodable) obj;
        try {
            return convertTo(eobj.getClass()).equals(obj);
        } catch (@SuppressWarnings("unused") final BACnetException e) {
            return false;
        }
    }

    @Override
    public void validate() throws BACnetServiceException {
        //No way to validate such a thing
    }
}
