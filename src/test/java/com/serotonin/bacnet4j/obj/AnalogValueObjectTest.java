package com.serotonin.bacnet4j.obj;

import static com.serotonin.bacnet4j.TestUtils.assertBACnetServiceException;
import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
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
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.notificationParameters.OutOfRangeNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class AnalogValueObjectTest extends AbstractTest {
    private AnalogValueObject av;
    private NotificationClassObject nc;

    @Override
    public void afterInit() throws Exception {
        av = new AnalogValueObject(d1, 0, "av0", 50, EngineeringUnits.amperes, false);
        nc = new NotificationClassObject(d1, 7, "nc7", 100, 5, 200, new EventTransitionBits(false, false, false));
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

        av.supportIntrinsicReporting(1, 7, 100, 20, 5, 120, 0, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 2);
        // Ensure that initializing the intrinsic reporting didn't fire any notifications.
        assertEquals(0, listener.getNotifCount());

        // Write a different normal value.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(60));
        assertEquals(EventState.normal, av.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.normal, av.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        // Ensure that no notifications are sent.
        assertEquals(0, listener.getNotifCount());

        // Set an out of range value and then set back to normal before the time delay.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(110));
        clock.plusMillis(500);
        quiesce();
        assertEquals(EventState.normal, av.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(90));
        clock.plusMillis(600);
        quiesce();
        assertEquals(EventState.normal, av.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.

        // Do a real state change. Write an out of range value. After 1 seconds the alarm will be raised.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(10));
        clock.plusMillis(500);
        quiesce();
        assertEquals(EventState.normal, av.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.lowLimit, av.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), av.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) av.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(7), notif.notificationClass());
        assertEquals(new UnsignedInteger(100), notif.priority());
        assertEquals(EventType.outOfRange, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.lowLimit, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(10), new StatusFlags(true, false, false, false), new Real(5),
                        new Real(20))), notif.eventValues());

        // Disable low limit checking. Will return to normal immediately.
        av.writePropertyInternal(PropertyIdentifier.limitEnable, new LimitEnable(false, true));
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, av.readProperty(PropertyIdentifier.eventState));
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) av.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(7), notif.notificationClass());
        assertEquals(new UnsignedInteger(200), notif.priority());
        assertEquals(EventType.outOfRange, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.lowLimit, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(10), new StatusFlags(false, false, false, false), new Real(5),
                        new Real(20))), notif.eventValues());

        // Re-enable low limit checking. Will return to low-limit after 1 second.
        av.writePropertyInternal(PropertyIdentifier.limitEnable, new LimitEnable(true, true));
        assertEquals(EventState.normal, av.readProperty(PropertyIdentifier.eventState));
        clock.plusMillis(1100);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.lowLimit, av.readProperty(PropertyIdentifier.eventState));
        notif = listener.removeNotif();
        assertEquals(EventType.outOfRange, notif.eventType());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.lowLimit, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(10), new StatusFlags(true, false, false, false), new Real(5),
                        new Real(20))), notif.eventValues());

        // Go to a high limit. Will change to high-limit after 1 second.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(110));
        assertEquals(EventState.lowLimit, av.readProperty(PropertyIdentifier.eventState));
        clock.plusMillis(1100);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.highLimit, av.readProperty(PropertyIdentifier.eventState));
        notif = listener.removeNotif();
        assertEquals(EventState.lowLimit, notif.fromState());
        assertEquals(EventState.highLimit, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(110), new StatusFlags(true, false, false, false), new Real(5),
                        new Real(100))), notif.eventValues());

        // Reduce to within the deadband. No notification.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(95));
        assertEquals(EventState.highLimit, av.readProperty(PropertyIdentifier.eventState));
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.highLimit, av.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        // Reduce to below the deadband. Return to normal after 2 seconds.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(94));
        assertEquals(EventState.highLimit, av.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());
        clock.plusMillis(1500);
        quiesce();
        assertEquals(EventState.highLimit, av.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, av.readProperty(PropertyIdentifier.eventState));
        notif = listener.removeNotif();
        assertEquals(EventState.highLimit, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(94), new StatusFlags(false, false, false, false), new Real(5),
                        new Real(100))), notif.eventValues());
    }

    @Test
    public void propertyConformanceReadOnly() {
        assertBACnetServiceException(() -> av.writeProperty(null,
                new PropertyValue(PropertyIdentifier.eventMessageTexts, new UnsignedInteger(2),
                        new CharacterString("should fail"), null)), ErrorClass.property, ErrorCode.writeAccessDenied);
    }
}
