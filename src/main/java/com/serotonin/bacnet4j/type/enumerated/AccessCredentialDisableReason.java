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

public class AccessCredentialDisableReason extends Enumerated {
    public static final AccessCredentialDisableReason disabled = new AccessCredentialDisableReason(0);
    public static final AccessCredentialDisableReason disabledNeedsProvisioning = new AccessCredentialDisableReason(1);
    public static final AccessCredentialDisableReason disabledUnassigned = new AccessCredentialDisableReason(2);
    public static final AccessCredentialDisableReason disabledNotYetActive = new AccessCredentialDisableReason(3);
    public static final AccessCredentialDisableReason disabledExpired = new AccessCredentialDisableReason(4);
    public static final AccessCredentialDisableReason disabledLockout = new AccessCredentialDisableReason(5);
    public static final AccessCredentialDisableReason disabledMaxDays = new AccessCredentialDisableReason(6);
    public static final AccessCredentialDisableReason disabledMaxUses = new AccessCredentialDisableReason(7);
    public static final AccessCredentialDisableReason disabledInactivity = new AccessCredentialDisableReason(8);
    public static final AccessCredentialDisableReason disabledManual = new AccessCredentialDisableReason(9);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static AccessCredentialDisableReason forId(final int id) {
        AccessCredentialDisableReason e = (AccessCredentialDisableReason) idMap.get(id);
        if (e == null)
            e = new AccessCredentialDisableReason(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AccessCredentialDisableReason forName(final String name) {
        return (AccessCredentialDisableReason) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AccessCredentialDisableReason(final int value) {
        super(value);
    }

    public AccessCredentialDisableReason(final ByteQueue queue) throws BACnetErrorException {
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
