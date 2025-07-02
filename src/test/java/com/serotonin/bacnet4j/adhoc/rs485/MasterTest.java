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

package com.serotonin.bacnet4j.adhoc.rs485;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.IAmListener;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.mstp.Constants;
import com.serotonin.bacnet4j.npdu.mstp.MasterNode;
import com.serotonin.bacnet4j.npdu.mstp.MstpNetwork;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.DiscoveryUtils;
import com.serotonin.bacnet4j.util.JsscSerialPortInputStream;
import com.serotonin.bacnet4j.util.JsscSerialPortOutputStream;

import jssc.SerialPort;

/**
 * Run up an MSTP Master and do a WhoIs
 */
public class MasterTest {

    public static void main(final String[] args) throws Exception {
        //Setup Serial Port
        final SerialPort serialPort = new SerialPort("/dev/cu.usbserial-A101OGX5");
        boolean b = serialPort.openPort();
        b = serialPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        try (JsscSerialPortInputStream in = new JsscSerialPortInputStream(serialPort);
                JsscSerialPortOutputStream out = new JsscSerialPortOutputStream(serialPort)) {
            //Create Local Device
            MasterNode masterNode = new MasterNode("test", in, out, (byte) 0, 1);
            masterNode.setMaxInfoFrames(Constants.MAX_INFO_FRAMES);
            masterNode.setMaxMaster(Constants.MAX_MASTER);
            masterNode.setUsageTimeout(Constants.USAGE_TIMEOUT);
            Network network = new MstpNetwork(masterNode, 0);
            Transport transport = new DefaultTransport(network);
            transport.setTimeout(Transport.DEFAULT_TIMEOUT);
            transport.setSegTimeout(Transport.DEFAULT_SEG_TIMEOUT);
            transport.setSegWindow(Transport.DEFAULT_SEG_WINDOW);
            transport.setRetries(Transport.DEFAULT_RETRIES);

            LocalDevice localDevice = new LocalDevice(0, transport);
            localDevice.getDeviceObject()
                    .writePropertyInternal(PropertyIdentifier.objectName, new CharacterString("Test"));
            //localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.vendorName, new CharacterString("InfiniteAutomation"));
            localDevice.getDeviceObject()
                    .writePropertyInternal(PropertyIdentifier.modelName, new CharacterString("BACnet4J"));


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

        } finally {
            serialPort.closePort();
        }
    }
}
