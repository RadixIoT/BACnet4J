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
import com.serotonin.bacnet4j.type.constructed.DateTime;
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
        ai = d1.addObject(new AnalogInputObject(d1, 0, "ai0", 50, EngineeringUnits.amperes, false));
        nc = d1.addObject(new NotificationClassObject(
                d1, 17, "nc17", 100, 5, 200, new EventTransitionBits(false, false, false)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void intrinsicReporting() throws Exception {
        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        EventNotifListener listener = new EventNotifListener();
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
        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        // Create an event listener on d2 to catch the event notifications.
        EventNotifListener listener = new EventNotifListener();
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
        EventNotifListener.Notif notif = listener.removeNotif();
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

    /**
     * Addendum 135-2016br-6: when Event_Message_Texts_Config carries a non-empty entry for a
     * transition, the notification's Message Text shall be derived from that entry, and the
     * Message Text conveyed shall be stored in the corresponding Event_Message_Texts array
     * element. Untouched Event_Message_Texts slots remain empty per Clause 12.x.25.
     */
    @Test
    public void br6_messageTextDerivedFromEventMessageTextsConfig() throws Exception {
        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        var listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        ai.supportIntrinsicReporting(1, 17, 100, 20, 5, 120, 0, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 2);

        // Baseline: Event_Message_Texts is initialized to three empty strings.
        BACnetArray<CharacterString> emtBefore = ai.readProperty(PropertyIdentifier.eventMessageTexts);
        assertEquals(CharacterString.EMPTY, emtBefore.getBase1(EventState.offnormal.getTransitionIndex()));
        assertEquals(CharacterString.EMPTY, emtBefore.getBase1(EventState.fault.getTransitionIndex()));
        assertEquals(CharacterString.EMPTY, emtBefore.getBase1(EventState.normal.getTransitionIndex()));

        // Configure Event_Message_Texts_Config with distinct entries per transition.
        var offNormalText = new CharacterString("went off normal");
        var faultText = new CharacterString("faulted");
        var normalText = new CharacterString("recovered");
        ai.writePropertyInternal(PropertyIdentifier.eventMessageTextsConfig,
                new BACnetArray<>(offNormalText, faultText, normalText));

        // Drive the value out of range and wait for the to-offnormal transition.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(10));
        clock.plusMillis(1100);
        awaitEquals(1, listener::getNotifCount);
        assertEquals(EventState.lowLimit, ai.readProperty(PropertyIdentifier.eventState));

        var notif = listener.removeNotif();
        assertEquals(EventState.lowLimit, notif.toState());
        // Notification Message Text is derived from the offnormal slot of Event_Message_Texts_Config.
        assertEquals(offNormalText, notif.messageText());

        // Event_Message_Texts[offnormal] reflects the transmitted Message Text; the two other slots
        // remain empty because no transition into them has occurred.
        BACnetArray<CharacterString> emt = ai.readProperty(PropertyIdentifier.eventMessageTexts);
        assertEquals(offNormalText, emt.getBase1(EventState.offnormal.getTransitionIndex()));
        assertEquals(CharacterString.EMPTY, emt.getBase1(EventState.fault.getTransitionIndex()));
        assertEquals(CharacterString.EMPTY, emt.getBase1(EventState.normal.getTransitionIndex()));
    }

    /**
     * Br-6: with the pragmatic default (Event_Message_Texts_Config initialized to empty entries),
     * the notification's Message Text parameter shall not be sent — an empty configured entry is
     * treated as "no configured message." Event_Message_Texts is not overwritten in that case, so
     * its per-transition slots remain empty per Clause 12.x.25.
     */
    @Test
    public void br6_emptyEventMessageTextsConfigYieldsNullMessageText() throws Exception {
        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        var listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        ai.supportIntrinsicReporting(1, 17, 100, 20, 5, 120, 0, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 2);
        // Do not modify Event_Message_Texts_Config — leave the default empty entries.

        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(10));
        clock.plusMillis(1100);
        awaitEquals(1, listener::getNotifCount);

        var notif = listener.removeNotif();
        assertNull(notif.messageText());

        // Event_Message_Texts is not overwritten — all slots remain empty.
        BACnetArray<CharacterString> emt = ai.readProperty(PropertyIdentifier.eventMessageTexts);
        assertEquals(CharacterString.EMPTY, emt.getBase1(EventState.offnormal.getTransitionIndex()));
        assertEquals(CharacterString.EMPTY, emt.getBase1(EventState.fault.getTransitionIndex()));
        assertEquals(CharacterString.EMPTY, emt.getBase1(EventState.normal.getTransitionIndex()));
    }

    /**
     * Br-6: over the course of multiple transitions each slot of Event_Message_Texts is populated
     * from the corresponding slot of Event_Message_Texts_Config independently. This test drives an
     * offnormal → normal → offnormal path and verifies each notification and each Event_Message_Texts
     * slot in turn.
     */
    @Test
    public void br6_messageTextConfigPerTransitionIsIndependent() throws Exception {
        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));

        var listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        ai.supportIntrinsicReporting(1, 17, 100, 20, 5, 120, 0, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 2);

        var offNormalText = new CharacterString("out of range");
        var normalText = new CharacterString("back to normal");
        // Fault slot is deliberately left empty to prove that "empty entry means no message" is
        // per-slot, independent of other slots being configured.
        ai.writePropertyInternal(PropertyIdentifier.eventMessageTextsConfig,
                new BACnetArray<>(offNormalText, CharacterString.EMPTY, normalText));

        // Drive to offnormal.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(10));
        clock.plusMillis(1100);
        awaitEquals(1, listener::getNotifCount);
        var toOffnormal = listener.removeNotif();
        assertEquals(EventState.lowLimit, toOffnormal.toState());
        assertEquals(offNormalText, toOffnormal.messageText());

        // Drive back to normal.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(50));
        clock.plusMillis(120_500);
        awaitEquals(1, listener::getNotifCount);
        var toNormal = listener.removeNotif();
        assertEquals(EventState.normal, toNormal.toState());
        assertEquals(normalText, toNormal.messageText());

        // Event_Message_Texts now carries the two texts we sent; the fault slot remains empty because
        // no fault transition ever occurred (and its config entry was empty anyway).
        BACnetArray<CharacterString> emt = ai.readProperty(PropertyIdentifier.eventMessageTexts);
        assertEquals(offNormalText, emt.getBase1(EventState.offnormal.getTransitionIndex()));
        assertEquals(CharacterString.EMPTY, emt.getBase1(EventState.fault.getTransitionIndex()));
        assertEquals(normalText, emt.getBase1(EventState.normal.getTransitionIndex()));
    }

    /**
     * Br-6: Event_Message_Texts_Config is writable via the standard WriteProperty path. Regression
     * guard confirming the property is not gated by the read-only mixin that also guards
     * Event_Message_Texts.
     */
    @Test
    public void br6_eventMessageTextsConfigIsWritableViaWriteProperty() throws Exception {
        ai.supportIntrinsicReporting(1, 17, 100, 20, 5, 120, 0, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 2);

        var offNormalText = new CharacterString("configured offnormal");
        var faultText = new CharacterString("configured fault");
        var normalText = new CharacterString("configured normal");
        ai.writeProperty(null, new PropertyValue(PropertyIdentifier.eventMessageTextsConfig, null,
                new BACnetArray<>(offNormalText, faultText, normalText), null));

        BACnetArray<CharacterString> emtc = ai.readProperty(PropertyIdentifier.eventMessageTextsConfig);
        assertEquals(offNormalText, emtc.getBase1(EventState.offnormal.getTransitionIndex()));
        assertEquals(faultText, emtc.getBase1(EventState.fault.getTransitionIndex()));
        assertEquals(normalText, emtc.getBase1(EventState.normal.getTransitionIndex()));

        // Confirm Event_Message_Texts remains read-only via the same path (regression guard for
        // EventReportingMixin.validateProperty).
        assertBACnetServiceException(() -> ai.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.eventMessageTexts, null,
                                new BACnetArray<>(CharacterString.EMPTY, CharacterString.EMPTY, CharacterString.EMPTY),
                                null)),
                ErrorClass.property, ErrorCode.writeAccessDenied);
    }

    /**
     * Br-6: acknowledging a fault-state notification shall carry EventType == CHANGE_OF_RELIABILITY
     * (Clauses 13.8.1.1.7 / 13.9.1.1.7). Previously the ACK path used the object's configured event
     * algorithm's EventType regardless of the acknowledged state.
     */
    @Test
    public void br6_ackOfFaultTransitionUsesChangeOfReliabilityEventType() throws Exception {
        SequenceOf<Destination> recipients = nc.get(PropertyIdentifier.recipientList);
        recipients.add(new Destination(new Recipient(rd2.getAddress()), new UnsignedInteger(10), Boolean.TRUE,
                new EventTransitionBits(true, true, true)));
        // Ack must be required for the ack notification to be dispatched by acknowledgeAlarm.
        nc.writePropertyInternal(PropertyIdentifier.ackRequired, new EventTransitionBits(true, true, true));

        var listener = new EventNotifListener();
        d2.getEventHandler().addListener(listener);

        ai.supportIntrinsicReporting(1, 17, 100, 20, 5, 120, 0, new LimitEnable(true, true),
                new EventTransitionBits(true, true, true), NotifyType.alarm, 2);

        // Drive to fault and consume the notification.
        ai.writePropertyInternal(PropertyIdentifier.presentValue, new Real(-5));
        awaitEquals(1, listener::getNotifCount);
        var faultNotif = listener.removeNotif();
        assertEquals(EventState.fault, faultNotif.toState());
        assertEquals(EventType.changeOfReliability, faultNotif.eventType());

        // Acknowledge the fault transition.
        var ackTime = new TimeStamp(new DateTime(d1));
        ai.acknowledgeAlarm(faultNotif.processIdentifier(), faultNotif.toState(), faultNotif.timeStamp(),
                new CharacterString("ack"), ackTime);
        awaitEquals(1, listener::getNotifCount);
        var ack = listener.removeNotif();

        // Per br-6, the ACK notification's EventType is CHANGE_OF_RELIABILITY when the acknowledged
        // state is FAULT — not the object's configured event-algorithm type (which for AI is out-of-range).
        assertEquals(NotifyType.ackNotification, ack.notifyType());
        assertEquals(EventState.fault, ack.toState());
        assertEquals(EventType.changeOfReliability, ack.eventType());
    }
}
