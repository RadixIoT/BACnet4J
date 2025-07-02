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

package com.serotonin.bacnet4j.npdu.mstp;

public class DataCRC {
    public static final int CHECK_VALUE = 0xF0B8;
    private int value = 0xffff;

    public void reset() {
        value = 0xffff;
    }

    public void accumulate(int data) {
        value = calcDataCRC(data, value);
    }

    public void accumulate(byte data) {
        accumulate(data & 0xFF);
    }

    public boolean isOk() {
        return value == CHECK_VALUE;
    }

    public int getCrc(Frame frame) {
        reset();
        for (byte b : frame.getData())
            accumulate(b);
        return onesComplement(value);
    }

    private static int calcDataCRC(int dataValue, int crcValue) {
        int crcLow = (crcValue & 0xff) ^ dataValue; /* XOR C7..C0 with D7..D0 */
        /* Exclusive OR the terms in the table (top down) */
        int crc = (crcValue >> 8) ^ (crcLow << 8) ^ (crcLow << 3) ^ (crcLow << 12) ^ (crcLow >> 4) ^ (crcLow & 0x0f)
                ^ ((crcLow & 0x0f) << 7);
        return crc & 0xffff;
    }

    private static int onesComplement(int i) {
        return (~i) & 0xffff;
    }

    public static void main(String[] args) {
        //        DataCRC crc = new DataCRC();
        //        crc.accumulate(0x01);
        //        crc.accumulate(0x22);
        //        crc.accumulate(0x33);
        //        System.out.println(Integer.toString(crc.close(), 16));

        DataCRC crc = new DataCRC();
        System.out.println(Integer.toString(crc.getCrc(new Frame(FrameType.bacnetDataNotExpectingReply, (byte) 0xff,
                (byte) 8, new byte[] {0x1, 0x20, (byte) 0xff, (byte) 0xfc, (byte) 0xfe, 0x20, (byte) 0xa0,
                (byte) 0xe7, (byte) 0x91, (byte) 0xf0, 0x3, 0x22, 0x1, 0x15, 0x2b, (byte) 0xf9, (byte) 0xff,
                0x55, (byte) 0xff, 0x0, 0x4})), 16));
        //        crc.accumulate(0xb8);
        //        crc.accumulate(0x96);
        System.out.println(Integer.toString(crc.value, 16));

        //        crc.accumulate(0x8);
        //        crc.accumulate(0x0);
        //        System.out.println(Integer.toString(crc.getValue(), 16));

        //[55,ff,6,ff,8,0,15,da,1,20,ff,fc,fe,20,a0,e7,91,f0,3,22,1,15,2b,f9,ff,55,ff,0,4,8,0,0,14]

    }
}
