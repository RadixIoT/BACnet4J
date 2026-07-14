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
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.service.confirmed.AcknowledgeAlarmRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.FaultParameter;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultOutOfRange;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultOutOfRange.FaultNormalValue;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultState;
import com.serotonin.bacnet4j.type.constructed.PropertyStates;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfState;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.eventParameter.OutOfRange;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfStateNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class EventEnrollmentObjectTest extends AbstractTest {
    private AnalogValueObject av0;
    private AnalogValueObject av1;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        av0 = d3.addObject(new AnalogValueObject(d3, 0, "av0", 0, EngineeringUnits.noUnits, false));
        av0.writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);

        av1 = d3.addObject(new AnalogValueObject(d3, 1, "av1", 0, EngineeringUnits.noUnits, false));
        av1.writePropertyInternal(PropertyIdentifier.minPresValue, new Real(50));

        nc = d1.addObject(new NotificationClassObject(
                d1, 5, "nc5", 100, 5, 200, new EventTransitionBits(false, false, false)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReportingNoFault() throws Exception {
        // Note: this test monitors the Units property (an EngineeringUnits enumeration) rather
        // than the Reliability property, because addendum 135-2016bu-6 (Clause 12.12.21) requires
        // that a non-noFaultDetected reliability on the monitored object drive the Event
        // Enrollment's own Reliability to MONITORED_OBJECT_FAULT — preempting the ChangeOfState
        // algorithm. Monitoring a non-Reliability enumerated property preserves this test's
        // intent (exercising the ChangeOfState algorithm) under bu-6 semantics.
        DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.analogValue, 0),
                        PropertyIdentifier.units, null, new ObjectIdentifier(ObjectType.device, 3));
        SequenceOf<PropertyStates> alarmValues = new SequenceOf<>( //
                new PropertyStates(EngineeringUnits.hertz), //
                new PropertyStates(EngineeringUnits.kilohertz), //
                new PropertyStates(EngineeringUnits.megahertz));
        EventEnrollmentObject ee = d1.addObject(new EventEnrollmentObject(
                d1, 0, "ee0", ref, NotifyType.event,
                new EventParameter(new ChangeOfState(new UnsignedInteger(1), alarmValues)),
                new EventTransitionBits(true, true, true), 5, 100, null, null));

        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Ensure that initializing the event enrollment object didn't fire any notifications.
        assertEquals(0, listener.getNotifCount());

        // Write a different normal value.
        av0.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.amperes);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        // Ensure that no notifications are sent.
        assertEquals(0, listener.getNotifCount());

        // Set an offnormal value and then set back to normal before the time delay.
        av0.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.hertz);
        clock.plusMillis(500);
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        av0.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.noUnits);
        // Allow the EE to poll
        clock.plusMillis(100);
        quiesce();
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.

        // Do a real state change. Write an offnormal value. After 1s the alarm will be raised.
        av0.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.kilohertz);
        clock.plusMillis(500);
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        // Allow the EE to poll
        clock.plusMillis(100);
        quiesce();
        clock.plusMillis(1100);
        awaitEquals(1, listener::getNotifCount);

        assertEquals(EventState.offnormal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), ee.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(5), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.changeOfState, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.offnormal, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfStateNotif(new PropertyStates(EngineeringUnits.kilohertz),
                        new StatusFlags(false, false, false, false))), notif.eventValues());

        // Set to a different offnormal value. Ensure that no notification is send, because condition (3) in 13.3.2
        // is not supported.
        av0.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.megahertz);
        // Allow the EE to poll
        clock.plusMillis(100);
        quiesce();
        clock.plusMillis(500);
        quiesce();
        assertEquals(0, listener.getNotifCount());
        // Allow the EE to poll
        clock.plusMillis(100);
        quiesce();
        clock.plusMillis(700);
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(5), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.changeOfState, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.offnormal, notif.fromState());
        assertEquals(EventState.offnormal, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfStateNotif(new PropertyStates(EngineeringUnits.megahertz),
                        new StatusFlags(false, false, false, false))), notif.eventValues());

        // Set a normal value and then set back to offnormal before the time delay.
        av0.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.volts);
        // Allow the EE to poll
        clock.plusMillis(100);
        quiesce();
        clock.plusMillis(500);
        quiesce();
        assertEquals(EventState.offnormal,
                ee.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.
        av0.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.hertz);
        // Allow the EE to poll
        clock.plusMillis(100);
        quiesce();
        clock.plusMillis(600);
        quiesce();
        assertEquals(EventState.offnormal,
                ee.readProperty(PropertyIdentifier.eventState)); // Still offnormal at this point.

        // Do a real state change. Write a normal value. After 1s the notification will be sent.
        av0.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.watts);
        // Allow the EE to poll
        clock.plusMillis(100);
        quiesce();
        clock.plusMillis(500);
        quiesce();
        assertEquals(EventState.offnormal,
                ee.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        // Allow the EE to poll
        clock.plusMillis(100);
        quiesce();
        clock.plusMillis(700);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), ee.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(5), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfState, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.offnormal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(EngineeringUnits.watts),
                new StatusFlags(false, false, false, false))), notif.eventValues());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReportingWithFault() throws Exception {
        DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.analogValue, 1),
                        PropertyIdentifier.minPresValue, null, new ObjectIdentifier(ObjectType.device, 3));
        EventEnrollmentObject ee = d1.addObject(new EventEnrollmentObject(
                d1, 0, "ee0", ref, NotifyType.alarm,
                new EventParameter(new OutOfRange(new UnsignedInteger(1), new Real(30), new Real(70), new Real(0))),
                new EventTransitionBits(true, true, true), 5, 100, null, new FaultParameter(
                new FaultOutOfRange(new FaultNormalValue(new Real(10)), new FaultNormalValue(new Real(90))))));

        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Ensure that initializing the event enrollment object didn't fire any notifications.
        assertEquals(0, listener.getNotifCount());

        // Write a different normal value.
        av1.writePropertyInternal(PropertyIdentifier.minPresValue, new Real(45));
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(100);
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        assertEquals(Reliability.noFaultDetected, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(new StatusFlags(false, false, false, false), ee.readProperty(PropertyIdentifier.statusFlags));
        // Ensure that no notifications are sent.
        assertEquals(0, listener.getNotifCount());

        // Write a fault value. Alarm will be sent.
        av1.writePropertyInternal(PropertyIdentifier.minPresValue, new Real(5));
        clock.plusMillis(100);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.fault, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(Reliability.underRange, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(new StatusFlags(true, true, false, false), ee.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(5), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfReliabilityNotif(Reliability.underRange, new StatusFlags(true, true, false, false),
                        new SequenceOf<>(new PropertyValue(PropertyIdentifier.objectPropertyReference, ref),
                                new PropertyValue(PropertyIdentifier.minPresValue, new Real(5)),
                                new PropertyValue(PropertyIdentifier.reliability, Reliability.noFaultDetected),
                                new PropertyValue(PropertyIdentifier.statusFlags,
                                        new StatusFlags(false, false, false, false))))), notif.eventValues());

        // Write a different value. Another notification will be sent.
        av1.writePropertyInternal(PropertyIdentifier.minPresValue, new Real(95));
        clock.plusMillis(100);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.fault, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(Reliability.overRange, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(new StatusFlags(true, true, false, false), ee.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(5), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.fault, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfReliabilityNotif(Reliability.overRange, new StatusFlags(true, true, false, false),
                        new SequenceOf<>(new PropertyValue(PropertyIdentifier.objectPropertyReference, ref),
                                new PropertyValue(PropertyIdentifier.minPresValue, new Real(95)),
                                new PropertyValue(PropertyIdentifier.reliability, Reliability.noFaultDetected),
                                new PropertyValue(PropertyIdentifier.statusFlags,
                                        new StatusFlags(false, false, false, false))))), notif.eventValues());

        // Write a normal value. Another notification will be sent.
        av1.writePropertyInternal(PropertyIdentifier.minPresValue, new Real(55));
        clock.plusMillis(100);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(Reliability.noFaultDetected, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(new StatusFlags(false, false, false, false), ee.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(5), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.fault, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfReliabilityNotif(Reliability.noFaultDetected, new StatusFlags(false, false, false, false),
                        new SequenceOf<>(new PropertyValue(PropertyIdentifier.objectPropertyReference, ref),
                                new PropertyValue(PropertyIdentifier.minPresValue, new Real(55)),
                                new PropertyValue(PropertyIdentifier.reliability, Reliability.noFaultDetected),
                                new PropertyValue(PropertyIdentifier.statusFlags,
                                        new StatusFlags(false, false, false, false))))), notif.eventValues());
    }

    @Test
    public void eventAcknowledgement() throws Exception {
        nc.writePropertyInternal(PropertyIdentifier.ackRequired, new EventTransitionBits(true, true, true));

        DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.analogValue, 1),
                        PropertyIdentifier.minPresValue, null, new ObjectIdentifier(ObjectType.device, 3));
        EventEnrollmentObject ee = d1.addObject(new EventEnrollmentObject(d1, 0, "ee0", ref, NotifyType.alarm,
                new EventParameter(new OutOfRange(new UnsignedInteger(1), new Real(30), new Real(70), new Real(0))),
                new EventTransitionBits(true, true, true), 5, 100, null, new FaultParameter(
                new FaultOutOfRange(new FaultNormalValue(new Real(10)), new FaultNormalValue(new Real(90))))));

        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Write a fault value. Alarm will be sent.
        av1.writePropertyInternal(PropertyIdentifier.minPresValue, new Real(5));
        clock.plusMillis(100);
        awaitEquals(1, listener::getNotifCount);

        // Check that the transition is marked as not acknowledged.
        assertEquals(new EventTransitionBits(true, false, true), ee.get(PropertyIdentifier.ackedTransitions));

        // Ensure that an event notification needing acknowledgement was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(Boolean.TRUE, notif.ackRequired());

        // Acknowledge the event.
        RemoteDevice rd = d2.getRemoteDevice(notif.initiatingDevice().getInstanceNumber()).get();
        var response = d2.send(rd,
                new AcknowledgeAlarmRequest(notif.processIdentifier(), notif.eventObjectIdentifier(), notif.toState(),
                        notif.timeStamp(), new CharacterString("Event acknowledgement test"),
                        new TimeStamp(new DateTime(d2)))).get();
        assertNull(response);

        // Ensure that the event is acknowledged.
        assertEquals(new EventTransitionBits(true, true, true), ee.get(PropertyIdentifier.ackedTransitions));

        // A notification of the acknowledgement should have been sent.
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(NotifyType.ackNotification, notif.notifyType());
    }

    @Test
    public void configureEventEnrollment() throws Exception {
        DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.analogValue, 1),
                        PropertyIdentifier.minPresValue, null, new ObjectIdentifier(ObjectType.device, 3));
        EventEnrollmentObject ee = d1.addObject(new EventEnrollmentObject(
                d1, 0, "ee0", ref, NotifyType.alarm,
                new EventParameter(new OutOfRange(new UnsignedInteger(1), new Real(30), new Real(70), new Real(0))),
                new EventTransitionBits(true, true, true), 5, 100, null, new FaultParameter(
                new FaultOutOfRange(new FaultNormalValue(new Real(10)), new FaultNormalValue(new Real(90))))));

        // Change the event parameters of the enrollment.
        var response = d2.send(rd1, new WritePropertyRequest(ee.getId(), PropertyIdentifier.eventParameters, null,
                new EventParameter(new OutOfRange(new UnsignedInteger(10), new Real(300), new Real(700), new Real(10))),
                null)).get();
        assertNull(response);

        // Verify that the changes have been written.
        EventParameter newParams = ee.get(PropertyIdentifier.eventParameters);
        OutOfRange newOor = newParams.getChoice().getDatum();
        assertEquals(new UnsignedInteger(10), newOor.getTimeDelay());
        assertEquals(new Real(300), newOor.getLowLimit());
        assertEquals(new Real(700), newOor.getHighLimit());
        assertEquals(new Real(10), newOor.getDeadband());
    }

    /**
     * Per addendum 135-2016bu-6 (Clause 12.12.21): once the Event Enrollment object's own
     * reliability is NO_FAULT_DETECTED, if the monitored object's reliability is not
     * NO_FAULT_DETECTED, the Event Enrollment's Reliability shall be MONITORED_OBJECT_FAULT.
     */
    @Test
    public void bu6_monitoredObjectFaultPropagates() throws Exception {
        DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.analogValue, 1),
                        PropertyIdentifier.minPresValue, null, new ObjectIdentifier(ObjectType.device, 3));
        EventEnrollmentObject ee = d1.addObject(new EventEnrollmentObject(
                d1, 0, "ee0", ref, NotifyType.alarm,
                new EventParameter(new OutOfRange(new UnsignedInteger(1), new Real(30), new Real(70), new Real(0))),
                new EventTransitionBits(true, true, true), 5, 100, null, null));

        // Initial state: monitored object reliability is noFaultDetected, EE reliability is
        // noFaultDetected.
        assertEquals(Reliability.noFaultDetected, ee.readProperty(PropertyIdentifier.reliability));

        // Non-noFaultDetected on monitored object → EE reliability shall be MONITORED_OBJECT_FAULT.
        av1.writePropertyInternal(PropertyIdentifier.reliability, Reliability.communicationFailure);
        clock.plusMillis(200);
        quiesce();
        assertEquals(Reliability.monitoredObjectFault, ee.readProperty(PropertyIdentifier.reliability));

        // Monitored object recovers → EE reliability returns to noFaultDetected.
        av1.writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        clock.plusMillis(200);
        quiesce();
        assertEquals(Reliability.noFaultDetected, ee.readProperty(PropertyIdentifier.reliability));
    }

    /**
     * Per 12.12.21 (addendum 135-2020co-2): the Reliability property takes on CONFIGURATION_ERROR
     * while the Low_Limit parameter is greater than the High_Limit parameter.
     */
    @Test
    public void parameterConflictRange() throws Exception {
        DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.analogValue, 1),
                        PropertyIdentifier.minPresValue, null, new ObjectIdentifier(ObjectType.device, 3));
        // Low_Limit 70 is greater than High_Limit 30.
        EventEnrollmentObject ee = d1.addObject(new EventEnrollmentObject(
                d1, 0, "ee0", ref, NotifyType.alarm,
                new EventParameter(new OutOfRange(new UnsignedInteger(1), new Real(70), new Real(30), new Real(0))),
                new EventTransitionBits(true, true, true), 5, 100, null, null));

        assertEquals(Reliability.configurationError, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(EventState.fault, ee.readProperty(PropertyIdentifier.eventState));

        // Writing consistent parameters resolves the conflict.
        ee.writeProperty(null, new PropertyValue(PropertyIdentifier.eventParameters, null,
                new EventParameter(new OutOfRange(new UnsignedInteger(1), new Real(30), new Real(70), new Real(0))),
                null));
        assertEquals(Reliability.noFaultDetected, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
    }

    /**
     * Per 12.12.21 (addendum 135-2020co-2): the Reliability property takes on CONFIGURATION_ERROR
     * while a value is present in both the List_Of_Values parameter in the Event_Parameters property
     * and the List_Of_Fault_Values parameter in the Fault_Parameters property.
     */
    @Test
    public void parameterConflictLists() throws Exception {
        DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.analogValue, 0),
                        PropertyIdentifier.units, null, new ObjectIdentifier(ObjectType.device, 3));
        // hertz is present in both the List_Of_Values and the List_Of_Fault_Values.
        EventEnrollmentObject ee = d1.addObject(new EventEnrollmentObject(
                d1, 0, "ee0", ref, NotifyType.alarm,
                new EventParameter(new ChangeOfState(new UnsignedInteger(1),
                        new SequenceOf<>(new PropertyStates(EngineeringUnits.hertz)))),
                new EventTransitionBits(true, true, true), 5, 100, null,
                new FaultParameter(new FaultState(new SequenceOf<>(new PropertyStates(EngineeringUnits.hertz))))));

        assertEquals(Reliability.configurationError, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(EventState.fault, ee.readProperty(PropertyIdentifier.eventState));

        // Removing the overlap resolves the conflict.
        ee.writeProperty(null, new PropertyValue(PropertyIdentifier.faultParameters, null,
                new FaultParameter(new FaultState(new SequenceOf<>(new PropertyStates(EngineeringUnits.kilohertz)))),
                null));
        assertEquals(Reliability.noFaultDetected, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
    }
}
