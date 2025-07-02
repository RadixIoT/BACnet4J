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

package com.serotonin.bacnet4j.service.unconfirmed;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.NPCI.NetworkPriority;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class UnconfirmedEventNotificationRequest extends UnconfirmedRequestService {
    public static final byte TYPE_ID = 3;

    private final UnsignedInteger processIdentifier; // 0
    private final ObjectIdentifier initiatingDeviceIdentifier; // 1
    private final ObjectIdentifier eventObjectIdentifier; // 2
    private final TimeStamp timeStamp; // 3
    private final UnsignedInteger notificationClass; // 4
    private final UnsignedInteger priority; // 5
    private final EventType eventType; // 6
    private final CharacterString messageText; // 7 optional
    private final NotifyType notifyType; // 8
    private final Boolean ackRequired; // 9 optional
    private final EventState fromState; // 10 optional
    private final EventState toState; // 11
    private final NotificationParameters eventValues; // 12 optional

    public UnconfirmedEventNotificationRequest(final UnsignedInteger processIdentifier,
            final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier eventObjectIdentifier,
            final TimeStamp timeStamp, final UnsignedInteger notificationClass, final UnsignedInteger priority,
            final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
            final Boolean ackRequired, final EventState fromState, final EventState toState,
            final NotificationParameters eventValues) {
        this.processIdentifier = processIdentifier;
        this.initiatingDeviceIdentifier = initiatingDeviceIdentifier;
        this.eventObjectIdentifier = eventObjectIdentifier;
        this.timeStamp = timeStamp;
        this.notificationClass = notificationClass;
        this.priority = priority;
        this.eventType = eventType;
        this.messageText = messageText;
        this.notifyType = notifyType;
        this.ackRequired = ackRequired;
        this.fromState = fromState;
        this.toState = toState;
        this.eventValues = eventValues;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public NetworkPriority getNetworkPriority() {
        return NetworkPriority.forEventPriority(priority.intValue());
    }

    @Override
    public void handle(final LocalDevice localDevice, final Address from) {
        localDevice.updateRemoteDevice(initiatingDeviceIdentifier.getInstanceNumber(), from);
        localDevice.getEventHandler().fireEventNotification(processIdentifier, initiatingDeviceIdentifier,
                eventObjectIdentifier, timeStamp, notificationClass, priority, eventType, messageText, notifyType,
                ackRequired, fromState, toState, eventValues);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, processIdentifier, 0);
        write(queue, initiatingDeviceIdentifier, 1);
        write(queue, eventObjectIdentifier, 2);
        write(queue, timeStamp, 3);
        write(queue, notificationClass, 4);
        write(queue, priority, 5);
        write(queue, eventType, 6);
        writeOptional(queue, messageText, 7);
        write(queue, notifyType, 8);
        writeOptional(queue, ackRequired, 9);
        writeOptional(queue, fromState, 10);
        write(queue, toState, 11);
        writeOptional(queue, eventValues, 12);
    }

    UnconfirmedEventNotificationRequest(final ByteQueue queue) throws BACnetException {
        processIdentifier = read(queue, UnsignedInteger.class, 0);
        initiatingDeviceIdentifier = read(queue, ObjectIdentifier.class, 1);
        eventObjectIdentifier = read(queue, ObjectIdentifier.class, 2);
        timeStamp = read(queue, TimeStamp.class, 3);
        notificationClass = read(queue, UnsignedInteger.class, 4);
        priority = read(queue, UnsignedInteger.class, 5);
        eventType = read(queue, EventType.class, 6);
        messageText = readOptional(queue, CharacterString.class, 7);
        notifyType = read(queue, NotifyType.class, 8);
        ackRequired = readOptional(queue, Boolean.class, 9);
        fromState = readOptional(queue, EventState.class, 10);
        toState = read(queue, EventState.class, 11);
        eventValues = read(queue, NotificationParameters.class, 12);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (ackRequired == null ? 0 : ackRequired.hashCode());
        result = PRIME * result + (eventObjectIdentifier == null ? 0 : eventObjectIdentifier.hashCode());
        result = PRIME * result + (eventType == null ? 0 : eventType.hashCode());
        result = PRIME * result + (eventValues == null ? 0 : eventValues.hashCode());
        result = PRIME * result + (fromState == null ? 0 : fromState.hashCode());
        result = PRIME * result + (initiatingDeviceIdentifier == null ? 0 : initiatingDeviceIdentifier.hashCode());
        result = PRIME * result + (messageText == null ? 0 : messageText.hashCode());
        result = PRIME * result + (notificationClass == null ? 0 : notificationClass.hashCode());
        result = PRIME * result + (notifyType == null ? 0 : notifyType.hashCode());
        result = PRIME * result + (priority == null ? 0 : priority.hashCode());
        result = PRIME * result + (processIdentifier == null ? 0 : processIdentifier.hashCode());
        result = PRIME * result + (timeStamp == null ? 0 : timeStamp.hashCode());
        result = PRIME * result + (toState == null ? 0 : toState.hashCode());
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
        final UnconfirmedEventNotificationRequest other = (UnconfirmedEventNotificationRequest) obj;
        if (ackRequired == null) {
            if (other.ackRequired != null)
                return false;
        } else if (!ackRequired.equals(other.ackRequired))
            return false;
        if (eventObjectIdentifier == null) {
            if (other.eventObjectIdentifier != null)
                return false;
        } else if (!eventObjectIdentifier.equals(other.eventObjectIdentifier))
            return false;
        if (eventType == null) {
            if (other.eventType != null)
                return false;
        } else if (!eventType.equals(other.eventType))
            return false;
        if (eventValues == null) {
            if (other.eventValues != null)
                return false;
        } else if (!eventValues.equals(other.eventValues))
            return false;
        if (fromState == null) {
            if (other.fromState != null)
                return false;
        } else if (!fromState.equals(other.fromState))
            return false;
        if (initiatingDeviceIdentifier == null) {
            if (other.initiatingDeviceIdentifier != null)
                return false;
        } else if (!initiatingDeviceIdentifier.equals(other.initiatingDeviceIdentifier))
            return false;
        if (messageText == null) {
            if (other.messageText != null)
                return false;
        } else if (!messageText.equals(other.messageText))
            return false;
        if (notificationClass == null) {
            if (other.notificationClass != null)
                return false;
        } else if (!notificationClass.equals(other.notificationClass))
            return false;
        if (notifyType == null) {
            if (other.notifyType != null)
                return false;
        } else if (!notifyType.equals(other.notifyType))
            return false;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
        if (processIdentifier == null) {
            if (other.processIdentifier != null)
                return false;
        } else if (!processIdentifier.equals(other.processIdentifier))
            return false;
        if (timeStamp == null) {
            if (other.timeStamp != null)
                return false;
        } else if (!timeStamp.equals(other.timeStamp))
            return false;
        if (toState == null) {
            if (other.toState != null)
                return false;
        } else if (!toState.equals(other.toState))
            return false;
        return true;
    }
}
