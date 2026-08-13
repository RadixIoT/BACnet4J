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

/**
 * Sequence number arithmetic for segmented messages, per clause 5.4.2.
 * <p>
 * APDU sequence numbers are unsigned eight bit values that wrap, so a segmented message is not limited to 256
 * segments. All computations and comparisons here are modulo 256, as the specification requires.
 */
public class SegmentSequence {
    private static final int MASK = 0xff;

    /**
     * The smallest legal window size. See the ASN.1 for 'proposed-window-size' in clause 20.1.2.
     */
    public static final int MIN_WINDOW_SIZE = 1;

    /**
     * The largest legal window size. See the ASN.1 for 'proposed-window-size' in clause 20.1.2.
     */
    public static final int MAX_WINDOW_SIZE = 127;

    private SegmentSequence() {
        // Static access only.
    }

    /**
     * The sequence number following the given one.
     */
    public static int next(int sequenceNumber) {
        return sequenceNumber + 1 & MASK;
    }

    /**
     * The modulo 256 difference between two sequence numbers, i.e. how far seqA is ahead of seqB.
     */
    public static int diff(int seqA, int seqB) {
        return seqA - seqB & MASK;
    }

    /**
     * The given sequence number advanced by the given number of segments.
     */
    public static int plus(int sequenceNumber, int count) {
        return sequenceNumber + count & MASK;
    }

    /**
     * Clause 5.4.2.1, function InWindow. Performs a modulo 256 compare of two unsigned eight bit sequence numbers.
     *
     * @param seqA             the sequence number to test
     * @param seqB             the sequence number to test against
     * @param actualWindowSize the current window size
     * @return true if seqA minus seqB, modulo 256, is less than actualWindowSize
     */
    public static boolean inWindow(int seqA, int seqB, int actualWindowSize) {
        return diff(seqA, seqB) < actualWindowSize;
    }

    /**
     * Clause 5.4.2.2, function DuplicateInWindow, as amended by addendum 135-2020ch-1. Determines whether a message
     * segment sequence number is within the range of successfully received message segments in the current incomplete
     * window, or, if called at the start of a new window before any segments of it have been received, whether it is
     * within the range of the previous window. Such a segment does not require a segment acknowledgement, because the
     * segments of the window have not all been successfully received yet.
     * <p>
     * Addendum ch-1 made three changes to the pre-addendum form of this function, all of which are implemented here:
     * step (2) tests receivedCount against actualWindowSize rather than against zero, step (4) is new, and callers
     * pass the sequence number following initialSequenceNumber rather than initialSequenceNumber itself. Step (4)
     * exists so that retransmissions of the previous window, which indicate that the segment acknowledgement for that
     * window was lost, are counted as duplicates rather than treated as out of order.
     *
     * @param seqA               the sequence number of the received message segment
     * @param firstSeqNumber     the sequence number following the last one of the previously completed window
     * @param lastSequenceNumber the sequence number of the last segment successfully received in order
     * @param actualWindowSize   the current window size
     * @return true if the message segment is a duplicate and so is to be ignored
     */
    public static boolean duplicateInWindow(int seqA, int firstSeqNumber, int lastSequenceNumber,
            int actualWindowSize) {
        // (1) The number of successfully received message segments in the current window.
        int receivedCount = diff(lastSequenceNumber, firstSeqNumber);

        // (2) More than a window's worth cannot have been received, so this is not the current window.
        if (receivedCount > actualWindowSize)
            return false;

        // (3) The segment is located in a previous part of the current window.
        if (diff(seqA, firstSeqNumber) <= receivedCount)
            return true;

        // (4) No segments of the current window have been received yet, and the segment belongs to the window before
        // it, so the acknowledgement of that window was not received by the sender.
        if (receivedCount == 0 && diff(firstSeqNumber, seqA) <= actualWindowSize)
            return true;

        // (5) An out-of-order segment in the current window that has not yet been successfully received.
        return false;
    }

    /**
     * Whether the given value is a valid window size, i.e. in the range 1 to 127 inclusive. See the ASN.1 for
     * 'proposed-window-size' in clause 20.1.2, and the window size determination notes in clause 5.4.
     */
    public static boolean isValidWindowSize(int windowSize) {
        return windowSize >= MIN_WINDOW_SIZE && windowSize <= MAX_WINDOW_SIZE;
    }
}
