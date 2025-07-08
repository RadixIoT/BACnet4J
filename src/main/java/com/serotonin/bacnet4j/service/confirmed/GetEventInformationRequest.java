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

package com.serotonin.bacnet4j.service.confirmed;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.acknowledgement.GetEventInformationAck;
import com.serotonin.bacnet4j.service.acknowledgement.GetEventInformationAck.EventSummary;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class GetEventInformationRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 29;

    private final ObjectIdentifier lastReceivedObjectIdentifier; // Optional

    public GetEventInformationRequest(final ObjectIdentifier lastReceivedObjectIdentifier) {
        this.lastReceivedObjectIdentifier = lastReceivedObjectIdentifier;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final SequenceOf<EventSummary> summaries = new SequenceOf<>();

        for (final BACnetObject bo : localDevice.getLocalObjects()) {
            final EventSummary eventSummary = bo.getEventSummary();
            if (eventSummary != null)
                summaries.add(eventSummary);
        }

        return new GetEventInformationAck(summaries, Boolean.FALSE);
    }

    @Override
    public void write(final ByteQueue queue) {
        writeOptional(queue, lastReceivedObjectIdentifier, 0);
    }

    GetEventInformationRequest(final ByteQueue queue) throws BACnetException {
        lastReceivedObjectIdentifier = readOptional(queue, ObjectIdentifier.class, 0);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (lastReceivedObjectIdentifier == null ? 0 : lastReceivedObjectIdentifier.hashCode());
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
        final GetEventInformationRequest other = (GetEventInformationRequest) obj;
        if (lastReceivedObjectIdentifier == null) {
            if (other.lastReceivedObjectIdentifier != null)
                return false;
        } else if (!lastReceivedObjectIdentifier.equals(other.lastReceivedObjectIdentifier))
            return false;
        return true;
    }
}
