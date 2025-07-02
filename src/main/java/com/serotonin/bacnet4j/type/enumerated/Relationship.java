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

public class Relationship extends Enumerated {
    public static final Relationship unknown = new Relationship(0);
    public static final Relationship _default = new Relationship(1);
    public static final Relationship contains = new Relationship(2);
    public static final Relationship containedBy = new Relationship(3);
    public static final Relationship uses = new Relationship(4);
    public static final Relationship usedBy = new Relationship(5);
    public static final Relationship commands = new Relationship(6);
    public static final Relationship commandedBy = new Relationship(7);
    public static final Relationship adjusts = new Relationship(8);
    public static final Relationship adjustedBy = new Relationship(9);
    public static final Relationship ingress = new Relationship(10);
    public static final Relationship egress = new Relationship(11);
    public static final Relationship suppliesAir = new Relationship(12);
    public static final Relationship receivesAir = new Relationship(13);
    public static final Relationship suppliesHotAir = new Relationship(14);
    public static final Relationship receivesHotAir = new Relationship(15);
    public static final Relationship suppliesCoolAir = new Relationship(16);
    public static final Relationship receivesCoolAir = new Relationship(17);
    public static final Relationship suppliesPower = new Relationship(18);
    public static final Relationship receivesPower = new Relationship(19);
    public static final Relationship suppliesGas = new Relationship(20);
    public static final Relationship receivesGas = new Relationship(21);
    public static final Relationship suppliesWater = new Relationship(22);
    public static final Relationship receivesWater = new Relationship(23);
    public static final Relationship suppliesHotWater = new Relationship(24);
    public static final Relationship receivesHotWater = new Relationship(25);
    public static final Relationship suppliesCoolWater = new Relationship(26);
    public static final Relationship receivesCoolWater = new Relationship(27);
    public static final Relationship suppliesSteam = new Relationship(28);
    public static final Relationship receivesSteam = new Relationship(29);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static Relationship forId(final int id) {
        Relationship e = (Relationship) idMap.get(id);
        if (e == null)
            e = new Relationship(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static Relationship forName(final String name) {
        return (Relationship) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private Relationship(final int value) {
        super(value);
    }

    public Relationship(final ByteQueue queue) throws BACnetErrorException {
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
