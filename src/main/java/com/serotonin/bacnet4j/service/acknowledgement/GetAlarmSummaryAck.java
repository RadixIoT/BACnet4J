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
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class GetAlarmSummaryAck extends AcknowledgementService {
    public static final byte TYPE_ID = 3;

    private final SequenceOf<AlarmSummary> values;

    public GetAlarmSummaryAck(final SequenceOf<AlarmSummary> values) {
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

    GetAlarmSummaryAck(final ByteQueue queue) throws BACnetException {
        values = readSequenceOf(queue, AlarmSummary.class);
    }

    public SequenceOf<AlarmSummary> getValues() {
        return values;
    }

    public static class AlarmSummary extends BaseType {
        private final ObjectIdentifier objectIdentifier;
        private final EventState alarmState;
        private final EventTransitionBits acknowledgedTransitions;

        public AlarmSummary(final ObjectIdentifier objectIdentifier, final EventState alarmState,
                final EventTransitionBits acknowledgedTransitions) {
            this.objectIdentifier = objectIdentifier;
            this.alarmState = alarmState;
            this.acknowledgedTransitions = acknowledgedTransitions;
        }

        @Override
        public void write(final ByteQueue queue) {
            objectIdentifier.write(queue);
            alarmState.write(queue);
            acknowledgedTransitions.write(queue);
        }

        public AlarmSummary(final ByteQueue queue) throws BACnetException {
            objectIdentifier = read(queue, ObjectIdentifier.class);
            alarmState = read(queue, EventState.class);
            acknowledgedTransitions = read(queue, EventTransitionBits.class);
        }

        public ObjectIdentifier getObjectIdentifier() {
            return objectIdentifier;
        }

        public EventState getAlarmState() {
            return alarmState;
        }

        public EventTransitionBits getAcknowledgedTransitions() {
            return acknowledgedTransitions;
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (acknowledgedTransitions == null ? 0 : acknowledgedTransitions.hashCode());
            result = PRIME * result + (alarmState == null ? 0 : alarmState.hashCode());
            result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
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
            final AlarmSummary other = (AlarmSummary) obj;
            if (acknowledgedTransitions == null) {
                if (other.acknowledgedTransitions != null)
                    return false;
            } else if (!acknowledgedTransitions.equals(other.acknowledgedTransitions))
                return false;
            if (alarmState == null) {
                if (other.alarmState != null)
                    return false;
            } else if (!alarmState.equals(other.alarmState))
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
        final GetAlarmSummaryAck other = (GetAlarmSummaryAck) obj;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }
}
