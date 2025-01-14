/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Infinite Automation Software,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.infiniteautomation.com for commercial license options.
 *
 * @author Matthew Lohbihler
 */
package com.serotonin.bacnet4j.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;

/**
 * A utility for finding devices on the network. Uses a callback to notify the client of found devices. Does not
 * provide duplicates even if multiple IAms are received from the same device. This utility does not time out; it
 * must be explicitly started and stopped by the client.
 *
 * If a callback is not provided - or even if it is - the getRemoteDevices method can be used to get all devices
 * discovered so far in a batch.
 *
 * @author Matthew
 */
public class RemoteDeviceDiscoverer implements AutoCloseable {
    private final LocalDevice localDevice;
    private final Consumer<RemoteDevice> callback;

    private DeviceEventAdapter adapter;
    private final Map<Integer, RemoteDevice> allDevices = new HashMap<>();
    private final List<RemoteDevice> latestDevices = new ArrayList<>();
    private final Predicate<RemoteDevice> expirationCheck;

    public RemoteDeviceDiscoverer(final LocalDevice localDevice) {
        this(localDevice, null);
    }

    public RemoteDeviceDiscoverer(final LocalDevice localDevice, final Consumer<RemoteDevice> callback) {
        this(localDevice, callback, remoteDevice -> false);
    }

    public RemoteDeviceDiscoverer(final LocalDevice localDevice, final Consumer<RemoteDevice> callback,
                                  final Predicate<RemoteDevice> expirationCheck) {
        this.localDevice = localDevice;
        this.callback = callback;
        this.expirationCheck = expirationCheck;
    }

    public synchronized void start() {
        if (adapter != null) {
            throw new IllegalStateException("Already started");
        }
        this.adapter = new DeviceEventAdapter() {
            @Override
            public void iAmReceived(final RemoteDevice newDevice) {
                BooleanHolder updated = new BooleanHolder();
                synchronized (allDevices) {
                    // Check if we already know about this device.
                    allDevices.compute(newDevice.getInstanceNumber(), (k, existing) -> {
                        if (existing == null || expirationCheck.test(existing)) {
                            updated.value = true;
                            return newDevice;
                        }
                        return existing;
                    });

                    if (updated.value) {
                        // Add to latest devices
                        latestDevices.add(newDevice);
                    }
                }

                // Notify the callback
                if (updated.value && callback != null) {
                    callback.accept(newDevice);
                }
            }
        };

        // Register self as an event listener
        localDevice.getEventHandler().addListener(adapter);

        // Send a WhoIs
        localDevice.sendGlobalBroadcast(new WhoIsRequest());
    }

    public synchronized void stop() {
        if (adapter != null) {
            // Unregister as a listener
            localDevice.getEventHandler().removeListener(adapter);
            this.adapter = null;
        }
    }

    /**
     * Returns all devices discovered by this discoverer so far.
     */
    public List<RemoteDevice> getRemoteDevices() {
        synchronized (allDevices) {
            return new ArrayList<>(allDevices.values());
        }
    }

    /**
     * Returns all devices discovered by this discoverer since the last time this method was called.
     */
    public List<RemoteDevice> getLatestRemoteDevices() {
        synchronized (allDevices) {
            final List<RemoteDevice> result = new ArrayList<>(latestDevices);
            latestDevices.clear();
            return result;
        }
    }

    @Override
    public void close() {
        stop();
    }

    private static class BooleanHolder {
        private boolean value = false;
    }
}
