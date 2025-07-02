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

package com.serotonin.bacnet4j.obj.mixin;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.GroupObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.DiscoveryUtils;

public class ObjectIdAndNameMixinTest {
    private final TestNetworkMap map = new TestNetworkMap();

    @Test
    public void uniqueDeviceName() throws Exception {
        final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        d1.getDeviceObject().writeProperty(null, PropertyIdentifier.objectName,
                new CharacterString("Unique device name"));

        final LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0))).initialize();
        RemoteDevice rd = d2.getRemoteDeviceBlocking(1);
        DiscoveryUtils.getExtendedDeviceInformation(d1, rd);

        TestUtils.assertBACnetServiceException(() -> {
            d2.getDeviceObject().writeProperty(null,
                    new PropertyValue(PropertyIdentifier.objectName, new CharacterString("Unique device name")));
        }, ErrorClass.property, ErrorCode.duplicateName);
    }

    @Test
    public void changeOidObjectType() throws Exception {
        final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        TestUtils.assertBACnetServiceException(() -> {
            d1.getDeviceObject().writeProperty(null,
                    new PropertyValue(PropertyIdentifier.objectIdentifier, new ObjectIdentifier(ObjectType.group, 0)));
        }, ErrorClass.property, ErrorCode.invalidValueInThisState);
    }

    @Test
    public void changeObjectType() throws Exception {
        final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        TestUtils.assertBACnetServiceException(() -> {
            d1.getDeviceObject().writeProperty(null,
                    new PropertyValue(PropertyIdentifier.objectType, ObjectType.group));
        }, ErrorClass.property, ErrorCode.writeAccessDenied);
    }

    @Test
    public void changeInstanceNumber() throws Exception {
        final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final GroupObject go = new GroupObject(d1, 0, "go", new SequenceOf<>());
        go.writeProperty(null,
                new PropertyValue(PropertyIdentifier.objectIdentifier, new ObjectIdentifier(ObjectType.group, 1)));
    }

    @Test
    public void uniqueInstanceNumber() throws Exception {
        final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final GroupObject go0 = new GroupObject(d1, 0, "go0", new SequenceOf<>());
        final GroupObject go1 = new GroupObject(d1, 1, "go1", new SequenceOf<>());
        TestUtils.assertBACnetServiceException(() -> {
            go0.writeProperty(null, new PropertyValue(PropertyIdentifier.objectIdentifier, go1.getId()));
        }, ErrorClass.property, ErrorCode.duplicateObjectId);
    }

    @Test
    public void changeName() throws Exception {
        final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final GroupObject go0 = new GroupObject(d1, 0, "go0", new SequenceOf<>());
        new GroupObject(d1, 1, "go1", new SequenceOf<>());
        go0.writeProperty(null, new PropertyValue(PropertyIdentifier.objectName, new CharacterString("that")));
    }

    @Test
    public void uniqueName() throws Exception {
        final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final GroupObject go0 = new GroupObject(d1, 0, "go0", new SequenceOf<>());
        new GroupObject(d1, 1, "go1", new SequenceOf<>());
        TestUtils.assertBACnetServiceException(() -> {
            go0.writeProperty(null, new PropertyValue(PropertyIdentifier.objectName, new CharacterString("go1")));
        }, ErrorClass.property, ErrorCode.duplicateName);
    }
}
