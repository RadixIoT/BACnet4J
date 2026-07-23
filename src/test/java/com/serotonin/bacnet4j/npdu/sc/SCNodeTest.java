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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.sc.msg.SCBVLC;
import com.serotonin.bacnet4j.npdu.sc.msg.SCOption;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * State machine coverage for SCNode. Mirrors SCConnectionTest / SCHubConnectorTest:
 * inline localDevice.execute(...) serializes events on the test thread, a ScheduledTask harness
 * captures schedule() calls so timer delay and cancellation can be asserted, and a TestNode
 * subclass overrides createHubConnector(...) to inject a Mockito mock.
 * <p>
 * The node has a small state machine (IDLE → STARTING → STARTED, plus a NEW_MAC_STOPPING
 * detour for VMAC collisions) and a separate message-handling path on the onIncoming(...)
 * method. Both are exercised here.
 */
public class SCNodeTest {
    private static final byte[] LOCAL_VMAC_BYTES = {0x02, 0x11, 0x22, 0x33, 0x44, 0x55};
    private static final byte[] LOCAL_UUID_BYTES = new byte[16];
    private static final int DISCONNECT_WAIT_SECS = 8;

    private SCNetwork network;
    private SCHubConnector hubConnector;
    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();
    private TestNode node;


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
        network = mock(SCNetwork.class);
        var transport = mock(Transport.class);
        var localDevice = mock(LocalDevice.class);
        hubConnector = mock(SCHubConnector.class);

        when(transport.getLocalDevice()).thenReturn(localDevice);
        when(network.getDisconnectWaitTimeout()).thenReturn(new UnsignedInteger(DISCONNECT_WAIT_SECS));
        when(network.getMaxBvlcLengthAccepted()).thenReturn(new UnsignedInteger(1500));
        when(network.getMaxNpduLengthAccepted()).thenReturn(new UnsignedInteger(1497));
        when(network.getVmac()).thenReturn(new OctetString(LOCAL_VMAC_BYTES));
        when(network.getDeviceUUID()).thenReturn(new OctetString(LOCAL_UUID_BYTES));

        // Run every queued event inline so the SM runs synchronously.
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(localDevice).execute(any(Runnable.class));

        // Capture scheduled tasks so timer delay and cancellation can be asserted.
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

        node = new TestNode(network, hubConnector);
        node.configure(transport);
    }

    // --------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------


    private static class TestNode extends SCNode {
        private final SCHubConnector mockHubConnector;

        TestNode(SCNetwork network, SCHubConnector mockHubConnector) {
            super(network);
            this.mockHubConnector = mockHubConnector;
        }

        @Override
        protected SCHubConnector createHubConnector(SCNetwork network) {
            return mockHubConnector;
        }
    }

    // ---- Scheduled-task helpers ----

    private ScheduledTask lastTask() {
        return scheduledTasks.get(scheduledTasks.size() - 1);
    }

    private void assertScheduledFor(ScheduledTask task, int expectedSeconds) {
        assertEquals("scheduled unit", TimeUnit.SECONDS, task.unit);
        assertEquals("scheduled delay (seconds)", expectedSeconds, task.delay);
    }

    private void assertCanceled(ScheduledTask task) {
        assertTrue("expected scheduled task to be canceled", task.canceled);
    }

    private void assertNotCanceled(ScheduledTask task) {
        assertFalse("expected scheduled task to be alive", task.canceled);
    }

    // ---- State drivers ----

    private void enterStarting() {
        node.initialize();
        assertEquals(SCNode.State.STARTING, node.getState());
    }

    private void enterStarted() {
        enterStarting();
        node.onConnected();
        assertEquals(SCNode.State.STARTED, node.getState());
    }

    private void enterNewMacStopping() {
        enterStarting();
        node.restartWithNewVMAC();
        assertEquals(SCNode.State.NEW_MAC_STOPPING, node.getState());
    }

    /** Drives the SM IDLE → STARTING → STOPPING via terminate(). */
    private void enterStopping() {
        enterStarting();
        node.terminate();
        assertEquals(SCNode.State.STOPPING, node.getState());
    }

    // ---- Message builders ----

    private static SCBVLC advertisementSolicitation(SCVmac originating) {
        return new SCBVLC(originating, null, SCBVLC.ADVERTISEMENT_SOLICITATION, 1);
    }

    private static SCBVLC encapsulatedNpdu(SCVmac originating) {
        return new SCBVLC(originating, null, SCBVLC.ENCAPSULATED_NPDU, new byte[] {0x01, 0x02}, 1);
    }

    private static SCBVLC withDestOption(SCBVLC base, SCOption option) {
        return new SCBVLC(base.getOriginating(), base.getDestination(), base.getFunction(),
                base.getPayload(), base.getId(), List.of(option), null);
    }

    private static SCVmac peerVmac() {
        return new SCVmac(new byte[] {0x02, 0x66, 0x77, 0x66, 0x77, 0x66});
    }

    // ======================================================================================
    // IDLE
    // ======================================================================================

    @Test
    public void idle_start_movesToStartingAndInitializesHubConnector() {
        node.initialize();

        assertEquals(SCNode.State.STARTING, node.getState());
        verify(hubConnector).initialize();
    }

    @Test
    public void idle_stop_staysIdle() {
        node.terminate();

        assertEquals(SCNode.State.IDLE, node.getState());
        // STOP from IDLE completes termination immediately (there is nothing to shut down).
        // The hub connector must NOT be terminated.
        verify(hubConnector, never()).terminate();
    }

    @Test
    public void idle_change_isIllegal() {
        node.onConnected(); // fires CHANGE
        assertEquals(SCNode.State.IDLE, node.getState());
        verify(hubConnector, never()).initialize();
    }

    @Test
    public void idle_newMac_isIllegal() {
        node.restartWithNewVMAC();
        assertEquals(SCNode.State.IDLE, node.getState());
        verify(network, never()).setVmac(any(OctetString.class));
    }

    @Test
    public void idle_disconnected_isIllegal() {
        node.onDisconnected();
        assertEquals(SCNode.State.IDLE, node.getState());
    }

    @Test
    public void idle_connectorIdle_isIllegal() {
        node.onConnectorIdle();
        assertEquals(SCNode.State.IDLE, node.getState());
    }

    // ======================================================================================
    // STARTING
    // ======================================================================================

    @Test
    public void starting_change_movesToStarted() {
        enterStarting();

        node.onConnected();

        assertEquals(SCNode.State.STARTED, node.getState());
    }

    @Test
    public void starting_newMac_movesToNewMacStoppingAndTerminatesHubConnector() {
        enterStarting();

        node.restartWithNewVMAC();

        assertEquals(SCNode.State.NEW_MAC_STOPPING, node.getState());
        verify(hubConnector).terminate();
        // Disconnect-wait timer is armed so we don't wait forever for the connector to idle.
        assertScheduledFor(lastTask(), DISCONNECT_WAIT_SECS);
        assertNotCanceled(lastTask());
        // VMAC is not yet rotated — that happens after the connector has gone idle.
        verify(network, never()).setVmac(any(OctetString.class));
    }

    @Test
    public void starting_stop_terminatesAndMovesToStopping() {
        enterStarting();

        node.terminate();

        assertEquals(SCNode.State.STOPPING, node.getState());
        // STOP is the only call to hubConnector.terminate() in this flow; initialize() doesn't terminate.
        verify(hubConnector, times(1)).terminate();
    }

    @Test
    public void starting_disconnected_isIllegal() {
        enterStarting();

        node.onDisconnected();

        assertEquals(SCNode.State.STARTING, node.getState());
    }

    @Test
    public void starting_connectorIdle_isIllegal() {
        enterStarting();

        node.onConnectorIdle();

        assertEquals(SCNode.State.STARTING, node.getState());
    }

    @Test
    public void starting_start_isIllegal() {
        enterStarting();

        node.initialize();

        assertEquals(SCNode.State.STARTING, node.getState());
        verify(hubConnector, times(1)).initialize(); // the original initialize from enterStarting()
    }

    // ======================================================================================
    // NEW_MAC_STOPPING
    // ======================================================================================

    @Test
    public void newMacStopping_disconnected_restartsWithNewVmac() {
        enterNewMacStopping();
        ScheduledTask timer = lastTask();

        node.onDisconnected();

        // VMAC is rotated and the node re-enters STARTING with hubConnector reinitialized.
        assertEquals(SCNode.State.STARTING, node.getState());
        verify(network).setVmac(any(OctetString.class));
        verify(hubConnector, times(2)).initialize();
        assertCanceled(timer);
        // We did NOT need to hard-terminate; the connector closed gracefully.
        verify(hubConnector, never()).hardTerminate();
    }

    @Test
    public void newMacStopping_connectorIdle_restartsWithNewVmac() {
        enterNewMacStopping();
        ScheduledTask timer = lastTask();

        node.onConnectorIdle();

        assertEquals(SCNode.State.STARTING, node.getState());
        verify(network).setVmac(any(OctetString.class));
        verify(hubConnector, times(2)).initialize();
        assertCanceled(timer);
        verify(hubConnector, never()).hardTerminate();
    }

    @Test
    public void newMacStopping_timeout_hardTerminatesAndRestartsWithNewVmac() {
        enterNewMacStopping();
        ScheduledTask timer = lastTask();
        assertScheduledFor(timer, DISCONNECT_WAIT_SECS);

        timer.runnable.run();

        // The disconnect-wait fallback: connector didn't idle in time, force-close it
        // and proceed with the VMAC rotation anyway.
        assertEquals(SCNode.State.STARTING, node.getState());
        verify(hubConnector).hardTerminate();
        verify(network).setVmac(any(OctetString.class));
        verify(hubConnector, times(2)).initialize();
    }

    @Test
    public void newMacStopping_stop_movesToStopping() {
        enterNewMacStopping();

        node.terminate();

        assertEquals(SCNode.State.STOPPING, node.getState());
        // STOP is the global handler — it always calls hubConnector.terminate().
        verify(hubConnector, times(2)).terminate(); // once on NEW_MAC, once on STOP
    }

    @Test
    public void newMacStopping_start_isIllegal() {
        enterNewMacStopping();

        node.initialize();

        assertEquals(SCNode.State.NEW_MAC_STOPPING, node.getState());
        verify(network, never()).setVmac(any(OctetString.class));
    }

    @Test
    public void newMacStopping_change_isIllegal() {
        enterNewMacStopping();

        node.onConnected();

        assertEquals(SCNode.State.NEW_MAC_STOPPING, node.getState());
        verify(network, never()).setVmac(any(OctetString.class));
    }

    @Test
    public void newMacStopping_newMac_isIllegal() {
        enterNewMacStopping();

        node.restartWithNewVMAC();

        assertEquals(SCNode.State.NEW_MAC_STOPPING, node.getState());
        verify(network, never()).setVmac(any(OctetString.class));
    }

    // ======================================================================================
    // STARTED
    // ======================================================================================

    @Test
    public void started_disconnected_isIgnored() {
        enterStarted();

        node.onDisconnected();

        // STARTED tolerates DISCONNECTED silently because the hub connector restarts itself.
        assertEquals(SCNode.State.STARTED, node.getState());
        verify(network, never()).setVmac(any(OctetString.class));
    }

    @Test
    public void started_stop_terminatesAndMovesToStopping() {
        enterStarted();

        node.terminate();

        assertEquals(SCNode.State.STOPPING, node.getState());
        verify(hubConnector, times(1)).terminate();
    }

    @Test
    public void started_start_isIllegal() {
        enterStarted();

        node.initialize();

        assertEquals(SCNode.State.STARTED, node.getState());
        verify(hubConnector, times(1)).initialize(); // only the one from enterStarted()
    }

    @Test
    public void started_change_isIllegal() {
        enterStarted();

        node.onConnected();

        assertEquals(SCNode.State.STARTED, node.getState());
    }

    @Test
    public void started_connectorIdle_isIllegal() {
        enterStarted();

        node.onConnectorIdle();

        assertEquals(SCNode.State.STARTED, node.getState());
    }

    @Test
    public void started_newMac_restartWithNewMac() {
        // Spec AB.6.2.2: duplicate-VMAC NAK can arrive at any point after the initial connect,
        // including after STARTED was reached (e.g. on a reconnect cycle). Both STARTING and
        // STARTED route through the same newMac() helper, so the side effects should match.
        enterStarted();

        node.restartWithNewVMAC();

        assertEquals(SCNode.State.NEW_MAC_STOPPING, node.getState());
        verify(hubConnector).terminate();
        // Disconnect-wait timer armed so we don't hang waiting for the connector to idle.
        assertScheduledFor(lastTask(), DISCONNECT_WAIT_SECS);
        assertNotCanceled(lastTask());
        // VMAC is not yet rotated — that happens after the connector has gone idle.
        verify(network, never()).setVmac(any(OctetString.class));
    }

    // ======================================================================================
    // STOPPING — the state the node sits in while waiting for the hub connector to drain
    // after terminate(). Legal exit is CONNECTOR_IDLE → IDLE; everything else is illegal,
    // except STOP which re-fires hubConnector.terminate() and stays in STOPPING.
    // ======================================================================================

    @Test
    public void stopping_connectorIdle_movesToIdle() {
        enterStopping();

        node.onConnectorIdle();

        assertEquals(SCNode.State.IDLE, node.getState());
    }

    @Test
    public void stopping_stop_remainsInStoppingAndReTerminates() {
        enterStopping();

        node.terminate();

        // The global STOP handler runs because state != IDLE, so hubConnector.terminate() is
        // called again (once from enterStopping's terminate, once from this one). Idempotent
        // on the connector, but worth pinning here.
        assertEquals(SCNode.State.STOPPING, node.getState());
        verify(hubConnector, times(2)).terminate();
    }

    @Test
    public void stopping_start_isIllegal() {
        enterStopping();

        node.initialize();

        assertEquals(SCNode.State.STOPPING, node.getState());
        verify(hubConnector, times(1)).initialize(); // only the one from enterStopping()
    }

    @Test
    public void stopping_change_isIllegal() {
        enterStopping();

        node.onConnected();

        assertEquals(SCNode.State.STOPPING, node.getState());
    }

    @Test
    public void stopping_newMac_isIllegal() {
        enterStopping();

        node.restartWithNewVMAC();

        assertEquals(SCNode.State.STOPPING, node.getState());
        verify(network, never()).setVmac(any(OctetString.class));
    }

    @Test
    public void stopping_disconnected_isIllegal() {
        enterStopping();

        node.onDisconnected();

        // Only CONNECTOR_IDLE exits STOPPING; DISCONNECTED is illegal here.
        assertEquals(SCNode.State.STOPPING, node.getState());
        verify(network, never()).setVmac(any(OctetString.class));
    }

    // ======================================================================================
    // awaitTermination — releases when the shutdown started by terminate() completes
    // ======================================================================================

    @Test
    public void awaitTermination_releasesWhenStoppingReachesIdle() throws Exception {
        enterStopping();
        assertFalse("node is still stopping", node.awaitTermination(0, TimeUnit.MILLISECONDS));

        node.onConnectorIdle();

        assertEquals(SCNode.State.IDLE, node.getState());
        assertTrue("node has terminated", node.awaitTermination(0, TimeUnit.MILLISECONDS));
    }

    @Test
    public void awaitTermination_releasesImmediatelyWhenStoppedWhileIdle() throws Exception {
        node.terminate();

        assertTrue(node.awaitTermination(0, TimeUnit.MILLISECONDS));
    }

    @Test
    public void awaitTermination_timesOutWhileStillStopping() throws Exception {
        enterStopping();

        assertFalse(node.awaitTermination(10, TimeUnit.MILLISECONDS));
    }

    // ======================================================================================
    // onIncoming — message handling, independent of the state machine
    // ======================================================================================

    @Test
    public void onIncoming_advertisementSolicitation_sendsAdvertisementToOriginator() {
        when(hubConnector.getStateAsInt()).thenReturn(1); // CONN_STAT_PRIMARY

        SCVmac peer = peerVmac();
        node.onIncoming(advertisementSolicitation(peer));

        // hubConnector.sendMessage was called with an ADVERTISEMENT BVLC.
        ArgumentCaptor<SCBVLC> cap = ArgumentCaptor.forClass(SCBVLC.class);
        verify(hubConnector).sendMessage(cap.capture());
        assertEquals(SCBVLC.ADVERTISEMENT, cap.getValue().getFunction());
    }

    @Test
    public void onIncoming_encapsulatedNpdu_passedToNetwork() {
        SCBVLC msg = encapsulatedNpdu(peerVmac());
        node.onIncoming(msg);

        verify(network).onIncoming(msg);
        verify(hubConnector, never()).sendMessage(any(SCBVLC.class));
    }

    @Test
    public void onIncoming_mustUnderstandDestOption_unicast_sendsErrorResponseAndDrops() {
        // ADVERTISEMENT_SOLICITATION is a unicast request per SCBVLC.isUnicastRequest().
        SCBVLC base = advertisementSolicitation(peerVmac());
        SCBVLC withOpt = withDestOption(base, new SCOption(SCOption.TYPE_PROPRIETARY, true));

        node.onIncoming(withOpt);

        // An error response went out via hubConnector.sendMessage with function BVLC_RESULT.
        ArgumentCaptor<SCBVLC> cap = ArgumentCaptor.forClass(SCBVLC.class);
        verify(hubConnector).sendMessage(cap.capture());
        SCBVLC sent = cap.getValue();
        assertEquals(SCBVLC.BVLC_RESULT, sent.getFunction());

        // Network was NOT given the message — it was rejected at the node level.
        verify(network, never()).onIncoming(any(SCBVLC.class));
    }

    @Test
    public void onIncoming_mustUnderstandDestOption_broadcast_dropsSilently() {
        // ADVERTISEMENT (not -SOLICITATION) is NOT a unicast request, so a must-understand
        // failure here doesn't generate a response.
        SCBVLC nonUnicast = new SCBVLC(peerVmac(), null, SCBVLC.ADVERTISEMENT,
                new byte[] {0, 0, 0, 0}, 1);
        SCBVLC withOpt = withDestOption(nonUnicast, new SCOption(SCOption.TYPE_PROPRIETARY, true));

        node.onIncoming(withOpt);

        verify(hubConnector, never()).sendMessage(any(SCBVLC.class));
        verify(network, never()).onIncoming(any(SCBVLC.class));
    }

    @Test
    public void onIncoming_unknownFunction_isLoggedAndDropped() {
        // HEARTBEAT_REQUEST is valid for SCConnection but not for the node-level dispatcher.
        SCBVLC unexpected = new SCBVLC(peerVmac(), null, SCBVLC.HEARTBEAT_REQUEST, 1);

        node.onIncoming(unexpected);

        verify(network, never()).onIncoming(any(SCBVLC.class));
        verify(hubConnector, never()).sendMessage(any(SCBVLC.class));
    }

    // ======================================================================================
    // sendMessage — delegates to hubConnector
    // ======================================================================================

    @Test
    public void sendMessage_delegatesToHubConnector() {
        SCBVLC msg = encapsulatedNpdu(peerVmac());
        node.sendMessage(msg);
        verify(hubConnector).sendMessage(msg);
    }

    // ======================================================================================
    // Multi-step scenarios
    // ======================================================================================

    /**
     * Plain startup + shutdown:
     * IDLE → STARTING → STARTED → IDLE
     * <p>
     * Proves the lifecycle wiring across states: initialize() drives hubConnector.initialize(),
     * onConnected() flips us to STARTED, and terminate() flows through the global STOP handler
     * to drive hubConnector.terminate() and reset to IDLE.
     */
    @Test
    public void scenario_startupAndShutdown() {
        node.initialize();
        assertEquals(SCNode.State.STARTING, node.getState());

        node.onConnected();
        assertEquals(SCNode.State.STARTED, node.getState());

        node.terminate();
        assertEquals(SCNode.State.STOPPING, node.getState());

        node.onConnectorIdle();
        assertEquals(SCNode.State.IDLE, node.getState());

        InOrder order = inOrder(hubConnector);
        order.verify(hubConnector).initialize();
        order.verify(hubConnector).terminate();
        verify(network, never()).setVmac(any(OctetString.class));
    }

    /**
     * VMAC-collision happy path (peer reports duplicate VMAC during connect):
     * STARTING → NEW_MAC_STOPPING → STARTING (with new VMAC)
     * <p>
     * Proves the full collision recovery:
     * - The disconnect-wait timer is armed during NEW_MAC_STOPPING.
     * - When the hub connector idles gracefully (CONNECTOR_IDLE), the timer is canceled,
     * a fresh VMAC is set on the network, and the connector is reinitialized.
     * - hardTerminate() is NOT called because the connector closed on its own.
     */
    @Test
    public void scenario_vmacCollisionWithGracefulIdle() {
        node.initialize();
        assertEquals(SCNode.State.STARTING, node.getState());

        // Peer says "duplicate VMAC" → restart requested.
        node.restartWithNewVMAC();
        assertEquals(SCNode.State.NEW_MAC_STOPPING, node.getState());
        ScheduledTask timer = lastTask();
        assertScheduledFor(timer, DISCONNECT_WAIT_SECS);

        // Connector finishes its shutdown.
        node.onConnectorIdle();

        assertEquals(SCNode.State.STARTING, node.getState());
        assertCanceled(timer);

        // Verify the spec-required side effects, in order: connector terminated (during
        // NEW_MAC), network VMAC replaced, connector reinitialized for the next attempt.
        InOrder order = inOrder(hubConnector, network);
        order.verify(hubConnector).initialize();   // initial start
        order.verify(hubConnector).terminate();    // triggered by NEW_MAC
        order.verify(network).setVmac(any(OctetString.class));
        order.verify(hubConnector).initialize();   // restart with new VMAC

        verify(hubConnector, never()).hardTerminate();
    }

    /**
     * VMAC-collision fallback (connector does not gracefully idle in time):
     * STARTING → NEW_MAC_STOPPING → (TIMEOUT) → STARTING (with new VMAC after hardTerminate)
     * <p>
     * Proves the timeout-based fallback: the disconnect-wait timer expires, the node calls
     * hardTerminate() on the connector to force it idle, then proceeds with VMAC rotation
     * exactly as in the graceful case.
     */
    @Test
    public void scenario_vmacCollisionWithTimeoutFallback() {
        node.initialize();
        node.restartWithNewVMAC();
        ScheduledTask timer = lastTask();
        assertEquals(SCNode.State.NEW_MAC_STOPPING, node.getState());

        // No CONNECTOR_IDLE callback ever arrives — the disconnect-wait fires.
        timer.runnable.run();

        assertEquals(SCNode.State.STARTING, node.getState());
        // Forced shutdown happened only because of the timeout, not via the graceful path.
        verify(hubConnector).hardTerminate();
        verify(network).setVmac(any(OctetString.class));
        verify(hubConnector, times(2)).initialize();
    }

    /**
     * VMAC-collision after the node has reached STARTED (regression scenario for the I4 fix):
     * STARTING → STARTED → (peer NAKs reconnect Connect-Request) → NEW_MAC_STOPPING
     * → STARTING → STARTED (with new VMAC)
     * <p>
     * Before the I4 fix, NEW_MAC arriving in STARTED hit illegalState and the VMAC was never
     * rotated. This scenario pins the corrected behavior end-to-end: the rotation works
     * identically whether the duplicate-VMAC NAK arrives on the initial Connect-Request or
     * on a reconnect after the node was already fully started.
     */
    @Test
    public void scenario_vmacCollisionAfterStartedRotatesAndRecovers() {
        node.initialize();
        node.onConnected();
        assertEquals(SCNode.State.STARTED, node.getState());

        // Duplicate-VMAC NAK arrives (delivered by SCConnection during a reconnect cycle).
        node.restartWithNewVMAC();
        assertEquals(SCNode.State.NEW_MAC_STOPPING, node.getState());
        ScheduledTask timer = lastTask();
        assertScheduledFor(timer, DISCONNECT_WAIT_SECS);

        // Hub connector drains gracefully.
        node.onConnectorIdle();
        assertEquals(SCNode.State.STARTING, node.getState());
        assertCanceled(timer);

        // Reconnection completes with the rotated VMAC.
        node.onConnected();
        assertEquals(SCNode.State.STARTED, node.getState());

        // Order of side effects across the recovery.
        InOrder order = inOrder(hubConnector, network);
        order.verify(hubConnector).initialize();   // initial start
        order.verify(hubConnector).terminate();    // triggered by NEW_MAC from STARTED
        order.verify(network).setVmac(any(OctetString.class));
        order.verify(hubConnector).initialize();   // restart with new VMAC

        verify(hubConnector, never()).hardTerminate();
    }
}
