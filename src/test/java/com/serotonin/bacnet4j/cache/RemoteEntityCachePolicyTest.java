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

package com.serotonin.bacnet4j.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;

import lohbihler.warp.WarpClock;

public class RemoteEntityCachePolicyTest {
    private final TestNetworkMap map = new TestNetworkMap();

    @Test
    public void test() {
        final WarpClock clock = new WarpClock();
        final LocalDevice d = new LocalDevice(0, new DefaultTransport(new TestNetwork(map, 1, 10))).withClock(clock);

        final Object neverCache = RemoteEntityCachePolicy.NEVER_CACHE.prepareState(d);
        final Object state5Seconds = RemoteEntityCachePolicy.EXPIRE_5_SECONDS.prepareState(d);
        final Object state1Minute = RemoteEntityCachePolicy.EXPIRE_1_MINUTE.prepareState(d);
        final Object state15Minutes = RemoteEntityCachePolicy.EXPIRE_15_MINUTES.prepareState(d);
        final Object state1Hour = RemoteEntityCachePolicy.EXPIRE_1_HOUR.prepareState(d);
        final Object state4Hours = RemoteEntityCachePolicy.EXPIRE_4_HOURS.prepareState(d);
        final Object state1Day = RemoteEntityCachePolicy.EXPIRE_1_DAY.prepareState(d);
        final Object neverExpire = RemoteEntityCachePolicy.NEVER_EXPIRE.prepareState(d);

        assertTrue(RemoteEntityCachePolicy.NEVER_CACHE.hasExpired(d, neverCache));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_5_SECONDS.hasExpired(d, state5Seconds));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_MINUTE.hasExpired(d, state1Minute));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_15_MINUTES.hasExpired(d, state15Minutes));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_HOUR.hasExpired(d, state1Hour));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_4_HOURS.hasExpired(d, state4Hours));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_DAY.hasExpired(d, state1Day));
        assertFalse(RemoteEntityCachePolicy.NEVER_EXPIRE.hasExpired(d, neverExpire));

        // Advance the clock 10 seconds
        clock.plusSeconds(10);

        assertTrue(RemoteEntityCachePolicy.NEVER_CACHE.hasExpired(d, neverCache));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_5_SECONDS.hasExpired(d, state5Seconds));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_MINUTE.hasExpired(d, state1Minute));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_15_MINUTES.hasExpired(d, state15Minutes));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_HOUR.hasExpired(d, state1Hour));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_4_HOURS.hasExpired(d, state4Hours));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_DAY.hasExpired(d, state1Day));
        assertFalse(RemoteEntityCachePolicy.NEVER_EXPIRE.hasExpired(d, neverExpire));

        // Advance the clock 1 minute
        clock.plusMinutes(1);

        assertTrue(RemoteEntityCachePolicy.NEVER_CACHE.hasExpired(d, neverCache));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_5_SECONDS.hasExpired(d, state5Seconds));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_MINUTE.hasExpired(d, state1Minute));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_15_MINUTES.hasExpired(d, state15Minutes));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_HOUR.hasExpired(d, state1Hour));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_4_HOURS.hasExpired(d, state4Hours));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_DAY.hasExpired(d, state1Day));
        assertFalse(RemoteEntityCachePolicy.NEVER_EXPIRE.hasExpired(d, neverExpire));

        // Advance the clock 15 minutes
        clock.plusMinutes(15);

        assertTrue(RemoteEntityCachePolicy.NEVER_CACHE.hasExpired(d, neverCache));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_5_SECONDS.hasExpired(d, state5Seconds));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_MINUTE.hasExpired(d, state1Minute));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_15_MINUTES.hasExpired(d, state15Minutes));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_HOUR.hasExpired(d, state1Hour));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_4_HOURS.hasExpired(d, state4Hours));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_DAY.hasExpired(d, state1Day));
        assertFalse(RemoteEntityCachePolicy.NEVER_EXPIRE.hasExpired(d, neverExpire));

        // Advance the clock 45 minutes
        clock.plusMinutes(45);

        assertTrue(RemoteEntityCachePolicy.NEVER_CACHE.hasExpired(d, neverCache));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_5_SECONDS.hasExpired(d, state5Seconds));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_MINUTE.hasExpired(d, state1Minute));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_15_MINUTES.hasExpired(d, state15Minutes));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_HOUR.hasExpired(d, state1Hour));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_4_HOURS.hasExpired(d, state4Hours));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_DAY.hasExpired(d, state1Day));
        assertFalse(RemoteEntityCachePolicy.NEVER_EXPIRE.hasExpired(d, neverExpire));

        // Advance the clock 4 hours
        clock.plusHours(4);

        assertTrue(RemoteEntityCachePolicy.NEVER_CACHE.hasExpired(d, neverCache));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_5_SECONDS.hasExpired(d, state5Seconds));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_MINUTE.hasExpired(d, state1Minute));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_15_MINUTES.hasExpired(d, state15Minutes));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_HOUR.hasExpired(d, state1Hour));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_4_HOURS.hasExpired(d, state4Hours));
        assertFalse(RemoteEntityCachePolicy.EXPIRE_1_DAY.hasExpired(d, state1Day));
        assertFalse(RemoteEntityCachePolicy.NEVER_EXPIRE.hasExpired(d, neverExpire));

        // Advance the clock 20 hours
        clock.plusHours(20);

        assertTrue(RemoteEntityCachePolicy.NEVER_CACHE.hasExpired(d, neverCache));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_5_SECONDS.hasExpired(d, state5Seconds));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_MINUTE.hasExpired(d, state1Minute));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_15_MINUTES.hasExpired(d, state15Minutes));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_HOUR.hasExpired(d, state1Hour));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_4_HOURS.hasExpired(d, state4Hours));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_DAY.hasExpired(d, state1Day));
        assertFalse(RemoteEntityCachePolicy.NEVER_EXPIRE.hasExpired(d, neverExpire));

        // Advance the clock 1 year
        clock.plusYears(1);

        assertTrue(RemoteEntityCachePolicy.NEVER_CACHE.hasExpired(d, neverCache));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_5_SECONDS.hasExpired(d, state5Seconds));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_MINUTE.hasExpired(d, state1Minute));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_15_MINUTES.hasExpired(d, state15Minutes));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_HOUR.hasExpired(d, state1Hour));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_4_HOURS.hasExpired(d, state4Hours));
        assertTrue(RemoteEntityCachePolicy.EXPIRE_1_DAY.hasExpired(d, state1Day));
        assertFalse(RemoteEntityCachePolicy.NEVER_EXPIRE.hasExpired(d, neverExpire));
    }
}
