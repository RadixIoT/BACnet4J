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
import com.serotonin.bacnet4j.type.ThreadLocalObjectTypeStack;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class WriteAccessSpecification extends BaseType {
    private final ObjectIdentifier objectIdentifier;
    private final SequenceOf<PropertyValue> listOfProperties;

    public WriteAccessSpecification(final ObjectIdentifier objectIdentifier,
            final SequenceOf<PropertyValue> listOfProperties) {
        this.objectIdentifier = objectIdentifier;
        this.listOfProperties = listOfProperties;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, listOfProperties, 1);
    }

    public WriteAccessSpecification(final ByteQueue queue) throws BACnetException {
        objectIdentifier = read(queue, ObjectIdentifier.class, 0);
        try {
            ThreadLocalObjectTypeStack.set(objectIdentifier.getObjectType());
            listOfProperties = readSequenceOf(queue, PropertyValue.class, 1);
        } finally {
            ThreadLocalObjectTypeStack.remove();
        }
    }

    public SequenceOf<PropertyValue> getListOfProperties() {
        return listOfProperties;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public int size() {
        return listOfProperties.getCount();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (listOfProperties == null ? 0 : listOfProperties.hashCode());
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
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
        final WriteAccessSpecification other = (WriteAccessSpecification) obj;
        if (listOfProperties == null) {
            if (other.listOfProperties != null)
                return false;
        } else if (!listOfProperties.equals(other.listOfProperties))
            return false;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "WriteAccessSpecification [objectIdentifier=" + objectIdentifier + ", listOfProperties=" + listOfProperties + ']';
    }
}
