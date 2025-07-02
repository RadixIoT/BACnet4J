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
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class NameValue extends BaseType {
    private final CharacterString name;
    private final Encodable value;

    public NameValue(final String name) {
        this(new CharacterString(name));
    }

    public NameValue(final CharacterString name) {
        this.name = name;
        this.value = null;
    }

    public NameValue(final String name, final Primitive value) {
        this(new CharacterString(name), value);
    }

    public NameValue(final CharacterString name, final Primitive value) {
        this.name = name;
        this.value = value;
    }

    public NameValue(final String name, final DateTime value) {
        this(new CharacterString(name), value);
    }

    public NameValue(final CharacterString name, final DateTime value) {
        this.name = name;
        this.value = value;
    }

    public NameValue(final ByteQueue queue) throws BACnetException {
        name = read(queue, CharacterString.class, 0);

        // Reading the optional ANY
        Encodable e = null;
        if (queue.size() > 0) {
            if (!isContextTag(queue)) {
                final int typeId = Primitive.getPrimitiveTypeId(queue.peek(0));
                if (typeId == Date.TYPE_ID) {
                    // Read the date and then check if there is a time following.
                    final Date d = new Date(queue);
                    if (queue.size() > 0 && Primitive.getPrimitiveTypeId(queue.peek(0)) == Time.TYPE_ID) {
                        final Time t = new Time(queue);
                        e = new DateTime(d, t);
                    } else {
                        e = d;
                    }
                } else if (typeId != -1) {
                    e = Primitive.createPrimitive(queue);
                }
            }
        }
        value = e;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, name, 0);
        writeOptional(queue, value);
    }

    public CharacterString getName() {
        return name;
    }

    public Encodable getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "NameValue [name=" + name + ", value=" + value + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (value == null ? 0 : value.hashCode());
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
        final NameValue other = (NameValue) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}
