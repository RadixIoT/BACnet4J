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
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfDiscreteValue;
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfTimer;
import com.serotonin.bacnet4j.type.notificationParameters.AccessEventNotif;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfBitStringNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfCharacterStringNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfLifeSafetyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfStateNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfStatusFlagsNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfValueNotif;
import com.serotonin.bacnet4j.type.notificationParameters.CommandFailureNotif;
import com.serotonin.bacnet4j.type.notificationParameters.DoubleOutOfRangeNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ExtendedNotif;
import com.serotonin.bacnet4j.type.notificationParameters.FloatingLimitNotif;
import com.serotonin.bacnet4j.type.notificationParameters.OutOfRangeNotif;
import com.serotonin.bacnet4j.type.notificationParameters.SignedOutOfRangeNotif;
import com.serotonin.bacnet4j.type.notificationParameters.UnsignedOutOfRangeNotif;
import com.serotonin.bacnet4j.type.notificationParameters.UnsignedRangeNotif;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class EventType extends Enumerated {
    public static final EventType changeOfBitstring = new EventType(ChangeOfBitStringNotif.TYPE_ID);
    public static final EventType changeOfState = new EventType(ChangeOfStateNotif.TYPE_ID);
    public static final EventType changeOfValue = new EventType(ChangeOfValueNotif.TYPE_ID);
    public static final EventType commandFailure = new EventType(CommandFailureNotif.TYPE_ID);
    public static final EventType floatingLimit = new EventType(FloatingLimitNotif.TYPE_ID);
    public static final EventType outOfRange = new EventType(OutOfRangeNotif.TYPE_ID);
    public static final EventType changeOfLifeSafety = new EventType(ChangeOfLifeSafetyNotif.TYPE_ID);
    public static final EventType extended = new EventType(ExtendedNotif.TYPE_ID);
    public static final EventType bufferReady = new EventType(BufferReadyNotif.TYPE_ID);
    public static final EventType unsignedRange = new EventType(UnsignedRangeNotif.TYPE_ID);
    public static final EventType accessEvent = new EventType(AccessEventNotif.TYPE_ID);
    public static final EventType doubleOutOfRange = new EventType(DoubleOutOfRangeNotif.TYPE_ID);
    public static final EventType signedOutOfRange = new EventType(SignedOutOfRangeNotif.TYPE_ID);
    public static final EventType unsignedOutOfRange = new EventType(UnsignedOutOfRangeNotif.TYPE_ID);
    public static final EventType changeOfCharacterstring = new EventType(ChangeOfCharacterStringNotif.TYPE_ID);
    public static final EventType changeOfStatusFlags = new EventType(ChangeOfStatusFlagsNotif.TYPE_ID);
    public static final EventType changeOfReliability = new EventType(ChangeOfReliabilityNotif.TYPE_ID);
    public static final EventType none = new EventType(20);
    public static final EventType changeOfDiscreteValue = new EventType(ChangeOfDiscreteValue.TYPE_ID);
    public static final EventType changeOfTimer = new EventType(ChangeOfTimer.TYPE_ID);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static EventType forId(final int id) {
        EventType e = (EventType) idMap.get(id);
        if (e == null)
            e = new EventType(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static EventType forName(final String name) {
        return (EventType) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private EventType(final int value) {
        super(value);
    }

    public EventType(final ByteQueue queue) throws BACnetErrorException {
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
