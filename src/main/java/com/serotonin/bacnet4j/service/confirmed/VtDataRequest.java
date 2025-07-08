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

package com.serotonin.bacnet4j.service.confirmed;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.NotImplementedException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class VtDataRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 23;

    private final UnsignedInteger vtSessionIdentifier;
    private final OctetString vtNewData;
    private final UnsignedInteger vtDataFlag;

    public VtDataRequest(final UnsignedInteger vtSessionIdentifier, final OctetString vtNewData,
            final UnsignedInteger vtDataFlag) {
        this.vtSessionIdentifier = vtSessionIdentifier;
        this.vtNewData = vtNewData;
        this.vtDataFlag = vtDataFlag;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, vtSessionIdentifier);
        write(queue, vtNewData);
        write(queue, vtDataFlag);
    }

    VtDataRequest(final ByteQueue queue) throws BACnetException {
        vtSessionIdentifier = read(queue, UnsignedInteger.class);
        vtNewData = read(queue, OctetString.class);
        vtDataFlag = read(queue, UnsignedInteger.class);
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        throw new NotImplementedException();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (vtDataFlag == null ? 0 : vtDataFlag.hashCode());
        result = PRIME * result + (vtNewData == null ? 0 : vtNewData.hashCode());
        result = PRIME * result + (vtSessionIdentifier == null ? 0 : vtSessionIdentifier.hashCode());
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
        final VtDataRequest other = (VtDataRequest) obj;
        if (vtDataFlag == null) {
            if (other.vtDataFlag != null)
                return false;
        } else if (!vtDataFlag.equals(other.vtDataFlag))
            return false;
        if (vtNewData == null) {
            if (other.vtNewData != null)
                return false;
        } else if (!vtNewData.equals(other.vtNewData))
            return false;
        if (vtSessionIdentifier == null) {
            if (other.vtSessionIdentifier != null)
                return false;
        } else if (!vtSessionIdentifier.equals(other.vtSessionIdentifier))
            return false;
        return true;
    }
}
