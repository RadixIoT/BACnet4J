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

public class UnsignedInteger extends Primitive {
    public static final UnsignedInteger ZERO = new UnsignedInteger(0);
    public static final byte TYPE_ID = 2;

    private int smallValue;
    private BigInteger bigValue;

    public UnsignedInteger(final int value) {
        if (value < 0)
            throw new IllegalArgumentException("Value cannot be less than zero");
        smallValue = value;
    }

    public UnsignedInteger(final long value) {
        bigValue = BigInteger.valueOf(value);
    }

    public UnsignedInteger(final BigInteger value) {
        if (value.signum() == -1)
            throw new IllegalArgumentException("Value cannot be less than zero");
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

    /**
     * Returns an UnsignedInteger that is one larger than this, wrapping at 0x100000000.
     */
    public UnsignedInteger increment32() {
        return increment32(1);
    }

    public UnsignedInteger increment32(final long amount) {
        return new UnsignedInteger((longValue() + amount) % 0x100000000L);
    }

    /**
     * Returns an UnsignedInteger that is one larger than this, wrapping at 0x10000.
     */
    public UnsignedInteger increment16() {
        return increment16(1);
    }

    public UnsignedInteger increment16(final int amount) {
        return new UnsignedInteger((intValue() + amount) % 0x10000L);
    }

    public boolean isSmallValue() {
        return bigValue == null;
    }

    //
    // Reading and writing
    //
    public UnsignedInteger(final ByteQueue queue) throws BACnetErrorException {
        int length = (int) readTag(queue, TYPE_ID);
        if (length < 4) {
            while (length > 0)
                smallValue |= (queue.pop() & 0xff) << --length * 8;
        } else {
            final byte[] bytes = new byte[length + 1];
            queue.pop(bytes, 1, length);
            bigValue = new BigInteger(bytes);
        }
    }

    @Override
    protected void writeImpl(final ByteQueue queue) {
        int length = (int) getLength();
        if (bigValue == null) {
            while (length > 0)
                queue.push(smallValue >> --length * 8);
        } else {
            final byte[] bytes = new byte[length];

            for (int i = 0; i < bigValue.bitLength(); i++) {
                if (bigValue.testBit(i))
                    bytes[length - i / 8 - 1] |= 1 << i % 8;
            }

            queue.push(bytes);
        }
    }

    @Override
    protected long getLength() {
        if (bigValue == null) {
            int length;
            if (smallValue < 0x100)
                length = 1;
            else if (smallValue < 0x10000)
                length = 2;
            else if (smallValue < 0x1000000)
                length = 3;
            else
                length = 4;

            return length;
        }

        if (bigValue.intValue() == 0)
            return 1;
        return (bigValue.bitLength() + 7) / 8;
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
        final UnsignedInteger other = (UnsignedInteger) obj;
        return bigIntegerValue().equals(other.bigIntegerValue());
    }

    @Override
    public String toString() {
        if (bigValue == null)
            return Integer.toString(smallValue);
        return bigValue.toString();
    }
}
