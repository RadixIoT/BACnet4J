/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Radix IoT LLC,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.radixiot.com for commercial license options.
 */

package com.serotonin.bacnet4j.npdu.sc;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An executor that runs tasks strictly one at a time, in submission order, on a delegate executor. Used to
 * serialize the events of the BACnet/SC state machines (node, hub connector, connections): the delegate — the
 * local device's executor — is a thread pool, which guarantees neither ordering nor mutual exclusion for tasks
 * submitted to it directly.
 * <p>
 * A task submitted from within a running task is appended to the queue (breadth-first), it does not run
 * inline. A task that throws does not prevent subsequent tasks from running.
 */
public class SerialExecutor implements Executor {
    static final Logger LOG = LoggerFactory.getLogger(SerialExecutor.class);

    private final Executor delegate;
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private Runnable active;
    private boolean stopped;

    public SerialExecutor(Executor delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized void execute(Runnable task) {
        if (stopped) {
            LOG.debug("Dropping task submitted after stop");
            return;
        }
        tasks.add(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LOG.error("Error in serially executed task", e);
            } finally {
                runNext();
            }
        });
        if (active == null) {
            runNext();
        }
    }

    /**
     * Permanently stops this executor: queued tasks are discarded and subsequent submissions are dropped.
     * A task already running is not interrupted; it completes normally. Used by hard termination, where
     * queued events are stale by definition and must not act on the forcibly reset state machines.
     */
    public synchronized void stop() {
        stopped = true;
        tasks.clear();
    }

    private synchronized void runNext() {
        if (stopped) {
            active = null;
            return;
        }
        active = tasks.poll();
        if (active != null) {
            try {
                delegate.execute(active);
            } catch (RejectedExecutionException e) {
                // The delegate has been shut down. Stop rather than wedge: leaving `active` pointing at a
                // task that will never run would silently swallow every subsequent submission.
                LOG.debug("Delegate rejected task; stopping serial executor", e);
                stop();
                active = null;
            }
        }
    }
}
