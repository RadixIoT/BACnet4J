package com.serotonin.bacnet4j.transport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.mockito.InOrder;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.ConfirmedRequest;
import com.serotonin.bacnet4j.apdu.SegmentACK;
import com.serotonin.bacnet4j.apdu.Segmentable;
import com.serotonin.bacnet4j.event.DeviceEventHandler;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.NPCI;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

public class DefaultTransportTest {
    // Recreation of this issue: https://github.com/infiniteautomation/BACnet4J/issues/8
    @Test
    public void criticalSegmentationBug() throws Exception {
        final Network network = mock(Network.class);
        when(network.isThisNetwork(any())).thenReturn(true);
        when(network.getAllLocalAddresses()).thenReturn(new Address[] {getSourceAddress()});

        final LocalDevice localDevice = mock(LocalDevice.class);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());

        final ServicesSupported servicesSupported = new ServicesSupported();
        servicesSupported.setAll(true);
        when(localDevice.getServicesSupported()).thenReturn(servicesSupported);

        final DefaultTransport transport = new DefaultTransport(network);
        transport.setLocalDevice(localDevice);
        transport.setSegTimeout(50);
        transport.initialize();

        final Address from = new Address(0, new byte[] { 1 });

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
        network.sendNPDU(from, null, nakNpdu, false, nak.expectsReply());
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

        final Address from = new Address(0, new byte[] { 1 });

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
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] { 1 }));
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] { 2 }));
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] { 3 }));
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] { 4 }));
        inOrder.verify(request).appendServiceData(new ByteQueue(new byte[] { 5 }));
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
        when(apdu.getServiceData()).thenReturn(new ByteQueue(new byte[] { (byte) sequenceNumber }));

        final NPDU npdu = mock(NPDU.class);
        when(npdu.isNetworkMessage()).thenReturn(false);
        when(npdu.getFrom()).thenReturn(from);
        when(npdu.getAPDU(any())).thenReturn(apdu);
        transport.incoming(npdu);

        return apdu;
    }
    // @AfterAll
    // public void tearDown(){
    //     System.gc();
    // }

}
