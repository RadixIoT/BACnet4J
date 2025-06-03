package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        Thread.sleep(40);
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(BinaryPV.inactive, bi.get(PropertyIdentifier.presentValue));
        assertEquals(BinaryPV.active, bi.get(PropertyIdentifier.alarmValue));

        // Do a state change. Write a value to indicate a change of state failure. After 5s the alarm will be raised.
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        clock.plus(4500, TimeUnit.MILLISECONDS, 4500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, bi.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 80);
        assertEquals(EventState.offnormal, bi.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), bi.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(bi.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) bi.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.offnormal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(100), notif.get("priority"));
        assertEquals(EventType.changeOfState, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.offnormal, notif.get("toState"));
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(BinaryPV.active),
                new StatusFlags(true, false, false, false))), notif.get("eventValues"));

        // Return to normal. After 12s the notification will be sent.
        bi.writePropertyInternal(PropertyIdentifier.alarmValue, BinaryPV.inactive);
        clock.plus(11500, TimeUnit.MILLISECONDS, 11500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.offnormal, bi.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, bi.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), bi.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(bi.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) bi.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(200), notif.get("priority"));
        assertEquals(EventType.changeOfState, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.offnormal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(BinaryPV.active),
                new StatusFlags(false, false, false, false))), notif.get("eventValues"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReporting() throws Exception {
        final DeviceObjectPropertyReference ref = new DeviceObjectPropertyReference(1, bi.getId(),
                PropertyIdentifier.presentValue);
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
        Thread.sleep(40);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        //
        // Go to alarm value
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
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
        Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(ee.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.offnormal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(100), notif.get("priority"));
        assertEquals(EventType.changeOfState, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.offnormal, notif.get("toState"));
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(BinaryPV.active),
                new StatusFlags(false, false, false, false))), notif.get("eventValues"));

        //
        // Return to normal
        bi.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
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
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(ee.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(200), notif.get("priority"));
        assertEquals(EventType.changeOfState, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.offnormal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(BinaryPV.inactive),
                new StatusFlags(false, false, false, false))), notif.get("eventValues"));
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
