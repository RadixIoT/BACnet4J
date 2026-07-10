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

package com.serotonin.bacnet4j.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.cache.CachePolicies;
import com.serotonin.bacnet4j.cache.RemoteEntityCachePolicy;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class DiscoveryUtilsTest {
    @Test
    public void noUnnecessaryNetworkCalls() throws BACnetException {
        var cachePolicies = mock(CachePolicies.class);
        doReturn(RemoteEntityCachePolicy.NEVER_EXPIRE).when(cachePolicies).getObjectPolicy(anyInt(), any());
        doReturn(RemoteEntityCachePolicy.NEVER_EXPIRE).when(cachePolicies).getPropertyPolicy(anyInt(), any(), any());

        var localDevice = mock(LocalDevice.class);
        doReturn(cachePolicies).when(localDevice).getCachePolicies();

        var remoteDevice = new RemoteDevice(localDevice, 123, new Address(new byte[] {0}));
        remoteDevice.setDeviceProperty(PropertyIdentifier.maxApduLengthAccepted, new UnsignedInteger(50));
        remoteDevice.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        remoteDevice.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, new ServicesSupported());
        remoteDevice.setDeviceProperty(PropertyIdentifier.objectName, new CharacterString("name"));
        remoteDevice.setDeviceProperty(PropertyIdentifier.protocolVersion, new UnsignedInteger(1));
        remoteDevice.setDeviceProperty(PropertyIdentifier.vendorIdentifier, new Unsigned16(165));
        remoteDevice.setDeviceProperty(PropertyIdentifier.modelName, new CharacterString("model"));
        remoteDevice.setDeviceProperty(PropertyIdentifier.maxSegmentsAccepted, new UnsignedInteger(500));

        var remoteDeviceSpy = spy(remoteDevice);
        DiscoveryUtils.getExtendedDeviceInformation(localDevice, remoteDeviceSpy);

        verify(localDevice, never()).send(any(RemoteDevice.class), any(ConfirmedRequestService.class));
        verify(remoteDeviceSpy, never()).setDeviceProperty(any(PropertyIdentifier.class), any());
    }
}
