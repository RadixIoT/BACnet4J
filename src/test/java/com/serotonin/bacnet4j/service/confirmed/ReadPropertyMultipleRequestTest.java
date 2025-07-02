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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.obj.GroupObject;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult.Result;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class ReadPropertyMultipleRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final Address addr = TestNetworkUtils.toAddress(2);
    private LocalDevice localDevice;
    private GroupObject g0;

    @Before
    public void before() throws Exception {
        localDevice = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        localDevice.initialize();

        g0 = new GroupObject(localDevice, 0, "g0", new SequenceOf<>());
        g0.writePropertyInternal(PropertyIdentifier.description, new CharacterString("my description"));
    }

    @After
    public void after() {
        localDevice.terminate();
    }

    @Test
    public void allProperties() throws BACnetException {
        final SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(g0.getId(), PropertyIdentifier.all));
        final ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(
                listOfReadAccessSpecs).handle(localDevice, addr);

        final List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(1, readAccessResults.size());
        assertEquals(g0.getId(), readAccessResults.get(0).getObjectIdentifier());
        final List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        assertEquals(6, results.size());
        assertEquals(new Result(PropertyIdentifier.objectType, null, ObjectType.group), results.get(0));
        assertEquals(new Result(PropertyIdentifier.listOfGroupMembers, null, new SequenceOf<>()), results.get(1));
        assertEquals(new Result(PropertyIdentifier.presentValue, null, new SequenceOf<>()), results.get(2));
        assertEquals(new Result(PropertyIdentifier.objectIdentifier, null, g0.getId()), results.get(3));
        assertEquals(new Result(PropertyIdentifier.description, null, new CharacterString("my description")),
                results.get(4));
        assertEquals(new Result(PropertyIdentifier.objectName, null, new CharacterString("g0")), results.get(5));
    }

    @Test
    public void requiredProperties() throws BACnetException {
        final SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(g0.getId(), PropertyIdentifier.required));
        final ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(
                listOfReadAccessSpecs).handle(localDevice, addr);

        final List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(1, readAccessResults.size());
        assertEquals(g0.getId(), readAccessResults.get(0).getObjectIdentifier());
        final List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        assertEquals(5, results.size());
        assertEquals(new Result(PropertyIdentifier.objectType, null, ObjectType.group), results.get(0));
        assertEquals(new Result(PropertyIdentifier.listOfGroupMembers, null, new SequenceOf<>()), results.get(1));
        assertEquals(new Result(PropertyIdentifier.presentValue, null, new SequenceOf<>()), results.get(2));
        assertEquals(new Result(PropertyIdentifier.objectIdentifier, null, g0.getId()), results.get(3));
        assertEquals(new Result(PropertyIdentifier.objectName, null, new CharacterString("g0")), results.get(4));
    }

    @Test
    public void optionalProperties() throws BACnetException {
        final SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(g0.getId(), PropertyIdentifier.optional));
        final ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(
                listOfReadAccessSpecs).handle(localDevice, addr);

        final List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(1, readAccessResults.size());
        assertEquals(g0.getId(), readAccessResults.get(0).getObjectIdentifier());
        final List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        assertEquals(1, results.size());
        assertEquals(new Result(PropertyIdentifier.description, null, new CharacterString("my description")),
                results.get(0));
    }

    @Test // 15.7.2 and standard test 135.1-2013 9.18.1.3
    public void uninitializedDeviceId() throws BACnetException {
        SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(new ObjectIdentifier(ObjectType.device, ObjectIdentifier.UNINITIALIZED),
                        PropertyIdentifier.vendorIdentifier));

        ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(listOfReadAccessSpecs)
                .handle(localDevice, addr);

        //The instance number of the localdevice must be sent if a request is made to the instance 0x3FFFFF (unitialized).
        for (ReadAccessResult listOfReadAccessResult : ack.getListOfReadAccessResults()) {
            assertEquals(new ObjectIdentifier(ObjectType.device, localDevice.getInstanceNumber()),
                    listOfReadAccessResult.getObjectIdentifier());
        }
    }

    @Test // BTL Test 9.20.1.6
    public void partialErrorProperties() throws BACnetException {
        //Property "description" exist in groupobject
        //Property "accessDoors" does not exist in groupobject
        //Object "analogInput" does not exist in device
        final SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(g0.getId(),
                        new SequenceOf<>(new PropertyReference(PropertyIdentifier.description),
                                new PropertyReference(PropertyIdentifier.accessDoors))),
                new ReadAccessSpecification(new ObjectIdentifier(ObjectType.analogInput, 0),
                        new SequenceOf<>(new PropertyReference(PropertyIdentifier.description),
                                new PropertyReference(PropertyIdentifier.objectName)))
        );

        final ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(
                listOfReadAccessSpecs).handle(localDevice, addr);

        final List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(2, readAccessResults.size());
        //spec 0
        assertEquals(g0.getId(), readAccessResults.get(0).getObjectIdentifier());
        final List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        assertEquals(2, results.size());
        assertEquals(new Result(PropertyIdentifier.description, null, new CharacterString("my description")),
                results.get(0));
        assertEquals(new Result(PropertyIdentifier.accessDoors, null,
                new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty)), results.get(1));
        //spec 1
        assertEquals(new ObjectIdentifier(ObjectType.analogInput, 0), readAccessResults.get(1).getObjectIdentifier());
        final List<Result> results1 = readAccessResults.get(1).getListOfResults().getValues();
        assertEquals(2, results1.size());
        assertEquals(new Result(PropertyIdentifier.description, null,
                new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject)), results1.get(0));
        assertEquals(new Result(PropertyIdentifier.objectName, null,
                new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject)), results1.get(1));
    }

}
