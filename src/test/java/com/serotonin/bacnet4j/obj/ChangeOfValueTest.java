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

import static com.serotonin.bacnet4j.TestUtils.assertEquals;
import static com.serotonin.bacnet4j.TestUtils.assertListEqualsIgnoreOrder;
import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static com.serotonin.bacnet4j.TestUtils.quiesce;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.exception.ErrorAPDUException;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyRequest;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVRequest;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.CovSubscription;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.OptionalUnsigned;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.RecipientProcess;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.Utils;

public class ChangeOfValueTest extends AbstractTest {
    @Test
    public void objectCovErrors() throws Exception {
        try {
            d2.send(rd1,
                    new SubscribeCOVRequest(new UnsignedInteger(4), new ObjectIdentifier(ObjectType.analogValue, 0),
                            Boolean.TRUE, new UnsignedInteger(1000))).get();
            fail("Should have thrown an exception");
        } catch (final ErrorAPDUException e) {
            assertEquals(ErrorClass.object, e.getError().getErrorClass());
            assertEquals(ErrorCode.unknownObject, e.getError().getErrorCode());
        }

        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false);

        try {
            d2.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(4), av.getId(), Boolean.TRUE,
                    new UnsignedInteger(1000))).get();
            fail("Should have thrown an exception");
        } catch (final ErrorAPDUException e) {
            assertEquals(ErrorClass.object, e.getError().getErrorClass());
            assertEquals(ErrorCode.optionalFunctionalityNotSupported, e.getError().getErrorCode());
        }

        av.supportCovReporting(4);

        d2.send(rd1,
                        new SubscribeCOVRequest(new UnsignedInteger(4), av.getId(), Boolean.TRUE, new UnsignedInteger(1000)))
                .get();
    }

    @Test
    public void propertyCovErrors() throws Exception {
        try {
            d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4),
                    new ObjectIdentifier(ObjectType.analogValue, 0), Boolean.TRUE, new UnsignedInteger(1000),
                    new PropertyReference(PropertyIdentifier.accessDoors), null)).get();
            fail("Should have thrown an exception");
        } catch (final ErrorAPDUException e) {
            assertEquals(ErrorClass.object, e.getError().getErrorClass());
            assertEquals(ErrorCode.unknownObject, e.getError().getErrorCode());
        }

        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false);

        try {
            d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4),
                    new ObjectIdentifier(ObjectType.analogValue, 0), Boolean.TRUE, new UnsignedInteger(1000),
                    new PropertyReference(PropertyIdentifier.accessDoors), null)).get();
            fail("Should have thrown an exception");
        } catch (final ErrorAPDUException e) {
            assertEquals(ErrorClass.object, e.getError().getErrorClass());
            assertEquals(ErrorCode.optionalFunctionalityNotSupported, e.getError().getErrorCode());
        }

        av.supportCovReporting(4);

        d2.send(rd1,
                new SubscribeCOVPropertyRequest(new UnsignedInteger(4), new ObjectIdentifier(ObjectType.analogValue, 0),
                        Boolean.TRUE, new UnsignedInteger(1000), new PropertyReference(PropertyIdentifier.presentValue),
                        null)).get();
    }

    @Test
    public void objectCov() throws Exception {
        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false);
        av.supportCovReporting(4);

        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        // Subscribe to changes.
        d2.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(4), av.getId(), Boolean.TRUE, //
                new UnsignedInteger(2))).get();

        // Ensure the subscription is in the device's list.
        final SequenceOf<CovSubscription> deviceList =
                d1.getDeviceObject().readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(1, deviceList.getCount());
        final CovSubscription subscription = deviceList.getBase1(1);
        assertNull(subscription.getCovIncrement());
        assertEquals(Boolean.TRUE, subscription.getIssueConfirmedNotifications());
        assertEquals(new ObjectPropertyReference(av.getId(), PropertyIdentifier.presentValue),
                subscription.getMonitoredPropertyReference());
        assertEquals(new RecipientProcess(new Recipient(rd2.getAddress()), new UnsignedInteger(4)),
                subscription.getRecipient());

        // Subscribing should have caused a notification to be sent.
        awaitEquals(1, listener::getNotifCount);
        CovNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(4), notif.subscriberProcessIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(10)), //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        // Write a new value that will trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(20));
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(4), notif.subscriberProcessIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(20)), //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        // Write a new value that won't trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(21));
        quiesce();
        assertEquals(0, listener.getNotifCount());

        // Change a value that is not monitored
        av.writePropertyInternal(PropertyIdentifier.objectName, new CharacterString("av0-new-name"));
        quiesce();
        assertEquals(0, listener.getNotifCount());

        // Change a different value that is monitored
        av.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.TRUE);
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(21)), //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif.listOfValues());

        // Change until the increment.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(22));
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(23));
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(24));
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(25));
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(25)), //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif.listOfValues());

        // Wait until the subscription expires and write a change that would trigger a notification.
        clock.plusSeconds(3);
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(40));
        quiesce();
        assertEquals(0, listener.getNotifCount());
    }

    @Test
    public void unsubscribe() throws Exception {
        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false);
        av.supportCovReporting(4);

        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        // Subscribe to changes.
        d2.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(4), av.getId(), Boolean.TRUE, new UnsignedInteger(2)))
                .get();

        // Ensure the subscription is in the device's list.
        SequenceOf<CovSubscription> deviceList =
                d1.getDeviceObject().readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(1, deviceList.getCount());
        final CovSubscription subscription = deviceList.getBase1(1);
        assertNull(subscription.getCovIncrement());
        assertEquals(Boolean.TRUE, subscription.getIssueConfirmedNotifications());
        assertEquals(new ObjectPropertyReference(av.getId(), PropertyIdentifier.presentValue),
                subscription.getMonitoredPropertyReference());
        assertEquals(new RecipientProcess(new Recipient(rd2.getAddress()), new UnsignedInteger(4)),
                subscription.getRecipient());

        // Unsubscribe
        d2.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(4), av.getId(), null, null)).get();

        // Ensure the subscription is gone from the device's list.
        deviceList = d1.getDeviceObject().readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(0, deviceList.getCount());
    }

    @Test
    public void propertyCov() throws Exception {
        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false);
        av.supportCovReporting(4);

        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        // Subscribe to changes.
        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4), av.getId(), Boolean.FALSE, //
                new UnsignedInteger(2), new PropertyReference(PropertyIdentifier.statusFlags), null)).get();

        // Ensure the subscription is in the device's list.
        final SequenceOf<CovSubscription> deviceList =
                d1.getDeviceObject().readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(1, deviceList.getCount());
        final CovSubscription subscription = deviceList.getBase1(1);
        assertNull(subscription.getCovIncrement());
        assertEquals(Boolean.FALSE, subscription.getIssueConfirmedNotifications());
        assertEquals(new ObjectPropertyReference(av.getId(), PropertyIdentifier.statusFlags),
                subscription.getMonitoredPropertyReference());
        assertEquals(new RecipientProcess(new Recipient(rd2.getAddress()), new UnsignedInteger(4)),
                subscription.getRecipient());

        // Subscribing should have caused a notification to be sent.
        awaitEquals(1, listener::getNotifCount);
        CovNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(4), notif.subscriberProcessIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        // Write a new value to a different monitored property. That will trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(20));
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(4), notif.subscriberProcessIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(20)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        // Write a new value to an unmonitored property. That will not trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.highLimit, new Real(60));
        quiesce();
        assertEquals(0, listener.getNotifCount());

        // Write a change to the status flags.
        av.writePropertyInternal(PropertyIdentifier.reliability, Reliability.memberFault);
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new UnsignedInteger(4), notif.subscriberProcessIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(20)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, true, false, false))),
                notif.listOfValues());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void multipleClients() throws Exception {
        final AnalogValueObject av0 = new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false) //
                .supportCovReporting(0);
        final AnalogValueObject av1 = new AnalogValueObject(d1, 1, "av1", 10, EngineeringUnits.amperes, true) //
                .supportCovReporting(0);

        final CovNotifListener listener2 = new CovNotifListener();
        d2.getEventHandler().addListener(listener2);

        final CovNotifListener listener3 = new CovNotifListener();
        d3.getEventHandler().addListener(listener3);

        // Subscribe to changes.
        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4), av0.getId(), Boolean.FALSE, //
                new UnsignedInteger(360), new PropertyReference(PropertyIdentifier.statusFlags), null)).get();
        d2.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(5), av0.getId(), Boolean.FALSE, //
                null)).get();
        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(6), av1.getId(), Boolean.FALSE, //
                new UnsignedInteger(360), new PropertyReference(PropertyIdentifier.statusFlags), null)).get();
        d2.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(7), av1.getId(), Boolean.FALSE, //
                null)).get();
        d3.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4), av0.getId(), Boolean.FALSE, //
                new UnsignedInteger(360), new PropertyReference(PropertyIdentifier.statusFlags), null)).get();
        d3.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(5), av0.getId(), Boolean.FALSE, //
                null)).get();
        d3.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(6), av1.getId(), Boolean.FALSE, //
                new UnsignedInteger(360), new PropertyReference(PropertyIdentifier.statusFlags), null)).get();
        d3.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(7), av1.getId(), Boolean.FALSE, //
                null)).get();

        // Ensure the subscriptions are in the device's list.
        SequenceOf<CovSubscription> deviceList =
                d1.getDeviceObject().readProperty(PropertyIdentifier.activeCovSubscriptions);
        final List<CovSubscription> expectedList = Utils.toList(
                new CovSubscription(new RecipientProcess(new Recipient(rd2.getAddress()), new UnsignedInteger(4)),
                        new ObjectPropertyReference(av0.getId(), PropertyIdentifier.statusFlags), Boolean.FALSE,
                        new UnsignedInteger(360), null),
                new CovSubscription(new RecipientProcess(new Recipient(rd2.getAddress()), new UnsignedInteger(5)),
                        new ObjectPropertyReference(av0.getId(), PropertyIdentifier.presentValue), Boolean.FALSE,
                        UnsignedInteger.ZERO, null),
                new CovSubscription(new RecipientProcess(new Recipient(rd2.getAddress()), new UnsignedInteger(6)),
                        new ObjectPropertyReference(av1.getId(), PropertyIdentifier.statusFlags), Boolean.FALSE,
                        new UnsignedInteger(360), null),
                new CovSubscription(new RecipientProcess(new Recipient(rd2.getAddress()), new UnsignedInteger(7)),
                        new ObjectPropertyReference(av1.getId(), PropertyIdentifier.presentValue), Boolean.FALSE,
                        UnsignedInteger.ZERO, null),
                new CovSubscription(new RecipientProcess(new Recipient(rd3.getAddress()), new UnsignedInteger(4)),
                        new ObjectPropertyReference(av0.getId(), PropertyIdentifier.statusFlags), Boolean.FALSE,
                        new UnsignedInteger(360), null),
                new CovSubscription(new RecipientProcess(new Recipient(rd3.getAddress()), new UnsignedInteger(5)),
                        new ObjectPropertyReference(av0.getId(), PropertyIdentifier.presentValue), Boolean.FALSE,
                        UnsignedInteger.ZERO, null),
                new CovSubscription(new RecipientProcess(new Recipient(rd3.getAddress()), new UnsignedInteger(6)),
                        new ObjectPropertyReference(av1.getId(), PropertyIdentifier.statusFlags), Boolean.FALSE,
                        new UnsignedInteger(360), null),
                new CovSubscription(new RecipientProcess(new Recipient(rd3.getAddress()), new UnsignedInteger(7)),
                        new ObjectPropertyReference(av1.getId(), PropertyIdentifier.presentValue), Boolean.FALSE,
                        UnsignedInteger.ZERO, null));

        assertListEqualsIgnoreOrder(expectedList, deviceList.getValues());

        // Subscribing should have caused a notification to be sent.
        awaitEquals(4, listener2::getNotifCount);

        // Notification can be received in any order.
        CovNotifListener.Notif notif = getNotification(listener2, 4);
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av0.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>(
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        notif = getNotification(listener2, 5);
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av0.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(10)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        notif = getNotification(listener2, 6);
        assertEquals(new UnsignedInteger(6), notif.subscriberProcessIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av1.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>(
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif.listOfValues());

        notif = getNotification(listener2, 7);
        assertEquals(new UnsignedInteger(7), notif.subscriberProcessIdentifier());
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av1.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(10)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif.listOfValues());

        awaitEquals(4, listener3::getNotifCount);

        notif = getNotification(listener3, 4);
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av0.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>(
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        notif = getNotification(listener3, 5);
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av0.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(10)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        notif = getNotification(listener3, 6);
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av1.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>(
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif.listOfValues());

        notif = getNotification(listener3, 7);
        assertEquals(rd1.getObjectIdentifier(), notif.initiatingDevice());
        assertEquals(av1.getId(), notif.monitoredObjectIdentifier());
        assertEquals(new SequenceOf<>(new PropertyValue(PropertyIdentifier.presentValue, new Real(10)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif.listOfValues());

        // Wait a bit and make sure the remaining times were updated.
        clock.plusSeconds(12);
        deviceList = d1.getDeviceObject().readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(new UnsignedInteger(348), deviceList.getBase1(1).getTimeRemaining());
        assertEquals(UnsignedInteger.ZERO, deviceList.getBase1(2).getTimeRemaining());
        assertEquals(new UnsignedInteger(348), deviceList.getBase1(3).getTimeRemaining());
        assertEquals(UnsignedInteger.ZERO, deviceList.getBase1(4).getTimeRemaining());
        assertEquals(new UnsignedInteger(348), deviceList.getBase1(5).getTimeRemaining());
        assertEquals(UnsignedInteger.ZERO, deviceList.getBase1(6).getTimeRemaining());
        assertEquals(new UnsignedInteger(348), deviceList.getBase1(7).getTimeRemaining());
        assertEquals(UnsignedInteger.ZERO, deviceList.getBase1(8).getTimeRemaining());

        // Cancel the subscriptions
        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4), av0.getId(), null, //
                null, new PropertyReference(PropertyIdentifier.statusFlags), null)).get();
        assertEquals(7, ((SequenceOf<CovSubscription>) d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions)).getCount());

        d2.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(5), av0.getId(), null, //
                null)).get();
        assertEquals(6, ((SequenceOf<CovSubscription>) d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions)).getCount());

        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(6), av1.getId(), null, //
                null, new PropertyReference(PropertyIdentifier.statusFlags), null)).get();
        assertEquals(5, ((SequenceOf<CovSubscription>) d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions)).getCount());

        d2.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(7), av1.getId(), null, //
                null)).get();
        assertEquals(4, ((SequenceOf<CovSubscription>) d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions)).getCount());

        d3.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4), av0.getId(), null, //
                null, new PropertyReference(PropertyIdentifier.statusFlags), null)).get();
        assertEquals(3, ((SequenceOf<CovSubscription>) d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions)).getCount());

        d3.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(5), av0.getId(), null, //
                null)).get();
        assertEquals(2, ((SequenceOf<CovSubscription>) d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions)).getCount());

        d3.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(6), av1.getId(), null, //
                null, new PropertyReference(PropertyIdentifier.statusFlags), null)).get();
        assertEquals(1, ((SequenceOf<CovSubscription>) d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions)).getCount());

        d3.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(7), av1.getId(), null, //
                null)).get();
        assertEquals(0, ((SequenceOf<CovSubscription>) d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions)).getCount());
    }

    private CovNotifListener.Notif getNotification(CovNotifListener listener, int subscriberProcessId) {
        UnsignedInteger id = new UnsignedInteger(subscriberProcessId);
        int count = listener.getNotifCount();
        for (int i = 0; i < count; i++) {
            if (listener.getNotif(i).subscriberProcessIdentifier().equals(id)) {
                return listener.removeNotif(i);
            }
        }
        fail("Can't find notification with subscriberProcessId of " + subscriberProcessId);
        throw new RuntimeException("Will not be thrown because fail throws an AssertionError");
    }

    @Test
    public void nonStandardProperties() throws Exception {
        final AnalogValueObject av =
                new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false).supportCovReporting(4);

        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        // Subscribe to changes.
        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4), av.getId(), Boolean.FALSE, //
                new UnsignedInteger(20), new PropertyReference(PropertyIdentifier.units), null)).get();

        // Subscribing should have caused a notification to be sent.
        awaitEquals(1, listener::getNotifCount);
        CovNotifListener.Notif notif = listener.removeNotif();
        assertEquals(new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.units, EngineeringUnits.amperes),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());

        // Write a new value to the property. That will trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.ampereSeconds);
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        assertEquals(new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.units, EngineeringUnits.ampereSeconds),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.listOfValues());
    }

    @Test
    public void valueSourceCommandable() throws Exception {
        final AnalogValueObject av =
                new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false).supportCommandable(0) //
                        .supportValueSource() //
                        .supportCovReporting(4);

        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        // Write a new present value to set up all the commandable values.
        final ValueSource vs = new ValueSource(new Address(new byte[] {2}));
        av.writeProperty(vs, PropertyIdentifier.presentValue, new Real(11));
        final TimeStamp setTime = new TimeStamp(new DateTime(d1));

        // Subscribe to changes.
        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4), av.getId(), Boolean.FALSE, //
                new UnsignedInteger(20), new PropertyReference(PropertyIdentifier.valueSource), null)).get();

        // Subscribing should have caused a notification to be sent.
        awaitEquals(1, listener::getNotifCount);
        CovNotifListener.Notif notif = listener.removeNotif();
        SequenceOf<PropertyValue> values = notif.listOfValues();
        // Ensure that the last command time looks like the set time.
        TimeStamp lastCommandTime = values.getBase1(4).getValue();
        assertEquals(setTime, lastCommandTime, 2);
        assertEquals(new SequenceOf<>( //
                new PropertyValue(PropertyIdentifier.presentValue, new Real(11)),
                new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                new PropertyValue(PropertyIdentifier.valueSource, vs),
                new PropertyValue(PropertyIdentifier.lastCommandTime, lastCommandTime),
                new PropertyValue(PropertyIdentifier.currentCommandPriority, new OptionalUnsigned(16))), values);

        // Write a new value to a different property. That will not trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.ampereSeconds);
        quiesce();
        assertEquals(0, listener.getNotifCount());

        // Write a new value to the present value. This will trigger three notifications because multiple values
        // will be updated.
        av.writeProperty(vs, PropertyIdentifier.presentValue, new Real(15));
        awaitEquals(1, listener::getNotifCount);
        notif = listener.removeNotif();
        values = notif.listOfValues();
        // Ensure that the last command time looks like the set time.
        lastCommandTime = values.getBase1(4).getValue();
        assertEquals(new SequenceOf<>( //
                new PropertyValue(PropertyIdentifier.presentValue, new Real(15)),
                new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                new PropertyValue(PropertyIdentifier.valueSource, vs),
                new PropertyValue(PropertyIdentifier.lastCommandTime, lastCommandTime),
                new PropertyValue(PropertyIdentifier.currentCommandPriority, new OptionalUnsigned(16))), values);
    }
}
