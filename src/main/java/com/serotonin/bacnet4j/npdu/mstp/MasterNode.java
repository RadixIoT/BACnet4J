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

public class MasterNode extends MstpNode {
    static final Logger LOG = LoggerFactory.getLogger(MasterNode.class);


    protected enum MasterNodeState {
        idle, useToken, waitForReply, doneWithToken, passToken, noToken, pollForMaster, answerDataRequest
    }


    private final List<Frame> framesToSend = new ArrayList<>();

    /**
     * The MAC address of the node to which This Station passes the token. If the Next
     * Station is unknown, NS shall be equal to TS.
     */
    protected byte nextStation;

    /**
     * The MAC address of the node to which This Station last sent a Poll For Master. This is
     * used during token maintenance.
     */
    protected byte pollStation;

    /**
     * A counter of transmission retries used for Token and Poll For Master transmission.
     */
    protected int retryCount;

    /**
     * A Boolean flag set to TRUE by the master machine if this node is the only known master node.
     */
    protected boolean soleMaster;

    /**
     * The number of tokens received by this node. When this counter reaches the value Npoll, the node
     * polls the address range between TS and NS for additional master nodes. TokenCount is set to one at
     * the end of the polling process.
     */
    protected int tokenCount;

    protected int maxMaster = Constants.MAX_MASTER;

    protected int maxInfoFrames = Constants.MAX_INFO_FRAMES;

    protected int usageTimeout = Constants.USAGE_TIMEOUT;

    protected MasterNodeState state;

    protected long replyDeadline;
    protected Frame replyFrame;

    /**
     * Set to true the first time this node has received a token, indicating that it has joined the network.
     */
    private boolean receivedToken;

    //    private long lastTokenPossession;

    public MasterNode(final SerialPortWrapper wrapper, final byte thisStation, final int retryCount)
            throws IllegalArgumentException {
        super(wrapper, thisStation);
        validate(retryCount);
    }

    public MasterNode(final String portId, final InputStream in, final OutputStream out, final byte thisStation,
            final int retryCount) throws IllegalArgumentException {
        super(portId, in, out, thisStation);
        validate(retryCount);
    }

    protected void validate(final int retryCount) {
        final int is = thisStation & 0xff;
        if (is > 127)
            throw new IllegalArgumentException("thisStation cannot be greater than 127");

        this.retryCount = retryCount;

        nextStation = thisStation;
        pollStation = thisStation;
        tokenCount = Constants.POLL;
        soleMaster = false;
        state = MasterNodeState.idle;
    }

    public void setMaxMaster(final int maxMaster) {
        if (maxMaster > Constants.MAX_MASTER)
            throw new IllegalArgumentException("Cannot be greater than " + Constants.MAX_MASTER);
        this.maxMaster = maxMaster;
    }

    /**
     * @param maxInfoFrames the maxInfoFrames to set
     */
    public void setMaxInfoFrames(final int maxInfoFrames) {
        if (maxInfoFrames < 1)
            throw new IllegalArgumentException("Cannot be less than 1");
        this.maxInfoFrames = maxInfoFrames;
    }

    public void setUsageTimeout(final int usageTimeout) {
        if (usageTimeout < 20) {
            throw new IllegalArgumentException("Cannot be less than 20");
        }
        if (usageTimeout > 100) {
            throw new IllegalArgumentException("Cannot be greater than 100");
        }
        this.usageTimeout = usageTimeout;
    }

    public boolean hasReceivedToken() {
        return receivedToken;
    }

    @Override
    public void initialize(final Transport transport) throws Exception {
        super.initialize(transport);

        transport.getLocalDevice().getDeviceObject().writePropertyInternal(PropertyIdentifier.maxMaster,
                new UnsignedInteger(maxMaster));
        transport.getLocalDevice().getDeviceObject().writePropertyInternal(PropertyIdentifier.maxInfoFrames,
                new UnsignedInteger(maxInfoFrames));
    }

    public void queueFrame(final FrameType type, final byte destination, final byte[] data) {
        if (!type.oneOf(FrameType.bacnetDataExpectingReply, FrameType.bacnetDataNotExpectingReply,
                FrameType.testRequest))
            throw new RuntimeException("Cannot send frame of type: " + type);

        final Frame frame = new Frame(type, destination, thisStation, data);
        synchronized (framesToSend) {
            framesToSend.add(frame);
        }
    }

    @Override
    public void setReplyFrame(final FrameType type, final byte destination, final byte[] data) {
        synchronized (this) {
            if (state == MasterNodeState.answerDataRequest)
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

        if (state == MasterNodeState.idle)
            idle();

        if (state == MasterNodeState.useToken)
            useToken();

        if (state == MasterNodeState.waitForReply)
            waitForReply();

        if (state == MasterNodeState.doneWithToken)
            doneWithToken();

        if (state == MasterNodeState.passToken)
            passToken();

        if (state == MasterNodeState.noToken)
            noToken();

        if (state == MasterNodeState.pollForMaster)
            pollForMaster();

        if (state == MasterNodeState.answerDataRequest)
            answerDataRequest();
    }

    protected void idle() {
        if (silence() >= Constants.NO_TOKEN) {
            // LostToken
            if (LOG.isDebugEnabled()) {
                LOG.debug("idle:LostToken");
            }
            state = MasterNodeState.noToken;
            activity = true;
        } else if (receivedInvalidFrame != null) {
            // ReceivedInvalidFrame
            if (LOG.isDebugEnabled()) {
                LOG.debug("idle:Received invalid frame: " + receivedInvalidFrame);
            }
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
        final FrameType type = frame.getFrameType();

        if (type == null) {
            // ReceivedUnwantedFrame
            if (LOG.isDebugEnabled())
                LOG.debug(thisStation + " idle:Unknown frame type");
        } else if (frame.broadcast()
                && type.oneOf(FrameType.token, FrameType.bacnetDataExpectingReply, FrameType.testRequest)) {
            // ReceivedUnwantedFrame
            if (LOG.isDebugEnabled())
                LOG.debug("idle:ReceiveUnwantedFrame Frame type should not be broadcast: " + type);
        } else if (frame.forStation(thisStation) && type == FrameType.token) {
            // ReceivedToken
            receivedToken = true;
            if (LOG.isDebugEnabled()) {
                LOG.debug("idle:ReceivedToken (" + frame.getSourceAddress() + ")");
            }
            frameCount = 0;
            soleMaster = false;
            state = MasterNodeState.useToken;
            //
            //            long now = clock.millis();
            //            if (lastTokenPossession > 0)
            //                debug("Since last token possession: " + (now - lastTokenPossession) + " ms");
            //            else
            //                debug("Received first token possession");
            //            lastTokenPossession = now;
        } else if (frame.forStation(thisStation) && type == FrameType.pollForMaster) {
            // ReceivedPFM
            if (LOG.isDebugEnabled()) {
                LOG.debug("idle:ReceivedPFM (" + frame.getSourceAddress() + ")");
            }
            sendFrame(FrameType.replyToPollForMaster, frame.getSourceAddress());
        } else if (frame.forStationOrBroadcast(thisStation)
                && type.oneOf(FrameType.bacnetDataNotExpectingReply, FrameType.testResponse)) {
            // ReceivedDataNoReply
            if (LOG.isDebugEnabled()) {
                LOG.debug("idle:ReceivedDataNoReply (" + frame.getSourceAddress() + ")");
            }
            receivedDataNoReply(frame);
        } else if (frame.forStation(thisStation)
                && type.oneOf(FrameType.bacnetDataExpectingReply, FrameType.testRequest)) {
            // ReceivedDataNeedingReply
            if (LOG.isDebugEnabled()) {
                LOG.debug("idle:ReceivedDataNeedingReply (" + frame.getSourceAddress() + ")");
            }
            receivedDataNeedingReply(frame);
            state = MasterNodeState.answerDataRequest;
            replyDeadline = lastNonSilence + Constants.REPLY_DELAY;
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug(thisStation + " idle:frame-other");
        }
    }

    protected void useToken() {
        Frame frameToSend = null;
        synchronized (framesToSend) {
            if (!framesToSend.isEmpty())
                frameToSend = framesToSend.remove(0);
        }

        if (frameToSend == null) {
            // NothingToSend
            frameCount = maxInfoFrames;
            state = MasterNodeState.doneWithToken;
        } else {
            activity = true;
            if (frameToSend.getFrameType().oneOf(FrameType.testResponse, FrameType.bacnetDataNotExpectingReply)) {
                // SendNoWait
                if (LOG.isDebugEnabled()) {
                    LOG.debug("useToken:SendNoWait (" + frameToSend.getDestinationAddress() + ")");
                }
                state = MasterNodeState.doneWithToken;
            } else if (frameToSend.getFrameType().oneOf(FrameType.testRequest, FrameType.bacnetDataExpectingReply)) {
                // SendAndWait
                if (LOG.isDebugEnabled()) {
                    LOG.debug("useToken:SendAndWait (" + frameToSend.getDestinationAddress() + ")");
                }
                state = MasterNodeState.waitForReply;
            } else
                throw new RuntimeException("Unhandled frame type: " + frameToSend.getFrameType());

            sendFrame(frameToSend);
            frameCount++;
        }
    }

    protected void waitForReply() {
        if (silence() > Constants.REPLY_TIMEOUT) {
            // ReplyTimeout - assume that the request has failed
            if (LOG.isDebugEnabled()) {
                LOG.debug("waitForReply:ReplyTimeout");
            }
            frameCount = maxInfoFrames;
            state = MasterNodeState.doneWithToken;
        } else if (receivedInvalidFrame != null) {
            // InvalidFrame
            if (LOG.isDebugEnabled()) {
                LOG.debug("waitForReply:InvalidFrame: " + receivedInvalidFrame + "(" + frame.getSourceAddress() + ")");
            }
            receivedInvalidFrame = null;
            state = MasterNodeState.doneWithToken;
            activity = true;
        } else if (receivedValidFrame) {
            activity = true;
            final FrameType type = frame.getFrameType();

            if (frame.forStation(thisStation)) {
                if (type.oneOf(FrameType.testResponse, FrameType.bacnetDataNotExpectingReply)) {
                    // ReceivedReply
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(thisStation + " waitForReply:ReceivedReply (" + frame.getSourceAddress() + ")");
                    }
                    receivedDataNoReply(frame);
                } else if (type.oneOf(FrameType.replyPostponed)) {
                    // ReceivedPostpone
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(thisStation + " waitForReply:ReceivedPostpone (" + frame.getSourceAddress() + ")");
                    }
                    // The reply to the message has been postponed until a later time.
                }

                state = MasterNodeState.doneWithToken;
            } else if (!type.oneOf(FrameType.testResponse, FrameType.bacnetDataNotExpectingReply)) {
                // ReceivedUnexpectedFrame
                if (LOG.isDebugEnabled()) {
                    LOG.debug(thisStation + " waitForReply:ReceivedUnexpectedFrame (" + frame.getSourceAddress() + ")");
                }

                // This may indicate the presence of multiple tokens.
                state = MasterNodeState.idle;
            }

            receivedValidFrame = false;
        }
    }

    /**
     * The DONE_WITH_TOKEN state either sends another data frame, passes the token, or initiates a Poll For Master
     * cycle.
     */
    private void doneWithToken() {
        activity = true;
        if (frameCount < maxInfoFrames) {
            // SendAnotherFrame
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " doneWithToken:SendAnotherFrame");
            }
            state = MasterNodeState.useToken;
        } else if (!soleMaster && nextStation == thisStation) {
            // NextStationUnknown
            pollStation = adjacentStation(thisStation);
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " doneWithToken:NextStationUnknown [" + pollStation + "]");
            }
            sendFrame(FrameType.pollForMaster, pollStation);
            retryCount = 0;
            state = MasterNodeState.pollForMaster;
        } else if (tokenCount < Constants.POLL - 1 && soleMaster) {
            // SoleMaster
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " doneWithToken:SoleMaster");
            }
            frameCount = 0;
            tokenCount++;
            state = MasterNodeState.useToken;
        } else if (tokenCount < Constants.POLL - 1 && !soleMaster || nextStation == adjacentStation(thisStation)) {
            // SendToken
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " doneWithToken:SendToken [" + nextStation + "]");
            }
            tokenCount++;
            sendFrame(FrameType.token, nextStation);
            retryCount = 0;
            eventCount = 0;
            state = MasterNodeState.passToken;
        } else if (tokenCount >= Constants.POLL - 1 && adjacentStation(pollStation) != nextStation) {
            // SendMaintenancePFM
            pollStation = adjacentStation(pollStation);
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " doneWithToken:SendMaintenancePFM [" + pollStation + "]");
            }
            sendFrame(FrameType.pollForMaster, pollStation);
            retryCount = 0;
            state = MasterNodeState.pollForMaster;
        } else if (tokenCount >= Constants.POLL - 1 && adjacentStation(pollStation) == nextStation && !soleMaster) {
            // ResetMaintenancePFM
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " doneWithToken:ResetMaintenancePFM [" + nextStation + "]");
            }
            pollStation = thisStation;
            sendFrame(FrameType.token, nextStation);
            retryCount = 0;
            eventCount = 0;
            tokenCount = 1;
            state = MasterNodeState.passToken;
        } else if (tokenCount >= Constants.POLL - 1 && adjacentStation(pollStation) == nextStation && soleMaster) {
            // SoleMasterRestartMaintenancePFM
            pollStation = adjacentStation(nextStation);
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " doneWithToken:SoleMasterRestartMaintenancePFM [" + pollStation + "]");
            }
            sendFrame(FrameType.pollForMaster, pollStation);
            nextStation = thisStation;
            retryCount = 0;
            eventCount = 0;
            tokenCount = 1;
            state = MasterNodeState.pollForMaster;
        }
    }

    /**
     * The PASS_TOKEN state listens for a successor to begin using the token that this node has just attempted to pass.
     */
    private void passToken() {
        activity = true;
        if (silence() < usageTimeout && eventCount > Constants.MIN_OCTETS) {
            // SawTokenUser
            if (LOG.isDebugEnabled())
                LOG.debug(thisStation + " passToken:SawTokenUser");
            state = MasterNodeState.idle;
        } else if (silence() >= usageTimeout && retryCount < Constants.RETRY_TOKEN) {
            // RetrySendToken
            if (LOG.isDebugEnabled())
                LOG.debug(thisStation + " passToken:RetrySendToken [" + nextStation + "]");
            retryCount++;
            sendFrame(FrameType.token, nextStation);
            eventCount = 0;
        } else if (silence() >= usageTimeout && retryCount >= Constants.RETRY_TOKEN) {
            // FindNewSuccessor
            pollStation = adjacentStation(nextStation);
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " passToken:FindNewSuccessor trying [" + pollStation + "]");
            }
            sendFrame(FrameType.pollForMaster, pollStation);
            nextStation = thisStation;
            retryCount = 0;
            tokenCount = 0;
            eventCount = 0;
            state = MasterNodeState.pollForMaster;
        }
    }

    /**
     * The NO_TOKEN state is entered if SilenceTimer becomes greater than Tno_token, indicating that there has been no
     * network activity for that period of time. The timeout is continued to determine whether or not this node may
     * create a token.
     */
    private void noToken() {
        final long silence = silence();
        final long delay = Constants.NO_TOKEN + Constants.SLOT * (thisStation & 0xff);
        if (silence < delay && eventCount > Constants.MIN_OCTETS) {
            // SawFrame
            if (LOG.isDebugEnabled())
                LOG.debug(thisStation + " noToken:SawFrame");
            state = MasterNodeState.idle;
            activity = true;
        } else if (silence >= delay && silence < delay + Constants.SLOT // Silence is in this master's slot.
                // Silence is beyond all slots.
                || silence > Constants.NO_TOKEN + Constants.SLOT * (maxMaster + 1)) {
            // GenerateToken
            pollStation = adjacentStation(thisStation);
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " noToken:GenerateToken [" + pollStation + "]");
            }
            sendFrame(FrameType.pollForMaster, pollStation);
            nextStation = thisStation;
            tokenCount = 0;
            retryCount = 0;
            eventCount = 0;
            state = MasterNodeState.pollForMaster;
            activity = true;
        }
    }

    /**
     * In the POLL_FOR_MASTER state, the node listens for a reply to a previously sent Poll For Master frame in order to
     * find a successor node.
     */
    private void pollForMaster() {
        if (receivedValidFrame) {
            if (frame.forStation(thisStation) && frame.getFrameType() == FrameType.replyToPollForMaster) {
                // ReceivedReplyToPFM
                if (LOG.isDebugEnabled()) {
                    LOG.debug(thisStation + " pollForMaster:ReceivedReplyToPFM (" + frame.getSourceAddress() + ")");
                }
                soleMaster = false;
                nextStation = frame.getSourceAddress();
                eventCount = 0;
                sendFrame(FrameType.token, nextStation);
                pollStation = thisStation;
                tokenCount = 0;
                retryCount = 0;
                receivedValidFrame = false;
                state = MasterNodeState.passToken;
            } else {
                // ReceivedUnexpectedFrame
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            thisStation + " pollForMaster:ReceivedUnexpectedFrame (" + frame.getSourceAddress() + ")");
                }
                receivedValidFrame = false;
                state = MasterNodeState.idle;
            }
            activity = true;
        } else if (soleMaster && (silence() >= usageTimeout || receivedInvalidFrame != null)) {
            // SoleMaster
            if (LOG.isDebugEnabled()) {
                LOG.debug(thisStation + " pollForMaster:SoleMaster");
            }
            frameCount = 0;
            receivedInvalidFrame = null;
            state = MasterNodeState.useToken;
            activity = true;
        } else if (!soleMaster) {
            final boolean longCondition = silence() >= usageTimeout || receivedInvalidFrame != null;
            if (nextStation != thisStation && longCondition) {
                // DoneWithPFM
                if (LOG.isDebugEnabled()) {
                    LOG.debug(thisStation + " pollForMaster:DoneWithPFM [" + nextStation + "]");
                }
                eventCount = 0;
                sendFrame(FrameType.token, nextStation);
                retryCount = 0;
                receivedInvalidFrame = null;
                state = MasterNodeState.passToken;
                activity = true;
            } else if (nextStation == thisStation) {
                if (adjacentStation(pollStation) != thisStation && longCondition) {
                    // SendNextPFM
                    pollStation = adjacentStation(pollStation);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(thisStation + " pollForMaster:SendNextPFM [" + pollStation + "]");
                    }
                    sendFrame(FrameType.pollForMaster, pollStation);
                    retryCount = 0;
                    receivedInvalidFrame = null;
                    activity = true;
                } else if (adjacentStation(pollStation) == thisStation && longCondition) {
                    // DeclareSoleMaster
                    receivedToken = true;
                    if (LOG.isDebugEnabled())
                        LOG.debug(thisStation + " pollForMaster:DeclareSoleMaster");
                    soleMaster = true;
                    frameCount = 0;
                    receivedInvalidFrame = null;
                    state = MasterNodeState.useToken;
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug(thisStation + " answerDataRequest:Reply");
                }
                sendFrame(replyFrame);
                replyFrame = null;
                state = MasterNodeState.idle;
                activity = true;
            } else {
                final long now = clock.millis();
                if (replyDeadline < now) {
                    // DeferredReply
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(thisStation + " answerDataRequest:DeferredReply (" + frame.getSourceAddress() + ")");
                    }
                    sendFrame(FrameType.replyPostponed, frame.getSourceAddress());
                    state = MasterNodeState.idle;
                    activity = true;
                } else {
                    // If the current time of the host was moved back, the above condition could cause an indefinite
                    // wait. So, we check if the reply deadline is too long, and correct if so.
                    final long timeDiff = replyDeadline - now;
                    if (timeDiff > Constants.REPLY_DELAY) {
                        LOG.warn("Correcting replyDeadline time because of timeDiff of " + timeDiff);
                        replyDeadline = now + Constants.REPLY_DELAY;
                    }
                }
            }
        }
    }

    private byte adjacentStation(final byte station) {
        int i = station & 0xff;
        i = (i + 1) % (maxMaster + 1);
        return (byte) i;
    }
}
