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

import static com.serotonin.bacnet4j.util.BACnetUtils.toInt;
import static com.serotonin.bacnet4j.util.BACnetUtils.toLong;

import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class TagData {
    /**
     * An LVT of 5 means the tag header carries an extended length.
     */
    static final int LVT_EXTENDED_LENGTH = 5;

    /**
     * An LVT of 6 marks a start tag, which carries no length of its own.
     */
    static final int LVT_START_TAG = 6;

    /**
     * An LVT of 7 marks an end tag, which carries no length of its own.
     */
    static final int LVT_END_TAG = 7;

    private int tagNumber;
    private boolean contextSpecific;
    /**
     * The raw length/value/type (LVT) field of the tag header. Values 0 through 4 are literal lengths, 5 means an
     * extended length follows, and 6 and 7 mark start and end tags.
     * <p>
     * Start and end tags must be recognized from this field rather than from {@link #length}, because an extended
     * length overwrites {@link #length} with a decoded value whose low bits are arbitrary and can therefore imitate
     * a start or end tag.
     */
    private int lvt;
    private long length;
    private int tagLength;

    public long getTotalLength() {
        return length + tagLength;
    }

    public boolean isStartTag() {
        return contextSpecific && lvt == LVT_START_TAG;
    }

    public boolean isStartTag(int contextId) {
        return isStartTag() && tagNumber == contextId;
    }

    public boolean isEndTag() {
        return contextSpecific && lvt == LVT_END_TAG;
    }

    public boolean isEndTag(int contextId) {
        return isEndTag() && tagNumber == contextId;
    }

    public int getTagNumber() {
        return tagNumber;
    }

    public boolean isContextSpecific() {
        return contextSpecific;
    }

    public int getLvt() {
        return lvt;
    }

    public long getLength() {
        return length;
    }

    public int getTagLength() {
        return tagLength;
    }

    public TagData pop(ByteQueue queue) {
        peek(queue);
        queue.pop(tagLength);
        return this;
    }

    public TagData peek(ByteQueue queue) {
        var peekIndex = 0;
        var b = queue.peek(peekIndex++);
        tagNumber = (b & 0xff) >> 4;
        contextSpecific = (b & 8) == 8;
        lvt = b & 7;
        length = lvt;

        if (tagNumber == 0xf) {
            // Extended tag.
            tagNumber = toInt(queue.peek(peekIndex++));
        }

        if (lvt == TagData.LVT_EXTENDED_LENGTH) {
            length = toInt(queue.peek(peekIndex++));
            if (length == 254) {
                length = toLong(queue.peek(peekIndex++)) << 8 | toLong(queue.peek(peekIndex++));
            } else if (length == 255) {
                length = toLong(queue.peek(peekIndex++)) << 24 | toLong(queue.peek(peekIndex++)) << 16
                        | toLong(queue.peek(peekIndex++)) << 8 | toLong(queue.peek(peekIndex++));
            }
        }

        tagLength = peekIndex;

        return this;
    }
}
