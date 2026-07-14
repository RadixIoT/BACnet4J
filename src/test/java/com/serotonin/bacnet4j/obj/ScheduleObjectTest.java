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
import static com.serotonin.bacnet4j.TestUtils.awaitTrue;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.ObjectTestUtils.ObjectWriteNotifier;
import com.serotonin.bacnet4j.service.confirmed.AddListElementRequest;
import com.serotonin.bacnet4j.service.confirmed.RemoveListElementRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.CalendarEntry;
import com.serotonin.bacnet4j.type.constructed.DailySchedule;
import com.serotonin.bacnet4j.type.constructed.DateRange;
import com.serotonin.bacnet4j.type.constructed.DateTime;
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

public class ScheduleObjectTest extends AbstractTest {
    @Test
    public void fullTest() throws Exception {
        // Not really a full test. The effective period could be better.
        clock.set(2115, java.time.Month.MAY, 1, 12, 0, 0);

        AnalogValueObject av0 = d2.addObject(new AnalogValueObject(
                d2, 0, "av0", 98, EngineeringUnits.amperes, false).supportCommandable(-2));
        AnalogValueObject av1 = d1.addObject(new AnalogValueObject(
                d1, 1, "av1", 99, EngineeringUnits.amperesPerMeter, false).supportCommandable(-1));

        Primitive defaultScheduledValue = new Real(999);
        ScheduleObject so = createScheduleObject(av0, av1, defaultScheduledValue);
        ObjectWriteNotifier<ScheduleObject> soNotifier = ObjectTestUtils.createObjectWriteNotifier(so);

        awaitEquals(new Real(14), () -> so.get(PropertyIdentifier.presentValue));
        awaitEquals(new Real(14), () -> av0.get(PropertyIdentifier.presentValue));
        awaitEquals(new Real(14), () -> av1.get(PropertyIdentifier.presentValue));

        // Start actual tests.
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 1, 17, 0, new Real(15)); // Wednesday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 2, 0, 0,
                defaultScheduledValue); // Thursday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 2, 9, 0, new Real(16)); // Thursday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 2, 20, 0, new Real(17)); // Thursday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 3, 0, 0,
                defaultScheduledValue); // Friday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 3, 13, 0,
                new Real(22)); // Exception schedule at 13:00 with priority 7
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 3, 14, 0,
                new Real(23)); // Exception schedule at 14:00 with priority 7
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 4, 0, 0,
                defaultScheduledValue); // Saturday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 5, 0, 0,
                defaultScheduledValue); // Sunday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 6, 0, 0,
                defaultScheduledValue); // Monday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 6, 8, 0, new Real(10)); // Monday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 6, 17, 0, new Real(11));  // Monday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 7, 0, 0,
                defaultScheduledValue); // Tuesday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 7, 8, 0, new Real(12)); // Tuesday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 7, 17, 0, new Null()); // Null schedule from weekly schedule
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 8, 0, 0,
                defaultScheduledValue); // Wednesday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 8, 10, 30,
                new Real(24)); // Exception schedule at 10:30 with priority 6
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 8, 17, 0,
                new Null());  // Null schedule from exception schedule
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 9, 0, 0,
                defaultScheduledValue); // Thursday beginning of the day
    }

    @Test
    public void nullScheduleDefaultTest() throws Exception {
        clock.set(2115, java.time.Month.MAY, 1, 12, 0, 0);

        AnalogValueObject av0 = d2.addObject(new AnalogValueObject(
                d2, 0, "av0", 98, EngineeringUnits.amperes, false).supportCommandable(-2));
        AnalogValueObject av1 = d1.addObject(new AnalogValueObject(
                d1, 1, "av1", 99, EngineeringUnits.amperesPerMeter, false).supportCommandable(-1));

        Primitive defaultScheduledValue = new Null();
        ScheduleObject so = createScheduleObject(av0, av1, defaultScheduledValue);
        ObjectWriteNotifier<ScheduleObject> soNotifier = ObjectTestUtils.createObjectWriteNotifier(so);

        awaitEquals(new Real(14), () -> so.get(PropertyIdentifier.presentValue));
        awaitEquals(new Real(14), () -> av0.get(PropertyIdentifier.presentValue));
        awaitEquals(new Real(14), () -> av1.get(PropertyIdentifier.presentValue));

        // Start actual tests.
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 1, 17, 0, new Real(15)); // Wednesday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 2, 0, 0,
                defaultScheduledValue); // Thursday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 2, 9, 0, new Real(16)); // Thursday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 2, 20, 0, new Real(17)); // Thursday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 3, 0, 0,
                defaultScheduledValue); // Friday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 3, 13, 0,
                new Real(22)); // Exception schedule at 13:00 with priority 7
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 3, 14, 0,
                new Real(23)); // Exception schedule at 14:00 with priority 7
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 4, 0, 0,
                defaultScheduledValue); // Saturday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 5, 0, 0,
                defaultScheduledValue); // Sunday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 6, 0, 0,
                defaultScheduledValue); // Monday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 6, 8, 0, new Real(10)); // Monday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 6, 17, 0, new Real(11));  // Monday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 7, 0, 0,
                defaultScheduledValue); // Tuesday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 7, 8, 0, new Real(12)); // Tuesday
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 7, 17, 0, new Null()); // Null schedule from weekly schedule
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 8, 0, 0,
                defaultScheduledValue); // Wednesday beginning of the day
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 8, 10, 30,
                new Real(24)); // Exception schedule at 10:30 with priority 6
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 8, 17, 0,
                new Null());  // Null schedule from exception schedule
        testTime(soNotifier, av0, av1, java.time.Month.MAY, 9, 0, 0,
                defaultScheduledValue); // Thursday beginning of the day
    }

    private void testTime(ObjectWriteNotifier<ScheduleObject> so, AnalogValueObject av0, AnalogValueObject av1,
            java.time.Month month, int day, int hour, int min, Primitive scheduledValue) throws Exception {
        so.clear();
        clock.set(2115, month, day, hour, min, 0);

        // Wait until the schedule has completed the update. We do this because in some cases the values have not
        // changed, and so the awaits below will complete immediately, which ultimately can cause another time update
        // to be called before the previous one has finished, which then results in missed updates.
        awaitTrue(() -> {
            DateTime lastUpdateTime = so.obj().getLastUpdateTime();
            return lastUpdateTime.getDate().getMonth().getId() == month.getValue() && lastUpdateTime.getDate()
                    .getDay() == day && lastUpdateTime.getTime().getHour() == hour && lastUpdateTime.getTime()
                    .getMinute() == min;
        });

        if (scheduledValue.getClass().equals(Null.class)) {
            Primitive scheduleDefault = so.obj().readProperty(PropertyIdentifier.scheduleDefault);
            if (scheduleDefault.getClass().equals(Null.class)) {
                so.waitFor(PropertyIdentifier.presentValue, new Null(), 5000);
                awaitEquals(av0.readProperty(PropertyIdentifier.relinquishDefault),
                        () -> av0.get(PropertyIdentifier.presentValue));
                awaitEquals(av1.readProperty(PropertyIdentifier.relinquishDefault),
                        () -> av1.get(PropertyIdentifier.presentValue));
            } else {
                so.waitFor(PropertyIdentifier.presentValue, scheduleDefault, 5000);
                awaitEquals(scheduleDefault, () -> av0.get(PropertyIdentifier.presentValue));
                awaitEquals(scheduleDefault, () -> av1.get(PropertyIdentifier.presentValue));
            }
        } else {
            so.waitFor(PropertyIdentifier.presentValue, scheduledValue, 5000);
            awaitEquals(scheduledValue, () -> av0.get(PropertyIdentifier.presentValue));
            awaitEquals(scheduledValue, () -> av1.get(PropertyIdentifier.presentValue));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicAlarms() throws Exception {
        NotificationClassObject nc = d1.addObject(new NotificationClassObject(
                d1, 7, "nc7", 100, 5, 200, new EventTransitionBits(false, false, false)));
        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        AnalogValueObject av1 = d1.addObject(new AnalogValueObject(
                d1, 1, "av1", 99, EngineeringUnits.amperesPerMeter, false).supportCommandable(-1));

        SequenceOf<SpecialEvent> exceptionSchedule = new SequenceOf<>(
                new SpecialEvent(new CalendarEntry(new Date(-1, null, -1, DayOfWeek.WEDNESDAY)), new SequenceOf<>(),
                        new UnsignedInteger(6)) // Wednesdays
        );
        SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = new SequenceOf<>( //
                new DeviceObjectPropertyReference(av1.getId(), PropertyIdentifier.presentValue, null, null) //
        );
        ScheduleObject so = d1.addObject(new ScheduleObject(
                d1, 0, "sch0", new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED), null,
                exceptionSchedule, new Real(8), listOfObjectPropertyReferences, 12, false));
        so.supportIntrinsicReporting(7, new EventTransitionBits(true, true, true), NotifyType.alarm);

        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        assertEquals(0, listener.getNotifCount());

        // Write a fault reliability value.
        so.writePropertyInternal(PropertyIdentifier.reliability, Reliability.memberFault);
        assertEquals(EventState.fault, so.readProperty(PropertyIdentifier.eventState));

        // Ensure that a proper looking event notification was received.
        awaitEquals(1, listener::getNotifCount);
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(so.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) so.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(7), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfReliabilityNotif(Reliability.memberFault, new StatusFlags(true, true, false, false),
                        new SequenceOf<>())), notif.eventValues());
    }

    /**
     * Ensures that schedule.listOfObjectPropertyReferences can be modified with WriteProperty
     */
    @Test
    public void listValues() throws Exception {
        ScheduleObject so = d1.addObject(new ScheduleObject(
                d1, 0, "sch0", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), null,
                new SequenceOf<>(), new Real(8), new SequenceOf<>(), 12, false));

        // Add a few items to the list.
        ObjectIdentifier oid = new ObjectIdentifier(ObjectType.analogInput, 0);

        DeviceObjectPropertyReference local1 =
                new DeviceObjectPropertyReference(oid, PropertyIdentifier.presentValue, null, null);
        DeviceObjectPropertyReference remote10 =
                new DeviceObjectPropertyReference(oid, PropertyIdentifier.presentValue, null,
                        new ObjectIdentifier(ObjectType.device, 10));
        DeviceObjectPropertyReference remote11 =
                new DeviceObjectPropertyReference(oid, PropertyIdentifier.presentValue, null,
                        new ObjectIdentifier(ObjectType.device, 11));

        // Ensure that the list is empty.
        SequenceOf<Destination> list =
                RequestUtils.getProperty(d2, rd1, so.getId(), PropertyIdentifier.listOfObjectPropertyReferences);
        assertEquals(new SequenceOf<>(), list);

        // Add a few elements.
        AddListElementRequest aler =
                new AddListElementRequest(so.getId(), PropertyIdentifier.listOfObjectPropertyReferences, null,
                        new SequenceOf<>(local1, remote10));
        d2.send(rd1, aler).get();
        list = RequestUtils.getProperty(d2, rd1, so.getId(), PropertyIdentifier.listOfObjectPropertyReferences);
        assertEquals(new SequenceOf<>(local1, remote10), list);

        // Write one more.
        d2.send(rd1, new AddListElementRequest(so.getId(), PropertyIdentifier.listOfObjectPropertyReferences, null,
                new SequenceOf<>(remote11))).get();
        list = RequestUtils.getProperty(d2, rd1, so.getId(), PropertyIdentifier.listOfObjectPropertyReferences);
        assertEquals(new SequenceOf<>(local1, remote10, remote11), list);

        // Remove some.
        d2.send(rd1, new RemoveListElementRequest(so.getId(), PropertyIdentifier.listOfObjectPropertyReferences, null,
                new SequenceOf<Encodable>(remote10, local1))).get();
        list = RequestUtils.getProperty(d2, rd1, so.getId(), PropertyIdentifier.listOfObjectPropertyReferences);
        assertEquals(new SequenceOf<>(remote11), list);
    }

    @Test
    public void changeDataType() throws Exception {
        AnalogValueObject av = d1.addObject(new AnalogValueObject(
                d1, 0, "av0", 98, EngineeringUnits.amperes, false).supportCommandable(-2));
        BinaryValueObject bv = d1.addObject(new BinaryValueObject(d1, 0, "bv0", BinaryPV.inactive, false));

        SequenceOf<SpecialEvent> binarySchedule = new SequenceOf<>(
                new SpecialEvent(new CalendarEntry(new Date(-1, null, -1, DayOfWeek.WEDNESDAY)),
                        new SequenceOf<>(new TimeValue(new Time(d1), BinaryPV.active)), new UnsignedInteger(1)));
        SequenceOf<SpecialEvent> analogSchedule = new SequenceOf<>(
                new SpecialEvent(new CalendarEntry(new Date(-1, null, -1, DayOfWeek.WEDNESDAY)),
                        new SequenceOf<>(new TimeValue(new Time(d1), new Real(2))), new UnsignedInteger(1)));

        SequenceOf<DeviceObjectPropertyReference> binaryReferences =
                new SequenceOf<>(new DeviceObjectPropertyReference(1, bv.getId(), PropertyIdentifier.presentValue));
        SequenceOf<DeviceObjectPropertyReference> analogReferences =
                new SequenceOf<>(new DeviceObjectPropertyReference(1, av.getId(), PropertyIdentifier.presentValue));

        BinaryPV binaryDefaultValue = BinaryPV.inactive;
        Real analogDefaultValue = new Real(1);

        ScheduleObject so = d1.addObject(new ScheduleObject(
                d1, 1, "sch0", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), null,
                binarySchedule, binaryDefaultValue, binaryReferences, 12, false));

        Assert.assertEquals(binaryDefaultValue, so.readProperty(PropertyIdentifier.scheduleDefault));
        Assert.assertEquals(binaryReferences, so.readProperty(PropertyIdentifier.listOfObjectPropertyReferences));

        List<PropertyValue> propertyValues = new ArrayList<>();
        propertyValues.add(new PropertyValue(PropertyIdentifier.listOfObjectPropertyReferences,
                new SequenceOf<>())); // Reset listOfObjectPropertyReferences
        propertyValues.add(new PropertyValue(PropertyIdentifier.scheduleDefault, new Null())); // Reset defaultSchedule
        propertyValues.add(
                new PropertyValue(PropertyIdentifier.exceptionSchedule, new SequenceOf<>())); // Reset exceptionSchedule

        // Change the data type later
        propertyValues.add(new PropertyValue(PropertyIdentifier.exceptionSchedule, analogSchedule));
        propertyValues.add(new PropertyValue(PropertyIdentifier.scheduleDefault, analogDefaultValue));
        propertyValues.add(new PropertyValue(PropertyIdentifier.listOfObjectPropertyReferences, analogReferences));

        List<WriteAccessSpecification> specs = new ArrayList<>();
        specs.add(new WriteAccessSpecification(new ObjectIdentifier(ObjectType.schedule, 1),
                new SequenceOf<>(propertyValues)));
        d2.send(rd1, new WritePropertyMultipleRequest(new SequenceOf<>(specs))).get();

        Assert.assertEquals(analogDefaultValue, so.readProperty(PropertyIdentifier.scheduleDefault));
        Assert.assertEquals(analogReferences, so.readProperty(PropertyIdentifier.listOfObjectPropertyReferences));
    }

    @Test
    public void validations() throws Exception {
        AnalogValueObject av = d2.addObject(new AnalogValueObject(
                d2, 0, "av0", 98, EngineeringUnits.amperes, false).supportCommandable(-2));

        //
        // Entries in the list of property references must reference properties of this type
        assertBACnetServiceException(
                () -> new ScheduleObject(d1, 0, "sch0", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), null,
                        new SequenceOf<>(), BinaryPV.inactive, new SequenceOf<>(
                        new DeviceObjectPropertyReference(1, av.getId(), PropertyIdentifier.presentValue)), 12, false),
                ErrorClass.property, ErrorCode.invalidDataType);

        //
        // Time value entries in the weekly and exception schedules must be of this type
        assertBACnetServiceException(() -> {
            BACnetArray<DailySchedule> weekly = new BACnetArray<>( //
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

        assertBACnetServiceException(() -> {
            SequenceOf<SpecialEvent> exceptions = new SequenceOf<>( //
                    new SpecialEvent(new CalendarEntry(new Date(d1)),
                            new SequenceOf<>(new TimeValue(new Time(d1), new Real(0))), new UnsignedInteger(10)));
            new ScheduleObject(d1, 2, "sch2", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), null, exceptions,
                    BinaryPV.inactive, new SequenceOf<>(), 12, false);
        }, ErrorClass.property, ErrorCode.invalidDataType);

        //
        // Time values must have times that are fully specific.
        assertBACnetServiceException(() -> {
            BACnetArray<DailySchedule> weekly = new BACnetArray<>( //
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

        assertBACnetServiceException(() -> {
            SequenceOf<SpecialEvent> exceptions = new SequenceOf<>( //
                    new SpecialEvent(new CalendarEntry(new Date(d1)),
                            new SequenceOf<>(new TimeValue(new Time(20, Time.UNSPECIFIC, 0, 0), BinaryPV.active)),
                            new UnsignedInteger(10)));
            new ScheduleObject(d1, 4, "sch4", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), null, exceptions,
                    BinaryPV.inactive, new SequenceOf<>(), 12, false);
        }, ErrorClass.property, ErrorCode.invalidConfigurationData);

        //
        // Validation for data type change
        assertBACnetServiceException(() -> {
            BinaryValueObject bv1 = d1.addObject(new BinaryValueObject(d1, 1, "bv1", BinaryPV.inactive, false));
            BACnetArray<DailySchedule> weekly = new BACnetArray<>( //
                    new DailySchedule(new SequenceOf<>(new TimeValue(new Time(d1), BinaryPV.active))), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()), //
                    new DailySchedule(new SequenceOf<>()));
            ScheduleObject so = d1.addObject(new ScheduleObject(
                    d1, 1, "sch5", new DateRange(Date.MINIMUM_DATE, Date.MAXIMUM_DATE), weekly,
                    new SequenceOf<>(), BinaryPV.inactive, new SequenceOf<>(
                    new DeviceObjectPropertyReference(1, bv1.getId(), PropertyIdentifier.presentValue)), 12,
                    false));

            // Try to change data type from binary to analog
            so.writeProperty(null, PropertyIdentifier.listOfObjectPropertyReferences, new SequenceOf<>(
                    new DeviceObjectPropertyReference(1, av.getId(), PropertyIdentifier.presentValue)));
        }, ErrorClass.property, ErrorCode.invalidDataType);
    }

    /**
     * 135-2020bv-1: when Write_Every_Scheduled_Action is TRUE, the schedule must write to its
     * property references on every scheduled transition, even when the calculated Present_Value
     * did not change from the previous transition.
     */
    @Test
    public void writeEveryScheduledActionTrue_writesOnUnchangedTransition() throws Exception {
        // Start at 08:30 Wednesday: the 08:00 time-value pair is already in effect (Present_Value=10)
        // and the next refresher will fire at 12:00, which yields the same value (10).
        clock.set(2115, java.time.Month.MAY, 1, 8, 30, 0);

        AnalogValueObject av = d1.addObject(new AnalogValueObject(
                d1, 0, "av0", 0, EngineeringUnits.amperes, false).supportCommandable(-1));

        CountingScheduleObject so = createSameValueScheduleObject(av);
        so.writePropertyInternal(PropertyIdentifier.writeEveryScheduledAction, Boolean.TRUE);

        awaitEquals(new Real(10), () -> so.get(PropertyIdentifier.presentValue));
        int initialCount = so.writeCount();

        // Advance past 12:00 — new time-value pair coming into effect, but same value.
        clock.set(2115, java.time.Month.MAY, 1, 12, 0, 0);
        awaitTrue(() -> so.writeCount() > initialCount);
    }

    /**
     * 135-2020bv-1: when Write_Every_Scheduled_Action is FALSE or not present, the schedule must
     * NOT write on a transition where the Present_Value did not change.
     */
    @Test
    public void writeEveryScheduledActionAbsent_skipsUnchangedTransition() throws Exception {
        clock.set(2115, java.time.Month.MAY, 1, 8, 30, 0);

        AnalogValueObject av = d1.addObject(new AnalogValueObject(
                d1, 0, "av0", 0, EngineeringUnits.amperes, false).supportCommandable(-1));

        CountingScheduleObject so = createSameValueScheduleObject(av);
        // Leave writeEveryScheduledAction unset (absent) — spec treats this the same as FALSE.
        assertNull(so.get(PropertyIdentifier.writeEveryScheduledAction));

        awaitEquals(new Real(10), () -> so.get(PropertyIdentifier.presentValue));
        int countBeforeTransition = so.writeCount();

        // Advance past 12:00 — new time-value pair, but same value. No writes should occur.
        clock.set(2115, java.time.Month.MAY, 1, 12, 0, 0);
        awaitTrue(() -> {
            DateTime last = so.getLastUpdateTime();
            return last != null && last.getDate().getDay() == 1 && last.getTime().getHour() == 12;
        });
        // Give the executor a brief window to flush any spurious write.
        quiesce();
        assertEquals("No doWrites should occur when Present_Value is unchanged and WESA is absent",
                countBeforeTransition, so.writeCount());
    }

    /**
     * A ScheduleObject that counts calls to {@code doWrites} for test observation.
     */
    private static class CountingScheduleObject extends ScheduleObject {
        // Lazy init because super() runs before subclass field initializers.
        private java.util.concurrent.atomic.AtomicInteger writes;

        CountingScheduleObject(com.serotonin.bacnet4j.LocalDevice localDevice, int instanceNumber, String name,
                DateRange effectivePeriod, BACnetArray<DailySchedule> weeklySchedule,
                SequenceOf<SpecialEvent> exceptionSchedule, Primitive scheduleDefault,
                SequenceOf<DeviceObjectPropertyReference> refs, int priority, boolean oos)
                throws BACnetServiceException {
            super(localDevice, instanceNumber, name, effectivePeriod, weeklySchedule, exceptionSchedule,
                    scheduleDefault, refs, priority, oos);
        }

        @Override
        void doWrites(Encodable value) {
            if (writes == null)
                writes = new java.util.concurrent.atomic.AtomicInteger();
            writes.incrementAndGet();
            super.doWrites(value);
        }

        int writeCount() {
            return writes == null ? 0 : writes.get();
        }
    }

    private CountingScheduleObject createSameValueScheduleObject(AnalogValueObject target) throws Exception {
        // Wednesday: 08:00 = 10, 12:00 = 10. Same value at both transitions.
        BACnetArray<DailySchedule> weeklySchedule = new BACnetArray<>(new DailySchedule(new SequenceOf<>()),
                new DailySchedule(new SequenceOf<>()),
                new DailySchedule(new SequenceOf<>(
                        new TimeValue(new Time(8, 0, 0, 0), new Real(10)),
                        new TimeValue(new Time(12, 0, 0, 0), new Real(10)))),
                new DailySchedule(new SequenceOf<>()),
                new DailySchedule(new SequenceOf<>()),
                new DailySchedule(new SequenceOf<>()),
                new DailySchedule(new SequenceOf<>()));

        SequenceOf<DeviceObjectPropertyReference> refs = new SequenceOf<>(
                new DeviceObjectPropertyReference(target.getId(), PropertyIdentifier.presentValue, null, null));

        return d1.addObject(new CountingScheduleObject(
                d1, 0, "sch-wesa", new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED), weeklySchedule,
                new SequenceOf<>(), new Real(999), refs, 12, false));
    }

    /**
     * Per BIBB SCHED-WS-I-B (K.3.8) as revised by addendum 135-2020ci-4: if the
     * List_Of_Object_Property_References property is capable of referencing a commandable property,
     * then the Priority_For_Writing property shall be writable. This schedule references commandable
     * analog values, so the condition applies.
     */
    @Test
    public void priorityForWritingIsWritable() throws Exception {
        clock.set(2115, java.time.Month.MAY, 1, 12, 0, 0);

        AnalogValueObject av0 = d2.addObject(new AnalogValueObject(
                d2, 0, "av0", 98, EngineeringUnits.amperes, false).supportCommandable(-2));
        AnalogValueObject av1 = d1.addObject(new AnalogValueObject(
                d1, 1, "av1", 99, EngineeringUnits.amperesPerMeter, false).supportCommandable(-1));
        ScheduleObject so = createScheduleObject(av0, av1, new Real(999));

        assertEquals(new UnsignedInteger(12), so.readProperty(PropertyIdentifier.priorityForWriting));

        var response = d2.send(rd1, new WritePropertyRequest(so.getId(), PropertyIdentifier.priorityForWriting, null,
                new UnsignedInteger(9), null)).get();
        assertNull(response);
        assertEquals(new UnsignedInteger(9), so.readProperty(PropertyIdentifier.priorityForWriting));
    }

    private ScheduleObject createScheduleObject(AnalogValueObject av0, AnalogValueObject av1, Primitive scheduleDefault)
            throws Exception {
        SequenceOf<CalendarEntry> dateList =
                new SequenceOf<>(new CalendarEntry(new Date(-1, null, -1, DayOfWeek.FRIDAY)), // Every Friday.
                        new CalendarEntry(new DateRange(new Date(-1, Month.NOVEMBER, -1, null),
                                new Date(-1, Month.FEBRUARY, -1, null))),
                        // November to February
                        new CalendarEntry(new WeekNDay(Month.UNSPECIFIED, WeekOfMonth.days22to28, DayOfWeek.WEDNESDAY))
                        // The Wednesday during the 4th week of each month.
                );

        CalendarObject co = d1.addObject(new CalendarObject(d1, 0, "cal0", dateList));

        DateRange effectivePeriod = new DateRange(Date.UNSPECIFIED, Date.UNSPECIFIED);

        // Monday-Wednesday 8:00 and 17:00
        // Thursday 9:00 and 20:00
        // Friday 9:00 and 21:30
        // Saturday-Sunday -
        BACnetArray<DailySchedule> weeklySchedule = new BACnetArray<>(new DailySchedule(
                new SequenceOf<>(new TimeValue(new Time(8, 0, 0, 0), new Real(10)),
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
        SequenceOf<SpecialEvent> exceptionSchedule = new SequenceOf<>(new SpecialEvent(co.getId(),
                new SequenceOf<>(new TimeValue(new Time(8, 0, 0, 0), new Real(20)),
                        new TimeValue(new Time(22, 0, 0, 0), new Real(21))), new UnsignedInteger(10)), // Calendar
                new SpecialEvent(co.getId(), new SequenceOf<>(new TimeValue(new Time(14, 0, 0, 0), new Real(23)),
                        new TimeValue(new Time(13, 0, 0, 0), new Real(22))),  // TimValue is in wrong order
                        new UnsignedInteger(7)), // Calendar
                new SpecialEvent(new CalendarEntry(new Date(-1, null, 8, DayOfWeek.WEDNESDAY)),
                        new SequenceOf<>(new TimeValue(new Time(10, 30, 0, 0), new Real(24)),
                                new TimeValue(new Time(17, 0, 0, 0), new Null())), new UnsignedInteger(6))
                // 7th is a Wednesday
        );
        SequenceOf<DeviceObjectPropertyReference> listOfObjectPropertyReferences = new SequenceOf<>( //
                new DeviceObjectPropertyReference(av0.getId(), PropertyIdentifier.presentValue, null,
                        rd2.getObjectIdentifier()), //
                new DeviceObjectPropertyReference(av1.getId(), PropertyIdentifier.presentValue, null, null) //
        );

        return d1.addObject(new ScheduleObject(
                d1, 0, "sch0", effectivePeriod, weeklySchedule, exceptionSchedule, scheduleDefault,
                listOfObjectPropertyReferences, 12, false));
    }
}
