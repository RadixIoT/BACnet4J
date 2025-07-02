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
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class KeyIdentifier extends BaseType {
    private final Unsigned8 algorithm;
    private final Unsigned8 keyId;

    public KeyIdentifier(final Unsigned8 algorithm, final Unsigned8 keyId) {
        this.algorithm = algorithm;
        this.keyId = keyId;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, algorithm);
        write(queue, keyId);
    }

    public KeyIdentifier(final ByteQueue queue) throws BACnetException {
        algorithm = read(queue, Unsigned8.class, 0);
        keyId = read(queue, Unsigned8.class, 1);
    }

    public Unsigned8 getAlgorithm() {
        return algorithm;
    }

    public Unsigned8 getKeyId() {
        return keyId;
    }

    @Override
    public String toString() {
        return "KeyIdentifier [algorithm=" + algorithm + ", keyId=" + keyId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (algorithm == null ? 0 : algorithm.hashCode());
        result = prime * result + (keyId == null ? 0 : keyId.hashCode());
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
        final KeyIdentifier other = (KeyIdentifier) obj;
        if (algorithm == null) {
            if (other.algorithm != null)
                return false;
        } else if (!algorithm.equals(other.algorithm))
            return false;
        if (keyId == null) {
            if (other.keyId != null)
                return false;
        } else if (!keyId.equals(other.keyId))
            return false;
        return true;
    }
}
