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

public class AccessEvent extends Enumerated {
    public static final AccessEvent none = new AccessEvent(0);
    public static final AccessEvent granted = new AccessEvent(1);
    public static final AccessEvent muster = new AccessEvent(2);
    public static final AccessEvent passbackDetected = new AccessEvent(3);
    public static final AccessEvent duress = new AccessEvent(4);
    public static final AccessEvent trace = new AccessEvent(5);
    public static final AccessEvent lockoutMaxAttempts = new AccessEvent(6);
    public static final AccessEvent lockoutOther = new AccessEvent(7);
    public static final AccessEvent lockoutRelinquished = new AccessEvent(8);
    public static final AccessEvent lockedByHigherPriority = new AccessEvent(9);
    public static final AccessEvent outOfService = new AccessEvent(10);
    public static final AccessEvent outOfServiceRelinquished = new AccessEvent(11);
    public static final AccessEvent accompanimentBy = new AccessEvent(12);
    public static final AccessEvent authenticationFactorRead = new AccessEvent(13);
    public static final AccessEvent authorizationDelayed = new AccessEvent(14);
    public static final AccessEvent verificationRequired = new AccessEvent(15);
    public static final AccessEvent noEntryAfterGrant = new AccessEvent(16);
    public static final AccessEvent deniedDenyAll = new AccessEvent(128);
    public static final AccessEvent deniedUnknownCredential = new AccessEvent(129);
    public static final AccessEvent deniedAuthenticationUnavailable = new AccessEvent(130);
    public static final AccessEvent deniedAuthenticationFactorTimeout = new AccessEvent(131);
    public static final AccessEvent deniedIncorrectAuthenticationFactor = new AccessEvent(132);
    public static final AccessEvent deniedZoneNoAccessRights = new AccessEvent(133);
    public static final AccessEvent deniedPointNoAccessRights = new AccessEvent(134);
    public static final AccessEvent deniedNoAccessRights = new AccessEvent(135);
    public static final AccessEvent deniedOutOfTimeRange = new AccessEvent(136);
    public static final AccessEvent deniedThreatLevel = new AccessEvent(137);
    public static final AccessEvent deniedPassback = new AccessEvent(138);
    public static final AccessEvent deniedUnexpectedLocationUsage = new AccessEvent(139);
    public static final AccessEvent deniedMaxAttempts = new AccessEvent(140);
    public static final AccessEvent deniedLowerOccupancyLimit = new AccessEvent(141);
    public static final AccessEvent deniedUpperOccupancyLimit = new AccessEvent(142);
    public static final AccessEvent deniedAuthenticationFactorLost = new AccessEvent(143);
    public static final AccessEvent deniedAuthenticationFactorStolen = new AccessEvent(144);
    public static final AccessEvent deniedAuthenticationFactorDamaged = new AccessEvent(145);
    public static final AccessEvent deniedAuthenticationFactorDestroyed = new AccessEvent(146);
    public static final AccessEvent deniedAuthenticationFactorDisabled = new AccessEvent(147);
    public static final AccessEvent deniedAuthenticationFactorError = new AccessEvent(148);
    public static final AccessEvent deniedCredentialUnassigned = new AccessEvent(149);
    public static final AccessEvent deniedCredentialNotProvisioned = new AccessEvent(150);
    public static final AccessEvent deniedCredentialNotYetActive = new AccessEvent(151);
    public static final AccessEvent deniedCredentialExpired = new AccessEvent(152);
    public static final AccessEvent deniedCredentialManualDisable = new AccessEvent(153);
    public static final AccessEvent deniedCredentialLockout = new AccessEvent(154);
    public static final AccessEvent deniedCredentialMaxDays = new AccessEvent(155);
    public static final AccessEvent deniedCredentialMaxUses = new AccessEvent(156);
    public static final AccessEvent deniedCredentialInactivity = new AccessEvent(157);
    public static final AccessEvent deniedCredentialDisabled = new AccessEvent(158);
    public static final AccessEvent deniedNoAccompaniment = new AccessEvent(159);
    public static final AccessEvent deniedIncorrectAccompaniment = new AccessEvent(160);
    public static final AccessEvent deniedLockout = new AccessEvent(161);
    public static final AccessEvent deniedVerificationFailed = new AccessEvent(162);
    public static final AccessEvent deniedVerificationTimeout = new AccessEvent(163);
    public static final AccessEvent deniedOther = new AccessEvent(164);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static AccessEvent forId(final int id) {
        AccessEvent e = (AccessEvent) idMap.get(id);
        if (e == null)
            e = new AccessEvent(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AccessEvent forName(final String name) {
        return (AccessEvent) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AccessEvent(final int value) {
        super(value);
    }

    public AccessEvent(final ByteQueue queue) throws BACnetErrorException {
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
