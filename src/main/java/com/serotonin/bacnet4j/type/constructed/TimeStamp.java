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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class TimeStamp extends BaseType {
    static final Logger LOG = LoggerFactory.getLogger(TimeStamp.class);

    private static final ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, Time.class);
        choiceOptions.addContextual(1, UnsignedInteger.class);
        choiceOptions.addContextual(2, DateTime.class);
    }

    public static final TimeStamp UNSPECIFIED_TIME = new TimeStamp(Time.UNSPECIFIED);
    public static final TimeStamp UNSPECIFIED_SEQUENCE = new TimeStamp(UnsignedInteger.ZERO);
    public static final TimeStamp UNSPECIFIED_DATETIME = new TimeStamp(DateTime.UNSPECIFIED);

    private final Choice choice;

    public TimeStamp(final Time time) {
        choice = new Choice(0, time, choiceOptions);
    }

    public TimeStamp(final UnsignedInteger sequenceNumber) {
        choice = new Choice(1, sequenceNumber, choiceOptions);
    }

    public TimeStamp(final DateTime dateTime) {
        choice = new Choice(2, dateTime, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public TimeStamp(final ByteQueue queue) throws BACnetException {
        choice = new Choice(queue, choiceOptions);
    }

    public boolean isTime() {
        return choice.isa(Time.class);
    }

    public Time getTime() {
        return choice.getDatum();
    }

    public boolean isSequenceNumber() {
        return choice.isa(UnsignedInteger.class);
    }

    public UnsignedInteger getSequenceNumber() {
        return choice.getDatum();
    }

    public boolean isDateTime() {
        return choice.isa(DateTime.class);
    }

    public DateTime getDateTime() {
        return choice.getDatum();
    }

    @Override
    public String toString() {
        return "TimeStamp [choice=" + choice + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (choice == null ? 0 : choice.hashCode());
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
        final TimeStamp other = (TimeStamp) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

}
