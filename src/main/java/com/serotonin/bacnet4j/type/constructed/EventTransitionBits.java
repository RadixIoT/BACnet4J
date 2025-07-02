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

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class EventTransitionBits extends BitString {
    public EventTransitionBits(final boolean toOffnormal, final boolean toFault, final boolean toNormal) {
        super(new boolean[] {toOffnormal, toFault, toNormal});
    }

    public EventTransitionBits(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public EventTransitionBits(final EventTransitionBits that) {
        super(that);
    }

    public boolean isToOffnormal() {
        return getValue()[0];
    }

    public boolean isToFault() {
        return getValue()[1];
    }

    public boolean isToNormal() {
        return getValue()[2];
    }

    public boolean contains(final EventState toState) {
        return getValue(toState.getTransitionIndex());
    }

    @Override
    public String toString() {
        return "EventTransitionBits [to-offnormal=" + isToOffnormal() + ", to-fault=" + isToFault() + ", to-normal=" + isToNormal() + "]";
    }
}
