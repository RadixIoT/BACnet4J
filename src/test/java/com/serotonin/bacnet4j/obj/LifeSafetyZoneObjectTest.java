package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVRequest;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.FaultParameter;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultLifeSafety;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyMode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.enumerated.SilencedState;
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfLifeSafety;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfLifeSafetyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class LifeSafetyZoneObjectTest extends AbstractTest {
    private LifeSafetyZoneObject lsz;
    private NotificationClassObject nc;
    private EventNotifListener listener;

    @Override
    public void afterInit() throws Exception {
        lsz = new LifeSafetyZoneObject(d1, 0, "lsz", LifeSafetyState.quiet, LifeSafetyMode.on, false,
                new SequenceOf<>(LifeSafetyMode.on, LifeSafetyMode.off, LifeSafetyMode.enabled),
                LifeSafetyOperation.none, SilencedState.unsilenced, new SequenceOf<>());
        nc = new NotificationClassObject(d1, 17, "nc17", 100, 5, 200, new EventTransitionBits(false, false, false));

        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        lsz.supportIntrinsicReporting(5, 17, new BACnetArray<>(LifeSafetyState.tamper, LifeSafetyState.testSupervisory),
                new BACnetArray<>(LifeSafetyState.testActive, LifeSafetyState.testAlarm), null,
                new EventTransitionBits(true, true, true), NotifyType.alarm, new UnsignedInteger(12));

        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        Thread.sleep(40);
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(LifeSafetyState.quiet, lsz.get(PropertyIdentifier.presentValue));
        assertEquals(LifeSafetyMode.on, lsz.get(PropertyIdentifier.mode));

        // Do a state change. Write a value to indicate a change of state failure. After 5s the alarm will be raised.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.tamper);
        clock.plus(4500, TimeUnit.MILLISECONDS, 4500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 80);
        assertEquals(EventState.lifeSafetyAlarm, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(lsz.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.lifeSafetyAlarm.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(100), notif.get("priority"));
        assertEquals(EventType.changeOfLifeSafety, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.lifeSafetyAlarm, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new ChangeOfLifeSafetyNotif(LifeSafetyState.tamper, LifeSafetyMode.on,
                        new StatusFlags(true, false, false, false), LifeSafetyOperation.none)),
                notif.get("eventValues"));

        // Return to normal. After 12s the notification will be sent.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.quiet);
        clock.plus(11500, TimeUnit.MILLISECONDS, 11500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.lifeSafetyAlarm, lsz.readProperty(PropertyIdentifier.eventState)); // Still lifeSafetyAlarm at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(lsz.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(200), notif.get("priority"));
        assertEquals(EventType.changeOfLifeSafety, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.lifeSafetyAlarm, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new ChangeOfLifeSafetyNotif(LifeSafetyState.quiet, LifeSafetyMode.on,
                        new StatusFlags(false, false, false, false), LifeSafetyOperation.none)),
                notif.get("eventValues"));
    }

    /**
     * Tests the timing of when notifications are sent. Normal to offnormal after time delay.
     */
    @Test
    public void changeOfLifeSafetyAlgoA1() throws Exception {
        lsz.supportIntrinsicReporting(5, 17, new BACnetArray<>(LifeSafetyState.tamper, LifeSafetyState.testSupervisory),
                new BACnetArray<>(LifeSafetyState.testActive, LifeSafetyState.testAlarm), null,
                new EventTransitionBits(true, true, true), NotifyType.alarm, new UnsignedInteger(12));

        // Do a state change. Write a value to indicate a change of state failure. After 5s the alarm will be raised.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.testAlarm);
        clock.plus(4999, TimeUnit.MILLISECONDS, 4999, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plus(1, TimeUnit.MILLISECONDS, 1, TimeUnit.MILLISECONDS, 0, 80);
        assertEquals(EventState.offnormal, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(1, listener.getNotifCount());
        listener.clearNotifs();

        // Return to normal. After 12s the notification will be sent.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.quiet);
        clock.plus(11999, TimeUnit.MILLISECONDS, 11999, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.offnormal, lsz.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.
        clock.plus(1, TimeUnit.MILLISECONDS, 1, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(1, listener.getNotifCount());
        listener.clearNotifs();
    }

    /**
     * Tests the timing of when notifications are sent. Normal to offnormal immediately after a mode change.
     *
     * @throws Exception
     */
    @Test
    public void changeOfLifeSafetyAlgoA2() throws Exception {
        lsz.supportIntrinsicReporting(5, 17, new BACnetArray<>(LifeSafetyState.tamper, LifeSafetyState.testSupervisory),
                new BACnetArray<>(LifeSafetyState.testActive, LifeSafetyState.testAlarm), null,
                new EventTransitionBits(true, true, true), NotifyType.alarm, new UnsignedInteger(12));

        // Write a value to indicate offnormal and advance the clock to just before the time delay.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.testAlarm);
        clock.plus(4999, TimeUnit.MILLISECONDS, 4999, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        assertEquals(0, listener.getNotifCount());

        // Change the mode. Notification should be sent immediately.
        lsz.writePropertyInternal(PropertyIdentifier.mode, LifeSafetyMode.fast);
        Thread.sleep(40);
        assertEquals(EventState.offnormal, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(1, listener.getNotifCount());
    }

    /**
     * Tests that a notification is sent when already in lifeSafetyAlarm and the monitored value changes to a different
     * lifeSafetyAlarm value.
     */
    @Test
    public void changeOfLifeSafetyAlgoJ() throws Exception {
        lsz.supportIntrinsicReporting(5, 17, new BACnetArray<>(LifeSafetyState.tamper, LifeSafetyState.testSupervisory),
                new BACnetArray<>(LifeSafetyState.testActive, LifeSafetyState.testAlarm), null,
                new EventTransitionBits(true, true, true), NotifyType.alarm, new UnsignedInteger(12));

        // Write a value to indicate lifeSafetyAlarm and advance the clock to past the the time delay.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.testSupervisory);
        clock.plus(5000, TimeUnit.MILLISECONDS, 5000, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.lifeSafetyAlarm, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(1, listener.getNotifCount());
        listener.clearNotifs();

        // Set the same lifeSafetyAlarm and advance the clock to past the the time delay. No notification should be sent.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.testSupervisory);
        clock.plus(5000, TimeUnit.MILLISECONDS, 5000, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.lifeSafetyAlarm, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        // Change to a different lifeSafetyAlarm and advance the clock to past the the time delay.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.tamper);
        clock.plus(5000, TimeUnit.MILLISECONDS, 5000, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.lifeSafetyAlarm, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(1, listener.getNotifCount());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReportingWithFault() throws Exception {
        lsz.supportIntrinsicReporting(5, 17, //
                new BACnetArray<>(LifeSafetyState.tamper, LifeSafetyState.testSupervisory), //
                new BACnetArray<>(LifeSafetyState.testActive, LifeSafetyState.testAlarm), //
                new BACnetArray<>(LifeSafetyState.fault, LifeSafetyState.faultAlarm),
                new EventTransitionBits(true, true, true), NotifyType.alarm, new UnsignedInteger(12));

        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        Thread.sleep(40);
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(LifeSafetyState.quiet, lsz.get(PropertyIdentifier.presentValue));
        assertEquals(LifeSafetyMode.on, lsz.get(PropertyIdentifier.mode));

        //
        // Write a fault value.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.faultAlarm);
        Thread.sleep(40);
        assertEquals(EventState.fault, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));
        assertEquals(1, listener.getNotifCount());
        Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(lsz.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(5), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        ChangeOfReliabilityNotif cor = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(Reliability.multiStateFault, cor.getReliability());
        assertEquals(new StatusFlags(true, true, false, false), cor.getStatusFlags());
        assertEquals(3, cor.getPropertyValues().size());
        assertEquals(PropertyIdentifier.presentValue, cor.getPropertyValues().get(0).getPropertyIdentifier());
        assertNull(cor.getPropertyValues().get(0).getPropertyArrayIndex());
        assertEquals(LifeSafetyState.faultAlarm,
                LifeSafetyState.forId(((Enumerated) cor.getPropertyValues().get(0).getValue()).intValue()));
        assertNull(cor.getPropertyValues().get(0).getPriority());
        assertEquals(new PropertyValue(PropertyIdentifier.mode, LifeSafetyMode.on), cor.getPropertyValues().get(1));
        assertEquals(new PropertyValue(PropertyIdentifier.operationExpected, LifeSafetyOperation.none),
                cor.getPropertyValues().get(2));

        //
        // Write a different mode.
        lsz.writePropertyInternal(PropertyIdentifier.mode, LifeSafetyMode.fast);
        Thread.sleep(40);
        assertEquals(EventState.fault, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(lsz.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(5), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.fault, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        cor = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(Reliability.multiStateFault, cor.getReliability());
        assertEquals(new StatusFlags(true, true, false, false), cor.getStatusFlags());
        assertEquals(3, cor.getPropertyValues().size());
        assertEquals(PropertyIdentifier.presentValue, cor.getPropertyValues().get(0).getPropertyIdentifier());
        assertNull(cor.getPropertyValues().get(0).getPropertyArrayIndex());
        assertEquals(LifeSafetyState.faultAlarm,
                LifeSafetyState.forId(((Enumerated) cor.getPropertyValues().get(0).getValue()).intValue()));
        assertNull(cor.getPropertyValues().get(0).getPriority());
        assertEquals(new PropertyValue(PropertyIdentifier.mode, LifeSafetyMode.fast), cor.getPropertyValues().get(1));
        assertEquals(new PropertyValue(PropertyIdentifier.operationExpected, LifeSafetyOperation.none),
                cor.getPropertyValues().get(2));

        //
        // Write the same fault state.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.faultAlarm);
        Thread.sleep(40);
        assertEquals(EventState.fault, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));
        assertEquals(0, listener.getNotifCount());

        //
        // Write a different fault state.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.fault);
        Thread.sleep(40);
        assertEquals(EventState.fault, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(lsz.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(5), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.fault, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        cor = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(Reliability.multiStateFault, cor.getReliability());
        assertEquals(new StatusFlags(true, true, false, false), cor.getStatusFlags());
        assertEquals(3, cor.getPropertyValues().size());
        assertEquals(PropertyIdentifier.presentValue, cor.getPropertyValues().get(0).getPropertyIdentifier());
        assertNull(cor.getPropertyValues().get(0).getPropertyArrayIndex());
        assertEquals(LifeSafetyState.fault,
                LifeSafetyState.forId(((Enumerated) cor.getPropertyValues().get(0).getValue()).intValue()));
        assertNull(cor.getPropertyValues().get(0).getPriority());
        assertEquals(new PropertyValue(PropertyIdentifier.mode, LifeSafetyMode.fast), cor.getPropertyValues().get(1));
        assertEquals(new PropertyValue(PropertyIdentifier.operationExpected, LifeSafetyOperation.none),
                cor.getPropertyValues().get(2));

        //
        // Write a non-fault state. This produces two notifications
        // 1) change of reliability from fault to normal
        // 2) change of life safety from normal to normal for the previous change in mode.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.active);
        Thread.sleep(40);
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));
        assertEquals(2, listener.getNotifCount());

        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(lsz.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(200), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.fault, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        cor = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(Reliability.noFaultDetected, cor.getReliability());
        assertEquals(new StatusFlags(false, false, false, false), cor.getStatusFlags());
        assertEquals(3, cor.getPropertyValues().size());
        assertEquals(PropertyIdentifier.presentValue, cor.getPropertyValues().get(0).getPropertyIdentifier());
        assertNull(cor.getPropertyValues().get(0).getPropertyArrayIndex());
        assertEquals(LifeSafetyState.active,
                LifeSafetyState.forId(((Enumerated) cor.getPropertyValues().get(0).getValue()).intValue()));
        assertNull(cor.getPropertyValues().get(0).getPriority());
        assertEquals(new PropertyValue(PropertyIdentifier.mode, LifeSafetyMode.fast), cor.getPropertyValues().get(1));
        assertEquals(new PropertyValue(PropertyIdentifier.operationExpected, LifeSafetyOperation.none),
                cor.getPropertyValues().get(2));

        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(lsz.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(200), notif.get("priority"));
        assertEquals(EventType.changeOfLifeSafety, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new ChangeOfLifeSafetyNotif(LifeSafetyState.active, LifeSafetyMode.fast,
                        new StatusFlags(false, false, false, false), LifeSafetyOperation.none)),
                notif.get("eventValues"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReporting() throws Exception {
        final DeviceObjectPropertyReference pvRef = new DeviceObjectPropertyReference(1, lsz.getId(),
                PropertyIdentifier.presentValue);
        final DeviceObjectPropertyReference modeRef = new DeviceObjectPropertyReference(1, lsz.getId(),
                PropertyIdentifier.mode);
        final EventEnrollmentObject ee = new EventEnrollmentObject(d1, 0, "ee", pvRef, NotifyType.alarm,
                new EventParameter(new ChangeOfLifeSafety(new UnsignedInteger(30),
                        new BACnetArray<>(LifeSafetyState.tamper, LifeSafetyState.testSupervisory), //
                        new BACnetArray<>(LifeSafetyState.testActive, LifeSafetyState.testAlarm), //
                        modeRef)),
                new EventTransitionBits(true, true, true), 17, 1000, new UnsignedInteger(50), //
                new FaultParameter(new FaultLifeSafety(
                        new BACnetArray<>(LifeSafetyState.fault, LifeSafetyState.faultAlarm), modeRef)));

        // Ensure that initializing the event enrollment object didn't fire any notifications.
        Thread.sleep(40);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        //
        // Go to alarm value
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.testAlarm);
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
        assertEquals(EventType.changeOfLifeSafety, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.offnormal, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new ChangeOfLifeSafetyNotif(LifeSafetyState.testAlarm, LifeSafetyMode.on,
                        new StatusFlags(false, false, false, false), LifeSafetyOperation.none)),
                notif.get("eventValues"));

        //
        // Change mode. Notification is sent immediately.
        lsz.writePropertyInternal(PropertyIdentifier.mode, LifeSafetyMode.disarmed);
        // Allow the EE to poll
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(ee.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.offnormal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(100), notif.get("priority"));
        assertEquals(EventType.changeOfLifeSafety, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.offnormal, notif.get("fromState"));
        assertEquals(EventState.offnormal, notif.get("toState"));
        assertEquals(
                new NotificationParameters(
                        new ChangeOfLifeSafetyNotif(LifeSafetyState.testAlarm, LifeSafetyMode.disarmed,
                                new StatusFlags(false, false, false, false), LifeSafetyOperation.none)),
                notif.get("eventValues"));

        //
        // Go to fault value
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.fault);
        // Allow the EE to poll
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(ee.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(5), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.offnormal, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        ChangeOfReliabilityNotif cor = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(Reliability.multiStateFault, cor.getReliability());
        assertEquals(new StatusFlags(true, true, false, false), cor.getStatusFlags());
        assertEquals(4, cor.getPropertyValues().size());
        assertEquals(new PropertyValue(PropertyIdentifier.objectPropertyReference, pvRef),
                cor.getPropertyValues().get(0));
        assertEquals(PropertyIdentifier.presentValue, cor.getPropertyValues().get(1).getPropertyIdentifier());
        assertNull(cor.getPropertyValues().get(1).getPropertyArrayIndex());
        assertEquals(LifeSafetyState.fault.intValue(),
                ((Enumerated) cor.getPropertyValues().get(1).getValue()).intValue());
        assertNull(cor.getPropertyValues().get(1).getPriority());
        assertEquals(new PropertyValue(PropertyIdentifier.reliability, Reliability.noFaultDetected),
                cor.getPropertyValues().get(2));
        assertEquals(new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                cor.getPropertyValues().get(3));

        //
        // Return to normal
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.blocked);
        // Allow the EE to poll
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);
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
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.fault, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        cor = ((NotificationParameters) notif.get("eventValues")).getParameter();
        assertEquals(Reliability.noFaultDetected, cor.getReliability());
        assertEquals(new StatusFlags(false, false, false, false), cor.getStatusFlags());
        assertEquals(4, cor.getPropertyValues().size());
        assertEquals(new PropertyValue(PropertyIdentifier.objectPropertyReference, pvRef),
                cor.getPropertyValues().get(0));
        assertEquals(PropertyIdentifier.presentValue, cor.getPropertyValues().get(1).getPropertyIdentifier());
        assertNull(cor.getPropertyValues().get(1).getPropertyArrayIndex());
        assertEquals(LifeSafetyState.blocked.intValue(),
                ((Enumerated) cor.getPropertyValues().get(1).getValue()).intValue());
        assertNull(cor.getPropertyValues().get(1).getPriority());
        assertEquals(new PropertyValue(PropertyIdentifier.reliability, Reliability.noFaultDetected),
                cor.getPropertyValues().get(2));
        assertEquals(new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                cor.getPropertyValues().get(3));
    }

    @Test
    public void covNotifications() throws Exception {
        lsz.supportCovReporting();

        // Create a COV listener to catch the notifications.
        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        //
        // Subscribe for notifications. Doing so should cause an initial notification to be sent.
        d2.send(rd1,
                new SubscribeCOVRequest(new UnsignedInteger(987), lsz.getId(), Boolean.FALSE, new UnsignedInteger(600)))
                .get();
        Thread.sleep(40);
        assertEquals(1, listener.getNotifCount());
        Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(987), notif.get("subscriberProcessIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(lsz.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(new UnsignedInteger(600), notif.get("timeRemaining"));
        assertEquals(
                new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, LifeSafetyState.quiet),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.get("listOfValues"));

        //
        // Change the present value to send another notification. Advance the clock to test time remaining
        // and the update time.
        clock.plusMinutes(2);

        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.blocked);
        Thread.sleep(40);
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(987), notif.get("subscriberProcessIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(lsz.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(new UnsignedInteger(480), notif.get("timeRemaining"));
        assertEquals(
                new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, LifeSafetyState.blocked),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.get("listOfValues"));
    }

    @Test
    public void validations() throws BACnetServiceException {
        // Write an accepted mode.
        lsz.writeProperty(new ValueSource(TestNetworkUtils.toAddress(10)),
                new PropertyValue(PropertyIdentifier.mode, LifeSafetyMode.enabled));

        // Check that the value source is correct
        assertEquals(new ValueSource(TestNetworkUtils.toAddress(10)), lsz.get(PropertyIdentifier.valueSource));

        // Write a mode that is not accepted
        TestUtils.assertBACnetServiceException(
                () -> lsz.writeProperty(new ValueSource(TestNetworkUtils.toAddress(10)),
                        new PropertyValue(PropertyIdentifier.mode, LifeSafetyMode.manned)),
                ErrorClass.property, ErrorCode.valueOutOfRange);

        // Write a good memberOf list
        lsz.writeProperty(null,
                new PropertyValue(PropertyIdentifier.memberOf,
                        new SequenceOf<>(new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                                new ObjectIdentifier(ObjectType.lifeSafetyZone, 0)))));

        // Write a bad memberOf list
        TestUtils.assertBACnetServiceException(
                () -> lsz.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.memberOf,
                                new SequenceOf<>(new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                                        new ObjectIdentifier(ObjectType.lifeSafetyPoint, 0))))),
                ErrorClass.property, ErrorCode.unsupportedObjectType);

        // Write a good zoneMembers list
        lsz.writeProperty(null,
                new PropertyValue(PropertyIdentifier.zoneMembers,
                        new SequenceOf<>( //
                                new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                                        new ObjectIdentifier(ObjectType.lifeSafetyPoint, 0)), //
                                new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                                        new ObjectIdentifier(ObjectType.lifeSafetyZone, 0)))));

        // Write a bad memberOf list
        TestUtils.assertBACnetServiceException(
                () -> lsz.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.memberOf,
                                new SequenceOf<>( //
                                        new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                                                new ObjectIdentifier(ObjectType.group, 0)), //
                                        new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                                                new ObjectIdentifier(ObjectType.loop, 0))))),
                ErrorClass.property, ErrorCode.unsupportedObjectType);
    }
}
