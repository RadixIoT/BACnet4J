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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class UnconfirmedEventNotificationRequestTest {
    /**
     * Event Values is conditional (13.9.1.1.13) and is omitted for ACK_NOTIFICATION. Decoding must accept its absence.
     */
    @Test
    public void ackNotificationWithoutEventValues() throws Exception {
        UnconfirmedEventNotificationRequest request = new UnconfirmedEventNotificationRequest(new UnsignedInteger(1),
                new ObjectIdentifier(ObjectType.device, 9), new ObjectIdentifier(ObjectType.analogInput, 2),
                new TimeStamp(new UnsignedInteger(16)), new UnsignedInteger(4), new UnsignedInteger(100),
                EventType.outOfRange, null, NotifyType.ackNotification, null, null, EventState.highLimit, null);

        ByteQueue queue = new ByteQueue();
        request.write(queue);

        UnconfirmedEventNotificationRequest parsed = new UnconfirmedEventNotificationRequest(queue);
        assertEquals(0, queue.size());
        assertEquals(request, parsed);
    }
}
