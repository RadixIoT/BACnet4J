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

package com.serotonin.bacnet4j.type.primitive;

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.util.BACnetUtils;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Double extends Primitive {
    public static final byte TYPE_ID = 5;

    private final double value;

    public Double(double value) {
        this.value = value;
    }

    public double doubleValue() {
        return value;
    }

    //
    // Reading and writing
    //
    public Double(ByteQueue queue) throws BACnetErrorException {
        // 135-2024 clause 20.2.7: eight contents octets.
        readTag(queue, TYPE_ID, 8, 8);
        value = java.lang.Double.longBitsToDouble(BACnetUtils.popLong(queue));
    }

    @Override
    public void writeImpl(ByteQueue queue) {
        BACnetUtils.pushLong(queue, java.lang.Double.doubleToLongBits(value));
    }

    @Override
    protected long getLength() {
        return 8;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Double aDouble = (Double) o;
        return java.lang.Double.compare(value, aDouble.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return java.lang.Double.toString(value);
    }
}
