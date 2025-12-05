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
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.obj.MultistateValueObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.WriteAccessSpecification;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class WritePropertyMultipleRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private LocalDevice localDevice;
    private LocalDevice remoteDevice;
    private final int localDeviceId = 1;
    private final int remoteDeviceId = 2;
    private MultistateValueObject msv0;
    private RemoteDevice remoteDeviceReference;

    @Before
    public void before() throws Exception {
        localDevice = new LocalDevice(localDeviceId, new DefaultTransport(new TestNetwork(map, localDeviceId, 0)))
                .initialize();
        remoteDevice = new LocalDevice(remoteDeviceId, new DefaultTransport(new TestNetwork(map, remoteDeviceId, 0)))
                .initialize();

        msv0 = new MultistateValueObject(remoteDevice, 0, "msv0", 4,
                new BACnetArray<>(new CharacterString("a"), new CharacterString("b"), new CharacterString("c"),
                        new CharacterString("d")),
                1, false);
        msv0.writePropertyInternal(PropertyIdentifier.description, new CharacterString("my description"));

        // Get the local reference to the remove device
        remoteDeviceReference = localDevice.getRemoteDeviceBlocking(remoteDeviceId, 1000);
    }

    @After
    public void after() {
        localDevice.terminate();
        remoteDevice.terminate();
    }

    @Test
    public void writableProperties() throws BACnetException {
        var stateText = new BACnetArray<>(new CharacterString("A"), new CharacterString("B"), new CharacterString("C"),
                new CharacterString("D"));
        var recipients = new SequenceOf<>(new Recipient(TestNetworkUtils.toAddress(21)),
                new Recipient(TestNetworkUtils.toAddress(22)));
        var writeAccessSpecs = new SequenceOf<>(
                new WriteAccessSpecification(msv0.getId(), new SequenceOf<>(
                        // Can write an entire array.
                        new PropertyValue(PropertyIdentifier.stateText, stateText),
                        // Can write a scalar property
                        new PropertyValue(PropertyIdentifier.description, new CharacterString("my new description")),
                        // Can write an element of an array.
                        new PropertyValue(PropertyIdentifier.stateText, new UnsignedInteger(3),
                                new CharacterString("CC"), null),
                        // Can write an entire list.
                        new PropertyValue(PropertyIdentifier.alarmValues,
                                new SequenceOf<>(new UnsignedInteger(11), new UnsignedInteger(12),
                                        new UnsignedInteger(13)))
                )),
                new WriteAccessSpecification(remoteDevice.getId(), new SequenceOf<>(
                        new PropertyValue(PropertyIdentifier.timeSynchronizationRecipients, recipients)
                ))
        );
        localDevice.send(remoteDeviceReference, new WritePropertyMultipleRequest(writeAccessSpecs)).get();


        BACnetArray<CharacterString> newStateText = msv0.get(PropertyIdentifier.stateText);
        assertEquals(4, newStateText.size());
        assertEquals("A", newStateText.getBase1(1).getValue());
        assertEquals("B", newStateText.getBase1(2).getValue());
        assertEquals("CC", newStateText.getBase1(3).getValue());
        assertEquals("D", newStateText.getBase1(4).getValue());
        assertEquals("my new description", msv0.get(PropertyIdentifier.description).toString());
        SequenceOf<UnsignedInteger> newAlarmValues = msv0.get(PropertyIdentifier.alarmValues);
        assertEquals(3, newAlarmValues.size());
        assertEquals(11, newAlarmValues.getBase1(1).intValue());
        assertEquals(12, newAlarmValues.getBase1(2).intValue());
        assertEquals(13, newAlarmValues.getBase1(3).intValue());

        SequenceOf<Recipient> newRecipients = remoteDevice.get(PropertyIdentifier.timeSynchronizationRecipients);
        assertEquals(2, newRecipients.size());
        assertEquals(21, (int) newRecipients.getBase1(1).getAddress().getMacAddress().getBytes()[0]);
        assertEquals(22, (int) newRecipients.getBase1(2).getAddress().getMacAddress().getBytes()[0]);
    }
}
