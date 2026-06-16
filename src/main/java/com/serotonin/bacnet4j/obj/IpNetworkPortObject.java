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

import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkUtils;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.IPMode;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class IpNetworkPortObject extends NetworkPortObject {
    private final IpNetwork network;

    public IpNetworkPortObject(IpNetwork network, int instanceNumber, String name, boolean outOfService) {
        super(network.getTransport().getLocalDevice(), instanceNumber, name, outOfService, NetworkType.ipv4,
                ProtocolLevel.bacnetApplication, Set.of());

        if (!network.isInitialized()) {
            throw new IllegalStateException("Network is not initialized");
        }

        this.network = network;

        var mode = network.getIpMode();
        writePropertyInternal(PropertyIdentifier.networkNumber, new UnsignedInteger(network.getLocalNetworkNumber()));
        writePropertyInternal(PropertyIdentifier.networkNumberQuality, NetworkNumberQuality.unknown);
        writePropertyInternal(PropertyIdentifier.apduLength, MaxApduLength.UP_TO_1476.getMaxLength());
        writePropertyInternal(PropertyIdentifier.maxBvlcLengthAccepted, new UnsignedInteger(1497));
        writePropertyInternal(PropertyIdentifier.maxNpduLengthAccepted, new UnsignedInteger(1497));
        writePropertyInternal(PropertyIdentifier.macAddress, IpNetworkUtils.toOctetString(network.getLocalAddress()));
        writePropertyInternal(PropertyIdentifier.bacnetIpMode, mode);
        writePropertyInternal(PropertyIdentifier.bacnetIpUdpPort, new Unsigned16(network.getPort()));
        if (mode == IPMode.bbmd) {
            updateBbmdBroadcastDistributionTable();
            writePropertyInternal(PropertyIdentifier.bbmdAcceptFdRegistrations, Boolean.TRUE);
            writePropertyInternal(PropertyIdentifier.bbmdForeignDeviceTable, new SequenceOf<>());
        } else if (mode == IPMode.foreign) {
            updateFdBbmdAddress();
            updateFdSubscriptionLifetime();
        }
        writePropertyInternal(PropertyIdentifier.ipAddress,
                IpNetworkUtils.toOctetString(network.getLocalAddress().getAddress()));
        writePropertyInternal(PropertyIdentifier.ipSubnetMask, network.getSubnetMask());
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) throws BACnetServiceException {
        super.beforeReadProperty(pid);

        if (network.getIpMode() == IPMode.bbmd) {
            if (pid.equals(PropertyIdentifier.bbmdBroadcastDistributionTable)) {
                updateBbmdBroadcastDistributionTable();
            } else if (pid.equals(PropertyIdentifier.bbmdForeignDeviceTable)) {
                properties.put(PropertyIdentifier.bbmdForeignDeviceTable,
                        new BACnetArray<>(network.getForeignDeviceTable()));
            }
        } else if (network.getIpMode() == IPMode.foreign) {
            if (pid.equals(PropertyIdentifier.fdBbmdAddress)) {
                updateFdBbmdAddress();
            } else if (pid.equals(PropertyIdentifier.fdSubscriptionLifetime)) {
                updateFdSubscriptionLifetime();
            }
        }
    }

    @Override
    protected Reliability evaluateReliability() {
        if (network.getIpMode() == IPMode.foreign && !network.isFdRegistered()) {
            return Reliability.renewFdRegistrationFailure;
        }
        return super.evaluateReliability();
    }

    protected void updateBbmdBroadcastDistributionTable() {
        properties.put(PropertyIdentifier.bbmdBroadcastDistributionTable,
                new BACnetArray<>(network.getBroadcastDistributionTable()));
    }

    protected void updateFdBbmdAddress() {
        properties.put(PropertyIdentifier.fdBbmdAddress, network.getForeignBBMDAddress());
    }

    protected void updateFdSubscriptionLifetime() {
        properties.put(PropertyIdentifier.fdSubscriptionLifetime, network.getForeignTTL());
    }

    @Override
    protected void validateCommandInternal(NetworkPortCommand command) throws BACnetServiceException {
        if (command == NetworkPortCommand.renewFdRegistration && network.getIpMode() == IPMode.foreign) {
            // Allow
            return;
        }

        super.validateCommandInternal(command);
    }

    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);

        if (pid == PropertyIdentifier.command && newValue == NetworkPortCommand.renewFdRegistration) {
            executeCommand(network::tryFdRegister);
        }
    }
}
