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

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.DateMatchable;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class WeekNDay extends OctetString implements DateMatchable {
    public static class WeekOfMonth extends Enumerated {
        public static final WeekOfMonth days1to7 = new WeekOfMonth(1);
        public static final WeekOfMonth days8to14 = new WeekOfMonth(2);
        public static final WeekOfMonth days15to21 = new WeekOfMonth(3);
        public static final WeekOfMonth days22to28 = new WeekOfMonth(4);
        public static final WeekOfMonth days29to31 = new WeekOfMonth(5);
        public static final WeekOfMonth last7Days = new WeekOfMonth(6);
        public static final WeekOfMonth any = new WeekOfMonth(255);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        static {
            Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        }

        public static WeekOfMonth forName(final String name) {
            return (WeekOfMonth) Enumerated.forName(nameMap, name);
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static WeekOfMonth valueOf(final byte b) {
            return switch (b) {
                case 1 -> days1to7;
                case 2 -> days8to14;
                case 3 -> days15to21;
                case 4 -> days22to28;
                case 5 -> days29to31;
                case 6 -> last7Days;
                default -> any;
            };
        }

        private WeekOfMonth(final int value) {
            super(value);
        }

        public WeekOfMonth(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        /**
         * Returns a unmodifiable map.
         *
         * @return unmodifiable map
         */
        public static Map<Integer, String> getPrettyMap() {
            return Collections.unmodifiableMap(prettyMap);
        }

        /**
         * Returns a unmodifiable nameMap.
         *
         * @return unmodifiable map
         */
        public static Map<String, Enumerated> getNameMap() {
            return Collections.unmodifiableMap(nameMap);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }

    public WeekNDay(final Month month, final WeekOfMonth weekOfMonth, final DayOfWeek dayOfWeek) {
        super(new byte[] {month.getId(), weekOfMonth.byteValue(), (byte) dayOfWeek.getId()});
    }

    public Month getMonth() {
        return Month.valueOf(getBytes()[0]);
    }

    public WeekOfMonth getWeekOfMonth() {
        return WeekOfMonth.valueOf(getBytes()[1]);
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.valueOf(getBytes()[2]);
    }

    public WeekNDay(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    @Override
    public boolean matches(final Date that) {
        if (!that.isSpecific())
            throw new BACnetRuntimeException("Dates for matching must be completely specified: " + that);

        if (!getMonth().matches(that.getMonth()))
            return false;

        if (!matchWeekOfMonth(that))
            return false;

        if (!getDayOfWeek().matches(that))
            return false;

        return true;
    }

    private boolean matchWeekOfMonth(final Date that) {
        final WeekOfMonth wom = getWeekOfMonth();
        if (wom.equals(WeekOfMonth.any))
            return true;
        final int day = that.getDay();
        if (wom.equals(WeekOfMonth.days1to7))
            return day >= 1 && day <= 7;
        if (wom.equals(WeekOfMonth.days8to14))
            return day >= 8 && day <= 14;
        if (wom.equals(WeekOfMonth.days15to21))
            return day >= 15 && day <= 21;
        if (wom.equals(WeekOfMonth.days22to28))
            return day >= 22 && day <= 28;
        if (wom.equals(WeekOfMonth.days29to31))
            return day >= 29 && day <= 31;

        // Calculate the last day of the month.
        final GregorianCalendar gc = that.calculateGC();
        final int lastDay = gc.getActualMaximum(Calendar.DATE);
        return day >= lastDay - 6 && day <= lastDay;
    }

    @Override
    public String toString() {
        return "WeekNDay [Month=" + getMonth() + ", WeekOfMonth=" + getWeekOfMonth() + ", DayOfWeek=" + getDayOfWeek() + "]";
    }

}
