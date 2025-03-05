/*
 * Copyright (c) 2017, Matthew Lohbihler
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
