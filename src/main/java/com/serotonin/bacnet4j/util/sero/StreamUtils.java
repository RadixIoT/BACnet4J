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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class StreamUtils {
    public static void transfer(final InputStream in, final OutputStream out) throws IOException {
        transfer(in, out, -1);
    }

    public static void transfer(final InputStream in, final OutputStream out, final long limit) throws IOException {
        final byte[] buf = new byte[1024];
        int readcount;
        long total = 0;
        while ((readcount = in.read(buf)) != -1) {
            if (limit != -1) {
                if (total + readcount > limit)
                    readcount = (int) (limit - total);
            }

            if (readcount > 0)
                out.write(buf, 0, readcount);

            total += readcount;
            if (limit != -1 && total >= limit)
                break;
        }
        out.flush();
    }

    public static void transfer(final InputStream in, final SocketChannel out) throws IOException {
        final byte[] buf = new byte[1024];
        final ByteBuffer bbuf = ByteBuffer.allocate(1024);
        int len;
        while ((len = in.read(buf)) != -1) {
            bbuf.put(buf, 0, len);
            bbuf.flip();
            while (bbuf.remaining() > 0)
                out.write(bbuf);
            bbuf.clear();
        }
    }

    public static void transfer(final Reader reader, final Writer writer) throws IOException {
        transfer(reader, writer, -1);
    }

    public static void transfer(final Reader reader, final Writer writer, final long limit) throws IOException {
        final char[] buf = new char[1024];
        int readcount;
        long total = 0;
        while ((readcount = reader.read(buf)) != -1) {
            if (limit != -1) {
                if (total + readcount > limit)
                    readcount = (int) (limit - total);
            }

            if (readcount > 0)
                writer.write(buf, 0, readcount);

            total += readcount;
            if (limit != -1 && total >= limit)
                break;
        }
        writer.flush();
    }

    public static byte[] read(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
        transfer(in, out);
        return out.toByteArray();
    }

    public static char[] read(final Reader reader) throws IOException {
        final CharArrayWriter writer = new CharArrayWriter();
        transfer(reader, writer);
        return writer.toCharArray();
    }

    public static char readChar(final InputStream in) throws IOException {
        return (char) in.read();
    }

    public static String readString(final InputStream in, final int length) throws IOException {
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(readChar(in));
        return sb.toString();
    }

    public static byte readByte(final InputStream in) throws IOException {
        return (byte) in.read();
    }

    public static int read4ByteSigned(final InputStream in) throws IOException {
        return in.read() | in.read() << 8 | in.read() << 16 | in.read() << 24;
    }

    public static long read4ByteUnsigned(final InputStream in) throws IOException {
        return in.read() | in.read() << 8 | in.read() << 16 | in.read() << 24;
    }

    public static int read2ByteUnsigned(final InputStream in) throws IOException {
        return in.read() | in.read() << 8;
    }

    public static short read2ByteSigned(final InputStream in) throws IOException {
        return (short) (in.read() | in.read() << 8);
    }

    public static void writeByte(final OutputStream out, final byte b) throws IOException {
        out.write(b);
    }

    public static void writeChar(final OutputStream out, final char c) throws IOException {
        out.write((byte) c);
    }

    public static void writeString(final OutputStream out, final String s) throws IOException {
        for (int i = 0; i < s.length(); i++)
            writeChar(out, s.charAt(i));
    }

    public static void write4ByteSigned(final OutputStream out, final int i) throws IOException {
        out.write((byte) (i & 0xFF));
        out.write((byte) (i >> 8 & 0xFF));
        out.write((byte) (i >> 16 & 0xFF));
        out.write((byte) (i >> 24 & 0xFF));
    }

    public static void write4ByteUnsigned(final OutputStream out, final long l) throws IOException {
        out.write((byte) (l & 0xFF));
        out.write((byte) (l >> 8 & 0xFF));
        out.write((byte) (l >> 16 & 0xFF));
        out.write((byte) (l >> 24 & 0xFF));
    }

    public static void write2ByteUnsigned(final OutputStream out, final int i) throws IOException {
        out.write((byte) (i & 0xFF));
        out.write((byte) (i >> 8 & 0xFF));
    }

    public static void write2ByteSigned(final OutputStream out, final short s) throws IOException {
        out.write((byte) (s & 0xFF));
        out.write((byte) (s >> 8 & 0xFF));
    }

    public static String dumpArray(final byte[] b) {
        return dumpArray(b, 0, b.length);
    }

    public static String dumpArray(final byte[] b, final int pos, final int len) {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = pos; i < len; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(b[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String dumpMessage(final byte[] b) {
        return dumpMessage(b, 0, b.length);
    }

    public static String dumpMessage(final byte[] b, final int pos, final int len) {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = pos; i < len; i++) {
            switch (b[i]) {
                case 2:
                    sb.append("&STX;");
                    break;
                case 3:
                    sb.append("&ETX;");
                    break;
                case 27:
                    sb.append("&ESC;");
                    break;
                default:
                    sb.append((char) b[i]);
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public static String dumpArrayHex(final byte[] b) {
        return dumpArrayHex(b, 0, b.length);
    }

    public static String dumpArrayHex(final byte[] b, final int pos, final int len) {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = pos; i < len; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(Integer.toHexString(b[i] & 0xff));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String dumpHex(final byte[] b) {
        return dumpHex(b, 0, b.length);
    }

    public static String dumpHex(final byte[] b, final int pos, final int len) {
        final StringBuilder sb = new StringBuilder();
        for (int i = pos; i < len; i++)
            sb.append(StringUtils.leftPad(Integer.toHexString(b[i] & 0xff), 2, '0'));
        return sb.toString();
    }

    public static String readFile(final String filename) throws IOException {
        return readFile(new File(filename));
    }

    public static String readFile(final File file) throws IOException {
        FileReader in = null;
        try {
            in = new FileReader(file);
            final StringWriter out = new StringWriter();
            transfer(in, out);
            return out.toString();
        } finally {
            if (in != null)
                in.close();
        }
    }

    public static List<String> readLines(final String filename) throws IOException {
        return readLines(new File(filename));
    }

    public static List<String> readLines(final File file) throws IOException {
        final List<String> lines = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null)
                lines.add(line);
            return lines;
        }
    }

    public static void writeFile(final String filename, final String content) throws IOException {
        writeFile(new File(filename), content);
    }

    public static void writeFile(final File file, final String content) throws IOException {
        try (FileWriter out = new FileWriter(file)) {
            out.write(content);
        }
    }

    public static void readLines(final String filename, final LineHandler lineHandler) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = in.readLine()) != null)
                lineHandler.handleLine(line);
            lineHandler.done();
        }
    }

    public static String toHex(final byte[] bs) {
        final StringBuilder sb = new StringBuilder(bs.length * 2);
        for (final byte b : bs)
            sb.append(StringUtils.leftPad(Integer.toHexString(b & 0xff), 2, '0'));
        return sb.toString();
    }

    public static String toHex(final byte b) {
        return StringUtils.leftPad(Integer.toHexString(b & 0xff), 2, '0');
    }

    public static String toHex(final short s) {
        return StringUtils.leftPad(Integer.toHexString(s & 0xffff), 4, '0');
    }

    public static String toHex(final int i) {
        return StringUtils.leftPad(Integer.toHexString(i), 8, '0');
    }

    public static String toHex(final long l) {
        return StringUtils.leftPad(Long.toHexString(l), 16, '0');
    }

    public static byte[] fromHex(final String s) {
        final byte[] bs = new byte[s.length() / 2];
        for (int i = 0; i < bs.length; i++)
            bs[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return bs;
    }
}
