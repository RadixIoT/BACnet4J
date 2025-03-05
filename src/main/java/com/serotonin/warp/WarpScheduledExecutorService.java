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
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * TODO add a configurable task execution time that has the tasks complete at an (absolute or randomized?) instant later
 * than the start.
 *
 * @author Matthew
 */
public class WarpScheduledExecutorService implements ScheduledExecutorService, ClockListener {

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
            return submitToExecutor(task);
        }
        return delegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        if (delegate == null) {
            return submitToExecutor(task, result);
        }
        return delegate.submit(task, result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        if (delegate == null) {
            return submitToExecutor(task);
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
            executeInExecutor(command);
        } else {
            delegate.execute(command);
        }
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay,
        final TimeUnit unit) {
        if (delegate == null) {
            return addTask(new OneTime(command, delay, unit));
        }
        return delegate.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
        final TimeUnit unit) {
        if (delegate == null) {
            return addTask(new OneTimeCallable<>(callable, delay, unit));
        }
        return delegate.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
        final long period,
        final TimeUnit unit) {
        if (delegate == null) {
            return addTask(new FixedRate(command, initialDelay, period, unit));
        }
        return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command,
        final long initialDelay, final long delay,
        final TimeUnit unit) {
        if (delegate == null) {
            return addTask(new FixedDelay(command, initialDelay, delay, unit));
        }
        return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }


    protected <V> ScheduleFutureImpl<V> addTask(final ScheduleFutureImpl<V> task) {
        synchronized (tasks) {
            if (task.getDelay(TimeUnit.MILLISECONDS) <= 0) {
                // Run now
                submitToExecutor(task.getRunnable());
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

    protected void executeInExecutor(final Runnable command) {
        executorService.execute(command);
    }

    protected Future<?> submitToExecutor(final Runnable runnable) {
        return executorService.submit(runnable);
    }

    protected <V> Future<V> submitToExecutor(final Callable<V> runnable) {
        return executorService.submit(runnable);
    }

    protected <T> Future<T> submitToExecutor(final Runnable task, final T result) {
        return executorService.submit(task, result);
    }

    abstract class ScheduleFutureImpl<V> implements ScheduledFuture<V> {

        private volatile boolean success;
        private volatile V result;
        private volatile Exception exception;
        private volatile boolean cancelled;
        private volatile boolean done;

        public void execute() {
            submitToExecutor(() -> executeImpl());
        }

        abstract void executeImpl();

        abstract Runnable getRunnable();

        @Override
        public int compareTo(final Delayed that) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS),
                that.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            synchronized (this) {
                if (!done) {
                    cancelled = true;
                    notifyAll();
                    done = true;
                    return true;
                }
                return false;
            }
        }

        @Override
        public boolean isCancelled() {
            synchronized (this) {
                return cancelled;
            }
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            try {
                return await(false, 0L);
            } catch (final TimeoutException e) {
                // Should not happen
                throw new RuntimeException(e);
            }
        }

        @Override
        public V get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            return await(true, unit.toMillis(timeout));
        }

        private V await(final boolean timed, final long millis)
            throws InterruptedException, ExecutionException, TimeoutException {
            final long expiry = clock.millis() + millis;

            while (true) {
                synchronized (this) {
                    final long remaining = expiry - clock.millis();
                    if (success) {
                        return result;
                    }
                    if (exception != null) {
                        throw new ExecutionException(exception);
                    }
                    if (isCancelled()) {
                        throw new CancellationException();
                    }

                    if (timed) {
                        if (remaining <= 0) {
                            throw new TimeoutException();
                        }
                        WarpUtils.wait(clock, this, remaining, TimeUnit.MILLISECONDS);
                    } else {
                        wait();
                    }
                }
            }
        }

        @Override
        public boolean isDone() {
            return done;
        }

        protected void success(final V result) {
            synchronized (this) {
                if (!done) {
                    success = true;
                    this.result = result;
                    notifyAll();
                    done = true;
                }
            }
        }

        protected void exception(final Exception exception) {
            synchronized (this) {
                if (!done) {
                    this.exception = exception;
                    notifyAll();
                    done = true;
                }
            }
        }
    }

    class OneTime extends ScheduleFutureImpl<Void> {

        private final Runnable command;
        private final long runtime;

        public OneTime(final Runnable command, final long delay, final TimeUnit unit) {
            this.command = command;
            runtime = clock.millis() + unit.toMillis(delay);
        }

        @Override
        Runnable getRunnable() {
            return command;
        }

        @Override
        void executeImpl() {
            command.run();
            success(null);
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final long millis = runtime - clock.millis();
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        }
    }

    abstract class Repeating extends ScheduleFutureImpl<Void> {

        private final Runnable command;
        protected final TimeUnit unit;

        protected long nextRuntime;

        public Repeating(final Runnable command, final long initialDelay, final TimeUnit unit) {
            this.command = () -> {
                command.run();
                if (!isCancelled()) {
                    // Reschedule to run at the period from the last run.
                    updateNextRuntime();
                    addTask(this);
                }
            };
            nextRuntime = clock.millis() + unit.toMillis(initialDelay);
            this.unit = unit;
        }

        @Override
        Runnable getRunnable() {
            return command;
        }

        @Override
        void executeImpl() {
            command.run();
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final long millis = nextRuntime - clock.millis();
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        }

        @Override
        public boolean isDone() {
            return isCancelled();
        }

        abstract void updateNextRuntime();
    }

    class FixedRate extends Repeating {

        private final long period;

        public FixedRate(final Runnable command, final long initialDelay, final long period,
            final TimeUnit unit) {
            super(command, initialDelay, unit);
            this.period = period;
        }

        @Override
        void updateNextRuntime() {
            nextRuntime += unit.toMillis(period);
        }
    }

    class FixedDelay extends Repeating {

        private final long delay;

        public FixedDelay(final Runnable command, final long initialDelay, final long delay,
            final TimeUnit unit) {
            super(command, initialDelay, unit);
            this.delay = delay;
        }

        @Override
        void updateNextRuntime() {
            nextRuntime = clock.millis() + unit.toMillis(delay);
        }
    }

    class OneTimeCallable<V> extends ScheduleFutureImpl<V> {

        private final Callable<V> command;
        private final long runtime;

        public OneTimeCallable(final Callable<V> command, final long delay, final TimeUnit unit) {
            this.command = command;
            runtime = clock.millis() + unit.toMillis(delay);
        }

        @Override
        Runnable getRunnable() {
            return () -> {
                try {
                    command.call();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }

        @Override
        void executeImpl() {
            try {
                success(command.call());
            } catch (final Exception e) {
                exception(e);
            }
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final long millis = runtime - clock.millis();
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        }
    }
}
