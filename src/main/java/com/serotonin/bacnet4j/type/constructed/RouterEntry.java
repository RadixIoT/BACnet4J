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
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class RouterEntry extends BaseType {
    private final Unsigned16 networkNumber; // 0
    private final OctetString macAddress; // 1
    private final RouterEntryStatus status; // 2
    private final Unsigned8 performanceIndex; // 3 optional

    public RouterEntry(final Unsigned16 networkNumber, final OctetString macAddress, final RouterEntryStatus status,
            final Unsigned8 performanceIndex) {
        this.networkNumber = networkNumber;
        this.macAddress = macAddress;
        this.status = status;
        this.performanceIndex = performanceIndex;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, networkNumber, 0);
        writeOptional(queue, macAddress, 1);
        writeANY(queue, status, 2);
        writeOptional(queue, performanceIndex, 3);
    }

    public RouterEntry(final ByteQueue queue) throws BACnetException {
        networkNumber = read(queue, Unsigned16.class, 0);
        macAddress = read(queue, OctetString.class, 1);
        status = read(queue, RouterEntryStatus.class, 2);
        performanceIndex = readOptional(queue, Unsigned8.class, 3);
    }

    public Unsigned16 getNetworkNumber() {
        return networkNumber;
    }

    public OctetString getMacAddress() {
        return macAddress;
    }

    public RouterEntryStatus getStatus() {
        return status;
    }

    public Unsigned8 getPerformanceIndex() {
        return performanceIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (macAddress == null ? 0 : macAddress.hashCode());
        result = prime * result + (networkNumber == null ? 0 : networkNumber.hashCode());
        result = prime * result + (performanceIndex == null ? 0 : performanceIndex.hashCode());
        result = prime * result + (status == null ? 0 : status.hashCode());
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
        final RouterEntry other = (RouterEntry) obj;
        if (macAddress == null) {
            if (other.macAddress != null)
                return false;
        } else if (!macAddress.equals(other.macAddress))
            return false;
        if (networkNumber == null) {
            if (other.networkNumber != null)
                return false;
        } else if (!networkNumber.equals(other.networkNumber))
            return false;
        if (performanceIndex == null) {
            if (other.performanceIndex != null)
                return false;
        } else if (!performanceIndex.equals(other.performanceIndex))
            return false;
        if (status == null) {
            if (other.status != null)
                return false;
        } else if (!status.equals(other.status))
            return false;
        return true;
    }

    public static class RouterEntryStatus extends Enumerated {
        public static final RouterEntryStatus available = new RouterEntryStatus(0);
        public static final RouterEntryStatus busy = new RouterEntryStatus(1);
        public static final RouterEntryStatus disconnected = new RouterEntryStatus(2);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        static {
            Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        }

        public static RouterEntryStatus forId(final int id) {
            RouterEntryStatus e = (RouterEntryStatus) idMap.get(id);
            if (e == null)
                e = new RouterEntryStatus(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static RouterEntryStatus forName(final String name) {
            return (RouterEntryStatus) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private RouterEntryStatus(final int value) {
            super(value);
        }

        public RouterEntryStatus(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }

    @Override
    public String toString() {
        return "RouterEntry [networkNumber=" + networkNumber + ", macAddress=" + macAddress + ", status=" + status + ", performanceIndex=" + performanceIndex + ']';
    }
}
