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

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.net.ssl.SSLParameters;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.protocols.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ScWebSocketClient extends WebSocketClient {
    private static final Logger LOG = LoggerFactory.getLogger(ScWebSocketClient.class);
    private static final String SUBPROTOCOL = "hub.bsc.bacnet.org"; // The only sub-protocol needed for node-only.
    private static final Draft_6455 draft = new Draft_6455(Collections.emptyList(), List.of(new Protocol(SUBPROTOCOL)));

    private final String name;
    private SCConnection connection;

    public ScWebSocketClient(String name, URI serverUri, SCConnection connection, int connectTimeout) {
        super(serverUri, draft, null, connectTimeout);
        this.name = name;
        this.connection = connection;
    }

    public void terminate() {
        // Setting this to null prevents the client from sending zombie messages to the connection.
        connection = null;
        close();
    }

    @Override
    protected void onSetSSLParameters(SSLParameters params) {
        params.setEndpointIdentificationAlgorithm(null);
    }

    private void withConnection(Consumer<SCConnection> consumer) {
        SCConnection localConnection = connection;
        if (localConnection != null) {
            consumer.accept(localConnection);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        withConnection(conn -> {
            LOG.debug("{} websocket onOpen: {}", name, handshake.getHttpStatus());
            conn.onWebsocketOpen();
        });
    }

    @Override
    public void onMessage(String message) {
        // AB.7.5.3
        withConnection(conn -> {
            conn.onTextData(message);
        });
    }

    @Override
    public void onMessage(ByteBuffer bb) {
        withConnection(conn -> {
            LOG.debug("{} websocket onMessage: {}", name, bb);
            var queue = new ByteQueue();
            queue.push(bb);
            conn.onWebsocketMessage(queue);
        });
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        withConnection(conn -> {
            LOG.debug("{} Websocket onClose: code={}, reason={}, remote={}", name, code, reason, remote);
            if (remote) {
                conn.onWebsocketClose(code, reason);
            }
        });
    }

    @Override
    public void onError(Exception ex) {
        withConnection(conn -> {
            LOG.warn("{} error on websocket connection", name, ex);
            // Codes for various error conditions are defined in AB.7.5.1. But the spec says that they merely "can be used",
            // so the best effort is made to determine the cause, but ultimately "other" is used.
            var code = ErrorCode.other;
            if (ex instanceof ConnectException cex) {
                if ("Connection refused".equals(cex.getMessage())) {
                    code = ErrorCode.tcpConnectionRefused; // This could also be a timeout.
                }
            } else if (ex instanceof NoRouteToHostException) {
                code = ErrorCode.ipAddressNotReachable;
            }
            conn.onWebsocketError(code, ex.getMessage());
        });
    }
}