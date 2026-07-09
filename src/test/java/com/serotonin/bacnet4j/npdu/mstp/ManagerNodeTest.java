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

package com.serotonin.bacnet4j.npdu.mstp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ManagerNodeTest {
    private static final byte TS = 0x05;

    private RecordingManagerNode node;

    @Before
    public void setUp() {
        node = new RecordingManagerNode();
    }

    // ---------- bm-1 C3: setUsageTimeout bounds ----------

    @Test
    public void setUsageTimeout_lowerBound_accepted() {
        node.setUsageTimeout(20);
        assertEquals(20, node.usageTimeout);
    }

    @Test
    public void setUsageTimeout_upperBound_accepted() {
        node.setUsageTimeout(35);
        assertEquals(35, node.usageTimeout);
    }

    @Test
    public void setUsageTimeout_belowLowerBound_rejected() {
        try {
            node.setUsageTimeout(19);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void setUsageTimeout_aboveUpperBound_rejected() {
        try {
            node.setUsageTimeout(36);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
            // ok
        }
    }

    // ---------- bm-3 C7: ReceivedUnwantedFrame IDLE-state cases ----------

    // Case (a): DestinationAddress is not TS and not broadcast.
    @Test
    public void idle_unwantedFrame_case_a_wrongDestination() {
        prepareFrame(FrameType.token, (byte) 0x42, (byte) 0x03);
        node.idle();

        assertEquals(ManagerNode.ManagerNodeState.idle, node.state);
        assertFalse(node.receivedValidFrame);
        assertNull(node.receivedInvalidFrame);
        assertTrue("no frame should have been sent", node.sentFrames.isEmpty());
    }

    // Case (b): DestinationAddress is broadcast and FrameType is Token, Test_Request, or a
    // proprietary type that expects a reply (BACnetDataExpectingReply).
    @Test
    public void idle_unwantedFrame_case_b_broadcastToken() {
        prepareFrame(FrameType.token, Constants.BROADCAST, (byte) 0x03);
        node.idle();
        assertUnwanted();
    }

    @Test
    public void idle_unwantedFrame_case_b_broadcastTestRequest() {
        prepareFrame(FrameType.testRequest, Constants.BROADCAST, (byte) 0x03);
        node.idle();
        assertUnwanted();
    }

    @Test
    public void idle_unwantedFrame_case_b_broadcastDataExpectingReply() {
        prepareFrame(FrameType.bacnetDataExpectingReply, Constants.BROADCAST, (byte) 0x03);
        node.idle();
        assertUnwanted();
    }

    // Case (c): FrameType is not known to this node (unknown byte outside 0..7).
    @Test
    public void idle_unwantedFrame_case_c_unknownFrameType() {
        node.frame.reset();
        node.frame.setFrameType(FrameType.forId((byte) 0x20));
        node.frame.setDestinationAddress(TS);
        node.frame.setSourceAddress((byte) 0x03);
        node.receivedValidFrame = true;
        node.idle();
        assertUnwanted();
    }

    // Case (d): DestinationAddress equals TS and FrameType is Reply To Poll For Manager or
    // Reply Postponed.
    @Test
    public void idle_unwantedFrame_case_d_replyToPollForManager() {
        prepareFrame(FrameType.replyToPollForManager, TS, (byte) 0x03);
        node.idle();
        assertUnwanted();
    }

    @Test
    public void idle_unwantedFrame_case_d_replyPostponed() {
        prepareFrame(FrameType.replyPostponed, TS, (byte) 0x03);
        node.idle();
        assertUnwanted();
    }

    // ---------- helpers ----------

    private void prepareFrame(final FrameType type, final byte destination, final byte source) {
        node.frame.reset();
        node.frame.setFrameType(type);
        node.frame.setDestinationAddress(destination);
        node.frame.setSourceAddress(source);
        node.receivedValidFrame = true;
    }

    private void assertUnwanted() {
        assertEquals("state must remain IDLE", ManagerNode.ManagerNodeState.idle, node.state);
        assertFalse("receivedValidFrame must be cleared", node.receivedValidFrame);
        assertNull("no invalid-frame flag should be set", node.receivedInvalidFrame);
        assertTrue("no frame should have been sent", node.sentFrames.isEmpty());
    }

    private static class RecordingManagerNode extends ManagerNode {
        final List<Frame> sentFrames = new ArrayList<>();

        RecordingManagerNode() {
            super("test", new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), TS, 1);
            this.clock = Clock.systemUTC();
            this.lastNonSilence = this.clock.millis();
        }

        @Override
        protected void sendFrame(final Frame frame) {
            sentFrames.add(frame.copy());
        }
    }
}
