/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.unconfirmed.IAmRequest;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class DeviceAddressProxyTableEntry extends BaseType {
    private final Address address;
    private final IAmRequest iAm;
    private final DateTime lastIAmTime;

    public DeviceAddressProxyTableEntry(Address address, IAmRequest iAm, DateTime lastIAmTime) {
        this.address = address;
        this.iAm = iAm;
        this.lastIAmTime = lastIAmTime;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, address, 0);
        write(queue, iAm, 1);
        write(queue, lastIAmTime, 2);
    }

    @Override
    public String toString() {
        return "DeviceAddressProxyTableEntry [" +
                "address=" + address +
                ", iAm=" + iAm +
                ", lastIAmTime=" + lastIAmTime +
                ']';
    }

    public Address getAddress() {
        return address;
    }

    public IAmRequest getiAm() {
        return iAm;
    }

    public DateTime getLastIAmTime() {
        return lastIAmTime;
    }

    public DeviceAddressProxyTableEntry(final ByteQueue queue) throws BACnetException {
        address = read(queue, Address.class, 0);
        iAm = read(queue, IAmRequest.class, 1);
        lastIAmTime = read(queue, DateTime.class, 2);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        DeviceAddressProxyTableEntry that = (DeviceAddressProxyTableEntry) o;
        return Objects.equals(address, that.address) && Objects.equals(iAm,
                that.iAm) && Objects.equals(lastIAmTime, that.lastIAmTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, iAm, lastIAmTime);
    }
}
