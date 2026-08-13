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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Enumerated extends Primitive {
    public static final byte TYPE_ID = 9;

    private int smallValue;
    private BigInteger bigValue;

    public Enumerated(int value) {
        if (value < 0)
            throw new IllegalArgumentException("Value cannot be less than zero");
        smallValue = value;
    }

    public Enumerated(BigInteger value) {
        if (value.signum() == -1)
            throw new IllegalArgumentException("Value cannot be less than zero");
        bigValue = value;
    }

    public int intValue() {
        if (bigValue == null)
            return smallValue;
        return saturatedIntValue(bigValue);
    }

    public BigInteger bigIntegerValue() {
        if (bigValue == null)
            return BigInteger.valueOf(smallValue);
        return bigValue;
    }

    public byte byteValue() {
        return (byte) intValue();
    }

    public boolean equals(int that) {
        return intValue() == that;
    }

    public boolean equals(Enumerated that) {
        if (that == null)
            return false;
        return intValue() == that.intValue();
    }

    public boolean isOneOf(Enumerated... those) {
        int id = intValue();
        for (Enumerated that : those) {
            if (id == that.intValue())
                return true;
        }
        return false;
    }

    public static Enumerated forName(Map<String, Enumerated> nameMap, String name) {
        Enumerated e = nameMap.get(name);
        if (e == null)
            throw new BACnetRuntimeException("No enumerated found for name '" + name + "'");
        return e;
    }

    public String toString(Map<Integer, String> prettyMap) {
        String s = prettyMap.get(intValue());
        if (s == null)
            s = Integer.toString(intValue());
        return s;
    }

    //
    // Reading and writing
    //
    public Enumerated(ByteQueue queue) throws BACnetErrorException {
        // 135-2024 clause 20.2.11: at least one contents octet.
        int length = readTag(queue, TYPE_ID, 1, MAX_INTEGER_LENGTH);
        if (length < 4) {
            while (length > 0)
                smallValue |= (queue.pop() & 0xff) << --length * 8;
        } else {
            byte[] bytes = new byte[length + 1];
            queue.pop(bytes, 1, length);
            bigValue = new BigInteger(bytes);
        }
    }

    @Override
    protected void writeImpl(ByteQueue queue) {
        int length = (int) getLength();
        if (bigValue == null) {
            while (length > 0)
                queue.push(smallValue >> --length * 8);
        } else {
            byte[] bytes = new byte[length];

            for (int i = 0; i < bigValue.bitLength(); i++) {
                if (bigValue.testBit(i))
                    bytes[length - i / 8 - 1] |= (byte) (1 << i % 8);
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

        if (bigValue.compareTo(BigInteger.ZERO) == 0)
            return 1;
        return (bigValue.bitLength() + 7) / 8;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    //
    // Initialization
    //
    protected static void init(Class<?> clazz, Map<Integer, Enumerated> idMap, Map<String, Enumerated> nameMap,
            Map<Integer, String> prettyMap) {
        try {
            Field[] fields = clazz.getFields();
            for (Field field : fields) {
                if (Modifier.isPublic(field.getModifiers()) //
                        && Modifier.isStatic(field.getModifiers()) //
                        && Modifier.isFinal(field.getModifiers()) //
                        && field.getType() == clazz) {
                    Enumerated e = (Enumerated) field.get(null);
                    String name = field.getName();
                    idMap.put(e.intValue(), e);

                    // Replace all capital letters in the name with dash and lower case.
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < name.length(); i++) {
                        char c = name.charAt(i);
                        if (Character.isUpperCase(c)) {
                            sb.append('-').append(Character.toLowerCase(c));
                        } else {
                            sb.append(c);
                        }
                    }

                    nameMap.put(sb.toString(), e);
                    prettyMap.put(e.intValue(), sb.toString());
                }
            }
        } catch (Exception e) {
            throw new BACnetRuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "Enumerated [" + Objects.requireNonNullElseGet(bigValue, () -> smallValue) + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Enumerated that = (Enumerated) o;
        return Objects.equals(bigIntegerValue(), that.bigIntegerValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(bigIntegerValue());
    }
}
