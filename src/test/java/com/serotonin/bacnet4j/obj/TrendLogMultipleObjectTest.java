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
import static com.serotonin.bacnet4j.TestUtils.await;
import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.awaitFalse;
import static com.serotonin.bacnet4j.TestUtils.awaitTrue;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LogData;
import com.serotonin.bacnet4j.type.constructed.LogData.LogDataElement;
import com.serotonin.bacnet4j.type.constructed.LogMultipleRecord;
import com.serotonin.bacnet4j.type.constructed.LogStatus;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.eventParameter.BufferReady;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class TrendLogMultipleObjectTest extends AbstractTest {
    static final Logger LOG = LoggerFactory.getLogger(TrendLogMultipleObjectTest.class);

    private NotificationClassObject nc;
    private AnalogValueObject ao;
    private AnalogInputObject ai;

    private BACnetArray<DeviceObjectPropertyReference> props;
    private final ErrorClassAndCode unknownObject = new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject);
    private final ErrorClassAndCode noPropSpecified =
            new ErrorClassAndCode(ErrorClass.property, ErrorCode.noPropertySpecified);

    @Override
    public void afterInit() throws Exception {
        nc = new NotificationClassObject(d1, 23, "nc", 1, 2, 3, new EventTransitionBits(true, true, true));
        ao = new AnalogValueObject(d1, 0, "ao", 0, EngineeringUnits.noUnits, false).supportCovReporting(0.5F);
        ai = new AnalogInputObject(d2, 0, "ai", 0, EngineeringUnits.noUnits, false);

        props = new BACnetArray<>( //
                // Remote
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue),
                // Local
                new DeviceObjectPropertyReference(1, ao.getId(), PropertyIdentifier.presentValue),
                // Unknown object
                new DeviceObjectPropertyReference(2, new ObjectIdentifier(ObjectType.accessCredential, 0),
                        PropertyIdentifier.presentValue),
                // Uninitialized object
                new DeviceObjectPropertyReference(2,
                        new ObjectIdentifier(ObjectType.accessDoor, ObjectIdentifier.UNINITIALIZED),
                        PropertyIdentifier.presentValue),
                // Uninitialized device
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.analogInput, 0),
                        PropertyIdentifier.presentValue, null,
                        new ObjectIdentifier(ObjectType.device, ObjectIdentifier.UNINITIALIZED)));
    }

    @Test
    public void pollingAlignedWithOffset() throws Exception {
        // Construct the log to poll each minute, aligned, and with a 2s offset.
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, props, 0, false, 20) //
                        .withPolled(1, MINUTES, true, 2, SECONDS);

        assertEquals(0, tl.getRecordCount());

        //
        // Advance the clock to the polling time.
        LOG.debug("start: {}", clock.instant());
        final int seconds = (62 - clock.get(ChronoField.SECOND_OF_MINUTE) - 1) % 60 + 1;
        clock.plusSeconds(seconds);
        LOG.debug("poll: {}", clock.instant());

        awaitEquals(1, tl::getRecordCount);
        final LogMultipleRecord record0 = tl.getRecord(0);
        // We asked for alignment and an offset of 2 seconds.
        assertEquals(2.0, record0.getTimestamp().getTime().getSecond(), 1.0);
        assertEquals(1, record0.getSequenceNumber());
        assertEquals(5, record0.getLogData().getData().size());
        assertEquals(new Real(0), record0.getLogData().getData().get(0).getDatum());
        assertEquals(new Real(0), record0.getLogData().getData().get(1).getDatum());
        assertEquals(unknownObject, record0.getLogData().getData().get(2).getDatum());
        assertEquals(noPropSpecified, record0.getLogData().getData().get(3).getDatum());
        assertEquals(noPropSpecified, record0.getLogData().getData().get(4).getDatum());

        //
        // Update the object present value.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(2));

        // Advance the clock another minute to poll again
        clock.plusMinutes(1);
        LOG.debug("poll: {}", clock.instant());

        awaitEquals(2, tl::getRecordCount);
        final LogMultipleRecord record1 = tl.getRecord(1);
        assertEquals(2, record1.getTimestamp().getTime().getSecond());
        assertEquals((record0.getTimestamp().getTime().getMinute() + 1) % 60,
                record1.getTimestamp().getTime().getMinute());
        assertEquals(2, record1.getSequenceNumber());
        assertEquals(5, record1.getLogData().getData().size());
        assertEquals(new Real(2), record1.getLogData().getData().get(0).getDatum());
        assertEquals(new Real(0), record1.getLogData().getData().get(1).getDatum());
        assertEquals(unknownObject, record1.getLogData().getData().get(2).getDatum());
        assertEquals(noPropSpecified, record1.getLogData().getData().get(3).getDatum());
        assertEquals(noPropSpecified, record1.getLogData().getData().get(4).getDatum());

        //
        // Update the log interval to 1 hour.
        tl.writeProperty(null, PropertyIdentifier.logInterval, new UnsignedInteger(60 * 60 * 100));
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(3));

        // Advance the clock to the new polling time.
        final int minutes = (62 - clock.get(ChronoField.MINUTE_OF_HOUR)) % 60;
        clock.plusMinutes(minutes);
        LOG.debug("poll: {}", clock.instant());

        awaitEquals(3, tl::getRecordCount);
        assertEquals(3, tl.getRecordCount());
        final LogMultipleRecord record2 = tl.getRecord(2);
        assertEquals(2, record2.getTimestamp().getTime().getMinute());
        assertEquals(3, record2.getSequenceNumber());
        assertEquals(5, record2.getLogData().getData().size());
        assertEquals(new Real(3), record2.getLogData().getData().get(0).getDatum());
        assertEquals(new Real(0), record2.getLogData().getData().get(1).getDatum());
        assertEquals(unknownObject, record2.getLogData().getData().get(2).getDatum());
        assertEquals(noPropSpecified, record2.getLogData().getData().get(3).getDatum());
        assertEquals(noPropSpecified, record2.getLogData().getData().get(4).getDatum());

        //
        // Try a trigger for fun.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(4));
        tl.trigger();

        // Wait for the polling to finish.
        awaitEquals(4, tl::getRecordCount);
        final LogMultipleRecord record3 = tl.getRecord(3);
        assertEquals(4, record3.getSequenceNumber());
        assertEquals(5, record3.getLogData().getData().size());
        assertEquals(new Real(4), record3.getLogData().getData().get(0).getDatum());
        assertEquals(new Real(0), record3.getLogData().getData().get(1).getDatum());
        assertEquals(unknownObject, record3.getLogData().getData().get(2).getDatum());
        assertEquals(noPropSpecified, record3.getLogData().getData().get(3).getDatum());
        assertEquals(noPropSpecified, record3.getLogData().getData().get(4).getDatum());
    }

    @Test
    public void trigger() throws Exception {
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, props, 0, false, 20);

        final DateTime now = new DateTime(clock.millis());

        // The buffer should still be empty
        assertEquals(0, tl.getRecordCount());

        // Update a monitored value
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(2));

        // The buffer should still be empty
        assertEquals(0, tl.getRecordCount());

        // Update the monitored value again
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(3));

        // The buffer should still be empty
        assertEquals(0, tl.getRecordCount());

        // Trigger an update
        tl.trigger();
        awaitEquals(1, tl::getRecordCount);

        // The log record should be there.
        final LogMultipleRecord record0 = tl.getRecord(0);
        assertEquals(now, record0.getTimestamp());
        assertEquals(1, record0.getSequenceNumber());
        final LogData data0 = record0.getLogData();
        assertEquals(new SequenceOf<>( //
                        new LogDataElement(new Real(3)), //
                        new LogDataElement(new Real(0)), //
                        new LogDataElement(new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject)),
                        new LogDataElement(new ErrorClassAndCode(ErrorClass.property, ErrorCode.noPropertySpecified)),
                        new LogDataElement(new ErrorClassAndCode(ErrorClass.property, ErrorCode.noPropertySpecified))),
                data0.getDatum());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        // Create a triggered trend log with intrinsic reporting enabled.
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, props, 0, false, 20) //
                        .supportIntrinsicReporting(5, 23, new EventTransitionBits(true, true, true), NotifyType.event);

        final RemoteDevice rd2 = d1.getRemoteDeviceBlocking(2);

        // Add d2 as an event recipient.
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(27), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        //
        // Write 4 triggers and make sure no notification was sent.
        doTriggers(tl, 4);
        assertEquals(4, tl.getRecordCount());
        assertEquals(0, listener.getNotifCount());
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(UnsignedInteger.ZERO, tl.get(PropertyIdentifier.lastNotifyRecord));

        //
        // Write one more and make sure a notification was received.
        doTriggers(tl, 1);
        awaitEquals(5, tl::getRecordCount);
        awaitEquals(1, listener::getNotifCount);
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(27), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(tl.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) tl.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
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
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                        UnsignedInteger.ZERO, new UnsignedInteger(5))), notif.eventValues());

        // Validate the internally maintained values.
        assertEquals(new UnsignedInteger(5), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(5), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(UnsignedInteger.ZERO, tl.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(new UnsignedInteger(5), tl.get(PropertyIdentifier.lastNotifyRecord));

        //
        // Write another 5 triggers and ensure that the notification looks ok.
        doTriggers(tl, 5);
        awaitEquals(10, tl::getRecordCount);
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(27), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(tl.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) tl.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
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
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                        new UnsignedInteger(5), new UnsignedInteger(10))), notif.eventValues());

        // Validate the internally maintained values.
        assertEquals(new UnsignedInteger(10), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(10), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(UnsignedInteger.ZERO, tl.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(new UnsignedInteger(10), tl.get(PropertyIdentifier.lastNotifyRecord));

        //
        // Update the values of the trend log such that we can trigger condition 2 in the buffer ready algo.
        tl.set(PropertyIdentifier.lastNotifyRecord, new UnsignedInteger(0xFFFFFFFDL));
        tl.set(PropertyIdentifier.totalRecordCount, new UnsignedInteger(0xFFFFFFFDL));
        doTriggers(tl, 5);
        awaitEquals(15, tl::getRecordCount);
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(27), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(tl.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) tl.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
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
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                        new UnsignedInteger(0xFFFFFFFDL), new UnsignedInteger(3))), notif.eventValues());

        // Validate the internally maintained values.
        assertEquals(new UnsignedInteger(15), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(3), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(UnsignedInteger.ZERO, tl.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(new UnsignedInteger(3), tl.get(PropertyIdentifier.lastNotifyRecord));
    }

    private static void doTriggers(final TrendLogMultipleObject tl, final int count) throws Exception {
        int remaining = count;
        while (remaining > 0) {
            await(tl::trigger);
            remaining--;
        }
        await(() -> !((Boolean) tl.get(PropertyIdentifier.trigger)).booleanValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void eventReporting() throws Exception {
        // Create a triggered trend log
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, props, 0, false, 20);

        // Create the event enrollment.
        final DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(tl.getId(), PropertyIdentifier.totalRecordCount, null, d1.getId());
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
        doTriggers(tl, 2);

        // Give the EE a chance to poll.
        clock.plusSeconds(1);
        quiesce();

        // Ensure that there are no notifications.
        assertEquals(0, listener.getNotifCount());

        // Trigger another notification so that a notification is sent.
        doTriggers(tl, 1);
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
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                        UnsignedInteger.ZERO, new UnsignedInteger(3))), notif.eventValues());

        // Trigger another batch of updates. One notification should be sent.
        doTriggers(tl, 7);
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
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                        new UnsignedInteger(3), new UnsignedInteger(10))), notif.eventValues());
    }

    @Test
    public void stopWhenFull() throws Exception {
        // Create a triggered trend log
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, props, 0, true, 4);

        final SequenceOf<LogDataElement> logData = new SequenceOf<>( //
                new LogDataElement(new Real(0)), //
                new LogDataElement(new Real(0)), //
                new LogDataElement(unknownObject), //
                new LogDataElement(noPropSpecified), //
                new LogDataElement(noPropSpecified));

        // Add a couple records and validate the buffer content
        doTriggers(tl, 2);
        assertEquals(2, tl.getRecordCount());
        assertEquals(logData, tl.getRecord(0).getLogData().getData());
        assertEquals(logData, tl.getRecord(1).getLogData().getData());
        assertEquals(new UnsignedInteger(2), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(2), tl.get(PropertyIdentifier.totalRecordCount));

        // Add another record. This will cause the buffer to be full after the buffer full notification is written.
        doTriggers(tl, 1);
        assertEquals(4, tl.getRecordCount());
        assertEquals(logData, tl.getRecord(0).getLogData().getData());
        assertEquals(logData, tl.getRecord(1).getLogData().getData());
        assertEquals(logData, tl.getRecord(2).getLogData().getData());
        assertEquals(new LogStatus(true, false, false), tl.getRecord(3).getLogData().getLogStatus());
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.totalRecordCount));
        assertTrue(tl.isLogDisabled());

        // Add more records. The log should not change. Advance the time just to be sure.
        clock.plusMinutes(1);
        doTriggers(tl, 2);
        assertEquals(4, tl.getRecordCount());
        assertEquals(logData, tl.getRecord(0).getLogData().getData());
        assertEquals(logData, tl.getRecord(1).getLogData().getData());
        assertEquals(logData, tl.getRecord(2).getLogData().getData());
        assertEquals(new LogStatus(true, false, false), tl.getRecord(3).getLogData().getLogStatus());
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.totalRecordCount));
        assertTrue(tl.isLogDisabled());

        // Set StopWhenFull to false and write a couple records.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.stopWhenFull, Boolean.FALSE));
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.enable, Boolean.TRUE));
        doTriggers(tl, 2);
        assertEquals(4, tl.getRecordCount());
        assertEquals(logData, tl.getRecord(0).getLogData().getData());
        assertEquals(new LogStatus(true, false, false), tl.getRecord(1).getLogData().getLogStatus());
        assertEquals(logData, tl.getRecord(2).getLogData().getData());
        assertEquals(logData, tl.getRecord(3).getLogData().getData());
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(6), tl.get(PropertyIdentifier.totalRecordCount));
        assertFalse(tl.isLogDisabled());

        // Set StopWhenFull back to true.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.stopWhenFull, Boolean.TRUE));
        assertEquals(4, tl.getRecordCount());
        assertEquals(new LogStatus(true, false, false), tl.getRecord(0).getLogData().getLogStatus());
        assertEquals(logData, tl.getRecord(1).getLogData().getData());
        assertEquals(logData, tl.getRecord(2).getLogData().getData());
        assertEquals(new LogStatus(true, false, false), tl.getRecord(3).getLogData().getLogStatus());
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(7), tl.get(PropertyIdentifier.totalRecordCount));
        assertTrue(tl.isLogDisabled());
    }

    @Test
    public void enableDisable() throws Exception {
        // Create a disabled triggered trend log
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), false, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, props, 0, true, 4);

        assertEquals(0, tl.getRecordCount());

        // Add a couple records and validate the buffer content
        doTriggers(tl, 2);
        assertEquals(0, tl.getRecordCount());

        // Enable and write a few records.
        tl.setEnabled(false);
        assertEquals(0, tl.getRecordCount());
        doTriggers(tl, 2);
        assertEquals(0, tl.getRecordCount());
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

        final SequenceOf<LogDataElement> logData = new SequenceOf<>( //
                new LogDataElement(new Real(0)), //
                new LogDataElement(new Real(0)), //
                new LogDataElement(unknownObject), //
                new LogDataElement(noPropSpecified), //
                new LogDataElement(noPropSpecified));

        // Create a triggered trend log
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), true, startTime, stopTime, props,
                        0, true, 7);

        assertTrue(tl.isLogDisabled());
        assertEquals(0, tl.getRecordCount());

        // Do some triggers.
        doTriggers(tl, 2);
        assertEquals(0, tl.getRecordCount());

        // Advance the time a bit and do some triggers.
        clock.plusMinutes(3);
        quiesce();
        assertTrue(tl.isLogDisabled());
        doTriggers(tl, 2);
        assertEquals(0, tl.getRecordCount());

        // Advance the time past the start time and do some triggers.
        clock.plusMinutes(3);
        awaitFalse(tl::isLogDisabled);
        doTriggers(tl, 2);
        assertEquals(2, tl.getRecordCount());

        // Advance the time past the stop time and do some triggers.
        clock.plusMinutes(5);
        awaitTrue(tl::isLogDisabled);
        awaitEquals(3, tl::getRecordCount);
        final DateTime now3 = new DateTime(clock.millis());
        doTriggers(tl, 2);
        assertEquals(3, tl.getRecordCount());
        assertEquals(logData, tl.getRecord(0).getLogData().getData());
        assertEquals(logData, tl.getRecord(1).getLogData().getData());
        assertEquals(new LogStatus(true, false, false), tl.getRecord(2).getLogData().getLogStatus());

        // Reset the start and stop times.
        nowgg = now3.getGC();
        nowgg.add(Calendar.MINUTE, 5);
        startTime = new DateTime(nowgg);
        nowgg.add(Calendar.MINUTE, 5);
        stopTime = new DateTime(nowgg);
        tl.writeProperty(null, PropertyIdentifier.startTime, startTime);
        tl.writeProperty(null, PropertyIdentifier.stopTime, stopTime);

        doTriggers(tl, 2);
        assertEquals(3, tl.getRecordCount());

        // Advance the time past the start time and do some triggers.
        clock.plusMinutes(6);
        awaitFalse(tl::isLogDisabled);
        doTriggers(tl, 2);
        assertEquals(5, tl.getRecordCount());

        // Advance the time past the stop time and do some triggers.
        clock.plusMinutes(5);
        awaitTrue(tl::isLogDisabled);
        awaitEquals(6, tl::getRecordCount);
        doTriggers(tl, 2);
        assertEquals(6, tl.getRecordCount());
    }

    @Test
    public void readLogBuffer() throws Exception {
        // Create a triggered trend log
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, props, 0, true, 7);

        // Try to do a network read of the buffer. It should not be readable.
        assertBACnetServiceException(() -> tl.readProperty(PropertyIdentifier.logBuffer, null), ErrorClass.property,
                ErrorCode.readAccessDenied);
    }

    @Test
    public void purge() throws Exception {
        // Create a triggered trend log
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, props, 0, true, 7);

        // Trigger a few updates.
        doTriggers(tl, 2);
        assertEquals(2, tl.getRecordCount());

        // Set the record count to non-zero.
        assertBACnetServiceException(
                () -> tl.writeProperty(null, new PropertyValue(PropertyIdentifier.recordCount, new UnsignedInteger(1))),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        // Set the record count to zero. There should be one log status record.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.recordCount, UnsignedInteger.ZERO));
        assertEquals(1, tl.getRecordCount());
        assertEquals(new LogStatus(false, true, false), tl.getRecord(0).getLogData().getLogStatus());
    }

    @Test
    public void writePropertyReference() throws Exception {
        ao.writePropertyInternal(PropertyIdentifier.presentValue, new Real(13));
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(14));

        final SequenceOf<LogDataElement> logData1 = new SequenceOf<>( //
                new LogDataElement(new Real(14)), //
                new LogDataElement(new Real(13)), //
                new LogDataElement(unknownObject), //
                new LogDataElement(noPropSpecified), //
                new LogDataElement(noPropSpecified));

        // Create a triggered trend log
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d1, 0, "tlm", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, props, 0, true, 100);

        doTriggers(tl, 2);
        assertEquals(2, tl.getRecordCount());
        assertEquals(logData1, tl.getRecord(0).getLogData().getData());
        assertEquals(logData1, tl.getRecord(1).getLogData().getData());

        final BACnetArray<DeviceObjectPropertyReference> newProps = new BACnetArray<>( //
                // Remote
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue),
                // Local
                new DeviceObjectPropertyReference(1, ao.getId(), PropertyIdentifier.presentValue),
                // Uninitialized object
                new DeviceObjectPropertyReference(2,
                        new ObjectIdentifier(ObjectType.accessDoor, ObjectIdentifier.UNINITIALIZED),
                        PropertyIdentifier.presentValue),
                // Uninitialized device
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.analogInput, 0),
                        PropertyIdentifier.presentValue, null,
                        new ObjectIdentifier(ObjectType.device, ObjectIdentifier.UNINITIALIZED)));

        final SequenceOf<LogDataElement> logData2 = new SequenceOf<>( //
                new LogDataElement(new Real(14)), //
                new LogDataElement(new Real(13)), //
                new LogDataElement(noPropSpecified), //
                new LogDataElement(noPropSpecified));

        // Modify the property reference as a network write.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.logDeviceObjectProperty, newProps));

        doTriggers(tl, 2);
        assertEquals(3, tl.getRecordCount());
        assertEquals(new LogStatus(false, true, false), tl.getRecord(0).getLogData().getLogStatus());
        assertEquals(logData2, tl.getRecord(1).getLogData().getData());
        assertEquals(logData2, tl.getRecord(2).getLogData().getData());
    }
}
