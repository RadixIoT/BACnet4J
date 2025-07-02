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
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ConfirmedPrivateTransferAck extends AcknowledgementService {
    public static final byte TYPE_ID = 18;

    private final UnsignedInteger vendorId;
    private final UnsignedInteger serviceNumber;
    private final Encodable resultBlock;

    public ConfirmedPrivateTransferAck(final UnsignedInteger vendorId, final UnsignedInteger serviceNumber,
            final Encodable resultBlock) {
        this.vendorId = vendorId;
        this.serviceNumber = serviceNumber;
        this.resultBlock = resultBlock;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, vendorId, 0);
        write(queue, serviceNumber, 1);
        writeOptional(queue, resultBlock, 2);
    }

    ConfirmedPrivateTransferAck(final ByteQueue queue) throws BACnetException {
        vendorId = read(queue, UnsignedInteger.class, 0);
        serviceNumber = read(queue, UnsignedInteger.class, 1);
        resultBlock = readEncodedValue(queue, 2);
    }

    public UnsignedInteger getVendorId() {
        return vendorId;
    }

    public UnsignedInteger getServiceNumber() {
        return serviceNumber;
    }

    public Encodable getResultBlock() {
        return resultBlock;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (resultBlock == null ? 0 : resultBlock.hashCode());
        result = PRIME * result + (serviceNumber == null ? 0 : serviceNumber.hashCode());
        result = PRIME * result + (vendorId == null ? 0 : vendorId.hashCode());
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
        final ConfirmedPrivateTransferAck other = (ConfirmedPrivateTransferAck) obj;
        if (resultBlock == null) {
            if (other.resultBlock != null)
                return false;
        } else if (!resultBlock.equals(other.resultBlock))
            return false;
        if (serviceNumber == null) {
            if (other.serviceNumber != null)
                return false;
        } else if (!serviceNumber.equals(other.serviceNumber))
            return false;
        if (vendorId == null) {
            if (other.vendorId != null)
                return false;
        } else if (!vendorId.equals(other.vendorId))
            return false;
        return true;
    }
}
