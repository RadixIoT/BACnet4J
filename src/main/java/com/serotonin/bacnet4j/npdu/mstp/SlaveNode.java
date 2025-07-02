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

import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.util.sero.SerialPortWrapper;

public class SlaveNode extends MstpNode {
    private static final Logger LOG = LoggerFactory.getLogger(SlaveNode.class);


    private enum SlaveNodeState {
        idle, answerDataRequest
    }


    private SlaveNodeState state;

    private long replyDeadline;
    private Frame replyFrame;

    public SlaveNode(final SerialPortWrapper wrapper, final byte thisStation) throws IllegalArgumentException {
        super(wrapper, thisStation);
        validate();
    }

    public SlaveNode(final String portId, final InputStream in, final OutputStream out, final byte thisStation)
            throws IllegalArgumentException {
        super(portId, in, out, thisStation);
        validate();
    }

    private void validate() {
        final int is = thisStation & 0xff;
        if (is > 254)
            throw new IllegalArgumentException("thisStation cannot be greater than 254");

        state = SlaveNodeState.idle;
    }

    @Override
    public void setReplyFrame(final FrameType type, final byte destination, final byte[] data) {
        synchronized (this) {
            if (state == SlaveNodeState.answerDataRequest)
                // If there is still time to reply immediately...
                replyFrame = new Frame(type, frame.getSourceAddress(), thisStation, data);
        }
    }

    @Override
    protected void doCycle() {
        readFrame();

        if (state == SlaveNodeState.idle)
            idle();

        if (state == SlaveNodeState.answerDataRequest)
            answerDataRequest();
    }

    /**
     * In the IDLE state, the node waits for a frame.
     */
    private void idle() {
        if (receivedInvalidFrame != null) {
            // ReceivedInvalidFrame
            // debug("idle:ReceivedInvalidFrame");
            if (LOG.isDebugEnabled())
                LOG.debug("Received invalid frame: " + receivedInvalidFrame);
            receivedInvalidFrame = null;
            activity = true;
        } else if (receivedValidFrame) {
            final FrameType type = frame.getFrameType();

            if (type == null) {
                // ReceivedUnwantedFrame
                if (LOG.isDebugEnabled())
                    LOG.debug("Unknown frame type");
            } else if (frame.broadcast()
                    && type.oneOf(FrameType.token, FrameType.bacnetDataExpectingReply, FrameType.testRequest)) {
                // ReceivedUnwantedFrame
                if (LOG.isDebugEnabled())
                    LOG.debug("Frame type should not be broadcast: " + type);
            } else if (type.oneOf(FrameType.pollForMaster)) {
                // ReceivedUnwantedFrame
                // It happens
            } else if (type.oneOf(FrameType.token, FrameType.pollForMaster, FrameType.replyToPollForMaster,
                    FrameType.replyPostponed)) {
                // ReceivedUnwantedFrame
                if (LOG.isDebugEnabled())
                    LOG.debug("Received unwanted frame type: " + type);
            } else if (frame.forStationOrBroadcast(thisStation)
                    && type.oneOf(FrameType.bacnetDataNotExpectingReply, FrameType.testResponse)) {
                // ReceivedDataNoReply
                //                debug("idle:ReceivedDataNoReply");
                receivedDataNoReply(frame);
            } else if (frame.forStation(thisStation)
                    && type.oneOf(FrameType.bacnetDataExpectingReply, FrameType.testRequest)) {
                // ReceivedDataNeedingReply
                //                debug("idle:ReceivedDataNeedingReply");
                state = SlaveNodeState.answerDataRequest;
                replyDeadline = lastNonSilence + Constants.REPLY_DELAY;
                replyFrame = null;
                receivedDataNeedingReply(frame);
            }

            receivedValidFrame = false;
            activity = true;
        }
    }

    /**
     * The ANSWER_DATA_REQUEST state is entered when a BACnet Data Expecting Reply, a Test_Request, or a proprietary
     * frame that expects a reply is received.
     */
    private void answerDataRequest() {
        synchronized (this) {
            if (replyFrame != null) {
                // Reply
                //                debug("answerDataRequest:Reply");
                sendFrame(replyFrame);
                replyFrame = null;
                state = SlaveNodeState.idle;
                activity = true;
            } else if (replyDeadline >= clock.millis()) {
                // CannotReply
                //                debug("answerDataRequest:CannotReply");
                if (LOG.isDebugEnabled())
                    LOG.debug("Failed to respond to request: " + frame);
                state = SlaveNodeState.idle;
                activity = true;
            }
        }
    }
}
