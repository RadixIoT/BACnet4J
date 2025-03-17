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

import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.warp.WarpClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ScheduledExecutorServiceVariablePoolTest {
    static final Logger LOG = LoggerFactory.getLogger(ScheduledExecutorServiceVariablePoolTest.class);

    // The tolerance in milliseconds that is allowed between delays and completion times
    // of the executor service version and the timer version.
    private static final long TOLERANCE_MILLIS = 25;

    private final WarpClock clock = new WarpClock(ZoneOffset.UTC);
    private ScheduledExecutorServiceVariablePool timer;
    private ScheduledExecutorService executor;

    @Before
    public void before() {
        executor = Executors.newScheduledThreadPool(3);
        timer = new ScheduledExecutorServiceVariablePool(clock);
    }

    @After
    public void after() {
        timer.shutdown();
    }

    @Test
    public void schedule() {
        final long startTime = clock.millis();

        final AtomicLong executorRuntime = new AtomicLong();
        final AtomicLong timerRuntime = new AtomicLong();

        final ScheduledFuture<?> executorFuture = executor.schedule(() -> executorRuntime.set(clock.millis()), 200,
                TimeUnit.MILLISECONDS);
        final ScheduledFuture<?> timerFuture = timer.schedule(() -> timerRuntime.set(clock.millis()), 200,
                TimeUnit.MILLISECONDS);

        // Check immediately
        assertEquals(200, timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, timerFuture.isDone());

        // Check after execution
        clock.plusMillis(200);

        //Await Condition
        TestUtils.assertTrueCondition(() -> timerRuntime.get() != 0, 1000);

        assertEquals(0, timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(true, timerFuture.isDone());
        assertEquals(startTime + 200, timerRuntime.get());

        // Try canceling now.
        executorFuture.cancel(true);
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(true, timerFuture.isDone());

        timerFuture.cancel(true);
        assertEquals(0, timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(true, timerFuture.isDone());
        assertEquals(startTime + 200, timerRuntime.get());
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
        clock.plusMillis(100);

        //Await Condition
        TestUtils.assertTrueCondition(() -> executorRuntime.get() == 0, 1000);
        TestUtils.assertTrueCondition(() -> timerRuntime.get() == 0, 1000);

        executorFuture.cancel(true);
        TestUtils.assertTrueCondition(executorFuture::isCancelled, 1000);
        assertEquals(true, executorFuture.isCancelled());
        assertEquals(true, executorFuture.isDone());
        assertEquals(0, executorRuntime.get());

        try {
            executorFuture.get();
            fail("Should have failed with CancellationException");
        } catch (final CancellationException e) {
            // Expected
        }

        timerFuture.cancel(true);
        TestUtils.assertTrueCondition(timerFuture::isCancelled, 1000);

        assertEquals(100, timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(true, timerFuture.isCancelled());
        assertEquals(true, timerFuture.isDone());
        assertEquals(0, timerRuntime.get());

        try {
            timerFuture.get();
            fail("Should have failed with CancellationException");
        } catch (final CancellationException e) {
            // Expected
        }
    }

    /**
     * Schedule tasks for 200ms in the future and have them continue to execute until the warp clock is past 250ms
     * in the future so we can check their running states
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    @Test
    public void scheduleTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        final long startTime = clock.millis();
        final AtomicLong executorRuntime = new AtomicLong();
        final AtomicLong timerRuntime = new AtomicLong();

        final ScheduledFuture<?> executorFuture = executor.schedule(() -> {
                executorRuntime.set(clock.millis());
                while(clock.millis() < startTime + 250) {
                    sleepPeacefully(10);
                }

            }, 200, TimeUnit.MILLISECONDS);
        final ScheduledFuture<?> timerFuture = timer.schedule(() -> {
                timerRuntime.set(clock.millis());
                while(clock.millis() < startTime + 250) {
                   sleepPeacefully(10);
                }
            }, 200, TimeUnit.MILLISECONDS);

        assertEquals(false, executorFuture.isCancelled());
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, executorFuture.isDone());
        assertEquals(false, timerFuture.isDone());

        // Get with timeout halfway
        try {
            executorFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e) {
            // Expected
        }

        clock.plusMillis(200);
        try {
            // Using a different clock, so wait 100ms
            timerFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e1) {
            // Expected
        }

        // Wait until both tasks are running, 200ms elapsed
        TestUtils.assertTrueCondition(() -> executorRuntime.get() != 0, 1000);
        TestUtils.assertTrueCondition(() -> timerRuntime.get() != 0, 1000);

        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, executorFuture.isDone());
        assertEquals(false, timerFuture.isDone());
        assertEquals(startTime + 200, executorRuntime.get());

        try {
            executorFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e) {
            // Expected
        }

        try {
            timerFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e1) {
            // Expected
        }

        assertEquals(false, executorFuture.isCancelled());
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, executorFuture.isDone());
        assertEquals(false, timerFuture.isDone());


        // Wait until both tasks are done by moving the time ahead 50ms
        clock.plusMillis(50);
        TestUtils.assertTrueCondition(executorFuture::isDone, 1000);
        TestUtils.assertTrueCondition(timerFuture::isDone, 1000);

        executorFuture.get(100 + TOLERANCE_MILLIS, TimeUnit.MILLISECONDS);
        timerFuture.get(TOLERANCE_MILLIS, TimeUnit.MILLISECONDS);

        assertEquals(false, executorFuture.isCancelled());
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(true, executorFuture.isDone());
        assertEquals(true, timerFuture.isDone());
    }

    @Test
    public void scheduleFixed() {
        final long startTime = clock.millis();
        final AtomicLong executorCompleteTime = new AtomicLong();
        final AtomicLong timerCompleteTime = new AtomicLong();
        final AtomicLong taskWaitForTime = new AtomicLong();

        final ScheduledFuture<?> executorFuture = executor.scheduleAtFixedRate(() -> {
            while(taskWaitForTime.get() < clock.millis()) {
                sleepPeacefully(10);
            }
            executorCompleteTime.set(clock.millis());
        }, 300, 200, TimeUnit.MILLISECONDS);

        final ScheduledFuture<?> timerFuture = timer.scheduleAtFixedRate(() -> {
            while(taskWaitForTime.get() < clock.millis()) {
                sleepPeacefully(10);
            }
            timerCompleteTime.set(clock.millis());
        }, 300, 200, TimeUnit.MILLISECONDS);

        // Check immediately
        assertEquals(300, timerFuture.getDelay(TimeUnit.MILLISECONDS));

        assertFalse(executorFuture.isCancelled());
        assertFalse(timerFuture.isCancelled());
        assertFalse(executorFuture.isDone());
        assertFalse(timerFuture.isDone());
        assertEquals(0, executorCompleteTime.get());
        assertEquals(0, timerCompleteTime.get());

        //Let them complete once
        clock.plusMillis(300);
        assertEquals(0, timerFuture.getDelay(TimeUnit.MILLISECONDS));

        taskWaitForTime.set(clock.millis());
        TestUtils.assertTrueCondition(() -> executorCompleteTime.get() != 0, 1000);
        TestUtils.assertTrueCondition(() -> timerCompleteTime.get() != 0, 1000);

        assertFalse(executorFuture.isCancelled());
        assertFalse(timerFuture.isCancelled());
        assertFalse(executorFuture.isDone());
        assertFalse(timerFuture.isDone());
        assertEquals(startTime + 300, executorCompleteTime.get());
        assertEquals(startTime + 300, timerCompleteTime.get());

        //Let them complete again to prove they have a fixed rate
        executorCompleteTime.set(0);
        timerCompleteTime.set(0);
        clock.plusMillis(200);
        assertEquals(0, timerFuture.getDelay(TimeUnit.MILLISECONDS));

        taskWaitForTime.set(clock.millis());
        TestUtils.assertTrueCondition(() -> executorCompleteTime.get() != 0, 1000);
        TestUtils.assertTrueCondition(() -> timerCompleteTime.get() != 0, 1000);

        assertFalse(executorFuture.isCancelled());
        assertFalse(timerFuture.isCancelled());
        assertFalse(executorFuture.isDone());
        assertFalse(timerFuture.isDone());
        assertEquals(startTime + 300 + 200, executorCompleteTime.get());
        assertEquals(startTime + 300 + 200, timerCompleteTime.get());

        // Cancel
        executorFuture.cancel(true);
        timerFuture.cancel(true);

        // Check again
        assertEquals(200, timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertTrue(executorFuture.isCancelled());
        assertTrue(timerFuture.isCancelled());
        assertTrue(executorFuture.isDone());
        assertTrue(timerFuture.isDone());
        assertEquals(startTime + 300 + 200, executorCompleteTime.get());
        assertEquals(startTime + 300 + 200, timerCompleteTime.get());
    }

    @Test
    public void scheduleDelay() {
        final long startTime = clock.millis();
        final AtomicLong executorCompleteTime = new AtomicLong();
        final AtomicLong timerCompleteTime = new AtomicLong();
        final AtomicLong taskWaitForTime = new AtomicLong();

        final ScheduledFuture<?> executorFuture = executor.scheduleWithFixedDelay(() -> {
            while(taskWaitForTime.get() < clock.millis()) {
                sleepPeacefully(10);
            }
            executorCompleteTime.set(clock.millis());
        }, 300, 200, TimeUnit.MILLISECONDS);

        final ScheduledFuture<?> timerFuture = timer.scheduleWithFixedDelay(() -> {
            while(taskWaitForTime.get() < clock.millis()) {
                sleepPeacefully(10);
            }
            timerCompleteTime.set(clock.millis());
        }, 300, 200, TimeUnit.MILLISECONDS);

        // Check immediately
        assertEquals(300, timerFuture.getDelay(TimeUnit.MILLISECONDS));

        assertEquals(false, executorFuture.isCancelled());
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, executorFuture.isDone());
        assertEquals(false, timerFuture.isDone());
        assertEquals(0, executorCompleteTime.get());
        assertEquals(0, timerCompleteTime.get());

        //Let them complete once
        clock.plusMillis(300);
        assertEquals(0, timerFuture.getDelay(TimeUnit.MILLISECONDS));

        taskWaitForTime.set(clock.millis());
        TestUtils.assertTrueCondition(() -> executorCompleteTime.get() != 0, 1000);
        TestUtils.assertTrueCondition(() -> timerCompleteTime.get() != 0, 1000);

        assertEquals(false, executorFuture.isCancelled());
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, executorFuture.isDone());
        assertEquals(false, timerFuture.isDone());
        assertEquals(startTime + 300, executorCompleteTime.get());
        assertEquals(startTime + 300, timerCompleteTime.get());

        // Cancel
        executorFuture.cancel(true);
        timerFuture.cancel(true);

        // Check again
        assertEquals(200, timerFuture.getDelay(TimeUnit.MILLISECONDS));
        assertEquals(true, executorFuture.isCancelled());
        assertEquals(true, timerFuture.isCancelled());
        assertEquals(true, executorFuture.isDone());
        assertEquals(true, timerFuture.isDone());
        assertEquals(startTime + 300, executorCompleteTime.get());
        assertEquals(startTime + 300, timerCompleteTime.get());
    }

    @Test
    public void scheduleCallable() throws InterruptedException, ExecutionException, TimeoutException {
        final AtomicLong executorStartTime = new AtomicLong();
        final AtomicLong timerStartTime = new AtomicLong();
        final AtomicLong executorCompleteTime = new AtomicLong();
        final AtomicLong timerCompleteTime = new AtomicLong();
        final AtomicLong taskWaitForTime = new AtomicLong();

        final Callable<String> executorCallable = () -> {
            executorStartTime.set(clock.millis());
            while(taskWaitForTime.get() < clock.millis()) {
                sleepPeacefully(10);
            }
            executorCompleteTime.set(clock.millis());
            return "executor";
        };
        final Callable<String> timerCallable = () -> {
            timerStartTime.set(clock.millis());
            while(taskWaitForTime.get() < clock.millis()) {
                sleepPeacefully(10);
            }
            timerCompleteTime.set(clock.millis());
            return "timer";
        };

        final ScheduledFuture<?> executorFuture = executor.schedule(executorCallable, 200, TimeUnit.MILLISECONDS);
        final ScheduledFuture<?> timerFuture = timer.schedule(timerCallable, 200, TimeUnit.MILLISECONDS);

        assertFalse(executorFuture.isCancelled());
        assertFalse(timerFuture.isCancelled());
        assertEquals(false, executorFuture.isDone());
        assertEquals(false, timerFuture.isDone());

        // Get with timeout halfway
        try {
            executorFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e) {
            // Expected
        }

        clock.plusMillis(200);
        try {
            // Wait for a task that is already running
            timerFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e1) {
            // Expected
        }

        //Ensure they are executing
        assertEquals(0, timerFuture.getDelay(TimeUnit.MILLISECONDS));

        TestUtils.assertTrueCondition(() -> executorStartTime.get() != 0, 1000);
        TestUtils.assertTrueCondition(() -> timerStartTime.get() != 0, 1000);

        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, executorFuture.isDone());
        assertEquals(false, timerFuture.isDone());

        try {
            executorFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e) {
            // Expected
        }

        try {
            // We already waited 100ms for the executor future, so don't wait here.
            timerFuture.get(100, TimeUnit.MILLISECONDS);
            fail("Should have failed with TimeoutException");
        } catch (final TimeoutException e1) {
            // Expected
        }

        assertEquals(false, executorFuture.isCancelled());
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(false, executorFuture.isDone());
        assertEquals(false, timerFuture.isDone());

        // Wait until both tasks are done.
        taskWaitForTime.set(clock.millis());
        TestUtils.assertTrueCondition(() -> executorCompleteTime.get() != 0, 1000);
        TestUtils.assertTrueCondition(() -> timerCompleteTime.get() != 0, 1000);

        assertEquals("executor", executorFuture.get(100 + TOLERANCE_MILLIS, TimeUnit.MILLISECONDS));
        assertEquals("timer", timerFuture.get(1, TimeUnit.MILLISECONDS));

        assertEquals(false, executorFuture.isCancelled());
        assertEquals(false, timerFuture.isCancelled());
        assertEquals(true, executorFuture.isDone());
        assertEquals(true, timerFuture.isDone());
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
        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime7.get() != 0, 1000);

        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime9.get() != 0, 1000);

        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime3.get() != 0, 1000);

        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime1.get() != 0, 1000);

        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime0.get() != 0, 1000);

        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime2.get() != 0, 1000);

        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime5.get() != 0, 1000);

        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime6.get() != 0, 1000);

        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime4.get() != 0, 1000);

        clock.plusMillis(100);
        TestUtils.assertTrueCondition(() -> executorRuntime8.get() != 0, 1000);

        // Verify the run times
        assertEquals(start + 500, executorRuntime0.get());
        assertEquals(start + 400, executorRuntime1.get());
        assertEquals(start + 600, executorRuntime2.get());
        assertEquals(start + 300, executorRuntime3.get());
        assertEquals(start + 900, executorRuntime4.get());
        assertEquals(start + 700, executorRuntime5.get());
        assertEquals(start + 800, executorRuntime6.get());
        assertEquals(start + 100, executorRuntime7.get());
        assertEquals(start + 1000, executorRuntime8.get());
        assertEquals(start + 200, executorRuntime9.get());
    }

    @Test
    public void getWhileScheduled() {

        AtomicBoolean taskDone = new AtomicBoolean();
        final ScheduledFuture<?> future = timer.schedule(() -> {
            /* no op */ }, 500, TimeUnit.MILLISECONDS);

        executor.execute(() -> {
            try {
                future.get();
                taskDone.set(true);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        clock.plusMillis(500);
        TestUtils.assertTrueCondition(taskDone::get, 1000);
    }

    @Test
    public void shutdown() throws InterruptedException {
        final AtomicLong timerStartTime = new AtomicLong();
        final AtomicLong timerCompleteTime = new AtomicLong();
        final AtomicLong taskWaitForTime = new AtomicLong();
        timer.schedule(() -> {
            timerStartTime.set(clock.millis());
            while(taskWaitForTime.get() < clock.millis()) {
                sleepPeacefully(10);
            }
            timerCompleteTime.set(clock.millis());
        }, 100, TimeUnit.MILLISECONDS);

        //Start the task
        clock.plusMillis(100);

        // Don't shut down until the task has started.
        TestUtils.assertTrueCondition(() -> timerStartTime.get() != 0, 1000);

        final long start = clock.millis();
        timer.shutdown();
        taskWaitForTime.set(clock.millis());
        final boolean done = timer.awaitTermination(1, TimeUnit.SECONDS);
        assertEquals(true, done);
        assertEquals(start, clock.millis());
    }

    @Test
    public void getTimedWhileScheduled() {
        final long startTime = clock.millis();
        AtomicBoolean taskDone = new AtomicBoolean();
        final ScheduledFuture<?> future = timer.schedule(() -> {
            /* no op */ }, 500, TimeUnit.MILLISECONDS);

        executor.execute(() -> {
            try {
                future.get(1000, TimeUnit.MILLISECONDS);
                taskDone.set(true);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        });
        clock.plusMillis(500);
        TestUtils.assertTrueCondition(taskDone::get, 1000);
        assertEquals(startTime + 500, clock.millis());
    }

    @Test
    public void cancel() {
        long startTime = clock.millis();
        AtomicLong runTime = new AtomicLong();
        final ScheduledFuture<?> future = timer.scheduleWithFixedDelay(() -> runTime.set(clock.millis()), 1, 1,
                TimeUnit.SECONDS);
        clock.plusMillis(1000);
        TestUtils.assertTrueCondition(() -> runTime.get() != 0, 1000);
        assertEquals(startTime + 1000, runTime.get());
        final boolean cancelled = future.cancel(false);
        assertEquals(true, cancelled);
        clock.plusMillis(1100);
        sleepPeacefully(100);
        assertEquals(startTime + 1000, runTime.get());
    }

    private static void sleepPeacefully(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
