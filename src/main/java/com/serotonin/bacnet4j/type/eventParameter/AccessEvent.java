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
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AccessEvent extends AbstractEventParameter {
    public static final byte TYPE_ID = 13;

    private final SequenceOf<com.serotonin.bacnet4j.type.enumerated.AccessEvent> listOfAccessEvents;
    private final DeviceObjectPropertyReference accessEventTimeReference;

    public AccessEvent(final SequenceOf<com.serotonin.bacnet4j.type.enumerated.AccessEvent> listOfAccessEvents,
            final DeviceObjectPropertyReference accessEventTimeReference) {
        this.listOfAccessEvents = listOfAccessEvents;
        this.accessEventTimeReference = accessEventTimeReference;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, listOfAccessEvents, 0);
        write(queue, accessEventTimeReference, 1);
    }

    public AccessEvent(final ByteQueue queue) throws BACnetException {
        listOfAccessEvents = readSequenceOf(queue, com.serotonin.bacnet4j.type.enumerated.AccessEvent.class, 0);
        accessEventTimeReference = read(queue, DeviceObjectPropertyReference.class, 1);
    }

    public SequenceOf<com.serotonin.bacnet4j.type.enumerated.AccessEvent> getListOfAccessEvents() {
        return listOfAccessEvents;
    }

    public DeviceObjectPropertyReference getAccessEventTimeReference() {
        return accessEventTimeReference;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return null;
    }

    @Override
    public DeviceObjectPropertyReference getReference() {
        return accessEventTimeReference;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (accessEventTimeReference == null ? 0 : accessEventTimeReference.hashCode());
        result = prime * result + (listOfAccessEvents == null ? 0 : listOfAccessEvents.hashCode());
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
        final AccessEvent other = (AccessEvent) obj;
        if (accessEventTimeReference == null) {
            if (other.accessEventTimeReference != null)
                return false;
        } else if (!accessEventTimeReference.equals(other.accessEventTimeReference))
            return false;
        if (listOfAccessEvents == null) {
            if (other.listOfAccessEvents != null)
                return false;
        } else if (!listOfAccessEvents.equals(other.listOfAccessEvents))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AccessEvent[ listOfAccessEvents=" + listOfAccessEvents + ", accessEventTimeReference=" + accessEventTimeReference + ']';
    }
}
