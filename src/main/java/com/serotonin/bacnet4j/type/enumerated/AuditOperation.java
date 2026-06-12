/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

public class AuditOperation extends Enumerated {
    public static final AuditOperation read = new AuditOperation(0);
    public static final AuditOperation write = new AuditOperation(1);
    public static final AuditOperation create = new AuditOperation(2);
    public static final AuditOperation delete = new AuditOperation(3);
    public static final AuditOperation lifeSafety = new AuditOperation(4);
    public static final AuditOperation acknowledgeAlarm = new AuditOperation(5);
    public static final AuditOperation deviceDisableComm = new AuditOperation(6);
    public static final AuditOperation deviceEnableComm = new AuditOperation(7);
    public static final AuditOperation deviceReset = new AuditOperation(8);
    public static final AuditOperation deviceBackup = new AuditOperation(9);
    public static final AuditOperation deviceRestore = new AuditOperation(10);
    public static final AuditOperation subscription = new AuditOperation(11);
    public static final AuditOperation notification = new AuditOperation(12);
    public static final AuditOperation auditingFailure = new AuditOperation(13);
    public static final AuditOperation networkChanges = new AuditOperation(14);
    public static final AuditOperation general = new AuditOperation(15);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static AuditOperation forId(final int id) {
        AuditOperation e = (AuditOperation) idMap.get(id);
        if (e == null)
            e = new AuditOperation(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AuditOperation forName(final String name) {
        return (AuditOperation) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AuditOperation(final int value) {
        super(value);
    }

    public AuditOperation(final ByteQueue queue) throws BACnetErrorException {
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
