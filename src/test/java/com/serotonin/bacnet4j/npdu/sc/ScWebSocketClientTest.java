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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.serotonin.bacnet4j.type.enumerated.ErrorCode;

/**
 * Tests the exception → BACnet ErrorCode mapping in {@link ScWebSocketClient#onError} per
 * AB.7.5.1. The spec says that the listed codes "can be used" but does not mandate them, so
 * the implementation makes a best effort based on exception type and message.
 */
public class ScWebSocketClientTest {
    private static final URI URI_UNDER_TEST = URI.create("wss://test.example.com:4443/");

    private ScWebSocketClient newClient(SCConnection connection) {
        return new ScWebSocketClient("test", URI_UNDER_TEST, connection, 5000);
    }

    private ErrorCode captureErrorCode(SCConnection connection) {
        ArgumentCaptor<ErrorCode> cap = ArgumentCaptor.forClass(ErrorCode.class);
        verify(connection).onWebsocketError(cap.capture(), org.mockito.ArgumentMatchers.anyString());
        return cap.getValue();
    }

    /**
     * ConnectException with the specific message "Connection refused" maps to
     * tcpConnectionRefused. This is the only ConnectException case with a distinct code.
     */
    @Test
    public void onError_connectionRefused_mapsToTcpConnectionRefused() {
        SCConnection connection = mock(SCConnection.class);
        newClient(connection).onError(new ConnectException("Connection refused"));
        assertEquals(ErrorCode.tcpConnectionRefused, captureErrorCode(connection));
    }

    /**
     * A ConnectException with a different message (not "Connection refused") falls back to
     * the generic "other" code. This includes cases like connection timeouts, which look
     * like ConnectException but don't have the refused message.
     */
    @Test
    public void onError_connectExceptionOtherMessage_mapsToOther() {
        SCConnection connection = mock(SCConnection.class);
        newClient(connection).onError(new ConnectException("Connection timed out"));
        assertEquals(ErrorCode.other, captureErrorCode(connection));
    }

    /**
     * NoRouteToHostException maps to ipAddressNotReachable (AB.7.5.1).
     */
    @Test
    public void onError_noRouteToHost_mapsToIpAddressNotReachable() {
        SCConnection connection = mock(SCConnection.class);
        newClient(connection).onError(new NoRouteToHostException("no route"));
        assertEquals(ErrorCode.ipAddressNotReachable, captureErrorCode(connection));
    }

    /**
     * Any other exception type falls back to the generic "other" code. Ensures the switch is
     * not accidentally exhaustive.
     */
    @Test
    public void onError_otherException_mapsToOther() {
        SCConnection connection = mock(SCConnection.class);
        newClient(connection).onError(new RuntimeException("boom"));
        assertEquals(ErrorCode.other, captureErrorCode(connection));
    }

    /**
     * onClose with remote=true forwards to the connection's onWebsocketClose with the code
     * and reason. When remote=false (local close initiated by our side), the connection is
     * NOT notified — the calling side already knows it's closing.
     */
    @Test
    public void onClose_remoteTrue_forwardsToConnection() {
        SCConnection connection = mock(SCConnection.class);
        newClient(connection).onClose(1002, "protocol error", true);
        verify(connection).onWebsocketClose(1002, "protocol error");
    }

    @Test
    public void onClose_remoteFalse_doesNotNotifyConnection() {
        SCConnection connection = mock(SCConnection.class);
        newClient(connection).onClose(1000, "normal", false);
        verify(connection, never()).onWebsocketClose(
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Binary WebSocket frames are forwarded to the connection as a ByteQueue for BVLC parsing.
     */
    @Test
    public void onMessage_binaryFrame_forwardsToConnectionAsByteQueue() {
        SCConnection connection = mock(SCConnection.class);
        var payload = new byte[] {0x01, 0x02, 0x03};
        newClient(connection).onMessage(ByteBuffer.wrap(payload));
        verify(connection).onWebsocketMessage(org.mockito.ArgumentMatchers.any());
    }

    /**
     * Text WebSocket frames are forwarded via onTextData per AB.7.5.3 (which requires the
     * connection to close on text frames since only binary is expected).
     */
    @Test
    public void onMessage_textFrame_forwardsToConnectionOnTextData() {
        SCConnection connection = mock(SCConnection.class);
        newClient(connection).onMessage("hello");
        verify(connection).onTextData("hello");
    }
}
