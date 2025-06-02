package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyRequest;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVRequest;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LimitEnable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.Prescale;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.Scale;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.notificationParameters.OutOfRangeNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class PulseConverterObjectTest extends AbstractTest {
    static final Logger LOG = LoggerFactory.getLogger(AccumulatorObjectTest.class);

    private PulseConverterObject pc;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        pc = new PulseConverterObject(d1, 0, "pc", 0, 7.5F, EngineeringUnits.amperes, false);
        nc = new NotificationClassObject(d1, 54, "nc54", 100, 5, 200, new EventTransitionBits(true, true, true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        // Default the present value by setting adjust value.
        pc.writePropertyInternal(PropertyIdentifier.adjustValue, new Real(-50));
        assertEquals(new Real(45), pc.get(PropertyIdentifier.presentValue));

        // Set up the notification destination
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Set up intrinsic reporting on the pulse converter.
        // Limits are 30/70. Time delay is 10s. Time delay normal is null, and so should default to time delay.
        pc.supportIntrinsicReporting(70, 30, 3, 10, null, 54, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.event);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        assertEquals(0, listener.notifs.size());

        // strictly required by the time delay.
        pc.pulse();
        assertEquals(new Real(52.5F), pc.readProperty(PropertyIdentifier.presentValue));
        assertEquals(new UnsignedInteger(7), pc.readProperty(PropertyIdentifier.count));
        assertEquals(new DateTime(d1), pc.readProperty(PropertyIdentifier.updateTime));
        assertEquals(EventState.normal, pc.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        // Ensure that no notifications are sent.
        assertEquals(0, listener.notifs.size());

        //
        // Write pulses to go out of range.
        pc.pulses(3);
        clock.plusSeconds(11);
        TestUtils.awaitCondition(() -> pc.readProperty(PropertyIdentifier.eventState) == EventState.highLimit, 5000);
        assertEquals(new StatusFlags(true, false, false, false), pc.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        TestUtils.awaitCondition(() -> listener.notifs.size() == 1, 5000);
        Map<String, Object> notif = listener.notifs.remove(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(pc.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) pc.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.offnormal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(54), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(100), notif.get("priority"));
        assertEquals(EventType.outOfRange, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.highLimit, notif.get("toState"));
        assertEquals(new NotificationParameters(new OutOfRangeNotif(new Real(75),
                new StatusFlags(true, false, false, false), new Real(3), new Real(70))), notif.get("eventValues"));

        //
        // Use adjust value to put back into normal.
        clock.plusSeconds(5);
        pc.writePropertyInternal(PropertyIdentifier.adjustValue, new Real(45));
        assertEquals(new Real(30), pc.get(PropertyIdentifier.presentValue));
        assertEquals(new UnsignedInteger(4), pc.readProperty(PropertyIdentifier.count));
        assertEquals(new DateTime(d1), pc.readProperty(PropertyIdentifier.updateTime));
        assertEquals(new UnsignedInteger(10), pc.readProperty(PropertyIdentifier.countBeforeChange));
        assertEquals(new DateTime(d1), pc.readProperty(PropertyIdentifier.countChangeTime));

        // Ensure that after 9 seconds we are still in high limit.
        clock.plusSeconds(9);
        assertEquals(EventState.highLimit, pc.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.notifs.size());

        clock.plusSeconds(2);
        TestUtils.awaitCondition(() -> listener.notifs.size() == 1, 5000);
        assertEquals(EventState.normal, pc.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), pc.readProperty(PropertyIdentifier.statusFlags));
        notif = listener.notifs.remove(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(pc.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) pc.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(54), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(200), notif.get("priority"));
        assertEquals(EventType.outOfRange, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.highLimit, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new OutOfRangeNotif(new Real(30),
                        new StatusFlags(false, false, false, false), new Real(3), new Real(70))),
                notif.get("eventValues"));

        // Remove the object
        d1.removeObject(pc.getId());
    }

    @Test
    public void propertyConformanceRequired() throws Exception {
        assertNotNull(pc.readProperty(PropertyIdentifier.objectIdentifier));
        assertNotNull(pc.readProperty(PropertyIdentifier.objectName));
        assertNotNull(pc.readProperty(PropertyIdentifier.objectType));
        assertNotNull(pc.readProperty(PropertyIdentifier.presentValue));
        assertNotNull(pc.readProperty(PropertyIdentifier.statusFlags));
        assertNotNull(pc.readProperty(PropertyIdentifier.eventState));
        assertNotNull(pc.readProperty(PropertyIdentifier.outOfService));
        assertNotNull(pc.readProperty(PropertyIdentifier.units));
        assertNotNull(pc.readProperty(PropertyIdentifier.scaleFactor));
        assertNotNull(pc.readProperty(PropertyIdentifier.count));
        assertNotNull(pc.readProperty(PropertyIdentifier.updateTime));
        assertNotNull(pc.readProperty(PropertyIdentifier.countChangeTime));
        assertNotNull(pc.readProperty(PropertyIdentifier.countBeforeChange));
        assertNotNull(pc.readProperty(PropertyIdentifier.propertyList));
    }

    @Test
    public void propertyConformanceEditableWhenOutOfService() throws BACnetServiceException {
        // Should not be writable while in service
        TestUtils.assertBACnetServiceException(
                () -> pc.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.presentValue, null, new Real(51), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        // Should be writable while out of service.
        pc.writeProperty(null, PropertyIdentifier.outOfService, Boolean.TRUE);
        pc.writeProperty(null, new PropertyValue(PropertyIdentifier.presentValue, null, new Real(51), null));
    }

    @Test
    public void propertyConformanceReadOnly() {
        TestUtils.assertBACnetServiceException(
                () -> pc.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.count, null, DateTime.UNSPECIFIED, null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        TestUtils.assertBACnetServiceException(
                () -> pc.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.updateTime, new UnsignedInteger(2),
                                new CharacterString("should fail"), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        TestUtils.assertBACnetServiceException(
                () -> pc.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.ackedTransitions, new UnsignedInteger(2),
                                new CharacterString("should fail"), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        TestUtils.assertBACnetServiceException(
                () -> pc.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.eventMessageTexts, new UnsignedInteger(2),
                                new CharacterString("should fail"), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
    }

    @Test
    public void propertyConformanceForbiddenWhenNotCov() throws Exception {
        assertNull(pc.readProperty(PropertyIdentifier.covIncrement));
        assertNull(pc.readProperty(PropertyIdentifier.covPeriod));
    }

    @Test
    public void propertyConformanceRequiredWhenCov() throws Exception {
        pc.supportCovReporting(2, 0);
        assertNotNull(pc.readProperty(PropertyIdentifier.covIncrement));
        assertNotNull(pc.readProperty(PropertyIdentifier.covPeriod));
    }

    @Test
    public void propertyConformanceRequiredWhenIntrinsicReporting() throws Exception {
        pc.supportIntrinsicReporting(90, 10, 10, 10, new UnsignedInteger(15), 54, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm);
        assertNotNull(pc.readProperty(PropertyIdentifier.timeDelay));
        assertNotNull(pc.readProperty(PropertyIdentifier.notificationClass));
        assertNotNull(pc.readProperty(PropertyIdentifier.highLimit));
        assertNotNull(pc.readProperty(PropertyIdentifier.lowLimit));
        assertNotNull(pc.readProperty(PropertyIdentifier.deadband));
        assertNotNull(pc.readProperty(PropertyIdentifier.limitEnable));
        assertNotNull(pc.readProperty(PropertyIdentifier.eventEnable));
        assertNotNull(pc.readProperty(PropertyIdentifier.ackedTransitions));
        assertNotNull(pc.readProperty(PropertyIdentifier.notifyType));
        assertNotNull(pc.readProperty(PropertyIdentifier.eventTimeStamps));
        assertNotNull(pc.readProperty(PropertyIdentifier.eventDetectionEnable));
    }

    @Test
    public void propertyConformanceForbiddenWhenNotIntrinsicReporting() throws Exception {
        assertNull(pc.readProperty(PropertyIdentifier.timeDelay));
        assertNull(pc.readProperty(PropertyIdentifier.notificationClass));
        assertNull(pc.readProperty(PropertyIdentifier.highLimit));
        assertNull(pc.readProperty(PropertyIdentifier.lowLimit));
        assertNull(pc.readProperty(PropertyIdentifier.deadband));
        assertNull(pc.readProperty(PropertyIdentifier.limitEnable));
        assertNull(pc.readProperty(PropertyIdentifier.eventEnable));
        assertNull(pc.readProperty(PropertyIdentifier.ackedTransitions));
        assertNull(pc.readProperty(PropertyIdentifier.notifyType));
        assertNull(pc.readProperty(PropertyIdentifier.eventTimeStamps));
        assertNull(pc.readProperty(PropertyIdentifier.eventMessageTexts));
        assertNull(pc.readProperty(PropertyIdentifier.eventMessageTextsConfig));
        assertNull(pc.readProperty(PropertyIdentifier.eventDetectionEnable));
        assertNull(pc.readProperty(PropertyIdentifier.eventAlgorithmInhibitRef));
        assertNull(pc.readProperty(PropertyIdentifier.eventAlgorithmInhibit));
        assertNull(pc.readProperty(PropertyIdentifier.timeDelayNormal));
    }

    @Test
    public void covNotifications() throws Exception {
        pc.supportCovReporting(10, 0);

        // Create a COV listener to catch the notifications.
        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        //
        // Subscribe for notifications. Doing so should cause an initial notification to be sent.
        d2.send(rd1,
                new SubscribeCOVRequest(new UnsignedInteger(987), pc.getId(), Boolean.FALSE, new UnsignedInteger(600)))
                .get();
        TestUtils.awaitCondition(() -> listener.notifs.size() == 1, 5000);
        Map<String, Object> notif = listener.notifs.remove(0);
        assertEquals(new UnsignedInteger(987), notif.get("subscriberProcessIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(pc.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(new UnsignedInteger(600), notif.get("timeRemaining"));
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(0)),
                new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                new PropertyValue(PropertyIdentifier.updateTime, new DateTime(d1))), notif.get("listOfValues"));

        //
        // Add a pulse. No notification should be sent.
        pc.pulse();
        Thread.sleep(100);
        assertEquals(0, listener.notifs.size());

        //
        // Add another pulse. Now a notification should be sent. Advance the clock to test time remaining
        // and the update time.
        clock.plusMinutes(2);
        pc.pulse();
        TestUtils.awaitCondition(() -> listener.notifs.size() == 1, 5000);
        notif = listener.notifs.remove(0);
        assertEquals(new UnsignedInteger(987), notif.get("subscriberProcessIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(pc.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(new UnsignedInteger(480), notif.get("timeRemaining"));
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(15)),
                new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                new PropertyValue(PropertyIdentifier.updateTime, new DateTime(d1))), notif.get("listOfValues"));
    }

    @Test
    public void covNotificationsWithCovPeriod() throws Exception {
        // Use an increment of 10, and a period of 60s
        pc.supportCovReporting(10, 60);

        // Create a COV listener to catch the notifications.
        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        //
        // Subscribe for notifications. Doing so should cause an initial notification to be sent.
        d2.send(rd1,
                new SubscribeCOVRequest(new UnsignedInteger(988), pc.getId(), Boolean.FALSE, new UnsignedInteger(6000)))
                .get();
        TestUtils.awaitCondition(() -> listener.notifs.size() == 1, 5000);
        Map<String, Object> notif = listener.notifs.remove(0);
        assertEquals(new UnsignedInteger(988), notif.get("subscriberProcessIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(pc.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(new UnsignedInteger(6000), notif.get("timeRemaining"));
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(0)),
                new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                new PropertyValue(PropertyIdentifier.updateTime, new DateTime(d1))), notif.get("listOfValues"));

        //
        // Subscribe to a property as well to ensure that periodic notification are not sent.
        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(989), pc.getId(), Boolean.TRUE,
                new UnsignedInteger(6000), new PropertyReference(PropertyIdentifier.reliability), null)).get();
        TestUtils.assertSize(listener.notifs, 1, 500);
        notif = listener.notifs.remove(0);
        assertEquals(new UnsignedInteger(989), notif.get("subscriberProcessIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(pc.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(new UnsignedInteger(6000), notif.get("timeRemaining"));
        assertEquals(
                new SequenceOf<>(new PropertyValue(PropertyIdentifier.reliability, Reliability.noFaultDetected),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.get("listOfValues"));

        //
        // Advance the clock 40s and add a pulse. No notification should be sent.
        clock.plusSeconds(40);
        final DateTime ts2 = new DateTime(d1);
        pc.pulse();
        Thread.sleep(410);
        assertEquals(0, listener.notifs.size());

        //
        // Advance the clock 21s. The periodic notification should be received.
        clock.plusSeconds(21);
        TestUtils.awaitCondition(() -> listener.notifs.size() == 1, 5000);
        notif = listener.notifs.remove(0);
        assertEquals(new UnsignedInteger(988), notif.get("subscriberProcessIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(pc.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(new UnsignedInteger(5939), notif.get("timeRemaining"));
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(7.5F)),
                new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                new PropertyValue(PropertyIdentifier.updateTime, ts2)), notif.get("listOfValues"));

        //
        // Add another pulse. No notification should be sent because the tolerance was reset by the period notification.
        clock.plusSeconds(30);
        final DateTime ts3 = new DateTime(d1);
        pc.pulse();
        Thread.sleep(100);
        assertEquals(0, listener.notifs.size());

        //
        // Change the COV period to 3m, and ensure that no notifications are sent before that time.
        pc.writePropertyInternal(PropertyIdentifier.covPeriod, new UnsignedInteger(180));
        clock.plusSeconds(179);
        Thread.sleep(100);
        assertEquals(0, listener.notifs.size());

        // Advance the time to get a periodic notification.
        clock.plusSeconds(1);
        TestUtils.awaitCondition(() -> listener.notifs.size() == 1, 5000);
        notif = listener.notifs.remove(0);
        assertEquals(new UnsignedInteger(988), notif.get("subscriberProcessIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(pc.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(new UnsignedInteger(5729), notif.get("timeRemaining"));
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(15F)),
                new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                new PropertyValue(PropertyIdentifier.updateTime, ts3)), notif.get("listOfValues"));
    }

    @Test
    public void polling() throws Exception {
        // Ensure that before the support is indicated, the property is not writable.
        TestUtils.assertBACnetServiceException(
                () -> pc.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.inputReference,
                                new ObjectPropertyReference(new ObjectIdentifier(ObjectType.accumulator, 0),
                                        PropertyIdentifier.presentValue))),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        pc.supportInputReference(new ObjectPropertyReference(new ObjectIdentifier(ObjectType.accumulator, 0),
                PropertyIdentifier.presentValue), 5000);

        // Advance the clock 6 seconds, to do a poll. The reliability should be in fault.
        clock.plusSeconds(6);
        TestUtils.awaitCondition(() -> pc.get(PropertyIdentifier.reliability) == Reliability.configurationError, 5000);
        assertEquals(new StatusFlags(false, true, false, false), pc.get(PropertyIdentifier.statusFlags));

        // Add the accumulator object. Polling should now succeed.
        final AccumulatorObject a = new AccumulatorObject(d1, 0, "a", 50, 0, EngineeringUnits.amperes, false,
                new Scale(new SignedInteger(10)), new Prescale(new UnsignedInteger(1), new UnsignedInteger(1)), 1000,
                5);
        clock.plusSeconds(5);
        TestUtils.awaitCondition(() -> pc.get(PropertyIdentifier.reliability) == Reliability.noFaultDetected, 5000);
        assertEquals(new StatusFlags(false, false, false, false), pc.get(PropertyIdentifier.statusFlags));

        // Point to a non-existent property.
        pc.writePropertyInternal(PropertyIdentifier.inputReference,
                new ObjectPropertyReference(a.getId(), PropertyIdentifier.stateText));
        clock.plusSeconds(5);
        TestUtils.awaitCondition(() -> pc.get(PropertyIdentifier.reliability) == Reliability.configurationError, 5000);
        assertEquals(new StatusFlags(false, true, false, false), pc.get(PropertyIdentifier.statusFlags));

        // Point to a property of the wrong type.
        pc.writePropertyInternal(PropertyIdentifier.inputReference,
                new ObjectPropertyReference(a.getId(), PropertyIdentifier.objectName));
        clock.plusSeconds(5);
        Thread.sleep(100);
        assertEquals(Reliability.configurationError, pc.get(PropertyIdentifier.reliability));
        assertEquals(new StatusFlags(false, true, false, false), pc.get(PropertyIdentifier.statusFlags));

        // Point back to the correct property.
        pc.writePropertyInternal(PropertyIdentifier.inputReference,
                new ObjectPropertyReference(a.getId(), PropertyIdentifier.presentValue));
        clock.plusSeconds(5);
        TestUtils.awaitCondition(() -> pc.get(PropertyIdentifier.reliability) == Reliability.noFaultDetected, 5000);
        assertEquals(new StatusFlags(false, false, false, false), pc.get(PropertyIdentifier.statusFlags));

        //
        // Try a pulse
        try {
            pc.pulse();
            fail("Should have thrown IllegalStateException");
        } catch (@SuppressWarnings("unused") final IllegalStateException e) {
            // Expected
        }

        //
        // Allow another poll and check the present value.
        assertEquals(new Real(0), pc.get(PropertyIdentifier.presentValue));
        a.pulses(50);
        clock.plusSeconds(5);
        TestUtils.awaitCondition(() -> TestUtils.equalsRegardingNull(pc.get(PropertyIdentifier.presentValue), new Real(375)), 5000);

        //
        // Change to out of service, add some pulses, advance the clock, and check that the present
        // value did not change.
        pc.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.TRUE);
        a.pulses(50);
        clock.plusSeconds(5);
        Thread.sleep(100);
        assertEquals(new Real(375), pc.get(PropertyIdentifier.presentValue));

        //
        // Change back to in service, add some pulses, advance the clock, and check that all the pulses
        // were added in.
        pc.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.FALSE);
        a.pulses(50);
        clock.plusSeconds(5);
        TestUtils.awaitCondition(() -> TestUtils.equalsRegardingNull(pc.get(PropertyIdentifier.presentValue), new Real(1125)), 5000);

        // Remove the object.
        d1.removeObject(pc.getId());
    }
}
