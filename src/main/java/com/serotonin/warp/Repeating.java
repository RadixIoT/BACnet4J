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

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * @author Matthew Lohbihler
 */
abstract class Repeating extends ScheduleFutureImpl<Void> {

    private final Runnable command;
    protected final TimeUnit unit;

    protected long nextRuntime;

    protected Repeating(WarpTaskExecutingScheduledExecutorService executorService, final Runnable command, final long initialDelay, final TimeUnit unit) {
        super(executorService);
        this.command = () -> {
            command.run();
            if (!isCancelled()) {
                // Reschedule to run at the period from the last run.
                updateNextRuntime();
                executorService.addTask(this);
            }
        };
        nextRuntime = executorService.getClock().millis() + unit.toMillis(initialDelay);
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
        return getDelay(unit, executorService.getClock().instant());
    }

    @Override
    public long getDelay(TimeUnit unit, Instant from) {
        final long millis = nextRuntime - from.toEpochMilli();
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isDone() {
        return isCancelled();
    }

    abstract void updateNextRuntime();
}
