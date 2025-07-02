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

package com.serotonin.bacnet4j.service.confirmed;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.acknowledgement.GetEnrollmentSummaryAck;
import com.serotonin.bacnet4j.service.acknowledgement.GetEnrollmentSummaryAck.EnrollmentSummary;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.RecipientProcess;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class GetEnrollmentSummaryRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 4;

    private final AcknowledgmentFilter acknowledgmentFilter; // 0
    private final RecipientProcess enrollmentFilter; // 1 optional
    private final EventStateFilter eventStateFilter; // 2 optional
    private final EventType eventTypeFilter; // 3 optional
    private final PriorityFilter priorityFilter; // 4 optional
    private final UnsignedInteger notificationClassFilter; // 5 optional

    public GetEnrollmentSummaryRequest(final AcknowledgmentFilter acknowledgmentFilter,
            final RecipientProcess enrollmentFilter, final EventStateFilter eventStateFilter,
            final EventType eventTypeFilter, final PriorityFilter priorityFilter,
            final UnsignedInteger notificationClassFilter) {
        this.acknowledgmentFilter = acknowledgmentFilter;
        this.enrollmentFilter = enrollmentFilter;
        this.eventStateFilter = eventStateFilter;
        this.eventTypeFilter = eventTypeFilter;
        this.priorityFilter = priorityFilter;
        this.notificationClassFilter = notificationClassFilter;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final SequenceOf<EnrollmentSummary> summaries = new SequenceOf<>();

        for (final BACnetObject bo : localDevice.getLocalObjects()) {
            final EnrollmentSummary enrollmentSummary = bo.getEnrollmentSummary(acknowledgmentFilter, enrollmentFilter,
                    eventStateFilter, eventTypeFilter, priorityFilter, notificationClassFilter);
            if (enrollmentSummary != null)
                summaries.add(enrollmentSummary);
        }

        return new GetEnrollmentSummaryAck(summaries);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, acknowledgmentFilter, 0);
        writeOptional(queue, enrollmentFilter, 1);
        writeOptional(queue, eventStateFilter, 2);
        writeOptional(queue, eventTypeFilter, 3);
        writeOptional(queue, priorityFilter, 4);
        writeOptional(queue, notificationClassFilter, 5);
    }

    GetEnrollmentSummaryRequest(final ByteQueue queue) throws BACnetException {
        acknowledgmentFilter = read(queue, AcknowledgmentFilter.class, 0);
        enrollmentFilter = readOptional(queue, RecipientProcess.class, 1);
        eventStateFilter = readOptional(queue, EventStateFilter.class, 2);
        eventTypeFilter = readOptional(queue, EventType.class, 3);
        priorityFilter = readOptional(queue, PriorityFilter.class, 4);
        notificationClassFilter = readOptional(queue, UnsignedInteger.class, 5);
    }

    public static class AcknowledgmentFilter extends Enumerated {
        public static final AcknowledgmentFilter all = new AcknowledgmentFilter(0);
        public static final AcknowledgmentFilter acked = new AcknowledgmentFilter(1);
        public static final AcknowledgmentFilter notAcked = new AcknowledgmentFilter(2);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        static {
            Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        }

        public static AcknowledgmentFilter forId(final int id) {
            AcknowledgmentFilter e = (AcknowledgmentFilter) idMap.get(id);
            if (e == null)
                e = new AcknowledgmentFilter(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static AcknowledgmentFilter forName(final String name) {
            return (AcknowledgmentFilter) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private AcknowledgmentFilter(final int value) {
            super(value);
        }

        public AcknowledgmentFilter(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }


    public static class EventStateFilter extends Enumerated {
        public static final EventStateFilter offnormal = new EventStateFilter(0);
        public static final EventStateFilter fault = new EventStateFilter(1);
        public static final EventStateFilter normal = new EventStateFilter(2);
        public static final EventStateFilter all = new EventStateFilter(3);
        public static final EventStateFilter active = new EventStateFilter(4);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        static {
            Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        }

        public static EventStateFilter forId(final int id) {
            EventStateFilter e = (EventStateFilter) idMap.get(id);
            if (e == null)
                e = new EventStateFilter(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static EventStateFilter forName(final String name) {
            return (EventStateFilter) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private EventStateFilter(final int value) {
            super(value);
        }

        public EventStateFilter(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }


    public static class PriorityFilter extends BaseType {
        private final UnsignedInteger minPriority;
        private final UnsignedInteger maxPriority;

        public PriorityFilter(final UnsignedInteger minPriority, final UnsignedInteger maxPriority) {
            this.minPriority = minPriority;
            this.maxPriority = maxPriority;
        }

        @Override
        public void write(final ByteQueue queue) {
            minPriority.write(queue, 0);
            maxPriority.write(queue, 1);
        }

        public PriorityFilter(final ByteQueue queue) throws BACnetException {
            minPriority = read(queue, UnsignedInteger.class, 0);
            maxPriority = read(queue, UnsignedInteger.class, 1);
        }

        public UnsignedInteger getMinPriority() {
            return minPriority;
        }

        public UnsignedInteger getMaxPriority() {
            return maxPriority;
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (maxPriority == null ? 0 : maxPriority.hashCode());
            result = PRIME * result + (minPriority == null ? 0 : minPriority.hashCode());
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
            final PriorityFilter other = (PriorityFilter) obj;
            if (maxPriority == null) {
                if (other.maxPriority != null)
                    return false;
            } else if (!maxPriority.equals(other.maxPriority))
                return false;
            if (minPriority == null) {
                if (other.minPriority != null)
                    return false;
            } else if (!minPriority.equals(other.minPriority))
                return false;
            return true;
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (acknowledgmentFilter == null ? 0 : acknowledgmentFilter.hashCode());
        result = PRIME * result + (enrollmentFilter == null ? 0 : enrollmentFilter.hashCode());
        result = PRIME * result + (eventStateFilter == null ? 0 : eventStateFilter.hashCode());
        result = PRIME * result + (eventTypeFilter == null ? 0 : eventTypeFilter.hashCode());
        result = PRIME * result + (notificationClassFilter == null ? 0 : notificationClassFilter.hashCode());
        result = PRIME * result + (priorityFilter == null ? 0 : priorityFilter.hashCode());
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
        final GetEnrollmentSummaryRequest other = (GetEnrollmentSummaryRequest) obj;
        if (acknowledgmentFilter == null) {
            if (other.acknowledgmentFilter != null)
                return false;
        } else if (!acknowledgmentFilter.equals(other.acknowledgmentFilter))
            return false;
        if (enrollmentFilter == null) {
            if (other.enrollmentFilter != null)
                return false;
        } else if (!enrollmentFilter.equals(other.enrollmentFilter))
            return false;
        if (eventStateFilter == null) {
            if (other.eventStateFilter != null)
                return false;
        } else if (!eventStateFilter.equals(other.eventStateFilter))
            return false;
        if (eventTypeFilter == null) {
            if (other.eventTypeFilter != null)
                return false;
        } else if (!eventTypeFilter.equals(other.eventTypeFilter))
            return false;
        if (notificationClassFilter == null) {
            if (other.notificationClassFilter != null)
                return false;
        } else if (!notificationClassFilter.equals(other.notificationClassFilter))
            return false;
        if (priorityFilter == null) {
            if (other.priorityFilter != null)
                return false;
        } else if (!priorityFilter.equals(other.priorityFilter))
            return false;
        return true;
    }
}
