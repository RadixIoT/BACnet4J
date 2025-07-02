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

import java.util.Calendar;
import java.util.GregorianCalendar;

import com.serotonin.bacnet4j.type.primitive.Date;

public enum DayOfWeek {
    MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4), FRIDAY(5), SATURDAY(6), SUNDAY(7), UNSPECIFIED(255);

    private int id;

    DayOfWeek(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isSpecific() {
        return this != UNSPECIFIED;
    }

    public static DayOfWeek forCalendarDow(final int calendarDow) {
        switch (calendarDow) {
            case Calendar.MONDAY:
                return MONDAY;
            case Calendar.TUESDAY:
                return TUESDAY;
            case Calendar.WEDNESDAY:
                return WEDNESDAY;
            case Calendar.THURSDAY:
                return THURSDAY;
            case Calendar.FRIDAY:
                return FRIDAY;
            case Calendar.SATURDAY:
                return SATURDAY;
            case Calendar.SUNDAY:
                return SUNDAY;
            default:
                throw new RuntimeException("Can't get day of week for calendar value: " + calendarDow);
        }
    }

    public static DayOfWeek valueOf(final int id) {
        if (id == MONDAY.id)
            return MONDAY;
        if (id == TUESDAY.id)
            return TUESDAY;
        if (id == WEDNESDAY.id)
            return WEDNESDAY;
        if (id == THURSDAY.id)
            return THURSDAY;
        if (id == FRIDAY.id)
            return FRIDAY;
        if (id == SATURDAY.id)
            return SATURDAY;
        if (id == SUNDAY.id)
            return SUNDAY;
        return UNSPECIFIED;
    }

    public static DayOfWeek forDate(final Date date) {
        final GregorianCalendar gc = date.calculateGC();
        final int calendarDow = gc.get(Calendar.DAY_OF_WEEK);
        return forCalendarDow(calendarDow);
    }

    public boolean matches(final Date date) {
        if (this == DayOfWeek.UNSPECIFIED)
            return true;

        final DayOfWeek thatDow = date.getDayOfWeek();

        // Tolerate the day of week of the given date being unspecified.
        if (thatDow.isSpecific())
            return this == thatDow;

        // Otherwise we need to calculate the given date's day of week.
        return equals(forDate(date));
    }
}
