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
import com.serotonin.bacnet4j.type.constructed.DeviceObjectReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyMode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;

/**
 * Common functionality between LifeSafetyPoint and LifeSafetyZone
 *
 * @author Matthew
 */
public class LifeSafetyMixin extends AbstractMixin {
    public LifeSafetyMixin(final BACnetObject owner) {
        super(owner);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (value.getPropertyIdentifier().equals(PropertyIdentifier.mode)) {
            final LifeSafetyMode mode = value.getValue();
            final SequenceOf<LifeSafetyMode> acceptedModes = get(PropertyIdentifier.acceptedModes);
            if (!acceptedModes.contains(mode)) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
            }
        } else if (value.getPropertyIdentifier().equals(PropertyIdentifier.memberOf)) {
            final SequenceOf<DeviceObjectReference> memberOf = value.getValue();
            for (final DeviceObjectReference ref : memberOf) {
                if (!ref.getObjectIdentifier().getObjectType().equals(ObjectType.lifeSafetyZone)) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.unsupportedObjectType);
                }
            }
        } else if (value.getPropertyIdentifier().equals(PropertyIdentifier.zoneMembers)) {
            final SequenceOf<DeviceObjectReference> zoneMembers = value.getValue();
            for (final DeviceObjectReference ref : zoneMembers) {
                if (!ref.getObjectIdentifier().getObjectType().isOneOf(ObjectType.lifeSafetyPoint,
                        ObjectType.lifeSafetyZone)) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.unsupportedObjectType);
                }
            }
        }
        return false;
    }

    @Override
    protected boolean writeProperty(final ValueSource valueSource, final PropertyValue value) {
        if (value.getPropertyIdentifier().equals(PropertyIdentifier.mode)) {
            writePropertyInternal(PropertyIdentifier.valueSource, valueSource);
        }
        return false;
    }
}
