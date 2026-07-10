/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.service.unconfirmed;

import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.exception.RejectAPDUException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.RejectReason;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;

/**
 * Tests the runtime restrictions on a BACnet4J device with Device Identifier 4194303 (the
 * "unconfigured" state defined by addendum 135-2016bz-1 Clause 19.X): outbound sends are limited
 * to Who-Am-I, and inbound execution is limited to Who-Is and You-Are.
 */
public class UnconfiguredDeviceRestrictionTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final Address unconfiguredAddr = TestNetworkUtils.toAddress(50);
    private final Address peerAddr = TestNetworkUtils.toAddress(51);

    /** The device under test — starts life with Device Identifier 4194303. */
    private LocalDevice unconfigured;

    /** A configured peer used to send stimulus to the unconfigured device. */
    private LocalDevice peer;

    @Before
    public void before() throws Exception {
        unconfigured = new LocalDevice(ObjectIdentifier.UNINITIALIZED,
                new DefaultTransport(new TestNetwork(map, 50, 0)));
        unconfigured.getDeviceObject().writePropertyInternal(
                PropertyIdentifier.serialNumber, new CharacterString("SN-under-test"));
        unconfigured.initialize();

        peer = new LocalDevice(51, new DefaultTransport(new TestNetwork(map, 51, 0)));
        peer.initialize();
    }

    @After
    public void after() {
        unconfigured.terminate();
        peer.terminate();
    }

    // ------------------------- Outbound restriction -------------------------

    /**
     * Per Clause 19.X, an unconfigured device shall not initiate any service other than Who-Am-I.
     * The library's send guard should reject an attempt to broadcast I-Am.
     */
    @Test
    public void outboundRestriction_iAmBroadcast_throws() {
        var iAm = unconfigured.getIAm();
        BACnetRuntimeException e = assertThrows(BACnetRuntimeException.class,
                () -> unconfigured.sendGlobalBroadcast(iAm));
        // Message should mention what was attempted.
        assertThrowsForNonWhoAmI(e, "IAmRequest");
    }

    /**
     * Same guard, confirmed-request send path.
     */
    @Test
    public void outboundRestriction_confirmedRequest_throws() {
        var rpr = new ReadPropertyRequest(new ObjectIdentifier(ObjectType.device, 51), PropertyIdentifier.objectName);
        BACnetRuntimeException e = assertThrows(BACnetRuntimeException.class,
                () -> unconfigured.send(peerAddr, rpr));
        assertThrowsForNonWhoAmI(e, "ReadPropertyRequest");
    }

    /**
     * Who-Am-I is the one service an unconfigured device is permitted to initiate.
     */
    @Test
    public void outboundRestriction_whoAmI_succeeds() {
        WhoAmIRequest whoAmI = new WhoAmIRequest(
                new Unsigned16(865),
                new CharacterString("BACnet4J"),
                new CharacterString("SN-under-test"));
        // Should not throw.
        unconfigured.sendGlobalBroadcast(whoAmI);
        assertFalse(false); // Have at least one assertion.
    }

    private static void assertThrowsForNonWhoAmI(BACnetRuntimeException e, String requestName) {
        // The library's guard message names the class it refused.
        if (!e.getMessage().contains(requestName)) {
            fail("Expected guard message to mention " + requestName + ", got: " + e.getMessage());
        }
    }

    // ------------------------- Inbound restriction -------------------------

    /**
     * Per Clause 19.X, an unconfigured device shall not execute any confirmed service. A peer
     * sending ReadProperty should receive a Reject(unrecognizedService) rather than a timeout.
     */
    @Test
    public void inboundRestriction_confirmedRequest_rejected() {
        // Address the unconfigured device by its MAC (instance is 4194303 and can't be used to
        // route). Use a made-up device object identifier — the reject fires before any object
        // lookup happens.
        var rpr = new ReadPropertyRequest(new ObjectIdentifier(ObjectType.device, ObjectIdentifier.UNINITIALIZED),
                PropertyIdentifier.objectName);
        var ex = assertThrows(RejectAPDUException.class, () -> peer.send(unconfiguredAddr, rpr).get());
        assertEquals(RejectReason.unrecognizedService, ex.getApdu().getRejectReason());
    }

    /**
     * Non-Who-Is / non-You-Are unconfirmed services shall be silently dropped by an unconfigured
     * device. We verify by asserting that the device's iAmReceived listener never fires.
     */
    @Test
    public void inboundRestriction_iAm_droppedSilently() {
        AtomicInteger iamReceived = new AtomicInteger(0);
        unconfigured.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void iAmReceived(RemoteDevice d) {
                iamReceived.incrementAndGet();
            }
        });

        peer.send(unconfiguredAddr, peer.getIAm());
        quiesce();

        assertEquals(0, iamReceived.get());
    }

    /**
     * Who-Is is one of the two services an unconfigured device is required to execute. A peer
     * sending Who-Is with a range that includes 4194303 shall receive a Who-Am-I reply unicast
     * back per Clause 16.X.2.
     */
    @Test
    public void inboundRestriction_whoIs_repliesWithWhoAmI() throws Exception {
        AtomicReference<WhoAmIRequest> received = new AtomicReference<>();
        peer.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void whoAmIReceived(Address from, Unsigned16 vendorId, CharacterString modelName,
                    CharacterString serialNumber) {
                received.set(new WhoAmIRequest(vendorId, modelName, serialNumber));
            }
        });

        peer.sendGlobalBroadcast(new WhoIsRequest(0, ObjectIdentifier.UNINITIALIZED));

        awaitEquals(1, () -> received.get() == null ? 0 : 1);
        WhoAmIRequest w = received.get();
        assertEquals(new Unsigned16(865), w.getVendorId());
        assertEquals(new CharacterString("BACnet4J"), w.getModelName());
        assertEquals(new CharacterString("SN-under-test"), w.getSerialNumber());
    }

    /**
     * You-Are is the other required-execution service. A peer sending You-Are with matching
     * vendor / model / serial shall cause the unconfigured device to fire youAreReceived. The
     * library does not itself apply the identifier — that's application-listener policy.
     */
    @Test
    public void inboundRestriction_youAre_matchingIdentity_firesListener() throws Exception {
        AtomicReference<ObjectIdentifier> assignedId = new AtomicReference<>();
        unconfigured.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void youAreReceived(Address from, ObjectIdentifier deviceIdentifier,
                    OctetString deviceMacAddress) {
                assignedId.set(deviceIdentifier);
            }
        });

        peer.send(unconfiguredAddr, new YouAreRequest(
                new Unsigned16(865),                       // matches
                new CharacterString("BACnet4J"),           // matches
                new CharacterString("SN-under-test"),      // matches
                new ObjectIdentifier(com.serotonin.bacnet4j.type.enumerated.ObjectType.device, 42),
                null));

        awaitEquals(1, () -> assignedId.get() == null ? 0 : 1);
        assertEquals(42, assignedId.get().getInstanceNumber());
    }

    /**
     * A You-Are whose vendor / model / serial doesn't match the local device shall be ignored,
     * even though the service was executable.
     */
    @Test
    public void inboundRestriction_youAre_nonMatchingIdentity_ignored() {
        AtomicInteger fired = new AtomicInteger(0);
        unconfigured.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void youAreReceived(Address from, ObjectIdentifier deviceIdentifier,
                    OctetString deviceMacAddress) {
                fired.incrementAndGet();
            }
        });

        peer.send(unconfiguredAddr, new YouAreRequest(
                new Unsigned16(999),                       // wrong vendor
                new CharacterString("BACnet4J"),
                new CharacterString("SN-under-test"),
                new ObjectIdentifier(com.serotonin.bacnet4j.type.enumerated.ObjectType.device, 42),
                null));

        quiesce();
        assertEquals(0, fired.get());
    }

    // ------------------------- Regression: configured device -------------------------

    /**
     * Once the device is assigned a real instance number, both the outbound and inbound gates
     * lift and normal service execution resumes.
     */
    @Test
    public void configuredDevice_normalReadPropertySucceeds() throws Exception {
        // Simulate a successful You-Are: write a real Device Identifier.
        unconfigured.getDeviceObject().writePropertyInternal(
                PropertyIdentifier.objectIdentifier,
                new ObjectIdentifier(ObjectType.device, 42));

        // Peer can now read the newly-configured device.
        assertNull(unconfigured.getDeviceObject().get(PropertyIdentifier.description));
        // A trivial round-trip: read the objectName. Should succeed with no reject.
        peer.send(unconfiguredAddr, new ReadPropertyRequest(
                new ObjectIdentifier(ObjectType.device, 42),
                PropertyIdentifier.objectName)).get();
    }

    // ------------------------- Additional outbound cases -------------------------

    /**
     * The outbound guard covers every send entry point, not only sendGlobalBroadcast.
     */
    @Test
    public void outboundRestriction_sendLocalBroadcast_iAm_throws() {
        var iAm = unconfigured.getIAm();
        BACnetRuntimeException e = assertThrows(BACnetRuntimeException.class,
                () -> unconfigured.sendLocalBroadcast(iAm));
        assertThrowsForNonWhoAmI(e, "IAmRequest");
    }

    /**
     * A Who-Am-I local broadcast is the natural corollary — permitted while unconfigured.
     */
    @Test
    public void outboundRestriction_sendLocalBroadcast_whoAmI_succeeds() {
        var whoAmI = new WhoAmIRequest(
                new Unsigned16(865),
                new CharacterString("BACnet4J"),
                new CharacterString("SN-under-test"));
        unconfigured.sendLocalBroadcast(whoAmI);
        assertFalse(false); // Have at least one assertion.
    }

    /**
     * Unicast unconfirmed sends are also gated.
     */
    @Test
    public void outboundRestriction_sendUnicastUnconfirmed_iAm_throws() {
        var iAm = unconfigured.getIAm();
        BACnetRuntimeException e = assertThrows(BACnetRuntimeException.class,
                () -> unconfigured.send(peerAddr, iAm));
        assertThrowsForNonWhoAmI(e, "IAmRequest");
    }

    // ------------------------- Additional inbound cases -------------------------

    /**
     * Per Clause 16.X.2, an unconfigured device treats its own instance as 4194303 when
     * checking Who-Is range inclusion. A Who-Is whose range excludes 4194303 shall get no
     * reply.
     */
    @Test
    public void inboundRestriction_whoIs_rangeExcludesUnconfigured_noReply() {
        AtomicInteger received = new AtomicInteger(0);
        peer.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void whoAmIReceived(Address from, Unsigned16 vendorId, CharacterString modelName,
                    CharacterString serialNumber) {
                received.incrementAndGet();
            }
        });

        peer.sendGlobalBroadcast(new WhoIsRequest(0, 100));
        quiesce();

        assertEquals(0, received.get());
    }

    /**
     * An unbounded Who-Is (no range limits) covers instance 4194303 by definition. The
     * unconfigured device shall reply with Who-Am-I.
     */
    @Test
    public void inboundRestriction_whoIs_unboundedRange_repliesWithWhoAmI() throws Exception {
        AtomicInteger received = new AtomicInteger(0);
        peer.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void whoAmIReceived(Address from, Unsigned16 vendorId, CharacterString modelName,
                    CharacterString serialNumber) {
                received.incrementAndGet();
            }
        });

        peer.sendGlobalBroadcast(new WhoIsRequest());   // no range limits
        awaitEquals(1, received::get);
        assertFalse(false); // Have at least one assertion.
    }

    /**
     * Per Clause 19.X, only Who-Is and You-Are are executable while unconfigured. Who-Am-I is
     * initiation-only; receiving one shall be silently dropped.
     */
    @Test
    public void inboundRestriction_whoAmI_droppedSilently() {
        AtomicInteger received = new AtomicInteger(0);
        unconfigured.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void whoAmIReceived(Address from, Unsigned16 vendorId, CharacterString modelName,
                    CharacterString serialNumber) {
                received.incrementAndGet();
            }
        });

        peer.send(unconfiguredAddr, new WhoAmIRequest(
                new Unsigned16(100),
                new CharacterString("PeerModel"),
                new CharacterString("peer-serial")));
        quiesce();

        assertEquals(0, received.get());
    }

    /**
     * Per Clause 16.X.3.1.4/5, either Device Identifier or MAC Address (or both) may be
     * provided in a You-Are. The library shall pass through a MAC-only You-Are the same way
     * it does for a Device-Identifier-only or both-fields You-Are.
     */
    @Test
    public void inboundRestriction_youAre_macOnly_firesListener() throws Exception {
        AtomicReference<OctetString> assignedMac = new AtomicReference<>();
        unconfigured.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void youAreReceived(Address from, ObjectIdentifier deviceIdentifier,
                    OctetString deviceMacAddress) {
                assignedMac.set(deviceMacAddress);
            }
        });

        OctetString newMac = new OctetString(new byte[] {(byte) 0xAB});
        peer.send(unconfiguredAddr, new YouAreRequest(
                new Unsigned16(865),
                new CharacterString("BACnet4J"),
                new CharacterString("SN-under-test"),
                null,
                newMac));

        awaitEquals(1, () -> assignedMac.get() == null ? 0 : 1);
        assertEquals(newMac, assignedMac.get());
    }

    // ------------------------- Regression: initialize on unconfigured device -------------------------

    /**
     * Regression for the bug where {@code initialize()} unconditionally sent a restart
     * notification, which the outbound guard now blocks. On an unconfigured device the
     * notification loop must be skipped so that {@code initialize()} completes cleanly.
     * The {@code @Before} setup would have thrown before reaching the test if this were broken.
     */
    @Test
    public void initialize_unconfigured_completesWithoutError() {
        assertEquals(ObjectIdentifier.UNINITIALIZED, unconfigured.getInstanceNumber());
    }

    // ------------------------- End-to-end listener plumbing -------------------------

    /**
     * The unconfigured device broadcasts a Who-Am-I; the peer's listener plumbing observes it
     * end-to-end.
     */
    @Test
    public void unconfiguredDevice_broadcastsWhoAmI_peerListenerFires() throws Exception {
        AtomicReference<CharacterString> received = new AtomicReference<>();
        peer.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void whoAmIReceived(Address from, Unsigned16 vendorId, CharacterString modelName,
                    CharacterString serialNumber) {
                received.set(serialNumber);
            }
        });

        unconfigured.sendGlobalBroadcast(new WhoAmIRequest(
                new Unsigned16(865),
                new CharacterString("BACnet4J"),
                new CharacterString("SN-under-test")));

        awaitEquals(1, () -> received.get() == null ? 0 : 1);
        assertEquals(new CharacterString("SN-under-test"), received.get());
    }

    // ------------------------- Transition: configured → unconfigured -------------------------

    /**
     * Writing 4194303 back to the Device Identifier returns the device to the unconfigured
     * state; the guards re-engage. This proves the gate reads the current instance dynamically
     * rather than caching an initial value.
     */
    @Test
    public void configuredToUnconfiguredTransition_gatesReEngage() {
        // Step 1: promote the device to configured and confirm the outbound gate is lifted.
        unconfigured.getDeviceObject().writePropertyInternal(
                PropertyIdentifier.objectIdentifier,
                new ObjectIdentifier(ObjectType.device, 42));
        unconfigured.sendGlobalBroadcast(unconfigured.getIAm());   // must not throw

        // Step 2: return the device to unconfigured.
        unconfigured.getDeviceObject().writePropertyInternal(
                PropertyIdentifier.objectIdentifier,
                new ObjectIdentifier(ObjectType.device, ObjectIdentifier.UNINITIALIZED));

        // Step 3: outbound guard is back in effect.
        var iAm = unconfigured.getIAm();
        BACnetRuntimeException e = assertThrows(BACnetRuntimeException.class,
                () -> unconfigured.sendGlobalBroadcast(iAm));
        assertThrowsForNonWhoAmI(e, "IAmRequest");
    }
}
