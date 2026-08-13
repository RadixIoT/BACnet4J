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

import java.math.BigInteger;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class EnumeratedTest {
    /**
     * The value is held in either an int or a BigInteger depending on how the instance was created, and that is an
     * implementation detail that equality must not expose.
     */
    @Test
    public void equalsIgnoresInternalRepresentation() {
        assertEquals(new Enumerated(5), new Enumerated(BigInteger.valueOf(5)));
        assertEquals(new Enumerated(5).hashCode(), new Enumerated(BigInteger.valueOf(5)).hashCode());

        assertEquals(new Enumerated(0), new Enumerated(BigInteger.ZERO));
        assertEquals(new Enumerated(0).hashCode(), new Enumerated(BigInteger.ZERO).hashCode());
    }

    /**
     * A value too large for an int is clamped rather than truncated.
     */
    @Test
    public void narrowingSaturatesInsteadOfTruncating() {
        Enumerated e = new Enumerated(new BigInteger("4294967295"));
        assertEquals(Integer.MAX_VALUE, e.intValue());
        assertEquals(new BigInteger("4294967295"), e.bigIntegerValue());
    }

    /**
     * getLength has to reflect the value, not its low order bits. Any multiple of 2^32 has an intValue of zero, so
     * testing that instead reported a length of 1 for a value needing five octets, and writing it then ran off the
     * end of the buffer.
     */
    @Test
    public void lengthOfValueWhoseLowOrderBitsAreZero() throws BACnetException {
        Enumerated original = new Enumerated(BigInteger.valueOf(1L << 32));

        ByteQueue queue = new ByteQueue();
        original.write(queue);

        assertEquals(original, new Enumerated(queue));
    }

    /**
     * A value written and read back has to equal the original. The parser uses the BigInteger field for any value
     * encoded in four or more octets, so anything from 0x1000000 up takes a different representation than the int
     * constructor does.
     */
    @Test
    public void roundTripEquality() throws BACnetException {
        for (int value : new int[] {0, 1, 0xFF, 0x100, 0xFFFF, 0xFFFFFF, 0x1000000, 0x2000000, Integer.MAX_VALUE}) {
            Enumerated original = new Enumerated(value);
            ByteQueue queue = new ByteQueue();
            original.write(queue);
            Enumerated parsed = new Enumerated(queue);

            assertEquals("value " + value, value, parsed.intValue());
            assertEquals("value " + value, original, parsed);
            assertEquals("value " + value, original.hashCode(), parsed.hashCode());
        }
    }
}
