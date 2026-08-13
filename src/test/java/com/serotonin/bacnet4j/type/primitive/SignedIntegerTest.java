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

public class SignedIntegerTest {
    /**
     * The value is held in either an int or a BigInteger depending on how the instance was created, and that is an
     * implementation detail that equality must not expose. Note that the long constructor uses the BigInteger field
     * whatever the magnitude, so this is not limited to large values.
     */
    @Test
    public void equalsIgnoresInternalRepresentation() {
        assertEquals(new SignedInteger(14), new SignedInteger(14L));
        assertEquals(new SignedInteger(14).hashCode(), new SignedInteger(14L).hashCode());

        assertEquals(new SignedInteger(-14), new SignedInteger(-14L));
        assertEquals(new SignedInteger(-14).hashCode(), new SignedInteger(-14L).hashCode());

        assertEquals(new SignedInteger(0), new SignedInteger(BigInteger.ZERO));
        assertEquals(new SignedInteger(0).hashCode(), new SignedInteger(BigInteger.ZERO).hashCode());
    }

    /**
     * A value outside the requested primitive type's range is clamped rather than truncated. Signed values clamp
     * at both ends.
     */
    @Test
    public void narrowingSaturatesInsteadOfTruncating() {
        SignedInteger aboveInt = new SignedInteger(new BigInteger("9223372036854775807"));
        assertEquals(Integer.MAX_VALUE, aboveInt.intValue());
        assertEquals(Long.MAX_VALUE, aboveInt.longValue());

        SignedInteger belowInt = new SignedInteger(new BigInteger("-9223372036854775808"));
        assertEquals(Integer.MIN_VALUE, belowInt.intValue());
        assertEquals(Long.MIN_VALUE, belowInt.longValue());

        SignedInteger aboveLong = new SignedInteger(new BigInteger("99999999999999999999"));
        assertEquals(Integer.MAX_VALUE, aboveLong.intValue());
        assertEquals(Long.MAX_VALUE, aboveLong.longValue());
        assertEquals(new BigInteger("99999999999999999999"), aboveLong.bigIntegerValue());

        SignedInteger belowLong = new SignedInteger(new BigInteger("-99999999999999999999"));
        assertEquals(Integer.MIN_VALUE, belowLong.intValue());
        assertEquals(Long.MIN_VALUE, belowLong.longValue());
    }

    /**
     * A value written and read back has to equal the original.
     */
    @Test
    public void roundTripEquality() throws BACnetException {
        for (int value : new int[] {0, 1, -1, 127, -128, 32767, -32768, 8388607, -8388608, Integer.MAX_VALUE,
                Integer.MIN_VALUE}) {
            SignedInteger original = new SignedInteger(value);
            ByteQueue queue = new ByteQueue();
            original.write(queue);
            SignedInteger parsed = new SignedInteger(queue);

            assertEquals("value " + value, value, parsed.intValue());
            assertEquals("value " + value, original, parsed);
            assertEquals("value " + value, original.hashCode(), parsed.hashCode());
        }
    }
}
