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

/**
 * @author Matthew Lohbihler
 */
public class NodeType extends Enumerated {
    public static final NodeType unknown = new NodeType(0);
    public static final NodeType system = new NodeType(1);
    public static final NodeType network = new NodeType(2);
    public static final NodeType device = new NodeType(3);
    public static final NodeType organizational = new NodeType(4);
    public static final NodeType area = new NodeType(5);
    public static final NodeType equipment = new NodeType(6);
    public static final NodeType point = new NodeType(7);
    public static final NodeType collection = new NodeType(8);
    public static final NodeType property = new NodeType(9);
    public static final NodeType functional = new NodeType(10);
    public static final NodeType other = new NodeType(11);
    public static final NodeType subsystem = new NodeType(12);
    public static final NodeType building = new NodeType(13);
    public static final NodeType floor = new NodeType(14);
    public static final NodeType section = new NodeType(15);
    public static final NodeType module = new NodeType(16);
    public static final NodeType tree = new NodeType(17);
    public static final NodeType member = new NodeType(18);
    public static final NodeType protocol = new NodeType(19);
    public static final NodeType room = new NodeType(20);
    public static final NodeType zone = new NodeType(21);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static NodeType forId(final int id) {
        NodeType e = (NodeType) idMap.get(id);
        if (e == null)
            e = new NodeType(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static NodeType forName(final String name) {
        return (NodeType) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private NodeType(final int value) {
        super(value);
    }

    public NodeType(final ByteQueue queue) throws BACnetErrorException {
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
