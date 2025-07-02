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

package com.serotonin.bacnet4j.type.notificationParameters;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.enumerated.TimerState;
import com.serotonin.bacnet4j.type.enumerated.TimerTransition;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfTimerNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 22;

    private final TimerState newState;
    private final StatusFlags statusFlags;
    private final DateTime updateTime;
    private final TimerTransition lastStateChange;
    private final UnsignedInteger initialTimeout;
    private final DateTime expirationTime;

    public ChangeOfTimerNotif(final TimerState newState, final StatusFlags statusFlags, final DateTime updateTime,
            final TimerTransition lastStateChange, final UnsignedInteger initialTimeout,
            final DateTime expirationTime) {
        this.newState = newState;
        this.statusFlags = statusFlags;
        this.updateTime = updateTime;
        this.lastStateChange = lastStateChange;
        this.initialTimeout = initialTimeout;
        this.expirationTime = expirationTime;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, newState, 0);
        write(queue, statusFlags, 1);
        write(queue, updateTime, 2);
        writeOptional(queue, lastStateChange, 3);
        writeOptional(queue, initialTimeout, 4);
        writeOptional(queue, expirationTime, 5);
    }

    public ChangeOfTimerNotif(final ByteQueue queue) throws BACnetException {
        newState = read(queue, TimerState.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        updateTime = read(queue, DateTime.class, 2);
        lastStateChange = readOptional(queue, TimerTransition.class, 3);
        initialTimeout = readOptional(queue, UnsignedInteger.class, 4);
        expirationTime = readOptional(queue, DateTime.class, 5);
    }

    public TimerState getNewState() {
        return newState;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public DateTime getUpdateTime() {
        return updateTime;
    }

    public TimerTransition getLastStateChange() {
        return lastStateChange;
    }

    public UnsignedInteger getInitialTimeout() {
        return initialTimeout;
    }

    public DateTime getExpirationTime() {
        return expirationTime;
    }

    @Override
    public String toString() {
        return "ChangeOfTimerNotif[ newState=" + newState + ", statusFlags=" + statusFlags + ", updateTime=" + updateTime + ", lastStateChange=" + lastStateChange + ", initialTimeout=" + initialTimeout + ", expirationTime=" + expirationTime + ']';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (expirationTime == null ? 0 : expirationTime.hashCode());
        result = prime * result + (initialTimeout == null ? 0 : initialTimeout.hashCode());
        result = prime * result + (lastStateChange == null ? 0 : lastStateChange.hashCode());
        result = prime * result + (newState == null ? 0 : newState.hashCode());
        result = prime * result + (statusFlags == null ? 0 : statusFlags.hashCode());
        result = prime * result + (updateTime == null ? 0 : updateTime.hashCode());
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
        final ChangeOfTimerNotif other = (ChangeOfTimerNotif) obj;
        if (expirationTime == null) {
            if (other.expirationTime != null)
                return false;
        } else if (!expirationTime.equals(other.expirationTime))
            return false;
        if (initialTimeout == null) {
            if (other.initialTimeout != null)
                return false;
        } else if (!initialTimeout.equals(other.initialTimeout))
            return false;
        if (lastStateChange == null) {
            if (other.lastStateChange != null)
                return false;
        } else if (!lastStateChange.equals(other.lastStateChange))
            return false;
        if (newState == null) {
            if (other.newState != null)
                return false;
        } else if (!newState.equals(other.newState))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        if (updateTime == null) {
            if (other.updateTime != null)
                return false;
        } else if (!updateTime.equals(other.updateTime))
            return false;
        return true;
    }
}
