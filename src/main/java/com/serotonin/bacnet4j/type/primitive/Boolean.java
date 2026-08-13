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

package com.serotonin.bacnet4j.type.primitive;

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Boolean extends Primitive {
    public static final Boolean FALSE = new Boolean(false);
    public static final Boolean TRUE = new Boolean(true);

    public static Boolean valueOf(final boolean b) {
        return b ? TRUE : FALSE;
    }

    public static boolean falsey(final Boolean b) {
        return b == null || !b.booleanValue();
    }

    public static boolean truthy(final Boolean b) {
        return !falsey(b);
    }

    public static final byte TYPE_ID = 1;

    protected final boolean value;

    private Boolean(final boolean value) {
        this.value = value;
    }

    public boolean booleanValue() {
        return value;
    }

    public Boolean(final ByteQueue queue) throws BACnetErrorException {
        // 135-2024 clause 20.2.3: Boolean is the one primitive whose length/value field can hold the value rather
        // than a count of contents octets, so the field cannot be range checked as a length here.
        long length = readTagHeader(queue, TYPE_ID);

        if (isContextSpecific()) {
            // Context-tagged Boolean data has exactly one contents octet.
            if (length != 1 || queue.size() < 1)
                throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType,
                        "Context-tagged boolean with a length of " + length);
            value = queue.pop() == 1;
        } else {
            // If the tagNumber is not contextSpecific, validate the type
            if (getTagNumber() != TYPE_ID) {
                throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType);
            }
            // Application-tagged Boolean values carry the value in the length/value field, with no contents octets.
            if (length > 1)
                throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType,
                        "Application-tagged boolean with a length/value of " + length);
            value = length == 1;
        }
    }

    @Override
    public void write(final ByteQueue queue) {
        writeTag(queue, getTypeId(), false, value ? 1 : 0);
    }

    @Override
    public void write(final ByteQueue queue, final int contextId) {
        writeTag(queue, contextId, true, 1);
        queue.push((byte) (value ? 1 : 0));
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        throw new BACnetRuntimeException("Should not be called because length is context specific");
    }

    @Override
    protected long getLength() {
        throw new BACnetRuntimeException("Should not be called because length is context specific");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Boolean aBoolean = (Boolean) o;
        return value == aBoolean.value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return java.lang.Boolean.toString(value);
    }
}
