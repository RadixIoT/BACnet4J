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

package com.serotonin.bacnet4j.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SegmentSequenceTest {
    @Test
    public void nextWraps() {
        assertEquals(1, SegmentSequence.next(0));
        assertEquals(255, SegmentSequence.next(254));
        assertEquals(0, SegmentSequence.next(255));
    }

    @Test
    public void plusWraps() {
        assertEquals(5, SegmentSequence.plus(0, 5));
        assertEquals(3, SegmentSequence.plus(254, 5));
        assertEquals(254, SegmentSequence.plus(254, 0));
    }

    @Test
    public void diffWraps() {
        assertEquals(0, SegmentSequence.diff(0, 0));
        assertEquals(1, SegmentSequence.diff(0, 255));
        assertEquals(255, SegmentSequence.diff(4, 5));
        assertEquals(4, SegmentSequence.diff(3, 255));
    }

    /**
     * The non-normative examples given for the function InWindow in clause 5.4.2.1, for an ActualWindowSize of 4.
     */
    @Test
    public void inWindowSpecExamples() {
        assertTrue(SegmentSequence.inWindow(0, 0, 4));
        assertTrue(SegmentSequence.inWindow(1, 0, 4));
        assertTrue(SegmentSequence.inWindow(3, 0, 4));
        assertFalse(SegmentSequence.inWindow(4, 0, 4));
        // Since the modulo 256 difference 4 - 5 = 255.
        assertFalse(SegmentSequence.inWindow(4, 5, 4));
        // Since the modulo 256 difference 0 - 255 = 1.
        assertTrue(SegmentSequence.inWindow(0, 255, 4));
    }

    /**
     * The non-normative examples given for the function DuplicateInWindow in clause 5.4.2.2, for an ActualWindowSize
     * of 4. Addendum 135-2020ch-1 revised the function but retained these examples.
     */
    @Test
    public void duplicateInWindowSpecExamples() {
        assertTrue(SegmentSequence.duplicateInWindow(0, 0, 1, 4));
        assertTrue(SegmentSequence.duplicateInWindow(1, 0, 1, 4));
        assertFalse(SegmentSequence.duplicateInWindow(2, 0, 1, 4));
        assertFalse(SegmentSequence.duplicateInWindow(3, 0, 1, 4));
    }

    /**
     * At the start of a new window no segments of it have been received, so the modulo 256 difference between
     * lastSequenceNumber and firstSeqNumber is 255. Step (2) of the addendum ch-1 form of the function reports that
     * as not being a duplicate, so the caller negatively acknowledges and the sender resumes.
     */
    @Test
    public void duplicateInWindowAtWindowStart() {
        // InitialSequenceNumber and LastSequenceNumber are both 8, so firstSeqNumber is 9.
        assertFalse(SegmentSequence.duplicateInWindow(5, 9, 8, 4));
        assertFalse(SegmentSequence.duplicateInWindow(8, 9, 8, 4));
    }

    /**
     * Step (4), added by addendum ch-1: once one segment of the new window has been received, retransmissions of the
     * preceding window are duplicates rather than out of order segments.
     */
    @Test
    public void duplicateInWindowRecognisesPreviousWindow() {
        // InitialSequenceNumber is 8 so firstSeqNumber is 9, and one segment has been received, so lastSequence
        // number is 9. Segments 5 through 8 belong to the previous window of size 4.
        assertTrue(SegmentSequence.duplicateInWindow(9, 9, 9, 4));
        assertTrue(SegmentSequence.duplicateInWindow(8, 9, 9, 4));
        assertTrue(SegmentSequence.duplicateInWindow(5, 9, 9, 4));
        // A segment further back than a window is not a duplicate.
        assertFalse(SegmentSequence.duplicateInWindow(4, 9, 9, 4));
        // Nor is a segment ahead of the one expected.
        assertFalse(SegmentSequence.duplicateInWindow(11, 9, 9, 4));
    }

    /**
     * The same, across the point at which sequence numbers wrap.
     */
    @Test
    public void duplicateInWindowAcrossWrap() {
        // InitialSequenceNumber is 254, so firstSeqNumber is 255, and segment 255 has been received.
        assertTrue(SegmentSequence.duplicateInWindow(255, 255, 255, 4));
        assertTrue(SegmentSequence.duplicateInWindow(252, 255, 255, 4));
        assertFalse(SegmentSequence.duplicateInWindow(0, 255, 255, 4));

        // InitialSequenceNumber is 255, so firstSeqNumber is 0, and segments 0 and 1 have been received.
        assertTrue(SegmentSequence.duplicateInWindow(0, 0, 1, 4));
        assertTrue(SegmentSequence.duplicateInWindow(1, 0, 1, 4));
        assertFalse(SegmentSequence.duplicateInWindow(2, 0, 1, 4));
    }

    @Test
    public void windowSizeRange() {
        assertFalse(SegmentSequence.isValidWindowSize(0));
        assertTrue(SegmentSequence.isValidWindowSize(1));
        assertTrue(SegmentSequence.isValidWindowSize(127));
        assertFalse(SegmentSequence.isValidWindowSize(128));
        assertFalse(SegmentSequence.isValidWindowSize(255));
    }
}
