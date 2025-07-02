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
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class VmacEntry extends BaseType {
    private final OctetString virtualMacAddress;
    private final OctetString nativeMacAddress;

    public VmacEntry(final OctetString virtualMacAddress, final OctetString nativeMacAddress) {
        this.virtualMacAddress = virtualMacAddress;
        this.nativeMacAddress = nativeMacAddress;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, virtualMacAddress, 0);
        write(queue, nativeMacAddress, 1);
    }

    public VmacEntry(final ByteQueue queue) throws BACnetException {
        virtualMacAddress = read(queue, OctetString.class, 0);
        nativeMacAddress = read(queue, OctetString.class, 1);
    }

    public OctetString getVirtualMacAddress() {
        return virtualMacAddress;
    }

    public OctetString getNativeMacAddress() {
        return nativeMacAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (nativeMacAddress == null ? 0 : nativeMacAddress.hashCode());
        result = prime * result + (virtualMacAddress == null ? 0 : virtualMacAddress.hashCode());
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
        final VmacEntry other = (VmacEntry) obj;
        if (nativeMacAddress == null) {
            if (other.nativeMacAddress != null)
                return false;
        } else if (!nativeMacAddress.equals(other.nativeMacAddress))
            return false;
        if (virtualMacAddress == null) {
            if (other.virtualMacAddress != null)
                return false;
        } else if (!virtualMacAddress.equals(other.virtualMacAddress))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "VmacEntry [virtualMacAddress=" + virtualMacAddress + ", nativeMacAddress=" + nativeMacAddress + ']';
    }
}
