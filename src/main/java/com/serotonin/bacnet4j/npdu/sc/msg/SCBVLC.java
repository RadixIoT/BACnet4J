/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.npdu.sc.msg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.serotonin.bacnet4j.npdu.sc.SCNetworkUtils;
import com.serotonin.bacnet4j.npdu.sc.SCVmac;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SCBVLC {
    // function + control + id +orig + dest + no dest opts + 4192 of data opts
    public static final int MESSAGE_HEADER_MAX_LENGTH = 4 + 6 + 6 + 0 + 4192;

    private static final String EMPTY_PAYLOAD = "error: EMPTY PAYLOAD!";

    private int function = -1; // 1 byte
    private int control; // 1 byte
    private int id; // 2 bytes
    private SCVmac originating = null;
    private SCVmac destination = null;
    private List<SCOption> destOptions = null;  // Destination Options Optional, N-octets options
    private List<SCOption> dataOptions = null;  // Data Options Optional, N-octets options
    private byte[] payload = null;  // Payload Variable, The payload of the BVLC message

    private boolean parseError;
    private ErrorCode parseErrorCode;
    private String parseErrorReason;

    public static final int BVLC_RESULT = 0;
    public static final int ENCAPSULATED_NPDU = 1;
    public static final int ADDRESS_RESOLUTION = 2;
    public static final int ADDRESS_RESOLUTION_ACK = 3;
    public static final int ADVERTISEMENT = 4;
    public static final int ADVERTISEMENT_SOLICITATION = 5;
    public static final int CONNECT_REQUEST = 6;
    public static final int CONNECT_ACCEPT = 7;
    public static final int DISCONNECT_REQUEST = 8;
    public static final int DISCONNECT_ACK = 9;
    public static final int HEARTBEAT_REQUEST = 10;
    public static final int HEARTBEAT_ACK = 11;
    public static final int PROPRIETARY_MESSAGE = 12;
    private static final int MAX_FUNCTION = 12;

    private static final int FLAG_ORIG_ADDR = 0x08;
    private static final int FLAG_DEST_ADDR = 0x04;
    private static final int FLAG_DEST_OPTS = 0x02;
    private static final int FLAG_DATA_OPTS = 0x01;
    private static final int FLAG_MASK = 0x0F;

    public SCBVLC(SCVmac originating, SCVmac destination, int function, int id) {
        this.originating = originating;
        this.destination = destination;
        this.function = function;
        this.id = id;
    }

    public SCBVLC(SCVmac originating, SCVmac destination, int function, byte[] payload) {
        this(originating, destination, function, payload, -1);
    }

    public SCBVLC(SCVmac originating, SCVmac destination, int function, byte[] payload, int id) {
        this.originating = originating;
        this.destination = destination;
        this.function = function;
        this.payload = payload;
        this.id = id;
    }

    public SCBVLC(SCVmac originating, SCVmac destination, int function, byte[] payload, int id,
            List<SCOption> destOptions, List<SCOption> dataOptions) {
        this.originating = originating;
        this.destination = destination;
        this.function = function;
        this.payload = payload;
        this.id = id;
        this.destOptions = destOptions;
        this.dataOptions = dataOptions;
    }

    public boolean needsId() {
        return id == -1;
    }

    public void setId(int id) {
        this.id = id;
    }

    public SCBVLC(ByteQueue queue) {
        parse(queue);
    }

    public SCVmac getOriginating() {
        return originating;
    }

    public SCVmac getDestination() {
        return destination;
    }

    public List<SCOption> getDestOptions() {
        return destOptions;
    }

    public List<SCOption> getDataOptions() {
        return dataOptions;
    }

    private void parse(ByteQueue queue) {
        if (id == -1) {
            // It is a bug if this happens, so handling is not required.
            throw new IllegalStateException("id not set");
        }

        // This will check for AB.3.1.5 Common Error Situations, including cases for "If a BVLC message is received..."
        // "...is truncated" - checked
        // "...a header has encoding errors" - not much to check for, actually
        // "...any control flag has an unexpected value" - checked
        // "...any parameter, field of a known header, or parameter in a BACnet/SC defined payload, is out of range"
        // "...any data inconsistency exists in any" - checked by users of this class, payload not checked here
        try {
            function = queue.popU1B();
            control = queue.popU1B();
            id = queue.popU2B();
            originating = ((control & FLAG_ORIG_ADDR) == 0) ? null : new SCVmac(queue);
            destination = ((control & FLAG_DEST_ADDR) == 0) ? null : new SCVmac(queue);
            destOptions = ((control & FLAG_DEST_OPTS) == 0) ? null : parseOptions(queue); // can set parseErrorXxxx
            dataOptions = ((control & FLAG_DATA_OPTS) == 0) ? null : parseOptions(queue); // can set parseErrorXxxx
            if (queue.size() > 0) {
                payload = new byte[queue.size()];
                queue.pop(payload);
            } else {
                payload = null;
            }

            // Check for errors and inconsistency
            if ((control & ~FLAG_MASK) != 0) {
                setParseError(ErrorCode.parameterOutOfRange, "reserved control bits are not 0");
            } else if (function > MAX_FUNCTION) {
                setParseError(ErrorCode.bvlcFunctionUnknown, "Function Code unknown");
            }
            // This only checked for general "parsing errors".  It did not check for address/payload inconsistencies or
            // unsupported things.  Because this class does not know the context in which the messages are used, the users
            // of SCMessage, like SCConnection, check for a lot more error conditions.
            // ALSO... checking options for "must understand" is also done by higher level users. This class doesn't want
            // to assume too much.
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            // AB.3.1.5 "If a BVLC message is received that is truncated..."
            setParseError(ErrorCode.messageIncomplete, "Not enough data in message - length wrong?");
        }
    }

    public int getId() {
        return id;
    }

    public int getFunction() {
        return function;
    }

    public byte[] getPayload() {
        return payload;
    }

    private void setParseError(ErrorCode code, String reason) {
        parseError = true;
        parseErrorCode = code;
        parseErrorReason = reason;
    }

    public boolean isParseError() {
        return parseError;
    }

    public ErrorCode getParseErrorCode() {
        return parseErrorCode;
    }

    public String getParseErrorReason() {
        return parseErrorReason;
    }

    private void updateControlFlags() {
        control = 0;
        if (originating != null) {
            control |= FLAG_ORIG_ADDR;
        }
        if (destination != null) {
            control |= FLAG_DEST_ADDR;
        }
        if (destOptions != null && !destOptions.isEmpty()) {
            control |= FLAG_DEST_OPTS;
        }
        if (dataOptions != null && !dataOptions.isEmpty()) {
            control |= FLAG_DATA_OPTS;
        }
    }

    public byte[] write() {
        updateControlFlags();

        var queue = new ByteQueue();
        queue.push((byte) function);
        queue.push((byte) control);
        queue.pushU2B(id);
        if (originating != null) {
            originating.write(queue);
        }
        if (destination != null) {
            destination.write(queue);
        }
        if (destOptions != null) {
            writeOptions(destOptions, queue);
        }
        if (dataOptions != null) {
            writeOptions(dataOptions, queue);
        }
        if (payload != null) {
            queue.push(payload);
        }

        return queue.popAll();
    }

    public boolean isUnicast() {
        return !SCNetworkUtils.isBroadcast(destination);
    }

    public boolean isBroadcast() {
        return SCNetworkUtils.isBroadcast(destination);
    }

    public boolean isUnicastRequest() {
        return isUnicast() && (
                function == CONNECT_REQUEST ||
                        function == DISCONNECT_REQUEST ||
                        function == ENCAPSULATED_NPDU ||
                        function == ADDRESS_RESOLUTION ||
                        function == ADVERTISEMENT_SOLICITATION ||
                        function == HEARTBEAT_REQUEST ||
                        function > MAX_FUNCTION);
    }

    private List<SCOption> parseOptions(ByteQueue queue) {
        List<SCOption> result = new ArrayList<>();
        boolean more = true;
        while (more) {
            SCOption option = new SCOption();
            more = option.parse(queue);
            if (option.hasParseError()) {
                parseError = true;
                parseErrorCode = option.getParseError();
                parseErrorReason = option.getParseErrorReason();
            } else {
                result.add(option);
            }
        }
        return result;
    }

    private void writeOptions(List<SCOption> options, ByteQueue queue) {
        // Have to use iterator not for..each because we need to know the last one
        Iterator<SCOption> iter = options.iterator();
        while (iter.hasNext()) {
            SCOption option = iter.next();
            option.write(queue, iter.hasNext()); // indicate the last one
        }
    }

    @Override
    public String toString() {
        if (function == -1)
            return "(unparsed)";
        String result =
                "{#" + id +
                        " f=" + functionToString(function) +
                        " d=" + (destination == null ? "none" : destination.toString()) +
                        " o=" + (originating == null ? "none" : originating.toString()) +
                        " i=" + id +
                        " c=" + ((control & FLAG_ORIG_ADDR) != 0 ? "O" : "-") +
                        ((control & FLAG_DEST_ADDR) != 0 ? "D" : "-") +
                        ((control & FLAG_DEST_OPTS) != 0 ? "L" : "-") +
                        ((control & FLAG_DATA_OPTS) != 0 ? "N" : "-") +
                        (destOptions == null ? "" : " destopt=" + destOptions) +
                        (dataOptions == null ? "" : " dataopt=" + dataOptions) +
                        " p={";
        switch (function) {
            case BVLC_RESULT -> {
                if (payload == null)
                    result += EMPTY_PAYLOAD;
                else
                    result += new SCPayloadBVLCResult(payload).toString();
            }
            case ENCAPSULATED_NPDU -> {
                if (payload == null)
                    result += EMPTY_PAYLOAD;
                else
                    result += "NPDU(" + new ByteQueue(payload) + ")";
            }
            case ADDRESS_RESOLUTION_ACK -> {
                if (payload == null)
                    result += EMPTY_PAYLOAD;
                else
                    result += new SCPayloadAddressResolutionAck(payload).toString();
            }
            case ADVERTISEMENT -> {
                if (payload == null)
                    result += EMPTY_PAYLOAD;
                else
                    result += new SCPayloadAdvertisement(payload).toString();
            }
            case CONNECT_REQUEST -> {
                if (payload == null)
                    result += EMPTY_PAYLOAD;
                else
                    result += new SCPayloadConnectRequest(payload).toString();
            }
            case CONNECT_ACCEPT -> {
                if (payload == null)
                    result += EMPTY_PAYLOAD;
                else
                    result += new SCPayloadConnectAccept(payload).toString();
            }
            case ADDRESS_RESOLUTION, ADVERTISEMENT_SOLICITATION, DISCONNECT_REQUEST, DISCONNECT_ACK, HEARTBEAT_REQUEST,
                 HEARTBEAT_ACK -> {
                if (payload == null)
                    result += "()";
                else
                    result += "error: NON-EMPTY PAYLOAD!";
            }
            default -> result += "error: INVALID FUNCTION";
        }
        return result + "}";
    }

    public static String functionToString(int function) {
        return switch (function) {
            case BVLC_RESULT -> "BR";
            case ENCAPSULATED_NPDU -> "EN";
            case ADDRESS_RESOLUTION -> "AR";
            case ADDRESS_RESOLUTION_ACK -> "AA";
            case ADVERTISEMENT -> "AD";
            case ADVERTISEMENT_SOLICITATION -> "AS";
            case CONNECT_REQUEST -> "CR";
            case CONNECT_ACCEPT -> "CA";
            case DISCONNECT_REQUEST -> "DR";
            case DISCONNECT_ACK -> "DA";
            case HEARTBEAT_REQUEST -> "HR";
            case HEARTBEAT_ACK -> "HA";
            case PROPRIETARY_MESSAGE -> "PM";
            default -> "?" + function + "?";
        };
    }
}
