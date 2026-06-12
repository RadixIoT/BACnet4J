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

package com.serotonin.bacnet4j.type.constructed;

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class StageLimitValue extends BaseType {
    private final Real limit;
    private final BitString values;
    private final Real deadband;

    public StageLimitValue(final Real limit, BitString values, Real deadband) {
        this.limit = limit;
        this.values = values;
        this.deadband = deadband;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, limit);
        write(queue, values);
        write(queue, deadband);
    }

    @Override
    public String toString() {
        return "StageLimitValue [" +
                "limit=" + limit +
                ", values=" + values +
                ", deadband=" + deadband +
                ']';
    }

    public Real getLimit() {
        return limit;
    }

    public BitString getValues() {
        return values;
    }

    public Real getDeadband() {
        return deadband;
    }

    public StageLimitValue(final ByteQueue queue) throws BACnetException {
        limit = read(queue, Real.class);
        values = read(queue, BitString.class);
        deadband = read(queue, Real.class);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        StageLimitValue that = (StageLimitValue) o;
        return Objects.equals(limit, that.limit) && Objects.equals(values,
                that.values) && Objects.equals(deadband, that.deadband);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, values, deadband);
    }
}
