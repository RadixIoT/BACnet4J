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

import java.util.LinkedHashMap;
import java.util.Map;

import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class DeviceObjectPropertyReferences {
    private final Map<Integer, PropertyReferences> properties = new LinkedHashMap<>();

    public DeviceObjectPropertyReferences add(final int deviceId, final ObjectType objectType, final int objectNumber,
            final PropertyIdentifier... pids) {
        return add(deviceId, new ObjectIdentifier(objectType, objectNumber), pids);
    }

    public DeviceObjectPropertyReferences add(final int deviceId, final ObjectIdentifier oid,
            final PropertyIdentifier... pids) {
        final PropertyReferences refs = getDeviceProperties(deviceId);
        for (final PropertyIdentifier pid : pids)
            refs.add(oid, pid);
        return this;
    }

    public DeviceObjectPropertyReferences addIndex(final int deviceId, final ObjectType objectType,
            final int objectNumber, final PropertyIdentifier pid, final int pin) {
        return addIndex(deviceId, objectType, objectNumber, pid, new UnsignedInteger(pin));
    }

    public DeviceObjectPropertyReferences addIndex(final int deviceId, final ObjectType objectType,
            final int objectNumber, final PropertyIdentifier pid, final UnsignedInteger pin) {
        return addIndex(deviceId, new ObjectIdentifier(objectType, objectNumber), pid, pin);
    }

    public DeviceObjectPropertyReferences addIndex(final int deviceId, final ObjectIdentifier oid,
            final PropertyIdentifier pid, final UnsignedInteger pin) {
        final PropertyReferences refs = getDeviceProperties(deviceId);
        refs.addIndex(oid, pid, pin);
        return this;
    }

    public DeviceObjectPropertyReferences add(final int deviceId, final PropertyReferences refs) {
        final PropertyReferences existing = properties.get(deviceId);
        if (existing == null)
            properties.put(deviceId, refs);
        else
            existing.add(refs);
        return this;
    }

    public PropertyReferences getDeviceProperties(final Integer deviceId) {
        PropertyReferences refs = properties.get(deviceId);
        if (refs == null) {
            refs = new PropertyReferences();
            properties.put(deviceId, refs);
        }
        return refs;
    }

    public Map<Integer, PropertyReferences> getProperties() {
        return properties;
    }

    public int size() {
        int size = 0;
        for (final PropertyReferences refs : properties.values())
            size += refs.size();
        return size;
    }

    public void clear() {
        properties.clear();
    }

    @Override
    public String toString() {
        return "DeviceObjectPropertyReferences [properties=" + properties + "]";
    }
}
