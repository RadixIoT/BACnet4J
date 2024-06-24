package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.service.confirmed.AddListElementRequest;
import com.serotonin.bacnet4j.service.confirmed.RemoveListElementRequest;
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
import com.serotonin.bacnet4j.type.constructed.WeekNDay;
import com.serotonin.bacnet4j.type.constructed.WeekNDay.WeekOfMonth;
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

public class ScheduleObjectTest extends AbstractTest {
    private final float defaultScheduledValue = 999;

    @Test
    public void fullTest() throws Exception {
        // Not really a full test. The effective period could be better.

        clock.set(2115, java.time.Month.MAY, 1, 12, 0, 0);

        final AnalogValueObject av0 = new AnalogValueObject(d2, 0, "av0", 98, EngineeringUnits.amperes, false)
                .supportCommandable(-2);
        final AnalogValueObject av1 = new AnalogValueObject(d1, 1, "av1", 99, EngineeringUnits.amperesPerMeter, false)
                .supportCommandable(-1);

        final SequenceOf<CalendarEntry> dateList = new SequenceOf<>( //
                new CalendarEntry(new Date(-1, null, -1, DayOfWeek.FRIDAY)), // Every Friday.
                new CalendarEntry(
                        new DateRange(new Date(-1, Month.NOVEMBER, -1, null), new Date(-1, Month.FEBRUARY, -1, null))), // November to February
                new CalendarEntry(new WeekNDay(Month.UNSPECIFIED, WeekOfMonth.days22to28, DayOfWeek.WEDNESDAY)) // The Wednesday during the 4th week of each month.
        );

        final CalendarObject co = new CalendarObject(d1, 0, "cal0", dateList);

        final DateRange effectivePeriod = new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED);
        final BACnetArray<DailySchedule> weeklySchedule = new BACnetArray<>( //
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(8, 0, 0, 0), new Real(10)),
                        new TimeValue(new Time(17, 0, 0, 0), new Real(11)))), //
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(8, 0, 0, 0), new Real(12)),
                        new TimeValue(new Time(17, 0, 0, 0), new Null()))), //
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(8, 0, 0, 0), new Real(14)),
                        new TimeValue(new Time(17, 0, 0, 0), new Real(15)))), //
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(9, 0, 0, 0), new Real(16)),
                        new TimeValue(new Time(20, 0, 0, 0), new Real(17)))), //
                new DailySchedule(new SequenceOf<>(new TimeValue(new Time(9, 0, 0, 0), new Real(18)),
                        new TimeValue(new Time(21, 30, 0, 0), new Real(19)))), //
                new DailySchedule(new SequenceOf<TimeValue>()), //
                new DailySchedule(new SequenceOf<TimeValue>()));
        final SequenceOf<SpecialEvent> exceptionSchedule = new SequenceOf<>( //
                new SpecialEvent(co.getId(),
                        new SequenceOf<>(new TimeValue(new Time(8, 0, 0, 0), new Real(20)),
                                new TimeValue(new Time(22, 0, 0, 0), new Real(21))),
                        new UnsignedInteger(10)), // Calendar
                new SpecialEvent(co.getId(),
                        new SequenceOf<>(new TimeValue(new Time(13, 0, 0, 0), new Real(22)),
                                new TimeValue(new Time(14, 0, 0, 0), new Real(23))),
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

        final ScheduleObject so = new ScheduleObject(d1, 0, "sch0", effectivePeriod, weeklySchedule, exceptionSchedule,
                new Real(defaultScheduledValue), listOfObjectPropertyReferences, 12, false);

        Thread.sleep(100); // Let the requests be received.
        Assert.assertEquals(new Real(14), so.get(PropertyIdentifier.presentValue));
        Assert.assertEquals(new Real(14), av0.get(PropertyIdentifier.presentValue));
        Assert.assertEquals(new Real(14), av1.get(PropertyIdentifier.presentValue));

        // Start actual tests.
        testTime(so, av0, av1, java.time.Month.MAY, 1, 17, 0, 15f);
        testTime(so, av0, av1, java.time.Month.MAY, 2, 0, 0, defaultScheduledValue);
        testTime(so, av0, av1, java.time.Month.MAY, 2, 9, 0, 16f);
        testTime(so, av0, av1, java.time.Month.MAY, 2, 20, 0, 17f);
        testTime(so, av0, av1, java.time.Month.MAY, 3, 0, 0, defaultScheduledValue);
        testTime(so, av0, av1, java.time.Month.MAY, 3, 13, 0, 22f);
        testTime(so, av0, av1, java.time.Month.MAY, 3, 14, 0, 23f);
        testTime(so, av0, av1, java.time.Month.MAY, 4, 0, 0, defaultScheduledValue);
        testTime(so, av0, av1, java.time.Month.MAY, 5, 0, 0, defaultScheduledValue);
        testTime(so, av0, av1, java.time.Month.MAY, 6, 0, 0, defaultScheduledValue);
        testTime(so, av0, av1, java.time.Month.MAY, 6, 8, 0, 10f);
        testTime(so, av0, av1, java.time.Month.MAY, 6, 17, 0, 11f);
        testTime(so, av0, av1, java.time.Month.MAY, 7, 0, 0, defaultScheduledValue);
        testTime(so, av0, av1, java.time.Month.MAY, 7, 8, 0, 12f);
        testTime(so, av0, av1, java.time.Month.MAY, 7, 17, 0, null); // Null schedule
        testTime(so, av0, av1, java.time.Month.MAY, 8, 0, 0, defaultScheduledValue);
        testTime(so, av0, av1, java.time.Month.MAY, 8, 10, 30, 24f);
        testTime(so, av0, av1, java.time.Month.MAY, 8, 17, 0, null);  // Null schedule
        testTime(so, av0, av1, java.time.Month.MAY, 9, 0, 0, defaultScheduledValue);
    }

    private void testTime(final ScheduleObject so, final AnalogValueObject av0, final AnalogValueObject av1,
            final java.time.Month month, final int day, final int hour, final int min, final Float scheduledValue)
            throws Exception {
        clock.set(2115, month, day, hour, min, 0);
        so.updatePresentValue();
        Thread.sleep(100); // Let the requests be received.

        Primitive expectedValue = scheduledValue == null ? new Real(defaultScheduledValue): new Real(scheduledValue);
        Assert.assertEquals(expectedValue, so.get(PropertyIdentifier.presentValue));
        Assert.assertEquals(expectedValue, av0.get(PropertyIdentifier.presentValue));
        Assert.assertEquals(expectedValue, av1.get(PropertyIdentifier.presentValue));
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
        assertEquals(0, listener.notifs.size());

        // Write a fault reliability value.
        so.writePropertyInternal(PropertyIdentifier.reliability, Reliability.memberFault);
        assertEquals(EventState.fault, so.readProperty(PropertyIdentifier.eventState));
        Thread.sleep(100);
        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.notifs.size());
        final Map<String, Object> notif = listener.notifs.remove(0);
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
    }
}
