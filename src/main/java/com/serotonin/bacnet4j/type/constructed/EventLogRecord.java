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
import com.serotonin.bacnet4j.obj.logBuffer.ILogRecord;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * @author Matthew Lohbihler
 */
public class EventLogRecord extends BaseType implements ILogRecord {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, LogStatus.class);
        choiceOptions.addContextual(1, ConfirmedEventNotificationRequest.class);
        choiceOptions.addContextual(2, Real.class);
    }

    private final DateTime timestamp;
    private final Choice choice;

    private long sequenceNumber;

    public EventLogRecord(final DateTime timestamp, final LogStatus logStatus) {
        this.timestamp = timestamp;
        choice = new Choice(0, logStatus, choiceOptions);
    }

    public EventLogRecord(final DateTime timestamp, final ConfirmedEventNotificationRequest notification) {
        this.timestamp = timestamp;
        choice = new Choice(1, notification, choiceOptions);
    }

    public EventLogRecord(final DateTime timestamp, final Real timeChange) {
        this.timestamp = timestamp;
        choice = new Choice(2, timeChange, choiceOptions);
    }

    public EventLogRecord(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        choice = new Choice(queue, choiceOptions, 1);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, choice, 1);
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    public boolean isLogStatus() {
        return choice.getContextId() == 0;
    }

    public LogStatus getLogStatus() {
        return choice.getDatum();
    }

    public boolean isNotification() {
        return choice.getContextId() == 1;
    }

    public ConfirmedEventNotificationRequest getNotification() {
        return choice.getDatum();
    }

    public boolean isTimeChange() {
        return choice.getContextId() == 2;
    }

    public Real getTimeChange() {
        return choice.getDatum();
    }

    public Choice getChoice() {
        return choice;
    }

    @Override
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(final long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (choice == null ? 0 : choice.hashCode());
        result = prime * result + (timestamp == null ? 0 : timestamp.hashCode());
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
        final EventLogRecord other = (EventLogRecord) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        if (timestamp == null) {
            if (other.timestamp != null)
                return false;
        } else if (!timestamp.equals(other.timestamp))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "EventLogRecord [timestamp=" + timestamp + ", choice=" + choice + ", sequenceNumber=" + sequenceNumber + ']';
    }
}
