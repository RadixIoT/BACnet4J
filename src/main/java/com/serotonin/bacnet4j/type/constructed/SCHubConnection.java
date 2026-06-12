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
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SCHubConnection extends BaseType {
    private final SCConnectionState connectionState;
    private final DateTime connectTimestamp;
    private final DateTime disconnectTimestamp;
    private final ErrorClassAndCode error;
    private final CharacterString errorDetails;

    public SCHubConnection(SCConnectionState connectionState, DateTime connectTimestamp, DateTime disconnectTimestamp,
            ErrorClassAndCode error, CharacterString errorDetails) {
        this.connectionState = connectionState;
        this.connectTimestamp = connectTimestamp;
        this.disconnectTimestamp = disconnectTimestamp;
        this.error = error;
        this.errorDetails = errorDetails;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, connectionState, 0);
        write(queue, connectTimestamp, 1);
        write(queue, disconnectTimestamp, 2);
        writeOptional(queue, error, 3);
        writeOptional(queue, errorDetails, 4);
    }

    @Override
    public String toString() {
        return "SCHubConnection [" +
                "connectionState=" + connectionState +
                ", connectTimestamp=" + connectTimestamp +
                ", disconnectTimestamp=" + disconnectTimestamp +
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

    public ErrorClassAndCode getError() {
        return error;
    }

    public CharacterString getErrorDetails() {
        return errorDetails;
    }

    public SCHubConnection(final ByteQueue queue) throws BACnetException {
        connectionState = read(queue, SCConnectionState.class, 0);
        connectTimestamp = read(queue, DateTime.class, 1);
        disconnectTimestamp = read(queue, DateTime.class, 2);
        error = readOptional(queue, ErrorClassAndCode.class, 3);
        errorDetails = readOptional(queue, CharacterString.class, 4);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        SCHubConnection that = (SCHubConnection) o;
        return Objects.equals(connectionState, that.connectionState) && Objects.equals(connectTimestamp,
                that.connectTimestamp) && Objects.equals(disconnectTimestamp,
                that.disconnectTimestamp) && Objects.equals(error, that.error) && Objects.equals(
                errorDetails, that.errorDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionState, connectTimestamp, disconnectTimestamp, error, errorDetails);
    }
}
