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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public class ByteQueue implements Cloneable {
    private byte[] queue;
    private int head = -1;
    private int tail = 0;
    private int size = 0;

    private int markHead;
    private int markTail;
    private int markSize;

    public ByteQueue() {
        this(1024);
    }

    public ByteQueue(final int initialLength) {
        queue = new byte[initialLength];
    }

    public ByteQueue(final byte[] b) {
        this(b.length);
        push(b, 0, b.length);
    }

    public ByteQueue(final byte[] b, final int pos, final int length) {
        this(length);
        push(b, pos, length);
    }

    public ByteQueue(final String hex) {
        this(hex.length() / 2);
        push(hex);
    }

    public void push(final String hex) {
        if (hex.length() % 2 != 0)
            throw new IllegalArgumentException("Hex string must have an even number of characters");
        final byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        push(b, 0, b.length);
    }

    public void push(final byte b) {
        if (room() == 0)
            expand();

        queue[tail] = b;

        if (head == -1)
            head = 0;
        tail = (tail + 1) % queue.length;
        size++;
    }

    public void push(final int i) {
        push((byte) i);
    }

    public void push(final long l) {
        push((byte) l);
    }

    /**
     * Push unsigned 2 bytes.
     */
    public void pushU2B(final int i) {
        push((byte) (i >> 8));
        push((byte) i);
    }

    /**
     * Push unsigned 3 bytes.
     */
    public void pushU3B(final int i) {
        push((byte) (i >> 16));
        push((byte) (i >> 8));
        push((byte) i);
    }

    /**
     * Push signed 4 bytes.
     */
    public void pushS4B(final int i) {
        pushInt(i);
    }

    /**
     * Push unsigned 4 bytes.
     */
    public void pushU4B(final long l) {
        push((byte) (l >> 24));
        push((byte) (l >> 16));
        push((byte) (l >> 8));
        push((byte) l);
    }

    public void pushChar(final char c) {
        push((byte) (c >> 8));
        push((byte) c);
    }

    public void pushDouble(final double d) {
        pushLong(Double.doubleToLongBits(d));
    }

    public void pushFloat(final float f) {
        pushInt(Float.floatToIntBits(f));
    }

    public void pushInt(final int i) {
        push((byte) (i >> 24));
        push((byte) (i >> 16));
        push((byte) (i >> 8));
        push((byte) i);
    }

    public void pushLong(final long l) {
        push((byte) (l >> 56));
        push((byte) (l >> 48));
        push((byte) (l >> 40));
        push((byte) (l >> 32));
        push((byte) (l >> 24));
        push((byte) (l >> 16));
        push((byte) (l >> 8));
        push((byte) l);
    }

    public void pushShort(final short s) {
        push((byte) (s >> 8));
        push((byte) s);
    }

    public void read(final InputStream in, final int length) throws IOException {
        if (length == 0)
            return;

        while (room() < length)
            expand();

        final int tailLength = queue.length - tail;
        if (tailLength > length)
            readImpl(in, tail, length);
        else
            readImpl(in, tail, tailLength);

        if (length > tailLength)
            readImpl(in, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    private void readImpl(final InputStream in, final int offset, final int length) throws IOException {
        int readcount;
        int off = offset;
        int len = length;
        while (length > 0) {
            readcount = in.read(queue, off, len);
            off += readcount;
            len -= readcount;
        }
    }

    public void push(final byte[] b) {
        push(b, 0, b.length);
    }

    public void push(final byte[] b, final int pos, final int length) {
        if (length == 0)
            return;

        while (room() < length)
            expand();

        final int tailLength = queue.length - tail;
        if (tailLength > length)
            System.arraycopy(b, pos, queue, tail, length);
        else
            System.arraycopy(b, pos, queue, tail, tailLength);

        if (length > tailLength)
            System.arraycopy(b, tailLength + pos, queue, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    public void push(final ByteQueue source) {
        if (source.size == 0)
            return;

        ByteQueue q = source;
        if (q == this)
            q = (ByteQueue) clone();

        int firstCopyLen = q.queue.length - q.head;
        if (q.size < firstCopyLen)
            firstCopyLen = q.size;
        push(q.queue, q.head, firstCopyLen);

        if (firstCopyLen < q.size)
            push(q.queue, 0, q.tail);
    }

    public void push(final ByteQueue source, final int len) {
        // TODO There is certainly a more elegant way to do this...
        int l = len;
        while (l-- > 0)
            push(source.pop());
    }

    public void push(final ByteBuffer source) {
        final int length = source.remaining();
        if (length == 0)
            return;

        while (room() < length)
            expand();

        final int tailLength = queue.length - tail;
        if (tailLength > length)
            source.get(queue, tail, length);
        else
            source.get(queue, tail, tailLength);

        if (length > tailLength)
            source.get(queue, 0, length - tailLength);

        if (head == -1)
            head = 0;
        tail = (tail + length) % queue.length;
        size += length;
    }

    public void mark() {
        markHead = head;
        markTail = tail;
        markSize = size;
    }

    public void reset() {
        head = markHead;
        tail = markTail;
        size = markSize;
    }

    public byte pop() {
        final byte retval = queue[head];

        if (size == 1) {
            head = -1;
            tail = 0;
        } else
            head = (head + 1) % queue.length;

        size--;

        return retval;
    }

    public int popU1B() {
        return pop() & 0xff;
    }

    public int popU2B() {
        return (pop() & 0xff) << 8 | pop() & 0xff;
    }

    public int popU3B() {
        return (pop() & 0xff) << 16 | (pop() & 0xff) << 8 | pop() & 0xff;
    }

    public short popS2B() {
        return (short) ((pop() & 0xff) << 8 | pop() & 0xff);
    }

    public int popS4B() {
        return (pop() & 0xff) << 24 | (pop() & 0xff) << 16 | (pop() & 0xff) << 8 | pop() & 0xff;
    }

    public long popU4B() {
        return (long) (pop() & 0xff) << 24 | (long) (pop() & 0xff) << 16 | (long) (pop() & 0xff) << 8 | pop() & 0xff;
    }

    public int pop(final byte[] buf) {
        return pop(buf, 0, buf.length);
    }

    public int pop(final byte[] buf, final int pos, final int length) {
        final int len = peek(buf, pos, length);

        size -= len;

        if (size == 0) {
            head = -1;
            tail = 0;
        } else
            head = (head + len) % queue.length;

        return len;
    }

    public int pop(final int length) {
        if (length == 0)
            return 0;
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        int len = length;
        if (len > size)
            len = size;

        size -= len;

        if (size == 0) {
            head = -1;
            tail = 0;
        } else
            head = (head + len) % queue.length;

        return len;
    }

    public String popString(final int length, final Charset charset) {
        final byte[] b = new byte[length];
        pop(b);
        return new String(b, charset);
    }

    public byte[] popAll() {
        final byte[] data = new byte[size];
        pop(data, 0, data.length);
        return data;
    }

    public void write(final OutputStream out) throws IOException {
        write(out, size);
    }

    public void write(final OutputStream out, final int length) throws IOException {
        if (length == 0)
            return;
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        int len = length;
        if (len > size)
            len = size;

        int firstCopyLen = queue.length - head;
        if (len < firstCopyLen)
            firstCopyLen = len;

        out.write(queue, head, firstCopyLen);
        if (firstCopyLen < len)
            out.write(queue, 0, len - firstCopyLen);

        size -= len;

        if (size == 0) {
            head = -1;
            tail = 0;
        } else
            head = (head + len) % queue.length;
    }

    public byte tailPop() {
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        tail = (tail + queue.length - 1) % queue.length;
        final byte retval = queue[tail];

        if (size == 1) {
            head = -1;
            tail = 0;
        }

        size--;

        return retval;
    }

    public byte peek(final int index) {
        if (index >= size)
            throw new IllegalArgumentException("index " + index + " is >= queue size " + size);

        final int pos = (index + head) % queue.length;
        return queue[pos];
    }

    public byte[] peek(final int index, final int length) {
        final byte[] result = new byte[length];
        // TODO: use System.arraycopy instead.
        for (int i = 0; i < length; i++)
            result[i] = peek(index + i);
        return result;
    }

    public byte[] peekAll() {
        final byte[] data = new byte[size];
        peek(data, 0, data.length);
        return data;
    }

    public int peek(final byte[] buf) {
        return peek(buf, 0, buf.length);
    }

    public int peek(final byte[] buf, final int pos, final int length) {
        if (length == 0)
            return 0;
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException(-1);

        int len = length;
        if (len > size)
            len = size;

        int firstCopyLen = queue.length - head;
        if (len < firstCopyLen)
            firstCopyLen = len;

        System.arraycopy(queue, head, buf, pos, firstCopyLen);
        if (firstCopyLen < len)
            System.arraycopy(queue, 0, buf, pos + firstCopyLen, len - firstCopyLen);

        return len;
    }

    public int indexOf(final byte b) {
        return indexOf(b, 0);
    }

    public int indexOf(final byte b, final int start) {
        if (start >= size)
            return -1;

        int index = (head + start) % queue.length;
        for (int i = start; i < size; i++) {
            if (queue[index] == b)
                return i;
            index = (index + 1) % queue.length;
        }
        return -1;
    }

    public int indexOf(final byte[] b) {
        return indexOf(b, 0);
    }

    public int indexOf(final byte[] b, final int start) {
        if (b == null || b.length == 0)
            throw new IllegalArgumentException("cannot search for empty values");

        int pos = start;
        while ((pos = indexOf(b[0], pos)) != -1 && pos < size - b.length + 1) {
            boolean found = true;
            for (int i = 1; i < b.length; i++) {
                if (peek(pos + i) != b[i]) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return pos;
            }

            pos++;
        }

        return -1;
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
        head = -1;
        tail = 0;
    }

    private int room() {
        return queue.length - size;
    }

    private void expand() {
        final byte[] newb = new byte[queue.length * 2];

        if (head == -1) {
            queue = newb;
            return;
        }

        if (tail > head) {
            System.arraycopy(queue, head, newb, head, tail - head);
            queue = newb;
            return;
        }

        System.arraycopy(queue, head, newb, head + queue.length, queue.length - head);
        System.arraycopy(queue, 0, newb, 0, tail);
        head += queue.length;
        queue = newb;
    }

    @Override
    public Object clone() {
        try {
            final ByteQueue clone = (ByteQueue) super.clone();
            // Array is mutable, so make a copy of it too.
            clone.queue = queue.clone();
            return clone;
        } catch (final CloneNotSupportedException e) {
            // Will never happen because we're Cloneable
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        if (size == 0)
            return "[]";

        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(Integer.toHexString(peek(0) & 0xff));
        for (int i = 1; i < size; i++)
            sb.append(',').append(Integer.toHexString(peek(i) & 0xff));
        sb.append("]");

        return sb.toString();
    }

    public String toHexString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++)
            sb.append(StringUtils.leftPad(Integer.toHexString(peek(i) & 0xff), 2, '0'));
        return sb.toString();
    }

    public String dumpQueue() {
        final StringBuilder sb = new StringBuilder();

        if (queue.length == 0)
            sb.append("[]");
        else {
            sb.append('[');
            sb.append(queue[0]);
            for (int i = 1; i < queue.length; i++) {
                sb.append(", ");
                sb.append(queue[i]);
            }
            sb.append("]");
        }

        sb.append(", h=").append(head).append(", t=").append(tail).append(", s=").append(size);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + head;
        result = prime * result + Arrays.hashCode(queue);
        result = prime * result + size;
        result = prime * result + tail;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ByteQueue other = (ByteQueue) obj;

        if (size != other.size)
            return false;

        for (int i = 0; i < size; i++) {
            if (peek(i) != other.peek(i))
                return false;
        }

        return true;
    }
}
