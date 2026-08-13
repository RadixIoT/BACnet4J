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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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

    /**
     * A fully populated NPCI still decodes, so the truncation checks below are not simply rejecting everything.
     */
    @Test
    public void roundTrip() throws Exception {
        NPCI original = new NPCI(new Address(2, new byte[] {1}));
        ByteQueue queue = new ByteQueue();
        original.write(queue);

        NPCI parsed = new NPCI(queue);
        assertEquals(1, parsed.getVersion());
        assertEquals(0xFFFF, parsed.getDestinationNetwork());
        assertEquals(0, queue.size());
    }

    /**
     * The control octet says which of the optional fields follow, so a message truncated part way through them used
     * to run the queue empty and fail with an ArrayIndexOutOfBoundsException naming an array index. Each field is
     * now checked against the data remaining, and reported as the malformed message it is.
     */
    @Test
    public void truncatedNpciIsReportedAsMalformed() {
        // Nothing at all, and the version octet without the control octet.
        assertTruncated("");
        assertTruncated("01");
        // Control bit 5 set: a destination network, length and address follow.
        assertTruncated("0120");
        assertTruncated("012000");
        assertTruncated("0120ffff");
        // A destination address length longer than the data that follows it.
        assertTruncated("0120ffff04010203");
        // Control bit 3 set: a source network, length and address follow.
        assertTruncated("0108");
        assertTruncated("01080002");
        assertTruncated("0108000204010203");
        // Control bit 5 also requires a hop count after the destination.
        assertTruncated("0120ffff00");
        // Control bit 7 set: a message type, and a vendor id when the type is 80 or above.
        assertTruncated("0180");
        assertTruncated("018050");
        assertTruncated("01805000");
    }

    private static void assertTruncated(String hex) {
        ByteQueue queue = new ByteQueue(hex);
        MessageValidationException e =
                assertThrows(hex, MessageValidationException.class, () -> new NPCI(queue));
        assertTrue(e.getMessage(), e.getMessage().startsWith("Truncated NPCI"));
    }
}
