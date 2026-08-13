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

package com.serotonin.bacnet4j.util.sero;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ByteQueueTest {
    /**
     * Popping an empty queue throws, whichever way the queue became empty.
     *
     * <p>Note that this held before the size check was added too, but only incidentally: emptying the queue sets
     * head to -1 as a sentinel, and the read of {@code queue[head]} then failed because -1 happens to be an invalid
     * array index. The check makes the throw deliberate and independent of how the empty state is represented, and
     * keeps the capacity of the backing array out of the message. The other extraction methods already checked.</p>
     */
    @Test
    public void popOnAnEmptyQueue() {
        var queue = new ByteQueue();
        assertThrows("never pushed", ArrayIndexOutOfBoundsException.class, queue::pop);

        ByteQueue emptiedByPop = new ByteQueue("01");
        assertEquals(1, emptiedByPop.pop());
        assertEquals(0, emptiedByPop.size());
        assertThrows("emptied by pop()", ArrayIndexOutOfBoundsException.class, emptiedByPop::pop);

        ByteQueue emptiedByPopLength = new ByteQueue("0102");
        emptiedByPopLength.pop(2);
        assertEquals(0, emptiedByPopLength.size());
        assertThrows("emptied by pop(int)", ArrayIndexOutOfBoundsException.class, emptiedByPopLength::pop);

        ByteQueue emptiedByPopArray = new ByteQueue("0102");
        emptiedByPopArray.pop(new byte[2]);
        assertEquals(0, emptiedByPopArray.size());
        assertThrows("emptied by pop(byte[])", ArrayIndexOutOfBoundsException.class, emptiedByPopArray::pop);

        ByteQueue emptiedByPopAll = new ByteQueue("0102");
        emptiedByPopAll.popAll();
        assertEquals(0, emptiedByPopAll.size());
        assertThrows("emptied by popAll()", ArrayIndexOutOfBoundsException.class, emptiedByPopAll::pop);
    }

    /**
     * The multi octet reads chain pop calls, so they run the queue empty part way through rather than at the start.
     */
    @Test
    public void multiOctetPopsPastTheEnd() {
        var queue1 = new ByteQueue("01");
        assertThrows(ArrayIndexOutOfBoundsException.class, queue1::popU2B);
        var queue2 = new ByteQueue("0102");
        assertThrows(ArrayIndexOutOfBoundsException.class, queue2::popU4B);
        var queue3 = new ByteQueue("01");
        assertThrows(ArrayIndexOutOfBoundsException.class, queue3::popS2B);
    }

    /**
     * The guard must not disturb a queue that has the data, including after it has wrapped around the end of the
     * backing array.
     */
    @Test
    public void popsWithinTheDataAreUnaffected() {
        ByteQueue queue = new ByteQueue(4);
        queue.push(new byte[] {1, 2, 3});
        queue.pop(2);
        // Head is now near the end of the four byte backing array, so this push wraps.
        queue.push(new byte[] {4, 5, 6});

        assertEquals(3, queue.pop());
        assertEquals(4, queue.pop());
        assertEquals(5, queue.pop());
        assertEquals(6, queue.pop());
        assertEquals(0, queue.size());
        assertThrows(ArrayIndexOutOfBoundsException.class, queue::pop);
    }
}
