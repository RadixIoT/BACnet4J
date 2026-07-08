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

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.CloseFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.sc.msg.SCBVLC;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayload;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadAdvertisement;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadBVLCResult;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadConnectAccept;
import com.serotonin.bacnet4j.npdu.sc.msg.SCPayloadConnectRequest;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.SCHubConnection;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class SCConnection {
    private static final Logger LOG = LoggerFactory.getLogger(SCConnection.class);


    protected enum State {
        IDLE,
        AWAITING_WEBSOCKET,
        AWAITING_ACCEPT,
        CONNECTED,
        DISCONNECTING;

        boolean isOneOf(State... states) {
            return Arrays.stream(states).anyMatch(s -> s == this);
        }
    }


    protected enum Event {
        INITIATE,
        ACCEPT,
        DISCONNECT,
        TIMEOUT,
        CONNECT_ERROR,
        MESSAGE,
        REMOTE_CLOSE,
        TEXT_DATA;

        boolean isOneOf(Event... events) {
            return Arrays.stream(events).anyMatch(e -> e == this);
        }
    }


    private final SCNetwork network;
    private final SCHubConnector owner;
    private final String name;
    private final URI uri;

    private LocalDevice localDevice;

    private State state = State.IDLE;
    private ScWebSocketClient client;
    private ScheduledFuture<Void> timeoutFuture;
    private ScheduledFuture<Void> heartbeatFuture;
    private int nextMessageId = 0; // Don't use this directly. Use getMessageId().

    // Reportable connection state
    private SCConnectionState connectionState = SCConnectionState.notConnected;
    private DateTime connectTimestamp = DateTime.UNSPECIFIED;
    private DateTime disconnectTimestamp = DateTime.UNSPECIFIED;
    private ErrorClassAndCode connectionError = null;
    private String connectionErrorDetails = null;

    private int expectedConnectAcceptMessageId;
    private int expectedHeartbeatMessageId;
    private int expectedDisconnectMessageId;

    private int peerMaxBvlcLength;
    private int peerMaxNpduLength;

    public SCConnection(SCHubConnector owner, String name, SCNetwork network, URI uri) {
        this.network = network;
        this.owner = owner;
        this.name = name; // For identification in logs
        this.uri = uri;
    }

    public void configure(Transport transport) {
        localDevice = transport.getLocalDevice();
        client = createClient();
    }

    protected ScWebSocketClient createClient() {
        var c = new ScWebSocketClient(name, uri, this);
        c.setSocketFactory(getSSLSocketFactory());
        // BACnet/SC uses BVLC HEARTBEAT messages for peer liveness (Annex AB Clause 6); the spec
        // does not require WebSocket Pongs, and many SC peers won't send them. Leaving the
        // library's default ping/pong watchdog on causes spurious ABNORMAL_CLOSE teardowns.
        c.setConnectionLostTimeout(0);
        return c;
    }

    protected synchronized State getState() {
        return state;
    }

    public void initialize() {
        handleEvent(Event.INITIATE);
    }

    public void terminate() {
        handleEvent(Event.DISCONNECT);
    }

    public synchronized void hardTerminate() {
        cancelTimeout();
        cancelHeartbeat();
        if (!client.isClosed()) {
            // Terminate with extreme prejudice.
            LOG.debug("{} hardTerminate", name);
            client.closeConnection(CloseFrame.NOCODE, "Hard termination");
            connectionState = SCConnectionState.notConnected;
            disconnectTimestamp = new DateTime(localDevice);
        }
        state = State.IDLE;
    }

    public SCHubConnection getConnectionStatus() {
        return new SCHubConnection(connectionState, connectTimestamp, disconnectTimestamp, connectionError,
                connectionErrorDetails == null ? null : new CharacterString(connectionErrorDetails));
    }

    public void sendMessage(SCBVLC message) {
        if (message.needsId()) {
            message.setId(getNextMessageId());
        }

        // Length issues shouldn't happen because segmentation should be controlled at the APDU level, so these checks
        // are hopefully redundant. Note that nothing in the spec says to discard these messages at this point. Only
        // that if a violating message is *received* that it be dropped (AB.7.5.3).
        if (message.getPayload() != null && message.getPayload().length > peerMaxNpduLength) {
            LOG.warn("Length of NPDU to send ({}) exceeds maximum NPDU length of peer ({})",
                    message.getPayload().length, peerMaxNpduLength);
        }

        var bytes = message.write();
        if (bytes.length > peerMaxBvlcLength) {
            LOG.warn("Length of BVLC to send ({}) exceeds maximum BVLC length of peer ({})",
                    bytes.length, peerMaxBvlcLength);
        }

        write(bytes);
    }

    private SSLSocketFactory getSSLSocketFactory() {
        SSLParameters params = new SSLParameters();
        params.setEndpointIdentificationAlgorithm(null);
        return new ConfiguredSSLSocketFactory(network.getSslContext().getSocketFactory(), params);
    }

    private void handleEvent(Event event, Object... args) {
        localDevice.execute(() -> handleEventImpl(event, args));
    }

    protected synchronized void handleEventImpl(Event event, Object... args) {
        LOG.debug("{} handleEvent start: {}, event={}, args={}", name, state, event, args);

        SCBVLC message = null;
        SCPayload payload = null;
        if (event == Event.MESSAGE) {
            message = parseMessage((ByteQueue) args[0]); // will send NAKs for badly formatted messages
            if (message != null) {
                // Centralize the parsing of payload here so that parsing issues can be handled.
                try {
                    payload = parsePayload(message);
                } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                    // AB.3.1.5 "If a BVLC message is received that is truncated..."
                    protocolViolationLogAndSend(message, ErrorCode.messageIncomplete,
                            "Not enough data in message - length wrong?");
                    return;
                }
            }
        }

        // Manage the state machine.
        switch (state) {
            case IDLE -> {
                if (event == Event.INITIATE) { // AB.6.2.2 "Initiating a WebSocket" transition
                    // Ensure that the URI uses the "wss" protocol.
                    if (!"wss".equals(uri.getScheme())) {
                        connectionState = SCConnectionState.failedToConnect;
                        connectionError =
                                new ErrorClassAndCode(ErrorClass.communication, ErrorCode.websocketSchemeNotSupported);
                        connectionClosed(false);
                    } else {
                        // Async calls to connect. The client will report what happened via callbacks.
                        if (client.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
                            client.connect();
                        } else {
                            client.reconnect();
                        }
                        state = State.AWAITING_WEBSOCKET;
                        setTimeoutFuture(network.getConnectWaitTimeout().intValue(), state);
                    }
                } else if (event == Event.REMOTE_CLOSE) {
                    LOG.debug("{} remote close event while idle.", name);
                    // Ignore. Just the remote closing its side after a normal disconnect.
                } else if (event == Event.DISCONNECT) {
                    owner.onConnectionIdle(this, false);
                } else {
                    illegalState(event, args);
                }
            }
            case AWAITING_WEBSOCKET -> {
                if (event.isOneOf(Event.TIMEOUT, Event.CONNECT_ERROR, Event.DISCONNECT, Event.REMOTE_CLOSE,
                        Event.TEXT_DATA) || !client.isOpen()) {
                    if (event == Event.TEXT_DATA) {
                        client.close(CloseFrame.REFUSE);
                    } else {
                        client.close();
                    }
                    connectionState = SCConnectionState.failedToConnect;
                    connectionClosed(false);
                } else if (event == Event.ACCEPT) {
                    LOG.debug("{} websocket connected", name);
                    // check AB.6.2.2 "WebSocket established" transition
                    sendConnectRequest();
                    state = State.AWAITING_ACCEPT;
                    setTimeoutFuture(network.getConnectWaitTimeout().intValue(), state);
                } else {
                    illegalState(event, args);
                }
            }
            case AWAITING_ACCEPT -> {
                // check AB-11 "Connection Wait Timeout Expired" transition
                if (event.isOneOf(Event.TIMEOUT, Event.DISCONNECT, Event.REMOTE_CLOSE, Event.TEXT_DATA)
                        || !client.isOpen()) {
                    LOG.debug("{} websocket connection closing: event={}, client state={}", name, event,
                            client.getReadyState());
                    if (event == Event.TEXT_DATA) {
                        client.close(CloseFrame.REFUSE);
                    } else {
                        client.close();
                    }
                    connectionState = SCConnectionState.failedToConnect;
                    connectionClosed(false);
                } else if (event == Event.MESSAGE && message != null) {
                    switch (message.getFunction()) {
                        case SCBVLC.CONNECT_ACCEPT -> {
                            if (checkId(message)) {
                                var accept = (SCPayloadConnectAccept) Objects.requireNonNull(payload);
                                LOG.info("{} connected to peer: accept={}", name, accept);
                                state = State.CONNECTED;
                                connectionState = SCConnectionState.connected;
                                connectTimestamp = new DateTime(localDevice);
                                connectionError = null;
                                connectionErrorDetails = null;
                                peerMaxBvlcLength = accept.maximumBVLCLength;
                                peerMaxNpduLength = accept.maximumNPDULength;
                                cancelTimeout();
                                resetHeartbeatFuture();
                                // Notify the connector
                                owner.onConnectionEstablished(this);
                            }
                        }
                        case SCBVLC.BVLC_RESULT -> {
                            if (checkId(message)) {
                                var result = (SCPayloadBVLCResult) Objects.requireNonNull(payload);
                                if (ErrorCode.nodeDuplicateVmac.equals(result.getErrorCode())) {
                                    LOG.info("{} BVLC-Result NAK, VMAC collision, generating a new VMAC", name);
                                    client.close(CloseFrame.NORMAL, "Changing VMAC. Be back soon...");
                                    state = State.IDLE;
                                    cancelTimeout();
                                    // This will cause a restart of the node.
                                    owner.restartWithNewVMAC();
                                } else if (result.isNak()) {
                                    LOG.info(
                                            "{} BVLC-Result NAK non-VMAC-collision, errorHeaderMarker={}, resultCode={}, errorClass={}, errorCode={}",
                                            name, result.getErrorHeaderMarker(), result.getResultCode(),
                                            result.getErrorCode(), result.getErrorClass());
                                    connectionState = SCConnectionState.failedToConnect;
                                    connectionError =
                                            new ErrorClassAndCode(result.getErrorClass(), result.getErrorCode());
                                    connectionErrorDetails = result.getErrorDetails();
                                    client.close(CloseFrame.NORMAL);
                                    connectionClosed(false);
                                } else {
                                    LOG.error(
                                            "{} protocol violation: Unexpected BVLC result: class={}, code={}, message={}",
                                            name, result.getErrorClass(), result.getErrorCode(),
                                            result.getErrorDetails());
                                }
                            }
                        }
                        case SCBVLC.DISCONNECT_REQUEST -> {
                            // How rude! we haven't even connected yet
                            cancelTimeout();
                            client.close(CloseFrame.NORMAL, "Disconnect-Request received");
                            connectionClosed(false);
                        }
                        default ->
                            // transition "BVLC message received other than a Disconnect-Request, a Disconnect-ACK, or a response to the Connect-Request initiated"
                                LOG.error(
                                        "{} protocol violation: Unexpected message {} received in WAITING_ACCEPT state",
                                        name, message.getFunction());
                    }
                } else {
                    illegalState(event, args);
                }
            }
            case CONNECTED -> {
                if (event == Event.DISCONNECT) {
                    sendDisconnectRequest();
                    state = State.DISCONNECTING;
                    setTimeoutFuture(network.getDisconnectWaitTimeout().intValue(), state);
                    cancelHeartbeat();
                } else if (event == Event.REMOTE_CLOSE) {
                    connectionClosed(true);
                } else if (event == Event.TEXT_DATA) {
                    client.close(CloseFrame.REFUSE);
                    connectionClosed(true);
                } else if (event == Event.MESSAGE && message != null) {
                    resetHeartbeatFuture();
                    switch (message.getFunction()) {
                        case SCBVLC.DISCONNECT_REQUEST -> { // "Disconnect-Request" received transition
                            sendDisconnectAck(message.getId());
                            client.close(CloseFrame.NORMAL, "Disconnect-Request received");
                            connectionClosed(true);
                        }
                        case SCBVLC.ENCAPSULATED_NPDU, SCBVLC.ADVERTISEMENT_SOLICITATION -> owner.onIncoming(message);
                        case SCBVLC.HEARTBEAT_REQUEST -> sendHeartbeatAck(message.getId());
                        case SCBVLC.HEARTBEAT_ACK -> {
                            // Just check that the message id is correct.
                            if (message.getId() != expectedHeartbeatMessageId) {
                                LOG.warn("{} protocol violation: Heartbeat-ACK ID does not match Heartbeat-Request",
                                        name);
                            } else {
                                // Got the heartbeat ack, cancel the timeout.
                                cancelTimeout();
                            }
                        }
                        case SCBVLC.ADDRESS_RESOLUTION ->
                                sendAddressResolutionNak(message.getId(), message.getOriginating());
                        case SCBVLC.ADVERTISEMENT -> {
                            var advertisement = (SCPayloadAdvertisement) Objects.requireNonNull(payload);
                            LOG.info("{} ADVERTISEMENT: {}, originating={}", name, advertisement,
                                    message.getOriginating());
                            // "Per AB.3.2, the node 'shall update its status information of the sending node.' This
                            // client has no use for peer status (hub-only, no direct connections, no peer diagnostics
                            // API). If/when direct-connect initiation or a peer-status network-port surface is added,
                            // capture sender VMAC → {connectionStatus, acceptsConnections, maxBVLC, maxNPDU} here."
                        }
                        default -> LOG.warn(
                                "{} protocol violation: Unexpected function code {} received in CONNECTED state - ignoring",
                                name, message.getFunction());
                    }
                } else if (event == Event.TIMEOUT) {
                    // The heartbeat ACK was not received. Per AB.6.3 initiate local disconnection.
                    LOG.error("{} heartbeat ACK not received. Disconnecting", name);
                    handleEvent(Event.DISCONNECT);
                } else {
                    illegalState(event, args);
                }
            }
            case DISCONNECTING -> {
                // Do not restart the connection process.
                if (event.isOneOf(Event.TIMEOUT, Event.REMOTE_CLOSE, Event.TEXT_DATA)) {
                    LOG.warn("{} timeout or remote close while waiting for disconnect ACK", name);
                    if (event == Event.TEXT_DATA) {
                        client.close(CloseFrame.REFUSE);
                    } else {
                        client.close();
                    }
                    connectionClosed(true);
                } else if (event == Event.MESSAGE && message != null) {
                    if (message.getFunction() == SCBVLC.DISCONNECT_ACK) {  // "Disconnect-ACK received" transition
                        if (message.getId() != expectedDisconnectMessageId) {
                            LOG.warn("{} protocol violation: Disconnect-ACK ID does not match our Disconnect-Request",
                                    name);
                            client.close(CloseFrame.PROTOCOL_ERROR,
                                    "Disconnect-ACK ID did not match our Disconnect-Request");
                        } else {
                            client.close(CloseFrame.NORMAL, "Disconnect-ACK received");
                        }
                        connectionClosed(true);
                    } else if (message.getFunction() == SCBVLC.BVLC_RESULT) {
                        // To be faithful to the spec, we are supposed to check for a NAK and ignore ACKs, so parse payload
                        var result = (SCPayloadBVLCResult) Objects.requireNonNull(payload);
                        // check for transition "On receipt of a Result-NAK response to the Disconnect-Request"
                        if (result.getForFunction() == SCBVLC.DISCONNECT_REQUEST && result.isNak()) {
                            client.close(CloseFrame.NORMAL, "Disconnect-NAK received");
                            connectionClosed(true);
                        }
                    }
                    // All other messages are ignored.
                } else {
                    illegalState(event, args);
                }
            }
        }
        LOG.debug("{} handleEvent end: {}", name, state);
    }

    private boolean checkId(SCBVLC message) {
        if (message.getId() != expectedConnectAcceptMessageId) {
            LOG.error("{} protocol violation: Message ID in Connect-Accept {} does not match Connect-Request {}",
                    name, message.getId(), expectedConnectAcceptMessageId);
            client.close(CloseFrame.ABNORMAL_CLOSE, "Incorrect Connect-Accept message ID");
            connectionClosed(false);
            return false;
        }
        return true;
    }

    private void connectionClosed(boolean wasEstablished) {
        state = State.IDLE;
        cancelHeartbeat(); // May be redundant, but not always.
        cancelTimeout(); // May be redundant, but not always.
        if (wasEstablished) {
            connectionState = SCConnectionState.notConnected;
            disconnectTimestamp = new DateTime(localDevice);
        }
        // Notify the connector
        owner.onConnectionIdle(this, wasEstablished);
    }

    private void illegalState(Event event, Object[] args) {
        LOG.error("{} illegal event '{}' for state '{}', args={}", name, event, state, args);
    }

    protected void onWebsocketError(ErrorCode errorCode, String message) {
        // Don't set the disconnect time because we were never connected.
        connectionError = new ErrorClassAndCode(ErrorClass.communication, errorCode);
        connectionErrorDetails = message;

        handleEvent(Event.CONNECT_ERROR);
    }

    protected void onWebsocketOpen() {
        handleEvent(Event.ACCEPT);
    }

    protected void onTextData(String text) {
        LOG.error("{} text '{}' received from web socket at state '{}'", name, text, state);
        handleEvent(Event.TEXT_DATA);
    }

    protected void onWebsocketMessage(ByteQueue queue) {
        handleEvent(Event.MESSAGE, queue);
    }

    protected void onWebsocketClose(int statusCode, String reason) {
        LOG.warn("{} websocket unexpectedly closed with status code {} and reason {}", name, statusCode, reason);

        // AB.7.5.5
        var errorCode = switch (statusCode) {
            case 1000 -> ErrorCode.websocketClosedByPeer;
            case 1001 -> ErrorCode.websocketEndpointLeaves;
            case 1002 -> ErrorCode.websocketProtocolError;
            case 1003 -> ErrorCode.websocketDataNotAccepted;
            case 1006 -> ErrorCode.websocketClosedAbnormally;
            case 1007 -> ErrorCode.websocketDataInconsistent;
            case 1008 -> ErrorCode.websocketDataAgainstPolicy;
            case 1009 -> ErrorCode.websocketFrameTooLong;
            case 1010 -> ErrorCode.websocketExtensionMissing;
            case 1011 -> ErrorCode.websocketRequestUnavailable;
            default -> ErrorCode.websocketError;
        };

        if (statusCode != 1000) { // Ignore normal
            connectionError = new ErrorClassAndCode(ErrorClass.communication, errorCode);
            connectionErrorDetails = reason;
        }
        handleEvent(Event.REMOTE_CLOSE);
    }

    private void setTimeoutFuture(int seconds, State source) {
        cancelTimeout();
        timeoutFuture = localDevice.schedule(() -> handleEventImpl(Event.TIMEOUT, source), seconds, TimeUnit.SECONDS);
    }

    private void cancelTimeout() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }
    }

    private void resetHeartbeatFuture() {
        cancelHeartbeat();
        LOG.debug("{} resetting heartbeat", name);
        heartbeatFuture = localDevice.schedule(() -> {
            LOG.debug("{} sending heartbeat", name);
            sendHeartbeatRequest();
        }, network.getHeartbeatTimeout().intValue(), TimeUnit.SECONDS);
    }

    private void cancelHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
        }
    }

    private void sendConnectRequest() {
        expectedConnectAcceptMessageId = getNextMessageId();
        var message = new SCBVLC(null, null, SCBVLC.CONNECT_REQUEST,
                new SCPayloadConnectRequest(
                        new SCVmac(network.getVmac().getBytes()),
                        new SCUuid(network.getDeviceUUID().getBytes()),
                        network.getMaxBvlcLengthAccepted().intValue(),
                        network.getMaxNpduLengthAccepted().intValue()
                ).write(),
                expectedConnectAcceptMessageId);
        write(message.write());
    }

    private void sendHeartbeatRequest() {
        expectedHeartbeatMessageId = getNextMessageId();
        var message = new SCBVLC(null, null, SCBVLC.HEARTBEAT_REQUEST, expectedHeartbeatMessageId);
        write(message.write());
        setTimeoutFuture(network.getHeartbeatAckTimeout(), state);
    }

    private void sendDisconnectRequest() {
        expectedDisconnectMessageId = getNextMessageId();
        SCBVLC message = new SCBVLC(null, null, SCBVLC.DISCONNECT_REQUEST, expectedDisconnectMessageId);
        write(message.write());
    }

    public void sendDisconnectAck(int id) {
        var message = new SCBVLC(null, null, SCBVLC.DISCONNECT_ACK, id);
        write(message.write());
    }

    public void sendHeartbeatAck(int id) {
        var message = new SCBVLC(null, null, SCBVLC.HEARTBEAT_ACK, id);
        write(message.write());
    }

    public void sendAddressResolutionNak(int id, SCVmac destination) {
        var message = new SCBVLC(null, destination, SCBVLC.BVLC_RESULT,
                new SCPayloadBVLCResult(SCBVLC.ADDRESS_RESOLUTION, 0, ErrorClass.communication,
                        ErrorCode.optionalFunctionalityNotSupported, null).write(),
                id);
        write(message.write());
    }

    public SCBVLC sendError(SCVmac source, SCVmac destination, int forFunction, int headerMarker,
            ErrorClass errorClass, ErrorCode errorCode, String errorDetails, int messageId) {
        var result = new SCPayloadBVLCResult(forFunction, headerMarker, errorClass, errorCode, errorDetails);
        var message = new SCBVLC(source, destination, SCBVLC.BVLC_RESULT, result.write(), messageId);
        if (SCNetworkUtils.isBroadcast(source) || SCNetworkUtils.isBroadcast(destination)) { // last minute sanity check
            LOG.warn("{} not sending error message to/from broadcast: {}", name, message);
            return message;
        }
        write(message.write());
        return message;
    }

    // Only call this from a synchronized method.
    private int getNextMessageId() {
        nextMessageId++;
        if (nextMessageId > 0xFFFF) {
            nextMessageId = 0;
        }
        return nextMessageId;
    }

    private void write(byte[] bytes) {
        if (client.isOpen()) {
            client.send(bytes);
        } else if (LOG.isInfoEnabled()) {
            LOG.info("{} message dropped because client is closed: {}", name, StreamUtils.toHex(bytes));
        }
    }

    private SCBVLC parseMessage(ByteQueue queue) {
        if (queue.size() > network.getMaxBvlcLengthAccepted().intValue()) {
            // AB.7.5.3 discard
            LOG.error("{} protocol violation: length of BVLC message ({}) exceeds max accepted size ({})",
                    name, queue.size(), network.getMaxBvlcLengthAccepted().intValue());
            return null;
        }

        var message = new SCBVLC(queue);
        // parsing can return partial failure (does not throw exception)
        if (message.isParseError()) {
            // not really sure if we can trust message.originating as the destination of this response, but, ...
            protocolViolationLogAndSend(message, message.getParseErrorCode(), message.getParseErrorReason());
            return null;
        }
        if (message.getPayload() != null && message.getPayload().length > network.getMaxNpduLengthAccepted()
                .intValue()) {
            LOG.error("{} protocol violation: length of NPDU ({}) exceeds max accepted size ({})",
                    name, queue.size(), network.getMaxNpduLengthAccepted().intValue());
            return null;
        }

        // We CAN'T check options here because the standard says "destination options" are checked by the
        // "destination BACnet/SC node", i.e., NOT the connection peer. An earlier draft had "peer options" that was
        // removed in favor of using new function codes if additions to connection-level messages are absolutely needed.
        // But we can check that data options is *not* present for non-NPDU messages
        if (message.getFunction() != SCBVLC.ENCAPSULATED_NPDU && message.getDataOptions() != null) {
            protocolViolationLogAndSend(message, ErrorCode.inconsistentParameters,
                    "Data options present with non-NPDU");
            return null;
        }

        LOG.debug("{} incoming message: function={}", name, message.getFunction());

        // Error checking, in particular AB.3.1.5
        switch (message.getFunction()) {
            // non-switchable messages - no addresses present
            case SCBVLC.DISCONNECT_ACK,
                 SCBVLC.DISCONNECT_REQUEST,
                 SCBVLC.HEARTBEAT_REQUEST,
                 SCBVLC.HEARTBEAT_ACK,
                 SCBVLC.CONNECT_REQUEST,
                 SCBVLC.CONNECT_ACCEPT -> {
                // check addresses
                if (message.getDestination() != null) {
                    protocolViolationLogAndSend(message, ErrorCode.headerEncodingError,
                            "destination field must be absent");
                    return null;
                }
                if (message.getOriginating() != null) {
                    protocolViolationLogAndSend(message, ErrorCode.headerEncodingError,
                            "originating field must be absent");
                    return null;
                }
                // check payload
                if (message.getFunction() == SCBVLC.CONNECT_REQUEST || message.getFunction() == SCBVLC.CONNECT_ACCEPT) {
                    // AB.3.1.5 Common Error Situations "If a BVLC message is received for which a payload is required, but no payload is present..."
                    if (message.getPayload() == null) {
                        protocolViolationLogAndSend(message, ErrorCode.payloadExpected, "payload must be present");
                        return null;
                    }
                } else {
                    if (message.getPayload() != null) {
                        protocolViolationLogAndSend(message, ErrorCode.unexpectedData, "payload must be absent");
                        return null;
                    }
                }
            }
            // switchable messages - address requirements are dependent on context and direction
            case SCBVLC.ENCAPSULATED_NPDU,
                 SCBVLC.ADVERTISEMENT,
                 SCBVLC.ADDRESS_RESOLUTION_ACK,
                 SCBVLC.BVLC_RESULT,
                 SCBVLC.ADDRESS_RESOLUTION,
                 SCBVLC.ADVERTISEMENT_SOLICITATION -> {
                // check addresses
                // This is a little yucky! some result messages are from the hub itself and some are from other nodes,
                // so we'll let results be either for originating
                if (message.getOriginating() == null && message.getFunction() != SCBVLC.BVLC_RESULT) {
                    protocolViolationLogAndSend(message, ErrorCode.headerEncodingError,
                            "originating field must be present in initiated Hub Connection");
                    return null;
                }
                if (message.getDestination() != null && !SCNetworkUtils.isBroadcast(message.getDestination())) {
                    protocolViolationLogOnly(ErrorCode.headerEncodingError,
                            "destination field must be absent in initiated Hub Connection");
                    return null;
                }
                // check payload
                if (message.getFunction() == SCBVLC.ADDRESS_RESOLUTION || message.getFunction() == SCBVLC.ADVERTISEMENT_SOLICITATION) {
                    // This is not a standard error situation - there is no code for UNEXPECTED_PAYLOAD so we'll use INCONSISTENT_PARAMETERS
                    if (message.getPayload() != null) {
                        protocolViolationLogAndSend(message, ErrorCode.inconsistentParameters,
                                "payload must be absent");
                        return null;
                    }
                } else {
                    // AB.3.1.5 Common Error Situations "If a BVLC message is received for which a payload is required, but no payload is present..."
                    // note that ADDRESS_RESOLUTION_ACK is quirky because it has a defined payload, but the payload can be zero bytes!
                    if (message.getPayload() == null && message.getFunction() != SCBVLC.ADDRESS_RESOLUTION_ACK) {
                        protocolViolationLogAndSend(message, ErrorCode.payloadExpected, "payload must be present");
                        return null;
                    }
                }
                return message;
            }
            // proprietary messages not supported
            case SCBVLC.PROPRIETARY_MESSAGE -> {
                // We don't support *any* proprietary functions so there is no need to look inside the payload at
                // actual proprietary function code
                protocolViolationLogAndSend(message, ErrorCode.bvlcProprietaryFunctionUnknown,
                        "Proprietary function is unknown");
                return null;
            }
            // Everything else is unknown, and we're supposed to NAK it
            default -> {
                // AB.3.1.5 Common Error Situations "If a BVLC message is received that is an unknown BVLC function..."
                protocolViolationLogAndSend(message, ErrorCode.bvlcFunctionUnknown, "Function code is unknown");
                return null;
            }
        }

        return message;
    }

    private SCPayload parsePayload(SCBVLC message) {
        return switch (message.getFunction()) {
            case SCBVLC.CONNECT_ACCEPT -> new SCPayloadConnectAccept(message.getPayload());
            case SCBVLC.BVLC_RESULT -> new SCPayloadBVLCResult(message.getPayload());
            case SCBVLC.ADVERTISEMENT -> new SCPayloadAdvertisement(message.getPayload());
            default -> null;
        };
    }

    private void protocolViolationLogOnly(ErrorCode errorCode, String errorDetails) {
        LOG.error("{} protocol violation: {}, details: {}", name, errorCode, errorDetails);
    }

    private void protocolViolationLogAndSend(SCBVLC message, ErrorCode errorCode, String errorDetails) {
        protocolViolationLogOnly(errorCode, errorDetails);
        if (message.isUnicastRequest()) {
            sendError(null, message.getOriginating(), message.getFunction(), 0,
                    ErrorClass.communication, errorCode, errorDetails, message.getId());
        }
    }

    @Override
    public String toString() {
        return "%s: state=%s, timeout=%s, heartbeat=%s, connectionState=%s, connectTimestamp=%s, disconnectTimestamp=%s, connectionError=%s, connectionErrorDetails=%s"
                .formatted(name, state,
                        (timeoutFuture == null || timeoutFuture.isCancelled()) ? "inactive" : "active",
                        (heartbeatFuture == null || heartbeatFuture.isCancelled()) ? "inactive" : "active",
                        connectionState, connectTimestamp, disconnectTimestamp, connectionError,
                        connectionErrorDetails);
    }
}
