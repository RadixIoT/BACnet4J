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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Clock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.SerialPortWrapper;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

abstract public class MstpNode implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MstpNode.class);

    private static final byte PREAMBLE1 = 0x55;
    private static final byte PREAMBLE2 = (byte) 0xFF;
    private static final int MAX_FRAME_LENGTH = 501;


    protected enum ReadFrameState {
        idle, preamble, header, headerCrc, data, dataCrc;
    }


    protected final String portId;

    //
    // Configuration
    protected int inactivityDelay = 1;

    /**
     * The MAC address of this node. TS is generally read from a hardware DIP switch, or from nonvolatile memory. Valid
     * values for TS are 0 to 254. The value 255 is used to denote broadcast when used as a destination address but is
     * not allowed as a value for TS.
     */
    protected final byte thisStation;

    //
    // Fields
    private MstpNetwork network;

    protected Clock clock;

    private SerialPortWrapper wrapper;
    protected OutputStream out;
    protected InputStream in;
    protected final byte[] readArray = new byte[512];
    protected int readCount;
    private final Frame sendFrame = new Frame();
    protected final HeaderCRC sendHeaderCRC = new HeaderCRC();
    protected final DataCRC sendDataCRC = new DataCRC();

    Thread thread;

    private volatile boolean running;

    private ReadFrameState state;

    protected String lastWriteError;
    protected long bytesOut;
    protected long bytesIn;

    public MstpNode(final SerialPortWrapper wrapper, final byte thisStation) {
        this(wrapper.getCommPortId(), wrapper, thisStation);
    }

    public MstpNode(final String portId, final SerialPortWrapper wrapper, final byte thisStation) {
        this.portId = portId;
        this.wrapper = wrapper;
        this.thisStation = thisStation;
    }

    public MstpNode(final String portId, final InputStream in, final OutputStream out, final byte thisStation) {
        this.portId = portId;
        this.in = in;
        this.out = out;
        this.thisStation = thisStation;
    }

    String getCommPortId() {
        return portId;
    }

    public void initialize(final Transport transport) throws Exception {
        this.clock = transport.getLocalDevice().getClock();
        initialize(true);
    }

    public long getBytesOut() {
        return bytesOut;
    }

    public long getBytesIn() {
        return bytesIn;
    }

    public void initialize(final boolean runInThread) throws Exception {
        if (!running) {
            if (wrapper != null) {
                wrapper.open();
                in = wrapper.getInputStream();
                out = wrapper.getOutputStream();
            }

            running = true;
            lastNonSilence = clock.millis();
            state = ReadFrameState.idle;
            if (runInThread) {
                thread = new Thread(this, "BACnet4J MS/TP node");
                thread.start();
            }
        }
    }

    public void terminate() {
        running = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            if (wrapper != null)
                wrapper.close();
        } catch (final Exception e) {
            LOG.warn("", e);
        }
    }

    public void setNetwork(final MstpNetwork network) {
        this.network = network;
    }

    /**
     * The flag ReceiveError is TRUE if an error is detected during the reception of an octet. Many common UARTs detect
     * several types of receive errors, in particular framing errors and overrun errors. ReceiveError shall be TRUE if
     * any of these errors is detected.
     */
    protected boolean receiveError;

    /**
     * Substitute for the silence timer.
     */
    protected long lastNonSilence;

    /**
     * Used to store the data on the incoming frame.
     */
    protected final Frame frame = new Frame();

    /**
     * The header CRC accumulator.
     */
    private final HeaderCRC headerCRC = new HeaderCRC();

    /**
     * The data CRC accumulator.
     */
    protected final DataCRC dataCRC = new DataCRC();

    /**
     * Used as an index by the Receive State Machine, up to a maximum value of InputBufferSize.
     */
    protected int index;

    /**
     * Used to count the number of received octets or errors. This is used in the detection of link activity.
     */
    int eventCount;

    /**
     * The number of frames sent by this node during a single token hold. When this counter reaches the value
     * Nmax_info_frames, the node must pass the token.
     */
    protected int frameCount;

    /**
     * An array of octets, used to store octets as they are received. InputBuffer is indexed from 0 to
     * InputBufferSize-1. The maximum size of a frame is 501 octets. A smaller value for InputBufferSize may be used by
     * some implementations.
     */
    protected final ByteQueue inputBuffer = new ByteQueue(MAX_FRAME_LENGTH);

    /**
     * ReceivedInvalidFrame A Boolean flag set to TRUE by the Receive State Machine if an error is detected during the
     * reception of a frame. Set to FALSE by the main state machine.
     */
    protected String receivedInvalidFrame;

    /**
     * A Boolean flag set to TRUE by the Receive State Machine if a valid frame is received. Set to FALSE by the main
     * state machine.
     */
    protected boolean receivedValidFrame;

    protected boolean activity;

    /**
     * @return the running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @return the thisStation
     */
    public byte getThisStation() {
        return thisStation;
    }

    @Override
    public final void run() {
        while (running) {
            activity = false;

            doCycle();

            if (!activity && inactivityDelay > 0) {
                try {
                    Thread.sleep(inactivityDelay);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        //
        //        try {
        //            wrapper.close();
        //        } catch (final Exception e) {
        //            LOG.warn("", e);
        //        }
    }

    abstract protected void doCycle();

    abstract public void setReplyFrame(FrameType type, byte destination, byte[] data);

    protected void readFrame() {
        readInputStream();

        if (receiveError) {
            // EatAnError
            receiveError = false;
            eventCount++;
            state = ReadFrameState.idle;
            activity = true;
        }

        if (!receivedValidFrame) {
            if (state == ReadFrameState.idle)
                idle();

            if (state == ReadFrameState.preamble)
                preamble();

            if (state == ReadFrameState.header)
                header();

            if (state == ReadFrameState.headerCrc)
                headerCrc();

            if (state == ReadFrameState.data)
                data();

            if (state == ReadFrameState.dataCrc)
                dataCrc();
        }
    }

    protected long silence() {
        return clock.millis() - lastNonSilence;
    }

    protected void readInputStream() {
        try {
            if (in.available() > 0) {
                readCount = in.read(readArray);
                bytesIn += readCount;
                if (LOG.isTraceEnabled())
                    LOG.trace(tracePrefix() + "in: " + StreamUtils.dumpArrayHex(readArray, 0, readCount));
                inputBuffer.push(readArray, 0, readCount);
                eventCount += readCount;
                //noise();
            }
        } catch (final IOException e) {
            if (StringUtils.equals(e.getMessage(), "Stream closed."))
                throw new RuntimeException(e);
            if (LOG.isDebugEnabled())
                LOG.debug(thisStation + " Input stream listener exception", e);
            receiveError = true;
        }
    }

    private void idle() {
        byte b;
        while (inputBuffer.size() > 0) {
            activity = true;

            b = inputBuffer.pop();
            if (b == PREAMBLE1) {
                // Preamble1
                state = ReadFrameState.preamble;
                noise();
                break;
            }
            // else
            // EatAnOctet
            //     ; // EatAnOctet
        }
    }

    private void preamble() {
        if (silence() > Constants.FRAME_ABORT) {
            // Timeout
            if (LOG.isDebugEnabled())
                LOG.debug(thisStation + " Frame abort due to timeout");
            state = ReadFrameState.idle;
            activity = true;
        } else {
            byte b;
            while (inputBuffer.size() > 0) {
                activity = true;
                b = inputBuffer.pop();

                if (b == PREAMBLE1) {
                    // RepeatedPreamble1
                    // no op
                } else if (b == PREAMBLE2) {
                    // Preamble2
                    frame.reset();
                    headerCRC.reset();
                    index = 0;
                    state = ReadFrameState.header;
                    break;
                } else {
                    // NotPreamble
                    state = ReadFrameState.idle;
                    break;
                }
            }
        }

    }

    private void header() {
        if (silence() > Constants.FRAME_ABORT) {
            // Timeout
            receivedInvalidFrame = "Timeout reading header";
            if (LOG.isDebugEnabled())
                LOG.debug(thisStation + " Timeout reading header: index=" + index + ", frame=" + frame);
            state = ReadFrameState.idle;
            activity = true;
        } else {
            byte b;
            while (inputBuffer.size() > 0) {
                activity = true;
                b = inputBuffer.pop();

                if (index == 0) {
                    // FrameType
                    headerCRC.accumulate(b);
                    frame.setFrameType(FrameType.forId(b));
                    if (LOG.isTraceEnabled() && frame.getFrameType() == null)
                        LOG.trace(tracePrefix() + "Unknown frame type for value: " + b);
                    index = 1;
                } else if (index == 1) {
                    // Destination
                    headerCRC.accumulate(b);
                    frame.setDestinationAddress(b);
                    index = 2;
                } else if (index == 2) {
                    // Source
                    headerCRC.accumulate(b);
                    frame.setSourceAddress(b);
                    index = 3;
                } else if (index == 3) {
                    // Length1
                    headerCRC.accumulate(b);
                    frame.setLength((b & 0xff) << 8);
                    index = 4;
                } else if (index == 4) {
                    // Length2
                    headerCRC.accumulate(b);
                    frame.setLength(frame.getLength() | b & 0xff);
                    index = 5;
                } else if (index == 5) {
                    // HeaderCRC
                    headerCRC.accumulate(b);
                    state = ReadFrameState.headerCrc;
                    break;
                }
            }
        }
    }

    private void headerCrc() {
        activity = true;

        if (!headerCRC.isOk()) {
            // BadCRC
            receivedInvalidFrame = "Bad header CRC. Frame: " + frame;
            state = ReadFrameState.idle;
        } else {
            if (!frame.forStationOrBroadcast(thisStation))
                // NotForUs
                state = ReadFrameState.idle;
            else if (frame.getLength() > MAX_FRAME_LENGTH) {
                // FrameTooLong
                receivedInvalidFrame = "Frame too long";
                state = ReadFrameState.idle;
            } else if (frame.getLength() == 0) {
                // NoData
                receivedValidFrame = true;
                if (frame.getFrameType() == null && LOG.isDebugEnabled())
                    LOG.debug(thisStation + " Received valid frame with no type (1): " + frame);
                state = ReadFrameState.idle;
            } else {
                // Data
                index = 0;
                dataCRC.reset();
                state = ReadFrameState.data;
                frame.setData(new byte[frame.getLength()]);
            }
        }
    }

    protected void data() {
        if (silence() > Constants.FRAME_ABORT) {
            // Timeout
            receivedInvalidFrame = "Timeout reading data";
            state = ReadFrameState.idle;
            activity = true;
        } else {
            while (inputBuffer.size() > 0) {
                activity = true;
                noise();
                final byte b = inputBuffer.pop();

                if (index < frame.getLength()) {
                    // DataOctet
                    dataCRC.accumulate(b);
                    frame.getData()[index] = b;
                    index++;
                } else if (index == frame.getLength()) {
                    // CRC1
                    dataCRC.accumulate(b);
                    index++;
                } else if (index == frame.getLength() + 1) {
                    // CRC2
                    dataCRC.accumulate(b);
                    state = ReadFrameState.dataCrc;
                    break;
                }
            }
        }
    }

    protected void dataCrc() {
        activity = true;

        if (!dataCRC.isOk())
            // BadCRC
            receivedInvalidFrame = "Bad data CRC";
        else {
            // GoodCRC
            receivedValidFrame = true;
            if (frame.getFrameType() == null && LOG.isDebugEnabled())
                LOG.debug(thisStation + " Received valid frame with no type (2): " + frame);
        }

        state = ReadFrameState.idle;
    }

    protected void sendFrame(final FrameType type, final byte destinationAddress) {
        sendFrame(type, destinationAddress, null);
    }

    protected void sendFrame(final FrameType type, final byte destinationAddress, final byte[] data) {
        sendFrame.reset();
        sendFrame.setFrameType(type);
        sendFrame.setDestinationAddress(destinationAddress);
        sendFrame.setSourceAddress(thisStation);
        sendFrame.setData(data);
        sendFrame(sendFrame);
    }

    public void testSendFrame(final Frame frame) {
        sendFrame(frame);
    }

    protected void sendFrame(final Frame frame) {
        //        long start = clock.millis();

        // Turnaround. 40 bit times at 9600 baud is around 4ms.
        //        long wait = 4 - silence();
        //        if (wait > 0) {
        //            debug("Turnaround wait time: " + wait);
        //            try {
        //                Thread.sleep(wait);
        //            }
        //            catch (InterruptedException e) {
        //                // no op
        //            }
        //        }

        try {
            if (LOG.isTraceEnabled())
                LOG.trace(tracePrefix() + "out: " + frame);
            //LOG.fine("writing frame: " + frame);

            // Preamble
            out.write(0x55);
            out.write(0xFF);

            // Header
            out.write(frame.getFrameType().id & 0xff);
            out.write(frame.getDestinationAddress() & 0xff);
            out.write(frame.getSourceAddress() & 0xff);
            out.write(frame.getLength() >> 8 & 0xff);
            out.write(frame.getLength() & 0xff);
            out.write(sendHeaderCRC.getCrc(frame));
            bytesOut += 8;

            if (frame.getLength() > 0) {
                // Data
                out.write(frame.getData());
                final int crc = sendDataCRC.getCrc(frame);
                out.write(crc & 0xff);
                out.write(crc >> 8 & 0xff);
                bytesOut += frame.getLength() + 2;
            }

            out.flush();
        } catch (final IOException e) {
            // Only write the same error message once. Prevents logs from getting filled up unnecessarily with repeated
            // error messages.
            if (!StringUtils.equals(e.getMessage(), lastWriteError)) {
                // NOTE: should anything else be informed of this?
                LOG.error("Error while sending frame", e);
                lastWriteError = e.getMessage();
            }
        }

        noise();

        //        debug("Write took " + (clock.millis() - start) + " ms");

        // NOTE: ??
        // Wait until the final stop bit of the most significant CRC octet has been transmitted
        // but not more than Tpostdrive.
    }

    private void noise() {
        lastNonSilence = clock.millis();
    }

    protected String tracePrefix() {
        return thisStation + "/" + clock.millis() % 10000000 + ": ";
    }

    //
    //
    // Incoming message handling
    //
    protected void receivedDataNoReply(final Frame frame) {
        if (frame.getFrameType() == FrameType.testResponse)
            LOG.info("Received test response frame");
        else if (network == null)
            LOG.info("Received data no reply: " + frame);
        else
            network.receivedFrame(frame.copy());
    }

    protected void receivedDataNeedingReply(final Frame frame) {
        if (frame.getFrameType() == FrameType.testRequest)
            // Echo the data back.
            sendFrame(FrameType.testResponse, frame.getSourceAddress(), frame.getData());
        else if (network == null)
            LOG.info("Received data needing reply: " + frame);
        else
            network.receivedFrame(frame.copy());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (portId == null ? 0 : portId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MstpNode other = (MstpNode) obj;
        if (portId == null) {
            if (other.portId != null)
                return false;
        } else if (!portId.equals(other.portId))
            return false;
        return true;
    }
}
