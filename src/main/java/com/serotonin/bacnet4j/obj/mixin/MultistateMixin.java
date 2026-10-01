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

package com.serotonin.bacnet4j.obj.mixin;

import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.PriorityArray;
import com.serotonin.bacnet4j.type.constructed.PriorityValue;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Common behavior for the Multi-state Input, Output and Value object types, including the first stage of
 * reliability-evaluation as described in Clause 13.2.2.2. This mixin is the only writer of the Reliability
 * property for the internal faults of those object types, so that the conditions below, which can hold at the
 * same time, cannot overwrite each other's reporting.
 */
public class MultistateMixin extends AbstractMixin {
    /**
     * Writes of these properties can change the outcome of reliability-evaluation. Current_Command_Priority stands
     * in for the Priority_Array, which is modified in place by CommandableMixin rather than written, but which is
     * always accompanied by a write of Current_Command_Priority. Reliability is included so that a first stage
     * fault can be reasserted over a value written by a fault algorithm, which is the second stage.
     */
    private static final PropertyIdentifier[] RELIABILITY_TRIGGERS = { //
            PropertyIdentifier.numberOfStates, //
            PropertyIdentifier.presentValue, //
            PropertyIdentifier.outOfService, //
            PropertyIdentifier.reliability, //
            PropertyIdentifier.currentCommandPriority, //
            PropertyIdentifier.alarmValues, //
            PropertyIdentifier.faultValues};

    /**
     * Properties whose values are required to lie within 1..Number_Of_States. Empty for the Multi-state Input,
     * whose Clause 12.18.11 has no equivalent of the CONFIGURATION_ERROR requirement in 12.19.11 and 12.20.10.
     */
    private PropertyIdentifier[] rangeCheckedProperties = new PropertyIdentifier[0];

    /** Whether this mixin, rather than IntrinsicReportingMixin, evaluates the Alarm_Values/Fault_Values overlap. */
    private boolean alarmFaultOverlapChecked;

    /** The reliability this mixin last wrote, or null if it currently reports no fault. */
    private Reliability assertedReliability;

    /** Per 13.2.2.3, set by an external write of Reliability while the object is out of service. */
    private boolean reliabilityWriteBlock;

    /** Guards against unbounded recursion through this mixin's own writes of Reliability. */
    private boolean evaluating;

    public MultistateMixin(BACnetObject bo) {
        super(bo);
    }

    /**
     * Declares the properties that 12.19.11 and 12.20.10 require to be reported as a configuration error when
     * they fall outside 1..Number_Of_States. The properties need not exist yet; absent properties are skipped.
     *
     * @param properties the properties to range check
     * @return this, for chaining
     */
    public MultistateMixin withRangeCheckedProperties(PropertyIdentifier... properties) {
        rangeCheckedProperties = properties;
        return this;
    }

    /**
     * Takes over evaluation of the CONFIGURATION_ERROR required by 12.20.8 when a value is present in both
     * Alarm_Values and Fault_Values, so that it and the range conditions above have a single owner and cannot
     * overwrite each other's reporting. Object types that have no range conditions have no such overlap to
     * resolve, and leave this check with IntrinsicReportingMixin.
     *
     * @return this, for chaining
     */
    public MultistateMixin withAlarmFaultOverlapCheck() {
        alarmFaultOverlapChecked = true;
        return this;
    }

    /**
     * @return whether the first stage of reliability-evaluation currently detects an internal fault. Per
     * 13.2.2.2 such a fault takes precedence over a fault algorithm, which is the second stage.
     */
    public boolean hasInternalFault() {
        return assertedReliability != null;
    }

    @Override
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
        // The WriteProperty error table in 15.9.1.3.1 gives VALUE_OUT_OF_RANGE for a value outside the range the
        // property can take on, which is what each of the checks here detects.
        if (PropertyIdentifier.presentValue.equals(value.getPropertyIdentifier())) {
            // Per 12.18.4, 12.19.4 and 12.20.4, Present_Value is one of 'n' states and is always greater than zero.
            UnsignedInteger pv = value.getValue();
            UnsignedInteger numStates = get(PropertyIdentifier.numberOfStates);
            if (outOfRange(pv, numStates))
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        } else if (PropertyIdentifier.numberOfStates.equals(value.getPropertyIdentifier())) {
            // Per 12.18.11, 12.19.11 and 12.20.10, Number_Of_States is always greater than zero.
            UnsignedInteger numStates = value.getValue();
            if (numStates.intValue() < 1)
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        } else if (PropertyIdentifier.stateText.equals(value.getPropertyIdentifier())) {
            UnsignedInteger pin = value.getPropertyArrayIndex();
            if (pin != null && pin.intValue() == 0) {
                // Ensure that the new array size is an integer.
                if (!(value.getValue() instanceof UnsignedInteger)) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                }
                return true;
            }
        } else if (value.getPropertyIdentifier().isOneOf(rangeCheckedProperties)) {
            UnsignedInteger numStates = get(PropertyIdentifier.numberOfStates);
            if (numStates != null && valueOutOfRange(value.getValue(), numStates)) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
            }
        }
        return false;
    }

    @Override
    protected boolean writeProperty(ValueSource valueSource, PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.reliability.equals(value.getPropertyIdentifier())
                && Boolean.TRUE.equals(get(PropertyIdentifier.outOfService))) {
            // Per 13.2.2.3, an external write of Reliability while Out_Of_Service is TRUE simulates an internal
            // fault, and blocks the object from updating the Reliability property until Out_Of_Service is written
            // FALSE. Only external writes reach this hook; this mixin's own updates use writePropertyInternal.
            reliabilityWriteBlock = true;
        } else if (PropertyIdentifier.stateText.equals(value.getPropertyIdentifier())) {
            UnsignedInteger pin = value.getPropertyArrayIndex();
            if (pin != null && pin.intValue() == 0) {
                UnsignedInteger size = value.getValue();
                BACnetArray<CharacterString> states = get(PropertyIdentifier.stateText);
                var newText = copyArrayWithNewSize(states, size.intValue());
                writePropertyInternal(PropertyIdentifier.stateText, newText);
                return true;
            }
        }

        return false;
    }

    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        if (PropertyIdentifier.numberOfStates.equals(pid)) {
            if (oldValue != null && !oldValue.equals(newValue)) {
                BACnetArray<CharacterString> stateText = get(PropertyIdentifier.stateText);
                if (stateText != null) {
                    int numStates = ((UnsignedInteger) newValue).intValue();
                    BACnetArray<CharacterString> newText = copyArrayWithNewSize(stateText, numStates);
                    writePropertyInternal(PropertyIdentifier.stateText, newText);
                }
            }
        } else if (PropertyIdentifier.stateText.equals(pid)) {
            var stateText = (BACnetArray<?>) newValue;
            UnsignedInteger numberOfStates = get(PropertyIdentifier.numberOfStates);
            if (stateText.size() != numberOfStates.intValue()) {
                writePropertyInternal(PropertyIdentifier.numberOfStates, new UnsignedInteger(stateText.size()));
            }
        } else if (PropertyIdentifier.outOfService.equals(pid) && Boolean.FALSE.equals(newValue)) {
            // Per 13.2.2.3, writing Out_Of_Service to FALSE removes the block, allowing the object to update
            // the Reliability property again.
            reliabilityWriteBlock = false;
        }

        if (pid.isOneOf(RELIABILITY_TRIGGERS) || pid.isOneOf(rangeCheckedProperties)) {
            evaluateReliability();
        }
    }

    /**
     * Performs the first stage of reliability-evaluation and applies the result, without disturbing a reliability
     * that came from anywhere else.
     */
    public void evaluateReliability() {
        if (evaluating) {
            return;
        }

        Reliability derived = deriveReliability();
        Reliability current = get(PropertyIdentifier.reliability);
        // Whether Reliability still holds the value this mixin wrote, and so may be changed by it.
        boolean owned = assertedReliability != null && assertedReliability.equals(current);

        Reliability target;
        if (derived != null) {
            if (!owned && isFaultOfUnknownOrigin(current)) {
                // Reliability reports a fault that this mixin neither wrote nor can rank, which the conditions
                // evaluated here do not displace. Reliability is itself a trigger, so this evaluation is
                // repeated if that fault is cleared.
                assertedReliability = null;
                return;
            }
            target = derived;
        } else if (owned) {
            // The condition this mixin reported has cleared. The value is only restored while it is still the
            // one this mixin wrote, so that a reliability from another source is not clobbered.
            target = Reliability.noFaultDetected;
        } else {
            assertedReliability = null;
            return;
        }

        if (reliabilityWriteBlock) {
            // Per 13.2.2.3 a simulated internal fault is in effect, and the object must not update the
            // Reliability property. The evaluation is repeated when the block is removed.
            return;
        }

        assertedReliability = derived;

        if (!target.equals(current)) {
            evaluating = true;
            try {
                writePropertyInternal(PropertyIdentifier.reliability, target);
            } finally {
                evaluating = false;
            }
        }
    }

    /**
     * @param current the current value of the Reliability property
     * @return whether it reports a fault that the conditions evaluated here do not displace. Only a
     * CONFIGURATION_ERROR from another source qualifies: that value is itself required by a first stage
     * condition, such as the Alarm_Values/Fault_Values overlap of 12.18.9, and the standard does not rank one
     * first stage condition against another. Every other value either comes from a fault algorithm, which per
     * 13.2.2.2 yields to the first stage, or is not required at all, so the "as long as this situation remains"
     * requirement of 12.19.11 and 12.20.10 prevails over it.
     */
    private static boolean isFaultOfUnknownOrigin(Reliability current) {
        return Reliability.configurationError.equals(current);
    }

    /**
     * @return the reliability indicated by the first stage of reliability-evaluation, or null if it detects no
     * internal fault.
     */
    private Reliability deriveReliability() {
        // Per 12.20.8, a value present in both Alarm_Values and Fault_Values is a configuration error. Unlike
        // the range conditions below, this one has no out of service exemption, so it is evaluated first.
        if (alarmFaultOverlapChecked
                && SequenceOf.intersection(get(PropertyIdentifier.alarmValues), get(PropertyIdentifier.faultValues))) {
            return Reliability.configurationError;
        }

        // Per 12.18.11, 12.19.11 and 12.20.10, the conditions below are not reported while the object is out of
        // service.
        if (Boolean.TRUE.equals(get(PropertyIdentifier.outOfService))) {
            return null;
        }

        UnsignedInteger numberOfStates = get(PropertyIdentifier.numberOfStates);
        if (numberOfStates == null) {
            return null;
        }

        // Per 12.18.11, 12.19.11 and 12.20.10, an out of range Present_Value is reported as
        // MULTI_STATE_OUT_OF_RANGE. It is evaluated ahead of the other properties because while it holds, the
        // condition in the Reliability description of 12.1.8 is met, and it is the more specific of the two.
        if (outOfRange(get(PropertyIdentifier.presentValue), numberOfStates)) {
            return Reliability.multiStateOutOfRange;
        }

        // Per 12.19.11 and 12.20.10, any other property left out of range when Number_Of_States was reduced is
        // reported as CONFIGURATION_ERROR.
        for (PropertyIdentifier pid : rangeCheckedProperties) {
            if (propertyOutOfRange(pid, numberOfStates)) {
                return Reliability.configurationError;
            }
        }

        return null;
    }

    /**
     * @param pid            the property to check, which may be absent from the object
     * @param numberOfStates the current value of Number_Of_States
     * @return whether the property holds a state value outside 1..Number_Of_States
     */
    private boolean propertyOutOfRange(PropertyIdentifier pid, UnsignedInteger numberOfStates) {
        Encodable value = get(pid);
        if (value == null) {
            return false;
        }

        if (value instanceof PriorityArray priorityArray) {
            // Only commands that are actually in effect are considered. The command that is currently active is
            // also the Present_Value, and so is caught by the check above; the remainder are latent, and become
            // the Present_Value if the commands above them are relinquished.
            for (int i = 1; i <= priorityArray.getCount(); i++) {
                PriorityValue priorityValue = priorityArray.getBase1(i);
                if (!priorityValue.isa(Null.class) && outOfRange(priorityValue.getUnsignedValue(), numberOfStates)) {
                    return true;
                }
            }
            return false;
        }

        return valueOutOfRange(value, numberOfStates);
    }

    /**
     * @param value          a state value, or a list of them
     * @param numberOfStates the current value of Number_Of_States
     * @return whether any state value it holds lies outside 1..Number_Of_States. A value of another datatype is
     * not a state value, and is left to the generic datatype validation.
     */
    private static boolean valueOutOfRange(Encodable value, UnsignedInteger numberOfStates) {
        if (value instanceof SequenceOf<?> sequence) {
            for (Encodable element : sequence) {
                if (element instanceof UnsignedInteger unsigned && outOfRange(unsigned, numberOfStates)) {
                    return true;
                }
            }
            return false;
        }

        return value instanceof UnsignedInteger unsigned && outOfRange(unsigned, numberOfStates);
    }

    /**
     * Per 12.18.4, 12.19.4 and 12.20.4 a state value is one of 'n' states and is always greater than zero.
     */
    private static boolean outOfRange(UnsignedInteger value, UnsignedInteger numberOfStates) {
        return value != null && (value.intValue() < 1 || value.intValue() > numberOfStates.intValue());
    }

    private BACnetArray<CharacterString> copyArrayWithNewSize(BACnetArray<CharacterString> oldText, int newSize) {
        BACnetArray<CharacterString> newText = new BACnetArray<>(newSize, CharacterString.EMPTY);

        // Copy the old state values in.
        int min = Math.min(newText.getCount(), oldText.getCount());
        for (int i = 0; i < min; i++)
            newText.set(i, oldText.get(i));

        return newText;
    }
}
