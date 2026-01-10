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
    public static void checkConfirmedRequestService(final ServicesSupported services, final byte type)
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

        // 135-2016 18.9 - Confirmed request PDUs can be rejected. So we have to return an RejectException.
        throw new BACnetRejectException(RejectReason.unrecognizedService);
    }

    public static ConfirmedRequestService createConfirmedRequestService(final byte type, final ByteQueue queue)
            throws BACnetException {

        ConfirmedRequestService result;
        switch (type) {
            case AcknowledgeAlarmRequest.TYPE_ID: // 0
                result = new AcknowledgeAlarmRequest(queue);
                break;
            case ConfirmedCovNotificationRequest.TYPE_ID: // 1
                result = new ConfirmedCovNotificationRequest(queue);
                break;
            case ConfirmedEventNotificationRequest.TYPE_ID: // 2
                result = new ConfirmedEventNotificationRequest(queue);
                break;
            case GetAlarmSummaryRequest.TYPE_ID: // 3
                result = new GetAlarmSummaryRequest(queue);
                break;
            case GetEnrollmentSummaryRequest.TYPE_ID: // 4
                result = new GetEnrollmentSummaryRequest(queue);
                break;
            case SubscribeCOVRequest.TYPE_ID: // 5
                result = new SubscribeCOVRequest(queue);
                break;
            case AtomicReadFileRequest.TYPE_ID: // 6
                result = new AtomicReadFileRequest(queue);
                break;
            case AtomicWriteFileRequest.TYPE_ID: // 7
                result = new AtomicWriteFileRequest(queue);
                break;
            case AddListElementRequest.TYPE_ID: // 8
                result = new AddListElementRequest(queue);
                break;
            case RemoveListElementRequest.TYPE_ID: // 9
                result = new RemoveListElementRequest(queue);
                break;
            case CreateObjectRequest.TYPE_ID: // 10
                result = new CreateObjectRequest(queue);
                break;
            case DeleteObjectRequest.TYPE_ID: // 11
                result = new DeleteObjectRequest(queue);
                break;
            case ReadPropertyRequest.TYPE_ID: // 12
                result = new ReadPropertyRequest(queue);
                break;
            case ReadPropertyMultipleRequest.TYPE_ID: // 14
                result = new ReadPropertyMultipleRequest(queue);
                break;
            case WritePropertyRequest.TYPE_ID: // 15
                result = new WritePropertyRequest(queue);
                break;
            case WritePropertyMultipleRequest.TYPE_ID: // 16
                result = new WritePropertyMultipleRequest(queue);
                break;
            case DeviceCommunicationControlRequest.TYPE_ID: // 17
                result = new DeviceCommunicationControlRequest(queue);
                break;
            case ConfirmedPrivateTransferRequest.TYPE_ID: // 18
                result = new ConfirmedPrivateTransferRequest(queue);
                break;
            case ConfirmedTextMessageRequest.TYPE_ID: // 19
                result = new ConfirmedTextMessageRequest(queue);
                break;
            case ReinitializeDeviceRequest.TYPE_ID: // 20
                result = new ReinitializeDeviceRequest(queue);
                break;
            case VtOpenRequest.TYPE_ID: // 21
                result = new VtOpenRequest(queue);
                break;
            case VtCloseRequest.TYPE_ID: // 22
                result = new VtCloseRequest(queue);
                break;
            case VtDataRequest.TYPE_ID: // 23
                result = new VtDataRequest(queue);
                break;
            case ReadRangeRequest.TYPE_ID: // 26
                result = new ReadRangeRequest(queue);
                break;
            case LifeSafetyOperationRequest.TYPE_ID: // 27
                result = new LifeSafetyOperationRequest(queue);
                break;
            case SubscribeCOVPropertyRequest.TYPE_ID: // 28
                result = new SubscribeCOVPropertyRequest(queue);
                break;
            case GetEventInformationRequest.TYPE_ID: // 29
                result = new GetEventInformationRequest(queue);
                break;
            case SubscribeCOVPropertyMultipleRequest.TYPE_ID: // 30
                result = new SubscribeCOVPropertyMultipleRequest(queue);
                break;
            case ConfirmedCovNotificationMultipleRequest.TYPE_ID: // 31
                result = new ConfirmedCovNotificationMultipleRequest(queue);
                break;
            default:
                //Standard test 135.1-2013, 9.39.1
                throw new BACnetRejectException(RejectReason.unrecognizedService);
        }
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
