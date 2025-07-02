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

public class HeaderCRC {
    public static final byte CHECK_VALUE = 0x55;
    private int value = 0xff;

    public void reset() {
        value = 0xff;
    }

    public void accumulate(int data) {
        value = calcHeaderCRC(data, value);
    }

    public void accumulate(byte data) {
        accumulate(data & 0xFF);
    }

    public boolean isOk() {
        return value == CHECK_VALUE;
    }

    public int getCrc(Frame frame) {
        reset();
        accumulate(frame.getFrameType().id);
        accumulate(frame.getDestinationAddress());
        accumulate(frame.getSourceAddress());
        accumulate((frame.getLength() >> 8) & 0xff);
        accumulate(frame.getLength() & 0xff);
        return onesComplement(value);
    }

    public static int calcHeaderCRC(int dataValue, int crcValue) {
        int crc = crcValue ^ dataValue;
        /* Exclusive OR the terms in the table (top down) */
        crc = crc ^ (crc << 1) ^ (crc << 2) ^ (crc << 3) ^ (crc << 4) ^ (crc << 5) ^ (crc << 6) ^ (crc << 7);
        /* Combine bits shifted out left hand end */
        return (crc & 0xfe) ^ ((crc >> 8) & 1);
    }

    private static int onesComplement(int i) {
        return (~i) & 0xff;
    }

    public static void main(String[] args) {
        //        HeaderCRC crc = new HeaderCRC();
        //        crc.accumulate(0x00);
        //        crc.accumulate(0x10);
        //        crc.accumulate(0x05);
        //        crc.accumulate(0x01);
        //        crc.accumulate(0x00);
        //        System.out.println(Integer.toString(crc.close(), 16));
        //
        //        byte b = (byte) 0xff;
        //        System.out.println(b == 0xff);

        //        HeaderCRC crc = new HeaderCRC();
        //        System.out.println(Integer.toString(
        //                crc.getCrc(new Frame(FrameType.bacnetDataNotExpectingReply, (byte) 0xff, (byte) 8, new byte[] { 0x1,
        //                        0x20, (byte) 0xff, (byte) 0xff, 0x0, (byte) 0xff, 0x10, 0x0, (byte) 0xc4, 0x2, 0x4, 0x3a, 0x10,
        //                        0x22, 0x1, (byte) 0xe0, (byte) 0x91, 0x3, 0x22, 0x1, 0x15 })), 16));
        //        System.out.println(Integer.toString(crc.close(), 16));

        HeaderCRC crc = new HeaderCRC();
        int frameCrc = crc.getCrc(new Frame(FrameType.bacnetDataNotExpectingReply, (byte) 0xff, (byte) 8, new byte[] {
                0x1, 0x20, (byte) 0xff, (byte) 0xfc, (byte) 0xfe, 0x20, (byte) 0xa0, (byte) 0xe7, (byte) 0x91,
                (byte) 0xf0, 0x3, 0x22, 0x1, 0x15, 0x2b, (byte) 0xf9, (byte) 0xff, 0x55, (byte) 0xff, 0x0, 0x4}));
        System.out.println(Integer.toString(frameCrc, 16));
        crc.accumulate(frameCrc);
        System.out.println(crc.isOk());

        //[55,ff,6,ff,8,0,15,da,1,20,ff,fc,fe,20,a0,e7,91,f0,3,22,1,15,2b,f9,ff,55,ff,0,4,8,0,0,14]

        //        crc.accumulate(0x01);
        //        crc.accumulate(0x09);
        //        crc.accumulate(0x08);
        //        crc.accumulate(0x00);
        //        crc.accumulate(0x00);
        //
        //        System.out.println(Integer.toString(crc.value, 16));
        //        System.out.println(Integer.toString(crc.close(), 16));
        //
        //        byte b = (byte) 0xff;
        //        System.out.println(b == 0xff);

    }
}
