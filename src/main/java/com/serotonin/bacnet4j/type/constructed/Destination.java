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
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Destination extends BaseType {
    private final DaysOfWeek validDays;
    private final Time fromTime;
    private final Time toTime;
    private final Recipient recipient;
    private final UnsignedInteger processIdentifier;
    private final Boolean issueConfirmedNotifications;
    private final EventTransitionBits transitions;

    public Destination(final DaysOfWeek validDays, final Time fromTime, final Time toTime, final Recipient recipient,
            final UnsignedInteger processIdentifier, final Boolean issueConfirmedNotifications,
            final EventTransitionBits transitions) {
        this.validDays = validDays;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.recipient = recipient;
        this.processIdentifier = processIdentifier;
        this.issueConfirmedNotifications = issueConfirmedNotifications;
        this.transitions = transitions;
    }

    public Destination(final Recipient recipient, final UnsignedInteger processIdentifier,
            final Boolean issueConfirmedNotifications, final EventTransitionBits transitions) {
        this.validDays = new DaysOfWeek(true);
        this.fromTime = new Time(0, 0, 0, 0);
        this.toTime = new Time(23, 59, 59, 99);
        this.recipient = recipient;
        this.processIdentifier = processIdentifier;
        this.issueConfirmedNotifications = issueConfirmedNotifications;
        this.transitions = transitions;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, validDays);
        write(queue, fromTime);
        write(queue, toTime);
        write(queue, recipient);
        write(queue, processIdentifier);
        write(queue, issueConfirmedNotifications);
        write(queue, transitions);
    }

    public Destination(final ByteQueue queue) throws BACnetException {
        validDays = read(queue, DaysOfWeek.class);
        fromTime = read(queue, Time.class);
        toTime = read(queue, Time.class);
        recipient = read(queue, Recipient.class);
        processIdentifier = read(queue, UnsignedInteger.class);
        issueConfirmedNotifications = read(queue, Boolean.class);
        transitions = read(queue, EventTransitionBits.class);
    }

    public boolean isSuitableForEvent(final TimeStamp timeStamp, final EventState toState) {
        // Only check date fields if the timestamp is not a sequence number.
        if (!timeStamp.isSequenceNumber()) {
            // Check if the timestamp day of week is in the valid days of week list.
            if (!validDays.contains(timeStamp.getDateTime().getDate().getDayOfWeek()))
                return false;

            // Check if the timestamp is between the from and to times.
            if (timeStamp.getDateTime().getTime().before(fromTime) || timeStamp.getDateTime().getTime().after(toTime))
                return false;
        }

        // Check if the destination is interested in this new event state.
        return transitions.contains(toState);
    }

    public DaysOfWeek getValidDays() {
        return validDays;
    }

    public Time getFromTime() {
        return fromTime;
    }

    public Time getToTime() {
        return toTime;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public UnsignedInteger getProcessIdentifier() {
        return processIdentifier;
    }

    public Boolean getIssueConfirmedNotifications() {
        return issueConfirmedNotifications;
    }

    public EventTransitionBits getTransitions() {
        return transitions;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (fromTime == null ? 0 : fromTime.hashCode());
        result = PRIME * result + (issueConfirmedNotifications == null ? 0 : issueConfirmedNotifications.hashCode());
        result = PRIME * result + (processIdentifier == null ? 0 : processIdentifier.hashCode());
        result = PRIME * result + (recipient == null ? 0 : recipient.hashCode());
        result = PRIME * result + (toTime == null ? 0 : toTime.hashCode());
        result = PRIME * result + (transitions == null ? 0 : transitions.hashCode());
        result = PRIME * result + (validDays == null ? 0 : validDays.hashCode());
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
        final Destination other = (Destination) obj;
        if (fromTime == null) {
            if (other.fromTime != null)
                return false;
        } else if (!fromTime.equals(other.fromTime))
            return false;
        if (issueConfirmedNotifications == null) {
            if (other.issueConfirmedNotifications != null)
                return false;
        } else if (!issueConfirmedNotifications.equals(other.issueConfirmedNotifications))
            return false;
        if (processIdentifier == null) {
            if (other.processIdentifier != null)
                return false;
        } else if (!processIdentifier.equals(other.processIdentifier))
            return false;
        if (recipient == null) {
            if (other.recipient != null)
                return false;
        } else if (!recipient.equals(other.recipient))
            return false;
        if (toTime == null) {
            if (other.toTime != null)
                return false;
        } else if (!toTime.equals(other.toTime))
            return false;
        if (transitions == null) {
            if (other.transitions != null)
                return false;
        } else if (!transitions.equals(other.transitions))
            return false;
        if (validDays == null) {
            if (other.validDays != null)
                return false;
        } else if (!validDays.equals(other.validDays))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Destination [validDays=" + validDays + ", fromTime=" + fromTime + ", toTime=" + toTime + ", recipient="
                + recipient + ", processIdentifier=" + processIdentifier + ", issueConfirmedNotifications="
                + issueConfirmedNotifications + ", transitions=" + transitions + "]";
    }
}
