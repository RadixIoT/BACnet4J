package com.serotonin.bacnet4j.obj;

import static com.serotonin.bacnet4j.type.enumerated.BinaryPV.active;
import static com.serotonin.bacnet4j.type.enumerated.BinaryPV.inactive;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.presentValue;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.priorityArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.type.AmbiguousValue;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PriorityArray;
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
import com.serotonin.bacnet4j.type.eventParameter.CommandFailure;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.CommandFailureNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.RequestUtils;

public class BinaryOutputObjectTest extends AbstractTest {
    static final Logger LOG = LoggerFactory.getLogger(BinaryOutputObjectTest.class);

    private BinaryOutputObject obj;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        obj = new BinaryOutputObject(d1, 0, "boName1", BinaryPV.inactive, false, Polarity.normal, BinaryPV.inactive);
        obj.addListener((pid, oldValue, newValue) -> LOG.debug("{} changed from {} to {}", pid, oldValue, newValue));

        nc = new NotificationClassObject(d1, 17, "nc17", 100, 5, 200, new EventTransitionBits(false, false, false));
    }

    @Test
    public void initialization() throws Exception {
        new BinaryOutputObject(d1, 1, "boName2", BinaryPV.inactive, true, Polarity.normal, BinaryPV.inactive);
    }

    @Test
    public void annexI() throws Exception {
        obj.writePropertyInternal(PropertyIdentifier.minimumOnTime, new UnsignedInteger(1)); // 2 seconds
        obj.writePropertyInternal(PropertyIdentifier.minimumOffTime, new UnsignedInteger(2)); // 4 seconds
        obj.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.FALSE);

        final PriorityArray pa = obj.readProperty(priorityArray);

        // See Annex I for a description of this process.
        // a)
        LOG.debug("a");
        assertEquals(new PriorityArray(), pa);
        assertEquals(inactive, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // b) starts min on for 2s
        LOG.debug("b");
        RequestUtils.writeProperty(d2, rd1, obj.getId(), presentValue, active, 9);
        assertEquals(new PriorityArray().put(6, active).put(9, active), pa);
        assertEquals(active, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // c)
        LOG.debug("c");
        RequestUtils.writeProperty(d2, rd1, obj.getId(), presentValue, inactive, 7);
        assertEquals(new PriorityArray().put(6, active).put(7, inactive).put(9, active), pa);
        assertEquals(active, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // d)
        LOG.debug("d");
        RequestUtils.writeProperty(d2, rd1, obj.getId(), presentValue, Null.instance, 9);
        assertEquals(new PriorityArray().put(6, active).put(7, inactive), pa);
        assertEquals(active, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // e), f) Wait for the timer to expire. Starts min off timer for 4s
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);
        LOG.debug("e,f");
        assertEquals(new PriorityArray().put(6, inactive).put(7, inactive), pa);
        assertEquals(inactive, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // Going off on our own now...
        // Write inactive into 10, and relinquish 7
        LOG.debug("A");
        RequestUtils.writeProperty(d2, rd1, obj.getId(), presentValue, inactive, 10);
        RequestUtils.writeProperty(d2, rd1, obj.getId(), presentValue, Null.instance, 7);
        assertEquals(new PriorityArray().put(6, inactive).put(10, inactive), pa);
        assertEquals(inactive, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // Wait for the timer to expire.
        clock.plus(2100, TimeUnit.MILLISECONDS, 2100, TimeUnit.MILLISECONDS, 0, 40);
        LOG.debug("B");
        assertEquals(new PriorityArray().put(10, inactive), pa);
        assertEquals(inactive, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // Relinquish at 10. No timer should be active, and the array should be empty.
        LOG.debug("C");
        RequestUtils.writeProperty(d2, rd1, obj.getId(), presentValue, Null.instance, 10);
        assertEquals(new PriorityArray(), pa);
        assertEquals(inactive, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // Write active to 9. Starts min on timer for 2s
        LOG.debug("D");
        RequestUtils.writeProperty(d2, rd1, obj.getId(), presentValue, active, 9);
        assertEquals(new PriorityArray().put(6, active).put(9, active), pa);
        assertEquals(active, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // Write inactive to 5. Cancels current timer and starts new off timer for 4s
        LOG.debug("E");
        RequestUtils.writeProperty(d2, rd1, obj.getId(), presentValue, inactive, 5);
        assertEquals(new PriorityArray().put(5, inactive).put(6, inactive).put(9, active), pa);
        assertEquals(inactive, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // Relinquish at 5. Timer remains active.
        clock.plus(1500, TimeUnit.MILLISECONDS, 1500, TimeUnit.MILLISECONDS, 0, 40);
        LOG.debug("F");
        RequestUtils.writeProperty(d2, rd1, obj.getId(), presentValue, Null.instance, 5);
        assertEquals(new PriorityArray().put(6, inactive).put(9, active), pa);
        assertEquals(inactive, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // Wait for the timer to expire. Starts min on timer for 2s
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        LOG.debug("G");
        assertEquals(new PriorityArray().put(6, active).put(9, active), pa);
        assertEquals(active, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));

        // Wait for the timer to expire.
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);
        LOG.debug("H");
        assertEquals(new PriorityArray().put(9, active), pa);
        assertEquals(active, RequestUtils.getProperty(d2, rd1, obj.getId(), presentValue));
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

        obj.supportIntrinsicReporting(5, 17, BinaryPV.inactive, new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        Thread.sleep(40);
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(BinaryPV.inactive, obj.get(PropertyIdentifier.presentValue));
        assertEquals(BinaryPV.inactive, obj.get(PropertyIdentifier.feedbackValue));

        // Do a state change. Write a value to indicate a command failure. After 5s the alarm will be raised.
        obj.writePropertyInternal(PropertyIdentifier.feedbackValue, BinaryPV.active);
        clock.plus(4500, TimeUnit.MILLISECONDS, 4500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, obj.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 80);
        assertEquals(EventState.offnormal, obj.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), obj.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(obj.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) obj.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.offnormal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(100), notif.get("priority"));
        assertEquals(EventType.commandFailure, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.offnormal, notif.get("toState"));
        CommandFailureNotif commandFailure = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(BinaryPV.inactive, AmbiguousValue.convertTo(commandFailure.getCommandValue(), BinaryPV.class));
        assertEquals(new StatusFlags(true, false, false, false), commandFailure.getStatusFlags());
        assertEquals(BinaryPV.active, AmbiguousValue.convertTo(commandFailure.getFeedbackValue(), BinaryPV.class));

        // Return to normal. After 12s the notification will be sent.
        obj.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        clock.plus(11500, TimeUnit.MILLISECONDS, 11500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.offnormal, obj.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, obj.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), obj.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(obj.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) obj.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(200), notif.get("priority"));
        assertEquals(EventType.commandFailure, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.offnormal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        commandFailure = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(BinaryPV.active, AmbiguousValue.convertTo(commandFailure.getCommandValue(), BinaryPV.class));
        assertEquals(new StatusFlags(false, false, false, false), commandFailure.getStatusFlags());
        assertEquals(BinaryPV.active, AmbiguousValue.convertTo(commandFailure.getFeedbackValue(), BinaryPV.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReporting() throws Exception {
        final DeviceObjectPropertyReference ref = new DeviceObjectPropertyReference(1, obj.getId(),
                PropertyIdentifier.presentValue);
        final EventEnrollmentObject ee = new EventEnrollmentObject(d1, 0, "ee", ref, NotifyType.alarm,
                new EventParameter(new CommandFailure(new UnsignedInteger(30),
                        new DeviceObjectPropertyReference(1, obj.getId(), PropertyIdentifier.feedbackValue))),
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
        // Go to high limit.
        obj.writePropertyInternal(PropertyIdentifier.feedbackValue, BinaryPV.active);
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
        assertEquals(EventType.commandFailure, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.offnormal, notif.get("toState"));
        CommandFailureNotif commandFailure = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(BinaryPV.inactive, AmbiguousValue.convertTo(commandFailure.getCommandValue(), BinaryPV.class));
        assertEquals(new StatusFlags(false, false, false, false), commandFailure.getStatusFlags());
        assertEquals(BinaryPV.active, AmbiguousValue.convertTo(commandFailure.getFeedbackValue(), BinaryPV.class));

        //
        // Return to normal
        obj.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
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
        assertEquals(EventType.commandFailure, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.offnormal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        commandFailure = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(BinaryPV.active, AmbiguousValue.convertTo(commandFailure.getCommandValue(), BinaryPV.class));
        assertEquals(new StatusFlags(false, false, false, false), commandFailure.getStatusFlags());
        assertEquals(BinaryPV.active, AmbiguousValue.convertTo(commandFailure.getFeedbackValue(), BinaryPV.class));

    }

    @Test
    public void activeTime() throws Exception {
        try {
            obj.supportActiveTime(true);
            fail("Should have thrown an IllegalStateException");
        } catch (@SuppressWarnings("unused") final IllegalStateException e) {
            // Expected
        }

        final DateTime start = new DateTime(d1);

        obj.writePropertyInternal(PropertyIdentifier.feedbackValue, BinaryPV.inactive);
        obj.supportActiveTime(true);

        clock.plusMillis(3500);
        assertEquals(UnsignedInteger.ZERO, obj.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        obj.writePropertyInternal(PropertyIdentifier.feedbackValue, BinaryPV.active);
        clock.plusMillis(1600); // Total active time 1600ms
        assertEquals(new UnsignedInteger(1), obj.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        obj.writePropertyInternal(PropertyIdentifier.feedbackValue, BinaryPV.active);
        clock.plusMillis(1600); // Total active time 3200ms
        assertEquals(new UnsignedInteger(3), obj.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        obj.writePropertyInternal(PropertyIdentifier.feedbackValue, BinaryPV.inactive);
        clock.plusMillis(7000);
        assertEquals(new UnsignedInteger(3), obj.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        obj.writePropertyInternal(PropertyIdentifier.feedbackValue, BinaryPV.inactive);
        clock.plusMillis(500);
        assertEquals(new UnsignedInteger(3), obj.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        obj.writePropertyInternal(PropertyIdentifier.feedbackValue, BinaryPV.active);
        clock.plusMillis(4700); // Total active time 7900ms
        assertEquals(new UnsignedInteger(7), obj.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        obj.writePropertyInternal(PropertyIdentifier.elapsedActiveTime, new UnsignedInteger(5));
        assertEquals(new UnsignedInteger(5), obj.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfActiveTimeReset));

        clock.plusMillis(1234);
        obj.writePropertyInternal(PropertyIdentifier.elapsedActiveTime, UnsignedInteger.ZERO);
        assertEquals(UnsignedInteger.ZERO, obj.readProperty(PropertyIdentifier.elapsedActiveTime));
        assertEquals(new DateTime(d1), obj.readProperty(PropertyIdentifier.timeOfActiveTimeReset));
    }

    @Test
    public void stateChanges() throws Exception {
        final DateTime start = new DateTime(d1);
        assertEquals(DateTime.UNSPECIFIED, obj.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(UnsignedInteger.ZERO, obj.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(4);
        final DateTime t1 = new DateTime(d1);
        obj.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        assertEquals(t1, obj.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(new UnsignedInteger(1), obj.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(5);
        obj.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        assertEquals(t1, obj.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(new UnsignedInteger(1), obj.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(6);
        final DateTime t2 = new DateTime(d1);
        obj.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
        obj.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        obj.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
        assertEquals(t2, obj.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(new UnsignedInteger(4), obj.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(7);
        obj.writePropertyInternal(PropertyIdentifier.changeOfStateCount, new UnsignedInteger(123));
        assertEquals(t2, obj.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(new UnsignedInteger(123), obj.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(start, obj.readProperty(PropertyIdentifier.timeOfStateCountReset));

        clock.plusMinutes(8);
        final DateTime t3 = new DateTime(d1);
        obj.writePropertyInternal(PropertyIdentifier.changeOfStateCount, UnsignedInteger.ZERO);
        assertEquals(t2, obj.readProperty(PropertyIdentifier.changeOfStateTime));
        assertEquals(UnsignedInteger.ZERO, obj.readProperty(PropertyIdentifier.changeOfStateCount));
        assertEquals(t3, obj.readProperty(PropertyIdentifier.timeOfStateCountReset));
    }

    @Test
    public void physicalState() {
        // Ensure the default state.
        assertEquals(Boolean.FALSE, obj.get(PropertyIdentifier.outOfService));
        assertEquals(BinaryPV.inactive, obj.get(PropertyIdentifier.presentValue));
        assertEquals(Polarity.normal, obj.get(PropertyIdentifier.polarity));

        // false, inactive, normal
        assertEquals(BinaryPV.inactive, obj.getPhysicalState());

        // true, inactive, normal
        obj.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.TRUE);
        assertEquals(BinaryPV.inactive, obj.getPhysicalState());

        // true, active, normal
        obj.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.active);
        assertEquals(BinaryPV.active, obj.getPhysicalState());

        // false, active, normal
        obj.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.FALSE);
        assertEquals(BinaryPV.active, obj.getPhysicalState());

        // false, active, reverse
        obj.writePropertyInternal(PropertyIdentifier.polarity, Polarity.reverse);
        assertEquals(BinaryPV.inactive, obj.getPhysicalState());

        // true, active, reverse
        obj.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.TRUE);
        assertEquals(BinaryPV.active, obj.getPhysicalState());

        // true, inactive, reverse
        obj.writePropertyInternal(PropertyIdentifier.presentValue, BinaryPV.inactive);
        assertEquals(BinaryPV.inactive, obj.getPhysicalState());

        // false, inactive, reverse
        obj.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.FALSE);
        assertEquals(BinaryPV.active, obj.getPhysicalState());
    }
}
