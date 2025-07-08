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

package com.serotonin.bacnet4j.npdu;

import java.math.BigInteger;

import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Network layer protocol control information. This class currently only implements the reading of information.
 *
 * @author mlohbihler
 */
public class NPCI {
    public static enum NetworkPriority {
        lifeSafety(3), criticalEquipment(2), urgent(1), normal(0);

        public final int value;

        private NetworkPriority(int value) {
            this.value = value;
        }

        // 13.2.5.4
        public static NetworkPriority forEventPriority(int eventPriority) {
            if (eventPriority < 64)
                return lifeSafety;
            if (eventPriority < 128)
                return criticalEquipment;
            if (eventPriority < 192)
                return urgent;
            return normal;
        }
    }


    private final int version;
    private BigInteger control;
    private int destinationNetwork;
    private int destinationLength;
    private byte[] destinationAddress;
    private int sourceNetwork;
    private int sourceLength;
    private byte[] sourceAddress;
    private int hopCount;
    private int messageType;
    private int vendorId;

    /**
     * For sending global broadcasts
     *
     * @param source optional source address
     */
    public NPCI(Address source) {
        version = 1;
        control = BigInteger.valueOf(0);

        control = control.setBit(5);
        destinationNetwork = 0xFFFF;
        hopCount = 0xFF;

        setSourceAddress(source);
    }

    public NPCI(Address destination, Address source, boolean expectsReply) {
        version = 1;
        control = BigInteger.valueOf(0);

        if (destination != null) {
            control = control.setBit(5);
            destinationNetwork = destination.getNetworkNumber().intValue();
            destinationAddress = destination.getMacAddress().getBytes();
            if (destinationAddress != null)
                destinationLength = destinationAddress.length;
            hopCount = 0xFF;
        }

        setSourceAddress(source);

        if (expectsReply)
            control = control.setBit(2);
    }

    public NPCI(Address destination, Address source, boolean expectsReply, int messageType, int vendorId) {
        this(destination, source, expectsReply);

        control = control.setBit(7);
        this.messageType = messageType;
        this.vendorId = vendorId;
    }

    public NPCI priority(NetworkPriority priority) {
        return priority(priority.value);
    }

    public NPCI priority(int priority) {
        if ((priority & 1) == 1)
            control = control.setBit(0);
        else
            control = control.clearBit(0);

        if ((priority & 2) == 2)
            control = control.setBit(1);
        else
            control = control.clearBit(1);

        return this;
    }

    private void setSourceAddress(Address source) {
        if (source != null) {
            control = control.setBit(3);
            sourceNetwork = source.getNetworkNumber().intValue();
            sourceAddress = source.getMacAddress().getBytes();
            sourceLength = sourceAddress.length;
        }
    }

    public NPCI(ByteQueue queue) {
        version = queue.popU1B();
        control = BigInteger.valueOf(queue.popU1B());

        if (control.testBit(5)) {
            destinationNetwork = queue.popU2B();
            destinationLength = queue.popU1B();
            if (destinationLength > 0) {
                destinationAddress = new byte[destinationLength];
                queue.pop(destinationAddress);
            }
        }

        if (control.testBit(3)) {
            sourceNetwork = queue.popU2B();
            sourceLength = queue.popU1B();
            sourceAddress = new byte[sourceLength];
            queue.pop(sourceAddress);
        }

        if (control.testBit(5))
            hopCount = queue.popU1B();

        if (control.testBit(7)) {
            messageType = queue.popU1B();
            if (messageType >= 80)
                vendorId = queue.popU2B();
        }
    }

    public void write(ByteQueue queue) {
        queue.push(version);
        queue.push(control.intValue());

        if (control.testBit(5)) {
            queue.pushU2B(destinationNetwork);
            queue.push(destinationLength);
            if (destinationAddress != null)
                queue.push(destinationAddress);
        }

        if (control.testBit(3)) {
            queue.pushU2B(sourceNetwork);
            queue.push(sourceLength);
            queue.push(sourceAddress);
        }

        if (control.testBit(5))
            queue.push(hopCount);

        if (control.testBit(7)) {
            queue.push(messageType);
            if (messageType >= 80)
                queue.pushU2B(vendorId);
        }
    }

    public boolean hasDestinationInfo() {
        return control.testBit(5);
    }

    public boolean isDestinationBroadcast() {
        return destinationLength == 0;
    }

    public boolean hasSourceInfo() {
        return control.testBit(3);
    }

    public boolean isExpectingReply() {
        return control.testBit(2);
    }

    public boolean isNetworkMessage() {
        return control.testBit(7);
    }

    public boolean isVendorSpecificNetworkMessage() {
        return isNetworkMessage() && messageType >= 80;
    }

    public int getNetworkPriority() {
        return (control.testBit(1) ? 2 : 0) | (control.testBit(0) ? 1 : 0);
    }

    public byte[] getDestinationAddress() {
        return destinationAddress;
    }

    public int getDestinationLength() {
        return destinationLength;
    }

    public int getDestinationNetwork() {
        return destinationNetwork;
    }

    public int getHopCount() {
        return hopCount;
    }

    public int getMessageType() {
        return messageType;
    }

    public byte[] getSourceAddress() {
        return sourceAddress;
    }

    public int getSourceLength() {
        return sourceLength;
    }

    public int getSourceNetwork() {
        return sourceNetwork;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getVersion() {
        return version;
    }
    //    
    // public static void main(String[] args) {
    // byte[] b1 = {(byte)0x81,(byte)0xb,(byte)0x0,(byte)0x18};
    // byte[] b2 = {(byte)0x1,(byte)0x8,(byte)0x0,(byte)0x64,(byte)0x1,
    // (byte)0x2,(byte)0x10,(byte)0x0,(byte)0xc4,(byte)0x2,(byte)0x0,(byte)0x7,(byte)0xd0,(byte)0x22,
    // (byte)0x1,(byte)0xe0,(byte)0x91,(byte)0x0,(byte)0x21,(byte)0x23};
    // ByteQueue q = new ByteQueue(b2);
    // new NPCI(q);
    //
    // }
}
