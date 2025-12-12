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

package com.serotonin.bacnet4j.npdu;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.ConfirmedRequest;
import com.serotonin.bacnet4j.apdu.UnconfirmedRequest;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.enums.MaxSegments;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedTextMessageRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedPrivateTransferRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedTextMessageRequest;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfBitStringNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class NetworkPriorityTest {
    @Test
    public void test() throws Exception {
        Assert.assertEquals(3, getNetworkPriorityCEN(5));
        Assert.assertEquals(3, getNetworkPriorityCEN(63));
        Assert.assertEquals(2, getNetworkPriorityCEN(64));
        Assert.assertEquals(2, getNetworkPriorityCEN(127));
        Assert.assertEquals(1, getNetworkPriorityCEN(128));
        Assert.assertEquals(1, getNetworkPriorityCEN(191));
        Assert.assertEquals(0, getNetworkPriorityCEN(192));

        Assert.assertEquals(3, getNetworkPriorityUEN(5));
        Assert.assertEquals(3, getNetworkPriorityUEN(63));
        Assert.assertEquals(2, getNetworkPriorityUEN(64));
        Assert.assertEquals(2, getNetworkPriorityUEN(127));
        Assert.assertEquals(1, getNetworkPriorityUEN(128));
        Assert.assertEquals(1, getNetworkPriorityUEN(191));
        Assert.assertEquals(0, getNetworkPriorityUEN(192));

        Assert.assertEquals(1, getNetworkPriorityCTM(MessagePriority.urgent));
        Assert.assertEquals(0, getNetworkPriorityCTM(MessagePriority.normal));

        Assert.assertEquals(1, getNetworkPriorityUTM(MessagePriority.urgent));
        Assert.assertEquals(0, getNetworkPriorityUTM(MessagePriority.normal));

        Assert.assertEquals(0, getNetworkPriorityOther());
    }

    private static int getNetworkPriorityCEN(final int eventPriority) throws Exception {
        final ConfirmedEventNotificationRequest req = new ConfirmedEventNotificationRequest(new UnsignedInteger(2),
                new ObjectIdentifier(ObjectType.device, 8), new ObjectIdentifier(ObjectType.analogInput, 9),
                new TimeStamp(new DateTime(123456789)), new UnsignedInteger(3), new UnsignedInteger(eventPriority),
                EventType.changeOfBitstring, new CharacterString("hi"), NotifyType.event, Boolean.FALSE,
                EventState.normal, EventState.offnormal,
                new NotificationParameters(
                        new ChangeOfBitStringNotif(new BitString(new boolean[] {false, true, false, true}),
                                new StatusFlags(true, false, false, false))));
        final ConfirmedRequest apdu = new ConfirmedRequest(false, false, true, MaxSegments.MORE_THAN_64,
                MaxApduLength.UP_TO_1476, (byte) 45, 0, 5, req);
        return getNetworkPriority(apdu);
    }

    private static int getNetworkPriorityUEN(final int eventPriority) throws Exception {
        final UnconfirmedEventNotificationRequest req = new UnconfirmedEventNotificationRequest(new UnsignedInteger(2),
                new ObjectIdentifier(ObjectType.device, 8), new ObjectIdentifier(ObjectType.analogInput, 9),
                new TimeStamp(new DateTime(123456789)), new UnsignedInteger(3), new UnsignedInteger(eventPriority),
                EventType.changeOfBitstring, new CharacterString("hi"), NotifyType.event, Boolean.FALSE,
                EventState.normal, EventState.offnormal,
                new NotificationParameters(
                        new ChangeOfBitStringNotif(new BitString(new boolean[] {false, true, false, true}),
                                new StatusFlags(true, false, false, false))));

        final UnconfirmedRequest apdu = new UnconfirmedRequest(req);

        return getNetworkPriority(apdu);
    }

    private static int getNetworkPriorityCTM(final MessagePriority priority) throws Exception {
        final ConfirmedTextMessageRequest req = new ConfirmedTextMessageRequest(
                new ObjectIdentifier(ObjectType.device, 8), priority, new CharacterString("hi"));
        final ConfirmedRequest apdu = new ConfirmedRequest(false, false, true, MaxSegments.MORE_THAN_64,
                MaxApduLength.UP_TO_1476, (byte) 45, 0, 5, req);
        return getNetworkPriority(apdu);
    }

    private static int getNetworkPriorityUTM(final MessagePriority priority) throws Exception {
        final UnconfirmedTextMessageRequest req = new UnconfirmedTextMessageRequest(
                new ObjectIdentifier(ObjectType.device, 8), priority, new CharacterString("hi"));
        final UnconfirmedRequest apdu = new UnconfirmedRequest(req);
        return getNetworkPriority(apdu);
    }

    private static int getNetworkPriorityOther() throws Exception {
        final UnconfirmedPrivateTransferRequest req = new UnconfirmedPrivateTransferRequest(11, 12, new Real(3.14F));
        final UnconfirmedRequest apdu = new UnconfirmedRequest(req);
        return getNetworkPriority(apdu);
    }

    private static int getNetworkPriority(final APDU apdu) throws Exception {
        final ByteQueue queue = new ByteQueue();
        final Network network = new Network() {
            @Override
            public void terminate() {
                throw new RuntimeException();
            }

            @Override
            public void sendNPDU(final Address recipient, final OctetString router, final ByteQueue npdu,
                    final boolean broadcast, final boolean expectsReply) throws BACnetException {
                queue.push(npdu);
            }

            @Override
            protected NPDU handleIncomingDataImpl(final ByteQueue queue, final OctetString linkService)
                    throws Exception {
                throw new RuntimeException();
            }

            @Override
            public NetworkIdentifier getNetworkIdentifier() {
                throw new RuntimeException();
            }

            @Override
            public MaxApduLength getMaxApduLength() {
                throw new RuntimeException();
            }

            @Override
            public long getBytesOut() {
                throw new RuntimeException();
            }

            @Override
            public long getBytesIn() {
                throw new RuntimeException();
            }

            @Override
            protected OctetString getBroadcastMAC() {
                throw new RuntimeException();
            }

            @Override
            public Address[] getAllLocalAddresses() {
                return new Address[] {new Address(2, new byte[] {1})};
            }

            @Override
            public Address getLoopbackAddress() {
                throw new RuntimeException();
            }

        };

        network.sendAPDU(new Address(2, new byte[] {2}), new OctetString(new byte[] {5}), apdu, false);

        queue.pop();
        final byte control = queue.pop();

        return control & 0x3;
    }
}
