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
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfReliabilityNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 19;

    private final Reliability reliability;
    private final StatusFlags statusFlags;
    private final SequenceOf<PropertyValue> propertyValues;

    public ChangeOfReliabilityNotif(final Reliability reliability, final StatusFlags statusFlags,
            final SequenceOf<PropertyValue> propertyValues) {
        this.reliability = reliability;
        this.statusFlags = statusFlags;
        this.propertyValues = propertyValues;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, reliability, 0);
        write(queue, statusFlags, 1);
        write(queue, propertyValues, 2);
    }

    public ChangeOfReliabilityNotif(final ByteQueue queue) throws BACnetException {
        reliability = read(queue, Reliability.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        propertyValues = readSequenceOf(queue, PropertyValue.class, 2);
    }

    public Reliability getReliability() {
        return reliability;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public SequenceOf<PropertyValue> getPropertyValues() {
        return propertyValues;
    }

    @Override
    public String toString() {
        return "ChangeOfReliabilityNotif [reliability=" + reliability + ", statusFlags=" + statusFlags
                + ", propertyValues=" + propertyValues + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (propertyValues == null ? 0 : propertyValues.hashCode());
        result = prime * result + (reliability == null ? 0 : reliability.hashCode());
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
        final ChangeOfReliabilityNotif other = (ChangeOfReliabilityNotif) obj;
        if (propertyValues == null) {
            if (other.propertyValues != null)
                return false;
        } else if (!propertyValues.equals(other.propertyValues))
            return false;
        if (reliability == null) {
            if (other.reliability != null)
                return false;
        } else if (!reliability.equals(other.reliability))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
