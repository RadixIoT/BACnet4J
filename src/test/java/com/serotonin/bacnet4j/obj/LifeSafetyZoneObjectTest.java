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

import static com.serotonin.bacnet4j.TestUtils.assertBACnetServiceException;
import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
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
        quiesce();
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(LifeSafetyState.quiet, lsz.get(PropertyIdentifier.presentValue));
        assertEquals(LifeSafetyMode.on, lsz.get(PropertyIdentifier.mode));

        // Do a state change. Write a value to indicate a change of state failure. After 5s the alarm will be raised.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.tamper);
        clock.plusMillis(4500);
        quiesce();
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(600);
        awaitEquals(EventState.lifeSafetyAlarm, () -> lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        awaitEquals(1, listener::getNotifCount);
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(lsz.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.lifeSafetyAlarm.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.changeOfLifeSafety, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.lifeSafetyAlarm, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfLifeSafetyNotif(LifeSafetyState.tamper, LifeSafetyMode.on,
                new StatusFlags(true, false, false, false), LifeSafetyOperation.none)), notif.eventValues());

        // Return to normal. After 12s the notification will be sent.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.quiet);
        clock.plusMillis(11500);
        quiesce();
        assertEquals(EventState.lifeSafetyAlarm,
                lsz.readProperty(PropertyIdentifier.eventState)); // Still lifeSafetyAlarm at this point.
        clock.plusMillis(600);
        awaitEquals(EventState.normal, () -> lsz.readProperty(PropertyIdentifier.eventState));
        awaitEquals(new StatusFlags(false, false, false, false),
                () -> lsz.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(lsz.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfLifeSafety, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.lifeSafetyAlarm, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfLifeSafetyNotif(LifeSafetyState.quiet, LifeSafetyMode.on,
                new StatusFlags(false, false, false, false), LifeSafetyOperation.none)), notif.eventValues());
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
        clock.plusMillis(4999);
        quiesce();
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(1);
        awaitEquals(EventState.offnormal, () -> lsz.readProperty(PropertyIdentifier.eventState));
        awaitEquals(1, listener::getNotifCount);
        listener.clearNotifs();

        // Return to normal. After 12s the notification will be sent.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.quiet);
        clock.plusMillis(11999);
        quiesce();
        assertEquals(EventState.offnormal,
                lsz.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.
        clock.plusMillis(1);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState));
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
        clock.plusMillis(4999);
        quiesce();
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        assertEquals(0, listener.getNotifCount());

        // Change the mode. Notification should be sent immediately.
        lsz.writePropertyInternal(PropertyIdentifier.mode, LifeSafetyMode.fast);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.offnormal, lsz.readProperty(PropertyIdentifier.eventState));
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
        clock.plusMillis(5000);
        awaitEquals(EventState.lifeSafetyAlarm, () -> lsz.readProperty(PropertyIdentifier.eventState));
        awaitEquals(1, listener::getNotifCount);
        listener.clearNotifs();

        // Set the same lifeSafetyAlarm and advance the clock to past the the time delay. No notification should be sent.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.testSupervisory);
        clock.plusMillis(5000);
        quiesce();
        assertEquals(EventState.lifeSafetyAlarm, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        // Change to a different lifeSafetyAlarm and advance the clock to past the the time delay.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.tamper);
        clock.plusMillis(5000);
        awaitEquals(EventState.lifeSafetyAlarm, () -> lsz.readProperty(PropertyIdentifier.eventState));
        awaitEquals(1, listener::getNotifCount);
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
        quiesce();
        assertEquals(0, listener.getNotifCount());

        // Check the starting values.
        assertEquals(LifeSafetyState.quiet, lsz.get(PropertyIdentifier.presentValue));
        assertEquals(LifeSafetyMode.on, lsz.get(PropertyIdentifier.mode));

        //
        // Write a fault value.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.faultAlarm);

        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.fault, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(lsz.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        ChangeOfReliabilityNotif cor = ((NotificationParameters) notif.eventValues()).getParameter();
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
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.fault, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(lsz.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.fault, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        cor = ((NotificationParameters) notif.eventValues()).getParameter();
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
        quiesce();
        assertEquals(EventState.fault, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));
        assertEquals(0, listener.getNotifCount());

        //
        // Write a different fault state.
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.fault);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.fault, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(lsz.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.fault, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        cor = ((NotificationParameters) notif.eventValues()).getParameter();
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
        awaitEquals(2, listener::getNotifCount);
        assertEquals(EventState.normal, lsz.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), lsz.readProperty(PropertyIdentifier.statusFlags));

        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(lsz.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.fault, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        cor = ((NotificationParameters) notif.eventValues()).getParameter();
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
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(lsz.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) lsz.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfLifeSafety, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfLifeSafetyNotif(LifeSafetyState.active, LifeSafetyMode.fast,
                new StatusFlags(false, false, false, false), LifeSafetyOperation.none)), notif.eventValues());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReporting() throws Exception {
        final DeviceObjectPropertyReference pvRef =
                new DeviceObjectPropertyReference(1, lsz.getId(), PropertyIdentifier.presentValue);
        final DeviceObjectPropertyReference modeRef =
                new DeviceObjectPropertyReference(1, lsz.getId(), PropertyIdentifier.mode);
        final EventEnrollmentObject ee = new EventEnrollmentObject(d1, 0, "ee", pvRef, NotifyType.alarm,
                new EventParameter(new ChangeOfLifeSafety(new UnsignedInteger(30),
                        new BACnetArray<>(LifeSafetyState.tamper, LifeSafetyState.testSupervisory), //
                        new BACnetArray<>(LifeSafetyState.testActive, LifeSafetyState.testAlarm), //
                        modeRef)), new EventTransitionBits(true, true, true), 17, 1000, new UnsignedInteger(50), //
                new FaultParameter(
                        new FaultLifeSafety(new BACnetArray<>(LifeSafetyState.fault, LifeSafetyState.faultAlarm),
                                modeRef)));

        // Ensure that initializing the event enrollment object didn't fire any notifications.
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        //
        // Go to alarm value
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.testAlarm);
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
        awaitEquals(EventState.offnormal, () -> ee.readProperty(PropertyIdentifier.eventState));

        // Ensure that a proper looking event notification was received.
        awaitEquals(1, listener::getNotifCount);
        EventNotifListener.Notif notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.changeOfLifeSafety, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.offnormal, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfLifeSafetyNotif(LifeSafetyState.testAlarm, LifeSafetyMode.on,
                        new StatusFlags(false, false, false, false), LifeSafetyOperation.none)), notif.eventValues());

        //
        // Change mode. Notification is sent immediately.
        lsz.writePropertyInternal(PropertyIdentifier.mode, LifeSafetyMode.disarmed);
        // Allow the EE to poll
        clock.plusMillis(1100);

        // Ensure that a proper looking event notification was received.
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.changeOfLifeSafety, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.offnormal, notif.fromState());
        assertEquals(EventState.offnormal, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfLifeSafetyNotif(LifeSafetyState.testAlarm, LifeSafetyMode.disarmed,
                        new StatusFlags(false, false, false, false), LifeSafetyOperation.none)), notif.eventValues());

        //
        // Go to fault value
        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.fault);
        // Allow the EE to poll
        clock.plusMillis(1100);

        // Ensure that a proper looking event notification was received.
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.offnormal, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        ChangeOfReliabilityNotif cor = ((NotificationParameters) notif.eventValues()).getParameter();
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
        clock.plusMillis(1100);
        awaitEquals(EventState.normal, () -> ee.readProperty(PropertyIdentifier.eventState));

        // Ensure that a proper looking event notification was received.
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.fault, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        cor = ((NotificationParameters) notif.eventValues()).getParameter();
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
        awaitEquals(1, listener::getNotifCount);
        CovNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(987), notif.subscriberProcessIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(lsz.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new UnsignedInteger(600), notif.timeRemaining());
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, LifeSafetyState.quiet),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        //
        // Change the present value to send another notification. Advance the clock to test time remaining
        // and the update time.
        clock.plusMinutes(2);

        lsz.writePropertyInternal(PropertyIdentifier.presentValue, LifeSafetyState.blocked);
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(987), notif.subscriberProcessIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(lsz.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new UnsignedInteger(480), notif.timeRemaining());
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, LifeSafetyState.blocked),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());
    }

    @Test
    public void validations() throws BACnetServiceException {
        // Write an accepted mode.
        lsz.writeProperty(new ValueSource(TestNetworkUtils.toAddress(10)),
                new PropertyValue(PropertyIdentifier.mode, LifeSafetyMode.enabled));

        // Check that the value source is correct
        assertEquals(new ValueSource(TestNetworkUtils.toAddress(10)), lsz.get(PropertyIdentifier.valueSource));

        // Write a mode that is not accepted
        assertBACnetServiceException(() -> lsz.writeProperty(new ValueSource(TestNetworkUtils.toAddress(10)),
                        new PropertyValue(PropertyIdentifier.mode, LifeSafetyMode.manned)), ErrorClass.property,
                ErrorCode.valueOutOfRange);

        // Write a good memberOf list
        lsz.writeProperty(null, new PropertyValue(PropertyIdentifier.memberOf, new SequenceOf<>(
                new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                        new ObjectIdentifier(ObjectType.lifeSafetyZone, 0)))));

        // Write a bad memberOf list
        assertBACnetServiceException(() -> lsz.writeProperty(null, new PropertyValue(PropertyIdentifier.memberOf,
                        new SequenceOf<>(new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                                new ObjectIdentifier(ObjectType.lifeSafetyPoint, 0))))), ErrorClass.property,
                ErrorCode.unsupportedObjectType);

        // Write a good zoneMembers list
        lsz.writeProperty(null, new PropertyValue(PropertyIdentifier.zoneMembers, new SequenceOf<>( //
                new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                        new ObjectIdentifier(ObjectType.lifeSafetyPoint, 0)), //
                new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                        new ObjectIdentifier(ObjectType.lifeSafetyZone, 0)))));

        // Write a bad memberOf list
        assertBACnetServiceException(
                () -> lsz.writeProperty(null, new PropertyValue(PropertyIdentifier.memberOf, new SequenceOf<>( //
                        new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                                new ObjectIdentifier(ObjectType.group, 0)), //
                        new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 10),
                                new ObjectIdentifier(ObjectType.loop, 0))))), ErrorClass.property,
                ErrorCode.unsupportedObjectType);
    }
}
