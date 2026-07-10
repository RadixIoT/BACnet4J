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

package com.serotonin.bacnet4j.service.unconfirmed;

import java.util.Objects;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.NotImplementedException;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.AuditNotification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class UnconfirmedAuditNotification extends UnconfirmedRequestService {
    public static final byte TYPE_ID = 12;

    private final SequenceOf<AuditNotification> notifications;

    public UnconfirmedAuditNotification(SequenceOf<AuditNotification> notifications) {
        this.notifications = notifications;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    public SequenceOf<AuditNotification> getNotifications() {
        return notifications;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, notifications, 0);
    }

    UnconfirmedAuditNotification(ByteQueue queue) throws BACnetException {
        notifications = readSequenceOf(queue, AuditNotification.class, 0);
    }

    @Override
    public void handle(LocalDevice localDevice, Address from) throws BACnetException {
        throw new NotImplementedException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        UnconfirmedAuditNotification that = (UnconfirmedAuditNotification) o;
        return Objects.equals(notifications, that.notifications);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(notifications);
    }
}
