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

package com.serotonin.bacnet4j.npdu.sc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExponentialBackoffTest {
    @Test
    public void testBackoff() {
        ExponentialBackoff eb = new ExponentialBackoff(1.2);
        eb.configure(2, 100);

        assertEquals(2, eb.getReconnectWaitTimeout());
        assertEquals(3, eb.getReconnectWaitTimeout());
        assertEquals(4, eb.getReconnectWaitTimeout());
        assertEquals(5, eb.getReconnectWaitTimeout());
        assertEquals(6, eb.getReconnectWaitTimeout());
        assertEquals(7, eb.getReconnectWaitTimeout());
        assertEquals(8, eb.getReconnectWaitTimeout());
        assertEquals(10, eb.getReconnectWaitTimeout());
        assertEquals(12, eb.getReconnectWaitTimeout());
        assertEquals(14, eb.getReconnectWaitTimeout());
        assertEquals(17, eb.getReconnectWaitTimeout());
        assertEquals(20, eb.getReconnectWaitTimeout());
        assertEquals(24, eb.getReconnectWaitTimeout());
        assertEquals(29, eb.getReconnectWaitTimeout());
        assertEquals(35, eb.getReconnectWaitTimeout());
        assertEquals(42, eb.getReconnectWaitTimeout());
        assertEquals(50, eb.getReconnectWaitTimeout());
        assertEquals(60, eb.getReconnectWaitTimeout());
        assertEquals(72, eb.getReconnectWaitTimeout());
        assertEquals(86, eb.getReconnectWaitTimeout());
        assertEquals(100, eb.getReconnectWaitTimeout());
        assertEquals(100, eb.getReconnectWaitTimeout());
        assertEquals(100, eb.getReconnectWaitTimeout());

        eb.reset();

        assertEquals(2, eb.getReconnectWaitTimeout());
        assertEquals(3, eb.getReconnectWaitTimeout());
    }

    /**
     * 12.56.82: when the minimum reconnect time is greater than the maximum, the wait between attempts
     * is a local matter but shall not exceed the maximum. The first wait after configure/reset was
     * previously the unclamped minimum.
     */
    @Test
    public void minimumGreaterThanMaximum_neverExceedsMaximum() {
        ExponentialBackoff eb = new ExponentialBackoff(1.5);
        eb.configure(100, 30);

        assertEquals(30, eb.getReconnectWaitTimeout());
        assertEquals(30, eb.getReconnectWaitTimeout());

        eb.reset();
        assertEquals(30, eb.getReconnectWaitTimeout());
    }
}
