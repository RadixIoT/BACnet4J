package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.AccumulatorObject.ValueSetWrite;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.FaultParameter;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultOutOfRange;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultOutOfRange.FaultNormalValue;
import com.serotonin.bacnet4j.type.constructed.LimitEnable;
import com.serotonin.bacnet4j.type.constructed.Prescale;
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
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.eventParameter.UnsignedRange;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.notificationParameters.UnsignedRangeNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * @author Matthew
 */
public class AccumulatorObjectTest extends AbstractTest {
    private AccumulatorObject a;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        a = new AccumulatorObject(d1, 0, "a0", 0, 0, EngineeringUnits.amperes, false, new Scale(new Real(1)),
                new Prescale(new UnsignedInteger(2), new UnsignedInteger(15)), 200, 1);
        nc = new NotificationClassObject(d1, 54, "nc54", 100, 5, 200, new EventTransitionBits(true, true, true));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        // Default the pulse rate
        a.set(PropertyIdentifier.pulseRate, new UnsignedInteger(40));

        // Set up the notification destination
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Set up intrinsic reporting on the accumulator.
        // Monitoring interval is 1s.
        // Limits are 30/50. Fault limits are 20/60.
        // Time delay is 3s. Time delay normal is 5s.
        a.supportIntrinsicReporting(50, 30, 60, 20, 3, new UnsignedInteger(5), 54, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.event);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        assertEquals(0, listener.getNotifCount());

        // Advance the clock half a second so that pulses are out of time with scheduled tasks.
        clock.plusMillis(500);

        //
        // Write enough pulses to stay normal. NOTE: the pulse rate is updated by the scheduled task in the
        // accumulator, which doesn't run until the next second. To get it to run, we add one more pulse than
        // strictly required by the time delay.
        doPulses(40, 41, 42, 41, 40, 40);
        assertEquals(new UnsignedInteger(32), a.readProperty(PropertyIdentifier.presentValue));
        assertEquals(8, a.getAccumulation());
        assertEquals(new UnsignedInteger(40), a.readProperty(PropertyIdentifier.pulseRate));
        assertEquals(EventState.normal, a.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        // Ensure that no notifications are sent.
        assertEquals(0, listener.getNotifCount());

        //
        // Write pulses to go out of range value and then set back to normal before the time delay.
        doPulses(55, 53);
        assertEquals(new UnsignedInteger(53), a.readProperty(PropertyIdentifier.pulseRate));
        assertEquals(EventState.normal, a.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.

        doPulses(35, 38, 42, 49, 35);
        assertEquals(new UnsignedInteger(35), a.readProperty(PropertyIdentifier.pulseRate));
        assertEquals(EventState.normal, a.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.

        //
        // Do a real state change. Write an out of range value. After 3s the alarm will be raised.
        doPulses(25, 23);
        assertEquals(EventState.normal, a.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        doPulses(29, 28);
        assertEquals(EventState.lowLimit, a.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), a.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(a.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) a.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.offnormal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(54), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(100), notif.get("priority"));
        assertEquals(EventType.unsignedRange, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.lowLimit, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new UnsignedRangeNotif(new UnsignedInteger(28),
                        new StatusFlags(true, false, false, false), new UnsignedInteger(30))),
                notif.get("eventValues"));

        // Disable low limit checking. Will return to normal immediately.
        a.writePropertyInternal(PropertyIdentifier.limitEnable, new LimitEnable(false, true));
        assertEquals(EventState.normal, a.readProperty(PropertyIdentifier.eventState));
        Thread.sleep(40);
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(a.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) a.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.normal.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(54), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(200), notif.get("priority"));
        assertEquals(EventType.unsignedRange, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.event, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.lowLimit, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new UnsignedRangeNotif(new UnsignedInteger(28),
                        new StatusFlags(false, false, false, false), new UnsignedInteger(30))),
                notif.get("eventValues"));

        // Re-enable low limit checking. Will return to low-limit after 3s.
        a.writePropertyInternal(PropertyIdentifier.limitEnable, new LimitEnable(true, true));
        assertEquals(EventState.normal, a.readProperty(PropertyIdentifier.eventState));
        doPulses(27, 27, 27, 27);
        assertEquals(EventState.lowLimit, a.readProperty(PropertyIdentifier.eventState));
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(EventType.unsignedRange, notif.get("eventType"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.lowLimit, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new UnsignedRangeNotif(new UnsignedInteger(27),
                        new StatusFlags(true, false, false, false), new UnsignedInteger(30))),
                notif.get("eventValues"));

        // Go past the fault high limit. Will change to fault immediately.
        doPulses(61);
        assertEquals(EventState.fault, a.readProperty(PropertyIdentifier.eventState));
        Thread.sleep(40);
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(EventState.lowLimit, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        assertEquals(
                new NotificationParameters(
                        new ChangeOfReliabilityNotif(Reliability.overRange, new StatusFlags(true, true, false, false),
                                new SequenceOf<>( //
                                        new PropertyValue(PropertyIdentifier.pulseRate, new UnsignedInteger(61)), //
                                        new PropertyValue(PropertyIdentifier.presentValue, new UnsignedInteger(110))))),
                notif.get("eventValues"));

        // Reduce to normal. Return to normal immediately.
        doPulses(52);
        assertEquals(EventState.normal, a.readProperty(PropertyIdentifier.eventState));
        Thread.sleep(40);
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(EventState.fault, notif.get("fromState"));
        assertEquals(EventState.normal, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new ChangeOfReliabilityNotif(Reliability.noFaultDetected,
                        new StatusFlags(false, false, false, false),
                        new SequenceOf<>( //
                                new PropertyValue(PropertyIdentifier.pulseRate, new UnsignedInteger(52)), //
                                new PropertyValue(PropertyIdentifier.presentValue, new UnsignedInteger(116))))),
                notif.get("eventValues"));

        // Remove the object.
        d1.removeObject(a.getId());
    }

    private void doPulses(final int... pulses) {
        for (final int i : pulses) {
            a.pulses(i);
            clock.plus(1, TimeUnit.SECONDS, 1, TimeUnit.SECONDS, 20, 0);
        }
    }

    @Test
    public void propertyConformanceRequired() throws Exception {
        assertNotNull(a.readProperty(PropertyIdentifier.objectIdentifier));
        assertNotNull(a.readProperty(PropertyIdentifier.objectName));
        assertNotNull(a.readProperty(PropertyIdentifier.objectType));
        assertNotNull(a.readProperty(PropertyIdentifier.presentValue));
        assertNotNull(a.readProperty(PropertyIdentifier.statusFlags));
        assertNotNull(a.readProperty(PropertyIdentifier.eventState));
        assertNotNull(a.readProperty(PropertyIdentifier.outOfService));
        assertNotNull(a.readProperty(PropertyIdentifier.scale));
        assertNotNull(a.readProperty(PropertyIdentifier.units));
        assertNotNull(a.readProperty(PropertyIdentifier.maxPresValue));
        assertNotNull(a.readProperty(PropertyIdentifier.propertyList));
    }

    @Test
    public void propertyConformanceEditableWhenOutOfService() throws BACnetServiceException {
        // Should not be writable while in service
        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.presentValue, null, new UnsignedInteger(51), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.pulseRate, null, new UnsignedInteger(51), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.reliability, null, Reliability.overRange, null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        // Should be writable while out of service.
        a.writeProperty(null, PropertyIdentifier.outOfService, Boolean.TRUE);
        a.writeProperty(null, new PropertyValue(PropertyIdentifier.presentValue, null, new UnsignedInteger(51), null));
        a.writeProperty(null, new PropertyValue(PropertyIdentifier.pulseRate, null, new UnsignedInteger(51), null));
        a.writeProperty(null, new PropertyValue(PropertyIdentifier.reliability, null, Reliability.overRange, null));
    }

    @Test
    public void propertyConformanceReadOnly() {
        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.eventMessageTexts, new UnsignedInteger(2),
                                new CharacterString("should fail"), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.valueChangeTime, null, DateTime.UNSPECIFIED, null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.loggingRecord, new UnsignedInteger(2),
                                new CharacterString("should fail"), null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        TestUtils
                .assertBACnetServiceException(
                        () -> a.writeProperty(null,
                                new PropertyValue(PropertyIdentifier.limitMonitoringInterval, null,
                                        new UnsignedInteger(51), null)),
                        ErrorClass.property, ErrorCode.writeAccessDenied);
    }

    @Test
    public void propertyConformanceRequiredWhenIntrinsicReporting() throws Exception {
        a.supportIntrinsicReporting(30, 17, 60, 40, 10, new UnsignedInteger(15), 54, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm);
        assertNotNull(a.readProperty(PropertyIdentifier.pulseRate));
        assertNotNull(a.readProperty(PropertyIdentifier.limitMonitoringInterval));
        assertNotNull(a.readProperty(PropertyIdentifier.timeDelay));
        assertNotNull(a.readProperty(PropertyIdentifier.notificationClass));
        assertNotNull(a.readProperty(PropertyIdentifier.highLimit));
        assertNotNull(a.readProperty(PropertyIdentifier.lowLimit));
        assertNotNull(a.readProperty(PropertyIdentifier.limitEnable));
        assertNotNull(a.readProperty(PropertyIdentifier.eventEnable));
        assertNotNull(a.readProperty(PropertyIdentifier.ackedTransitions));
        assertNotNull(a.readProperty(PropertyIdentifier.notifyType));
        assertNotNull(a.readProperty(PropertyIdentifier.eventTimeStamps));
        assertNotNull(a.readProperty(PropertyIdentifier.eventDetectionEnable));
    }

    @Test
    public void propertyConformanceForbiddenWhenNotIntrinsicReporting() throws Exception {
        assertNull(a.readProperty(PropertyIdentifier.timeDelay));
        assertNull(a.readProperty(PropertyIdentifier.notificationClass));
        assertNull(a.readProperty(PropertyIdentifier.highLimit));
        assertNull(a.readProperty(PropertyIdentifier.lowLimit));
        assertNull(a.readProperty(PropertyIdentifier.limitEnable));
        assertNull(a.readProperty(PropertyIdentifier.eventEnable));
        assertNull(a.readProperty(PropertyIdentifier.ackedTransitions));
        assertNull(a.readProperty(PropertyIdentifier.notifyType));
        assertNull(a.readProperty(PropertyIdentifier.eventTimeStamps));
        assertNull(a.readProperty(PropertyIdentifier.eventMessageTexts));
        assertNull(a.readProperty(PropertyIdentifier.eventMessageTextsConfig));
        assertNull(a.readProperty(PropertyIdentifier.eventDetectionEnable));
        assertNull(a.readProperty(PropertyIdentifier.eventAlgorithmInhibitRef));
        assertNull(a.readProperty(PropertyIdentifier.eventAlgorithmInhibit));
        assertNull(a.readProperty(PropertyIdentifier.timeDelayNormal));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void algorithmicReportingWithFault() throws Exception {
        // Default the pulse rate
        a.set(PropertyIdentifier.pulseRate, new UnsignedInteger(40));

        final DeviceObjectPropertyReference ref = new DeviceObjectPropertyReference(1, a.getId(),
                PropertyIdentifier.pulseRate);
        final EventEnrollmentObject ee = new EventEnrollmentObject(d1, 0, "ee", ref, NotifyType.alarm,
                new EventParameter(
                        new UnsignedRange(new UnsignedInteger(3), new UnsignedInteger(30), new UnsignedInteger(50))),
                new EventTransitionBits(true, true, true), 54, 100, null,
                new FaultParameter(new FaultOutOfRange(new FaultNormalValue(new UnsignedInteger(20)),
                        new FaultNormalValue(new UnsignedInteger(60)))));

        // Set up the notification destination
        final SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        final EventNotifListener listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        // Ensure that initializing the event enrollment object didn't fire any notifications.
        Thread.sleep(40);
        assertEquals(EventState.normal, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        // Go to high limit.
        doPulses(53, 53, 53, 53, 53);
        Thread.sleep(40);
        assertEquals(EventState.highLimit, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(Reliability.noFaultDetected, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(new StatusFlags(true, false, false, false), ee.readProperty(PropertyIdentifier.statusFlags));
        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        Map<String, Object> notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(ee.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.highLimit.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(54), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(100), notif.get("priority"));
        assertEquals(EventType.unsignedRange, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.normal, notif.get("fromState"));
        assertEquals(EventState.highLimit, notif.get("toState"));
        assertEquals(
                new NotificationParameters(new UnsignedRangeNotif(new UnsignedInteger(53),
                        new StatusFlags(false, false, false, false), new UnsignedInteger(50))),
                notif.get("eventValues"));

        // Go to a fault value.
        doPulses(10, 10);
        Thread.sleep(60);
        assertEquals(EventState.fault, ee.readProperty(PropertyIdentifier.eventState));
        assertEquals(Reliability.underRange, ee.readProperty(PropertyIdentifier.reliability));
        assertEquals(new StatusFlags(true, true, false, false), ee.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        assertEquals(1, listener.getNotifCount());
        notif = listener.removeNotif(0);
        assertEquals(new UnsignedInteger(10), notif.get("processIdentifier"));
        assertEquals(d1.getId(), notif.get("initiatingDevice"));
        assertEquals(ee.getId(), notif.get("eventObjectIdentifier"));
        assertEquals(((BACnetArray<TimeStamp>) ee.readProperty(PropertyIdentifier.eventTimeStamps))
                .getBase1(EventState.fault.getTransitionIndex()), notif.get("timeStamp"));
        assertEquals(new UnsignedInteger(54), notif.get("notificationClass"));
        assertEquals(new UnsignedInteger(5), notif.get("priority"));
        assertEquals(EventType.changeOfReliability, notif.get("eventType"));
        assertNull(notif.get("messageText"));
        assertEquals(NotifyType.alarm, notif.get("notifyType"));
        assertEquals(Boolean.TRUE, notif.get("ackRequired"));
        assertEquals(EventState.highLimit, notif.get("fromState"));
        assertEquals(EventState.fault, notif.get("toState"));
        assertEquals(
                new NotificationParameters(
                        new ChangeOfReliabilityNotif(Reliability.underRange, new StatusFlags(true, true, false, false),
                                new SequenceOf<>(new PropertyValue(PropertyIdentifier.objectPropertyReference, ref),
                                        new PropertyValue(PropertyIdentifier.pulseRate, new UnsignedInteger(10)),
                                        new PropertyValue(PropertyIdentifier.statusFlags,
                                                new StatusFlags(false, false, false, false))))),
                notif.get("eventValues"));
    }

    @Test
    public void construction() throws Exception {
        final AccumulatorObject a1 = new AccumulatorObject(d1, 1, "a1", 456, 0, EngineeringUnits.amperes, false,
                new Scale(new Real(1)), new Prescale(new UnsignedInteger(2), new UnsignedInteger(15)), 200, 1);
        assertEquals(new UnsignedInteger(456), a1.get(PropertyIdentifier.presentValue));
    }

    @Test
    public void valueSet() throws Exception {
        assertEquals(UnsignedInteger.ZERO, a.get(PropertyIdentifier.presentValue));
        assertEquals(UnsignedInteger.ZERO, a.get(PropertyIdentifier.valueBeforeChange));
        assertEquals(UnsignedInteger.ZERO, a.get(PropertyIdentifier.valueSet));
        assertEquals(DateTime.UNSPECIFIED, a.get(PropertyIdentifier.valueChangeTime));

        //
        // The object defaults to read only. Ensure that the properties cannot be written.
        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.valueBeforeChange, UnsignedInteger.ZERO)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null, new PropertyValue(PropertyIdentifier.valueSet, UnsignedInteger.ZERO)),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        //
        // Set to allow valueBeforeChange
        a.supportValueWrite(ValueSetWrite.valueBeforeChange);

        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null, new PropertyValue(PropertyIdentifier.valueSet, UnsignedInteger.ZERO)),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        a.writeProperty(null, new PropertyValue(PropertyIdentifier.valueBeforeChange, new UnsignedInteger(7)));
        assertEquals(UnsignedInteger.ZERO, a.get(PropertyIdentifier.presentValue));
        assertEquals(new UnsignedInteger(7), a.get(PropertyIdentifier.valueBeforeChange));
        assertEquals(UnsignedInteger.ZERO, a.get(PropertyIdentifier.valueSet));
        assertEquals(new DateTime(d1), a.get(PropertyIdentifier.valueChangeTime));

        //
        // Set to allow valueSet
        a.supportValueWrite(ValueSetWrite.valueSet);

        TestUtils.assertBACnetServiceException(
                () -> a.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.valueBeforeChange, UnsignedInteger.ZERO)),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        a.writeProperty(null, new PropertyValue(PropertyIdentifier.valueSet, new UnsignedInteger(13)));
        assertEquals(new UnsignedInteger(13), a.get(PropertyIdentifier.presentValue));
        assertEquals(UnsignedInteger.ZERO, a.get(PropertyIdentifier.valueBeforeChange));
        assertEquals(new UnsignedInteger(13), a.get(PropertyIdentifier.valueSet));
        assertEquals(new DateTime(d1), a.get(PropertyIdentifier.valueChangeTime));
    }
}
