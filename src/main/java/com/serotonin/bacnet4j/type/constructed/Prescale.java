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
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Prescale extends BaseType {
    private final UnsignedInteger multiplier;
    private final UnsignedInteger moduloDivide;

    public Prescale(final UnsignedInteger multiplier, final UnsignedInteger moduloDivide) {
        this.multiplier = multiplier;
        this.moduloDivide = moduloDivide;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, multiplier, 0);
        write(queue, moduloDivide, 1);
    }

    public Prescale(final ByteQueue queue) throws BACnetException {
        multiplier = read(queue, UnsignedInteger.class, 0);
        moduloDivide = read(queue, UnsignedInteger.class, 1);
    }

    public UnsignedInteger getMultiplier() {
        return multiplier;
    }

    public UnsignedInteger getModuloDivide() {
        return moduloDivide;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (moduloDivide == null ? 0 : moduloDivide.hashCode());
        result = PRIME * result + (multiplier == null ? 0 : multiplier.hashCode());
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
        final Prescale other = (Prescale) obj;
        if (moduloDivide == null) {
            if (other.moduloDivide != null)
                return false;
        } else if (!moduloDivide.equals(other.moduloDivide))
            return false;
        if (multiplier == null) {
            if (other.multiplier != null)
                return false;
        } else if (!multiplier.equals(other.multiplier))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Prescale [multiplier=" + multiplier + ", moduloDivide=" + moduloDivide + ']';
    }
}
