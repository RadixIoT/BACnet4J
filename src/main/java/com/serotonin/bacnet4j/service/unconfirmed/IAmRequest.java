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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class IAmRequest extends UnconfirmedRequestService {
    static final Logger LOG = LoggerFactory.getLogger(IAmRequest.class);

    public static final byte TYPE_ID = 0;

    private final ObjectIdentifier iAmDeviceIdentifier;
    private final UnsignedInteger maxAPDULengthAccepted;
    private final Segmentation segmentationSupported;
    private final UnsignedInteger vendorId;

    /**
     * This field allows us to properly implement 16.1.2.
     */
    private boolean isResponseToWhoIs;

    public IAmRequest(final ObjectIdentifier iamDeviceIdentifier, final UnsignedInteger maxAPDULengthAccepted,
            final Segmentation segmentationSupported, final UnsignedInteger vendorId) {
        this.iAmDeviceIdentifier = iamDeviceIdentifier;
        this.maxAPDULengthAccepted = maxAPDULengthAccepted;
        this.segmentationSupported = segmentationSupported;
        this.vendorId = vendorId;
    }

    public IAmRequest withIsResponseToWhoIs(final boolean isResponseToWhoIs) {
        this.isResponseToWhoIs = isResponseToWhoIs;
        return this;
    }

    public boolean isResponseToWhoIs() {
        return isResponseToWhoIs;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void handle(final LocalDevice localDevice, final Address from) {
        if (!ObjectType.device.equals(iAmDeviceIdentifier.getObjectType())) {
            LOG.warn("Received IAm from an object that is not a device from {}", from);
            return;
        }

        // Make sure we're not hearing from ourselves.
        final int myDoi = localDevice.getInstanceNumber();
        final int remoteDoi = iAmDeviceIdentifier.getInstanceNumber();
        if (remoteDoi == myDoi) {
            // Get my bacnet address and compare the addresses
            for (final Address addr : localDevice.getAllLocalAddresses()) {
                if (addr.getMacAddress().equals(from.getMacAddress()))
                    // This is a local address, so ignore.
                    return;
            }
            LOG.warn("Another instance with my device instance ID found at {}", from);
            localDevice.notifySameDeviceIdCallback(from);
        }

        localDevice.updateRemoteDevice(remoteDoi, from);

        var rd = localDevice.getCachedRemoteDevice(remoteDoi);
        if (rd == null) {
            rd = new RemoteDevice(localDevice, remoteDoi, from);
        }
        rd.setDeviceProperty(PropertyIdentifier.maxApduLengthAccepted, maxAPDULengthAccepted);
        rd.setDeviceProperty(PropertyIdentifier.segmentationSupported, segmentationSupported);
        rd.setDeviceProperty(PropertyIdentifier.vendorIdentifier, vendorId);
        var finalRd = rd;
        localDevice.execute(() -> localDevice.getEventHandler().fireIAmReceived(finalRd));
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, iAmDeviceIdentifier);
        write(queue, maxAPDULengthAccepted);
        write(queue, segmentationSupported);
        write(queue, vendorId);
    }

    IAmRequest(final ByteQueue queue) throws BACnetException {
        iAmDeviceIdentifier = read(queue, ObjectIdentifier.class);
        maxAPDULengthAccepted = read(queue, UnsignedInteger.class);
        segmentationSupported = read(queue, Segmentation.class);
        vendorId = read(queue, UnsignedInteger.class);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (iAmDeviceIdentifier == null ? 0 : iAmDeviceIdentifier.hashCode());
        result = PRIME * result + (maxAPDULengthAccepted == null ? 0 : maxAPDULengthAccepted.hashCode());
        result = PRIME * result + (segmentationSupported == null ? 0 : segmentationSupported.hashCode());
        result = PRIME * result + (vendorId == null ? 0 : vendorId.hashCode());
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
        final IAmRequest other = (IAmRequest) obj;
        if (iAmDeviceIdentifier == null) {
            if (other.iAmDeviceIdentifier != null)
                return false;
        } else if (!iAmDeviceIdentifier.equals(other.iAmDeviceIdentifier))
            return false;
        if (maxAPDULengthAccepted == null) {
            if (other.maxAPDULengthAccepted != null)
                return false;
        } else if (!maxAPDULengthAccepted.equals(other.maxAPDULengthAccepted))
            return false;
        if (segmentationSupported == null) {
            if (other.segmentationSupported != null)
                return false;
        } else if (!segmentationSupported.equals(other.segmentationSupported))
            return false;
        if (vendorId == null) {
            if (other.vendorId != null)
                return false;
        } else if (!vendorId.equals(other.vendorId))
            return false;
        return true;
    }
}
