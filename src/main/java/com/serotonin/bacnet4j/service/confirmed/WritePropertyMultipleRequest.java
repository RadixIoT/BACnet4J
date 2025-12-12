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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.constructed.WriteAccessSpecification;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.error.WritePropertyMultipleError;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class WritePropertyMultipleRequest extends ConfirmedRequestService {
    static final Logger LOG = LoggerFactory.getLogger(WritePropertyMultipleRequest.class);
    public static final byte TYPE_ID = 16;

    private final SequenceOf<WriteAccessSpecification> listOfWriteAccessSpecifications;

    public WritePropertyMultipleRequest(final SequenceOf<WriteAccessSpecification> listOfWriteAccessSpecifications) {
        this.listOfWriteAccessSpecifications = listOfWriteAccessSpecifications;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, listOfWriteAccessSpecifications);
    }

    WritePropertyMultipleRequest(final ByteQueue queue) throws BACnetException {
        listOfWriteAccessSpecifications = readSequenceOf(queue, WriteAccessSpecification.class);
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        BACnetObject obj;
        for (final WriteAccessSpecification spec : listOfWriteAccessSpecifications) {
            obj = localDevice.getObject(spec.getObjectIdentifier());
            if (obj == null)
                throw createException(ErrorClass.property, ErrorCode.unknownObject, spec, null);

            for (final PropertyValue pv : spec.getListOfProperties()) {
                LOG.info("Writing property {} into {}", pv, obj);
                try {
                    if (localDevice.getEventHandler().checkAllowPropertyWrite(from, obj, pv)) {
                        obj.writeProperty(new ValueSource(from), pv);
                        localDevice.getEventHandler().propertyWritten(from, obj, pv);
                    } else
                        throw createException(ErrorClass.property, ErrorCode.writeAccessDenied, spec, pv);
                } catch (final BACnetServiceException e) {
                    throw createException(e.getErrorClass(), e.getErrorCode(), spec, pv);
                }
            }
        }

        return null;
    }

    private BACnetErrorException createException(final ErrorClass errorClass, final ErrorCode errorCode,
            final WriteAccessSpecification spec, final PropertyValue pv) {
        final PropertyValue pvToUse = pv == null ? spec.getListOfProperties().getBase1(1) : pv;
        return new BACnetErrorException(getChoiceId(),
                new WritePropertyMultipleError(new ErrorClassAndCode(errorClass, errorCode),
                        new ObjectPropertyReference(spec.getObjectIdentifier(), pvToUse.getPropertyIdentifier(),
                                pvToUse.getPropertyArrayIndex())));
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result
                + (listOfWriteAccessSpecifications == null ? 0 : listOfWriteAccessSpecifications.hashCode());
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
        final WritePropertyMultipleRequest other = (WritePropertyMultipleRequest) obj;
        if (listOfWriteAccessSpecifications == null) {
            if (other.listOfWriteAccessSpecifications != null)
                return false;
        } else if (!listOfWriteAccessSpecifications.equals(other.listOfWriteAccessSpecifications))
            return false;
        return true;
    }
}
