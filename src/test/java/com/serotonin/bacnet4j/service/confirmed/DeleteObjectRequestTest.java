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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class DeleteObjectRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final Address addr = TestNetworkUtils.toAddress(2);
    private LocalDevice localDevice;

    @Before
    public void before() throws Exception {
        localDevice = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
    }

    @After
    public void after() {
        localDevice.terminate();
    }

    @Test // 15.4.1.3.1
    public void errorTypes() {
        // Ask for an object that doesn't exist.
        TestUtils.assertRequestHandleException( //
                () -> new DeleteObjectRequest(new ObjectIdentifier(ObjectType.accessDoor, 0)).handle(localDevice, addr),
                ErrorClass.object, ErrorCode.unknownObject);
    }

    @Test // 15.4.1.3.1
    public void moreErrorTypes() throws BACnetServiceException {
        // Ask for an object that isn't deletable
        final BACnetObject bo = new BACnetObject(localDevice, ObjectType.accessDoor, 0);
        localDevice.addObject(bo);

        TestUtils.assertRequestHandleException( //
                () -> new DeleteObjectRequest(bo.getId()).handle(localDevice, addr), ErrorClass.object,
                ErrorCode.objectDeletionNotPermitted);
    }

    @Test
    public void delete() throws Exception {
        // Ask for an object that isn't deletable
        final BACnetObject bo = new BACnetObject(localDevice, ObjectType.accessDoor, 0);
        localDevice.addObject(bo);
        bo.setDeletable(true);

        new DeleteObjectRequest(bo.getId()).handle(localDevice, addr);
    }
}
