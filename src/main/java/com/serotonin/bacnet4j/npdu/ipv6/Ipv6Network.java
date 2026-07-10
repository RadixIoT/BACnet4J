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

package com.serotonin.bacnet4j.npdu.ipv6;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.npdu.MessageValidationException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.NetworkIdentifier;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.VmacEntry;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.BACnetUtils;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class Ipv6Network extends Network implements Runnable {
    static final Logger LOG = LoggerFactory.getLogger(Ipv6Network.class);

    public static final byte BVLC_TYPE = (byte) 0x82;
    public static final int DEFAULT_PORT = 0xBAC0; // == 47808
    public static final String DEFAULT_BIND_ADDRESS = "::";

    private static final int MESSAGE_LENGTH = 2048;

    // Map of VMAC to IPv6 addresses.
    private static final Map<OctetString, OctetString> vmacTable = new HashMap<>();

    private final String multicastAddress;
    private final int port;
    private final String localBindAddress;

    // Runtime
    private MulticastSocket socket;
    private OctetString broadcastMAC;
    private OctetString thisVMAC;
    private long bytesOut;
    private long bytesIn;

    private final List<PendingAddressResolution> pendingAddressResolutions = new CopyOnWriteArrayList<>();

    public Ipv6Network(String multicastAddress) {
        this(multicastAddress, DEFAULT_PORT);
    }

    public Ipv6Network(String multicastAddress, int port) {
        this(multicastAddress, port, DEFAULT_BIND_ADDRESS);
    }

    public Ipv6Network(String multicastAddress, int port, String localBindAddress) {
        this(multicastAddress, port, localBindAddress, 0);
    }

    public Ipv6Network(String multicastAddress, int port, String localBindAddress, int localNetworkNumber) {
        super(localNetworkNumber);
        this.multicastAddress = multicastAddress;
        this.port = port;
        this.localBindAddress = localBindAddress;
    }

    @Override
    public NetworkIdentifier getNetworkIdentifier() {
        return new Ipv6NetworkIdentifier(multicastAddress, port, localBindAddress);
    }

    @Override
    public MaxApduLength getMaxApduLength() {
        return MaxApduLength.UP_TO_1476;
    }

    public String getMulticastAddress() {
        return multicastAddress;
    }

    public int getPort() {
        return port;
    }

    public String getLocalBindAddress() {
        return localBindAddress;
    }

    @Override
    public long getBytesOut() {
        return bytesOut;
    }

    @Override
    public long getBytesIn() {
        return bytesIn;
    }

    @Override
    public boolean isInitialized() {
        return thisVMAC != null;
    }

    public OctetString getLocalVMAC() {
        return thisVMAC;
    }

    public List<VmacEntry> getVirtualMacAddressTable() {
        return vmacTable.entrySet().stream().map(e -> new VmacEntry(e.getKey(), e.getValue())).toList();
    }

    public OctetString getMulticastMAC() {
        return broadcastMAC;
    }

    @Override
    public void initialize(Transport transport) throws BACnetException {
        super.initialize(transport);

        InetAddress ia;
        try {
            if (DEFAULT_BIND_ADDRESS.equals(localBindAddress))
                socket = new MulticastSocket(port);
            else
                socket = new MulticastSocket(new InetSocketAddress(InetAddress.getByName(localBindAddress), port));
            ia = InetAddress.getByName(multicastAddress);
            socket.joinGroup(ia);
        } catch (IOException e) {
            throw new BACnetException("Failed to initialize Ipv6 network", e);
        }

        broadcastMAC = Ipv6NetworkUtils.toOctetString(ia.getAddress(), port);

        thisVMAC = BACnetUtils.toVirtualAddressBytes(transport.getLocalDevice().getInstanceNumber());

        try {
            vmacTable.put(thisVMAC, Ipv6NetworkUtils.toOctetString(InetAddress.getByName("::1").getAddress(), port));
        } catch (UnknownHostException e) {
            // Should never happen
            throw new BACnetRuntimeException(e);
        }

        new Thread(this, "BACnet4J IPv6 socket listener").start();
    }

    @Override
    public void terminate() {
        if (socket != null)
            socket.close();
    }

    @Override
    protected OctetString getBroadcastMAC() {
        return broadcastMAC;
    }

    /**
     * Sends a foreign device registration request to addr. On successful registration (ACK), we are added
     * in the foreign device table (FDT).
     *
     * @param addr       The address of the device where our device wants to be registered as foreign device
     * @param timeToLive The time until we are automatically removed out of the FDT.
     * @throws BACnetException if the packet can't be sent
     */
    public void sendRegisterForeignDeviceMessage(InetSocketAddress addr, int timeToLive) throws BACnetException {
        ByteQueue queue = new ByteQueue();
        queue.push(BVLC_TYPE);
        queue.push(0x09); // Register-Foreign-Device
        queue.pushU2B(9); // Length
        queue.push(thisVMAC.getBytes()); // Source
        queue.pushU2B(timeToLive); // TTL
        sendPacket(addr, queue.popAll());
    }

    @Override
    public void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast, boolean expectsReply)
            throws BACnetException {
        LOG.debug("Sending npdu: recipient={}, router={}, npdu={}, broadcast={}", recipient, router, npdu, broadcast);

        ByteQueue queue = new ByteQueue();

        // BACnet virtual link layer detail
        queue.push(BVLC_TYPE);

        // Original-Unicast-NPDU, or Original-Broadcast-NPDU
        queue.push(broadcast ? 0x2 : 0x1);

        // Length
        queue.pushU2B(queue.size() + npdu.size() + (broadcast ? 5 : 8));

        // Write the source virtual address
        queue.push(thisVMAC.getBytes());

        if (!broadcast)
            // Write the destination virtual address
            queue.push(recipient.getMacAddress().getBytes());

        // Combine the queues
        queue.push(npdu);

        OctetString dest = getDestination(recipient, router);

        // Get the IP address for this destination.
        if (broadcast)
            sendPacket(Ipv6NetworkUtils.getInetSocketAddress(dest), queue.popAll());
        else {
            OctetString ipAddr = vmacTable.get(dest);
            if (ipAddr == null) {
                purgePendingAddressResolutions();

                // The IP address for this destination is not known. Queue the message and send an address
                // resolution request.
                PendingAddressResolution par = new PendingAddressResolution();
                par.destination = dest;
                par.data = queue.popAll();
                pendingAddressResolutions.add(par);

                ByteQueue req = new ByteQueue();
                req.push(BVLC_TYPE);
                req.push(0x3); // Function
                req.pushU2B(0xa); // Length
                req.push(thisVMAC.getBytes()); // Source
                req.push(dest.getBytes()); // Destination
                sendPacket(Ipv6NetworkUtils.getInetSocketAddress(broadcastMAC), req.popAll());
            } else
                // The IP address is known. Send the message now.
                sendPacket(Ipv6NetworkUtils.getInetSocketAddress(ipAddr), queue.popAll());
        }
    }

    private void sendPacket(InetSocketAddress addr, byte[] data) throws BACnetException {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending datagram to {}: {}", addr, StreamUtils.dumpArrayHex(data));
            }
            DatagramPacket packet = new DatagramPacket(data, data.length, addr);
            socket.send(packet);
            bytesOut += data.length;
        } catch (IOException e) {
            throw new BACnetException(e);
        }
    }

    //
    // For receiving
    @Override
    public void run() {
        byte[] buffer = new byte[MESSAGE_LENGTH];
        DatagramPacket p = new DatagramPacket(buffer, buffer.length);

        while (!socket.isClosed()) {
            try {
                socket.receive(p);

                bytesIn += p.getLength();
                ByteQueue queue = new ByteQueue(p.getData(), p.getOffset(), p.getLength());
                OctetString link = Ipv6NetworkUtils.toOctetString(p.getAddress().getAddress(), p.getPort());

                LOG.debug("Received datagram from {}: {}", link, queue);

                handleIncomingData(queue, link);

                // Reset the packet.
                p.setData(buffer);
            } catch (@SuppressWarnings("unused") IOException e) {
                // no op. This happens if the socket gets closed by the destroy method.
            }
        }
    }

    @Override
    protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString fromIpv6) throws BACnetException {
        if (queue.size() < 4)
            throw new MessageValidationException("Message too short to be BACnet/IPv6: " + queue + " from " + fromIpv6);

        // Initial parsing of IP message.
        // BACnet/IPv6
        if (queue.pop() != BVLC_TYPE)
            throw new MessageValidationException("Protocol id is not BACnet/IPv6 (0x82)");

        byte function = queue.pop();

        int length = BACnetUtils.popShort(queue);
        if (length != queue.size() + 4)
            throw new MessageValidationException(
                    "Length field does not match data: given=" + length + ", expected=" + (queue.size() + 4));

        OctetString sourceVMAC = BACnetUtils.popDeviceId(queue);

        // Add the resolution to the table
        vmacTable.put(sourceVMAC, fromIpv6);

        NPDU npdu = null;

        if (function == 0x0) {
            // BVLC-Result. Currently can only be the answer to the foreign device registration request.
            int result = BACnetUtils.popShort(queue);
            if (result != 0)
                // TODO need to do something better here.
                LOG.warn("Foreign device registration not successful! result: {}", result);
        } else if (function == 0x1 || function == 0x2) {
            // Original-Unicast-NPDU or Original-Broadcast-NPDU
            if (function == 0x1) // Unicast only
                BACnetUtils.popDeviceId(queue);
            npdu = parseNpduData(queue, sourceVMAC);
        } else if (function == 0x3 || function == 0x4) {
            // Address-Resolution or Forwarded-Address-Resolution
            OctetString targetVMAC = BACnetUtils.popDeviceId(queue);

            // How is this source address used?
            byte[] sourceIpv6Address;
            if (function == 0x4) {
                sourceIpv6Address = new byte[18];
                queue.pop(sourceIpv6Address);
            }

            if (thisVMAC.equals(targetVMAC)) {
                // This is the device you are looking for. Reply with an Address-Resolution-Ack
                ByteQueue ack = new ByteQueue();
                ack.push(BVLC_TYPE);
                ack.push(0x5); // Function
                ack.pushU2B(0xa); // Length
                ack.push(thisVMAC.getBytes()); // Source
                ack.push(sourceVMAC.getBytes()); // Destination
                sendPacket(Ipv6NetworkUtils.getInetSocketAddress(fromIpv6), ack.popAll());
            }
        } else if (function == 0x5) {
            // Address-Resolution-Ack. Check the pending list for matching requests.
            BACnetUtils.popDeviceId(queue);

            List<PendingAddressResolution> matched = null;
            for (PendingAddressResolution par : pendingAddressResolutions) {
                if (par.destination.equals(sourceVMAC)) {
                    if (matched == null)
                        matched = new ArrayList<>();
                    matched.add(par);
                    sendPacket(Ipv6NetworkUtils.getInetSocketAddress(fromIpv6), par.data);
                }
            }
            if (matched != null)
                pendingAddressResolutions.removeAll(matched);
        } else if (function == 0x6) {
            // Virtual-Address-Resolution. Reply with a Virtual-Address-Resolution-Ack
            ByteQueue ack = new ByteQueue();
            ack.push(BVLC_TYPE);
            ack.push(0x7); // Function
            ack.pushU2B(0xa); // Length
            ack.push(thisVMAC.getBytes()); // Source
            ack.push(sourceVMAC.getBytes()); // Destination
            sendPacket(Ipv6NetworkUtils.getInetSocketAddress(fromIpv6), queue.popAll());
        } else if (function == 0x7) {
            // Virtual-Address-Resolution-Ack.
            BACnetUtils.popDeviceId(queue);
        } else if (function == 0x8) {
            // Forwarded-NPDU. Use the address/port as the link service address.
            byte[] addr = new byte[18];
            queue.pop(addr);
            vmacTable.put(sourceVMAC, new OctetString(addr));
            npdu = parseNpduData(queue, sourceVMAC);
        } else
            throw new MessageValidationException(
                    "Unhandled BVLC function type: 0x" + Integer.toHexString(function & 0xff));

        return npdu;
    }

    //
    //
    // Convenience methods
    //
    @Override
    public Address[] getAllLocalAddresses() {
        return new Address[] {new Address(getLocalNetworkNumber(), thisVMAC)};
    }

    @Override
    public Address getLoopbackAddress() {
        return new Address(getLocalNetworkNumber(), thisVMAC);
    }

    private void purgePendingAddressResolutions() {
        long now = getTransport().getLocalDevice().getClock().millis();
        List<PendingAddressResolution> toRemove = null;
        for (PendingAddressResolution par : pendingAddressResolutions) {
            if (par.deadline < now) {
                if (toRemove == null)
                    toRemove = new ArrayList<>();
                toRemove.add(par);
            }
        }
        if (toRemove != null)
            pendingAddressResolutions.removeAll(toRemove);
    }

    class PendingAddressResolution {
        OctetString destination;
        byte[] data;
        final long deadline = getTransport().getLocalDevice().getClock().millis() + 1000 * 10;
    }
}
