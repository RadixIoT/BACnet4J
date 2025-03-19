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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
abstract class Repeating extends ScheduleFutureImpl<Void> {
    protected final ExecutorService executorService;
    protected final TimeUnit unit;
    private final Runnable command;
    protected long nextRuntime;

    public Repeating(TaskExecutingScheduledExecutorService scheduledExecutorService, ExecutorService executorService, final Runnable command, final long initialDelay, final TimeUnit unit) {
        super(scheduledExecutorService);
        this.command = () -> {
            command.run();
            synchronized (this) {
                if (!isCancelled()) {
                    // Reschedule to run at the period from the last run.
                    updateNextRuntime();
                    clearFuture();
                    scheduledExecutorService.addTask(this);
                }
            }
        };
        this.executorService = executorService;
        nextRuntime = scheduledExecutorService.getClock().millis() + unit.toMillis(initialDelay);
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
        final long millis = nextRuntime - scheduledExecutorService.getClock().millis();
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isDone() {
        return isCancelled();
    }

    abstract void updateNextRuntime();
}
