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

package com.serotonin.bacnet4j.util.sero;

import java.util.List;

public class ArrayUtils {
    public static String toHexString(final byte[] bytes) {
        return toHexString(bytes, 0, bytes.length);
    }

    public static String toHexString(final byte[] bytes, final int start, final int len) {
        if (len == 0)
            return "[]";

        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(Integer.toHexString(bytes[start] & 0xff));
        for (int i = 1; i < len; i++)
            sb.append(',').append(Integer.toHexString(bytes[start + i] & 0xff));
        sb.append("]");

        return sb.toString();
    }

    public static String toPlainHexString(final byte[] bytes) {
        return toPlainHexString(bytes, 0, bytes.length);
    }

    public static String toPlainHexString(final byte[] bytes, final int start, final int len) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            final String s = Integer.toHexString(bytes[start + i] & 0xff);
            if (s.length() < 2)
                sb.append('0');
            sb.append(s);
        }
        return sb.toString();
    }

    public static byte[] fromPlainHexString(final String s) {
        final byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = Byte.parseByte(s.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    public static String toString(final byte[] bytes) {
        return toString(bytes, 0, bytes.length);
    }

    public static String toString(final byte[] bytes, final int start, final int len) {
        if (len == 0)
            return "[]";

        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(Integer.toString(bytes[start] & 0xff));
        for (int i = 1; i < len; i++)
            sb.append(',').append(Integer.toString(bytes[start + i] & 0xff));
        sb.append("]");

        return sb.toString();
    }

    public static boolean isEmpty(final int[] value) {
        return value == null || value.length == 0;
    }

    public static int indexOf(final String[] values, final String value) {
        if (values == null)
            return -1;

        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value))
                return i;
        }

        return -1;
    }

    public static boolean containsIgnoreCase(final String[] values, final String value) {
        if (values == null)
            return false;

        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(value))
                return true;
        }

        return false;
    }

    public static int indexOf(final byte[] src, final byte[] target) {
        return indexOf(src, 0, src.length, target);
    }

    public static int indexOf(final byte[] src, final int len, final byte[] target) {
        return indexOf(src, 0, len, target);
    }

    public static int indexOf(final byte[] src, final int start, final int len, final byte[] target) {
        int pos = start;
        int i;
        boolean matched;
        while (pos + target.length <= len) {
            // Check for a match on the first character
            if (src[pos] == target[0]) {
                // Now check for matches in the rest of the characters
                matched = true;
                i = 1;
                while (i < target.length) {
                    if (src[pos + i] != target[i]) {
                        matched = false;
                        break;
                    }
                    i++;
                }

                if (matched)
                    return pos;
            }
            pos++;
        }

        return -1;
    }

    /**
     * Returns the value of the bits in the given range. Ranges can extend multiple bytes. No range checking is done.
     * Invalid ranges will result in {@link ArrayIndexOutOfBoundsException}.
     *
     * @param b      the array of bytes.
     * @param offset the location at which to begin
     * @param length the number of bits to include in the value.
     * @return the value of the bits in the range.
     */
    public static long bitRangeValueLong(final byte[] b, final int offset, final int length) {
        if (length <= 0)
            return 0;

        final int byteFrom = offset / 8;
        final int byteTo = (offset + length - 1) / 8;

        long result = b[byteFrom] & bitFromMask[offset % 8];

        for (int i = byteFrom + 1; i <= byteTo; i++) {
            result <<= 8;
            result |= b[i] & 0xff;
        }

        result >>= 8 - ((offset + length - 1) % 8 + 1);

        return result;
    }

    private static int[] bitFromMask = {0xff, 0x7f, 0x3f, 0x1f, 0xf, 0x7, 0x3, 0x1};

    public static int bitRangeValue(final byte[] b, final int offset, final int length) {
        return (int) bitRangeValueLong(b, offset, length);
    }

    public static long byteRangeValueLong(final byte[] b, final int offset, final int length) {
        long result = 0;

        for (int i = offset; i < offset + length; i++) {
            result <<= 8;
            result |= b[i] & 0xff;
        }

        return result;
    }

    public static int byteRangeValue(final byte[] b, final int offset, final int length) {
        return (int) byteRangeValueLong(b, offset, length);
    }

    public static int sum(final int[] a) {
        int sum = 0;
        for (int i = 0; i < a.length; i++)
            sum += a[i];
        return sum;
    }

    public static int[] toIntArray(final List<Integer> list) {
        final int[] result = new int[list.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = list.get(i);
        return result;
    }

    public static double[] toDoubleArray(final List<Double> list) {
        final double[] result = new double[list.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = list.get(i);
        return result;
    }

    public static String concatenate(final Object[] a, final String delimiter) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final Object o : a) {
            if (first)
                first = false;
            else
                sb.append(delimiter);
            sb.append(o);
        }
        return sb.toString();
    }

    public static void shift(final Object[] a, final int count) {
        if (count > 0)
            System.arraycopy(a, 0, a, count, a.length - count);
        else
            System.arraycopy(a, -count, a, 0, a.length + count);
    }
}
