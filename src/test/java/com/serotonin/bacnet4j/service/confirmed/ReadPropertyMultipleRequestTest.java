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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import java.util.Set;

import com.serotonin.bacnet4j.obj.GroupObject;
import com.serotonin.bacnet4j.obj.NetworkPortObject;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult.Result;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ReadPropertyMultipleRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final Address addr = TestNetworkUtils.toAddress(2);
    private LocalDevice localDevice;
    private GroupObject g0;

    @Before
    public void before() throws Exception {
        localDevice = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        localDevice.initialize();

        g0 = localDevice.addObject(new GroupObject(localDevice, 0, "g0", new SequenceOf<>()));
        g0.writePropertyInternal(PropertyIdentifier.description, new CharacterString("my description"));
    }

    @After
    public void after() {
        localDevice.terminate();
    }

    @Test
    public void allProperties() throws BACnetException {
        g0.writePropertyInternal(PropertyIdentifier.forId(888), new TestProprietary(999, true));

        SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(g0.getId(), PropertyIdentifier.all));
        ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(
                listOfReadAccessSpecs).handle(localDevice, addr);

        List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(1, readAccessResults.size());
        assertEquals(g0.getId(), readAccessResults.get(0).getObjectIdentifier());
        List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        // Compare ignoring order.
        assertEquals(
                Set.of(
                        new Result(PropertyIdentifier.objectType, null, ObjectType.group),
                        new Result(PropertyIdentifier.listOfGroupMembers, null, new SequenceOf<>()),
                        new Result(PropertyIdentifier.presentValue, null, new SequenceOf<>()),
                        new Result(PropertyIdentifier.forId(888), null, new TestProprietary(999, true)),
                        new Result(PropertyIdentifier.objectIdentifier, null, g0.getId()),
                        new Result(PropertyIdentifier.description, null, new CharacterString("my description")),
                        new Result(PropertyIdentifier.objectName, null, new CharacterString("g0"))
                ),
                new HashSet<>(results)
        );
    }

    @Test
    public void requiredProperties() throws BACnetException {
        SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(g0.getId(), PropertyIdentifier.required));
        ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(
                listOfReadAccessSpecs).handle(localDevice, addr);

        List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(1, readAccessResults.size());
        assertEquals(g0.getId(), readAccessResults.get(0).getObjectIdentifier());
        List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        assertEquals(5, results.size());
        assertEquals(new Result(PropertyIdentifier.objectType, null, ObjectType.group), results.get(0));
        assertEquals(new Result(PropertyIdentifier.listOfGroupMembers, null, new SequenceOf<>()), results.get(1));
        assertEquals(new Result(PropertyIdentifier.presentValue, null, new SequenceOf<>()), results.get(2));
        assertEquals(new Result(PropertyIdentifier.objectIdentifier, null, g0.getId()), results.get(3));
        assertEquals(new Result(PropertyIdentifier.objectName, null, new CharacterString("g0")), results.get(4));
    }

    @Test
    public void optionalProperties() throws BACnetException {
        SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(g0.getId(), PropertyIdentifier.optional));
        ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(
                listOfReadAccessSpecs).handle(localDevice, addr);

        List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(1, readAccessResults.size());
        assertEquals(g0.getId(), readAccessResults.get(0).getObjectIdentifier());
        List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        assertEquals(1, results.size());
        assertEquals(new Result(PropertyIdentifier.description, null, new CharacterString("my description")),
                results.get(0));
    }

    /**
     * Per addendum 135-2016bl-2: when a ReadPropertyMultiple request specifies OPTIONAL for an
     * object that has no optional properties present, the response must be a valid ACK carrying
     * a single ReadAccessResult with an empty List of Results. It must not be a NAK or an error
     * response.
     */
    @Test
    public void optionalProperties_objectHasNoOptionalPropertiesPresent_returnsEmptyResults() throws Exception {
        // Create a bare Group object that has none of its type's optional properties set
        // (description, auditLevel, auditableOperations, tags, profileLocation, profileName).
        // The Group constructor doesn't populate any of these.
        GroupObject g1 = localDevice.addObject(new GroupObject(localDevice, 1, "g1", new SequenceOf<>()));

        SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(g1.getId(), PropertyIdentifier.optional));
        ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(
                listOfReadAccessSpecs).handle(localDevice, addr);

        List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(1, readAccessResults.size());
        assertEquals(g1.getId(), readAccessResults.get(0).getObjectIdentifier());
        // Empty list of results — no properties returned, no error results either.
        List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        assertEquals(0, results.size());
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
        SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(g0.getId(),
                        new SequenceOf<>(new PropertyReference(PropertyIdentifier.description),
                                new PropertyReference(PropertyIdentifier.accessDoors))),
                new ReadAccessSpecification(new ObjectIdentifier(ObjectType.analogInput, 0),
                        new SequenceOf<>(new PropertyReference(PropertyIdentifier.description),
                                new PropertyReference(PropertyIdentifier.objectName)))
        );

        ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(
                listOfReadAccessSpecs).handle(localDevice, addr);

        List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(2, readAccessResults.size());
        //spec 0
        assertEquals(g0.getId(), readAccessResults.get(0).getObjectIdentifier());
        List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        assertEquals(2, results.size());
        assertEquals(new Result(PropertyIdentifier.description, null, new CharacterString("my description")),
                results.get(0));
        assertEquals(new Result(PropertyIdentifier.accessDoors, null,
                new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty)), results.get(1));
        //spec 1
        assertEquals(new ObjectIdentifier(ObjectType.analogInput, 0), readAccessResults.get(1).getObjectIdentifier());
        List<Result> results1 = readAccessResults.get(1).getListOfResults().getValues();
        assertEquals(2, results1.size());
        assertEquals(new Result(PropertyIdentifier.description, null,
                new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject)), results1.get(0));
        assertEquals(new Result(PropertyIdentifier.objectName, null,
                new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject)), results1.get(1));
    }

    static class TestProprietary extends BaseType {
        private final UnsignedInteger testUnsigned;
        private final Boolean testBoolean;

        public TestProprietary(int testUnsigned, boolean testBoolean) {
            this(new UnsignedInteger(testUnsigned), Boolean.valueOf(testBoolean));
        }

        public TestProprietary(UnsignedInteger testUnsigned, Boolean testBoolean) {
            this.testUnsigned = testUnsigned;
            this.testBoolean = testBoolean;
        }

        @Override
        public void write(ByteQueue queue) {
            write(queue, testUnsigned, 0);
            write(queue, testBoolean, 1);
        }

        public TestProprietary(ByteQueue queue) throws BACnetException {
            testUnsigned = read(queue, UnsignedInteger.class, 0);
            testBoolean = read(queue, Boolean.class, 1);
        }

        public UnsignedInteger getTestUnsigned() {
            return testUnsigned;
        }

        public Boolean getTestBoolean() {
            return testBoolean;
        }

        @Override
        public String toString() {
            return "TestProprietary{testUnsigned=" + testUnsigned + ", testBoolean=" + testBoolean + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass())
                return false;
            TestProprietary that = (TestProprietary) o;
            return Objects.equals(testUnsigned, that.testUnsigned) && Objects.equals(testBoolean,
                    that.testBoolean);
        }

        @Override
        public int hashCode() {
            return Objects.hash(testUnsigned, testBoolean);
        }
    }

    /**
     * Addendum 135-2016br-8: ReadPropertyMultiple with (NETWORK_PORT, 4194303) shall be treated as
     * the local Network Port object representing the network port through which the request was
     * received.
     */
    @Test
    public void networkPortWildcardInstance() throws Exception {
        NetworkPortObject npo = localDevice.addObject(new NetworkPortObject(localDevice, 42, "port42",
                false, NetworkType.virtual, ProtocolLevel.bacnetApplication, Set.of()));

        SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>(
                new ReadAccessSpecification(
                        new ObjectIdentifier(ObjectType.networkPort, ObjectIdentifier.UNINITIALIZED),
                        PropertyIdentifier.objectName));

        ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck) new ReadPropertyMultipleRequest(listOfReadAccessSpecs)
                .handle(localDevice, addr);

        List<ReadAccessResult> readAccessResults = ack.getListOfReadAccessResults().getValues();
        assertEquals(1, readAccessResults.size());
        assertEquals(npo.getId(), readAccessResults.get(0).getObjectIdentifier());
        List<Result> results = readAccessResults.get(0).getListOfResults().getValues();
        assertEquals(1, results.size());
        assertEquals(new Result(PropertyIdentifier.objectName, null, new CharacterString("port42")),
                results.get(0));
    }
}
