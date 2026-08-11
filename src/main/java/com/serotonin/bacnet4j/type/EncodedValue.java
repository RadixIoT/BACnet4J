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

package com.serotonin.bacnet4j.type;

import java.util.Arrays;
import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class EncodedValue extends Encodable {
    private final byte[] data;

    public EncodedValue(byte[] data) {
        this.data = data;
    }

    public EncodedValue(Encodable... sequence) {
        ByteQueue queue = new ByteQueue();
        for (Encodable e : sequence) {
            e.write(queue);
        }
        data = queue.popAll();
    }

    public EncodedValue(ByteQueue queue, int contextId) throws BACnetException {
        popStart(queue, contextId);

        var tagData = new TagData();
        var valueData = new ByteQueue();
        while (true) {
            tagData.peek(queue);
            if (tagData.isEndTag(contextId))
                break;
            readAmbiguousData(queue, tagData, valueData);
        }
        this.data = valueData.popAll();

        popEnd(queue, contextId);
    }

    @Override
    public void write(ByteQueue queue, int contextId) {
        writeContextTag(queue, contextId, true);
        queue.push(data);
        writeContextTag(queue, contextId, false);
    }

    @Override
    public void write(ByteQueue queue) {
        queue.push(data);
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Encoded(" + StreamUtils.dumpArrayHex(data) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        EncodedValue that = (EncodedValue) o;
        return Objects.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public void validate() throws BACnetServiceException {
        //Not necessary
    }
}
