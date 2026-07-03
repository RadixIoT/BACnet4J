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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.CloseFrame;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.sc.msg.SCBVLC;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadAdvertisement;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadBVLCResult;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadConnectAccept;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.SCHubConnection;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * State machine coverage for SCConnection. One test per legal (state, event) transition plus
 * smoke tests for illegal events per state. The WebSocket client is a Mockito mock returned by
 * a test subclass that overrides createClient(); no real network or SSL setup occurs.
 * <p>
 * Event delivery now flows through localDevice.execute(...). In tests we stub execute() to
 * invoke the Runnable inline so the SM runs synchronously on the calling thread.
 */
public class SCConnectionTest {
    private static final URI WSS_URI = URI.create("wss://example.com:47808/");
    private static final byte[] LOCAL_VMAC = {0x02, 0x11, 0x22, 0x33, 0x44, 0x55};
    private static final byte[] LOCAL_UUID = new byte[16];
    private static final byte[] PEER_VMAC = {0x02, 0x66, 0x77, 0x66, 0x77, 0x66};
    private static final byte[] PEER_UUID = new byte[16];

    static {
        for (int i = 0; i < 16; i++) {
            PEER_UUID[i] = (byte) (0xA0 + i);
        }
    }

    private static final int CONNECT_WAIT_SECS = 10;
    private static final int DISCONNECT_WAIT_SECS = 8;
    private static final int HEARTBEAT_SECS = 30;

    private SCHubConnector owner;
    private SCNetwork network;
    private Transport transport;
    private ScWebSocketClient client;
    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();
    private TestConnection connection;


    /** Captures everything we want to assert about a localDevice.schedule(...) call. */
    private static final class ScheduledTask {
        final Runnable runnable;
        final long delay;
        final TimeUnit unit;
        boolean canceled;

        ScheduledTask(Runnable runnable, long delay, TimeUnit unit) {
            this.runnable = runnable;
            this.delay = delay;
            this.unit = unit;
        }
    }

    @Before
    public void setUp() {
        owner = mock(SCHubConnector.class);
        network = mock(SCNetwork.class);
        transport = mock(Transport.class);
        var localDevice = mock(LocalDevice.class);
        client = mock(ScWebSocketClient.class);

        when(transport.getLocalDevice()).thenReturn(localDevice);
        when(localDevice.getClock()).thenReturn(Clock.systemUTC());

        // Run every queued event inline so the SM runs synchronously on the test thread.
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(localDevice).execute(any(Runnable.class));

        when(network.getConnectWaitTimeout()).thenReturn(new UnsignedInteger(CONNECT_WAIT_SECS));
        when(network.getDisconnectWaitTimeout()).thenReturn(new UnsignedInteger(DISCONNECT_WAIT_SECS));
        when(network.getHeartbeatTimeout()).thenReturn(new UnsignedInteger(HEARTBEAT_SECS));
        when(network.getVmac()).thenReturn(new OctetString(LOCAL_VMAC));
        when(network.getDeviceUUID()).thenReturn(new OctetString(LOCAL_UUID));
        when(network.getMaxBvlcLengthAccepted()).thenReturn(new UnsignedInteger(1500));
        when(network.getMaxNpduLengthAccepted()).thenReturn(new UnsignedInteger(1497));

        when(client.isOpen()).thenReturn(false);
        when(client.isClosing()).thenReturn(false);
        when(client.isClosed()).thenReturn(true);
        when(client.getReadyState()).thenReturn(ReadyState.NOT_YET_CONNECTED);

        // Each schedule() call captures the runnable, delay, and unit; the returned mock future
        // routes cancel(...) back into the task so tests can assert cancellation occurred.
        scheduledTasks.clear();
        when(localDevice.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
                .thenAnswer(inv -> {
                    ScheduledTask task = new ScheduledTask(
                            inv.getArgument(0), inv.getArgument(1), inv.getArgument(2));
                    scheduledTasks.add(task);
                    @SuppressWarnings("unchecked")
                    ScheduledFuture<Void> future = mock(ScheduledFuture.class);
                    when(future.cancel(anyBoolean())).thenAnswer(c -> {
                        task.canceled = true;
                        return true;
                    });
                    return future;
                });

        connection = new TestConnection(owner, "test", network, WSS_URI, client);
        connection.configure(transport);
    }

    // ---- Scheduled-task helpers ----

    /** Fires the i-th scheduled task's runnable. */
    private void fireScheduled(int index) {
        scheduledTasks.get(index).runnable.run();
    }

    private ScheduledTask lastTask() {
        return scheduledTasks.get(scheduledTasks.size() - 1);
    }

    private void assertScheduledFor(ScheduledTask task, int expectedSeconds) {
        assertEquals("scheduled unit", TimeUnit.SECONDS, task.unit);
        assertEquals("scheduled delay (seconds)", expectedSeconds, task.delay);
    }

    private void assertCanceled(ScheduledTask task) {
        assertTrue("task canceled", task.canceled);
    }

    private void assertNotCanceled(ScheduledTask task) {
        assertFalse("task not canceled", task.canceled);
    }

    // ---- Connection-status (SCHubConnection) helpers ----

    /** Returns the connection's current SCHubConnection after asserting the connection-state field. */
    private SCHubConnection assertConnectionState(SCConnection conn, SCConnectionState expectedState) {
        SCHubConnection status = conn.getConnectionStatus();
        assertEquals("connectionState", expectedState, status.getConnectionState());
        return status;
    }

    private SCHubConnection assertConnectionState(SCConnectionState expectedState) {
        return assertConnectionState(connection, expectedState);
    }

    /** Asserts state + error class/code + details on the SCHubConnection. */
    private void assertConnectionError(SCConnection conn, SCConnectionState expectedState,
            ErrorCode expectedCode, String expectedDetails) {
        SCHubConnection status = assertConnectionState(conn, expectedState);
        assertNotNull("expected an error", status.getError());
        assertTrue("error class/code mismatch (got " + status.getError() + ")",
                status.getError().equals(ErrorClass.communication, expectedCode));
        if (expectedDetails == null) {
            assertNull("error details should be null", status.getErrorDetails());
        } else {
            assertNotNull("expected error details", status.getErrorDetails());
            assertEquals(expectedDetails, status.getErrorDetails().toString());
        }
    }

    private void assertConnectionError(SCConnectionState expectedState, ErrorCode expectedCode,
            String expectedDetails) {
        assertConnectionError(connection, expectedState, expectedCode, expectedDetails);
    }

    /** Asserts state + no error + no error details. */
    private void assertConnectionStatusOk(SCConnectionState expectedState) {
        SCHubConnection status = assertConnectionState(expectedState);
        assertNull("error should be cleared on success", status.getError());
        assertNull("error details should be cleared on success", status.getErrorDetails());
    }

    // --------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------


    private static class TestConnection extends SCConnection {
        private final ScWebSocketClient mockClient;

        TestConnection(SCHubConnector owner, String name, SCNetwork network, URI uri, ScWebSocketClient client) {
            super(owner, name, network, uri);
            this.mockClient = client;
        }

        @Override
        protected ScWebSocketClient createClient() {
            return mockClient;
        }
    }

    /** Sets client.isOpen()=true / isClosed()=false, mirroring a successful WebSocket upgrade. */
    private void simulateClientOpen() {
        when(client.isOpen()).thenReturn(true);
        when(client.isClosed()).thenReturn(false);
    }

    private SCBVLC lastSent() {
        ArgumentCaptor<byte[]> c = ArgumentCaptor.forClass(byte[].class);
        verify(client, atLeastOnce()).send(c.capture());
        byte[] bytes = c.getAllValues().get(c.getAllValues().size() - 1);
        return new SCBVLC(new ByteQueue(bytes));
    }

    private int countSentMessages() {
        ArgumentCaptor<byte[]> c = ArgumentCaptor.forClass(byte[].class);
        verify(client, atLeast(0)).send(c.capture());
        return c.getAllValues().size();
    }

    private void feedMessage(SCBVLC message) {
        connection.onWebsocketMessage(new ByteQueue(message.write()));
    }

    /** Drives the SM IDLE -> AWAITING_WEBSOCKET. */
    private void enterAwaitingWebsocket() {
        connection.initialize();
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, connection.getState());
    }

    /** Drives the SM IDLE -> AWAITING_ACCEPT. Returns the Connect-Request message id. */
    private int enterAwaitingAccept() {
        enterAwaitingWebsocket();
        simulateClientOpen();
        connection.onWebsocketOpen();
        assertEquals(SCConnection.State.AWAITING_ACCEPT, connection.getState());
        SCBVLC sent = lastSent();
        assertEquals(SCBVLC.CONNECT_REQUEST, sent.getFunction());
        return sent.getId();
    }

    /** Drives the SM IDLE -> CONNECTED. Returns the Connect-Request/Accept message id used. */
    private int enterConnected() {
        int connectId = enterAwaitingAccept();
        feedMessage(connectAccept(connectId));
        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        return connectId;
    }

    /** Drives the SM IDLE -> DISCONNECTING. Returns the Disconnect-Request message id. */
    private int enterDisconnecting() {
        enterConnected();
        connection.terminate();
        assertEquals(SCConnection.State.DISCONNECTING, connection.getState());
        SCBVLC sent = lastSent();
        assertEquals(SCBVLC.DISCONNECT_REQUEST, sent.getFunction());
        return sent.getId();
    }

    // ---- Message builders ----

    private static SCBVLC connectAccept(int messageId) {
        return new SCBVLC(null, null, SCBVLC.CONNECT_ACCEPT,
                new SCPayloadConnectAccept(new SCVmac(PEER_VMAC), new SCUuid(PEER_UUID), 1500, 1497).write(),
                messageId);
    }

    private static SCBVLC bvlcResultNak(int messageId, ErrorCode code) {
        return new SCBVLC(null, null, SCBVLC.BVLC_RESULT,
                new SCPayloadBVLCResult(SCBVLC.CONNECT_REQUEST, 0, ErrorClass.communication, code, "").write(),
                messageId);
    }

    private static SCBVLC disconnectRequest(int messageId) {
        return new SCBVLC(null, null, SCBVLC.DISCONNECT_REQUEST, messageId);
    }

    private static SCBVLC disconnectAck(int messageId) {
        return new SCBVLC(null, null, SCBVLC.DISCONNECT_ACK, messageId);
    }

    private static SCBVLC heartbeatRequest(int messageId) {
        return new SCBVLC(null, null, SCBVLC.HEARTBEAT_REQUEST, messageId);
    }

    private static SCBVLC heartbeatAck(int messageId) {
        return new SCBVLC(null, null, SCBVLC.HEARTBEAT_ACK, messageId);
    }

    // "Switchable" messages received over a hub connection MUST have a non-null originating
    // (the original sender's VMAC; the hub inserts it). parseMessage rejects them otherwise.
    private static final SCVmac PEER_ORIGIN = new SCVmac(PEER_VMAC);

    private static SCBVLC encapsulatedNpdu(int messageId) {
        return new SCBVLC(PEER_ORIGIN, null, SCBVLC.ENCAPSULATED_NPDU, new byte[] {0x01, 0x02}, messageId);
    }

    private static SCBVLC addressResolution(int messageId) {
        return new SCBVLC(PEER_ORIGIN, null, SCBVLC.ADDRESS_RESOLUTION, messageId);
    }

    private static SCBVLC advertisement(int messageId) {
        // SCPayloadAdvertisement = 1B connStatus + 1B acceptConns + 2B maxBVLC + 2B maxNPDU = 6 bytes.
        return new SCBVLC(PEER_ORIGIN, null, SCBVLC.ADVERTISEMENT,
                new SCPayloadAdvertisement(0, false, 1500, 1497).write(), messageId);
    }

    private static SCBVLC advertisementSolicitation(int messageId) {
        return new SCBVLC(PEER_ORIGIN, null, SCBVLC.ADVERTISEMENT_SOLICITATION, messageId);
    }

    private static SCBVLC bvlcWithFunction(int function) {
        return org.mockito.ArgumentMatchers.argThat(b -> b != null && b.getFunction() == function);
    }

    // ======================================================================================
    // IDLE
    // ======================================================================================

    @Test
    public void idle_initiate_validScheme_movesToAwaitingWebsocketAndCallsConnect() {
        connection.initialize();
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, connection.getState());
        verify(client).connect();

        // The connect-wait timer is armed exactly once with the connect-wait timeout.
        assertEquals(1, scheduledTasks.size());
        assertScheduledFor(lastTask(), CONNECT_WAIT_SECS);
        assertNotCanceled(lastTask());
    }

    @Test
    public void idle_initiate_clientAlreadyConnected_callsReconnectNotConnect() {
        when(client.getReadyState()).thenReturn(ReadyState.CLOSED);
        connection.initialize();
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, connection.getState());
        verify(client, never()).connect();
        verify(client).reconnect();
    }

    @Test
    public void idle_initiate_invalidScheme_staysIdleAndNotifiesOwner() {
        TestConnection bad = new TestConnection(owner, "bad", network,
                URI.create("http://example.com/"), client);
        bad.configure(transport);

        bad.initialize();

        assertEquals(SCConnection.State.IDLE, bad.getState());
        verify(client, never()).connect();
        verify(client, never()).reconnect();
        verify(owner).onConnectionIdle(bad, false);
        assertEquals(0, scheduledTasks.size());
        // Status surfaces the failure with a specific error code.
        assertConnectionError(bad, SCConnectionState.failedToConnect,
                ErrorCode.websocketSchemeNotSupported, null);
    }

    // (Deleted: idle_initiate_clientStillOpen_* and idle_initiate_clientStillClosing_*. The
    // SM no longer has a "client in weird state" early-close branch — it just dispatches to
    // connect() or reconnect() based on getReadyState. The connect/reconnect branching is
    // covered by idle_initiate_clientAlreadyConnected_callsReconnectNotConnect above.)

    @Test
    public void idle_timeout_isIllegal() {
        // TIMEOUT can only be delivered while a timer is armed. Drive the SM into
        // AWAITING_WEBSOCKET so a Runnable is scheduled, transition back to IDLE via
        // terminate(), then fire the stale Runnable.
        connection.initialize();
        connection.terminate();
        assertEquals(SCConnection.State.IDLE, connection.getState());

        fireScheduled(0); // delivers Event.TIMEOUT to IDLE

        // IDLE now treats TIMEOUT as illegal — no transition, no client interaction.
        assertEquals(SCConnection.State.IDLE, connection.getState());
    }

    @Test
    public void idle_disconnect_notifiesOwnerStaysIdle() {
        connection.terminate();
        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client, never()).connect();
        verify(client, never()).close();
        verify(client, never()).closeConnection(anyInt(), anyString());
        verify(owner).onConnectionIdle(connection, false);
        assertEquals(0, scheduledTasks.size());
    }

    @Test
    public void idle_remoteClose_isSilentlyIgnored() {
        connection.onWebsocketClose(1000, "after a clean disconnect");
        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(owner, never()).onConnectionIdle(any(SCConnection.class), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    public void idle_accept_isIllegal() {
        connection.onWebsocketOpen();
        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client, never()).send(any(byte[].class));
    }

    @Test
    public void idle_message_isIllegal() {
        feedMessage(heartbeatRequest(1));
        assertEquals(SCConnection.State.IDLE, connection.getState());
    }

    @Test
    public void idle_connectError_isIllegal() {
        connection.onWebsocketError(ErrorCode.tlsError, "test error");
        assertEquals(SCConnection.State.IDLE, connection.getState());
    }

    // ======================================================================================
    // AWAITING_WEBSOCKET
    // ======================================================================================

    @Test
    public void awaitingWebsocket_accept_sendsConnectRequestAndAwaitsAccept() {
        enterAwaitingWebsocket();
        ScheduledTask websocketTimer = lastTask();

        simulateClientOpen();
        connection.onWebsocketOpen();

        assertEquals(SCConnection.State.AWAITING_ACCEPT, connection.getState());
        assertEquals(SCBVLC.CONNECT_REQUEST, lastSent().getFunction());

        // The previous connect-wait timer was canceled; a fresh one was armed for AWAITING_ACCEPT.
        assertEquals(2, scheduledTasks.size());
        assertCanceled(websocketTimer);
        assertScheduledFor(lastTask(), CONNECT_WAIT_SECS);
        assertNotCanceled(lastTask());
    }

    @Test
    public void awaitingWebsocket_timeout_closesAndReturnsToIdle() {
        enterAwaitingWebsocket();
        ScheduledTask websocketTimer = lastTask();
        assertScheduledFor(websocketTimer, CONNECT_WAIT_SECS);

        websocketTimer.runnable.run(); // fires TIMEOUT in AWAITING_WEBSOCKET

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close();
        verify(owner).onConnectionIdle(connection, false);
        // connectionClosed() cancels the timer (now-stale but documented behavior).
        assertCanceled(websocketTimer);
    }

    @Test
    public void awaitingWebsocket_connectError_closesAndReturnsToIdle() {
        enterAwaitingWebsocket();

        connection.onWebsocketError(ErrorCode.tlsError, "TLS failed");

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close();
        verify(owner).onConnectionIdle(connection, false);
        // Status records the WebSocket-level error code and the supplied details.
        assertConnectionError(SCConnectionState.failedToConnect, ErrorCode.tlsError, "TLS failed");
        assertEquals(DateTime.UNSPECIFIED, connection.getConnectionStatus().getDisconnectTimestamp());
    }

    @Test
    public void awaitingWebsocket_disconnect_closesAndReturnsToIdle() {
        enterAwaitingWebsocket();

        connection.terminate();

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close();
        verify(owner).onConnectionIdle(connection, false);
    }

    @Test
    public void awaitingWebsocket_remoteClose_closesAndReturnsToIdle() {
        enterAwaitingWebsocket();

        connection.onWebsocketClose(1006, "RST");

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close();
        verify(owner).onConnectionIdle(connection, false);
    }

    @Test
    public void awaitingWebsocket_clientNotOpenWithAccept_closesAndReturnsToIdle() {
        // Per the SM, if !client.isOpen() the close branch wins regardless of event.
        enterAwaitingWebsocket();

        connection.onWebsocketOpen(); // ACCEPT, but client.isOpen() is still false

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close();
    }

    @Test
    public void awaitingWebsocket_initiate_isIllegal() {
        enterAwaitingWebsocket();
        simulateClientOpen();

        connection.initialize(); // INITIATE in AWAITING_WEBSOCKET

        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, connection.getState());
    }

    @Test
    public void awaitingWebsocket_message_isIllegal() {
        enterAwaitingWebsocket();
        simulateClientOpen();

        feedMessage(heartbeatRequest(1));

        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, connection.getState());
    }

    @Test
    public void awaitingWebsocket_textData_closesWithRefuseAndReturnsToIdle() {
        // AB.7.5.3: any non-binary frame received must close the WebSocket with 1003.
        enterAwaitingWebsocket();

        connection.onTextData("garbage");

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(eq(CloseFrame.REFUSE));
        verify(owner).onConnectionIdle(connection, false);
    }

    // ======================================================================================
    // AWAITING_ACCEPT
    // ======================================================================================

    @Test
    public void awaitingAccept_connectAccept_matchingId_movesToConnected() {
        int connectId = enterAwaitingAccept();
        ScheduledTask acceptTimer = lastTask();
        assertScheduledFor(acceptTimer, CONNECT_WAIT_SECS);

        feedMessage(connectAccept(connectId));

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        verify(owner).onConnectionEstablished(connection);
        // The AWAITING_ACCEPT timer was canceled on the successful transition.
        assertCanceled(acceptTimer);
        // Status reflects an established connection with cleared error fields and a real
        // connect timestamp.
        assertConnectionStatusOk(SCConnectionState.connected);
        assertNotEquals(DateTime.UNSPECIFIED, connection.getConnectionStatus().getConnectTimestamp());
    }

    @Test
    public void awaitingAccept_connectAccept_wrongId_closesAndReturnsToIdle() {
        int connectId = enterAwaitingAccept();

        feedMessage(connectAccept(connectId + 1));

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(anyInt(), anyString());
        verify(owner).onConnectionIdle(connection, false);
    }

    @Test
    public void awaitingAccept_bvlcResultNak_duplicateVmac_restartsWithNewVmac() {
        int connectId = enterAwaitingAccept();

        feedMessage(bvlcResultNak(connectId, ErrorCode.nodeDuplicateVmac));

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(anyInt(), anyString());
        verify(owner).restartWithNewVMAC();
        // VMAC-collision path bypasses connectionClosed(), so no onConnectionIdle notification.
        verify(owner, never()).onConnectionIdle(any(SCConnection.class), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    public void awaitingAccept_bvlcResultNak_otherCode_closesAndRecordsError() {
        int connectId = enterAwaitingAccept();

        feedMessage(bvlcResultNak(connectId, ErrorCode.headerEncodingError));

        // Spec AB.6.2.2: any BVLC-Result NAK on the Connect-Request closes the WebSocket and
        // returns the SM to IDLE. Only NODE_DUPLICATE_VMAC triggers the additional VMAC rotation;
        // other NAKs surface as a recorded failedToConnect with the peer's class/code.
        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(anyInt());                       // single-arg form used by this branch
        verify(owner).onConnectionIdle(connection, false);
        verify(owner, never()).restartWithNewVMAC();
        assertConnectionError(SCConnectionState.failedToConnect,
                ErrorCode.headerEncodingError, null);
    }

    @Test
    public void awaitingAccept_bvlcResult_wrongId_closesAndReturnsToIdle() {
        int connectId = enterAwaitingAccept();

        feedMessage(bvlcResultNak(connectId + 1, ErrorCode.nodeDuplicateVmac));

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(anyInt(), anyString());
        verify(owner, never()).restartWithNewVMAC();
    }

    @Test
    public void awaitingAccept_disconnectRequest_closesAndReturnsToIdle() {
        enterAwaitingAccept();

        feedMessage(disconnectRequest(99));

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(anyInt(), anyString());
        verify(owner).onConnectionIdle(connection, false);
    }

    @Test
    public void awaitingAccept_unexpectedMessage_logsAndIgnores() {
        enterAwaitingAccept();

        feedMessage(heartbeatRequest(99));

        // Unexpected message type in AWAITING_ACCEPT is logged but no transition.
        assertEquals(SCConnection.State.AWAITING_ACCEPT, connection.getState());
    }

    @Test
    public void awaitingAccept_timeout_closesAndReturnsToIdle() {
        enterAwaitingAccept();
        ScheduledTask acceptTimer = lastTask();
        assertScheduledFor(acceptTimer, CONNECT_WAIT_SECS);

        acceptTimer.runnable.run();

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close();
        verify(owner).onConnectionIdle(connection, false);
        assertCanceled(acceptTimer);
    }

    @Test
    public void awaitingAccept_disconnect_closesAndReturnsToIdle() {
        enterAwaitingAccept();

        connection.terminate();

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close();
        verify(owner).onConnectionIdle(connection, false);
    }

    @Test
    public void awaitingAccept_remoteClose_closesAndReturnsToIdle() {
        enterAwaitingAccept();

        connection.onWebsocketClose(1006, "RST");

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close();
        verify(owner).onConnectionIdle(connection, false);
    }

    @Test
    public void awaitingAccept_initiate_isIllegal() {
        enterAwaitingAccept();

        connection.initialize();

        assertEquals(SCConnection.State.AWAITING_ACCEPT, connection.getState());
    }

    @Test
    public void awaitingAccept_accept_isIllegal() {
        enterAwaitingAccept();

        connection.onWebsocketOpen();

        assertEquals(SCConnection.State.AWAITING_ACCEPT, connection.getState());
    }

    @Test
    public void awaitingAccept_textData_closesWithRefuseAndReturnsToIdle() {
        // AB.7.5.3: any non-binary frame received must close the WebSocket with 1003.
        enterAwaitingAccept();

        connection.onTextData("garbage");

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(eq(CloseFrame.REFUSE));
        verify(owner).onConnectionIdle(connection, false);
    }

    // ======================================================================================
    // CONNECTED
    // ======================================================================================

    @Test
    public void connected_disconnect_sendsDisconnectRequestAndMovesToDisconnecting() {
        enterConnected();

        connection.terminate();

        assertEquals(SCConnection.State.DISCONNECTING, connection.getState());
        assertEquals(SCBVLC.DISCONNECT_REQUEST, lastSent().getFunction());

        // A fresh disconnect-wait timer is armed (not the connect-wait value).
        assertScheduledFor(lastTask(), DISCONNECT_WAIT_SECS);
        assertNotCanceled(lastTask());
    }

    @Test
    public void connected_disconnectRequest_sendsAckAndReturnsToIdle() {
        enterConnected();

        feedMessage(disconnectRequest(123));

        assertEquals(SCConnection.State.IDLE, connection.getState());
        SCBVLC sent = lastSent();
        assertEquals(SCBVLC.DISCONNECT_ACK, sent.getFunction());
        assertEquals(123, sent.getId());
        verify(client).close(anyInt(), anyString());
        // Peer-initiated clean disconnect from an established connection.
        verify(owner).onConnectionIdle(connection, true);
        // Status walks from connected back to notConnected with a fresh disconnectTimestamp.
        assertConnectionStatusOk(SCConnectionState.notConnected);
        assertNotEquals(DateTime.UNSPECIFIED, connection.getConnectionStatus().getDisconnectTimestamp());
    }

    @Test
    public void connected_remoteClose_returnsToIdleAsEstablished() {
        enterConnected();

        connection.onWebsocketClose(1006, "RST");

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(owner).onConnectionIdle(connection, true);
    }

    @Test
    public void connected_encapsulatedNpdu_passedToOwner() {
        enterConnected();

        feedMessage(encapsulatedNpdu(42));

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        verify(owner).onIncoming(bvlcWithFunction(SCBVLC.ENCAPSULATED_NPDU));
    }

    @Test
    public void connected_advertisementSolicitation_passedToOwner() {
        enterConnected();

        feedMessage(advertisementSolicitation(43));

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        verify(owner).onIncoming(bvlcWithFunction(SCBVLC.ADVERTISEMENT_SOLICITATION));
    }

    @Test
    public void connected_heartbeatRequest_sendsAck() {
        enterConnected();

        feedMessage(heartbeatRequest(77));

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        SCBVLC sent = lastSent();
        assertEquals(SCBVLC.HEARTBEAT_ACK, sent.getFunction());
        assertEquals(77, sent.getId());
    }

    @Test
    public void connected_heartbeatAck_matchingId_isOk() {
        enterConnected();
        // Heartbeat is only armed once a message arrives in CONNECTED; feed a benign one.
        feedMessage(advertisement(100));

        // The latest scheduled task is the heartbeat timer — confirm and fire it.
        ScheduledTask heartbeatTask = lastTask();
        assertScheduledFor(heartbeatTask, HEARTBEAT_SECS);
        heartbeatTask.runnable.run();

        SCBVLC sentRequest = lastSent();
        assertEquals(SCBVLC.HEARTBEAT_REQUEST, sentRequest.getFunction());

        feedMessage(heartbeatAck(sentRequest.getId()));

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
    }

    @Test
    public void connected_heartbeatAck_wrongId_logsAndStays() {
        enterConnected();

        feedMessage(heartbeatAck(9999));

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
    }

    @Test
    public void connected_addressResolution_sendsNakAddressedToOriginator() {
        // AB.3.3: a node that doesn't accept direct connections shall return a BVLC-Result NAK
        // for any Address-Resolution it receives. AB.3.1.2: the NAK is addressed to the
        // original requester (the message's originating VMAC) so the hub can route it back.
        enterConnected();

        feedMessage(addressResolution(55));

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        SCBVLC sent = lastSent();
        assertEquals(SCBVLC.BVLC_RESULT, sent.getFunction());
        assertEquals(55, sent.getId());
        // Destination must be the requester (PEER_ORIGIN), not null.
        assertEquals(PEER_ORIGIN, sent.getDestination());
        // Payload identifies the NAK as a response to ADDRESS_RESOLUTION with the spec'd code.
        SCPayloadBVLCResult result = new SCPayloadBVLCResult(sent.getPayload());
        assertEquals(SCBVLC.ADDRESS_RESOLUTION, result.getForFunction());
        assertTrue(result.isNak());
        assertEquals(ErrorCode.optionalFunctionalityNotSupported, result.getErrorCode());
    }

    @Test
    public void connected_advertisement_isIgnored() {
        enterConnected();
        int sendCountBefore = countSentMessages();

        feedMessage(advertisement(56));

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        assertEquals("Advertisement should not provoke a send",
                sendCountBefore, countSentMessages());
    }

    @Test
    public void connected_initiate_isIllegal() {
        enterConnected();

        connection.initialize();

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
    }

    @Test
    public void connected_accept_isIllegal() {
        enterConnected();

        connection.onWebsocketOpen();

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
    }

    @Test
    public void connected_connectError_isIllegal() {
        enterConnected();

        connection.onWebsocketError(ErrorCode.tlsError, "test");

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
    }

    @Test
    public void connected_textData_closesWithRefuseAndReturnsToIdle() {
        // AB.7.5.3: any non-binary frame must close the WebSocket with 1003.
        enterConnected();

        connection.onTextData("garbage");

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(eq(CloseFrame.REFUSE));
        // Connection had reached CONNECTED, so wasEstablished=true.
        verify(owner).onConnectionIdle(connection, true);
    }

    @Test
    public void connected_heartbeatAckTimeout_initiatesLocalDisconnect() {
        // AB.6.3 (Protocol_Revision 24): if the Heartbeat-ACK doesn't arrive, the initiating
        // peer must initiate the Local Disconnection procedure (i.e., send Disconnect-Request
        // and move to DISCONNECTING).
        enterConnected();
        // Heartbeat is only armed once a message arrives in CONNECTED; feed a benign one.
        feedMessage(advertisement(100));

        // Fire the heartbeat timer — sends Heartbeat-Request and arms the ack-wait timer.
        lastTask().runnable.run();
        assertEquals(SCBVLC.HEARTBEAT_REQUEST, lastSent().getFunction());

        // ACK never arrives — the ack-wait timer fires.
        lastTask().runnable.run();

        assertEquals(SCConnection.State.DISCONNECTING, connection.getState());
        assertEquals(SCBVLC.DISCONNECT_REQUEST, lastSent().getFunction());
    }

    // ======================================================================================
    // DISCONNECTING
    // ======================================================================================

    @Test
    public void disconnecting_disconnectAck_matchingId_returnsToIdleAsEstablished() {
        int disconnectId = enterDisconnecting();

        feedMessage(disconnectAck(disconnectId));

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(anyInt(), anyString());
        verify(owner).onConnectionIdle(connection, true);
        // After a graceful local disconnect, status returns to notConnected with no error.
        assertConnectionStatusOk(SCConnectionState.notConnected);
        assertNotEquals(DateTime.UNSPECIFIED, connection.getConnectionStatus().getDisconnectTimestamp());
    }

    @Test
    public void disconnecting_disconnectAck_wrongId_returnsToIdleAsEstablished() {
        int disconnectId = enterDisconnecting();

        feedMessage(disconnectAck(disconnectId + 1));

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(anyInt(), anyString());
        verify(owner).onConnectionIdle(connection, true);
    }

    @Test
    public void disconnecting_timeout_closesAndReturnsToIdleAsEstablished() {
        enterDisconnecting();
        ScheduledTask disconnectTimer = lastTask();
        assertScheduledFor(disconnectTimer, DISCONNECT_WAIT_SECS);

        disconnectTimer.runnable.run();

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client, atLeastOnce()).close();
        verify(owner).onConnectionIdle(connection, true);
        assertCanceled(disconnectTimer);
    }

    @Test
    public void disconnecting_remoteClose_returnsToIdleAsEstablished() {
        enterDisconnecting();

        connection.onWebsocketClose(1006, "RST");

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(owner).onConnectionIdle(connection, true);
    }

    @Test
    public void disconnecting_otherMessage_isIgnored() {
        enterDisconnecting();
        int sendCountBefore = countSentMessages();

        feedMessage(heartbeatRequest(1));

        assertEquals(SCConnection.State.DISCONNECTING, connection.getState());
        assertEquals(sendCountBefore, countSentMessages());
    }

    @Test
    public void disconnecting_initiate_isIllegal() {
        enterDisconnecting();

        connection.initialize();

        assertEquals(SCConnection.State.DISCONNECTING, connection.getState());
    }

    @Test
    public void disconnecting_disconnect_isIllegal() {
        enterDisconnecting();
        int sendCountBefore = countSentMessages();

        connection.terminate(); // DISCONNECT in DISCONNECTING

        assertEquals(SCConnection.State.DISCONNECTING, connection.getState());
        // No additional Disconnect-Request sent.
        assertEquals(sendCountBefore, countSentMessages());
    }

    @Test
    public void disconnecting_connectError_isIllegal() {
        enterDisconnecting();

        connection.onWebsocketError(ErrorCode.tlsError, "test");

        assertEquals(SCConnection.State.DISCONNECTING, connection.getState());
    }

    @Test
    public void disconnecting_textData_closesWithRefuseAndReturnsToIdle() {
        // AB.7.5.3: any non-binary frame must close the WebSocket with 1003.
        enterDisconnecting();

        connection.onTextData("garbage");

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(eq(CloseFrame.REFUSE));
        // Connection had reached CONNECTED before terminate, so wasEstablished=true.
        verify(owner).onConnectionIdle(connection, true);
    }

    @Test
    public void disconnecting_bvlcResultNakToDisconnectRequest_closesAndReturnsToIdle() {
        // AB.6.2.2 DISCONNECTING: "On receipt of a Result-NAK response to the Disconnect-Request,
        // close the WebSocket connection, and enter the IDLE state."
        int disconnectId = enterDisconnecting();

        SCBVLC nak = new SCBVLC(null, null, SCBVLC.BVLC_RESULT,
                new SCPayloadBVLCResult(SCBVLC.DISCONNECT_REQUEST, 0, ErrorClass.communication,
                        ErrorCode.headerEncodingError, "").write(),
                disconnectId);
        feedMessage(nak);

        assertEquals(SCConnection.State.IDLE, connection.getState());
        verify(client).close(eq(CloseFrame.NORMAL), anyString());
        verify(owner).onConnectionIdle(connection, true);
    }

    // ======================================================================================
    // WebSocket close-code mapping (AB.7.5.5)
    // ======================================================================================

    /**
     * Documents a real bug in SCConnection (not a test bug): when onWebsocketClose receives a
     * non-1000 status code from CONNECTED, it sets `connectionError` BEFORE the SM fires
     * REMOTE_CLOSE. The REMOTE_CLOSE handler in CONNECTED calls connectionClosed(true), which
     * unconditionally sets connectionState = notConnected. SCHubConnection's constructor
     * rejects (notConnected | connected) combined with a non-null error, so getConnectionStatus()
     * throws IllegalArgumentException afterward. A proper fix would have connectionClosed
     * choose disconnectedWithErrors when connectionError != null.
     * <p>
     * Keeping this test (passing) as a regression marker — when the bug is fixed, this test
     * will start to fail and should be replaced with a positive assertion of the mapping.
     */
    @Test
    public void remoteClose_nonNormalCode_currentlyBreaksGetConnectionStatus() {
        enterConnected();
        connection.onWebsocketClose(1002, "proto");

        // Pre-bug-fix: getConnectionStatus throws because notConnected + non-null error is
        // disallowed by SCHubConnection's constructor.
        try {
            connection.getConnectionStatus();
            org.junit.Assert.fail("getConnectionStatus should throw — bug is masked");
        } catch (IllegalArgumentException expected) {
            // Documented bug.
        }
    }

    @Test
    public void remoteClose_normalCode_doesNotRecordError() {
        // 1000 is the "normal" close — used at the end of a graceful Disconnect-ACK exchange.
        // The SM intentionally suppresses the error so the status surface stays clean.
        enterConnected();

        connection.onWebsocketClose(1000, "normal");

        SCHubConnection status = connection.getConnectionStatus();
        assertNull("normal close shall not record a connectionError", status.getError());
    }

    // ======================================================================================
    // Incoming length validation (AB.7.5.3)
    // ======================================================================================

    @Test
    public void incoming_bvlcMessageExceedsMaxBvlc_isDropped() {
        // AB.7.5.3: messages exceeding the receiving node's max BVLC length shall be discarded.
        // MaxBvlcLengthAccepted is 1500 in setUp; we feed 1501 raw bytes.
        enterConnected();
        clearInvocations(owner);

        connection.onWebsocketMessage(new ByteQueue(new byte[1501]));

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        verify(owner, never()).onIncoming(any());
    }

    // ======================================================================================
    // hardTerminate — synchronous, bypasses the state machine. Cancels timers, force-closes
    // the WebSocket, and snaps state back to IDLE without notifying the owner.
    // ======================================================================================

    /**
     * From CONNECTED, hardTerminate must:
     * - Snap state straight to IDLE.
     * - Cancel both the (now-irrelevant) heartbeat and any pending timeout.
     * - Force the WebSocket closed with CloseFrame.NOCODE (1005), since the SM is no longer
     * able to negotiate a graceful disconnect.
     * - NOT route through connectionClosed() — owner.onConnectionIdle is bypassed because
     * hardTerminate is meant for forced teardown (shutdown, hard error) where the owner
     * is already aware (or doesn't care).
     * - Update getConnectionStatus() to notConnected.
     */
    @Test
    public void hardTerminate_fromConnected_cancelsTimersAndForceClosesWithoutNotifyingOwner() {
        enterConnected();
        // Arm the heartbeat timer by feeding a message into the CONNECTED state.
        feedMessage(advertisement(0));
        ScheduledTask heartbeatTask = lastTask();
        assertScheduledFor(heartbeatTask, HEARTBEAT_SECS);
        assertNotCanceled(heartbeatTask);
        assertEquals(SCConnectionState.connected,
                connection.getConnectionStatus().getConnectionState());

        connection.hardTerminate();

        assertEquals(SCConnection.State.IDLE, connection.getState());
        assertCanceled(heartbeatTask);

        // Forced close with NOCODE — the spec'd graceful Disconnect handshake is skipped.
        verify(client).closeConnection(eq(CloseFrame.NOCODE), anyString());
        // No graceful close was attempted.
        verify(client, never()).close();
        verify(client, never()).close(anyInt(), anyString());

        // hardTerminate intentionally does NOT call connectionClosed(...), so the owner's
        // lifecycle hook is never invoked.
        verify(owner, never()).onConnectionIdle(any(SCConnection.class), anyBoolean());

        // Status reflects forced disconnect.
        assertEquals(SCConnectionState.notConnected,
                connection.getConnectionStatus().getConnectionState());
    }

    /**
     * From AWAITING_WEBSOCKET, hardTerminate must still cancel the connect-wait timer
     * even though no peer connection is established yet. This is the "kill a hung attempt"
     * use case (e.g., during shutdown while still dialing).
     */
    @Test
    public void hardTerminate_fromAwaitingWebsocket_cancelsConnectWaitTimer() {
        enterAwaitingWebsocket();
        ScheduledTask connectTimer = lastTask();
        assertScheduledFor(connectTimer, CONNECT_WAIT_SECS);
        // Simulate a partly-open client so the forced-close branch runs.
        when(client.isClosed()).thenReturn(false);

        connection.hardTerminate();

        assertEquals(SCConnection.State.IDLE, connection.getState());
        assertCanceled(connectTimer);
        verify(client).closeConnection(eq(CloseFrame.NOCODE), anyString());
        verify(owner, never()).onConnectionIdle(any(SCConnection.class), anyBoolean());
    }

    /**
     * From DISCONNECTING, hardTerminate must cancel the disconnect-wait timer. This covers
     * the case where the application gives up on a graceful disconnect that's stuck.
     */
    @Test
    public void hardTerminate_fromDisconnecting_cancelsDisconnectWaitTimer() {
        enterDisconnecting();
        ScheduledTask disconnectTimer = lastTask();
        assertScheduledFor(disconnectTimer, DISCONNECT_WAIT_SECS);

        connection.hardTerminate();

        assertEquals(SCConnection.State.IDLE, connection.getState());
        assertCanceled(disconnectTimer);
        verify(client).closeConnection(eq(CloseFrame.NOCODE), anyString());
        verify(owner, never()).onConnectionIdle(any(SCConnection.class), anyBoolean());
    }

    /**
     * If the underlying WebSocket is ALREADY closed when hardTerminate runs, the SM must
     * skip the closeConnection call (it's a no-op and emits a stack trace from the lib)
     * AND skip the status update — the disconnectTimestamp / connectionState should not
     * be touched. The state-to-IDLE assignment is unconditional and harmless.
     */
    @Test
    public void hardTerminate_whenClientAlreadyClosed_skipsForceCloseAndStatusUpdate() {
        // Default mock has client.isClosed() == true; we're in IDLE with no work in flight.
        assertEquals(SCConnection.State.IDLE, connection.getState());

        connection.hardTerminate();

        assertEquals(SCConnection.State.IDLE, connection.getState());
        // Skipped because client.isClosed() returned true.
        verify(client, never()).closeConnection(anyInt(), anyString());
        verify(owner, never()).onConnectionIdle(any(SCConnection.class), anyBoolean());
    }

    // ======================================================================================
    // Multi-step scenarios — verify cumulative behavior across state transitions that
    // single-transition tests cannot catch (ordering, owner-callback counts, metadata
    // progression, reuse after failure).
    // ======================================================================================

    /**
     * End-to-end success path:
     * IDLE → AWAITING_WEBSOCKET → AWAITING_ACCEPT → CONNECTED
     * → DISCONNECTING → IDLE
     * <p>
     * What this proves that the per-transition tests can't:
     * - The wire-protocol exchange happens in spec order: Connect-Request is sent
     * before Disconnect-Request, the close happens last. Order is verified with
     * Mockito InOrder.
     * - The owner sees exactly ONE onConnectionEstablished and ONE
     * onConnectionIdle(true) across the entire flow — no double-callbacks, no
     * spurious onIncoming, no restartWithNewVMAC.
     * - getConnectionStatus() walks notConnected → connected → notConnected, which
     * is the surface the network-port object exposes to BACnet clients.
     */
    @Test
    public void scenario_happyPathConnectAndDisconnect() {
        // Pre-state: nothing has happened yet.
        assertEquals(SCConnectionState.notConnected,
                connection.getConnectionStatus().getConnectionState());

        // 1. Local: start the connection. Connect-wait timer is armed.
        connection.initialize();
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, connection.getState());
        ScheduledTask websocketTimer = lastTask();
        assertScheduledFor(websocketTimer, CONNECT_WAIT_SECS);
        assertEquals(1, scheduledTasks.size());

        // 2. WebSocket layer reports the upgrade succeeded. The websocket timer is canceled
        // and a fresh connect-wait timer is armed for the AWAITING_ACCEPT phase.
        simulateClientOpen();
        connection.onWebsocketOpen();
        assertEquals(SCConnection.State.AWAITING_ACCEPT, connection.getState());
        assertCanceled(websocketTimer);
        ScheduledTask acceptTimer = lastTask();
        assertScheduledFor(acceptTimer, CONNECT_WAIT_SECS);
        assertEquals(2, scheduledTasks.size());
        int connectId = lastSent().getId();

        // 3. Peer accepts. The accept timer is canceled; the heartbeat timer is started.
        feedMessage(connectAccept(connectId));
        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        // Status is now "connected" with no error and a real connect timestamp.
        assertConnectionStatusOk(SCConnectionState.connected);
        DateTime connectTimestamp = connection.getConnectionStatus().getConnectTimestamp();
        assertNotEquals(DateTime.UNSPECIFIED, connectTimestamp);
        assertCanceled(acceptTimer);
        ScheduledTask heartbeatTimer = lastTask();
        assertScheduledFor(heartbeatTimer, HEARTBEAT_SECS);
        assertEquals(3, scheduledTasks.size());

        // 4. Local: initiate disconnect. A disconnect-wait timer is armed at the
        // disconnect-wait timeout — NOT the connect-wait value.
        connection.terminate();
        assertEquals(SCConnection.State.DISCONNECTING, connection.getState());
        ScheduledTask disconnectTimer = lastTask();
        assertScheduledFor(disconnectTimer, DISCONNECT_WAIT_SECS);
        assertEquals(4, scheduledTasks.size());
        int disconnectId = lastSent().getId();

        // 5. Peer acks the disconnect. We're idle and the disconnect-wait timer is canceled.
        feedMessage(disconnectAck(disconnectId));
        assertEquals(SCConnection.State.IDLE, connection.getState());
        assertCanceled(disconnectTimer);

        // ---- Cumulative verification across the whole flow ----

        // Owner saw lifecycle events exactly once, in order. Critically: NO restart,
        // NO incoming-message forwarding (we never sent app-level traffic).
        InOrder ownerOrder = inOrder(owner);
        ownerOrder.verify(owner).onConnectionEstablished(connection);
        ownerOrder.verify(owner).onConnectionIdle(connection, true);
        verify(owner, never()).restartWithNewVMAC();
        verify(owner, never()).onIncoming(any());

        // Exactly two outbound messages, in spec order.
        ArgumentCaptor<byte[]> sentCap = ArgumentCaptor.forClass(byte[].class);
        verify(client, times(2)).send(sentCap.capture());
        SCBVLC firstSend = new SCBVLC(new ByteQueue(sentCap.getAllValues().get(0)));
        SCBVLC secondSend = new SCBVLC(new ByteQueue(sentCap.getAllValues().get(1)));
        assertEquals(SCBVLC.CONNECT_REQUEST, firstSend.getFunction());
        assertEquals(SCBVLC.DISCONNECT_REQUEST, secondSend.getFunction());

        // The connect/close client API was called in order.
        InOrder clientOrder = inOrder(client);
        clientOrder.verify(client).connect();
        clientOrder.verify(client, times(2)).send(any(byte[].class));
        clientOrder.verify(client).close(anyInt(), anyString());

        // Final metadata: back to notConnected, no error, both timestamps populated, and
        // the connectTimestamp from step 3 was NOT overwritten by the disconnect.
        SCHubConnection status = connection.getConnectionStatus();
        assertEquals(SCConnectionState.notConnected, status.getConnectionState());
        assertNull(status.getError());
        assertNull(status.getErrorDetails());
        assertEquals(connectTimestamp, status.getConnectTimestamp());
        assertNotEquals(DateTime.UNSPECIFIED, status.getDisconnectTimestamp());
    }

    /**
     * Connect-failure recovery:
     * IDLE → AWAITING_WEBSOCKET → TIMEOUT → IDLE → INITIATE
     * → AWAITING_WEBSOCKET (via reconnect() this time, not connect())
     * <p>
     * What this proves:
     * - After a failed connect, the SCConnection is reusable. A second
     * initialize() proceeds normally rather than getting stuck on stale state
     * (left-over expected message id, uncanceled timer, etc.).
     * - The second attempt routes through client.reconnect() because the client
     * is no longer in NOT_YET_CONNECTED state. The first attempt used connect().
     * - The owner receives exactly ONE onConnectionIdle(false) — for the failed
     * first attempt. A regression that double-fires owner callbacks would fail
     * here.
     */
    @Test
    public void scenario_connectFailsThenReconnects() {
        // First attempt — initiate, then time out before the WebSocket opens.
        connection.initialize();
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, connection.getState());

        fireScheduled(0); // fire AWAITING_WEBSOCKET timeout
        assertEquals(SCConnection.State.IDLE, connection.getState());

        // Failed first attempt notified the owner exactly once. Connection was never
        // established, so onConnectionEstablished must NOT have fired.
        verify(owner, times(1)).onConnectionIdle(connection, false);
        verify(owner, never()).onConnectionEstablished(any());

        // Second attempt — the underlying WS client is now in a "post-close" state,
        // so the SM must route via reconnect() rather than connect().
        when(client.getReadyState()).thenReturn(ReadyState.CLOSED);

        connection.initialize();
        assertEquals(SCConnection.State.AWAITING_WEBSOCKET, connection.getState());

        // Wire-level: connect() exactly once (first attempt), reconnect() exactly
        // once (second attempt), in that order.
        InOrder clientOrder = inOrder(client);
        clientOrder.verify(client).connect();
        clientOrder.verify(client).reconnect();
        verify(client, times(1)).connect();
        verify(client, times(1)).reconnect();

        // Owner-callback count is STILL exactly one onConnectionIdle. The second
        // initialize() did not produce a spurious additional notification.
        verify(owner, times(1)).onConnectionIdle(connection, false);
    }

    /**
     * VMAC-collision recovery (AB.6.2.2 special path):
     * IDLE → AWAITING_WEBSOCKET → AWAITING_ACCEPT
     * → BVLC-Result NAK (nodeDuplicateVmac) → IDLE
     * <p>
     * What this proves:
     * - The duplicate-VMAC path BYPASSES connectionClosed() — the owner sees
     * restartWithNewVMAC() but NOT onConnectionIdle(...). This bypass is
     * critical: the hub connector reacts to restart() by regenerating its
     * Random-48 VMAC; a spurious onConnectionIdle would let it interpret this
     * as a "normal" disconnect and skip the VMAC regen.
     * - The WebSocket is closed with the "be back soon" NORMAL code, not
     * PROTOCOL_ERROR or ABNORMAL_CLOSE.
     * - Exactly one outbound message in the whole scenario: the Connect-Request
     * that triggered the collision response. No Disconnect-Request follows.
     */
    @Test
    public void scenario_vmacCollisionRestartsWithNewVmac() {
        connection.initialize();
        simulateClientOpen();
        connection.onWebsocketOpen();
        assertEquals(SCConnection.State.AWAITING_ACCEPT, connection.getState());
        int connectId = lastSent().getId();

        feedMessage(bvlcResultNak(connectId, ErrorCode.nodeDuplicateVmac));

        assertEquals(SCConnection.State.IDLE, connection.getState());

        // Owner saw the restart signal — and CRITICALLY did NOT see the regular
        // onConnectionIdle callback. The VMAC-collision branch bypasses
        // connectionClosed() on purpose.
        verify(owner).restartWithNewVMAC();
        verify(owner, never()).onConnectionIdle(any(SCConnection.class), anyBoolean());
        verify(owner, never()).onConnectionEstablished(any());

        // Close uses NORMAL ("Changing VMAC. Be back soon..."), not an error code.
        verify(client).close(eq(CloseFrame.NORMAL), anyString());
        verify(client, never()).close(eq(CloseFrame.PROTOCOL_ERROR), anyString());
        verify(client, never()).close(eq(CloseFrame.ABNORMAL_CLOSE), anyString());

        // Only the Connect-Request went out. The NAK response did NOT trigger any
        // BVLC-Result or Disconnect-Request from our side.
        ArgumentCaptor<byte[]> sentCap = ArgumentCaptor.forClass(byte[].class);
        verify(client, times(1)).send(sentCap.capture());
        SCBVLC sent = new SCBVLC(new ByteQueue(sentCap.getValue()));
        assertEquals(SCBVLC.CONNECT_REQUEST, sent.getFunction());
    }

    /**
     * Peer-initiated graceful disconnect:
     * established CONNECTED → incoming DISCONNECT_REQUEST → IDLE
     * <p>
     * What this proves:
     * - The Disconnect-ACK is sent on the wire BEFORE client.close(). If those
     * were reversed in production code, the ACK would never reach the peer
     * and the peer would treat its own request as having timed out. InOrder
     * pins this ordering.
     * - The Disconnect-ACK carries the SAME message id as the incoming
     * Disconnect-Request (per spec) — NOT our local counter. A bug that used
     * our own message id would be caught here.
     * - onConnectionIdle is called with wasEstablished=true because the
     * connection HAD been established before the peer ended it. (A peer
     * disconnect during AWAITING_ACCEPT would pass false.)
     */
    @Test
    public void scenario_peerInitiatedDisconnect() {
        enterConnected();

        // Peer sends a Disconnect-Request with its own message id (789).
        feedMessage(disconnectRequest(789));

        assertEquals(SCConnection.State.IDLE, connection.getState());

        // The last outbound message must be the ACK with id=789 (peer's id, not ours).
        SCBVLC ackSent = lastSent();
        assertEquals(SCBVLC.DISCONNECT_ACK, ackSent.getFunction());
        assertEquals(789, ackSent.getId());

        // Ordering: connect → CONNECT_REQUEST → DISCONNECT_ACK → close.
        // If close() came before the ACK send, the peer would never receive the ACK.
        InOrder order = inOrder(client);
        order.verify(client).connect();
        order.verify(client, times(2)).send(any(byte[].class)); // CONNECT_REQUEST then DISCONNECT_ACK
        order.verify(client).close(anyInt(), anyString());

        // wasEstablished=true because the connection had reached CONNECTED.
        verify(owner).onConnectionIdle(connection, true);
        verify(owner, never()).onConnectionIdle(connection, false);
    }

    /**
     * Local disconnect with no peer ACK (Disconnect-Wait Timeout transition):
     * established CONNECTED → local DISCONNECT → DISCONNECTING
     * → disconnect-wait TIMEOUT → IDLE (forced close)
     * <p>
     * What this proves:
     * - When the peer never sends a Disconnect-ACK, the disconnect-wait timer
     * forces the connection closed. The SM does NOT wait indefinitely
     * (which is what would happen if the WebSocket close handshake also
     * silently dropped).
     * - client.close() is called exactly once and ONLY after the timeout fires
     * — not eagerly when DISCONNECT was first received. The SM waits for the
     * ACK first.
     * - onConnectionIdle(true) still fires because the connection WAS
     * established before the failed disconnect handshake.
     */
    @Test
    public void scenario_disconnectTimeoutWhenPeerStaysQuiet() {
        enterConnected();
        int sendCountBeforeDisconnect = countSentMessages();

        // Local termination — send Disconnect-Request, arm disconnect-wait timer
        // with the spec'd disconnect-wait timeout (NOT the connect-wait value).
        connection.terminate();
        assertEquals(SCConnection.State.DISCONNECTING, connection.getState());
        ScheduledTask disconnectTimer = lastTask();
        assertScheduledFor(disconnectTimer, DISCONNECT_WAIT_SECS);
        assertNotCanceled(disconnectTimer);

        // Disconnect-Request went out, but the WebSocket close has NOT yet been
        // attempted. The SM is waiting for the peer's ACK.
        assertEquals(sendCountBeforeDisconnect + 1, countSentMessages());
        assertEquals(SCBVLC.DISCONNECT_REQUEST, lastSent().getFunction());
        verify(client, never()).close();
        verify(client, never()).close(anyInt(), anyString());

        // Peer never acks; the disconnect-wait timer eventually fires.
        disconnectTimer.runnable.run();

        assertEquals(SCConnection.State.IDLE, connection.getState());

        // The timer is canceled by connectionClosed() (defensive — was already
        // fired, but the SM cancels it anyway to clear references).
        assertCanceled(disconnectTimer);

        // Exactly one forced close (the no-args overload, as used by the
        // DISCONNECTING-TIMEOUT branch). No additional Disconnect-Request was
        // sent during the wait.
        verify(client, times(1)).close();
        assertEquals(sendCountBeforeDisconnect + 1, countSentMessages());

        // Connection had been established, so wasEstablished=true.
        verify(owner).onConnectionIdle(connection, true);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    // ======================================================================================
    // Payload parsing failures — AB.3.1.5
    // ======================================================================================
    //
    // These messages are not "unicast requests" per SCBVLC.isUnicastRequest, so per AB.3.1.5's
    // sender-not-identifiable rule ("shall not attempt to send a BVLC-Result NAK"), the
    // correct behavior on a truncated payload is a silent drop. What the try/catch in
    // handleEventImpl guarantees is that the parse failure does NOT propagate out — the state
    // machine handles it gracefully and stays in the current state.

    /**
     * A CONNECT_ACCEPT with a truncated payload (shorter than the fixed 26-octet expected
     * size) must not throw out of handleEventImpl. Since CONNECT_ACCEPT is a response (not a
     * unicast request), no NAK is emitted; the parse failure is logged and the state remains
     * AWAITING_ACCEPT so the connection can time out normally.
     */
    @Test
    public void awaitingAccept_truncatedConnectAcceptPayload_isHandledGracefully() {
        int connectId = enterAwaitingAccept();
        clearInvocations(client);

        // CONNECT_ACCEPT payload is fixed 26 bytes (6+16+2+2). Send only 4.
        var truncated = new SCBVLC(null, null, SCBVLC.CONNECT_ACCEPT, new byte[] {0x01, 0x02, 0x03, 0x04},
                connectId);
        feedMessage(truncated);

        // No exception propagated; SM stays put; no NAK because CONNECT_ACCEPT is not
        // a unicast request per isUnicastRequest.
        assertEquals(SCConnection.State.AWAITING_ACCEPT, connection.getState());
        verify(client, never()).send(any(byte[].class));
    }

    /**
     * A CONNECTED-state peer BVLC-Result with truncated payload must not throw. BVLC_RESULT
     * is a response so no NAK is emitted; the state stays CONNECTED.
     */
    @Test
    public void connected_truncatedBvlcResultPayload_isHandledGracefully() {
        enterConnected();
        clearInvocations(client);

        var truncated = new SCBVLC(null, null, SCBVLC.BVLC_RESULT, new byte[] {0x00}, 0);
        feedMessage(truncated);

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        verify(client, never()).send(any(byte[].class));
    }

    /**
     * A CONNECTED-state ADVERTISEMENT with a truncated payload must not throw. ADVERTISEMENT
     * is broadcast in nature (not a unicast request) so no NAK is emitted.
     */
    @Test
    public void connected_truncatedAdvertisementPayload_isHandledGracefully() {
        enterConnected();
        clearInvocations(client);

        // ADVERTISEMENT payload is 6 bytes. Send only 3.
        var truncated = new SCBVLC(PEER_ORIGIN, null, SCBVLC.ADVERTISEMENT,
                new byte[] {0x00, 0x00, 0x00}, 0);
        feedMessage(truncated);

        assertEquals(SCConnection.State.CONNECTED, connection.getState());
        verify(client, never()).send(any(byte[].class));
    }

    // ======================================================================================
    // sendError address safety — AB.3.1.2 / AB.5.4
    // ======================================================================================

    /**
     * On a hub connection, protocolViolationLogAndSend must emit NAKs with a null source
     * (per AB.5.4 — the hub inserts our identity implicitly). Verifies the fix for the
     * source-spoofing issue where the receiving-message's originating VMAC was being
     * echoed as the outgoing source.
     * <p>
     * Uses ADVERTISEMENT_SOLICITATION with an unexpected payload to trigger the NAK path —
     * ADVERTISEMENT_SOLICITATION is a unicast request (matches isUnicastRequest) but its
     * payload must be absent, so parseMessage emits an inconsistentParameters NAK.
     */
    @Test
    public void protocolViolation_naksOnHubConnection_haveNullSource() {
        enterConnected();
        clearInvocations(client);

        // ADVERTISEMENT_SOLICITATION with a payload (must be absent per parseMessage). The
        // originating VMAC is set — this is what the buggy code would have echoed as source.
        var badSolicitation = new SCBVLC(PEER_ORIGIN, null, SCBVLC.ADVERTISEMENT_SOLICITATION,
                new byte[] {0x00}, 42);
        feedMessage(badSolicitation);

        SCBVLC nak = lastSent();
        assertEquals(SCBVLC.BVLC_RESULT, nak.getFunction());
        assertNull("NAK source must be null on hub connection", nak.getOriginating());
        // The destination on a NAK sent back through the hub is the original sender.
        assertEquals(PEER_ORIGIN, nak.getDestination());
    }

    /**
     * sendError with a broadcast source must be refused (no wire send). Guards against
     * accidentally emitting a BVLC-Result claiming to be from the broadcast VMAC.
     */
    @Test
    public void sendError_broadcastSource_isRefusedWithoutSend() {
        enterConnected();
        clearInvocations(client);

        connection.sendError(
                new SCVmac(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}),
                new SCVmac(PEER_VMAC),
                SCBVLC.ENCAPSULATED_NPDU, 0,
                ErrorClass.communication, ErrorCode.other, "test", 0);

        // No bytes sent — the broadcast-source guard should have short-circuited.
        verify(client, never()).send(any(byte[].class));
    }

    /**
     * sendError with a broadcast destination must be refused (no wire send). A BVLC-Result
     * NAK by definition is a targeted response; broadcasting one is meaningless and
     * potentially disruptive.
     */
    @Test
    public void sendError_broadcastDestination_isRefusedWithoutSend() {
        enterConnected();
        clearInvocations(client);

        connection.sendError(
                new SCVmac(PEER_VMAC),
                new SCVmac(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}),
                SCBVLC.ENCAPSULATED_NPDU, 0,
                ErrorClass.communication, ErrorCode.other, "test", 0);

        verify(client, never()).send(any(byte[].class));
    }

    // ======================================================================================
    // Silent-drop on non-broadcast destination — AB.5.4
    // ======================================================================================

    /**
     * A message received on a hub connection with a destination address that is neither
     * absent nor broadcast is a spec violation on the sender's part, but AB.5.4 says the
     * receiver should silently drop it — not NAK. Verifies the log-only path (no wire
     * response).
     */
    @Test
    public void connected_nonBroadcastDestination_isSilentlyDropped() {
        enterConnected();
        clearInvocations(client);

        // Encapsulated-NPDU with a unicast destination that isn't ours. Note that a real
        // hub would never forward this to us; we're testing defensive behavior.
        var otherVmac = new SCVmac(new byte[] {0x02, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE});
        var msg = new SCBVLC(PEER_ORIGIN, otherVmac, SCBVLC.ENCAPSULATED_NPDU, new byte[] {0x01, 0x02}, 42);
        feedMessage(msg);

        // Silent drop — nothing sent back on the wire, connection stays CONNECTED.
        verify(client, never()).send(any(byte[].class));
        assertEquals(SCConnection.State.CONNECTED, connection.getState());
    }

    // ======================================================================================
    // WebSocket close code → ErrorCode mapping (AB.7.5.5)
    // ======================================================================================
    // The full mapping is exercised via AWAITING_WEBSOCKET, where the REMOTE_CLOSE path
    // uses connectionState=failedToConnect, which SCHubConnection accepts alongside a
    // non-null error. Codes 1000 (normal) and 1002/1006 are already covered elsewhere;
    // these tests fill in the remaining codes plus the default fallback.

    private void assertMapsCloseCodeToError(int closeCode, ErrorCode expected) {
        enterAwaitingWebsocket();
        connection.onWebsocketClose(closeCode, "test");
        SCHubConnection status = connection.getConnectionStatus();
        assertNotNull("expected an error for code " + closeCode, status.getError());
        assertTrue("code " + closeCode + " should map to " + expected + " but got " + status.getError(),
                status.getError().equals(ErrorClass.communication, expected));
    }

    @Test
    public void closeCode_1001_mapsToEndpointLeaves() {
        assertMapsCloseCodeToError(1001, ErrorCode.websocketEndpointLeaves);
    }

    @Test
    public void closeCode_1003_mapsToDataNotAccepted() {
        assertMapsCloseCodeToError(1003, ErrorCode.websocketDataNotAccepted);
    }

    @Test
    public void closeCode_1007_mapsToDataInconsistent() {
        assertMapsCloseCodeToError(1007, ErrorCode.websocketDataInconsistent);
    }

    @Test
    public void closeCode_1008_mapsToDataAgainstPolicy() {
        assertMapsCloseCodeToError(1008, ErrorCode.websocketDataAgainstPolicy);
    }

    @Test
    public void closeCode_1009_mapsToFrameTooLong() {
        assertMapsCloseCodeToError(1009, ErrorCode.websocketFrameTooLong);
    }

    @Test
    public void closeCode_1010_mapsToExtensionMissing() {
        assertMapsCloseCodeToError(1010, ErrorCode.websocketExtensionMissing);
    }

    @Test
    public void closeCode_1011_mapsToRequestUnavailable() {
        assertMapsCloseCodeToError(1011, ErrorCode.websocketRequestUnavailable);
    }

    @Test
    public void closeCode_unknown_mapsToGenericWebsocketError() {
        // AB.7.5.5 lists the standard codes 1000-1011 (excluding 1004/1005 which are reserved).
        // Anything else falls back to the generic websocketError code.
        assertMapsCloseCodeToError(4999, ErrorCode.websocketError);
    }
}
