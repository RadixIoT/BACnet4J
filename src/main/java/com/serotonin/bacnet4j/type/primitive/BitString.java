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

import java.util.Arrays;
import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.BACnetUtils;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class BitString extends Primitive {
    public static final byte TYPE_ID = 8;

    private final boolean[] value;

    public BitString(boolean[] value) {
        this.value = value;
    }

    public BitString(int size, boolean defaultValue) {
        value = new boolean[size];
        if (defaultValue) {
            for (int i = 0; i < size; i++)
                value[i] = true;
        }
    }

    public BitString(BitString that) {
        this(Arrays.copyOf(that.value, that.value.length));
    }

    public boolean[] getValue() {
        return value;
    }

    public boolean getValue(int indexBase1) {
        return value[indexBase1 - 1];
    }

    public boolean getArrayValue(int index) {
        boolean[] ba = getValue();
        try {
            return ba[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public BitString setAll(boolean b) {
        Arrays.fill(value, b);
        return this;
    }

    public void setValue(int indexBase1, boolean b) {
        value[indexBase1 - 1] = b;
    }

    public boolean allFalse() {
        for (boolean b : value) {
            if (b)
                return false;
        }
        return true;
    }

    public boolean allTrue() {
        for (boolean b : value) {
            if (!b)
                return false;
        }
        return true;
    }

    /**
     * Performs a bit-wise AND operation.
     */
    public BitString and(BitString that) {
        if (value.length != that.value.length)
            throw new IllegalArgumentException("Bitstrings are of different lengths");

        boolean[] result = new boolean[value.length];
        for (int i = 0; i < value.length; i++) {
            result[i] = value[i] && that.value[i];
        }
        return new BitString(result);
    }

    //
    // Reading and writing
    //
    public BitString(ByteQueue queue) throws BACnetErrorException {
        // 135-2024 clause 20.2.10: an initial octet holding the number of unused bits, then zero or more octets of
        // bit string, so the declared length is at least one and the subsequent octet count is length - 1.
        int length = readTag(queue, TYPE_ID, 1, NO_MAX_LENGTH) - 1;
        // Bit strings are encoded in bytes, and so if the number of bits is not a multiple of 8, there will be a
        // non-zero number of unused bits.
        int unusedBits = queue.popU1B();

        // The number of unused bits shall be zero to seven, and shall be zero when the bit string is empty. Left
        // unchecked, a larger value makes the bit count negative or pushes it past the end of the data.
        if (unusedBits > 7 || length == 0 && unusedBits != 0) {
            throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType,
                    "Invalid unused bit count " + unusedBits + " for a bit string of " + length + " octets");
        }

        if (length == 0)
            value = new boolean[0];
        else {
            byte[] data = new byte[length];
            queue.pop(data);
            value = BACnetUtils.convertToBooleans(data, length * 8 - unusedBits);
        }
    }

    @Override
    public void writeImpl(ByteQueue queue) {
        if (value.length == 0)
            queue.push((byte) 0);
        else {
            int unusedBits = value.length % 8;
            if (unusedBits > 0)
                unusedBits = 8 - unusedBits;
            queue.push((byte) unusedBits);
            queue.push(BACnetUtils.convertToBytes(value));
        }
    }

    @Override
    protected long getLength() {
        if (value.length == 0)
            return 1;
        return (value.length - 1) / 8L + 2;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        BitString bitString = (BitString) o;
        return Objects.deepEquals(value, bitString.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return Arrays.toString(value);
    }
}
