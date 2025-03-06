package com.serotonin.bacnet4j.npdu;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.npdu.NPCI.NetworkPriority;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class NPCITest {
    @Test
    public void priorityTest() {
        NPCI npci = new NPCI(new Address(2, new byte[] { 1 }));
        npci.priority(NetworkPriority.criticalEquipment);
        ByteQueue queue = new ByteQueue();
        npci.write(queue);

        byte[] expected = { 0x1, // version
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
