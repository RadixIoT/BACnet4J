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
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class MultistateMixin extends AbstractMixin {
    public MultistateMixin(final BACnetObject bo) {
        super(bo);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.presentValue.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger pv = value.getValue();
            final UnsignedInteger numStates = get(PropertyIdentifier.numberOfStates);
            if (pv.intValue() < 1 || pv.intValue() > numStates.intValue())
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.inconsistentConfiguration);
        } else if (PropertyIdentifier.numberOfStates.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger numStates = value.getValue();
            if (numStates.intValue() < 1)
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.inconsistentConfiguration);
        } else if (PropertyIdentifier.stateText.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger numStates = get(PropertyIdentifier.numberOfStates);
            if (value.getPropertyArrayIndex() == null) {
                final BACnetArray<CharacterString> stateText = value.getValue();
                if (numStates.intValue() != stateText.getCount())
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.inconsistentConfiguration);
            }
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (PropertyIdentifier.numberOfStates.equals(pid)) {
            if (oldValue != null && !oldValue.equals(newValue)) {
                final BACnetArray<CharacterString> stateText = get(PropertyIdentifier.stateText);
                if (stateText != null) {
                    final int numStates = ((UnsignedInteger) newValue).intValue();
                    final BACnetArray<CharacterString> newText = new BACnetArray<>(numStates, CharacterString.EMPTY);

                    // Copy the old state values in.
                    final int min = Math.min(newText.getCount(), stateText.getCount());
                    for (int i = 0; i < min; i++)
                        newText.set(i, stateText.get(i));

                    writePropertyInternal(PropertyIdentifier.stateText, newText);
                }
            }
        }
    }
}
