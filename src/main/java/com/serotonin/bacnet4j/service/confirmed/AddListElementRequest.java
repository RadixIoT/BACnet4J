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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.ObjectProperties;
import com.serotonin.bacnet4j.obj.ObjectPropertyTypeDefinition;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ChangeListError;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AddListElementRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 8;

    private final ObjectIdentifier objectIdentifier;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger propertyArrayIndex;
    private final SequenceOf<? extends Encodable> listOfElements;
    static final Logger LOG = LoggerFactory.getLogger(AddListElementRequest.class);

    public AddListElementRequest(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex, final SequenceOf<? extends Encodable> listOfElements) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
        this.listOfElements = listOfElements;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final BACnetObject obj = localDevice.getObject(objectIdentifier);
        if (obj == null)
            throw createException(ErrorClass.object, ErrorCode.unknownObject, UnsignedInteger.ZERO);

        final PropertyValue pv = new PropertyValue(propertyIdentifier, propertyArrayIndex, listOfElements, null);
        if (!localDevice.getEventHandler().checkAllowPropertyWrite(from, obj, pv))
            throw createException(ErrorClass.property, ErrorCode.writeAccessDenied, UnsignedInteger.ZERO);

        ObjectPropertyTypeDefinition def = ObjectProperties
                .getObjectPropertyTypeDefinition(objectIdentifier.getObjectType(), propertyIdentifier);

        Encodable e;
        try {
            e = obj.readPropertyRequired(propertyIdentifier);
        } catch (final BACnetServiceException ex) {
            throw createException(ex.getErrorClass(), ex.getErrorCode(), UnsignedInteger.ZERO);
        }

        BACnetArray<Encodable> array = null;
        if (propertyArrayIndex != null) {
            // The property must be an array.
            if (!(e instanceof BACnetArray))
                throw createException(ErrorClass.property, ErrorCode.propertyIsNotAnArray, UnsignedInteger.ZERO);

            array = (BACnetArray<Encodable>) e;

            // Check the requested index.
            final int index = propertyArrayIndex.intValue();
            if (index < 1 || index > array.getCount())
                throw createException(ErrorClass.property, ErrorCode.invalidArrayIndex, UnsignedInteger.ZERO);

            e = array.getBase1(index);
            if (e == null)
                // This should never happen, but check.
                throw new RuntimeException("Array with null element: " + array);

            // Set the def null because we don't know the inner type of a list in an array.
            def = null;
        }

        // The value we end up with must be a list.
        if (!(e instanceof SequenceOf))
            throw createException(ErrorClass.services, ErrorCode.propertyIsNotAList, UnsignedInteger.ZERO);
        if (e instanceof BACnetArray)
            throw createException(ErrorClass.services, ErrorCode.propertyIsNotAList, UnsignedInteger.ZERO);

        //Copy the original sequence
        final SequenceOf<Encodable> copyList =
                new SequenceOf<>(new ArrayList<>(((SequenceOf<Encodable>) e).getValues()));
        final SequenceOf<Encodable> list = new SequenceOf<>(copyList.getValues());
        for (int i = 0; i < listOfElements.getCount(); i++) {
            final Encodable pr = listOfElements.getBase1(i + 1);

            if (def != null) {
                // If we have a property def, check it.
                if (!def.getPropertyTypeDefinition().getClazz().isAssignableFrom(pr.getClass())) {
                    throw createException(ErrorClass.property, ErrorCode.invalidDataType, new UnsignedInteger(i + 1));
                }
            } else {
                // Otherwise, if there are already elements in the list, we can check against the first one.
                if (list.getCount() > 0) {
                    if (list.getBase1(1).getClass() != pr.getClass()) {
                        throw createException(ErrorClass.property, ErrorCode.invalidDataType,
                                new UnsignedInteger(i + 1));
                    }
                }
            }

            final int pos = list.indexOf(pr);
            if (pos != -1)
                list.set(pos, pr);
            else
                list.add(pr);
        }

        if (array != null) {
            array.setBase1(propertyArrayIndex.intValue(), copyList);
            e = array;
        } else {
            e = copyList;
        }

        try {
            obj.writeProperty(new ValueSource(from), propertyIdentifier, e);
        } catch (final BACnetServiceException ex) {
            throw createException(ex.getErrorClass(), ex.getErrorCode(), UnsignedInteger.ZERO);
        }

        localDevice.getEventHandler().propertyWritten(from, obj, pv);

        return null;
    }

    private BACnetErrorException createException(final ErrorClass errorClass, final ErrorCode errorCode,
            final UnsignedInteger firstFailedElementNumber) {
        return new BACnetErrorException(getChoiceId(),
                new ChangeListError(new ErrorClassAndCode(errorClass, errorCode), firstFailedElementNumber));
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, propertyIdentifier, 1);
        writeOptional(queue, propertyArrayIndex, 2);
        writeANY(queue, listOfElements, 3);
    }

    AddListElementRequest(final ByteQueue queue) throws BACnetException {
        int errorIndex = 0;
        try {
            objectIdentifier = read(queue, ObjectIdentifier.class, 0);
            propertyIdentifier = read(queue, PropertyIdentifier.class, 1);
            propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 2);
            // We have to encode the values here because we need the correct 
            // errorIndex to fulfill the standard test 135.1-2013 9.14.2.3
            final ObjectPropertyTypeDefinition def = ObjectProperties
                    .getObjectPropertyTypeDefinition(objectIdentifier.getObjectType(), propertyIdentifier);
            popStart(queue, 3);
            List<Encodable> values = new ArrayList<>();
            while (readEnd(queue) != 3) {
                errorIndex++;
                values.add(read(queue, def.getPropertyTypeDefinition().getClazz()));
            }
            popEnd(queue, 3);
            listOfElements = new SequenceOf<>(values);
        } catch (BACnetErrorException ex) {
            ErrorClassAndCode errorClassAndCode = ex.getBacnetError().getError().getErrorClassAndCode();
            throw createException(errorClassAndCode.getErrorClass(), errorClassAndCode.getErrorCode(),
                    new UnsignedInteger(errorIndex));
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + (listOfElements == null ? 0 : listOfElements.hashCode());
        result = PRIME * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
        result = PRIME * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
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
        final AddListElementRequest other = (AddListElementRequest) obj;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        if (listOfElements == null) {
            if (other.listOfElements != null)
                return false;
        } else if (!listOfElements.equals(other.listOfElements))
            return false;
        if (propertyArrayIndex == null) {
            if (other.propertyArrayIndex != null)
                return false;
        } else if (!propertyArrayIndex.equals(other.propertyArrayIndex))
            return false;
        if (propertyIdentifier == null) {
            if (other.propertyIdentifier != null)
                return false;
        } else if (!propertyIdentifier.equals(other.propertyIdentifier))
            return false;
        return true;
    }
}
