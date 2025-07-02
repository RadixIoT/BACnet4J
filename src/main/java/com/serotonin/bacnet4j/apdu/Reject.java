/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.apdu;

import com.serotonin.bacnet4j.type.enumerated.RejectReason;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * The BACnet-Reject-PDU is used to reject a received confirmed request PDU based on syntactical flaws or other protocol
 * errors that prevent the PDU from being interpreted or the requested service from being provided. Only confirmed
 * request PDUs may be rejected.
 */
public class Reject extends AckAPDU {
    public static final byte TYPE_ID = 6;

    /**
     * This parameter, of type BACnetRejectReason, contains the reason the PDU with the indicated 'invokeID' is being
     * rejected.
     */
    private final RejectReason rejectReason;

    public Reject(final byte originalInvokeId, final RejectReason rejectReason) {
        this.originalInvokeId = originalInvokeId;
        this.rejectReason = rejectReason;
    }

    @Override
    public byte getPduType() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        queue.push(getShiftedTypeId(TYPE_ID));
        queue.push(originalInvokeId);
        queue.push(rejectReason.byteValue());
    }

    Reject(final ByteQueue queue) {
        queue.pop(); // Ignore the first byte. No news there.
        originalInvokeId = queue.pop();
        rejectReason = RejectReason.forId(queue.popU1B());
    }

    public RejectReason getRejectReason() {
        return rejectReason;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + originalInvokeId;
        result = PRIME * result + (rejectReason == null ? 0 : rejectReason.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Reject other = (Reject) obj;
        if (originalInvokeId != other.originalInvokeId)
            return false;
        if (rejectReason == null) {
            if (other.rejectReason != null)
                return false;
        } else if (!rejectReason.equals(other.rejectReason))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Reject(originalInvokeId=" + originalInvokeId + ", rejectReason=" + rejectReason + ")";
    }

    @Override
    public boolean expectsReply() {
        return false;
    }
}
