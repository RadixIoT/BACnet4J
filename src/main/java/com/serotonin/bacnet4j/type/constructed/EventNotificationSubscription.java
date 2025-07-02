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

package com.serotonin.bacnet4j.type.constructed;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * @author Matthew Lohbihler
 */
public class EventNotificationSubscription extends BaseType {
    private final Recipient recipient; // 0
    private final Unsigned32 processIdentifier; // 1
    private final Boolean issueConfirmedNotifications; // 2
    private final UnsignedInteger timeRemaining; // 3

    // This field is used by the notification forwarder object to track subscriptions that are changed via the
    // AddListElement and RemoveListElement services. It should not be used otherwise.
    private int subscriptionId = -1;

    public EventNotificationSubscription(final Recipient recipient, final Unsigned32 processIdentifier,
            final Boolean issueConfirmedNotifications, final UnsignedInteger timeRemaining) {
        this.recipient = recipient;
        this.processIdentifier = processIdentifier;
        this.issueConfirmedNotifications = issueConfirmedNotifications;
        this.timeRemaining = timeRemaining;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public Unsigned32 getProcessIdentifier() {
        return processIdentifier;
    }

    public Boolean getIssueConfirmedNotifications() {
        return issueConfirmedNotifications;
    }

    public UnsignedInteger getTimeRemaining() {
        return timeRemaining;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(final int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, recipient, 0);
        write(queue, processIdentifier, 1);
        writeOptional(queue, issueConfirmedNotifications, 2);
        writeOptional(queue, timeRemaining, 3);
    }

    public EventNotificationSubscription(final ByteQueue queue) throws BACnetException {
        recipient = read(queue, Recipient.class, 0);
        processIdentifier = read(queue, Unsigned32.class, 1);
        issueConfirmedNotifications = readOptional(queue, Boolean.class, 2);
        timeRemaining = readOptional(queue, UnsignedInteger.class, 3);
    }

    @Override
    public String toString() {
        return "EventNotificationSubscription [recipient=" + recipient + ", processIdentifier=" + processIdentifier
                + ", issueConfirmedNotifications=" + issueConfirmedNotifications + ", timeRemaining=" + timeRemaining
                + "]";
    }

    /**
     * NOTE: For the purposes of the RemoveListElement service this object does NOT use all parameters for hashcode and
     * equals. Only the recipient and processIdentifier are used.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (processIdentifier == null ? 0 : processIdentifier.hashCode());
        result = prime * result + (recipient == null ? 0 : recipient.hashCode());
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
        final EventNotificationSubscription other = (EventNotificationSubscription) obj;
        if (processIdentifier == null) {
            if (other.processIdentifier != null)
                return false;
        } else if (!processIdentifier.equals(other.processIdentifier))
            return false;
        if (recipient == null) {
            if (other.recipient != null)
                return false;
        } else if (!recipient.equals(other.recipient))
            return false;
        return true;
    }
}
