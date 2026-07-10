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
import com.serotonin.bacnet4j.exception.BACnetRejectException;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.RejectReason;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public abstract class ConfirmedRequestService extends Service {
    public static void checkConfirmedRequestService(ServicesSupported services, byte type)
            throws BACnetRejectException {
        if (type == AcknowledgeAlarmRequest.TYPE_ID && services.isAcknowledgeAlarm()) // 0
            return;
        if (type == ConfirmedCovNotificationRequest.TYPE_ID && services.isConfirmedCovNotification()) // 1
            return;
        if (type == ConfirmedEventNotificationRequest.TYPE_ID && services.isConfirmedEventNotification()) // 2
            return;
        if (type == GetAlarmSummaryRequest.TYPE_ID && services.isGetAlarmSummary()) // 3
            return;
        if (type == GetEnrollmentSummaryRequest.TYPE_ID && services.isGetEnrollmentSummary()) // 4
            return;
        if (type == SubscribeCOVRequest.TYPE_ID && services.isSubscribeCov()) // 5
            return;
        if (type == AtomicReadFileRequest.TYPE_ID && services.isAtomicReadFile()) // 6
            return;
        if (type == AtomicWriteFileRequest.TYPE_ID && services.isAtomicWriteFile()) // 7
            return;
        if (type == AddListElementRequest.TYPE_ID && services.isAddListElement()) // 8
            return;
        if (type == RemoveListElementRequest.TYPE_ID && services.isRemoveListElement()) // 9
            return;
        if (type == CreateObjectRequest.TYPE_ID && services.isCreateObject()) // 10
            return;
        if (type == DeleteObjectRequest.TYPE_ID && services.isDeleteObject()) // 11
            return;
        if (type == ReadPropertyRequest.TYPE_ID && services.isReadProperty()) // 12
            return;
        if (type == ReadPropertyMultipleRequest.TYPE_ID && services.isReadPropertyMultiple()) // 14
            return;
        if (type == WritePropertyRequest.TYPE_ID && services.isWriteProperty()) // 15
            return;
        if (type == WritePropertyMultipleRequest.TYPE_ID && services.isWritePropertyMultiple()) // 16
            return;
        if (type == DeviceCommunicationControlRequest.TYPE_ID && services.isDeviceCommunicationControl()) // 17
            return;
        if (type == ConfirmedPrivateTransferRequest.TYPE_ID && services.isConfirmedPrivateTransfer()) // 18
            return;
        if (type == ConfirmedTextMessageRequest.TYPE_ID && services.isConfirmedTextMessage()) // 19
            return;
        if (type == ReinitializeDeviceRequest.TYPE_ID && services.isReinitializeDevice()) // 20
            return;
        if (type == VtOpenRequest.TYPE_ID && services.isVtOpen()) // 21
            return;
        if (type == VtCloseRequest.TYPE_ID && services.isVtClose()) // 22
            return;
        if (type == VtDataRequest.TYPE_ID && services.isVtData()) // 23
            return;
        if (type == ReadRangeRequest.TYPE_ID && services.isReadRange()) // 26
            return;
        if (type == LifeSafetyOperationRequest.TYPE_ID && services.isLifeSafetyOperation()) // 27
            return;
        if (type == SubscribeCOVPropertyRequest.TYPE_ID && services.isSubscribeCovProperty()) // 28
            return;
        if (type == GetEventInformationRequest.TYPE_ID && services.isGetEventInformation()) // 29
            return;
        if (type == SubscribeCOVPropertyMultipleRequest.TYPE_ID && services.isSubscribeCovPropertyMultiple()) // 30
            return;
        if (type == ConfirmedCovNotificationMultipleRequest.TYPE_ID && services.isConfirmedCovNotificationMultiple()) // 31
            return;
        if (type == ConfirmedAuditNotificationRequest.TYPE_ID && services.isConfirmedAuditNotification()) // 32
            return;
        if (type == AuditLogQueryRequest.TYPE_ID && services.isAuditLogQuery()) // 33
            return;
        if (type == AuthRequestRequest.TYPE_ID && services.isAuthRequest()) // 34
            return;

        // 135-2016 18.9 - Confirmed request PDUs can be rejected. So we have to return an RejectException.
        throw new BACnetRejectException(RejectReason.unrecognizedService);
    }

    public static ConfirmedRequestService createConfirmedRequestService(byte type, ByteQueue queue)
            throws BACnetException {
        ConfirmedRequestService result = switch (type) {
            case AcknowledgeAlarmRequest.TYPE_ID -> new AcknowledgeAlarmRequest(queue); // 0
            case ConfirmedCovNotificationRequest.TYPE_ID -> new ConfirmedCovNotificationRequest(queue); // 1
            case ConfirmedEventNotificationRequest.TYPE_ID -> new ConfirmedEventNotificationRequest(queue); // 2
            case GetAlarmSummaryRequest.TYPE_ID -> new GetAlarmSummaryRequest(queue); // 3
            case GetEnrollmentSummaryRequest.TYPE_ID -> new GetEnrollmentSummaryRequest(queue); // 4
            case SubscribeCOVRequest.TYPE_ID -> new SubscribeCOVRequest(queue); // 5
            case AtomicReadFileRequest.TYPE_ID -> new AtomicReadFileRequest(queue); // 6
            case AtomicWriteFileRequest.TYPE_ID -> new AtomicWriteFileRequest(queue); // 7
            case AddListElementRequest.TYPE_ID -> new AddListElementRequest(queue); // 8
            case RemoveListElementRequest.TYPE_ID -> new RemoveListElementRequest(queue); // 9
            case CreateObjectRequest.TYPE_ID -> new CreateObjectRequest(queue); // 10
            case DeleteObjectRequest.TYPE_ID -> new DeleteObjectRequest(queue); // 11
            case ReadPropertyRequest.TYPE_ID -> new ReadPropertyRequest(queue); // 12
            case ReadPropertyMultipleRequest.TYPE_ID -> new ReadPropertyMultipleRequest(queue); // 14
            case WritePropertyRequest.TYPE_ID -> new WritePropertyRequest(queue); // 15
            case WritePropertyMultipleRequest.TYPE_ID -> new WritePropertyMultipleRequest(queue); // 16
            case DeviceCommunicationControlRequest.TYPE_ID -> new DeviceCommunicationControlRequest(queue); // 17
            case ConfirmedPrivateTransferRequest.TYPE_ID -> new ConfirmedPrivateTransferRequest(queue); // 18
            case ConfirmedTextMessageRequest.TYPE_ID -> new ConfirmedTextMessageRequest(queue); // 19
            case ReinitializeDeviceRequest.TYPE_ID -> new ReinitializeDeviceRequest(queue); // 20
            case VtOpenRequest.TYPE_ID -> new VtOpenRequest(queue); // 21
            case VtCloseRequest.TYPE_ID -> new VtCloseRequest(queue); // 22
            case VtDataRequest.TYPE_ID -> new VtDataRequest(queue); // 23
            case ReadRangeRequest.TYPE_ID -> new ReadRangeRequest(queue); // 26
            case LifeSafetyOperationRequest.TYPE_ID -> new LifeSafetyOperationRequest(queue); // 27
            case SubscribeCOVPropertyRequest.TYPE_ID -> new SubscribeCOVPropertyRequest(queue); // 28
            case GetEventInformationRequest.TYPE_ID -> new GetEventInformationRequest(queue); // 29
            case SubscribeCOVPropertyMultipleRequest.TYPE_ID -> new SubscribeCOVPropertyMultipleRequest(queue); // 30
            case ConfirmedCovNotificationMultipleRequest.TYPE_ID ->
                    new ConfirmedCovNotificationMultipleRequest(queue); // 31
            case ConfirmedAuditNotificationRequest.TYPE_ID -> new ConfirmedAuditNotificationRequest(queue); // 32
            case AuditLogQueryRequest.TYPE_ID -> new AuditLogQueryRequest(queue); // 33
            case AuthRequestRequest.TYPE_ID -> new AuthRequestRequest(queue); // 34
            default ->
                //Standard test 135.1-2013, 9.39.1
                    throw new BACnetRejectException(RejectReason.unrecognizedService);
        };
        // To pass the standard test 135.1-2013 13.4.5 we have to check if too many arguments have been sent.
        if (queue.size() != 0) {
            throw new BACnetRejectException(RejectReason.tooManyArguments);
        }
        return result;
    }

    public abstract AcknowledgementService handle(LocalDevice localDevice, Address from) throws BACnetException;

    /**
     * This method determines whether responses to requests are sent when the device has had its communication set
     * to disabled with a DeviceCommunicationControlRequest. Method for maintaining this state are found in
     * LocalDevice. This method defaults to false, and is overridden by requests as necessary.
     */
    public boolean isCommunicationControlOverride() {
        return false;
    }
}
