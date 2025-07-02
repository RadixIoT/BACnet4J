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
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class BufferReadyNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 10;

    private final DeviceObjectPropertyReference bufferProperty;
    private final UnsignedInteger previousNotification;
    private final UnsignedInteger currentNotification;

    public BufferReadyNotif(final DeviceObjectPropertyReference bufferProperty,
            final UnsignedInteger previousNotification, final UnsignedInteger currentNotification) {
        this.bufferProperty = bufferProperty;
        this.previousNotification = previousNotification;
        this.currentNotification = currentNotification;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, bufferProperty, 0);
        write(queue, previousNotification, 1);
        write(queue, currentNotification, 2);
    }

    public BufferReadyNotif(final ByteQueue queue) throws BACnetException {
        bufferProperty = read(queue, DeviceObjectPropertyReference.class, 0);
        previousNotification = read(queue, UnsignedInteger.class, 1);
        currentNotification = read(queue, UnsignedInteger.class, 2);
    }

    public DeviceObjectPropertyReference getBufferProperty() {
        return bufferProperty;
    }

    public UnsignedInteger getPreviousNotification() {
        return previousNotification;
    }

    public UnsignedInteger getCurrentNotification() {
        return currentNotification;
    }

    @Override
    public String toString() {
        return "BufferReadyNotif [bufferProperty=" + bufferProperty + ", previousNotification=" + previousNotification
                + ", currentNotification=" + currentNotification + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (bufferProperty == null ? 0 : bufferProperty.hashCode());
        result = PRIME * result + (currentNotification == null ? 0 : currentNotification.hashCode());
        result = PRIME * result + (previousNotification == null ? 0 : previousNotification.hashCode());
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
        final BufferReadyNotif other = (BufferReadyNotif) obj;
        if (bufferProperty == null) {
            if (other.bufferProperty != null)
                return false;
        } else if (!bufferProperty.equals(other.bufferProperty))
            return false;
        if (currentNotification == null) {
            if (other.currentNotification != null)
                return false;
        } else if (!currentNotification.equals(other.currentNotification))
            return false;
        if (previousNotification == null) {
            if (other.previousNotification != null)
                return false;
        } else if (!previousNotification.equals(other.previousNotification))
            return false;
        return true;
    }
}
