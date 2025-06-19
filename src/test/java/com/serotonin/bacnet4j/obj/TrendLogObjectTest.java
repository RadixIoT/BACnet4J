package com.serotonin.bacnet4j.obj;

import static com.serotonin.bacnet4j.TestUtils.assertBACnetServiceException;
import static com.serotonin.bacnet4j.TestUtils.await;
import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.awaitFalse;
import static com.serotonin.bacnet4j.TestUtils.awaitTrue;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.ClientCov;
import com.serotonin.bacnet4j.type.constructed.CovSubscription;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LogRecord;
import com.serotonin.bacnet4j.type.constructed.LogStatus;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.eventParameter.BufferReady;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class TrendLogObjectTest extends AbstractTest {
    static final Logger LOG = LoggerFactory.getLogger(TrendLogObjectTest.class);

    private NotificationClassObject nc;
    private AnalogValueObject ao;
    private AnalogInputObject ai;

    @Override
    public void afterInit() throws Exception {
        nc = new NotificationClassObject(d1, 23, "nc", 1, 2, 3, new EventTransitionBits(true, true, true));
        ao = new AnalogValueObject(d1, 0, "ao", 0, EngineeringUnits.noUnits, false).supportCovReporting(0.5F);
        ai = new AnalogInputObject(d2, 0, "ai", 0, EngineeringUnits.noUnits, false).supportCovReporting(0.5F);
    }

    @Test
    public void remotePollingAlignedWithOffset() throws Exception {
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false,
                        20).withPolled(1, TimeUnit.MINUTES, true, 2, TimeUnit.SECONDS);
        polling(tl, ai);
    }

    @Test
    public void localPolling() throws Exception {
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(1, ao.getId(), PropertyIdentifier.presentValue), 0, false,
                        20).withPolled(1, TimeUnit.MINUTES, true, 2, TimeUnit.SECONDS);
        polling(tl, ao);
    }

    private void polling(final TrendLogObject tl, final BACnetObject bo) throws Exception {
        assertEquals(0, tl.getRecordCount());

        //
        // Advance the clock to the polling time.
        LOG.info("Starting time: {}", clock.instant());
        final int seconds = (62 - clock.get(ChronoField.SECOND_OF_MINUTE) - 1) % 60 + 1;
        clock.plusSeconds(seconds);
        quiesce();
        LOG.info("First poll time: {}", clock.instant());

        assertEquals(1, tl.getRecordCount());
        final LogRecord record1 = tl.getRecord(0);
        // We asked for alignment and an offset of 2 seconds.
        assertEquals(2.0, record1.getTimestamp().getTime().getSecond(), 1.0);
        assertEquals(new Real(0), record1.getChoice());
        assertEquals(new StatusFlags(false, false, false, false), record1.getStatusFlags());

        //
        // Update the object present value.
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(2));

        // Advance the clock another minute to poll again.
        clock.plusMinutes(1);

        awaitEquals(2, tl::getRecordCount);
        final LogRecord record2 = tl.getRecord(1);
        assertEquals(2, record2.getTimestamp().getTime().getSecond());
        assertEquals((record1.getTimestamp().getTime().getMinute() + 1) % 60,
                record2.getTimestamp().getTime().getMinute());
        assertEquals(new Real(2), record2.getChoice());
        assertEquals(new StatusFlags(false, false, false, false), record2.getStatusFlags());

        //
        // Update the object overridden.
        bo.setOverridden(true);

        // Advance the clock another minute to poll again.
        clock.plusMinutes(1);
        awaitEquals(3, tl::getRecordCount);

        final LogRecord record3 = tl.getRecord(2);
        assertEquals(2, record3.getTimestamp().getTime().getSecond());
        assertEquals((record1.getTimestamp().getTime().getMinute() + 2) % 60,
                record3.getTimestamp().getTime().getMinute());
        assertEquals(new Real(2), record3.getChoice());
        assertEquals(new StatusFlags(false, false, true, false), record3.getStatusFlags());

        //
        // Update the log interval to 1 hour.
        tl.writeProperty(null, PropertyIdentifier.logInterval, new UnsignedInteger(60 * 60 * 100));
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(3));

        // Advance the clock to the new polling time.
        final int minutes = (62 - clock.get(ChronoField.MINUTE_OF_HOUR)) % 60;
        clock.plusMinutes(minutes);

        awaitEquals(4, tl::getRecordCount);
        final LogRecord record4 = tl.getRecord(3);
        assertEquals(2, record4.getTimestamp().getTime().getMinute());
        assertEquals(new Real(3), record4.getChoice());
        assertEquals(new StatusFlags(false, false, true, false), record4.getStatusFlags());

        //
        // Try a trigger for fun.
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(4));
        bo.setOverridden(false);
        tl.trigger();

        // Wait for the polling to finish.
        awaitEquals(5, tl::getRecordCount);
        final LogRecord record5 = tl.getRecord(4);
        assertEquals(new Real(4), record5.getChoice());
        assertEquals(new StatusFlags(false, false, false, false), record5.getStatusFlags());
    }

    @Test
    public void remoteCov() throws Exception {
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false,
                        20).withCov(100, new ClientCov(Null.instance));
        cov(tl, d2, ai);
    }

    @Test
    public void localCov() throws Exception {
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(1, ao.getId(), PropertyIdentifier.presentValue), 0, false,
                        20).withCov(100, new ClientCov(new Real(0.3F)));
        cov(tl, d1, ao);
    }

    private void cov(final TrendLogObject tl, final LocalDevice d, final BACnetObject bo) throws Exception {
        DateTime now = new DateTime(clock.millis());

        // Wait for the COV to set up, and for the initial notification to be sent.
        // Wait for the subscription to be there.
        awaitEquals(1, () -> {
            SequenceOf<CovSubscription> subscriptions =
                    d.getObject(d.getId()).readProperty(PropertyIdentifier.activeCovSubscriptions);
            return subscriptions.getCount();
        });

        // Remember the process id.
        SequenceOf<CovSubscription> subscriptions =
                d.getObject(d.getId()).readProperty(PropertyIdentifier.activeCovSubscriptions);
        final int processId = subscriptions.getBase1(1).getRecipient().getProcessIdentifier().intValue();

        // The initial notification should be there.
        awaitEquals(1, tl::getRecordCount);
        final LogRecord record1 = tl.getRecord(0);
        assertEquals(now, record1.getTimestamp());
        assertEquals(new Real(0), record1.getChoice());
        assertEquals(new StatusFlags(false, false, false, false), record1.getStatusFlags());

        //
        // Update the value to cause a COV notification.
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(1));
        LOG.info("Update");
        awaitEquals(2, tl::getRecordCount);
        final LogRecord record2 = tl.getRecord(1);
        assertEquals(now, record2.getTimestamp());
        assertEquals(new Real(1), record2.getChoice());
        assertEquals(new StatusFlags(false, false, false, false), record2.getStatusFlags());

        //
        // Advance the clock a bit and update again. No notification will be received because the update was below the
        // update threshold.
        clock.plusSeconds(45);
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(1.2F));
        awaitEquals(2, tl::getRecordCount);
        assertEquals(2, tl.getRecordCount());

        //
        // Advance the clock a bit and update again. No notification will be received because the update was below the
        // update threshold.
        clock.plusSeconds(25);
        now = new DateTime(clock.millis());
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(1.6F));
        awaitEquals(3, tl::getRecordCount);
        final LogRecord record3 = tl.getRecord(2);
        assertEquals(now, record3.getTimestamp());
        assertEquals(new Real(1.6F), record3.getChoice());
        assertEquals(new StatusFlags(false, false, false, false), record3.getStatusFlags());

        //
        // Advance the clock a bit and override.
        clock.plusSeconds(12);
        now = new DateTime(clock.millis());
        bo.setOverridden(true);
        awaitEquals(4, tl::getRecordCount);
        final LogRecord record4 = tl.getRecord(3);
        assertEquals(now, record4.getTimestamp());
        assertEquals(new Real(1.6F), record4.getChoice());
        assertEquals(new StatusFlags(false, false, true, false), record4.getStatusFlags());

        //
        // Advance the clock past the resubscription period. Another notification should be received.
        clock.plusSeconds(20);
        now = new DateTime(clock.millis());
        awaitEquals(5, tl::getRecordCount);
        final LogRecord record5 = tl.getRecord(4);
        assertEquals(now, record5.getTimestamp());
        assertEquals(new Real(1.6F), record5.getChoice());
        assertEquals(new StatusFlags(false, false, true, false), record5.getStatusFlags());

        //
        // Change the resubscription interval
        tl.writeProperty(null, PropertyIdentifier.covResubscriptionInterval, new UnsignedInteger(300));
        // Wait for there to be an update due to the resubscription.
        awaitEquals(6, tl::getRecordCount);

        // Check that there is still only one subscription, and that it has a different process id.
        subscriptions = d.getObject(d.getId()).readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(1, subscriptions.getValues().size());
        final int processId2 = subscriptions.getBase1(1).getRecipient().getProcessIdentifier().intValue();
        assertEquals(processId + 1, processId2);

        // Check that an update was sent due to the resubscription.
        assertEquals(6, tl.getRecordCount());

        //
        // Advance the clock past the new resubscription period. Only one more notification should be received.
        clock.plusSeconds(310);
        awaitEquals(7, tl::getRecordCount);

        //
        // Try a trigger for fun.
        tl.trigger();

        // Wait for the polling to finish.
        awaitEquals(8, tl::getRecordCount);
    }

    @Test
    public void trigger() throws Exception {
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false,
                        20).withTriggered();

        final DateTime now = new DateTime(clock.millis());

        // The buffer should still be empty
        assertEquals(0, tl.getRecordCount());

        // Update the monitored value
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(2));

        // The buffer should still be empty
        assertEquals(0, tl.getRecordCount());

        // Update the monitored value again
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(3));

        // The buffer should still be empty
        assertEquals(0, tl.getRecordCount());

        // Trigger an update
        LOG.info("Trigger");
        tl.trigger();
        awaitEquals(1, tl::getRecordCount);

        // The log record should be there.
        assertEquals(1, tl.getRecordCount());
        final LogRecord record1 = tl.getRecord(0);
        assertEquals(now, record1.getTimestamp());
        assertEquals(new Real(3), record1.getChoice());
        assertEquals(new StatusFlags(false, false, false, false), record1.getStatusFlags());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        // Create a triggered trend log with intrinsic reporting enabled.
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false,
                        20).withTriggered()
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
        assertEquals(1, listener.getNotifCount());
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

    private static void doTriggers(final TrendLogObject tl, final int count) throws Exception {
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
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false,
                        20).withTriggered();

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
        // Wait for a bit for some assurance that nothing has changed.
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
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true,
                        4).withTriggered();

        final DateTime now = new DateTime(clock.millis());
        final StatusFlags sf = new StatusFlags(false, false, false, false);

        // Add a couple records and validate the buffer content
        doTriggers(tl, 2);
        assertEquals(2, tl.getRecordCount());
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), tl.getRecord(0));
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), tl.getRecord(1));
        assertEquals(new UnsignedInteger(2), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(2), tl.get(PropertyIdentifier.totalRecordCount));

        // Add another record. This will cause the buffer to be full after the buffer full notification is written.
        doTriggers(tl, 1);
        assertEquals(4, tl.getRecordCount());
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), tl.getRecord(0));
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), tl.getRecord(1));
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), tl.getRecord(2));
        assertEquals(new LogRecord(now, new LogStatus(true, false, false), null), tl.getRecord(3));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.totalRecordCount));
        assertTrue(tl.isLogDisabled());

        // Add more records. The log should not change. Advance the time just to be sure.
        clock.plusMinutes(1);
        doTriggers(tl, 2);
        assertEquals(4, tl.getRecordCount());
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), tl.getRecord(0));
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), tl.getRecord(1));
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), tl.getRecord(2));
        assertEquals(new LogRecord(now, new LogStatus(true, false, false), null), tl.getRecord(3));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.totalRecordCount));
        assertTrue(tl.isLogDisabled());

        final DateTime now2 = new DateTime(clock.millis());

        // Set StopWhenFull to false and write a couple records.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.stopWhenFull, Boolean.FALSE));
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.enable, Boolean.TRUE));
        doTriggers(tl, 2);
        assertEquals(4, tl.getRecordCount());
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), tl.getRecord(0));
        assertEquals(new LogRecord(now, new LogStatus(true, false, false), null), tl.getRecord(1));
        assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), tl.getRecord(2));
        assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), tl.getRecord(3));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(6), tl.get(PropertyIdentifier.totalRecordCount));
        assertFalse(tl.isLogDisabled());

        // Set StopWhenFull back to true.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.stopWhenFull, Boolean.TRUE));
        assertEquals(4, tl.getRecordCount());
        assertEquals(new LogRecord(now, new LogStatus(true, false, false), null), tl.getRecord(0));
        assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), tl.getRecord(1));
        assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), tl.getRecord(2));
        assertEquals(new LogRecord(now2, new LogStatus(true, false, false), null), tl.getRecord(3));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(7), tl.get(PropertyIdentifier.totalRecordCount));
        assertTrue(tl.isLogDisabled());
    }

    @Test
    public void enableDisable() throws Exception {
        // Create a disabled triggered trend log
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<>(), false, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true, 4);
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

        // Create a triggered trend log
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<>(), true, startTime, stopTime,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true, 7);
        assertTrue(tl.isLogDisabled());
        assertEquals(0, tl.getRecordCount());

        final StatusFlags sf = new StatusFlags(false, false, false, false);

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
        quiesce();
        final DateTime now2 = new DateTime(clock.millis());
        assertFalse(tl.isLogDisabled());
        doTriggers(tl, 2);
        assertEquals(2, tl.getRecordCount());

        // Advance the time past the stop time and do some triggers.
        clock.plusMinutes(5);
        final DateTime now3 = new DateTime(clock.millis());
        awaitEquals(3, tl::getRecordCount);
        assertTrue(tl.isLogDisabled());
        doTriggers(tl, 2);
        assertEquals(3, tl.getRecordCount());
        assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), tl.getRecord(0));
        assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), tl.getRecord(1));
        assertEquals(new LogRecord(now3, new LogStatus(true, false, false), null), tl.getRecord(2));

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
        clock.plusMinutes(6);
        awaitTrue(tl::isLogDisabled);
        awaitEquals(6, tl::getRecordCount);
        doTriggers(tl, 2);
        assertEquals(6, tl.getRecordCount());
    }

    @Test
    public void readLogBuffer() throws Exception {
        // Create a triggered trend log
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true, 7);

        // Try to do a network read of the buffer. It should not be readable.
        assertBACnetServiceException(() -> tl.readProperty(PropertyIdentifier.logBuffer, null), ErrorClass.property,
                ErrorCode.readAccessDenied);
    }

    @Test
    public void purge() throws Exception {
        final DateTime now = new DateTime(clock.millis());

        // Create a triggered trend log
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true, 7);

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
        assertEquals(new LogRecord(now, new LogStatus(false, true, false), null), tl.getRecord(0));
    }

    @Test
    public void writePropertyReference() throws Exception {
        final DateTime now = new DateTime(clock.millis());
        final StatusFlags sf = new StatusFlags(false, false, false, false);

        // Create a triggered trend log
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true,
                        100);

        ao.writePropertyInternal(PropertyIdentifier.presentValue, new Real(13));
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(14));

        doTriggers(tl, 2);
        assertEquals(2, tl.getRecordCount());
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(14), sf), tl.getRecord(0));
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(14), sf), tl.getRecord(1));

        // Modify the property reference as a network write.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.logDeviceObjectProperty,
                new DeviceObjectPropertyReference(1, ao.getId(), PropertyIdentifier.presentValue)));

        doTriggers(tl, 2);
        assertEquals(3, tl.getRecordCount());
        assertEquals(new LogRecord(now, new LogStatus(false, true, false), null), tl.getRecord(0));
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(13), sf), tl.getRecord(1));
        assertEquals(LogRecord.createFromMonitoredValue(now, new Real(13), sf), tl.getRecord(2));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fault() throws Exception {
        final RemoteDevice rd2 = d1.getRemoteDeviceBlocking(2);

        // Add d2 as an event recipient.
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(27), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        LOG.debug("start");
        // Create a COV trend log that references a device that doesn't exist.
        final TrendLogObject tl =
                new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED,
                        new DeviceObjectPropertyReference(3, ai.getId(), PropertyIdentifier.presentValue), 0, false,
                        20).supportIntrinsicReporting(20, 23, new EventTransitionBits(true, true, true),
                        NotifyType.event).withCov(100, new ClientCov(Null.instance));

        // Wait for the notification.
        awaitEquals(1, listener::getNotifCount);

        // Validate notification
        final EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(27), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(tl.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) tl.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(23), notif.notificationClass());
        assertEquals(new UnsignedInteger(2), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.TRUE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfReliabilityNotif(Reliability.configurationError, new StatusFlags(true, true, false, false),
                        new SequenceOf<>())), notif.eventValues());
    }
}
