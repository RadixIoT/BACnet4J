/*
 * Copyright (c) 2017, Matthew Lohbihler
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.serotonin.warp;

import com.serotonin.scheduler.ScheduledExecutorServiceVariablePool;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * TODO add a configurable task execution time that has the tasks complete at an (absolute or randomized?) instant later
 * than the start.
 *
 * @author Matthew
 */
public class WarpScheduledExecutorService implements WarpTaskExecutingScheduledExecutorService, ClockListener {

    protected final WarpClock clock;
    protected final ExecutorService executorService;
    protected final ScheduledExecutorServiceVariablePool delegate;

    protected final List<ScheduleFutureImpl<?>> tasks = new ArrayList<>();
    protected boolean shutdown;

    public WarpScheduledExecutorService(final Clock clock) {
        this(clock, Executors.newCachedThreadPool());
    }

    public WarpScheduledExecutorService(final Clock clock, final ExecutorService executorService) {
        if (clock instanceof WarpClock) {
            this.clock = (WarpClock) clock;
            this.clock.addListener(this);
            this.executorService = executorService;
            this.delegate = null;
        } else {
            this.clock = null;
            this.executorService = null;
            this.delegate = new ScheduledExecutorServiceVariablePool(clock);
        }
    }

    @Override
    public void clockUpdate(final LocalDateTime dateTime) {
        while (true) {
            // Poll for a task.
            final ScheduleFutureImpl<?> task;
            synchronized (tasks) {
                if (tasks.isEmpty()) {
                    break;
                }
                task = tasks.get(0);
                final long waitTime = task.getDelay(TimeUnit.MILLISECONDS);
                if (waitTime > 0) {
                    break;
                }
                // Remove the task
                tasks.remove(0);
            }
            if (!task.isCancelled()) {
                // Execute the task
                task.execute();
            }
        }
    }

    @Override
    public List<ScheduleFutureImpl<?>> getTasks() {
        return tasks;
    }

    @Override
    public void shutdown() {
        if (delegate == null) {
            executorService.shutdown();
            clock.removeListener(this);
            shutdown = true;
        } else {
            delegate.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        if (delegate == null) {
            executorService.shutdownNow();
            clock.removeListener(this);
            shutdown = true;

            final List<Runnable> runnables = new ArrayList<>(tasks.size());
            for (final ScheduleFutureImpl<?> task : tasks) {
                runnables.add(task.getRunnable());
            }
            return runnables;
        }
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        if (delegate == null) {
            return shutdown;
        }
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        if (delegate == null) {
            return shutdown;
        }
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit)
        throws InterruptedException {
        if (delegate == null) {
            return executorService.awaitTermination(timeout, unit);
        }
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        if (delegate == null) {
            return executorService.submit(task);
        }
        return delegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        if (delegate == null) {
            return executorService.submit(task, result);
        }
        return delegate.submit(task, result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        if (delegate == null) {
            return executorService.submit(task);
        }
        return delegate.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
        if (delegate == null) {
            return executorService.invokeAll(tasks);
        }
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
        final long timeout,
        final TimeUnit unit) throws InterruptedException {
        if (delegate == null) {
            return executorService.invokeAll(tasks, timeout, unit);
        }
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
        if (delegate == null) {
            return executorService.invokeAny(tasks);
        }
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout,
        final TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (delegate == null) {
            return executorService.invokeAny(tasks, timeout, unit);
        }
        return delegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        if (delegate == null) {
            executorService.execute(command);
        } else {
            delegate.execute(command);
        }
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay,
        final TimeUnit unit) {
        if (delegate == null) {
            return addTask(new OneTime(this, command, delay, unit));
        }
        return delegate.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
        final TimeUnit unit) {
        if (delegate == null) {
            return addTask(new OneTimeCallable<>(this, callable, delay, unit));
        }
        return delegate.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
        final long period,
        final TimeUnit unit) {
        if (delegate == null) {
            return addTask(new FixedRate(this, command, initialDelay, period, unit));
        }
        return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command,
        final long initialDelay, final long delay,
        final TimeUnit unit) {
        if (delegate == null) {
            return addTask(new FixedDelay(this, command, initialDelay, delay, unit));
        }
        return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }


    @Override
    public <V> ScheduleFutureImpl<V> addTask(final ScheduleFutureImpl<V> task) {
        synchronized (tasks) {
            if (task.getDelay(TimeUnit.MILLISECONDS) <= 0) {
                // Run now
                executorService.submit(task.getRunnable());
            } else {
                int index = Collections.binarySearch(tasks, task);
                if (index < 0) {
                    index = -index - 1;
                }
                tasks.add(index, task);
            }
            return task;
        }
    }

    @Override
    public Clock getClock() {
        return clock;
    }

}
