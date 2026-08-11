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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * getTotalLength used to compute in long and cast the result to int. A 4-byte extended length can exceed
 * Integer.MAX_VALUE, so the cast could wrap the total to zero or negative. Callers consume getTotalLength bytes at
 * a time, so a non-positive total made them consume nothing and spin forever.
 */
public class TagDataTest {
    /**
     * The lowest and highest 4-byte extended lengths that used to wrap, for a 6 byte tag header. The raw sums are
     * 0x80000000 and 0x100000000, which truncate to Integer.MIN_VALUE and 0 respectively.
     */
    @Test
    public void wrappingLengthsStayPositive() {
        assertTotalLength(0x7ffffffaL, 6);
        assertTotalLength(0xfffffffaL, 6);
    }

    /**
     * The same boundaries for a 7 byte header, which an extended tag number produces.
     */
    @Test
    public void wrappingLengthsStayPositiveWithExtendedTagNumber() {
        assertTotalLength(0x7ffffff9L, 7);
        assertTotalLength(0xfffffff9L, 7);
    }

    /**
     * The largest length that can be encoded at all.
     */
    @Test
    public void maximumLengthStaysPositive() {
        assertTotalLength(0xffffffffL, 6);
    }

    @Test
    public void ordinaryLengthsAreUnaffected() {
        assertTotalLength(0, 1);
        assertTotalLength(4, 1);
        assertTotalLength(0xffff, 4);
    }

    /**
     * The decoded length must survive the trip through peekTagData intact.
     */
    @Test
    public void peekTagDataDecodesFourByteExtendedLength() {
        // 7 = tag number, 5 = extended length, ff = 4-byte length follows.
        TagData tagData = new TagData();
        Encodable.peekTagData(new ByteQueue("75fff0000000"), tagData);

        assertEquals(7, tagData.tagNumber);
        assertEquals(0xf0000000L, tagData.length);
        assertEquals(6, tagData.tagLength);
        assertEquals(0xf0000000L + 6, tagData.getTotalLength());
        assertTrue(tagData.getTotalLength() > 0);
    }

    private static void assertTotalLength(long length, int tagLength) {
        TagData tagData = new TagData();
        tagData.length = length;
        tagData.tagLength = tagLength;

        assertEquals(length + tagLength, tagData.getTotalLength());
        assertTrue("total for length " + length + " must stay positive", tagData.getTotalLength() > 0);
    }
}
