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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author Matthew Lohbihler
 */
class OneTimeCallable<V> extends ScheduleFutureImpl<V> {

    private final Callable<V> command;
    private final long runtime;

    public OneTimeCallable(WarpTaskExecutingScheduledExecutorService executorService, final Callable<V> command, final long delay, final TimeUnit unit) {
        super(executorService);
        this.command = command;
        runtime = executorService.getClock().millis() + unit.toMillis(delay);
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
        final long millis = runtime - executorService.getClock().millis();
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }
}
