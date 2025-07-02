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
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.NotImplementedException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.ThreadLocalObjectTypeStack;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ConfirmedCovNotificationMultipleRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 31;

    private final Unsigned32 subscriberProcessIdentifier;
    private final ObjectIdentifier initiatingDeviceIdentifier;
    private final UnsignedInteger timeRemaining;
    private final DateTime timestamp;
    private final SequenceOf<CovNotification> listOfCovNotifications;

    public ConfirmedCovNotificationMultipleRequest(final Unsigned32 subscriberProcessIdentifier,
            final ObjectIdentifier initiatingDeviceIdentifier, final UnsignedInteger timeRemaining,
            final DateTime timestamp, final SequenceOf<CovNotification> listOfCovNotifications) {
        this.subscriberProcessIdentifier = subscriberProcessIdentifier;
        this.initiatingDeviceIdentifier = initiatingDeviceIdentifier;
        this.timeRemaining = timeRemaining;
        this.timestamp = timestamp;
        this.listOfCovNotifications = listOfCovNotifications;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        throw new NotImplementedException();
        //        localDevice.updateRemoteDevice(initiatingDeviceIdentifier.getInstanceNumber(), from);
        //        localDevice.getEventHandler().fireCovNotification(subscriberProcessIdentifier, initiatingDeviceIdentifier,
        //                monitoredObjectIdentifier, timeRemaining, listOfValues);
        //        return null;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, subscriberProcessIdentifier, 0);
        write(queue, initiatingDeviceIdentifier, 1);
        write(queue, timeRemaining, 2);
        writeOptional(queue, timestamp, 3);
        write(queue, listOfCovNotifications, 4);
    }

    ConfirmedCovNotificationMultipleRequest(final ByteQueue queue) throws BACnetException {
        subscriberProcessIdentifier = read(queue, Unsigned32.class, 0);
        initiatingDeviceIdentifier = read(queue, ObjectIdentifier.class, 1);
        timeRemaining = read(queue, UnsignedInteger.class, 2);
        timestamp = readOptional(queue, DateTime.class, 3);
        listOfCovNotifications = readSequenceOf(queue, CovNotification.class, 4);
    }

    public Unsigned32 getSubscriberProcessIdentifier() {
        return subscriberProcessIdentifier;
    }

    public ObjectIdentifier getInitiatingDeviceIdentifier() {
        return initiatingDeviceIdentifier;
    }

    public UnsignedInteger getTimeRemaining() {
        return timeRemaining;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public SequenceOf<CovNotification> getListOfCovNotifications() {
        return listOfCovNotifications;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (initiatingDeviceIdentifier == null ? 0 : initiatingDeviceIdentifier.hashCode());
        result = prime * result + (listOfCovNotifications == null ? 0 : listOfCovNotifications.hashCode());
        result = prime * result + (subscriberProcessIdentifier == null ? 0 : subscriberProcessIdentifier.hashCode());
        result = prime * result + (timeRemaining == null ? 0 : timeRemaining.hashCode());
        result = prime * result + (timestamp == null ? 0 : timestamp.hashCode());
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
        final ConfirmedCovNotificationMultipleRequest other = (ConfirmedCovNotificationMultipleRequest) obj;
        if (initiatingDeviceIdentifier == null) {
            if (other.initiatingDeviceIdentifier != null)
                return false;
        } else if (!initiatingDeviceIdentifier.equals(other.initiatingDeviceIdentifier))
            return false;
        if (listOfCovNotifications == null) {
            if (other.listOfCovNotifications != null)
                return false;
        } else if (!listOfCovNotifications.equals(other.listOfCovNotifications))
            return false;
        if (subscriberProcessIdentifier == null) {
            if (other.subscriberProcessIdentifier != null)
                return false;
        } else if (!subscriberProcessIdentifier.equals(other.subscriberProcessIdentifier))
            return false;
        if (timeRemaining == null) {
            if (other.timeRemaining != null)
                return false;
        } else if (!timeRemaining.equals(other.timeRemaining))
            return false;
        if (timestamp == null) {
            if (other.timestamp != null)
                return false;
        } else if (!timestamp.equals(other.timestamp))
            return false;
        return true;
    }

    public static class CovNotification extends BaseType {
        private final ObjectIdentifier monitoredObjectIdentifier;
        private final SequenceOf<CovNotificationValue> listOfValues;

        public CovNotification(final ObjectIdentifier monitoredObjectIdentifier,
                final SequenceOf<CovNotificationValue> listOfValues) {
            this.monitoredObjectIdentifier = monitoredObjectIdentifier;
            this.listOfValues = listOfValues;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, monitoredObjectIdentifier, 0);
            write(queue, listOfValues, 1);
        }

        public CovNotification(final ByteQueue queue) throws BACnetException {
            monitoredObjectIdentifier = read(queue, ObjectIdentifier.class, 0);
            try {
                ThreadLocalObjectTypeStack.set(monitoredObjectIdentifier.getObjectType());
                listOfValues = readSequenceOf(queue, CovNotificationValue.class, 1);
            } finally {
                ThreadLocalObjectTypeStack.remove();
            }
        }

        public ObjectIdentifier getMonitoredObjectIdentifier() {
            return monitoredObjectIdentifier;
        }

        public SequenceOf<CovNotificationValue> getListOfValues() {
            return listOfValues;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (listOfValues == null ? 0 : listOfValues.hashCode());
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
            final CovNotification other = (CovNotification) obj;
            if (listOfValues == null) {
                if (other.listOfValues != null)
                    return false;
            } else if (!listOfValues.equals(other.listOfValues))
                return false;
            if (monitoredObjectIdentifier == null) {
                if (other.monitoredObjectIdentifier != null)
                    return false;
            } else if (!monitoredObjectIdentifier.equals(other.monitoredObjectIdentifier))
                return false;
            return true;
        }

        public static class CovNotificationValue extends BaseType {
            private final PropertyIdentifier propertyIdentifier;
            private final UnsignedInteger propertyArrayIndex;
            private final Encodable propertyValue;
            private final Time timeOfChange;

            public CovNotificationValue(final PropertyIdentifier propertyIdentifier,
                    final UnsignedInteger propertyArrayIndex, final Encodable propertyValue, final Time timeOfChange) {
                this.propertyIdentifier = propertyIdentifier;
                this.propertyArrayIndex = propertyArrayIndex;
                this.propertyValue = propertyValue;
                this.timeOfChange = timeOfChange;
            }

            @Override
            public void write(final ByteQueue queue) {
                write(queue, propertyIdentifier, 0);
                writeOptional(queue, propertyArrayIndex, 1);
                writeANY(queue, propertyValue, 2);
                writeOptional(queue, timeOfChange, 3);
            }

            public CovNotificationValue(final ByteQueue queue) throws BACnetException {
                propertyIdentifier = read(queue, PropertyIdentifier.class, 0);
                propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 1);
                propertyValue = readANY(queue, ThreadLocalObjectTypeStack.get(), propertyIdentifier, propertyArrayIndex,
                        2);
                timeOfChange = readOptional(queue, Time.class, 3);
            }

            public PropertyIdentifier getPropertyIdentifier() {
                return propertyIdentifier;
            }

            public UnsignedInteger getPropertyArrayIndex() {
                return propertyArrayIndex;
            }

            @SuppressWarnings("unchecked")
            public <T extends Encodable> T getPropertyValue() {
                return (T) propertyValue;
            }

            public Time getTimeOfChange() {
                return timeOfChange;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
                result = prime * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
                result = prime * result + (propertyValue == null ? 0 : propertyValue.hashCode());
                result = prime * result + (timeOfChange == null ? 0 : timeOfChange.hashCode());
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
                final CovNotificationValue other = (CovNotificationValue) obj;
                if (propertyArrayIndex == null) {
                    if (other.propertyArrayIndex != null)
                        return false;
                } else if (!propertyArrayIndex.equals(other.propertyArrayIndex))
                    return false;
                if (propertyIdentifier == null) {
                    if (other.propertyIdentifier != null)
                        return false;
                } else if (!propertyIdentifier.equals(other.propertyIdentifier))
                    return false;
                if (propertyValue == null) {
                    if (other.propertyValue != null)
                        return false;
                } else if (!propertyValue.equals(other.propertyValue))
                    return false;
                if (timeOfChange == null) {
                    if (other.timeOfChange != null)
                        return false;
                } else if (!timeOfChange.equals(other.timeOfChange))
                    return false;
                return true;
            }
        }
    }
}
