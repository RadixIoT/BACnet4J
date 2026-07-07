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
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class MultistateMixin extends AbstractMixin {
    public MultistateMixin(BACnetObject bo) {
        super(bo);
    }

    @Override
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
        if (PropertyIdentifier.presentValue.equals(value.getPropertyIdentifier())) {
            UnsignedInteger pv = value.getValue();
            UnsignedInteger numStates = get(PropertyIdentifier.numberOfStates);
            if (pv.intValue() < 1 || pv.intValue() > numStates.intValue())
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.inconsistentConfiguration);
        } else if (PropertyIdentifier.numberOfStates.equals(value.getPropertyIdentifier())) {
            UnsignedInteger numStates = value.getValue();
            if (numStates.intValue() < 1)
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.inconsistentConfiguration);
        } else if (PropertyIdentifier.stateText.equals(value.getPropertyIdentifier())) {
            UnsignedInteger pin = value.getPropertyArrayIndex();
            if (pin != null && pin.intValue() == 0) {
                // Ensure that the new array size is an integer.
                if (!(value.getValue() instanceof UnsignedInteger)) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean writeProperty(ValueSource valueSource, PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.stateText.equals(value.getPropertyIdentifier())) {
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
            updateMultiStateOutOfRangeReliability();
        } else if (PropertyIdentifier.stateText.equals(pid)) {
            var stateText = (BACnetArray<?>) newValue;
            UnsignedInteger numberOfStates = get(PropertyIdentifier.numberOfStates);
            if (stateText.size() != numberOfStates.intValue()) {
                writePropertyInternal(PropertyIdentifier.numberOfStates, new UnsignedInteger(stateText.size()));
            }
        } else if (PropertyIdentifier.presentValue.equals(pid) || PropertyIdentifier.outOfService.equals(pid)) {
            updateMultiStateOutOfRangeReliability();
        }
    }

    /**
     * Per addendum 135-2016br-5: when Number_Of_States becomes less than Present_Value, Reliability shall be
     * MULTI_STATE_OUT_OF_RANGE for as long as the situation remains, unless the object is out of service. When the
     * situation resolves (Number_Of_States is raised again, or Present_Value is brought back in range) the reliability
     * is restored to noFaultDetected. A pre-existing CONFIGURATION_ERROR shall not be downgraded to
     * MULTI_STATE_OUT_OF_RANGE.
     */
    private void updateMultiStateOutOfRangeReliability() {
        Boolean oos = get(PropertyIdentifier.outOfService);
        if (Boolean.TRUE.equals(oos)) {
            return;
        }
        Reliability current = get(PropertyIdentifier.reliability);
        if (Reliability.configurationError.equals(current)) {
            return;
        }
        UnsignedInteger pv = get(PropertyIdentifier.presentValue);
        UnsignedInteger nos = get(PropertyIdentifier.numberOfStates);
        if (pv == null || nos == null) {
            return; // Should this be a configuration error?
        }
        boolean outOfRange = pv.intValue() > nos.intValue();
        if (outOfRange && !Reliability.multiStateOutOfRange.equals(current)) {
            writePropertyInternal(PropertyIdentifier.reliability, Reliability.multiStateOutOfRange);
        } else if (!outOfRange && Reliability.multiStateOutOfRange.equals(current)) {
            writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        }
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
