/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.util.sero;

public class ThreadUtils {
    public static void sleep(long millis) {
        if (millis <= 0)
            return;

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void wait(Object monitor) {
        try {
            monitor.wait();
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void wait(Object monitor, long timeout) {
        try {
            monitor.wait(timeout);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void wait(Object monitor, long timeout, int nanos) {
        try {
            monitor.wait(timeout, nanos);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void waitSync(Object monitor) {
        synchronized (monitor) {
            try {
                monitor.wait();
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }
    }

    public static void waitSync(Object monitor, long timeout) {
        synchronized (monitor) {
            try {
                monitor.wait(timeout);
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }
    }

    public static void waitSync(Object monitor, long timeout, int nanos) {
        synchronized (monitor) {
            try {
                monitor.wait(timeout, nanos);
            } catch (InterruptedException e) {
                throw new UncheckedInterruptedException(e);
            }
        }
    }

    public static void notifySync(Object monitor) {
        synchronized (monitor) {
            monitor.notify();
        }
    }

    public static void notifyAllSync(Object monitor) {
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    public static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void join(Thread thread, long timeout) {
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static void join(Thread thread, long timeout, int nanos) {
        try {
            thread.join(timeout, nanos);
        } catch (InterruptedException e) {
            throw new UncheckedInterruptedException(e);
        }
    }

    public static class UncheckedInterruptedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public UncheckedInterruptedException(Throwable cause) {
            super(cause);
        }
    }
}
