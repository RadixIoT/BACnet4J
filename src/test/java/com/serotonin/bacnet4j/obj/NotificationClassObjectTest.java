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

package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.service.confirmed.AddListElementRequest;
import com.serotonin.bacnet4j.service.confirmed.RemoveListElementRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.RequestUtils;

public class NotificationClassObjectTest extends AbstractTest {
    NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        nc = new NotificationClassObject(d1, 0, "notifClass", 100, 5, 200, new EventTransitionBits(true, true, true));
    }

    /**
     * Ensures that notificationClass.recipientList can be modified with WriteProperty
     */
    @Test
    public void listValues() throws Exception {
        // Add a few items to the list.
        final Recipient recipient = new Recipient(new Address(new byte[] {3}));
        final Boolean issueConfirmedNotifications = Boolean.TRUE;
        final EventTransitionBits transitions = new EventTransitionBits(true, true, true);

        final Destination dest1 = new Destination(recipient, new UnsignedInteger(1), issueConfirmedNotifications,
                transitions);
        final Destination dest2 = new Destination(recipient, new UnsignedInteger(2), issueConfirmedNotifications,
                transitions);
        final Destination dest3 = new Destination(recipient, new UnsignedInteger(3), issueConfirmedNotifications,
                transitions);
        final Destination dest4 = new Destination(recipient, new UnsignedInteger(4), issueConfirmedNotifications,
                transitions);
        final Destination dest5 = new Destination(recipient, new UnsignedInteger(5), issueConfirmedNotifications,
                transitions);
        final AddListElementRequest aler = new AddListElementRequest(nc.getId(), PropertyIdentifier.recipientList, null,
                new SequenceOf<>(dest1, dest2, dest3));
        d2.send(rd1, aler).get();

        // Read the whole list
        SequenceOf<Destination> list = RequestUtils.getProperty(d2, rd1, nc.getId(), PropertyIdentifier.recipientList);
        assertEquals(list, new SequenceOf<>(dest1, dest2, dest3));

        // Write a couple more.
        d2.send(rd1, new AddListElementRequest(nc.getId(), PropertyIdentifier.recipientList, null,
                new SequenceOf<>(dest2, dest5, dest4))).get();
        list = RequestUtils.getProperty(d2, rd1, nc.getId(), PropertyIdentifier.recipientList);
        assertEquals(list, new SequenceOf<>(dest1, dest2, dest3, dest5, dest4));

        // Remove at an index.
        d2.send(rd1, new RemoveListElementRequest(nc.getId(), PropertyIdentifier.recipientList, null,
                new SequenceOf<Encodable>(dest2, dest5))).get();

        // Read the whole list
        list = RequestUtils.getProperty(d2, rd1, nc.getId(), PropertyIdentifier.recipientList);
        assertEquals(list, new SequenceOf<>(dest1, dest3, dest4));
    }
}
