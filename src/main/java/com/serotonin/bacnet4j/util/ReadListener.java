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

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

@FunctionalInterface
public interface ReadListener {
    /**
     * Provides the current progress from 0 (which is a value that is never actually sent to the method) up to 1
     * (finished and will not called again) as well as an opportunity for the client to cancel the request. Other
     * parameters represent the property that was just received.
     *
     * @param progress the current progress amount
     * @param deviceId the id of the device from which the property was received
     * @param oid      the oid of the property that was received
     * @param pid      the property id of the property that was received
     * @param pin      the index of the property that was received
     * @param value    the value of the property that was received
     * @return true if the request should be cancelled.
     */
    boolean progress(double progress, int deviceId, ObjectIdentifier oid, PropertyIdentifier pid,
            UnsignedInteger pin, Encodable value);
}
