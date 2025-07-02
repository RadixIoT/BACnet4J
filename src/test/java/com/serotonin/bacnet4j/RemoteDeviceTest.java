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

package com.serotonin.bacnet4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class RemoteDeviceTest {
    private final TestNetworkMap map = new TestNetworkMap();

    @Test
    public void nonsequenceProperties() {
        final LocalDevice d = new LocalDevice(1234, new DefaultTransport(new TestNetwork(map, 1, 10)));
        final RemoteDevice rd = new RemoteDevice(d, 1235);
        final ObjectIdentifier oid = new ObjectIdentifier(ObjectType.binaryValue, 0);

        assertNull(rd.getDeviceProperty(PropertyIdentifier.modelName));
        assertNull(rd.getObjectProperty(oid, PropertyIdentifier.activeText));
        assertNull(rd.getObjectProperty(oid, PropertyIdentifier.presentValue));

        rd.setDeviceProperty(PropertyIdentifier.modelName, new CharacterString("The model name"));
        rd.setObjectProperty(oid, PropertyIdentifier.activeText, new CharacterString("The active text"));
        rd.setObjectProperty(oid, PropertyIdentifier.presentValue, BinaryPV.active);

        assertEquals(new CharacterString("The model name"), rd.getDeviceProperty(PropertyIdentifier.modelName));
        assertEquals(new CharacterString("The active text"), rd.getObjectProperty(oid, PropertyIdentifier.activeText));
        // Present value is not cached.
        assertNull(rd.getObjectProperty(oid, PropertyIdentifier.presentValue));

        rd.removeDeviceProperty(PropertyIdentifier.modelName);
        rd.removeObjectProperty(oid, PropertyIdentifier.activeText);
        rd.removeObjectProperty(oid, PropertyIdentifier.presentValue);

        assertNull(rd.getDeviceProperty(PropertyIdentifier.modelName));
        assertNull(rd.getObjectProperty(oid, PropertyIdentifier.activeText));
        assertNull(rd.getObjectProperty(oid, PropertyIdentifier.presentValue));
    }

    public void sequenceProperties() {
        final LocalDevice d = new LocalDevice(1234, new DefaultTransport(new TestNetwork(map, 1, 10)));
        final RemoteDevice rd = new RemoteDevice(d, 1235);
        final ObjectIdentifier ai1 = new ObjectIdentifier(ObjectType.analogInput, 0);
        final ObjectIdentifier ai2 = new ObjectIdentifier(ObjectType.analogInput, 1);
        final ObjectIdentifier ai3 = new ObjectIdentifier(ObjectType.analogInput, 2);
        final ObjectIdentifier ai4 = new ObjectIdentifier(ObjectType.analogInput, 3);
        final ObjectIdentifier ai5 = new ObjectIdentifier(ObjectType.analogInput, 4);

        assertNull(rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(3)));
        assertNull(rd.getDeviceProperty(PropertyIdentifier.objectList, UnsignedInteger.ZERO));

        rd.setDeviceProperty(PropertyIdentifier.objectList, UnsignedInteger.ZERO, new UnsignedInteger(5));
        rd.setDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(3), ai3);

        assertNull(rd.getDeviceProperty(PropertyIdentifier.objectList, UnsignedInteger.ZERO));
        assertEquals(ai3, rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(3)));

        final SequenceOf<ObjectIdentifier> objectList = rd.getDeviceProperty(PropertyIdentifier.objectList);
        assertEquals(3, objectList.getCount());
        assertNull(objectList.getBase1(1));
        assertNull(objectList.getBase1(2));
        assertEquals(ai3, objectList.getBase1(3));

        rd.setDeviceProperty(PropertyIdentifier.objectList, new SequenceOf<>(ai1, ai2, ai3, ai4, ai5));

        assertEquals(ai1, rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(1)));
        assertEquals(ai2, rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(2)));
        assertEquals(ai3, rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(3)));
        assertEquals(ai4, rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(4)));
        assertEquals(ai5, rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(5)));

        assertEquals(ai2, rd.removeDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(2)));
        assertEquals(ai4, rd.removeDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(4)));

        assertEquals(ai1, rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(1)));
        assertNull(rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(2)));
        assertEquals(ai3, rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(3)));
        assertNull(rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(4)));
        assertEquals(ai5, rd.getDeviceProperty(PropertyIdentifier.objectList, new UnsignedInteger(5)));
    }
}
