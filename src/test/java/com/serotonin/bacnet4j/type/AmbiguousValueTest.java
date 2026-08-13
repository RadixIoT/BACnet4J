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

package com.serotonin.bacnet4j.type;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AmbiguousValueTest {
    @Test
    public void recognizesContextualNullAsNull() throws BACnetException {
        var content = new ByteQueue("4e004f");
        var amb = new AmbiguousValue(content, 4);
        assertTrue(amb.isNull());
        assertEquals(Null.instance, amb.convertTo(Null.class));
    }

    @Test
    public void recognizesContextualBlankAsNull() throws BACnetException {
        var content = new ByteQueue("4e4f");
        var amb = new AmbiguousValue(content, 4);
        assertTrue(amb.isNull());
    }

    @Test
    public void recognizesContextualIntAsNotNull() throws BACnetException {
        var content = new ByteQueue("ae210daf");
        var amb = new AmbiguousValue(content, 0xa);
        assertFalse(amb.isNull());
        assertEquals(new UnsignedInteger(13), amb.convertTo(UnsignedInteger.class));
    }

    /**
     * A corrupt 4-byte extended length used to wrap to zero or negative in TagData.getTotalLength, which made
     * copyAmbiguousData consume nothing and left the enclosing read loops spinning forever. Each vector below is
     * "3e" (context 3 start tag) followed by a tag whose declared length exceeds the remaining content.
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
     * The single argument constructor reaches the same read loop via the start tag branch.
     */
    @Test(timeout = 5_000)
    public void corruptExtendedLengthIsRejectedWithoutContextId() {
        var queue = new ByteQueue("3e25fff0000000");
        assertThrows(BACnetErrorException.class, () -> new AmbiguousValue(queue));
    }

    /**
     * An extended length is an arbitrary 32 bit number, so its low bits can imitate the start and end tag markers.
     * Such a tag must not be mistaken for a start or end tag, since that would consume the wrong number of bytes.
     */
    @Test(timeout = 5_000)
    public void extendedLengthIsNotMistakenForStartOrEndTag() {
        // 7 = tag number, d = context specific with extended length, ff = 4-byte length follows.
        // The low bits of f0000006 match the start tag marker, and those of f0000007 match the end tag marker.
        assertCorrupt("3e7dfff0000006", 3);
        assertCorrupt("3e7dfff0000007", 3);
    }

    /**
     * An end tag closing a context that was never opened is malformed rather than something to copy through.
     */
    @Test(timeout = 5_000)
    public void unbalancedEndTagIsRejected() {
        assertCorrupt("3e4f", 3);
    }

    /**
     * The matching end tag still terminates the read, leaving empty content.
     */
    @Test(timeout = 5_000)
    public void matchingEndTagTerminatesRead() throws BACnetException {
        var amb = new AmbiguousValue(new ByteQueue("3e3f"), 3);
        assertTrue(amb.isNull());
    }

    /**
     * Nested start and end tags are copied through intact.
     */
    @Test(timeout = 5_000)
    public void nestedStartAndEndTagsAreCopied() throws BACnetException {
        // 3e ( 5e ( 21 0d ) 5f ) 3f
        var amb = new AmbiguousValue(new ByteQueue("3e5e210d5f3f"), 3);
        assertArrayEquals(new byte[] {0x5e, 0x21, 0x0d, 0x5f}, amb.getData());
    }

    /**
     * A tag declaring a length of zero is legitimate: only the tag byte itself is consumed.
     */
    @Test(timeout = 5_000)
    public void zeroLengthTagIsNotCorrupt() throws BACnetException {
        var queue = new ByteQueue("3e203f");
        var amb = new AmbiguousValue(queue, 3);
        assertArrayEquals(new byte[] {0x20}, amb.getData());
    }

    private static void assertCorrupt(String hex, int contextId) {
        var queue = new ByteQueue(hex);
        assertThrows(hex, BACnetErrorException.class, () -> new AmbiguousValue(queue, contextId));
    }
}
