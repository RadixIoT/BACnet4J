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

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AddressBinding extends BaseType {
    private final ObjectIdentifier deviceObjectIdentifier;
    private final Address deviceAddress;

    public AddressBinding(final ObjectIdentifier deviceObjectIdentifier, final Address deviceAddress) {
        this.deviceObjectIdentifier = deviceObjectIdentifier;
        this.deviceAddress = deviceAddress;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, deviceObjectIdentifier);
        write(queue, deviceAddress);
    }

    public AddressBinding(final ByteQueue queue) throws BACnetException {
        deviceObjectIdentifier = read(queue, ObjectIdentifier.class);
        deviceAddress = read(queue, Address.class);
    }

    public ObjectIdentifier getDeviceObjectIdentifier() {
        return deviceObjectIdentifier;
    }

    public Address getDeviceAddress() {
        return deviceAddress;
    }

    @Override
    public String toString() {
        return "AddressBinding [deviceObjectIdentifier=" + deviceObjectIdentifier + ", deviceAddress=" + deviceAddress
                + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (deviceAddress == null ? 0 : deviceAddress.hashCode());
        result = PRIME * result + (deviceObjectIdentifier == null ? 0 : deviceObjectIdentifier.hashCode());
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
        final AddressBinding other = (AddressBinding) obj;
        if (deviceAddress == null) {
            if (other.deviceAddress != null)
                return false;
        } else if (!deviceAddress.equals(other.deviceAddress))
            return false;
        if (deviceObjectIdentifier == null) {
            if (other.deviceObjectIdentifier != null)
                return false;
        } else if (!deviceObjectIdentifier.equals(other.deviceObjectIdentifier))
            return false;
        return true;
    }
}
