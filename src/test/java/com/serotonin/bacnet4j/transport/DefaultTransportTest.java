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

import static com.serotonin.bacnet4j.TestUtils.await;
import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.ResponseConsumer;
import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.Abort;
import com.serotonin.bacnet4j.apdu.AckAPDU;
import com.serotonin.bacnet4j.apdu.ComplexACK;
import com.serotonin.bacnet4j.apdu.ConfirmedRequest;
import com.serotonin.bacnet4j.apdu.SegmentACK;
import com.serotonin.bacnet4j.apdu.Segmentable;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.enums.MaxSegments;
import com.serotonin.bacnet4j.obj.DeviceObject;
import com.serotonin.bacnet4j.event.DeviceEventHandler;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
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
import com.serotonin.bacnet4j.type.enumerated.AbortReason;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
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
        ThreadUtils.sleep(transport.getSegTimeout() * 8L);

        // Clean up
        transport.terminate();
    }

    private static Address getSourceAddress() {
        return new Address(0, new byte[] {2});
    }

    /**
     * Segments received in order are assembled and the message is delivered to the application.
     * <p>
     * The window size here is 3, so segments 3 and 6 are the last of their windows and are acknowledged; the others
     * are not. Segment 6 is also the last of the message.
     */
    @Test
    public void inOrderSegmentsAreAssembled() throws Exception {
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
        transport.setSegWindow(3);
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});

        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);

        final Segmentable request = addIncomingSegmentedMessage(true, 3, 0, from, transport, service);
        for (int seq = 1; seq <= 5; seq++)
            addIncomingSegmentedMessage(seq != 5, 3, seq, from, transport, service);

        // Wait for the messages to be processed.
        ThreadUtils.sleep(100);

        transport.terminate();

        // Verify that the service's handle method was called.
        verify(service).handle(localDevice, from);

        // Verify the data that was parsed from the segments.
        final InOrder inOrder = inOrder(request);
        for (int seq = 1; seq <= 5; seq++) {
            inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] {(byte) seq}));
        }

        // Segment 3 completes the first window and segment 5 completes the message, so both are acknowledged, as is
        // the opening segment. Segments 1, 2 and 4 are not.
        verifySegmentAck(network, from, false, 0, 3);
        verifySegmentAck(network, from, false, 3, 3);
        verifySegmentAck(network, from, false, 5, 3);

        // Clause 5.4 issues every segment acknowledgement with 'data_expecting_reply' = FALSE. SegmentACK.equals
        // ignores that field, so it has to be asserted directly.
        final ArgumentCaptor<SegmentACK> acks = ArgumentCaptor.forClass(SegmentACK.class);
        verify(network, times(3)).sendAPDU(any(), any(), acks.capture(), anyBoolean());
        for (final SegmentACK sentAck : acks.getAllValues()) {
            assertFalse("segment ack should not expect a reply", sentAck.expectsReply());
        }
    }

    /**
     * Clause 5.4.5.2 SegmentReceivedOutOfOrder. A segment other than the one expected is discarded and negatively
     * acknowledged so that the sender resumes from the last segment received in order.
     * <p>
     * This replaces the permissive behaviour of <a href="https://github.com/infiniteautomation/BACnet4J/issues/7">issue
     * 7</a>, in which out of order segments within a window were buffered and reordered.
     */
    @Test
    public void outOfOrderSegmentIsNegativelyAcknowledged() throws Exception {
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
        transport.setSegWindow(3);
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});

        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);

        final Segmentable request = addIncomingSegmentedMessage(true, 3, 0, from, transport, service);
        addIncomingSegmentedMessage(true, 3, 1, from, transport, service);
        // Segment 2 is expected, so segment 3 is out of order.
        addIncomingSegmentedMessage(true, 3, 3, from, transport, service);

        ThreadUtils.sleep(100);
        transport.terminate();

        // The message was not completed, and the out-of-order segment was not saved.
        verify(service, never()).handle(any(), any());
        verify(request, never()).appendServiceData(new ByteQueue(new byte[] {3}));

        // The last segment received in order was 1, so that is what is negatively acknowledged.
        verifySegmentAck(network, from, true, 1, 3);
    }

    /**
     * Clause 5.4.5.1 ConfirmedSegmentedReceivedWindowSizeOutOfRange. A proposed window size outside the range 1 to
     * 127 is aborted rather than, as previously, stalling until the transaction times out.
     */
    @Test
    public void windowSizeOutOfRangeIsAborted() throws Exception {
        assertWindowSizeRejected(0);
        assertWindowSizeRejected(128);
    }

    private static void assertWindowSizeRejected(int proposedWindowSize) throws Exception {
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

        addIncomingSegmentedMessage(true, proposedWindowSize, 0, from, transport, service);

        ThreadUtils.sleep(100);
        transport.terminate();

        verify(service, never()).handle(any(), any());
        verify(network).sendAPDU(eq(from), any(), eq(new Abort(true, (byte) 0, AbortReason.windowSizeOutOfRange)),
                eq(false));
    }

    /**
     * Clause 5.4.5.1 UnexpectedPDU_Received. A segment other than the first, for a transaction that does not exist,
     * is aborted. Previously this threw a NullPointerException out of the receive handler.
     */
    @Test
    public void segmentForUnknownRequestIsAborted() throws Exception {
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

        // A segment with a non-zero sequence number, for which no transaction exists.
        addIncomingSegmentedMessage(true, 3, 4, from, transport, service);

        ThreadUtils.sleep(100);
        transport.terminate();

        verify(network).sendAPDU(eq(from), any(),
                eq(new Abort(true, (byte) 0, AbortReason.invalidApduInThisState)), eq(false));
    }

    /**
     * Sequence numbers are modulo 256, so a message may be longer than 256 segments. Before the segmentation rewrite
     * such a message stalled at the wrap and timed out.
     */
    @Test
    public void messageLongerThanTheSequenceNumberSpaceIsAssembled() throws Exception {
        final int segmentCount = 300;

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
        transport.setSegWindow(4);
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});
        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);

        final Segmentable request = addIncomingSegmentedMessage(true, 4, 0, from, transport, service);
        for (int i = 1; i < segmentCount; i++)
            addIncomingSegmentedMessage(i != segmentCount - 1, 4, i & 0xff, from, transport, service);

        ThreadUtils.sleep(500);
        transport.terminate();

        // The message completed, which requires the sequence number wrap at segment 256 to have been handled.
        verify(service).handle(localDevice, from);
        verify(request, times(segmentCount - 1)).appendServiceData(any(ByteQueue.class));
    }

    /**
     * Clause 5.4.5.2 DuplicateSegmentReceived and TooManyDuplicateSegmentsReceived. Ndup duplicates are silently
     * dropped, where clause 5.4.1 defines Ndup as being equal to ActualWindowSize; the next one is negatively
     * acknowledged so that the sender is told where the message stands.
     */
    @Test
    public void duplicateSegmentsAreDroppedUntilNdup() throws Exception {
        final int windowSize = 4;

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
        transport.setSegWindow(windowSize);
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});
        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);

        addIncomingSegmentedMessage(true, windowSize, 0, from, transport, service);
        addIncomingSegmentedMessage(true, windowSize, 1, from, transport, service);

        // Ndup duplicates of segment 1 are dropped silently.
        for (int i = 0; i < windowSize; i++)
            addIncomingSegmentedMessage(true, windowSize, 1, from, transport, service);

        ThreadUtils.sleep(100);

        // Only the acknowledgement of the opening segment has been sent. Segment 1 does not complete the window.
        verify(network, times(1)).sendAPDU(any(), any(), any(SegmentACK.class), anyBoolean());

        // One more duplicate produces a negative acknowledgement of the last segment received in order.
        addIncomingSegmentedMessage(true, windowSize, 1, from, transport, service);

        ThreadUtils.sleep(100);
        transport.terminate();

        verifySegmentAck(network, from, true, 1, windowSize);
    }

    /**
     * Step (4) of the addendum 135-2020ch-1 form of DuplicateInWindow. Once a segment of a new window has been
     * received, a retransmission belonging to the preceding window is a duplicate and is dropped silently rather
     * than being treated as out of order.
     */
    @Test
    public void retransmissionOfPreviousWindowIsADuplicate() throws Exception {
        final int windowSize = 4;

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
        transport.setSegWindow(windowSize);
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});
        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);

        // Segments 0 through 4. Segment 4 completes the first window and is acknowledged.
        for (int seq = 0; seq <= 4; seq++)
            addIncomingSegmentedMessage(true, windowSize, seq, from, transport, service);
        // Segment 5 opens the next window.
        addIncomingSegmentedMessage(true, windowSize, 5, from, transport, service);

        ThreadUtils.sleep(100);
        verify(network, times(2)).sendAPDU(any(), any(), any(SegmentACK.class), anyBoolean());

        // A retransmission of a segment of the previous window is dropped without a negative acknowledgement.
        addIncomingSegmentedMessage(true, windowSize, 3, from, transport, service);

        ThreadUtils.sleep(100);
        transport.terminate();

        verify(network, times(2)).sendAPDU(any(), any(), any(SegmentACK.class), anyBoolean());
    }

    /**
     * Clause 5.4.4.2 Timeout. When a window is not acknowledged, FillWindow retransmits the segments of that window.
     * Before the segmentation rewrite the first segment of the message was retransmitted instead.
     */
    @Test(timeout = 10_000)
    public void segmentTimeoutRetransmitsTheCurrentWindow() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        when(network.getMaxApduLength()).thenReturn(MaxApduLength.UP_TO_1476);

        final var sent = new ArrayList<ConfirmedRequest>();
        doAnswer(invocation -> {
            APDU apdu = invocation.getArgument(2);
            if (apdu instanceof ConfirmedRequest cr)
                synchronized (sent) {
                    sent.add(cr);
                }
            return null;
        }).when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.setSegWindow(2);
        transport.setSegTimeout(200);
        transport.setRetries(1);
        transport.initialize();

        final Address to = new Address(0, new byte[] {1});

        // A request large enough to need several segments at an APDU length of 50.
        transport.send(to, 50, Segmentation.segmentedBoth, buildReadPropertyMultipleRequest(1_000));

        // The opening segment.
        awaitEquals(1, () -> {
            synchronized (sent) {
                return sent.size();
            }
        });
        byte invokeId = transport.unackedMessages.getRequests().keySet().iterator().next().getInvokeId();

        // Acknowledge it, which opens a window of two segments.
        addIncomingNPDU(transport, to, new SegmentACK(false, true, invokeId, 0, 2, true));
        awaitEquals(3, () -> {
            synchronized (sent) {
                return sent.size();
            }
        });

        synchronized (sent) {
            assertEquals(1, sent.get(1).getSequenceNumber());
            assertEquals(2, sent.get(2).getSequenceNumber());
        }

        // Say nothing, so that the window times out and is retransmitted.
        awaitEquals(5, () -> {
            synchronized (sent) {
                return sent.size();
            }
        }, 5_000);

        transport.terminate();

        synchronized (sent) {
            // The current window is retransmitted, not the first segment of the message.
            assertEquals(1, sent.get(3).getSequenceNumber());
            assertEquals(2, sent.get(4).getSequenceNumber());
        }
    }

    /**
     * The proposed window size is encoded in a single octet and clause 20.1.2 restricts it to 1 to 127. A value
     * outside that range would make a peer reply with a windowSizeOutOfRange abort, so it is clamped on the way in.
     */
    @Test
    public void segWindowIsClampedToTheLegalRange() {
        final DefaultTransport transport = new DefaultTransport(mock(Network.class));

        transport.setSegWindow(64);
        assertEquals(64, transport.getSegWindow());

        transport.setSegWindow(0);
        assertEquals(SegmentSequence.MIN_WINDOW_SIZE, transport.getSegWindow());

        transport.setSegWindow(128);
        assertEquals(SegmentSequence.MAX_WINDOW_SIZE, transport.getSegWindow());

        transport.setSegWindow(-1);
        assertEquals(SegmentSequence.MIN_WINDOW_SIZE, transport.getSegWindow());
    }

    /**
     * Clause 5.4 defines no transition for a segment acknowledgement whose 'actual-window-size' is out of range, and
     * a value of zero would stall the transmission until it timed out, so it is clamped to one instead.
     */
    @Test(timeout = 10_000)
    public void outOfRangeActualWindowSizeIsClamped() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        when(network.getMaxApduLength()).thenReturn(MaxApduLength.UP_TO_1476);

        final var sent = new ArrayList<ConfirmedRequest>();
        doAnswer(invocation -> {
            APDU apdu = invocation.getArgument(2);
            if (apdu instanceof ConfirmedRequest cr)
                synchronized (sent) {
                    sent.add(cr);
                }
            return null;
        }).when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.setSegWindow(4);
        transport.initialize();

        final Address to = new Address(0, new byte[] {1});
        transport.send(to, 50, Segmentation.segmentedBoth, buildReadPropertyMultipleRequest(1_000));

        awaitEquals(1, () -> {
            synchronized (sent) {
                return sent.size();
            }
        });
        final byte invokeId = transport.unackedMessages.getRequests().keySet().iterator().next().getInvokeId();

        // Acknowledge the opening segment, asking for a window of zero.
        addIncomingNPDU(transport, to, new SegmentACK(false, true, invokeId, 0, 0, false));

        // The window is clamped to one, so exactly one further segment is sent rather than none.
        awaitEquals(2, () -> {
            synchronized (sent) {
                return sent.size();
            }
        });
        ThreadUtils.sleep(100);

        transport.terminate();

        synchronized (sent) {
            assertEquals(2, sent.size());
            assertEquals(1, sent.get(1).getSequenceNumber());
        }
    }

    /**
     * Clause 5.4.5.2 UnexpectedPDU_Received. An unsegmented request carries no sequence number, so feeding it to the
     * assembler would either produce a spurious negative acknowledgement or, when the message being assembled is at
     * sequence number 255, splice the unrelated request onto it.
     */
    @Test
    public void unsegmentedRequestDuringSegmentedOneIsAborted() throws Exception {
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
        transport.setSegWindow(3);
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});
        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);

        // Begin assembling a segmented request.
        final Segmentable request = addIncomingSegmentedMessage(true, 3, 0, from, transport, service);
        addIncomingSegmentedMessage(true, 3, 1, from, transport, service);

        // An unsegmented request arrives reusing the same invoke id.
        final ConfirmedRequest unsegmented = mock(ConfirmedRequest.class);
        when(unsegmented.isSegmentedMessage()).thenReturn(false);
        when(unsegmented.getServiceRequest()).thenReturn(service);
        when(unsegmented.getServiceData()).thenReturn(new ByteQueue(new byte[] {(byte) 0xff}));
        addIncomingNPDU(transport, from, unsegmented);

        ThreadUtils.sleep(100);
        transport.terminate();

        // The transaction is aborted, the unsegmented request is not handled, and its data is not appended to the
        // message being assembled.
        verify(network).sendAPDU(eq(from), any(),
                eq(new Abort(true, (byte) 0, AbortReason.invalidApduInThisState)), eq(false));
        verify(service, never()).handle(any(), any());
        verify(request, never()).appendServiceData(new ByteQueue(new byte[] {(byte) 0xff}));
    }

    /**
     * Clause 5.4.4.1 UnexpectedSegmentInfoReceived and 5.4.5.1 UnexpectedPDU_Received. A PDU that shows the peer
     * still has an active state machine is aborted, rather than discarded, so that the peer stops retransmitting.
     */
    @Test
    public void segmentationPduForUnknownTransactionIsAborted() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});

        // A segment ack from a server, for which this device has no transaction.
        addIncomingNPDU(transport, from, new SegmentACK(false, true, (byte) 7, 0, 2, false));

        ThreadUtils.sleep(100);
        transport.terminate();

        // This device is the client of that transaction, so the abort carries 'server' = FALSE.
        verify(network).sendAPDU(eq(from), any(),
                eq(new Abort(false, (byte) 7, AbortReason.invalidApduInThisState)), eq(false));
    }

    /**
     * Clause 5.4.4.3 UnexpectedPDU_Received. The first segment of a response must carry sequence number zero; any
     * other value aborts the transaction and reports the abort to the application.
     */
    @Test(timeout = 10_000)
    public void segmentedResponseWithNonZeroOpeningSequenceNumberIsAborted() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        when(network.getMaxApduLength()).thenReturn(MaxApduLength.UP_TO_1476);

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.initialize();

        final Address to = new Address(0, new byte[] {1});

        final var failure = new AtomicReference<AckAPDU>();
        transport.send(to, 1476, Segmentation.segmentedBoth, buildReadPropertyMultipleRequest(1),
                new ResponseConsumer() {
                    @Override
                    public void success(final AcknowledgementService ack) {
                        // Not expected.
                    }

                    @Override
                    public void fail(final AckAPDU ack) {
                        failure.set(ack);
                    }

                    @Override
                    public void ex(final BACnetException e) {
                        // Not expected.
                    }
                });

        assertTrue(await(() -> transport.unackedMessages.getRequests().size() == 1, 1_000));
        final byte invokeId = transport.unackedMessages.getRequests().keySet().iterator().next().getInvokeId();

        // The opening segment of a response must have sequence number zero. This one does not.
        addIncomingNPDU(transport, to,
                new ComplexACK(true, true, invokeId, 3, 2, (byte) 14, new ByteQueue(new byte[] {1, 2, 3})));

        assertTrue(await(() -> failure.get() != null, 1_000));
        transport.terminate();

        assertEquals(new Abort(false, invokeId, AbortReason.invalidApduInThisState), failure.get());
        verify(network).sendAPDU(eq(to), any(),
                eq(new Abort(false, invokeId, AbortReason.invalidApduInThisState)), eq(false));
    }

    /**
     * Clause 5.4.4.2 AbortPDU_Received. A peer that cannot accept as many segments as are being sent aborts partway
     * through, and the abort is reported to the application rather than answered with another abort.
     * <p>
     * This is the path that catches an over-large request when the peer's Max_Segments_Accepted is not known, which
     * is the usual case, since the property is not carried by I-Am. The clause 5.4.4.1 CannotSend check cannot fire
     * then, so the peer telling us to stop is the only thing that ends the transfer.
     */
    @Test(timeout = 10_000)
    public void abortFromPeerDuringSegmentedSendIsReported() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        when(network.getMaxApduLength()).thenReturn(MaxApduLength.UP_TO_1476);

        final var sent = new ArrayList<APDU>();
        doAnswer(invocation -> {
            synchronized (sent) {
                sent.add(invocation.getArgument(2));
            }
            return null;
        }).when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.setSegWindow(2);
        transport.initialize();

        final Address to = new Address(0, new byte[] {1});

        final var failure = new AtomicReference<AckAPDU>();
        // The peer's Max_Segments_Accepted is not known, so no local limit applies to this request.
        transport.send(to, 50, Segmentation.segmentedBoth, null, buildReadPropertyMultipleRequest(1_000),
                new ResponseConsumer() {
                    @Override
                    public void success(final AcknowledgementService ack) {
                        // Not expected.
                    }

                    @Override
                    public void fail(final AckAPDU ack) {
                        failure.set(ack);
                    }

                    @Override
                    public void ex(final BACnetException e) {
                        // Not expected.
                    }
                });

        awaitEquals(1, () -> {
            synchronized (sent) {
                return sent.size();
            }
        });
        final byte invokeId = transport.unackedMessages.getRequests().keySet().iterator().next().getInvokeId();

        // Acknowledge the opening segment so that a window of segments goes out.
        addIncomingNPDU(transport, to, new SegmentACK(false, true, invokeId, 0, 2, false));
        awaitEquals(3, () -> {
            synchronized (sent) {
                return sent.size();
            }
        });

        // The peer gives up rather than accepting the rest.
        addIncomingNPDU(transport, to, new Abort(true, invokeId, AbortReason.bufferOverflow));

        assertTrue(await(() -> failure.get() != null, 1_000));

        // Nothing further is sent, and the transaction is gone.
        ThreadUtils.sleep(200);
        transport.terminate();

        assertEquals(new Abort(true, invokeId, AbortReason.bufferOverflow), failure.get());
        assertTrue(transport.unackedMessages.getRequests().isEmpty());
        synchronized (sent) {
            assertEquals("no further segments should be sent after the abort", 3, sent.size());
            for (final APDU apdu : sent)
                assertFalse("an abort must not be answered with an abort", apdu instanceof Abort);
        }
    }

    /**
     * Clause 5.4.4.4 NewSegmentReceived_NoSpace. A message longer than this device is prepared to assemble is
     * aborted with `bufferOverflow` rather than accumulated until the heap is exhausted.
     */
    @Test
    public void incomingMessageBeyondTheSegmentLimitIsAborted() throws Exception {
        final int maxSegments = 5;

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
        transport.setSegWindow(2);
        when(localDevice.get(PropertyIdentifier.maxSegmentsAccepted))
                .thenReturn(new UnsignedInteger(maxSegments));
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});
        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);

        // Send more segments than the limit allows, all of them in order and all claiming more to follow.
        final Segmentable request = addIncomingSegmentedMessage(true, 2, 0, from, transport, service);
        for (int seq = 1; seq <= maxSegments; seq++)
            addIncomingSegmentedMessage(true, 2, seq, from, transport, service);

        ThreadUtils.sleep(200);
        transport.terminate();

        // The transaction is aborted rather than assembled, and the segment beyond the limit is not saved.
        verify(network).sendAPDU(eq(from), any(), eq(new Abort(true, (byte) 0, AbortReason.bufferOverflow)),
                eq(false));
        verify(service, never()).handle(any(), any());
        verify(request, times(maxSegments - 1)).appendServiceData(any(ByteQueue.class));
        assertTrue(transport.unackedMessages.getRequests().isEmpty());
    }

    /**
     * A message of exactly the segment limit is still accepted, so the bound is inclusive.
     */
    @Test
    public void incomingMessageAtTheSegmentLimitIsAccepted() throws Exception {
        final int maxSegments = 5;

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
        transport.setSegWindow(2);
        when(localDevice.get(PropertyIdentifier.maxSegmentsAccepted))
                .thenReturn(new UnsignedInteger(maxSegments));
        transport.initialize();

        final Address from = new Address(0, new byte[] {1});
        final ConfirmedRequestService service = mock(ConfirmedRequestService.class);

        addIncomingSegmentedMessage(true, 2, 0, from, transport, service);
        for (int seq = 1; seq < maxSegments; seq++)
            addIncomingSegmentedMessage(seq != maxSegments - 1, 2, seq, from, transport, service);

        ThreadUtils.sleep(200);
        transport.terminate();

        verify(service).handle(localDevice, from);
        verify(network, never()).sendAPDU(any(), any(), any(Abort.class), anyBoolean());
    }

    /**
     * The segment limit is advertised to peers in the 'max-segments-accepted' field of outgoing confirmed requests,
     * rather than the previously hardcoded 'more than 64'.
     */
    @Test(timeout = 10_000)
    public void segmentLimitIsAdvertisedInConfirmedRequests() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});
        when(network.getMaxApduLength()).thenReturn(MaxApduLength.UP_TO_1476);

        final var sent = new ArrayList<ConfirmedRequest>();
        doAnswer(invocation -> {
            APDU apdu = invocation.getArgument(2);
            if (apdu instanceof ConfirmedRequest cr)
                synchronized (sent) {
                    sent.add(cr);
                }
            return null;
        }).when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        when(localDevice.get(PropertyIdentifier.maxSegmentsAccepted)).thenReturn(new UnsignedInteger(16));
        transport.initialize();

        transport.send(new Address(0, new byte[] {1}), 1476, Segmentation.segmentedBoth,
                buildReadPropertyMultipleRequest(1));

        awaitEquals(1, () -> {
            synchronized (sent) {
                return sent.size();
            }
        });
        transport.terminate();

        synchronized (sent) {
            assertEquals(MaxSegments.UP_TO_16, sent.get(0).getMaxSegmentsAccepted());
        }
    }


    /**
     * The limit comes from the local device's Max_Segments_Accepted property, so that it cannot disagree with what
     * this device advertises to peers. A value that is absent, of the wrong type, or below the minimum of clause
     * 12.11.20 falls back to the default rather than leaving a message unbounded.
     */
    @Test
    public void maxSegmentsComesFromTheDeviceObject() {
        final LocalDevice localDevice = mock(LocalDevice.class);
        final DefaultTransport transport = new DefaultTransport(mock(Network.class));
        transport.setLocalDevice(localDevice);

        when(localDevice.get(PropertyIdentifier.maxSegmentsAccepted)).thenReturn(new UnsignedInteger(64));
        assertEquals(64, transport.getMaxSegments());

        // Client code can change the property, and the transport follows it.
        when(localDevice.get(PropertyIdentifier.maxSegmentsAccepted)).thenReturn(new UnsignedInteger(2));
        assertEquals(2, transport.getMaxSegments());

        when(localDevice.get(PropertyIdentifier.maxSegmentsAccepted)).thenReturn(null);
        assertEquals(DeviceObject.DEFAULT_MAX_SEGMENTS_ACCEPTED, transport.getMaxSegments());

        when(localDevice.get(PropertyIdentifier.maxSegmentsAccepted)).thenReturn(new UnsignedInteger(1));
        assertEquals(DeviceObject.DEFAULT_MAX_SEGMENTS_ACCEPTED, transport.getMaxSegments());

        when(localDevice.get(PropertyIdentifier.maxSegmentsAccepted)).thenReturn(new CharacterString("nonsense"));
        assertEquals(DeviceObject.DEFAULT_MAX_SEGMENTS_ACCEPTED, transport.getMaxSegments());
    }

    private static void verifySegmentAck(Network network, Address to, boolean negativeAck, int sequenceNumber,
            int windowSize) throws BACnetException {
        verify(network).sendAPDU(eq(to), any(),
                eq(new SegmentACK(negativeAck, true, (byte) 0, sequenceNumber, windowSize, true)), eq(false));
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
        ServiceFuture result = sut.send(to, 50, Segmentation.segmentedBoth, mock(ConfirmedRequestService.class));

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
        doAnswer(invocation -> {
            apduCount.incrementAndGet();
            return null;
        }).when(network).sendAPDU(any(), any(), any(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        // Ensure sending is allowed
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.setSegWindow(1);
        transport.setTimeout(250);
        // Retransmission of unacknowledged segments is governed by the segment timeout, not the request timeout.
        transport.setSegTimeout(250);
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
        long start = Clock.systemUTC().millis();
        assertThrows(BACnetTimeoutException.class, future::get);
        System.out.println("timeout:" + (Clock.systemUTC().millis() - start));

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
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
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
        awaitEquals(1, transport::getDelayedOutgoingCount, 500);

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
        var sendNPDUInvokeCount = new AtomicInteger(0);
        doAnswer(invocation -> sendNPDUInvokeCount.incrementAndGet())
                .when(network).sendNPDU(any(), any(), any(), anyBoolean(), anyBoolean());

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());
        when(localDevice.getServicesSupported()).thenReturn(new ServicesSupported());
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

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
        assertTrue(await(() -> transport.unackedMessages.getRequests().size() == 1, 200));

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

        // verify that 2 APDUs (1 request, 1 segAck) and optionally the Broadcast NPDU were sent over the network
        await(() -> sendNPDUInvokeCount.get() >= 2, 10000);
        verify(network, times(2)).sendAPDU(any(), any(), any(), anyBoolean());
        verify(network, times(1)).sendAPDU(eq(new Address(new byte[] {0x1})), eq(null), any(ConfirmedRequest.class),
                eq(false));
        var segAck = new SegmentACK(false, false, (byte) 0, 0, 2, true);
        verify(network, times(1)).sendAPDU(new Address(new byte[] {0x1}), null, segAck, false);

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
        when(localDevice.getCommunicationControlState()).thenReturn(EnableDisable.enable);

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
