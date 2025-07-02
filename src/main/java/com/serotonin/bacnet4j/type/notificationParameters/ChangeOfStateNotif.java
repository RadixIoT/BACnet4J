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
import com.serotonin.bacnet4j.type.constructed.PropertyStates;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfStateNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 1;

    private final PropertyStates newState;
    private final StatusFlags statusFlags;

    public ChangeOfStateNotif(final PropertyStates newState, final StatusFlags statusFlags) {
        this.newState = newState;
        this.statusFlags = statusFlags;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, newState, 0);
        write(queue, statusFlags, 1);
    }

    public ChangeOfStateNotif(final ByteQueue queue) throws BACnetException {
        newState = read(queue, PropertyStates.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
    }

    public PropertyStates getNewState() {
        return newState;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    @Override
    public String toString() {
        return "ChangeOfStateNotif [newState=" + newState + ", statusFlags=" + statusFlags + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (newState == null ? 0 : newState.hashCode());
        result = PRIME * result + (statusFlags == null ? 0 : statusFlags.hashCode());
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
        final ChangeOfStateNotif other = (ChangeOfStateNotif) obj;
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
        return true;
    }
}
