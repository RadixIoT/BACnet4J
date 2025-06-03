package com.serotonin.bacnet4j.obj;

import static com.serotonin.bacnet4j.TestUtils.assertBACnetServiceException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LimitEnable;
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
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.notificationParameters.OutOfRangeNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class AnalogInputObjectTest extends AbstractTest {
    private AnalogInputObject ai;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        ai = new AnalogInputObject(d1, 0, "ai0", 50, EngineeringUnits.amperes, false);
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

        ai.supportIntrinsicReporting(1, 17, 100, 20, 5, 120, 0, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 2);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        assertEquals(0, listener.getNotifCount());

        // Write a different normal value.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(60));
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        // Ensure that no notifications are sent.
        assertEquals(0, listener.getNotifCount());

        // Set an out of range value and then set back to normal before the time delay.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(110));
        clock.plus(500, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(90));
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.

        // Do a real state change. Write an out of range value. After 1 second the alarm will be raised.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(10));
        clock.plus(500, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.lowLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), ai.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(ai.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ai.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.offnormal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(100), notif.get("priority"));
        assertEquals(EventType.outOfRange, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.lowLimit, notif.get("toState"));
        assertEquals(new NotificationParameters(new OutOfRangeNotif(new Real(10),
                new StatusFlags(true, false, false, false), new Real(5), new Real(20))), notif.get("eventValues"));

        // Disable low limit checking. Will return to normal immediately.
        ai.writePropertyInternal(PropertyIdentifier.limitEnable, new LimitEnable(false, true));
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState));
        Thread.sleep(100);
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(ai.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ai.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(200), notif.get("priority"));
        assertEquals(EventType.outOfRange, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.lowLimit, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new OutOfRangeNotif(new Real(10),
                        new StatusFlags(false, false, false, false), new Real(5), new Real(20))),
                notif.get("eventValues"));

        // Re-enable low limit checking. Will return to low-limit after 1 second.
        ai.writePropertyInternal(PropertyIdentifier.limitEnable, new LimitEnable(true, true));
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState));
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.lowLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(EventType.outOfRange, notif.get("eventType"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.lowLimit, notif.get("toState"));
        assertEquals(new NotificationParameters(new OutOfRangeNotif(new Real(10),
                new StatusFlags(true, false, false, false), new Real(5), new Real(20))), notif.get("eventValues"));

        // Go to a high limit. Will change to high-limit after 1 second.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(110));
        assertEquals(EventState.lowLimit, ai.readProperty(PropertyIdentifier.eventState));
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(EventState.lowLimit, notif.get("fromState"));
        assertEquals(EventState.highLimit, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new OutOfRangeNotif(new Real(110),
                        new StatusFlags(true, false, false, false), new Real(5), new Real(100))),
                notif.get("eventValues"));

        // Reduce to within the deadband. No notification.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(95));
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        clock.plus(1100, TimeUnit.MILLISECONDS, 1100, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        // Reduce to below the deadband. Return to normal after 2 seconds.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(94));
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());
        clock.plus(1500, TimeUnit.MILLISECONDS, 1500, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());
        clock.plus(600, TimeUnit.MILLISECONDS, 600, TimeUnit.MILLISECONDS, 0, 40);
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(EventState.highLimit, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new OutOfRangeNotif(new Real(94),
                        new StatusFlags(false, false, false, false), new Real(5), new Real(100))),
                notif.get("eventValues"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fault() throws Exception {
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        ai.supportIntrinsicReporting(1, 17, 100, 20, 5, 120, 0, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 2);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        assertEquals(0, listener.getNotifCount());

        // Write a fault value.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(-5));
        Thread.sleep(40);
        assertEquals(EventState.fault, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), ai.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        final Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(ai.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ai.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(17), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(5), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.FALSE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        assertEquals(
                new NotificationParameters(
                        new ChangeOfReliabilityNotif(Reliability.underRange, new StatusFlags(true, true, false, false),
                                new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(-5))))),
                notif.get("eventValues"));
    }

    @Test
    public void propertyConformanceRequired() throws Exception {
        assertNotNull(ai.readProperty(PropertyIdentifier.objectIdentifier));
        assertNotNull(ai.readProperty(PropertyIdentifier.objectName));
        assertNotNull(ai.readProperty(PropertyIdentifier.objectType));
        assertNotNull(ai.readProperty(PropertyIdentifier.presentValue));
        assertNotNull(ai.readProperty(PropertyIdentifier.statusFlags));
        assertNotNull(ai.readProperty(PropertyIdentifier.eventState));
        assertNotNull(ai.readProperty(PropertyIdentifier.outOfService));
        assertNotNull(ai.readProperty(PropertyIdentifier.units));
        assertNotNull(ai.readProperty(PropertyIdentifier.propertyList));
    }

    @Test
    public void propertyConformanceEditableWhenOutOfService() throws BACnetServiceException {
        // Should not be writable while in service
        assertBACnetServiceException(
                () -> ai.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.presentValue, null, new Real(51), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        // Should be writable while out of service.
        ai.writeProperty(null, PropertyIdentifier.outOfService, Boolean.TRUE);
        ai.writeProperty(null, new PropertyValue(PropertyIdentifier.presentValue, null, new Real(51), null));
    }

    @Test
    public void propertyConformanceReadOnly() {
        assertBACnetServiceException(
                () -> ai.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.eventMessageTexts, new UnsignedInteger(2),
                                new CharacterString("should fail"), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
    }

    @Test
    public void propertyConformanceRequiredWhenCOVReporting() throws Exception {
        ai.supportCovReporting(1);
        assertNotNull(ai.readProperty(PropertyIdentifier.covIncrement));
    }

    @Test
    public void propertyConformanceRequiredWhenIntrinsicReporting() throws Exception {
        ai.supportIntrinsicReporting(30, 17, 60, 40, 1, 70, 30, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 10);
        assertNotNull(ai.readProperty(PropertyIdentifier.timeDelay));
        assertNotNull(ai.readProperty(PropertyIdentifier.notificationClass));
        assertNotNull(ai.readProperty(PropertyIdentifier.highLimit));
        assertNotNull(ai.readProperty(PropertyIdentifier.lowLimit));
        assertNotNull(ai.readProperty(PropertyIdentifier.deadband));
        assertNotNull(ai.readProperty(PropertyIdentifier.faultHighLimit));
        assertNotNull(ai.readProperty(PropertyIdentifier.faultLowLimit));
        assertNotNull(ai.readProperty(PropertyIdentifier.limitEnable));
        assertNotNull(ai.readProperty(PropertyIdentifier.eventEnable));
        assertNotNull(ai.readProperty(PropertyIdentifier.ackedTransitions));
        assertNotNull(ai.readProperty(PropertyIdentifier.notifyType));
        assertNotNull(ai.readProperty(PropertyIdentifier.eventTimeStamps));
        assertNotNull(ai.readProperty(PropertyIdentifier.eventDetectionEnable));
    }

    @Test
    public void propertyConformanceForbiddenWhenNotIntrinsicReporting() throws Exception {
        assertNull(ai.readProperty(PropertyIdentifier.timeDelay));
        assertNull(ai.readProperty(PropertyIdentifier.notificationClass));
        assertNull(ai.readProperty(PropertyIdentifier.highLimit));
        assertNull(ai.readProperty(PropertyIdentifier.lowLimit));
        assertNull(ai.readProperty(PropertyIdentifier.deadband));
        assertNull(ai.readProperty(PropertyIdentifier.faultHighLimit));
        assertNull(ai.readProperty(PropertyIdentifier.faultLowLimit));
        assertNull(ai.readProperty(PropertyIdentifier.limitEnable));
        assertNull(ai.readProperty(PropertyIdentifier.eventEnable));
        assertNull(ai.readProperty(PropertyIdentifier.ackedTransitions));
        assertNull(ai.readProperty(PropertyIdentifier.notifyType));
        assertNull(ai.readProperty(PropertyIdentifier.eventTimeStamps));
        assertNull(ai.readProperty(PropertyIdentifier.eventMessageTexts));
        assertNull(ai.readProperty(PropertyIdentifier.eventMessageTextsConfig));
        assertNull(ai.readProperty(PropertyIdentifier.eventDetectionEnable));
        assertNull(ai.readProperty(PropertyIdentifier.eventAlgorithmInhibitRef));
        assertNull(ai.readProperty(PropertyIdentifier.eventAlgorithmInhibit));
        assertNull(ai.readProperty(PropertyIdentifier.timeDelayNormal));
    }
}
