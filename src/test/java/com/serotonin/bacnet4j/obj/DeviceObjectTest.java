package com.serotonin.bacnet4j.obj;

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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeviceObjectTest extends AbstractTest {
    static final Logger LOG = LoggerFactory.getLogger(DeviceObjectTest.class);

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
        clock.plus(minutes, MINUTES, 0);

        latch.await(1, TimeUnit.SECONDS);

        // Check the results.
        assertNotNull(d2Time.get());
        assertNotNull(d3Time.get());

        final int offsetHundredths = TimeZone.getDefault().getOffset(clock.millis()) / 10;
        final int adjustedHundredths = (d3Time.get().getTime().getHundredthInDay() + offsetHundredths + 8_640_000)
                % 8_640_000;
        assertEquals(new DateTime(d1), d2Time.get());
        assertEquals(d2Time.get().getTime().getHundredthInDay(), adjustedHundredths);

        assertEquals(false, d2Utc.get());
        assertEquals(true, d3Utc.get());
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
        assertEquals(
                new SequenceOf<>(new AddressBinding(d2.getId(), d2.getAllLocalAddresses()[0]),
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
        TimeStamp ts = new TimeStamp(new DateTime(d1));
        d1.initialize(RestartReason.warmstart);
        Thread.sleep(40);

        assertEquals(1, listener.size());
        final Map<String, Object> notif = listener.poll();
        assertEquals(UnsignedInteger.ZERO, notif.get("subscriberProcessIdentifier"));
        assertEquals(d1.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(UnsignedInteger.ZERO, notif.get("timeRemaining"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));

        PropertyValue expectedSystemStatus = new PropertyValue(PropertyIdentifier.systemStatus, DeviceStatus.operational);
        PropertyValue expectedRestartTime = new PropertyValue(PropertyIdentifier.timeOfDeviceRestart, ts);
        PropertyValue expectedRestartReason = new PropertyValue(PropertyIdentifier.lastRestartReason, RestartReason.warmstart);

        SequenceOf<PropertyValue> covProperties = (SequenceOf<PropertyValue>) notif.get("listOfValues");
        assertEquals(expectedSystemStatus, findProperty(PropertyIdentifier.systemStatus, covProperties));
        //Since we can't know down to the exact millisecond when the device started, lets just confirm the property exists
        assertNotNull(findProperty(PropertyIdentifier.timeOfDeviceRestart, covProperties));
        assertEquals(expectedRestartReason, findProperty(PropertyIdentifier.lastRestartReason, covProperties));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicAlarms() throws Exception {
        final DeviceObject dev = d1.getDeviceObject();
        final NotificationClassObject nc = new NotificationClassObject(d1, 7, "nc7", 100, 5, 200,
                new EventTransitionBits(false, false, false));
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.FALSE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        dev.supportIntrinsicReporting(7, new EventTransitionBits(true, true, true), NotifyType.event);
        assertEquals(0, listener.size());

        // Write a fault reliability value.
        dev.writePropertyInternal(PropertyIdentifier.reliability, Reliability.memberFault);
        assertEquals(EventState.fault, dev.readProperty(PropertyIdentifier.eventState));
        Thread.sleep(100);
        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.size());
        final Map<String, Object> notif = listener.poll();
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(dev.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) dev.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(7), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(5), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertEquals(null, notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new ChangeOfReliabilityNotif(Reliability.memberFault,
                        new StatusFlags(true, true, false, false), new SequenceOf<PropertyValue>())),
                notif.get("eventValues"));
    }
}
