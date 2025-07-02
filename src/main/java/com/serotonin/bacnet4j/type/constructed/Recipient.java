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

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Recipient extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, ObjectIdentifier.class);
        choiceOptions.addContextual(1, Address.class);
    }

    private final Choice choice;

    public Recipient(final ObjectIdentifier device) {
        choice = new Choice(0, device, choiceOptions);
    }

    public Recipient(final Address address) {
        choice = new Choice(1, address, choiceOptions);
    }

    public boolean isDevice() {
        return choice.isa(ObjectIdentifier.class);
    }

    public ObjectIdentifier getDevice() {
        return choice.getDatum();
    }

    public boolean isAddress() {
        return choice.isa(Address.class);
    }

    public Address getAddress() {
        return choice.getDatum();
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public Recipient(final ByteQueue queue) throws BACnetException {
        choice = new Choice(queue, choiceOptions);
    }

    public Address toAddress(final LocalDevice localDevice) throws BACnetException {
        if (isAddress())
            return getAddress();

        final int deviceId = getDevice().getInstanceNumber();
        final RemoteDevice rd = localDevice.getRemoteDevice(deviceId).get();
        return rd.getAddress();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (choice == null ? 0 : choice.hashCode());
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
        final Recipient other = (Recipient) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Recipient [choice=" + choice + "]";
    }
}
