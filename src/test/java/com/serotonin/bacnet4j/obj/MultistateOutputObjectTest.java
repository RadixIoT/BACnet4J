package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
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
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
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

        try {
            new MultistateOutputObject(d1, 2, "mv2", 0, null, 1, 1, false);
            Assert.fail("Should have thrown an IllegalArgumentException");
        } catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void inconsistentStateText() throws Exception {
        try {
            new MultistateOutputObject(d1, 1, "mv1", 7, new BACnetArray<>(new CharacterString("a")), 1, 1, true);
            Assert.fail("Should have thrown an IllegalArgumentException");
        } catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void missingStateText() throws Exception {
        final MultistateOutputObject mv = new MultistateOutputObject(d1, 1, "mv1", 7, null, 1, 1, true);

        try {
            mv.writeProperty(null,
                    new PropertyValue(PropertyIdentifier.stateText, new BACnetArray<>(new CharacterString("a"))));
            fail("Should have thrown an exception");
        } catch (final BACnetServiceException e) {
            assertEquals(ErrorClass.property, e.getErrorClass());
            assertEquals(ErrorCode.inconsistentConfiguration, e.getErrorCode());
        }
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
        Thread.sleep(40);
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(new UnsignedInteger(2), mo.get(PropertyIdentifier.presentValue));
        assertEquals(new UnsignedInteger(2), mo.get(PropertyIdentifier.feedbackValue));

        // Do a state change. Write a value to indicate a command failure. After 5s the alarm will be raised.
        mo.writePropertyInternal(PropertyIdentifier.feedbackValue, new UnsignedInteger(1));
        clock.plus(4500, TimeUnit.MILLISECONDS, 4500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, mo.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 80);
        assertEquals(EventState.offnormal, mo.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), mo.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
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
        clock.plus(11500, TimeUnit.MILLISECONDS, 11500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.offnormal,
                mo.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, mo.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), mo.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
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
        Thread.sleep(40);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        //
        // Go to off normal.
        mo.writePropertyInternal(PropertyIdentifier.feedbackValue, new UnsignedInteger(1));
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
