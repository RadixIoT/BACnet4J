package com.serotonin.warp;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Terry Packer
 */
abstract class ScheduleFutureImpl<V> implements ScheduledFuture<V> {

    protected final WarpTaskExecutingScheduledExecutorService executorService;
    private volatile boolean success;
    private volatile V result;
    private volatile Exception exception;
    private volatile boolean cancelled;
    private volatile boolean done;

    public ScheduleFutureImpl(WarpTaskExecutingScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public void execute() {
        executorService.submit(() -> executeImpl());
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
        final long expiry = executorService.getClock().millis() + millis;

        while (true) {
            synchronized (this) {
                final long remaining = expiry - executorService.getClock().millis();
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
                    WarpUtils.wait(executorService.getClock(), this, remaining, TimeUnit.MILLISECONDS);
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
