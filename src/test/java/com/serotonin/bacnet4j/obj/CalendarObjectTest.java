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

package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.service.confirmed.AddListElementRequest;
import com.serotonin.bacnet4j.service.confirmed.RemoveListElementRequest;
import com.serotonin.bacnet4j.type.constructed.CalendarEntry;
import com.serotonin.bacnet4j.type.constructed.DateRange;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.WeekNDay;
import com.serotonin.bacnet4j.type.constructed.WeekNDay.WeekOfMonth;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Date;

public class CalendarObjectTest extends AbstractTest {
    @Test
    public void test() throws Exception {
        clock.set(2115, java.time.Month.JANUARY, 1, 12, 0, 0);

        final CalendarEntry ce = new CalendarEntry(
                new WeekNDay(Month.UNSPECIFIED, WeekOfMonth.days22to28,
                        DayOfWeek.WEDNESDAY)); // The Wednesday during the 4th week of each month.
        final SequenceOf<CalendarEntry> dateList = new SequenceOf<>( //
                new CalendarEntry(new Date(-1, null, -1, DayOfWeek.FRIDAY)), // Every Friday.
                new CalendarEntry(
                        new DateRange(new Date(-1, Month.NOVEMBER, -1, null), new Date(-1, Month.FEBRUARY, -1, null))),
                // November to February
                ce);

        final CalendarObject co = new CalendarObject(d1, 0, "cal0", dateList);

        co.updatePresentValue(); // November to February
        assertEquals(Boolean.TRUE, co.get(PropertyIdentifier.presentValue));

        clock.set(2115, java.time.Month.MARCH, 2, 12, 0, 0);
        co.updatePresentValue();
        assertEquals(Boolean.FALSE, co.get(PropertyIdentifier.presentValue));

        clock.set(2115, java.time.Month.MARCH, 8, 12, 0, 0); // A Friday
        co.updatePresentValue();
        assertEquals(Boolean.TRUE, co.get(PropertyIdentifier.presentValue));

        clock.set(2115, java.time.Month.MAY, 27, 12, 0, 0);
        co.updatePresentValue();
        assertEquals(Boolean.FALSE, co.get(PropertyIdentifier.presentValue));

        clock.set(2115, java.time.Month.MAY, 22, 12, 0, 0); // The Wednesday during the 4th week of each month.
        co.updatePresentValue();
        assertEquals(Boolean.TRUE, co.get(PropertyIdentifier.presentValue));

        // Set the time source to a time that does not match the current date list, but
        // will match a new entry.
        clock.set(2115, java.time.Month.JUNE, 17, 12, 0, 0);
        co.updatePresentValue(); // Uses the above time source.
        assertEquals(Boolean.FALSE, co.get(PropertyIdentifier.presentValue));

        final CalendarEntry newEntry = new CalendarEntry(new Date(-1, Month.JUNE, -1, null));
        final AddListElementRequest addReq = new AddListElementRequest(co.getId(), PropertyIdentifier.dateList, null,
                new SequenceOf<>(newEntry));
        d2.send(rd1, addReq).get();
        assertEquals(Boolean.TRUE, co.get(PropertyIdentifier.presentValue));

        clock.set(2115, java.time.Month.JULY, 24, 12, 0, 0);
        co.updatePresentValue(); // Uses the above time source.
        assertEquals(Boolean.TRUE, co.get(PropertyIdentifier.presentValue));

        final RemoveListElementRequest remReq = new RemoveListElementRequest(co.getId(), PropertyIdentifier.dateList,
                null, new SequenceOf<>(ce));
        d2.send(rd1, remReq).get();
        assertEquals(Boolean.FALSE, co.get(PropertyIdentifier.presentValue));

        // Check that the compensatory time works.
        co.setTimeTolerance(1000 * 60 * 3);
        clock.set(2115, java.time.Month.AUGUST, 8, 23, 58, 0);
        co.updatePresentValue(); // Uses the above time source.
        assertEquals(Boolean.TRUE, co.get(PropertyIdentifier.presentValue));
    }
}
