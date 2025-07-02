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
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        // Ensure that no notifications are sent.
        assertEquals(0, listener.getNotifCount());

        // Set an out of range value and then set back to normal before the time delay.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(110));
        clock.plusMillis(500);
        quiesce();
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(90));
        clock.plusMillis(600);
        quiesce();
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.

        // Do a real state change. Write an out of range value. After 1 second the alarm will be raised.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(10));
        clock.plusMillis(500);
        quiesce();
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState)); // Still normal at this point.
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.lowLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, false, false, false), ai.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ai.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ai.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.offnormal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
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
        ai.writePropertyInternal(PropertyIdentifier.limitEnable, new LimitEnable(false, true));
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState));
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ai.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ai.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.normal.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
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
        ai.writePropertyInternal(PropertyIdentifier.limitEnable, new LimitEnable(true, true));
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState));
        clock.plusMillis(1100);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.lowLimit, ai.readProperty(PropertyIdentifier.eventState));
        notif = listener.removeNotif();
        assertEquals(EventType.outOfRange, notif.eventType());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.lowLimit, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(10), new StatusFlags(true, false, false, false), new Real(5),
                        new Real(20))), notif.eventValues());

        // Go to a high limit. Will change to high-limit after 1 second.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(110));
        assertEquals(EventState.lowLimit, ai.readProperty(PropertyIdentifier.eventState));
        clock.plusMillis(1100);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        notif = listener.removeNotif();
        assertEquals(EventState.lowLimit, notif.fromState());
        assertEquals(EventState.highLimit, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(110), new StatusFlags(true, false, false, false), new Real(5),
                        new Real(100))), notif.eventValues());

        // Reduce to within the deadband. No notification.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(95));
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        clock.plusMillis(1100);
        quiesce();
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());

        // Reduce to below the deadband. Return to normal after 2 seconds.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(94));
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());
        clock.plusMillis(1500);
        quiesce();
        assertEquals(EventState.highLimit, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(0, listener.getNotifCount());
        clock.plusMillis(600);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.normal, ai.readProperty(PropertyIdentifier.eventState));
        notif = listener.removeNotif();
        assertEquals(EventState.highLimit, notif.fromState());
        assertEquals(EventState.normal, notif.toState());
        assertEquals(new NotificationParameters(
                new OutOfRangeNotif(new Real(94), new StatusFlags(false, false, false, false), new Real(5),
                        new Real(100))), notif.eventValues());
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
        awaitEquals(1, listener::getNotifCount);

        assertEquals(EventState.fault, ai.readProperty(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), ai.readProperty(PropertyIdentifier.statusFlags));

        // Ensure that a proper looking event notification was received.
        final EventNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(10), notif.processIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(ai.getId(), notif.eventObjectIdentifier());
        assertEquals(((BACnetArray<TimeStamp>) ai.readProperty(PropertyIdentifier.eventTimeStamps)).getBase1(
                EventState.fault.getTransitionIndex()), notif.timeStamp());
        assertEquals(new UnsignedInteger(17), notif.notificationClass());
        assertEquals(new UnsignedInteger(5), notif.priority());
        assertEquals(EventType.changeOfReliability, notif.eventType());
        assertNull(notif.messageText());
        assertEquals(NotifyType.alarm, notif.notifyType());
        assertEquals(Boolean.FALSE, notif.ackRequired());
        assertEquals(EventState.normal, notif.fromState());
        assertEquals(EventState.fault, notif.toState());
        assertEquals(new NotificationParameters(
                        new ChangeOfReliabilityNotif(Reliability.underRange, new StatusFlags(true, true, false, false),
                                new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(-5))))),
                notif.eventValues());
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
        assertBACnetServiceException(() -> ai.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.presentValue, null, new Real(51), null)), ErrorClass.property,
                ErrorCode.writeAccessDenied);

        // Should be writable while out of service.
        ai.writeProperty(null, PropertyIdentifier.outOfService, Boolean.TRUE);
        ai.writeProperty(null, new PropertyValue(PropertyIdentifier.presentValue, null, new Real(51), null));
    }

    @Test
    public void propertyConformanceReadOnly() {
        assertBACnetServiceException(() -> ai.writeProperty(null,
                new PropertyValue(PropertyIdentifier.eventMessageTexts, new UnsignedInteger(2),
                        new CharacterString("should fail"), null)), ErrorClass.property, ErrorCode.writeAccessDenied);
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
