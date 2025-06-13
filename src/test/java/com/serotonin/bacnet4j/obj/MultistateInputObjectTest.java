package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PropertyStates;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfState;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfStateNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class MultistateInputObjectTest extends AbstractTest {
    private MultistateInputObject mi;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        mi = new MultistateInputObject(d1, 0, "mi", 5, new BACnetArray<>(new CharacterString("Off"), //
                new CharacterString("On"), //
                new CharacterString("Auto"), //
                new CharacterString("Fan"), //
                new CharacterString("Other")), 1, false);
        nc = new NotificationClassObject(d1, 17, "nc17", 100, 5, 200, new EventTransitionBits(false, false, false));
    }

    @Test
    public void initialization() throws Exception {
        new MultistateInputObject(d1, 1, "mi1", 7, null, 1, false);

        try {
            new MultistateInputObject(d1, 2, "mi2", 0, null, 1, false);
            Assert.fail("Should have thrown an IllegalArgumentException");
        } catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
            // Expected
        }
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

        mi.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(4), new UnsignedInteger(5)), null,
                new EventTransitionBits(true, true, true), NotifyType.alarm, new UnsignedInteger(12));
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        Thread.sleep(40);
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(new UnsignedInteger(1), mi.get(PropertyIdentifier.presentValue));

        // Do a state change. Write a value to indicate a command failure. After 5s the alarm will be raised.
        mi.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(4));
        clock.plus(4500, TimeUnit.MILLISECONDS, 4500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, mi.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 80);
        assertEquals(EventState.offnormal, mi.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), mi.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(mi.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) mi.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.changeOfState, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.offnormal, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(new UnsignedInteger(4)),
                new StatusFlags(true, false, false, false))), notif.eventValues());

        // Return to normal. After 12s the notification will be sent.
        mi.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(3));
        clock.plus(11500, TimeUnit.MILLISECONDS, 11500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.offnormal,
                mi.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, mi.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), mi.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(mi.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) mi.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfState, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.offnormal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(new UnsignedInteger(3)),
                new StatusFlags(false, false, false, false))), notif.eventValues());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReporting() throws Exception {
        // Check the starting values.
        assertEquals(new UnsignedInteger(1), mi.get(PropertyIdentifier.presentValue));

        final DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(1, mi.getId(), PropertyIdentifier.presentValue);
        final EventEnrollmentObject ee = new EventEnrollmentObject(d1, 0, "ee", ref, NotifyType.alarm,
                new EventParameter(new ChangeOfState(new UnsignedInteger(30),
                        new SequenceOf<>(new PropertyStates(new UnsignedInteger(4))))),
                new EventTransitionBits(true, true, true), 17, 1000, null, null);

        // Set up the notification destination
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Ensure that initializing the event enrollment object didn't fire any notifications.
        Thread.sleep(40);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        //
        // Go to off normal.
        mi.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(4));
        // Allow the EE to poll
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until just before the time delay.
        clock.plus(29500, TimeUnit.MILLISECONDS, 29500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until after the time delay.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.offnormal, ee.readProperty(PropertyIdentifier.eventState));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
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
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(new UnsignedInteger(4)),
                new StatusFlags(false, false, false, false))), notif.eventValues());

        //
        // Return to normal
        mi.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(5));
        // Allow the EE to poll
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.offnormal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until just before the time delay.
        clock.plus(29500, TimeUnit.MILLISECONDS, 29500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.offnormal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until after the time delay.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
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
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(new UnsignedInteger(5)),
                new StatusFlags(false, false, false, false))), notif.eventValues());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fault() throws Exception {
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        mi.supportIntrinsicReporting(5, 17, new BACnetArray<>(),
                new BACnetArray<>(new UnsignedInteger(4), new UnsignedInteger(5)),
                new EventTransitionBits(true, true, true), NotifyType.alarm, null);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        Thread.sleep(40);
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(new UnsignedInteger(1), mi.get(PropertyIdentifier.presentValue));

        // Do a state change. Write to a fault state.
        mi.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(4));
        Thread.sleep(40);
        assertEquals(EventState.fault, mi.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), mi.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(mi.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) mi.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        assertEquals(new NotificationParameters(
                        new ChangeOfReliabilityNotif(Reliability.multiStateFault, new StatusFlags(true, true, false, false),
                                new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new UnsignedInteger(4))))),
                notif.eventValues());

        // Return to normal. After 12s the notification will be sent.
        mi.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(2));
        Thread.sleep(40);
        assertEquals(EventState.normal, mi.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), mi.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(mi.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) mi.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.fault, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                        new ChangeOfReliabilityNotif(Reliability.noFaultDetected, new StatusFlags(false, false, false, false),
                                new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new UnsignedInteger(2))))),
                notif.eventValues());
    }
}
