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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.CalendarObject;
import com.serotonin.bacnet4j.obj.EventLogObject;
import com.serotonin.bacnet4j.obj.GroupObject;
import com.serotonin.bacnet4j.obj.NotificationClassObject;
import com.serotonin.bacnet4j.obj.ScheduleObject;
import com.serotonin.bacnet4j.obj.TrendLogMultipleObject;
import com.serotonin.bacnet4j.obj.TrendLogObject;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.acknowledgement.CreateObjectAck;
import com.serotonin.bacnet4j.type.ThreadLocalObjectTypeStack;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.constructed.ObjectTypesSupported;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.CreateObjectError;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class CreateObjectRequest extends ConfirmedRequestService {
    static final Logger LOG = LoggerFactory.getLogger(CreateObjectRequest.class);

    public static final byte TYPE_ID = 10;

    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, ObjectType.class);
        choiceOptions.addContextual(1, ObjectIdentifier.class);
    }

    private static Map<ObjectType, ObjectCreator> creators = new HashMap<>();

    static {
        creators.put(ObjectType.calendar, (d, number) -> CalendarObject.create(d, number));
        creators.put(ObjectType.eventLog, (d, number) -> EventLogObject.create(d, number));
        creators.put(ObjectType.group, (d, number) -> GroupObject.create(d, number));
        creators.put(ObjectType.notificationClass, (d, number) -> NotificationClassObject.create(d, number));
        creators.put(ObjectType.schedule, (d, number) -> ScheduleObject.create(d, number));
        creators.put(ObjectType.trendLog, (d, number) -> TrendLogObject.create(d, number));
        creators.put(ObjectType.trendLogMultiple, (d, number) -> TrendLogMultipleObject.create(d, number));
    }

    private final Choice objectSpecifier;
    private final SequenceOf<PropertyValue> listOfInitialValues;

    public CreateObjectRequest(final ObjectType objectType, final SequenceOf<PropertyValue> listOfInitialValues) {
        objectSpecifier = new Choice(0, objectType, choiceOptions);
        this.listOfInitialValues = listOfInitialValues;
    }

    public CreateObjectRequest(final ObjectIdentifier objectIdentifier,
            final SequenceOf<PropertyValue> listOfInitialValues) {
        objectSpecifier = new Choice(1, objectIdentifier, choiceOptions);
        this.listOfInitialValues = listOfInitialValues;
    }

    CreateObjectRequest(final ByteQueue queue) throws BACnetException {
        objectSpecifier = readChoice(queue, choiceOptions, 0);
        try {
            ThreadLocalObjectTypeStack.set(getObjectType());
            listOfInitialValues = readOptionalSequenceOf(queue, PropertyValue.class, 1);
        } finally {
            ThreadLocalObjectTypeStack.remove();
        }
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectSpecifier, 0);
        writeOptional(queue, listOfInitialValues, 1);
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    private ObjectType getObjectType() {
        if (objectSpecifier.isa(ObjectType.class)) {
            return objectSpecifier.getDatum();
        }
        final ObjectIdentifier oid = objectSpecifier.getDatum();
        return oid.getObjectType();
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final ObjectType objectType;
        int instanceNumber;

        if (objectSpecifier.isa(ObjectType.class)) {
            objectType = objectSpecifier.getDatum();
            instanceNumber = -1;
        } else {
            final ObjectIdentifier oid = objectSpecifier.getDatum();
            objectType = oid.getObjectType();
            instanceNumber = oid.getInstanceNumber();
        }

        // Get the list of object types supported by the local device.
        final ObjectTypesSupported objectTypesSupported = localDevice.getDeviceObject()
                .get(PropertyIdentifier.protocolObjectTypesSupported);
        if (!objectTypesSupported.is(objectType)) {
            throw createException(ErrorClass.object, ErrorCode.unsupportedObjectType, 0);
        }

        // Find the creator
        final ObjectCreator creator = creators.get(objectType);
        if (creator == null) {
            throw createException(ErrorClass.object, ErrorCode.dynamicCreationNotSupported, 0);
        }

        // Get a new instance number if needed.
        if (instanceNumber == -1) {
            instanceNumber = localDevice.getNextInstanceObjectNumber(objectType);
        }

        try {
            final BACnetObject bo;
            try {
                bo = creator.create(localDevice, instanceNumber);
            } catch (final BACnetServiceException e) {
                throw createException(e.getErrorClass(), e.getErrorCode(), 0);
            }

            // Set other default properties.
            bo.writePropertyInternal(PropertyIdentifier.description, CharacterString.EMPTY);
            bo.writePropertyInternal(PropertyIdentifier.tags, new BACnetArray<>());
            bo.writePropertyInternal(PropertyIdentifier.profileLocation, CharacterString.EMPTY);
            bo.writePropertyInternal(PropertyIdentifier.profileName, CharacterString.EMPTY);

            // Try to write in the given property values.
            final ValueSource valueSource = new ValueSource(from);
            for (int i = 0; i < listOfInitialValues.getCount(); i++) {
                try {
                    bo.writeProperty(valueSource, listOfInitialValues.get(i));
                } catch (final BACnetServiceException e) {
                    throw createException(e.getErrorClass(), e.getErrorCode(), i + 1);
                }
            }

            // Dynamically created objects can also be deleted.
            bo.setDeletable(true);

            return new CreateObjectAck(bo.getId());
        } catch (final Exception e) {
            // Upon any exception, remove the object if it was created.
            try {
                localDevice.removeObject(new ObjectIdentifier(ObjectType.group, instanceNumber));
            } catch (final BACnetServiceException e1) {
                LOG.warn("Error removing object that failed to initialize", e1);
            }
            throw e;
        }
    }

    private static BACnetErrorException createException(final ErrorClass errorClass, final ErrorCode errorCode,
            final int firstFailedElementNumber) {
        return new BACnetErrorException(TYPE_ID, new CreateObjectError(new ErrorClassAndCode(errorClass, errorCode),
                new UnsignedInteger(firstFailedElementNumber)));
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (listOfInitialValues == null ? 0 : listOfInitialValues.hashCode());
        result = PRIME * result + (objectSpecifier == null ? 0 : objectSpecifier.hashCode());
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
        final CreateObjectRequest other = (CreateObjectRequest) obj;
        if (listOfInitialValues == null) {
            if (other.listOfInitialValues != null)
                return false;
        } else if (!listOfInitialValues.equals(other.listOfInitialValues))
            return false;
        if (objectSpecifier == null) {
            if (other.objectSpecifier != null)
                return false;
        } else if (!objectSpecifier.equals(other.objectSpecifier))
            return false;
        return true;
    }

    @FunctionalInterface
    public static interface ObjectCreator {
        BACnetObject create(LocalDevice d, int instanceNumber) throws BACnetServiceException;
    }
}
