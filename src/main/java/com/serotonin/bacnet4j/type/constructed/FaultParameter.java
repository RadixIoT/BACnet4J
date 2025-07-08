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

import java.util.ArrayList;
import java.util.List;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultAlgorithm;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultLifeSafetyAlgo;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultOutOfRangeAlgo;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultStateAlgo;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.FaultType;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
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
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.Utils;

public class FaultParameter extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, Null.class);
        choiceOptions.addContextual(1, FaultCharacterString.class);
        choiceOptions.addContextual(2, FaultExtended.class);
        choiceOptions.addContextual(3, FaultLifeSafety.class);
        choiceOptions.addContextual(4, FaultState.class);
        choiceOptions.addContextual(5, FaultStatusFlags.class);
        choiceOptions.addContextual(6, FaultOutOfRange.class);
        choiceOptions.addContextual(7, FaultListed.class);
    }

    private final Choice entry;

    public FaultParameter(final Null value) {
        entry = new Choice(0, value, choiceOptions);
    }

    public FaultParameter(final FaultCharacterString value) {
        entry = new Choice(1, value, choiceOptions);
    }

    public FaultParameter(final FaultExtended value) {
        entry = new Choice(2, value, choiceOptions);
    }

    public FaultParameter(final FaultLifeSafety value) {
        entry = new Choice(3, value, choiceOptions);
    }

    public FaultParameter(final FaultState value) {
        entry = new Choice(4, value, choiceOptions);
    }

    public FaultParameter(final FaultStatusFlags value) {
        entry = new Choice(5, value, choiceOptions);
    }

    public FaultParameter(final FaultOutOfRange value) {
        entry = new Choice(6, value, choiceOptions);
    }

    public FaultParameter(final FaultListed value) {
        entry = new Choice(7, value, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, entry);
    }

    public FaultParameter(final ByteQueue queue) throws BACnetException {
        entry = new Choice(queue, choiceOptions);
    }

    public Choice getEntry() {
        return entry;
    }

    public boolean isNull() {
        return entry.isa(Null.class);
    }

    public boolean isFaultCharacterString() {
        return entry.isa(FaultCharacterString.class);
    }

    public boolean isFaultExtended() {
        return entry.isa(FaultExtended.class);
    }

    public boolean isFaultLifeSafety() {
        return entry.isa(FaultLifeSafety.class);
    }

    public boolean isFaultState() {
        return entry.isa(FaultState.class);
    }

    public boolean isFaultStatusFlags() {
        return entry.isa(FaultStatusFlags.class);
    }

    public boolean isFaultOutOfRange() {
        return entry.isa(FaultOutOfRange.class);
    }

    public boolean isFaultListed() {
        return entry.isa(FaultListed.class);
    }

    public Null getNull() {
        return (Null) entry.getDatum();
    }

    public FaultCharacterString getFaultCharacterString() {
        return (FaultCharacterString) entry.getDatum();
    }

    public FaultExtended getFaultExtended() {
        return (FaultExtended) entry.getDatum();
    }

    public FaultLifeSafety getFaultLifeSafety() {
        return (FaultLifeSafety) entry.getDatum();
    }

    public FaultState getFaultState() {
        return (FaultState) entry.getDatum();
    }

    public FaultStatusFlags getFaultStatusFlags() {
        return (FaultStatusFlags) entry.getDatum();
    }

    public FaultOutOfRange getFaultOutOfRange() {
        return (FaultOutOfRange) entry.getDatum();
    }

    public FaultListed getFaultListed() {
        return (FaultListed) entry.getDatum();
    }

    public FaultType getFaultType() {
        return FaultType.forId(entry.getContextId());
    }

    @Override
    public String toString() {
        return "FaultParameter [entry=" + entry + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (entry == null ? 0 : entry.hashCode());
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
        final FaultParameter other = (FaultParameter) obj;
        if (entry == null) {
            if (other.entry != null)
                return false;
        } else if (!entry.equals(other.entry))
            return false;
        return true;
    }

    abstract public static class AbstractFaultParameter extends BaseType {
        abstract public FaultAlgorithm createFaultAlgorithm();

        /**
         * Override as required to return a reference property if the subclass has one.
         *
         * @return the property reference.
         */
        public List<DeviceObjectPropertyReference> getReferences() {
            return null;
        }
    }


    public static class FaultCharacterString extends AbstractFaultParameter {
        private final SequenceOf<CharacterString> listOfFaultValues;

        public FaultCharacterString(final SequenceOf<CharacterString> listOfFaultValues) {
            this.listOfFaultValues = listOfFaultValues;
        }

        public SequenceOf<CharacterString> getListOfFaultValues() {
            return listOfFaultValues;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, listOfFaultValues, 0);
        }

        @Override
        public String toString() {
            return "FaultCharacterString [listOfFaultValues=" + listOfFaultValues + "]";
        }

        public FaultCharacterString(final ByteQueue queue) throws BACnetException {
            listOfFaultValues = readSequenceOf(queue, CharacterString.class, 0);
        }

        @Override
        public FaultAlgorithm createFaultAlgorithm() {
            return null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (listOfFaultValues == null ? 0 : listOfFaultValues.hashCode());
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
            final FaultCharacterString other = (FaultCharacterString) obj;
            if (listOfFaultValues == null) {
                if (other.listOfFaultValues != null)
                    return false;
            } else if (!listOfFaultValues.equals(other.listOfFaultValues))
                return false;
            return true;
        }
    }


    public static class FaultExtended extends AbstractFaultParameter {
        private final Unsigned16 vendorId;
        private final UnsignedInteger extendedFaultType;
        private final SequenceOf<FaultExtendedParameter> parameters;

        public FaultExtended(final Unsigned16 vendorId, final UnsignedInteger extendedFaultType,
                final SequenceOf<FaultExtendedParameter> parameters) {
            this.vendorId = vendorId;
            this.extendedFaultType = extendedFaultType;
            this.parameters = parameters;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, vendorId, 0);
            write(queue, extendedFaultType, 1);
            write(queue, parameters, 2);
        }

        @Override
        public String toString() {
            return "FaultExtended [vendorId=" + vendorId + ", extendedFaultType=" + extendedFaultType + ", parameters="
                    + parameters + "]";
        }

        public Unsigned16 getVendorId() {
            return vendorId;
        }

        public UnsignedInteger getExtendedFaultType() {
            return extendedFaultType;
        }

        public SequenceOf<FaultExtendedParameter> getParameters() {
            return parameters;
        }

        public FaultExtended(final ByteQueue queue) throws BACnetException {
            vendorId = read(queue, Unsigned16.class, 0);
            extendedFaultType = read(queue, UnsignedInteger.class, 1);
            parameters = readSequenceOf(queue, FaultExtendedParameter.class, 2);
        }

        @Override
        public FaultAlgorithm createFaultAlgorithm() {
            return null;
        }

        @Override
        public List<DeviceObjectPropertyReference> getReferences() {
            List<DeviceObjectPropertyReference> refs = null;
            for (final FaultExtendedParameter p : parameters) {
                if (p.isReference()) {
                    if (refs == null) {
                        refs = new ArrayList<>();
                    }
                    refs.add(p.getValue());
                }
            }
            return refs;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (extendedFaultType == null ? 0 : extendedFaultType.hashCode());
            result = prime * result + (parameters == null ? 0 : parameters.hashCode());
            result = prime * result + (vendorId == null ? 0 : vendorId.hashCode());
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
            final FaultExtended other = (FaultExtended) obj;
            if (extendedFaultType == null) {
                if (other.extendedFaultType != null)
                    return false;
            } else if (!extendedFaultType.equals(other.extendedFaultType))
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

        public static class FaultExtendedParameter extends BaseType {
            private static ChoiceOptions valueChoiceOptions = new ChoiceOptions();

            static {
                valueChoiceOptions.addPrimitive(Null.class);
                valueChoiceOptions.addPrimitive(Real.class);
                valueChoiceOptions.addPrimitive(UnsignedInteger.class);
                valueChoiceOptions.addPrimitive(Boolean.class);
                valueChoiceOptions.addPrimitive(SignedInteger.class);
                valueChoiceOptions.addPrimitive(Double.class);
                valueChoiceOptions.addPrimitive(OctetString.class);
                valueChoiceOptions.addPrimitive(CharacterString.class);
                valueChoiceOptions.addPrimitive(BitString.class);
                valueChoiceOptions.addPrimitive(Enumerated.class);
                valueChoiceOptions.addPrimitive(Date.class);
                valueChoiceOptions.addPrimitive(Time.class);
                valueChoiceOptions.addPrimitive(ObjectIdentifier.class);
                valueChoiceOptions.addContextual(0, DeviceObjectPropertyReference.class);
            }

            private final Choice choice;

            public FaultExtendedParameter(final Null nullValue) {
                choice = new Choice(nullValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final Real realValue) {
                choice = new Choice(realValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final UnsignedInteger unsignedValue) {
                choice = new Choice(unsignedValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final Boolean booleanValue) {
                choice = new Choice(booleanValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final SignedInteger integerValue) {
                choice = new Choice(integerValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final Double doubleValue) {
                choice = new Choice(doubleValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final OctetString octetValue) {
                choice = new Choice(octetValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final CharacterString characterStringValue) {
                choice = new Choice(characterStringValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final BitString bitStringValue) {
                choice = new Choice(bitStringValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final Enumerated enumValue) {
                choice = new Choice(enumValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final Date dateValue) {
                choice = new Choice(dateValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final Time timeValue) {
                choice = new Choice(timeValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final ObjectIdentifier objectIdentifierValue) {
                choice = new Choice(objectIdentifierValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final DeviceObjectPropertyReference referenceValue) {
                choice = new Choice(0, referenceValue, valueChoiceOptions);
            }

            public FaultExtendedParameter(final ByteQueue queue) throws BACnetException {
                choice = readChoice(queue, valueChoiceOptions);
            }

            @Override
            public void write(final ByteQueue queue) {
                write(queue, choice);
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
            public String toString() {
                return "FaultExtendedParameter [choice=" + choice + "]";
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
                final FaultExtendedParameter other = (FaultExtendedParameter) obj;
                if (choice == null) {
                    if (other.choice != null)
                        return false;
                } else if (!choice.equals(other.choice))
                    return false;
                return true;
            }
        }
    }


    public static class FaultLifeSafety extends AbstractFaultParameter {
        private final SequenceOf<LifeSafetyState> listOfFaultValues;
        private final DeviceObjectPropertyReference modePropertyReference;

        public FaultLifeSafety(final SequenceOf<LifeSafetyState> listOfFaultValues,
                final DeviceObjectPropertyReference modePropertyReference) {
            this.listOfFaultValues = listOfFaultValues;
            this.modePropertyReference = modePropertyReference;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, listOfFaultValues, 0);
            write(queue, modePropertyReference, 1);
        }

        @Override
        public String toString() {
            return "FaultLifeSafety [listOfFaultValues=" + listOfFaultValues + ", modePropertyReference="
                    + modePropertyReference + "]";
        }

        public SequenceOf<LifeSafetyState> getListOfFaultValues() {
            return listOfFaultValues;
        }

        public DeviceObjectPropertyReference getModePropertyReference() {
            return modePropertyReference;
        }

        public FaultLifeSafety(final ByteQueue queue) throws BACnetException {
            listOfFaultValues = readSequenceOf(queue, LifeSafetyState.class, 0);
            modePropertyReference = read(queue, DeviceObjectPropertyReference.class, 1);
        }

        @Override
        public FaultAlgorithm createFaultAlgorithm() {
            return new FaultLifeSafetyAlgo();
        }

        @Override
        public List<DeviceObjectPropertyReference> getReferences() {
            return Utils.toList(modePropertyReference);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (listOfFaultValues == null ? 0 : listOfFaultValues.hashCode());
            result = prime * result + (modePropertyReference == null ? 0 : modePropertyReference.hashCode());
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
            final FaultLifeSafety other = (FaultLifeSafety) obj;
            if (listOfFaultValues == null) {
                if (other.listOfFaultValues != null)
                    return false;
            } else if (!listOfFaultValues.equals(other.listOfFaultValues))
                return false;
            if (modePropertyReference == null) {
                if (other.modePropertyReference != null)
                    return false;
            } else if (!modePropertyReference.equals(other.modePropertyReference))
                return false;
            return true;
        }
    }


    public static class FaultState extends AbstractFaultParameter {
        private final SequenceOf<PropertyStates> listOfFaultValues;

        public FaultState(final SequenceOf<PropertyStates> listOfFaultValues) {
            this.listOfFaultValues = listOfFaultValues;
        }

        public SequenceOf<PropertyStates> getListOfFaultValues() {
            return listOfFaultValues;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, listOfFaultValues, 0);
        }

        @Override
        public String toString() {
            return "FaultState [listOfFaultValues=" + listOfFaultValues + "]";
        }

        public FaultState(final ByteQueue queue) throws BACnetException {
            listOfFaultValues = readSequenceOf(queue, PropertyStates.class, 0);
        }

        @Override
        public FaultAlgorithm createFaultAlgorithm() {
            return new FaultStateAlgo();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (listOfFaultValues == null ? 0 : listOfFaultValues.hashCode());
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
            final FaultState other = (FaultState) obj;
            if (listOfFaultValues == null) {
                if (other.listOfFaultValues != null)
                    return false;
            } else if (!listOfFaultValues.equals(other.listOfFaultValues))
                return false;
            return true;
        }
    }


    public static class FaultStatusFlags extends AbstractFaultParameter {
        private final DeviceObjectPropertyReference statusFlagsReference;

        public FaultStatusFlags(final DeviceObjectPropertyReference statusFlagsReference) {
            this.statusFlagsReference = statusFlagsReference;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, statusFlagsReference, 0);
        }

        @Override
        public String toString() {
            return "FaultStatusFlags [statusFlagsReference=" + statusFlagsReference + "]";
        }

        public DeviceObjectPropertyReference getStatusFlagsReference() {
            return statusFlagsReference;
        }

        public FaultStatusFlags(final ByteQueue queue) throws BACnetException {
            statusFlagsReference = read(queue, DeviceObjectPropertyReference.class, 0);
        }

        @Override
        public FaultAlgorithm createFaultAlgorithm() {
            return null;
        }

        @Override
        public List<DeviceObjectPropertyReference> getReferences() {
            return Utils.toList(statusFlagsReference);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (statusFlagsReference == null ? 0 : statusFlagsReference.hashCode());
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
            final FaultStatusFlags other = (FaultStatusFlags) obj;
            if (statusFlagsReference == null) {
                if (other.statusFlagsReference != null)
                    return false;
            } else if (!statusFlagsReference.equals(other.statusFlagsReference))
                return false;
            return true;
        }
    }


    public static class FaultOutOfRange extends AbstractFaultParameter {
        private final FaultNormalValue minNormalValue;
        private final FaultNormalValue maxNormalValue;

        public FaultOutOfRange(final FaultNormalValue minNormalValue, final FaultNormalValue maxNormalValue) {
            this.minNormalValue = minNormalValue;
            this.maxNormalValue = maxNormalValue;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, minNormalValue, 0);
            write(queue, maxNormalValue, 1);
        }

        public FaultOutOfRange(final ByteQueue queue) throws BACnetException {
            minNormalValue = read(queue, FaultNormalValue.class, 0);
            maxNormalValue = read(queue, FaultNormalValue.class, 1);
        }

        @Override
        public String toString() {
            return "FaultOutOfRange [minNormalValue=" + minNormalValue + ", maxNormalValue=" + maxNormalValue + "]";
        }

        public FaultNormalValue getMinNormalValue() {
            return minNormalValue;
        }

        public FaultNormalValue getMaxNormalValue() {
            return maxNormalValue;
        }

        @Override
        public FaultAlgorithm createFaultAlgorithm() {
            return new FaultOutOfRangeAlgo();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (maxNormalValue == null ? 0 : maxNormalValue.hashCode());
            result = prime * result + (minNormalValue == null ? 0 : minNormalValue.hashCode());
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
            final FaultOutOfRange other = (FaultOutOfRange) obj;
            if (maxNormalValue == null) {
                if (other.maxNormalValue != null)
                    return false;
            } else if (!maxNormalValue.equals(other.maxNormalValue))
                return false;
            if (minNormalValue == null) {
                if (other.minNormalValue != null)
                    return false;
            } else if (!minNormalValue.equals(other.minNormalValue))
                return false;
            return true;
        }

        public static class FaultNormalValue extends BaseType {
            private static ChoiceOptions valueChoiceOptions = new ChoiceOptions();

            static {
                valueChoiceOptions.addPrimitive(Real.class);
                valueChoiceOptions.addPrimitive(UnsignedInteger.class);
                valueChoiceOptions.addPrimitive(Double.class);
                valueChoiceOptions.addPrimitive(SignedInteger.class);
            }

            private final Choice choice;

            public FaultNormalValue(final Real realValue) {
                choice = new Choice(realValue, valueChoiceOptions);
            }

            public FaultNormalValue(final UnsignedInteger unsignedValue) {
                choice = new Choice(unsignedValue, valueChoiceOptions);
            }

            public FaultNormalValue(final Double doubleValue) {
                choice = new Choice(doubleValue, valueChoiceOptions);
            }

            public FaultNormalValue(final SignedInteger integerValue) {
                choice = new Choice(integerValue, valueChoiceOptions);
            }

            public <T extends Encodable> T getValue() {
                return choice.getDatum();
            }

            @Override
            public void write(final ByteQueue queue) {
                write(queue, choice);
            }

            public FaultNormalValue(final ByteQueue queue) throws BACnetException {
                choice = readChoice(queue, valueChoiceOptions);
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
                final FaultNormalValue other = (FaultNormalValue) obj;
                if (choice == null) {
                    if (other.choice != null)
                        return false;
                } else if (!choice.equals(other.choice))
                    return false;
                return true;
            }

            @Override
            public String toString() {
                return "FaultNormalValue [choice=" + choice.toString() + ']';
            }
        }
    }


    public static class FaultListed extends AbstractFaultParameter {
        private final DeviceObjectPropertyReference faultListReference;

        public FaultListed(final DeviceObjectPropertyReference faultListReference) {
            this.faultListReference = faultListReference;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, faultListReference, 0);
        }

        public FaultListed(final ByteQueue queue) throws BACnetException {
            faultListReference = read(queue, DeviceObjectPropertyReference.class, 0);
        }

        public DeviceObjectPropertyReference getFaultListReference() {
            return faultListReference;
        }

        @Override
        public String toString() {
            return "FaultLifeSafety [faultListReference=" + faultListReference + "]";
        }

        @Override
        public FaultAlgorithm createFaultAlgorithm() {
            return null;
        }

        @Override
        public List<DeviceObjectPropertyReference> getReferences() {
            return Utils.toList(faultListReference);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (faultListReference == null ? 0 : faultListReference.hashCode());
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
            final FaultListed other = (FaultListed) obj;
            if (faultListReference == null) {
                if (other.faultListReference != null)
                    return false;
            } else if (!faultListReference.equals(other.faultListReference))
                return false;
            return true;
        }
    }
}
