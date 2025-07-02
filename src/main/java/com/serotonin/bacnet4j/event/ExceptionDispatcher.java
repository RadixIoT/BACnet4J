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

package com.serotonin.bacnet4j.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ExceptionDispatcher {
    private final List<ExceptionListener> listeners = new CopyOnWriteArrayList<>();
    private final ExceptionListener defaultExceptionListener = new DefaultExceptionListener();

    public ExceptionDispatcher() {
        listeners.add(defaultExceptionListener);
    }

    public void addListener(final ExceptionListener l) {
        listeners.add(l);
    }

    public void removeListener(final ExceptionListener l) {
        listeners.remove(l);
    }

    public void removeDefaultExceptionListener() {
        listeners.remove(defaultExceptionListener);
    }

    public void fireUnimplementedVendorService(final UnsignedInteger vendorId, final UnsignedInteger serviceNumber,
            final ByteQueue queue) {
        for (final ExceptionListener l : listeners)
            l.unimplementedVendorService(vendorId, serviceNumber, queue);
    }

    public void fireReceivedException(final Exception e) {
        for (final ExceptionListener l : listeners)
            l.receivedException(e);
    }

    public void fireReceivedThrowable(final Throwable t) {
        for (final ExceptionListener l : listeners)
            l.receivedThrowable(t);
    }
}
