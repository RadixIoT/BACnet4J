/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.npdu.sc.msg;

import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class SCOption {
    public static final int TYPE_SECURE_PATH = 1;
    public static final int TYPE_HELLO = 2;
    public static final int TYPE_IDENTITY = 3;
    public static final int TYPE_HINT = 4;
    public static final int TYPE_TOKEN = 5;
    public static final int TYPE_PROPRIETARY = 31;

    private static final int FLAG_MORE = 0x80; // bit 7
    private static final int FLAG_UNDERSTAND = 0x40; // bit 6
    private static final int FLAG_DATA = 0x20; // bit 5
    private static final int TYPE_MASK = 0x1F; // bits 4..0

    private int marker;
    // original marker including the more flag is needed for the 'Error Header Marker' field in a NAK
    private int type;
    private boolean mustUnderstand;
    private byte[] data;

    private boolean parseError;
    private ErrorCode parseErrorCode;
    private String parseErrorReason;

    SCOption() {
        // For parsing
    }

    public SCOption(int type, boolean mustUnderstand) {
        this.type = type;
        this.mustUnderstand = mustUnderstand;
    }

    public SCOption(int type, boolean mustUnderstand, byte[] data) {
        this(type, mustUnderstand);
        this.data = data;
    }

    public boolean isMustUnderstand() {
        return mustUnderstand;
    }

    public int getMarker() {
        return marker;
    }

    public boolean parse(ByteQueue queue) {
        marker = queue.popU1B() & 0xFF;
        boolean more = (marker & FLAG_MORE) != 0;
        mustUnderstand = (marker & FLAG_UNDERSTAND) != 0;
        type = marker & TYPE_MASK;
        if ((marker & FLAG_DATA) != 0) {
            try {
                int length = queue.popU2B();
                data = new byte[length];
                queue.pop(data);
            } catch (ArrayIndexOutOfBoundsException e) {
                // AB.3.1.5 "If a BVLC message is received that is truncated..."
                parseError = true;
                parseErrorCode = ErrorCode.messageIncomplete;
                parseErrorReason = "Not enough data in option - length field wrong?";
                return false;
            }
        }
        return more;
    }

    public boolean hasParseError() {
        return parseError;
    }

    public ErrorCode getParseError() {
        return parseErrorCode;
    }

    public String getParseErrorReason() {
        return parseErrorReason;
    }

    public void write(ByteQueue queue, boolean more) {
        marker = type;
        if (mustUnderstand)
            marker |= FLAG_UNDERSTAND;
        if (data != null)
            marker |= FLAG_DATA;
        if (more)
            marker |= FLAG_MORE;
        queue.push((byte) marker);
        if (data != null) {
            queue.pushU2B(data.length);
            queue.push(data);
        }
    }

    public String toString() {
        // brief option format (<number><must-understand-as-Y-or-N><[hexdata]>)  e.g., secure path = (1Y)
        return "(" + (type == -1 ? "C" : type) + (mustUnderstand ? "Y" : "N")
                + (data == null ? "" : "[" + StreamUtils.toHex(data) + "]") + ")";
    }
}
