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

package com.serotonin.bacnet4j.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.serotonin.bacnet4j.exception.PropertyValueException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class PropertyValues implements Iterable<ObjectPropertyReference> {
    private final Map<ObjectPropertyReference, Encodable> values = new HashMap<>();

    public void add(final ObjectIdentifier oid, final PropertyIdentifier pid, final UnsignedInteger pin,
            final Encodable value) {
        values.put(new ObjectPropertyReference(oid, pid, pin), value);
    }

    public Encodable getNoErrorCheck(final ObjectPropertyReference opr) {
        return values.get(opr);
    }

    public Encodable get(final ObjectPropertyReference opr) throws PropertyValueException {
        final Encodable e = getNoErrorCheck(opr);

        if (e instanceof ErrorClassAndCode)
            throw new PropertyValueException((ErrorClassAndCode) e);

        return e;
    }

    public Encodable getNoErrorCheck(final ObjectIdentifier oid, final PropertyReference ref) {
        return getNoErrorCheck(
                new ObjectPropertyReference(oid, ref.getPropertyIdentifier(), ref.getPropertyArrayIndex()));
    }

    public Encodable getNoErrorCheck(final ObjectIdentifier oid, final PropertyIdentifier pid) {
        return getNoErrorCheck(new ObjectPropertyReference(oid, pid));
    }

    public Encodable get(final ObjectIdentifier oid, final PropertyIdentifier pid) throws PropertyValueException {
        return get(new ObjectPropertyReference(oid, pid));
    }

    public Encodable getNullOnError(final ObjectIdentifier oid, final PropertyIdentifier pid) {
        return getNullOnError(getNoErrorCheck(new ObjectPropertyReference(oid, pid)));
    }

    public String getString(final ObjectIdentifier oid, final PropertyIdentifier pid) {
        return getString(getNoErrorCheck(oid, pid));
    }

    public String getString(final ObjectIdentifier oid, final PropertyIdentifier pid, final String defaultValue) {
        return getString(getNoErrorCheck(oid, pid), defaultValue);
    }

    @Override
    public Iterator<ObjectPropertyReference> iterator() {
        return values.keySet().iterator();
    }

    public static String getString(final Encodable value) {
        if (value == null)
            return null;
        return value.toString();
    }

    public static String getString(final Encodable value, final String defaultValue) {
        if (value == null || value instanceof ErrorClassAndCode)
            return defaultValue;
        return value.toString();
    }

    public static Encodable getNullOnError(final Encodable value) {
        if (value instanceof ErrorClassAndCode)
            return null;
        return value;
    }

    public int size() {
        return values.size();
    }

    @Override
    public String toString() {
        return "PropertyValues [values=" + values + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + values.hashCode();
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
        final PropertyValues other = (PropertyValues) obj;
        return values.equals(other.values);
    }
}
