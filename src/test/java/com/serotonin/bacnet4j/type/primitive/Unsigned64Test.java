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

package com.serotonin.bacnet4j.type.primitive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Unsigned64Test {
    private static final BigInteger MAX = new BigInteger("18446744073709551615");

    @Test
    public void zero() throws BACnetException {
        roundTrip(BigInteger.ZERO);
    }

    @Test
    public void smallInt() throws BACnetException {
        final Unsigned64 u = new Unsigned64(42);
        final ByteQueue queue = new ByteQueue();
        u.write(queue);

        final Unsigned64 u2 = new Unsigned64(queue);
        assertEquals(BigInteger.valueOf(42), u2.bigIntegerValue());
    }

    @Test
    public void above32Bit() throws BACnetException {
        // Above 32-bit but with non-zero low word; must round-trip cleanly through Unsigned64.
        roundTrip(BigInteger.ONE.shiftLeft(33).add(BigInteger.ONE));
    }

    @Test
    public void above63Bit() throws BACnetException {
        // 2^63 + 1: Java's signed long would flip negative here; BigInteger must carry it intact.
        roundTrip(BigInteger.ONE.shiftLeft(63).add(BigInteger.ONE));
    }

    @Test
    public void multipleOf2pow32() throws BACnetException {
        roundTrip(BigInteger.ONE.shiftLeft(33));
    }

    @Test
    public void maxValue() throws BACnetException {
        // 2^64 - 1, the largest legal Unsigned64 per BACnet spec
        roundTrip(MAX);
    }

    @Test
    public void constructorRejectsAboveMax() {
        assertThrows(IllegalArgumentException.class, () -> new Unsigned64(MAX.add(BigInteger.ONE)));
    }

    @Test
    public void validateAcceptsMax() throws BACnetServiceException {
        new Unsigned64(MAX).validate();
    }

    private static void roundTrip(BigInteger value) throws BACnetException {
        final Unsigned64 u = new Unsigned64(value);
        final ByteQueue queue = new ByteQueue();
        u.write(queue);

        final Unsigned64 u2 = new Unsigned64(queue);
        assertEquals(value, u2.bigIntegerValue());
    }
}
