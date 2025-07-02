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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest.RangeReadable;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SequenceOf<E extends Encodable> extends BaseType implements Iterable<E>, RangeReadable<E> {
    protected final List<E> values;

    public SequenceOf() {
        values = new ArrayList<>();
    }

    public SequenceOf(final int capacity) {
        values = new ArrayList<>(capacity);
    }

    public SequenceOf(final List<E> values) {
        this.values = values;
    }

    @SafeVarargs
    public SequenceOf(final E... values) {
        this();
        for (final E value : values)
            this.values.add(value);
    }

    @Override
    public void write(final ByteQueue queue) {
        for (final Encodable value : values)
            value.write(queue);
    }

    public SequenceOf(final ByteQueue queue, final Class<E> clazz) throws BACnetException {
        values = new ArrayList<>();
        while (peekTagNumber(queue) != -1)
            values.add(read(queue, clazz));
    }

    public SequenceOf(final ByteQueue queue, final int count, final Class<E> clazz) throws BACnetException {
        values = new ArrayList<>();
        int _count = count;
        while (_count-- > 0)
            values.add(read(queue, clazz));
    }

    public SequenceOf(final ByteQueue queue, final Class<E> clazz, final int contextId) throws BACnetException {
        values = new ArrayList<>();
        while (readEnd(queue) != contextId)
            values.add(read(queue, clazz));
    }

    public E getBase1(final int indexBase1) {
        return values.get(indexBase1 - 1);
    }

    @Override
    public E get(final int index) {
        return values.get(index);
    }

    public boolean has(final UnsignedInteger indexBase1) {
        final int index = indexBase1.intValue() - 1;
        return index >= 0 && index < values.size();
    }

    @Override
    public int size() {
        return values.size();
    }

    public int getCount() {
        return values.size();
    }

    public void setBase1(final int indexBase1, final E value) {
        final int index = indexBase1 - 1;
        while (values.size() <= index)
            values.add(null);
        values.set(index, value);
    }

    public void set(final int index, final E value) {
        values.set(index, value);
    }

    public void add(final E value) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == null) {
                values.set(i, value);
                return;
            }
        }
        values.add(value);
    }

    public Encodable remove(final int indexBase1) {
        final int index = indexBase1 - 1;
        if (index < values.size())
            return values.remove(index);
        return null;
    }

    public void remove(final E value) {
        if (value == null)
            return;

        for (int i = 0; i < values.size(); i++) {
            if (Objects.equals(values.get(i), value)) {
                remove(i + 1);
                break;
            }
        }
    }

    public void removeAll(final E value) {
        for (final ListIterator<E> it = values.listIterator(); it.hasNext(); ) {
            final E e = it.next();
            if (Objects.equals(e, value))
                it.remove();
        }
    }

    public boolean contains(final E value) {
        for (final E e : values) {
            if (Objects.equals(e, value))
                return true;
        }
        return false;
    }

    public int indexOf(final Object value) {
        return values.indexOf(value);
    }

    @Override
    public Iterator<E> iterator() {
        return values.iterator();
    }

    @Override
    public String toString() {
        return values.toString();
    }

    public List<E> getValues() {
        return values;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (values == null ? 0 : values.hashCode());
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
        final SequenceOf<?> other = (SequenceOf<?>) obj;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }
}
