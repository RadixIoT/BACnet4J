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

import java.util.List;
import java.util.Map;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyReferences;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyValues;
import com.serotonin.bacnet4j.util.PropertyReferences;
import com.serotonin.bacnet4j.util.PropertyUtils;

public class PollingDelegate {
    private final LocalDevice localDevice;
    private final PropertyReferences localReferences;
    private final DeviceObjectPropertyReferences remoteReferences;

    public PollingDelegate(final LocalDevice localDevice, final DeviceObjectPropertyReferences polledReferences) {
        this.localDevice = localDevice;

        // Split the given references into local and remote.
        localReferences = new PropertyReferences();
        remoteReferences = new DeviceObjectPropertyReferences();
        for (final Map.Entry<Integer, PropertyReferences> deviceRefs : polledReferences.getProperties().entrySet()) {
            if (deviceRefs.getKey() == localDevice.getInstanceNumber()) {
                // Local references
                localReferences.add(deviceRefs.getValue());
            } else {
                // Remote references
                remoteReferences.add(deviceRefs.getKey(), deviceRefs.getValue());
            }
        }
    }

    public DeviceObjectPropertyReferences getRemoteReferences() {
        return remoteReferences;
    }

    public DeviceObjectPropertyValues doPoll() {
        // Get the remote properties first. If there are no remote properties this will return an empty values object.
        final DeviceObjectPropertyValues result = PropertyUtils.readProperties(localDevice, remoteReferences, null);

        for (final Map.Entry<ObjectIdentifier, List<PropertyReference>> oidRefs : localReferences.getProperties()
                .entrySet()) {
            final BACnetObject localObject = localDevice.getObject(oidRefs.getKey());
            if (localObject == null) {
                // Add errors for each of the references
                final ErrorClassAndCode ecac = new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject);
                for (final PropertyReference ref : oidRefs.getValue()) {
                    result.add(localDevice.getInstanceNumber(), oidRefs.getKey(), ref.getPropertyIdentifier(),
                            ref.getPropertyArrayIndex(), ecac);
                }
            } else {
                for (final PropertyReference ref : oidRefs.getValue()) {
                    Encodable value;
                    try {
                        value = localObject.readProperty(ref.getPropertyIdentifier(), ref.getPropertyArrayIndex());
                    } catch (final BACnetServiceException e) {
                        value = new ErrorClassAndCode(e);
                    }
                    result.add(localDevice.getInstanceNumber(), oidRefs.getKey(), ref.getPropertyIdentifier(),
                            ref.getPropertyArrayIndex(), value);
                }
            }
        }

        return result;
    }
}
