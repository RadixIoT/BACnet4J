/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.obj;

import java.util.Set;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.npdu.ipv6.Ipv6Network;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.IPMode;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class Ipv6NetworkPortObject extends NetworkPortObject {
    private final Ipv6Network network;

    public Ipv6NetworkPortObject(LocalDevice localDevice, Ipv6Network network, int instanceNumber) {
        super(localDevice, instanceNumber, network.getNetworkIdentifier().getIdString(),
                false, NetworkType.ipv6, ProtocolLevel.bacnetApplication, Set.of());

        if (network.isInitialized()) {
            throw new IllegalStateException("Network is already initialized");
        }

        this.network = network;

        writePropertyInternal(PropertyIdentifier.networkNumber, new Unsigned16(network.getLocalNetworkNumber()));
        writePropertyInternal(PropertyIdentifier.networkNumberQuality, NetworkNumberQuality.unknown);
        writePropertyInternal(PropertyIdentifier.apduLength, MaxApduLength.UP_TO_1476.getMaxLength());
        writePropertyInternal(PropertyIdentifier.maxBvlcLengthAccepted, new UnsignedInteger(1497));
        writePropertyInternal(PropertyIdentifier.maxNpduLengthAccepted, new UnsignedInteger(1497));
        writePropertyInternal(PropertyIdentifier.virtualMacAddressTable, new SequenceOf<>());
        writePropertyInternal(PropertyIdentifier.bacnetIpv6Mode, IPMode.normal);
        writePropertyInternal(PropertyIdentifier.bacnetIpv6UdpPort, new Unsigned16(network.getPort()));
    }

    @Override
    protected void initializeImpl() {
        writePropertyInternal(PropertyIdentifier.macAddress, network.getLocalVMAC());
        writePropertyInternal(PropertyIdentifier.bacnetIpv6MulticastAddress, network.getMulticastMAC());

        super.initializeImpl();
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) {
        if (pid.equals(PropertyIdentifier.virtualMacAddressTable) && !isChanged()) {
            writePropertyInternal(PropertyIdentifier.virtualMacAddressTable,
                    new SequenceOf<>(network.getVirtualMacAddressTable()));
        }
    }
}
