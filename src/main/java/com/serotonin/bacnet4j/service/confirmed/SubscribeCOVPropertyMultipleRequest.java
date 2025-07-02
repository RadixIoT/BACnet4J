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

package com.serotonin.bacnet4j.service.confirmed;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.exception.NotImplementedException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SubscribeCOVPropertyMultipleRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 30;

    private final Unsigned32 subscriberProcessIdentifier;
    private final Boolean issueConfirmedNotifications; // optional
    private final UnsignedInteger lifetime; // optional
    private final UnsignedInteger maxNotificationDelay; // optional
    private final SequenceOf<CovSubscriptionSpecification> listOfCovSubscriptionSpecifications;

    public SubscribeCOVPropertyMultipleRequest(final Unsigned32 subscriberProcessIdentifier,
            final Boolean issueConfirmedNotifications, final UnsignedInteger lifetime,
            final UnsignedInteger maxNotificationDelay,
            final SequenceOf<CovSubscriptionSpecification> listOfCovSubscriptionSpecifications) {
        if (subscriberProcessIdentifier.intValue() == 0)
            throw new BACnetRuntimeException("Cannot use 0 as subscriberProcessIdentifier. See 13.1.1");

        this.subscriberProcessIdentifier = subscriberProcessIdentifier;
        this.issueConfirmedNotifications = issueConfirmedNotifications;
        this.lifetime = lifetime;
        this.maxNotificationDelay = maxNotificationDelay;
        this.listOfCovSubscriptionSpecifications = listOfCovSubscriptionSpecifications;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, subscriberProcessIdentifier, 0);
        writeOptional(queue, issueConfirmedNotifications, 1);
        writeOptional(queue, lifetime, 2);
        writeOptional(queue, maxNotificationDelay, 3);
        write(queue, listOfCovSubscriptionSpecifications, 4);
    }

    SubscribeCOVPropertyMultipleRequest(final ByteQueue queue) throws BACnetException {
        subscriberProcessIdentifier = read(queue, Unsigned32.class, 0);
        issueConfirmedNotifications = readOptional(queue, Boolean.class, 1);
        lifetime = readOptional(queue, UnsignedInteger.class, 2);
        maxNotificationDelay = readOptional(queue, UnsignedInteger.class, 3);
        listOfCovSubscriptionSpecifications = readSequenceOf(queue, CovSubscriptionSpecification.class, 4);
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        // Ensure that these two parameter are either both null, or both not null.
        if (issueConfirmedNotifications == null != (lifetime == null))
            throw new BACnetErrorException(ErrorClass.services, ErrorCode.inconsistentParameters);

        throw new NotImplementedException();

        //        try {
        //            final BACnetObject obj = localDevice.getObjectRequired(monitoredObjectIdentifier);
        //            if (issueConfirmedNotifications == null && lifetime == null)
        //                obj.removeCovSubscription(from, subscriberProcessIdentifier);
        //            else
        //                obj.addCovSubscription(from, subscriberProcessIdentifier, issueConfirmedNotifications, lifetime,
        //                        monitoredPropertyIdentifier, covIncrement);
        //            return null;
        //        } catch (final BACnetServiceException e) {
        //            throw new BACnetErrorException(getChoiceId(), e);
        //        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (issueConfirmedNotifications == null ? 0 : issueConfirmedNotifications.hashCode());
        result = prime * result + (lifetime == null ? 0 : lifetime.hashCode());
        result = prime * result
                + (listOfCovSubscriptionSpecifications == null ? 0 : listOfCovSubscriptionSpecifications.hashCode());
        result = prime * result + (maxNotificationDelay == null ? 0 : maxNotificationDelay.hashCode());
        result = prime * result + (subscriberProcessIdentifier == null ? 0 : subscriberProcessIdentifier.hashCode());
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
        final SubscribeCOVPropertyMultipleRequest other = (SubscribeCOVPropertyMultipleRequest) obj;
        if (issueConfirmedNotifications == null) {
            if (other.issueConfirmedNotifications != null)
                return false;
        } else if (!issueConfirmedNotifications.equals(other.issueConfirmedNotifications))
            return false;
        if (lifetime == null) {
            if (other.lifetime != null)
                return false;
        } else if (!lifetime.equals(other.lifetime))
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
        if (subscriberProcessIdentifier == null) {
            if (other.subscriberProcessIdentifier != null)
                return false;
        } else if (!subscriberProcessIdentifier.equals(other.subscriberProcessIdentifier))
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
            private final Real covIncrement; // optional
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

            public PropertyReference getMonitoredProperty() {
                return monitoredProperty;
            }

            public Real getCovIncrement() {
                return covIncrement;
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
        }
    }
}
