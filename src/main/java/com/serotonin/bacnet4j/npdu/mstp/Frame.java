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

package com.serotonin.bacnet4j.npdu.mstp;

import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class Frame implements Cloneable {
    private FrameType frameType;
    private byte destinationAddress;
    private byte sourceAddress;
    private int length;
    private byte[] data;

    public Frame() {
        // no op
    }

    public void reset() {
        frameType = null;
        destinationAddress = 0;
        sourceAddress = 0;
        length = 0;
        data = null;
    }

    public Frame copy() {
        try {
            return (Frame) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public Frame(FrameType frameType, byte destinationAddress, byte sourceAddress) {
        this(frameType, destinationAddress, sourceAddress, null);
    }

    public Frame(FrameType frameType, byte destinationAddress, byte sourceAddress, byte[] data) {
        this.frameType = frameType;
        this.destinationAddress = destinationAddress;
        this.sourceAddress = sourceAddress;
        this.length = data == null ? 0 : data.length;
        this.data = data;
    }

    public boolean forStation(byte thisStation) {
        return destinationAddress == thisStation;
    }

    public boolean broadcast() {
        return destinationAddress == Constants.BROADCAST;
    }

    public boolean forStationOrBroadcast(byte thisStation) {
        return forStation(thisStation) || broadcast();
    }

    /**
     * @return the frameType
     */
    public FrameType getFrameType() {
        return frameType;
    }

    /**
     * @param frameType the frameType to set
     */
    public void setFrameType(FrameType frameType) {
        this.frameType = frameType;
    }

    /**
     * @return the destinationAddress
     */
    public byte getDestinationAddress() {
        return destinationAddress;
    }

    /**
     * @param destinationAddress the destinationAddress to set
     */
    public void setDestinationAddress(byte destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    /**
     * @return the sourceAddress
     */
    public byte getSourceAddress() {
        return sourceAddress;
    }

    /**
     * @param sourceAddress the sourceAddress to set
     */
    public void setSourceAddress(byte sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.length = data == null ? 0 : data.length;
        this.data = data;
    }

    @Override
    public String toString() {
        return "Frame [frameType=" + frameType + ", destinationAddress=" + destinationAddress + ", sourceAddress="
                + sourceAddress + ", length=" + length + ", data="
                + (data == null ? "null" : StreamUtils.dumpHex(data)) + "]";
    }
}
