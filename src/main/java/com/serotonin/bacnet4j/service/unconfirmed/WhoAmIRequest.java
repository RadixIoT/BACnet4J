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

package com.serotonin.bacnet4j.service.unconfirmed;

import java.util.Objects;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.NotImplementedException;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class WhoAmIRequest extends UnconfirmedRequestService {
    public static final byte TYPE_ID = 13;

    private final Unsigned16 vendorId;
    private final CharacterString modelName;
    private final CharacterString serialNumber;

    public WhoAmIRequest(Unsigned16 vendorId, CharacterString modelName, CharacterString serialNumber) {
        this.vendorId = vendorId;
        this.modelName = modelName;
        this.serialNumber = serialNumber;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    public Unsigned16 getVendorId() {
        return vendorId;
    }

    public CharacterString getModelName() {
        return modelName;
    }

    public CharacterString getSerialNumber() {
        return serialNumber;
    }

    @Override
    public void handle(LocalDevice localDevice, Address from) throws BACnetException {
        throw new NotImplementedException();
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, vendorId);
        write(queue, modelName);
        write(queue, serialNumber);
    }

    WhoAmIRequest(ByteQueue queue) throws BACnetException {
        vendorId = read(queue, Unsigned16.class);
        modelName = read(queue, CharacterString.class);
        serialNumber = read(queue, CharacterString.class);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        WhoAmIRequest that = (WhoAmIRequest) o;
        return Objects.equals(vendorId, that.vendorId) && Objects.equals(modelName,
                that.modelName) && Objects.equals(serialNumber, that.serialNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, modelName, serialNumber);
    }
}
