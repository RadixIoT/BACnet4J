/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditOperationFlags extends BitString {
    public AuditOperationFlags() {
        super(new boolean[16]);
    }

    public AuditOperationFlags(
            boolean read,
            boolean write,
            boolean create,
            boolean delete,
            boolean lifeSafety,
            boolean acknowledgeAlarm,
            boolean deviceDisableComm,
            boolean deviceEnableComm,
            boolean deviceReset,
            boolean deviceBackup,
            boolean deviceRestore,
            boolean subscription,
            boolean notification,
            boolean auditingFailure,
            boolean networkChanges,
            boolean general
    ) {
        super(new boolean[] {
                read,
                write,
                create,
                delete,
                lifeSafety,
                acknowledgeAlarm,
                deviceDisableComm,
                deviceEnableComm,
                deviceReset,
                deviceBackup,
                deviceRestore,
                subscription,
                notification,
                auditingFailure,
                networkChanges,
                general
        });
    }

    public AuditOperationFlags(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean isRead() {
        return getValue()[0];
    }

    public void setRead(boolean b) {
        getValue()[0] = b;
    }

    public boolean isWrite() {
        return getValue()[1];
    }

    public void setWrite(boolean b) {
        getValue()[1] = b;
    }

    public boolean isCreate() {
        return getValue()[2];
    }

    public void setCreate(boolean b) {
        getValue()[2] = b;
    }

    public boolean isDelete() {
        return getValue()[3];
    }

    public void setDelete(boolean b) {
        getValue()[3] = b;
    }

    public boolean isLifeSafety() {
        return getValue()[4];
    }

    public void setLifeSafety(boolean b) {
        getValue()[4] = b;
    }

    public boolean isAcknowledgeAlarm() {
        return getValue()[5];
    }

    public void setAcknowledgeAlarm(boolean b) {
        getValue()[5] = b;
    }

    public boolean isDeviceDisableComm() {
        return getValue()[6];
    }

    public void setDeviceDisableComm(boolean b) {
        getValue()[6] = b;
    }

    public boolean isDeviceEnableComm() {
        return getValue()[7];
    }

    public void setDeviceEnableComm(boolean b) {
        getValue()[7] = b;
    }

    public boolean isDeviceReset() {
        return getValue()[8];
    }

    public void setDeviceReset(boolean b) {
        getValue()[8] = b;
    }

    public boolean isDeviceBackup() {
        return getValue()[9];
    }

    public void setDeviceBackup(boolean b) {
        getValue()[9] = b;
    }

    public boolean isDeviceRestore() {
        return getValue()[10];
    }

    public void setDeviceRestore(boolean b) {
        getValue()[10] = b;
    }

    public boolean isSubscription() {
        return getValue()[11];
    }

    public void setSubscription(boolean b) {
        getValue()[11] = b;
    }

    public boolean isNotification() {
        return getValue()[12];
    }

    public void setNotification(boolean b) {
        getValue()[12] = b;
    }

    public boolean isAuditingFailure() {
        return getValue()[13];
    }

    public void setAuditingFailure(boolean b) {
        getValue()[13] = b;
    }

    public boolean isNetworkChanges() {
        return getValue()[14];
    }

    public void setNetworkChanges(boolean b) {
        getValue()[14] = b;
    }

    public boolean isGeneral() {
        return getValue()[15];
    }

    public void setGeneral(boolean b) {
        getValue()[15] = b;
    }

    @Override
    public String toString() {
        return "AuditOperationFlags [" +
                "read=" + isRead() +
                ", write=" + isWrite() +
                ", create=" + isCreate() +
                ", delete=" + isDelete() +
                ", life-safety=" + isLifeSafety() +
                ", acknowledge-alarm=" + isAcknowledgeAlarm() +
                ", device-disable-comm=" + isDeviceDisableComm() +
                ", device-enable-comm=" + isDeviceEnableComm() +
                ", device-reset=" + isDeviceReset() +
                ", device-backup=" + isDeviceBackup() +
                ", device-restore=" + isDeviceRestore() +
                ", subscription=" + isSubscription() +
                ", notification=" + isNotification() +
                ", auditing-failure=" + isAuditingFailure() +
                ", network-changes=" + isNetworkChanges() +
                ", general=" + isGeneral() +
                "]";
    }
}
