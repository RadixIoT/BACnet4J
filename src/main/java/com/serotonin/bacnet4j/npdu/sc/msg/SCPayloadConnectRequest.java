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

import com.serotonin.bacnet4j.npdu.sc.SCUuid;
import com.serotonin.bacnet4j.npdu.sc.SCVmac;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * A data object that holds a payload of a Connect-Request message, as defined in AB.2.10.
 * This includes methods to parse from, and generate to, byte buffers.
 */
public class SCPayloadConnectRequest implements SCPayload {
    private final SCVmac vmac;
    private final SCUuid uuid;
    private final int maxBVLC;
    private final int maxNPDU;

    public SCPayloadConnectRequest(SCVmac vmac, SCUuid uuid, int maximumBVLCLength, int maximumNPDULength) {
        this.vmac = vmac;
        this.uuid = uuid;
        this.maxBVLC = maximumBVLCLength;
        this.maxNPDU = maximumNPDULength;
    }

    public SCPayloadConnectRequest(byte[] bytes) {
        var queue = new ByteQueue(bytes);
        vmac = new SCVmac(queue);
        uuid = new SCUuid(queue);
        maxBVLC = queue.popU2B();
        maxNPDU = queue.popU2B();
    }

    public byte[] write() {
        var queue = new ByteQueue();
        vmac.write(queue);
        uuid.write(queue);
        queue.pushU2B(maxBVLC);
        queue.pushU2B(maxNPDU);
        return queue.popAll();
    }

    public String toString() {
        return "(v=" + vmac +
                " u=" + uuid +
                " b=" + maxBVLC +
                " n=" + maxNPDU +
                ")";
    }
}
