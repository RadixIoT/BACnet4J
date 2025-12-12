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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class PropertyReferenceValues {
    private final Map<ObjectIdentifier, List<PropertyValue>> properties = new LinkedHashMap<>();

    public PropertyReferenceValues() {
        // no op
    }

    //    public PropertyReferenceValues(final List<PropertyReferenceValues> partitions) {
    //        for (final PropertyReferenceValues that : partitions) {
    //            for (final Map.Entry<ObjectIdentifier, List<PropertyReference>> e : that.getProperties().entrySet()) {
    //                for (final PropertyReference ref : e.getValue()) {
    //                    add(e.getKey(), ref);
    //                }
    //            }
    //        }
    //    }

    public PropertyReferenceValues add(final ObjectType objectType, final int objectNumber,
            final PropertyValue... values) {
        return add(new ObjectIdentifier(objectType, objectNumber), values);
    }

    public PropertyReferenceValues add(final ObjectIdentifier oid, final PropertyValue... values) {
        final List<PropertyValue> list = getOidList(oid);
        for (final PropertyValue value : values)
            list.add(value);
        return this;
    }

    //    public PropertyReferenceValues add(final ObjectType objectType, final int objectNumber,
    //            final PropertyIdentifier... pids) {
    //        return add(new ObjectIdentifier(objectType, objectNumber), pids);
    //    }
    //
    //    public PropertyReferenceValues add(final ObjectIdentifier oid, final PropertyIdentifier... pids) {
    //        final List<PropertyReference> list = getOidList(oid);
    //        for (final PropertyIdentifier pid : pids)
    //            list.add(new PropertyReference(pid));
    //        return this;
    //    }
    //
    //    public PropertyReferenceValues addIndex(final ObjectIdentifier oid, final PropertyIdentifier pid,
    //            final UnsignedInteger propertyArrayIndex) {
    //        final List<PropertyReference> list = getOidList(oid);
    //        list.add(new PropertyReference(pid, propertyArrayIndex));
    //        return this;
    //    }

    public PropertyReferenceValues add(final PropertyReferenceValues that) {
        for (final Map.Entry<ObjectIdentifier, List<PropertyValue>> e : that.properties.entrySet()) {
            final List<PropertyValue> existing = properties.get(e.getKey());
            if (existing == null)
                properties.put(e.getKey(), e.getValue());
            else {
                for (final PropertyValue value : e.getValue()) {
                    if (!existing.contains(value))
                        existing.add(value);
                }
            }
        }
        return this;
    }

    private List<PropertyValue> getOidList(final ObjectIdentifier oid) {
        List<PropertyValue> list = properties.get(oid);
        if (list == null) {
            list = new ArrayList<>();
            properties.put(oid, list);
        }
        return list;
    }

    public Map<ObjectIdentifier, List<PropertyValue>> getProperties() {
        return properties;
    }

    //    public List<PropertyReferenceValues> getPropertiesPartitioned(final int maxPartitionSize) {
    //        final List<PropertyReferenceValues> partitions = new ArrayList<>();
    //
    //        if (size() <= maxPartitionSize)
    //            partitions.add(this);
    //        else {
    //            PropertyReferenceValues partition = null;
    //            List<PropertyReference> refs;
    //            for (final ObjectIdentifier oid : properties.keySet()) {
    //                refs = properties.get(oid);
    //                for (final PropertyReference ref : refs) {
    //                    if (partition == null || partition.size() >= maxPartitionSize) {
    //                        partition = new PropertyReferenceValues();
    //                        partitions.add(partition);
    //                    }
    //                    partition.add(oid, ref);
    //                }
    //            }
    //        }
    //
    //        return partitions;
    //    }

    public int size() {
        int size = 0;
        for (final List<PropertyValue> list : properties.values())
            size += list.size();
        return size;
    }
}
