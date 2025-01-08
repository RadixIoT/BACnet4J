/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.bacnet4j.adhoc;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.IAmListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkUtils;
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
        Map<Integer, List<String>> interfaceDetails = IpNetworkUtils.getLocalInterfaceAddresses();
        String ifaceName = interfaceDetails.get(0).get(0);
        Network network  = new IpNetworkBuilder()
                .withInterfaceName(ifaceName)
               // .withLocalBindAddress("0.0.0.0")
            //    .withBroadcast("255.255.255.255", 24)
                .withPort(47808)
                .withLocalNetworkNumber(5)
            //    .withReuseAddress(true)
                .build();

        Transport transport = new DefaultTransport(network);
        transport.setTimeout(Transport.DEFAULT_TIMEOUT);
        transport.setSegTimeout(Transport.DEFAULT_SEG_TIMEOUT);
        transport.setSegWindow(Transport.DEFAULT_SEG_WINDOW);
        transport.setRetries(1);

        LocalDevice localDevice = new LocalDevice(99, transport);
        localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.objectName, new CharacterString("Test"));
        //localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.vendorName, new CharacterString("InfiniteAutomation"));
        localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.modelName, new CharacterString("BACnet4J"));

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

        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}
