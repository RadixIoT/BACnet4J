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

public class SCDirectConnection extends BaseType {
    private final CharacterString uri;
    private final SCConnectionState connectionState;
    private final DateTime connectTimestamp;
    private final DateTime disconnectTimestamp;
    private final HostNPort peerAddress;
    private final OctetString peerVmac;
    private final OctetString peerUuid;
    private final ErrorClassAndCode error;
    private final CharacterString errorDetails;

    public SCDirectConnection(CharacterString uri, SCConnectionState connectionState, DateTime connectTimestamp,
            DateTime disconnectTimestamp, HostNPort peerAddress, OctetString peerVmac, OctetString peerUuid,
            ErrorClassAndCode error, CharacterString errorDetails) {
        this.uri = uri;
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
        write(queue, uri, 0);
        write(queue, connectionState, 1);
        write(queue, connectTimestamp, 2);
        write(queue, disconnectTimestamp, 3);
        writeOptional(queue, peerAddress, 4);
        writeOptional(queue, peerVmac, 5);
        writeOptional(queue, peerUuid, 6);
        writeOptional(queue, error, 7);
        writeOptional(queue, errorDetails, 8);
    }

    @Override
    public String toString() {
        return "SCDirectConnection [" +
                "uri=" + uri +
                ", connectionState=" + connectionState +
                ", connectTimestamp=" + connectTimestamp +
                ", disconnectTimestamp=" + disconnectTimestamp +
                ", peerAddress=" + peerAddress +
                ", peerVmac=" + peerVmac +
                ", peerUuid=" + peerUuid +
                ", error=" + error +
                ", errorDetails=" + errorDetails +
                ']';
    }

    public CharacterString getUri() {
        return uri;
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

    public SCDirectConnection(final ByteQueue queue) throws BACnetException {
        uri = read(queue, CharacterString.class, 0);
        connectionState = read(queue, SCConnectionState.class, 1);
        connectTimestamp = read(queue, DateTime.class, 2);
        disconnectTimestamp = read(queue, DateTime.class, 3);
        peerAddress = read(queue, HostNPort.class, 4);
        peerVmac = read(queue, OctetString.class, 5);
        peerUuid = read(queue, OctetString.class, 6);
        error = readOptional(queue, ErrorClassAndCode.class, 7);
        errorDetails = readOptional(queue, CharacterString.class, 8);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        SCDirectConnection that = (SCDirectConnection) o;
        return Objects.equals(uri, that.uri) && Objects.equals(connectionState,
                that.connectionState) && Objects.equals(connectTimestamp,
                that.connectTimestamp) && Objects.equals(disconnectTimestamp,
                that.disconnectTimestamp) && Objects.equals(peerAddress,
                that.peerAddress) && Objects.equals(peerVmac, that.peerVmac) && Objects.equals(peerUuid,
                that.peerUuid) && Objects.equals(error, that.error) && Objects.equals(errorDetails,
                that.errorDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, connectionState, connectTimestamp, disconnectTimestamp, peerAddress, peerVmac,
                peerUuid,
                error, errorDetails);
    }
}
