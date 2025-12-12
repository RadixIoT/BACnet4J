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

package com.serotonin.bacnet4j.type.eventParameter;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.BufferReadyAlgo;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class BufferReady extends AbstractEventParameter {
    public static final byte TYPE_ID = 10;

    private final UnsignedInteger notificationThreshold;
    private final UnsignedInteger previousNotificationCount;

    // This parameter is stateful.
    private UnsignedInteger mutablePreviousNotificationCount;

    public BufferReady(final UnsignedInteger notificationThreshold, final UnsignedInteger previousNotificationCount) {
        this.notificationThreshold = notificationThreshold;
        this.previousNotificationCount = previousNotificationCount;
        mutablePreviousNotificationCount = previousNotificationCount;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, notificationThreshold, 0);
        write(queue, previousNotificationCount, 1);
    }

    public BufferReady(final ByteQueue queue) throws BACnetException {
        notificationThreshold = read(queue, UnsignedInteger.class, 0);
        previousNotificationCount = read(queue, UnsignedInteger.class, 1);
    }

    public UnsignedInteger getNotificationThreshold() {
        return notificationThreshold;
    }

    public UnsignedInteger getPreviousNotificationCount() {
        return previousNotificationCount;
    }

    public UnsignedInteger getMutablePreviousNotificationCount() {
        return mutablePreviousNotificationCount;
    }

    @Override
    public void postNotification(final NotificationParameters notifParams) {
        // The previous notification count has to be updated following a notification.
        final BufferReadyNotif brn = (BufferReadyNotif) notifParams.getParameter();
        mutablePreviousNotificationCount = brn.getCurrentNotification();
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return new BufferReadyAlgo();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (notificationThreshold == null ? 0 : notificationThreshold.hashCode());
        result = PRIME * result + (previousNotificationCount == null ? 0 : previousNotificationCount.hashCode());
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
        final BufferReady other = (BufferReady) obj;
        if (notificationThreshold == null) {
            if (other.notificationThreshold != null)
                return false;
        } else if (!notificationThreshold.equals(other.notificationThreshold))
            return false;
        if (previousNotificationCount == null) {
            if (other.previousNotificationCount != null)
                return false;
        } else if (!previousNotificationCount.equals(other.previousNotificationCount))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BufferReady[ notificationThreshold=" + notificationThreshold + ", previousNotificationCount=" + previousNotificationCount + ", mutablePreviousNotificationCount=" + mutablePreviousNotificationCount + ']';
    }
}
