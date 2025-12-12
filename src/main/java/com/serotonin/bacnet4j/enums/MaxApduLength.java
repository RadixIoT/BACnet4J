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

package com.serotonin.bacnet4j.enums;

import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public enum MaxApduLength {
    UP_TO_50(0, 50), // MinimumMessageSize
    UP_TO_128(1, 128), //
    UP_TO_206(2, 206), // Fits in a LonTalk frame
    UP_TO_480(3, 480), // Fits in an ARCNET frame
    UP_TO_1024(4, 1024), //
    UP_TO_1476(5, 1476); // Fits in an ISO 8802-3 frame

    private byte id;
    private UnsignedInteger maxLength;

    MaxApduLength(final int id, final int maxLength) {
        this.id = (byte) id;
        this.maxLength = new UnsignedInteger(maxLength);
    }

    public byte getId() {
        return id;
    }

    public int getMaxLengthInt() {
        return maxLength.intValue();
    }

    public UnsignedInteger getMaxLength() {
        return maxLength;
    }

    public static MaxApduLength valueOf(final byte id) {
        if (id == UP_TO_50.id)
            return UP_TO_50;
        if (id == UP_TO_128.id)
            return UP_TO_128;
        if (id == UP_TO_206.id)
            return UP_TO_206;
        if (id == UP_TO_480.id)
            return UP_TO_480;
        if (id == UP_TO_1024.id)
            return UP_TO_1024;
        if (id == UP_TO_1476.id)
            return UP_TO_1476;

        throw new IllegalArgumentException("Unknown id: " + id);
    }
}
