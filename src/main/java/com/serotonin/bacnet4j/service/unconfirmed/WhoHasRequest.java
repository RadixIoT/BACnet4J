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

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class WhoHasRequest extends UnconfirmedRequestService {
    public static final byte TYPE_ID = 7;

    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(2, ObjectIdentifier.class);
        choiceOptions.addContextual(3, CharacterString.class);
    }

    private final Limits limits;
    private final Choice object;

    public WhoHasRequest(final Limits limits, final ObjectIdentifier identifier) {
        this.limits = limits;
        object = new Choice(2, identifier, choiceOptions);
    }

    public WhoHasRequest(final Limits limits, final CharacterString name) {
        this.limits = limits;
        object = new Choice(3, name, choiceOptions);
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        // Check if we're in the device id range.
        if (limits != null) {
            final int localId = localDevice.getInstanceNumber();
            if (localId < limits.getDeviceInstanceRangeLowLimit().intValue()
                    || localId > limits.getDeviceInstanceRangeHighLimit().intValue())
                return;
        }

        // Check if we have the thing being looking for.
        BACnetObject result;
        if (object.isa(ObjectIdentifier.class)) {
            final ObjectIdentifier oid = (ObjectIdentifier) object.getDatum();
            result = localDevice.getObject(oid);
        } else if (object.isa(CharacterString.class)) {
            final String name = ((CharacterString) object.getDatum()).toString();
            result = localDevice.getObject(name);
        } else
            return;

        if (result != null) {
            // Return the result in an i have message.
            final IHaveRequest response = new IHaveRequest(localDevice.getId(), result.getId(),
                    (CharacterString) result.get(PropertyIdentifier.objectName));
            localDevice.sendGlobalBroadcast(response);
        }
    }

    @Override
    public void write(final ByteQueue queue) {
        writeOptional(queue, limits);
        write(queue, object);
    }

    public WhoHasRequest(final ByteQueue queue) throws BACnetException {
        final Limits l = new Limits(queue);
        limits = l.getDeviceInstanceRangeLowLimit() == null ? null : l;
        object = readChoice(queue, choiceOptions);
    }

    public static class Limits extends BaseType {
        private UnsignedInteger deviceInstanceRangeLowLimit;
        private UnsignedInteger deviceInstanceRangeHighLimit;

        @Override
        public void write(final ByteQueue queue) {
            write(queue, deviceInstanceRangeLowLimit, 0);
            write(queue, deviceInstanceRangeHighLimit, 1);
        }

        Limits(final ByteQueue queue) throws BACnetException {
            deviceInstanceRangeLowLimit = readOptional(queue, UnsignedInteger.class, 0);
            deviceInstanceRangeHighLimit = readOptional(queue, UnsignedInteger.class, 1);
        }

        public Limits(final UnsignedInteger deviceInstanceRangeLowLimit,
                final UnsignedInteger deviceInstanceRangeHighLimit) {
            if (deviceInstanceRangeLowLimit == null || deviceInstanceRangeHighLimit == null)
                throw new RuntimeException("Both the low and high limits must be set");
            this.deviceInstanceRangeLowLimit = deviceInstanceRangeLowLimit;
            this.deviceInstanceRangeHighLimit = deviceInstanceRangeHighLimit;
        }

        public UnsignedInteger getDeviceInstanceRangeLowLimit() {
            return deviceInstanceRangeLowLimit;
        }

        public void setDeviceInstanceRangeLowLimit(final UnsignedInteger deviceInstanceRangeLowLimit) {
            this.deviceInstanceRangeLowLimit = deviceInstanceRangeLowLimit;
        }

        public UnsignedInteger getDeviceInstanceRangeHighLimit() {
            return deviceInstanceRangeHighLimit;
        }

        public void setDeviceInstanceRangeHighLimit(final UnsignedInteger deviceInstanceRangeHighLimit) {
            this.deviceInstanceRangeHighLimit = deviceInstanceRangeHighLimit;
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (limits == null ? 0 : limits.hashCode());
        result = PRIME * result + (object == null ? 0 : object.hashCode());
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
        final WhoHasRequest other = (WhoHasRequest) obj;
        if (limits == null) {
            if (other.limits != null)
                return false;
        } else if (!limits.equals(other.limits))
            return false;
        if (object == null) {
            if (other.object != null)
                return false;
        } else if (!object.equals(other.object))
            return false;
        return true;
    }
}
