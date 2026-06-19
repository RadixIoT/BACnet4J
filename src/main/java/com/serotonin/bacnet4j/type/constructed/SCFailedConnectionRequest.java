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

package com.serotonin.bacnet4j.type.constructed;

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SCFailedConnectionRequest extends BaseType {
    private final DateTime timestamp;
    private final HostNPort peerAddress;
    private final OctetString peerVmac;
    private final OctetString peerUuid;
    private final ErrorClassAndCode error;
    private final CharacterString errorDetails;

    public SCFailedConnectionRequest(DateTime timestamp, HostNPort peerAddress, OctetString peerVmac,
            OctetString peerUuid, ErrorClassAndCode error, CharacterString errorDetails) {
        // Per spec, peer-vmac is OctetString(SIZE(6)) and peer-uuid is OctetString(SIZE(16)).
        if (peerVmac != null && peerVmac.getLength() != 6L) {
            throw new IllegalArgumentException("invalid peerVmac length");
        }
        if (peerUuid != null && peerUuid.getLength() != 16L) {
            throw new IllegalArgumentException("invalid peerUuid length");
        }

        this.timestamp = timestamp;
        this.peerAddress = peerAddress;
        this.peerVmac = peerVmac;
        this.peerUuid = peerUuid;
        this.error = error;
        this.errorDetails = errorDetails;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, peerAddress, 1);
        writeOptional(queue, peerVmac, 2);
        writeOptional(queue, peerUuid, 3);
        write(queue, error, 4);
        writeOptional(queue, errorDetails, 5);
    }

    @Override
    public String toString() {
        return "SCFailedConnectionRequest [" +
                "timestamp=" + timestamp +
                ", peerAddress=" + peerAddress +
                ", peerVmac=" + peerVmac +
                ", peerUuid=" + peerUuid +
                ", error=" + error +
                ", errorDetails=" + errorDetails +
                ']';
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public HostNPort getPeerAddress() {
        return peerAddress;
    }

    public OctetString getPeerVmac() {
        return peerVmac;
    }

    public OctetString getPeerUuid() {
        return peerUuid;
    }

    public ErrorClassAndCode getError() {
        return error;
    }

    public CharacterString getErrorDetails() {
        return errorDetails;
    }

    public SCFailedConnectionRequest(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        peerAddress = read(queue, HostNPort.class, 1);
        peerVmac = readOptional(queue, OctetString.class, 2);
        peerUuid = readOptional(queue, OctetString.class, 3);
        error = read(queue, ErrorClassAndCode.class, 4);
        errorDetails = readOptional(queue, CharacterString.class, 5);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        SCFailedConnectionRequest that = (SCFailedConnectionRequest) o;
        return Objects.equals(timestamp, that.timestamp) && Objects.equals(peerAddress,
                that.peerAddress) && Objects.equals(peerVmac, that.peerVmac) && Objects.equals(peerUuid,
                that.peerUuid) && Objects.equals(error, that.error) && Objects.equals(errorDetails,
                that.errorDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, peerAddress, peerVmac, peerUuid, error, errorDetails);
    }
}
