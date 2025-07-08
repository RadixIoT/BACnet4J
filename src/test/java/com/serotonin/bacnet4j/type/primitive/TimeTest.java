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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TimeTest {
    @Test
    public void comparison() {
        assertFalse(new Time(12, 1, 1, 1).before(new Time(11, 2, 2, 2)));
        assertFalse(new Time(12, 1, 1, 1).before(new Time(12, 0, 2, 2)));
        assertFalse(new Time(12, 1, 1, 1).before(new Time(12, 1, 0, 2)));
        assertFalse(new Time(12, 1, 1, 1).before(new Time(12, 1, 1, 1)));
        assertTrue(new Time(12, 1, 1, 1).before(new Time(12, 1, 1, 2)));
        assertTrue(new Time(12, 1, 1, 1).before(new Time(12, 1, 2, 0)));
        assertTrue(new Time(12, 1, 1, 1).before(new Time(12, 2, 0, 0)));
        assertTrue(new Time(12, 1, 1, 1).before(new Time(13, 0, 0, 0)));
    }

    @Test
    public void diff() {
        assertEquals(1, new Time(1, 1, 1, 1).getSmallestDiff(new Time(1, 1, 1, 0)));
        assertEquals(1, new Time(1, 1, 1, 0).getSmallestDiff(new Time(1, 1, 1, 1)));
        assertEquals(240_000, new Time(23, 30, 0, 0).getSmallestDiff(new Time(0, 10, 0, 0)));
        assertEquals(240_000, new Time(0, 10, 0, 0).getSmallestDiff(new Time(23, 30, 0, 0)));
    }
}
