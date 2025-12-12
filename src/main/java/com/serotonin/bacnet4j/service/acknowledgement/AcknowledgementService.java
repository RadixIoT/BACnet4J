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
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

abstract public class AcknowledgementService extends Service {
    public static AcknowledgementService createAcknowledgementService(final byte type, final ByteQueue queue)
            throws BACnetException {

        if (type == GetAlarmSummaryAck.TYPE_ID) // 3
            return new GetAlarmSummaryAck(queue);
        if (type == GetEnrollmentSummaryAck.TYPE_ID) // 4
            return new GetEnrollmentSummaryAck(queue);
        if (type == AtomicReadFileAck.TYPE_ID) // 6
            return new AtomicReadFileAck(queue);
        if (type == AtomicWriteFileAck.TYPE_ID) // 7
            return new AtomicWriteFileAck(queue);
        if (type == CreateObjectAck.TYPE_ID) // 10
            return new CreateObjectAck(queue);
        if (type == ReadPropertyAck.TYPE_ID) // 12
            return new ReadPropertyAck(queue);
        if (type == ReadPropertyMultipleAck.TYPE_ID) // 14
            return new ReadPropertyMultipleAck(queue);
        if (type == ConfirmedPrivateTransferAck.TYPE_ID) // 18
            return new ConfirmedPrivateTransferAck(queue);
        if (type == VtOpenAck.TYPE_ID) // 21
            return new VtOpenAck(queue);
        if (type == VtDataAck.TYPE_ID) // 23
            return new VtDataAck(queue);
        if (type == ReadRangeAck.TYPE_ID) // 26
            return new ReadRangeAck(queue);
        if (type == GetEventInformationAck.TYPE_ID) // 29
            return new GetEventInformationAck(queue);

        throw new BACnetException("Unsupported service acknowledgement: " + (type & 0xff));
    }
}
