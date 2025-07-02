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
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class GetEventInformationAck extends AcknowledgementService {
    public static final byte TYPE_ID = 29;

    private final SequenceOf<EventSummary> listOfEventSummaries;
    private final Boolean moreEvents;

    public GetEventInformationAck(final SequenceOf<EventSummary> listOfEventSummaries, final Boolean moreEvents) {
        this.listOfEventSummaries = listOfEventSummaries;
        this.moreEvents = moreEvents;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, listOfEventSummaries, 0);
        write(queue, moreEvents, 1);
    }

    GetEventInformationAck(final ByteQueue queue) throws BACnetException {
        listOfEventSummaries = readSequenceOf(queue, EventSummary.class, 0);
        moreEvents = read(queue, Boolean.class, 1);
    }

    public SequenceOf<EventSummary> getListOfEventSummaries() {
        return listOfEventSummaries;
    }

    public Boolean getMoreEvents() {
        return moreEvents;
    }

    public static class EventSummary extends BaseType {
        private final ObjectIdentifier objectIdentifier;
        private final EventState eventState;
        private final EventTransitionBits acknowledgedTransitions;
        private final BACnetArray<TimeStamp> eventTimeStamps;
        private final NotifyType notifyType;
        private final EventTransitionBits eventEnable;
        private final BACnetArray<UnsignedInteger> eventPriorities;

        public EventSummary(final ObjectIdentifier objectIdentifier, final EventState eventState,
                final EventTransitionBits acknowledgedTransitions, final BACnetArray<TimeStamp> eventTimeStamps,
                final NotifyType notifyType, final EventTransitionBits eventEnable,
                final BACnetArray<UnsignedInteger> eventPriorities) {
            this.objectIdentifier = objectIdentifier;
            this.eventState = eventState;
            this.acknowledgedTransitions = acknowledgedTransitions;
            this.eventTimeStamps = eventTimeStamps;
            this.notifyType = notifyType;
            this.eventEnable = eventEnable;
            this.eventPriorities = eventPriorities;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, objectIdentifier, 0);
            write(queue, eventState, 1);
            write(queue, acknowledgedTransitions, 2);
            write(queue, eventTimeStamps, 3);
            write(queue, notifyType, 4);
            write(queue, eventEnable, 5);
            write(queue, eventPriorities, 6);
        }

        public EventSummary(final ByteQueue queue) throws BACnetException {
            objectIdentifier = read(queue, ObjectIdentifier.class, 0);
            eventState = read(queue, EventState.class, 1);
            acknowledgedTransitions = read(queue, EventTransitionBits.class, 2);
            eventTimeStamps = readArray(queue, TimeStamp.class, 3);
            notifyType = read(queue, NotifyType.class, 4);
            eventEnable = read(queue, EventTransitionBits.class, 5);
            eventPriorities = readArray(queue, UnsignedInteger.class, 6);
        }

        public ObjectIdentifier getObjectIdentifier() {
            return objectIdentifier;
        }

        public EventState getEventState() {
            return eventState;
        }

        public EventTransitionBits getAcknowledgedTransitions() {
            return acknowledgedTransitions;
        }

        public BACnetArray<TimeStamp> getEventTimeStamps() {
            return eventTimeStamps;
        }

        public NotifyType getNotifyType() {
            return notifyType;
        }

        public EventTransitionBits getEventEnable() {
            return eventEnable;
        }

        public BACnetArray<UnsignedInteger> getEventPriorities() {
            return eventPriorities;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (acknowledgedTransitions == null ? 0 : acknowledgedTransitions.hashCode());
            result = prime * result + (eventEnable == null ? 0 : eventEnable.hashCode());
            result = prime * result + (eventPriorities == null ? 0 : eventPriorities.hashCode());
            result = prime * result + (eventState == null ? 0 : eventState.hashCode());
            result = prime * result + (eventTimeStamps == null ? 0 : eventTimeStamps.hashCode());
            result = prime * result + (notifyType == null ? 0 : notifyType.hashCode());
            result = prime * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
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
            final EventSummary other = (EventSummary) obj;
            if (acknowledgedTransitions == null) {
                if (other.acknowledgedTransitions != null)
                    return false;
            } else if (!acknowledgedTransitions.equals(other.acknowledgedTransitions))
                return false;
            if (eventEnable == null) {
                if (other.eventEnable != null)
                    return false;
            } else if (!eventEnable.equals(other.eventEnable))
                return false;
            if (eventPriorities == null) {
                if (other.eventPriorities != null)
                    return false;
            } else if (!eventPriorities.equals(other.eventPriorities))
                return false;
            if (eventState == null) {
                if (other.eventState != null)
                    return false;
            } else if (!eventState.equals(other.eventState))
                return false;
            if (eventTimeStamps == null) {
                if (other.eventTimeStamps != null)
                    return false;
            } else if (!eventTimeStamps.equals(other.eventTimeStamps))
                return false;
            if (notifyType == null) {
                if (other.notifyType != null)
                    return false;
            } else if (!notifyType.equals(other.notifyType))
                return false;
            if (objectIdentifier == null) {
                if (other.objectIdentifier != null)
                    return false;
            } else if (!objectIdentifier.equals(other.objectIdentifier))
                return false;
            return true;
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (listOfEventSummaries == null ? 0 : listOfEventSummaries.hashCode());
        result = PRIME * result + (moreEvents == null ? 0 : moreEvents.hashCode());
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
        final GetEventInformationAck other = (GetEventInformationAck) obj;
        if (listOfEventSummaries == null) {
            if (other.listOfEventSummaries != null)
                return false;
        } else if (!listOfEventSummaries.equals(other.listOfEventSummaries))
            return false;
        if (moreEvents == null) {
            if (other.moreEvents != null)
                return false;
        } else if (!moreEvents.equals(other.moreEvents))
            return false;
        return true;
    }
}
