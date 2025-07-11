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

package com.serotonin.bacnet4j.docker;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkUtils;
import com.serotonin.bacnet4j.service.unconfirmed.IHaveRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;

/**
 * A class for testing network connectivity within a docker container. It receives its configuration from a properties
 * file, the location of which is given as an arg. With that configuration it creates a local device, and then sends
 * unicast and broadcast messages on a regular schedule. When all of this is set up and running it writes the file
 * running.txt to the root directory, to be used as a health check.
 */
public class DockerRemoteDevice {
    static final Logger LOG = LoggerFactory.getLogger(DockerRemoteDevice.class);

    private static final Path RUNNING_FLAG_PATH = Paths.get("/running.txt");

    public static void main(String[] args) throws Exception {
        Files.deleteIfExists(RUNNING_FLAG_PATH);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Cleaning up remote device");
                Files.deleteIfExists(RUNNING_FLAG_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        if (args.length != 1) {
            throw new RuntimeException("Class requires a path to a configuration file as only argument");
        }
        URL url = DockerRemoteDevice.class.getClassLoader().getResource(args[0]);
        if (url == null) {
            throw new RuntimeException("Cannot find file at path " + args[0]);
        }
        LOG.info("Starting remote device from: {}", url);

        String path = url.getPath();
        Properties p = new Properties();
        p.load(new FileReader(path));

        IpNetwork ipNetwork = new IpNetworkBuilder()
                .withLocalBindAddress(p.getProperty("localBindAddress"))
                .withBroadcast(p.getProperty("broadcastAddress"),
                        Integer.parseInt(p.getProperty("networkPrefixLength")))
                .build();
        Address testDeviceAddress =
                new Address(IpNetworkUtils.toOctetString(p.getProperty("testDeviceAddress"), IpNetwork.DEFAULT_PORT));

        LocalDevice ld =
                new LocalDevice(Integer.parseInt(p.getProperty("deviceNumber")), new DefaultTransport(ipNetwork));
        ld.initialize();

        // Schedule the send of broadcast and direct messages, the latter to the given direct message address.
        ld.scheduleAtFixedRate(() -> {
            // Broadcast an IAm
            ld.sendGlobalBroadcast(ld.getIAm());

            // Unicast an IHave
            ld.send(testDeviceAddress, new IHaveRequest(ld.getId(), ld.getId(),
                    ld.getDeviceObject().get(PropertyIdentifier.modelName)));
        }, 4, 2, TimeUnit.SECONDS);

        LOG.info("Remote device {} started at {}", p.getProperty("deviceNumber"), p.get("localBindAddress"));
        Files.createFile(Paths.get("/running.txt"));
    }
}
