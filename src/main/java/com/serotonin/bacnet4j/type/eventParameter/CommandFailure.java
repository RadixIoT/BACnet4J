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
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.CommandFailureAlgo;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class CommandFailure extends AbstractEventParameter {
    public static final byte TYPE_ID = 3;

    private final UnsignedInteger timeDelay;
    private final DeviceObjectPropertyReference feedbackPropertyReference;

    public CommandFailure(final UnsignedInteger timeDelay,
            final DeviceObjectPropertyReference feedbackPropertyReference) {
        this.timeDelay = timeDelay;
        this.feedbackPropertyReference = feedbackPropertyReference;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, feedbackPropertyReference, 1);
    }

    public CommandFailure(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        feedbackPropertyReference = read(queue, DeviceObjectPropertyReference.class, 1);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public DeviceObjectPropertyReference getFeedbackPropertyReference() {
        return feedbackPropertyReference;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return new CommandFailureAlgo();
    }

    @Override
    public DeviceObjectPropertyReference getReference() {
        return feedbackPropertyReference;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (feedbackPropertyReference == null ? 0 : feedbackPropertyReference.hashCode());
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
        final CommandFailure other = (CommandFailure) obj;
        if (feedbackPropertyReference == null) {
            if (other.feedbackPropertyReference != null)
                return false;
        } else if (!feedbackPropertyReference.equals(other.feedbackPropertyReference))
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
        return "CommandFailure[ timeDelay=" + timeDelay + ", feedbackPropertyReference=" + feedbackPropertyReference + ']';
    }
}
