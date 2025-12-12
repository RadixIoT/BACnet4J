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

package com.serotonin.bacnet4j.type.enumerated;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AbortReason extends Enumerated {
    public static final AbortReason other = new AbortReason(0);
    public static final AbortReason bufferOverflow = new AbortReason(1);
    public static final AbortReason invalidApduInThisState = new AbortReason(2);
    public static final AbortReason preemptedByHigherPriorityTask = new AbortReason(3);
    public static final AbortReason segmentationNotSupported = new AbortReason(4);
    public static final AbortReason securityError = new AbortReason(5);
    public static final AbortReason insufficientSecurity = new AbortReason(6);
    public static final AbortReason windowSizeOutOfRange = new AbortReason(7);
    public static final AbortReason applicationExceededReplyTime = new AbortReason(8);
    public static final AbortReason outOfResources = new AbortReason(9);
    public static final AbortReason tsmTimeout = new AbortReason(10);
    public static final AbortReason apduTooLong = new AbortReason(11);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static AbortReason forId(final int id) {
        AbortReason e = (AbortReason) idMap.get(id);
        if (e == null)
            e = new AbortReason(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AbortReason forName(final String name) {
        return (AbortReason) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AbortReason(final int value) {
        super(value);
    }

    public AbortReason(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    /**
     * Returns a unmodifiable map.
     *
     * @return unmodifiable map
     */
    public static Map<Integer, String> getPrettyMap() {
        return Collections.unmodifiableMap(prettyMap);
    }

    /**
     * Returns a unmodifiable nameMap.
     *
     * @return unmodifiable map
     */
    public static Map<String, Enumerated> getNameMap() {
        return Collections.unmodifiableMap(nameMap);
    }

    @Override
    public String toString() {
        return super.toString(prettyMap);
    }
}
