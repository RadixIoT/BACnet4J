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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WarpClockTest {
    static final Logger LOG = LoggerFactory.getLogger(WarpClockTest.class);

    @Test
    public void listenerTest() {
        final WarpClock clock = new WarpClock();
        final LocalDateTime start = clock.getDateTime();

        final AtomicReference<LocalDateTime> update = new AtomicReference<>(null);

        final ClockListener listener = new ClockListener() {
            @Override
            public void clockUpdate(final LocalDateTime dateTime) {
                update.set(dateTime);
            }
        };
        clock.addListener(listener);

        clock.plusNanos(10);
        assertEquals(Duration.ofNanos(10), Duration.between(start, update.get()));

        clock.plusMillis(11);
        assertEquals(Duration.ofNanos(10) //
                .plus(Duration.ofMillis(11)), Duration.between(start, update.get()));

        clock.plusSeconds(12);
        assertEquals(Duration.ofNanos(10) //
                .plus(Duration.ofMillis(11)) //
                .plus(Duration.ofSeconds(12)), Duration.between(start, update.get()));

        clock.plusMinutes(13);
        assertEquals(Duration.ofNanos(10) //
                .plus(Duration.ofMillis(11)) //
                .plus(Duration.ofSeconds(12)) //
                .plus(Duration.ofMinutes(13)), Duration.between(start, update.get()));

        clock.plusHours(14);
        assertEquals(Duration.ofNanos(10) //
                .plus(Duration.ofMillis(11)) //
                .plus(Duration.ofSeconds(12)) //
                .plus(Duration.ofMinutes(13)) //
                .plus(Duration.ofHours(14)), Duration.between(start, update.get()));
    }

    @Test
    public void timeoutTest() throws InterruptedException {
        final WarpClock clock = new WarpClock();

        final AtomicBoolean commandCompleted = new AtomicBoolean(false);
        final TimeoutFuture<?> future = clock.setTimeout(() -> commandCompleted.set(true), 555, TimeUnit.MILLISECONDS);

        final AtomicBoolean futureCompleted = new AtomicBoolean(false);
        final Thread thread = new Thread(() -> {
            try {
                future.get();
                futureCompleted.set(true);
            } catch (final Exception e) {
                LOG.error("Error in future get call", e);
            }
        });
        thread.start();
        Thread.yield();

        clock.plusMillis(554);
        assertFalse(commandCompleted.get());
        Thread.yield();
        assertFalse(futureCompleted.get());

        clock.plusMillis(1);
        assertTrue(commandCompleted.get());
        thread.join();
        assertTrue(futureCompleted.get());
    }

    @Test
    public void timeoutTest2() throws InterruptedException {
        final WarpClock clock = new WarpClock();

        final AtomicBoolean commandCompleted = new AtomicBoolean(false);
        final TimeoutFuture<?> future = clock.setTimeout(() -> commandCompleted.set(true), 555, TimeUnit.MILLISECONDS);

        final AtomicBoolean futureCompleted = new AtomicBoolean(false);
        final Thread thread = new Thread(() -> {
            try {
                future.get();
                futureCompleted.set(true);
            } catch (final Exception e) {
                LOG.error("Error in future get call", e);
            }
        });
        thread.start();
        Thread.sleep(50);

        clock.plusMillis(554);
        assertFalse(commandCompleted.get());
        Thread.sleep(50);
        assertFalse(futureCompleted.get());

        clock.plusMillis(1);
        assertTrue(commandCompleted.get());
        thread.join();
        assertTrue(futureCompleted.get());
    }

    @Test
    public void timeoutCancelTest() throws InterruptedException {
        final WarpClock clock = new WarpClock();

        final AtomicBoolean commandCompleted = new AtomicBoolean(false);
        final TimeoutFuture<?> future = clock.setTimeout(() -> commandCompleted.set(true), 555, TimeUnit.MILLISECONDS);

        final AtomicBoolean futureCompleted = new AtomicBoolean(false);
        final Thread thread = new Thread(() -> {
            try {
                future.get();
                LOG.error("Should have thrown an exception");
            } catch (final CancellationException e) {
                futureCompleted.set(true);
            } catch (final Exception e) {
                LOG.error("Error in future get call", e);
            }
        });
        thread.start();
        Thread.yield();

        clock.plusMillis(554);
        assertFalse(commandCompleted.get());
        Thread.yield();
        assertFalse(futureCompleted.get());

        assertTrue(future.cancel());

        clock.plusMillis(1);
        assertFalse(commandCompleted.get());
        thread.join();
        assertTrue(futureCompleted.get());
    }

    @Test
    public void timeoutExceptionTest() throws InterruptedException {
        final WarpClock clock = new WarpClock();

        final TimeoutFuture<?> future = clock.setTimeout(() -> {
            throw new Exception("test ex");
        }, 555, TimeUnit.MILLISECONDS);

        final AtomicBoolean futureCompleted = new AtomicBoolean(false);
        final Thread thread = new Thread(() -> {
            try {
                future.get();
                LOG.error("Should have thrown an exception");
            } catch (final Exception e) {
                if ("test ex".equals(e.getMessage()))
                    futureCompleted.set(true);
                else
                    LOG.error("Error in future get call", e);
            }
        });
        thread.start();

        clock.plusMillis(554);
        Thread.yield();
        assertFalse(futureCompleted.get());

        clock.plusMillis(1);
        thread.join();
        assertTrue(futureCompleted.get());
    }

    @Test
    public void multipleTimeoutsTest() {
        final WarpClock clock = new WarpClock();

        final AtomicBoolean commandCompleted1 = new AtomicBoolean(false);
        clock.setTimeout(() -> commandCompleted1.set(true), 10, TimeUnit.MILLISECONDS);

        final AtomicBoolean commandCompleted2 = new AtomicBoolean(false);
        clock.setTimeout(() -> commandCompleted2.set(true), 11, TimeUnit.MILLISECONDS);

        final AtomicBoolean commandCompleted3 = new AtomicBoolean(false);
        clock.setTimeout(() -> commandCompleted3.set(true), 12, TimeUnit.MILLISECONDS);

        final AtomicBoolean commandCompleted4 = new AtomicBoolean(false);
        clock.setTimeout(() -> commandCompleted4.set(true), 13, TimeUnit.MILLISECONDS);

        clock.plusMillis(9);
        assertFalse(commandCompleted1.get());
        assertFalse(commandCompleted2.get());
        assertFalse(commandCompleted3.get());
        assertFalse(commandCompleted4.get());

        clock.plusMillis(1);
        assertTrue(commandCompleted1.get());
        assertFalse(commandCompleted2.get());
        assertFalse(commandCompleted3.get());
        assertFalse(commandCompleted4.get());

        clock.plusMillis(2);
        assertTrue(commandCompleted1.get());
        assertTrue(commandCompleted2.get());
        assertTrue(commandCompleted3.get());
        assertFalse(commandCompleted4.get());

        clock.plusMillis(1);
        assertTrue(commandCompleted1.get());
        assertTrue(commandCompleted2.get());
        assertTrue(commandCompleted3.get());
        assertTrue(commandCompleted4.get());
    }

    @Test
    public void gregorian() {
        final WarpClock clock = new WarpClock();
        clock.set(2115, Month.AUGUST, 8, 23, 58, 0);
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(clock.millis());

        assertEquals(2115, gc.get(Calendar.YEAR));
        assertEquals(Calendar.AUGUST, gc.get(Calendar.MONTH));
        assertEquals(8, gc.get(Calendar.DATE));
        assertEquals(23, gc.get(Calendar.HOUR_OF_DAY));
        assertEquals(58, gc.get(Calendar.MINUTE));
        assertEquals(0, gc.get(Calendar.SECOND));
    }
}
