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

package com.serotonin.bacnet4j.util;

import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class BACnetUtils {
    public static void pushInt(ByteQueue queue, long value) {
        queue.push((byte) (0xff & (value >> 24)));
        queue.push((byte) (0xff & (value >> 16)));
        queue.push((byte) (0xff & (value >> 8)));
        queue.push((byte) (0xff & value));
    }

    public static void pushLong(ByteQueue queue, long value) {
        queue.push((byte) (0xff & (value >> 56)));
        queue.push((byte) (0xff & (value >> 48)));
        queue.push((byte) (0xff & (value >> 40)));
        queue.push((byte) (0xff & (value >> 32)));
        queue.push((byte) (0xff & (value >> 24)));
        queue.push((byte) (0xff & (value >> 16)));
        queue.push((byte) (0xff & (value >> 8)));
        queue.push((byte) (0xff & value));
    }

    public static OctetString toVirtualAddressBytes(int deviceId) {
        byte[] b = new byte[3];
        b[0] = (byte) (0xff & (deviceId >> 16));
        b[1] = (byte) (0xff & (deviceId >> 8));
        b[2] = (byte) (0xff & deviceId);
        return new OctetString(b);
    }

    public static int toDeviceId(byte[] virtualAddressBytes) {
        int deviceId = toInt(virtualAddressBytes[0]) << 16;
        deviceId |= toInt(virtualAddressBytes[1]) << 8;
        deviceId |= toInt(virtualAddressBytes[2]);
        return deviceId;
    }

    public static OctetString popDeviceId(ByteQueue queue) {
        byte[] b = new byte[3];
        queue.pop(b);
        return new OctetString(b);
    }

    public static int popShort(ByteQueue queue) {
        return (short) ((toInt(queue.pop()) << 8) | toInt(queue.pop()));
    }

    public static int popInt(ByteQueue queue) {
        return (toInt(queue.pop()) << 24) | (toInt(queue.pop()) << 16) | (toInt(queue.pop()) << 8) | toInt(queue.pop());
    }

    public static long popLong(ByteQueue queue) {
        return (toLong(queue.pop()) << 56) | (toLong(queue.pop()) << 48) | (toLong(queue.pop()) << 40)
                | (toLong(queue.pop()) << 32) | (toLong(queue.pop()) << 24) | (toLong(queue.pop()) << 16)
                | (toLong(queue.pop()) << 8) | toLong(queue.pop());
    }

    public static int toInt(byte b) {
        return b & 0xff;
    }

    public static long toLong(byte b) {
        return (b & 0xff);
    }

    public static byte[] convertToBytes(boolean[] bdata) {
        int byteCount = (bdata.length + 7) / 8;
        byte[] data = new byte[byteCount];
        for (int i = 0; i < bdata.length; i++)
            data[i / 8] |= (bdata[i] ? 1 : 0) << (7 - (i % 8));
        return data;
    }

    public static boolean[] convertToBooleans(byte[] data, int length) {
        boolean[] bdata = new boolean[length];
        for (int i = 0; i < bdata.length; i++)
            bdata[i] = ((data[i / 8] >> (7 - (i % 8))) & 0x1) == 1;
        return bdata;
    }

    public static byte[] dottedStringToBytes(String s) throws NumberFormatException {
        String[] parts = s.split("\\.");
        byte[] b = new byte[parts.length];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte) Integer.parseInt(parts[i]);
        return b;
    }

    public static String bytesToDottedString(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (i > 0)
                sb.append('.');
            sb.append(0xff & b[i]);
        }
        return sb.toString();
    }
}
