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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.sc.msg.SCBVLC;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadAdvertisement;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.SCHubConnection;
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;

public class SCHubConnector {
    private static final Logger LOG = LoggerFactory.getLogger(SCHubConnector.class);


    protected enum State {
        IDLE,
        TRY_PRIMARY,
        WAIT_PRIMARY,
        CONNECTED_PRIMARY,
        TRY_FAILOVER,
        WAIT_FAILOVER,
        CONNECTED_FAILOVER,
        REWAIT_PRIMARY,
        DELAY,
        DELAYING,
        STOPPING
    }


    protected enum Event {
        START,
        STOP,
        CHANGE, // State was changed by self and needs to be handled.
        CONNECTION_ESTABLISHED, // Connection was fully established
        CONNECTION_CLOSED, // Established connection was closed.
        CONNECTION_IDLE, // Connection was not established and has returned to idle.
        TIMEOUT;

        boolean isOneOf(Event... events) {
            return Arrays.stream(events).anyMatch(event -> event == this);
        }
    }


    private final SCNode node;
    private final SCNetwork network;
    private final BackoffPolicy backoff;

    private LocalDevice localDevice;

    private State state = State.IDLE;
    // We need both connections because while we are connected to the failover we are
    // still trying to connect to the primary.
    private SCConnection primaryConnection;
    private SCConnection failoverConnection;
    private ScheduledFuture<Void> timeoutFuture;

    public SCHubConnector(SCNode node, SCNetwork network) {
        this.node = node;
        this.network = network;
        this.backoff = network.getBackoffPolicy();
    }

    public void configure(Transport transport) {
        localDevice = transport.getLocalDevice();
        if (network.getPrimaryHub() != null) {
            primaryConnection = createConnection("primary", network, network.getPrimaryHub());
            primaryConnection.configure(transport);
        }
        if (network.getFailoverHub() != null) {
            failoverConnection = createConnection("failover", network, network.getFailoverHub());
            failoverConnection.configure(transport);
        }
    }

    protected SCConnection createConnection(String name, SCNetwork network, java.net.URI uri) {
        return new SCConnection(this, name, network, uri);
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

    public synchronized void hardTerminate() {
        cancelTimeout();
        if (primaryConnection != null) {
            primaryConnection.hardTerminate();
        }
        if (failoverConnection != null) {
            failoverConnection.hardTerminate();
        }
        state = State.IDLE;
    }

    public SCHubConnectorState getHubConnectorState() {
        return switch (state) {
            case CONNECTED_PRIMARY -> SCHubConnectorState.connectedToPrimary;
            case CONNECTED_FAILOVER, REWAIT_PRIMARY -> SCHubConnectorState.connectedToFailover;
            default -> SCHubConnectorState.noHubConnection;
        };
    }

    public SCHubConnection getPrimaryHubConnectionStatus() {
        if (primaryConnection == null) {
            return new SCHubConnection(SCConnectionState.notConnected, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED);
        }
        return primaryConnection.getConnectionStatus();
    }

    public SCHubConnection getFailoverHubConnectionStatus() {
        if (failoverConnection == null) {
            return new SCHubConnection(SCConnectionState.notConnected, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED);
        }
        return failoverConnection.getConnectionStatus();
    }

    public void sendMessage(SCBVLC message) {
        if (state == State.CONNECTED_PRIMARY) {
            primaryConnection.sendMessage(message);
        } else if (state == State.CONNECTED_FAILOVER || state == State.REWAIT_PRIMARY) {
            failoverConnection.sendMessage(message);
        } // Otherwise drop the message.
    }

    private void queueEvent(Event event, Object... args) {
        localDevice.execute(() -> handleEvent(event, args));
    }

    protected synchronized void handleEvent(Event event, Object... args) {
        LOG.debug("handleEvent start: {}, event={}, args={}", state, event, args);

        if (event == Event.STOP) {
            state = State.STOPPING;
            if (primaryConnection != null) {
                primaryConnection.terminate();
            }
            if (failoverConnection != null) {
                failoverConnection.terminate();
            }
            LOG.debug("handleEvent end: {}", state);
            return;
        }

        switch (state) {
            case IDLE -> {
                if (event == Event.START) {
                    raiseChange(State.TRY_PRIMARY);
                } else if (!event.isOneOf(Event.CONNECTION_CLOSED, Event.CONNECTION_IDLE, Event.STOP)) {
                    illegalState(event, args);
                }
            }
            case TRY_PRIMARY -> {
                if (event == Event.CHANGE) {
                    if (primaryConnection == null) {
                        LOG.debug("No primary URI configured");
                        raiseChange(State.TRY_FAILOVER);
                    } else {
                        LOG.debug("Attempting to connect to primary hub");
                        primaryConnection.initialize();
                        state = State.WAIT_PRIMARY;
                    }
                } else {
                    illegalState(event, args);
                }
            }
            case WAIT_PRIMARY -> {
                if (event.isOneOf(Event.CONNECTION_CLOSED, Event.CONNECTION_IDLE)) {
                    raiseChange(State.TRY_FAILOVER);
                } else if (event == Event.CONNECTION_ESTABLISHED) {
                    backoff.reset();
                    state = State.CONNECTED_PRIMARY;
                    node.onConnected();
                } else {
                    illegalState(event, args);
                }
            }
            case CONNECTED_PRIMARY -> {
                if (event == Event.CONNECTION_CLOSED) {
                    // Check what connection closed. It could just be the failover connection closing after the
                    // primary connection was established.
                    if (args[0] == primaryConnection) {
                        node.onDisconnected();
                        raiseChange(State.TRY_PRIMARY);
                    }
                } else {
                    illegalState(event, args);
                }
            }
            case TRY_FAILOVER -> {
                if (event == Event.CHANGE) {
                    if (failoverConnection == null) {
                        LOG.debug("No failover URI configured");
                        raiseChange(State.DELAY);
                    } else {
                        LOG.debug("Attempting to connect to failover hub");
                        failoverConnection.initialize();
                        state = State.WAIT_FAILOVER;
                    }
                } else {
                    illegalState(event, args);
                }
            }
            case WAIT_FAILOVER -> {
                if (event.isOneOf(Event.CONNECTION_CLOSED, Event.CONNECTION_IDLE)) {
                    raiseChange(State.DELAY);
                } else if (event == Event.CONNECTION_ESTABLISHED) {
                    backoff.reset();
                    state = State.CONNECTED_FAILOVER;
                    node.onConnected();
                    if (primaryConnection != null) {
                        // Set a timeout for retrying the primary.
                        setTimeoutFuture(backoff.getReconnectWaitTimeout());
                    }
                } else {
                    illegalState(event, args);
                }
            }
            case CONNECTED_FAILOVER -> {
                if (event == Event.CONNECTION_CLOSED) {
                    node.onDisconnected();
                    raiseChange(State.TRY_PRIMARY);
                } else if (event == Event.TIMEOUT) {
                    state = State.REWAIT_PRIMARY;
                    primaryConnection.initialize();
                } else {
                    illegalState(event, args);
                }
            }
            case REWAIT_PRIMARY -> {
                // REWAIT_PRIMARY is a substate of CONNECTED_FAILOVER, so we need to check for the same things.
                if (event.isOneOf(Event.CONNECTION_CLOSED, Event.CONNECTION_IDLE)) {
                    if (failoverConnection == args[0]) {
                        node.onDisconnected();
                        raiseChange(State.TRY_PRIMARY);
                    } else if (primaryConnection == args[0]) {
                        primaryConnection.hardTerminate();
                        state = State.CONNECTED_FAILOVER;
                        // This is a return to the parent state, so no connected notification is needed.
                        setTimeoutFuture(backoff.getReconnectWaitTimeout());
                    } // Otherwise ignore.
                } else if (event == Event.CONNECTION_ESTABLISHED) {
                    backoff.reset();
                    failoverConnection.terminate();
                    state = State.CONNECTED_PRIMARY;
                } else {
                    illegalState(event, args);
                }
            }
            case DELAY -> {
                if (event == Event.CHANGE) {
                    if (primaryConnection != null) {
                        primaryConnection.hardTerminate();
                    }
                    if (failoverConnection != null) {
                        failoverConnection.hardTerminate();
                    }
                    state = State.DELAYING;
                    setTimeoutFuture(backoff.getReconnectWaitTimeout());
                } else {
                    illegalState(event, args);
                }
            }
            case DELAYING -> {
                if (event == Event.TIMEOUT) {
                    raiseChange(State.TRY_PRIMARY);
                } else {
                    illegalState(event, args);
                }
            }
            case STOPPING -> {
                if (event.isOneOf(Event.CONNECTION_CLOSED, Event.CONNECTION_IDLE)) {
                    if ((primaryConnection == null || primaryConnection.getState() == SCConnection.State.IDLE)
                            && (failoverConnection == null || failoverConnection.getState() == SCConnection.State.IDLE)) {
                        state = State.IDLE;
                        node.onConnectorIdle();
                    }
                } else {
                    illegalState(event, args);
                }
            }
        }

        LOG.debug("handleEvent end: {}", state);
    }

    private void raiseChange(State newState) {
        state = newState;
        queueEvent(Event.CHANGE);
    }

    public void onIncoming(SCBVLC message) {
        node.onIncoming(message);
    }

    public void onConnectionEstablished(SCConnection connection) {
        if (connection != primaryConnection && connection != failoverConnection) {
            LOG.warn("onConnectionEstablished received from unknown (zombie?) connection: {}", connection);
        } else {
            queueEvent(Event.CONNECTION_ESTABLISHED, connection);
        }
    }

    public void onConnectionIdle(SCConnection connection, boolean wasEstablished) {
        if (connection != primaryConnection && connection != failoverConnection) {
            LOG.warn("onConnectionIdle received from unknown (zombie?) connection: {}", connection);
        } else {
            queueEvent(wasEstablished ? Event.CONNECTION_CLOSED : Event.CONNECTION_IDLE, connection);
        }
    }

    void restartWithNewVMAC() {
        node.restartWithNewVMAC();
    }

    private void illegalState(Event event, Object[] args) {
        LOG.error("Illegal event '{}' for state '{}', args={}", event, state, args);
    }

    private synchronized void setTimeoutFuture(int seconds) {
        LOG.debug("setTimeoutFuture with {} seconds", seconds);
        cancelTimeout();
        timeoutFuture = localDevice.schedule(() -> handleEvent(Event.TIMEOUT), seconds, TimeUnit.SECONDS);
    }

    private synchronized void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    public int getStateAsInt() {
        return switch (state) {
            case CONNECTED_PRIMARY -> SCPayloadAdvertisement.CONN_STAT_PRIMARY;
            case CONNECTED_FAILOVER, REWAIT_PRIMARY -> SCPayloadAdvertisement.CONN_STAT_FAILOVER;
            default -> SCPayloadAdvertisement.CONN_STAT_NONE;
        };
    }
}
