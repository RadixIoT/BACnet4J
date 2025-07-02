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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class RealTest {
    @Test
    public void nan() throws BACnetException {
        final Real r = new Real(Float.NaN);
        final ByteQueue queue = new ByteQueue();
        r.write(queue);

        final Real r2 = Encodable.read(queue, Real.class);
        assertEquals(Float.NaN, r2.floatValue(), 0);
    }

    @Test
    public void neginf() throws BACnetException {
        final Real r = new Real(Float.NEGATIVE_INFINITY);
        final ByteQueue queue = new ByteQueue();
        r.write(queue);

        final Real r2 = Encodable.read(queue, Real.class);
        assertEquals(Float.NEGATIVE_INFINITY, r2.floatValue(), 0);
    }

    @Test
    public void posinf() throws BACnetException {
        final Real r = new Real(Float.POSITIVE_INFINITY);
        final ByteQueue queue = new ByteQueue();
        r.write(queue);

        final Real r2 = Encodable.read(queue, Real.class);
        assertEquals(Float.POSITIVE_INFINITY, r2.floatValue(), 0);
    }
}
