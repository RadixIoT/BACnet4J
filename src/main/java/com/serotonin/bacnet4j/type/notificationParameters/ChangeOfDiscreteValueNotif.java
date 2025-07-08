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

package com.serotonin.bacnet4j.type.notificationParameters;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfDiscreteValueNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 21;

    private final NewValue newValue;
    private final StatusFlags statusFlags;

    public ChangeOfDiscreteValueNotif(final NewValue newValue, final StatusFlags statusFlags) {
        this.newValue = newValue;
        this.statusFlags = statusFlags;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, newValue, 0);
        write(queue, statusFlags, 1);
    }

    public ChangeOfDiscreteValueNotif(final ByteQueue queue) throws BACnetException {
        newValue = read(queue, NewValue.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
    }

    public NewValue getNewValue() {
        return newValue;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    @Override
    public String toString() {
        return "ChangeOfDiscreteValueNotif[ newValue=" + newValue + ", statusFlags=" + statusFlags + ']';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (newValue == null ? 0 : newValue.hashCode());
        result = prime * result + (statusFlags == null ? 0 : statusFlags.hashCode());
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
        final ChangeOfDiscreteValueNotif other = (ChangeOfDiscreteValueNotif) obj;
        if (newValue == null) {
            if (other.newValue != null)
                return false;
        } else if (!newValue.equals(other.newValue))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }

    public static class NewValue extends BaseType {
        private static ChoiceOptions choiceOptions = new ChoiceOptions();

        static {
            choiceOptions.addPrimitive(Boolean.class);
            choiceOptions.addPrimitive(UnsignedInteger.class);
            choiceOptions.addPrimitive(SignedInteger.class);
            choiceOptions.addPrimitive(Enumerated.class);
            choiceOptions.addPrimitive(CharacterString.class);
            choiceOptions.addPrimitive(OctetString.class);
            choiceOptions.addPrimitive(Date.class);
            choiceOptions.addPrimitive(Time.class);
            choiceOptions.addPrimitive(ObjectIdentifier.class);
            choiceOptions.addContextual(0, DateTime.class);
        }

        private final Choice choice;

        public NewValue(final Boolean datum) {
            choice = new Choice(datum, choiceOptions);
        }

        public NewValue(final UnsignedInteger datum) {
            choice = new Choice(datum, choiceOptions);
        }

        public NewValue(final SignedInteger datum) {
            choice = new Choice(datum, choiceOptions);
        }

        public NewValue(final Enumerated datum) {
            choice = new Choice(datum, choiceOptions);
        }

        public NewValue(final CharacterString datum) {
            choice = new Choice(datum, choiceOptions);
        }

        public NewValue(final OctetString datum) {
            choice = new Choice(datum, choiceOptions);
        }

        public NewValue(final Date datum) {
            choice = new Choice(datum, choiceOptions);
        }

        public NewValue(final Time datum) {
            choice = new Choice(datum, choiceOptions);
        }

        public NewValue(final ObjectIdentifier datum) {
            choice = new Choice(datum, choiceOptions);
        }

        public NewValue(final DateTime dateTime) {
            choice = new Choice(0, dateTime, choiceOptions);
        }

        public Boolean getBoolean() {
            return choice.getDatum();
        }

        public UnsignedInteger getUnsignedInteger() {
            return choice.getDatum();
        }

        public SignedInteger getSignedInteger() {
            return choice.getDatum();
        }

        public Enumerated getEnumerated() {
            return choice.getDatum();
        }

        public CharacterString getCharacterString() {
            return choice.getDatum();
        }

        public OctetString getOctetString() {
            return choice.getDatum();
        }

        public Date getDate() {
            return choice.getDatum();
        }

        public Time getTime() {
            return choice.getDatum();
        }

        public ObjectIdentifier getObjectIdentifier() {
            return choice.getDatum();
        }

        public DateTime getDateTime() {
            return choice.getDatum();
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, choice);
        }

        public NewValue(final ByteQueue queue) throws BACnetException {
            choice = readChoice(queue, choiceOptions);
        }

        @Override
        public String toString() {
            return "NewValue[ choice=" + choice + ']';
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
            final NewValue other = (NewValue) obj;
            if (choice == null) {
                if (other.choice != null)
                    return false;
            } else if (!choice.equals(other.choice))
                return false;
            return true;
        }
    }
}
