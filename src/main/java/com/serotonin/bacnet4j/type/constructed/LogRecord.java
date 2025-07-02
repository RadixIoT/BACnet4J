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

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.logBuffer.ILogRecord;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class LogRecord extends BaseType implements ILogRecord {
    /**
     * Method to ensure that the log record gets created with the correct context id.
     */
    public static LogRecord createFromMonitoredValue(final DateTime timestamp, final Encodable value,
            final StatusFlags statusFlags) {
        if (value instanceof Boolean)
            return new LogRecord(timestamp, (Boolean) value, statusFlags);
        if (value instanceof Real)
            return new LogRecord(timestamp, false, (Real) value, statusFlags);
        if (value instanceof Enumerated)
            return new LogRecord(timestamp, (Enumerated) value, statusFlags);
        if (value instanceof UnsignedInteger)
            return new LogRecord(timestamp, (UnsignedInteger) value, statusFlags);
        if (value instanceof SignedInteger)
            return new LogRecord(timestamp, (SignedInteger) value, statusFlags);
        if (value instanceof BitString)
            return new LogRecord(timestamp, (BitString) value, statusFlags);
        if (value instanceof Null)
            return new LogRecord(timestamp, (Null) value, statusFlags);
        if (value instanceof ErrorClassAndCode)
            return new LogRecord(timestamp, (ErrorClassAndCode) value, statusFlags);
        return new LogRecord(timestamp, value, statusFlags);
    }

    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, LogStatus.class);
        choiceOptions.addContextual(1, Boolean.class);
        choiceOptions.addContextual(2, Real.class);
        choiceOptions.addContextual(3, Enumerated.class);
        choiceOptions.addContextual(4, UnsignedInteger.class);
        choiceOptions.addContextual(5, SignedInteger.class);
        choiceOptions.addContextual(6, BitString.class);
        choiceOptions.addContextual(7, Null.class);
        choiceOptions.addContextual(8, ErrorClassAndCode.class);
        choiceOptions.addContextual(9, Real.class);
        choiceOptions.addContextual(10, Encodable.class);
    }

    private final DateTime timestamp;
    private final Choice choice;
    private final StatusFlags statusFlags;

    private long sequenceNumber;

    private LogRecord(final DateTime timestamp, final Choice choice, final StatusFlags statusFlags) {
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(choice);

        this.timestamp = timestamp;
        this.choice = choice;
        this.statusFlags = statusFlags;
    }

    public LogRecord(final DateTime timestamp, final LogStatus datum, final StatusFlags statusFlags) {
        this(timestamp, new Choice(0, datum, choiceOptions), statusFlags);
    }

    public LogRecord(final DateTime timestamp, final Boolean datum, final StatusFlags statusFlags) {
        this(timestamp, new Choice(1, datum, choiceOptions), statusFlags);
    }

    public LogRecord(final DateTime timestamp, final boolean timeChange, final Real datum,
            final StatusFlags statusFlags) {
        this(timestamp, new Choice(timeChange ? 9 : 2, datum, choiceOptions), statusFlags);
    }

    public LogRecord(final DateTime timestamp, final Enumerated datum, final StatusFlags statusFlags) {
        this(timestamp, new Choice(3, datum, choiceOptions), statusFlags);
    }

    public LogRecord(final DateTime timestamp, final UnsignedInteger datum, final StatusFlags statusFlags) {
        this(timestamp, new Choice(4, datum, choiceOptions), statusFlags);
    }

    public LogRecord(final DateTime timestamp, final SignedInteger datum, final StatusFlags statusFlags) {
        this(timestamp, new Choice(5, datum, choiceOptions), statusFlags);
    }

    public LogRecord(final DateTime timestamp, final BitString datum, final StatusFlags statusFlags) {
        this(timestamp, new Choice(6, datum, choiceOptions), statusFlags);
    }

    public LogRecord(final DateTime timestamp, final Null datum, final StatusFlags statusFlags) {
        this(timestamp, new Choice(7, datum, choiceOptions), statusFlags);
    }

    public LogRecord(final DateTime timestamp, final ErrorClassAndCode datum, final StatusFlags statusFlags) {
        this(timestamp, new Choice(8, datum, choiceOptions), statusFlags);
    }

    public LogRecord(final DateTime timestamp, final Encodable datum, final StatusFlags statusFlags) {
        this(timestamp, new Choice(10, datum, choiceOptions), statusFlags);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, choice, 1);
        writeOptional(queue, statusFlags, 2);
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public boolean isLogStatus() {
        return choice.getContextId() == 0;
    }

    public boolean isBoolean() {
        return choice.getContextId() == 1;
    }

    public boolean isReal() {
        return choice.getContextId() == 2;
    }

    public boolean isEnumerated() {
        return choice.getContextId() == 3;
    }

    public boolean isUnsignedInteger() {
        return choice.getContextId() == 4;
    }

    public boolean isSignedInteger() {
        return choice.getContextId() == 5;
    }

    public boolean isBitString() {
        return choice.getContextId() == 6;
    }

    public boolean isNull() {
        return choice.getContextId() == 7;
    }

    public boolean isBACnetError() {
        return choice.getContextId() == 8;
    }

    public boolean isTimeChange() {
        return choice.getContextId() == 9;
    }

    public boolean isAny() {
        return choice.getContextId() == 10;
    }

    public <T extends Encodable> T getChoice() {
        return choice.getDatum();
    }

    public LogRecord(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        choice = new Choice(queue, choiceOptions, 1);
        statusFlags = readOptional(queue, StatusFlags.class, 2);
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
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (choice == null ? 0 : choice.hashCode());
        result = PRIME * result + (statusFlags == null ? 0 : statusFlags.hashCode());
        result = PRIME * result + (timestamp == null ? 0 : timestamp.hashCode());
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
        final LogRecord other = (LogRecord) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
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
        return "LogRecord [seq=" + sequenceNumber + ", timestamp=" + timestamp + ", choice=" + choice + ", statusFlags="
                + statusFlags + "]";
    }
}
