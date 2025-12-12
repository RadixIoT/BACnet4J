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

package com.serotonin.bacnet4j.service.acknowledgement;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ReadPropertyMultipleAck extends AcknowledgementService {
    public static final byte TYPE_ID = 14;

    private final SequenceOf<ReadAccessResult> listOfReadAccessResults;

    public ReadPropertyMultipleAck(final SequenceOf<ReadAccessResult> listOfReadAccessResults) {
        this.listOfReadAccessResults = listOfReadAccessResults;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, listOfReadAccessResults);
    }

    ReadPropertyMultipleAck(final ByteQueue queue) throws BACnetException {
        listOfReadAccessResults = readSequenceOf(queue, ReadAccessResult.class);
    }

    @Override
    public String toString() {
        return "ReadPropertyMultipleAck(" + listOfReadAccessResults + ")";
    }

    public SequenceOf<ReadAccessResult> getListOfReadAccessResults() {
        return listOfReadAccessResults;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (listOfReadAccessResults == null ? 0 : listOfReadAccessResults.hashCode());
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
        final ReadPropertyMultipleAck other = (ReadPropertyMultipleAck) obj;
        if (listOfReadAccessResults == null) {
            if (other.listOfReadAccessResults != null)
                return false;
        } else if (!listOfReadAccessResults.equals(other.listOfReadAccessResults))
            return false;
        return true;
    }
}
