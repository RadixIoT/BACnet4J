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

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.DateMatchable;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class CalendarEntry extends BaseType implements DateMatchable {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, Date.class);
        choiceOptions.addContextual(1, DateRange.class);
        choiceOptions.addContextual(2, WeekNDay.class);
    }

    private final Choice entry;

    public CalendarEntry(final Date date) {
        entry = new Choice(0, date, choiceOptions);
    }

    public CalendarEntry(final DateRange dateRange) {
        entry = new Choice(1, dateRange, choiceOptions);
    }

    public CalendarEntry(final WeekNDay weekNDay) {
        entry = new Choice(2, weekNDay, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, entry);
    }

    public CalendarEntry(final ByteQueue queue) throws BACnetException {
        entry = new Choice(queue, choiceOptions);
    }

    public boolean isDate() {
        return entry.getDatum() instanceof Date;
    }

    public boolean isDateRange() {
        return entry.getDatum() instanceof DateRange;
    }

    public boolean isWeekNDay() {
        return entry.getDatum() instanceof WeekNDay;
    }

    public Date getDate() {
        return (Date) entry.getDatum();
    }

    public DateRange getDateRange() {
        return (DateRange) entry.getDatum();
    }

    public WeekNDay getWeekNDay() {
        return (WeekNDay) entry.getDatum();
    }

    @Override
    public boolean matches(final Date date) {
        DateMatchable matcher;
        if (isDate())
            matcher = getDate();
        else if (isDateRange())
            matcher = getDateRange();
        else if (isWeekNDay())
            matcher = getWeekNDay();
        else
            throw new BACnetRuntimeException("Unhandled calendar entry type");
        return matcher.matches(date);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (entry == null ? 0 : entry.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CalendarEntry other = (CalendarEntry) obj;
        if (entry == null) {
            if (other.entry != null)
                return false;
        } else if (!entry.equals(other.entry))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CalendarEntry [entry=" + entry + "]";
    }
}
