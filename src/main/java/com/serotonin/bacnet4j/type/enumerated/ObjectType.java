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
    public static final ObjectType networkSecurity = new ObjectType(38);
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

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static ObjectType forId(final int id) {
        ObjectType e = (ObjectType) idMap.get(id);
        if (e == null)
            e = new ObjectType(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static ObjectType forName(final String name) {
        return (ObjectType) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private ObjectType(final int value) {
        super(value);
    }

    public ObjectType(final ByteQueue queue) throws BACnetErrorException {
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

    //    @Override
    //    public String toString() {
    //        final int type = intValue();
    //        if (type == analogInput.intValue())
    //            return "Analog Input";
    //        if (type == analogOutput.intValue())
    //            return "Analog Output";
    //        if (type == analogValue.intValue())
    //            return "Analog Value";
    //        if (type == binaryInput.intValue())
    //            return "Binary Input";
    //        if (type == binaryOutput.intValue())
    //            return "Binary Output";
    //        if (type == binaryValue.intValue())
    //            return "Binary Value";
    //        if (type == calendar.intValue())
    //            return "Calendar";
    //        if (type == command.intValue())
    //            return "Command";
    //        if (type == device.intValue())
    //            return "Device";
    //        if (type == eventEnrollment.intValue())
    //            return "Event Enrollment";
    //        if (type == file.intValue())
    //            return "File";
    //        if (type == group.intValue())
    //            return "Group";
    //        if (type == loop.intValue())
    //            return "Loop";
    //        if (type == multiStateInput.intValue())
    //            return "Multi-state Input";
    //        if (type == multiStateOutput.intValue())
    //            return "Multi-state Output";
    //        if (type == notificationClass.intValue())
    //            return "Notification Class";
    //        if (type == program.intValue())
    //            return "Program";
    //        if (type == schedule.intValue())
    //            return "Schedule";
    //        if (type == averaging.intValue())
    //            return "Averaging";
    //        if (type == multiStateValue.intValue())
    //            return "Multi-state Value";
    //        if (type == trendLog.intValue())
    //            return "Trend Log";
    //        if (type == lifeSafetyPoint.intValue())
    //            return "Life Safety Point";
    //        if (type == lifeSafetyZone.intValue())
    //            return "Life Safety Zone";
    //        if (type == accumulator.intValue())
    //            return "Accumulator";
    //        if (type == pulseConverter.intValue())
    //            return "Pulse Converter";
    //        if (type == eventLog.intValue())
    //            return "Event Log";
    //        if (type == globalGroup.intValue())
    //            return "Global Group";
    //        if (type == trendLogMultiple.intValue())
    //            return "Trend Log Multiple";
    //        if (type == loadControl.intValue())
    //            return "Load Control";
    //        if (type == structuredView.intValue())
    //            return "Structured View";
    //        if (type == accessDoor.intValue())
    //            return "Access Door";
    //        if (type == timer.intValue())
    //            return "Timer";
    //        if (type == accessCredential.intValue())
    //            return "Access Credential";
    //        if (type == accessPoint.intValue())
    //            return "Access Point";
    //        if (type == accessRights.intValue())
    //            return "Access Rights";
    //        if (type == accessUser.intValue())
    //            return "Access User";
    //        if (type == accessZone.intValue())
    //            return "Access Zone";
    //        if (type == credentialDataInput.intValue())
    //            return "Credential Data Input";
    //        if (type == networkSecurity.intValue())
    //            return "Network Security";
    //        if (type == bitstringValue.intValue())
    //            return "Bitstring Value";
    //        if (type == characterstringValue.intValue())
    //            return "Characterstring Value";
    //        if (type == datePatternValue.intValue())
    //            return "Date Pattern Value";
    //        if (type == dateValue.intValue())
    //            return "Date Value";
    //        if (type == datetimePatternValue.intValue())
    //            return "Datetime Pattern Value";
    //        if (type == datetimeValue.intValue())
    //            return "Datetime Value";
    //        if (type == integerValue.intValue())
    //            return "Integer Value";
    //        if (type == largeAnalogValue.intValue())
    //            return "Large Analog Value";
    //        if (type == octetstringValue.intValue())
    //            return "Octetstring Value";
    //        if (type == positiveIntegerValue.intValue())
    //            return "Positive Integer Value";
    //        if (type == timePatternValue.intValue())
    //            return "Time Pattern Value";
    //        if (type == timeValue.intValue())
    //            return "Time Value";
    //        if (type == notificationForwarder.intValue())
    //            return "Notification Forwarder";
    //        if (type == alertEnrollment.intValue())
    //            return "Alert Enrollment";
    //        if (type == channel.intValue())
    //            return "Channel";
    //        if (type == lightingOutput.intValue())
    //            return "Lighting Output";
    //        if (type == binaryLightingOutput.intValue())
    //            return "Binary Lighting Output";
    //        if (type == networkPort.intValue())
    //            return "Network Port";
    //        if (type == elevatorGroup.intValue())
    //            return "Elevator Group";
    //        if (type == escalator.intValue())
    //            return "Escalator";
    //        if (type == lift.intValue())
    //            return "Lift";
    //        return "Vendor Specific (" + type + ")";
    //    }
}
