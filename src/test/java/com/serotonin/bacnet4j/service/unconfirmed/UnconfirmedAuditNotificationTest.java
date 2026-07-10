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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.AuditNotification;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.AuditOperation;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class UnconfirmedAuditNotificationTest {
    @Test
    public void roundTrip() throws BACnetException {
        AuditNotification n = new AuditNotification(
                null, null,
                new Recipient(new ObjectIdentifier(ObjectType.device, 100)),
                null,
                AuditOperation.deviceReset,
                null, null, null, null, null,
                new Recipient(new ObjectIdentifier(ObjectType.device, 200)),
                null, null, null, null, null, null);

        UnconfirmedAuditNotification req = new UnconfirmedAuditNotification(new SequenceOf<>(n));

        ByteQueue queue = new ByteQueue();
        req.write(queue);

        assertEquals(req, new UnconfirmedAuditNotification(queue));
    }
}
