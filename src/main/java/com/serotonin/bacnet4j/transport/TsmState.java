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

package com.serotonin.bacnet4j.transport;

/**
 * The states of the Transaction State Machines described in clause 5.4. A transaction is in the IDLE state when no
 * state machine exists for it, so IDLE is not represented here; the creation of a transaction context is the
 * transition out of IDLE, and its disposal is the transition into IDLE.
 */
public enum TsmState {
    /**
     * Clause 5.4.4.2. The client waits for a segment acknowledgement for one or more segments of a confirmed request
     * that it is sending.
     */
    SEGMENTED_REQUEST_CLIENT,

    /**
     * Clause 5.4.4.3. The client waits for a response to a confirmed request.
     */
    AWAIT_CONFIRMATION,

    /**
     * Clause 5.4.4.4. The client waits for one or more segments of a complex acknowledgement that it is receiving.
     */
    SEGMENTED_CONF,

    /**
     * Clause 5.4.5.2. The server waits for segments of a confirmed request that it is receiving.
     */
    SEGMENTED_REQUEST_SERVER,

    /**
     * Clause 5.4.5.3. The server waits for the local application program to respond to a confirmed request.
     */
    AWAIT_RESPONSE,

    /**
     * Clause 5.4.5.4. The server waits for a segment acknowledgement for one or more segments of a complex
     * acknowledgement that it is sending.
     */
    SEGMENTED_RESPONSE;

    /**
     * Whether a message is being received in this state, as opposed to being sent.
     */
    public boolean isReceivingSegments() {
        return this == SEGMENTED_CONF || this == SEGMENTED_REQUEST_SERVER;
    }

    /**
     * Whether segments are being sent in this state.
     */
    public boolean isSendingSegments() {
        return this == SEGMENTED_REQUEST_CLIENT || this == SEGMENTED_RESPONSE;
    }
}
