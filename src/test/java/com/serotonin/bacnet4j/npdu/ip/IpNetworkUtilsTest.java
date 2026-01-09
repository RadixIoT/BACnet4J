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

package com.serotonin.bacnet4j.npdu.ip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IpNetworkUtilsTest {
    @Test
    public void createSubmask() {
        assertEquals(0x00000000L, IpNetworkUtils.createMask(0));
        assertEquals(0x80000000L, IpNetworkUtils.createMask(1));
        assertEquals(0xC0000000L, IpNetworkUtils.createMask(2));
        assertEquals(0xE0000000L, IpNetworkUtils.createMask(3));
        assertEquals(0xF0000000L, IpNetworkUtils.createMask(4));
        assertEquals(0xF8000000L, IpNetworkUtils.createMask(5));
        assertEquals(0xFC000000L, IpNetworkUtils.createMask(6));
        assertEquals(0xFE000000L, IpNetworkUtils.createMask(7));
        assertEquals(0xFF000000L, IpNetworkUtils.createMask(8));
        assertEquals(0xFF800000L, IpNetworkUtils.createMask(9));
        assertEquals(0xFFC00000L, IpNetworkUtils.createMask(10));
        assertEquals(0xFFE00000L, IpNetworkUtils.createMask(11));
        assertEquals(0xFFF00000L, IpNetworkUtils.createMask(12));
        assertEquals(0xFFF80000L, IpNetworkUtils.createMask(13));
        assertEquals(0xFFFC0000L, IpNetworkUtils.createMask(14));
        assertEquals(0xFFFE0000L, IpNetworkUtils.createMask(15));
        assertEquals(0xFFFF0000L, IpNetworkUtils.createMask(16));
        assertEquals(0xFFFF8000L, IpNetworkUtils.createMask(17));
        assertEquals(0xFFFFC000L, IpNetworkUtils.createMask(18));
        assertEquals(0xFFFFE000L, IpNetworkUtils.createMask(19));
        assertEquals(0xFFFFF000L, IpNetworkUtils.createMask(20));
        assertEquals(0xFFFFF800L, IpNetworkUtils.createMask(21));
        assertEquals(0xFFFFFC00L, IpNetworkUtils.createMask(22));
        assertEquals(0xFFFFFE00L, IpNetworkUtils.createMask(23));
        assertEquals(0xFFFFFF00L, IpNetworkUtils.createMask(24));
        assertEquals(0xFFFFFF80L, IpNetworkUtils.createMask(25));
        assertEquals(0xFFFFFFC0L, IpNetworkUtils.createMask(26));
        assertEquals(0xFFFFFFE0L, IpNetworkUtils.createMask(27));
        assertEquals(0xFFFFFFF0L, IpNetworkUtils.createMask(28));
        assertEquals(0xFFFFFFF8L, IpNetworkUtils.createMask(29));
        assertEquals(0xFFFFFFFCL, IpNetworkUtils.createMask(30));
        assertEquals(0xFFFFFFFEL, IpNetworkUtils.createMask(31));
        assertEquals(0xFFFFFFFFL, IpNetworkUtils.createMask(32));
    }

    @Test
    public void matchWithMask() {
        assertTrue(IpNetworkUtils.matchWithMask("1.2.3.4", "1.2.3.5", "255.255.255.0"));
        assertTrue(IpNetworkUtils.matchWithMask("1.2.3.4", "1.2.3.5", "255.255.255.254"));
        assertFalse(IpNetworkUtils.matchWithMask("1.2.3.4", "1.2.3.5", "255.255.255.255"));
        assertTrue(IpNetworkUtils.matchWithMask("0.0.0.0", "255.255.255.255", "0.0.0.0"));
        assertFalse(IpNetworkUtils.matchWithMask("0.0.0.0", "255.255.255.255", "128.0.0.0"));
        assertFalse(IpNetworkUtils.matchWithMask("0.0.0.0", "255.255.255.255", "0.64.0.0"));
    }
}
