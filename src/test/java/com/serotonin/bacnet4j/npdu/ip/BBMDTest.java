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

package com.serotonin.bacnet4j.npdu.ip;

import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

import lohbihler.warp.WarpClock;

public class BBMDTest {
    static final Logger LOG = LoggerFactory.getLogger(BBMDTest.class);

    static final int port = 0xBAC0;

    private final WarpClock clock = new WarpClock();
    boolean canRun;

    LDInfo ld11, ld12, ld13;
    LDInfo ld21, ld22, ld23;
    LDInfo ld31, ld32, ld33;
    LDInfo fd151, fd152, fd153;

    DatagramSocket configurer;
    Broadcaster br1, br2, br3;

    @Before
    public void before() throws Exception {
        // On OSX 127.x.x.x are not localhost addresses
        // Can do this from terminal:
        // sudo ifconfig lo0 alias 127.0.1.3
        // sudo ifconfig lo0 alias 127.0.2.1
        // sudo ifconfig lo0 alias 127.0.1.1
        // sudo ifconfig lo0 alias 127.0.2.2
        // sudo ifconfig lo0 alias 127.0.2.3
        // sudo ifconfig lo0 alias 127.0.3.1
        // sudo ifconfig lo0 alias 127.0.3.2
        // sudo ifconfig lo0 alias 127.0.3.3
        // sudo ifconfig lo0 alias 127.0.151.1
        // sudo ifconfig lo0 alias 127.0.152.1
        // sudo ifconfig lo0 alias 127.0.153.1
        // sudo ifconfig lo0 alias 127.0.254.254
        // sudo ifconfig lo0 alias 127.0.1.255
        // sudo ifconfig lo0 alias 127.0.2.255
        // sudo ifconfig lo0 alias 127.0.3.255

        try {
            InetSocketAddress addr = new InetSocketAddress("127.0.1.1", 47808);
            try (DatagramSocket socket = new DatagramSocket(addr)) {
                canRun = true;
            }
        } catch (SocketException e) {
            canRun = false;
        }
        Assume.assumeTrue(canRun);

        ld11 = createLocalDevice(1, 1);
        ld12 = createLocalDevice(1, 2);
        ld13 = createLocalDevice(1, 3);

        ld21 = createLocalDevice(2, 1);
        ld22 = createLocalDevice(2, 2);
        ld23 = createLocalDevice(2, 3);

        ld31 = createLocalDevice(3, 1);
        ld32 = createLocalDevice(3, 2);
        ld33 = createLocalDevice(3, 3);

        // Foreign devices.
        fd151 = createLocalDevice(151, 1);
        fd152 = createLocalDevice(152, 1);
        fd153 = createLocalDevice(153, 1);

        // Separate endpoint used to configure the BDTs in the local device instances.
        configurer = new DatagramSocket(port, InetAddress.getByName("127.0.254.254"));

        br1 = createBroadcaster(1);
        br2 = createBroadcaster(2);
        br3 = createBroadcaster(3);
    }

    @After
    public void after() throws Exception {
        if (!canRun)
            return;

        br1.close();
        br2.close();
        br3.close();

        ld11.ld.terminate();
        ld12.ld.terminate();
        ld13.ld.terminate();
        ld21.ld.terminate();
        ld22.ld.terminate();
        ld23.ld.terminate();
        ld31.ld.terminate();
        ld32.ld.terminate();
        ld33.ld.terminate();

        fd151.ld.terminate();
        fd152.ld.terminate();
        fd153.ld.terminate();

        configurer.close();

        allSockets.clear();
    }

    @Test
    public void noBBMD() {
        // Send some broadcasts
        ld11.ld.sendLocalBroadcast(ld11.ld.getIAm());
        ld12.ld.sendLocalBroadcast(ld12.ld.getIAm());
        ld33.ld.sendLocalBroadcast(new WhoIsRequest());
        clock.plusMillis(400);
        quiesce();

        // Confirm that the above broadcasts were only received on their subnets.
        assertEquals(1, ld11.iamCount()); // IAm from 12
        assertEquals(1, ld12.iamCount()); // IAm from 11
        assertEquals(2, ld13.iamCount()); // IAm from 11 and 12
        assertEquals(0, ld21.iamCount()); // No IAms
        assertEquals(0, ld22.iamCount()); // No IAms
        assertEquals(0, ld23.iamCount()); // No IAms
        assertEquals(2, ld31.iamCount()); // WhoIs
        assertEquals(2, ld32.iamCount()); // WhoIs
        assertEquals(2, ld33.iamCount()); // WhoIs
    }

    @Test
    public void bdt() throws Exception {
        // Write a BDT
        configurer.send(packet(1, "7F000101BAC0FFFFFFFF" + "7F000201BAC0FFFFFFFF", ld11));

        // Get the response
        final DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
        configurer.receive(response);
        assertPacketEquals("810000060000", response);

        // Read a BDT
        configurer.send(packet(2, "", ld11));

        // Get the response
        configurer.receive(response);
        assertPacketEquals("810300187F000101BAC0FFFFFFFF7F000201BAC0FFFFFFFF", response);
    }

    @Test
    public void fdt() throws Exception {
        final InetSocketAddress target = ld11.network.getLocalBindAddress();

        // Set the clock.
        clock.plusSeconds(1);

        // PART 1
        // Write FD registrations
        fd151.network.registerAsForeignDevice(target, 3);
        fd152.network.registerAsForeignDevice(target, 30);
        fd153.network.registerAsForeignDevice(target, 300);

        // Advance the clock one second.
        clock.plusSeconds(1);
        quiesce();

        // Read registrations
        configurer.send(packet(6, "", ld11));

        // Get the response
        final DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
        configurer.receive(response);
        assertPacketEquals("810700227F009701BAC0000300207F009801BAC0001E003B7F009901BAC0012C0149", response);

        // PART 2
        // Delete a registration
        fd153.network.unregisterAsForeignDevice();

        // Delete again. NOTE: this should return a NAK, but there is currently no facility to check. There will
        // instead be an error written to the log.
        fd153.network.unregisterAsForeignDevice();
        quiesce();

        // Read registrations again
        configurer.send(packet(6, "", ld11));

        // Get the response
        configurer.receive(response);
        assertPacketEquals("810700187F009701BAC0000300207F009801BAC0001E003B", response);

        // PART 4
        // Manually register a foreign device.
        fd153.network.getSocket().send(packet(5, "0001", target));
        quiesce();

        // Verify that it is there
        configurer.send(packet(6, "", ld11));
        configurer.receive(response);
        assertPacketEquals("810700227F009701BAC0000300207F009801BAC0001E003B7F009901BAC00001001F", response);

        // Wait for the manual entry to expire.
        // Advance the clock 42 seconds. The TTL was 1s, plus 30s grace, and the expiry thread runs every 10s.
        clock.plus(42, TimeUnit.SECONDS, 1, TimeUnit.SECONDS, 50, 100);

        // Read registrations again
        configurer.send(packet(6, "", ld11));

        // Get the response
        configurer.receive(response);
        assertPacketEquals("810700187F009701BAC0000300147F009801BAC0001E002F", response);
    }

    @Test
    public void broadcasts() throws Exception {
        // ***** Set up the BBMDs *****
        // 127.0.1.1
        String payload = "7F000101BAC0FFFFFFFF"; // Self
        payload += "7F000201BAC0FFFFFFFF"; // 127.0.2.1
        payload += "7F000301BAC0FFFFFF00"; // 127.0.3.1
        configurer.send(packet(1, payload, ld11));

        final DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
        configurer.receive(response);
        assertPacketEquals("810000060000", response);

        // 127.0.2.1
        payload = "7F000101BAC0FFFFFFFF"; // 127.0.1.1
        payload += "7F000201BAC0FFFFFFFF"; // Self
        configurer.send(packet(1, payload, ld21));

        configurer.receive(response);
        assertPacketEquals("810000060000", response);

        // 127.0.2.2
        payload = "7F000202BAC0FFFFFFFF"; // Self
        payload += "7F000301BAC0FFFFFF00"; // 127.0.3.1
        configurer.send(packet(1, payload, ld22));

        configurer.receive(response);
        assertPacketEquals("810000060000", response);

        // 127.0.3.1
        payload = "7F000101BAC0FFFFFFFF"; // 127.0.1.1
        payload += "7F000202BAC0FFFFFFFF"; // 127.0.2.2
        payload += "7F000301BAC0FFFFFF00"; // Self
        configurer.send(packet(1, payload, ld31));

        configurer.receive(response);
        assertPacketEquals("810000060000", response);

        // ***** Foreign device registrations *****
        fd151.network.registerAsForeignDevice(ld11.network.getLocalBindAddress(), 5000);
        fd152.network.registerAsForeignDevice(ld11.network.getLocalBindAddress(), 5000);
        fd153.network.registerAsForeignDevice(ld22.network.getLocalBindAddress(), 5000);
        clock.plusMillis(100);
        quiesce();

        // ********** TEST 1 **********
        // ***** Send a broadcast from 127.0.1.1 *****
        // 127.0.1.255 (broadcaster)
        //   - 127.0.1.1 (original)
        //     - 127.0.2.1 (forward)
        //       - 127.0.2.2 (forward)
        //         - 127.0.153.1 (forward)
        //       - 127.0.2.3 (forward)
        //     - 127.0.3.255 (forward)
        //       - 127.0.3.1 (forward)
        //       - 127.0.3.2 (forward)
        //     - 127.0.3.3 (forward)
        //     - 127.0.151.1 (forward)
        //     - 127.0.152.1 (forward)
        //   - 127.0.1.2 (original)
        //   - 127.0.1.3 (original)
        ld11.ld.sendLocalBroadcast(ld11.ld.getIAm());
        clock.plusMillis(100);
        quiesce();

        // Confirm that the broadcast was received across the B/N network
        assertEquals(0, ld11.iamCount());
        assertEquals(1, ld12.iamCount());
        assertEquals(1, ld13.iamCount());
        assertEquals(1, ld21.iamCount());
        assertEquals(1, ld22.iamCount());
        assertEquals(1, ld23.iamCount());
        assertEquals(1, ld31.iamCount());
        assertEquals(1, ld32.iamCount());
        assertEquals(1, ld33.iamCount());
        assertEquals(1, fd151.iamCount());
        assertEquals(1, fd152.iamCount());
        assertEquals(1, fd153.iamCount());
        reset();

        // ********** TEST 2 **********
        ld12.ld.sendLocalBroadcast(ld12.ld.getIAm());
        clock.plusMillis(100);
        quiesce();

        // Confirm that the broadcast was received across the B/N network
        assertEquals(1, ld11.iamCount());
        assertEquals(0, ld12.iamCount());
        assertEquals(1, ld13.iamCount());
        assertEquals(1, ld21.iamCount());
        assertEquals(1, ld22.iamCount());
        assertEquals(1, ld23.iamCount());
        assertEquals(1, ld31.iamCount());
        assertEquals(1, ld32.iamCount());
        assertEquals(1, ld33.iamCount());
        assertEquals(1, fd151.iamCount());
        assertEquals(1, fd152.iamCount());
        assertEquals(1, fd153.iamCount());
        reset();

        // ********** TEST 3 **********
        ld21.ld.sendLocalBroadcast(ld21.ld.getIAm());
        clock.plusMillis(100);
        quiesce();

        // Confirm that the broadcast was received across the B/N network
        assertEquals(1, ld11.iamCount());
        assertEquals(1, ld12.iamCount());
        assertEquals(1, ld13.iamCount());
        assertEquals(0, ld21.iamCount());
        assertEquals(1, ld22.iamCount());
        assertEquals(1, ld23.iamCount());
        assertEquals(1, ld31.iamCount());
        assertEquals(1, ld32.iamCount());
        assertEquals(1, ld33.iamCount());
        assertEquals(1, fd151.iamCount());
        assertEquals(1, fd152.iamCount());
        assertEquals(1, fd153.iamCount());
        reset();

        // ********** TEST 4 **********
        ld22.ld.sendLocalBroadcast(ld22.ld.getIAm());
        clock.plusMillis(100);
        quiesce();

        // Confirm that the broadcast was received across the B/N network
        assertEquals(1, ld11.iamCount());
        assertEquals(1, ld12.iamCount());
        assertEquals(1, ld13.iamCount());
        assertEquals(1, ld21.iamCount());
        assertEquals(0, ld22.iamCount());
        assertEquals(1, ld23.iamCount());
        assertEquals(1, ld31.iamCount());
        assertEquals(1, ld32.iamCount());
        assertEquals(1, ld33.iamCount());
        assertEquals(1, fd151.iamCount());
        assertEquals(1, fd152.iamCount());
        assertEquals(1, fd153.iamCount());
        reset();

        // ********** TEST 5 **********
        ld31.ld.sendLocalBroadcast(ld31.ld.getIAm());
        clock.plusMillis(100);
        quiesce();

        // Confirm that the broadcast was received across the B/N network
        assertEquals(1, ld11.iamCount());
        assertEquals(1, ld12.iamCount());
        assertEquals(1, ld13.iamCount());
        assertEquals(1, ld21.iamCount());
        assertEquals(1, ld22.iamCount());
        assertEquals(1, ld23.iamCount());
        assertEquals(0, ld31.iamCount());
        assertEquals(1, ld32.iamCount());
        assertEquals(1, ld33.iamCount());
        assertEquals(1, fd151.iamCount());
        assertEquals(1, fd152.iamCount());
        assertEquals(1, fd153.iamCount());
        reset();

        // ********** TEST 6 **********
        // ***** Send a broadcast from 127.0.152.1 *****
        // - 127.0.153.1
        //   - 127.0.2.2 (distribute broadcast to network)
        //     - 127.0.2.1 (forward)
        //       - 127.0.1.x DOES NOT FORWARD HERE, and so does not get to the other foreign devices either. Hmm....
        //     - 127.0.2.3 (forward)
        //     - 127.0.3.1 (forward)
        //     - 127.0.3.2 (forward)
        //     - 127.0.3.3 (forward)
        fd152.ld.sendLocalBroadcast(fd152.ld.getIAm());
        clock.plusMillis(100);
        quiesce();

        // Confirm that the broadcast was received across the B/N network
        assertEquals(1, ld11.iamCount());
        assertEquals(1, ld12.iamCount());
        assertEquals(1, ld13.iamCount());
        assertEquals(1, ld21.iamCount());
        assertEquals(1, ld22.iamCount());
        assertEquals(1, ld23.iamCount());
        assertEquals(1, ld31.iamCount());
        assertEquals(1, ld32.iamCount());
        assertEquals(1, ld33.iamCount());
        assertEquals(1, fd151.iamCount());
        assertEquals(0, fd152.iamCount());
        assertEquals(1, fd153.iamCount());
        reset();

        // ********** TEST 7 **********
        // ***** Send a broadcast from 127.0.153.1 *****
        // - 127.0.153.1
        //   - 127.0.2.2 (distribute broadcast to network)
        //     - 127.0.2.1 (forward)
        //       - 127.0.1.x DOES NOT FORWARD HERE, and so does not get to the other foreign devices either. Hmm....
        //     - 127.0.2.3 (forward)
        //     - 127.0.3.1 (forward)
        //     - 127.0.3.2 (forward)
        //     - 127.0.3.3 (forward)
        fd153.ld.sendLocalBroadcast(fd153.ld.getIAm());
        clock.plusMillis(100);
        quiesce();

        // Confirm that the broadcast was received across the B/N network
        assertEquals(0, ld11.iamCount());
        assertEquals(0, ld12.iamCount());
        assertEquals(0, ld13.iamCount());
        assertEquals(1, ld21.iamCount());
        assertEquals(1, ld22.iamCount());
        assertEquals(1, ld23.iamCount());
        assertEquals(1, ld31.iamCount());
        assertEquals(1, ld32.iamCount());
        assertEquals(1, ld33.iamCount());
        assertEquals(0, fd151.iamCount());
        assertEquals(0, fd152.iamCount());
        assertEquals(0, fd153.iamCount());
        reset();
    }

    void reset() {
        // Reset all of the LDs
        ld11.reset();
        ld12.reset();
        ld13.reset();
        ld21.reset();
        ld22.reset();
        ld23.reset();
        ld31.reset();
        ld32.reset();
        ld33.reset();
        fd151.reset();
        fd152.reset();
        fd153.reset();
    }

    private static void assertPacketEquals(final String expected, final DatagramPacket packet) {
        final byte[] exp = new ByteQueue(expected).popAll();
        assertEquals(exp.length, packet.getLength());
        final byte[] actual = packet.getData();
        for (int i = 0; i < exp.length; i++)
            assertEquals("Byte mismatch at index " + i + ": expected " + Integer.toHexString(
                    exp[i] & 0xFF) + " but got " + Integer.toHexString(actual[i] & 0xFF), exp[i], actual[i]);
    }

    private static DatagramPacket packet(final int function, final String payload, final LDInfo dest) {
        return packet(function, payload, dest.network.getLocalBindAddress());
    }

    private static DatagramPacket packet(final int function, final String payload, final InetSocketAddress dest) {
        final ByteQueue queue = new ByteQueue();
        queue.push(0x81); // Type
        queue.push(function); // Function
        queue.pushU2B(4 + payload.length() / 2); // Length
        queue.push(payload);
        final byte[] data = queue.popAll();
        return new DatagramPacket(data, data.length, dest);
    }

    private LDInfo createLocalDevice(final int subnet, final int addr) throws Exception {
        final LDInfo info = new LDInfo();

        info.network = new IpNetworkBuilder().withLocalBindAddress("127.0." + subnet + "." + addr) //
                .withSubnet("127.0." + subnet + ".0", 24) //
                .withLocalNetworkNumber(1) //
                .build();
        info.network.enableBBMD();

        info.ld = new LocalDevice(subnet * 10 + addr, new DefaultTransport(info.network)) //
                .withClock(clock) //
                .initialize();

        info.iamCount = new MutableInt();

        info.ld.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void iAmReceived(final RemoteDevice d) {
                info.iamCount.increment();
            }
        });

        allSockets.add(info.network.getSocket());

        return info;
    }

    Broadcaster createBroadcaster(final int subnet) throws IOException {
        final String ip = "127.0." + subnet + ".255";
        final DatagramSocket s = new DatagramSocket(IpNetwork.DEFAULT_PORT, InetAddress.getByName(ip));

        // Find all the sockets on the same virtual subnet.
        final List<DatagramSocket> to = new ArrayList<>();
        for (final DatagramSocket dev : allSockets) {
            if (dev.getLocalAddress().getAddress()[2] == subnet)
                to.add(dev);
        }

        final Broadcaster br = new Broadcaster(ip, s, to);
        final Thread t = new Thread(br, "Broadcaster " + subnet);
        //        t.setDaemon(true);
        t.start();
        return br;
    }

    List<DatagramSocket> allSockets = new ArrayList<>();


    static class LDInfo {
        LocalDevice ld;
        IpNetwork network;
        MutableInt iamCount;

        void reset() {
            ld.clearRemoteDevices();
            iamCount.setValue(0);
        }

        int iamCount() {
            return iamCount.intValue();
        }
    }


    /**
     * Simulates broadcasting by knowing all the sockets on a particular virtual subnet, and listening for messages on a
     * virtual broadcast address. When it receives a message, it
     */
    class Broadcaster implements Runnable {
        final String ip;
        final DatagramSocket listenAt;
        final List<DatagramSocket> forwardTo;
        Thread thread;

        public Broadcaster(final String ip, final DatagramSocket listenAt, final List<DatagramSocket> forwardTo) {
            this.ip = ip;
            this.listenAt = listenAt;
            this.forwardTo = forwardTo;
        }

        @Override
        public void run() {
            thread = Thread.currentThread();

            final DatagramPacket p = new DatagramPacket(new byte[1024], 1024);

            while (true) {
                try {
                    listenAt.receive(p);

                    // Determine where this came from.
                    DatagramSocket from = null;
                    for (final DatagramSocket s : allSockets) {
                        if (s.getLocalAddress().equals(p.getAddress())) {
                            from = s;
                            break;
                        }
                    }

                    if (from == null)
                        System.out.println("Can't find from socket for address" + p.getAddress());
                    else {
                        // Use that socket to send to individual addresses.
                        for (final DatagramSocket to : forwardTo) {
                            final DatagramPacket fwd =
                                    new DatagramPacket(p.getData(), p.getLength(), to.getLocalSocketAddress());
                            from.send(fwd);
                        }
                    }
                } catch (final SocketException e) {
                    if ("Socket closed".equalsIgnoreCase(e.getMessage()))
                        break;

                    // ignore
                    LOG.error("", e);
                } catch (final IOException e) {
                    LOG.error("", e);
                }
            }
        }

        public void close() throws Exception {
            listenAt.close();
            if (thread != null)
                thread.join();
        }
    }
}
