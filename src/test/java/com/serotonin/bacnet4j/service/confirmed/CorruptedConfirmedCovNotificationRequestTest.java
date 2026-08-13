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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.ExceptionListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.ReflectionException;
import com.serotonin.bacnet4j.exception.RejectAPDUException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.RejectReason;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Covers the reported failure in which a corrupted ConfirmedCovNotificationRequest caused a BigInteger
 * construction failure while the receiver was decoding the service data, and pins down what a client now gets
 * back when it sends one.
 *
 * <p>The corruption is applied to the bytes on the wire, so the whole receive path is exercised: NPDU parse,
 * APDU parse, service choice check, and finally the ConfirmedCovNotificationRequest ByteQueue constructor.</p>
 *
 * <p>The corruption is always to a tag's declared length. That length used to be used directly to size a byte
 * array that was handed to {@code new BigInteger(byte[])} (in {@code UnsignedInteger}, {@code Enumerated} and
 * {@code SignedInteger}), so a corrupt value failed with an unchecked NegativeArraySizeException or
 * ArithmeticException. Those are not the {@code BACnetErrorException} the parser is written to expect, so they
 * surfaced as a {@link ReflectionException} out of {@code Encodable.read}, which the transport answered with a
 * generic {@code services / operationalProblem} Error and also dispatched to the exception listeners.</p>
 *
 * <p>{@code Primitive.readTag} now validates the declared length before it is used, so these decode as ordinary
 * errors. Per 135-2024 clause 18.9 a confirmed request whose parameters cannot be decoded was not understood and
 * so is rejected rather than answered with an Error: the client gets a Reject of {@code invalidDataEncoding} and
 * the server reports nothing.</p>
 */
public class CorruptedConfirmedCovNotificationRequestTest {
    private static final int TIMEOUT = 30000;

    private final TestNetworkMap map = new TestNetworkMap();
    private final CorruptingTestNetwork clientNetwork = new CorruptingTestNetwork(map, 1);
    private final LocalDevice client = new LocalDevice(1,
            new DefaultTransport(clientNetwork.withTimeout(TIMEOUT)));
    private final LocalDevice server = new LocalDevice(2,
            new DefaultTransport(new TestNetwork(map, 2, 0).withTimeout(TIMEOUT)));

    private final BlockingQueue<Exception> serverExceptions = new LinkedBlockingQueue<>();
    private RemoteDevice remoteServer;

    @Before
    public void before() throws Exception {
        server.getExceptionDispatcher().addListener(new ExceptionListener() {
            @Override
            public void unimplementedVendorService(UnsignedInteger vendorId, UnsignedInteger serviceNumber,
                    ByteQueue queue) {
                // Not of interest here.
            }

            @Override
            public void receivedException(Exception e) {
                serverExceptions.add(e);
            }
        });

        client.initialize();
        server.initialize();

        // Discover the server before any corruption is armed.
        remoteServer = client.getRemoteDevice(2).get();
    }

    @After
    public void after() {
        client.terminate();
        server.terminate();
    }

    /**
     * The length of the subscriberProcessIdentifier is corrupted to 0x7FFFFFFF. Before the length was validated,
     * UnsignedInteger allocated <code>new byte[length + 1]</code>, which overflowed int to Integer.MIN_VALUE and
     * failed with a NegativeArraySizeException before BigInteger was even reached.
     */
    @Test
    public void unsignedIntegerLengthOverflowIsRejected() throws Exception {
        // Context tag 0, length form 5 (extended), 4-byte length of 0x7FFFFFFF.
        RejectAPDUException ex = sendCorrupted(cleanRequest(),
                encodedSubscriberProcessIdentifier(),
                new byte[] {0x0D, (byte) 0xFF, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});

        assertRejectedAsInvalidEncoding(Objects.requireNonNull(ex));
        assertServerReportedNoException();
    }

    /**
     * The literal reported failure. The length of the subscriberProcessIdentifier is corrupted to 0x10000000,
     * which was large enough that the magnitude of the resulting BigInteger sat exactly at
     * BigInteger.MAX_MAG_LENGTH with its top bit set, so BigInteger rejected it with
     * "BigInteger would overflow supported range".
     *
     * <p>Reaching that failure used to require the parser to allocate a 256MB byte array and BigInteger to
     * allocate a 256MB int array. The length is now rejected before anything is allocated, so this case is as
     * cheap as any other.</p>
     */
    @Test
    public void bigIntegerOverflowLengthIsRejected() throws Exception {
        // Context tag 0, length form 5 (extended), 4-byte length of 0x10000000, followed by a data byte with
        // its high bit set. That byte became the most significant byte of the BigInteger magnitude, which is
        // what pushed it over the supported range rather than merely up against it.
        RejectAPDUException ex = sendCorrupted(cleanRequest(),
                encodedSubscriberProcessIdentifier(),
                new byte[] {0x0D, (byte) 0xFF, 0x10, 0x00, 0x00, 0x00, (byte) 0x80});

        assertRejectedAsInvalidEncoding(Objects.requireNonNull(ex));
        assertServerReportedNoException();
    }

    /**
     * The same corruption applied one level deeper, to the propertyIdentifier of a value in the listOfValues.
     * PropertyIdentifier is an Enumerated, which had the same unvalidated BigInteger construction.
     */
    @Test
    public void enumeratedLengthOverflowIsRejected() throws Exception {
        ByteQueue queue = new ByteQueue();
        PropertyIdentifier.presentValue.write(queue, 0);

        RejectAPDUException ex = sendCorrupted(cleanRequest(), queue.popAll(),
                new byte[] {0x0D, (byte) 0xFF, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});

        assertRejectedAsInvalidEncoding(Objects.requireNonNull(ex));
        assertServerReportedNoException();
    }

    /**
     * A sanity check that the harness itself does not change behaviour: with no corruption armed, the same
     * request is accepted normally.
     */
    @Test
    public void uncorruptedRequestSucceeds() throws Exception {
        client.send(remoteServer, cleanRequest()).get();
        assertTrue(serverExceptions.isEmpty());
    }

    //
    // Helpers
    //

    private static ConfirmedCovNotificationRequest cleanRequest() {
        return new ConfirmedCovNotificationRequest( //
                new UnsignedInteger(1), //
                new ObjectIdentifier(ObjectType.device, 1), //
                new ObjectIdentifier(ObjectType.analogInput, 0), //
                new UnsignedInteger(60), //
                new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(3.14f))));
    }

    /**
     * The encoding of the subscriberProcessIdentifier of {@link #cleanRequest()}, i.e. the first field of the
     * service data.
     */
    private static byte[] encodedSubscriberProcessIdentifier() {
        ByteQueue queue = new ByteQueue();
        new UnsignedInteger(1).write(queue, 0);
        return queue.popAll();
    }

    /**
     * Sends the given request to the server, replacing <code>target</code> with <code>replacement</code> in the
     * outgoing bytes, and returns the rejection the client gets back.
     */
    private RejectAPDUException sendCorrupted(ConfirmedCovNotificationRequest request, byte[] target,
            byte[] replacement) {
        // Search the whole encoded service data so that the replacement cannot be applied to a coincidentally
        // matching byte sequence in the NPCI or the APDU header.
        ByteQueue queue = new ByteQueue();
        request.write(queue);
        byte[] serviceData = queue.popAll();

        clientNetwork.corrupt(serviceData, replaceFirst(serviceData, target, replacement));

        try {
            client.send(remoteServer, request).get();
        } catch (RejectAPDUException e) {
            return e;
        } catch (BACnetException e) {
            fail("Expected a RejectAPDUException, but got " + e);
        }
        fail("Expected a RejectAPDUException, but the request succeeded");
        return null;
    }

    /**
     * Asserts that the client received the rejection that a corrupted length now produces. Per 135-2024 clause
     * 18.9 a confirmed request whose parameters cannot be decoded is rejected rather than answered with an Error.
     */
    private static void assertRejectedAsInvalidEncoding(RejectAPDUException ex) {
        assertEquals(RejectReason.invalidDataEncoding, ex.getApdu().getRejectReason());
    }

    /**
     * Asserts that the server treated the corrupt request as a decoding error and not as an internal fault. The
     * length is now validated before it is used, so the failure is a BACnetErrorException that the transport
     * answers with an Error APDU, rather than a ReflectionException wrapping an unchecked exception out of an
     * array allocation or a BigInteger construction, which was additionally dispatched to the exception listeners.
     *
     * <p>The client has already received the response by the time this runs, so the server has finished handling
     * the request; the short poll covers the window between the response being sent and a dispatch that would
     * follow it.</p>
     */
    private void assertServerReportedNoException() throws Exception {
        Exception e = serverExceptions.poll(500, TimeUnit.MILLISECONDS);
        assertNull("The server reported the malformed request as an exception: " + e, e);
    }

    private static byte[] replaceFirst(byte[] source, byte[] target, byte[] replacement) {
        int index = indexOf(source, target);
        if (index == -1)
            throw new IllegalArgumentException("Target bytes not found in the encoded request");

        byte[] result = new byte[source.length - target.length + replacement.length];
        System.arraycopy(source, 0, result, 0, index);
        System.arraycopy(replacement, 0, result, index, replacement.length);
        System.arraycopy(source, index + target.length, result, index + replacement.length,
                source.length - index - target.length);
        return result;
    }

    private static int indexOf(byte[] source, byte[] target) {
        outer:
        for (int i = 0; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j])
                    continue outer;
            }
            return i;
        }
        return -1;
    }

    /**
     * A test network that can corrupt a byte sequence in the messages it sends, simulating a peer that encodes
     * a malformed request.
     */
    private static class CorruptingTestNetwork extends TestNetwork {
        private volatile byte[] target;
        private volatile byte[] replacement;

        CorruptingTestNetwork(TestNetworkMap map, int address) {
            super(map, address, 0);
        }

        void corrupt(byte[] target, byte[] replacement) {
            this.target = target;
            this.replacement = replacement;
        }

        @Override
        public void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast,
                boolean expectsReply) throws BACnetException {
            ByteQueue toSend = npdu;
            byte[] t = target;
            if (t != null) {
                byte[] data = npdu.popAll();
                toSend = new ByteQueue(indexOf(data, t) == -1 ? data : replaceFirst(data, t, replacement));
            }
            super.sendNPDU(recipient, router, toSend, broadcast, expectsReply);
        }
    }
}
