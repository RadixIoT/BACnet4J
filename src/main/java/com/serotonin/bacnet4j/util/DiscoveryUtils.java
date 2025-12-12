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

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class DiscoveryUtils {
    public static void getExtendedDeviceInformation(final LocalDevice localDevice, final RemoteDevice d)
            throws BACnetException {
        final ObjectIdentifier oid = d.getObjectIdentifier();

        // Get the device's supported services
        if (d.getServicesSupported() == null) {
            final ReadPropertyAck supportedServicesAck = (ReadPropertyAck) localDevice
                    .send(d, new ReadPropertyRequest(oid, PropertyIdentifier.protocolServicesSupported)).get();
            d.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, supportedServicesAck.getValue());
        }

        // Uses the readProperties method here because this list will probably be extended.
        final PropertyReferences properties = new PropertyReferences();
        addIfMissing(d, properties, PropertyIdentifier.objectName);
        addIfMissing(d, properties, PropertyIdentifier.protocolVersion);
        addIfMissing(d, properties, PropertyIdentifier.vendorIdentifier);
        addIfMissing(d, properties, PropertyIdentifier.modelName);
        addIfMissing(d, properties, PropertyIdentifier.maxSegmentsAccepted);

        if (properties.size() > 0) {
            // Only send a request if we have to.
            final PropertyValues values = RequestUtils.readProperties(localDevice, d, properties, false, null);

            values.forEach((opr) -> {
                final Encodable value = values.getNullOnError(oid, opr.getPropertyIdentifier());
                d.setDeviceProperty(opr.getPropertyIdentifier(), value);
            });
        }
    }

    private static void addIfMissing(final RemoteDevice d, final PropertyReferences properties,
            final PropertyIdentifier pid) {
        if (d.getDeviceProperty(pid) == null)
            properties.add(d.getObjectIdentifier(), pid);
    }
}
