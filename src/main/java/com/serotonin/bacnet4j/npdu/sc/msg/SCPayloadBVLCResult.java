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

import java.nio.charset.StandardCharsets;

import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

/**
 * A data object that holds a payload of a BVLC-Result message, as defined in AB.2.4.
 * This includes methods to parse from, and generate to, byte buffers.
 */
public class SCPayloadBVLCResult implements SCPayload {
    private final int forFunction;
    private final int resultCode;
    private int errorHeaderMarker;
    private ErrorClass errorClass;
    private ErrorCode errorCode;
    private String errorDetails;

    public SCPayloadBVLCResult(int forFunction) {
        this.forFunction = forFunction;
        this.resultCode = 0; // ack
    }

    public SCPayloadBVLCResult(int forFunction, int errorHeaderMarker, ErrorClass errorClass, ErrorCode errorCode,
            String errorDetails) {
        this.forFunction = forFunction;
        this.resultCode = 1; // nak
        this.errorHeaderMarker = errorHeaderMarker;
        this.errorClass = errorClass;
        this.errorCode = errorCode;
        this.errorDetails = errorDetails;
    }

    public int getForFunction() {
        return forFunction;
    }

    public boolean isAck() {
        return resultCode == 0;
    }

    public boolean isNak() {
        return resultCode != 0;
    }

    public int getResultCode() {
        return resultCode;
    }

    public int getErrorHeaderMarker() {
        return errorHeaderMarker;
    }

    public ErrorClass getErrorClass() {
        return errorClass;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public SCPayloadBVLCResult(byte[] bytes) {
        var queue = new ByteQueue(bytes);
        forFunction = queue.popU1B();
        resultCode = queue.popU1B();
        if (isNak()) {
            errorHeaderMarker = queue.popU1B();
            errorClass = ErrorClass.forId(queue.popU2B());
            errorCode = ErrorCode.forId(queue.popU2B());
            if (queue.size() > 0) {
                errorDetails = queue.popString(queue.size(), StandardCharsets.UTF_8);
            }
        }
    }

    public byte[] write() {
        var queue = new ByteQueue();
        queue.push((byte) forFunction);
        queue.push((byte) resultCode);
        if (isNak()) { // nak
            queue.push((byte) errorHeaderMarker);
            queue.pushU2B(errorClass.intValue());
            queue.pushU2B(errorCode.intValue());
            if (errorDetails != null && !errorDetails.isEmpty()) {
                queue.push(errorDetails.getBytes(StandardCharsets.UTF_8));
            }
        }
        return queue.popAll();
    }

    public String toString() {
        if (resultCode != 0) {
            return "(f=" + forFunction +
                    " r=" + resultCode +
                    " m=" + StreamUtils.toHex((byte) errorHeaderMarker) +
                    " e=" + errorClass +
                    " c=" + errorCode +
                    " d=" + errorDetails +
                    ")";
        } else {
            return "(f=" + forFunction +
                    " r=" + resultCode +
                    ")";
        }
    }
}
