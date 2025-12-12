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

import static com.serotonin.bacnet4j.TestUtils.advanceClock;
import static com.serotonin.bacnet4j.TestUtils.assertBACnetServiceException;
import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventLogRecord;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LogStatus;
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
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.eventParameter.BufferReady;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.notificationParameters.OutOfRangeNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class EventLogObjectTest extends AbstractTest {
    private NotificationClassObject nc;

    private final DateTime now = new DateTime(clock.millis());
    private final ConfirmedEventNotificationRequest n1 =
            new ConfirmedEventNotificationRequest(new UnsignedInteger(123), new ObjectIdentifier(ObjectType.device, 50),
                    new ObjectIdentifier(ObjectType.device, 50), new TimeStamp(now), new UnsignedInteger(456),
                    new UnsignedInteger(1), EventType.accessEvent, new CharacterString("message"), NotifyType.event,
                    Boolean.FALSE, EventState.fault, EventState.highLimit, new NotificationParameters(
                    new BufferReadyNotif(
                            new DeviceObjectPropertyReference(51, new ObjectIdentifier(ObjectType.trendLog, 0),
                                    PropertyIdentifier.logBuffer), new UnsignedInteger(1000),
                            new UnsignedInteger(2000))));
    private final ConfirmedEventNotificationRequest n2 =
            new ConfirmedEventNotificationRequest(new UnsignedInteger(124), new ObjectIdentifier(ObjectType.device, 60),
                    new ObjectIdentifier(ObjectType.device, 60), new TimeStamp(now), new UnsignedInteger(789),
                    new UnsignedInteger(109), EventType.commandFailure, new CharacterString("message2"),
                    NotifyType.alarm, Boolean.TRUE, EventState.offnormal, EventState.normal, new NotificationParameters(
                    new OutOfRangeNotif(new Real(34), new StatusFlags(true, true, true, true), new Real(35),
                            new Real(36))));

    @Override
    public void afterInit() throws Exception {
        nc = new NotificationClassObject(d1, 23, "nc", 1, 2, 3, new EventTransitionBits(true, true, true));
    }

    @Test
    public void logging() throws Exception {
        final EventLogObject el =
                new EventLogObject(d1, 0, "el", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, false, 20);

        // The buffer should still be empty
        assertEquals(0, el.getBuffer().size());

        // Send an event
        d2.send(rd1, n1).get();

        // The log record should be there.
        assertEquals(1, el.getBuffer().size());
        final EventLogRecord record0 = el.getBuffer().get(0);
        assertEquals(now, record0.getTimestamp());
        assertEquals(1, record0.getSequenceNumber());
        assertEquals(n1, record0.getNotification());

        // Send a couple more
        d2.send(rd1, n2).get();
        d2.send(rd1, n1).get();

        // The log record should be there.
        assertEquals(3, el.getBuffer().size());
        final EventLogRecord record1 = el.getBuffer().get(1);
        assertEquals(now, record1.getTimestamp());
        assertEquals(2, record1.getSequenceNumber());
        assertEquals(n2, record1.getNotification());

        final EventLogRecord record2 = el.getBuffer().get(2);
        assertEquals(now, record2.getTimestamp());
        assertEquals(3, record2.getSequenceNumber());
        assertEquals(n1, record2.getNotification());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        // Create a triggered trend log with intrinsic reporting enabled.
        final EventLogObject el =
                new EventLogObject(d1, 0, "el", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, false, 20) //
                        .supportIntrinsicReporting(5, 23, new EventTransitionBits(true, true, true), NotifyType.event);

        // Add d2 as an event recipient.
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(27), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        //
        // Send 4 events and make sure no notification was sent.
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        assertEquals(4, el.getBuffer().size());
        assertEquals(0, listener.getNotifCount());
        assertEquals(new UnsignedInteger(4), el.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), el.get(PropertyIdentifier.totalRecordCount));
        assertEquals(new UnsignedInteger(4), el.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(UnsignedInteger.ZERO, el.get(PropertyIdentifier.lastNotifyRecord));

        //
        // Write one more and make sure a notification was received.
        d2.send(rd1, n1).get();
        assertEquals(5, el.getBuffer().size());
        awaitEquals(1, listener::getNotifCount);
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(27), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(el.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) el.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(23), notif.notificationClass());
        assertEquals(new UnsignedInteger(3), notif.priority());
        assertEquals(EventType.bufferReady, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.TRUE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, el.getId(), PropertyIdentifier.logBuffer),
                        UnsignedInteger.ZERO, new UnsignedInteger(5))), notif.eventValues());

        // Validate the internally maintained values.
        assertEquals(new UnsignedInteger(5), el.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(5), el.get(PropertyIdentifier.totalRecordCount));
        assertEquals(UnsignedInteger.ZERO, el.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(new UnsignedInteger(5), el.get(PropertyIdentifier.lastNotifyRecord));

        //
        // Write another 5 triggers and ensure that the notification looks ok.
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        assertEquals(10, el.getBuffer().size());
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(27), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(el.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) el.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(23), notif.notificationClass());
        assertEquals(new UnsignedInteger(3), notif.priority());
        assertEquals(EventType.bufferReady, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.TRUE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, el.getId(), PropertyIdentifier.logBuffer),
                        new UnsignedInteger(5), new UnsignedInteger(10))), notif.eventValues());

        // Validate the internally maintained values.
        assertEquals(new UnsignedInteger(10), el.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(10), el.get(PropertyIdentifier.totalRecordCount));
        assertEquals(UnsignedInteger.ZERO, el.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(new UnsignedInteger(10), el.get(PropertyIdentifier.lastNotifyRecord));

        //
        // Update the values of the trend log such that we can trigger condition 2 in the buffer ready algo.
        el.set(PropertyIdentifier.lastNotifyRecord, new UnsignedInteger(0xFFFFFFFDL));
        el.set(PropertyIdentifier.totalRecordCount, new UnsignedInteger(0xFFFFFFFDL));
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        assertEquals(15, el.getBuffer().size());
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(27), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(el.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) el.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(23), notif.notificationClass());
        assertEquals(new UnsignedInteger(3), notif.priority());
        assertEquals(EventType.bufferReady, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.TRUE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, el.getId(), PropertyIdentifier.logBuffer),
                        new UnsignedInteger(0xFFFFFFFDL), new UnsignedInteger(3))), notif.eventValues());

        // Validate the internally maintained values.
        assertEquals(new UnsignedInteger(15), el.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(3), el.get(PropertyIdentifier.totalRecordCount));
        assertEquals(UnsignedInteger.ZERO, el.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(new UnsignedInteger(3), el.get(PropertyIdentifier.lastNotifyRecord));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void eventReporting() throws Exception {
        // Create a triggered trend log
        final EventLogObject el =
                new EventLogObject(d1, 0, "el", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, false, 20);

        // Create the event enrollment.
        final DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(el.getId(), PropertyIdentifier.totalRecordCount, null, d1.getId());
        final EventEnrollmentObject ee = new EventEnrollmentObject(d1, 0, "ee", ref, NotifyType.event,
                new EventParameter(new BufferReady(new UnsignedInteger(3), UnsignedInteger.ZERO)),
                new EventTransitionBits(true, true, true), 23, 1000, null, null);

        // Set d2 as an event recipient.
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(d2.getId()), new UnsignedInteger(28), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Trigger updates, but not enough to cause a notification.
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();

        // Give the EE a chance to poll.
        clock.plusSeconds(1);
        quiesce();

        // Ensure that there are no notifications.
        assertEquals(0, listener.getNotifCount());

        // Trigger another notification so that a notification is sent.
        d2.send(rd1, n1).get();
        clock.plusSeconds(1);
        awaitEquals(1, listener::getNotifCount);
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(28), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(23), notif.notificationClass());
        assertEquals(new UnsignedInteger(3), notif.priority());
        assertEquals(EventType.bufferReady, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.TRUE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, el.getId(), PropertyIdentifier.logBuffer),
                        UnsignedInteger.ZERO, new UnsignedInteger(3))), notif.eventValues());

        // Trigger another batch of updates. One notification should be sent.
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        clock.plusSeconds(1);
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(28), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(23), notif.notificationClass());
        assertEquals(new UnsignedInteger(3), notif.priority());
        assertEquals(EventType.bufferReady, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.TRUE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, el.getId(), PropertyIdentifier.logBuffer),
                        new UnsignedInteger(3), new UnsignedInteger(10))), notif.eventValues());
    }

    @Test
    public void stopWhenFull() throws Exception {
        // Create a triggered trend log
        final EventLogObject el =
                new EventLogObject(d1, 0, "el", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, true, 4);

        // Add a couple records and validate the buffer content
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        assertEquals(2, el.getBuffer().size());
        assertEquals(n1, el.getBuffer().get(0).getNotification());
        assertEquals(n1, el.getBuffer().get(1).getNotification());
        assertEquals(new UnsignedInteger(2), el.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(2), el.get(PropertyIdentifier.totalRecordCount));

        // Add another record. This will cause the buffer to be full after the buffer full notification is written.
        d2.send(rd1, n1).get();
        assertEquals(4, el.getBuffer().size());
        assertEquals(n1, el.getBuffer().get(0).getNotification());
        assertEquals(n1, el.getBuffer().get(1).getNotification());
        assertEquals(n1, el.getBuffer().get(2).getNotification());
        assertEquals(new LogStatus(true, false, false), el.getBuffer().get(3).getLogStatus());
        assertEquals(new UnsignedInteger(4), el.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), el.get(PropertyIdentifier.totalRecordCount));
        assertTrue(el.isLogDisabled());

        // Add more records. The log should not change. Advance the time just to be sure.
        clock.plusMinutes(1);
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        assertEquals(4, el.getBuffer().size());
        assertEquals(n1, el.getBuffer().get(0).getNotification());
        assertEquals(n1, el.getBuffer().get(1).getNotification());
        assertEquals(n1, el.getBuffer().get(2).getNotification());
        assertEquals(new LogStatus(true, false, false), el.getBuffer().get(3).getLogStatus());
        assertEquals(new UnsignedInteger(4), el.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), el.get(PropertyIdentifier.totalRecordCount));
        assertTrue(el.isLogDisabled());

        // Set StopWhenFull to false and write a couple records.
        el.writeProperty(null, new PropertyValue(PropertyIdentifier.stopWhenFull, Boolean.FALSE));
        el.writeProperty(null, new PropertyValue(PropertyIdentifier.enable, Boolean.TRUE));
        d2.send(rd1, n1).get();
        d2.send(rd1, n1).get();
        assertEquals(4, el.getBuffer().size());
        assertEquals(n1, el.getBuffer().get(0).getNotification());
        assertEquals(new LogStatus(true, false, false), el.getBuffer().get(1).getLogStatus());
        assertEquals(n1, el.getBuffer().get(2).getNotification());
        assertEquals(n1, el.getBuffer().get(3).getNotification());
        assertEquals(new UnsignedInteger(4), el.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(6), el.get(PropertyIdentifier.totalRecordCount));
        assertFalse(el.isLogDisabled());

        // Set StopWhenFull back to true.
        el.writeProperty(null, new PropertyValue(PropertyIdentifier.stopWhenFull, Boolean.TRUE));
        assertEquals(4, el.getBuffer().size());
        assertEquals(new LogStatus(true, false, false), el.getBuffer().get(0).getLogStatus());
        assertEquals(n1, el.getBuffer().get(1).getNotification());
        assertEquals(n1, el.getBuffer().get(2).getNotification());
        assertEquals(new LogStatus(true, false, false), el.getBuffer().get(3).getLogStatus());
        assertEquals(new UnsignedInteger(4), el.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(7), el.get(PropertyIdentifier.totalRecordCount));
        assertTrue(el.isLogDisabled());
    }

    @Test
    public void enableDisable() throws Exception {
        // Create a disabled triggered trend log
        final EventLogObject el =
                new EventLogObject(d1, 0, "el", new LinkedListLogBuffer<>(), false, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, true, 4);
        assertEquals(0, el.getBuffer().size());

        // Add a couple records and validate the buffer content
        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(0, el.getBuffer().size());

        // Enable and write a few records.
        el.setEnabled(false);
        assertEquals(0, el.getBuffer().size());
        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(0, el.getBuffer().size());
    }

    @Test
    public void startStopTimes() throws Exception {
        final DateTime now = new DateTime(clock.millis());
        GregorianCalendar nowgg = now.getGC();

        // Set the start time to 5 minutes from now.
        nowgg.add(Calendar.MINUTE, 5);
        DateTime startTime = new DateTime(nowgg);

        // Set the stop time to 10 minutes from now.
        nowgg.add(Calendar.MINUTE, 5);
        DateTime stopTime = new DateTime(nowgg);

        // Create a triggered trend log
        final EventLogObject el =
                new EventLogObject(d1, 0, "el", new LinkedListLogBuffer<>(), true, startTime, stopTime, true, 7);
        assertTrue(el.isLogDisabled());
        assertEquals(0, el.getBuffer().size());

        // Do some triggers.
        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(0, el.getBuffer().size());

        // Advance the time a bit and do some triggers.
        advanceClock(clock, 3, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, null, TestUtils::quiesce);
        assertTrue(el.isLogDisabled());
        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(0, el.getBuffer().size());

        // Advance the time past the start time and do some triggers.
        advanceClock(clock, 3, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, null, TestUtils::quiesce);
        assertFalse(el.isLogDisabled());
        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(2, el.getBuffer().size());

        // Advance the time past the stop time and do some triggers.
        advanceClock(clock, 5, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, null, TestUtils::quiesce);
        final DateTime now3 = new DateTime(clock.millis());
        assertTrue(el.isLogDisabled());
        assertEquals(3, el.getBuffer().size());
        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(3, el.getBuffer().size());
        assertEquals(n2, el.getBuffer().get(0).getNotification());
        assertEquals(n2, el.getBuffer().get(1).getNotification());
        assertEquals(new LogStatus(true, false, false), el.getBuffer().get(2).getLogStatus());

        // Reset the start and stop times.
        nowgg = now3.getGC();
        nowgg.add(Calendar.MINUTE, 5);
        startTime = new DateTime(nowgg);
        nowgg.add(Calendar.MINUTE, 5);
        stopTime = new DateTime(nowgg);
        el.writeProperty(null, PropertyIdentifier.startTime, startTime);
        el.writeProperty(null, PropertyIdentifier.stopTime, stopTime);

        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(3, el.getBuffer().size());

        // Advance the time past the start time and do some triggers.
        advanceClock(clock, 6, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, null, TestUtils::quiesce);
        assertFalse(el.isLogDisabled());
        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(5, el.getBuffer().size());

        // Advance the time past the stop time and do some triggers.
        advanceClock(clock, 5, TimeUnit.MINUTES, 1, TimeUnit.MINUTES, null, TestUtils::quiesce);
        assertTrue(el.isLogDisabled());
        assertEquals(6, el.getBuffer().size());
        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(6, el.getBuffer().size());
    }

    @Test
    public void readLogBuffer() throws Exception {
        // Create a triggered trend log
        final EventLogObject el =
                new EventLogObject(d1, 0, "el", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, true, 7);

        // Try to do a network read of the buffer. It should not be readable.
        assertBACnetServiceException(() -> el.readProperty(PropertyIdentifier.logBuffer, null), ErrorClass.property,
                ErrorCode.readAccessDenied);
    }

    @Test
    public void purge() throws Exception {
        // Create a triggered trend log
        final EventLogObject el =
                new EventLogObject(d1, 0, "el", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, true, 7);

        // Trigger a few updates.
        d2.send(rd1, n2).get();
        d2.send(rd1, n2).get();
        assertEquals(2, el.getBuffer().size());

        // Set the record count to non-zero.
        assertBACnetServiceException(
                () -> el.writeProperty(null, new PropertyValue(PropertyIdentifier.recordCount, new UnsignedInteger(1))),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        // Set the record count to zero. There should be one log status record.
        el.writeProperty(null, new PropertyValue(PropertyIdentifier.recordCount, UnsignedInteger.ZERO));
        assertEquals(1, el.getBuffer().size());
        assertEquals(new LogStatus(false, true, false), el.getBuffer().get(0).getLogStatus());
    }
}
