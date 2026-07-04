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

import java.util.Arrays;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.mstp.ManagerNode;
import com.serotonin.bacnet4j.npdu.mstp.MstpNetwork;
import com.serotonin.bacnet4j.npdu.mstp.SubordinateNode;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.util.JsscSerialPortInputStream;
import com.serotonin.bacnet4j.util.JsscSerialPortOutputStream;
import com.serotonin.bacnet4j.util.RemoteDeviceDiscoverer;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

public class PortTest {
    public static void main(String[] args) throws Exception {
        System.out.println(Arrays.toString(SerialPortList.getPortNames()));

        SerialPort serialPort = new SerialPort("/dev/cu.usbserial-A101OGX5");
        boolean b = serialPort.openPort();
        System.out.println(b);
        b = serialPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        System.out.println(b);

        //        listen(serialPort);
        //        subordinate(serialPort);
        manager(serialPort);

        b = serialPort.closePort();
        System.out.println(b);
    }

    static void manager(SerialPort serialPort) throws Exception {
        try (JsscSerialPortInputStream in = new JsscSerialPortInputStream(serialPort);
                JsscSerialPortOutputStream out = new JsscSerialPortOutputStream(serialPort)) {
            ManagerNode node = new ManagerNode("test", in, out, (byte) 3, 2);
            node.setMaxInfoFrames(5);
            node.setUsageTimeout(100);
            MstpNetwork network = new MstpNetwork(node, 0);
            Transport transport = new DefaultTransport(network);
            LocalDevice ld = new LocalDevice(1970, transport);
            ld.initialize();
            System.out.println(prefix() + "Initialized");

            for (int i = 0; i < 10; i++) {
                System.out.println(prefix() + "Discovering");
                RemoteDeviceDiscoverer rdd = ld.startRemoteDeviceDiscovery((r) -> {
                    System.out.println(prefix() + "Device: " + r + ", " + r.getName());
                });

                ThreadUtils.sleep(6000);

                System.out.println(rdd.getRemoteDevices());
                rdd.stop();
            }

            //            System.out.println(node.getBytesIn());
            //            System.out.println(node.getBytesOut());

            System.out.println(prefix() + "Terminating");
            ld.terminate();
        }
    }

    static void subordinate(SerialPort serialPort) throws Exception {
        try (JsscSerialPortInputStream in = new JsscSerialPortInputStream(serialPort);
                JsscSerialPortOutputStream out = new JsscSerialPortOutputStream(serialPort)) {
            SubordinateNode node = new SubordinateNode("test", in, out, (byte) 3);
            MstpNetwork network = new MstpNetwork(node, 0);
            Transport transport = new DefaultTransport(network);
            LocalDevice ld = new LocalDevice(1968, transport);
            ld.initialize();

            ld.startRemoteDeviceDiscovery((r) -> {
                System.out.println(r.getInstanceNumber());
            });

            ThreadUtils.sleep(10000);
            System.out.println(node.getBytesIn());
            System.out.println(node.getBytesOut());

            ld.terminate();
        }
    }

    static void listen(SerialPort serialPort) throws SerialPortException {
        while (true) {
            //             byte[] buf = serialPort.readBytes();
            //            if (buf != null) {
            //                System.out.println(Arrays.toString(buf));
            //            }

            String s = serialPort.readHexString();
            if (s != null) {
                System.out.println(prefix() + s.toLowerCase());
                //            } else {
                //                System.out.println("null was returned");
            }
        }
    }

    static String prefix() {
        return System.currentTimeMillis() % 10000000 + ": ";
    }
}
