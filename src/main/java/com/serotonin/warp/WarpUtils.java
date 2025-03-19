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
package com.serotonin.warp;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for warping time
 */
public class WarpUtils {

    public static void wait(final Clock clock, final Object o, final long timeout, final TimeUnit timeUnit)
            throws InterruptedException {
        if (clock instanceof WarpClock) {
            final WarpClock warpClock = (WarpClock) clock;

            final TimeoutFuture<?> future = warpClock.setTimeout(() -> {
                synchronized (o) {
                    o.notify();
                }
            }, timeout, timeUnit);

            try {
                synchronized (o) {
                    o.wait();
                }
            } finally {
                future.cancel();
            }
        } else {
            o.wait(timeUnit.toMillis(timeout));
        }
    }

    public static void sleep(final Clock clock, final long timeout, final TimeUnit timeUnit)
            throws InterruptedException {
        if (clock instanceof WarpClock) {
            final WarpClock warpClock = (WarpClock) clock;
            final Object o = new Object();

            final TimeoutFuture<?> future = warpClock.setTimeout(() -> {
                synchronized (o) {
                    o.notify();
                }
            }, timeout, timeUnit);

            try {
                synchronized (o) {
                    o.wait();
                }
            } finally {
                future.cancel();
            }
        } else {
            Thread.sleep(timeUnit.toMillis(timeout));
        }
    }
}
