package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
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
    static final Logger LOG = LoggerFactory.getLogger(ChangeOfValueTest.class);

    @Test
    public void objectCovErrors() throws Exception {
        try {
            d2.send(rd1, new SubscribeCOVRequest(new UnsignedInteger(4),
                    new ObjectIdentifier(ObjectType.analogValue, 0), Boolean.TRUE, new UnsignedInteger(1000))).get();
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
            d2.send(rd1,
                    new SubscribeCOVPropertyRequest(new UnsignedInteger(4),
                            new ObjectIdentifier(ObjectType.analogValue, 0), Boolean.TRUE, new UnsignedInteger(1000),
                            new PropertyReference(PropertyIdentifier.accessDoors), null))
                    .get();
            fail("Should have thrown an exception");
        } catch (final ErrorAPDUException e) {
            assertEquals(ErrorClass.object, e.getError().getErrorClass());
            assertEquals(ErrorCode.unknownObject, e.getError().getErrorCode());
        }

        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false);

        try {
            d2.send(rd1,
                    new SubscribeCOVPropertyRequest(new UnsignedInteger(4),
                            new ObjectIdentifier(ObjectType.analogValue, 0), Boolean.TRUE, new UnsignedInteger(1000),
                            new PropertyReference(PropertyIdentifier.accessDoors), null))
                    .get();
            fail("Should have thrown an exception");
        } catch (final ErrorAPDUException e) {
            assertEquals(ErrorClass.object, e.getError().getErrorClass());
            assertEquals(ErrorCode.optionalFunctionalityNotSupported, e.getError().getErrorCode());
        }

        av.supportCovReporting(4);

        d2.send(rd1,
                new SubscribeCOVPropertyRequest(new UnsignedInteger(4), new ObjectIdentifier(ObjectType.analogValue, 0),
                        Boolean.TRUE, new UnsignedInteger(1000), new PropertyReference(PropertyIdentifier.presentValue),
                        null))
                .get();
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
        final SequenceOf<CovSubscription> deviceList = d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(1, deviceList.getCount());
        final CovSubscription subscription = deviceList.getBase1(1);
        assertEquals(null, subscription.getCovIncrement());
        assertEquals(Boolean.TRUE, subscription.getIssueConfirmedNotifications());
        assertEquals(new ObjectPropertyReference(av.getId(), PropertyIdentifier.presentValue),
                subscription.getMonitoredPropertyReference());
        assertEquals(new RecipientProcess(new Recipient(rd2.getAddress()), new UnsignedInteger(4)),
                subscription.getRecipient());

        // Subscribing should have caused a notification to be sent.
        Thread.sleep(50);
        assertEquals(1, listener.size());
        Map<String, Object> notif = listener.poll();
        assertEquals(new UnsignedInteger(4), notif.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(av.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(10)), //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.get("listOfValues"));

        // Write a new value that will trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(20));
        Thread.sleep(100);
        assertEquals(1, listener.size());
        notif = listener.poll();
        assertEquals(new UnsignedInteger(4), notif.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(av.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(20)), //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.get("listOfValues"));

        // Write a new value that won't trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(21));
        Thread.sleep(100);
        assertEquals(0, listener.size());

        // Change a value that is not monitored
        av.writePropertyInternal(PropertyIdentifier.objectName, new CharacterString("av0-new-name"));
        Thread.sleep(100);
        assertEquals(0, listener.size());

        // Change a different value that is monitored
        av.writePropertyInternal(PropertyIdentifier.outOfService, Boolean.TRUE);
        Thread.sleep(100);
        assertEquals(1, listener.size());
        notif = listener.poll();
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(21)), //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif.get("listOfValues"));

        // Change until the increment.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(22));
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(23));
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(24));
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(25));
        Thread.sleep(100);
        assertEquals(1, listener.size());
        notif = listener.poll();
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(25)), //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif.get("listOfValues"));

        // Wait until the subscription expires and write a change that would trigger a notification.
        clock.plusSeconds(3);
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(40));
        Thread.sleep(100);
        assertEquals(0, listener.size());
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
        SequenceOf<CovSubscription> deviceList = d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(1, deviceList.getCount());
        final CovSubscription subscription = deviceList.getBase1(1);
        assertEquals(null, subscription.getCovIncrement());
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
        final SequenceOf<CovSubscription> deviceList = d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions);
        assertEquals(1, deviceList.getCount());
        final CovSubscription subscription = deviceList.getBase1(1);
        assertEquals(null, subscription.getCovIncrement());
        assertEquals(Boolean.FALSE, subscription.getIssueConfirmedNotifications());
        assertEquals(new ObjectPropertyReference(av.getId(), PropertyIdentifier.statusFlags),
                subscription.getMonitoredPropertyReference());
        assertEquals(new RecipientProcess(new Recipient(rd2.getAddress()), new UnsignedInteger(4)),
                subscription.getRecipient());

        // Subscribing should have caused a notification to be sent.
        Thread.sleep(50);
        assertEquals(1, listener.size());
        Map<String, Object> notif = listener.poll();
        assertEquals(new UnsignedInteger(4), notif.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(av.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.get("listOfValues"));

        // Write a new value to a different monitored property. That will trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.presentValue, new Real(20));
        Thread.sleep(100);
        assertEquals(1, listener.size());
        notif = listener.poll();
        assertEquals(new UnsignedInteger(4), notif.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(av.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(20)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.get("listOfValues"));

        // Write a new value to an unmonitored property. That will not trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.highLimit, new Real(60));
        Thread.sleep(100);
        assertEquals(0, listener.size());

        // Write a change to the status flags.
        av.writePropertyInternal(PropertyIdentifier.reliability, Reliability.memberFault);
        Thread.sleep(100);
        assertEquals(1, listener.size());
        notif = listener.poll();
        assertEquals(new UnsignedInteger(4), notif.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif.get("initiatingDevice"));
        assertEquals(av.getId(), notif.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(20)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, true, false, false))),
                notif.get("listOfValues"));
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
        SequenceOf<CovSubscription> deviceList = d1.getDeviceObject()
                .readProperty(PropertyIdentifier.activeCovSubscriptions);
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

        TestUtils.assertListEqualsIgnoreOrder(expectedList, deviceList.getValues());

        // Subscribing should have caused a notification to be sent.
        Thread.sleep(50);

        assertEquals(4, listener2.size());

        Map<String, Object> notif1 = listener2.poll();
        assertEquals(new UnsignedInteger(4), notif1.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif1.get("initiatingDevice"));
        assertEquals(av0.getId(), notif1.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif1.get("listOfValues"));

        Map<String, Object> notif2 = listener2.poll();
        assertEquals(new UnsignedInteger(5), notif2.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif2.get("initiatingDevice"));
        assertEquals(av0.getId(), notif2.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(10)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif2.get("listOfValues"));

        Map<String, Object> notif3 = listener2.poll();
        assertEquals(new UnsignedInteger(6), notif3.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif3.get("initiatingDevice"));
        assertEquals(av1.getId(), notif3.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif3.get("listOfValues"));

        Map<String, Object> notif4 = listener2.poll();
        assertEquals(new UnsignedInteger(7), notif4.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), notif4.get("initiatingDevice"));
        assertEquals(av1.getId(), notif4.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(10)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                notif4.get("listOfValues"));

        assertEquals(4, listener3.size());

        Map<String, Object> l3Notif1 = listener3.poll();
        assertEquals(new UnsignedInteger(4), l3Notif1.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), l3Notif1.get("initiatingDevice"));
        assertEquals(av0.getId(), l3Notif1.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                l3Notif1.get("listOfValues"));

        Map<String, Object> l3Notif2 = listener3.poll();
        assertEquals(new UnsignedInteger(5), l3Notif2.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), l3Notif2.get("initiatingDevice"));
        assertEquals(av0.getId(), l3Notif2.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(10)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                l3Notif2.get("listOfValues"));

        Map<String, Object> l3Notif3 = listener3.poll();
        assertEquals(new UnsignedInteger(6), l3Notif3.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), l3Notif3.get("initiatingDevice"));
        assertEquals(av1.getId(), l3Notif3.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                l3Notif3.get("listOfValues"));

        Map<String, Object> l3Notif4 = listener3.poll();
        assertEquals(new UnsignedInteger(7), l3Notif4.get("subscriberProcessIdentifier"));
        assertEquals(rd1.getObjectIdentifier(), l3Notif4.get("initiatingDevice"));
        assertEquals(av1.getId(), l3Notif4.get("monitoredObjectIdentifier"));
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(10)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true))),
                l3Notif4.get("listOfValues"));

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

    @Test
    public void nonStandardProperties() throws Exception {
        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false)
                .supportCovReporting(4);

        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        // Subscribe to changes.
        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4), av.getId(), Boolean.FALSE, //
                new UnsignedInteger(20), new PropertyReference(PropertyIdentifier.units), null)).get();

        // Subscribing should have caused a notification to be sent.
        Thread.sleep(50);
        assertEquals(1, listener.size());
        Map<String, Object> notif = listener.poll();
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.units, EngineeringUnits.amperes),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.get("listOfValues"));

        // Write a new value to the property. That will trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.ampereSeconds);
        Thread.sleep(100);
        assertEquals(1, listener.size());
        notif = listener.poll();
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.units, EngineeringUnits.ampereSeconds),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false))),
                notif.get("listOfValues"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void valueSourceCommandable() throws Exception {
        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 10, EngineeringUnits.amperes, false)
                .supportCommandable(0) //
                .supportValueSource() //
                .supportCovReporting(4);

        final CovNotifListener listener = new CovNotifListener();
        d2.getEventHandler().addListener(listener);

        // Write a new present value to set up all of the commandable values.
        final ValueSource vs = new ValueSource(new Address(new byte[] { 2 }));
        av.writeProperty(vs, PropertyIdentifier.presentValue, new Real(11));
        final TimeStamp setTime = new TimeStamp(new DateTime(d1));

        // Subscribe to changes.
        d2.send(rd1, new SubscribeCOVPropertyRequest(new UnsignedInteger(4), av.getId(), Boolean.FALSE, //
                new UnsignedInteger(20), new PropertyReference(PropertyIdentifier.valueSource), null)).get();

        // Subscribing should have caused a notification to be sent.
        Thread.sleep(50);
        assertEquals(1, listener.size());
        Map<String, Object> notif = listener.poll();
        SequenceOf<PropertyValue> values = (SequenceOf<PropertyValue>) notif.get("listOfValues");
        // Ensure that the last command time looks like the set time.
        TimeStamp lastCommandTime = (TimeStamp) values.getBase1(4).getValue();
        TestUtils.assertEquals(setTime, lastCommandTime, 2);
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(11)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                        new PropertyValue(PropertyIdentifier.valueSource, vs),
                        new PropertyValue(PropertyIdentifier.lastCommandTime, lastCommandTime),
                        new PropertyValue(PropertyIdentifier.currentCommandPriority, new OptionalUnsigned(16))),
                values);

        // Write a new value to a different property. That will not trigger a notification.
        av.writePropertyInternal(PropertyIdentifier.units, EngineeringUnits.ampereSeconds);
        Thread.sleep(50);
        assertEquals(0, listener.size());

        // Write a new value to the present value. This will trigger three notifications because multiple values
        // will be updated.
        av.writeProperty(vs, PropertyIdentifier.presentValue, new Real(15));
        Thread.sleep(60);
        assertEquals(1, listener.size());
        notif = listener.poll();
        values = (SequenceOf<PropertyValue>) notif.get("listOfValues");
        // Ensure that the last command time looks like the set time.
        lastCommandTime = (TimeStamp) values.getBase1(4).getValue();
        assertEquals(
                new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.presentValue, new Real(15)),
                        new PropertyValue(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false)),
                        new PropertyValue(PropertyIdentifier.valueSource, vs),
                        new PropertyValue(PropertyIdentifier.lastCommandTime, lastCommandTime),
                        new PropertyValue(PropertyIdentifier.currentCommandPriority, new OptionalUnsigned(16))),
                values);
    }
}
