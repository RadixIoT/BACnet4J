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

package com.serotonin.bacnet4j.type;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AmbiguousValueTest {
    /**
     * A corrupt 4-byte extended length used to wrap to zero or negative in TagData.getTotalLength, which made
     * copyData consume nothing and left the read loop below spinning forever. The timeouts on these tests are the
     * regression check: before the fix they never returned.
     * <p>
     * Each vector is "3e" (context 3 start tag) followed by a tag whose declared length exceeds the content that
     * follows it, so the copy length guard rejects it.
     */
    @Test(timeout = 5_000)
    public void corruptExtendedLengthIsRejected() {
        // 2 = tag number, 5 = extended length, ff = 4-byte length follows.
        // The lowest and highest lengths that wrapped to a non-positive int, plus one that wrapped to 1.
        assertCorrupt("3e25ff7ffffffa", 3);
        assertCorrupt("3e25fffffffffa", 3);
        assertCorrupt("3e25fffffffffb", 3);
        // Same, but context specific rather than application class.
        assertCorrupt("3e2dfff0000000", 3);
        // f5 = extended tag number, which makes the tag header one byte longer and shifts the wrap point.
        assertCorrupt("3ef550ff7ffffff9", 3);
        assertCorrupt("3ef550fffffffff9", 3);
        // A large length that never wrapped, but still overruns the queue.
        assertCorrupt("3e25ff7ffffff9", 3);
    }

    /**
     * The single argument constructor reaches the same read loop through the start tag branch.
     */
    @Test(timeout = 5_000)
    public void corruptExtendedLengthIsRejectedWithoutContextId() {
        ByteQueue queue = new ByteQueue("3e25fff0000000");
        assertThrows(BACnetRuntimeException.class, () -> new AmbiguousValue(queue));
    }

    /**
     * A tag declaring a length of zero is legitimate: only the tag byte itself is consumed. This guards against the
     * copy length check being over-broad.
     */
    @Test(timeout = 5_000)
    public void zeroLengthTagIsNotCorrupt() throws BACnetException {
        AmbiguousValue amb = new AmbiguousValue(new ByteQueue("3e203f"), 3);
        assertArrayEquals(new byte[] { 0x20 }, amb.getData());
    }

    /**
     * A length that exactly consumes the remaining content is legitimate.
     */
    @Test(timeout = 5_000)
    public void exactLengthIsNotCorrupt() throws BACnetException {
        AmbiguousValue amb = new AmbiguousValue(new ByteQueue("3e210d3f"), 3);
        assertArrayEquals(new byte[] { 0x21, 0x0d }, amb.getData());
        assertEquals(new UnsignedInteger(13), amb.convertTo(UnsignedInteger.class));
    }

    /**
     * The matching end tag terminates the read immediately, leaving no content.
     * <p>
     * Note that isNull() cannot be called on the result on this branch: it dereferences data without a null check,
     * so an empty ambiguous value throws NullPointerException. That is a pre-existing defect, unrelated to the copy
     * length fix, so this test asserts the parsed state directly instead.
     */
    @Test(timeout = 5_000)
    public void matchingEndTagTerminatesRead() throws BACnetException {
        AmbiguousValue amb = new AmbiguousValue(new ByteQueue("3e3f"), 3);
        assertNull(amb.getData());
    }

    /**
     * Several values under one start tag are all accumulated.
     */
    @Test(timeout = 5_000)
    public void multipleValuesAreAccumulated() throws BACnetException {
        AmbiguousValue amb = new AmbiguousValue(new ByteQueue("3e210d21173f"), 3);
        assertArrayEquals(new byte[] { 0x21, 0x0d, 0x21, 0x17 }, amb.getData());
    }

    private static void assertCorrupt(String hex, int contextId) {
        ByteQueue queue = new ByteQueue(hex);
        assertThrows(hex, BACnetRuntimeException.class, () -> new AmbiguousValue(queue, contextId));
    }
}
