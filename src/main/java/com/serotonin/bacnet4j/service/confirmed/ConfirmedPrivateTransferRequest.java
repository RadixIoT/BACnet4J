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
import com.serotonin.bacnet4j.event.PrivateTransferHandler;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.acknowledgement.ConfirmedPrivateTransferAck;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.EncodedValue;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.error.ConfirmedPrivateTransferError;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ConfirmedPrivateTransferRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 18;

    private final UnsignedInteger vendorId;
    private final UnsignedInteger serviceNumber;
    private final Encodable serviceParameters;

    public ConfirmedPrivateTransferRequest(final int vendorId, final int serviceNumber,
            final Encodable serviceParameters) {
        this(new UnsignedInteger(vendorId), new UnsignedInteger(serviceNumber), serviceParameters);
    }

    public ConfirmedPrivateTransferRequest(final UnsignedInteger vendorId, final UnsignedInteger serviceNumber,
            final Encodable serviceParameters) {
        this.vendorId = vendorId;
        this.serviceNumber = serviceNumber;
        this.serviceParameters = serviceParameters;
    }

    ConfirmedPrivateTransferRequest(final ByteQueue queue) throws BACnetException {
        vendorId = read(queue, UnsignedInteger.class, 0);
        serviceNumber = read(queue, UnsignedInteger.class, 1);
        serviceParameters = readEncodedValue(queue, 2);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, vendorId, 0);
        write(queue, serviceNumber, 1);
        writeOptional(queue, serviceParameters, 2);
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final PrivateTransferHandler handler = localDevice.getPrivateTransferHandler(vendorId, serviceNumber);

        if (handler == null) {
            throw new BACnetErrorException(TYPE_ID,
                    new ConfirmedPrivateTransferError(
                            new ErrorClassAndCode(ErrorClass.services, ErrorCode.optionalFunctionalityNotSupported),
                            vendorId, serviceNumber, new CharacterString("No handler for vendorId/serviceNumber")));
        }

        final Encodable resultBlock = handler.handle(localDevice, from, vendorId, serviceNumber,
                (EncodedValue) serviceParameters, true);

        return new ConfirmedPrivateTransferAck(vendorId, serviceNumber, resultBlock);
    }

    public UnsignedInteger getVendorId() {
        return vendorId;
    }

    public UnsignedInteger getServiceNumber() {
        return serviceNumber;
    }

    public Encodable getServiceParameters() {
        return serviceParameters;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (serviceNumber == null ? 0 : serviceNumber.hashCode());
        result = PRIME * result + (serviceParameters == null ? 0 : serviceParameters.hashCode());
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
        final ConfirmedPrivateTransferRequest other = (ConfirmedPrivateTransferRequest) obj;
        if (serviceNumber == null) {
            if (other.serviceNumber != null)
                return false;
        } else if (!serviceNumber.equals(other.serviceNumber))
            return false;
        if (serviceParameters == null) {
            if (other.serviceParameters != null)
                return false;
        } else if (!serviceParameters.equals(other.serviceParameters))
            return false;
        if (vendorId == null) {
            if (other.vendorId != null)
                return false;
        } else if (!vendorId.equals(other.vendorId))
            return false;
        return true;
    }
}
