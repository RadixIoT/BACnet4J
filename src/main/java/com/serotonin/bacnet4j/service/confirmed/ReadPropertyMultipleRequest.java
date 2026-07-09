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
import java.util.Objects;
import java.util.stream.Collectors;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.ObjectProperties;
import com.serotonin.bacnet4j.obj.ObjectPropertyTypeDefinition;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult.Result;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ReadPropertyMultipleRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 14;

    private final SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs;

    public ReadPropertyMultipleRequest(SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs) {
        this.listOfReadAccessSpecs = listOfReadAccessSpecs;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, listOfReadAccessSpecs);
    }

    ReadPropertyMultipleRequest(ByteQueue queue) throws BACnetException {
        listOfReadAccessSpecs = readSequenceOf(queue, ReadAccessSpecification.class);
    }

    @Override
    public AcknowledgementService handle(LocalDevice localDevice, Address from) throws BACnetException {
        List<ReadAccessResult> readAccessResults = new ArrayList<>();

        for (ReadAccessSpecification req : listOfReadAccessSpecs) {
            var oid = req.getObjectIdentifier();
            var obj = localDevice.getObject(oid, true);
            if (obj != null) {
                oid = obj.getId();
            }
            var results = new ArrayList<Result>();
            for (PropertyReference propRef : req.getListOfPropertyReferences()) {
                addProperty(obj, results, propRef.getPropertyIdentifier(), propRef.getPropertyArrayIndex());
            }
            readAccessResults.add(new ReadAccessResult(oid, new SequenceOf<>(results)));
        }

        return new ReadPropertyMultipleAck(new SequenceOf<>(readAccessResults));
    }

    public SequenceOf<ReadAccessSpecification> getListOfReadAccessSpecs() {
        return listOfReadAccessSpecs;
    }

    public int getNumberOfProperties() {
        int sum = 0;
        for (ReadAccessSpecification spec : listOfReadAccessSpecs) {
            sum += spec.getNumberOfProperties();
        }
        return sum;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReadPropertyMultipleRequest that))
            return false;
        return Objects.equals(listOfReadAccessSpecs, that.listOfReadAccessSpecs);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(listOfReadAccessSpecs);
    }

    @Override
    public String toString() {
        return "ReadPropertyMultipleRequest [listOfReadAccessSpecs=" + listOfReadAccessSpecs + "]";
    }

    private static void addProperty(BACnetObject obj, List<Result> results, PropertyIdentifier pid,
            UnsignedInteger pin) {
        if (obj == null) {
            results.add(new Result(pid, pin, new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject)));
        } else if (pid.intValue() == PropertyIdentifier.all.intValue()) {
            // Some object properties can be dynamically created, so we can't just return those properties that are
            // already defined in the object.
            var pids = ObjectProperties.getObjectPropertyTypeDefinitions(obj.getId().getObjectType()).stream()
                    .map(d -> d.getPropertyTypeDefinition().getPropertyIdentifier()).collect(
                            Collectors.toSet());
            // Include properties that are defined in the object to ensure that proprietary properties are returned.
            pids.addAll(obj.getPropertyIds());

            for (PropertyIdentifier op : pids) {
                // Do not add the property list
                if (op != PropertyIdentifier.propertyList) {
                    addNonSpecialProperty(obj, results, op, pin, true);
                }
            }
        } else if (pid.intValue() == PropertyIdentifier.required.intValue()) {
            for (ObjectPropertyTypeDefinition def : ObjectProperties
                    .getRequiredObjectPropertyTypeDefinitions(obj.getId().getObjectType())) {
                // Do not add the property list
                if (def.getPropertyTypeDefinition().getPropertyIdentifier() != PropertyIdentifier.propertyList) {
                    addNonSpecialProperty(obj, results, def.getPropertyTypeDefinition().getPropertyIdentifier(), pin,
                            true);
                }
            }
        } else if (pid.intValue() == PropertyIdentifier.optional.intValue()) {
            for (ObjectPropertyTypeDefinition def : ObjectProperties
                    .getOptionalObjectPropertyTypeDefinitions(obj.getId().getObjectType())) {
                addNonSpecialProperty(obj, results, def.getPropertyTypeDefinition().getPropertyIdentifier(), pin, true);
            }
        } else {
            // Get the specified property.
            addNonSpecialProperty(obj, results, pid, pin, false);
        }
    }

    private static void addNonSpecialProperty(BACnetObject obj, List<Result> results, PropertyIdentifier pid,
            UnsignedInteger pin, boolean ignoreNotFound) {
        try {
            results.add(new Result(pid, pin, obj.readPropertyRequired(pid, pin)));
        } catch (BACnetServiceException e) {
            if (ignoreNotFound && e.getErrorClass() == ErrorClass.property && e.getErrorCode() == ErrorCode.unknownProperty) {
                return;
            }
            results.add(new Result(pid, pin, new ErrorClassAndCode(e.getErrorClass(), e.getErrorCode())));
        }
    }
}
