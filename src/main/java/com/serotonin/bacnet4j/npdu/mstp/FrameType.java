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

import org.apache.commons.lang3.ArrayUtils;

/**
 * Using a different convention for instance naming here to see how it feels.
 */
public enum FrameType {
    token(0), //
    pollForMaster(1), //
    replyToPollForMaster(2), //
    testRequest(3), //
    testResponse(4), //
    bacnetDataExpectingReply(5), //
    bacnetDataNotExpectingReply(6), //
    replyPostponed(7), //
    ;

    public final byte id;

    private FrameType(int id) {
        this.id = (byte) id;
    }

    public static FrameType forId(byte b) {
        for (FrameType e : values()) {
            if (e.id == b)
                return e;
        }
        return null;
    }

    public boolean oneOf(FrameType... types) {
        return ArrayUtils.contains(types, this);
    }
}
