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

package com.serotonin.bacnet4j.type.constructed;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AccessRule extends BaseType {
    private final TimeRangeSpecifier timeRangeSpecifier;
    private final DeviceObjectPropertyReference timeRange;
    private final LocationSpecifier locationSpecifier;
    private final DeviceObjectReference location;
    private final Boolean enable;

    public AccessRule(final Boolean enable) {
        this(TimeRangeSpecifier.always, null, LocationSpecifier.all, null, enable);
    }

    public AccessRule(final DeviceObjectPropertyReference timeRange, final Boolean enable) {
        this(TimeRangeSpecifier.specified, timeRange, LocationSpecifier.all, null, enable);
    }

    public AccessRule(final DeviceObjectReference location, final Boolean enable) {
        this(TimeRangeSpecifier.always, null, LocationSpecifier.specified, location, enable);
    }

    public AccessRule(final DeviceObjectPropertyReference timeRange, final DeviceObjectReference location,
            final Boolean enable) {
        this(TimeRangeSpecifier.specified, timeRange, LocationSpecifier.specified, location, enable);
    }

    private AccessRule(final TimeRangeSpecifier timeRangeSpecifier, final DeviceObjectPropertyReference timeRange,
            final LocationSpecifier locationSpecifier, final DeviceObjectReference location, final Boolean enable) {
        this.timeRangeSpecifier = timeRangeSpecifier;
        this.timeRange = timeRange;
        this.locationSpecifier = locationSpecifier;
        this.location = location;
        this.enable = enable;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeRangeSpecifier, 0);
        writeOptional(queue, timeRange, 1);
        write(queue, locationSpecifier, 2);
        writeOptional(queue, location, 3);
        write(queue, enable, 4);
    }

    public AccessRule(final ByteQueue queue) throws BACnetException {
        timeRangeSpecifier = read(queue, TimeRangeSpecifier.class, 0);
        timeRange = readOptional(queue, DeviceObjectPropertyReference.class, 1);
        locationSpecifier = read(queue, LocationSpecifier.class, 2);
        location = readOptional(queue, DeviceObjectReference.class, 3);
        enable = read(queue, Boolean.class, 4);
    }

    public TimeRangeSpecifier getTimeRangeSpecifier() {
        return timeRangeSpecifier;
    }

    public DeviceObjectPropertyReference getTimeRange() {
        return timeRange;
    }

    public LocationSpecifier getLocationSpecifier() {
        return locationSpecifier;
    }

    public DeviceObjectReference getLocation() {
        return location;
    }

    public Boolean getEnable() {
        return enable;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enable == null ? 0 : enable.hashCode());
        result = prime * result + (location == null ? 0 : location.hashCode());
        result = prime * result + (locationSpecifier == null ? 0 : locationSpecifier.hashCode());
        result = prime * result + (timeRange == null ? 0 : timeRange.hashCode());
        result = prime * result + (timeRangeSpecifier == null ? 0 : timeRangeSpecifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final AccessRule other = (AccessRule) obj;
        if (enable == null) {
            if (other.enable != null)
                return false;
        } else if (!enable.equals(other.enable))
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (locationSpecifier == null) {
            if (other.locationSpecifier != null)
                return false;
        } else if (!locationSpecifier.equals(other.locationSpecifier))
            return false;
        if (timeRange == null) {
            if (other.timeRange != null)
                return false;
        } else if (!timeRange.equals(other.timeRange))
            return false;
        if (timeRangeSpecifier == null) {
            if (other.timeRangeSpecifier != null)
                return false;
        } else if (!timeRangeSpecifier.equals(other.timeRangeSpecifier))
            return false;
        return true;
    }

    public static class TimeRangeSpecifier extends Enumerated {
        public static final TimeRangeSpecifier specified = new TimeRangeSpecifier(0);
        public static final TimeRangeSpecifier always = new TimeRangeSpecifier(1);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        static {
            Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        }

        public static TimeRangeSpecifier forId(final int id) {
            TimeRangeSpecifier e = (TimeRangeSpecifier) idMap.get(id);
            if (e == null)
                e = new TimeRangeSpecifier(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static TimeRangeSpecifier forName(final String name) {
            return (TimeRangeSpecifier) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private TimeRangeSpecifier(final int value) {
            super(value);
        }

        public TimeRangeSpecifier(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }


    public static class LocationSpecifier extends Enumerated {
        public static final LocationSpecifier specified = new LocationSpecifier(0);
        public static final LocationSpecifier all = new LocationSpecifier(1);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        static {
            Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        }

        public static LocationSpecifier forId(final int id) {
            LocationSpecifier e = (LocationSpecifier) idMap.get(id);
            if (e == null)
                e = new LocationSpecifier(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static LocationSpecifier forName(final String name) {
            return (LocationSpecifier) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private LocationSpecifier(final int value) {
            super(value);
        }

        public LocationSpecifier(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }

    @Override
    public String toString() {
        return "AccessRule [timeRangeSpecifier=" + timeRangeSpecifier + ", timeRange=" + timeRange + ", locationSpecifier=" + locationSpecifier + ", location=" + location + ", enable=" + enable + ']';
    }


}
