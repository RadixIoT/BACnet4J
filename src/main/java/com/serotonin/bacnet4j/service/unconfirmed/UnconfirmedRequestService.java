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

package com.serotonin.bacnet4j.service.unconfirmed;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public abstract class UnconfirmedRequestService extends Service {
    public static void checkUnconfirmedRequestService(ServicesSupported services, byte type)
            throws BACnetException {
        if (type == IAmRequest.TYPE_ID && services.isIAm()) // 0
            return;
        if (type == IHaveRequest.TYPE_ID && services.isIHave()) // 1
            return;
        if (type == UnconfirmedCovNotificationRequest.TYPE_ID && services.isUnconfirmedCovNotification()) // 2
            return;
        if (type == UnconfirmedEventNotificationRequest.TYPE_ID && services.isUnconfirmedEventNotification()) // 3
            return;
        if (type == UnconfirmedPrivateTransferRequest.TYPE_ID && services.isUnconfirmedPrivateTransfer()) // 4
            return;
        if (type == UnconfirmedTextMessageRequest.TYPE_ID && services.isUnconfirmedTextMessage()) // 5
            return;
        if (type == TimeSynchronizationRequest.TYPE_ID && services.isTimeSynchronization()) // 6
            return;
        if (type == WhoHasRequest.TYPE_ID && services.isWhoHas()) // 7
            return;
        if (type == WhoIsRequest.TYPE_ID && services.isWhoIs()) // 8
            return;
        if (type == UTCTimeSynchronizationRequest.TYPE_ID && services.isUtcTimeSynchronization()) // 9
            return;
        if (type == WriteGroupRequest.TYPE_ID && services.isWriteGroup()) // 10
            return;
        if (type == UnconfirmedCovNotificationMultipleRequest.TYPE_ID
                && services.isUnconfirmedCovNotificationMultiple()) // 11
            return;
        if (type == UnconfirmedAuditNotification.TYPE_ID && services.isUnconfirmedAuditNotification()) // 12
            return;
        if (type == WhoAmIRequest.TYPE_ID && services.isWhoAmI()) // 13
            return;
        if (type == YouAreRequest.TYPE_ID && services.isYouAre()) // 14
            return;

        throw new BACnetErrorException(ErrorClass.device, ErrorCode.serviceRequestDenied);
    }

    public static UnconfirmedRequestService createUnconfirmedRequestService(byte type, ByteQueue queue)
            throws BACnetException {
        return switch (type) {
            case IAmRequest.TYPE_ID -> new IAmRequest(queue);
            case IHaveRequest.TYPE_ID -> new IHaveRequest(queue);
            case UnconfirmedCovNotificationRequest.TYPE_ID -> new UnconfirmedCovNotificationRequest(queue);
            case UnconfirmedEventNotificationRequest.TYPE_ID -> new UnconfirmedEventNotificationRequest(queue);
            case UnconfirmedPrivateTransferRequest.TYPE_ID -> new UnconfirmedPrivateTransferRequest(queue);
            case UnconfirmedTextMessageRequest.TYPE_ID -> new UnconfirmedTextMessageRequest(queue);
            case TimeSynchronizationRequest.TYPE_ID -> new TimeSynchronizationRequest(queue);
            case WhoHasRequest.TYPE_ID -> new WhoHasRequest(queue);
            case WhoIsRequest.TYPE_ID -> new WhoIsRequest(queue);
            case UTCTimeSynchronizationRequest.TYPE_ID -> new UTCTimeSynchronizationRequest(queue);
            case WriteGroupRequest.TYPE_ID -> new WriteGroupRequest(queue);
            case UnconfirmedCovNotificationMultipleRequest.TYPE_ID ->
                    new UnconfirmedCovNotificationMultipleRequest(queue);
            case UnconfirmedAuditNotification.TYPE_ID -> new UnconfirmedAuditNotification(queue);
            case WhoAmIRequest.TYPE_ID -> new WhoAmIRequest(queue);
            case YouAreRequest.TYPE_ID -> new YouAreRequest(queue);
            default -> throw new BACnetException("Unsupported unconfirmed service: " + (type & 0xff));
        };
    }

    public abstract void handle(LocalDevice localDevice, Address from) throws BACnetException;
}
