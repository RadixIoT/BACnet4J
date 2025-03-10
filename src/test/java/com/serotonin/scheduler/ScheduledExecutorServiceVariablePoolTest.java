/*
 * MIT License
 *
 * Copyright (c) 2017 Matthew Lohbihler
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.serotonin.scheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ScheduledExecutorServiceVariablePoolTest {
    static final Logger LOG = LoggerFactory.getLogger(ScheduledExecutorServiceVariablePoolTest.class);

    // The tolerance in milliseconds that is allowed between delays and completion times
    // of the executor service version and the timer version.
    private static final long TOLERANCE_MILLIS = 25;

    private final Clock clock = Clock.systemUTC();
    private ScheduledExecutorService executor;
    private ScheduledExecutorServiceVariablePool timer;

    @Before
    public void before() {
        executor = Executors.newScheduledThreadPool(3);
        timer = new ScheduledExecutorServiceVariablePool();
    }

    @After
    public void after() {
        executor.shutdown();
        timer.shutdown();
    }

    @Test
    public void schedule() {
        final AtomicLong executorRuntime = new AtomicLong();
        final AtomicLong timerRuntime = new AtomicLong();

        final ScheduledFuture<?> executorFuture = executor.schedule(() -> executorRuntime.set(clock.millis()), 200,
                TimeUnit.MILLISECONDS);
        final ScheduledFuture<?> timerFuture = timer.schedule(() -> timerRuntime.set(clock.millis()), 200,
                TimeUnit.MILLISECONDS);

        // Check immediately
        assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS), timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());

        // Check after execution
        sleepPeacefully(201);
        assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS), timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());
        assertTolerance(executorRuntime.get(), timerRuntime.get());

        // Try canceling now.
        executorFuture.cancel(true);
        timerFuture.cancel(true);
        assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS), timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());
        assertTolerance(executorRuntime.get(), timerRuntime.get());
    }

    @Test
    public void scheduleCancel() throws InterruptedException, ExecutionException {
        final AtomicLong executorRuntime = new AtomicLong();
        final AtomicLong timerRuntime = new AtomicLong();

        final ScheduledFuture<?> executorFuture = executor.schedule(() -> executorRuntime.set(clock.millis()), 200,
                TimeUnit.MILLISECONDS);
        final ScheduledFuture<?> timerFuture = timer.schedule(() -> timerRuntime.set(clock.millis()), 200,
                TimeUnit.MILLISECONDS);

        // Cancel half way
        sleepPeacefully(100);
        executorFuture.cancel(true);
        timerFuture.cancel(true);
        assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS), timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());
        assertEquals(executorRuntime.get(), timerRuntime.get());

        try {
            executorFuture.get();
            fail("Should have failed with CancellationException");
        } catch (final CancellationException e) {
            // Expected
        }

        try {
            timerFuture.get();
            fail("Should have failed with CancellationException");
        } catch (final CancellationException e) {
            // Expected
        }
    }

    @Test
    public void scheduleTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        final ScheduledFuture<?> executorFuture = executor.schedule(() -> sleepPeacefully(200), 200,
                TimeUnit.MILLISECONDS);
        final ScheduledFuture<?> timerFuture = timer.schedule(() -> sleepPeacefully(200), 200, TimeUnit.MILLISECONDS);

        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());

        // Get with timeout halfway
        try {
            executorFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e) {
            // Expected
        }

        try {
            // We already waited 100ms for the executor future, so don't wait here.
            timerFuture.get(0, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e1) {
            // Expected
        }

        // Wait until both tasks are running. Sleeping for 100ms will bring us to ~200ms elapsed
        sleepPeacefully(100);
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());

        try {
            executorFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e) {
            // Expected
        }

        try {
            // We already waited 100ms for the executor future, so don't wait here.
            timerFuture.get(0, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e1) {
            // Expected
        }

        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());

        // Wait until both tasks are done.
        executorFuture.get(100 + TOLERANCE_MILLIS, TimeUnit.MILLISECONDS);
        timerFuture.get(TOLERANCE_MILLIS, TimeUnit.MILLISECONDS);

        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());
    }

    @Test
    public void scheduleFixed() {
        final AtomicLong executorCompleteTime = new AtomicLong();
        final AtomicLong timerCompleteTime = new AtomicLong();

        final ScheduledFuture<?> executorFuture = executor.scheduleAtFixedRate(() -> {
            sleepPeacefully(100);
            executorCompleteTime.set(clock.millis());
        }, 300, 200, TimeUnit.MILLISECONDS);

        final ScheduledFuture<?> timerFuture = timer.scheduleAtFixedRate(() -> {
            sleepPeacefully(100);
            timerCompleteTime.set(clock.millis());
        }, 300, 200, TimeUnit.MILLISECONDS);

        // Check immediately
        assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS), timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());
        assertEquals(executorCompleteTime.get(), timerCompleteTime.get());

        // Sleep for 50 and then check every 100 for a while
        long next = clock.millis() + 50;
        sleepPeacefully(next - clock.millis());
        for (int i = 0; i < 10; i++) {
            assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS),
                    timerFuture.getDelay(TimeUnit.MILLISECONDS));
            assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
            assertEquals(executorFuture.isDone(), timerFuture.isDone());
            assertTolerance(executorCompleteTime.get(), timerCompleteTime.get());
            next += 100;
            sleepPeacefully(next - clock.millis());
        }

        // Cancel
        executorFuture.cancel(true);
        timerFuture.cancel(true);

        // Check again
        assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS), timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());
        assertTolerance(executorCompleteTime.get(), timerCompleteTime.get());
    }

    @Test
    public void scheduleDelay() {
        final AtomicLong executorCompleteTime = new AtomicLong();
        final AtomicLong timerCompleteTime = new AtomicLong();

        final ScheduledFuture<?> executorFuture = executor.scheduleWithFixedDelay(() -> {
            sleepPeacefully(100);
            executorCompleteTime.set(clock.millis());
        }, 300, 200, TimeUnit.MILLISECONDS);

        final ScheduledFuture<?> timerFuture = timer.scheduleWithFixedDelay(() -> {
            sleepPeacefully(100);
            timerCompleteTime.set(clock.millis());
        }, 300, 200, TimeUnit.MILLISECONDS);

        // Check immediately
        assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS), timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());
        assertEquals(executorCompleteTime.get(), timerCompleteTime.get());

        // Sleep for 50 and then check every 100 for a while
        long next = clock.millis() + 50;
        sleepPeacefully(next - clock.millis());
        for (int i = 0; i < 10; i++) {
            assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS),
                    timerFuture.getDelay(TimeUnit.MILLISECONDS));
            assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
            assertEquals(executorFuture.isDone(), timerFuture.isDone());
            assertTolerance(executorCompleteTime.get(), timerCompleteTime.get());
            next += 100;
            sleepPeacefully(next - clock.millis());
        }

        // Cancel
        executorFuture.cancel(true);
        timerFuture.cancel(true);

        // Check again
        assertTolerance(executorFuture.getDelay(TimeUnit.MILLISECONDS), timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());
        assertTolerance(executorCompleteTime.get(), timerCompleteTime.get());
    }

    @Test
    public void scheduleCallable() throws InterruptedException, ExecutionException, TimeoutException {
        final Callable<String> executorCallable = () -> {
            sleepPeacefully(200);
            return "executor";
        };
        final Callable<String> timerCallable = () -> {
            sleepPeacefully(200);
            return "timer";
        };

        final ScheduledFuture<?> executorFuture = executor.schedule(executorCallable, 200, TimeUnit.MILLISECONDS);
        final ScheduledFuture<?> timerFuture = timer.schedule(timerCallable, 200, TimeUnit.MILLISECONDS);

        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());

        // Get with timeout halfway
        try {
            executorFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e) {
            // Expected
        }

        try {
            // We already waited 100ms for the executor future, so don't wait here.
            timerFuture.get(0, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e1) {
            // Expected
        }

        // Wait until both tasks are running. Sleeping for 100ms will bring us to ~200ms elapsed
        sleepPeacefully(100);
        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());

        try {
            executorFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e) {
            // Expected
        }

        try {
            // We already waited 100ms for the executor future, so don't wait here.
            timerFuture.get(0, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e1) {
            // Expected
        }

        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());

        // Wait until both tasks are done.
        assertEquals("executor", executorFuture.get(100 + TOLERANCE_MILLIS, TimeUnit.MILLISECONDS));
        assertEquals("timer", timerFuture.get(1, TimeUnit.MILLISECONDS));

        assertEquals(executorFuture.isCancelled(), timerFuture.isCancelled());
        assertEquals(executorFuture.isDone(), timerFuture.isDone());
    }

    @Test
    public void multipleTasks() {
        final AtomicLong executorRuntime0 = new AtomicLong();
        final AtomicLong executorRuntime1 = new AtomicLong();
        final AtomicLong executorRuntime2 = new AtomicLong();
        final AtomicLong executorRuntime3 = new AtomicLong();
        final AtomicLong executorRuntime4 = new AtomicLong();
        final AtomicLong executorRuntime5 = new AtomicLong();
        final AtomicLong executorRuntime6 = new AtomicLong();
        final AtomicLong executorRuntime7 = new AtomicLong();
        final AtomicLong executorRuntime8 = new AtomicLong();
        final AtomicLong executorRuntime9 = new AtomicLong();

        final long start = clock.millis();
        timer.schedule(() -> executorRuntime0.set(clock.millis()), 500, TimeUnit.MILLISECONDS);
        timer.schedule(() -> executorRuntime1.set(clock.millis()), 400, TimeUnit.MILLISECONDS);
        timer.schedule(() -> executorRuntime2.set(clock.millis()), 600, TimeUnit.MILLISECONDS);
        timer.schedule(() -> executorRuntime3.set(clock.millis()), 300, TimeUnit.MILLISECONDS);
        timer.schedule(() -> executorRuntime4.set(clock.millis()), 900, TimeUnit.MILLISECONDS);
        timer.schedule(() -> executorRuntime5.set(clock.millis()), 700, TimeUnit.MILLISECONDS);
        timer.schedule(() -> executorRuntime6.set(clock.millis()), 800, TimeUnit.MILLISECONDS);
        timer.schedule(() -> executorRuntime7.set(clock.millis()), 100, TimeUnit.MILLISECONDS);
        timer.schedule(() -> executorRuntime8.set(clock.millis()), 1000, TimeUnit.MILLISECONDS);
        timer.schedule(() -> executorRuntime9.set(clock.millis()), 200, TimeUnit.MILLISECONDS);

        // Wait for all tasks to run
        sleepPeacefully(1100);

        // Verify the run times
        assertTolerance(start + 500, executorRuntime0.get(), 100);
        assertTolerance(start + 400, executorRuntime1.get(), 100);
        assertTolerance(start + 600, executorRuntime2.get(), 100);
        assertTolerance(start + 300, executorRuntime3.get(), 100);
        assertTolerance(start + 900, executorRuntime4.get(), 100);
        assertTolerance(start + 700, executorRuntime5.get(), 100);
        assertTolerance(start + 800, executorRuntime6.get(), 100);
        assertTolerance(start + 100, executorRuntime7.get(), 100);
        assertTolerance(start + 1000, executorRuntime8.get(), 100);
        assertTolerance(start + 200, executorRuntime9.get(), 100);
    }

    @Test
    public void getWhileScheduled() throws InterruptedException, ExecutionException {
        final long start = clock.millis();
        final ScheduledFuture<?> future = timer.schedule(() -> {
            /* no op */ }, 500, TimeUnit.MILLISECONDS);
        future.get();
        assertTolerance(start + 500, clock.millis(), 100);
    }

    @Test
    public void shutdown() throws InterruptedException {
        timer.schedule(() -> sleepPeacefully(500), 100, TimeUnit.MILLISECONDS);

        // Don't shut down until the task has started.
        sleepPeacefully(150);
        final long start = clock.millis();
        timer.shutdown();
        final boolean done = timer.awaitTermination(1, TimeUnit.SECONDS);
        assertEquals(true, done);
        assertTolerance(start + 450, clock.millis());
    }

    @Test
    public void getTimedWhileScheduled() throws InterruptedException, ExecutionException, TimeoutException {
        final long start = clock.millis();
        final ScheduledFuture<?> future = timer.schedule(() -> {
            /* no op */ }, 500, TimeUnit.MILLISECONDS);
        future.get(1000, TimeUnit.MILLISECONDS);
        assertTolerance(start + 500, clock.millis(), 100);
    }

    private static void assertTolerance(final long expected, final long actual) {
        assertTolerance(expected, actual, TOLERANCE_MILLIS);
    }

    private static void assertTolerance(final long expected, final long actual, final long tolerance) {
        long diff = expected - actual;
        if (diff < 0)
            diff = -diff;
        if (diff > tolerance)
            fail("Out of tolerance: expected=" + expected + ", actual=" + actual + ", tolerance=" + TOLERANCE_MILLIS
                    + ", diff=" + diff);
    }

    private static void sleepPeacefully(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void cancel() {
        final ScheduledFuture<?> future = timer.scheduleWithFixedDelay(() -> LOG.info("Executed"), 1, 1,
                TimeUnit.SECONDS);
        sleepPeacefully(1100);
        final boolean cancelled = future.cancel(false);
        LOG.info("Cancelled: {}", cancelled);
        sleepPeacefully(3000);
    }
}
