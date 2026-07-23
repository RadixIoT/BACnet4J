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

package com.serotonin.bacnet4j;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.cache.CachePolicies;
import com.serotonin.bacnet4j.cache.RemoteEntityCache;
import com.serotonin.bacnet4j.cache.RemoteEntityCachePolicy;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.event.DefaultReinitializeDeviceHandler;
import com.serotonin.bacnet4j.event.DeviceEventHandler;
import com.serotonin.bacnet4j.event.ExceptionDispatcher;
import com.serotonin.bacnet4j.event.PrivateTransferHandler;
import com.serotonin.bacnet4j.event.ReinitializeDeviceHandler;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.NetworkIdentifier;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.DeviceObject;
import com.serotonin.bacnet4j.obj.mixin.CovContext;
import com.serotonin.bacnet4j.persistence.IPersistence;
import com.serotonin.bacnet4j.persistence.NullPersistence;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.service.VendorServiceKey;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable;
import com.serotonin.bacnet4j.service.unconfirmed.IAmRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedCovNotificationRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.service.unconfirmed.WhoAmIRequest;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.NetworkSourceAddress;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.RestartReason;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.DiscoveryUtils;
import com.serotonin.bacnet4j.util.RemoteDeviceDiscoverer;
import com.serotonin.bacnet4j.util.RemoteDeviceFinder;
import com.serotonin.bacnet4j.util.RemoteDeviceFinder.RemoteDeviceFuture;

import lohbihler.warp.WarpScheduledExecutorService;

/**
 * Enhancements: - Optional persistence of COV subscriptions - default character string encoding - persistence of
 * recipient lists in notification forwarder object
 */
public class LocalDevice implements AutoCloseable {
    static final Logger LOG = LoggerFactory.getLogger(LocalDevice.class);
    public static final String VERSION =
            Objects.requireNonNullElse(LocalDevice.class.getPackage().getImplementationVersion(), "unknown");
    public static final SecureRandom RANDOM = new SecureRandom();

    private Transport transport;

    /**
     * The other objects contained by this device.
     */
    private final List<BACnetObject> localObjects = new CopyOnWriteArrayList<>();

    /**
     * The policies used for caching of devices, objects, and properties.
     */
    private final CachePolicies cachePolicies = new CachePolicies();

    /**
     * A collection of known peer devices on the network.
     */
    private final RemoteEntityCache<Integer, RemoteDevice> remoteDeviceCache = new RemoteEntityCache<>(this);

    /**
     * The amount of time to remember that a device lookup timed out in milliseconds. Default to 30 seconds.
     */
    private long timeoutDeviceRetention = 30000;

    /**
     * A map of devices for which lookups resulted in a timeout. Keeping this information around means that processes
     * that loop though lists of objects and so potentially ask for the same remote device multiple times don't need to
     * wait for the full timeout period each time. The key of the map is the device id, and the value is the time at
     * which it is ok to look for the device again. See timeoutDeviceRetention.
     */
    private final Map<Integer, Long> timeoutDevices = new HashMap<>();

    /**
     * The BACnet object that represents this as the local device.
     */
    private DeviceObject deviceObject;

    /**
     * The clock used for all timing, except for Object.wait and Thread.sleep calls.
     */
    private Clock clock = Clock.systemUTC();

    private boolean initialized;

    /**
     * The local password of the device. Used in the ReinitializeDeviceRequest service.
     */
    private String password;

    /**
     * The list of all COV subscriptions currently active in the device.
     */
    private final Map<ObjectIdentifier, List<CovContext>> covContexts = new ConcurrentHashMap<>();

    // Event listeners
    private final DeviceEventHandler eventHandler = new DeviceEventHandler();
    private final ExceptionDispatcher exceptionDispatcher = new ExceptionDispatcher();

    // Confirmed private transfer handlers.
    private final Map<VendorServiceKey, PrivateTransferHandler> privateTransferHandlers = new HashMap<>();

    // Reinitialize device handler
    private ReinitializeDeviceHandler reinitializeDeviceHandler = new DefaultReinitializeDeviceHandler();

    private ScheduledExecutorService timer;

    // Callback if other devices have the same id like us
    private Consumer<Address> sameDeviceIdCallback;

    /**
     * Useful when objects want to make COV subscriptions, in that it will provide a device-unique id.
     */
    private final AtomicInteger nextProcessId = new AtomicInteger(1);

    // Default persistence to null.
    private IPersistence persistence = new NullPersistence();

    public LocalDevice(int deviceNumber, Transport transport) {
        this.transport = transport;
        transport.setLocalDevice(this);
        afterInstantiation(deviceNumber);
    }

    private void afterInstantiation(int deviceNumber) {
        try {
            // Initialize the device object.
            addObject(new DeviceObject(this, deviceNumber));
        } catch (BACnetServiceException e) {
            // Should not happen
            throw new BACnetRuntimeException(e);
        }
    }

    public LocalDevice withClock(Clock clock) {
        setClock(clock);
        return this;
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        if (initialized)
            throw new IllegalStateException("Clock needs to be set before LocalDevice is initialized");
        this.clock = clock;
    }

    public DeviceObject getDeviceObject() {
        return deviceObject;
    }

    public Network getNetwork() {
        return transport.getNetwork();
    }

    public NetworkIdentifier getNetworkIdentifier() {
        return transport.getNetworkIdentifier();
    }

    public CachePolicies getCachePolicies() {
        return cachePolicies;
    }

    public Map<ObjectIdentifier, List<CovContext>> getCovContexts() {
        return covContexts;
    }

    public ObjectIdentifier getId() {
        return deviceObject.getId();
    }

    public int getInstanceNumber() {
        return deviceObject.getInstanceId();
    }

    public <T extends Encodable> T get(PropertyIdentifier pid) {
        return deviceObject.get(pid);
    }

    public LocalDevice writePropertyInternal(PropertyIdentifier pid, Encodable value) {
        deviceObject.writePropertyInternal(pid, value);
        return this;
    }

    public DeviceEventHandler getEventHandler() {
        return eventHandler;
    }

    public ExceptionDispatcher getExceptionDispatcher() {
        return exceptionDispatcher;
    }

    public int getNextProcessId() {
        return nextProcessId.getAndIncrement();
    }

    public void addPrivateTransferHandler(int vendorId, int serviceNumber, PrivateTransferHandler handler) {
        privateTransferHandlers.put(new VendorServiceKey(vendorId, serviceNumber), handler);
    }

    public PrivateTransferHandler getPrivateTransferHandler(UnsignedInteger vendorId, UnsignedInteger serviceNumber) {
        return privateTransferHandlers.get(new VendorServiceKey(vendorId, serviceNumber));
    }

    public ReinitializeDeviceHandler getReinitializeDeviceHandler() {
        return reinitializeDeviceHandler;
    }

    public void setReinitializeDeviceHandler(ReinitializeDeviceHandler reinitializeDeviceHandler) {
        this.reinitializeDeviceHandler = reinitializeDeviceHandler;
    }

    public long getTimeoutDeviceRetention() {
        return timeoutDeviceRetention;
    }

    public void setTimeoutDeviceRetention(long timeoutDeviceRetention) {
        this.timeoutDeviceRetention = timeoutDeviceRetention;
    }

    /**
     * @return the number of bytes sent by the transport
     */
    public long getBytesOut() {
        return transport.getBytesOut();
    }

    /**
     * @return the number of bytes received by the transport
     */
    public long getBytesIn() {
        return transport.getBytesIn();
    }

    public boolean isUnconfigured() {
        return deviceObject.getId().isUninitialized();
    }

    public synchronized LocalDevice initialize() throws BACnetException {
        return initialize(RestartReason.unknown);
    }

    public synchronized LocalDevice initialize(RestartReason lastRestartReason) throws BACnetException {
        deviceObject.writePropertyInternal(PropertyIdentifier.lastRestartReason, lastRestartReason);

        timer = createScheduledExecutorService();
        transport.initialize();
        initialized = true;

        // Notify objects.
        for (BACnetObject bo : localObjects) {
            bo.initialize();
        }

        if (!isUnconfigured()) {
            //
            // Send restart notifications. Note an uninitialized device will not send these.

            // The defaulting of the list of recipients is done here because sometimes the network has to be initialized
            // before the local broadcast address is known.
            SequenceOf<Recipient> restartNotificationRecipients = getPersistence().loadSequenceOf(
                    deviceObject.getPersistenceKey(PropertyIdentifier.restartNotificationRecipients), Recipient.class);
            if (restartNotificationRecipients == null) {
                restartNotificationRecipients = new SequenceOf<>(new Recipient(getLocalBroadcastAddress()));
                deviceObject.writePropertyInternal(PropertyIdentifier.restartNotificationRecipients,
                        restartNotificationRecipients);
            }
            UnconfirmedCovNotificationRequest restartNotif = new UnconfirmedCovNotificationRequest(
                    UnsignedInteger.ZERO, getId(), getId(), UnsignedInteger.ZERO, new SequenceOf<>(
                    new PropertyValue(PropertyIdentifier.systemStatus,
                            deviceObject.get(PropertyIdentifier.systemStatus)),
                    new PropertyValue(PropertyIdentifier.timeOfDeviceRestart,
                            deviceObject.get(PropertyIdentifier.timeOfDeviceRestart)),
                    new PropertyValue(PropertyIdentifier.lastRestartReason,
                            deviceObject.get(PropertyIdentifier.lastRestartReason))));
            for (Recipient recipient : restartNotificationRecipients) {
                Address address = recipient.toAddress(this);
                send(address, restartNotif);
            }
        }

        return this;
    }

    /**
     * Allows updating the device's network while preserving local objects and other state. Note that network port
     * objects may also need to be recreated with the new network too.w
     */
    public void replaceTransport(Transport newTransport) throws BACnetException {
        transport.terminate();
        transport = newTransport;
        transport.setLocalDevice(this);
        transport.initialize();
    }

    /**
     * Create a ScheduledExecutorService for use by the local device
     *
     * @see java.util.concurrent.ScheduledExecutorService
     */
    protected ScheduledExecutorService createScheduledExecutorService() {
        return new WarpScheduledExecutorService(clock);
    }

    public synchronized void terminate() {
        terminate(10, TimeUnit.SECONDS);
    }

    /**
     * @param timeout     the total time budget for the shutdown: time spent waiting for the network to
     *                    terminate is deducted from the time given to the executor shutdown.
     * @param timeoutUnit the unit of the timeout.
     */
    public synchronized void terminate(long timeout, TimeUnit timeoutUnit) {
        long deadline = System.nanoTime() + timeoutUnit.toNanos(timeout);

        // Terminate the transport before shutting down the timer: network shutdown - in particular
        // the SC state machines - dispatches events and timeouts through the timer, so it must
        // still be operational while the transport closes its connections.
        transport.terminate();
        // Wait for asynchronous network shutdown (e.g. the SC disconnect handshakes) to complete
        // while the timer can still dispatch its events.
        try {
            if (!transport.getNetwork().awaitTermination(timeout, timeoutUnit)) {
                LOG.warn("BACnet network did not terminate within the timeout");
                transport.getNetwork().hardTerminate();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            transport.getNetwork().hardTerminate();
            LOG.warn("Interrupted while waiting for network termination", e);
        }
        if (timer != null) {
            timer.shutdown();
            try {
                long remaining = Math.max(deadline - System.nanoTime(), 1000); // At least one millisecond
                if (!timer.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
                    LOG.warn("BACnet4J timer did not shutdown within the timeout");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupted while waiting for shutdown of executors", e);
            }
        }
        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /*-----------------------------------------------------------
     *-----------------------------------------------------------
     * Executors
     *-----------------------------------------------------------
     -----------------------------------------------------------*/

    /**
     * Schedules the given command for later execution.
     */
    @SuppressWarnings("unchecked")
    public <T> ScheduledFuture<T> schedule(Runnable command, long period, TimeUnit unit) {
        return (ScheduledFuture<T>) timer.schedule(command, period, unit);
    }

    /**
     * Schedules the given command for later execution.
     */
    @SuppressWarnings("unchecked")
    public <T> ScheduledFuture<T> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return (ScheduledFuture<T>) timer.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    /**
     * Schedules the given command for later execution.
     */
    @SuppressWarnings("unchecked")
    public <T> ScheduledFuture<T> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
            TimeUnit unit) {
        return (ScheduledFuture<T>) timer.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    /**
     * Submits the given task for immediate execution.
     */
    @SuppressWarnings("unchecked")
    public <T> Future<T> submit(Runnable task) {
        return (Future<T>) timer.submit(task);
    }

    /**
     * Submits the given task for immediate execution.
     */
    public void execute(Runnable task) {
        timer.execute(task);
    }

    /*-----------------------------------------------------------
     *-----------------------------------------------------------
     * Device configuration
     *-----------------------------------------------------------
     -----------------------------------------------------------*/

    public String getPassword() {
        return password;
    }

    public LocalDevice withPassword(String password) {
        this.password = password;
        return this;
    }

    public LocalDevice withAPDUSegmentTimeout(UnsignedInteger apduSegmentTimeout) {
        deviceObject.writePropertyInternal(PropertyIdentifier.apduSegmentTimeout, apduSegmentTimeout);
        transport.setSegTimeout(apduSegmentTimeout.intValue());
        return this;
    }

    public LocalDevice withAPDUTimeout(UnsignedInteger apduTimeout) {
        deviceObject.writePropertyInternal(PropertyIdentifier.apduTimeout, apduTimeout);
        transport.setTimeout(apduTimeout.intValue());
        return this;
    }

    public LocalDevice withNumberOfApduRetries(UnsignedInteger numberOfApduRetries) {
        deviceObject.writePropertyInternal(PropertyIdentifier.numberOfApduRetries, numberOfApduRetries);
        transport.setRetries(numberOfApduRetries.intValue());
        return this;
    }

    /**
     * Returns the currently configured timeout in ms within the transport.
     */
    public int getTransportTimeout() {
        return transport.getTimeout();
    }

    /*-----------------------------------------------------------
     *-----------------------------------------------------------
     * Local object management
     *-----------------------------------------------------------
     -----------------------------------------------------------*/

    public <T extends BACnetObject> T getObjectRequired(ObjectIdentifier id) throws BACnetServiceException {
        return getObjectRequired(id, false);
    }

    public <T extends BACnetObject> T getObjectRequired(ObjectIdentifier id, boolean allowWildcard)
            throws BACnetServiceException {
        T o = getObject(id, allowWildcard);
        if (o == null)
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.unknownObject);
        return o;
    }

    public List<BACnetObject> getLocalObjects() {
        return localObjects;
    }

    public <T extends BACnetObject> T getObject(ObjectIdentifier id) {
        return getObject(id, false);
    }

    @SuppressWarnings("unchecked")
    public <T extends BACnetObject> T getObject(ObjectIdentifier id, boolean allowWildcard) {
        BACnetObject obj = null;
        if (id.getInstanceNumber() == ObjectIdentifier.UNINITIALIZED && allowWildcard) {
            // 15.5.2.
            if (id.getObjectType().equals(ObjectType.device)) {
                obj = deviceObject;
            } else if (id.getObjectType().equals(ObjectType.networkPort)) {
                // BACnet4J has only one transport and one network, and so returning the first network port object found
                // is sufficient.
                obj = localObjects.stream()
                        .filter(o -> o.getId().getObjectType().equals(ObjectType.networkPort))
                        .findFirst().orElse(null);
            }
        } else {
            obj = localObjects.stream()
                    .filter(o -> o.getId().equals(id))
                    .findFirst().orElse(null);
        }

        return (T) obj;
    }

    public BACnetObject getObject(String name) {
        for (BACnetObject obj : localObjects) {
            if (name.equals(obj.getObjectName()))
                return obj;
        }
        return null;
    }

    public <T extends BACnetObject> T addObject(T obj) throws BACnetServiceException {
        if (obj.getId().getObjectType().equals(ObjectType.device)) {
            if (deviceObject == null) {
                deviceObject = (DeviceObject) obj;
            } else {
                // Don't allow the addition of devices.
                throw new BACnetServiceException(ErrorClass.object, ErrorCode.dynamicCreationNotSupported);
            }
        }
        if (getObject(obj.getId()) != null)
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.objectIdentifierAlreadyExists);
        if (getObject(obj.getObjectName()) != null)
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.duplicateName);

        if (obj.getLocalDevice() != this) {
            throw new IllegalArgumentException("Cannot add an object not created with this local device");
        }
        localObjects.add(obj);

        if (initialized) {
            // If the local device is already initialized, initialize the object.
            obj.initialize();
        }

        return obj;
    }

    public ObjectIdentifier getNextInstanceObjectIdentifier(ObjectType objectType) {
        return new ObjectIdentifier(objectType, getNextInstanceObjectNumber(objectType));
    }

    public int getNextInstanceObjectNumber(ObjectType objectType) {
        // Make a list of existing ids.
        List<Integer> ids = new ArrayList<>();
        int type = objectType.intValue();
        ObjectIdentifier id;
        for (BACnetObject obj : localObjects) {
            id = obj.getId();
            if (id.getObjectType().intValue() == type)
                ids.add(id.getInstanceNumber());
        }

        // Sort the list.
        Collections.sort(ids);

        // Find the first hole in the list.
        int i = 0;
        for (; i < ids.size(); i++) {
            if (ids.get(i) != i)
                break;
        }

        return i;
    }

    public BACnetObject removeObject(ObjectIdentifier id) throws BACnetServiceException {
        BACnetObject obj = getObject(id);
        if (obj != null) {
            localObjects.remove(obj);

            // Notify the object that it was removed.
            obj.terminate();
        } else
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.unknownObject);

        return obj;
    }

    public ServicesSupported getServicesSupported() {
        return deviceObject.get(PropertyIdentifier.protocolServicesSupported);
    }

    /*-----------------------------------------------------------
     *-----------------------------------------------------------
     * Remote device management
     *-----------------------------------------------------------
     -----------------------------------------------------------*/

    /**
     * Returns the cached remote device, or null if not found.
     *
     * @param instanceNumber the instance number of the desired device
     * @return the remote device or null if not found.
     */
    public RemoteDevice getCachedRemoteDevice(int instanceNumber) {
        return remoteDeviceCache.getCachedEntity(instanceNumber);
    }

    public RemoteDevice getCachedRemoteDevice(Address address) {
        return remoteDeviceCache.getCachedEntity(rd -> rd.getAddress().equals(address));
    }

    public RemoteDevice removeCachedRemoteDevice(int instanceNumber) {
        return remoteDeviceCache.removeEntity(instanceNumber);
    }

    /**
     * Finds a remote device for the given instanceNumber by notifying a given callback. If a cached instance is found
     * the callback is called by the calling thread. Otherwise, a finder will be used to try to find it. If this is
     * successful the device will be cached.
     * <p>
     * The benefits of this method are: 1) It will cache the remote device if it is found 2) No blocking is performed
     *
     * @param instanceNumber  the instance number of the desired device
     * @param callback        the callback with which to receive the device
     * @param timeoutCallback the callback to notify of a timeout
     * @param finallyCallback the callback to call in any case
     * @param timeout         the timeout scalar
     * @param unit            the timeout unit
     */
    public void getRemoteDevice(int instanceNumber, Consumer<RemoteDevice> callback, Runnable timeoutCallback,
            Runnable finallyCallback, long timeout, TimeUnit unit) {
        Objects.requireNonNull(callback);

        // Check for a cached instance.
        RemoteDevice rd = getCachedRemoteDevice(instanceNumber);

        if (rd != null) {
            LOG.debug("Found a cached device: {}", instanceNumber);
            // Provide it to the callback in this thread.
            callback.accept(rd);
        } else {
            if (deviceFindTimedOut(instanceNumber)) {
                timeoutCallback.run();
            } else {
                LOG.debug("Requesting the remote device from the remote device finder: {}", instanceNumber);
                RemoteDeviceFinder.findDevice(this, instanceNumber, cbrd -> {
                    forgetDeviceTimeout(instanceNumber);

                    // Cache the device.
                    remoteDeviceCache.putEntity(instanceNumber, cbrd, cachePolicies.getDevicePolicy(instanceNumber));

                    // Notify the client callback
                    callback.accept(cbrd);
                }, () -> {
                    rememberDeviceTimeout(instanceNumber);
                    timeoutCallback.run();
                }, finallyCallback, timeout, unit);
            }
        }
    }

    /**
     * Returns a future to get the remote device for the given instanceNumber. If a cached instance is found the future
     * will be set immediately. Otherwise, a finder will be used to try to find it. If this is successful the device
     * will be cached.
     * <p>
     * The benefits of this method are: 1) It will cache the remote device if it is found. 2) It returns a cancelable
     * future.
     * <p>
     * If multiple threads are likely to request a remote device reference around the same time, it may be better to use
     * the blocking method below.
     *
     * @param instanceNumber the instance number of the desired device
     * @return the remote device future
     */
    public RemoteDeviceFuture getRemoteDevice(int instanceNumber) {
        return new RemoteDeviceFuture() {
            private RemoteDevice remoteDevice;
            private RemoteDeviceFuture future;

            {
                // Check for a cached instance
                RemoteDevice rd = getCachedRemoteDevice(instanceNumber);

                if (rd != null) {
                    LOG.debug("Found a cached device: {}", instanceNumber);
                    remoteDevice = rd;
                } else {
                    if (!deviceFindTimedOut(instanceNumber)) {
                        LOG.debug("Creating a new future to get device: {}", instanceNumber);
                        future = RemoteDeviceFinder.findDevice(LocalDevice.this, instanceNumber);
                    }
                }
            }

            @Override
            public RemoteDevice get(long timeoutMillis) throws BACnetException, CancellationException {
                if (remoteDevice != null)
                    return remoteDevice;
                if (future == null)
                    throw new BACnetTimeoutException("No response from instanceId " + instanceNumber);

                RemoteDevice rd;
                try {
                    rd = future.get(timeoutMillis);
                } catch (BACnetTimeoutException e) {
                    rememberDeviceTimeout(instanceNumber);
                    throw e;
                }

                forgetDeviceTimeout(instanceNumber);

                // Cache the device.
                remoteDeviceCache.putEntity(instanceNumber, rd, cachePolicies.getDevicePolicy(instanceNumber));

                return rd;
            }

            @Override
            public void cancel() {
                if (future != null)
                    future.cancel();
            }
        };
    }

    /**
     * Returns the remote device for the given instanceNumber using the default timeout. If a cached instance is not
     * found the finder will be used to try and find it. A timeout exception is thrown if it can't be found.
     *
     * @param instanceNumber the instance number of the desired device
     * @return the remote device
     * @throws BACnetException if anything goes wrong, including timeout.
     */
    public RemoteDevice getRemoteDeviceBlocking(int instanceNumber) throws BACnetException {
        return getRemoteDeviceBlocking(instanceNumber, transport.getTimeout());
    }

    /**
     * A list of existing futures for each device. Multiple threads may want the same device, and so we also them all to
     * wait on the same future. This has timeout implications since the timeout will be based upon the first thread that
     * made the request, meaning that subsequent threads may experience a shorter timeout than requested.
     */
    private final Map<Integer, RemoteDeviceFuture> futures = new HashMap<>();

    /**
     * Returns the remote device for the given instanceNumber. If a cached instance is not found the finder will be used
     * to try and find it. A timeout exception is thrown if it can't be found.
     * <p/>
     * The benefits of this method are: 1) It will cache the remote device if it is found. 2) Multiple threads that
     * request the same remote device around the same time will be joined on the same request
     * <p/>
     * If you require the ability to cancel a request, use the non-blocking method above.
     *
     * @param instanceNumber the instance number of the desired device
     * @return the remote device
     * @throws BACnetException if anything goes wrong, including timeout.
     */
    public RemoteDevice getRemoteDeviceBlocking(int instanceNumber, long timeoutMillis) throws BACnetException {
        // Check for a cached instance
        RemoteDevice rd = getCachedRemoteDevice(instanceNumber);

        if (rd == null) {
            RemoteDeviceFuture future;
            synchronized (futures) {
                // Check if there is an existing future for the device.
                future = futures.get(instanceNumber);
                if (future == null) {
                    if (deviceFindTimedOut(instanceNumber)) {
                        LOG.debug("Device {} is in the timed out list. Not attempting to find again.", instanceNumber);
                        throw new BACnetTimeoutException("No response from instanceId " + instanceNumber);
                    }

                    LOG.debug("Creating a new future to get device: {}", instanceNumber);
                    // Create a request to get a fresh copy.
                    future = RemoteDeviceFinder.findDevice(this, instanceNumber);
                    futures.put(instanceNumber, future);
                } else {
                    LOG.debug("Using existing future: {}", instanceNumber);
                }
            }

            // Wait for the device.
            try {
                LOG.debug("Waiting on future: {}", instanceNumber);
                if (timeoutMillis == 0)
                    rd = future.get();
                else
                    rd = future.get(timeoutMillis);
                forgetDeviceTimeout(instanceNumber);
            } catch (BACnetTimeoutException e) {
                rememberDeviceTimeout(instanceNumber);
                throw e;
            } finally {
                LOG.debug("Future completed: {}", instanceNumber);

                // Multiple threads can wait on a single future, and only one thread need run the following code.
                synchronized (future) {
                    if (futures.containsKey(instanceNumber)) {
                        LOG.debug("Doing futures cleanup: {}", instanceNumber);

                        // Remove the future.
                        futures.remove(instanceNumber);

                        // Cache the device.
                        if (rd != null) {
                            remoteDeviceCache.putEntity(instanceNumber, rd,
                                    cachePolicies.getDevicePolicy(instanceNumber));
                        }
                    }
                }
            }
        }

        return rd;
    }

    public void addRemoteDevice(RemoteDevice rd) throws BACnetException {
        // Ensure that the remote device has all the required properties. Is a no op if it has already been fully
        // configured.
        DiscoveryUtils.getExtendedDeviceInformation(this, rd);
        // Add to cache with a policy to never expire because this was not automatically discovered.
        remoteDeviceCache.putEntity(rd.getInstanceNumber(), rd, RemoteEntityCachePolicy.NEVER_EXPIRE);
    }

    public void removeRemoteDevice(int deviceId) {
        remoteDeviceCache.removeEntity(deviceId);
    }

    @Override
    public synchronized void close() throws Exception {
        if (initialized) {
            terminate();
        }
    }

    public enum CacheUpdate {
        /**
         * Always update the remote device cache, even if the existing entry has not expired.
         */
        ALWAYS,
        /**
         * Never update the remote device cache, even if the existing entry has expired.
         */
        NEVER,
        /**
         * Only update the remote device cache if the existing entry has expired.
         */
        IF_EXPIRED
    }

    public RemoteDeviceDiscoverer startRemoteDeviceDiscovery() {
        return startRemoteDeviceDiscovery(CacheUpdate.NEVER, null);
    }

    public RemoteDeviceDiscoverer startRemoteDeviceDiscovery(Consumer<RemoteDevice> callback) {
        return startRemoteDeviceDiscovery(CacheUpdate.NEVER, callback);
    }

    /**
     * Creates and starts a remote device discovery. Discovered devices are added to the cache as they are found. The
     * returned discoverer must be stopped by the caller.
     *
     * @param cacheUpdate controls if the remote device cache should be updated
     * @param callback    optional client callback
     * @return the discoverer, which must be stopped by the caller
     */
    public RemoteDeviceDiscoverer startRemoteDeviceDiscovery(CacheUpdate cacheUpdate, Consumer<RemoteDevice> callback) {
        RemoteDeviceDiscoverer discoverer = new RemoteDeviceDiscoverer(this, discoveredDevice -> {
            // Cache the device.
            remoteDeviceCache.putEntity(discoveredDevice.getInstanceNumber(), discoveredDevice,
                    cachePolicies.getDevicePolicy(discoveredDevice.getInstanceNumber()));

            // Call the given callback
            if (callback != null) {
                callback.accept(discoveredDevice);
            }
        }, getExpirationCheck(cacheUpdate));
        discoverer.start();
        return discoverer;
    }

    private Predicate<RemoteDevice> getExpirationCheck(CacheUpdate cacheUpdate) {
        return switch (cacheUpdate) {
            case ALWAYS -> d -> true;
            case NEVER -> d -> false;
            case IF_EXPIRED -> d -> remoteDeviceCache.getCachedEntity(d.getInstanceNumber()) == null;
        };
    }

    /**
     * Updates the remote device with the given number with the given address, but only if the remote device is cached.
     * Otherwise, nothing happens.
     *
     * @param instanceNumber the instance number of the device to update
     * @param address        the device address
     */
    public void updateRemoteDevice(int instanceNumber, Address address) {
        if (address == null)
            throw new NullPointerException("address cannot be null");
        RemoteDevice d = getCachedRemoteDevice(instanceNumber);
        if (d != null) {
            if (address instanceof NetworkSourceAddress) {
                LOG.debug("Updating address with source info, newAddress={}, existingAddress={}", address,
                        d.getAddress());
                // We can confidently change the network number
                d.setAddress(address);
            } else {
                Address newAddress = new Address(d.getAddress().getNetworkNumber().intValue(), address.getMacAddress());
                LOG.debug("Not updating address without source info, newAddress={}, existingAddress={}", newAddress,
                        d.getAddress());
                // This address can be from the source of the socket message (link service)
                // and may not be what we really want to update here.  It was decided in 5.0.0
                // to track incoming addresses via the NetworkSourceAddress class
                // and not blindly set the remote devices new address here
            }
        }
    }

    /**
     * Clears the cache of remote devices.
     */
    public void clearRemoteDevices() {
        remoteDeviceCache.clear();
    }

    public List<RemoteDevice> getRemoteDevices() {
        return remoteDeviceCache.getEntities();
    }

    public RemoteEntityCache<Integer, RemoteDevice> getRemoteDeviceCache() {
        return remoteDeviceCache;
    }

    private void rememberDeviceTimeout(int instanceNumber) {
        synchronized (timeoutDevices) {
            timeoutDevices.put(instanceNumber, clock.millis() + timeoutDeviceRetention);
        }
    }

    private void forgetDeviceTimeout(int instanceNumber) {
        synchronized (timeoutDevices) {
            timeoutDevices.remove(instanceNumber);
        }
    }

    private boolean deviceFindTimedOut(int instanceNumber) {
        synchronized (timeoutDevices) {
            Long expiry = timeoutDevices.get(instanceNumber);
            if (expiry == null)
                return false;
            if (expiry <= clock.millis()) {
                timeoutDevices.remove(instanceNumber);
                return false;
            }
            return true;
        }
    }

    /*-----------------------------------------------------------
     *-----------------------------------------------------------
     * Cached property management
     *-----------------------------------------------------------
     -----------------------------------------------------------*/

    //
    // Get properties
    public <T extends Encodable> T getCachedRemoteProperty(int did, ObjectIdentifier oid, PropertyIdentifier pid) {
        return getCachedRemoteProperty(did, oid, pid, null);
    }

    public <T extends Encodable> T getCachedRemoteProperty(int did, ObjectIdentifier oid, PropertyIdentifier pid,
            UnsignedInteger pin) {
        RemoteDevice rd = getCachedRemoteDevice(did);
        if (rd == null)
            return null;
        return rd.getObjectProperty(oid, pid, pin);
    }

    //
    // Set properties

    public void setCachedRemoteProperty(int did, ObjectIdentifier oid, PropertyIdentifier pid, Encodable value) {
        setCachedRemoteProperty(did, oid, pid, null, value);
    }

    public void setCachedRemoteProperty(int did, ObjectIdentifier oid, PropertyIdentifier pid, UnsignedInteger pin,
            Encodable value) {
        if (value instanceof ErrorClassAndCode e && ErrorClass.device.equals(e.getErrorClass())) {
            // Don't cache devices if the error is about the device. In fact, delete the cached device.
            remoteDeviceCache.removeEntity(did);
            return;
        }

        RemoteDevice rd = getCachedRemoteDevice(did);
        if (rd != null) {
            rd.setObjectProperty(oid, pid, pin, value);
        }
    }

    //
    // Remove properties

    public <T extends Encodable> T removeCachedRemoteProperty(int did, ObjectIdentifier oid, PropertyIdentifier pid) {
        return removeCachedRemoteProperty(did, oid, pid, null);
    }

    public <T extends Encodable> T removeCachedRemoteProperty(int did, ObjectIdentifier oid, PropertyIdentifier pid,
            UnsignedInteger pin) {
        RemoteDevice rd = getCachedRemoteDevice(did);
        if (rd == null)
            return null;
        return rd.removeObjectProperty(oid, pid, pin);
    }

    /*-----------------------------------------------------------
     *-----------------------------------------------------------
     * Message sending
     *-----------------------------------------------------------
     -----------------------------------------------------------*/

    public ServiceFuture send(RemoteDevice d, ConfirmedRequestService serviceRequest) {
        ensureInitialized(serviceRequest);
        return transport.send(d.getAddress(), d.getMaxAPDULengthAccepted(), d.getSegmentationSupported(),
                serviceRequest);
    }

    public ServiceFuture send(Address address, ConfirmedRequestService serviceRequest) {
        ensureInitialized(serviceRequest);
        RemoteDevice d = getCachedRemoteDevice(address);
        if (d == null) {
            // Just use some hopeful defaults.
            return transport.send(address, MaxApduLength.UP_TO_50.getMaxLengthInt(), Segmentation.noSegmentation,
                    serviceRequest);
        }
        return send(d, serviceRequest);
    }

    public void send(RemoteDevice d, ConfirmedRequestService serviceRequest, ResponseConsumer consumer) {
        ensureInitialized(serviceRequest);
        transport.send(d.getAddress(), d.getMaxAPDULengthAccepted(), d.getSegmentationSupported(), serviceRequest,
                consumer);
    }

    public void send(Address address, ConfirmedRequestService serviceRequest, ResponseConsumer consumer) {
        ensureInitialized(serviceRequest);
        RemoteDevice d = getCachedRemoteDevice(address);
        if (d == null) {
            // Just use some hopeful defaults.
            transport.send(address, MaxApduLength.UP_TO_50.getMaxLengthInt(), Segmentation.noSegmentation,
                    serviceRequest, consumer);
        } else
            send(d, serviceRequest, consumer);
    }

    public void send(RemoteDevice d, UnconfirmedRequestService serviceRequest) {
        ensureInitialized(serviceRequest);
        transport.send(d.getAddress(), serviceRequest);
    }

    public void send(Address address, UnconfirmedRequestService serviceRequest) {
        ensureInitialized(serviceRequest);
        transport.send(address, serviceRequest);
    }

    public void sendLocalBroadcast(UnconfirmedRequestService serviceRequest) {
        ensureInitialized(serviceRequest);
        transport.send(getLocalBroadcastAddress(), serviceRequest);
    }

    public void sendGlobalBroadcast(UnconfirmedRequestService serviceRequest) {
        ensureInitialized(serviceRequest);
        transport.send(Address.GLOBAL, serviceRequest);
    }

    private void ensureInitialized(Service service) {
        if (!initialized) {
            throw new BACnetRuntimeException("LocalDevice is not initialized");
        }

        if (isUnconfigured() && !(service instanceof WhoAmIRequest)) {
            // Per clause 19.7, an unconfigured device (Device Identifier 4194303) may only
            // initiate Who-Am-I. Any other service initiation is a spec violation.
            throw new BACnetRuntimeException(
                    "Unconfigured device (Device Identifier 4194303) may only initiate Who-Am-I; "
                            + "attempted to send " + service.getClass().getSimpleName());
        }
    }

    /*-----------------------------------------------------------
     *-----------------------------------------------------------
     * Communication control
     *-----------------------------------------------------------
     -----------------------------------------------------------*/

    private final Object communicationControlMonitor = new Object();
    private EnableDisable communicationControlState = EnableDisable.enable;
    private ScheduledFuture<?> communicationControlFuture;

    public void setCommunicationControl(EnableDisable enableDisable, int minutes) {
        // Per addendum 135-2016bi-2: 'disable' was deprecated in Protocol Revision 20. Callers
        // must migrate to 'disableInitiation'. Requests carrying 'disable' are rejected at the
        // service handler; this guard protects the state field from any other caller.
        if (EnableDisable.disable.equals(enableDisable)) {
            throw new IllegalArgumentException(
                    "EnableDisable.disable is deprecated (bi-2); use EnableDisable.disableInitiation");
        }
        synchronized (communicationControlMonitor) {
            communicationControlState = enableDisable;
            cancelCommunicationControlFuture();
            if (EnableDisable.disableInitiation.equals(enableDisable) && minutes > 0) {
                communicationControlFuture = schedule(() -> {
                    synchronized (communicationControlMonitor) {
                        communicationControlState = EnableDisable.enable;
                        communicationControlFuture = null;
                    }
                }, minutes, TimeUnit.MINUTES);
            }
        }
    }

    private void cancelCommunicationControlFuture() {
        if (communicationControlFuture != null) {
            communicationControlFuture.cancel(false);
            communicationControlFuture = null;
        }
    }

    public EnableDisable getCommunicationControlState() {
        return communicationControlState;
    }

    /*-----------------------------------------------------------
     *-----------------------------------------------------------
     * Persistence
     *-----------------------------------------------------------
     -----------------------------------------------------------*/

    public IPersistence getPersistence() {
        return persistence;
    }

    public void setPersistence(IPersistence persistence) {
        this.persistence = Objects.requireNonNullElseGet(persistence, NullPersistence::new);
    }

    /*-----------------------------------------------------------
     *-----------------------------------------------------------
     * Convenience methods
     *-----------------------------------------------------------
     -----------------------------------------------------------*/

    public Address[] getAllLocalAddresses() {
        return transport.getNetwork().getAllLocalAddresses();
    }

    public Address getLoopbackAddress() {
        return transport.getNetwork().getLoopbackAddress();
    }

    public IAmRequest getIAm() {
        return new IAmRequest(getId(), deviceObject.get(PropertyIdentifier.maxApduLengthAccepted),
                deviceObject.get(PropertyIdentifier.segmentationSupported),
                deviceObject.get(PropertyIdentifier.vendorIdentifier));
    }

    @Override
    public String toString() {
        return deviceObject.getInstanceId() + ": " + deviceObject.getObjectName();
    }

    public void incrementDatabaseRevision() {
        UnsignedInteger databaseRevision = deviceObject.get(PropertyIdentifier.databaseRevision);
        databaseRevision = databaseRevision.increment32();
        deviceObject.writePropertyInternal(PropertyIdentifier.databaseRevision, databaseRevision);
        persistence.saveEncodable("databaseRevision", databaseRevision);
    }

    public Address getLocalBroadcastAddress() {
        return transport.getLocalBroadcastAddress();
    }

    /**
     * Register a callback to use if other devices have the same id as this.
     *
     * @param callback to notify in case of duplicate id
     */
    public void setSameDeviceIdCallback(Consumer<Address> callback) {
        sameDeviceIdCallback = callback;
    }

    /**
     * Notify the registered callback that another device with the same Device id was discovered.
     *
     * @param from the address of the device with the same id.
     */
    public void notifySameDeviceIdCallback(Address from) {
        if (sameDeviceIdCallback != null) {
            // Do this async
            execute(() -> sameDeviceIdCallback.accept(from));
        }
    }
}
