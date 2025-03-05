package com.serotonin.bacnet4j.obj;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.TestUtils;
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
import com.serotonin.warp.TestingWarpScheduledExecutorService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

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
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false, 20)
                        .withPolled(1, TimeUnit.MINUTES, true, 2, TimeUnit.SECONDS);
        polling(tl, ai);
    }

    @Test
    public void localPolling() throws Exception {
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(1, ao.getId(), PropertyIdentifier.presentValue), 0, false, 20)
                        .withPolled(1, TimeUnit.MINUTES, true, 2, TimeUnit.SECONDS);
        polling(tl, ao);
    }

    private void polling(final TrendLogObject tl, final BACnetObject bo) throws Exception {
        tl.withBuffer(buffer -> {
            assertEquals(0, buffer.size());
        });

        //
        // Advance the clock to the polling time.
        LOG.info("Starting time: {}", clock.instant());
        final int seconds = (62 - clock.get(ChronoField.SECOND_OF_MINUTE) - 1) % 60 + 1;
        clock.plus(seconds, SECONDS, 300);
        LOG.info("First poll time: {}", clock.instant());

        AtomicReference<LogRecord> record1Ref = new AtomicReference<>();
        tl.withBuffer(buffer -> {
            assertEquals(1, buffer.size());
            final LogRecord record1 = buffer.get(0);
            record1Ref.set(record1);
            // We asked for alignment and an offset of 2 seconds.
            assertEquals(2.0, record1.getTimestamp().getTime().getSecond(), 1.0);
            assertEquals(new Real(0), record1.getChoice());
            assertEquals(new StatusFlags(false, false, false, false), record1.getStatusFlags());
        });

        //
        // Update the object present value.
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(2));

        // Advance the clock another minute to poll again.
        clock.plus(1, MINUTES, 0);
        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 2, 500);

        tl.withBuffer(buffer -> {
            final LogRecord record2 = buffer.get(1);
            assertEquals(2, record2.getTimestamp().getTime().getSecond());
            assertEquals((record1Ref.get().getTimestamp().getTime().getMinute() + 1) % 60,
                    record2.getTimestamp().getTime().getMinute());
            assertEquals(new Real(2), record2.getChoice());
            assertEquals(new StatusFlags(false, false, false, false), record2.getStatusFlags());
        });

        //
        // Update the object overridden.
        bo.setOverridden(true);

        // Advance the clock another minute to poll again.
        clock.plus(1, MINUTES, 0);

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 3, 500);
        tl.withBuffer(buffer -> {
            final LogRecord record3 = buffer.get(2);
            assertEquals(2, record3.getTimestamp().getTime().getSecond());
            assertEquals((record1Ref.get().getTimestamp().getTime().getMinute() + 2) % 60,
                    record3.getTimestamp().getTime().getMinute());
            assertEquals(new Real(2), record3.getChoice());
            assertEquals(new StatusFlags(false, false, true, false), record3.getStatusFlags());
        });

        //
        // Update the log interval to 1 hour.
        tl.writeProperty(null, PropertyIdentifier.logInterval, new UnsignedInteger(60 * 60 * 100));
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(3));

        // Advance the clock to the new polling time.
        final int minutes = (62 - clock.get(ChronoField.MINUTE_OF_HOUR)) % 60;
        clock.plus(minutes, MINUTES, 0);

        //Wait for the tasks
        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, true);
        TestUtils.assertSize(tl::size, 4, 500);
        tl.withBuffer(buffer -> {
            final LogRecord record4 = buffer.get(3);
            assertEquals(2, record4.getTimestamp().getTime().getMinute());
            assertEquals(new Real(3), record4.getChoice());
            assertEquals(new StatusFlags(false, false, true, false), record4.getStatusFlags());
        });

        //
        // Try a trigger for fun.
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(4));
        bo.setOverridden(false);
        tl.trigger();

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        // Wait for the polling to finish.
        TestUtils.assertSize(tl::size, 5, 500);
        tl.withBuffer(buffer -> {
            final LogRecord record5 = buffer.get(4);
            assertEquals(new Real(4), record5.getChoice());
            assertEquals(new StatusFlags(false, false, false, false), record5.getStatusFlags());
        });

    }

    @Test
    public void remoteCov() throws Exception {
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false, 20)
                        .withCov(100, new ClientCov(Null.instance));
        cov(tl, d2, ai);
    }

    @Test
    public void localCov() throws Exception {
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(1, ao.getId(), PropertyIdentifier.presentValue), 0, false, 20)
                        .withCov(100, new ClientCov(new Real(0.3F)));
        cov(tl, d1, ao);
    }

    private void cov(final TrendLogObject tl, final LocalDevice d, final BACnetObject bo) throws Exception {
        DateTime now = new DateTime(clock.millis());
        AtomicReference<DateTime> nowRef = new AtomicReference<>(now);
        // Wait for the COV to set up, and for the initial notification to be sent.
        Thread.sleep(300);

        // Make sure the subscription is there.
        SequenceOf<CovSubscription> subscriptions = d.getObject(d.getId())
                .readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(1, subscriptions.getCount());

        // Remember the process id.
        final int processId = subscriptions.getBase1(1).getRecipient().getProcessIdentifier().intValue();

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        // The initial notification should be there.
        TestUtils.assertSize(tl::size, 1, 500);
        tl.withBuffer(buffer -> {
            final LogRecord record1 = buffer.get(0);
            assertEquals(nowRef.get(), record1.getTimestamp());
            assertEquals(new Real(0), record1.getChoice());
            assertEquals(new StatusFlags(false, false, false, false), record1.getStatusFlags());
        });

        //
        // Update the value to cause a COV notification.
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(1));

        //Wait for the messages
        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 2, 500);
        tl.withBuffer(buffer -> {
            final LogRecord record2 = buffer.get(1);
            assertEquals(nowRef.get(), record2.getTimestamp());
            assertEquals(new Real(1), record2.getChoice());
            assertEquals(new StatusFlags(false, false, false, false), record2.getStatusFlags());
        });

        //
        // Advance the clock a bit and update again. No notification will be received because the update was below the
        // update threshold.
        clock.plusSeconds(45);
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(1.2F));

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 2, 500);

        //
        // Advance the clock a bit and update again. No notification will be received because the update was below the
        // update threshold.
        clock.plusSeconds(25);
        now = new DateTime(clock.millis());
        nowRef.set(now);
        bo.writePropertyInternal(PropertyIdentifier.presentValue, new Real(1.6F));

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 3, 500);
        tl.withBuffer(buffer -> {
            final LogRecord record3 = buffer.get(2);
            assertEquals(nowRef.get(), record3.getTimestamp());
            assertEquals(new Real(1.6F), record3.getChoice());
            assertEquals(new StatusFlags(false, false, false, false), record3.getStatusFlags());
        });

        //
        // Advance the clock a bit and override.
        clock.plusSeconds(12);
        now = new DateTime(clock.millis());
        nowRef.set(now);
        bo.setOverridden(true);

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 4, 500);
        tl.withBuffer(buffer -> {
            final LogRecord record4 = buffer.get(3);
            assertEquals(nowRef.get(), record4.getTimestamp());
            assertEquals(new Real(1.6F), record4.getChoice());
            assertEquals(new StatusFlags(false, false, true, false), record4.getStatusFlags());
        });

        //
        // Advance the clock past the resubscription period. Another notification should be received.
        clock.plusSeconds(20);
        now = new DateTime(clock.millis());
        nowRef.set(now);

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 5, 500);
        tl.withBuffer(buffer -> {
            final LogRecord record5 = buffer.get(4);
            assertEquals(nowRef.get(), record5.getTimestamp());
            assertEquals(new Real(1.6F), record5.getChoice());
            assertEquals(new StatusFlags(false, false, true, false), record5.getStatusFlags());
        });

        //
        // Change the resubscription interval
        tl.writeProperty(null, PropertyIdentifier.covResubscriptionInterval, new UnsignedInteger(300));
        Thread.sleep(50);

        // Check that there is still only one subscription, and that it has a different process id.
        subscriptions = d.getObject(d.getId()).readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(1, subscriptions.getValues().size());
        final int processId2 = subscriptions.getBase1(1).getRecipient().getProcessIdentifier().intValue();
        assertEquals(processId + 1, processId2);

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        // Check that an update was sent due to the resubscription.
        TestUtils.assertSize(tl::size, 6, 500);

        //
        // Advance the clock past the new resubscription period. Only one more notification should be received.
        clock.plusSeconds(310);
        now = new DateTime(clock.millis());
        nowRef.set(now);

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 7, 500);

        //
        // Try a trigger for fun.
        tl.trigger();

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        // Wait for the polling to finish.
        TestUtils.assertSize(tl::size, 8, 500);
    }

    @Test
    public void trigger() throws Exception {
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false, 20)
                        .withTriggered();

        final DateTime now = new DateTime(clock.millis());

        // The buffer should still be empty
        assertEquals(0, tl.size());

        // Update the monitored value
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(2));

        // The buffer should still be empty
        assertEquals(0, tl.size());

        // Update the monitored value again
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(3));

        // The buffer should still be empty
        assertEquals(0, tl.size());

        // Trigger an update
        LOG.info("Trigger");
        tl.trigger();

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        //Wait for the log to show up
        TestUtils.assertSize(tl::size, 1, 500);

        // The log record should be there.
        tl.withBuffer(buffer -> {
            assertEquals(1, buffer.size());
            final LogRecord record1 = buffer.get(0);
            assertEquals(now, record1.getTimestamp());
            assertEquals(new Real(3), record1.getChoice());
            assertEquals(new StatusFlags(false, false, false, false), record1.getStatusFlags());
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        // Create a triggered trend log with intrinsic reporting enabled.
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl0", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false, 20)
                        .withTriggered()
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
        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 4, 500);

        assertEquals(0, listener.size());
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(UnsignedInteger.ZERO, tl.get(PropertyIdentifier.lastNotifyRecord));

        //
        // Write one more and make sure a notification was received.
        doTriggers(tl, 1);
        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 5, 500);
        TestUtils.assertSize(listener::size, 1, 500);

        Map<String, Object> notif = listener.poll();
        assertEquals(new UnsignedInteger(27), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(tl.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) tl.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(23), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(3), notif.get("priority"));
        assertEquals(EventType.bufferReady, notif.get("eventType"));
        assertEquals(null, notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(new NotificationParameters(
                        new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                                UnsignedInteger.ZERO, new UnsignedInteger(5))),
                notif.get("eventValues"));


        // Validate the internally maintained values.
        assertEquals(new UnsignedInteger(5), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(5), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(UnsignedInteger.ZERO, tl.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(new UnsignedInteger(5), tl.get(PropertyIdentifier.lastNotifyRecord));

        //
        // Write another 5 triggers and ensure that the notification looks ok.
        doTriggers(tl, 5);
        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(() -> {
            AtomicInteger size = new AtomicInteger(0);
            tl.withBuffer(buffer -> size.set(buffer.size()));
            return size.get();
        }, 10, 500);
        TestUtils.assertSize(listener::size, 1, 500);
        assertEquals(10, tl.size());
        assertEquals(1, listener.size());

        notif = listener.poll();
        assertEquals(new UnsignedInteger(27), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(tl.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) tl.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(23), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(3), notif.get("priority"));
        assertEquals(EventType.bufferReady, notif.get("eventType"));
        assertEquals(null, notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(new NotificationParameters(
                        new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                                new UnsignedInteger(5), new UnsignedInteger(10))),
                notif.get("eventValues"));

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

        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(tl::size, 15, 500);
        TestUtils.assertSize(listener::size, 1, 500);
        assertEquals(1, listener.size());


        notif = listener.poll();
        assertEquals(new UnsignedInteger(27), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(tl.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) tl.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(23), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(3), notif.get("priority"));
        assertEquals(EventType.bufferReady, notif.get("eventType"));
        assertEquals(null, notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(new NotificationParameters(
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                        new UnsignedInteger(0xFFFFFFFDL), new UnsignedInteger(3))),
                notif.get("eventValues"));

        // Validate the internally maintained values.
        assertEquals(new UnsignedInteger(15), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(3), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(UnsignedInteger.ZERO, tl.get(PropertyIdentifier.recordsSinceNotification));
        assertEquals(new UnsignedInteger(3), tl.get(PropertyIdentifier.lastNotifyRecord));
    }

    private static void doTriggers(final TrendLogObject tl, final int count) throws InterruptedException {
        int remaining = count;
        while (remaining > 0) {
            if (tl.trigger())
                remaining--;
            Thread.sleep(10);
        }
        Thread.sleep(10);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void eventReporting() throws Exception {
        // Create a triggered trend log
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, false, 20)
                        .withTriggered();

        // Create the event enrollment.
        final DeviceObjectPropertyReference ref = new DeviceObjectPropertyReference(tl.getId(),
                PropertyIdentifier.totalRecordCount, null, d1.getId());
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
        Thread.sleep(300);

        // Ensure that there are no notifications.
        assertEquals(0, listener.size());

        // Trigger another notification so that a notification is sent.
        doTriggers(tl, 1);
        clock.plusSeconds(1);
        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(listener::size, 1, 500);
        Map<String, Object> notif = listener.poll();
        assertEquals(new UnsignedInteger(28), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(ee.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(23), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(3), notif.get("priority"));
        assertEquals(EventType.bufferReady, notif.get("eventType"));
        assertEquals(null, notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(new NotificationParameters(
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                        UnsignedInteger.ZERO, new UnsignedInteger(3))),
                notif.get("eventValues"));

        // Trigger another batch of updates. One notification should be sent.
        doTriggers(tl, 7);
        clock.plusSeconds(1);
        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(listener::size, 1, 500);
        notif = listener.poll();
        assertEquals(new UnsignedInteger(28), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(ee.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(23), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(3), notif.get("priority"));
        assertEquals(EventType.bufferReady, notif.get("eventType"));
        assertEquals(null, notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(new NotificationParameters(
                new BufferReadyNotif(new DeviceObjectPropertyReference(1, tl.getId(), PropertyIdentifier.logBuffer),
                        new UnsignedInteger(3), new UnsignedInteger(10))),
                notif.get("eventValues"));
    }

    @Test
    public void stopWhenFull() throws Exception {
        // Create a triggered trend log
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true, 4)
                        .withTriggered();

        final DateTime now = new DateTime(clock.millis());
        final StatusFlags sf = new StatusFlags(false, false, false, false);

        // Add a couple records and validate the buffer content
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(2, buffer.size());
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), buffer.get(0));
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), buffer.get(1));
        });

        assertEquals(new UnsignedInteger(2), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(2), tl.get(PropertyIdentifier.totalRecordCount));

        // Add another record. This will cause the buffer to be full after the buffer full notification is written.
        doTriggers(tl, 1);
        tl.withBuffer(buffer -> {
            assertEquals(4, buffer.size());
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), buffer.get(0));
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), buffer.get(1));
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), buffer.get(2));
            assertEquals(new LogRecord(now, new LogStatus(true, false, false), null), buffer.get(3));
        });

        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(true, tl.isLogDisabled());

        // Add more records. The log should not change. Advance the time just to be sure.
        clock.plusMinutes(1);
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(4, buffer.size());
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), buffer.get(0));
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), buffer.get(1));
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), buffer.get(2));
            assertEquals(new LogRecord(now, new LogStatus(true, false, false), null), buffer.get(3));
        });

        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(true, tl.isLogDisabled());

        final DateTime now2 = new DateTime(clock.millis());

        // Set StopWhenFull to false and write a couple records.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.stopWhenFull, Boolean.FALSE));
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.enable, Boolean.TRUE));
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(4, buffer.size());
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(0), sf), buffer.get(0));
            assertEquals(new LogRecord(now, new LogStatus(true, false, false), null), buffer.get(1));
            assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), buffer.get(2));
            assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), buffer.get(3));
        });

        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(6), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(false, tl.isLogDisabled());

        // Set StopWhenFull back to true.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.stopWhenFull, Boolean.TRUE));
        tl.withBuffer(buffer -> {
            assertEquals(4, buffer.size());
            assertEquals(new LogRecord(now, new LogStatus(true, false, false), null), buffer.get(0));
            assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), buffer.get(1));
            assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), buffer.get(2));
            assertEquals(new LogRecord(now2, new LogStatus(true, false, false), null), buffer.get(3));
        });
        assertEquals(new UnsignedInteger(4), tl.get(PropertyIdentifier.recordCount));
        assertEquals(new UnsignedInteger(7), tl.get(PropertyIdentifier.totalRecordCount));
        assertEquals(true, tl.isLogDisabled());
    }

    @Test
    public void enableDisable() throws Exception {
        // Create a disabled triggered trend log
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<LogRecord>(), false,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true, 4);
        tl.withBuffer(buffer -> {
            assertEquals(0, buffer.size());
        });
        // Add a couple records and validate the buffer content
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(0, buffer.size());
        });
        // Enable and write a few records.
        tl.setEnabled(false);
        tl.withBuffer(buffer -> {
            assertEquals(0, buffer.size());
        });
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(0, buffer.size());
        });
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
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<LogRecord>(), true, startTime,
                stopTime, new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true,
                7);
        assertEquals(true, tl.isLogDisabled());
        tl.withBuffer(buffer -> {
            assertEquals(0, buffer.size());
        });

        final StatusFlags sf = new StatusFlags(false, false, false, false);

        // Do some triggers.
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(0, buffer.size());
        });

        // Advance the time a bit and do some triggers.
        clock.plus(3, TimeUnit.MINUTES, 40);
        assertEquals(true, tl.isLogDisabled());
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(0, buffer.size());
        });

        // Advance the time past the start time and do some triggers.
        clock.plus(3, TimeUnit.MINUTES, 40);
        final DateTime now2 = new DateTime(clock.millis());
        assertEquals(false, tl.isLogDisabled());
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(2, buffer.size());
        });

        // Advance the time past the stop time and do some triggers.
        clock.plus(5, TimeUnit.MINUTES, 40);
        final DateTime now3 = new DateTime(clock.millis());
        assertEquals(true, tl.isLogDisabled());
        tl.withBuffer(buffer -> {
            assertEquals(3, buffer.size());
        });
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(3, buffer.size());
            assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), buffer.get(0));
            assertEquals(LogRecord.createFromMonitoredValue(now2, new Real(0), sf), buffer.get(1));
            assertEquals(new LogRecord(now3, new LogStatus(true, false, false), null), buffer.get(2));
        });

        // Reset the start and stop times.
        nowgg = now3.getGC();
        nowgg.add(Calendar.MINUTE, 5);
        startTime = new DateTime(nowgg);
        nowgg.add(Calendar.MINUTE, 5);
        stopTime = new DateTime(nowgg);
        tl.writeProperty(null, PropertyIdentifier.startTime, startTime);
        tl.writeProperty(null, PropertyIdentifier.stopTime, stopTime);

        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(3, buffer.size());
        });

        // Advance the time past the start time and do some triggers.
        clock.plus(6, TimeUnit.MINUTES, 40);
        assertEquals(false, tl.isLogDisabled());
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(5, buffer.size());
        });

        // Advance the time past the stop time and do some triggers.
        clock.plus(6, TimeUnit.MINUTES, 40);
        assertEquals(true, tl.isLogDisabled());
        tl.withBuffer(buffer -> {
            assertEquals(6, buffer.size());
        });
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(6, buffer.size());
        });
    }

    @Test
    public void readLogBuffer() throws Exception {
        // Create a triggered trend log
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true, 7);

        // Try to do a network read of the buffer. It should not be readable.
        TestUtils.assertBACnetServiceException(() -> {
            tl.readProperty(PropertyIdentifier.logBuffer, null);
        }, ErrorClass.property, ErrorCode.readAccessDenied);
    }

    @Test
    public void purge() throws Exception {
        final DateTime now = new DateTime(clock.millis());

        // Create a triggered trend log
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true, 7);

        // Trigger a few updates.
        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(2, buffer.size());
        });

        // Set the record count to non-zero.
        TestUtils.assertBACnetServiceException(() -> {
            tl.writeProperty(null, new PropertyValue(PropertyIdentifier.recordCount, new UnsignedInteger(1)));
        }, ErrorClass.property, ErrorCode.writeAccessDenied);

        // Set the record count to zero. There should be one log status record.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.recordCount, UnsignedInteger.ZERO));
        tl.withBuffer(buffer -> {
            assertEquals(1, buffer.size());
            assertEquals(new LogRecord(now, new LogStatus(false, true, false), null), buffer.get(0));
        });

    }

    @Test
    public void writePropertyReference() throws Exception {
        final DateTime now = new DateTime(clock.millis());
        final StatusFlags sf = new StatusFlags(false, false, false, false);

        // Create a triggered trend log
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(2, ai.getId(), PropertyIdentifier.presentValue), 0, true, 100);

        ao.writePropertyInternal(PropertyIdentifier.presentValue, new Real(13));
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(14));

        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(2, buffer.size());
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(14), sf), buffer.get(0));
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(14), sf), buffer.get(1));
        });

        // Modify the property reference as a network write.
        tl.writeProperty(null, new PropertyValue(PropertyIdentifier.logDeviceObjectProperty,
                new DeviceObjectPropertyReference(1, ao.getId(), PropertyIdentifier.presentValue)));

        doTriggers(tl, 2);
        tl.withBuffer(buffer -> {
            assertEquals(3, buffer.size());
            assertEquals(new LogRecord(now, new LogStatus(false, true, false), null), buffer.get(0));
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(13), sf), buffer.get(1));
            assertEquals(LogRecord.createFromMonitoredValue(now, new Real(13), sf), buffer.get(2));
        });
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
        final TrendLogObject tl = new TrendLogObject(d1, 0, "tl", new LinkedListLogBuffer<LogRecord>(), true,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(3, ai.getId(), PropertyIdentifier.presentValue), 0, false, 20)
                        .supportIntrinsicReporting(20, 23, new EventTransitionBits(true, true, true), NotifyType.event)
                        .withCov(100, new ClientCov(Null.instance));

        // Wait for the notification.
        ((TestingWarpScheduledExecutorService)this.executor).waitForExecutorTasks(10, TimeUnit.MILLISECONDS, 50, true, true, false);
        TestUtils.assertSize(listener::size, 1, 500);

        // Validate notification
        final Map<String, Object> notif = listener.poll();
        assertEquals(new UnsignedInteger(27), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(tl.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) tl.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(23), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(2), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertEquals(null, notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        assertEquals(new NotificationParameters(new ChangeOfReliabilityNotif(Reliability.configurationError,
                new StatusFlags(true, true, false, false), new SequenceOf<>())), notif.get("eventValues"));
    }
}
