/*
 * Copyright (c) 2017, Matthew Lohbihler
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.serotonin.warp;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WarpScheduledExecutorServiceTest {

    private WarpClock clock;
    private Instant start;
    private ObservableScheduledExecutorService scheduler;

    @Before
    public void before() {
        ZoneId zone = ZoneId.systemDefault();
        clock = new WarpClock();
        start = clock.instant();
        scheduler = new ObservableScheduledExecutorService(new WarpScheduledExecutorService(clock));
    }

    @Test
    public void callables() throws InterruptedException {
        final ScheduledFuture<Boolean> success = scheduler.schedule(() -> true, 100,
            TimeUnit.MINUTES);
        final ScheduledFuture<Boolean> timeout = scheduler.schedule(() -> true, 100,
            TimeUnit.MINUTES);
        final ScheduledFuture<Boolean> exception = scheduler.schedule(() -> {
            throw new Exception("test ex");
        }, 100, TimeUnit.MINUTES);
        final ScheduledFuture<Boolean> cancel = scheduler.schedule(() -> true, 100,
            TimeUnit.MINUTES);

        // Create threads to get future results.
        final AtomicBoolean successResult = new AtomicBoolean(false);
        scheduler.execute(() -> {
            try {
                assertTrue(success.get());
                assertEquals(Duration.ofMinutes(100), Duration.between(start, clock.instant()));
                successResult.set(true);
            } catch (final Exception e) {
                fail(e.getMessage());
            }
        });

        final AtomicBoolean timeoutResult = new AtomicBoolean(false);
        scheduler.execute(() -> {
            try {
                timeout.get(70, TimeUnit.MINUTES);
                fail("Should have timed out");
            } catch (final TimeoutException e) {
                assertEquals(Duration.ofMinutes(70), Duration.between(start, clock.instant()));
                timeoutResult.set(true);
            } catch (final Exception e) {
                fail(e.getMessage());
            }
        });

        final AtomicBoolean exceptionResult = new AtomicBoolean(false);
        scheduler.execute(() -> {
            try {
                exception.get(101, TimeUnit.MINUTES);
                fail("Should have thrown an exception");
            } catch (final ExecutionException e) {
                assertEquals("test ex", e.getCause().getMessage());
                assertEquals(Duration.ofMinutes(100), Duration.between(start, clock.instant()));
                exceptionResult.set(true);
            } catch (final Exception e) {
                fail(e.getMessage());
            }
        });

        final AtomicBoolean cancelResult = new AtomicBoolean(false);
        scheduler.execute(() -> {
            try {
                cancel.get(51, TimeUnit.MINUTES);
                fail("Should have been cancelled");
            } catch (final CancellationException e) {
                assertEquals(Duration.ofMinutes(50), Duration.between(start, clock.instant()));
                cancelResult.set(true);
            } catch (final Exception e) {
                fail(e.getMessage());
            }
        });

        // Let the threads get started
        Thread.sleep(5);

        // Advance the clock 50 minutes and cancel. Do so in a loop for more realistic testing.
        clock.plus(50, TimeUnit.MINUTES, 5, TimeUnit.MINUTES, 20, 2);
        cancel.cancel(false);
        Thread.sleep(20);

        // Advance the clock another 50 minutes.
        clock.plus(50, TimeUnit.MINUTES, 5, TimeUnit.MINUTES, 20, 5);

        assertFalse(success.isCancelled());
        assertFalse(timeout.isCancelled());
        assertFalse(exception.isCancelled());
        assertTrue(cancel.isCancelled());

        assertTrue(successResult.get());
        assertTrue(timeoutResult.get());
        assertTrue(exceptionResult.get());
        assertTrue(cancelResult.get());
    }

    @Test
    public void fixedRate() throws InterruptedException {
        final List<Instant> instants1 = new ArrayList<>();
        final ScheduledFuture<?> future1 = scheduler.scheduleAtFixedRate(
            () -> instants1.add(clock.instant()), 3, 2,
            TimeUnit.MINUTES);
        final List<Instant> instants2 = new ArrayList<>();
        final ScheduledFuture<?> future2 = scheduler.scheduleAtFixedRate(
            () -> instants2.add(clock.instant()), 4, 2,
            TimeUnit.MINUTES);

        // Run the minutes individually to ensure the runtimes.
        clock.plus(7, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, 20, 0);

        assertEquals(3, instants1.size());
        assertEquals(Duration.ofMinutes(3), Duration.between(start, instants1.get(0)));
        assertEquals(Duration.ofMinutes(5), Duration.between(start, instants1.get(1)));
        assertEquals(Duration.ofMinutes(7), Duration.between(start, instants1.get(2)));

        assertEquals(2, instants2.size());
        assertEquals(Duration.ofMinutes(4), Duration.between(start, instants2.get(0)));
        assertEquals(Duration.ofMinutes(6), Duration.between(start, instants2.get(1)));

        // Advance the clock by multiple minutes. The instants in the list will all have the end time, not the proper
        // run times.
        clock.plusHours(1);
        Thread.sleep(50);
        assertEquals(33, instants1.size());
        assertEquals(32, instants2.size());

        // Cancel a task, and ensure that no more runs occur.
        future1.cancel(false);
        clock.plusHours(1);
        Thread.sleep(50);
        assertEquals(33, instants1.size());
        assertEquals(62, instants2.size());

        final AtomicBoolean cancelSuccess = new AtomicBoolean(false);
        scheduler.execute(() -> {
            try {
                future2.get();
                fail("Should have thrown CancellationException");
            } catch (final CancellationException e) {
                cancelSuccess.set(true);
            } catch (final Exception e) {
                fail(e.getMessage());
            }
        });

        Thread.sleep(50);
        future2.cancel(false);
        Thread.sleep(50);

        assertTrue(cancelSuccess.get());
    }

    @Test
    public void fixedDelay() throws InterruptedException {
        final List<Instant> instants1 = new ArrayList<>();
        final ScheduledFuture<?> future1 = scheduler.scheduleWithFixedDelay(
            () -> instants1.add(clock.instant()), 3, 2,
            TimeUnit.MINUTES);
        final List<Instant> instants2 = new ArrayList<>();
        final ScheduledFuture<?> future2 = scheduler.scheduleWithFixedDelay(
            () -> instants2.add(clock.instant()), 4, 2,
            TimeUnit.MINUTES);

        // Run the minutes individually to ensure the runtimes.
        clock.plus(7, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, 20, 20);

        assertEquals(3, instants1.size());
        assertEquals(Duration.ofMinutes(3), Duration.between(start, instants1.get(0)));
        assertEquals(Duration.ofMinutes(5), Duration.between(start, instants1.get(1)));
        assertEquals(Duration.ofMinutes(7), Duration.between(start, instants1.get(2)));

        assertEquals(2, instants2.size());
        assertEquals(Duration.ofMinutes(4), Duration.between(start, instants2.get(0)));
        assertEquals(Duration.ofMinutes(6), Duration.between(start, instants2.get(1)));

        // Advance the clock by multiple minutes. Because these are delayed, each will have run only once.
        clock.plusHours(1);
        Thread.sleep(50);
        assertEquals(4, instants1.size());
        assertEquals(3, instants2.size());

        // Cancel a task, and ensure that no more runs occur.
        future1.cancel(false);
        clock.plusHours(1);
        Thread.sleep(50);
        assertEquals(4, instants1.size());
        assertEquals(4, instants2.size());

        final AtomicBoolean cancelSuccess = new AtomicBoolean(false);
        scheduler.execute(() -> {
            try {
                future2.get();
                fail("Should have thrown CancellationException");
            } catch (final CancellationException e) {
                cancelSuccess.set(true);
            } catch (final Exception e) {
                fail(e.getMessage());
            }
        });

        Thread.sleep(50);
        future2.cancel(false);
        Thread.sleep(50);

        assertTrue(cancelSuccess.get());
    }

    @Test
    public void scheduled() {
        final List<Instant> instants = new ArrayList<>();

        final ScheduledFuture<?> future = scheduler.schedule(() -> instants.add(clock.instant()),
            10, TimeUnit.MINUTES);
        scheduler.schedule((Runnable) () -> instants.add(clock.instant()), 11, TimeUnit.MINUTES);
        scheduler.schedule((Runnable) () -> instants.add(clock.instant()), 12, TimeUnit.MINUTES);
        scheduler.schedule((Runnable) () -> instants.add(clock.instant()), 15, TimeUnit.MINUTES);
        scheduler.schedule((Runnable) () -> instants.add(clock.instant()), 20, TimeUnit.MINUTES);
        scheduler.schedule((Runnable) () -> instants.add(clock.instant()), 25, TimeUnit.MINUTES);
        scheduler.schedule((Runnable) () -> instants.add(clock.instant()), 26, TimeUnit.MINUTES);
        scheduler.schedule((Runnable) () -> instants.add(clock.instant()), 27, TimeUnit.MINUTES);
        scheduler.schedule((Runnable) () -> instants.add(clock.instant()), 28, TimeUnit.MINUTES);
        scheduler.schedule((Runnable) () -> instants.add(clock.instant()), 30, TimeUnit.MINUTES);

        clock.plus(9, TimeUnit.MINUTES, 9, TimeUnit.MINUTES, 20, 10);
        assertEquals(0, instants.size());

        clock.plus(1, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, 20, 10);
        assertEquals(1, instants.size());

        clock.plus(5, TimeUnit.MINUTES, 5, TimeUnit.MINUTES, 20, 10);
        assertEquals(4, instants.size());

        clock.plus(10, TimeUnit.MINUTES, 10, TimeUnit.MINUTES, 20, 10);
        assertEquals(6, instants.size());

        clock.plus(10, TimeUnit.MINUTES, 10, TimeUnit.MINUTES, 20, 10);
        assertEquals(10, instants.size());

        assertEquals(Duration.ofMinutes(10), Duration.between(start, instants.get(0)));
        assertEquals(Duration.ofMinutes(15), Duration.between(start, instants.get(1)));
        assertEquals(Duration.ofMinutes(15), Duration.between(start, instants.get(2)));
        assertEquals(Duration.ofMinutes(15), Duration.between(start, instants.get(3)));
        assertEquals(Duration.ofMinutes(25), Duration.between(start, instants.get(4)));
        assertEquals(Duration.ofMinutes(25), Duration.between(start, instants.get(5)));
        assertEquals(Duration.ofMinutes(35), Duration.between(start, instants.get(6)));
        assertEquals(Duration.ofMinutes(35), Duration.between(start, instants.get(7)));
        assertEquals(Duration.ofMinutes(35), Duration.between(start, instants.get(8)));
        assertEquals(Duration.ofMinutes(35), Duration.between(start, instants.get(9)));

        // Check cancelling
        assertTrue(future.isDone());
        assertFalse(future.cancel(false));
    }

    @Test
    public void shutdownNow() {
        final AtomicInteger counter = new AtomicInteger(0);

        scheduler.schedule((Runnable) () -> counter.incrementAndGet(), 1, TimeUnit.MINUTES);
        scheduler.schedule(() -> counter.incrementAndGet(), 1, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(() -> counter.incrementAndGet(), 1, 1, TimeUnit.MINUTES);
        scheduler.scheduleWithFixedDelay(() -> counter.incrementAndGet(), 1, 1, TimeUnit.MINUTES);

        final List<Runnable> runnables = scheduler.shutdownNow();
        for (final Runnable runnable : runnables) {
            runnable.run();
        }

        assertEquals(4, counter.get());
    }
}
