package com.serotonin.scheduler;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Terry Packer
 */
abstract class ScheduleFutureImpl<V> implements ScheduledFuture<V> {
    protected final TaskExecutingScheduledExecutorService scheduledExecutorService;
    protected volatile Future<V> future;
    private volatile boolean cancelled;

    public ScheduleFutureImpl(TaskExecutingScheduledExecutorService scheduledExecutorServiceVariablePool) {
        this.scheduledExecutorService = scheduledExecutorServiceVariablePool;
    }

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
        final long expiry = scheduledExecutorService.getClock().millis() + millis;

        while (true) {
            synchronized (this) {
                final long remaining = expiry - scheduledExecutorService.getClock().millis();
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
