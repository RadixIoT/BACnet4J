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

package com.serotonin.bacnet4j.transport;

import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyBoolean;

import java.time.Clock;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.ComplexACK;
import com.serotonin.bacnet4j.apdu.ConfirmedRequest;
import com.serotonin.bacnet4j.apdu.SegmentACK;
import com.serotonin.bacnet4j.apdu.Segmentable;
import com.serotonin.bacnet4j.event.DeviceEventHandler;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.npdu.NPCI;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyMultipleRequest;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

public class DefaultTransportTest {
    // Recreation of this issue: https://github.com/infiniteautomation/BACnet4J/issues/8
    @Test
    public void criticalSegmentationBug() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        doCallRealMethod().when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());

        final ServicesSupported servicesSupported = new ServicesSupported();
        servicesSupported.setAll(true);
        when(localDevice.getServicesSupported()).thenReturn(servicesSupported);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.setSegTimeout(50);
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});

        // Add an incoming message that is the start of segmentation
        addIncomingSegmentedMessage(true, 3, 0, from, transport, null);

        // Add another message which is the first segment
        addIncomingSegmentedMessage(true, 3, 1, from, transport, null);

        // Wait for the message to time out.
        ThreadUtils.sleep(transport.getSegTimeout() * 8);

        // Clean up
        transport.terminate();

        // Ensure the NAK was sent.
        final SegmentACK nak = new SegmentACK(true, false, (byte) 0, 1, 3, true);
        final ByteQueue nakNpdu = createNPDU(nak);
        verify(network).sendNPDU(from, null, nakNpdu, false, nak.expectsReply());
    }

    private static ByteQueue createNPDU(final APDU apdu) {
        final ByteQueue npdu = new ByteQueue();
        final NPCI npci = new NPCI(null, null, apdu.expectsReply());
        npci.write(npdu);
        apdu.write(npdu);
        return npdu;
    }

    private static Address getSourceAddress() {
        return new Address(0, new byte[] {2});
    }

    // Recreation of this issue: https://github.com/infiniteautomation/BACnet4J/issues/7
    @Test
    public void orderSegmentedMessages() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getEventHandler()).thenReturn(new DeviceEventHandler());

        final ServicesSupported servicesSupported = new ServicesSupported();
        servicesSupported.setAll(true);
        when(localDevice.getServicesSupported()).thenReturn(servicesSupported);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});

        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);

        // Add an incoming message that is the start of segmentation
        final Segmentable request = addIncomingSegmentedMessage(true, 3, 0, from, transport, service);

        // Add messages which are the segments, but out of order. This is the first batch.
        addIncomingSegmentedMessage(true, 3, 1, from, transport, service);
        addIncomingSegmentedMessage(true, 3, 3, from, transport, service);
        addIncomingSegmentedMessage(true, 3, 2, from, transport, service);

        // Add messages which are the segments, but out of order. This is the second batch.
        addIncomingSegmentedMessage(false, 3, 5, from, transport, service);
        addIncomingSegmentedMessage(true, 3, 4, from, transport, service);

        // Wait for the messages to be processed.
        ThreadUtils.sleep(100);

        // Clean up
        transport.terminate();

        // Verify that the service's handle method was called.
        verify(service).handle(localDevice, from);

        // Verify the data that was parsed from the segments.
        final InOrder inOrder = inOrder(request);
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] {1}));
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] {2}));
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] {3}));
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] {4}));
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] {5}));
    }

    private static Segmentable addIncomingSegmentedMessage(final boolean moreFollows, final int windowSize,
            final int sequenceNumber, final Address from, final Transport transport,
            final ConfirmedRequestService service) throws BACnetException {

        final ConfirmedRequest apdu = mock(ConfirmedRequest.class);
        when(apdu.isSegmentedMessage()).thenReturn(true);
        when(apdu.isMoreFollows()).thenReturn(moreFollows);
        when(apdu.getProposedWindowSize()).thenReturn(windowSize);
        when(apdu.getSequenceNumber()).thenReturn(sequenceNumber);
        when(apdu.getServiceRequest()).thenReturn(service);
        when(apdu.getServiceData()).thenReturn(new ByteQueue(new byte[] {(byte) sequenceNumber}));

        final NPDU npdu = mock(NPDU.class);
        when(npdu.isNetworkMessage()).thenReturn(false);
        when(npdu.getFrom()).thenReturn(from);
        when(npdu.getAPDU(any())).thenReturn(apdu);
        transport.incoming(npdu);

        return apdu;
    }

    @Test(timeout = 10_000)
    public void futuresCompleteExceptionallyForRequestsSentAfterTerminate() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

        final DefaultTransport sut = new DefaultTransport(network);
        sut.setLocalDevice(localDevice);
        sut.initialize();

        final Address to = new Address(0, new byte[] {1});

        sut.terminate();
        ServiceFuture result = sut.send(to, 20, Segmentation.segmentedBoth, mock(ConfirmedRequestService.class));

        BACnetException e = Assert.assertThrows(BACnetException.class, result::get);
        Assert.assertTrue(e.getMessage().contains("not running"));
    }

    /**
     * Reproduces the issue where the unacked message context was discarded when a duplicate SegACK was received.
     * Expectation: Since the message context is now retained, DefaultTransport should continue processing received
     * SegACKs and sending additional segments
     */
    @Test(timeout = 10_000)
    public void duplicateSegmentAckDoesntDropCtx() throws Exception {
        AtomicInteger apduCount = new AtomicInteger(0);
        // Mock network and device
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        when(network.getMaxApduLength()).thenReturn(MaxApduLength.UP_TO_1476);
        doAnswer(_invocation -> {
            apduCount.incrementAndGet();
            return null;
        }).when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        // Ensure sending is allowed
        when(localDevice.getCommunicationControlState())
                .thenReturn(
                        com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.setSegWindow(1);
        transport.setTimeout(250);
        transport.initialize();

        final Address to = new Address(0, new byte[] {1});

        final ConfirmedRequestService readPropertiesRequest = buildReadPropertyMultipleRequest(1_000);

        // Send the request with a small APDU length to guarantee segmentation
        var future = transport.send(to, 50, Segmentation.segmentedBoth, readPropertiesRequest);

        awaitEquals(1, apduCount::get);

        // Obtain the current invokeId from the unacked messages
        assertEquals("expected one in-flight request", 1, transport.unackedMessages.getRequests().size());
        byte invokeId = transport.unackedMessages.getRequests().keySet().iterator().next().getInvokeId();

        final SegmentACK firstAck = new SegmentACK(false, true, invokeId, 0, transport.getSegWindow(), true);
        addIncomingNPDU(transport, to, firstAck);
        addIncomingNPDU(transport, to, firstAck);

        // assert next window has been processed
        awaitEquals(2, apduCount::get);

        final SegmentACK secondAck = new SegmentACK(false, true, invokeId, 1, transport.getSegWindow(), true);
        addIncomingNPDU(transport, to, secondAck);
        addIncomingNPDU(transport, to, secondAck);

        // assert next window has been processed
        awaitEquals(3, apduCount::get);

        final SegmentACK thirdAck = new SegmentACK(false, true, invokeId, 2, transport.getSegWindow(), true);
        addIncomingNPDU(transport, to, thirdAck);
        addIncomingNPDU(transport, to, thirdAck);

        // assert next window has been processed
        awaitEquals(4, apduCount::get);

        // If we've responded to more than two duplicate segAcks, then we should be safe. But make one last check that
        // the future doesn't hang:
        assertThrows(BACnetTimeoutException.class, future::get);

        transport.terminate();
    }

    /**
     * Reproduces the issue where the unacked message context was discarded when a confirmed request is parked in the
     * delayedOutgoing queue due to a recoverable error (e.g., invokeId exhaustion).
     * Expectation: When transport.terminate() is called, any such queued requests must be completed exceptionally so
     * that threads waiting on the associated ServiceFutureImpl do not hang. This test asserts that terminate() cancels
     * delayedOutgoing entries and the future is completed exceptionally with an indication of shutdown.
     */
    @Test(timeout = 10_000)
    public void terminateCancelsDelayedOutgoing() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        when(network.getMaxApduLength()).thenReturn(MaxApduLength.UP_TO_1476);
        doCallRealMethod().when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        when(localDevice.getCommunicationControlState()).thenReturn(
                com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        //        transport.setTimeout(50);
        transport.setRetries(0);
        transport.initialize();

        final Address to = new Address(0, new byte[] {1});
        // Prepare service with some payload
        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);
        when(service.getChoiceId()).thenReturn((byte) 1);
        doAnswer(inv -> {
            ByteQueue q = inv.getArgument(0);
            q.push((byte) 0x42);
            return null;
        }).when(service).write(any(ByteQueue.class));

        // Pre-fill all 256 invokeIds to force BACnetRecoverableException in addClient
        for (int i = 0; i < 256; i++) {
            transport.send(to, 1476, Segmentation.noSegmentation, service);
        }

        // Send request; Outgoing.send() should catch BACnetRecoverableException and add to delayedOutgoing
        ServiceFuture future = transport.send(to, 1476, Segmentation.noSegmentation, service);

        // Give the transport thread a moment to process and enqueue into delayedOutgoing
        awaitEquals(1, transport.delayedOutgoing::size, 500);

        // Now terminate, which should cancel delayedOutgoing and complete the future exceptionally
        transport.terminate();

        BACnetException e = assertThrows(BACnetException.class, future::get);
        assertTrue(e.getMessage().contains("shutdown"));
    }

    /**
     * Reproduces the issue where the unacked message context was discarded when a segmented ComplexACK response begins
     * (first segments received), but then no further segments of the ComplexACK arrive. This resulted in the related
     * `ServiceFutureImpl` never completing.
     * Expectation: Timeout while waiting for the next segment window does not discard the unacked message context, and
     * the associated ServiceFutureImpl completes with a BACnetTimeoutException, ensuring the future is not orphaned.
     */
    @Test(timeout = 10_000)
    public void segmentedResponseTimeoutCompletesFuture() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        when(network.getMaxApduLength()).thenReturn(MaxApduLength.UP_TO_480);
        doCallRealMethod().when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        when(localDevice.getCommunicationControlState()).thenReturn(
                com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.setTimeout(500);
        transport.setRetries(0);
        transport.setSegTimeout(50);
        transport.setSegWindow(2);
        transport.initialize();

        final Address to = new Address(0, new byte[] {1});

        // Force segmentation of response
        final ConfirmedRequestService requestService = buildReadPropertyMultipleRequest(32);
        ServiceFuture future = transport.send(to, 480, Segmentation.segmentedBoth, requestService);

        // Allow transport to send request
        assertTrue(TestUtils.await(() -> transport.unackedMessages.getRequests().size() == 1, 200));

        // Obtain invokeId
        byte invokeId = transport.unackedMessages.getRequests().keySet().iterator().next().getInvokeId();

        // Simulate receiving the first ComplexACK segment only (segmented response), then stall
        var ack = buildReadPropertyMultipleAck(32);
        var bytes = new ByteQueue();
        ack.write(bytes);

        byte[] segmentBytes = new byte[200];

        // send 1 of 3 segments
        bytes.pop(segmentBytes);
        var complexAckSegment = new ComplexACK(
                true, true, invokeId, 0, transport.getSegWindow(), ack.getChoiceId(), new ByteQueue(segmentBytes));
        addIncomingNPDU(transport, to, complexAckSegment);

        // send 2 of 3 segments
        bytes.pop(segmentBytes);
        complexAckSegment = new ComplexACK(
                true, true, invokeId, 1, transport.getSegWindow(), ack.getChoiceId(), new ByteQueue(segmentBytes)
        );
        addIncomingNPDU(transport, to, complexAckSegment);

        assertThrows(BACnetTimeoutException.class, future::get);

        // verify that 3 APDUs (1 request, 2 segAcks) and the Broadcast NPDU were sent over the network
        verify(network, times(3)).sendAPDU(any(), any(), any(), anyBoolean());
        verify(network, times(4)).sendNPDU(any(), any(), any(), anyBoolean(), anyBoolean());

        transport.terminate();
    }

    /**
     * Scenario: A non-segmented confirmed request is sent and no acknowledgement is ever received.
     * Expectation: The request expires via expire() and the associated ServiceFutureImpl completes
     * with a BACnetTimeoutException (i.e., it is not orphaned and callers do not block indefinitely).
     */
    @Test(timeout = 10_000)
    public void nonSegmentedTimeoutCompletesFuture() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        when(network.getMaxApduLength()).thenReturn(MaxApduLength.UP_TO_1476);
        doCallRealMethod().when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        when(localDevice.getCommunicationControlState()).thenReturn(
                com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.setTimeout(50);
        transport.setRetries(0);
        transport.initialize();

        final Address to = new Address(0, new byte[] {1});
        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);
        when(service.getChoiceId()).thenReturn((byte) 1);
        doAnswer(inv -> {
            ByteQueue q = inv.getArgument(0);
            q.push((byte) 0x01);
            return null;
        }).when(service).write(any(ByteQueue.class));

        ServiceFutureImpl future = (ServiceFutureImpl) transport.send(to, 1476, Segmentation.noSegmentation, service);

        assertThrows(BACnetTimeoutException.class, future::get);

        transport.terminate();
    }

    private static void addIncomingNPDU(final Transport transport, final Address from, final APDU apdu)
            throws BACnetException {
        final NPDU npdu = mock(NPDU.class);
        when(npdu.isNetworkMessage()).thenReturn(false);
        when(npdu.getFrom()).thenReturn(from);
        when(npdu.getAPDU(any())).thenReturn(apdu);
        transport.incoming(npdu);
    }

    private static ReadPropertyMultipleRequest buildReadPropertyMultipleRequest(int propertyCount) {
        var specs = new ArrayList<ReadAccessSpecification>();

        for (int i = 0; i < propertyCount; i++) {
            specs.add(new ReadAccessSpecification(
                    new ObjectIdentifier(ObjectType.binaryValue, i),
                    new SequenceOf<>(new PropertyReference(PropertyIdentifier.forId(28))))
            );
        }

        return new ReadPropertyMultipleRequest(new SequenceOf<>(specs));
    }

    private static ReadPropertyMultipleAck buildReadPropertyMultipleAck(int propertyCount) {
        var results = new ArrayList<ReadAccessResult>();

        for (int i = 0; i < propertyCount; i++) {
            results.add(new ReadAccessResult(
                    new ObjectIdentifier(ObjectType.binaryValue, i),
                    new SequenceOf<>(
                            new ReadAccessResult.Result(
                                    PropertyIdentifier.forId(28),
                                    null,
                                    new CharacterString("desc"))))
            );
        }
        return new ReadPropertyMultipleAck(new SequenceOf<>(results));
    }

}
