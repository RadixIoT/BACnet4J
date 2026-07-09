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

public class ObjectType extends Enumerated {
    public static final ObjectType analogInput = new ObjectType(0);
    public static final ObjectType analogOutput = new ObjectType(1);
    public static final ObjectType analogValue = new ObjectType(2);
    public static final ObjectType binaryInput = new ObjectType(3);
    public static final ObjectType binaryOutput = new ObjectType(4);
    public static final ObjectType binaryValue = new ObjectType(5);
    public static final ObjectType calendar = new ObjectType(6);
    public static final ObjectType command = new ObjectType(7);
    public static final ObjectType device = new ObjectType(8);
    public static final ObjectType eventEnrollment = new ObjectType(9);
    public static final ObjectType file = new ObjectType(10);
    public static final ObjectType group = new ObjectType(11);
    public static final ObjectType loop = new ObjectType(12);
    public static final ObjectType multiStateInput = new ObjectType(13);
    public static final ObjectType multiStateOutput = new ObjectType(14);
    public static final ObjectType notificationClass = new ObjectType(15);
    public static final ObjectType program = new ObjectType(16);
    public static final ObjectType schedule = new ObjectType(17);
    public static final ObjectType averaging = new ObjectType(18);
    public static final ObjectType multiStateValue = new ObjectType(19);
    public static final ObjectType trendLog = new ObjectType(20);
    public static final ObjectType lifeSafetyPoint = new ObjectType(21);
    public static final ObjectType lifeSafetyZone = new ObjectType(22);
    public static final ObjectType accumulator = new ObjectType(23);
    public static final ObjectType pulseConverter = new ObjectType(24);
    public static final ObjectType eventLog = new ObjectType(25);
    public static final ObjectType globalGroup = new ObjectType(26);
    public static final ObjectType trendLogMultiple = new ObjectType(27);
    public static final ObjectType loadControl = new ObjectType(28);
    public static final ObjectType structuredView = new ObjectType(29);
    public static final ObjectType accessDoor = new ObjectType(30);
    public static final ObjectType timer = new ObjectType(31);
    public static final ObjectType accessCredential = new ObjectType(32);
    public static final ObjectType accessPoint = new ObjectType(33);
    public static final ObjectType accessRights = new ObjectType(34);
    public static final ObjectType accessUser = new ObjectType(35);
    public static final ObjectType accessZone = new ObjectType(36);
    public static final ObjectType credentialDataInput = new ObjectType(37);
    public static final ObjectType bitstringValue = new ObjectType(39);
    public static final ObjectType characterstringValue = new ObjectType(40);
    public static final ObjectType datePatternValue = new ObjectType(41);
    public static final ObjectType dateValue = new ObjectType(42);
    public static final ObjectType datetimePatternValue = new ObjectType(43);
    public static final ObjectType datetimeValue = new ObjectType(44);
    public static final ObjectType integerValue = new ObjectType(45);
    public static final ObjectType largeAnalogValue = new ObjectType(46);
    public static final ObjectType octetstringValue = new ObjectType(47);
    public static final ObjectType positiveIntegerValue = new ObjectType(48);
    public static final ObjectType timePatternValue = new ObjectType(49);
    public static final ObjectType timeValue = new ObjectType(50);
    public static final ObjectType notificationForwarder = new ObjectType(51);
    public static final ObjectType alertEnrollment = new ObjectType(52);
    public static final ObjectType channel = new ObjectType(53);
    public static final ObjectType lightingOutput = new ObjectType(54);
    public static final ObjectType binaryLightingOutput = new ObjectType(55);
    public static final ObjectType networkPort = new ObjectType(56);
    public static final ObjectType elevatorGroup = new ObjectType(57);
    public static final ObjectType escalator = new ObjectType(58);
    public static final ObjectType lift = new ObjectType(59);
    public static final ObjectType staging = new ObjectType(60);
    public static final ObjectType auditLog = new ObjectType(61);
    public static final ObjectType auditReporter = new ObjectType(62);
    public static final ObjectType color = new ObjectType(63);
    public static final ObjectType colorTemperature = new ObjectType(64);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static ObjectType forId(int id) {
        ObjectType e = (ObjectType) idMap.get(id);
        if (e == null)
            e = new ObjectType(id);
        return e;
    }

    public static String nameForId(int id) {
        return prettyMap.get(id);
    }

    public static ObjectType forName(String name) {
        return (ObjectType) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private ObjectType(int value) {
        super(value);
    }

    public ObjectType(ByteQueue queue) throws BACnetErrorException {
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
