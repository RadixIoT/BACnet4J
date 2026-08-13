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

public class UnsignedIntegerTest {
    /**
     * The value is held in either an int or a BigInteger depending on how the instance was created, and that is an
     * implementation detail that equality must not expose. Note that the long constructor uses the BigInteger field
     * whatever the magnitude, so this is not limited to large values.
     */
    @Test
    public void equalsIgnoresInternalRepresentation() {
        assertEquals(new UnsignedInteger(14), new UnsignedInteger(14L));
        assertEquals(new UnsignedInteger(14).hashCode(), new UnsignedInteger(14L).hashCode());

        assertEquals(new UnsignedInteger(0), new UnsignedInteger(BigInteger.ZERO));
        assertEquals(new UnsignedInteger(0).hashCode(), new UnsignedInteger(BigInteger.ZERO).hashCode());

        assertEquals(new UnsignedInteger(0xFFFFFFFFL), new UnsignedInteger(new BigInteger("4294967295")));
    }

    /**
     * A value written and read back has to equal the original. The parser uses the BigInteger field for any value
     * encoded in four or more octets, so anything from 0x1000000 up takes a different representation than the int
     * constructor does.
     */
    @Test
    public void roundTripEquality() throws BACnetException {
        for (long value : new long[] {0, 1, 0xFF, 0x100, 0xFFFF, 0xFFFFFF, 0x1000000, 0x2000000, 0xFFFFFFFFL}) {
            UnsignedInteger original = new UnsignedInteger(value);
            ByteQueue queue = new ByteQueue();
            original.write(queue);
            UnsignedInteger parsed = new UnsignedInteger(queue);

            assertEquals("value " + value, value, parsed.longValue());
            assertEquals("value " + value, original, parsed);
            assertEquals("value " + value, original.hashCode(), parsed.hashCode());
        }
    }

    /**
     * A value too large for the requested primitive type is clamped rather than truncated. Truncating produced a
     * negative number for a value that cannot be negative, which inverts any comparison the caller makes against
     * it - see the sizes, counts and bounds derived from Unsigned32 properties.
     */
    @Test
    public void narrowingSaturatesInsteadOfTruncating() {
        // Unsigned32 maximum: fits in a long, not in an int.
        UnsignedInteger u32Max = new UnsignedInteger(0xFFFFFFFFL);
        assertEquals(Integer.MAX_VALUE, u32Max.intValue());
        assertEquals(0xFFFFFFFFL, u32Max.longValue());

        // Either side of the int boundary.
        assertEquals(Integer.MAX_VALUE, new UnsignedInteger(0x7FFFFFFF).intValue());
        assertEquals(Integer.MAX_VALUE, new UnsignedInteger(0x80000000L).intValue());
        assertEquals(0x80000000L, new UnsignedInteger(0x80000000L).longValue());

        // Unsigned64 maximum: fits in neither.
        UnsignedInteger u64Max = new UnsignedInteger(new BigInteger("18446744073709551615"));
        assertEquals(Integer.MAX_VALUE, u64Max.intValue());
        assertEquals(Long.MAX_VALUE, u64Max.longValue());

        // The exact value is always available.
        assertEquals(new BigInteger("18446744073709551615"), u64Max.bigIntegerValue());
    }

    @Test
    public void increment32() {
        UnsignedInteger i = new UnsignedInteger(0xFFFFFFFDL);
        assertEquals(0xFFFFFFFDL, i.longValue());

        i = i.increment32();
        assertEquals(0xFFFFFFFEL, i.longValue());

        i = i.increment32();
        assertEquals(0xFFFFFFFFL, i.longValue());

        i = i.increment32();
        assertEquals(0, i.longValue());

        i = i.increment32();
        assertEquals(1, i.longValue());
    }

    @Test
    public void increment16() {
        UnsignedInteger i = new UnsignedInteger(65533);
        assertEquals(65533, i.intValue());

        i = i.increment16();
        assertEquals(65534, i.intValue());

        i = i.increment16();
        assertEquals(65535, i.intValue());

        i = i.increment16();
        assertEquals(0, i.intValue());

        i = i.increment16();
        assertEquals(1, i.intValue());
    }
}
