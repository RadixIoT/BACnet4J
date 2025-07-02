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

package com.serotonin.bacnet4j.type.eventParameter;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Double;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Extended extends AbstractEventParameter {
    public static final byte TYPE_ID = 9;

    private final UnsignedInteger vendorId;
    private final UnsignedInteger extendedEventType;
    private final SequenceOf<Parameter> parameters;

    public Extended(final UnsignedInteger vendorId, final UnsignedInteger extendedEventType,
            final SequenceOf<Parameter> parameters) {
        this.vendorId = vendorId;
        this.extendedEventType = extendedEventType;
        this.parameters = parameters;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, vendorId, 0);
        write(queue, extendedEventType, 1);
        write(queue, parameters, 2);
    }

    public Extended(final ByteQueue queue) throws BACnetException {
        vendorId = read(queue, UnsignedInteger.class, 0);
        extendedEventType = read(queue, UnsignedInteger.class, 1);
        parameters = readSequenceOf(queue, Parameter.class, 2);
    }

    public UnsignedInteger getVendorId() {
        return vendorId;
    }

    public UnsignedInteger getExtendedEventType() {
        return extendedEventType;
    }

    public SequenceOf<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return null;
    }

    public static class Parameter extends BaseType {
        private static ChoiceOptions choiceOptions = new ChoiceOptions();

        static {
            choiceOptions.addPrimitive(Null.class);
            choiceOptions.addPrimitive(Real.class);
            choiceOptions.addPrimitive(UnsignedInteger.class);
            choiceOptions.addPrimitive(Boolean.class);
            choiceOptions.addPrimitive(SignedInteger.class);
            choiceOptions.addPrimitive(Double.class);
            choiceOptions.addPrimitive(OctetString.class);
            choiceOptions.addPrimitive(CharacterString.class);
            choiceOptions.addPrimitive(BitString.class);
            choiceOptions.addPrimitive(Enumerated.class);
            choiceOptions.addPrimitive(Date.class);
            choiceOptions.addPrimitive(Time.class);
            choiceOptions.addPrimitive(ObjectIdentifier.class);
            choiceOptions.addContextual(0, DeviceObjectPropertyReference.class);
        }

        private final Choice choice;

        public Parameter(final Null nullValue) {
            this.choice = new Choice(nullValue, choiceOptions);
        }

        public Parameter(final Real realValue) {
            this.choice = new Choice(realValue, choiceOptions);
        }

        public Parameter(final UnsignedInteger unsignedValue) {
            this.choice = new Choice(unsignedValue, choiceOptions);
        }

        public Parameter(final Boolean booleanValue) {
            this.choice = new Choice(booleanValue, choiceOptions);
        }

        public Parameter(final SignedInteger signedValue) {
            this.choice = new Choice(signedValue, choiceOptions);
        }

        public Parameter(final Double doubleValue) {
            this.choice = new Choice(doubleValue, choiceOptions);
        }

        public Parameter(final OctetString octetStringValue) {
            this.choice = new Choice(octetStringValue, choiceOptions);
        }

        public Parameter(final CharacterString characterStringValue) {
            this.choice = new Choice(characterStringValue, choiceOptions);
        }

        public Parameter(final BitString bitStringValue) {
            this.choice = new Choice(bitStringValue, choiceOptions);
        }

        public Parameter(final Enumerated enumeratedValue) {
            this.choice = new Choice(enumeratedValue, choiceOptions);
        }

        public Parameter(final Date dateValue) {
            this.choice = new Choice(dateValue, choiceOptions);
        }

        public Parameter(final Time timeValue) {
            this.choice = new Choice(timeValue, choiceOptions);
        }

        public Parameter(final ObjectIdentifier oidValue) {
            this.choice = new Choice(oidValue, choiceOptions);
        }

        public Parameter(final DeviceObjectPropertyReference reference) {
            this.choice = new Choice(0, reference, choiceOptions);
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, choice);
        }

        public Parameter(final ByteQueue queue) throws BACnetException {
            choice = readChoice(queue, choiceOptions);
        }

        public <T extends Encodable> T getValue() {
            return choice.getDatum();
        }

        public boolean isNull() {
            return choice.getDatum() instanceof Null;
        }

        public boolean isReal() {
            return choice.getDatum() instanceof Real;
        }

        public boolean isUnsigned() {
            return choice.getDatum() instanceof UnsignedInteger;
        }

        public boolean isBoolean() {
            return choice.getDatum() instanceof Boolean;
        }

        public boolean isInteger() {
            return choice.getDatum() instanceof SignedInteger;
        }

        public boolean isDouble() {
            return choice.getDatum() instanceof Double;
        }

        public boolean isOctet() {
            return choice.getDatum() instanceof OctetString;
        }

        public boolean isCharacterString() {
            return choice.getDatum() instanceof CharacterString;
        }

        public boolean isBitString() {
            return choice.getDatum() instanceof BitString;
        }

        public boolean isEnum() {
            return choice.getDatum() instanceof Enumerated;
        }

        public boolean isDate() {
            return choice.getDatum() instanceof Date;
        }

        public boolean isTime() {
            return choice.getDatum() instanceof Time;
        }

        public boolean isObjectIdentifier() {
            return choice.getDatum() instanceof ObjectIdentifier;
        }

        public boolean isReference() {
            return choice.getDatum() instanceof DeviceObjectPropertyReference;
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
            final Parameter other = (Parameter) obj;
            if (choice == null) {
                if (other.choice != null)
                    return false;
            } else if (!choice.equals(other.choice))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Parameter[ choice=" + choice + ']';
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (extendedEventType == null ? 0 : extendedEventType.hashCode());
        result = PRIME * result + (parameters == null ? 0 : parameters.hashCode());
        result = PRIME * result + (vendorId == null ? 0 : vendorId.hashCode());
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
        final Extended other = (Extended) obj;
        if (extendedEventType == null) {
            if (other.extendedEventType != null)
                return false;
        } else if (!extendedEventType.equals(other.extendedEventType))
            return false;
        if (parameters == null) {
            if (other.parameters != null)
                return false;
        } else if (!parameters.equals(other.parameters))
            return false;
        if (vendorId == null) {
            if (other.vendorId != null)
                return false;
        } else if (!vendorId.equals(other.vendorId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Extended[ vendorId=" + vendorId + ", extendedEventType=" + extendedEventType + ", parameters=" + parameters + ']';
    }
}
