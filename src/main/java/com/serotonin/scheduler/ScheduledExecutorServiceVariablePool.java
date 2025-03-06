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
        this.state = State.running;
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
            while (state == State.running) {
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
            state = State.stopped;
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
            state = State.stopping;
            tasks.notify();
        }
    }

    @Override
    public boolean isShutdown() {
        return state != State.running && executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return state == State.stopped && executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        final long start = Clock.systemUTC().millis();

        final long millis = unit.toMillis(timeout);
        scheduler.join(millis);
        if (state != State.stopped)
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
        running, stopping, stopped;
    }

}
