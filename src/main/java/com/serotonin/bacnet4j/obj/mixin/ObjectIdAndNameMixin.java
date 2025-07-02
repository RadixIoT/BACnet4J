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

import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class ObjectIdAndNameMixin extends AbstractMixin {
    public ObjectIdAndNameMixin(final BACnetObject owner) {
        super(owner);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (value.getPropertyIdentifier().equals(PropertyIdentifier.objectIdentifier)) {
            final ObjectIdentifier objectIdentifier = value.getValue();

            // Only validate if the id is changing.
            if (!objectIdentifier.equals(getId())) {
                // The object type of the identifier cannot change.
                if (!objectIdentifier.getObjectType().equals(getId().getObjectType())) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidValueInThisState);
                }

                // The instance number must be unique among objects of the same type.
                if (getLocalDevice().getObject(objectIdentifier) != null) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.duplicateObjectId);
                }
            }
        } else if (value.getPropertyIdentifier().equals(PropertyIdentifier.objectName)) {
            final CharacterString objectName = value.getValue();

            // Only validate if the name is changing.
            if (!objectName.equals(get(PropertyIdentifier.objectName))) {
                if (getId().getObjectType().equals(ObjectType.device)) {
                    // Devices are supposed to have names that are unique across the network. Without going out in
                    // search of the name, the best we can do is to look at the remote devices we know about.
                    for (final RemoteDevice d : getLocalDevice().getRemoteDevices()) {
                        if (objectName.getValue().equals(d.getName())) {
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.duplicateName);
                        }
                    }
                } else {
                    // The instance name must be unique among objects of the same type.
                    if (getLocalDevice().getObject(objectName.getValue()) != null) {
                        throw new BACnetServiceException(ErrorClass.property, ErrorCode.duplicateName);
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (pid.isOneOf(PropertyIdentifier.objectIdentifier, PropertyIdentifier.objectName)) {
            getLocalDevice().incrementDatabaseRevision();
        }
    }
}
