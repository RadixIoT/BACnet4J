/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 * @Author Terry Packer
 *
 */

package com.serotonin.bacnet4j.adhoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.IAmListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.DiscoveryUtils;

/**
 * This example will only work if the underlying implementation of the java DatagramSocket supports receiving
 *   broadcasts on addresses other than the wildcard address. i.e. Windows
 */
public class MultipleLocalDevices {

    static final Logger LOG = LoggerFactory.getLogger(MultipleLocalDevices.class);

    public static void main(final String[] args) throws Exception {
        String bindAddress1 = "0.0.0.0";
        String bindAddress2 = "192.168.1.100";

        //Configure the first network, ensure we set reuse address
        IpNetwork networkOne = new IpNetworkBuilder()
                .withLocalBindAddress(bindAddress1)
                .withBroadcast("255.255.255.255", 24)
                .withLocalNetworkNumber(1).withPort(47808).build();
        Transport transportOne = new DefaultTransport(networkOne);
        LocalDevice localDeviceOne = new LocalDevice(1, transportOne);

        localDeviceOne.getDeviceObject().writePropertyInternal(PropertyIdentifier.objectName, new CharacterString("DeviceOne"));
        localDeviceOne.getDeviceObject().writePropertyInternal(PropertyIdentifier.modelName, new CharacterString("BACnet4J"));

        //Create listener
        IAmListener listenerOne = (RemoteDevice d) -> {
            try {
                LOG.info("Local device one found device {}", d);
                DiscoveryUtils.getExtendedDeviceInformation(localDeviceOne, d);
            } catch (BACnetException e) {
                e.printStackTrace();
            }
        };

        localDeviceOne.getEventHandler().addListener(listenerOne);

        IpNetwork networkTwo = new IpNetworkBuilder()
                .withLocalBindAddress(bindAddress2)
                .withBroadcast("255.255.255.255", 24)
                .withLocalNetworkNumber(1).withPort(47808).build();
        Transport transportTwo = new DefaultTransport(networkTwo);
        LocalDevice localDeviceTwo = new LocalDevice(2, transportTwo);

        localDeviceTwo.getDeviceObject().writePropertyInternal(PropertyIdentifier.objectName, new CharacterString("DeviceTwo"));
        localDeviceTwo.getDeviceObject().writePropertyInternal(PropertyIdentifier.modelName, new CharacterString("BACnet4J"));

        //Create listener
        IAmListener listenerTwo = (RemoteDevice d) -> {
            try {
                LOG.info("Local device two found device {}", d);
                DiscoveryUtils.getExtendedDeviceInformation(localDeviceTwo, d);
            } catch (BACnetException e) {
                e.printStackTrace();
            }
        };

        localDeviceTwo.getEventHandler().addListener(listenerTwo);


        localDeviceOne.initialize();
        localDeviceTwo.initialize();

        localDeviceOne.sendGlobalBroadcast(new WhoIsRequest());

        Thread.sleep(100000);
    }

}
