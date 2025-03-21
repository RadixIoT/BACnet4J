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
 * See www.radixiot.com for commercial license options.
 *
 * @author Matthew Lohbihler
 */
package com.serotonin.warp;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Clock that can control time
 * @author Matthew Lohbihler
 */
public class WarpClock extends Clock {

    private final ZoneId zoneId;
    private AtomicReference<LocalDateTime> dateTime;
    private final List<ClockListener> listeners = new CopyOnWriteArrayList<>();

    public WarpClock() {
        this(ZoneId.systemDefault());
    }

    public WarpClock(final ZoneId zoneId) {
        this(zoneId, LocalDateTime.now(Clock.system(zoneId)));
    }

    /**
     * @param zoneId
     * @param dateTime
     */
    public WarpClock(final ZoneId zoneId, final LocalDateTime dateTime) {
        Objects.requireNonNull(zoneId, "zoneId");
        Objects.requireNonNull(dateTime, "dateTime");
        this.zoneId = zoneId;
        this.dateTime = new AtomicReference<>(dateTime);

    }

    public <V> TimeoutFuture<V> setTimeout(final Runnable command, final long timeout,
        final TimeUnit timeUnit) {
        return setTimeout(() -> {
            command.run();
            return null;
        }, timeout, timeUnit);
    }

    public <V> TimeoutFuture<V> setTimeout(final Callable<V> callable, final long timeout,
        final TimeUnit timeUnit) {
        final LocalDateTime deadline = dateTime.get().plusNanos(timeUnit.toNanos(timeout));
        final TimeoutFutureImpl<V> future = new TimeoutFutureImpl<>();
        final ClockListener listener = new ClockListener() {
            @Override
            public void clockUpdate(final LocalDateTime dateTime) {
                if (!dateTime.isBefore(deadline)) {
                    if (!future.isCancelled()) {
                        try {
                            future.setResult(callable.call());
                        } catch (final Exception e) {
                            future.setException(e);
                        }
                    }
                    listeners.remove(this);
                }
            }
        };
        listeners.add(listener);
        return future;
    }

    class TimeoutFutureImpl<V> implements TimeoutFuture<V> {

        private boolean success;
        private boolean cancelled;
        private Exception ex;
        private V result;
        private volatile boolean done;

        @Override
        public V get() throws CancellationException, InterruptedException, Exception {
            if (success) {
                return result;
            }
            if (ex != null) {
                throw ex;
            }
            if (cancelled) {
                throw new CancellationException();
            }

            synchronized (this) {
                wait();
            }

            if (success) {
                return result;
            }
            if (ex != null) {
                throw ex;
            }
            throw new CancellationException();
        }

        public synchronized boolean isCancelled() {
            return cancelled;
        }

        @Override
        public synchronized boolean cancel() {
            if (!done) {
                cancelled = true;
                done();
                return true;
            }
            return false;
        }

        synchronized void setResult(final V result) {
            this.result = result;
            success = true;
            done();
        }

        synchronized void setException(final Exception ex) {
            this.ex = ex;
            done();
        }

        void done() {
            synchronized (this) {
                notifyAll();
                done = true;
            }
        }
    }

    public void addListener(final ClockListener listener) {
        listeners.add(listener);
    }

    public void removeListener(final ClockListener listener) {
        listeners.remove(listener);
    }

    public LocalDateTime set(final int year, final Month month, final int dayOfMonth,
        final int hour,
        final int minute) {
        return fireUpdate(LocalDateTime.of(year, month, dayOfMonth, hour, minute));
    }

    public LocalDateTime set(final int year, final Month month, final int dayOfMonth,
        final int hour, final int minute,
        final int second) {
        return fireUpdate(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second));
    }

    public LocalDateTime set(final int year, final Month month, final int dayOfMonth,
        final int hour, final int minute,
        final int second, final int nanoOfSecond) {
        return fireUpdate(
            LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond));
    }

    public LocalDateTime set(final int year, final int month, final int dayOfMonth, final int hour,
        final int minute) {
        return fireUpdate(LocalDateTime.of(year, month, dayOfMonth, hour, minute));
    }

    public LocalDateTime set(final int year, final int month, final int dayOfMonth, final int hour,
        final int minute,
        final int second) {
        return fireUpdate(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second));
    }

    public LocalDateTime set(final int year, final int month, final int dayOfMonth, final int hour,
        final int minute,
        final int second, final int nanoOfSecond) {
        return fireUpdate(
            LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond));
    }

    public LocalDateTime plus(final TemporalAmount amountToAdd) {
        return fireUpdate(dateTime.get().plus(amountToAdd));
    }

    public LocalDateTime plus(final long amountToAdd, final TemporalUnit unit) {
        return fireUpdate(dateTime.get().plus(amountToAdd, unit));
    }

    public LocalDateTime plusYears(final long years) {
        return fireUpdate(dateTime.get().plusYears(years));
    }

    public LocalDateTime plusMonths(final long months) {
        return fireUpdate(dateTime.get().plusMonths(months));
    }

    public LocalDateTime plusWeeks(final long weeks) {
        return fireUpdate(dateTime.get().plusWeeks(weeks));
    }

    public LocalDateTime plusDays(final long days) {
        return fireUpdate(dateTime.get().plusDays(days));
    }

    public LocalDateTime plusHours(final long hours) {
        return fireUpdate(dateTime.get().plusHours(hours));
    }

    public LocalDateTime plusMinutes(final long minutes) {
        return fireUpdate(dateTime.get().plusMinutes(minutes));
    }

    public LocalDateTime plusSeconds(final long seconds) {
        return fireUpdate(dateTime.get().plusSeconds(seconds));
    }

    public LocalDateTime plusMillis(final long millis) {
        return fireUpdate(dateTime.get().plusNanos(millis * 1_000_000L));
    }

    public LocalDateTime plusNanos(final long nanos) {
        return fireUpdate(dateTime.get().plusNanos(nanos));
    }

    public LocalDateTime plus(final int amount, final TimeUnit unit, final long endSleep) {
        return plus(amount, unit, 0, null, 0, endSleep);
    }

    public LocalDateTime plus(final int amount, final TimeUnit unit, final int byAmount,
        final TimeUnit byUnit,
        final long eachSleep, final long endSleep) {
        long remainder = unit.toNanos(amount);
        final long each = (byUnit == null ? unit : byUnit).toNanos(
            byAmount == 0 ? amount : byAmount);

        LocalDateTime result = null;
        try {
            if (remainder <= 0) {
                result = plusNanos(0);
                Thread.sleep(eachSleep);
            } else {
                while (remainder > 0) {
                    long nanos = each;
                    if (each > remainder) {
                        nanos = remainder;
                    }
                    result = plusNanos(nanos);
                    remainder -= nanos;
                    Thread.sleep(eachSleep);
                }
            }

            Thread.sleep(endSleep);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private LocalDateTime fireUpdate(final LocalDateTime newDateTime) {
        dateTime.set(newDateTime);
        for (final ClockListener l : listeners) {
            l.clockUpdate(newDateTime);
        }
        return dateTime.get();
    }

    public int get(final TemporalField field) {
        return dateTime.get().get(field);
    }

    public long getLong(final TemporalField field) {
        return dateTime.get().getLong(field);
    }

    public LocalDateTime getDateTime() {
        return dateTime.get();
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(final ZoneId zone) {
        return new WarpClock(zoneId, dateTime.get());
    }

    @Override
    public Instant instant() {
        return dateTime.get().atZone(zoneId).toInstant();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WarpClock warpClock = (WarpClock) o;
        return Objects.equals(zoneId, warpClock.zoneId) && Objects.equals(dateTime, warpClock.dateTime) && Objects.equals(listeners, warpClock.listeners);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zoneId, dateTime, listeners);
    }
}
