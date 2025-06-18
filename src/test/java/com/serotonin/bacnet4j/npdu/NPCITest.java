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

package com.serotonin.bacnet4j.npdu;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.npdu.NPCI.NetworkPriority;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class NPCITest {
    @Test
    public void priorityTest() {
        NPCI npci = new NPCI(new Address(2, new byte[] {1}));
        System.out.println(npci.getNetworkPriority());
        npci.priority(NetworkPriority.criticalEquipment);
        System.out.println(npci.getNetworkPriority());

        ByteQueue queue = new ByteQueue();
        npci.write(queue);
        System.out.println(queue);

        byte[] expected = {0x1, // version
                0x2a, // control Bx00101010
                (byte) 0xff, (byte) 0xff, // dest all networks
                0x0, // dest address length
                0x0, 0x2, // source network
                0x1, // source address length
                0x1, // source address
                (byte) 0xff, // hop count
        };
        assertArrayEquals(expected, queue.popAll());
    }
}
