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
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.AnalogValueObject;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.PriorityArray;
import com.serotonin.bacnet4j.type.constructed.PriorityValue;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class ReadPriorityArrayTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private LocalDevice localDevice;
    private LocalDevice remoteDevice;
    private final int localDeviceId = 1;
    private final int remoteDeviceId = 2;
    private PriorityArray priorityArray;
    private RemoteDevice rDevice;

    @Before
    public void before() throws Exception {
        localDevice = new LocalDevice(localDeviceId, new DefaultTransport(new TestNetwork(map, localDeviceId, 0)))
                .initialize();
        remoteDevice = new LocalDevice(remoteDeviceId, new DefaultTransport(new TestNetwork(map, remoteDeviceId, 0)))
                .initialize();

        //Add Objects and properties
        priorityArray = new PriorityArray().put(1, new UnsignedInteger(11111)).put(2, new UnsignedInteger(22222)).put(3,
                new UnsignedInteger(33333));
        final AnalogValueObject analogValueObject = new AnalogValueObject(remoteDevice, 1, "analogValueOne", 77.7f,
                EngineeringUnits.degreesFahrenheit, false);
        analogValueObject.writePropertyInternal(PropertyIdentifier.priorityArray, priorityArray);

        //Search Remotedevice
        rDevice = localDevice.getRemoteDeviceBlocking(remoteDeviceId, 1000);
    }

    @After
    public void after() {
        localDevice.terminate();
        remoteDevice.terminate();
    }

    @Test
    public void readPriorityArrayCompletely() throws BACnetException {
        final ReadPropertyRequest req = new ReadPropertyRequest(new ObjectIdentifier(ObjectType.analogValue, 1),
                PropertyIdentifier.priorityArray);
        final ReadPropertyAck ack = localDevice.send(rDevice, req).get();
        //Check the Class
        assertEquals(PriorityArray.class, ack.getValue().getClass());
        assertEquals(priorityArray.toString(), ack.getValue().toString());
    }

    @Test
    public void readPriorityArraySize() throws BACnetException {
        final ReadPropertyRequest req = new ReadPropertyRequest(new ObjectIdentifier(ObjectType.analogValue, 1),
                PropertyIdentifier.priorityArray, new UnsignedInteger(0)); //Reading size going wrong
        final ReadPropertyAck ack = localDevice.send(rDevice, req).get();
        //Check the Class
        assertEquals(UnsignedInteger.class, ack.getValue().getClass());
        //Check the Size
        assertEquals("16", ack.getValue().toString());
    }

    @Test
    public void readPriorityArrayElement() throws BACnetException {
        final ReadPropertyRequest req = new ReadPropertyRequest(new ObjectIdentifier(ObjectType.analogValue, 1),
                PropertyIdentifier.priorityArray, new UnsignedInteger(3)); //Reading element going wrong
        final ReadPropertyAck ack = localDevice.send(rDevice, req).get();
        //Check the Class
        assertEquals(PriorityValue.class, ack.getValue().getClass());
        //Check the Size
        assertEquals("PriorityValue(33333)", ack.getValue().toString());
    }
}
