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

import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PropertyStates;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.Polarity;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfState;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfStateNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class BinaryInputObjectTest extends AbstractTest {
    private BinaryInputObject bi;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        bi = new BinaryInputObject(d1, 0, "bi", BinaryPV.inactive, false, Polarity.normal);
        nc = new NotificationClassObject(d1, 17, "nc17", 100, 5, 200, new EventTransitionBits(false, false, false));
    }

    @Test
    public void initialization() throws Exception {
        new BinaryInputObject(d1, 1, "bi1", BinaryPV.inactive, true, Polarity.normal);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        bi.supportIntrinsicReporting(5, 17, BinaryPV.active, new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        quiesce();
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(BinaryPV.inactive, bi.get(PropertyIdentifier.presentValue));
        assertEquals(BinaryPV.active, bi.get(PropertyIdentifier.alarmValue));

        // Do a state change. Write a value to indicate a change of state failure. After 5s the alarm will be raised.
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        clock.plusMillis(4500);
        quiesce();
        assertEquals(EventState.normal, bi.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.offnormal, bi.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), bi.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(bi.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) bi.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.changeOfState, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.offnormal, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(BinaryPV.active),
                new StatusFlags(true, false, false, false))), notif.eventValues());

        // Return to normal. After 12s the notification will be sent.
        bi.writePropertyInternal(PropertyIdentifier.alarmValue, BinaryPV.inactive);
        clock.plusMillis(11500);
        quiesce();
        assertEquals(EventState.offnormal,
                bi.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, bi.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), bi.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(bi.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) bi.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfState, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.offnormal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(BinaryPV.active),
                new StatusFlags(false, false, false, false))), notif.eventValues());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReporting() throws Exception {
        final DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(1, bi.getId(), PropertyIdentifier.presentValue);
        final EventEnrollmentObject ee = new EventEnrollmentObject(d1, 0, "ee", ref, NotifyType.alarm,
                new EventParameter(new ChangeOfState(new UnsignedInteger(30),
                        new SequenceOf<>(new PropertyStates(BinaryPV.active)))),
                new EventTransitionBits(true, true, true), 17, 1000, null, null);

        // Set up the notification destination
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Ensure that initializing the event enrollment object didn't fire any notifications.
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        //
        // Go to alarm value
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        // Allow the EE to poll
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until just before the time delay.
        clock.plusMillis(29500);
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until after the time delay.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.offnormal, ee.readProperty(PropertyIdentifier.eventState));

        // Ensure that a proper looking event notification was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.changeOfState, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.offnormal, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(BinaryPV.active),
                new StatusFlags(false, false, false, false))), notif.eventValues());

        //
        // Return to normal
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
        // Allow the EE to poll
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.offnormal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until just before the time delay.
        clock.plusMillis(29500);
        quiesce();
        assertEquals(EventState.offnormal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until after the time delay.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));

        // Ensure that a proper looking event notification was received.
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfState, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.offnormal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(BinaryPV.inactive),
                new StatusFlags(false, false, false, false))), notif.eventValues());
    }

    @Test
    public void activeTime() throws Exception {
        final DateTime start = new DateTime(d1);

        bi.supportActiveTime();

        clock.plusMillis(3500);
        assertEquals(UnsignedInteger.ZERO, bi.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        clock.plusMillis(1600); // Total active time 1600ms
        assertEquals(new UnsignedInteger(1), bi.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        clock.plusMillis(1600); // Total active time 3200ms
        assertEquals(new UnsignedInteger(3), bi.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
        clock.plusMillis(7000);
        assertEquals(new UnsignedInteger(3), bi.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
        clock.plusMillis(500);
        assertEquals(new UnsignedInteger(3), bi.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        clock.plusMillis(4700); // Total active time 7900ms
        assertEquals(new UnsignedInteger(7), bi.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        bi.writePropertyInternal(PropertyIdentifier.elapsedActiveTime, new UnsignedInteger(5));
        assertEquals(new UnsignedInteger(5), bi.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        clock.plusMillis(1234);
        bi.writePropertyInternal(PropertyIdentifier.elapsedActiveTime, UnsignedInteger.ZERO);
        assertEquals(UnsignedInteger.ZERO, bi.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(new DateTime(d1), bi.readProperty(PropertyIdentifier.timeOfActiveTimeReset));
    }

    @Test
    public void stateChanges() throws Exception {
        final DateTime start = new DateTime(d1);
        assertEquals(DateTime.UNSPECIFIED, bi.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(UnsignedInteger.ZERO, bi.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(4);
        final DateTime t1 = new DateTime(d1);
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        assertEquals(t1, bi.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(new UnsignedInteger(1), bi.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(5);
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        assertEquals(t1, bi.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(new UnsignedInteger(1), bi.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(6);
        final DateTime t2 = new DateTime(d1);
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
        assertEquals(t2, bi.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(new UnsignedInteger(4), bi.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(7);
        bi.writePropertyInternal(PropertyIdentifier.changeOfStateCount, new UnsignedInteger(123));
        assertEquals(t2, bi.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(new UnsignedInteger(123), bi.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, bi.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(8);
        final DateTime t3 = new DateTime(d1);
        bi.writePropertyInternal(PropertyIdentifier.changeOfStateCount, UnsignedInteger.ZERO);
        assertEquals(t2, bi.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(UnsignedInteger.ZERO, bi.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(t3, bi.readProperty(PropertyIdentifier.timeOfStateCountReset));
    }

    @Test
    public void physicalState() {
        // Ensure the default state.
        assertEquals(Boolean.FALSE, bi.get(PropertyIdentifier.outOfService));
        assertEquals(BinaryPV.inactive, bi.get(PropertyIdentifier.presentValue));
        assertEquals(Polarity.normal, bi.get(PropertyIdentifier.polarity));

        // false, inactive, normal
        assertEquals(BinaryPV.inactive, bi.getPhysicalState());

        // true, inactive, normal
        bi.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.TRUE);
        assertEquals(BinaryPV.inactive, bi.getPhysicalState());

        // true, active, normal
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        assertEquals(BinaryPV.active, bi.getPhysicalState());

        // false, active, normal
        bi.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.FALSE);
        assertEquals(BinaryPV.active, bi.getPhysicalState());

        // false, active, reverse
        bi.writePropertyInternal(PropertyIdentifier.polarity, Polarity.reverse);
        assertEquals(BinaryPV.inactive, bi.getPhysicalState());

        // true, active, reverse
        bi.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.TRUE);
        assertEquals(BinaryPV.active, bi.getPhysicalState());

        // true, inactive, reverse
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
        assertEquals(BinaryPV.inactive, bi.getPhysicalState());

        // false, inactive, reverse
        bi.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.FALSE);
        assertEquals(BinaryPV.active, bi.getPhysicalState());
    }
}
