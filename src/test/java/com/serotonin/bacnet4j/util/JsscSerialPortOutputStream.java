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
import java.io.OutputStream;

import jssc.SerialPort;
import jssc.SerialPortException;

public class JsscSerialPortOutputStream extends OutputStream {
    private final SerialPort serialPort;

    public JsscSerialPortOutputStream(final SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    @Override
    public void write(final int b) throws IOException {
        try {
            serialPort.writeByte((byte) b);
        } catch (final SerialPortException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(final byte[] b) throws IOException {
        try {
            serialPort.writeBytes(b);
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
