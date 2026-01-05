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
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.type.AmbiguousValue;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.eventParameter.CommandFailure;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.CommandFailureNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class MultistateOutputObjectTest extends AbstractTest {
    private MultistateOutputObject mo;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        mo = new MultistateOutputObject(d1, 0, "mo", 5, new BACnetArray<>(new CharacterString("Off"), //
                new CharacterString("On"), //
                new CharacterString("Auto"), //
                new CharacterString("Fan"), //
                new CharacterString("Other")), 2, 5, false);
        nc = new NotificationClassObject(d1, 17, "nc17", 100, 5, 200, new EventTransitionBits(false, false, false));
    }

    @Test
    public void initialization() throws Exception {
        new MultistateOutputObject(d1, 1, "mo1", 7, null, 1, 1, false);
        assertThrows(IllegalArgumentException.class,
                () -> new MultistateOutputObject(d1, 2, "mv2", 0, null, 1, 1, false));
    }

    @Test
    public void inconsistentStateText() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultistateOutputObject(d1, 1, "mv1", 7, new BACnetArray<>(new CharacterString("a")), 1, 1,
                        true));
    }

    @Test
    public void missingStateText() throws Exception {
        final MultistateOutputObject mv = new MultistateOutputObject(d1, 1, "mv1", 7, null, 1, 1, true);

        mv.writeProperty(null,
                new PropertyValue(PropertyIdentifier.stateText, new BACnetArray<>(new CharacterString("a"))));
        assertEquals(new UnsignedInteger(1), mv.get(PropertyIdentifier.numberOfStates));
    }

    @Test
    public void stateText() throws Exception {
        final MultistateOutputObject mv = new MultistateOutputObject(d1, 1, "mv1", 7, null, 1, 1, true);

        mv.writeProperty(null, new PropertyValue(PropertyIdentifier.stateText,
                new BACnetArray<>(new CharacterString("a"), new CharacterString("b"), new CharacterString("c"),
                        new CharacterString("d"), new CharacterString("e"), new CharacterString("f"),
                        new CharacterString("g"))));

        mv.writeProperty(null, new PropertyValue(PropertyIdentifier.numberOfStates, new UnsignedInteger(6)));
        assertEquals(new BACnetArray<>(new CharacterString("a"), new CharacterString("b"), new CharacterString("c"),
                        new CharacterString("d"), new CharacterString("e"), new CharacterString("f")),
                mv.get(PropertyIdentifier.stateText));

        mv.writeProperty(null, new PropertyValue(PropertyIdentifier.numberOfStates, new UnsignedInteger(8)));
        assertEquals(new BACnetArray<>(new CharacterString("a"), new CharacterString("b"), new CharacterString("c"),
                new CharacterString("d"), new CharacterString("e"), new CharacterString("f"), CharacterString.EMPTY,
                CharacterString.EMPTY), mv.get(PropertyIdentifier.stateText));
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

        mo.supportIntrinsicReporting(5, 17, 2, new EventTransitionBits(true, true, true), NotifyType.alarm, 12);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        quiesce();
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(new UnsignedInteger(2), mo.get(PropertyIdentifier.presentValue));
        assertEquals(new UnsignedInteger(2), mo.get(PropertyIdentifier.feedbackValue));

        // Do a state change. Write a value to indicate a command failure. After 5s the alarm will be raised.
        mo.writePropertyInternal(PropertyIdentifier.feedbackValue, new UnsignedInteger(1));
        clock.plusMillis(4500);
        quiesce();
        assertEquals(EventState.normal, mo.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.offnormal, mo.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), mo.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(mo.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) mo.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.commandFailure, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.offnormal, notif.toState());
        CommandFailureNotif commandFailure = ((NotificationParameters) notif.eventValues()).getParameter();
        assertEquals(new UnsignedInteger(2),
                AmbiguousValue.convertTo(commandFailure.getCommandValue(), UnsignedInteger.class));
        assertEquals(new StatusFlags(true, false, false, false), commandFailure.getStatusFlags());
        assertEquals(new UnsignedInteger(1),
                AmbiguousValue.convertTo(commandFailure.getFeedbackValue(), UnsignedInteger.class));

        // Return to normal. After 12s the notification will be sent.
        mo.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(1));
        clock.plusMillis(11500);
        quiesce();
        assertEquals(EventState.offnormal,
                mo.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, mo.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), mo.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(mo.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) mo.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.commandFailure, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.offnormal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        commandFailure = ((NotificationParameters) notif.eventValues()).getParameter();
        assertEquals(new UnsignedInteger(1),
                AmbiguousValue.convertTo(commandFailure.getCommandValue(), UnsignedInteger.class));
        assertEquals(new StatusFlags(false, false, false, false), commandFailure.getStatusFlags());
        assertEquals(new UnsignedInteger(1),
                AmbiguousValue.convertTo(commandFailure.getFeedbackValue(), UnsignedInteger.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReporting() throws Exception {
        // Set the feedback value to match the prsent value
        mo.writePropertyInternal(PropertyIdentifier.feedbackValue, new UnsignedInteger(2));

        // Check the starting values.
        assertEquals(new UnsignedInteger(2), mo.get(PropertyIdentifier.presentValue));
        assertEquals(new UnsignedInteger(2), mo.get(PropertyIdentifier.feedbackValue));

        final DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(1, mo.getId(), PropertyIdentifier.presentValue);
        final EventEnrollmentObject ee = new EventEnrollmentObject(d1, 0, "ee", ref, NotifyType.alarm,
                new EventParameter(new CommandFailure(new UnsignedInteger(30),
                        new DeviceObjectPropertyReference(1, mo.getId(), PropertyIdentifier.feedbackValue))),
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
        // Go to off normal.
        mo.writePropertyInternal(PropertyIdentifier.feedbackValue, new UnsignedInteger(1));
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
        assertEquals(EventType.commandFailure, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.offnormal, notif.toState());
        CommandFailureNotif commandFailure = ((NotificationParameters) notif.eventValues()).getParameter();
        assertEquals(new UnsignedInteger(2),
                AmbiguousValue.convertTo(commandFailure.getCommandValue(), UnsignedInteger.class));
        assertEquals(new StatusFlags(false, false, false, false), commandFailure.getStatusFlags());
        assertEquals(new UnsignedInteger(1),
                AmbiguousValue.convertTo(commandFailure.getFeedbackValue(), UnsignedInteger.class));

        //
        // Return to normal
        mo.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(1));
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
        assertEquals(EventType.commandFailure, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.offnormal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        commandFailure = ((NotificationParameters) notif.eventValues()).getParameter();
        assertEquals(new UnsignedInteger(1),
                AmbiguousValue.convertTo(commandFailure.getCommandValue(), UnsignedInteger.class));
        assertEquals(new StatusFlags(false, false, false, false), commandFailure.getStatusFlags());
        assertEquals(new UnsignedInteger(1),
                AmbiguousValue.convertTo(commandFailure.getFeedbackValue(), UnsignedInteger.class));
    }
}
