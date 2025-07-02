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

package com.serotonin.bacnet4j.type.eventParameter;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.ChangeOfLifeSafetyAlgo;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfLifeSafety extends AbstractEventParameter {
    public static final byte TYPE_ID = 8;

    private final UnsignedInteger timeDelay;
    private final SequenceOf<LifeSafetyState> listOfLifeSafetyAlarmValues;
    private final SequenceOf<LifeSafetyState> listOfAlarmValues;
    private final DeviceObjectPropertyReference modePropertyReference;

    public ChangeOfLifeSafety(final UnsignedInteger timeDelay,
            final SequenceOf<LifeSafetyState> listOfLifeSafetyAlarmValues,
            final SequenceOf<LifeSafetyState> listOfAlarmValues,
            final DeviceObjectPropertyReference modePropertyReference) {
        this.timeDelay = timeDelay;
        this.listOfLifeSafetyAlarmValues = listOfLifeSafetyAlarmValues;
        this.listOfAlarmValues = listOfAlarmValues;
        this.modePropertyReference = modePropertyReference;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, listOfLifeSafetyAlarmValues, 1);
        write(queue, listOfAlarmValues, 2);
        write(queue, modePropertyReference, 3);
    }

    public ChangeOfLifeSafety(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        listOfLifeSafetyAlarmValues = readSequenceOf(queue, LifeSafetyState.class, 1);
        listOfAlarmValues = readSequenceOf(queue, LifeSafetyState.class, 2);
        modePropertyReference = read(queue, DeviceObjectPropertyReference.class, 3);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public SequenceOf<LifeSafetyState> getListOfLifeSafetyAlarmValues() {
        return listOfLifeSafetyAlarmValues;
    }

    public SequenceOf<LifeSafetyState> getListOfAlarmValues() {
        return listOfAlarmValues;
    }

    public DeviceObjectPropertyReference getModePropertyReference() {
        return modePropertyReference;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return new ChangeOfLifeSafetyAlgo();
    }

    @Override
    public DeviceObjectPropertyReference getReference() {
        return modePropertyReference;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (listOfAlarmValues == null ? 0 : listOfAlarmValues.hashCode());
        result = PRIME * result + (listOfLifeSafetyAlarmValues == null ? 0 : listOfLifeSafetyAlarmValues.hashCode());
        result = PRIME * result + (modePropertyReference == null ? 0 : modePropertyReference.hashCode());
        result = PRIME * result + (timeDelay == null ? 0 : timeDelay.hashCode());
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
        final ChangeOfLifeSafety other = (ChangeOfLifeSafety) obj;
        if (listOfAlarmValues == null) {
            if (other.listOfAlarmValues != null)
                return false;
        } else if (!listOfAlarmValues.equals(other.listOfAlarmValues))
            return false;
        if (listOfLifeSafetyAlarmValues == null) {
            if (other.listOfLifeSafetyAlarmValues != null)
                return false;
        } else if (!listOfLifeSafetyAlarmValues.equals(other.listOfLifeSafetyAlarmValues))
            return false;
        if (modePropertyReference == null) {
            if (other.modePropertyReference != null)
                return false;
        } else if (!modePropertyReference.equals(other.modePropertyReference))
            return false;
        if (timeDelay == null) {
            if (other.timeDelay != null)
                return false;
        } else if (!timeDelay.equals(other.timeDelay))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ChangeOfLifeSafety[ timeDelay=" + timeDelay + ", listOfLifeSafetyAlarmValues=" + listOfLifeSafetyAlarmValues + ", listOfAlarmValues=" + listOfAlarmValues + ", modePropertyReference=" + modePropertyReference + ']';
    }
}
