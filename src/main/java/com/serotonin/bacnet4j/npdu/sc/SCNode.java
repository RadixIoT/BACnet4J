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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.sc.msg.SCBVLC;
import com.serotonin.bacnet4j.npdu.sc.msg.SCOption;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadAdvertisement;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadBVLCResult;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.SCHubConnection;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;
import com.serotonin.bacnet4j.type.primitive.OctetString;

/**
 * SCNode holds and manages the required hub connector. This state machine is responsible for starting up the node.
 * It can also shut everything down and restart in the case that its VMAC needs to change. Like other state machines,
 * calls like initialize() and terminate() actually fire events themselves that the state machine processes from its one
 * entry point. This keeps "side effects" out of the state machine.
 */
public class SCNode {
    private static final Logger LOG = LoggerFactory.getLogger(SCNode.class);


    protected enum State {
        IDLE,
        STARTING,
        STARTED,
        NEW_MAC_STOPPING,
        STOPPING,
    }


    protected enum Event {
        START,
        STOP,
        CONNECTED,
        NEW_MAC,
        TIMEOUT,
        DISCONNECTED,
        CONNECTOR_IDLE;

        boolean isOneOf(Event... events) {
            return Arrays.stream(events).anyMatch(event -> event == this);
        }
    }


    private final SCNetwork network;
    private SCHubConnector hubConnector;

    private LocalDevice localDevice;

    private State state = State.IDLE;
    private ScheduledFuture<Void> timeoutFuture;
    private final CountDownLatch terminationLatch = new CountDownLatch(1);

    public SCNode(SCNetwork network) {
        this.network = network;
    }

    public void configure(Transport transport) {
        localDevice = transport.getLocalDevice();
        hubConnector = createHubConnector(network);
        hubConnector.configure(transport);
    }

    protected SCHubConnector createHubConnector(SCNetwork network) {
        return new SCHubConnector(this, network);
    }

    protected State getState() {
        return state;
    }

    public void initialize() {
        queueEvent(Event.START);
    }

    public void terminate() {
        queueEvent(Event.STOP);
    }

    /**
     * Immediately forces the node and everything below it to idle. Deliberately bypasses the serial event
     * queue: this is the escalation used when orderly shutdown has failed, so it must not depend on the
     * queue (or its delegate executor, which may be about to shut down) still draining. It is therefore the
     * one entry point that can run out of order with queued events.
     */
    public void hardTerminate() {
        hubConnector.hardTerminate();
        state = State.IDLE;
        terminationLatch.countDown();
    }

    /**
     * Waits for this node to complete the shutdown started by {@link #terminate()}. The shutdown is
     * asynchronous: the connections perform their disconnect handshakes, and their events dispatch through
     * the local device's executor, which must still be running while waiting.
     *
     * @return true if the node reached the idle state within the timeout.
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return terminationLatch.await(timeout, unit);
    }

    public SCHubConnectorState getHubConnectorState() {
        return hubConnector.getHubConnectorState();
    }

    public SCHubConnection getPrimaryHubConnectionStatus() {
        return hubConnector.getPrimaryHubConnectionStatus();
    }

    public SCHubConnection getFailoverHubConnectionStatus() {
        return hubConnector.getFailoverHubConnectionStatus();
    }

    private void queueEvent(Event event) {
        network.executeSerially(() -> handleEvent(event));
    }

    protected void handleEvent(Event event) {
        LOG.debug("manageState start: {}, event={}", state, event);

        if (event == Event.STOP) {
            if (state == State.IDLE) {
                // Nothing to shut down; the node is already terminated.
                terminationLatch.countDown();
            } else {
                hubConnector.terminate();
                state = State.STOPPING;
            }
            return;
        }

        switch (state) {
            case IDLE -> {
                if (event == Event.START) {
                    starting();
                } else {
                    illegalState(event);
                }
            }
            case STARTING -> {
                if (event == Event.CONNECTED) {
                    state = State.STARTED;
                } else if (event == Event.NEW_MAC) {
                    newMac();
                } else {
                    illegalState(event);
                }
            }
            case NEW_MAC_STOPPING -> {
                if (event.isOneOf(Event.TIMEOUT, Event.DISCONNECTED, Event.CONNECTOR_IDLE)) {
                    cancelTimeout();
                    if (event == Event.TIMEOUT) {
                        // Didn't get a disconnect, so make sure it is closed.
                        hubConnector.hardTerminate();
                    }
                    network.setVmac(new OctetString(SCVmac.makeRandom().getBytes()));
                    starting();
                } else {
                    illegalState(event);
                }
            }
            case STARTED -> {
                // A disconnected hub connector should restart itself.
                if (event == Event.NEW_MAC) {
                    newMac();
                } else if (!event.isOneOf(Event.DISCONNECTED, Event.CONNECTED)) {
                    illegalState(event);
                }
            }
            case STOPPING -> {
                if (event == Event.CONNECTOR_IDLE) {
                    state = State.IDLE;
                    terminationLatch.countDown();
                } else {
                    illegalState(event);
                }
            }
        }

        LOG.debug("manageState end: {}", state);
    }

    private void newMac() {
        state = State.NEW_MAC_STOPPING;
        hubConnector.terminate();
        setTimeoutFuture(network.getDisconnectWaitTimeout().intValue());
    }

    private void starting() {
        LOG.debug("Node starting with vmac={}, uuid={}", network.getVmac(), network.getDeviceUUID());
        state = State.STARTING;
        hubConnector.initialize();
    }

    private void illegalState(Event event) {
        LOG.error("Illegal event '{}' for state '{}'", event, state);
    }

    private synchronized void setTimeoutFuture(int seconds) {
        cancelTimeout();
        // The timeout fires on a scheduler thread, but the event is processed through the network's serial
        // queue like every other event so that it cannot overtake events submitted before it. No stale-timeout
        // guard is needed: TIMEOUT is only legal in NEW_MAC_STOPPING, and every path that cancels this timeout
        // also leaves that state, so a stale event is rejected by the state machine as illegal.
        timeoutFuture = localDevice.schedule(() -> queueEvent(Event.TIMEOUT), seconds, TimeUnit.SECONDS);
    }

    private synchronized void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    public void onIncoming(SCBVLC message) {
        LOG.debug("onIncoming: function={}, data={}", message.getFunction(), message);

        // This is the only place that destination options are checked because AB.3.1.4 says
        // that the "destination BACnet/SC node shall process header options".
        // Note that this only applies to "destination options" - the "data options" are NOT processed by the node/datalink
        if (message.getDestOptions() != null) {
            for (SCOption option : message.getDestOptions()) {
                // We don't understand *any* must-understands, so everything is an error
                if (option.isMustUnderstand()) {
                    // if this was unicast, then send a NAK, otherwise, just return
                    LOG.error("Rejecting must-understand destination option {}", option);
                    if (message.isUnicastRequest()) {
                        sendErrorResponse(message, option.getMarker(), ErrorClass.communication,
                                ErrorCode.headerNotUnderstood, "'Must Understand' option not understood");
                    }
                    return;
                }
            }
        }

        // These are messages that are handled "by the node", i.e., not by the connections or the hub connector
        switch (message.getFunction()) {
            case SCBVLC.ADVERTISEMENT_SOLICITATION -> {
                LOG.info("Node Responding to Advertisement Solicitation");
                sendAdvertisement(message.getOriginating(), hubConnector.getStateAsInt(), false,
                        network.getMaxBvlcLengthAccepted().intValue(), network.getMaxNpduLengthAccepted().intValue());
            }
            case SCBVLC.ENCAPSULATED_NPDU -> network.onIncoming(message);
            default -> LOG.warn("Node received unexpected message function {}", message);
        }
    }

    void onConnected() {
        queueEvent(Event.CONNECTED);
    }

    void onDisconnected() {
        queueEvent(Event.DISCONNECTED);
    }

    public void onConnectorIdle() {
        queueEvent(Event.CONNECTOR_IDLE);
    }

    void restartWithNewVMAC() {
        // This happens when the current VMAC is detected as being a duplicate. See AB.6.2.2.
        queueEvent(Event.NEW_MAC);
    }

    public void sendMessage(SCBVLC message) {
        hubConnector.sendMessage(message);
    }

    public void sendAdvertisement(SCVmac destination, int connectionStatus, boolean acceptConnections,
            int maximumBVLCLength, int maximumNPDULength) {
        var ad = new SCPayloadAdvertisement(connectionStatus, acceptConnections, maximumBVLCLength, maximumNPDULength);
        sendMessage(new SCBVLC(null, destination, SCBVLC.ADVERTISEMENT, ad.write()));
    }

    public void sendErrorResponse(SCBVLC message, int headerMarker, ErrorClass errorClass, ErrorCode errorCode,
            String errorDetails) {
        sendError(message.getOriginating(), message.getFunction(), headerMarker, errorClass,
                errorCode, errorDetails, message.getId());
    }

    public void sendErrorResponse(SCBVLC message, ErrorClass errorClass, ErrorCode errorCode, String errorDetails) {
        sendError(message.getOriginating(), message.getFunction(), 0, errorClass, errorCode,
                errorDetails, message.getId());
    }

    public void sendError(SCVmac destination, int forFunction, int headerMarker, ErrorClass errorClass,
            ErrorCode errorCode, String errorDetails, int id) {
        var result = new SCPayloadBVLCResult(forFunction, headerMarker, errorClass, errorCode, errorDetails);
        sendMessage(new SCBVLC(null, destination, SCBVLC.BVLC_RESULT, result.write(), id));
    }
}
