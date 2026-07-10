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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.SystemUtils;
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
import com.serotonin.bacnet4j.type.constructed.HostAddress;
import com.serotonin.bacnet4j.type.constructed.HostNPort;
import com.serotonin.bacnet4j.type.enumerated.IPMode;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.util.BACnetUtils;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.IpAddressUtils;

/**
 * Use IpNetworkBuilder to create.
 */
public class IpNetwork extends Network {
    static final Logger LOG = LoggerFactory.getLogger(IpNetwork.class);

    public static final byte BVLC_TYPE = (byte) 0x81;
    public static final int DEFAULT_PORT = 0xBAC0; // == 47808
    public static final String DEFAULT_BIND_IP = "0.0.0.0";

    private static final int MESSAGE_LENGTH = 2048;

    private final int port;
    private final String localBindAddressStr;
    private final String broadcastAddressStr;
    private final String subnetMaskStr;
    private final boolean reuseAddress;

    // BBMD support
    private List<Address> localAddresses;
    private InetSocketAddress localAddress;
    private List<BDTEntry> broadcastDistributionTable = new ArrayList<>();
    final List<FDTEntry> foreignDeviceTable = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> ftdMaintenance;
    private final AtomicBoolean bbmdEnabled = new AtomicBoolean(false);

    // When acting as a foreign device...
    final Object foreignBBMDLock = new Object();
    InetSocketAddress foreignBBMD;
    private int foreignTTL;
    private int bbmdResponse;
    private ScheduledFuture<?> foreignRegistrationMaintenance;
    private ForeignDeviceRegistrant fDRegistrant;
    private boolean fdRegistered = false;

    // Runtime
    private DatagramSocket unicastSocket;
    private DatagramSocket broadcastSocket;
    private OctetString broadcastMAC;
    private InetSocketAddress localBindAddress;
    private byte[] subnetMask;
    private long bytesOut;
    private long bytesIn;

    /**
     * Use an IpNetworkBuilder to create instances.
     */
    IpNetwork(int port, String localBindAddress, String broadcastAddress, String subnetMask, int localNetworkNumber,
            boolean reuseAddress) {
        super(localNetworkNumber);
        this.port = port;
        this.localBindAddressStr = localBindAddress;
        this.broadcastAddressStr = broadcastAddress;
        this.subnetMaskStr = subnetMask;
        this.reuseAddress = reuseAddress;
    }

    @Override
    public NetworkIdentifier getNetworkIdentifier() {
        return new IpNetworkIdentifier(port, localBindAddressStr);
    }

    @Override
    public MaxApduLength getMaxApduLength() {
        return MaxApduLength.UP_TO_1476;
    }

    public int getPort() {
        return port;
    }

    public InetSocketAddress getLocalBindAddress() {
        return localBindAddress;
    }

    public String getBroadcastAddress() {
        return broadcastAddressStr;
    }

    @Override
    public long getBytesOut() {
        return bytesOut;
    }

    @Override
    public long getBytesIn() {
        return bytesIn;
    }

    /**
     * Get the network socket, useful for routing purposes
     */
    public DatagramSocket getSocket() {
        return unicastSocket;
    }

    public IPMode getIpMode() {
        if (bbmdEnabled.get()) {
            return IPMode.bbmd;
        } else if (fDRegistrant != null) {
            return IPMode.foreign;
        }
        return IPMode.normal;
    }

    @Override
    public boolean isInitialized() {
        return localAddress != null;
    }

    public List<com.serotonin.bacnet4j.type.constructed.BDTEntry> getBroadcastDistributionTable() {
        return broadcastDistributionTable.stream().map(e ->
                new com.serotonin.bacnet4j.type.constructed.BDTEntry(
                        new HostNPort(new HostAddress(new OctetString(e.address)), new Unsigned16(e.port)),
                        new OctetString(e.distributionMask))
        ).toList();
    }

    public List<com.serotonin.bacnet4j.type.constructed.FDTEntry> getForeignDeviceTable() {
        return foreignDeviceTable.stream().map(e ->
                new com.serotonin.bacnet4j.type.constructed.FDTEntry(
                        IpNetworkUtils.toOctetString(e.address),
                        new Unsigned16(e.timeToLive),
                        new Unsigned16(e.getRemainingTimeToLive()))
        ).toList();
    }

    public HostNPort getForeignBBMDAddress() {
        if (fDRegistrant != null) {
            return new HostNPort(
                    new HostAddress(new OctetString(IpAddressUtils.toIpAddress(foreignBBMD.getHostString()))),
                    new Unsigned16(foreignBBMD.getPort()));
        }
        return null;
    }

    public Unsigned16 getForeignTTL() {
        if (fDRegistrant != null) {
            return new Unsigned16(foreignTTL);
        }
        return null;
    }

    public OctetString getSubnetMask() {
        return new OctetString(subnetMask);
    }

    public boolean isFdRegistered() {
        return fdRegistered;
    }

    public void tryFdRegister() {
        var localRegistrant = fDRegistrant;
        if (localRegistrant != null) {
            localRegistrant.run();
        }
    }

    @Override
    public void initialize(Transport transport) throws BACnetException {
        super.initialize(transport);

        localBindAddress = InetAddrCache.get(localBindAddressStr, port);
        try {
            unicastSocket = createSocket(localBindAddress);
        } catch (SocketException e) {
            throw new BACnetException(e);
        }

        broadcastMAC = IpNetworkUtils.toOctetString(broadcastAddressStr, port);
        subnetMask = BACnetUtils.dottedStringToBytes(subnetMaskStr);

        if (!DEFAULT_BIND_IP.equals(localBindAddressStr) && (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC)) {
            // If the bind address is the wildcard address (i.e. 0.0.0.0) then we will get messages to the broadcast
            // addresses automatically. The same is true if the OS is Windows regardless of the bind address. But on
            // Linux we need to open a socket on the broadcast address and get the broadcasts that way.
            try {
                InetSocketAddress broadcastAddress = InetAddrCache.get(broadcastAddressStr, port);
                broadcastSocket = createSocket(broadcastAddress);
            } catch (SocketException e) {
                unicastSocket.close();
                throw new BACnetException(e);
            }
        }

        // If the bindings were successful, start the listener threads.
        Thread unicastThread = new Thread(() -> listen(unicastSocket),
                "BACnet4J IP socket listener for " + transport.getLocalDevice().getId());
        unicastThread.start();

        if (broadcastSocket != null) {
            Thread broadcastThread = new Thread(() -> listen(broadcastSocket),
                    "BACnet4J IP broadcast socket listener for " + transport.getLocalDevice().getId());
            broadcastThread.start();
        }

        localAddresses = getLocalAddressList();
        localAddress = getLocalAddress();

        initializeBBMD();
    }

    protected DatagramSocket createSocket(InetSocketAddress bindAddress) throws SocketException {
        LOG.info("Binding to address {}", bindAddress);
        DatagramSocket socket;
        if (reuseAddress) {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            if (!socket.getReuseAddress())
                LOG.warn("reuseAddress was set but not supported by the underlying platform");
            socket.bind(bindAddress);
        } else {
            socket = new DatagramSocket(bindAddress);
        }
        socket.setBroadcast(true);
        return socket;
    }

    @Override
    public void terminate() {
        unregisterAsForeignDevice();
        if (unicastSocket != null)
            unicastSocket.close();
        if (broadcastSocket != null)
            broadcastSocket.close();
        if (ftdMaintenance != null)
            ftdMaintenance.cancel(false);
    }

    @Override
    protected OctetString getBroadcastMAC() {
        return broadcastMAC;
    }

    public Address getBroadcastAddress(int port) {
        return IpNetworkUtils.toAddress(broadcastAddressStr, port);
    }

    /**
     * The policy that tells the foreign device registration process how long to wait before a retry is attempted
     * after a registration failure. It also allows the specification of the renewal margin, or the amount of time
     * before a lease ends to attempt a re-registration.
     * <p>
     * Default methods are an example of how it can work. More sophisticated implementations might provide exponential
     * backoff or different results based upon the given exception.
     */
    public interface ForeignDeviceRegistrationRetryDelayPolicy {
        /**
         * Notifies this policy that a (re)registration has succeeded. This provides the policy an opportunity to reset,
         * or perhaps to dismiss an error condition.
         */
        default void registrationSucceeded() {
            // Allows implementations to reset after a series of failures.
        }

        /**
         * @param e the exception that caused the registration to fail.
         * @return the amount of time to wait before attempting the registration again.
         */
        default Duration registrationFailed(BACnetException e) {
            return Duration.ofSeconds(10);
        }

        /**
         * Provides the renewal margin, or the amount of time before a lease ends to attempt a re-registration.
         *
         * @param timeToLive the original TTL given for the registration.
         * @return the amount of time before the end of the lease to attempt a re-registration
         */
        default Duration renewalMargin(Duration timeToLive) {
            return Duration.ofSeconds(30);
        }
    }

    /**
     * Starts the process of registration and re-registration of this device as a foreign device in a BBMD. This method
     * is non-blocking.
     * <p>
     * If a registration attempt succeeds, a re-registration will be scheduled for just before the current registration
     * times out. If the registration attempt fails, the `retryDelayPolicy` will be asked for the delay before a
     * retry. This process will not end until the {@link #unregisterAsForeignDevice} method is called.
     * <p>
     * The {@link #unregisterAsForeignDevice} method will be called automatically if the local device is terminated.
     *
     * @param addr             The address of the BBMD where our device wants to be registered
     * @param timeToLive       The time in seconds until we are automatically removed out of the FDT
     * @param retryDelayPolicy Provides the amount of time to delay before attempting another registration after a
     *                         failure.
     */
    public void registerAsForeignDevice(InetSocketAddress addr, Duration timeToLive,
            ForeignDeviceRegistrationRetryDelayPolicy retryDelayPolicy) {
        int ttlSeconds = (int) timeToLive.getSeconds();
        if (ttlSeconds < 1)
            throw new IllegalArgumentException("timeToLive cannot be less than 1");
        if (getTransport() == null || getTransport().getLocalDevice() == null)
            throw new IllegalArgumentException(
                    "Network must be used within a local device before foreign device registration can be performed.");

        synchronized (foreignBBMDLock) {
            if (fDRegistrant != null)
                throw new IllegalStateException("Already registered as a foreign device");

            foreignBBMD = addr;
            foreignTTL = ttlSeconds;

            fDRegistrant = new ForeignDeviceRegistrant(retryDelayPolicy);

            // Initially schedule to run immediately.
            foreignRegistrationMaintenance =
                    getTransport().getLocalDevice().schedule(fDRegistrant, 0, TimeUnit.SECONDS);
        }
    }

    class ForeignDeviceRegistrant implements Runnable {
        private final ForeignDeviceRegistrationRetryDelayPolicy retryDelayPolicy;

        ForeignDeviceRegistrant(ForeignDeviceRegistrationRetryDelayPolicy retryDelayPolicy) {
            this.retryDelayPolicy = retryDelayPolicy;
        }

        public void run() {
            synchronized (foreignBBMDLock) {
                // Ensure expected state.
                if (foreignRegistrationMaintenance == null || foreignRegistrationMaintenance.isCancelled()) {
                    // Abort
                    return;
                }

                int delay;
                try {
                    sendForeignDeviceRegistration();
                    LOG.info("Successfully registered as foreign device");
                    fdRegistered = true;
                    retryDelayPolicy.registrationSucceeded();

                    // Successful registration. Schedule the next re-registration.
                    int interval = foreignTTL -
                            (int) retryDelayPolicy.renewalMargin(Duration.ofSeconds(foreignTTL)).getSeconds();
                    if (interval < 15) {
                        // Minimum of 15s.
                        interval = 15;
                    }
                    delay = interval;
                } catch (BACnetException e) {
                    LOG.warn("Failed to register foreign device", e);
                    fdRegistered = false;
                    // Failure. Ask the retry provider what to do.
                    delay = (int) retryDelayPolicy.registrationFailed(e).toSeconds();
                    if (delay < 0) {
                        delay = 0;
                    }
                }

                foreignRegistrationMaintenance =
                        getTransport().getLocalDevice().schedule(this, delay, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Requests that this device be un-registered at the location at which it was previously registered. Quietly
     * ignores the case where it is not already registered.
     */
    public void unregisterAsForeignDevice() {
        synchronized (foreignBBMDLock) {
            if (fDRegistrant != null) {
                try {
                    deleteForeignDeviceTableEntry(foreignBBMD, localAddress);
                } catch (BACnetException e) {
                    LOG.info("Device de-registration failed", e);
                }
            }
            if (foreignRegistrationMaintenance != null)
                foreignRegistrationMaintenance.cancel(false);
            foreignBBMD = null;
            foreignRegistrationMaintenance = null;
            fDRegistrant = null;
            fdRegistered = false;
        }
    }

    @Override
    public void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast, boolean expectsReply)
            throws BACnetException {
        ByteQueue queue = new ByteQueue();

        // BACnet virtual link layer detail
        queue.push(BVLC_TYPE);

        InetSocketAddress addr = foreignBBMD;
        if (addr != null && broadcast) {
            // Distribute-Broadcast-To-Network. This device is registered as a foreign device in a BBMD, so send the
            // message as a distribute broadcast to network.
            queue.push(9);
        } else {
            // Original-Unicast-NPDU, or Original-Broadcast-NPDU
            queue.push(broadcast ? 0xb : 0xa);

            OctetString dest = getDestination(recipient, router);
            addr = IpNetworkUtils.getInetSocketAddress(dest);
        }

        queue.pushU2B(npdu.size() + 4);

        // Combine the queues
        queue.push(npdu);

        sendPacket(addr, queue.popAll());
    }

    protected void sendPacket(InetSocketAddress addr, byte[] data) throws BACnetException {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, addr);
            unicastSocket.send(packet);
            bytesOut += data.length;
        } catch (Exception e) {
            throw new BACnetException(e);
        }
    }

    //
    // For receiving
    protected void listen(DatagramSocket socket) {
        byte[] buffer = new byte[MESSAGE_LENGTH];
        DatagramPacket p = new DatagramPacket(buffer, buffer.length);

        while (!socket.isClosed()) {
            try {
                socket.receive(p);

                bytesIn += p.getLength();
                // Create a new byte queue for the message, because the queue will probably be processed in the
                // transport thread.
                ByteQueue queue = new ByteQueue(p.getData(), 0, p.getLength());
                OctetString link = IpNetworkUtils.toOctetString(p.getAddress().getAddress(), p.getPort());

                handleIncomingData(queue, link);

                // Reset the packet.
                p.setData(buffer);
            } catch (@SuppressWarnings("unused") IOException e) {
                // no op. This happens if the socket gets closed by the destroy method.
            }
        }
    }

    @Override
    protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws BACnetException {
        LOG.trace("Received request from {}", linkService);

        // Initial parsing of IP message.
        // BACnet/IP
        if (queue.pop() != BVLC_TYPE)
            throw new MessageValidationException("Protocol id is not BACnet/IP (0x81)");

        byte function = queue.pop();

        int length = BACnetUtils.popShort(queue);
        if (length != queue.size() + 4)
            throw new MessageValidationException(
                    "Length field does not match data: given=" + length + ", expected=" + (queue.size() + 4));

        NPDU npdu = null;
        if (function == 0x0) {
            int result = BACnetUtils.popShort(queue);

            if (result == 0x10)
                LOG.error("Write-Broadcast-Distribution-Table failed!");
            else if (result == 0x20)
                LOG.error("Read-Broadcast-Distribution-Table failed!");
            else if (result == 0x30)
                LOG.error("Register-Foreign-Device failed!");
            else if (result == 0x40)
                LOG.error("Read-Foreign-Device-Table failed!");
            else if (result == 0x50)
                LOG.error("Delete-Foreign-Device-Table-Entry failed!");
            else if (result == 0x60)
                LOG.error("Distribute-Broadcast-To-Network failed!");
            else if (result != 0)
                LOG.warn("Received unexpected BVLC result: {}", result);

            // Response management.
            bbmdResponse = result;
            InetSocketAddress addr = foreignBBMD;
            if (addr != null) {
                synchronized (addr) {
                    addr.notifyAll();
                }
            }
        } else if (function == 0x1)
            // Write-Broadcast-Distribution-Table
            writeBDT(queue, linkService);
        else if (function == 0x2)
            // Read-Broadcast-Distribution-Table
            readBDT(linkService);
        else if (function == 0x3)
            // Not implemented because this does not send Read-Broadcast-Distribution-Table requests, and so should
            // not receive any responses.
            throw new BACnetException("Read-Broadcast-Distribution-Table-Ack not implemented");
        else if (function == 0x4) {
            // Forwarded-NPDU.
            // If this is a BBMD, it could forward this message to its local broadcast address, meaning it could
            // receive its own forwards. Check if the link service is itself, and ignore if so.
            if (!linkService.equals(IpNetworkUtils.toOctetString(localAddress))) {
                forwardNPDU(queue, linkService);

                // Process the NPDU locally
                byte[] address = new byte[6];
                queue.pop(address);
                OctetString origin = new OctetString(address);
                npdu = parseNpduData(queue, origin);
            }
        } else if (function == 0x5)
            // Register-Foreign-Device
            registerForeignDevice(queue, linkService);
        else if (function == 0x6)
            // Read-Foreign-Device-Table
            readFDT(linkService);
        else if (function == 0x7)
            // Not implemented because this does not send Read-Foreign-Device-Table requests, and so should
            // not receive any responses.
            throw new BACnetException("Read-Foreign-Device-Table-Ack not implemented");
        else if (function == 0x8)
            // Delete-Foreign-Device-Table-Entry
            deleteFDT(queue, linkService);
        else if (function == 0x9) {
            // Distribute-Broadcast-To-Network
            boolean ok = distributeBroadcastToNetwork(queue, linkService);
            if (ok)
                // Only process locally if the foreign device is valid.
                npdu = parseNpduData(queue, linkService);
        } else if (function == 0xa)
            // Original-Unicast-NPDU
            npdu = parseNpduData(queue, linkService);
        else if (function == 0xb) {
            // Original-Broadcast-NPDU
            originalBroadcast(queue, linkService);

            npdu = parseNpduData(queue, linkService);
        } else
            throw new MessageValidationException(
                    "Unhandled BVLC function type: 0x" + Integer.toHexString(function & 0xff));

        return npdu;
    }

    //
    //
    // Convenience methods
    //
    public Address getAddress(InetAddress inetAddress) {
        try {
            return IpNetworkUtils.toAddress(getLocalNetworkNumber(), inetAddress.getAddress(), port);
        } catch (Exception e) {
            // Should never happen, so just wrap in a RuntimeException
            throw new BACnetRuntimeException(e);
        }
    }

    public static InetAddress getDefaultLocalInetAddress() throws UnknownHostException, SocketException {
        for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                if (!addr.isLoopbackAddress())
                    return addr;
            }
        }

        return InetAddress.getLocalHost();
    }

    protected List<Address> getLocalAddressList() {
        try {
            ArrayList<Address> result = new ArrayList<>();
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address)
                        result.add(getAddress(addr));
                }
            }

            // Check if the configured local bind address is one of the addresses. If not, add it. This is
            // necessary if the local bind address is a loopback.
            Address config = IpNetworkUtils.toAddress(localBindAddress);
            boolean found = false;
            for (Address addr : result) {
                if (addr.equals(config)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                result.add(config);

            return result;
        } catch (Exception e) {
            // Should never happen, so just wrap in a RuntimeException
            throw new BACnetRuntimeException(e);
        }
    }

    public InetSocketAddress getLocalAddress() {
        var wildcard = IpAddressUtils.toIpAddress(DEFAULT_BIND_IP);
        // If the local bind address is not the wildcard address, use it.
        if (!Arrays.equals(localBindAddress.getAddress().getAddress(), wildcard)) {
            return localBindAddress;
        }

        // Otherwise, find the first local address that is not the wildcard address.
        var address = localAddresses.stream()
                .filter(a -> !Arrays.equals(IpNetworkUtils.getIpBytes(a.getMacAddress()), wildcard))
                .findFirst().orElse(null);

        return address == null ? null : IpNetworkUtils.getInetSocketAddress(address.getMacAddress());
    }

    @Override
    public Address[] getAllLocalAddresses() {
        return localAddresses.toArray(new Address[0]);
    }

    @Override
    public Address getLoopbackAddress() {
        return IpNetworkUtils.toAddress("127.0.0.1", DEFAULT_PORT);
    }

    //
    //
    // BBMD
    //
    public static class BDTEntry {
        protected static final byte[] DEFAULT_DISTRIBUTION_MASK =
                new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255};

        // The IP address of the other BBMD
        final byte[] address;
        final int port;

        // If messages are to be distributed on the remote IP subnet using directed broadcasts, the broadcast
        // distribution mask shall be identical to the subnet mask associated with the subnet, i.e., all 1's in the
        // network portion of the 4-octet IP address field and all 0's in the host portion. If messages are to be
        // distributed on the remote IP subnet by sending the message directly to the remote BBMD, the broadcast
        // distribution mask shall be all 1's. The broadcast distribution masks referring to the same IP subnet shall
        // be identical in each BDT.
        final byte[] distributionMask;

        public BDTEntry(byte[] address, int port, byte[] distributionMask) {
            this.address = address;
            this.port = port;
            this.distributionMask = distributionMask;
        }

        public BDTEntry(String addressDottedString, int port) {
            this(BACnetUtils.dottedStringToBytes(addressDottedString), port, DEFAULT_DISTRIBUTION_MASK);
        }

        public BDTEntry(String addressDottedString, int port, String distributionMaskDottedString) {
            this(BACnetUtils.dottedStringToBytes(addressDottedString), port,
                    BACnetUtils.dottedStringToBytes(distributionMaskDottedString));
        }

        Address toAddress() {
            return new Address(IpNetworkUtils.toOctetString(address, port));
        }
    }


    public class FDTEntry {
        final InetSocketAddress address;
        int timeToLive;
        long endTime;

        public FDTEntry(InetSocketAddress address, int timeToLive) {
            this.address = address;
            update(timeToLive);
        }

        public FDTEntry(String address, int port, int timeToLive) {
            this(new InetSocketAddress(address, port), timeToLive);
        }

        void update(int timeToLive) {
            this.timeToLive = timeToLive;
            // Adds a 30-second grace period, as per J.5.2.3
            endTime = getTransport().getLocalDevice().getClock().millis() + (timeToLive + 30) * 1000L;
        }

        int getRemainingTimeToLive() {
            return (int) ((endTime - getTransport().getLocalDevice().getClock().millis()) / 1000);
        }
    }

    protected void writeBDT(ByteQueue queue, OctetString origin) throws BACnetException {
        ByteQueue response = new ByteQueue();
        response.push(BVLC_TYPE);
        response.push(0); // Result
        response.pushU2B(6); // Length

        if (bbmdEnabled.get()) {
            try {
                List<BDTEntry> list = new ArrayList<>();

                while (queue.size() > 0) {
                    var address = new byte[4];
                    queue.pop(address);
                    var bdtPort = queue.popU2B();
                    var distributionMask = new byte[4];
                    queue.pop(distributionMask);
                    list.add(new BDTEntry(address, bdtPort, distributionMask));
                }

                // Successfully read. Replace the current BDT.
                broadcastDistributionTable = list;

                response.pushU2B(0); // Ok
            } catch (Exception e) {
                LOG.error("BDT write failed", e);
                response.pushU2B(0x10); // NAK
            }
        } else {
            response.pushU2B(0x10); // NAK
        }
        sendPacket(IpNetworkUtils.getInetSocketAddress(origin), response.popAll());
    }

    private void readBDT(OctetString origin) throws BACnetException {
        ByteQueue response = new ByteQueue();
        response.push(BVLC_TYPE);
        if (bbmdEnabled.get()) {
            try {
                ByteQueue list = new ByteQueue();

                for (BDTEntry e : broadcastDistributionTable) {
                    list.push(e.address);
                    list.pushU2B(e.port);
                    list.push(e.distributionMask);
                }

                // Successfully written.
                response.push(3); // Read-Broadcast-Distribution-Table-Ack
                response.pushU2B(4 + list.size()); // Length
                response.push(list); // List
            } catch (Exception e) {
                LOG.error("BDT read failed", e);
                response.push(0); // Result
                response.pushU2B(6); // Length
                response.pushU2B(0x20); // NAK
            }
        } else {
            response.push(0); // Result
            response.pushU2B(6); // Length
            response.pushU2B(0x20); // NAK
        }
        sendPacket(IpNetworkUtils.getInetSocketAddress(origin), response.popAll());
    }

    protected void forwardNPDU(ByteQueue partial, OctetString origin) throws BACnetException {
        // Determine whether to the message should be broadcast locally.
        boolean doLocalBroadcast = !broadcastDistributionTable.isEmpty();

        if (doLocalBroadcast) {
            // 1) If the origin is on the same subnet, do not broadcast locally.
            doLocalBroadcast = !IpNetworkUtils.matchWithMask(localAddress.getAddress().getAddress(), origin.getBytes(),
                    subnetMask);
        }

        if (doLocalBroadcast) {
            // 2) If the mask of the BDT entry for this BBMD is not all 1s, do not broadcast locally.
            byte[] myAddress = localAddress.getAddress().getAddress();
            // Find the BDT entry matching this.
            BDTEntry thisEntry = broadcastDistributionTable.stream()
                    .filter(e -> Arrays.equals(e.address, myAddress) && e.port == port)
                    .findFirst()
                    .orElse(null);

            if (thisEntry == null) {
                // Not found. This is a configuration problem. Don't broadcast.
                LOG.warn("Configuration error: could not find BDT entry for this instance.");
                doLocalBroadcast = false;
            } else if (!Arrays.equals(thisEntry.distributionMask, BDTEntry.DEFAULT_DISTRIBUTION_MASK)) {
                doLocalBroadcast = false;
            }
        }

        // Check if anything needs to be done.
        if (foreignDeviceTable.isEmpty() && !doLocalBroadcast)
            return;

        // The BVLC type, function and length were removed from this queue, so recreate.
        ByteQueue fwd = new ByteQueue();
        fwd.push(BVLC_TYPE);
        fwd.push(4); // Forward
        fwd.pushU2B(4 + partial.size()); // Length
        fwd.push(partial);
        byte[] toSend = fwd.popAll();

        if (doLocalBroadcast) {
            sendPacket(InetAddrCache.get(broadcastAddressStr, port), toSend);
        }

        // Forward to all foreign devices.
        for (FDTEntry fd : foreignDeviceTable)
            sendPacket(fd.address, toSend);
    }

    protected void originalBroadcast(ByteQueue partial, OctetString originStr) throws BACnetException {
        // Check if anything needs to be done.
        if (foreignDeviceTable.isEmpty() && broadcastDistributionTable.isEmpty())
            return;

        ByteQueue fwd = new ByteQueue();
        fwd.push(BVLC_TYPE);
        fwd.push(4); // Forward
        fwd.pushU2B(10 + partial.size()); // Length
        fwd.push(originStr.getBytes()); // Origin
        fwd.push(partial);
        byte[] toSend = fwd.popAll();

        try {
            byte[] myAddress = localAddress.getAddress().getAddress();

            // Send to all subnets except own
            for (BDTEntry e : broadcastDistributionTable) {
                if (Arrays.equals(e.address, myAddress) && e.port == port) {
                    continue;
                }
                sendToBDT(e, toSend);
            }

            // Forward to all foreign devices.
            for (FDTEntry fd : foreignDeviceTable)
                sendPacket(fd.address, toSend);
        } catch (UnknownHostException e) {
            throw new BACnetException(e);
        }
    }

    private void registerForeignDevice(ByteQueue queue, OctetString originStr) throws BACnetException {
        InetSocketAddress origin = IpNetworkUtils.getInetSocketAddress(originStr);
        LOG.debug("Received registerForeignDevice request from {}", origin);

        ByteQueue response = new ByteQueue();
        response.push(BVLC_TYPE);
        response.push(0); // Response type
        response.pushU2B(6); // Length

        if (bbmdEnabled.get()) {
            int timeToLive = queue.popU2B();
            if (timeToLive < 1) {
                response.pushU2B(0x30); // NAK
            } else {
                // Check if the device is already in the list. If so, update its start time. Otherwise, add it.
                FDTEntry fd = null;
                synchronized (foreignDeviceTable) {
                    for (FDTEntry e : foreignDeviceTable) {
                        if (e.address.equals(origin)) {
                            fd = e;
                            break;
                        }
                    }

                    if (fd == null) {
                        // Add the FDT entry
                        foreignDeviceTable.add(new FDTEntry(origin, timeToLive));
                    } else {
                        fd.update(timeToLive);
                    }
                }

                response.pushU2B(0); // Success
            }
        } else {
            response.pushU2B(0x30); // NAK
        }
        sendPacket(origin, response.popAll());
    }

    private void readFDT(OctetString origin) throws BACnetException {
        ByteQueue response = new ByteQueue();
        response.push(BVLC_TYPE);

        if (bbmdEnabled.get()) {
            long now = getTransport().getLocalDevice().getClock().millis();
            try {
                ByteQueue list = new ByteQueue();

                for (FDTEntry e : foreignDeviceTable) {
                    pushISA(list, e.address);
                    list.pushU2B(e.timeToLive);

                    int remaining = (int) (e.endTime - now) / 1000;
                    if (remaining < 0)
                        // Hasn't yet been cleaned up.
                        remaining = 0;
                    if (remaining > 65535)
                        remaining = 65535;

                    list.pushU2B(remaining);
                }

                // Successfully written.
                response.push(7); // Read-Foreign-Device-Table-Ack
                response.pushU2B(4 + list.size()); // Length
                response.push(list); // List
            } catch (Exception e) {
                LOG.error("FDT read failed", e);
                response.push(0); // Result
                response.pushU2B(6); // Length
                response.pushU2B(0x40); // NAK
            }
        } else {
            response.push(0); // Result
            response.pushU2B(6); // Length
            response.pushU2B(0x40); // NAK
        }

        sendPacket(IpNetworkUtils.getInetSocketAddress(origin), response.popAll());
    }

    private void deleteFDT(ByteQueue queue, OctetString origin) throws BACnetException {
        byte[] addr = new byte[4];
        queue.pop(addr);
        int fdtPort = queue.popU2B();

        ByteQueue response = new ByteQueue();
        response.push(BVLC_TYPE);
        response.push(0); // Response type
        response.pushU2B(6); // Length

        synchronized (foreignDeviceTable) {
            FDTEntry toDelete = null;
            for (FDTEntry fd : foreignDeviceTable) {
                if (Arrays.equals(fd.address.getAddress().getAddress(), addr) && fd.address.getPort() == fdtPort) {
                    toDelete = fd;
                    break;
                }
            }

            if (toDelete != null) {
                foreignDeviceTable.remove(toDelete);
                response.pushU2B(0); // Success
            } else
                response.pushU2B(0x50); // NAK
        }

        sendPacket(IpNetworkUtils.getInetSocketAddress(origin), response.popAll());
    }

    protected boolean distributeBroadcastToNetwork(ByteQueue queue, OctetString originStr) throws BACnetException {
        InetSocketAddress origin = IpNetworkUtils.getInetSocketAddress(originStr);

        // Find the foreign device.
        FDTEntry originFDT = foreignDeviceTable.stream().filter(e -> e.address.equals(origin)).findFirst().orElse(null);

        ByteQueue response = new ByteQueue();
        response.push(BVLC_TYPE);
        response.push(0); // Response type
        response.pushU2B(6); // Length

        // If the FDT was not found, send a NAK and return false.
        if (originFDT == null) {
            response.pushU2B(0x60); // NAK
            sendPacket(origin, response.popAll());
            return false;
        }

        // The FDT was found. Forward the message around.
        ByteQueue fwd = new ByteQueue();
        fwd.push(BVLC_TYPE);
        fwd.push(4); // Forward
        fwd.pushU2B(10 + queue.size()); // Length
        fwd.push(originStr.getBytes()); // Origin
        fwd.push(queue);
        byte[] toSend = fwd.popAll();

        // Send locally
        sendPacket(InetAddrCache.get(broadcastAddressStr, port), toSend);

        try {
            // Send to all BDTs except own
            byte[] myAddress = localAddress.getAddress().getAddress();
            for (BDTEntry e : broadcastDistributionTable) {
                if (Arrays.equals(e.address, myAddress) && e.port == port) {
                    continue;
                }
                sendToBDT(e, toSend);
            }
        } catch (UnknownHostException e1) {
            LOG.warn("Error forwarding to BDT", e1);
        }

        // Forward to all foreign devices except the origin.
        for (FDTEntry fd : foreignDeviceTable) {
            if (fd != originFDT)
                sendPacket(fd.address, toSend);
        }

        response.pushU2B(0); // Success
        sendPacket(origin, response.popAll());
        return true;
    }

    protected void sendToBDT(BDTEntry e, byte[] toSend) throws UnknownHostException, BACnetException {
        // J.4.5: The B/IP address to which the Forwarded-NPDU message is sent is formed by inverting the broadcast
        // distribution mask in the BDT entry and logically ORing it with the BBMD address of the same entry.
        byte[] target = new byte[4];
        for (int i = 0; i < 4; i++)
            target[i] = (byte) (e.address[i] | ~e.distributionMask[i]);

        InetSocketAddress to = InetAddrCache.get(InetAddress.getByAddress(target), e.port);
        sendPacket(to, toSend);
    }

    private static void pushISA(ByteQueue queue, InetSocketAddress isa) {
        queue.push(isa.getAddress().getAddress());
        queue.pushU2B(isa.getPort());
    }

    //
    //
    // Foreign device registration (of this local device in a foreign BBMD)
    //
    public void sendForeignDeviceRegistration() throws BACnetException {
        InetSocketAddress addr = foreignBBMD;
        if (addr == null)
            return;

        ByteQueue queue = new ByteQueue();
        queue.push(BVLC_TYPE);
        queue.push(0x05); // Register foreign device
        queue.pushU2B(6); // Length
        queue.pushU2B(foreignTTL); // TTL

        synchronized (addr) {
            bbmdResponse = -1;

            sendPacket(addr, queue.popAll());
            try {
                addr.wait(5000); // Wait up to 5 seconds.
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                // no op
            }

            if (bbmdResponse == -1)
                throw new BACnetException("Foreign registration timeout");
            if (bbmdResponse != 0)
                throw new BACnetException("NAK response: " + bbmdResponse);
        }
    }

    /**
     * Utility method that allows the de-registration of an arbitrary foreign device in a BBMD. If the intention is
     * to unregister this device, the {@link #unregisterAsForeignDevice} method should be used instead.
     *
     * @param addr     the address at which to find the FDT
     * @param fdtEntry the entry to remove
     * @throws BACnetException if something goes wrong
     */
    public void deleteForeignDeviceTableEntry(InetSocketAddress addr, InetSocketAddress fdtEntry)
            throws BACnetException {
        ByteQueue queue = new ByteQueue();
        queue.push(BVLC_TYPE);
        queue.push(0x08); // Delete foreign device table entry
        queue.pushU2B(0xA); // Length
        pushISA(queue, fdtEntry);
        sendPacket(addr, queue.popAll());
    }

    /**
     * Enable BBMD support. Allow other device to register as BBMD or foreign device. *
     */
    public void enableBBMD() {
        bbmdEnabled.set(true);
        initializeBBMD();
    }

    protected void initializeBBMD() {
        // BBMD must be enabled and the network needs to be initialized before BBMD initialization.
        if (bbmdEnabled.get() && localBindAddress != null) {
            if (broadcastDistributionTable.isEmpty()) {
                // If the BDT is still empty, as a default we add an entry for this self, using the first
                // non-wildcard address.
                var local = getLocalAddress();
                broadcastDistributionTable.add(new BDTEntry(local.getAddress().getAddress(), local.getPort(),
                        BDTEntry.DEFAULT_DISTRIBUTION_MASK));
            }

            // Add a job to expire foreign device registrations.
            ftdMaintenance = getTransport().getLocalDevice().scheduleAtFixedRate(() -> {
                long now = getTransport().getLocalDevice().getClock().millis();

                synchronized (foreignDeviceTable) {
                    List<FDTEntry> toRemove = new ArrayList<>();

                    for (FDTEntry e : foreignDeviceTable) {
                        if (e.endTime < now) {
                            LOG.debug("Removing expired foreign device: {}", e);
                            toRemove.add(e);
                        }
                    }

                    if (!toRemove.isEmpty()) {
                        foreignDeviceTable.removeAll(toRemove);
                    }
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    public void writeBDT(List<BDTEntry> entries) {
        broadcastDistributionTable = new ArrayList<>(entries);
    }

    public void writeFDT(List<FDTEntry> entries) {
        foreignDeviceTable.clear();
        foreignDeviceTable.addAll(entries);
    }
}
