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
