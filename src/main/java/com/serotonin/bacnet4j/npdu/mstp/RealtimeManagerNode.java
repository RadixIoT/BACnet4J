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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.mstp.realtime.RealtimeDriver;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

/**
 * MS/TP Manager node using a real-time serial driver
 * installed in the linux Kernel.
 */
public class RealtimeManagerNode extends ManagerNode {
    private final byte thisStation;
    private final RealtimeDriver driver;
    private final int baud;
    private int responseTimeoutMs = 1000;
    private long lastFrameSendTime; //Track response timeouts

    public RealtimeManagerNode(String portId, File driver, File configProgram, byte thisStation, int retryCount,
            int baud, int responseTimeoutMs) throws IllegalArgumentException {
        super(portId, null, null, (byte) 0xFF, retryCount);
        this.thisStation = thisStation;
        this.baud = baud;
        this.driver = new RealtimeDriver(driver, configProgram);
        this.responseTimeoutMs = responseTimeoutMs;
    }

    @Override
    protected void validate(int retryCount) {
        this.retryCount = retryCount;
        nextStation = thisStation;
        pollStation = thisStation;
        tokenCount = Constants.POLL;
        soleManager = false;
        state = ManagerNodeState.idle;
    }

    public void setResponseTimeoutMs(int responseTimeoutMs) {
        this.responseTimeoutMs = responseTimeoutMs;
    }

    @Override
    public void initialize(Transport transport) throws BACnetException {
        try {
            //Setup I/O
            File file = new File(portId);
            in = new FileInputStream(file);
            out = new FileOutputStream(file);

            //Configure Driver
            this.driver.configure(portId, baud, thisStation, maxManager, maxInfoFrames, usageTimeout);
            super.initialize(transport);
        } catch (Exception e) {
            throw new BACnetException(e);
        }
    }

    @Override
    protected void doCycle() {
        readFrame();

        if (state == ManagerNodeState.idle)
            idle();

        if (state == ManagerNodeState.useToken)
            useToken();

        if (state == ManagerNodeState.doneWithToken)
            state = ManagerNodeState.idle;

        if (state == ManagerNodeState.waitForReply)
            waitForReply();

        answerDataRequest();
    }

    /* (non-Javadoc)
     * @see com.serotonin.bacnet4j.npdu.mstp.MstpNode#readFrame()
     */
    @Override
    protected void readFrame() {
        readInputStream();
        if (receiveError) {
            // EatAnError
            receiveError = false;
            eventCount++;
            activity = true;
        }
    }

    @Override
    protected void readInputStream() {
        try {
            //Read 1 message from the driver
            readCount = in.read(readArray);
            if (readCount > 0) {
                bytesIn += readCount;
                if (LOG.isTraceEnabled()) {
                    LOG.trace("{} in: {}", tracePrefix(), StreamUtils.dumpArrayHex(readArray, 0, readCount));
                }
                inputBuffer.push(readArray, 0, readCount);
                eventCount += readCount;
                int pos = 0;
                frame.setSourceAddress(readArray[pos++]);
                byte[] data = new byte[readCount - 1];
                for (int i = 0; i < readCount - 1; i++) {
                    data[i] = readArray[pos++];
                }
                frame.setData(data);
                LOG.trace("in: {}", frame);
                receivedValidFrame = true;
            }
        } catch (IOException e) {
            if (StringUtils.equals(e.getMessage(), "Stream closed."))
                throw new RuntimeException(e);
            LOG.debug("{} Input stream listener exception", thisStation, e);
            receiveError = true;
        }
    }

    @Override
    protected void idle() {
        //Don't worry about invalid frames, assume we can use token if we didn't get a frame
        if (receivedValidFrame) {
            LOG.debug("{} idle:receivedValidFrame", thisStation);
            frame();
            receivedValidFrame = false;
            activity = true;
        } else {
            //We can use the token
            state = ManagerNodeState.useToken;
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.bacnet4j.npdu.mstp.ManagerNode#frame()
     */
    @Override
    protected void frame() {
        receivedDataNoReply(frame);

        //TODO How to decide?  via NPDU or do we modify the driver
        //The idea here is that we assume the driver will always 
        // reply for us...?
        //state = ManagerNodeState.answerDataRequest;
        //replyDeadline = lastNonSilence + Constants.REPLY_DELAY;
    }

    @Override
    protected void waitForReply() {
        if (clock.millis() > lastFrameSendTime + responseTimeoutMs) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} waitForReply:ReplyTimeout", thisStation);
            state = ManagerNodeState.idle;
        } else if (receivedValidFrame) {
            if (LOG.isDebugEnabled())
                LOG.debug("{} waitForReply:ReceivedReply", thisStation);
            receivedDataNoReply(frame);
            state = ManagerNodeState.idle;
            receivedValidFrame = false;
        }
    }

    /* (non-Javadoc)
     * @see com.serotonin.bacnet4j.npdu.mstp.ManagerNode#answerDataRequest()
     */
    @Override
    protected void answerDataRequest() {
        synchronized (this) {
            if (replyFrame != null) {
                // Reply
                if (LOG.isDebugEnabled())
                    LOG.debug("{} answerDataRequest:Reply", thisStation);
                sendFrame(replyFrame);
                replyFrame = null;
                state = ManagerNodeState.idle;
                activity = true;
            }
        }
    }


    @Override
    protected void sendFrame(Frame frame) {
        LOG.info("Sending frame: {}", frame);
        try {
            if (LOG.isTraceEnabled()) {
                LOG.trace("{} out: {}", tracePrefix(), frame);
            }

            // Header
            byte[] writeArray = new byte[5 + frame.getLength()];
            int pos = 0;
            //Skip preamble, the driver will add it
            writeArray[pos++] = frame.getFrameType().id;
            writeArray[pos++] = frame.getDestinationAddress();
            writeArray[pos++] = frame.getSourceAddress();
            writeArray[pos++] = (byte) (frame.getLength() >> 8 & 0xff);
            writeArray[pos++] = (byte) (frame.getLength() & 0xff);
            //Skip 2 byte header CRC, the driver will add it

            if (frame.getLength() > 0) {
                // Data
                for (int i = 0; i < frame.getLength(); i++)
                    writeArray[pos++] = frame.getData()[i];
                //Driver will add CRC
            }
            out.write(writeArray);
            out.flush();
            bytesOut += frame.getLength() + 10; //Imply the missing bytes that the driver will add
            lastFrameSendTime = clock.millis();
            LOG.info("Sent frame {}", frame);
        } catch (IOException e) {
            // Only write the same error message once. Prevents logs from getting filled up unnecessarily with repeated
            // error messages.
            if (!StringUtils.equals(e.getMessage(), lastWriteError)) {
                // NOTE: should anything else be informed of this?
                LOG.error("Error while sending frame", e);
                lastWriteError = e.getMessage();
            }
        }
    }

    @Override
    public void terminate() {
        super.terminate();
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        } catch (IOException e) {
            LOG.error("Error closing streams.", e);
        }
    }
}
