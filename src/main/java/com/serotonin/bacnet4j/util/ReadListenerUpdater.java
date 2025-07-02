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

package com.serotonin.bacnet4j.util;

import java.util.concurrent.atomic.AtomicInteger;

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class ReadListenerUpdater {
    private final ReadListener callback;
    private final PropertyValues propertyValues;
    private final int max;
    private final AtomicInteger current = new AtomicInteger(0);
    private boolean cancelled;

    public ReadListenerUpdater(final ReadListener callback, final PropertyValues propertyValues, final int max) {
        this.callback = callback;
        this.propertyValues = propertyValues;
        this.max = max;
    }

    public void increment(final int deviceId, final ObjectIdentifier oid, final PropertyIdentifier pid,
            final UnsignedInteger pin, final Encodable value) {
        final int cur = current.incrementAndGet();
        if (callback != null)
            cancelled = callback.progress((double) cur / max, deviceId, oid, pid, pin, value);
        propertyValues.add(oid, pid, pin, value);
    }

    public boolean cancelled() {
        return cancelled;
    }
}
