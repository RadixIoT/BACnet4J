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

package com.serotonin.bacnet4j.service.acknowledgement;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class VtDataAck extends AcknowledgementService {
    public static final byte TYPE_ID = 23;

    private final Boolean allNewDataAccepted;
    private final UnsignedInteger acceptedOctetCount;

    public VtDataAck(final Boolean allNewDataAccepted, final UnsignedInteger acceptedOctetCount) {
        this.allNewDataAccepted = allNewDataAccepted;
        this.acceptedOctetCount = acceptedOctetCount;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, allNewDataAccepted, 0);
        writeOptional(queue, acceptedOctetCount, 1);
    }

    VtDataAck(final ByteQueue queue) throws BACnetException {
        allNewDataAccepted = read(queue, Boolean.class, 0);
        acceptedOctetCount = readOptional(queue, UnsignedInteger.class, 1);
    }

    public Boolean getAllNewDataAccepted() {
        return allNewDataAccepted;
    }

    public UnsignedInteger getAcceptedOctetCount() {
        return acceptedOctetCount;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (acceptedOctetCount == null ? 0 : acceptedOctetCount.hashCode());
        result = PRIME * result + (allNewDataAccepted == null ? 0 : allNewDataAccepted.hashCode());
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
        final VtDataAck other = (VtDataAck) obj;
        if (acceptedOctetCount == null) {
            if (other.acceptedOctetCount != null)
                return false;
        } else if (!acceptedOctetCount.equals(other.acceptedOctetCount))
            return false;
        if (allNewDataAccepted == null) {
            if (other.allNewDataAccepted != null)
                return false;
        } else if (!allNewDataAccepted.equals(other.allNewDataAccepted))
            return false;
        return true;
    }
}
