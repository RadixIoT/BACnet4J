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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class MaxSegmentsTest {
    /**
     * The encoding of clause 20.1.2.4 has a value for each power of two up to 64, so a count between two tiers must
     * be reported as the lower one rather than overstating what the device accepts.
     */
    @Test
    public void forCountDoesNotOverstate() {
        assertEquals(MaxSegments.UP_TO_2, MaxSegments.forCount(2));
        assertEquals(MaxSegments.UP_TO_2, MaxSegments.forCount(3));
        assertEquals(MaxSegments.UP_TO_4, MaxSegments.forCount(4));
        assertEquals(MaxSegments.UP_TO_4, MaxSegments.forCount(7));
        assertEquals(MaxSegments.UP_TO_8, MaxSegments.forCount(8));
        assertEquals(MaxSegments.UP_TO_16, MaxSegments.forCount(16));
        assertEquals(MaxSegments.UP_TO_32, MaxSegments.forCount(32));
        assertEquals(MaxSegments.UP_TO_64, MaxSegments.forCount(64));
    }

    /**
     * Above 64 the encoding cannot be more precise.
     */
    @Test
    public void forCountAboveSixtyFour() {
        assertEquals(MaxSegments.MORE_THAN_64, MaxSegments.forCount(65));
        assertEquals(MaxSegments.MORE_THAN_64, MaxSegments.forCount(4096));
        assertEquals(MaxSegments.MORE_THAN_64, MaxSegments.forCount(Integer.MAX_VALUE));
    }

    /**
     * Clause 12.11.20 requires Max_Segments_Accepted to be greater than one for a device that receives segmented
     * messages, and the encoding has no value below two.
     */
    @Test
    public void forCountBelowTwo() {
        assertThrows(IllegalArgumentException.class, () -> MaxSegments.forCount(1));
        assertThrows(IllegalArgumentException.class, () -> MaxSegments.forCount(0));
        assertThrows(IllegalArgumentException.class, () -> MaxSegments.forCount(-1));
    }
}
