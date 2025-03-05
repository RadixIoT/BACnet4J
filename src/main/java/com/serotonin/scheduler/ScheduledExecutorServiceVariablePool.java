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
public class ScheduledExecutorServiceVariablePool implements ScheduledExecutorService, Runnable {
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
        scheduler = new Thread(this, "ScheduledExecutorServiceVariablePool");
        state = State.running;
        scheduler.start();
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return addTask(new OneTime(command, delay, unit));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
                                                  final TimeUnit unit) {
        return addTask(new FixedRate(command, initialDelay, period, unit));
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
                                                     final TimeUnit unit) {
        return addTask(new FixedDelay(command, initialDelay, delay, unit));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return addTask(new OneTimeCallable<>(callable, delay, unit));
    }

    private <V> ScheduleFutureImpl<V> addTask(final ScheduleFutureImpl<V> task) {
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

    abstract class ScheduleFutureImpl<V> implements ScheduledFuture<V> {
        protected volatile Future<V> future;
        private volatile boolean cancelled;

        abstract void execute();

        void setFuture(final Future<V> future) {
            synchronized (this) {
                this.future = future;
                notifyAll();
            }
        }

        void clearFuture() {
            future = null;
        }

        @Override
        public int compareTo(final Delayed that) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), that.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            synchronized (this) {
                if (future != null)
                    return future.cancel(mayInterruptIfRunning);

                cancelled = true;
                notifyAll();
                return true;
            }
        }

        @Override
        public boolean isCancelled() {
            synchronized (this) {
                if (future != null)
                    return future.isCancelled();
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
                    if (future != null) {
                        if (timed)
                            return future.get(remaining, TimeUnit.MILLISECONDS);
                        return future.get();
                    }
                    if (isCancelled())
                        throw new CancellationException();

                    if (timed) {
                        if (remaining <= 0)
                            throw new TimeoutException();
                        wait(remaining);
                    } else {
                        wait();
                    }
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

        @SuppressWarnings("unchecked")
        @Override
        void execute() {
            synchronized (this) {
                setFuture((Future<Void>) executorService.submit(command));
            }
        }

        @Override
        public boolean isDone() {
            synchronized (this) {
                if (future != null)
                    return future.isDone();
                return isCancelled();
            }
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final long millis = runtime - clock.millis();
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        }
    }

    abstract class Repeating extends ScheduleFutureImpl<Void> {
        protected final TimeUnit unit;
        private final Runnable command;
        protected long nextRuntime;

        public Repeating(final Runnable command, final long initialDelay, final TimeUnit unit) {
            this.command = () -> {
                command.run();
                synchronized (this) {
                    if (!isCancelled()) {
                        // Reschedule to run at the period from the last run.
                        updateNextRuntime();
                        clearFuture();
                        addTask(this);
                    }
                }
            };
            nextRuntime = clock.millis() + unit.toMillis(initialDelay);
            this.unit = unit;
        }

        @SuppressWarnings("unchecked")
        @Override
        void execute() {
            synchronized (this) {
                setFuture((Future<Void>) executorService.submit(command));
            }
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

        public FixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
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

        public FixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
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
        void execute() {
            setFuture(executorService.submit(command));
        }

        @Override
        public boolean isDone() {
            synchronized (this) {
                if (future != null)
                    return future.isDone();
                return isCancelled();
            }
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final long millis = runtime - clock.millis();
            return unit.convert(millis, TimeUnit.MILLISECONDS);
        }
    }
}
