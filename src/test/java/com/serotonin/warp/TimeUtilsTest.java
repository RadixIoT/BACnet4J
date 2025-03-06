/*
 * Copyright (c) 2017, Matthew Lohbihler
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.serotonin.warp;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TimeUtilsTest {
    @Test
    public void simWaitTimeout() throws InterruptedException {
        final WarpClock clock = new WarpClock();

        final AtomicBoolean monitor = new AtomicBoolean(false);
        final Thread thread = new Thread(() -> {
            try {
                WarpUtils.wait(clock, monitor, 1000, TimeUnit.SECONDS);
                monitor.set(true);
            } catch (final InterruptedException e) {
                fail();
            }
        });
        thread.start();
        // Give the thread a chance to get into the wait.
        Thread.sleep(50);

        clock.plusSeconds(999);
        Thread.yield();
        assertFalse(monitor.get());

        clock.plusSeconds(1);
        thread.join();
        assertTrue(monitor.get());
    }

    @Test
    public void simWaitMultiTimeout() throws InterruptedException {
        final WarpClock clock = new WarpClock();
        final Instant start = clock.instant();
        final Object monitor = new Object();

        new Thread(() -> {
            synchronized (monitor) {
                try {
                    WarpUtils.wait(clock, monitor, 10, TimeUnit.HOURS);
                    assertEquals(Duration.ofHours(10), Duration.between(start, clock.instant()));
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            synchronized (monitor) {
                try {
                    WarpUtils.wait(clock, monitor, 20, TimeUnit.HOURS);
                    assertEquals(Duration.ofHours(20), Duration.between(start, clock.instant()));
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Thread.sleep(50);
        for (int i = 0; i < 30; i++) {
            clock.plusHours(1);
            Thread.sleep(2);
        }
    }

    @Test
    public void simWaitNotify() throws InterruptedException {
        final WarpClock clock = new WarpClock();

        final AtomicBoolean monitor = new AtomicBoolean(false);
        final Thread thread = new Thread(() -> {
            try {
                WarpUtils.wait(clock, monitor, 1000, TimeUnit.SECONDS);
                monitor.set(true);
            } catch (final InterruptedException e) {
                fail();
            }
        });
        thread.start();
        // Give the thread a chance to get into the wait.
        Thread.sleep(50);

        clock.plusSeconds(999);
        Thread.yield();
        assertFalse(monitor.get());

        synchronized (monitor) {
            monitor.notify();
        }
        thread.join();
        assertTrue(monitor.get());
    }

    @Test
    public void simSleep() throws InterruptedException {
        final WarpClock clock = new WarpClock();

        final AtomicBoolean monitor = new AtomicBoolean(false);
        final Thread thread = new Thread(() -> {
            try {
                WarpUtils.sleep(clock, 1000, TimeUnit.SECONDS);
                monitor.set(true);
            } catch (final InterruptedException e) {
                fail();
            }
        });
        thread.start();
        // Give the thread a chance to get into the sleep.
        Thread.sleep(50);

        clock.plusSeconds(999);
        Thread.yield();
        assertFalse(monitor.get());

        clock.plusSeconds(1);
        thread.join();
        assertTrue(monitor.get());
    }
}
