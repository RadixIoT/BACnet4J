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

package com.serotonin.bacnet4j.service.unconfirmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class IHaveRequest extends UnconfirmedRequestService {
    static final Logger LOG = LoggerFactory.getLogger(IHaveRequest.class);

    public static final byte TYPE_ID = 1;

    private final ObjectIdentifier deviceIdentifier;
    private final ObjectIdentifier objectIdentifier;
    private final CharacterString objectName;

    public IHaveRequest(final ObjectIdentifier deviceIdentifier, final ObjectIdentifier objectIdentifier,
            final CharacterString objectName) {
        super();
        this.deviceIdentifier = deviceIdentifier;
        this.objectIdentifier = objectIdentifier;
        this.objectName = objectName;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void handle(final LocalDevice localDevice, final Address from) {
        // Ignore requests from our own device id.
        if (localDevice.getInstanceNumber() == deviceIdentifier.getInstanceNumber()) {
            return;
        }

        localDevice.updateRemoteDevice(deviceIdentifier.getInstanceNumber(), from);

        final RemoteDevice d = localDevice.getCachedRemoteDevice(deviceIdentifier.getInstanceNumber());
        if (d == null) {
            // Populate the object with discovered values, but do so in a different thread.
            localDevice.execute(() -> {
                try {
                    final RemoteDevice rd = localDevice.getRemoteDeviceBlocking(deviceIdentifier.getInstanceNumber());
                    rd.setObjectProperty(objectIdentifier, PropertyIdentifier.objectName, objectName);
                    localDevice.getEventHandler().fireIHaveReceived(rd, rd.getObject(objectIdentifier));
                } catch (final BACnetException e) {
                    LOG.warn("Error while discovering extended device information", e);
                }
            });
        } else {
            d.setObjectProperty(objectIdentifier, PropertyIdentifier.objectName, objectName);
            localDevice.getEventHandler().fireIHaveReceived(d, d.getObject(objectIdentifier));
        }
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, deviceIdentifier);
        write(queue, objectIdentifier);
        write(queue, objectName);
    }

    IHaveRequest(final ByteQueue queue) throws BACnetException {
        deviceIdentifier = read(queue, ObjectIdentifier.class);
        objectIdentifier = read(queue, ObjectIdentifier.class);
        objectName = read(queue, CharacterString.class);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (deviceIdentifier == null ? 0 : deviceIdentifier.hashCode());
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + (objectName == null ? 0 : objectName.hashCode());
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
        final IHaveRequest other = (IHaveRequest) obj;
        if (deviceIdentifier == null) {
            if (other.deviceIdentifier != null)
                return false;
        } else if (!deviceIdentifier.equals(other.deviceIdentifier))
            return false;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        if (objectName == null) {
            if (other.objectName != null)
                return false;
        } else if (!objectName.equals(other.objectName))
            return false;
        return true;
    }
}
