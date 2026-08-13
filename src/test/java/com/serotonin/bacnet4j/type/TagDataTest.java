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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class TagDataTest {
    @Test
    public void applicationTagWithLiteralLength() throws BACnetErrorException {
        TagData tagData = new TagData().peek(new ByteQueue("210d"));

        assertEquals(2, tagData.getTagNumber());
        assertFalse(tagData.isContextSpecific());
        assertEquals(1, tagData.getLvt());
        assertEquals(1, tagData.getLength());
        assertEquals(1, tagData.getTagLength());
        assertEquals(2, tagData.getTotalLength());
    }

    @Test
    public void extendedTagNumberLengthensTheHeader() throws BACnetErrorException {
        // f = extended tag number, so the tag number is taken from the second byte.
        TagData tagData = new TagData().peek(new ByteQueue("f55002"));

        assertEquals(0x50, tagData.getTagNumber());
        assertEquals(2, tagData.getLength());
        assertEquals(3, tagData.getTagLength());
    }

    @Test
    public void twoByteExtendedLength() throws BACnetErrorException {
        // 5 = extended length, fe = 2-byte length follows.
        TagData tagData = new TagData().peek(new ByteQueue("25fe1234"));

        assertEquals(TagData.LVT_EXTENDED_LENGTH, tagData.getLvt());
        assertEquals(0x1234, tagData.getLength());
        assertEquals(4, tagData.getTagLength());
    }

    /**
     * A 4-byte extended length can hold values that do not fit in a signed int. getTotalLength must report the
     * decoded length without truncating it, otherwise callers see a non-positive length and consume nothing.
     */
    @Test
    public void fourByteExtendedLengthIsNotTruncated() throws BACnetErrorException {
        // 7 = tag number, 5 = extended length, ff = 4-byte length follows.
        TagData tagData = new TagData().peek(new ByteQueue("75fff0000000"));

        assertEquals(7, tagData.getTagNumber());
        assertFalse(tagData.isContextSpecific());
        assertEquals(0xf0000000L, tagData.getLength());
        assertEquals(6, tagData.getTagLength());
        assertEquals(0xf0000000L + 6, tagData.getTotalLength());
    }

    /**
     * The largest encodable length must still yield a positive total, since a non-positive one stalls the read
     * loops that consume getTotalLength bytes at a time.
     */
    @Test
    public void maximumExtendedLengthStaysPositive() throws BACnetErrorException {
        TagData tagData = new TagData().peek(new ByteQueue("75ffffffffff"));

        assertEquals(0xffffffffL, tagData.getLength());
        assertEquals(0xffffffffL + 6, tagData.getTotalLength());
        assertTrue(tagData.getTotalLength() > 0);
    }

    /**
     * A start tag is not an end tag and vice versa. The two markers previously overlapped, so an end tag also
     * reported itself as a start tag.
     */
    @Test
    public void startAndEndTagsAreDistinct() throws BACnetErrorException {
        TagData tagData = new TagData();

        tagData.peek(new ByteQueue("3e"));
        assertEquals(TagData.LVT_START_TAG, tagData.getLvt());
        assertTrue(tagData.isStartTag());
        assertTrue(tagData.isStartTag(3));
        assertFalse(tagData.isStartTag(4));
        assertFalse(tagData.isEndTag());

        tagData.peek(new ByteQueue("3f"));
        assertEquals(TagData.LVT_END_TAG, tagData.getLvt());
        assertTrue(tagData.isEndTag());
        assertTrue(tagData.isEndTag(3));
        assertFalse(tagData.isEndTag(4));
        assertFalse(tagData.isStartTag());
    }

    /**
     * The markers are only meaningful for context specific tags. For an application class tag the same lvt values
     * are ordinary lengths.
     */
    @Test
    public void applicationTagIsNeverAStartOrEndTag() throws BACnetErrorException {
        TagData tagData = new TagData();

        tagData.peek(new ByteQueue("26"));
        assertEquals(TagData.LVT_START_TAG, tagData.getLvt());
        assertFalse(tagData.isContextSpecific());
        assertFalse(tagData.isStartTag());
        assertFalse(tagData.isEndTag());

        tagData.peek(new ByteQueue("27"));
        assertEquals(TagData.LVT_END_TAG, tagData.getLvt());
        assertFalse(tagData.isStartTag());
        assertFalse(tagData.isEndTag());
    }

    /**
     * Start and end tags are identified by the raw lvt field, so an extended length whose low bits happen to match
     * those markers must not be reported as a start or end tag.
     */
    @Test
    public void extendedLengthIsNotMistakenForAMarker() throws BACnetErrorException {
        TagData tagData = new TagData();

        // 7 = tag number, d = context specific with extended length, ff = 4-byte length follows.
        // The low bits of f0000006 match the start tag marker, and those of f0000007 match the end tag marker.
        tagData.peek(new ByteQueue("7dfff0000006"));
        assertEquals(TagData.LVT_EXTENDED_LENGTH, tagData.getLvt());
        assertTrue(tagData.isContextSpecific());
        assertEquals(0xf0000006L, tagData.getLength());
        assertFalse(tagData.isStartTag());
        assertFalse(tagData.isEndTag());

        tagData.peek(new ByteQueue("7dfff0000007"));
        assertEquals(0xf0000007L, tagData.getLength());
        assertFalse(tagData.isStartTag());
        assertFalse(tagData.isEndTag());
    }

    @Test
    public void peekLeavesTheQueueIntact() throws BACnetErrorException {
        ByteQueue queue = new ByteQueue("25fe1234aabb");
        TagData tagData = new TagData().peek(queue);

        assertEquals(4, tagData.getTagLength());
        assertEquals(6, queue.size());
    }

    @Test
    public void popConsumesExactlyTheHeader() throws BACnetErrorException {
        ByteQueue queue = new ByteQueue("25fe1234aabb");
        TagData tagData = new TagData().pop(queue);

        assertEquals(4, tagData.getTagLength());
        assertEquals(0x1234, tagData.getLength());
        assertArrayEquals(new byte[] {(byte) 0xaa, (byte) 0xbb}, queue.popAll());
    }

    /**
     * Both entry points return this, so they can be chained from a constructor call.
     */
    @Test
    public void peekAndPopReturnTheSameInstance() throws BACnetErrorException {
        TagData tagData = new TagData();
        assertSame(tagData, tagData.peek(new ByteQueue("210d")));
        assertSame(tagData, tagData.pop(new ByteQueue("210d")));
    }
}
