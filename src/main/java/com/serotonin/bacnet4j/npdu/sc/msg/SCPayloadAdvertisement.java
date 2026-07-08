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

package com.serotonin.bacnet4j.npdu.sc.msg;

import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * A data object that holds a payload of an Advertisement message, as defined in AB.2.8.
 * This includes methods to parse from, and generate to, byte buffers.
 */
public class SCPayloadAdvertisement implements SCPayload {
    public static final int CONN_STAT_NONE = 0;
    public static final int CONN_STAT_PRIMARY = 1;
    public static final int CONN_STAT_FAILOVER = 2;

    private final int connStatus;
    // Accept Connections 1-octet X'00' The node does not accept WebSocket connections, X'01' The node accepts WebSocket connections.
    private final boolean acceptConnections;
    // Maximum BVLC Length 2-octet The maximum BVLC message size that can be received and processed by the node, in number of octets.
    private final int maximumBVLCLength;
    // Maximum NPDU Length 2-octets The maximum NPDU message size that can be handled
    private final int maximumNPDULength;

    public SCPayloadAdvertisement(int connStatus, boolean acceptConnections, int maximumBVLCLength,
            int maximumNPDULength) {
        this.connStatus = connStatus;
        this.acceptConnections = acceptConnections;
        this.maximumBVLCLength = maximumBVLCLength;
        this.maximumNPDULength = maximumNPDULength;
    }

    public SCPayloadAdvertisement(byte[] bytes) {
        var queue = new ByteQueue(bytes);
        connStatus = queue.popU1B();
        acceptConnections = queue.popU1B() != 0;
        maximumBVLCLength = queue.popU2B();
        maximumNPDULength = queue.popU2B();
    }

    public int getMaximumBVLCLength() {
        return maximumBVLCLength;
    }

    public int getMaximumNPDULength() {
        return maximumNPDULength;
    }

    public byte[] write() {
        var queue = new ByteQueue();
        queue.push((byte) connStatus);
        queue.push(acceptConnections ? (byte) 1 : (byte) 0);
        queue.pushU2B(maximumBVLCLength);
        queue.pushU2B(maximumNPDULength);
        return queue.popAll();
    }

    public String toString() {
        return "(c=" + connStatus +
                " a=" + acceptConnections +
                " b=" + maximumBVLCLength +
                " n=" + maximumNPDULength +
                ")";
    }
}
