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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.DateMatchable;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Date extends Primitive implements Comparable<Date>, DateMatchable {
    public static final Date MINIMUM_DATE = new Date(0, Month.JANUARY, 1, null);
    public static final Date MAXIMUM_DATE = new Date(254, Month.DECEMBER, 31, null);

    public static final int UNSPECIFIED_YEAR = 255;
    public static final int UNSPECIFIED_DAY = 255;
    public static final int LAST_DAY_OF_MONTH = 32;
    public static final int ODD_DAYS = 33;
    public static final int EVEN_DAYS = 34;
    public static final Date UNSPECIFIED = new Date(-1, Month.UNSPECIFIED, -1, DayOfWeek.UNSPECIFIED);

    public static final byte TYPE_ID = 10;

    private int year;
    private Month month;
    private int day;
    private DayOfWeek dayOfWeek;

    public Date(int year, Month month, int day, DayOfWeek dayOfWeek) {
        if (year >= 1900)
            this.year = year - 1900;
        else if (year == -1)
            this.year = UNSPECIFIED_YEAR;
        else
            this.year = year;

        if (day == -1)
            this.day = UNSPECIFIED_DAY;
        else if ((day < 1 || day > EVEN_DAYS) && day != UNSPECIFIED_DAY)
            throw new BACnetRuntimeException("Invalid day value");
        else
            this.day = day;

        this.month = Objects.requireNonNullElse(month, Month.UNSPECIFIED);
        this.dayOfWeek = Objects.requireNonNullElse(dayOfWeek, DayOfWeek.UNSPECIFIED);
    }

    public Date(LocalDevice localDevice) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(localDevice.getClock().millis());
        resetTo(gc);
    }

    public Date(GregorianCalendar gc) {
        resetTo(gc);
    }

    private void resetTo(GregorianCalendar gc) {
        this.year = gc.get(Calendar.YEAR) - 1900;
        this.month = Month.valueOf((byte) (gc.get(Calendar.MONTH) + 1));
        this.day = gc.get(Calendar.DATE);
        this.dayOfWeek = DayOfWeek.valueOf((byte) ((gc.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1));
    }

    public int getYear() {
        return year;
    }

    public int getCenturyYear() {
        return year + 1900;
    }

    public Month getMonth() {
        return month;
    }

    public boolean isLastDayOfMonth() {
        return day == LAST_DAY_OF_MONTH;
    }

    public boolean isOddDays() {
        return day == ODD_DAYS;
    }

    public boolean isEvenDays() {
        return day == EVEN_DAYS;
    }

    public int getDay() {
        return day;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public GregorianCalendar calculateGC() {
        if (!isSpecific())
            throw new BACnetRuntimeException("Date must be completely specified to calculate calendar");
        return new GregorianCalendar(year + 1900, (month.getId() & 0xff) - 1, day, 12, 0);
    }

    /**
     * @return true if the date has been completely specified, false if any fields is unspecified.
     */
    public boolean isSpecific() {
        if (year == UNSPECIFIED_YEAR)
            return false;
        if (!month.isSpecific())
            return false;
        return day != UNSPECIFIED_DAY && day != LAST_DAY_OF_MONTH && day != ODD_DAYS && day != EVEN_DAYS;
    }

    /**
     * Matches this presumably wildcard date with a (that) necessarily specifically defined date to determine if (true)
     * the given date is one of this's defined dates or (false) not.
     *
     * @param that the specific date with which to compare.
     */
    @Override
    public boolean matches(Date that) {
        if (!that.isSpecific())
            throw new BACnetRuntimeException("Dates for matching must be completely specified: " + that);

        if (!matchYear(that.year))
            return false;

        if (!month.matches(that.month))
            return false;

        if (!matchDay(that))
            return false;

        return dayOfWeek.matches(that);
    }

    private boolean matchYear(int that) {
        if (year == UNSPECIFIED_YEAR)
            return true;
        return year == that;
    }

    private boolean matchDay(Date that) {
        if (day == UNSPECIFIED_DAY)
            return true;
        if (day == LAST_DAY_OF_MONTH) {
            GregorianCalendar gc = that.calculateGC();
            int lastDay = gc.getActualMaximum(Calendar.DATE);
            return lastDay == that.day;
        }
        if (day == ODD_DAYS) {
            GregorianCalendar gc = that.calculateGC();
            int dom = gc.get(Calendar.DAY_OF_MONTH);
            if (dom % 2 != 0) {
                return true;
            }
        }
        if (day == EVEN_DAYS) {
            GregorianCalendar gc = that.calculateGC();
            int dom = gc.get(Calendar.DAY_OF_MONTH);
            if (dom % 2 == 0) {
                return true;
            }
        }
        return day == that.day;
    }

    @Override
    public int compareTo(Date that) {
        if (!isSpecific())
            throw new BACnetRuntimeException("Comparisons can only be made between specific dates: " + this);
        if (!that.isSpecific())
            throw new BACnetRuntimeException("Comparisons can only be made between specific dates: " + that);

        if (year == that.year) {
            if (month == that.month)
                return day - that.day;
            return month.ordinal() - that.month.ordinal();
        }
        return year - that.year;
    }

    public boolean before(Date that) {
        return compareTo(that) < 0;
    }

    public boolean after(Date that) {
        return compareTo(that) > 0;
    }

    public boolean sameAs(Date that) {
        return compareTo(that) == 0;
    }

    public Date calculateLeastMatchOnOrBefore(Date that) {
        if (equals(UNSPECIFIED)) // Performance improvement
            return MINIMUM_DATE;

        boolean matched = matches(that);
        GregorianCalendar gc = that.calculateGC();

        if (year != UNSPECIFIED_YEAR && year < that.year) // Performance improvement
            gc.add(Calendar.YEAR, year - that.year + 1);

        Date date = new Date(gc);
        while (true) {
            if (date.sameAs(MINIMUM_DATE))
                return matched ? date : null;
            gc.add(Calendar.DATE, -1);
            date.resetTo(gc);
            boolean b = matches(date);
            if (b && !matched)
                matched = true;
            else if (matched && !b)
                break;
            if (year != UNSPECIFIED_YEAR && year > date.year) // Performance improvement
                return null;
        }

        gc.add(Calendar.DATE, 1);
        date.resetTo(gc);
        return date;
    }

    public Date calculateGreatestMatchOnOrBefore(Date that) {
        if (equals(UNSPECIFIED)) // Performance improvement
            return null;

        GregorianCalendar gc = that.calculateGC();
        if (year != UNSPECIFIED_YEAR && year < that.year) // Performance improvement
            gc.add(Calendar.YEAR, year - that.year + 1);

        if (!that.sameAs(MAXIMUM_DATE)) {
            // Start a day ahead
            gc.add(Calendar.DATE, 1);
        }
        Date date = new Date(gc);

        boolean matched = matches(date);
        while (true) {
            gc.add(Calendar.DATE, -1);
            date.resetTo(gc);
            boolean b = matches(date);
            if (!b && matched)
                matched = false;
            else if (!matched && b)
                break;
            if (date.sameAs(MINIMUM_DATE))
                return null;
            if (year != UNSPECIFIED_YEAR && year > date.year) // Performance improvement
                return null;
        }

        return date;
    }

    public Date calculateLeastMatchOnOrAfter(Date that) {
        GregorianCalendar gc = that.calculateGC();
        if (!that.sameAs(MINIMUM_DATE)) {
            // Start a day behind
            gc.add(Calendar.DATE, -1);
        }
        Date date = new Date(gc);

        boolean matched = matches(date);
        while (true) {
            gc.add(Calendar.DATE, 1);
            date.resetTo(gc);
            boolean b = matches(date);
            if (!b && matched)
                matched = false;
            else if (!matched && b)
                break;
            if (date.sameAs(MAXIMUM_DATE))
                return null;
        }

        return date;
    }

    public Date calculateGreatestMatchOnOrAfter(Date that) {
        if (equals(UNSPECIFIED))
            return MAXIMUM_DATE;

        boolean matched = matches(that);
        GregorianCalendar gc = that.calculateGC();

        Date date = new Date(gc);
        while (true) {
            gc.add(Calendar.DATE, 1);
            date.resetTo(gc);
            boolean b = matches(date);
            if (b && !matched)
                matched = true;
            else if (matched && !b)
                break;
            if (date.sameAs(MAXIMUM_DATE))
                return date;
        }

        gc.add(Calendar.DATE, -1);
        date.resetTo(gc);
        return date;
    }

    //
    // Reading and writing
    //
    public Date(ByteQueue queue) throws BACnetErrorException {
        // 135-2024 clause 20.2.12: four contents octets.
        readTag(queue, TYPE_ID, 4, 4);
        year = queue.popU1B();
        month = Month.valueOf(queue.pop());
        day = queue.popU1B();
        dayOfWeek = DayOfWeek.valueOf(queue.pop());
    }

    @Override
    public void writeImpl(ByteQueue queue) {
        queue.push(year);
        queue.push(month.getId());
        queue.push((byte) day);
        queue.push(dayOfWeek.getId());
    }

    @Override
    protected long getLength() {
        return 4;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Date date = (Date) o;
        return year == date.year && day == date.day && month == date.month && dayOfWeek == date.dayOfWeek;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day, dayOfWeek);
    }

    @Override
    public String toString() {
        return "Date [year=" + year + ", month=" + month + ", day=" + day + ", dayOfWeek=" + dayOfWeek + "]";
    }
}
