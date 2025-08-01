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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.confirmed.DeleteObjectRequest;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.AddressBinding;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.DeviceStatus;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.enumerated.RestartReason;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.RequestUtils;

public class DeviceObjectTest extends AbstractTest {
    private AnalogValueObject av0;

    @Override
    public void afterInit() throws Exception {
        av0 = new AnalogValueObject(d1, 0, "av0", 50, EngineeringUnits.amperes, false);
        new AnalogValueObject(d1, 1, "av1", 50, EngineeringUnits.amperes, false);
        new AnalogValueObject(d1, 2, "av2", 50, EngineeringUnits.amperes, false);
        new BinaryValueObject(d1, 0, "bv0", BinaryPV.inactive, false);
        new BinaryValueObject(d1, 1, "bv1", BinaryPV.inactive, false);
        new BinaryValueObject(d1, 2, "bv2", BinaryPV.inactive, false);
        new BinaryValueObject(d1, 3, "bv3", BinaryPV.inactive, false);
    }

    @Test
    public void objectList() throws BACnetException {
        // Get the whole list.
        final SequenceOf<ObjectIdentifier> oids = RequestUtils.getProperty(d2, rd1, PropertyIdentifier.objectList);
        assertEquals(new ObjectIdentifier(ObjectType.device, 1), oids.getBase1(1));
        assertEquals(new ObjectIdentifier(ObjectType.analogValue, 0), oids.getBase1(2));
        assertEquals(new ObjectIdentifier(ObjectType.analogValue, 1), oids.getBase1(3));
        assertEquals(new ObjectIdentifier(ObjectType.analogValue, 2), oids.getBase1(4));
        assertEquals(new ObjectIdentifier(ObjectType.binaryValue, 0), oids.getBase1(5));
        assertEquals(new ObjectIdentifier(ObjectType.binaryValue, 1), oids.getBase1(6));
        assertEquals(new ObjectIdentifier(ObjectType.binaryValue, 2), oids.getBase1(7));
        assertEquals(new ObjectIdentifier(ObjectType.binaryValue, 3), oids.getBase1(8));

        // Get one element of the list.
        final UnsignedInteger length = RequestUtils.getProperty(d2, rd1, new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.objectList, 0);
        assertEquals(8, length.intValue());

        ObjectIdentifier oid = RequestUtils.getProperty(d2, rd1, new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.objectList, 1);
        assertEquals(new ObjectIdentifier(ObjectType.device, 1), oid);

        oid = RequestUtils.getProperty(d2, rd1, new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.objectList, 4);
        assertEquals(new ObjectIdentifier(ObjectType.analogValue, 2), oid);

        oid = RequestUtils.getProperty(d2, rd1, new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.objectList, 8);
        assertEquals(new ObjectIdentifier(ObjectType.binaryValue, 3), oid);

        final ErrorClassAndCode e = RequestUtils.getProperty(d2, rd1, new ObjectIdentifier(ObjectType.device, 1),
                PropertyIdentifier.objectList, 9);
        assertEquals(ErrorClass.property, e.getErrorClass());
        assertEquals(ErrorCode.invalidArrayIndex, e.getErrorCode());
    }

    @Test
    public void incrementDatabaseRevision() throws BACnetErrorException {
        av0.setDeletable(true);
        final UnsignedInteger databaseRevision = d1.getDeviceObject().get(PropertyIdentifier.databaseRevision);
        new DeleteObjectRequest(av0.getId()).handle(d1, null);
        assertEquals(databaseRevision.increment32(), d1.getDeviceObject().get(PropertyIdentifier.databaseRevision));
    }

    @Test
    public void timeSynchronization() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        // Set up time sync for every 4 hours aligned with a 5 minute offset.
        d1.getDeviceObject().supportTimeSynchronization(new SequenceOf<>(new Recipient(d2.getId())),
                new SequenceOf<>(new Recipient(d3.getId())), 240, true, 5);

        // Add listeners to d2 and d3
        final AtomicReference<DateTime> d2Time = new AtomicReference<>();
        final AtomicBoolean d2Utc = new AtomicBoolean();
        d2.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void synchronizeTime(final Address from, final DateTime dateTime, final boolean utc) {
                d2Time.set(dateTime);
                d2Utc.set(utc);
                latch.countDown();
            }
        });

        final AtomicReference<DateTime> d3Time = new AtomicReference<>();
        final AtomicBoolean d3Utc = new AtomicBoolean();
        d3.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void synchronizeTime(final Address from, final DateTime dateTime, final boolean utc) {
                d3Time.set(dateTime);
                d3Utc.set(utc);
                latch.countDown();
            }
        });

        //
        // Advance the clock to the notification time.
        //clock.plusMillis((1000 - clock.get(ChronoField.MILLI_OF_SECOND)) % 1000);
        //clock.plusSeconds((60 - clock.get(ChronoField.SECOND_OF_MINUTE)) % 60);
        //Advance past 4hrs to make sure there is at least 1 change
        final int minutes = 240; //(1445 - clock.get(ChronoField.MINUTE_OF_DAY)) % 240;
        clock.plusMinutes(minutes);

        latch.await(1, TimeUnit.SECONDS);

        // Check the results.
        assertNotNull(d2Time.get());
        assertNotNull(d3Time.get());

        final int offsetHundredths = TimeZone.getDefault().getOffset(clock.millis()) / 10;
        final int adjustedHundredths =
                (d3Time.get().getTime().getHundredthInDay() + offsetHundredths + 8_640_000) % 8_640_000;
        assertEquals(new DateTime(d1), d2Time.get());
        assertEquals(d2Time.get().getTime().getHundredthInDay(), adjustedHundredths);

        assertFalse(d2Utc.get());
        assertTrue(d3Utc.get());
    }

    @Test
    public void calculatedProperties() throws Exception {
        final TimeZone tz = TimeZone.getDefault();

        assertEquals(new Date(d1), d1.getDeviceObject().readProperty(PropertyIdentifier.localDate));
        assertEquals(new Time(d1), d1.getDeviceObject().readProperty(PropertyIdentifier.localTime));
        assertEquals(new SignedInteger(tz.getOffset(clock.millis()) / 1000 / 60),
                d1.getDeviceObject().readProperty(PropertyIdentifier.utcOffset));
        assertEquals(Boolean.valueOf(tz.inDaylightTime(new java.util.Date(clock.millis()))),
                d1.getDeviceObject().readProperty(PropertyIdentifier.daylightSavingsStatus));

        d1.getRemoteDevice(2).get();
        d1.getRemoteDevice(3).get();
        assertEquals(new SequenceOf<>(new AddressBinding(d2.getId(), d2.getAllLocalAddresses()[0]),
                        new AddressBinding(d3.getId(), d3.getAllLocalAddresses()[0])),
                d1.getDeviceObject().readProperty(PropertyIdentifier.deviceAddressBinding));
    }

    @Test
    public void restartNotification() throws Exception {
        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        // Stop the device.
        d1.terminate();
        // Restart the device.
        d1.initialize(RestartReason.warmstart);
        // The time of device restart is set on device instantiation using the system clock, before the warp clock
        // is set. So, we can't use the warp clock time here, but have to instead get the property from the device.
        TimeStamp ts = d1.getDeviceObject().get(PropertyIdentifier.timeOfDeviceRestart);

        awaitEquals(1, listener::getNotifCount);
        CovNotifListener.Notif notif = listener.removeNotif();
        assertEquals(UnsignedInteger.ZERO, notif.subscriberProcessIdentifier());
        assertEquals(d1.getId(), notif.monitoredObjectIdentifier());
        assertEquals(UnsignedInteger.ZERO, notif.timeRemaining());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.systemStatus, DeviceStatus.operational),
                        new PropertyValue(PropertyIdentifier.timeOfDeviceRestart, ts),
                        new PropertyValue(PropertyIdentifier.lastRestartReason, RestartReason.warmstart)),
                notif.listOfValues());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicAlarms() throws Exception {
        final DeviceObject dev = d1.getDeviceObject();
        final NotificationClassObject nc =
                new NotificationClassObject(d1, 7, "nc7", 100, 5, 200, new EventTransitionBits(false, false, false));
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.FALSE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        dev.supportIntrinsicReporting(7, new EventTransitionBits(true, true, true), NotifyType.event);
        assertEquals(0, listener.getNotifCount());

        // Write a fault reliability value.
        dev.writePropertyInternal(PropertyIdentifier.reliability, Reliability.memberFault);
        assertEquals(EventState.fault, dev.readProperty(PropertyIdentifier.eventState));
        // Ensure that a proper looking event notification was received.
        awaitEquals(1, listener::getNotifCount);
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(dev.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) dev.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(7), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.event, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        assertEquals(new NotificationParameters(
                new ChangeOfReliabilityNotif(Reliability.memberFault, new StatusFlags(true, true, false, false),
                        new SequenceOf<>())), notif.eventValues());
    }
}
