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
import com.serotonin.bacnet4j.type.AmbiguousValue;
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

public class LogData extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, LogStatus.class);
        choiceOptions.addContextualSequence(1, LogDataElement.class);
        choiceOptions.addContextual(2, Real.class);
    }

    private final Choice choice;

    public LogData(final LogStatus datum) {
        choice = new Choice(0, datum, choiceOptions);
    }

    public LogData(final SequenceOf<LogDataElement> datum) {
        choice = new Choice(1, datum, choiceOptions);
    }

    public LogData(final Real datum) {
        choice = new Choice(2, datum, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public LogData(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    public boolean isLogStatus() {
        return choice.getContextId() == 0;
    }

    public LogStatus getLogStatus() {
        return choice.getDatum();
    }

    public boolean isLogData() {
        return choice.getContextId() == 1;
    }

    public SequenceOf<LogDataElement> getData() {
        return choice.getDatum();
    }

    public boolean isTimeChange() {
        return choice.getContextId() == 2;
    }

    public Real getTimeChange() {
        return choice.getDatum();
    }

    public <T extends Encodable> T getDatum() {
        return choice.getDatum();
    }

    @Override
    public String toString() {
        return "LogData [choice=" + choice + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (choice == null ? 0 : choice.hashCode());
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
        final LogData other = (LogData) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    public static class LogDataElement extends BaseType {
        /**
         * Method to ensure that the log record gets created with the correct context id.
         */
        public static LogDataElement createFromMonitoredValue(final Encodable value) {
            if (value instanceof Boolean)
                return new LogDataElement((Boolean) value);
            if (value instanceof Real)
                return new LogDataElement((Real) value);
            if (value instanceof Enumerated)
                return new LogDataElement((Enumerated) value);
            if (value instanceof UnsignedInteger)
                return new LogDataElement((UnsignedInteger) value);
            if (value instanceof SignedInteger)
                return new LogDataElement((SignedInteger) value);
            if (value instanceof BitString)
                return new LogDataElement((BitString) value);
            if (value instanceof Null)
                return new LogDataElement((Null) value);
            if (value instanceof ErrorClassAndCode)
                return new LogDataElement((ErrorClassAndCode) value);
            return new LogDataElement(value);
        }

        private static ChoiceOptions choiceOptions = new ChoiceOptions();

        static {
            choiceOptions.addContextual(0, Boolean.class);
            choiceOptions.addContextual(1, Real.class);
            choiceOptions.addContextual(2, Enumerated.class);
            choiceOptions.addContextual(3, UnsignedInteger.class);
            choiceOptions.addContextual(4, SignedInteger.class);
            choiceOptions.addContextual(5, BitString.class);
            choiceOptions.addContextual(6, Null.class);
            choiceOptions.addContextual(7, ErrorClassAndCode.class);
            choiceOptions.addContextual(8, AmbiguousValue.class);
        }

        private final Choice choice;

        public LogDataElement(final Boolean datum) {
            choice = new Choice(0, datum, choiceOptions);
        }

        public LogDataElement(final Real datum) {
            choice = new Choice(1, datum, choiceOptions);
        }

        public LogDataElement(final Enumerated datum) {
            choice = new Choice(2, datum, choiceOptions);
        }

        public LogDataElement(final UnsignedInteger datum) {
            choice = new Choice(3, datum, choiceOptions);
        }

        public LogDataElement(final SignedInteger datum) {
            choice = new Choice(4, datum, choiceOptions);
        }

        public LogDataElement(final BitString datum) {
            choice = new Choice(5, datum, choiceOptions);
        }

        public LogDataElement(final Null datum) {
            choice = new Choice(6, datum, choiceOptions);
        }

        public LogDataElement(final ErrorClassAndCode datum) {
            choice = new Choice(7, datum, choiceOptions);
        }

        public LogDataElement(final Encodable datum) {
            choice = new Choice(8, datum, choiceOptions);
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, choice);
        }

        public LogDataElement(final ByteQueue queue) throws BACnetException {
            choice = readChoice(queue, choiceOptions);
        }

        public boolean isBoolean() {
            return choice.getContextId() == 0;
        }

        public boolean isReal() {
            return choice.getContextId() == 1;
        }

        public boolean isEnumerated() {
            return choice.getContextId() == 2;
        }

        public boolean isUnsignedInteger() {
            return choice.getContextId() == 3;
        }

        public boolean isSignedInteger() {
            return choice.getContextId() == 4;
        }

        public boolean isBitString() {
            return choice.getContextId() == 5;
        }

        public boolean isNull() {
            return choice.getContextId() == 6;
        }

        public boolean isBACnetError() {
            return choice.getContextId() == 7;
        }

        public boolean isAny() {
            return choice.getContextId() == 8;
        }

        public <T extends Encodable> T getDatum() {
            return choice.getDatum();
        }

        @Override
        public String toString() {
            return "LogDataElement [choice=" + choice + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (choice == null ? 0 : choice.hashCode());
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
            final LogDataElement other = (LogDataElement) obj;
            if (choice == null) {
                if (other.choice != null)
                    return false;
            } else if (!choice.equals(other.choice))
                return false;
            return true;
        }
    }
}
