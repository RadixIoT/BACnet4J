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

package com.serotonin.bacnet4j.adhoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.IAmListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.DiscoveryUtils;

/**
 * Example of creating an Ip Master
 */
public class IpMasterTest {

    static final Logger LOG = LoggerFactory.getLogger(IpMasterTest.class);

    public static void main(final String[] args) throws Exception {
        new IpMasterTest().runWhoIs();
    }

    public LocalDevice createIpLocalDevice() throws Exception {
        Network network = network = new IpNetworkBuilder()
                .withLocalBindAddress("0.0.0.0")
                .withBroadcast("255.255.255.255", 24)
                .withPort(47808)
                .withLocalNetworkNumber(5)
                .withReuseAddress(true)
                .build();

        Transport transport = new DefaultTransport(network);
        transport.setTimeout(Transport.DEFAULT_TIMEOUT);
        transport.setSegTimeout(Transport.DEFAULT_SEG_TIMEOUT);
        transport.setSegWindow(Transport.DEFAULT_SEG_WINDOW);
        transport.setRetries(1);

        LocalDevice localDevice = new LocalDevice(99, transport);
        localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.objectName, new CharacterString("Test"));
        //localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.vendorName, new CharacterString("InfiniteAutomation"));
        localDevice.getDeviceObject()
                .writePropertyInternal(PropertyIdentifier.modelName, new CharacterString("BACnet4J"));

        return localDevice;
    }

    public void runWhoIs() {
        try {

            LocalDevice localDevice = createIpLocalDevice();
            //Create listener
            IAmListener listener = (RemoteDevice d) -> {
                try {
                    System.out.println("Found device" + d);
                    DiscoveryUtils.getExtendedDeviceInformation(localDevice, d);
                } catch (BACnetException e) {
                    e.printStackTrace();
                }
            };

            localDevice.getEventHandler().addListener(listener);

            localDevice.initialize();

            //Send WhoIs
            localDevice.sendGlobalBroadcast(new WhoIsRequest());

            //Wait for responses
            int count = 30;
            while (count > 0) {
                Thread.sleep(1000);
                count--;
            }
            localDevice.terminate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
