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

import com.serotonin.bacnet4j.apdu.Segmentable;

public class SegmentWindow {
    private int firstSequenceId;
    private final Segmentable[] segments;

    public SegmentWindow(final int windowSize, final int firstSequenceId) {
        this.firstSequenceId = firstSequenceId;
        segments = new Segmentable[windowSize];
    }

    public int getFirstSequenceId() {
        return firstSequenceId;
    }

    public Segmentable getSegment(final int sequenceId) {
        return segments[sequenceId - firstSequenceId];
    }

    public void setSegment(final Segmentable segment) {
        segments[segment.getSequenceNumber() - firstSequenceId] = segment;
    }

    public boolean fitsInWindow(final Segmentable segment) {
        final int index = segment.getSequenceNumber() - firstSequenceId;
        if (index < 0 || index >= segments.length)
            return false;
        return true;
    }

    public boolean isEmpty() {
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] != null)
                return false;
        }
        return true;
    }

    /**
     * The segment window is full if all slots are non-null.
     *
     * @return
     */
    public boolean isFull() {
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == null)
                return false;
        }
        return true;
    }

    /**
     * The message is complete if the last full slot is marked as not more follows, and all slots before it are filled.
     * (Full slots following it are ignored.)
     *
     * @return
     */
    public boolean isMessageComplete() {
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == null)
                return false;
            if (!segments[i].isMoreFollows())
                return true;
        }
        return false;
    }

    public void clear(final int firstSequenceId) {
        this.firstSequenceId = firstSequenceId;
        for (int i = 0; i < segments.length; i++)
            segments[i] = null;
    }

    public boolean isLastSegment(final int sequenceId) {
        return sequenceId == segments.length + firstSequenceId - 1;
    }

    public Segmentable[] getSegments() {
        return segments;
    }

    public int getLatestSequenceId() {
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i] != null)
                return segments[i].getSequenceNumber();
        }
        return -1;
    }

    public int getWindowSize() {
        return segments.length;
    }
}
