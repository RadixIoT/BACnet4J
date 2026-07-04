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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.SerialPortWrapper;

public class ManagerNode extends MstpNode {
    static final Logger LOG = LoggerFactory.getLogger(ManagerNode.class);


    protected enum ManagerNodeState {
        idle, useToken, waitForReply, doneWithToken, passToken, noToken, pollForManager, answerDataRequest
    }


    private final List<Frame> framesToSend = new ArrayList<>();

    /**
     * The MAC address of the node to which This Station passes the token. If the Next
     * Station is unknown, NS shall be equal to TS.
     */
    protected byte nextStation;

    /**
     * The MAC address of the node to which This Station last sent a Poll For Manager. This is
     * used during token maintenance.
     */
    protected byte pollStation;

    /**
     * A counter of transmission retries used for Token and Poll For Manager transmission.
     */
    protected int retryCount;

    /**
     * A Boolean flag set to TRUE by the manager machine if this node is the only known manager node.
     */
    protected boolean soleManager;

    /**
     * The number of tokens received by this node. When this counter reaches the value Npoll, the node
     * polls the address range between TS and NS for additional manager nodes. TokenCount is set to one at
     * the end of the polling process.
     */
    protected int tokenCount;

    protected int maxManager = Constants.MAX_MANAGER;

    protected int maxInfoFrames = Constants.MAX_INFO_FRAMES;

    protected int usageTimeout = Constants.USAGE_TIMEOUT;

    protected ManagerNodeState state;

    protected long replyDeadline;
    protected Frame replyFrame;

    /**
     * Set to true the first time this node has received a token, indicating that it has joined the network.
     */
    private boolean receivedToken;

    public ManagerNode(SerialPortWrapper wrapper, byte thisStation, int retryCount) throws IllegalArgumentException {
        super(wrapper, thisStation);
        validate(retryCount);
    }

    public ManagerNode(String portId, InputStream in, OutputStream out, byte thisStation, int retryCount)
            throws IllegalArgumentException {
        super(portId, in, out, thisStation);
        validate(retryCount);
    }

    protected void validate(int retryCount) {
        int is = thisStation & 0xff;
        if (is > 127)
            throw new IllegalArgumentException("thisStation cannot be greater than 127");

        this.retryCount = retryCount;

        nextStation = thisStation;
        pollStation = thisStation;
        tokenCount = Constants.POLL;
        soleManager = false;
        state = ManagerNodeState.idle;
    }

    public void setMaxManager(int maxManager) {
        if (maxManager > Constants.MAX_MANAGER)
            throw new IllegalArgumentException("Cannot be greater than " + Constants.MAX_MANAGER);
        this.maxManager = maxManager;
    }

    /**
     * @param maxInfoFrames the maxInfoFrames to set
     */
    public void setMaxInfoFrames(int maxInfoFrames) {
        if (maxInfoFrames < 1)
            throw new IllegalArgumentException("Cannot be less than 1");
        this.maxInfoFrames = maxInfoFrames;
    }

    public void setUsageTimeout(int usageTimeout) {
        if (usageTimeout < 20) {
            throw new IllegalArgumentException("Cannot be less than 20");
        }
        if (usageTimeout > 35) {
            throw new IllegalArgumentException("Cannot be greater than 35");
        }
        this.usageTimeout = usageTimeout;
    }

    public boolean hasReceivedToken() {
        return receivedToken;
    }

    @Override
    public void initialize(Transport transport) throws Exception {
        super.initialize(transport);

        transport.getLocalDevice().getDeviceObject().writePropertyInternal(PropertyIdentifier.maxManager,
                new UnsignedInteger(maxManager));
        transport.getLocalDevice().getDeviceObject().writePropertyInternal(PropertyIdentifier.maxInfoFrames,
                new UnsignedInteger(maxInfoFrames));
    }

    public void queueFrame(FrameType type, byte destination, byte[] data) {
        if (!type.oneOf(FrameType.bacnetDataExpectingReply, FrameType.bacnetDataNotExpectingReply,
                FrameType.testRequest))
            throw new RuntimeException("Cannot send frame of type: " + type);

        Frame frame = new Frame(type, destination, thisStation, data);
        synchronized (framesToSend) {
            framesToSend.add(frame);
        }
    }

    @Override
    public void setReplyFrame(FrameType type, byte destination, byte[] data) {
        synchronized (this) {
            if (state == ManagerNodeState.answerDataRequest)
                // If there is still time to reply immediately...
                replyFrame = new Frame(type, destination, thisStation, data);
            else
                // The response has already exceeded the timeout, so just queue it.
                queueFrame(type, destination, data);
        }
    }

    @Override
    protected void doCycle() {
        readFrame();

        if (state == ManagerNodeState.idle)
            idle();

        if (state == ManagerNodeState.useToken)
            useToken();

        if (state == ManagerNodeState.waitForReply)
            waitForReply();

        if (state == ManagerNodeState.doneWithToken)
            doneWithToken();

        if (state == ManagerNodeState.passToken)
            passToken();

        if (state == ManagerNodeState.noToken)
            noToken();

        if (state == ManagerNodeState.pollForManager)
            pollForManager();

        if (state == ManagerNodeState.answerDataRequest)
            answerDataRequest();
    }

    protected void idle() {
        if (silence() >= Constants.NO_TOKEN) {
            // LostToken
            if (LOG.isDebugEnabled()) {
                LOG.debug("idle:LostToken");
            }
            state = ManagerNodeState.noToken;
            activity = true;
        } else if (receivedInvalidFrame != null) {
            // ReceivedInvalidFrame
            LOG.debug("idle:Received invalid frame: {}", receivedInvalidFrame);
            receivedInvalidFrame = null;
            activity = true;
        } else if (receivedValidFrame) {
            frame();
            receivedValidFrame = false;
            activity = true;
            if (LOG.isDebugEnabled()) {
                LOG.debug("idle:other");
            }
        }
    }

    protected void frame() {
        FrameType type = frame.getFrameType();

        if (type == null) {
            // ReceivedUnwantedFrame (c): FrameType has a value that is not known to this node.
            LOG.debug("{} idle:ReceivedUnwantedFrame Unknown frame type", thisStation);
        } else if (frame.broadcast()
                && type.oneOf(FrameType.token, FrameType.testRequest, FrameType.bacnetDataExpectingReply)) {
            // ReceivedUnwantedFrame (b): DestinationAddress is broadcast and FrameType is Token,
            // Test_Request, or a proprietary type that expects a reply.
            LOG.debug("{} idle:ReceivedUnwantedFrame Frame type should not be broadcast: {}", thisStation, type);
        } else if (frame.forStation(thisStation)
                && type.oneOf(FrameType.replyToPollForManager, FrameType.replyPostponed)) {
            // ReceivedUnwantedFrame (d): DestinationAddress equals TS and FrameType is
            // Reply To Poll For Manager or Reply Postponed.
            LOG.debug("{} idle:ReceivedUnwantedFrame Unexpected reply frame: {}", thisStation, type);
        } else if (frame.forStation(thisStation) && type == FrameType.token) {
            // ReceivedToken
            receivedToken = true;
            LOG.debug("idle:ReceivedToken ({})", frame.getSourceAddress());
            frameCount = 0;
            soleManager = false;
            state = ManagerNodeState.useToken;
        } else if (frame.forStation(thisStation) && type == FrameType.pollForManager) {
            // ReceivedPFM
            LOG.debug("idle:ReceivedPFM ({})", frame.getSourceAddress());
            sendFrame(FrameType.replyToPollForManager, frame.getSourceAddress());
        } else if (frame.forStationOrBroadcast(thisStation)
                && type.oneOf(FrameType.bacnetDataNotExpectingReply, FrameType.testResponse)) {
            // ReceivedDataNoReply
            LOG.debug("idle:ReceivedDataNoReply ({})", frame.getSourceAddress());
            receivedDataNoReply(frame);
        } else if (frame.forStation(thisStation)
                && type.oneOf(FrameType.bacnetDataExpectingReply, FrameType.testRequest)) {
            // ReceivedDataNeedingReply
            LOG.debug("idle:ReceivedDataNeedingReply ({})", frame.getSourceAddress());
            receivedDataNeedingReply(frame);
            state = ManagerNodeState.answerDataRequest;
            replyDeadline = lastNonSilence + Constants.REPLY_DELAY;
        } else {
            // ReceivedUnwantedFrame (a): DestinationAddress is neither TS nor broadcast, or any
            // other frame not addressed to this node in a way this state handles.
            LOG.debug("{} idle:ReceivedUnwantedFrame", thisStation);
        }
    }

    protected void useToken() {
        Frame frameToSend = null;
        synchronized (framesToSend) {
            if (!framesToSend.isEmpty()) {
                frameToSend = framesToSend.remove(0);
            }
        }

        if (frameToSend == null) {
            // NothingToSend
            frameCount = maxInfoFrames;
            state = ManagerNodeState.doneWithToken;
        } else {
            activity = true;
            if (frameToSend.getFrameType().oneOf(FrameType.testResponse, FrameType.bacnetDataNotExpectingReply)) {
                // SendNoWait
                LOG.debug("useToken:SendNoWait ({})", frameToSend.getDestinationAddress());
                state = ManagerNodeState.doneWithToken;
            } else if (frameToSend.getFrameType().oneOf(FrameType.testRequest, FrameType.bacnetDataExpectingReply)) {
                // SendAndWait
                LOG.debug("useToken:SendAndWait ({})", frameToSend.getDestinationAddress());
                state = ManagerNodeState.waitForReply;
            } else {
                throw new RuntimeException("Unhandled frame type: " + frameToSend.getFrameType());
            }

            sendFrame(frameToSend);
            frameCount++;
        }
    }

    protected void waitForReply() {
        if (silence() > Constants.REPLY_TIMEOUT) {
            // ReplyTimeout - assume that the request has failed
            LOG.debug("waitForReply:ReplyTimeout");
            frameCount = maxInfoFrames;
            state = ManagerNodeState.doneWithToken;
        } else if (receivedInvalidFrame != null) {
            // InvalidFrame
            LOG.debug("waitForReply:InvalidFrame: {} ({})", receivedInvalidFrame, frame.getSourceAddress());
            receivedInvalidFrame = null;
            state = ManagerNodeState.doneWithToken;
            activity = true;
        } else if (receivedValidFrame) {
            activity = true;
            FrameType type = frame.getFrameType();

            if (frame.forStation(thisStation)) {
                if (type.oneOf(FrameType.testResponse, FrameType.bacnetDataNotExpectingReply)) {
                    // ReceivedReply
                    LOG.debug("{} waitForReply:ReceivedReply ({})", thisStation, frame.getSourceAddress());
                    receivedDataNoReply(frame);
                } else if (type.oneOf(FrameType.replyPostponed)) {
                    // ReceivedPostpone
                    LOG.debug("{} waitForReply:ReceivedPostpone ({})", thisStation, frame.getSourceAddress());
                    // The reply to the message has been postponed until a later time.
                }

                state = ManagerNodeState.doneWithToken;
            } else if (!type.oneOf(FrameType.testResponse, FrameType.bacnetDataNotExpectingReply)) {
                // ReceivedUnexpectedFrame
                LOG.debug("{} waitForReply:ReceivedUnexpectedFrame ({})", thisStation, frame.getSourceAddress());

                // This may indicate the presence of multiple tokens.
                state = ManagerNodeState.idle;
            }

            receivedValidFrame = false;
        }
    }

    /**
     * The DONE_WITH_TOKEN state either sends another data frame, passes the token, or initiates a Poll For Manager
     * cycle.
     */
    private void doneWithToken() {
        activity = true;
        if (frameCount < maxInfoFrames) {
            // SendAnotherFrame
            LOG.debug("{} doneWithToken:SendAnotherFrame", thisStation);
            state = ManagerNodeState.useToken;
        } else if (!soleManager && nextStation == thisStation) {
            // NextStationUnknown
            pollStation = adjacentStation(thisStation);
            LOG.debug("{} doneWithToken:NextStationUnknown [{}]", thisStation, pollStation);
            sendFrame(FrameType.pollForManager, pollStation);
            retryCount = 0;
            state = ManagerNodeState.pollForManager;
        } else if (tokenCount < Constants.POLL - 1 && soleManager) {
            // SoleManager
            LOG.debug("{} doneWithToken:SoleManager", thisStation);
            frameCount = 0;
            tokenCount++;
            state = ManagerNodeState.useToken;
        } else if (tokenCount < Constants.POLL - 1 && !soleManager || nextStation == adjacentStation(thisStation)) {
            // SendToken
            LOG.debug("{} doneWithToken:SendToken [{}]", thisStation, nextStation);
            tokenCount++;
            sendFrame(FrameType.token, nextStation);
            retryCount = 0;
            eventCount = 0;
            state = ManagerNodeState.passToken;
        } else if (tokenCount >= Constants.POLL - 1 && adjacentStation(pollStation) != nextStation) {
            // SendMaintenancePFM
            pollStation = adjacentStation(pollStation);
            LOG.debug("{} doneWithToken:SendMaintenancePFM [{}]", thisStation, pollStation);
            sendFrame(FrameType.pollForManager, pollStation);
            retryCount = 0;
            state = ManagerNodeState.pollForManager;
        } else if (tokenCount >= Constants.POLL - 1 && adjacentStation(pollStation) == nextStation && !soleManager) {
            // ResetMaintenancePFM
            LOG.debug("{} doneWithToken:ResetMaintenancePFM [{}]", thisStation, nextStation);
            pollStation = thisStation;
            sendFrame(FrameType.token, nextStation);
            retryCount = 0;
            eventCount = 0;
            tokenCount = 1;
            state = ManagerNodeState.passToken;
        } else if (tokenCount >= Constants.POLL - 1 && adjacentStation(pollStation) == nextStation && soleManager) {
            // SoleManagerRestartMaintenancePFM
            pollStation = adjacentStation(nextStation);
            LOG.debug("{} doneWithToken:SoleManagerRestartMaintenancePFM [{}]", thisStation, pollStation);
            sendFrame(FrameType.pollForManager, pollStation);
            nextStation = thisStation;
            retryCount = 0;
            eventCount = 0;
            tokenCount = 1;
            state = ManagerNodeState.pollForManager;
        }
    }

    /**
     * The PASS_TOKEN state listens for a successor to begin using the token that this node has just attempted to pass.
     */
    private void passToken() {
        activity = true;
        if (silence() < usageTimeout && eventCount > Constants.MIN_OCTETS) {
            // SawTokenUser
            LOG.debug("{} passToken:SawTokenUser", thisStation);
            state = ManagerNodeState.idle;
        } else if (silence() >= usageTimeout && retryCount < Constants.RETRY_TOKEN) {
            // RetrySendToken
            LOG.debug("{} passToken:RetrySendToken [{}]", thisStation, nextStation);
            retryCount++;
            sendFrame(FrameType.token, nextStation);
            eventCount = 0;
        } else if (silence() >= usageTimeout && retryCount >= Constants.RETRY_TOKEN) {
            // FindNewSuccessor
            pollStation = adjacentStation(nextStation);
            LOG.debug("{} passToken:FindNewSuccessor trying [{}]", thisStation, pollStation);
            sendFrame(FrameType.pollForManager, pollStation);
            nextStation = thisStation;
            retryCount = 0;
            tokenCount = 0;
            eventCount = 0;
            state = ManagerNodeState.pollForManager;
        }
    }

    /**
     * The NO_TOKEN state is entered if SilenceTimer becomes greater than Tno_token, indicating that there has been no
     * network activity for that period of time. The timeout is continued to determine whether or not this node may
     * create a token.
     */
    private void noToken() {
        long silence = silence();
        long delay = Constants.NO_TOKEN + Constants.SLOT * (thisStation & 0xffL);
        if (silence < delay && eventCount > Constants.MIN_OCTETS) {
            // SawFrame
            LOG.debug("{} noToken:SawFrame", thisStation);
            state = ManagerNodeState.idle;
            activity = true;
        } else if (silence >= delay && silence < delay + Constants.SLOT // Silence is in this manager's slot.
                // Silence is beyond all slots.
                || silence > Constants.NO_TOKEN + Constants.SLOT * (maxManager + 1L)) {
            // GenerateToken
            pollStation = adjacentStation(thisStation);
            LOG.debug("{} noToken:GenerateToken [{}]", thisStation, pollStation);
            sendFrame(FrameType.pollForManager, pollStation);
            nextStation = thisStation;
            tokenCount = 0;
            retryCount = 0;
            eventCount = 0;
            state = ManagerNodeState.pollForManager;
            activity = true;
        }
    }

    /**
     * In the POLL_FOR_MANAGER state, the node listens for a reply to a previously sent Poll For Manager frame in order to
     * find a successor node.
     */
    private void pollForManager() {
        if (receivedValidFrame) {
            if (frame.forStation(thisStation) && frame.getFrameType() == FrameType.replyToPollForManager) {
                // ReceivedReplyToPFM
                LOG.debug("{} pollForManager:ReceivedReplyToPFM ({})", thisStation, frame.getSourceAddress());
                soleManager = false;
                nextStation = frame.getSourceAddress();
                eventCount = 0;
                sendFrame(FrameType.token, nextStation);
                pollStation = thisStation;
                tokenCount = 0;
                retryCount = 0;
                receivedValidFrame = false;
                state = ManagerNodeState.passToken;
            } else {
                // ReceivedUnexpectedFrame
                LOG.debug("{} pollForManager:ReceivedUnexpectedFrame ({})", thisStation, frame.getSourceAddress());
                receivedValidFrame = false;
                state = ManagerNodeState.idle;
            }
            activity = true;
        } else if (soleManager && (silence() >= usageTimeout || receivedInvalidFrame != null)) {
            // SoleManager
            LOG.debug("{} pollForManager:SoleManager", thisStation);
            frameCount = 0;
            receivedInvalidFrame = null;
            state = ManagerNodeState.useToken;
            activity = true;
        } else if (!soleManager) {
            boolean longCondition = silence() >= usageTimeout || receivedInvalidFrame != null;
            if (nextStation != thisStation && longCondition) {
                // DoneWithPFM
                LOG.debug("{} pollForManager:DoneWithPFM [{}]", thisStation, nextStation);
                eventCount = 0;
                sendFrame(FrameType.token, nextStation);
                retryCount = 0;
                receivedInvalidFrame = null;
                state = ManagerNodeState.passToken;
                activity = true;
            } else if (nextStation == thisStation) {
                if (adjacentStation(pollStation) != thisStation && longCondition) {
                    // SendNextPFM
                    pollStation = adjacentStation(pollStation);
                    LOG.debug("{} pollForManager:SendNextPFM [{}]", thisStation, pollStation);
                    sendFrame(FrameType.pollForManager, pollStation);
                    retryCount = 0;
                    receivedInvalidFrame = null;
                    activity = true;
                } else if (adjacentStation(pollStation) == thisStation && longCondition) {
                    // DeclareSoleManager
                    receivedToken = true;
                    LOG.debug("{} pollForManager:DeclareSoleManager", thisStation);
                    soleManager = true;
                    frameCount = 0;
                    receivedInvalidFrame = null;
                    state = ManagerNodeState.useToken;
                    activity = true;
                }
            }
        }
    }

    /**
     * The ANSWER_DATA_REQUEST state is entered when a BACnet Data Expecting Reply, a Test_Request, or a proprietary
     * frame that expects a reply is received.
     */
    protected void answerDataRequest() {
        synchronized (this) {
            if (replyFrame != null) {
                // Reply
                LOG.debug("{} answerDataRequest:Reply", thisStation);
                sendFrame(replyFrame);
                replyFrame = null;
                state = ManagerNodeState.idle;
                activity = true;
            } else {
                long now = clock.millis();
                if (replyDeadline < now) {
                    // DeferredReply
                    LOG.debug("{} answerDataRequest:DeferredReply ({})", thisStation, frame.getSourceAddress());
                    sendFrame(FrameType.replyPostponed, frame.getSourceAddress());
                    state = ManagerNodeState.idle;
                    activity = true;
                } else {
                    // If the current time of the host was moved back, the above condition could cause an indefinite
                    // wait. So, we check if the reply deadline is too long, and correct if so.
                    long timeDiff = replyDeadline - now;
                    if (timeDiff > Constants.REPLY_DELAY) {
                        LOG.warn("Correcting replyDeadline time because of timeDiff of {}", timeDiff);
                        replyDeadline = now + Constants.REPLY_DELAY;
                    }
                }
            }
        }
    }

    private byte adjacentStation(byte station) {
        int i = station & 0xff;
        i = (i + 1) % (maxManager + 1);
        return (byte) i;
    }
}
