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

package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class GroupObjectTest extends AbstractTest {
    @Test
    public void preventGroupReferences() throws Exception {
        final GroupObject g = new GroupObject(d1, 0, "g", new SequenceOf<>());

        // Make sure groups cannot be added.
        TestUtils.assertBACnetServiceException(() -> {
            g.writeProperty(null,
                    new PropertyValue(PropertyIdentifier.listOfGroupMembers,
                            new SequenceOf<>(
                                    new ReadAccessSpecification(new ObjectIdentifier(ObjectType.analogInput, 0),
                                            PropertyIdentifier.presentValue),
                                    new ReadAccessSpecification(new ObjectIdentifier(ObjectType.group, 0),
                                            PropertyIdentifier.presentValue),
                                    new ReadAccessSpecification(new ObjectIdentifier(ObjectType.trendLog, 0),
                                            PropertyIdentifier.action))));
        }, ErrorClass.object, ErrorCode.inconsistentObjectType);

        // Make sure global groups cannot be added.
        TestUtils.assertBACnetServiceException(() -> {
            g.writeProperty(null,
                    new PropertyValue(PropertyIdentifier.listOfGroupMembers,
                            new SequenceOf<>(
                                    new ReadAccessSpecification(new ObjectIdentifier(ObjectType.analogInput, 0),
                                            PropertyIdentifier.presentValue),
                                    new ReadAccessSpecification(new ObjectIdentifier(ObjectType.globalGroup, 0),
                                            PropertyIdentifier.presentValue))));
        }, ErrorClass.object, ErrorCode.inconsistentObjectType);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void readPresentValue() throws Exception {
        // Create some objects to read.
        final AnalogInputObject ai = new AnalogInputObject(d1, 0, "ai", 0, EngineeringUnits.noUnits, false);
        final BinaryValueObject bv = new BinaryValueObject(d1, 0, "bv", BinaryPV.inactive, false);
        final MultistateValueObject mv = new MultistateValueObject(d1, 0, "mv", 4,
                new BACnetArray<>(new CharacterString("Off"), new CharacterString("On"), new CharacterString("Auto"),
                        new CharacterString("Optional")),
                1, false);

        // Create the group
        final GroupObject g = new GroupObject(d1, 0, "g",
                new SequenceOf<>( //
                        new ReadAccessSpecification(ai.getId(),
                                new SequenceOf<>( //
                                        new PropertyReference(PropertyIdentifier.presentValue), //
                                        new PropertyReference(PropertyIdentifier.units), //
                                        new PropertyReference(PropertyIdentifier.stateText))), //
                        new ReadAccessSpecification(bv.getId(),
                                new SequenceOf<>( //
                                        new PropertyReference(PropertyIdentifier.presentValue), //
                                        new PropertyReference(PropertyIdentifier.activeText), //
                                        new PropertyReference(PropertyIdentifier.inactiveText))), //
                        new ReadAccessSpecification(mv.getId(),
                                new SequenceOf<>( //
                                        new PropertyReference(PropertyIdentifier.presentValue), //
                                        new PropertyReference(PropertyIdentifier.stateText, new UnsignedInteger(2)), //
                                        new PropertyReference(PropertyIdentifier.stateText, new UnsignedInteger(10)))),
                        //
                        new ReadAccessSpecification(new ObjectIdentifier(ObjectType.accumulator, 0),
                                PropertyIdentifier.presentValue)));

        SequenceOf<ReadAccessResult> presentValue = (SequenceOf<ReadAccessResult>) g
                .readProperty(PropertyIdentifier.presentValue, null);
        assertEquals(4, presentValue.size());

        assertEquals(ai.getId(), presentValue.get(0).getObjectIdentifier());
        assertEquals(3, presentValue.get(0).getListOfResults().size());
        assertEquals(PropertyIdentifier.presentValue,
                presentValue.get(0).getListOfResults().get(0).getPropertyIdentifier());
        assertEquals(null, presentValue.get(0).getListOfResults().get(0).getPropertyArrayIndex());
        assertEquals(new Real(0), presentValue.get(0).getListOfResults().get(0).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.units, presentValue.get(0).getListOfResults().get(1).getPropertyIdentifier());
        assertEquals(null, presentValue.get(0).getListOfResults().get(1).getPropertyArrayIndex());
        assertEquals(EngineeringUnits.noUnits,
                presentValue.get(0).getListOfResults().get(1).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.stateText,
                presentValue.get(0).getListOfResults().get(2).getPropertyIdentifier());
        assertEquals(null, presentValue.get(0).getListOfResults().get(2).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty),
                presentValue.get(0).getListOfResults().get(2).getReadResult().getDatum());

        assertEquals(bv.getId(), presentValue.get(1).getObjectIdentifier());
        assertEquals(3, presentValue.get(1).getListOfResults().size());
        assertEquals(PropertyIdentifier.presentValue,
                presentValue.get(1).getListOfResults().get(0).getPropertyIdentifier());
        assertEquals(null, presentValue.get(1).getListOfResults().get(0).getPropertyArrayIndex());
        assertEquals(BinaryPV.inactive, presentValue.get(1).getListOfResults().get(0).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.activeText,
                presentValue.get(1).getListOfResults().get(1).getPropertyIdentifier());
        assertEquals(null, presentValue.get(1).getListOfResults().get(1).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty),
                presentValue.get(1).getListOfResults().get(1).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.inactiveText,
                presentValue.get(1).getListOfResults().get(2).getPropertyIdentifier());
        assertEquals(null, presentValue.get(1).getListOfResults().get(2).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty),
                presentValue.get(1).getListOfResults().get(2).getReadResult().getDatum());

        assertEquals(mv.getId(), presentValue.get(2).getObjectIdentifier());
        assertEquals(3, presentValue.get(2).getListOfResults().size());
        assertEquals(PropertyIdentifier.presentValue,
                presentValue.get(2).getListOfResults().get(0).getPropertyIdentifier());
        assertEquals(null, presentValue.get(2).getListOfResults().get(0).getPropertyArrayIndex());
        assertEquals(new UnsignedInteger(1), presentValue.get(2).getListOfResults().get(0).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.stateText,
                presentValue.get(2).getListOfResults().get(1).getPropertyIdentifier());
        assertEquals(new UnsignedInteger(2), presentValue.get(2).getListOfResults().get(1).getPropertyArrayIndex());
        assertEquals(new CharacterString("On"),
                presentValue.get(2).getListOfResults().get(1).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.stateText,
                presentValue.get(2).getListOfResults().get(2).getPropertyIdentifier());
        assertEquals(new UnsignedInteger(10), presentValue.get(2).getListOfResults().get(2).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.invalidArrayIndex),
                presentValue.get(2).getListOfResults().get(2).getReadResult().getDatum());

        assertEquals(new ObjectIdentifier(ObjectType.accumulator, 0), presentValue.get(3).getObjectIdentifier());
        assertEquals(1, presentValue.get(3).getListOfResults().size());
        assertEquals(PropertyIdentifier.presentValue,
                presentValue.get(3).getListOfResults().get(0).getPropertyIdentifier());
        assertEquals(null, presentValue.get(3).getListOfResults().get(0).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject),
                presentValue.get(3).getListOfResults().get(0).getReadResult().getDatum());

        //
        // Update the object values and read again.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(3.14F));
        bv.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        mv.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(4));

        presentValue = (SequenceOf<ReadAccessResult>) g.readProperty(PropertyIdentifier.presentValue, null);
        assertEquals(4, presentValue.size());

        assertEquals(ai.getId(), presentValue.get(0).getObjectIdentifier());
        assertEquals(3, presentValue.get(0).getListOfResults().size());
        assertEquals(PropertyIdentifier.presentValue,
                presentValue.get(0).getListOfResults().get(0).getPropertyIdentifier());
        assertEquals(null, presentValue.get(0).getListOfResults().get(0).getPropertyArrayIndex());
        assertEquals(new Real(3.14F), presentValue.get(0).getListOfResults().get(0).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.units, presentValue.get(0).getListOfResults().get(1).getPropertyIdentifier());
        assertEquals(null, presentValue.get(0).getListOfResults().get(1).getPropertyArrayIndex());
        assertEquals(EngineeringUnits.noUnits,
                presentValue.get(0).getListOfResults().get(1).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.stateText,
                presentValue.get(0).getListOfResults().get(2).getPropertyIdentifier());
        assertEquals(null, presentValue.get(0).getListOfResults().get(2).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty),
                presentValue.get(0).getListOfResults().get(2).getReadResult().getDatum());

        assertEquals(bv.getId(), presentValue.get(1).getObjectIdentifier());
        assertEquals(3, presentValue.get(1).getListOfResults().size());
        assertEquals(PropertyIdentifier.presentValue,
                presentValue.get(1).getListOfResults().get(0).getPropertyIdentifier());
        assertEquals(null, presentValue.get(1).getListOfResults().get(0).getPropertyArrayIndex());
        assertEquals(BinaryPV.active, presentValue.get(1).getListOfResults().get(0).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.activeText,
                presentValue.get(1).getListOfResults().get(1).getPropertyIdentifier());
        assertEquals(null, presentValue.get(1).getListOfResults().get(1).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty),
                presentValue.get(1).getListOfResults().get(1).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.inactiveText,
                presentValue.get(1).getListOfResults().get(2).getPropertyIdentifier());
        assertEquals(null, presentValue.get(1).getListOfResults().get(2).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty),
                presentValue.get(1).getListOfResults().get(2).getReadResult().getDatum());

        assertEquals(mv.getId(), presentValue.get(2).getObjectIdentifier());
        assertEquals(3, presentValue.get(2).getListOfResults().size());
        assertEquals(PropertyIdentifier.presentValue,
                presentValue.get(2).getListOfResults().get(0).getPropertyIdentifier());
        assertEquals(null, presentValue.get(2).getListOfResults().get(0).getPropertyArrayIndex());
        assertEquals(new UnsignedInteger(4), presentValue.get(2).getListOfResults().get(0).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.stateText,
                presentValue.get(2).getListOfResults().get(1).getPropertyIdentifier());
        assertEquals(new UnsignedInteger(2), presentValue.get(2).getListOfResults().get(1).getPropertyArrayIndex());
        assertEquals(new CharacterString("On"),
                presentValue.get(2).getListOfResults().get(1).getReadResult().getDatum());
        assertEquals(PropertyIdentifier.stateText,
                presentValue.get(2).getListOfResults().get(2).getPropertyIdentifier());
        assertEquals(new UnsignedInteger(10), presentValue.get(2).getListOfResults().get(2).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.invalidArrayIndex),
                presentValue.get(2).getListOfResults().get(2).getReadResult().getDatum());

        assertEquals(new ObjectIdentifier(ObjectType.accumulator, 0), presentValue.get(3).getObjectIdentifier());
        assertEquals(1, presentValue.get(3).getListOfResults().size());
        assertEquals(PropertyIdentifier.presentValue,
                presentValue.get(3).getListOfResults().get(0).getPropertyIdentifier());
        assertEquals(null, presentValue.get(3).getListOfResults().get(0).getPropertyArrayIndex());
        assertEquals(new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject),
                presentValue.get(3).getListOfResults().get(0).getReadResult().getDatum());
    }
}
