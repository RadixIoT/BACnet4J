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

package com.serotonin.bacnet4j.npdu.sc;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.enums.ReadyState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.npdu.sc.msg.SCBVLC;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadAdvertisement;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadBVLCResult;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadConnectAccept;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Integration tests for the full SC stack: real SCNode, real SCHubConnector, real SCConnection
 * objects (primary + failover) — only the ScWebSocketClient and SCNetwork are mocked. This exercises the
 * lifecycle callbacks and state propagation across all three layers in concert, where the
 * per-class unit tests can only mock the adjacent layer.
 * <p>
 * Driving an event is done by calling the appropriate SCConnection callback on a real
 * connection instance (e.g. onWebsocketOpen, onWebsocketMessage). The connection updates its
 * own state machine, which fires callbacks up to the hub connector, which fires callbacks up
 * to the node — all on the test thread, because localDevice.execute is stubbed to run inline.
 */
public class SCNodeIntegrationTest {
    private static final URI PRIMARY_URI = URI.create("wss://primary.example.com/");
    private static final URI FAILOVER_URI = URI.create("wss://failover.example.com/");
    private static final OctetString LOCAL_VMAC = OctetString.fromHex("021122334455");
    private static final OctetString LOCAL_UUID = OctetString.fromHex("22112211221122112211221122112211");
    private static final OctetString PEER_VMAC = OctetString.fromHex("026677667766");
    private static final OctetString PEER_UUID = OctetString.fromHex("a0a1a2a3a4a5a6a7a8a9aaabacadaeaf");

    private static final int CONNECT_WAIT_SECS = 10;
    private static final int DISCONNECT_WAIT_SECS = 8;
    private static final int HEARTBEAT_SECS = 30;
    private static final int RECONNECT_SECS = 5;

    private SCNetwork network;
    private ScWebSocketClient primaryClient;
    private ScWebSocketClient failoverClient;
    private IntegrationNode node;
    private final Clock clock = Clock.systemUTC();

    /**
     * Pending events queued by localDevice.execute. We process them FIFO from the top-level
     * test call rather than running each one inline, because inline execution recurses
     * depth-first and breaks ordering invariants the real (single-threaded) executor would
     * enforce. Without this, e.g. SCHubConnector's STOP handler (which calls primary.terminate
     * then failover.terminate) would let primary's full restart complete before failover's
     * terminate even began.
     */
    private final Deque<Runnable> pendingEvents = new ArrayDeque<>();
    private boolean draining;

    /** Scheduled tasks captured from localDevice.schedule(...). Fire by `runnable.run()`. */
    private final List<Runnable> scheduledRunnables = new ArrayList<>();

    @Before
    public void setUp() {
        network = mock(SCNetwork.class);
        var transport = mock(Transport.class);
        var localDevice = mock(LocalDevice.class);
        primaryClient = mock(ScWebSocketClient.class);
        failoverClient = mock(ScWebSocketClient.class);
        var backoff = mock(BackoffPolicy.class);

        when(transport.getLocalDevice()).thenReturn(localDevice);
        when(localDevice.getClock()).thenReturn(clock);

        when(network.getPrimaryHub()).thenReturn(PRIMARY_URI);
        when(network.getFailoverHub()).thenReturn(FAILOVER_URI);
        when(network.getBackoffPolicy()).thenReturn(backoff);
        when(network.getConnectWaitTimeout()).thenReturn(new UnsignedInteger(CONNECT_WAIT_SECS));
        when(network.getDisconnectWaitTimeout()).thenReturn(new UnsignedInteger(DISCONNECT_WAIT_SECS));
        when(network.getHeartbeatTimeout()).thenReturn(new UnsignedInteger(HEARTBEAT_SECS));
        when(network.getVmac()).thenReturn(LOCAL_VMAC);
        when(network.getDeviceUUID()).thenReturn(LOCAL_UUID);
        when(network.getMaxBvlcLengthAccepted()).thenReturn(new UnsignedInteger(1500));
        when(network.getMaxNpduLengthAccepted()).thenReturn(new UnsignedInteger(1497));
        when(backoff.getReconnectWaitTimeout()).thenReturn(RECONNECT_SECS);

        setupClientMock(primaryClient);
        setupClientMock(failoverClient);

        // Events queued via localDevice.execute(...) are appended to a FIFO and drained from
        // the outermost call on the test thread. This mirrors a real single-threaded executor:
        // nested execute() calls enqueue, they don't recurse.
        draining = false;
        doAnswer(inv -> {
            pendingEvents.add(inv.getArgument(0));
            if (!draining) {
                draining = true;
                try {
                    while (!pendingEvents.isEmpty()) {
                        pendingEvents.poll().run();
                    }
                } finally {
                    draining = false;
                }
            }
            return null;
        }).when(localDevice).execute(any(Runnable.class));

        // Capture every schedule call. The returned mock future's cancel() is a no-op for our
        // purposes, but we keep the Runnable around so timer-driven tests can fire it manually.
        scheduledRunnables.clear();
        when(localDevice.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenAnswer(inv -> {
                    scheduledRunnables.add(inv.getArgument(0));
                    @SuppressWarnings("unchecked")
                    ScheduledFuture<Void> future = mock(ScheduledFuture.class);
                    when(future.cancel(anyBoolean())).thenReturn(true);
                    return future;
                });

        node = new IntegrationNode(network, primaryClient, failoverClient);
        node.configure(transport);
    }

    private static void setupClientMock(ScWebSocketClient client) {
        when(client.isOpen()).thenReturn(false);
        when(client.isClosing()).thenReturn(false);
        when(client.isClosed()).thenReturn(true);
        when(client.getReadyState()).thenReturn(ReadyState.NOT_YET_CONNECTED);

        // Snap the reported client state to "closed" whenever any close variant runs. Without
        // this, simulating a websocket-open (isOpen=true) leaves the mock stuck in that
        // reported state even after the SCConnection has closed it, which then breaks any
        // subsequent reconnect attempt.
        Runnable snapClosed = () -> {
            when(client.isOpen()).thenReturn(false);
            when(client.isClosing()).thenReturn(false);
            when(client.isClosed()).thenReturn(true);
            when(client.getReadyState()).thenReturn(ReadyState.CLOSED);
        };
        doAnswer(inv -> {
            snapClosed.run();
            return null;
        }).when(client).close();
        doAnswer(inv -> {
            snapClosed.run();
            return null;
        }).when(client).close(anyInt(), anyString());
        doAnswer(inv -> {
            snapClosed.run();
            return null;
        }).when(client).closeConnection(anyInt(), anyString());
    }

    // --------------------------------------------------------------------------------------
    // Real-stack test subclasses — each layer's create-hook is overridden to return a child
    // that also overrides its own create-hook. Only the very bottom is mocked.
    // --------------------------------------------------------------------------------------


    private static class IntegrationConnection extends SCConnection {
        private final ScWebSocketClient mockClient;

        IntegrationConnection(SCHubConnector owner, String name, SCNetwork network, URI uri,
                ScWebSocketClient mockClient) {
            super(owner, name, network, uri);
            this.mockClient = mockClient;
        }

        @Override
        protected ScWebSocketClient createClient() {
            return mockClient;
        }
    }


    private static class IntegrationHubConnector extends SCHubConnector {
        final ScWebSocketClient primaryClient;
        final ScWebSocketClient failoverClient;
        IntegrationConnection primary;
        IntegrationConnection failover;

        IntegrationHubConnector(SCNode node, SCNetwork network,
                ScWebSocketClient primaryClient, ScWebSocketClient failoverClient) {
            super(node, network);
            this.primaryClient = primaryClient;
            this.failoverClient = failoverClient;
        }

        @Override
        protected SCConnection createConnection(String name, SCNetwork network, URI uri) {
            IntegrationConnection conn = new IntegrationConnection(this, name, network, uri,
                    "primary".equals(name) ? primaryClient : failoverClient);
            if ("primary".equals(name)) {
                primary = conn;
            } else {
                failover = conn;
            }
            return conn;
        }
    }


    private static class IntegrationNode extends SCNode {
        final ScWebSocketClient primaryClient;
        final ScWebSocketClient failoverClient;
        IntegrationHubConnector hub;

        IntegrationNode(SCNetwork network,
                ScWebSocketClient primaryClient, ScWebSocketClient failoverClient) {
            super(network);
            this.primaryClient = primaryClient;
            this.failoverClient = failoverClient;
        }

        @Override
        protected SCHubConnector createHubConnector(SCNetwork network) {
            hub = new IntegrationHubConnector(this, network, primaryClient, failoverClient);
            return hub;
        }
    }

    // --------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------

    private void simulateOpen(ScWebSocketClient client) {
        when(client.isOpen()).thenReturn(true);
        when(client.isClosed()).thenReturn(false);
    }

    /** Returns the most recent SCBVLC message sent through the given client. */
    private SCBVLC lastSent(ScWebSocketClient client) {
        ArgumentCaptor<byte[]> cap = ArgumentCaptor.forClass(byte[].class);
        verify(client, atLeastOnce()).send(cap.capture());
        return new SCBVLC(new ByteQueue(cap.getAllValues().get(cap.getAllValues().size() - 1)));
    }

    private static SCBVLC connectAccept(int messageId) {
        return new SCBVLC(null, null, SCBVLC.CONNECT_ACCEPT,
                new SCPayloadConnectAccept(new SCVmac(PEER_VMAC.getBytes()), new SCUuid(PEER_UUID.getBytes()), 1500,
                        1497).write(), messageId);
    }

    private static SCBVLC disconnectAck(int messageId) {
        return new SCBVLC(null, null, SCBVLC.DISCONNECT_ACK, messageId);
    }

    private static SCBVLC bvlcResultNak(int messageId, ErrorCode code) {
        return new SCBVLC(null, null, SCBVLC.BVLC_RESULT,
                new SCPayloadBVLCResult(SCBVLC.CONNECT_REQUEST, 0, ErrorClass.communication, code, "").write(),
                messageId);
    }

    private void feed(SCConnection connection, SCBVLC message) {
        connection.onWebsocketMessage(new ByteQueue(message.write()));
    }

    /** Fires the most recently scheduled Runnable (typically a heartbeat or ack-wait timeout). */
    private void fireLatestScheduled() {
        scheduledRunnables.get(scheduledRunnables.size() - 1).run();
    }

    // ======================================================================================
    // Tests
    // ======================================================================================

    /**
     * End-to-end happy path: a node starts, the primary connection is initiated by the hub
     * connector, the websocket opens, the peer accepts, and the node ends up in STARTED with
     * the hub connector at CONNECTED_PRIMARY and the connection at CONNECTED. Then the node is
     * terminated and the full stack winds back down to IDLE through the normal handshake.
     * <p>
     * This proves the cross-layer wiring works:
     * - SCConnection.onConnectionEstablished flows up to SCHubConnector, which updates its
     * state and calls node.onConnected.
     * - SCNode receives CHANGE and moves to STARTED.
     * - node.terminate() flows down: SCNode emits STOP, SCHubConnector terminates each child,
     * each SCConnection sends DISCONNECT_REQUEST and moves to DISCONNECTING.
     * - The peer's DISCONNECT_ACK flips the connection back to IDLE, the hub connector sees
     * both children idle and notifies node.onConnectorIdle (which the node logs as illegal
     * in IDLE — see the note in the test).
     * - All three status surfaces report the right values at each step.
     */
    @Test
    public void integration_happyPath_primaryConnectAndDisconnect() {
        // Pre-state: everything IDLE.
        assertEquals(SCNode.State.IDLE, node.getState());
        assertEquals(SCHubConnector.State.IDLE, node.hub.getState());
        assertEquals(SCConnection.State.IDLE, node.hub.primary.getState());
        assertEquals(SCConnection.State.IDLE, node.hub.failover.getState());
        assertEquals(SCHubConnectorState.noHubConnection, node.getHubConnectorState());

        // 1. Start. Node -> STARTING, hub -> WAIT_PRIMARY, primary -> AWAITING_WEBSOCKET.
        node.initialize();
        assertEquals(SCNode.State.STARTING, node.getState());
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, node.hub.getState());
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, node.hub.primary.getState());
        verify(primaryClient).connect();
        // Failover is untouched.
        assertEquals(SCConnection.State.IDLE, node.hub.failover.getState());
        verify(failoverClient, never()).connect();

        // 2. WebSocket opens. Primary -> AWAITING_ACCEPT, Connect-Request goes out.
        simulateOpen(primaryClient);
        node.hub.primary.onWebsocketOpen();
        assertEquals(SCConnection.State.AWAITING_ACCEPT, node.hub.primary.getState());

        SCBVLC connectReq = lastSent(primaryClient);
        assertEquals(SCBVLC.CONNECT_REQUEST, connectReq.getFunction());

        // 3. Peer accepts. Connection -> CONNECTED, hub -> CONNECTED_PRIMARY, node -> STARTED.
        feed(node.hub.primary, connectAccept(connectReq.getId()));

        assertEquals(SCConnection.State.CONNECTED, node.hub.primary.getState());
        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, node.hub.getState());
        assertEquals(SCNode.State.STARTED, node.getState());

        // All three status surfaces align.
        assertEquals(SCHubConnectorState.connectedToPrimary, node.getHubConnectorState());
        assertEquals(SCConnectionState.connected,
                node.getPrimaryHubConnectionStatus().getConnectionState());
        DateTime connectTimestamp = node.getPrimaryHubConnectionStatus().getConnectTimestamp();
        TestUtils.assertEquals(new DateTime(clock.millis()), connectTimestamp, 10);

        // 4. Local disconnect. Node -> IDLE immediately (STOP handler), hub -> STOPPING,
        //    primary -> DISCONNECTING (Disconnect-Request sent).
        clearInvocations(primaryClient); // reset send count so lastSent picks the disconnect
        node.terminate();

        assertEquals(SCNode.State.STOPPING, node.getState());
        assertEquals(SCHubConnector.State.STOPPING, node.hub.getState());
        assertEquals(SCConnection.State.DISCONNECTING, node.hub.primary.getState());
        SCBVLC disconnectReq = lastSent(primaryClient);
        assertEquals(SCBVLC.DISCONNECT_REQUEST, disconnectReq.getFunction());

        // 5. Peer acks. Primary -> IDLE, hub -> IDLE, node stays IDLE.
        feed(node.hub.primary, disconnectAck(disconnectReq.getId()));

        assertEquals(SCConnection.State.IDLE, node.hub.primary.getState());
        assertEquals(SCHubConnector.State.IDLE, node.hub.getState());
        assertEquals(SCNode.State.IDLE, node.getState());

        // Status surfaces reflect a clean shutdown.
        assertEquals(SCHubConnectorState.noHubConnection, node.getHubConnectorState());
        assertEquals(SCConnectionState.notConnected,
                node.getPrimaryHubConnectionStatus().getConnectionState());
        assertEquals(connectTimestamp,
                node.getPrimaryHubConnectionStatus().getConnectTimestamp());
        TestUtils.assertEquals(new DateTime(clock.millis()),
                node.getPrimaryHubConnectionStatus().getDisconnectTimestamp(), 10);
    }

    /**
     * Duplicate-VMAC recovery from the perspective of the whole stack:
     * - Node starts, primary connects via TLS+WS, sends Connect-Request.
     * - Peer responds with BVLC-Result NAK / nodeDuplicateVmac (spec AB.6.2.2).
     * - SCConnection short-circuits to IDLE and calls owner.restartWithNewVMAC().
     * - SCHubConnector relays to node.restartWithNewVMAC().
     * - SCNode: STARTING → NEW_MAC_STOPPING; calls hubConnector.terminate().
     * - SCHubConnector terminates both children → STOPPING; children flip to IDLE.
     * - SCHubConnector reports onConnectorIdle to the node.
     * - SCNode rotates the VMAC on the network and re-enters STARTING with a fresh primary
     * attempt.
     * <p>
     * Proves the duplicate-VMAC handshake works end-to-end and that the VMAC is actually
     * rotated by SCNetwork.setVmac(...) before the restart attempt.
     */
    @Test
    public void integration_vmacCollision_restartsWithNewVmac() {
        node.initialize();
        simulateOpen(primaryClient);
        node.hub.primary.onWebsocketOpen();
        SCBVLC connectReq = lastSent(primaryClient);
        assertEquals(SCBVLC.CONNECT_REQUEST, connectReq.getFunction());

        // Peer says: that VMAC is already in use.
        feed(node.hub.primary, bvlcResultNak(connectReq.getId(), ErrorCode.nodeDuplicateVmac));

        // Final state: the node rotated VMAC, hubConnector re-initialized, primary is back in
        // AWAITING_WEBSOCKET. The whole rotation happened synchronously through inline execute.
        assertEquals(SCNode.State.STARTING, node.getState());
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, node.hub.getState());
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, node.hub.primary.getState());

        // Network was asked to rotate VMAC.
        verify(network).setVmac(any(OctetString.class));

        // The hub connector and node never observed a "connected" state across this flow.
        assertEquals(SCHubConnectorState.noHubConnection, node.getHubConnectorState());
    }

    /**
     * Primary fails to connect, failover takes over, then the primary recovers and the
     * connector falls back to primary-only.
     * <p>
     * Phase 1 — failover takeover:
     * - Node starts, primary connection attempt fails (simulated by onWebsocketError).
     * - SCConnection records the error and notifies the hub connector with
     * onConnectionIdle(wasEstablished=false).
     * - SCHubConnector: WAIT_PRIMARY → TRY_FAILOVER → WAIT_FAILOVER. Failover gets initialize.
     * - Failover websocket opens, peer accepts, hub connector → CONNECTED_FAILOVER, node → STARTED.
     * - The retry-primary timer is armed.
     * <p>
     * Phase 2 — primary recovery:
     * - The retry-primary timer fires. Connector → REWAIT_PRIMARY and re-initializes the primary.
     * - Primary websocket opens, peer accepts, connector → CONNECTED_PRIMARY, failover is
     * terminated (Disconnect-Request sent).
     * - The failover acks the disconnect and lands back in IDLE.
     * - Final stack: node STARTED, hub CONNECTED_PRIMARY, primary CONNECTED, failover IDLE.
     * Status surface reports connectedToPrimary.
     */
    @Test
    public void integration_primaryFails_failoverTakesOver_thenPrimaryRecovers() {
        node.initialize();
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, node.hub.primary.getState());

        // Primary fails before the websocket ever opens (e.g., TLS handshake error).
        node.hub.primary.onWebsocketError(ErrorCode.tlsError, "TLS handshake failed");

        // Primary's connection-status surface reports the failure cause.
        assertEquals(SCConnection.State.IDLE, node.hub.primary.getState());
        assertEquals(SCConnectionState.failedToConnect,
                node.getPrimaryHubConnectionStatus().getConnectionState());
        // Hub connector switched over to attempting failover.
        assertEquals(SCHubConnector.State.WAIT_FAILOVER, node.hub.getState());
        verify(failoverClient).connect();

        // Failover establishes successfully.
        simulateOpen(failoverClient);
        node.hub.failover.onWebsocketOpen();
        SCBVLC failoverConnectReq = lastSent(failoverClient);
        assertEquals(SCBVLC.CONNECT_REQUEST, failoverConnectReq.getFunction());

        feed(node.hub.failover, connectAccept(failoverConnectReq.getId()));

        // Whole stack now reports the failover connection.
        assertEquals(SCConnection.State.CONNECTED, node.hub.failover.getState());
        assertEquals(SCHubConnector.State.CONNECTED_FAILOVER, node.hub.getState());
        assertEquals(SCNode.State.STARTED, node.getState());

        assertEquals(SCHubConnectorState.connectedToFailover, node.getHubConnectorState());
        assertEquals(SCConnectionState.connected,
                node.getFailoverHubConnectionStatus().getConnectionState());
        // Primary status retains the prior error — it's the surface a network port object would
        // expose on the next read.
        assertEquals(SCConnectionState.failedToConnect,
                node.getPrimaryHubConnectionStatus().getConnectionState());
        assertEquals(DateTime.UNSPECIFIED, node.getPrimaryHubConnectionStatus().getConnectTimestamp());
        assertEquals(DateTime.UNSPECIFIED, node.getPrimaryHubConnectionStatus().getDisconnectTimestamp());
        assertEquals(new ErrorClassAndCode(ErrorClass.communication, ErrorCode.tlsError),
                node.getPrimaryHubConnectionStatus().getError());
        assertEquals(new CharacterString("TLS handshake failed"),
                node.getPrimaryHubConnectionStatus().getErrorDetails());

        // === Phase 2: primary recovers ===

        // The retry-primary timer was armed when WAIT_FAILOVER → CONNECTED_FAILOVER. Firing it
        // moves the connector to REWAIT_PRIMARY (a substate of CONNECTED_FAILOVER) and triggers a
        // fresh primary connection attempt. The primary's websocket client has been closed since
        // the original failure (getReadyState=CLOSED), so SCConnection issues reconnect() rather
        // than connect() this time.
        clearInvocations(primaryClient);
        fireLatestScheduled();

        assertEquals(SCHubConnector.State.REWAIT_PRIMARY, node.hub.getState());
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, node.hub.primary.getState());
        // Failover is still serving traffic; status surface still reports failover.
        assertEquals(SCConnection.State.CONNECTED, node.hub.failover.getState());
        assertEquals(SCHubConnectorState.connectedToFailover, node.getHubConnectorState());
        verify(primaryClient).reconnect();

        // Primary websocket opens; SCConnection sends a new Connect-Request.
        simulateOpen(primaryClient);
        node.hub.primary.onWebsocketOpen();
        SCBVLC primaryRetryReq = lastSent(primaryClient);
        assertEquals(SCBVLC.CONNECT_REQUEST, primaryRetryReq.getFunction());

        // Peer accepts. REWAIT_PRIMARY → CONNECTED_PRIMARY, and the connector terminates the
        // failover synchronously. The failover sends Disconnect-Request and transitions to
        // DISCONNECTING; it will finish closing when its peer acks.
        feed(node.hub.primary, connectAccept(primaryRetryReq.getId()));

        assertEquals(SCConnection.State.CONNECTED, node.hub.primary.getState());
        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, node.hub.getState());
        assertEquals(SCNode.State.STARTED, node.getState());

        assertEquals(SCConnection.State.DISCONNECTING, node.hub.failover.getState());
        SCBVLC failoverDisconnectReq = lastSent(failoverClient);
        assertEquals(SCBVLC.DISCONNECT_REQUEST, failoverDisconnectReq.getFunction());

        // Status surface now reports the primary as the active connection.
        assertEquals(SCHubConnectorState.connectedToPrimary, node.getHubConnectorState());
        assertEquals(SCConnectionState.connected,
                node.getPrimaryHubConnectionStatus().getConnectionState());
        // Earlier primary error is cleared on a successful (re)connect.
        TestUtils.assertEquals(new DateTime(clock.millis()),
                node.getPrimaryHubConnectionStatus().getConnectTimestamp(), 10);

        // Failover finishes its goodbye: peer acks, failover transitions to IDLE.
        feed(node.hub.failover, disconnectAck(failoverDisconnectReq.getId()));

        assertEquals(SCConnection.State.IDLE, node.hub.failover.getState());
        // Primary remains the only active connection.
        assertEquals(SCConnection.State.CONNECTED, node.hub.primary.getState());
        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, node.hub.getState());
        assertEquals(SCNode.State.STARTED, node.getState());
        assertEquals(SCHubConnectorState.connectedToPrimary, node.getHubConnectorState());
    }

    /**
     * Duplicate-VMAC NAK that arrives AFTER the node has reached STARTED (the I4 fix path):
     * - Node is fully started on the primary.
     * - The primary hub connection drops normally (1000).
     * - SCHubConnector attempts to re-establish the primary (AB.5.2 primary-first).
     * - Primary websocket re-opens and re-issues Connect-Request.
     * - Peer NAKs the new Connect-Request with nodeDuplicateVmac.
     * - SCConnection emits restartWithNewVMAC → SCHubConnector → SCNode (in STARTED).
     * - SCNode handles NEW_MAC from STARTED (the recently-added route) and transitions through
     * NEW_MAC_STOPPING, terminating the hub connector and rotating the VMAC.
     * - The connector idles, the node rotates VMAC, re-initializes, and we end back at STARTING
     * on the primary with the new VMAC.
     * <p>
     * Before the I4 fix, the SCNode.STARTED + NEW_MAC path was illegal and the VMAC was never
     * rotated. This test pins the corrected behavior across all three layers.
     */
    @Test
    public void integration_vmacCollisionAfterStartedRotatesAndRecovers() {
        // 1. Get to STARTED on the primary.
        node.initialize();
        simulateOpen(primaryClient);
        node.hub.primary.onWebsocketOpen();
        SCBVLC primaryConnectReq = lastSent(primaryClient);
        feed(node.hub.primary, connectAccept(primaryConnectReq.getId()));
        assertEquals(SCNode.State.STARTED, node.getState());

        // 2. Primary drops cleanly. Post-fix (AB.5.2), the connector attempts to re-establish
        //    the primary first (rather than immediately falling over to failover).
        node.hub.primary.onWebsocketClose(1000, "normal");
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, node.hub.getState());
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, node.hub.primary.getState());
        // Node stays STARTED through the reconnect (DISCONNECTED is ignored in STARTED).
        assertEquals(SCNode.State.STARTED, node.getState());

        // 3. Primary websocket re-opens; SCConnection sends a fresh Connect-Request. Peer
        //    NAKs it with duplicate-VMAC.
        simulateOpen(primaryClient);
        node.hub.primary.onWebsocketOpen();
        SCBVLC retryConnectReq = lastSent(primaryClient);
        feed(node.hub.primary, bvlcResultNak(retryConnectReq.getId(), ErrorCode.nodeDuplicateVmac));

        // 4. NEW_MAC cascades up: SCConnection → SCHubConnector → SCNode(STARTED) → NEW_MAC_STOPPING.
        //    The connector terminates and both children idle. The node rotates VMAC and restarts.
        assertEquals(SCNode.State.STARTING, node.getState());
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, node.hub.getState());
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, node.hub.primary.getState());
        verify(network).setVmac(any(OctetString.class));
    }

    /**
     * Heartbeat ACK never arrives → Local Disconnection cascade (AB.6.3 Protocol_Revision 24).
     * Verifies that when SCConnection's ack-wait timeout fires, the resulting DISCONNECT event
     * propagates correctly through all three layers:
     * - SCConnection: CONNECTED → DISCONNECTING (sends Disconnect-Request).
     * - After Disconnect-ACK: SCConnection → IDLE, SCHubConnector attempts to re-establish
     * the primary connection first (AB.5.2 primary-first re-establishment).
     * - SCNode remains STARTED throughout because DISCONNECTED in STARTED is silently ignored.
     */
    @Test
    public void integration_heartbeatAckTimeoutTriggersLocalDisconnect() {
        // 1. Get to STARTED on the primary.
        node.initialize();
        simulateOpen(primaryClient);
        node.hub.primary.onWebsocketOpen();
        SCBVLC connectReq = lastSent(primaryClient);
        feed(node.hub.primary, connectAccept(connectReq.getId()));
        assertEquals(SCNode.State.STARTED, node.getState());

        // 2. Arm the heartbeat by feeding a benign incoming message. Heartbeat is only armed
        //    when a message arrives in CONNECTED.
        feed(node.hub.primary, new SCBVLC(new SCVmac(PEER_VMAC.getBytes()), null, SCBVLC.ADVERTISEMENT,
                new SCPayloadAdvertisement(1, false, 1500, 1497).write(), 0));

        // 3. Fire the heartbeat timer → SCConnection sends Heartbeat-Request and arms ack-wait.
        clearInvocations(primaryClient);
        fireLatestScheduled();
        SCBVLC heartbeat = lastSent(primaryClient);
        assertEquals(SCBVLC.HEARTBEAT_REQUEST, heartbeat.getFunction());

        // 4. Fire the ack-wait timer (no ACK ever arrives) → SCConnection initiates local disconnect.
        clearInvocations(primaryClient);
        fireLatestScheduled();

        // Cascade: SCConnection → DISCONNECTING with Disconnect-Request sent.
        assertEquals(SCConnection.State.DISCONNECTING, node.hub.primary.getState());
        SCBVLC disconnectReq = lastSent(primaryClient);
        assertEquals(SCBVLC.DISCONNECT_REQUEST, disconnectReq.getFunction());
        // Hub connector and node still report connected — the disconnect handshake hasn't completed.
        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, node.hub.getState());
        assertEquals(SCNode.State.STARTED, node.getState());

        // 5. Peer ACKs the disconnect. SCConnection → IDLE briefly, then re-initializes (primary
        //    first per AB.5.2). The primary connection ends up in AWAITING_WEBSOCKET.
        feed(node.hub.primary, disconnectAck(disconnectReq.getId()));
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, node.hub.primary.getState());
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, node.hub.getState());
        // Node stays STARTED — DISCONNECTED in STARTED is silently ignored so the connector can
        // bring the connection back without bouncing the upper layer.
        assertEquals(SCNode.State.STARTED, node.getState());
        verify(primaryClient).reconnect();
    }

    /**
     * End-to-end NPDU round-trip across all three layers:
     * - Outbound: node.sendMessage(npdu) → SCHubConnector → primary.sendMessage → client.send.
     * - Inbound: peer Encapsulated-NPDU → SCConnection.onWebsocketMessage → SCHubConnector
     * → SCNode → network.onIncoming.
     * <p>
     * Verifies the data path is wired correctly across the stack.
     */
    @Test
    public void integration_npduTrafficRoundTripWhileConnected() {
        // 1. Get to STARTED on the primary.
        node.initialize();
        simulateOpen(primaryClient);
        node.hub.primary.onWebsocketOpen();
        SCBVLC connectReq = lastSent(primaryClient);
        feed(node.hub.primary, connectAccept(connectReq.getId()));
        assertEquals(SCNode.State.STARTED, node.getState());
        clearInvocations(primaryClient);

        // 2. Outbound: send an NPDU through the node. It should reach the wire via primary.
        byte[] npdu = new byte[] {0x01, 0x02, 0x03};
        node.sendMessage(new SCBVLC(null, null, SCBVLC.ENCAPSULATED_NPDU, npdu));
        SCBVLC sent = lastSent(primaryClient);
        assertEquals(SCBVLC.ENCAPSULATED_NPDU, sent.getFunction());
        org.junit.Assert.assertArrayEquals(npdu, sent.getPayload());

        // 3. Inbound: peer sends an Encapsulated-NPDU. It should reach network.onIncoming.
        SCBVLC incoming = new SCBVLC(new SCVmac(PEER_VMAC.getBytes()), null, SCBVLC.ENCAPSULATED_NPDU,
                new byte[] {0x04, 0x05}, 99);
        feed(node.hub.primary, incoming);
        verify(network).onIncoming(org.mockito.ArgumentMatchers.argThat(
                m -> m != null && m.getFunction() == SCBVLC.ENCAPSULATED_NPDU));
    }
}
