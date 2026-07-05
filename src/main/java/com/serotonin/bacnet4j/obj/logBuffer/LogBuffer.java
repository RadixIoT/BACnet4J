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
 * <p>
 * A disk-based log buffer might be nice.
 */
public abstract class LogBuffer<E extends ILogRecord> extends Encodable implements RangeReadable<E> {
    @Override
    public void write(ByteQueue queue) {
        throw new RuntimeException("not actually encodable");
    }

    @Override
    public void write(ByteQueue queue, int contextId) {
        throw new RuntimeException("not actually encodable");
    }

    /**
     * Clears the buffer of all of its records.
     */
    public abstract void clear();

    /**
     * Adds the given record to the buffer
     */
    public abstract void add(E rec);

    /**
     * Removes the oldest record from the buffer, or does nothing if the buffer is empty.
     */
    public abstract void remove();

    /**
     * Reports whether the buffer has room for another record. Default implementation returns true — the in-memory
     * buffer is bounded only by JVM heap. Storage-backed implementations override to signal exhaustion, which drives
     * the LOG_BUFFER_FULL / discard-oldest behavior when Buffer_Size is the reserved sentinel value 2^32-1 per
     * addendum 135-2016bi-3.
     */
    public boolean hasSpaceForAnotherRecord() {
        return true;
    }
}
