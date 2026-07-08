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

import java.util.Objects;

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

    public GetEnrollmentSummaryAck(SequenceOf<EnrollmentSummary> values) {
        this.values = values;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, values);
    }

    GetEnrollmentSummaryAck(ByteQueue queue) throws BACnetException {
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

        public EnrollmentSummary(ObjectIdentifier objectIdentifier, EventType eventType, EventState eventState,
                UnsignedInteger priority, UnsignedInteger notificationClass) {
            this.objectIdentifier = objectIdentifier;
            this.eventType = eventType;
            this.eventState = eventState;
            this.priority = priority;
            this.notificationClass = notificationClass;
        }

        @Override
        public void write(ByteQueue queue) {
            write(queue, objectIdentifier);
            write(queue, eventType);
            write(queue, eventState);
            write(queue, priority);
            writeOptional(queue, notificationClass);
        }

        public EnrollmentSummary(ByteQueue queue) throws BACnetException {
            objectIdentifier = read(queue, ObjectIdentifier.class);
            eventType = read(queue, EventType.class);
            eventState = read(queue, EventState.class);
            priority = read(queue, UnsignedInteger.class);
            notificationClass = readOptional(queue, UnsignedInteger.class);
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
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass())
                return false;
            EnrollmentSummary that = (EnrollmentSummary) o;
            return Objects.equals(objectIdentifier, that.objectIdentifier) && Objects.equals(eventType,
                    that.eventType) && Objects.equals(eventState, that.eventState) && Objects.equals(
                    priority, that.priority) && Objects.equals(notificationClass, that.notificationClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(objectIdentifier, eventType, eventState, priority, notificationClass);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        GetEnrollmentSummaryAck that = (GetEnrollmentSummaryAck) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(values);
    }
}
