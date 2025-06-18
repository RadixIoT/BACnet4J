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

package com.serotonin.bacnet4j.util;

import java.io.IOException;
import java.io.InputStream;

import com.serotonin.bacnet4j.util.sero.ThreadUtils;

import jssc.SerialPort;
import jssc.SerialPortException;

public class JsscSerialPortInputStream extends InputStream {
    private final SerialPort serialPort;

    public JsscSerialPortInputStream(final SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    @Override
    public int read() throws IOException {
        try {
            while (true) {
                final byte[] b = serialPort.readBytes(1);
                if (b == null) {
                    ThreadUtils.sleep(20);
                    continue;
                }
                return b[0];
            }
        } catch (final SerialPortException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }

        try {
            int length = serialPort.getInputBufferBytesCount();
            if (length > len) {
                length = len;
            }

            final byte[] buf = serialPort.readBytes(length);
            System.arraycopy(buf, 0, b, off, length);
            return length;
        } catch (final SerialPortException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return serialPort.getInputBufferBytesCount();
        } catch (final SerialPortException e) {
            throw new IOException(e);
        }
    }

    //    @Override
    //    public void close() throws IOException {
    //        // TODO Auto-generated method stub
    //        super.close();
    //    }
}
