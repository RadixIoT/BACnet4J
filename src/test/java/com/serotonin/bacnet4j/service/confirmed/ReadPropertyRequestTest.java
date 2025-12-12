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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class ReadPropertyRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final Address addr = TestNetworkUtils.toAddress(2);
    private LocalDevice localDevice;

    @Before
    public void before() throws Exception {
        localDevice = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        localDevice.initialize();
    }

    @After
    public void after() {
        localDevice.terminate();
    }

    @Test // 15.5.1.3.1
    public void errorTypes() {
        TestUtils.assertRequestHandleException(
                () -> new ReadPropertyRequest(new ObjectIdentifier(ObjectType.accessDoor, 0),
                        PropertyIdentifier.absenteeLimit).handle(localDevice, addr),
                ErrorClass.object, ErrorCode.unknownObject);

        TestUtils.assertRequestHandleException(
                () -> new ReadPropertyRequest(new ObjectIdentifier(ObjectType.device, 1),
                        PropertyIdentifier.absenteeLimit).handle(localDevice, addr),
                ErrorClass.property, ErrorCode.unknownProperty);

        TestUtils.assertRequestHandleException(
                () -> new ReadPropertyRequest(new ObjectIdentifier(ObjectType.device, 1),
                        PropertyIdentifier.systemStatus, new UnsignedInteger(1)).handle(localDevice, addr),
                ErrorClass.property, ErrorCode.propertyIsNotAnArray);

        TestUtils.assertRequestHandleException(
                () -> new ReadPropertyRequest(new ObjectIdentifier(ObjectType.device, 1), PropertyIdentifier.objectList,
                        new UnsignedInteger(10)).handle(localDevice, addr),
                ErrorClass.property, ErrorCode.invalidArrayIndex);
    }

    @Test // 15.5.2 and standard test 135.1-2013 9.18.1.3
    public void uninitializedDeviceId() throws BACnetException {
        // If this does not throw an error, then it's good.
        ReadPropertyAck ack = (ReadPropertyAck) new ReadPropertyRequest(
                new ObjectIdentifier(ObjectType.device, ObjectIdentifier.UNINITIALIZED),
                PropertyIdentifier.systemStatus)
                .handle(localDevice, addr);

        //The instance number of the localdevice must be sent if a request is made to the instance 0x3FFFFF (unitialized).
        assertEquals(new ObjectIdentifier(ObjectType.device, localDevice.getInstanceNumber()),
                ack.getEventObjectIdentifier());
    }

    @Test
    public void specialProperties() {
        TestUtils
                .assertRequestHandleException(
                        () -> new ReadPropertyRequest(new ObjectIdentifier(ObjectType.device, 4194303),
                                PropertyIdentifier.all).handle(localDevice, addr),
                        ErrorClass.services, ErrorCode.inconsistentParameters);
        TestUtils.assertRequestHandleException(
                () -> new ReadPropertyRequest(new ObjectIdentifier(ObjectType.device, 4194303),
                        PropertyIdentifier.required).handle(localDevice, addr),
                ErrorClass.services, ErrorCode.inconsistentParameters);
        TestUtils.assertRequestHandleException(
                () -> new ReadPropertyRequest(new ObjectIdentifier(ObjectType.device, 4194303),
                        PropertyIdentifier.optional).handle(localDevice, addr),
                ErrorClass.services, ErrorCode.inconsistentParameters);
    }
}
