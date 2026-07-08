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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest;
import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest.RangeReadable;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SequenceOf<E extends Encodable> extends BaseType implements Iterable<E>, RangeReadable<E> {
    protected final List<E> values;

    public SequenceOf() {
        values = new ArrayList<>();
    }

    public SequenceOf(int capacity) {
        values = new ArrayList<>(capacity);
    }

    public SequenceOf(List<E> values) {
        this.values = values;
    }

    @SafeVarargs
    public SequenceOf(E... values) {
        this();
        Collections.addAll(this.values, values);
    }

    @Override
    public void write(ByteQueue queue) {
        for (Encodable value : values)
            value.write(queue);
    }

    public SequenceOf(ByteQueue queue, Class<E> clazz) throws BACnetException {
        values = new ArrayList<>();
        while (peekTagNumber(queue) != -1)
            values.add(read(queue, clazz));
    }

    public SequenceOf(ByteQueue queue, int count, Class<E> clazz) throws BACnetException {
        values = new ArrayList<>();
        int rem = count;
        while (rem-- > 0)
            values.add(read(queue, clazz));
    }

    public SequenceOf(ByteQueue queue, Class<E> clazz, int contextId) throws BACnetException {
        values = new ArrayList<>();
        while (readEnd(queue) != contextId)
            values.add(read(queue, clazz));
    }

    public E getBase1(int indexBase1) {
        if (indexBase1 < 0 || indexBase1 > values.size()) {
            throw new IndexOutOfBoundsException(
                    "Base 1 index %s out of bounds for length %s".formatted(indexBase1, values.size()));
        }
        return values.get(indexBase1 - 1);
    }

    @Override
    public E get(int index) {
        return values.get(index);
    }

    public boolean has(UnsignedInteger indexBase1) {
        int index = indexBase1.intValue() - 1;
        return index >= 0 && index < values.size();
    }

    @Override
    public int size() {
        return values.size();
    }

    public int getCount() {
        return values.size();
    }

    public void setBase1(int indexBase1, E value) {
        int index = indexBase1 - 1;
        while (values.size() <= index)
            values.add(null);
        values.set(index, value);
    }

    public void set(int index, E value) {
        values.set(index, value);
    }

    public void add(E value) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == null) {
                values.set(i, value);
                return;
            }
        }
        values.add(value);
    }

    public Encodable remove(int indexBase1) {
        int index = indexBase1 - 1;
        if (index < values.size())
            return values.remove(index);
        return null;
    }

    public void remove(E value) {
        if (value == null)
            return;

        for (int i = 0; i < values.size(); i++) {
            if (Objects.equals(values.get(i), value)) {
                remove(i + 1);
                break;
            }
        }
    }

    public void removeAll(E value) {
        values.removeIf(e -> Objects.equals(e, value));
    }

    public boolean contains(E value) {
        for (E e : values) {
            if (Objects.equals(e, value))
                return true;
        }
        return false;
    }

    public int indexOf(E value) {
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

    public Stream<E> stream() {
        return values.stream();
    }

    @Override
    public void inSynchronizedBlock(ReadRangeRequest.BACnetServiceRunnable task) throws BACnetServiceException {
        synchronized (this) {
            task.run();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        SequenceOf<?> that = (SequenceOf<?>) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(values);
    }
}
