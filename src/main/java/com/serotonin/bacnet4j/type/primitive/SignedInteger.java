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

import java.math.BigInteger;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Represents the INTERGER type
 */
public class SignedInteger extends Primitive {
    public static final byte TYPE_ID = 3;

    private int smallValue;
    private BigInteger bigValue;

    public SignedInteger(final int value) {
        smallValue = value;
    }

    public SignedInteger(final long value) {
        bigValue = BigInteger.valueOf(value);
    }

    public SignedInteger(final BigInteger value) {
        bigValue = value;
    }

    public int intValue() {
        if (bigValue == null)
            return smallValue;
        return bigValue.intValue();
    }

    public long longValue() {
        if (bigValue == null)
            return smallValue;
        return bigValue.longValue();
    }

    public BigInteger bigIntegerValue() {
        if (bigValue == null)
            return BigInteger.valueOf(smallValue);
        return bigValue;
    }

    //
    // Reading and writing
    //
    public SignedInteger(final ByteQueue queue) throws BACnetErrorException {
        // Read the data length value.
        final int length = (int) readTag(queue, TYPE_ID);

        final byte[] bytes = new byte[length];
        queue.pop(bytes);
        final BigInteger bi = new BigInteger(bytes);

        if (length < 5)
            smallValue = bi.intValue();
        else
            bigValue = bi;
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        if (bigValue == null) {
            long length = getLength();
            while (length > 0)
                queue.push(smallValue >> --length * 8);
        } else
            queue.push(bigValue.toByteArray());
    }

    @Override
    protected long getLength() {
        if (bigValue == null) {
            int length;
            if (smallValue < Byte.MAX_VALUE && smallValue > Byte.MIN_VALUE)
                length = 1;
            else if (smallValue < Short.MAX_VALUE && smallValue > Short.MIN_VALUE)
                length = 2;
            else if (smallValue < 8388607 && smallValue > -8388608)
                length = 3;
            else
                length = 4;
            return length;
        }
        return bigValue.toByteArray().length;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (bigValue == null ? 0 : bigValue.hashCode());
        result = PRIME * result + smallValue;
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
        final SignedInteger other = (SignedInteger) obj;
        return bigIntegerValue().equals(other.bigIntegerValue());
    }

    @Override
    public String toString() {
        if (bigValue == null)
            return Integer.toString(smallValue);
        return bigValue.toString();
    }
}
