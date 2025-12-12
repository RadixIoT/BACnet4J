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

public class LiftCarMode extends Enumerated {
    public static final LiftCarMode unknown = new LiftCarMode(0);
    public static final LiftCarMode normal = new LiftCarMode(1);
    public static final LiftCarMode vip = new LiftCarMode(2);
    public static final LiftCarMode homing = new LiftCarMode(3);
    public static final LiftCarMode parking = new LiftCarMode(4);
    public static final LiftCarMode attendantControl = new LiftCarMode(5);
    public static final LiftCarMode firefighterControl = new LiftCarMode(6);
    public static final LiftCarMode emergencyPower = new LiftCarMode(7);
    public static final LiftCarMode inspection = new LiftCarMode(8);
    public static final LiftCarMode cabinetRecall = new LiftCarMode(9);
    public static final LiftCarMode earthquakeOperation = new LiftCarMode(10);
    public static final LiftCarMode fireOperation = new LiftCarMode(11);
    public static final LiftCarMode outOfService = new LiftCarMode(12);
    public static final LiftCarMode occupantEvacuation = new LiftCarMode(13);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static LiftCarMode forId(final int id) {
        LiftCarMode e = (LiftCarMode) idMap.get(id);
        if (e == null)
            e = new LiftCarMode(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LiftCarMode forName(final String name) {
        return (LiftCarMode) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LiftCarMode(final int value) {
        super(value);
    }

    public LiftCarMode(final ByteQueue queue) throws BACnetErrorException {
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
