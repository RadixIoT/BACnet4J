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

package com.serotonin.bacnet4j.obj.logBuffer;

import java.util.LinkedList;

import com.serotonin.bacnet4j.exception.BACnetServiceException;

public class LinkedListLogBuffer<T extends ILogRecord> extends LogBuffer<T> {
    private final LinkedList<T> list = new LinkedList<>();

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public void add(final T record) {
        list.add(record);
    }

    @Override
    public void remove() {
        list.removeFirst();
    }

    @Override
    public T get(final int index) {
        return list.get(index);
    }

    @Override
    public String toString() {
        return "LinkedListLogBuffer" + list;
    }

    @Override
    public void validate() throws BACnetServiceException {
        //Not written, validation not necessary
    }
}
