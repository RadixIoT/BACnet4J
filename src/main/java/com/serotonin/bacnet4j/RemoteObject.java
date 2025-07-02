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

package com.serotonin.bacnet4j;

import java.io.Serializable;

import com.serotonin.bacnet4j.cache.RemoteEntityCache;
import com.serotonin.bacnet4j.cache.RemoteEntityCachePolicy;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class RemoteObject implements Serializable {
    private static final long serialVersionUID = 2962046198697775365L;

    private final ObjectIdentifier oid;
    private final RemoteEntityCache<PropertyIdentifier, Encodable> propertyCache;

    public RemoteObject(final LocalDevice localDevice, final ObjectIdentifier oid) {
        propertyCache = new RemoteEntityCache<>(localDevice);
        this.oid = oid;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return oid;
    }

    public String getObjectName() {
        final CharacterString cs = getProperty(PropertyIdentifier.objectName);
        if (cs == null)
            return null;
        return cs.getValue();
    }

    public <T extends Encodable> T getProperty(final PropertyIdentifier pid) {
        return getProperty(pid, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Encodable> T getProperty(final PropertyIdentifier pid, final UnsignedInteger pin) {
        final Encodable e = propertyCache.getCachedEntity(pid);
        if (pin == null)
            return (T) e;
        if (pin.intValue() == 0)
            // No length support
            return null;
        if (e instanceof SequenceOf)
            return (T) ((SequenceOf<Encodable>) e).getBase1(pin.intValue());
        // The property is not a sequence, but a pin was specified. Just return null.
        return null;
    }

    public void setProperty(final PropertyIdentifier pid, final Encodable value, final RemoteEntityCachePolicy policy) {
        setProperty(pid, null, value, policy);
    }

    @SuppressWarnings("unchecked")
    public void setProperty(final PropertyIdentifier pid, final UnsignedInteger pin, final Encodable value,
            final RemoteEntityCachePolicy policy) {
        if (value instanceof ErrorClassAndCode) {
            // Don't cache errors. In fact, remove whatever is currently there.
            // TODO this may not be a good thing to do for the same reason as why we don't remove remote objects.
            propertyCache.removeEntity(pid);
            return;
        }

        if (pin == null) {
            propertyCache.putEntity(pid, value, policy);
        } else {
            synchronized (propertyCache) {
                final Encodable e = propertyCache.getCachedEntity(pid);
                SequenceOf<Encodable> seq;
                if (e == null) {
                    // Cannot set the indexed property because we don't have the sequence. Even if we create a sequence
                    // and leave all other properties null, calls that ask for the whole list won't know that it is
                    // incomplete, so it's better to just not create it at all.
                    return;
                } else if (e instanceof SequenceOf) {
                    seq = (SequenceOf<Encodable>) e;
                } else {
                    // The property is not a sequence. Ignore.
                    return;
                }
                if (pin.intValue() > 0)
                    // No length support
                    seq.setBase1(pin.intValue(), value);
            }
        }
    }

    public <T extends Encodable> T removeProperty(final PropertyIdentifier pid) {
        return removeProperty(pid, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Encodable> T removeProperty(final PropertyIdentifier pid, final UnsignedInteger pin) {
        if (pin == null)
            return (T) propertyCache.removeEntity(pid);

        if (pin.intValue() == 0)
            // No length support
            return null;

        synchronized (propertyCache) {
            final Encodable e = propertyCache.getCachedEntity(pid);
            if (e == null)
                return null;
            if (!(e instanceof SequenceOf))
                throw new BACnetRuntimeException("Property " + pid + " of " + oid + " is not a sequence");

            final SequenceOf<Encodable> seq = (SequenceOf<Encodable>) e;
            return (T) seq.remove(pin.intValue());
        }
    }

    //    /**
    //     * Add properties that are in 'that' if they are not already in 'this'.
    //     *
    //     * @param that
    //     */
    //    public void merge(final RemoteObject that) {
    //        synchronized (properties) {
    //            for (final Map.Entry<PropertyIdentifier, Encodable> e : that.properties.entrySet()) {
    //                if (!properties.containsKey(e.getKey()) && e.getValue() != null) {
    //                    properties.put(e.getKey(), e.getValue());
    //                }
    //            }
    //        }
    //    }

    @Override
    public String toString() {
        return oid.toString();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (oid == null ? 0 : oid.hashCode());
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
        final RemoteObject other = (RemoteObject) obj;
        if (oid == null) {
            if (other.oid != null)
                return false;
        } else if (!oid.equals(other.oid))
            return false;
        return true;
    }
}
