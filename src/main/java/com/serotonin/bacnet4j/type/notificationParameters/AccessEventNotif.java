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
import com.serotonin.bacnet4j.type.constructed.AuthenticationFactor;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectReference;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.AccessEvent;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AccessEventNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 13;

    private final AccessEvent accessEvent;
    private final StatusFlags statusFlags;
    private final UnsignedInteger accessEventTag;
    private final TimeStamp accessEventTime;
    private final DeviceObjectReference accessCredential;
    private final AuthenticationFactor authenticationFactor;

    public AccessEventNotif(final AccessEvent accessEvent, final StatusFlags statusFlags,
            final UnsignedInteger accessEventTag, final TimeStamp accessEventTime,
            final DeviceObjectReference accessCredential, final AuthenticationFactor authenticationFactor) {
        this.accessEvent = accessEvent;
        this.statusFlags = statusFlags;
        this.accessEventTag = accessEventTag;
        this.accessEventTime = accessEventTime;
        this.accessCredential = accessCredential;
        this.authenticationFactor = authenticationFactor;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, accessEvent, 0);
        write(queue, statusFlags, 1);
        write(queue, accessEventTag, 2);
        write(queue, accessEventTime, 3);
        write(queue, accessCredential, 4);
        writeOptional(queue, authenticationFactor, 5);
    }

    public AccessEventNotif(final ByteQueue queue) throws BACnetException {
        accessEvent = read(queue, AccessEvent.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        accessEventTag = read(queue, UnsignedInteger.class, 2);
        accessEventTime = read(queue, TimeStamp.class, 3);
        accessCredential = read(queue, DeviceObjectReference.class, 4);
        authenticationFactor = readOptional(queue, AuthenticationFactor.class, 5);
    }

    public AccessEvent getAccessEvent() {
        return accessEvent;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public UnsignedInteger getAccessEventTag() {
        return accessEventTag;
    }

    public TimeStamp getAccessEventTime() {
        return accessEventTime;
    }

    public DeviceObjectReference getAccessCredential() {
        return accessCredential;
    }

    public AuthenticationFactor getAuthenticationFactor() {
        return authenticationFactor;
    }

    @Override
    public String toString() {
        return "AccessEventNotif[ accessEvent=" + accessEvent + ", statusFlags=" + statusFlags + ", accessEventTag=" + accessEventTag + ", accessEventTime=" + accessEventTime + ", accessCredential=" + accessCredential + ", authenticationFactor=" + authenticationFactor + ']';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (accessCredential == null ? 0 : accessCredential.hashCode());
        result = prime * result + (accessEvent == null ? 0 : accessEvent.hashCode());
        result = prime * result + (accessEventTag == null ? 0 : accessEventTag.hashCode());
        result = prime * result + (accessEventTime == null ? 0 : accessEventTime.hashCode());
        result = prime * result + (authenticationFactor == null ? 0 : authenticationFactor.hashCode());
        result = prime * result + (statusFlags == null ? 0 : statusFlags.hashCode());
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
        final AccessEventNotif other = (AccessEventNotif) obj;
        if (accessCredential == null) {
            if (other.accessCredential != null)
                return false;
        } else if (!accessCredential.equals(other.accessCredential))
            return false;
        if (accessEvent == null) {
            if (other.accessEvent != null)
                return false;
        } else if (!accessEvent.equals(other.accessEvent))
            return false;
        if (accessEventTag == null) {
            if (other.accessEventTag != null)
                return false;
        } else if (!accessEventTag.equals(other.accessEventTag))
            return false;
        if (accessEventTime == null) {
            if (other.accessEventTime != null)
                return false;
        } else if (!accessEventTime.equals(other.accessEventTime))
            return false;
        if (authenticationFactor == null) {
            if (other.authenticationFactor != null)
                return false;
        } else if (!authenticationFactor.equals(other.authenticationFactor))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
