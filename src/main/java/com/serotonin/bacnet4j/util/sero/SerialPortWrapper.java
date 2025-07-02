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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper to further aid in abstracting BACnet4J from a serial port implementation
 *
 * @author Terry Packer
 */
public abstract class SerialPortWrapper {

    public static final int PARITY_NONE = 0;
    public static final int STOPBITS_1 = 1;
    public static final int DATABITS_8 = 8;
    public static final int FLOWCONTROL_NONE = 0;

    /**
     * Close the Serial Port
     */
    public abstract void close() throws Exception;

    /**
     *
     */
    public abstract void open() throws Exception;

    /**
     * Return the input stream for an open port
     *
     * @return
     */
    public abstract InputStream getInputStream();

    /**
     * Return the output stream for an open port
     *
     * @return
     */
    public abstract OutputStream getOutputStream();

    /**
     * @return
     */
    public int getFlowControlIn() {
        return FLOWCONTROL_NONE;
    }

    /**
     * @return
     */
    public int getFlowControlOut() {
        return FLOWCONTROL_NONE;
    }

    /**
     * @return
     */
    public int getDataBits() {
        return DATABITS_8;
    }

    /**
     * @return
     */
    public int getStopBits() {
        return STOPBITS_1;
    }

    /**
     * @return
     */
    public int getParity() {
        return PARITY_NONE;
    }

    /**
     * @return
     */
    public abstract String getCommPortId();
}
