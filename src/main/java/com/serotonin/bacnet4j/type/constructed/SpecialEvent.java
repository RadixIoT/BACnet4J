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
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SpecialEvent extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, CalendarEntry.class);
        choiceOptions.addContextual(1, ObjectIdentifier.class);
    }

    private final Choice period;
    private final SequenceOf<TimeValue> listOfTimeValues;
    private final UnsignedInteger eventPriority;

    public SpecialEvent(final CalendarEntry calendarEntry, final SequenceOf<TimeValue> listOfTimeValues,
            final UnsignedInteger eventPriority) {
        period = new Choice(0, calendarEntry, choiceOptions);
        this.listOfTimeValues = listOfTimeValues;
        this.eventPriority = eventPriority;
    }

    public SpecialEvent(final ObjectIdentifier calendarReference, final SequenceOf<TimeValue> listOfTimeValues,
            final UnsignedInteger eventPriority) {
        period = new Choice(1, calendarReference, choiceOptions);
        this.listOfTimeValues = listOfTimeValues;
        this.eventPriority = eventPriority;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, period);
        write(queue, listOfTimeValues, 2);
        write(queue, eventPriority, 3);
    }

    public boolean isCalendarEntry() {
        return period.isa(CalendarEntry.class);
    }

    public CalendarEntry getCalendarEntry() {
        return period.getDatum();
    }

    public boolean isCalendarReference() {
        return period.isa(ObjectIdentifier.class);
    }

    public ObjectIdentifier getCalendarReference() {
        return period.getDatum();
    }

    public boolean isListOfTimeValues() {
        return period.isa(SequenceOf.class);
    }

    public SequenceOf<TimeValue> getListOfTimeValues() {
        return listOfTimeValues;
    }

    public boolean isEventPriority() {
        return period.isa(UnsignedInteger.class);
    }

    public UnsignedInteger getEventPriority() {
        return eventPriority;
    }

    public SpecialEvent(final ByteQueue queue) throws BACnetException {
        period = new Choice(queue, choiceOptions);
        listOfTimeValues = readSequenceOf(queue, TimeValue.class, 2);
        eventPriority = read(queue, UnsignedInteger.class, 3);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (period == null ? 0 : period.hashCode());
        result = PRIME * result + (eventPriority == null ? 0 : eventPriority.hashCode());
        result = PRIME * result + (listOfTimeValues == null ? 0 : listOfTimeValues.hashCode());
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
        final SpecialEvent other = (SpecialEvent) obj;
        if (period == null) {
            if (other.period != null)
                return false;
        } else if (!period.equals(other.period))
            return false;
        if (eventPriority == null) {
            if (other.eventPriority != null)
                return false;
        } else if (!eventPriority.equals(other.eventPriority))
            return false;
        if (listOfTimeValues == null) {
            if (other.listOfTimeValues != null)
                return false;
        } else if (!listOfTimeValues.equals(other.listOfTimeValues))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SpecialEvent [calendar=" + period + ", listOfTimeValues=" + listOfTimeValues + ", eventPriority="
                + eventPriority + "]";
    }
}
