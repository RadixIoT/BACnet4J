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
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfValue extends AbstractEventParameter {
    public static final byte TYPE_ID = 2;

    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, BitString.class);
        choiceOptions.addContextual(1, Real.class);
    }

    private final UnsignedInteger timeDelay;
    private final Choice newValue;

    public ChangeOfValue(final UnsignedInteger timeDelay, final BitString bitmask) {
        this.timeDelay = timeDelay;
        this.newValue = new Choice(0, bitmask, choiceOptions);
    }

    public ChangeOfValue(final UnsignedInteger timeDelay, final Real referencedPropertyIncrement) {
        this.timeDelay = timeDelay;
        this.newValue = new Choice(1, referencedPropertyIncrement, choiceOptions);
    }

    public ChangeOfValue(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        newValue = new Choice(queue, choiceOptions, 1);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, newValue, 1);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public Choice getNewValue() {
        return newValue;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (newValue == null ? 0 : newValue.hashCode());
        result = prime * result + (timeDelay == null ? 0 : timeDelay.hashCode());
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
        final ChangeOfValue other = (ChangeOfValue) obj;
        if (newValue == null) {
            if (other.newValue != null)
                return false;
        } else if (!newValue.equals(other.newValue))
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
        return "ChangeOfValue[ timeDelay=" + timeDelay + ", newValue=" + newValue + ']';
    }
}
