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

package com.serotonin.bacnet4j.service.unconfirmed;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class WhoIsRequest extends UnconfirmedRequestService {
    private static final Logger LOG = LoggerFactory.getLogger(WhoIsRequest.class);

    public static final byte TYPE_ID = 8;

    private UnsignedInteger deviceInstanceRangeLowLimit;
    private UnsignedInteger deviceInstanceRangeHighLimit;

    public WhoIsRequest() {
        // no op
    }

    public WhoIsRequest(int deviceInstanceRangeLowLimit, int deviceInstanceRangeHighLimit) {
        this(new UnsignedInteger(deviceInstanceRangeLowLimit), new UnsignedInteger(deviceInstanceRangeHighLimit));
    }

    public WhoIsRequest(UnsignedInteger deviceInstanceRangeLowLimit, UnsignedInteger deviceInstanceRangeHighLimit) {
        this.deviceInstanceRangeLowLimit = deviceInstanceRangeLowLimit;
        this.deviceInstanceRangeHighLimit = deviceInstanceRangeHighLimit;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void handle(LocalDevice localDevice, Address from) throws BACnetException {
        int instanceId = localDevice.getInstanceNumber();

        // Check if we're in the device id range.
        if (deviceInstanceRangeLowLimit != null && instanceId < deviceInstanceRangeLowLimit.intValue())
            return;

        if (deviceInstanceRangeHighLimit != null && instanceId > deviceInstanceRangeHighLimit.intValue())
            return;

        if (localDevice.isUnconfigured()) {
            // All three properties must be defined to send WhoAmI.
            Unsigned16 vendorId = localDevice.get(PropertyIdentifier.vendorIdentifier);
            CharacterString modelName = localDevice.get(PropertyIdentifier.modelName);
            CharacterString serialNumber = localDevice.get(PropertyIdentifier.serialNumber);
            if (vendorId != null && modelName != null && serialNumber != null) {
                var whoAmI = new WhoAmIRequest(vendorId, modelName, serialNumber);
                localDevice.send(from, whoAmI);
            } else {
                LOG.warn("Not configured to send WhoAmI: vendorId={}, modelName={}, serialNumber={}",
                        vendorId, modelName, serialNumber);
            }
        } else {
            // Return the result in an iAm message.
            IAmRequest iam = localDevice.getIAm().withIsResponseToWhoIs(true);
            localDevice.sendGlobalBroadcast(iam);
        }
    }

    @Override
    public void write(ByteQueue queue) {
        writeOptional(queue, deviceInstanceRangeLowLimit, 0);
        writeOptional(queue, deviceInstanceRangeHighLimit, 1);
    }

    WhoIsRequest(ByteQueue queue) throws BACnetException {
        deviceInstanceRangeLowLimit = readOptional(queue, UnsignedInteger.class, 0);
        deviceInstanceRangeHighLimit = readOptional(queue, UnsignedInteger.class, 1);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        WhoIsRequest that = (WhoIsRequest) o;
        return Objects.equals(deviceInstanceRangeLowLimit,
                that.deviceInstanceRangeLowLimit) && Objects.equals(deviceInstanceRangeHighLimit,
                that.deviceInstanceRangeHighLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceInstanceRangeLowLimit, deviceInstanceRangeHighLimit);
    }
}
