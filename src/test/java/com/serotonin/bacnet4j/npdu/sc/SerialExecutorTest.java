/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.npdu.sc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class SerialExecutorTest {
    /**
     * Tasks run in submission order, and a task submitted from within a running task is appended to the
     * queue (breadth-first) rather than run inline.
     */
    @Test
    public void submissionOrderAndBreadthFirstNesting() {
        var executor = new SerialExecutor(Runnable::run);
        List<String> order = new ArrayList<>();

        executor.execute(() -> {
            order.add("a");
            // Nested submissions must run after everything already queued, not inline.
            executor.execute(() -> order.add("a.nested"));
            order.add("a.end");
        });
        executor.execute(() -> order.add("b"));

        // With an inline delegate everything has drained by now. The nested task ran after "a" completed,
        // but before "b", because "b" was submitted after the drain had already begun processing.
        assertEquals(List.of("a", "a.end", "a.nested", "b"), order);
    }

    /**
     * A task that throws does not prevent subsequent tasks from running.
     */
    @Test
    public void taskExceptionDoesNotStopTheQueue() {
        var executor = new SerialExecutor(Runnable::run);
        List<String> order = new ArrayList<>();

        executor.execute(() -> {
            throw new RuntimeException("task failure");
        });
        executor.execute(() -> order.add("after"));

        assertEquals(List.of("after"), order);
    }

    /**
     * stop() discards queued tasks and drops subsequent submissions. The task that calls stop() (standing
     * in for a running task) completes normally.
     */
    @Test
    public void stopDiscardsQueuedAndDropsSubsequentTasks() {
        // A delegate that holds tasks so the queue can build up behind a "running" task.
        List<Runnable> held = new ArrayList<>();
        var executor = new SerialExecutor(held::add);
        List<String> order = new ArrayList<>();

        executor.execute(() -> {
            order.add("running");
            executor.stop();
        });
        executor.execute(() -> order.add("queued")); // Queued behind "running"; discarded by stop.

        // Release the first task.
        held.remove(0).run();
        // Submissions after stop are dropped.
        executor.execute(() -> order.add("late"));

        assertEquals(List.of("running"), order);
        assertTrue("no further task was handed to the delegate", held.isEmpty());
    }

    /**
     * A delegate that rejects (e.g. the local device's executor after shutdown) must not wedge the
     * queue or propagate the rejection to the submitter; the executor stops itself.
     */
    @Test
    public void delegateRejectionStopsWithoutWedgingOrThrowing() {
        AtomicInteger accepted = new AtomicInteger();
        var executor = new SerialExecutor(task -> {
            if (accepted.incrementAndGet() > 1) {
                throw new java.util.concurrent.RejectedExecutionException("shut down");
            }
            task.run();
        });
        List<String> order = new ArrayList<>();

        executor.execute(() -> order.add("first"));
        // The delegate rejects the second task; the executor stops itself and the submitter sees no exception.
        executor.execute(() -> order.add("second"));
        executor.execute(() -> order.add("third"));

        assertEquals(List.of("first"), order);
    }

    /**
     * With a multi-threaded delegate — the production case, where the local device's executor is a cached
     * thread pool — tasks still run one at a time and in submission order.
     */
    @Test
    public void oneAtATimeAndOrderedOnAThreadPool() throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();
        try {
            var executor = new SerialExecutor(pool);
            int taskCount = 500;
            List<Integer> order = new ArrayList<>();
            AtomicInteger running = new AtomicInteger();
            AtomicInteger maxConcurrency = new AtomicInteger();
            CountDownLatch done = new CountDownLatch(taskCount);

            for (int i = 0; i < taskCount; i++) {
                int index = i;
                executor.execute(() -> {
                    int concurrent = running.incrementAndGet();
                    maxConcurrency.accumulateAndGet(concurrent, Math::max);
                    order.add(index);
                    running.decrementAndGet();
                    done.countDown();
                });
            }

            assertTrue("tasks did not complete in time", done.await(10, TimeUnit.SECONDS));
            assertEquals(1, maxConcurrency.get());
            List<Integer> expected = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                expected.add(i);
            }
            assertEquals(expected, order);
        } finally {
            pool.shutdownNow();
        }
    }
}
