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
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Scale extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, Real.class);
        choiceOptions.addContextual(1, SignedInteger.class);
    }

    private final Choice scale;

    public Scale(final Real scale) {
        this.scale = new Choice(0, scale, choiceOptions);
    }

    public Scale(final SignedInteger scale) {
        this.scale = new Choice(1, scale, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, scale);
    }

    public Scale(final ByteQueue queue) throws BACnetException {
        scale = new Choice(queue, choiceOptions);
    }

    public Choice getScale() {
        return scale;
    }

    public boolean isReal() {
        return scale.getDatum() instanceof Real;
    }

    public boolean isSignedInteger() {
        return scale.getDatum() instanceof SignedInteger;
    }

    public Real getReal() {
        return (Real) scale.getDatum();
    }

    public SignedInteger getSignedInteger() {
        return (SignedInteger) scale.getDatum();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (scale == null ? 0 : scale.hashCode());
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
        final Scale other = (Scale) obj;
        if (scale == null) {
            if (other.scale != null)
                return false;
        } else if (!scale.equals(other.scale))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Scale [scale=" + scale + ']';
    }
}
