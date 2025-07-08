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

public enum MaxSegments {
    UNSPECIFIED(0, Integer.MAX_VALUE), //
    UP_TO_2(1, 2), //
    UP_TO_4(2, 4), //
    UP_TO_8(3, 8), //
    UP_TO_16(4, 16), //
    UP_TO_32(5, 32), //
    UP_TO_64(6, 64), //
    MORE_THAN_64(7, Integer.MAX_VALUE), //
    ;

    private byte id;
    private int maxSegments;

    MaxSegments(int id, int maxSegments) {
        this.id = (byte) id;
        this.maxSegments = maxSegments;
    }

    public byte getId() {
        return id;
    }

    public int getMaxSegments() {
        return maxSegments;
    }

    public static MaxSegments valueOf(byte id) {
        if (id == UNSPECIFIED.id)
            return UNSPECIFIED;
        if (id == UP_TO_2.id)
            return UP_TO_2;
        if (id == UP_TO_4.id)
            return UP_TO_4;
        if (id == UP_TO_8.id)
            return UP_TO_8;
        if (id == UP_TO_16.id)
            return UP_TO_16;
        if (id == UP_TO_32.id)
            return UP_TO_32;
        if (id == UP_TO_64.id)
            return UP_TO_64;
        if (id == MORE_THAN_64.id)
            return MORE_THAN_64;

        throw new IllegalArgumentException("Unknown id: " + id);
    }
}
