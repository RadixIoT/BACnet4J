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

package com.serotonin.bacnet4j.type.constructed;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.enums.DayOfWeek;

public class DaysOfWeekTest {
    @Test
    public void containsDayOfWeek() {
        final DaysOfWeek dow = new DaysOfWeek(false);
        assertEquals(false, dow.contains(DayOfWeek.MONDAY));
        assertEquals(false, dow.contains(DayOfWeek.TUESDAY));
        assertEquals(false, dow.contains(DayOfWeek.WEDNESDAY));
        assertEquals(false, dow.contains(DayOfWeek.THURSDAY));
        assertEquals(false, dow.contains(DayOfWeek.FRIDAY));
        assertEquals(false, dow.contains(DayOfWeek.SATURDAY));
        assertEquals(false, dow.contains(DayOfWeek.SUNDAY));

        dow.setMonday(true);
        assertEquals(true, dow.contains(DayOfWeek.MONDAY));
        assertEquals(false, dow.contains(DayOfWeek.TUESDAY));
        assertEquals(false, dow.contains(DayOfWeek.WEDNESDAY));
        assertEquals(false, dow.contains(DayOfWeek.THURSDAY));
        assertEquals(false, dow.contains(DayOfWeek.FRIDAY));
        assertEquals(false, dow.contains(DayOfWeek.SATURDAY));
        assertEquals(false, dow.contains(DayOfWeek.SUNDAY));

        dow.setMonday(false);
        dow.setTuesday(true);
        assertEquals(false, dow.contains(DayOfWeek.MONDAY));
        assertEquals(true, dow.contains(DayOfWeek.TUESDAY));
        assertEquals(false, dow.contains(DayOfWeek.WEDNESDAY));
        assertEquals(false, dow.contains(DayOfWeek.THURSDAY));
        assertEquals(false, dow.contains(DayOfWeek.FRIDAY));
        assertEquals(false, dow.contains(DayOfWeek.SATURDAY));
        assertEquals(false, dow.contains(DayOfWeek.SUNDAY));

        dow.setTuesday(false);
        dow.setWednesday(true);
        assertEquals(false, dow.contains(DayOfWeek.MONDAY));
        assertEquals(false, dow.contains(DayOfWeek.TUESDAY));
        assertEquals(true, dow.contains(DayOfWeek.WEDNESDAY));
        assertEquals(false, dow.contains(DayOfWeek.THURSDAY));
        assertEquals(false, dow.contains(DayOfWeek.FRIDAY));
        assertEquals(false, dow.contains(DayOfWeek.SATURDAY));
        assertEquals(false, dow.contains(DayOfWeek.SUNDAY));

        dow.setWednesday(false);
        dow.setThursday(true);
        assertEquals(false, dow.contains(DayOfWeek.MONDAY));
        assertEquals(false, dow.contains(DayOfWeek.TUESDAY));
        assertEquals(false, dow.contains(DayOfWeek.WEDNESDAY));
        assertEquals(true, dow.contains(DayOfWeek.THURSDAY));
        assertEquals(false, dow.contains(DayOfWeek.FRIDAY));
        assertEquals(false, dow.contains(DayOfWeek.SATURDAY));
        assertEquals(false, dow.contains(DayOfWeek.SUNDAY));

        dow.setThursday(false);
        dow.setFriday(true);
        assertEquals(false, dow.contains(DayOfWeek.MONDAY));
        assertEquals(false, dow.contains(DayOfWeek.TUESDAY));
        assertEquals(false, dow.contains(DayOfWeek.WEDNESDAY));
        assertEquals(false, dow.contains(DayOfWeek.THURSDAY));
        assertEquals(true, dow.contains(DayOfWeek.FRIDAY));
        assertEquals(false, dow.contains(DayOfWeek.SATURDAY));
        assertEquals(false, dow.contains(DayOfWeek.SUNDAY));

        dow.setFriday(false);
        dow.setSaturday(true);
        assertEquals(false, dow.contains(DayOfWeek.MONDAY));
        assertEquals(false, dow.contains(DayOfWeek.TUESDAY));
        assertEquals(false, dow.contains(DayOfWeek.WEDNESDAY));
        assertEquals(false, dow.contains(DayOfWeek.THURSDAY));
        assertEquals(false, dow.contains(DayOfWeek.FRIDAY));
        assertEquals(true, dow.contains(DayOfWeek.SATURDAY));
        assertEquals(false, dow.contains(DayOfWeek.SUNDAY));

        dow.setSaturday(false);
        dow.setSunday(true);
        assertEquals(false, dow.contains(DayOfWeek.MONDAY));
        assertEquals(false, dow.contains(DayOfWeek.TUESDAY));
        assertEquals(false, dow.contains(DayOfWeek.WEDNESDAY));
        assertEquals(false, dow.contains(DayOfWeek.THURSDAY));
        assertEquals(false, dow.contains(DayOfWeek.FRIDAY));
        assertEquals(false, dow.contains(DayOfWeek.SATURDAY));
        assertEquals(true, dow.contains(DayOfWeek.SUNDAY));
    }

    @Test
    public void containsIndex() {
        final DaysOfWeek dow = new DaysOfWeek(false);
        assertEquals(false, dow.contains(0));
        assertEquals(false, dow.contains(1));
        assertEquals(false, dow.contains(2));
        assertEquals(false, dow.contains(3));
        assertEquals(false, dow.contains(4));
        assertEquals(false, dow.contains(5));
        assertEquals(false, dow.contains(6));

        dow.setMonday(true);
        assertEquals(true, dow.contains(0));
        assertEquals(false, dow.contains(1));
        assertEquals(false, dow.contains(2));
        assertEquals(false, dow.contains(3));
        assertEquals(false, dow.contains(4));
        assertEquals(false, dow.contains(5));
        assertEquals(false, dow.contains(6));

        dow.setMonday(false);
        dow.setTuesday(true);
        assertEquals(false, dow.contains(0));
        assertEquals(true, dow.contains(1));
        assertEquals(false, dow.contains(2));
        assertEquals(false, dow.contains(3));
        assertEquals(false, dow.contains(4));
        assertEquals(false, dow.contains(5));
        assertEquals(false, dow.contains(6));

        dow.setTuesday(false);
        dow.setWednesday(true);
        assertEquals(false, dow.contains(0));
        assertEquals(false, dow.contains(1));
        assertEquals(true, dow.contains(2));
        assertEquals(false, dow.contains(3));
        assertEquals(false, dow.contains(4));
        assertEquals(false, dow.contains(5));
        assertEquals(false, dow.contains(6));

        dow.setWednesday(false);
        dow.setThursday(true);
        assertEquals(false, dow.contains(0));
        assertEquals(false, dow.contains(1));
        assertEquals(false, dow.contains(2));
        assertEquals(true, dow.contains(3));
        assertEquals(false, dow.contains(4));
        assertEquals(false, dow.contains(5));
        assertEquals(false, dow.contains(6));

        dow.setThursday(false);
        dow.setFriday(true);
        assertEquals(false, dow.contains(0));
        assertEquals(false, dow.contains(1));
        assertEquals(false, dow.contains(2));
        assertEquals(false, dow.contains(3));
        assertEquals(true, dow.contains(4));
        assertEquals(false, dow.contains(5));
        assertEquals(false, dow.contains(6));

        dow.setFriday(false);
        dow.setSaturday(true);
        assertEquals(false, dow.contains(0));
        assertEquals(false, dow.contains(1));
        assertEquals(false, dow.contains(2));
        assertEquals(false, dow.contains(3));
        assertEquals(false, dow.contains(4));
        assertEquals(true, dow.contains(5));
        assertEquals(false, dow.contains(6));

        dow.setSaturday(false);
        dow.setSunday(true);
        assertEquals(false, dow.contains(0));
        assertEquals(false, dow.contains(1));
        assertEquals(false, dow.contains(2));
        assertEquals(false, dow.contains(3));
        assertEquals(false, dow.contains(4));
        assertEquals(false, dow.contains(5));
        assertEquals(true, dow.contains(6));
    }
}
