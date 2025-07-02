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

package com.serotonin.bacnet4j.type.constructed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ServicesSupported extends BitString {
    public ServicesSupported() {
        super(new boolean[44]);
    }

    public ServicesSupported(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean isAcknowledgeAlarm() {
        return getArrayValue(0);
    }

    public void setAcknowledgeAlarm(final boolean acknowledgeAlarm) {
        getValue()[0] = acknowledgeAlarm;
    }

    public boolean isConfirmedCovNotification() {
        return getArrayValue(1);
    }

    public void setConfirmedCovNotification(final boolean confirmedCovNotification) {
        getValue()[1] = confirmedCovNotification;
    }

    public boolean isConfirmedEventNotification() {
        return getArrayValue(2);
    }

    public void setConfirmedEventNotification(final boolean confirmedEventNotification) {
        getValue()[2] = confirmedEventNotification;
    }

    public boolean isGetAlarmSummary() {
        return getArrayValue(3);
    }

    public void setGetAlarmSummary(final boolean getAlarmSummary) {
        getValue()[3] = getAlarmSummary;
    }

    public boolean isGetEnrollmentSummary() {
        return getArrayValue(4);
    }

    public void setGetEnrollmentSummary(final boolean getEnrollmentSummary) {
        getValue()[4] = getEnrollmentSummary;
    }

    public boolean isSubscribeCov() {
        return getArrayValue(5);
    }

    public void setSubscribeCov(final boolean subscribeCov) {
        getValue()[5] = subscribeCov;
    }

    public boolean isAtomicReadFile() {
        return getArrayValue(6);
    }

    public void setAtomicReadFile(final boolean atomicReadFile) {
        getValue()[6] = atomicReadFile;
    }

    public boolean isAtomicWriteFile() {
        return getArrayValue(7);
    }

    public void setAtomicWriteFile(final boolean atomicWriteFile) {
        getValue()[7] = atomicWriteFile;
    }

    public boolean isAddListElement() {
        return getArrayValue(8);
    }

    public void setAddListElement(final boolean addListElement) {
        getValue()[8] = addListElement;
    }

    public boolean isRemoveListElement() {
        return getArrayValue(9);
    }

    public void setRemoveListElement(final boolean removeListElement) {
        getValue()[9] = removeListElement;
    }

    public boolean isCreateObject() {
        return getArrayValue(10);
    }

    public void setCreateObject(final boolean createObject) {
        getValue()[10] = createObject;
    }

    public boolean isDeleteObject() {
        return getArrayValue(11);
    }

    public void setDeleteObject(final boolean deleteObject) {
        getValue()[11] = deleteObject;
    }

    public boolean isReadProperty() {
        return getArrayValue(12);
    }

    public void setReadProperty(final boolean readProperty) {
        getValue()[12] = readProperty;
    }

    public boolean isReadPropertyMultiple() {
        return getArrayValue(14);
    }

    public void setReadPropertyMultiple(final boolean readPropertyMultiple) {
        getValue()[14] = readPropertyMultiple;
    }

    public boolean isWriteProperty() {
        return getArrayValue(15);
    }

    public void setWriteProperty(final boolean writeProperty) {
        getValue()[15] = writeProperty;
    }

    public boolean isWritePropertyMultiple() {
        return getArrayValue(16);
    }

    public void setWritePropertyMultiple(final boolean writePropertyMultiple) {
        getValue()[16] = writePropertyMultiple;
    }

    public boolean isDeviceCommunicationControl() {
        return getArrayValue(17);
    }

    public void setDeviceCommunicationControl(final boolean deviceCommunicationControl) {
        getValue()[17] = deviceCommunicationControl;
    }

    public boolean isConfirmedPrivateTransfer() {
        return getArrayValue(18);
    }

    public void setConfirmedPrivateTransfer(final boolean confirmedPrivateTransfer) {
        getValue()[18] = confirmedPrivateTransfer;
    }

    public boolean isConfirmedTextMessage() {
        return getArrayValue(19);
    }

    public void setConfirmedTextMessage(final boolean confirmedTextMessage) {
        getValue()[19] = confirmedTextMessage;
    }

    public boolean isReinitializeDevice() {
        return getArrayValue(20);
    }

    public void setReinitializeDevice(final boolean reinitializeDevice) {
        getValue()[20] = reinitializeDevice;
    }

    public boolean isVtOpen() {
        return getArrayValue(21);
    }

    public void setVtOpen(final boolean vtOpen) {
        getValue()[21] = vtOpen;
    }

    public boolean isVtClose() {
        return getArrayValue(22);
    }

    public void setVtClose(final boolean vtClose) {
        getValue()[22] = vtClose;
    }

    public boolean isVtData() {
        return getArrayValue(23);
    }

    public void setVtData(final boolean vtData) {
        getValue()[23] = vtData;
    }

    public boolean isIAm() {
        return getArrayValue(26);
    }

    public void setIAm(final boolean iAm) {
        getValue()[26] = iAm;
    }

    public boolean isIHave() {
        return getArrayValue(27);
    }

    public void setIHave(final boolean iHave) {
        getValue()[27] = iHave;
    }

    public boolean isUnconfirmedCovNotification() {
        return getArrayValue(28);
    }

    public void setUnconfirmedCovNotification(final boolean unconfirmedCovNotification) {
        getValue()[28] = unconfirmedCovNotification;
    }

    public boolean isUnconfirmedEventNotification() {
        return getArrayValue(29);
    }

    public void setUnconfirmedEventNotification(final boolean unconfirmedEventNotification) {
        getValue()[29] = unconfirmedEventNotification;
    }

    public boolean isUnconfirmedPrivateTransfer() {
        return getArrayValue(30);
    }

    public void setUnconfirmedPrivateTransfer(final boolean unconfirmedPrivateTransfer) {
        getValue()[30] = unconfirmedPrivateTransfer;
    }

    public boolean isUnconfirmedTextMessage() {
        return getArrayValue(31);
    }

    public void setUnconfirmedTextMessage(final boolean unconfirmedTextMessage) {
        getValue()[31] = unconfirmedTextMessage;
    }

    public boolean isTimeSynchronization() {
        return getArrayValue(32);
    }

    public void setTimeSynchronization(final boolean timeSynchronization) {
        getValue()[32] = timeSynchronization;
    }

    public boolean isWhoHas() {
        return getArrayValue(33);
    }

    public void setWhoHas(final boolean whoHas) {
        getValue()[33] = whoHas;
    }

    public boolean isWhoIs() {
        return getArrayValue(34);
    }

    public void setWhoIs(final boolean whoIs) {
        getValue()[34] = whoIs;
    }

    public boolean isReadRange() {
        return getArrayValue(35);
    }

    public void setReadRange(final boolean readRange) {
        getValue()[35] = readRange;
    }

    public boolean isUtcTimeSynchronization() {
        return getArrayValue(36);
    }

    public void setUtcTimeSynchronization(final boolean utcTimeSynchronization) {
        getValue()[36] = utcTimeSynchronization;
    }

    public boolean isLifeSafetyOperation() {
        return getArrayValue(37);
    }

    public void setLifeSafetyOperation(final boolean lifeSafetyOperation) {
        getValue()[37] = lifeSafetyOperation;
    }

    public boolean isSubscribeCovProperty() {
        return getArrayValue(38);
    }

    public void setSubscribeCovProperty(final boolean subscribeCovProperty) {
        getValue()[38] = subscribeCovProperty;
    }

    public boolean isGetEventInformation() {
        return getArrayValue(39);
    }

    public void setGetEventInformation(final boolean getEventInformation) {
        getValue()[39] = getEventInformation;
    }

    public boolean isWriteGroup() {
        return getArrayValue(40);
    }

    public void setWriteGroup(final boolean writeGroup) {
        getValue()[40] = writeGroup;
    }

    public boolean isSubscribeCovPropertyMultiple() {
        return getArrayValue(41);
    }

    public void setSubscribeCovPropertyMultiple(final boolean subscribeCovPropertyMultiple) {
        getValue()[41] = subscribeCovPropertyMultiple;
    }

    public boolean isConfirmedCovNotificationMultiple() {
        return getArrayValue(42);
    }

    public void setConfirmedCovNotificationMultiple(final boolean confirmedCovNotificationMultiple) {
        getValue()[42] = confirmedCovNotificationMultiple;
    }

    public boolean isUnconfirmedCovNotificationMultiple() {
        return getArrayValue(43);
    }

    public void setUnconfirmedCovNotificationMultiple(final boolean confirmedCovNotificationMultiple) {
        getValue()[43] = confirmedCovNotificationMultiple;
    }

    @Override
    public String toString() {
        try {
            StringBuilder result = new StringBuilder("ServicesSupported[");
            Method[] methods = this.getClass().getDeclaredMethods();
            boolean first = true;
            for (Method method : methods) {
                if (method.getReturnType() == boolean.class) {
                    if (first) {
                        first = false;
                    } else {
                        result.append(", ");
                    }
                    result.append(method.getName().substring(2));
                    result.append('=');
                    result.append(method.invoke(this));
                }
            }
            result.append(']');
            return result.toString();
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            //Should never happen
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

}
