package com.serotonin.bacnet4j.obj;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.service.confirmed.AddListElementRequest;
import com.serotonin.bacnet4j.service.confirmed.RemoveListElementRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyMultipleRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.CalendarEntry;
import com.serotonin.bacnet4j.type.constructed.DailySchedule;
import com.serotonin.bacnet4j.type.constructed.DateRange;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.SpecialEvent;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.TimeValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.constructed.WeekNDay;
import com.serotonin.bacnet4j.type.constructed.WeekNDay.WeekOfMonth;
import com.serotonin.bacnet4j.type.constructed.WriteAccessSpecification;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.RequestUtils;
import com.serotonin.warp.WarpClock;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ScheduleObjectTest extends AbstractTest {
    static {
        //-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
        System.setProperty("org.slf4j.simpleLogger.log.com.serotonin.bacnet4j.obj.ScheduleObject", "debug");
        System.setProperty("org.slf4j.simpleLogger.log.com.serotonin.bacnet4j.obj.ScheduleObjectTest", "debug");
    }
    static final Logger LOG = LoggerFactory.getLogger(ScheduleObjectTest.class);

    private volatile Encodable lastScheduleValue;

    @Override
    public WarpClock getClock() {
        //Because our schedules are based on our local timezone we don't want to use the UTC Clock for this
        return new WarpClock(ZoneId.systemDefault());
    }

    @Test
    public void fullTest() throws Exception {
        // Not really a full test. The effective period could be better.
        clock.set(2115, java.time.Month.MAY, 1, 12, 0, 0);

        final AnalogValueObject av0 = new AnalogValueObject(d2, 0, "av0", 98, EngineeringUnits.amperes, false)
                .supportCommandable(-2);
        final ObjectWriteListenerMixin av0Mixin = new ObjectWriteListenerMixin(av0);
        av0.addMixin(0, av0Mixin);

        final AnalogValueObject av1 = new AnalogValueObject(d1, 1, "av1", 99, EngineeringUnits.amperesPerMeter, false)
                .supportCommandable(-1);
        final ObjectWriteListenerMixin av1Mixin = new ObjectWriteListenerMixin(av1);
        av1.addMixin(0, av1Mixin);

        final Primitive defaultScheduledValue = new Real(999);
        final ScheduleObject so = createScheduleObject(av0, av1 , defaultScheduledValue);
        final ObjectWriteListenerMixin soMixin = new ObjectWriteListenerMixin(so);
        so.addMixin(0, soMixin);

        this.lastScheduleValue = new Real(14);
        TestUtils.assertCondition(() -> lastScheduleValue.equals(so.get(PropertyIdentifier.presentValue)), true, 1000);
        TestUtils.assertCondition(() -> lastScheduleValue.equals(av0.get(PropertyIdentifier.presentValue)), true, 1000);
        TestUtils.assertCondition(() -> lastScheduleValue.equals(av1.get(PropertyIdentifier.presentValue)), true, 1000);

        // Start actual tests.
        testTime(so, av0, av1, java.time.Month.MAY, 1, 17, 0, new Real(15), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Wednesday
        testTime(so, av0, av1, java.time.Month.MAY, 2, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Thursday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 2, 9, 0, new Real(16), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Thursday
        testTime(so, av0, av1, java.time.Month.MAY, 2, 20, 0, new Real(17), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Thursday
        testTime(so, av0, av1, java.time.Month.MAY, 3, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Friday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 3, 13, 0,  new Real(22), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Exception schedule at 13:00 with priority 7
        testTime(so, av0, av1, java.time.Month.MAY, 3, 14, 0,  new Real(23), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Exception schedule at 14:00 with priority 7
        testTime(so, av0, av1, java.time.Month.MAY, 4, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Saturday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 5, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Sunday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 6, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Monday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 6, 8, 0,  new Real(10), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Monday
        testTime(so, av0, av1, java.time.Month.MAY, 6, 17, 0,  new Real(11), defaultScheduledValue, soMixin, av0Mixin, av1Mixin);  // Monday
        testTime(so, av0, av1, java.time.Month.MAY, 7, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Tuesday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 7, 8, 0,  new Real(12), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Tuesday
        testTime(so, av0, av1, java.time.Month.MAY, 7, 17, 0, new Null(), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Null schedule from weekly schedule
        testTime(so, av0, av1, java.time.Month.MAY, 8, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Wednesday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 8, 10, 30,  new Real(24), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Exception schedule at 10:30 with priority 6
        testTime(so, av0, av1, java.time.Month.MAY, 8, 17, 0, new Null(), defaultScheduledValue, soMixin, av0Mixin, av1Mixin);  // Null schedule from exception schedule
        testTime(so, av0, av1, java.time.Month.MAY, 9, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Thursday beginning of the day
    }

    @Test
    public void nullScheduleDefaultTest() throws Exception {
        clock.set(2115, java.time.Month.MAY, 1, 12, 0, 0);

        final AnalogValueObject av0 = new AnalogValueObject(d2, 0, "av0", 98, EngineeringUnits.amperes, false)
                .supportCommandable(-2);
        final ObjectWriteListenerMixin av0Mixin = new ObjectWriteListenerMixin(av0);
        av0.addMixin(0, av0Mixin);

        final AnalogValueObject av1 = new AnalogValueObject(d1, 1, "av1", 99, EngineeringUnits.amperesPerMeter, false)
                .supportCommandable(-1);
        final ObjectWriteListenerMixin av1Mixin = new ObjectWriteListenerMixin(av1);
        av1.addMixin(0, av1Mixin);

        final Primitive defaultScheduledValue = new Null();
        final ScheduleObject so = createScheduleObject(av0, av1, defaultScheduledValue);
        final ObjectWriteListenerMixin soMixin = new ObjectWriteListenerMixin(so);
        so.addMixin(0, soMixin);

        this.lastScheduleValue = new Real(14);
        Assert.assertEquals(this.lastScheduleValue, so.get(PropertyIdentifier.presentValue));
        Assert.assertEquals(this.lastScheduleValue, av0.get(PropertyIdentifier.presentValue));
        Assert.assertEquals(this.lastScheduleValue, av1.get(PropertyIdentifier.presentValue));

        // Start actual tests.
        testTime(so, av0, av1, java.time.Month.MAY, 1, 17, 0, new Real(15), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Wednesday
        testTime(so, av0, av1, java.time.Month.MAY, 2, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Thursday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 2, 9, 0, new Real(16), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Thursday
        testTime(so, av0, av1, java.time.Month.MAY, 2, 20, 0, new Real(17), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Thursday
        testTime(so, av0, av1, java.time.Month.MAY, 3, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Friday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 3, 13, 0,  new Real(22), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Exception schedule at 13:00 with priority 7
        testTime(so, av0, av1, java.time.Month.MAY, 3, 14, 0,  new Real(23), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Exception schedule at 14:00 with priority 7
        testTime(so, av0, av1, java.time.Month.MAY, 4, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Saturday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 5, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Sunday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 6, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Monday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 6, 8, 0,  new Real(10), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Monday
        testTime(so, av0, av1, java.time.Month.MAY, 6, 17, 0,  new Real(11), defaultScheduledValue, soMixin, av0Mixin, av1Mixin);  // Monday
        testTime(so, av0, av1, java.time.Month.MAY, 7, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Tuesday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 7, 8, 0,  new Real(12), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Tuesday
        testTime(so, av0, av1, java.time.Month.MAY, 7, 17, 0, new Null(), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Null schedule from weekly schedule
        testTime(so, av0, av1, java.time.Month.MAY, 8, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Wednesday beginning of the day
        testTime(so, av0, av1, java.time.Month.MAY, 8, 10, 30,  new Real(24), defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Exception schedule at 10:30 with priority 6
        testTime(so, av0, av1, java.time.Month.MAY, 8, 17, 0, new Null(), defaultScheduledValue, soMixin, av0Mixin, av1Mixin);  // Null schedule from exception schedule
        testTime(so, av0, av1, java.time.Month.MAY, 9, 0, 0, defaultScheduledValue, defaultScheduledValue, soMixin, av0Mixin, av1Mixin); // Thursday beginning of the day
    }

    private void testTime(final ScheduleObject so, final AnalogValueObject av0, final AnalogValueObject av1,
            final java.time.Month month, final int day, final int hour, final int min, final Primitive scheduledValue, Encodable defaultScheduleValue,
                          ObjectWriteListenerMixin soMixin, ObjectWriteListenerMixin av0Mixin, ObjectWriteListenerMixin av1Mixin)
            throws Exception {

        CompletableFuture<Boolean> scheduleReady = new CompletableFuture<>();
        CompletableFuture<Boolean> av0Ready = new CompletableFuture<>();
        CompletableFuture<Boolean> av1Ready = new CompletableFuture<>();

        ScheduleObject.ScheduleListener scheduleObjectListener = (dateTime, value) -> {
            LOG.debug("SO Change at {} to {}", dateTime.getGC().getTime(), value);
            scheduleReady.complete(Boolean.TRUE);
        };
        so.addScheduleListener(scheduleObjectListener);

        BACnetObjectWriteListener scheduleListener = (pid, propertyValue) -> {
            if(pid.equals(PropertyIdentifier.presentValue) && propertyValue.equals(scheduledValue)) {
                scheduleReady.complete(Boolean.TRUE);
                LOG.debug("SO SET {} {}", pid, propertyValue);
                //For when Null is set to the schedule as we know this won't propagate to the objects
                if(propertyValue.equals(new Null())) {
                    av0Ready.complete(Boolean.TRUE);
                    av1Ready.complete(Boolean.TRUE);
                }
            }
        };
        BACnetObjectWriteListener av0Listener = (pid, propertyValue) -> {
            //In this case the schedule will send out the default value
            if(pid.equals(PropertyIdentifier.presentValue) && propertyValue.equals(scheduledValue)) {
                av0Ready.complete(Boolean.TRUE);
                LOG.debug("AV0 SET {}", propertyValue);
            }else if(pid.equals(PropertyIdentifier.presentValue) && scheduledValue.equals(new Null()) && propertyValue.equals(defaultScheduleValue)) {
                av0Ready.complete(Boolean.TRUE);
                LOG.debug("AV0 SET DEFAULT {} {}", pid, propertyValue);
            }else {
                LOG.debug("AV0 {}", pid);
            }
        };
        BACnetObjectWriteListener av1Listener = (pid, propertyValue) -> {
            if(pid.equals(PropertyIdentifier.presentValue) && propertyValue.equals(scheduledValue)) {
                av1Ready.complete(Boolean.TRUE);
                LOG.debug("AV1 SET {}", propertyValue);
            }else if(pid.equals(PropertyIdentifier.presentValue) && scheduledValue.equals(new Null()) && propertyValue.equals(defaultScheduleValue)) {
                av1Ready.complete(Boolean.TRUE);
                LOG.debug("AV1 SET DEFAULT {} {}", pid, propertyValue);
            }else {
                LOG.debug("AV0 {}", pid);
            }
        };

        soMixin.setListener(scheduleListener);
        av0Mixin.setListener(av0Listener);
        av1Mixin.setListener(av1Listener);

        clock.set(2115, month, day, hour, min, 0);
        LOG.debug("Fast Forwarded Timer to {}", clock.getDateTime());

        scheduleReady.get(1, TimeUnit.SECONDS);
        //There is a potential bug where if the schedule's value doesn't change it won't doWrites to other objects
        // See ScheduleMixin.afterWriteProperty
        //There is also a potential bug where a Null schedule value isn't propagated to the other objects, but this may be per the spec
        //TODO Improve use of private "lastScheduledValue"
        if(!lastScheduleValue.equals(scheduledValue)) {
            av0Ready.get(1, TimeUnit.SECONDS);
            av1Ready.get(1, TimeUnit.SECONDS);
        }

        if(scheduledValue.equals(new Null())) {
            lastScheduleValue = defaultScheduleValue;
        }else {
            lastScheduleValue = scheduledValue;
        }

        try {
            if (scheduledValue.getClass().equals(Null.class)) {
                final Primitive scheduleDefault = so.readProperty(PropertyIdentifier.scheduleDefault);
                if (scheduleDefault.getClass().equals(Null.class)) {
                    Assert.assertEquals(new Null(), so.get(PropertyIdentifier.presentValue));
                    Assert.assertEquals(av0.readProperty(PropertyIdentifier.relinquishDefault), av0.get(PropertyIdentifier.presentValue));
                    Assert.assertEquals(av1.readProperty(PropertyIdentifier.relinquishDefault), av1.get(PropertyIdentifier.presentValue));
                } else {
                    Assert.assertEquals(scheduleDefault, so.get(PropertyIdentifier.presentValue));
                    Assert.assertEquals(scheduleDefault, av0.get(PropertyIdentifier.presentValue));
                    Assert.assertEquals(scheduleDefault, av1.get(PropertyIdentifier.presentValue));
                }
            } else {
                Assert.assertEquals(scheduledValue, so.get(PropertyIdentifier.presentValue));
                Assert.assertEquals(scheduledValue, av0.get(PropertyIdentifier.presentValue));
                Assert.assertEquals(scheduledValue, av1.get(PropertyIdentifier.presentValue));
            }
        }finally {
            so.removeScheduleListener(scheduleObjectListener);
            soMixin.setListener(null);
            av0Mixin.setListener(null);
            av1Mixin.setListener(null);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicAlarms() throws Exception {
        final NotificationClassObject nc = new NotificationClassObject(d1, 7, "nc7", 100, 5, 200,
                new EventTransitionBits(false, false, false));
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        final AnalogValueObject av1 = new AnalogValueObject(d1, 1, "av1", 99, EngineeringUnits.amperesPerMeter, false)
                .supportCommandable(-1);

        final SequenceOf<SpecialEvent> exceptionSchedule = new SequenceOf<>( //
                new SpecialEvent(new CalendarEntry(new Date(-1, null, -1, DayOfWeek.WEDNESDAY)),
                        new SequenceOf<TimeValue>(), new UnsignedInteger(6)) // Wednesdays
        );
        final SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = new SequenceOf<>( //
                new DeviceObjectPropertyReference(av1.getId(), PropertyIdentifier.presentValue, null, null) //
        );
        final ScheduleObject so = new ScheduleObject(d1, 0, "sch0", new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED),
                null, exceptionSchedule, new Real(8), listOfObjectPropertyReferences, 12, false);
        so.supportIntrinsicReporting(7, new EventTransitionBits(true, true, true), NotifyType.alarm);

        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        assertEquals(0, listener.size());

        // Write a fault reliability value.
        so.writePropertyInternal(PropertyIdentifier.reliability, Reliability.memberFault);
        assertEquals(EventState.fault, so.readProperty(PropertyIdentifier.eventState));
        Thread.sleep(100);
        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.size());
        final Map<String, Object> notif = listener.poll();
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(so.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) so.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(7), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(5), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertEquals(null, notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new ChangeOfReliabilityNotif(Reliability.memberFault,
                        new StatusFlags(true, true, false, false), new SequenceOf<PropertyValue>())),
                notif.get("eventValues"));
    }

    /**
     * Ensures that schedule.listOfObjectPropertyReferences can be modified with WriteProperty
     */
    @Test
    public void listValues() throws Exception {
        final ScheduleObject so = new ScheduleObject(d1, 0, "sch0", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE),
                null, new SequenceOf<>(), new Real(8), new SequenceOf<>(), 12, false);

        // Add a few items to the list.
        final ObjectIdentifier oid = new ObjectIdentifier(ObjectType.analogInput, 0);

        final DeviceObjectPropertyReference local1 = new DeviceObjectPropertyReference(oid,
                PropertyIdentifier.presentValue, null, null);
        final DeviceObjectPropertyReference remote10 = new DeviceObjectPropertyReference(oid,
                PropertyIdentifier.presentValue, null, new ObjectIdentifier(ObjectType.device, 10));
        final DeviceObjectPropertyReference remote11 = new DeviceObjectPropertyReference(oid,
                PropertyIdentifier.presentValue, null, new ObjectIdentifier(ObjectType.device, 11));

        // Ensure that the list is empty.
        SequenceOf<Destination> list = RequestUtils.getProperty(d2, rd1, so.getId(),
                PropertyIdentifier.listOfObjectPropertyReferences);
        assertEquals(list, new SequenceOf<>());

        // Add a few elements.
        final AddListElementRequest aler = new AddListElementRequest(so.getId(),
                PropertyIdentifier.listOfObjectPropertyReferences, null, new SequenceOf<>(local1, remote10));
        d2.send(rd1, aler).get();
        list = RequestUtils.getProperty(d2, rd1, so.getId(), PropertyIdentifier.listOfObjectPropertyReferences);
        assertEquals(list, new SequenceOf<>(local1, remote10));

        // Write one more.
        d2.send(rd1, new AddListElementRequest(so.getId(), PropertyIdentifier.listOfObjectPropertyReferences, null,
                new SequenceOf<>(remote11))).get();
        list = RequestUtils.getProperty(d2, rd1, so.getId(), PropertyIdentifier.listOfObjectPropertyReferences);
        assertEquals(list, new SequenceOf<>(local1, remote10, remote11));

        // Remove some.
        d2.send(rd1, new RemoveListElementRequest(so.getId(), PropertyIdentifier.listOfObjectPropertyReferences, null,
                new SequenceOf<Encodable>(remote10, local1))).get();
        list = RequestUtils.getProperty(d2, rd1, so.getId(), PropertyIdentifier.listOfObjectPropertyReferences);
        assertEquals(list, new SequenceOf<>(remote11));
    }

    @Test
    public void changeDataType() throws Exception {
        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 98, EngineeringUnits.amperes, false)
                .supportCommandable(-2);
        final BinaryValueObject bv = new BinaryValueObject(d1, 0, "bv0", BinaryPV.inactive, false);

        final SequenceOf<SpecialEvent> binarySchedule = new SequenceOf<>(
                new SpecialEvent(new CalendarEntry(new Date(-1, null, -1, DayOfWeek.WEDNESDAY)),
                new SequenceOf<>(new TimeValue(new Time(d1), BinaryPV.active)), new UnsignedInteger(1))
        );
        final SequenceOf<SpecialEvent> analogSchedule = new SequenceOf<>(
                new SpecialEvent(new CalendarEntry(new Date(-1, null, -1, DayOfWeek.WEDNESDAY)),
                        new SequenceOf<>(new TimeValue(new Time(d1), new Real(2))), new UnsignedInteger(1))
        );

        final SequenceOf<DeviceObjectPropertyReference> binaryReferences = new SequenceOf<>(new DeviceObjectPropertyReference(1, bv.getId(), PropertyIdentifier.presentValue));
        final SequenceOf<DeviceObjectPropertyReference> analogReferences = new SequenceOf<>(new DeviceObjectPropertyReference(1, av.getId(), PropertyIdentifier.presentValue));

        final BinaryPV binaryDefaultValue = BinaryPV.inactive;
        final Real analogDefaultValue = new Real(1);

        final ScheduleObject so = new ScheduleObject(d1, 1, "sch0", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), null,
                binarySchedule, binaryDefaultValue, binaryReferences, 12, false);

        Assert.assertEquals(binaryDefaultValue, so.readProperty(PropertyIdentifier.scheduleDefault));
        Assert.assertEquals(binaryReferences, so.readProperty(PropertyIdentifier.listOfObjectPropertyReferences));

        final List<PropertyValue> propertyValues = new ArrayList<>();
        propertyValues.add(new PropertyValue(PropertyIdentifier.listOfObjectPropertyReferences, new SequenceOf<>())); // Reset listOfObjectPropertyReferences
        propertyValues.add(new PropertyValue(PropertyIdentifier.scheduleDefault, new Null())); // Reset defaultSchedule
        propertyValues.add(new PropertyValue(PropertyIdentifier.exceptionSchedule, new SequenceOf<>())); // Reset exceptionSchedule

        // Change the data type later
        propertyValues.add(new PropertyValue(PropertyIdentifier.exceptionSchedule, analogSchedule));
        propertyValues.add(new PropertyValue(PropertyIdentifier.scheduleDefault, analogDefaultValue));
        propertyValues.add(new PropertyValue(PropertyIdentifier.listOfObjectPropertyReferences, analogReferences));

        final List<WriteAccessSpecification> specs = new ArrayList<>();
        specs.add(new WriteAccessSpecification(new ObjectIdentifier(ObjectType.schedule, 1), new SequenceOf<>(propertyValues)));
        d2.send(rd1, new WritePropertyMultipleRequest(new SequenceOf<>(specs))).get();

        Assert.assertEquals(analogDefaultValue, so.readProperty(PropertyIdentifier.scheduleDefault));
        Assert.assertEquals(analogReferences, so.readProperty(PropertyIdentifier.listOfObjectPropertyReferences));
    }

    @Test
    public void validations() throws Exception {
        final AnalogValueObject av = new AnalogValueObject(d2, 0, "av0", 98, EngineeringUnits.amperes, false)
                .supportCommandable(-2);

        //
        // Entries in the list of property references must reference properties of this type
        TestUtils.assertBACnetServiceException(() -> {
            new ScheduleObject(d1, 0, "sch0", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), null,
                    new SequenceOf<>(), BinaryPV.inactive,
                    new SequenceOf<>(new DeviceObjectPropertyReference(1, av.getId(), PropertyIdentifier.presentValue)),
                    12, false);
        }, ErrorClass.property, ErrorCode.invalidDataType);

        //
        // Time value entries in the weekly and exception schedules must be of this type
        TestUtils.assertBACnetServiceException(() -> {
            final BACnetArray<DailySchedule> weekly = new BACnetArray<>( //
                    new DailySchedule(new SequenceOf<>(new TimeValue(new Time(d1), new Real(0)))), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()));
            new ScheduleObject(d1, 1, "sch1", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), weekly,
                    new SequenceOf<>(), BinaryPV.inactive, new SequenceOf<>(), 12, false);
        }, ErrorClass.property, ErrorCode.invalidDataType);

        TestUtils.assertBACnetServiceException(() -> {
            final SequenceOf<SpecialEvent> exceptions = new SequenceOf<>( //
                    new SpecialEvent(new CalendarEntry(new Date(d1)),
                            new SequenceOf<>(new TimeValue(new Time(d1), new Real(0))), new UnsignedInteger(10)));
            new ScheduleObject(d1, 2, "sch2", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), null, exceptions,
                    BinaryPV.inactive, new SequenceOf<>(), 12, false);
        }, ErrorClass.property, ErrorCode.invalidDataType);

        //
        // Time values must have times that are fully specific.
        TestUtils.assertBACnetServiceException(() -> {
            final BACnetArray<DailySchedule> weekly = new BACnetArray<>( //
                    new DailySchedule(new SequenceOf<>(new TimeValue(Time.UNSPECIFIED, BinaryPV.active))), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()));
            new ScheduleObject(d1, 3, "sch3", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), weekly,
                    new SequenceOf<>(), BinaryPV.inactive, new SequenceOf<>(), 12, false);
        }, ErrorClass.property, ErrorCode.invalidConfigurationData);

        TestUtils.assertBACnetServiceException(() -> {
            final SequenceOf<SpecialEvent> exceptions = new SequenceOf<>( //
                    new SpecialEvent(new CalendarEntry(new Date(d1)),
                            new SequenceOf<>(new TimeValue(new Time(20, Time.UNSPECIFIC, 0, 0), BinaryPV.active)),
                            new UnsignedInteger(10)));
            new ScheduleObject(d1, 4, "sch4", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), null, exceptions,
                    BinaryPV.inactive, new SequenceOf<>(), 12, false);
        }, ErrorClass.property, ErrorCode.invalidConfigurationData);

        //
        // Validation for data type change
        TestUtils.assertBACnetServiceException(() -> {
            BinaryValueObject bv1 = new BinaryValueObject(d1, 1, "bv1", BinaryPV.inactive, false);
            final BACnetArray<DailySchedule> weekly = new BACnetArray<>( //
                    new DailySchedule(new SequenceOf<>(new TimeValue(new Time(d1), BinaryPV.active))), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()));
            ScheduleObject so = new ScheduleObject(d1, 1, "sch5", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), weekly,
                    new SequenceOf<>(), BinaryPV.inactive, new SequenceOf<>(new DeviceObjectPropertyReference(1, bv1.getId(), PropertyIdentifier.presentValue)), 12, false);

            // Try to change data type from binary to analog
            so.writeProperty(null, PropertyIdentifier.listOfObjectPropertyReferences, new SequenceOf<>(new DeviceObjectPropertyReference(1, av.getId(), PropertyIdentifier.presentValue)));
        }, ErrorClass.property, ErrorCode.invalidDataType);
    }

    private ScheduleObject createScheduleObject(AnalogValueObject av0, AnalogValueObject av1, Primitive scheduleDefault) throws Exception {
        final SequenceOf<CalendarEntry> dateList = new SequenceOf<>( //
                new CalendarEntry(new Date(-1, null, -1, DayOfWeek.FRIDAY)), // Every Friday.
                new CalendarEntry(
                        new DateRange(new Date(-1, Month.NOVEMBER, -1, null), new Date(-1, Month.FEBRUARY, -1, null))), // November to February
                new CalendarEntry(new WeekNDay(Month.UNSPECIFIED, WeekOfMonth.days22to28, DayOfWeek.WEDNESDAY)) // The Wednesday during the 4th week of each month.
        );

        final CalendarObject co = new CalendarObject(d1, 0, "cal0", dateList);

        final DateRange effectivePeriod = new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED);

        // Monday-Wednesday 8:00 and 17:00
        // Thursday 9:00 and 20:00
        // Friday 9:00 and 21:30
        // Saturday-Sunday -
        final BACnetArray<DailySchedule> weeklySchedule = new BACnetArray<>(
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(8, 0, 0, 0), new Real(10)),
                        new TimeValue(new Time(17, 0, 0, 0), new Real(11)))), //
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(8, 0, 0, 0), new Real(12)),
                        new TimeValue(new Time(17, 0, 0, 0), new Null()))), //
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(17, 0, 0, 0), new Real(15)),
                        new TimeValue(new Time(8, 0, 0, 0), new Real(14)))), // TimeValue is in wrong order
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(9, 0, 0, 0), new Real(16)),
                        new TimeValue(new Time(20, 0, 0, 0), new Real(17)))), //
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(9, 0, 0, 0), new Real(18)),
                        new TimeValue(new Time(21, 30, 0, 0), new Real(19)))), //
                new DailySchedule(new SequenceOf<>()), //
                new DailySchedule(new SequenceOf<>()));

        // Every Friday, November to February, The Wednesday during the 4th week of each month - 8:00 and 22:00 (Priority:10)
        // Every Friday, November to February, The Wednesday during the 4th week of each month - 13:00 and 14:00 (Priority:7)
        // 8.th day of month and also Wednesday - 10:30 and 17:00 (Priority:6)
        final SequenceOf<SpecialEvent> exceptionSchedule = new SequenceOf<>(
                new SpecialEvent(co.getId(),
                        new SequenceOf<>(new TimeValue(new Time(8, 0, 0, 0), new Real(20)),
                                new TimeValue(new Time(22, 0, 0, 0), new Real(21))),
                        new UnsignedInteger(10)), // Calendar
                new SpecialEvent(co.getId(),
                        new SequenceOf<>(new TimeValue(new Time(14, 0, 0, 0), new Real(23)),
                                new TimeValue(new Time(13, 0, 0, 0), new Real(22))),  // TimValue is in wrong order
                        new UnsignedInteger(7)), // Calendar
                new SpecialEvent(new CalendarEntry(new Date(-1, null, 8, DayOfWeek.WEDNESDAY)),
                        new SequenceOf<>(new TimeValue(new Time(10, 30, 0, 0), new Real(24)),
                                new TimeValue(new Time(17, 0, 0, 0), new Null())),
                        new UnsignedInteger(6)) // 7th is a Wednesday
        );
        final SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = new SequenceOf<>( //
                new DeviceObjectPropertyReference(av0.getId(), PropertyIdentifier.presentValue, null,
                        rd2.getObjectIdentifier()), //
                new DeviceObjectPropertyReference(av1.getId(), PropertyIdentifier.presentValue, null, null) //
        );

        return new ScheduleObject(d1, 0, "sch0", effectivePeriod, weeklySchedule, exceptionSchedule,
                scheduleDefault, listOfObjectPropertyReferences, 12, false);
    }

    private static class ObjectWriteListenerMixin extends AbstractMixin {

        private volatile BACnetObjectWriteListener listener;
        public ObjectWriteListenerMixin(BACnetObject bo) {
            super(bo);
        }

        @Override
        protected boolean writeProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
            if(listener != null) {
                listener.writeProperty(value.getPropertyIdentifier(), value);
            }
            return false;
        }

        @Override
        protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
            if(listener != null) {
                listener.writeProperty(pid, newValue);
            }
        }

        public void setListener(BACnetObjectWriteListener listener) {
            this.listener = listener;
        }
    }

    @FunctionalInterface
    private static interface BACnetObjectWriteListener {
        void writeProperty(PropertyIdentifier pid, Encodable value);
    }
}
