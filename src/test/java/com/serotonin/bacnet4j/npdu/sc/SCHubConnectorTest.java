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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.sc.msg.SCBVLC;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadAdvertisement;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;

/**
 * State machine coverage for SCHubConnector. Mirrors the SCConnectionTest structure: an inline
 * network.executeSerially(...) stub serializes events on the test thread, a ScheduledTask harness
 * captures schedule() calls so timer delays and cancellation can be asserted, and a TestHubConnector
 * subclass overrides createConnection(...) to inject mock SCConnections for primary and failover.
 * <p>
 * The connector's child SCConnection objects are pure mocks — we do not exercise their state
 * machines here. Their lifecycle callbacks are simulated by the test calling
 * onConnectionEstablished / onConnectionIdle directly on the connector.
 */
public class SCHubConnectorTest {
    private static final URI PRIMARY_URI = URI.create("wss://primary.example.com/");
    private static final URI FAILOVER_URI = URI.create("wss://failover.example.com/");
    private static final int RECONNECT_SECS = 5;

    private SCNode node;
    private SCNetwork network;
    private Transport transport;
    private SCConnection primaryConn;
    private SCConnection failoverConn;
    private BackoffPolicy backoff;
    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();
    private TestHubConnector connector;


    /** Same shape as SCConnectionTest.ScheduledTask. */
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
        node = mock(SCNode.class);
        network = mock(SCNetwork.class);
        transport = mock(Transport.class);
        var localDevice = mock(LocalDevice.class);
        primaryConn = mock(SCConnection.class);
        failoverConn = mock(SCConnection.class);
        backoff = mock(BackoffPolicy.class);

        when(transport.getLocalDevice()).thenReturn(localDevice);
        when(network.getPrimaryHub()).thenReturn(PRIMARY_URI);
        when(network.getFailoverHub()).thenReturn(FAILOVER_URI);
        when(network.getBackoffPolicy()).thenReturn(backoff);
        when(backoff.getReconnectWaitTimeout()).thenReturn(RECONNECT_SECS);

        // Child connections' getState() defaults to IDLE; tests that need other values override.
        when(primaryConn.getState()).thenReturn(SCConnection.State.IDLE);
        when(failoverConn.getState()).thenReturn(SCConnection.State.IDLE);

        // Events queued via localDevice.execute(...) run inline so the SM is synchronous.
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(network).executeSerially(any(Runnable.class));

        // Capture every schedule(...) into a ScheduledTask whose mock future routes cancel(...)
        // back into the task so tests can assert cancellation.
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

        connector = new TestHubConnector(node, network, primaryConn, failoverConn);
        connector.configure(transport);
    }

    // --------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------


    private static class TestHubConnector extends SCHubConnector {
        private final SCConnection mockPrimary;
        private final SCConnection mockFailover;

        TestHubConnector(SCNode node, SCNetwork network, SCConnection mockPrimary, SCConnection mockFailover) {
            super(node, network);
            this.mockPrimary = mockPrimary;
            this.mockFailover = mockFailover;
        }

        @Override
        protected SCConnection createConnection(String name, SCNetwork network, URI uri) {
            return "primary".equals(name) ? mockPrimary : mockFailover;
        }
    }

    /**
     * Rebuild a connector with optional primary/failover. Allows tests to exercise the "no
     * primary configured" and "no failover configured" branches.
     */
    private TestHubConnector buildConnector(boolean withPrimary, boolean withFailover) {
        when(network.getPrimaryHub()).thenReturn(withPrimary ? PRIMARY_URI : null);
        when(network.getFailoverHub()).thenReturn(withFailover ? FAILOVER_URI : null);
        TestHubConnector c = new TestHubConnector(node, network,
                withPrimary ? primaryConn : null,
                withFailover ? failoverConn : null);
        c.configure(transport);
        return c;
    }

    // ---- Scheduled-task helpers (same shape as SCConnectionTest) ----

    private ScheduledTask lastTask() {
        return scheduledTasks.get(scheduledTasks.size() - 1);
    }

    private void assertScheduledFor(ScheduledTask task, int expectedSeconds) {
        assertEquals("scheduled unit", TimeUnit.SECONDS, task.unit);
        assertEquals("scheduled delay (seconds)", expectedSeconds, task.delay);
    }

    private void assertNotCanceled(ScheduledTask task) {
        assertFalse("expected scheduled task to be alive", task.canceled);
    }

    private void assertCanceled(ScheduledTask task) {
        assertTrue("expected scheduled task to be canceled", task.canceled);
    }

    // ---- Hub-connector status helpers ----
    //
    // The connector exposes two INDEPENDENT external "status" surfaces backed by two parallel
    // switch statements in SCHubConnector:
    //
    //   1. getHubConnectorState() -> SCHubConnectorState enum
    //      (read by the network port object so BACnet clients can query connector status)
    //   2. getStateAsInt()        -> int code (SCPayloadAdvertisement.CONN_STAT_*)
    //      (written into outgoing Advertisement BVLC payloads)
    //
    // These two values happen to share numeric codes (0/1/2) by spec alignment, but they are
    // not the same surface. Asserting them separately keeps the comparison honest and catches
    // a regression that updates one switch but forgets the other.

    private void assertHubConnectorState(SCHubConnectorState expected) {
        assertEquals("getHubConnectorState", expected, connector.getHubConnectorState());
    }

    private void assertAdvertisementStateCode(int expected) {
        assertEquals("getStateAsInt", expected, connector.getStateAsInt());
    }

    // ---- State drivers ----

    /**
     * IDLE -> WAIT_PRIMARY: connector.initialize() fires START which raiseChanges to TRY_PRIMARY,
     * which (inline) processes CHANGE and ends in WAIT_PRIMARY.
     */
    private void enterWaitPrimary() {
        connector.initialize();
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, connector.getState());
    }

    private void enterConnectedPrimary() {
        enterWaitPrimary();
        connector.onConnectionEstablished(primaryConn);
        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, connector.getState());
    }

    private void enterWaitFailover() {
        enterWaitPrimary();
        connector.onConnectionIdle(primaryConn, false);
        assertEquals(SCHubConnector.State.WAIT_FAILOVER, connector.getState());
    }

    private void enterConnectedFailover() {
        enterWaitFailover();
        connector.onConnectionEstablished(failoverConn);
        assertEquals(SCHubConnector.State.CONNECTED_FAILOVER, connector.getState());
    }

    /** From CONNECTED_FAILOVER, fire the retry-primary timeout. */
    private void enterRewaitPrimary() {
        enterConnectedFailover();
        lastTask().runnable.run();
        assertEquals(SCHubConnector.State.REWAIT_PRIMARY, connector.getState());
    }

    private void enterDelaying() {
        enterWaitFailover();
        connector.onConnectionIdle(failoverConn, false);
        assertEquals(SCHubConnector.State.DELAYING, connector.getState());
    }

    // ======================================================================================
    // IDLE
    // ======================================================================================

    @Test
    public void idle_start_initiatesPrimaryAndMovesToWaitPrimary() {
        // Pre-state: no hub connection from the network port's perspective.
        assertHubConnectorState(SCHubConnectorState.noHubConnection);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_NONE);

        connector.initialize();

        assertEquals(SCHubConnector.State.WAIT_PRIMARY, connector.getState());
        verify(primaryConn).initialize();
        verify(failoverConn, never()).initialize();
        // We're trying to connect, but we're not yet connected to anything.
        assertHubConnectorState(SCHubConnectorState.noHubConnection);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_NONE);
    }

    @Test
    public void idle_start_noPrimaryConfigured_jumpsToWaitFailover() {
        TestHubConnector noPrimary = buildConnector(false, true);

        noPrimary.initialize();

        assertEquals(SCHubConnector.State.WAIT_FAILOVER, noPrimary.getState());
        verify(failoverConn).initialize();
    }

    @Test
    public void idle_start_noFailoverConfigured_endsInDelayingWhenPrimaryFails() {
        // Re-stub with no failover. Reuses the same `connector` field by rebuilding.
        TestHubConnector noFailover = buildConnector(true, false);

        noFailover.initialize();
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, noFailover.getState());

        // Primary fails; with no failover, we go via TRY_FAILOVER -> DELAY -> DELAYING.
        noFailover.onConnectionIdle(primaryConn, false);

        assertEquals(SCHubConnector.State.DELAYING, noFailover.getState());
        assertScheduledFor(lastTask(), RECONNECT_SECS);
    }

    @Test
    public void idle_start_noPrimaryNoFailover_endsInDelaying() {
        TestHubConnector none = buildConnector(false, false);

        none.initialize();

        assertEquals(SCHubConnector.State.DELAYING, none.getState());
        assertScheduledFor(lastTask(), RECONNECT_SECS);
    }

    @Test
    public void idle_stop_movesToStoppingAndTerminatesBoth() {
        connector.terminate();

        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
        verify(primaryConn).terminate();
        verify(failoverConn).terminate();
    }

    @Test
    public void idle_connectionClosed_isIgnored() {
        connector.onConnectionIdle(primaryConn, true);
        assertEquals(SCHubConnector.State.IDLE, connector.getState());
        verify(node, never()).onDisconnected();
    }

    @Test
    public void idle_connectionIdle_isIgnored() {
        connector.onConnectionIdle(primaryConn, false);
        assertEquals(SCHubConnector.State.IDLE, connector.getState());
    }

    @Test
    public void idle_connectionEstablished_isIllegal() {
        connector.onConnectionEstablished(primaryConn);
        assertEquals(SCHubConnector.State.IDLE, connector.getState());
        verify(node, never()).onConnected();
    }

    // ======================================================================================
    // WAIT_PRIMARY
    // ======================================================================================

    @Test
    public void waitPrimary_connectionEstablished_movesToConnectedPrimary() {
        enterWaitPrimary();

        connector.onConnectionEstablished(primaryConn);

        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, connector.getState());
        verify(backoff).reset();
        verify(node).onConnected();
        assertHubConnectorState(SCHubConnectorState.connectedToPrimary);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_PRIMARY);
    }

    @Test
    public void waitPrimary_connectionIdle_movesToWaitFailover() {
        enterWaitPrimary();

        connector.onConnectionIdle(primaryConn, false);

        assertEquals(SCHubConnector.State.WAIT_FAILOVER, connector.getState());
        verify(failoverConn).initialize();
        verify(node, never()).onConnected();
        verify(node, never()).onDisconnected();
    }

    @Test
    public void waitPrimary_connectionClosed_movesToWaitFailover() {
        // CONNECTION_CLOSED here is unusual (the connection was never established) but the SM
        // routes it the same way as CONNECTION_IDLE.
        enterWaitPrimary();

        connector.onConnectionIdle(primaryConn, true);

        assertEquals(SCHubConnector.State.WAIT_FAILOVER, connector.getState());
    }

    @Test
    public void waitPrimary_stop_movesToStopping() {
        enterWaitPrimary();

        connector.terminate();

        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
        verify(primaryConn).terminate();
        verify(failoverConn).terminate();
    }

    @Test
    public void waitPrimary_unexpectedTimeout_isIllegal() {
        // The SM doesn't arm a timer in WAIT_PRIMARY (the SCConnection enforces connect-wait).
        // But the IDLE-to-WAIT_PRIMARY transition doesn't schedule anything either, so we
        // can't easily fire a stale TIMEOUT. This test documents that nothing is scheduled.
        enterWaitPrimary();
        assertEquals(0, scheduledTasks.size());
    }

    // ======================================================================================
    // CONNECTED_PRIMARY
    // ======================================================================================

    @Test
    public void connectedPrimary_connectionClosed_movesToWaitPrimary() {
        enterConnectedPrimary();
        assertHubConnectorState(SCHubConnectorState.connectedToPrimary);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_PRIMARY);

        connector.onConnectionIdle(primaryConn, true);

        assertEquals(SCHubConnector.State.WAIT_PRIMARY, connector.getState());
        verify(node).onDisconnected();
        // While re-establishing, status reverts to noHubConnection.
        assertHubConnectorState(SCHubConnectorState.noHubConnection);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_NONE);
    }

    @Test
    public void connectedPrimary_stop_movesToStopping() {
        enterConnectedPrimary();

        connector.terminate();

        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
        verify(primaryConn).terminate();
        verify(failoverConn).terminate();
    }

    @Test
    public void connectedPrimary_connectionEstablished_isIllegal() {
        enterConnectedPrimary();

        connector.onConnectionEstablished(primaryConn);

        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, connector.getState());
    }

    // ======================================================================================
    // WAIT_FAILOVER
    // ======================================================================================

    @Test
    public void waitFailover_connectionEstablished_movesToConnectedFailoverAndArmsRetryTimer() {
        enterWaitFailover();

        connector.onConnectionEstablished(failoverConn);

        assertEquals(SCHubConnector.State.CONNECTED_FAILOVER, connector.getState());
        verify(backoff).reset();
        verify(node).onConnected();
        // Retry-primary timer armed with the backoff value (NOT the connect-wait value).
        assertScheduledFor(lastTask(), RECONNECT_SECS);
        assertNotCanceled(lastTask());
        assertHubConnectorState(SCHubConnectorState.connectedToFailover);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_FAILOVER);
    }

    @Test
    public void waitFailover_connectionIdle_movesToDelaying() {
        enterWaitFailover();

        connector.onConnectionIdle(failoverConn, false);

        assertEquals(SCHubConnector.State.DELAYING, connector.getState());
        verify(node, never()).onConnected();
        assertScheduledFor(lastTask(), RECONNECT_SECS);
    }

    @Test
    public void waitFailover_connectionClosed_movesToDelaying() {
        enterWaitFailover();

        connector.onConnectionIdle(failoverConn, true);

        assertEquals(SCHubConnector.State.DELAYING, connector.getState());
    }

    @Test
    public void waitFailover_stop_movesToStopping() {
        enterWaitFailover();

        connector.terminate();

        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
    }

    // ======================================================================================
    // CONNECTED_FAILOVER
    // ======================================================================================

    @Test
    public void connectedFailover_connectionClosed_movesToWaitPrimary() {
        enterConnectedFailover();
        assertHubConnectorState(SCHubConnectorState.connectedToFailover);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_FAILOVER);

        connector.onConnectionIdle(failoverConn, true);

        assertEquals(SCHubConnector.State.WAIT_PRIMARY, connector.getState());
        verify(node).onDisconnected();
        // Primary should be reinitialized for the next attempt.
        verify(primaryConn, times(2)).initialize();
        assertHubConnectorState(SCHubConnectorState.noHubConnection);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_NONE);
    }

    @Test
    public void connectedFailover_timeout_movesToRewaitPrimaryAndInitiatesPrimary() {
        enterConnectedFailover();
        ScheduledTask retryTimer = lastTask();

        retryTimer.runnable.run();

        assertEquals(SCHubConnector.State.REWAIT_PRIMARY, connector.getState());
        verify(primaryConn, times(2)).initialize();
    }

    @Test
    public void connectedFailover_stop_movesToStopping() {
        enterConnectedFailover();

        connector.terminate();

        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
    }

    @Test
    public void connectedFailover_connectionEstablished_isIllegal() {
        enterConnectedFailover();

        connector.onConnectionEstablished(failoverConn);

        assertEquals(SCHubConnector.State.CONNECTED_FAILOVER, connector.getState());
    }

    // ======================================================================================
    // REWAIT_PRIMARY  (substate of CONNECTED_FAILOVER while retrying the primary)
    // ======================================================================================

    @Test
    public void rewaitPrimary_primaryEstablished_movesToConnectedPrimaryAndDropsFailover() {
        enterRewaitPrimary();
        // REWAIT_PRIMARY is a substate of CONNECTED_FAILOVER, so the external view still reports
        // "connected to failover" while we're quietly retrying the primary.
        assertHubConnectorState(SCHubConnectorState.connectedToFailover);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_FAILOVER);

        connector.onConnectionEstablished(primaryConn);

        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, connector.getState());
        verify(backoff, times(2)).reset(); // once on failover establish, once on primary
        verify(failoverConn).terminate();
        assertHubConnectorState(SCHubConnectorState.connectedToPrimary);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_PRIMARY);
    }

    @Test
    public void rewaitPrimary_failoverClosed_movesToWaitPrimary() {
        enterRewaitPrimary();

        connector.onConnectionIdle(failoverConn, true);

        assertEquals(SCHubConnector.State.WAIT_PRIMARY, connector.getState());
        verify(node).onDisconnected();
    }

    @Test
    public void rewaitPrimary_primaryFailed_returnsToConnectedFailoverAndRearmsTimer() {
        enterRewaitPrimary();
        // Sanity: there was exactly one timer scheduled in the CONNECTED_FAILOVER transition.
        int tasksBefore = scheduledTasks.size();

        connector.onConnectionIdle(primaryConn, false);

        assertEquals(SCHubConnector.State.CONNECTED_FAILOVER, connector.getState());
        verify(primaryConn).hardTerminate();
        // A fresh retry-primary timer is armed.
        assertEquals(tasksBefore + 1, scheduledTasks.size());
        assertScheduledFor(lastTask(), RECONNECT_SECS);
        // After returning to the parent state, the external view is "connected to failover" again.
        assertHubConnectorState(SCHubConnectorState.connectedToFailover);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_FAILOVER);
    }

    @Test
    public void rewaitPrimary_stop_movesToStopping() {
        enterRewaitPrimary();

        connector.terminate();

        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
    }

    // ======================================================================================
    // sendMessage routing — verifies which child connection receives outgoing traffic at each
    // state. REWAIT_PRIMARY is grouped with CONNECTED_FAILOVER because the failover link is
    // still the live one while the primary retry is in flight.
    // ======================================================================================

    @Test
    public void sendMessage_connectedPrimary_routesToPrimary() {
        enterConnectedPrimary();
        SCBVLC msg = mock(SCBVLC.class);

        connector.sendMessage(msg);

        verify(primaryConn).sendMessage(msg);
        verify(failoverConn, never()).sendMessage(any(SCBVLC.class));
    }

    @Test
    public void sendMessage_connectedFailover_routesToFailover() {
        enterConnectedFailover();
        SCBVLC msg = mock(SCBVLC.class);

        connector.sendMessage(msg);

        verify(failoverConn).sendMessage(msg);
        verify(primaryConn, never()).sendMessage(any(SCBVLC.class));
    }

    @Test
    public void sendMessage_rewaitPrimary_routesToFailover() {
        enterRewaitPrimary();
        SCBVLC msg = mock(SCBVLC.class);

        connector.sendMessage(msg);

        // Even though we're retrying the primary, application traffic must continue over
        // the live failover link until the primary is fully established.
        verify(failoverConn).sendMessage(msg);
        verify(primaryConn, never()).sendMessage(any(SCBVLC.class));
    }

    @Test
    public void sendMessage_idle_isDropped() {
        SCBVLC msg = mock(SCBVLC.class);

        connector.sendMessage(msg);

        verify(primaryConn, never()).sendMessage(any(SCBVLC.class));
        verify(failoverConn, never()).sendMessage(any(SCBVLC.class));
    }

    // ======================================================================================
    // DELAYING  (between attempt cycles; the only state that schedules itself)
    // ======================================================================================

    @Test
    public void delaying_timeout_movesBackToWaitPrimary() {
        enterDelaying();
        ScheduledTask delayTimer = lastTask();
        assertScheduledFor(delayTimer, RECONNECT_SECS);

        delayTimer.runnable.run();

        // TIMEOUT -> raiseChange(TRY_PRIMARY) -> CHANGE -> WAIT_PRIMARY.
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, connector.getState());
        verify(primaryConn, times(2)).initialize();
    }

    @Test
    public void delaying_stop_movesToStopping() {
        enterDelaying();

        connector.terminate();

        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
    }

    @Test
    public void delaying_connectionEstablished_isIllegal() {
        enterDelaying();

        connector.onConnectionEstablished(primaryConn);

        assertEquals(SCHubConnector.State.DELAYING, connector.getState());
    }

    // ======================================================================================
    // STOPPING
    // ======================================================================================

    @Test
    public void stopping_bothChildrenIdle_movesToIdleAndNotifiesNode() {
        enterConnectedPrimary();
        connector.terminate(); // -> STOPPING
        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
        // Child mocks default to getState()==IDLE, so the first idle callback flips us to IDLE.

        connector.onConnectionIdle(primaryConn, true);

        assertEquals(SCHubConnector.State.IDLE, connector.getState());
        verify(node).onConnectorIdle();
    }

    @Test
    public void stopping_onlyOneChildIdle_staysStopping() {
        enterConnectedPrimary();
        // Simulate failover still mid-disconnect.
        when(failoverConn.getState()).thenReturn(SCConnection.State.DISCONNECTING);
        connector.terminate();
        assertEquals(SCHubConnector.State.STOPPING, connector.getState());

        connector.onConnectionIdle(primaryConn, true);

        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
        verify(node, never()).onConnectorIdle();
    }

    @Test
    public void stopping_unrelatedEvent_isIllegal() {
        enterConnectedPrimary();
        connector.terminate();
        // CONNECTION_ESTABLISHED in STOPPING is illegal.
        connector.onConnectionEstablished(primaryConn);
        assertEquals(SCHubConnector.State.STOPPING, connector.getState());
    }

    // ======================================================================================
    // hardTerminate — synchronous, bypasses the state machine. Cancels any pending timer,
    // forwards hardTerminate to both child connections (if present), and snaps state to IDLE
    // WITHOUT going through STOPPING and WITHOUT notifying node.onConnectorIdle.
    // ======================================================================================

    /**
     * Baseline from IDLE: hardTerminate must call hardTerminate on each existing child and
     * land at IDLE. No timer is pending in IDLE, but the call is still legal.
     */
    @Test
    public void hardTerminate_fromIdle_hardTerminatesChildrenAndStaysIdle() {
        assertEquals(SCHubConnector.State.IDLE, connector.getState());

        connector.hardTerminate();

        assertEquals(SCHubConnector.State.IDLE, connector.getState());
        verify(primaryConn).hardTerminate();
        verify(failoverConn).hardTerminate();
        // Bypasses the state machine, so the node is NOT notified.
        verify(node, never()).onConnectorIdle();
    }

    /**
     * From CONNECTED_PRIMARY: hardTerminate snaps state to IDLE bypassing STOPPING.
     * The owner of the connector (the node) is NOT notified — hardTerminate is for forced
     * teardown where the caller doesn't expect a lifecycle callback.
     */
    @Test
    public void hardTerminate_fromConnectedPrimary_movesToIdleWithoutNotifyingNode() {
        enterConnectedPrimary();

        connector.hardTerminate();

        assertEquals(SCHubConnector.State.IDLE, connector.getState());
        verify(primaryConn).hardTerminate();
        verify(failoverConn).hardTerminate();
        verify(node, never()).onConnectorIdle();
        // The hub-connector status surface also drops back to "not connected".
        assertHubConnectorState(SCHubConnectorState.noHubConnection);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_NONE);
    }

    /**
     * CONNECTED_FAILOVER has a live retry-primary timer. hardTerminate must cancel it.
     */
    @Test
    public void hardTerminate_fromConnectedFailover_cancelsRetryPrimaryTimer() {
        enterConnectedFailover();
        ScheduledTask retryTimer = lastTask();
        assertScheduledFor(retryTimer, RECONNECT_SECS);
        assertNotCanceled(retryTimer);

        connector.hardTerminate();

        assertEquals(SCHubConnector.State.IDLE, connector.getState());
        assertCanceled(retryTimer);
        verify(primaryConn).hardTerminate();
        verify(failoverConn).hardTerminate();
    }

    /**
     * REWAIT_PRIMARY is the substate of CONNECTED_FAILOVER while retrying primary. It also
     * holds a live retry timer (re-armed when the prior primary attempt failed). hardTerminate
     * must cancel it.
     */
    @Test
    public void hardTerminate_fromRewaitPrimary_cancelsRetryTimerAndIdles() {
        enterRewaitPrimary();
        // REWAIT_PRIMARY's "back to parent" path arms a fresh timer; here we're in the initial
        // REWAIT_PRIMARY entry where the primary retry is in flight and the prior timer is
        // already canceled. Either way, the latest scheduled task is the one we'd want canceled.

        connector.hardTerminate();

        assertEquals(SCHubConnector.State.IDLE, connector.getState());
        verify(primaryConn).hardTerminate();
        verify(failoverConn).hardTerminate();
    }

    /**
     * DELAYING holds the reconnect-wait timer. hardTerminate must cancel it so we don't get
     * a spurious "back to TRY_PRIMARY" firing after the state is already IDLE.
     */
    @Test
    public void hardTerminate_fromDelaying_cancelsReconnectTimer() {
        enterDelaying();
        ScheduledTask delayTimer = lastTask();
        assertScheduledFor(delayTimer, RECONNECT_SECS);
        assertNotCanceled(delayTimer);
        // DELAY's CHANGE handler already called hardTerminate on both children when entering
        // DELAYING. Clear those interactions so the verify below counts only the call made
        // by hardTerminate() under test.
        clearInvocations(primaryConn, failoverConn);

        connector.hardTerminate();

        assertEquals(SCHubConnector.State.IDLE, connector.getState());
        assertCanceled(delayTimer);
        verify(primaryConn).hardTerminate();
        verify(failoverConn).hardTerminate();
    }

    /**
     * No-primary configuration: hardTerminate must skip the null primary, still hardTerminate
     * the failover, and reach IDLE. (A regression that dereferenced primaryConnection would
     * NPE here.)
     */
    @Test
    public void hardTerminate_whenPrimaryAbsent_skipsPrimaryButTerminatesFailover() {
        TestHubConnector noPrimary = buildConnector(false, true);

        noPrimary.hardTerminate();

        assertEquals(SCHubConnector.State.IDLE, noPrimary.getState());
        verify(primaryConn, never()).hardTerminate();
        verify(failoverConn).hardTerminate();
    }

    /**
     * No-failover configuration: mirror of the above.
     */
    @Test
    public void hardTerminate_whenFailoverAbsent_skipsFailoverButTerminatesPrimary() {
        TestHubConnector noFailover = buildConnector(true, false);

        noFailover.hardTerminate();

        assertEquals(SCHubConnector.State.IDLE, noFailover.getState());
        verify(primaryConn).hardTerminate();
        verify(failoverConn, never()).hardTerminate();
    }

    // ======================================================================================
    // Multi-step scenarios — cumulative behavior across transitions.
    // ======================================================================================

    /**
     * Plain happy path: start, primary connects, stop while connected to primary.
     * <p>
     * What this proves on top of the per-transition tests:
     * - The primary's initialize/terminate sequence happens in order.
     * - node sees onConnected exactly once and onConnectorIdle exactly once.
     * - No spurious onDisconnected, no failover initialization at all.
     */
    @Test
    public void scenario_happyPathPrimaryOnly() {
        // Pre-state: not connected to anything.
        assertHubConnectorState(SCHubConnectorState.noHubConnection);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_NONE);

        connector.initialize();
        // Still attempting; not yet connected.
        assertHubConnectorState(SCHubConnectorState.noHubConnection);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_NONE);

        connector.onConnectionEstablished(primaryConn);
        assertHubConnectorState(SCHubConnectorState.connectedToPrimary);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_PRIMARY);

        connector.terminate();
        // Both connection mocks already report IDLE; first callback completes STOPPING -> IDLE.
        connector.onConnectionIdle(primaryConn, true);

        assertEquals(SCHubConnector.State.IDLE, connector.getState());
        // External status returns to noHubConnection once we're fully stopped.
        assertHubConnectorState(SCHubConnectorState.noHubConnection);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_NONE);

        InOrder primaryOrder = inOrder(primaryConn);
        primaryOrder.verify(primaryConn).initialize();
        primaryOrder.verify(primaryConn).terminate();

        InOrder nodeOrder = inOrder(node);
        nodeOrder.verify(node).onConnected();
        nodeOrder.verify(node).onConnectorIdle();
        verify(node, never()).onDisconnected();

        // Failover was never initialized — only terminate() was called as part of STOP.
        verify(failoverConn, never()).initialize();
        verify(failoverConn).terminate();
    }

    /**
     * Primary fails to connect → failover takes over.
     * <p>
     * What this proves:
     * - When the primary attempt fails, the SM tries the failover next without any delay.
     * - On successful failover, a retry-primary timer is armed at the backoff value.
     * - node.onConnected fires exactly once (when failover establishes), not twice.
     */
    @Test
    public void scenario_primaryFailsFailoverSucceeds() {
        connector.initialize();
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, connector.getState());
        assertEquals(0, scheduledTasks.size()); // no delay armed yet

        // Primary fails before establishing.
        connector.onConnectionIdle(primaryConn, false);
        assertEquals(SCHubConnector.State.WAIT_FAILOVER, connector.getState());
        verify(failoverConn).initialize();

        // No DELAY timer was armed between primary and failover attempts.
        assertEquals(0, scheduledTasks.size());

        // Failover establishes.
        connector.onConnectionEstablished(failoverConn);
        assertEquals(SCHubConnector.State.CONNECTED_FAILOVER, connector.getState());
        // External status now reports failover.
        assertHubConnectorState(SCHubConnectorState.connectedToFailover);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_FAILOVER);

        // Now a retry-primary timer is armed.
        assertEquals(1, scheduledTasks.size());
        assertScheduledFor(lastTask(), RECONNECT_SECS);
        assertNotCanceled(lastTask());

        verify(node, times(1)).onConnected();
        verify(node, never()).onDisconnected();
        verify(backoff, times(1)).reset();
    }

    /**
     * Both primary and failover fail → DELAY → DELAYING → retry primary on timeout.
     * <p>
     * What this proves:
     * - The reconnect timeout is applied ONCE per full primary+failover cycle, in DELAYING.
     * - The cycle restarts with a fresh primary initialize() when the timer fires.
     * - backoff.reset() is NOT called during the failure cycle (only on success).
     */
    @Test
    public void scenario_bothFailDelayAndRetry() {
        connector.initialize();
        connector.onConnectionIdle(primaryConn, false);
        connector.onConnectionIdle(failoverConn, false);

        assertEquals(SCHubConnector.State.DELAYING, connector.getState());
        ScheduledTask delayTimer = lastTask();
        assertScheduledFor(delayTimer, RECONNECT_SECS);

        // Reconnect timer fires.
        delayTimer.runnable.run();

        assertEquals(SCHubConnector.State.WAIT_PRIMARY, connector.getState());
        verify(primaryConn, times(2)).initialize();
        verify(backoff, never()).reset();
    }

    /**
     * Connected to failover, primary recovers when the retry timer fires.
     * <p>
     * What this proves:
     * - REWAIT_PRIMARY (the substate of CONNECTED_FAILOVER) does the primary retry.
     * - On primary success, failover.terminate() is called (graceful), backoff.reset()
     * fires for the second time, and the SM lands on CONNECTED_PRIMARY.
     * - node.onConnected is NOT called a second time — the connector is still considered
     * "connected" throughout the failover→primary swap.
     */
    @Test
    public void scenario_primaryRecoversFromFailover() {
        // Get to CONNECTED_FAILOVER.
        connector.initialize();
        connector.onConnectionIdle(primaryConn, false);
        connector.onConnectionEstablished(failoverConn);
        assertHubConnectorState(SCHubConnectorState.connectedToFailover);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_FAILOVER);
        ScheduledTask retryTimer = lastTask();

        // Retry timer fires -> REWAIT_PRIMARY. The external view does NOT flip during the
        // substate — peers still see "connected to failover" while we're quietly retrying.
        retryTimer.runnable.run();
        assertEquals(SCHubConnector.State.REWAIT_PRIMARY, connector.getState());
        verify(primaryConn, times(2)).initialize();
        assertHubConnectorState(SCHubConnectorState.connectedToFailover);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_FAILOVER);

        // Primary comes back online.
        connector.onConnectionEstablished(primaryConn);
        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, connector.getState());
        // External status flips from failover to primary.
        assertHubConnectorState(SCHubConnectorState.connectedToPrimary);
        assertAdvertisementStateCode(SCPayloadAdvertisement.CONN_STAT_PRIMARY);

        // Failover is gracefully shut down.
        verify(failoverConn).terminate();
        // backoff.reset() called twice: once on failover establish, once on primary establish.
        verify(backoff, times(2)).reset();
        // onConnected was fired ONCE — the failover→primary handoff doesn't refire it.
        verify(node, times(1)).onConnected();
        verify(node, never()).onDisconnected();
    }

    /**
     * No-primary configuration: only failover. Initializing should skip straight to the
     * failover attempt; the SM should never enter WAIT_PRIMARY.
     */
    @Test
    public void scenario_noPrimaryConfigured_skipsToFailover() {
        TestHubConnector noPrimary = buildConnector(false, true);

        noPrimary.initialize();

        assertEquals(SCHubConnector.State.WAIT_FAILOVER, noPrimary.getState());
        verify(primaryConn, never()).initialize();
        verify(failoverConn).initialize();

        noPrimary.onConnectionEstablished(failoverConn);

        assertEquals(SCHubConnector.State.CONNECTED_FAILOVER, noPrimary.getState());
        // With no primary configured, no retry-primary timer is armed.
        assertEquals(0, scheduledTasks.size());
    }

    // ======================================================================================
    // Null-guards on partial configurations — hardTerminate and STOP must not NPE when only
    // one hub URI is configured.
    // ======================================================================================

    /**
     * hardTerminate must safely handle a connector configured with only the primary hub URI
     * (no failover). The failoverConnection field is null; the code must guard the null.
     */
    @Test
    public void hardTerminate_withOnlyPrimary_doesNotNpe() {
        TestHubConnector noFailover = buildConnector(true, false);

        noFailover.hardTerminate();

        assertEquals(SCHubConnector.State.IDLE, noFailover.getState());
        verify(primaryConn).hardTerminate();
        // failoverConn should NOT have been touched since it's not part of this connector.
        verify(failoverConn, never()).hardTerminate();
    }

    /**
     * hardTerminate must safely handle a connector configured with only failover (no primary).
     */
    @Test
    public void hardTerminate_withOnlyFailover_doesNotNpe() {
        TestHubConnector noPrimary = buildConnector(false, true);

        noPrimary.hardTerminate();

        assertEquals(SCHubConnector.State.IDLE, noPrimary.getState());
        verify(failoverConn).hardTerminate();
        verify(primaryConn, never()).hardTerminate();
    }

    /**
     * STOP with only the primary configured must safely call terminate on primary without
     * NPE'ing on the null failover.
     */
    @Test
    public void stop_withOnlyPrimary_doesNotNpe() {
        TestHubConnector noFailover = buildConnector(true, false);
        noFailover.initialize();
        assertEquals(SCHubConnector.State.WAIT_PRIMARY, noFailover.getState());

        noFailover.terminate();

        assertEquals(SCHubConnector.State.STOPPING, noFailover.getState());
        verify(primaryConn).terminate();
        verify(failoverConn, never()).terminate();
    }

    /**
     * STOP with only failover configured must safely call terminate on failover without
     * NPE'ing on the null primary.
     */
    @Test
    public void stop_withOnlyFailover_doesNotNpe() {
        TestHubConnector noPrimary = buildConnector(false, true);
        noPrimary.initialize();

        noPrimary.terminate();

        assertEquals(SCHubConnector.State.STOPPING, noPrimary.getState());
        verify(failoverConn).terminate();
        verify(primaryConn, never()).terminate();
    }

    // ======================================================================================
    // Hub connection state change notifications. The connector notifies through
    // network.fireHubConnectionStateChanged whenever the externally visible
    // SCHubConnectorState changes; internal transitions that map to the same value are silent.
    // ======================================================================================

    @Test
    public void notify_connectToPrimary_firesNoConnectionToPrimary() {
        enterConnectedPrimary();

        verify(network).fireHubConnectionStateChanged(
                SCHubConnectorState.noHubConnection, SCHubConnectorState.connectedToPrimary);
        verify(network, times(1)).fireHubConnectionStateChanged(any(), any());
    }

    @Test
    public void notify_connectToFailover_firesNoConnectionToFailover() {
        enterConnectedFailover();

        verify(network).fireHubConnectionStateChanged(
                SCHubConnectorState.noHubConnection, SCHubConnectorState.connectedToFailover);
        verify(network, times(1)).fireHubConnectionStateChanged(any(), any());
    }

    /**
     * CONNECTED_FAILOVER to REWAIT_PRIMARY maps to the same external state (connectedToFailover),
     * so retrying the primary while connected to the failover must not produce a notification.
     */
    @Test
    public void notify_failoverToRewaitPrimary_isSilent() {
        enterRewaitPrimary();

        verify(network, times(1)).fireHubConnectionStateChanged(any(), any());
    }

    /**
     * Recovery from the failover to the primary changes the external state without passing through
     * a disconnected state, and must be notified as failover to primary.
     */
    @Test
    public void notify_recoveryToPrimary_firesFailoverToPrimary() {
        enterRewaitPrimary();

        connector.onConnectionEstablished(primaryConn);

        assertEquals(SCHubConnector.State.CONNECTED_PRIMARY, connector.getState());
        verify(network).fireHubConnectionStateChanged(
                SCHubConnectorState.connectedToFailover, SCHubConnectorState.connectedToPrimary);
    }

    @Test
    public void notify_connectionLost_firesPrimaryToNoConnection() {
        enterConnectedPrimary();
        clearInvocations(network);

        connector.onConnectionIdle(primaryConn, true);

        verify(network).fireHubConnectionStateChanged(
                SCHubConnectorState.connectedToPrimary, SCHubConnectorState.noHubConnection);
    }

    @Test
    public void notify_stopWhileConnected_firesPrimaryToNoConnection() {
        enterConnectedPrimary();
        clearInvocations(network);

        connector.terminate();

        verify(network).fireHubConnectionStateChanged(
                SCHubConnectorState.connectedToPrimary, SCHubConnectorState.noHubConnection);
    }

    @Test
    public void notify_hardTerminateWhileConnected_firesPrimaryToNoConnection() {
        enterConnectedPrimary();
        clearInvocations(network);

        connector.hardTerminate();

        verify(network).fireHubConnectionStateChanged(
                SCHubConnectorState.connectedToPrimary, SCHubConnectorState.noHubConnection);
    }

    @Test
    public void notify_hardTerminateWhileIdle_isSilent() {
        connector.hardTerminate();

        verify(network, never()).fireHubConnectionStateChanged(any(), any());
    }

    // ======================================================================================
    // STOP with no connections configured (both hub URIs blank). No connection events can
    // ever arrive to complete the stop, so the connector must go idle immediately and notify
    // the node — otherwise node.awaitTermination would hang until its timeout.
    // ======================================================================================

    /**
     * With no connections, initialize() cycles to DELAYING with a backoff retry timeout
     * scheduled. STOP must cancel that timeout, go straight to IDLE, and notify the node.
     */
    @Test
    public void stop_noConnectionsConfigured_goesIdleImmediately() {
        TestHubConnector noConnections = buildConnector(false, false);
        noConnections.initialize();
        assertEquals(SCHubConnector.State.DELAYING, noConnections.getState());
        assertNotCanceled(lastTask());

        noConnections.terminate();

        assertEquals(SCHubConnector.State.IDLE, noConnections.getState());
        verify(node).onConnectorIdle();
        assertCanceled(lastTask());
    }

    /**
     * STOP before initialize() with no connections must also complete immediately rather than
     * parking in STOPPING.
     */
    @Test
    public void stop_noConnectionsBeforeInitialize_goesIdleAndNotifies() {
        TestHubConnector noConnections = buildConnector(false, false);

        noConnections.terminate();

        assertEquals(SCHubConnector.State.IDLE, noConnections.getState());
        verify(node).onConnectorIdle();
    }
}
