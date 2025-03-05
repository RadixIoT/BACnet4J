/*
 * Copyright (c) 2017, Matthew Lohbihler
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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

public class WarpClock extends Clock {

    private final ZoneId zoneId;
    private LocalDateTime dateTime;
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
        this.dateTime = dateTime;

    }

    public TimeoutFuture<?> setTimeout(final Runnable command, final long timeout,
        final TimeUnit timeUnit) {
        return setTimeout(() -> {
            command.run();
            return null;
        }, timeout, timeUnit);
    }

    public <V> TimeoutFuture<V> setTimeout(final Callable<V> callable, final long timeout,
        final TimeUnit timeUnit) {
        final LocalDateTime deadline = dateTime.plusNanos(timeUnit.toNanos(timeout));
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
            notifyAll();
            done = true;
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
        return fireUpdate(dateTime.plus(amountToAdd));
    }

    public LocalDateTime plus(final long amountToAdd, final TemporalUnit unit) {
        return fireUpdate(dateTime.plus(amountToAdd, unit));
    }

    public LocalDateTime plusYears(final long years) {
        return fireUpdate(dateTime.plusYears(years));
    }

    public LocalDateTime plusMonths(final long months) {
        return fireUpdate(dateTime.plusMonths(months));
    }

    public LocalDateTime plusWeeks(final long weeks) {
        return fireUpdate(dateTime.plusWeeks(weeks));
    }

    public LocalDateTime plusDays(final long days) {
        return fireUpdate(dateTime.plusDays(days));
    }

    public LocalDateTime plusHours(final long hours) {
        return fireUpdate(dateTime.plusHours(hours));
    }

    public LocalDateTime plusMinutes(final long minutes) {
        return fireUpdate(dateTime.plusMinutes(minutes));
    }

    public LocalDateTime plusSeconds(final long seconds) {
        return fireUpdate(dateTime.plusSeconds(seconds));
    }

    public LocalDateTime plusMillis(final long millis) {
        return fireUpdate(dateTime.plusNanos(millis * 1_000_000L));
    }

    public LocalDateTime plusNanos(final long nanos) {
        return fireUpdate(dateTime.plusNanos(nanos));
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
        dateTime = newDateTime;
        for (final ClockListener l : listeners) {
            l.clockUpdate(newDateTime);
        }
        return dateTime;
    }

    public int get(final TemporalField field) {
        return dateTime.get(field);
    }

    public long getLong(final TemporalField field) {
        return dateTime.getLong(field);
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(final ZoneId zone) {
        return new WarpClock(zoneId, dateTime);
    }

    @Override
    public Instant instant() {
        return dateTime.atZone(zoneId).toInstant();
    }
}
