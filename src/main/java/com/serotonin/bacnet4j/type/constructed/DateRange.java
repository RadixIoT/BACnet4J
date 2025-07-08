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

import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.DateMatchable;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class DateRange extends BaseType implements DateMatchable {
    private final Date startDate;
    private final Date endDate;

    public DateRange(final Date startDate, final Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        validateInput();
    }

    public DateRange(final ByteQueue queue) throws BACnetException {
        startDate = read(queue, Date.class);
        endDate = read(queue, Date.class);
        validateInput();
    }


    private void validateInput() {
        if (startDate.getYear() != Date.UNSPECIFIED_YEAR && endDate.getYear() == Date.UNSPECIFIED_YEAR
                || startDate.getYear() == Date.UNSPECIFIED_YEAR && endDate.getYear() != Date.UNSPECIFIED_YEAR)
            throw new BACnetRuntimeException("start and end years must both be specific or unspecific");
        if (startDate.getMonth() == Month.EVEN_MONTHS || startDate.getMonth() == Month.ODD_MONTHS)
            throw new BACnetRuntimeException("even/odd months are not supported in date ranges");
        if (endDate.getMonth() == Month.EVEN_MONTHS || endDate.getMonth() == Month.ODD_MONTHS)
            throw new BACnetRuntimeException("even/odd months are not supported in date ranges");
        if (startDate.getMonth() != Month.UNSPECIFIED && endDate.getMonth() == Month.UNSPECIFIED
                || startDate.getMonth() == Month.UNSPECIFIED && endDate.getMonth() != Month.UNSPECIFIED)
            throw new BACnetRuntimeException("start and end months must both be specific or unspecific");
        if (startDate.getDay() != Date.UNSPECIFIED_DAY && endDate.getDay() == Date.UNSPECIFIED_DAY
                || startDate.getDay() == Date.UNSPECIFIED_DAY && endDate.getDay() != Date.UNSPECIFIED_DAY)
            throw new BACnetRuntimeException("start and end day must both be specific or unspecific");
        if ((startDate.getDay() == Date.UNSPECIFIED_DAY && startDate.getDayOfWeek() != DayOfWeek.UNSPECIFIED)
                || (endDate.getDay() == Date.UNSPECIFIED_DAY && endDate.getDayOfWeek() != DayOfWeek.UNSPECIFIED))
            throw new BACnetRuntimeException("day of week ranges are not supported");
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, startDate);
        write(queue, endDate);
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (endDate == null ? 0 : endDate.hashCode());
        result = PRIME * result + (startDate == null ? 0 : startDate.hashCode());
        return result;
    }

    @Override
    public boolean matches(final Date date) {
        if (!date.isSpecific())
            throw new BACnetRuntimeException("Dates for matching must be completely specified: " + date);

        final Date leastBefore = startDate.calculateLeastMatchOnOrBefore(date);
        final Date greatestBefore = endDate.calculateGreatestMatchOnOrBefore(date);

        if (greatestBefore == null)
            return leastBefore != null;
        if (greatestBefore.before(leastBefore))
            return true;
        if (date.sameAs(greatestBefore))
            return true;
        return false;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DateRange other = (DateRange) obj;
        if (endDate == null) {
            if (other.endDate != null)
                return false;
        } else if (!endDate.equals(other.endDate))
            return false;
        if (startDate == null) {
            if (other.startDate != null)
                return false;
        } else if (!startDate.equals(other.startDate))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DateRange [startDate=" + startDate + ", endDate=" + endDate + "]";
    }
}
