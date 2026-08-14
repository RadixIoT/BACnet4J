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

    private final byte id;
    private final int maxSegmentCount;

    MaxSegments(int id, int maxSegmentCount) {
        this.id = (byte) id;
        this.maxSegmentCount = maxSegmentCount;
    }

    public byte getId() {
        return id;
    }

    public int getMaxSegmentCount() {
        return maxSegmentCount;
    }

    /**
     * Returns the value to encode in the 'max-segments-accepted' field of a confirmed request for a device that
     * accepts the given number of segments. This is the greatest value that does not claim to accept more segments
     * than the given count, since the field cannot express an exact number above 64.
     *
     * @param count the number of segments accepted, which must be at least 2. Clause 12.11.20 requires
     *              Max_Segments_Accepted to be greater than one for a device that receives segmented messages.
     * @return the value to encode
     */
    public static MaxSegments forCount(int count) {
        if (count < UP_TO_2.maxSegmentCount)
            throw new IllegalArgumentException("Segment count must be at least 2: " + count);
        if (count > UP_TO_64.maxSegmentCount)
            return MORE_THAN_64;

        // The greatest tier that does not overstate the count. UNSPECIFIED and MORE_THAN_64 are excluded implicitly,
        // because the check above has established that the count is no greater than that of UP_TO_64.
        MaxSegments best = UP_TO_2;
        for (MaxSegments value : values()) {
            if (value.maxSegmentCount <= count)
                best = value;
        }
        return best;
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
