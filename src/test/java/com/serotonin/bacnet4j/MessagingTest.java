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

import static com.serotonin.bacnet4j.TestUtils.assertListEqualsIgnoreOrder;
import static com.serotonin.bacnet4j.TestUtils.awaitTrue;
import static com.serotonin.bacnet4j.TestUtils.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.AbortAPDUException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.exception.ErrorAPDUException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.NetworkSourceAddress;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult.Result;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.WriteAccessSpecification;
import com.serotonin.bacnet4j.type.enumerated.AbortReason;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Primarily this is a test of the DefaultTransport, but also tests aspects of Network and LocalDevice.
 */
public class MessagingTest {
    private TestNetworkMap map;

    @Before
    public void before() {
        map = new TestNetworkMap();
    }

    @Test
    public void networkTest() throws Exception {
        TestNetwork network1 = new TestNetwork(map, 1, 200);
        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(network1));

        MutableObject<RemoteDevice> o = new MutableObject<>();
        d1.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void iAmReceived(RemoteDevice d) {
                o.setValue(d);
            }
        });
        d1.initialize();

        Address a2 = new NetworkSourceAddress(Address.LOCAL_NETWORK, new byte[] {2});
        TestNetwork network2 = new TestNetwork(map, a2, 200);
        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(network2));
        d2.initialize();

        d1.sendLocalBroadcast(new WhoIsRequest());

        awaitTrue(() -> o.get() != null);

        d1.terminate();
        d2.terminate();

        assertEquals(a2, o.get().getAddress());
    }

    @Test
    public void readRequest() throws Exception {
        // Create the first local device.
        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 200)));
        d1.initialize();

        // Create the second local device.
        LocalDevice d2 =
                new LocalDevice(2, new DefaultTransport(new TestNetwork(map, new Address(new byte[] {2}), 200)));
        createAnalogValue(d2, 0);
        d2.initialize();

        d1.sendGlobalBroadcast(d1.getIAm());
        d2.sendGlobalBroadcast(d2.getIAm());

        // Create the remote proxy for device 2.
        RemoteDevice r2 = d1.getRemoteDevice(2).get();

        r2.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        ServicesSupported ss = new ServicesSupported();
        ss.setAll(true);
        r2.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, ss);
        r2.setDeviceProperty(PropertyIdentifier.maxApduLengthAccepted, MaxApduLength.UP_TO_1476.getMaxLength());

        // Send an object list request from the first to the second.
        List<ReadAccessSpecification> specs = new ArrayList<>();
        specs.add(
                new ReadAccessSpecification(new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.objectList));
        ServiceFuture future = d1.send(r2, new ReadPropertyMultipleRequest(new SequenceOf<>(specs)));
        ReadPropertyMultipleAck ack = future.get();

        assertEquals(1, ack.getListOfReadAccessResults().getCount());
        ReadAccessResult readResult = ack.getListOfReadAccessResults().getBase1(1);
        assertEquals(d2.getId(), readResult.getObjectIdentifier());
        assertEquals(1, readResult.getListOfResults().getCount());
        Result result = readResult.getListOfResults().getBase1(1);
        assertEquals(PropertyIdentifier.objectList, result.getPropertyIdentifier());
        SequenceOf<ObjectIdentifier> idList = result.getReadResult().getDatum();
        assertEquals(2, idList.getCount());
        assertEquals(d2.getId(), idList.getBase1(1));
        assertEquals(new ObjectIdentifier(ObjectType.analogValue, 0), idList.getBase1(2));

        // Send the same request, but with a null consumer.
        d1.send(r2, new ReadPropertyMultipleRequest(new SequenceOf<>(specs)), null);

        d1.terminate();
        d2.terminate();
    }

    @Test
    public void segmentedResponse() throws Exception {
        // Create the first local device.
        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 200)));
        d1.initialize();

        // Create the second local device.
        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 200)));
        for (int i = 0; i < 1000; i++)
            createAnalogValue(d2, i);
        d2.initialize();

        d1.sendGlobalBroadcast(d1.getIAm());
        d2.sendGlobalBroadcast(d2.getIAm());

        // Create the remote proxy for device 2.
        RemoteDevice r2 = d1.getRemoteDevice(2).get();
        r2.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        ServicesSupported ss = new ServicesSupported();
        ss.setAll(true);
        r2.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, ss);
        r2.setDeviceProperty(PropertyIdentifier.maxApduLengthAccepted, MaxApduLength.UP_TO_1476.getMaxLength());

        // Send an object list request from the first to the second.
        List<ReadAccessSpecification> specs = new ArrayList<>();
        specs.add(
                new ReadAccessSpecification(new ObjectIdentifier(ObjectType.device, 2), PropertyIdentifier.objectList));
        ServiceFuture future = d1.send(r2, new ReadPropertyMultipleRequest(new SequenceOf<>(specs)));
        ReadPropertyMultipleAck ack = future.get();

        assertEquals(1, ack.getListOfReadAccessResults().getCount());
        ReadAccessResult readResult = ack.getListOfReadAccessResults().getBase1(1);
        assertEquals(d2.getId(), readResult.getObjectIdentifier());
        assertEquals(1, readResult.getListOfResults().getCount());
        Result result = readResult.getListOfResults().getBase1(1);
        assertEquals(PropertyIdentifier.objectList, result.getPropertyIdentifier());
        SequenceOf<ObjectIdentifier> idList = result.getReadResult().getDatum();
        assertEquals(1001, idList.getCount());
        assertEquals(d2.getId(), idList.getBase1(1));

        // Send the same request, but with a null consumer.
        d1.send(r2, new ReadPropertyMultipleRequest(new SequenceOf<>(specs)), null);

        d1.terminate();
        d2.terminate();
    }

    @Test
    public void writeRequest() throws Exception {
        // Create the first local device.
        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 20)));
        d1.initialize();

        // Create the second local device.
        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 30)));
        ObjectIdentifier av0 = new ObjectIdentifier(ObjectType.analogValue, 0);
        createAnalogValue(d2, 0);
        d2.initialize();

        d1.sendGlobalBroadcast(d1.getIAm());
        d2.sendGlobalBroadcast(d2.getIAm());

        // Create the remote proxy for device 2.
        RemoteDevice r2 = d1.getRemoteDevice(2).get();
        r2.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        ServicesSupported ss = new ServicesSupported();
        ss.setAll(true);
        r2.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, ss);
        r2.setDeviceProperty(PropertyIdentifier.maxApduLengthAccepted, MaxApduLength.UP_TO_1476.getMaxLength());

        // Send a write request from the first to the second.
        d1.send(r2, new WritePropertyRequest(av0, PropertyIdentifier.presentValue, null, new Real(3.14F), null));

        ServiceFuture future = d1.send(r2, new ReadPropertyRequest(av0, PropertyIdentifier.presentValue));
        ReadPropertyAck ack = future.get();

        assertEquals(av0, ack.getEventObjectIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(PropertyIdentifier.presentValue, ack.getPropertyIdentifier());
        assertEquals(new Real(3.14F), ack.getValue());

        // Send the same request, but with a null consumer.
        d1.send(r2, new ReadPropertyRequest(av0, PropertyIdentifier.presentValue), null);

        d1.terminate();
        d2.terminate();
    }

    @Test
    public void segmentedRequest() throws Exception {
        // Create the first local device.
        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 20)));
        d1.initialize();

        // Create the second local device.
        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 25)));
        for (int i = 0; i < 1000; i++)
            createAnalogValue(d2, i);
        d2.initialize();

        d1.sendGlobalBroadcast(d1.getIAm());
        d2.sendGlobalBroadcast(d2.getIAm());

        // Create the remote proxy for device 2.
        RemoteDevice r2 = d1.getRemoteDevice(2).get();
        r2.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        ServicesSupported ss = new ServicesSupported();
        ss.setAll(true);
        r2.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, ss);
        r2.setDeviceProperty(PropertyIdentifier.maxApduLengthAccepted, MaxApduLength.UP_TO_1476.getMaxLength());

        // Create a write multiple request
        List<PropertyValue> propertyValues = new ArrayList<>();
        propertyValues.add(new PropertyValue(PropertyIdentifier.presentValue, new Real(2.28F)));
        propertyValues.add(new PropertyValue(PropertyIdentifier.units, EngineeringUnits.btus));
        List<WriteAccessSpecification> specs = new ArrayList<>();
        for (int i = 0; i < 1000; i++)
            specs.add(new WriteAccessSpecification(new ObjectIdentifier(ObjectType.analogValue, i),
                    new SequenceOf<>(propertyValues)));

        // Send the request and wait for the response.
        d1.send(r2, new WritePropertyMultipleRequest(new SequenceOf<>(specs))).get();

        // Send the same request, but with a null consumer.
        d1.send(r2, new WritePropertyMultipleRequest(new SequenceOf<>(specs)), null);

        // Read one of the just-written values and verify.
        ReadPropertyAck ack = d1.send(r2,
                        new ReadPropertyRequest(new ObjectIdentifier(ObjectType.analogValue, 567), PropertyIdentifier.units))
                .get();

        assertEquals(new ObjectIdentifier(ObjectType.analogValue, 567), ack.getEventObjectIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(PropertyIdentifier.units, ack.getPropertyIdentifier());
        assertEquals(EngineeringUnits.btus, ack.getValue());

        d1.terminate();
        d2.terminate();
    }

    /**
     * Sequence numbers are modulo 256, so a segmented message may be longer than 256 segments. Sending the request
     * to a device that accepts only 50 octet APDUs forces enough segments for the sequence numbers to wrap. Before
     * the segmentation rewrite this stalled at the wrap and timed out.
     */
    @Test
    public void segmentedRequestLongerThanTheSequenceNumberSpace() throws Exception {
        int objectCount = 600;

        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        d1.initialize();

        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0)));
        for (int i = 0; i < objectCount; i++)
            createAnalogValue(d2, i);
        d2.initialize();
        // Have the second device accept only short APDUs, so that the request needs more than 256 segments. This is
        // what its I-Am advertises, and so what the first device uses when it segments.
        d2.getDeviceObject().writePropertyInternal(PropertyIdentifier.maxApduLengthAccepted,
                MaxApduLength.UP_TO_50.getMaxLength());

        d1.sendGlobalBroadcast(d1.getIAm());
        d2.sendGlobalBroadcast(d2.getIAm());

        RemoteDevice r2 = d1.getRemoteDevice(2).get();
        r2.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        ServicesSupported ss = new ServicesSupported();
        ss.setAll(true);
        r2.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, ss);

        List<PropertyValue> propertyValues = new ArrayList<>();
        propertyValues.add(new PropertyValue(PropertyIdentifier.presentValue, new Real(2.28F)));
        propertyValues.add(new PropertyValue(PropertyIdentifier.units, EngineeringUnits.btus));
        List<WriteAccessSpecification> specs = new ArrayList<>();
        for (int i = 0; i < objectCount; i++)
            specs.add(new WriteAccessSpecification(new ObjectIdentifier(ObjectType.analogValue, i),
                    new SequenceOf<>(propertyValues)));

        assertEquals(MaxApduLength.UP_TO_50.getMaxLengthInt(), r2.getMaxAPDULengthAccepted());
        d1.send(r2, new WritePropertyMultipleRequest(new SequenceOf<>(specs))).get();

        // Read one of the just-written values to confirm the whole request was received and applied.
        ReadPropertyAck ack = d1.send(r2, new ReadPropertyRequest(
                new ObjectIdentifier(ObjectType.analogValue, objectCount - 1), PropertyIdentifier.units)).get();
        assertEquals(EngineeringUnits.btus, ack.getValue());

        d1.terminate();
        d2.terminate();
    }

    /**
     * Writes a single long character string, which has to be segmented on the way out and again on the way back, and
     * compares it with what was sent.
     * <p>
     * The other segmentation tests establish that a transfer completes; this one establishes that it reassembles
     * faithfully. Because the value is one contiguous string rather than many independent elements, an off by one at
     * a segment boundary, or a dropped, duplicated or reordered window, changes the value that comes back. Each
     * sixteen character block carries its own index so that such a fault is localised rather than merely detected.
     */
    @Test
    public void segmentedCharacterStringRoundTrip() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++)
            sb.append(String.format("%04d-abcdefghij-", i));
        String longString = sb.toString();

        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        d1.initialize();

        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0)));
        ObjectIdentifier av0 = new ObjectIdentifier(ObjectType.analogValue, 0);
        createAnalogValue(d2, 0);
        d2.initialize();
        // Have the second device accept only short APDUs, so that the write has to be segmented.
        d2.getDeviceObject().writePropertyInternal(PropertyIdentifier.maxApduLengthAccepted,
                MaxApduLength.UP_TO_50.getMaxLength());

        d1.sendGlobalBroadcast(d1.getIAm());
        d2.sendGlobalBroadcast(d2.getIAm());

        RemoteDevice r2 = d1.getRemoteDevice(2).get();
        r2.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        ServicesSupported ss = new ServicesSupported();
        ss.setAll(true);
        r2.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, ss);

        assertEquals(MaxApduLength.UP_TO_50.getMaxLengthInt(), r2.getMaxAPDULengthAccepted());
        d1.send(r2, new WritePropertyRequest(av0, PropertyIdentifier.description, null,
                new CharacterString(longString), null)).get();

        // Read it back. The response exceeds this device's APDU length, so it is segmented as well.
        ReadPropertyAck ack = d1.send(r2, new ReadPropertyRequest(av0, PropertyIdentifier.description)).get();

        CharacterString received = ack.getValue();
        assertEquals(longString.length(), received.getValue().length());
        assertEquals(longString, received.getValue());

        d1.terminate();
        d2.terminate();
    }

    /**
     * A device lowers its Max_Segments_Accepted, and a peer sending a message longer than that is aborted with
     * `bufferOverflow` rather than having the message assembled without bound.
     * <p>
     * This exercises the property as the source of the limit: the value is written by client code after the device
     * is running, and the transport honours it without being reconfigured.
     */
    @Test
    public void messageBeyondTheReceiversSegmentLimitIsAborted() throws Exception {
        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        d1.initialize();

        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0)));
        ObjectIdentifier av0 = new ObjectIdentifier(ObjectType.analogValue, 0);
        createAnalogValue(d2, 0);
        d2.initialize();
        // Only short APDUs, so the write has to be segmented, and only a handful of segments accepted.
        d2.getDeviceObject().writePropertyInternal(PropertyIdentifier.maxApduLengthAccepted,
                MaxApduLength.UP_TO_50.getMaxLength());
        d2.getDeviceObject().writePropertyInternal(PropertyIdentifier.maxSegmentsAccepted, new UnsignedInteger(4));

        d1.sendGlobalBroadcast(d1.getIAm());
        d2.sendGlobalBroadcast(d2.getIAm());

        RemoteDevice r2 = d1.getRemoteDevice(2).get();
        r2.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        ServicesSupported ss = new ServicesSupported();
        ss.setAll(true);
        r2.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, ss);

        // Long enough to need far more than four segments.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++)
            sb.append(String.format("%04d-abcdefghij-", i));

        AbortAPDUException e = assertThrows(AbortAPDUException.class,
                () -> d1.send(r2, new WritePropertyRequest(av0, PropertyIdentifier.description, null,
                        new CharacterString(sb.toString()), null)).get());
        assertEquals(AbortReason.bufferOverflow.intValue(), e.getApdu().getAbortReason().intValue());

        d1.terminate();
        d2.terminate();
    }

    /**
     * A remote device cannot raise this device's Max_Segments_Accepted. The transport enforces that property as the
     * limit on the segments it will assemble, so a writable property would let a peer lift the limit and then send a
     * message of any size. Table 12-13 gives the property a conformance code of O rather than W, so refusing the
     * write is conformant.
     */
    @Test
    public void segmentationPropertiesCannotBeWrittenRemotely() throws Exception {
        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        d1.initialize();

        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0)));
        d2.initialize();

        d1.sendGlobalBroadcast(d1.getIAm());
        d2.sendGlobalBroadcast(d2.getIAm());

        RemoteDevice r2 = d1.getRemoteDevice(2).get();
        ServicesSupported ss = new ServicesSupported();
        ss.setAll(true);
        r2.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, ss);

        UnsignedInteger before = d2.get(PropertyIdentifier.maxSegmentsAccepted);

        ErrorAPDUException e = assertThrows(ErrorAPDUException.class,
                () -> d1.send(r2, new WritePropertyRequest(d2.getId(), PropertyIdentifier.maxSegmentsAccepted, null,
                        new UnsignedInteger(2_000_000_000), null)).get());
        assertEquals(ErrorClass.property, e.getError().getErrorClass());
        assertEquals(ErrorCode.writeAccessDenied, e.getError().getErrorCode());

        // The limit is unchanged, so the bound still holds.
        assertEquals(before, d2.get(PropertyIdentifier.maxSegmentsAccepted));

        // Segmentation_Supported is likewise read only. It declares a capability of this device rather than a
        // setting, and Table 12-13 gives it a conformance code of R.
        ErrorAPDUException e2 = assertThrows(ErrorAPDUException.class,
                () -> d1.send(r2, new WritePropertyRequest(d2.getId(), PropertyIdentifier.segmentationSupported, null,
                        Segmentation.noSegmentation, null)).get());
        assertEquals(ErrorCode.writeAccessDenied, e2.getError().getErrorCode());
        assertEquals(Segmentation.segmentedBoth, d2.get(PropertyIdentifier.segmentationSupported));

        // Max_APDU_Length_Accepted is read only for the same reason, and is likewise code R.
        ErrorAPDUException e3 = assertThrows(ErrorAPDUException.class,
                () -> d1.send(r2, new WritePropertyRequest(d2.getId(), PropertyIdentifier.maxApduLengthAccepted, null,
                        MaxApduLength.UP_TO_50.getMaxLength(), null)).get());
        assertEquals(ErrorCode.writeAccessDenied, e3.getError().getErrorCode());

        // Local code may still change them.
        d2.getDeviceObject().writePropertyInternal(PropertyIdentifier.maxSegmentsAccepted, new UnsignedInteger(32));
        assertEquals(new UnsignedInteger(32), d2.get(PropertyIdentifier.maxSegmentsAccepted));

        d1.terminate();
        d2.terminate();
    }

    /**
     * A device configured with a custom Max_Segments_Accepted before it is started advertises that value to peers
     * and enforces it, rather than the default. A message within the limit still succeeds, so the test distinguishes
     * the configured limit being applied from everything simply being refused.
     */
    @Test
    public void customMaxSegmentsIsAdvertisedAndEnforced() throws Exception {
        int maxSegments = 8;

        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        d1.initialize();

        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0)));
        ObjectIdentifier av0 = new ObjectIdentifier(ObjectType.analogValue, 0);
        createAnalogValue(d2, 0);
        // Configure the device before starting it. Short APDUs, so that a modest string needs many segments.
        d2.getDeviceObject().writePropertyInternal(PropertyIdentifier.maxApduLengthAccepted,
                MaxApduLength.UP_TO_50.getMaxLength());
        d2.getDeviceObject().writePropertyInternal(PropertyIdentifier.maxSegmentsAccepted,
                new UnsignedInteger(maxSegments));
        d2.initialize();

        d1.sendGlobalBroadcast(d1.getIAm());
        d2.sendGlobalBroadcast(d2.getIAm());

        RemoteDevice r2 = d1.getRemoteDevice(2).get();
        r2.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        ServicesSupported ss = new ServicesSupported();
        ss.setAll(true);
        r2.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, ss);

        // The configured value is what a peer reads, so it is also what a peer would size its messages against.
        ReadPropertyAck limitAck = d1.send(r2,
                new ReadPropertyRequest(d2.getId(), PropertyIdentifier.maxSegmentsAccepted)).get();
        assertEquals(new UnsignedInteger(maxSegments), limitAck.getValue());

        // A segmented write needing fewer than the configured number of segments is accepted.
        String shortValue = "a".repeat(100);
        d1.send(r2, new WritePropertyRequest(av0, PropertyIdentifier.description, null,
                new CharacterString(shortValue), null)).get();
        ReadPropertyAck ack = d1.send(r2,
                new ReadPropertyRequest(av0, PropertyIdentifier.description)).get();
        assertEquals(shortValue, ack.getValue().toString());

        // One needing more than the configured number is aborted.
        AbortAPDUException e = assertThrows(AbortAPDUException.class,
                () -> d1.send(r2, new WritePropertyRequest(av0, PropertyIdentifier.description, null,
                        new CharacterString("b".repeat(2_000)), null)).get());
        assertEquals(AbortReason.bufferOverflow.intValue(), e.getApdu().getAbortReason().intValue());

        // The rejected write did not take effect.
        ReadPropertyAck after = d1.send(r2,
                new ReadPropertyRequest(av0, PropertyIdentifier.description)).get();
        assertEquals(shortValue, after.getValue().toString());

        d1.terminate();
        d2.terminate();
    }

    @Test
    public void disappearingRemoteDevice() throws Exception {
        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        d1.initialize();

        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0)));
        createAnalogValue(d2, 0);
        d2.initialize();

        RemoteDevice rd2 = d1.getRemoteDeviceBlocking(2);

        // Read properties from d2
        SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>( //
                new ReadAccessSpecification(new ObjectIdentifier(ObjectType.analogValue, 0), new SequenceOf<>( //
                        new PropertyReference(PropertyIdentifier.presentValue), //
                        new PropertyReference(PropertyIdentifier.units), //
                        new PropertyReference(PropertyIdentifier.statusFlags))));
        ReadPropertyMultipleAck ack = d1.send(rd2, new ReadPropertyMultipleRequest(listOfReadAccessSpecs)).get();

        assertEquals(1, ack.getListOfReadAccessResults().getCount());
        ReadAccessResult readAccessResult = ack.getListOfReadAccessResults().getBase1(1);
        assertEquals(ObjectType.analogValue, readAccessResult.getObjectIdentifier().getObjectType());
        assertEquals(0, readAccessResult.getObjectIdentifier().getInstanceNumber());

        List<Result> expectedListOfResults = toList( //
                new Result(PropertyIdentifier.presentValue, null, new Real(3.14F)), //
                new Result(PropertyIdentifier.units, null, EngineeringUnits.noUnits), //
                new Result(PropertyIdentifier.statusFlags, null, new StatusFlags(false, false, false, false)));

        assertListEqualsIgnoreOrder(expectedListOfResults, readAccessResult.getListOfResults().getValues());

        // Get rid of the d2
        d2.terminate();

        // Try the request again.
        var future = d1.send(rd2, new ReadPropertyMultipleRequest(listOfReadAccessSpecs));
        assertThrows(BACnetTimeoutException.class, future::get);
    }

    @Test
    public void readError() throws Exception {
        LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        d1.initialize();

        LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0)));
        d2.initialize();

        RemoteDevice rd2 = d1.getRemoteDeviceBlocking(2);

        // Read properties from d2 that don't exist.
        SequenceOf<ReadAccessSpecification> listOfReadAccessSpecs = new SequenceOf<>( //
                new ReadAccessSpecification(new ObjectIdentifier(ObjectType.analogValue, 0), new SequenceOf<>( //
                        new PropertyReference(PropertyIdentifier.presentValue), //
                        new PropertyReference(PropertyIdentifier.units), //
                        new PropertyReference(PropertyIdentifier.statusFlags))));
        try {
            d1.send(rd2, new ReadPropertyMultipleRequest(listOfReadAccessSpecs)).get();
        } catch (ErrorAPDUException e) {
            assertEquals(ErrorClass.object, e.getError().getErrorClass());
            assertEquals(ErrorCode.unknownObject, e.getError().getErrorCode());
        }
    }

    private static BACnetObject createAnalogValue(LocalDevice localDevice, int id)
            throws BACnetServiceException {
        BACnetObject bo = new BACnetObject(localDevice, ObjectType.analogValue, id) //
                .writePropertyInternal(PropertyIdentifier.presentValue, new Real(3.14F)) //
                .writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.noUnits) //
                .writePropertyInternal(PropertyIdentifier.outOfService, Boolean.FALSE) //
                .writePropertyInternal(PropertyIdentifier.eventState, EventState.normal) //
                .writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        localDevice.addObject(bo);
        return bo;
    }
}
