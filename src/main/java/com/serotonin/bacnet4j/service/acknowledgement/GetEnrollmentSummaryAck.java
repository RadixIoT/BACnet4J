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

package com.serotonin.bacnet4j.service.acknowledgement;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class GetEnrollmentSummaryAck extends AcknowledgementService {
    public static final byte TYPE_ID = 4;

    private final SequenceOf<EnrollmentSummary> values;

    public GetEnrollmentSummaryAck(final SequenceOf<EnrollmentSummary> values) {
        this.values = values;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, values);
    }

    GetEnrollmentSummaryAck(final ByteQueue queue) throws BACnetException {
        values = readSequenceOf(queue, EnrollmentSummary.class);
    }

    public SequenceOf<EnrollmentSummary> getValues() {
        return values;
    }

    public static class EnrollmentSummary extends BaseType {
        private final ObjectIdentifier objectIdentifier;
        private final EventType eventType;
        private final EventState eventState;
        private final UnsignedInteger priority;
        private final UnsignedInteger notificationClass; // optional

        public EnrollmentSummary(final ObjectIdentifier objectIdentifier, final EventType eventType,
                final EventState eventState, final UnsignedInteger priority, final UnsignedInteger notificationClass) {
            this.objectIdentifier = objectIdentifier;
            this.eventType = eventType;
            this.eventState = eventState;
            this.priority = priority;
            this.notificationClass = notificationClass;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, objectIdentifier);
            write(queue, eventType);
            write(queue, eventState);
            write(queue, priority);
            writeOptional(queue, notificationClass);
        }

        public EnrollmentSummary(final ByteQueue queue) throws BACnetException {
            objectIdentifier = read(queue, ObjectIdentifier.class);
            eventType = read(queue, EventType.class);
            eventState = read(queue, EventState.class);
            priority = read(queue, UnsignedInteger.class);
            if (peekTagNumber(queue) == UnsignedInteger.TYPE_ID)
                notificationClass = read(queue, UnsignedInteger.class);
            else
                notificationClass = null;
        }

        public ObjectIdentifier getObjectIdentifier() {
            return objectIdentifier;
        }

        public EventType getEventType() {
            return eventType;
        }

        public EventState getEventState() {
            return eventState;
        }

        public UnsignedInteger getPriority() {
            return priority;
        }

        public UnsignedInteger getNotificationClass() {
            return notificationClass;
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (eventState == null ? 0 : eventState.hashCode());
            result = PRIME * result + (eventType == null ? 0 : eventType.hashCode());
            result = PRIME * result + (notificationClass == null ? 0 : notificationClass.hashCode());
            result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
            result = PRIME * result + (priority == null ? 0 : priority.hashCode());
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
            final EnrollmentSummary other = (EnrollmentSummary) obj;
            if (eventState == null) {
                if (other.eventState != null)
                    return false;
            } else if (!eventState.equals(other.eventState))
                return false;
            if (eventType == null) {
                if (other.eventType != null)
                    return false;
            } else if (!eventType.equals(other.eventType))
                return false;
            if (notificationClass == null) {
                if (other.notificationClass != null)
                    return false;
            } else if (!notificationClass.equals(other.notificationClass))
                return false;
            if (objectIdentifier == null) {
                if (other.objectIdentifier != null)
                    return false;
            } else if (!objectIdentifier.equals(other.objectIdentifier))
                return false;
            if (priority == null) {
                if (other.priority != null)
                    return false;
            } else if (!priority.equals(other.priority))
                return false;
            return true;
        }

    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (values == null ? 0 : values.hashCode());
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
        final GetEnrollmentSummaryAck other = (GetEnrollmentSummaryAck) obj;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }
}
