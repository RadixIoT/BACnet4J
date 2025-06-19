package com.serotonin.bacnet4j.obj;

import static com.serotonin.bacnet4j.TestUtils.advanceClock;
import static com.serotonin.bacnet4j.TestUtils.assertBACnetServiceException;
import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LimitEnable;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.eventParameter.OutOfRange;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.notificationParameters.OutOfRangeNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class AnalogOutputObjectTest extends AbstractTest {
    private AnalogOutputObject ao;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        ao = new AnalogOutputObject(d1, 0, "ao", 50, EngineeringUnits.amperes, false, 45);
        nc = new NotificationClassObject(d1, 17, "nc17", 100, 5, 200, new EventTransitionBits(false, false, false));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        ao.supportIntrinsicReporting(60, 17, 100, 20, 5, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 180);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        quiesce();
        assertEquals(0, listener.getNotifCount());

        // Do a real state change. Write an out of range value. After 60s the alarm will be raised.
        ao.writePropertyInternal(PropertyIdentifier.presentValue, new Real(101));
        advanceClock(clock, 59500, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS, null,
                () -> assertEquals(EventState.normal, ao.readProperty(PropertyIdentifier.eventState)));
        quiesce();
        assertEquals(EventState.normal, ao.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.highLimit, ao.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), ao.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ao.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ao.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.outOfRange, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.highLimit, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(101), new StatusFlags(true, false, false, false), new Real(5),
                        new Real(100))), notif.eventValues());

        // Return to normal. After 180s the notification will be sent.
        ao.writePropertyInternal(PropertyIdentifier.presentValue, new Real(94));
        assertEquals(EventState.highLimit, ao.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());
        clock.plusMillis(179999);
        quiesce();
        assertEquals(EventState.highLimit, ao.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());
        clock.plusMillis(2);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, ao.readProperty(PropertyIdentifier.eventState));
        notif = listener.removeNotif();
        assertEquals(EventState.highLimit, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(94), new StatusFlags(false, false, false, false), new Real(5),
                        new Real(100))), notif.eventValues());
    }

    @Test
    public void propertyConformanceRequired() throws Exception {
        assertNotNull(ao.readProperty(PropertyIdentifier.objectIdentifier));
        assertNotNull(ao.readProperty(PropertyIdentifier.objectName));
        assertNotNull(ao.readProperty(PropertyIdentifier.objectType));
        assertNotNull(ao.readProperty(PropertyIdentifier.presentValue));
        assertNotNull(ao.readProperty(PropertyIdentifier.statusFlags));
        assertNotNull(ao.readProperty(PropertyIdentifier.eventState));
        assertNotNull(ao.readProperty(PropertyIdentifier.outOfService));
        assertNotNull(ao.readProperty(PropertyIdentifier.units));
        assertNotNull(ao.readProperty(PropertyIdentifier.priorityArray));
        assertNotNull(ao.readProperty(PropertyIdentifier.relinquishDefault));
        assertNotNull(ao.readProperty(PropertyIdentifier.propertyList));
        assertNotNull(ao.readProperty(PropertyIdentifier.currentCommandPriority));
    }

    @Test
    public void propertyConformanceEditableWhenOutOfService() throws BACnetServiceException {
        // Should not be writable while in service
        assertBACnetServiceException(() -> ao.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.reliability, null, Reliability.unreliableOther, null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        // Should be writable while out of service.
        ao.writeProperty(null, PropertyIdentifier.outOfService, Boolean.TRUE);
        ao.writeProperty(new ValueSource(TestNetworkUtils.toAddress(1)),
                new PropertyValue(PropertyIdentifier.presentValue, null, new Real(51), null));
        ao.writeProperty(null,
                new PropertyValue(PropertyIdentifier.reliability, null, Reliability.unreliableOther, null));
    }

    @Test
    public void propertyConformanceRequiredWhenCOVReporting() throws Exception {
        ao.supportCovReporting(1);
        assertNotNull(ao.readProperty(PropertyIdentifier.covIncrement));
    }

    @Test
    public void propertyConformanceRequiredWhenIntrinsicReporting() throws Exception {
        ao.supportIntrinsicReporting(30, 17, 60, 40, 1, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 10);
        assertNotNull(ao.readProperty(PropertyIdentifier.timeDelay));
        assertNotNull(ao.readProperty(PropertyIdentifier.notificationClass));
        assertNotNull(ao.readProperty(PropertyIdentifier.highLimit));
        assertNotNull(ao.readProperty(PropertyIdentifier.lowLimit));
        assertNotNull(ao.readProperty(PropertyIdentifier.deadband));
        assertNotNull(ao.readProperty(PropertyIdentifier.limitEnable));
        assertNotNull(ao.readProperty(PropertyIdentifier.eventEnable));
        assertNotNull(ao.readProperty(PropertyIdentifier.ackedTransitions));
        assertNotNull(ao.readProperty(PropertyIdentifier.notifyType));
        assertNotNull(ao.readProperty(PropertyIdentifier.eventTimeStamps));
        assertNotNull(ao.readProperty(PropertyIdentifier.eventDetectionEnable));
    }

    @Test
    public void propertyConformanceForbiddenWhenNotIntrinsicReporting() throws Exception {
        assertNull(ao.readProperty(PropertyIdentifier.timeDelay));
        assertNull(ao.readProperty(PropertyIdentifier.notificationClass));
        assertNull(ao.readProperty(PropertyIdentifier.highLimit));
        assertNull(ao.readProperty(PropertyIdentifier.lowLimit));
        assertNull(ao.readProperty(PropertyIdentifier.deadband));
        assertNull(ao.readProperty(PropertyIdentifier.limitEnable));
        assertNull(ao.readProperty(PropertyIdentifier.eventEnable));
        assertNull(ao.readProperty(PropertyIdentifier.ackedTransitions));
        assertNull(ao.readProperty(PropertyIdentifier.notifyType));
        assertNull(ao.readProperty(PropertyIdentifier.eventTimeStamps));
        assertNull(ao.readProperty(PropertyIdentifier.eventMessageTexts));
        assertNull(ao.readProperty(PropertyIdentifier.eventMessageTextsConfig));
        assertNull(ao.readProperty(PropertyIdentifier.eventDetectionEnable));
        assertNull(ao.readProperty(PropertyIdentifier.eventAlgorithmInhibitRef));
        assertNull(ao.readProperty(PropertyIdentifier.eventAlgorithmInhibit));
        assertNull(ao.readProperty(PropertyIdentifier.timeDelayNormal));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReporting() throws Exception {
        final DeviceObjectPropertyReference ref =
                new DeviceObjectPropertyReference(1, ao.getId(), PropertyIdentifier.presentValue);
        final EventEnrollmentObject ee = new EventEnrollmentObject(d1, 0, "ee", ref, NotifyType.alarm,
                new EventParameter(new OutOfRange(new UnsignedInteger(30), new Real(40), new Real(60), new Real(2))),
                new EventTransitionBits(true, true, true), 17, 1000, null, null);

        // Set up the notification destination
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Ensure that initializing the event enrollment object didn't fire any notifications.
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        //
        // Go to high limit.
        ao.writePropertyInternal(PropertyIdentifier.presentValue, new Real(70));
        // Allow the EE to poll
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until just before the time delay.
        clock.plusMillis(29500);
        quiesce();
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until after the time delay.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.highLimit, ee.readProperty(PropertyIdentifier.eventState));

        // Ensure that a proper looking event notification was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.highLimit.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.outOfRange, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.highLimit, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(70), new StatusFlags(false, false, false, false), new Real(2),
                        new Real(60))), notif.eventValues());

        //
        // Return to normal
        ao.writePropertyInternal(PropertyIdentifier.presentValue, new Real(40));
        // Allow the EE to poll
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.highLimit, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until just before the time delay.
        clock.plusMillis(29500);
        quiesce();
        assertEquals(EventState.highLimit, ee.readProperty(PropertyIdentifier.eventState));
        // Wait until after the time delay.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));

        // Ensure that a proper looking event notification was received.
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(d1.getId(), notif.initiatingDevice());
        assertEquals(ee.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.outOfRange, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.highLimit, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(40), new StatusFlags(false, false, false, false), new Real(2),
                        new Real(60))), notif.eventValues());
    }
}
