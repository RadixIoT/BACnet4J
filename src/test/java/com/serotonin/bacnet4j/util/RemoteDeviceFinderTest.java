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

package com.serotonin.bacnet4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.util.RemoteDeviceFinder.RemoteDeviceFuture;

public class RemoteDeviceFinderTest {
    private final TestNetworkMap map = new TestNetworkMap();
    LocalDevice d1;
    LocalDevice d2;
    LocalDevice d3;
    RemoteDevice rd1;
    RemoteDevice rd2;
    RemoteDevice rd3;

    @Before
    public void before() throws Exception {
        d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 20))).initialize();
        d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 20))).initialize();
        d3 = new LocalDevice(3, new DefaultTransport(new TestNetwork(map, 3, 20))).initialize();
    }

    @After
    public void after() {
        // Shut down
        d1.terminate();
        d2.terminate();
        d3.terminate();
    }

    @Test
    public void success() throws CancellationException, BACnetException {
        final RemoteDeviceFuture future1 = RemoteDeviceFinder.findDevice(d1, 2);
        final RemoteDeviceFuture future2 = RemoteDeviceFinder.findDevice(d1, 3);
        final RemoteDeviceFuture future3 = RemoteDeviceFinder.findDevice(d2, 1);
        final RemoteDeviceFuture future4 = RemoteDeviceFinder.findDevice(d2, 3);
        final RemoteDeviceFuture future5 = RemoteDeviceFinder.findDevice(d3, 1);
        final RemoteDeviceFuture future6 = RemoteDeviceFinder.findDevice(d3, 2);

        final RemoteDevice discovered1 = future1.get();
        final RemoteDevice discovered2 = future2.get();
        final RemoteDevice discovered3 = future3.get();
        final RemoteDevice discovered4 = future4.get();
        final RemoteDevice discovered5 = future5.get();
        final RemoteDevice discovered6 = future6.get();
        final RemoteDevice discovered61 = future6.get();

        assertEquals(2, discovered1.getInstanceNumber());
        assertEquals(3, discovered2.getInstanceNumber());
        assertEquals(1, discovered3.getInstanceNumber());
        assertEquals(3, discovered4.getInstanceNumber());
        assertEquals(1, discovered5.getInstanceNumber());
        assertEquals(2, discovered6.getInstanceNumber());
        assertEquals(2, discovered61.getInstanceNumber());

        // Ensure extended information
        assertEquals(865, discovered1.getVendorIdentifier());

        assertEquals(0, d1.getEventHandler().getListenerCount());
        assertEquals(0, d2.getEventHandler().getListenerCount());
        assertEquals(0, d3.getEventHandler().getListenerCount());
    }

    @Test(expected = CancellationException.class)
    public void cancel() throws CancellationException, BACnetException {
        try {
            final RemoteDeviceFuture future = RemoteDeviceFinder.findDevice(d1, 2);
            future.cancel();
            future.get();
        } finally {
            assertEquals(0, d1.getEventHandler().getListenerCount());
        }
    }

    @Test(expected = CancellationException.class)
    public void cancelAfterWait() throws CancellationException, BACnetException {
        try {
            final RemoteDeviceFuture future = RemoteDeviceFinder.findDevice(d2, 3);
            d2.schedule(() -> future.cancel(), 1, TimeUnit.MILLISECONDS);
            future.get();
        } finally {
            assertEquals(0, d2.getEventHandler().getListenerCount());
        }
    }

    @Test(expected = BACnetTimeoutException.class)
    public void timeout() throws CancellationException, BACnetException {
        try {
            final RemoteDeviceFuture future = RemoteDeviceFinder.findDevice(d3, 4);
            future.get(100);
        } finally {
            assertEquals(0, d3.getEventHandler().getListenerCount());
        }
    }

    @Test
    public void callbackSuccess() throws InterruptedException {
        final CountDownLatch countDown = new CountDownLatch(6);

        final AtomicReference<RemoteDevice> discovered1 = new AtomicReference<>(null);
        final AtomicReference<RemoteDevice> discovered2 = new AtomicReference<>(null);
        final AtomicReference<RemoteDevice> discovered3 = new AtomicReference<>(null);
        final AtomicReference<RemoteDevice> discovered4 = new AtomicReference<>(null);
        final AtomicReference<RemoteDevice> discovered5 = new AtomicReference<>(null);
        final AtomicReference<RemoteDevice> discovered6 = new AtomicReference<>(null);

        final AtomicBoolean finallyCalled = new AtomicBoolean(false);

        RemoteDeviceFinder.findDevice(d1, 2, (rd) -> {
            discovered1.set(rd);
            countDown.countDown();
        }, () -> System.out.println("Timeout in 1 getting" + 2), () -> finallyCalled.set(true), 1, TimeUnit.SECONDS);
        RemoteDeviceFinder.findDevice(d1, 3, (rd) -> {
            discovered2.set(rd);
            countDown.countDown();
        }, () -> System.out.println("Timeout in 2 getting" + 3), null, 1, TimeUnit.SECONDS);
        RemoteDeviceFinder.findDevice(d2, 1, (rd) -> {
            discovered3.set(rd);
            countDown.countDown();
        }, () -> System.out.println("Timeout in 3 getting" + 1), null, 1, TimeUnit.SECONDS);
        RemoteDeviceFinder.findDevice(d2, 3, (rd) -> {
            discovered4.set(rd);
            countDown.countDown();
        }, () -> System.out.println("Timeout in 4 getting" + 3), null, 1, TimeUnit.SECONDS);
        RemoteDeviceFinder.findDevice(d3, 1, (rd) -> {
            discovered5.set(rd);
            countDown.countDown();
        }, () -> System.out.println("Timeout in 5 getting" + 1), null, 1, TimeUnit.SECONDS);
        RemoteDeviceFinder.findDevice(d3, 2, (rd) -> {
            discovered6.set(rd);
            countDown.countDown();
        }, () -> System.out.println("Timeout in 6 getting" + 2), null, 1, TimeUnit.SECONDS);

        countDown.await();

        assertEquals(2, discovered1.get().getInstanceNumber());
        assertEquals(3, discovered2.get().getInstanceNumber());
        assertEquals(1, discovered3.get().getInstanceNumber());
        assertEquals(3, discovered4.get().getInstanceNumber());
        assertEquals(1, discovered5.get().getInstanceNumber());
        assertEquals(2, discovered6.get().getInstanceNumber());

        assertEquals(true, finallyCalled.get());

        assertEquals(0, d1.getEventHandler().getListenerCount());
        assertEquals(0, d2.getEventHandler().getListenerCount());
        assertEquals(0, d3.getEventHandler().getListenerCount());
    }

    @Test
    public void callbackTimeout() throws InterruptedException {
        final CountDownLatch countDown = new CountDownLatch(2);
        final AtomicBoolean timeout = new AtomicBoolean(false);
        final AtomicBoolean finallyCalled = new AtomicBoolean(false);

        RemoteDeviceFinder.findDevice(d1, 4, (rd) -> {
            System.out.println("Succeeded getting" + rd);
            fail();
        }, () -> {
            timeout.set(true);
            countDown.countDown();
        }, () -> {
            finallyCalled.set(true);
            countDown.countDown();
        }, 1, TimeUnit.SECONDS);

        countDown.await();

        assertEquals(true, timeout.get());
        assertEquals(true, finallyCalled.get());

        assertEquals(0, d1.getEventHandler().getListenerCount());
    }
}
