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
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.ChangeOfBitstringAlgo;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfBitString extends AbstractEventParameter {
    public static final byte TYPE_ID = 0;

    private final UnsignedInteger timeDelay;
    private final BitString bitMask;
    private final SequenceOf<BitString> listOfBitstringValues;

    public ChangeOfBitString(final UnsignedInteger timeDelay, final BitString bitMask,
            final SequenceOf<BitString> listOfBitstringValues) {
        this.timeDelay = timeDelay;
        this.bitMask = bitMask;
        this.listOfBitstringValues = listOfBitstringValues;
    }

    public ChangeOfBitString(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        bitMask = read(queue, BitString.class, 1);
        listOfBitstringValues = readSequenceOf(queue, BitString.class, 2);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, bitMask, 1);
        write(queue, listOfBitstringValues, 2);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public BitString getBitMask() {
        return bitMask;
    }

    public SequenceOf<BitString> getListOfBitstringValues() {
        return listOfBitstringValues;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return new ChangeOfBitstringAlgo();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (bitMask == null ? 0 : bitMask.hashCode());
        result = PRIME * result + (listOfBitstringValues == null ? 0 : listOfBitstringValues.hashCode());
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
        final ChangeOfBitString other = (ChangeOfBitString) obj;
        if (bitMask == null) {
            if (other.bitMask != null)
                return false;
        } else if (!bitMask.equals(other.bitMask))
            return false;
        if (listOfBitstringValues == null) {
            if (other.listOfBitstringValues != null)
                return false;
        } else if (!listOfBitstringValues.equals(other.listOfBitstringValues))
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
        return "ChangeOfBitString[ timeDelay=" + timeDelay + ", bitMask=" + bitMask + ", listOfBitstringValues=" + listOfBitstringValues + ']';
    }
}
