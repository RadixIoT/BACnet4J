/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.npdu.sc;

import java.util.Arrays;
import java.util.Objects;

import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public abstract class SCId {
    private final byte[] bytes;

    protected SCId(byte[] bytes) {
        if (bytes.length != size()) {
            throw new IllegalArgumentException("Invalid array length given");
        }
        this.bytes = bytes;
    }

    protected SCId(ByteQueue queue) {
        bytes = new byte[size()];
        int len = queue.pop(bytes);
        if (len != size()) {
            throw new IllegalArgumentException("Unable to read required length from queue");
        }
    }

    public void write(ByteQueue queue) {
        queue.push(bytes);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public OctetString getOctetString() {
        return new OctetString(bytes);
    }

    protected abstract int size();

    @Override
    public String toString() {
        return StreamUtils.toHex(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        SCId scId = (SCId) o;
        return Objects.deepEquals(bytes, scId.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
