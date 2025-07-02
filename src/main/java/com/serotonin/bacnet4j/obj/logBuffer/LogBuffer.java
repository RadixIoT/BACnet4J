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

import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest.RangeReadable;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Base class for all implementations of log buffers. The class extends Encodable so that it can be one of its host
 * object's properties, but the property is not network readable. It's elements, however, are network readable via the
 * ReadRange request.
 *
 * TODO a disk-based log buffer might be nice.
 *
 * @author Matthew
 */
abstract public class LogBuffer<E extends ILogRecord> extends Encodable implements RangeReadable<E> {
    @Override
    public void write(final ByteQueue queue) {
        throw new RuntimeException("not actually encodable");
    }

    @Override
    public void write(final ByteQueue queue, final int contextId) {
        throw new RuntimeException("not actually encodable");
    }

    /**
     * Returns the current size of the buffer.
     */
    @Override
    abstract public int size();

    /**
     * Clears the buffer of all of its records.
     */
    abstract public void clear();

    /**
     * Adds the given record to the buffer
     */
    abstract public void add(E record);

    /**
     * Removes the oldest record from the buffer, or does nothing if the buffer is empty.
     */
    abstract public void remove();

    /**
     * Returns the record at the given index where 0 is the oldest.
     */
    @Override
    abstract public E get(int index);
}
