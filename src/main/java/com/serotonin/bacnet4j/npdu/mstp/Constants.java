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

public class Constants {
    /**
     * This parameter represents the value of the Max_Info_Frames property of the node's Device object. The value
     * of Max_Info_Frames specifies the maximum number of information frames the node may send before it must
     * pass the token. Max_Info_Frames may have different values on different nodes. This may be used to allocate
     * more or less of the available link bandwidth to particular nodes. If Max_Info_Frames is not writable in a node,
     * its value shall be 1.
     */
    public static final int MAX_INFO_FRAMES = 1;

    /**
     * This parameter represents the value of the Max_Master property of the node's Device object. The value of
     * Max_Master specifies the highest allowable address for master nodes. The value of Max_Master shall be less
     * than or equal to 127. If Max_Master is not writable in a node, its value shall be 127.
     */
    public static final int MAX_MASTER = 127;

    /**
     * The number of tokens received or used before a Poll For Master cycle is executed: 50.
     */
    public static final int POLL = 50;

    /**
     * The number of retries on sending Token: 1.
     */
    public static final int RETRY_TOKEN = 1;

    /**
     * The minimum number of DataAvailable or ReceiveError events that must be seen by a receiving node in order
     * to declare the line "active": 4.
     */
    public static final int MIN_OCTETS = 4;

    /**
     * The minimum time without a DataAvailable or ReceiveError event within a frame before a receiving node
     * may discard the frame: 60 bit times. (Implementations may use larger values for this timeout, not to exceed
     * 100 milliseconds.)
     */
    public static final int FRAME_ABORT = 100;

    /**
     * The maximum idle time a sending node may allow to elapse between octets of a frame the node is
     * transmitting: 20 bit times.
     */
    public static final int FRAME_GAP = 20;

    /**
     * The time without a DataAvailable or ReceiveError event before declaration of loss of token: 500 milliseconds.
     */
    public static final int NO_TOKEN = 500;

    /**
     * The maximum time after the end of the stop bit of the final octet of a transmitted frame before a node must
     * disable its EIA-485 driver: 15 bit times.
     */
    public static final int POSTDRIVE = 15;

    /**
     * The maximum time a node may wait after reception of a frame that expects a reply before sending the first
     * octet of a reply or Reply Postponed frame: 250 milliseconds.
     */
    public static final int REPLY_DELAY = 250;

    /**
     * The minimum time without a DataAvailable or ReceiveError event that a node must wait for a station to begin
     * replying to a confirmed request: 255 milliseconds. (Implementations may use larger values for this timeout,
     * not to exceed 300 milliseconds.)
     */
    public static final int REPLY_TIMEOUT = 255;

    /**
     * Repeater turnoff delay. The duration of a continuous logical one state at the active input port of an MS/TP
     * repeater after which the repeater will enter the IDLE state: 29 bit times < Troff < 40 bit times.
     */
    public static final int ROFF = 35;

    /**
     * The width of the time slot within which a node may generate a token: 10 milliseconds.
     */
    public static final int SLOT = 10;

    /**
     * The minimum time after the end of the stop bit of the final octet of a received frame before a node may enable
     * its EIA-485 driver: 40 bit times.
     */
    public static final int TURNAROUND = 40;

    /**
     * The maximum time a node may wait after reception of the token or a Poll For Master frame before sending
     * the first octet of a frame: 15 milliseconds.
     */
    public static final int USAGE_DELAY = 15;

    /**
     * The minimum time without a DataAvailable or ReceiveError event that a node must wait for a remote node to
     * begin using a token or replying to a Poll For Master frame: 20 milliseconds. (Implementations may use larger
     * values for this timeout, not to exceed 100 milliseconds.)
     */
    public static final int USAGE_TIMEOUT = 20;

    public static final byte BROADCAST = (byte) 0xFF;
}
