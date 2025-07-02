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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;

import lohbihler.warp.WarpClock;

public class RemoteEntityCacheTest {
    private final TestNetworkMap map = new TestNetworkMap();

    @Test
    public void test() {
        final WarpClock clock = new WarpClock();
        final LocalDevice d = new LocalDevice(0, new DefaultTransport(new TestNetwork(map, 1, 10))).withClock(clock);

        final RemoteEntityCache<String, String> cache = new RemoteEntityCache<>(d);

        cache.putEntity("key1", "value1", RemoteEntityCachePolicy.NEVER_CACHE);
        cache.putEntity("key2", "value2", RemoteEntityCachePolicy.EXPIRE_5_SECONDS);
        cache.putEntity("key3", "value3", RemoteEntityCachePolicy.NEVER_EXPIRE);

        assertNull(cache.getCachedEntity("key1"));
        assertEquals("value2", cache.getCachedEntity("key2"));
        assertEquals("value3", cache.getCachedEntity("key3"));

        // Advance the clock 10 seconds
        clock.plusSeconds(10);

        assertNull(cache.getCachedEntity("key1"));
        assertNull(cache.getCachedEntity("key2"));
        assertEquals("value3", cache.getCachedEntity("key3"));

        // Advance the clock 1 year
        clock.plusYears(1);

        assertNull(cache.getCachedEntity("key1"));
        assertNull(cache.getCachedEntity("key2"));
        assertEquals("value3", cache.getCachedEntity("key3"));
    }
}
