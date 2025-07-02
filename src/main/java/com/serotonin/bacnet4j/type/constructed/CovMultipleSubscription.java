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
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class CovMultipleSubscription extends BaseType {
    private final RecipientProcess recipient;
    private final Boolean issueConfirmedNotifications;
    private final UnsignedInteger timeRemaining;
    private final UnsignedInteger maxNotificationDelay;
    private final SequenceOf<CovSubscriptionSpecification> listOfCovSubscriptionSpecifications;

    public CovMultipleSubscription(final RecipientProcess recipient, final Boolean issueConfirmedNotifications,
            final UnsignedInteger timeRemaining, final UnsignedInteger maxNotificationDelay,
            final SequenceOf<CovSubscriptionSpecification> listOfCovSubscriptionSpecifications) {
        this.recipient = recipient;
        this.issueConfirmedNotifications = issueConfirmedNotifications;
        this.timeRemaining = timeRemaining;
        this.maxNotificationDelay = maxNotificationDelay;
        this.listOfCovSubscriptionSpecifications = listOfCovSubscriptionSpecifications;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, recipient, 0);
        write(queue, issueConfirmedNotifications, 1);
        write(queue, timeRemaining, 2);
        write(queue, maxNotificationDelay, 3);
        writeOptional(queue, listOfCovSubscriptionSpecifications, 4);
    }

    public CovMultipleSubscription(final ByteQueue queue) throws BACnetException {
        recipient = read(queue, RecipientProcess.class, 0);
        issueConfirmedNotifications = read(queue, Boolean.class, 1);
        timeRemaining = read(queue, UnsignedInteger.class, 2);
        maxNotificationDelay = read(queue, UnsignedInteger.class, 3);
        listOfCovSubscriptionSpecifications = readSequenceOf(queue, CovSubscriptionSpecification.class, 4);
    }

    public RecipientProcess getRecipient() {
        return recipient;
    }

    public Boolean getIssueConfirmedNotifications() {
        return issueConfirmedNotifications;
    }

    public UnsignedInteger getTimeRemaining() {
        return timeRemaining;
    }

    public UnsignedInteger getMaxNotificationDelay() {
        return maxNotificationDelay;
    }

    public SequenceOf<CovSubscriptionSpecification> getListOfCovSubscriptionSpecifications() {
        return listOfCovSubscriptionSpecifications;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (issueConfirmedNotifications == null ? 0 : issueConfirmedNotifications.hashCode());
        result = prime * result
                + (listOfCovSubscriptionSpecifications == null ? 0 : listOfCovSubscriptionSpecifications.hashCode());
        result = prime * result + (maxNotificationDelay == null ? 0 : maxNotificationDelay.hashCode());
        result = prime * result + (recipient == null ? 0 : recipient.hashCode());
        result = prime * result + (timeRemaining == null ? 0 : timeRemaining.hashCode());
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
        final CovMultipleSubscription other = (CovMultipleSubscription) obj;
        if (issueConfirmedNotifications == null) {
            if (other.issueConfirmedNotifications != null)
                return false;
        } else if (!issueConfirmedNotifications.equals(other.issueConfirmedNotifications))
            return false;
        if (listOfCovSubscriptionSpecifications == null) {
            if (other.listOfCovSubscriptionSpecifications != null)
                return false;
        } else if (!listOfCovSubscriptionSpecifications.equals(other.listOfCovSubscriptionSpecifications))
            return false;
        if (maxNotificationDelay == null) {
            if (other.maxNotificationDelay != null)
                return false;
        } else if (!maxNotificationDelay.equals(other.maxNotificationDelay))
            return false;
        if (recipient == null) {
            if (other.recipient != null)
                return false;
        } else if (!recipient.equals(other.recipient))
            return false;
        if (timeRemaining == null) {
            if (other.timeRemaining != null)
                return false;
        } else if (!timeRemaining.equals(other.timeRemaining))
            return false;
        return true;
    }

    public static class CovSubscriptionSpecification extends BaseType {
        private final ObjectIdentifier monitoredObjectIdentifier;
        private final SequenceOf<CovReference> listOfCovReferences;

        public CovSubscriptionSpecification(final ObjectIdentifier monitoredObjectIdentifier,
                final SequenceOf<CovReference> listOfCovReferences) {
            this.monitoredObjectIdentifier = monitoredObjectIdentifier;
            this.listOfCovReferences = listOfCovReferences;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, monitoredObjectIdentifier, 0);
            write(queue, listOfCovReferences, 1);
        }

        public CovSubscriptionSpecification(final ByteQueue queue) throws BACnetException {
            monitoredObjectIdentifier = read(queue, ObjectIdentifier.class, 0);
            listOfCovReferences = readSequenceOf(queue, CovReference.class, 1);
        }

        public ObjectIdentifier getMonitoredObjectIdentifier() {
            return monitoredObjectIdentifier;
        }

        public SequenceOf<CovReference> getListOfCovReferences() {
            return listOfCovReferences;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (listOfCovReferences == null ? 0 : listOfCovReferences.hashCode());
            result = prime * result + (monitoredObjectIdentifier == null ? 0 : monitoredObjectIdentifier.hashCode());
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
            final CovSubscriptionSpecification other = (CovSubscriptionSpecification) obj;
            if (listOfCovReferences == null) {
                if (other.listOfCovReferences != null)
                    return false;
            } else if (!listOfCovReferences.equals(other.listOfCovReferences))
                return false;
            if (monitoredObjectIdentifier == null) {
                if (other.monitoredObjectIdentifier != null)
                    return false;
            } else if (!monitoredObjectIdentifier.equals(other.monitoredObjectIdentifier))
                return false;
            return true;
        }

        public static class CovReference extends BaseType {

            private final PropertyReference monitoredProperty;
            private final Real covIncrement;
            private final Boolean timestamped;

            public CovReference(final PropertyReference monitoredProperty, final Real covIncrement,
                    final Boolean timestamped) {
                this.monitoredProperty = monitoredProperty;
                this.covIncrement = covIncrement;
                this.timestamped = timestamped;
            }

            @Override
            public void write(final ByteQueue queue) {
                write(queue, monitoredProperty, 0);
                writeOptional(queue, covIncrement, 1);
                write(queue, timestamped, 2);
            }

            public CovReference(final ByteQueue queue) throws BACnetException {
                monitoredProperty = read(queue, PropertyReference.class, 0);
                covIncrement = readOptional(queue, Real.class, 1);
                timestamped = read(queue, Boolean.class, 2);
            }

            public Real getCovIncrement() {
                return covIncrement;
            }

            public PropertyReference getMonitoredProperty() {
                return monitoredProperty;
            }

            public Boolean getTimestamped() {
                return timestamped;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + (covIncrement == null ? 0 : covIncrement.hashCode());
                result = prime * result + (monitoredProperty == null ? 0 : monitoredProperty.hashCode());
                result = prime * result + (timestamped == null ? 0 : timestamped.hashCode());
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
                final CovReference other = (CovReference) obj;
                if (covIncrement == null) {
                    if (other.covIncrement != null)
                        return false;
                } else if (!covIncrement.equals(other.covIncrement))
                    return false;
                if (monitoredProperty == null) {
                    if (other.monitoredProperty != null)
                        return false;
                } else if (!monitoredProperty.equals(other.monitoredProperty))
                    return false;
                if (timestamped == null) {
                    if (other.timestamped != null)
                        return false;
                } else if (!timestamped.equals(other.timestamped))
                    return false;
                return true;
            }

            @Override
            public String toString() {
                return "CovReference [monitoredProperty=" + monitoredProperty + ", covIncrement=" + covIncrement + ", timestamped=" + timestamped + ']';
            }
        }

        @Override
        public String toString() {
            return "CovSubscriptionSpecification [monitoredObjectIdentifier=" + monitoredObjectIdentifier + ", listOfCovReferences=" + listOfCovReferences + ']';
        }
    }

    @Override
    public String toString() {
        return "CovMultipleSubscription [recipient=" + recipient + ", issueConfirmedNotifications=" + issueConfirmedNotifications + ", timeRemaining=" + timeRemaining + ", maxNotificationDelay=" + maxNotificationDelay + ", listOfCovSubscriptionSpecifications=" + listOfCovSubscriptionSpecifications + ']';
    }
}
