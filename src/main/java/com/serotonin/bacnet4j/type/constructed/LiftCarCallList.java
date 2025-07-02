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
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class LiftCarCallList extends BaseType {
    private final SequenceOf<Unsigned8> floorNumbers;

    public LiftCarCallList(final SequenceOf<Unsigned8> floorNumbers) {
        this.floorNumbers = floorNumbers;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, floorNumbers, 0);
    }

    public LiftCarCallList(final ByteQueue queue) throws BACnetException {
        floorNumbers = readSequenceOf(queue, Unsigned8.class, 0);
    }

    public SequenceOf<Unsigned8> getFloorNumbers() {
        return floorNumbers;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (floorNumbers == null ? 0 : floorNumbers.hashCode());
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
        final LiftCarCallList other = (LiftCarCallList) obj;
        if (floorNumbers == null) {
            if (other.floorNumbers != null)
                return false;
        } else if (!floorNumbers.equals(other.floorNumbers))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "LiftCarCallList[" + "floorNumbers=" + floorNumbers + ']';
    }
}
