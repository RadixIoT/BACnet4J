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

package com.serotonin.bacnet4j.type;

import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class ObjectTypePropertyReference {
    private final ObjectType objectType;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger propertyArrayIndex;

    public ObjectTypePropertyReference(final ObjectType objectType, final PropertyIdentifier propertyIdentifier) {
        this(objectType, propertyIdentifier, null);
    }

    public ObjectTypePropertyReference(final ObjectType objectType, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex) {
        this.objectType = objectType;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public PropertyIdentifier getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public UnsignedInteger getPropertyArrayIndex() {
        return propertyArrayIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (objectType == null ? 0 : objectType.hashCode());
        result = prime * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
        result = prime * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
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
        final ObjectTypePropertyReference other = (ObjectTypePropertyReference) obj;
        if (objectType == null) {
            if (other.objectType != null)
                return false;
        } else if (!objectType.equals(other.objectType))
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
        return true;
    }
}
