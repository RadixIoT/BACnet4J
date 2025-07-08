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
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.ThreadLocalObjectTypeStack;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class PropertyValue extends BaseType {
    private final PropertyIdentifier propertyIdentifier; // 0
    private final UnsignedInteger propertyArrayIndex; // 1 optional
    private final Encodable value; // 2
    private final UnsignedInteger priority; // 3 optional

    public PropertyValue(final PropertyIdentifier propertyIdentifier, final Encodable value) {
        this(propertyIdentifier, null, value, null);
    }

    public PropertyValue(final PropertyIdentifier propertyIdentifier, final UnsignedInteger propertyArrayIndex,
            final Encodable value, final UnsignedInteger priority) {
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
        this.value = value;
        this.priority = priority;
    }

    public UnsignedInteger getPriority() {
        return priority;
    }

    public UnsignedInteger getPropertyArrayIndex() {
        return propertyArrayIndex;
    }

    public PropertyIdentifier getPropertyIdentifier() {
        return propertyIdentifier;
    }

    @Override
    public void validate() throws BACnetServiceException {
        this.value.validate();
    }

    @SuppressWarnings("unchecked")
    public <T extends Encodable> T getValue() {
        return (T) value;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, propertyIdentifier, 0);
        writeOptional(queue, propertyArrayIndex, 1);
        writeANY(queue, value, 2);
        writeOptional(queue, priority, 3);
    }

    public PropertyValue(final ByteQueue queue) throws BACnetException {
        propertyIdentifier = read(queue, PropertyIdentifier.class, 0);
        propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 1);
        value = readANY(queue, ThreadLocalObjectTypeStack.get(), propertyIdentifier, propertyArrayIndex, 2);
        priority = readOptional(queue, UnsignedInteger.class, 3);
    }

    @Override
    public String toString() {
        return "PropertyValue(propertyIdentifier=" + propertyIdentifier + ", propertyArrayIndex=" + propertyArrayIndex
                + ", value=" + value + ", priority=" + priority + ")";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (priority == null ? 0 : priority.hashCode());
        result = PRIME * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
        result = PRIME * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
        result = PRIME * result + (value == null ? 0 : value.hashCode());
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
        final PropertyValue other = (PropertyValue) obj;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
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
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
    //
    //    public static class Builder {
    //        private final PropertyIdentifier propertyIdentifier; // 0
    //        private UnsignedInteger propertyArrayIndex; // 1 optional
    //        private final Encodable value; // 2
    //        private UnsignedInteger priority; // 3 optional
    //
    //        public Builder(PropertyIdentifier propertyIdentifier, Encodable value) {
    //
    //        }
    //    }
}
