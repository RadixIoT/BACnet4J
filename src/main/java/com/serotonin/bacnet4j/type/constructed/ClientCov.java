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
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ClientCov extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addPrimitive(Real.class);
        choiceOptions.addPrimitive(Null.class);
    }

    private final Choice entry;

    public ClientCov(final Real realIncrement) {
        entry = new Choice(realIncrement, choiceOptions);
    }

    public ClientCov(final Null defaultIncrement) {
        entry = new Choice(defaultIncrement, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, entry);
    }

    public ClientCov(final ByteQueue queue) throws BACnetException {
        entry = readChoice(queue, choiceOptions);
    }

    public boolean isRealIncrement() {
        return entry.isa(Real.class);
    }

    public boolean isDefaultIncrement() {
        return entry.isa(Null.class);
    }

    public Real getRealIncrement() {
        return (Real) entry.getDatum();
    }

    public Null getDefaultIncrement() {
        return (Null) entry.getDatum();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (entry == null ? 0 : entry.hashCode());
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
        final ClientCov other = (ClientCov) obj;
        if (entry == null) {
            if (other.entry != null)
                return false;
        } else if (!entry.equals(other.entry))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ClientCov [entry=" + entry + ']';
    }
}
