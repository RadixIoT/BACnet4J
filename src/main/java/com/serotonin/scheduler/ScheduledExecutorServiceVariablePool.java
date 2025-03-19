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
package com.serotonin.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * A scheduled executor service backed by variable executor service so that it
 * has a variable thread pool size. This is useful when tasks that are
 * submitted may perform blocking operations. It is meant as a simple
 * replacement for the java.util.concurrent.ScheduledThreadPoolExecutor.
 * <p>
 * This class is a mash-up of java.util.Timer and
 * java.util.concurrent.ThreadPoolExecutor. Tasks are stored for execution by a
 * timer thread, and when it is time to execute they are submitted to the
 * executor.
 *
 * @author Matthew Lohbihler
 */
public class ScheduledExecutorServiceVariablePool implements TaskExecutingScheduledExecutorService, Runnable {

    static final Logger LOG = LoggerFactory.getLogger(ScheduledExecutorServiceVariablePool.class);
    private final Clock clock;
    private final ExecutorService executorService;
    private final Thread scheduler;
    private final List<ScheduleFutureImpl<?>> tasks = new LinkedList<>();
    private volatile State state;
    public ScheduledExecutorServiceVariablePool() {
        this(Clock.systemUTC());
    }

    public ScheduledExecutorServiceVariablePool(final Clock clock) {
        this.clock = clock;
        this.scheduler = new Thread(this, "ScheduledExecutorServiceVariablePool");
        this.state = State.RUNNING;
        this.scheduler.start();
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return addTask(new OneTime(this, executorService, command, delay, unit));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
                                                  final TimeUnit unit) {
        return addTask(new FixedRate(this, executorService, command, initialDelay, period, unit));
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
                                                     final TimeUnit unit) {
        return addTask(new FixedDelay(this, executorService, command, initialDelay, delay, unit));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return addTask(new OneTimeCallable<>(this, executorService, callable, delay, unit));
    }

    @Override
    public List<ScheduleFutureImpl<?>> getTasks() {
        synchronized (tasks) {
            return tasks.stream().collect(Collectors.toUnmodifiableList());
        }
    }

    public <V> ScheduleFutureImpl<V> addTask(final ScheduleFutureImpl<V> task) {
        synchronized (tasks) {
            int index = Collections.binarySearch(tasks, task);
            if (index < 0)
                index = -index - 1;
            tasks.add(index, task);
            tasks.notify();
        }
        return task;
    }

    @Override
    public Clock getClock() {
        return clock;
    }

    @Override
    public void run() {
        try {
            while (state == State.RUNNING) {
                synchronized (tasks) {
                    long waitTime;
                    ScheduleFutureImpl<?> task = null;

                    // Poll for a task.
                    if (tasks.isEmpty())
                        // No tasks are scheduled. We could wait indefinitely here since we'll be notified
                        // of a change, but out of paranoia we'll only wait one second. When there are no
                        // tasks, this introduces nominal overhead.
                        waitTime = 1000;
                    else {
                        task = tasks.get(0);
                        waitTime = task.getDelay(TimeUnit.MILLISECONDS);
                        if (waitTime <= 0) {
                            // Remove the task
                            tasks.remove(0);
                            if (!task.isCancelled()) {
                                // Execute the task
                                task.execute();
                            }
                        }
                    }

                    if (waitTime > 0) {
                        try {
                            tasks.wait(waitTime);
                        } catch (final InterruptedException e) {
                            LOG.warn("Interrupted", e);
                        }
                    }
                }
            }
        } finally {
            state = State.STOPPED;
        }
    }

    @Override
    public void shutdown() {
        shutdownScheduler();
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdownScheduler();
        return executorService.shutdownNow();
    }

    private void shutdownScheduler() {
        synchronized (tasks) {
            state = State.STOPPING;
            tasks.notify();
        }
    }

    @Override
    public boolean isShutdown() {
        return state != State.RUNNING && executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return state == State.STOPPED && executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        final long start = Clock.systemUTC().millis();

        final long millis = unit.toMillis(timeout);
        scheduler.join(millis);
        if (state != State.STOPPED)
            return false;

        final long remaining = millis - (Clock.systemUTC().millis() - start);
        if (remaining <= 0)
            return false;

        return executorService.awaitTermination(remaining, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return executorService.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return executorService.submit(task, result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return executorService.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout,
                                         final TimeUnit unit) throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        executorService.execute(command);
    }

    private static enum State {
        RUNNING, STOPPING, STOPPED;
    }

}
