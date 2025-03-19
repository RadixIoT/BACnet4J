/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 RadixIoT. All rights reserved.
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
