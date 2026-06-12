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
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SCHubFunctionConnection extends BaseType {
    private final SCConnectionState connectionState;
    private final DateTime connectTimestamp;
    private final DateTime disconnectTimestamp;
    private final HostNPort peerAddress;
    private final OctetString peerVmac;
    private final OctetString peerUuid;
    private final ErrorClassAndCode error;
    private final CharacterString errorDetails;

    public SCHubFunctionConnection(SCConnectionState connectionState, DateTime connectTimestamp,
            DateTime disconnectTimestamp, HostNPort peerAddress, OctetString peerVmac, OctetString peerUuid,
            ErrorClassAndCode error, CharacterString errorDetails) {
        this.connectionState = connectionState;
        this.connectTimestamp = connectTimestamp;
        this.disconnectTimestamp = disconnectTimestamp;
        this.peerAddress = peerAddress;
        this.peerVmac = peerVmac;
        this.peerUuid = peerUuid;
        this.error = error;
        this.errorDetails = errorDetails;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, connectionState, 0);
        write(queue, connectTimestamp, 1);
        write(queue, disconnectTimestamp, 2);
        write(queue, peerAddress, 3);
        write(queue, peerVmac, 4);
        write(queue, peerUuid, 5);
        writeOptional(queue, error, 6);
        writeOptional(queue, errorDetails, 7);
    }

    @Override
    public String toString() {
        return "SCHubFunctionConnection [" +
                "connectionState=" + connectionState +
                ", connectTimestamp=" + connectTimestamp +
                ", disconnectTimestamp=" + disconnectTimestamp +
                ", peerAddress=" + peerAddress +
                ", peerVmac=" + peerVmac +
                ", peerUuid=" + peerUuid +
                ", error=" + error +
                ", errorDetails=" + errorDetails +
                ']';
    }

    public SCConnectionState getConnectionState() {
        return connectionState;
    }

    public DateTime getConnectTimestamp() {
        return connectTimestamp;
    }

    public DateTime getDisconnectTimestamp() {
        return disconnectTimestamp;
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

    public SCHubFunctionConnection(final ByteQueue queue) throws BACnetException {
        connectionState = read(queue, SCConnectionState.class, 0);
        connectTimestamp = read(queue, DateTime.class, 1);
        disconnectTimestamp = read(queue, DateTime.class, 2);
        peerAddress = read(queue, HostNPort.class, 3);
        peerVmac = read(queue, OctetString.class, 4);
        peerUuid = read(queue, OctetString.class, 5);
        error = readOptional(queue, ErrorClassAndCode.class, 6);
        errorDetails = readOptional(queue, CharacterString.class, 7);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        SCHubFunctionConnection that = (SCHubFunctionConnection) o;
        return Objects.equals(connectionState, that.connectionState) && Objects.equals(connectTimestamp,
                that.connectTimestamp) && Objects.equals(disconnectTimestamp,
                that.disconnectTimestamp) && Objects.equals(peerAddress,
                that.peerAddress) && Objects.equals(peerVmac, that.peerVmac) && Objects.equals(peerUuid,
                that.peerUuid) && Objects.equals(error, that.error) && Objects.equals(errorDetails,
                that.errorDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionState, connectTimestamp, disconnectTimestamp, peerAddress, peerVmac, peerUuid,
                error,
                errorDetails);
    }
}
