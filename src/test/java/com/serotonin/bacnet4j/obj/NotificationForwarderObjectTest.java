package com.serotonin.bacnet4j.obj;

import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.persistence.FilePersistence;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.serotonin.bacnet4j.service.confirmed.AddListElementRequest;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.service.confirmed.RemoveListElementRequest;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventNotificationSubscription;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PortPermission;
import com.serotonin.bacnet4j.type.constructed.ProcessIdSelection;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.notificationParameters.OutOfRangeNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class NotificationForwarderObjectTest extends AbstractTest {
    static final Logger LOG = LoggerFactory.getLogger(TrendLogMultipleObjectTest.class);

    private final BACnetArray<PortPermission> portFilter =
            new BACnetArray<>(new PortPermission(new Unsigned8(0), Boolean.TRUE));

    private final TimeStamp now = new TimeStamp(new DateTime(d1));
    private final ConfirmedEventNotificationRequest n1 =
            new ConfirmedEventNotificationRequest(new UnsignedInteger(122), new ObjectIdentifier(ObjectType.device, 50),
                    new ObjectIdentifier(ObjectType.device, 50), now, new UnsignedInteger(456), new UnsignedInteger(1),
                    EventType.accessEvent, new CharacterString("message"), NotifyType.event, Boolean.FALSE,
                    EventState.fault, EventState.highLimit, new NotificationParameters(new BufferReadyNotif(
                    new DeviceObjectPropertyReference(51, new ObjectIdentifier(ObjectType.trendLog, 0),
                            PropertyIdentifier.logBuffer), new UnsignedInteger(1000), new UnsignedInteger(2000))));
    private final ConfirmedEventNotificationRequest n2 =
            new ConfirmedEventNotificationRequest(new UnsignedInteger(123), new ObjectIdentifier(ObjectType.device, 1),
                    new ObjectIdentifier(ObjectType.device, 1), now, new UnsignedInteger(456), new UnsignedInteger(1),
                    EventType.accessEvent, new CharacterString("message"), NotifyType.event, Boolean.FALSE,
                    EventState.fault, EventState.highLimit, new NotificationParameters(new BufferReadyNotif(
                    new DeviceObjectPropertyReference(51, new ObjectIdentifier(ObjectType.trendLog, 0),
                            PropertyIdentifier.logBuffer), new UnsignedInteger(1000), new UnsignedInteger(2000))));
    private final ConfirmedEventNotificationRequest n3 =
            new ConfirmedEventNotificationRequest(new UnsignedInteger(124), new ObjectIdentifier(ObjectType.device, 60),
                    new ObjectIdentifier(ObjectType.device, 60), now, new UnsignedInteger(789),
                    new UnsignedInteger(109), EventType.commandFailure, new CharacterString("message2"),
                    NotifyType.alarm, Boolean.TRUE, EventState.offnormal, EventState.normal, new NotificationParameters(
                    new OutOfRangeNotif(new Real(34), new StatusFlags(true, true, true, true), new Real(35),
                            new Real(36))));

    @Test
    public void subscriptions() throws Exception {
        final NotificationForwarderObject nf =
                new NotificationForwarderObject(d1, 0, "nf", false, new ProcessIdSelection(Null.instance), portFilter,
                        false);

        // Add a few subscribers.
        new AddListElementRequest(nf.getId(), PropertyIdentifier.subscribedRecipients, null, new SequenceOf<>(
                new EventNotificationSubscription(new Recipient(d2.getId()), new Unsigned32(1), Boolean.TRUE,
                        new UnsignedInteger(3600)),
                new EventNotificationSubscription(new Recipient(d3.getId()), new Unsigned32(2), Boolean.FALSE,
                        new UnsignedInteger(360)),
                new EventNotificationSubscription(new Recipient(d4.getId()), new Unsigned32(3), Boolean.TRUE,
                        new UnsignedInteger(36)))).handle(d1, null);

        // Ensure that the subscribers are there, and that the various ways of getting the data produce the same result.
        SequenceOf<EventNotificationSubscription> enss = nf.readProperty(PropertyIdentifier.subscribedRecipients);
        assertEquals(3, enss.size());
        assertEquals(new Recipient(d2.getId()), enss.get(0).getRecipient());
        assertEquals(new Unsigned32(1), enss.get(0).getProcessIdentifier());
        assertEquals(Boolean.TRUE, enss.get(0).getIssueConfirmedNotifications());
        assertEquals(new UnsignedInteger(3600), enss.get(0).getTimeRemaining());
        assertEquals(new Recipient(d3.getId()), enss.get(1).getRecipient());
        assertEquals(new Unsigned32(2), enss.get(1).getProcessIdentifier());
        assertEquals(Boolean.FALSE, enss.get(1).getIssueConfirmedNotifications());
        assertEquals(new UnsignedInteger(360), enss.get(1).getTimeRemaining());
        assertEquals(new Recipient(d4.getId()), enss.get(2).getRecipient());
        assertEquals(new Unsigned32(3), enss.get(2).getProcessIdentifier());
        assertEquals(Boolean.TRUE, enss.get(2).getIssueConfirmedNotifications());
        assertEquals(new UnsignedInteger(36), enss.get(2).getTimeRemaining());

        final ReadPropertyAck ack =
                (ReadPropertyAck) new ReadPropertyRequest(nf.getId(), PropertyIdentifier.subscribedRecipients,
                        null).handle(d1, null);
        assertEquals(enss, ack.getValue());

        // Advance the clock and ensure that subscriptions have expired and the times remaining has been updated.
        clock.plusSeconds(180);
        enss = nf.readProperty(PropertyIdentifier.subscribedRecipients);
        assertEquals(2, enss.size());
        assertEquals(new Unsigned32(1), enss.get(0).getProcessIdentifier());
        assertEquals(new UnsignedInteger(3420), enss.get(0).getTimeRemaining());
        assertEquals(new Unsigned32(2), enss.get(1).getProcessIdentifier());
        assertEquals(new UnsignedInteger(180), enss.get(1).getTimeRemaining());

        // Add a few more subscribers, and a refresh.
        new AddListElementRequest(nf.getId(), PropertyIdentifier.subscribedRecipients, null, new SequenceOf<>(
                new EventNotificationSubscription(new Recipient(d2.getId()), new Unsigned32(4), Boolean.FALSE,
                        new UnsignedInteger(1000)),
                new EventNotificationSubscription(new Recipient(d3.getId()), new Unsigned32(2), Boolean.TRUE,
                        new UnsignedInteger(360)),
                new EventNotificationSubscription(new Recipient(d4.getId()), new Unsigned32(1), Boolean.TRUE,
                        new UnsignedInteger(36)))).handle(d1, null);
        enss = nf.readProperty(PropertyIdentifier.subscribedRecipients);
        assertEquals(4, enss.size());
        assertEquals(new Recipient(d2.getId()), enss.get(0).getRecipient());
        assertEquals(new Unsigned32(1), enss.get(0).getProcessIdentifier());
        assertEquals(Boolean.TRUE, enss.get(0).getIssueConfirmedNotifications());
        assertEquals(new UnsignedInteger(3420), enss.get(0).getTimeRemaining());
        assertEquals(new Recipient(d3.getId()), enss.get(1).getRecipient());
        assertEquals(new Unsigned32(2), enss.get(1).getProcessIdentifier());
        assertEquals(Boolean.TRUE, enss.get(1).getIssueConfirmedNotifications());
        assertEquals(new UnsignedInteger(360), enss.get(1).getTimeRemaining());
        assertEquals(new Recipient(d2.getId()), enss.get(2).getRecipient());
        assertEquals(new Unsigned32(4), enss.get(2).getProcessIdentifier());
        assertEquals(Boolean.FALSE, enss.get(2).getIssueConfirmedNotifications());
        assertEquals(new UnsignedInteger(1000), enss.get(2).getTimeRemaining());
        assertEquals(new Recipient(d4.getId()), enss.get(3).getRecipient());
        assertEquals(new Unsigned32(1), enss.get(3).getProcessIdentifier());
        assertEquals(Boolean.TRUE, enss.get(3).getIssueConfirmedNotifications());
        assertEquals(new UnsignedInteger(36), enss.get(3).getTimeRemaining());

        // Remove some of the subscribers
        new RemoveListElementRequest(nf.getId(), PropertyIdentifier.subscribedRecipients, null, new SequenceOf<>(
                new EventNotificationSubscription(new Recipient(d2.getId()), new Unsigned32(1), Boolean.FALSE,
                        UnsignedInteger.ZERO),
                new EventNotificationSubscription(new Recipient(d3.getId()), new Unsigned32(2), Boolean.TRUE,
                        UnsignedInteger.ZERO),
                new EventNotificationSubscription(new Recipient(d2.getId()), new Unsigned32(4), Boolean.TRUE,
                        new UnsignedInteger(36)))).handle(d1, null);
        enss = nf.readProperty(PropertyIdentifier.subscribedRecipients);
        assertEquals(1, enss.size());
        assertEquals(new Recipient(d4.getId()), enss.get(0).getRecipient());
        assertEquals(new Unsigned32(1), enss.get(0).getProcessIdentifier());
        assertEquals(Boolean.TRUE, enss.get(0).getIssueConfirmedNotifications());
        assertEquals(new UnsignedInteger(36), enss.get(0).getTimeRemaining());
    }

    @Test
    public void notifications() throws Exception {
        final NotificationForwarderObject nf =
                new NotificationForwarderObject(d1, 0, "nf", false, new ProcessIdSelection(Null.instance), portFilter,
                        false);

        // Create EventLog objects to track forwardings
        final EventLogObject el2 =
                new EventLogObject(d2, 0, "el", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, false, 100);
        final EventLogObject el3 =
                new EventLogObject(d3, 0, "el", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, false, 100);
        final EventLogObject el4 =
                new EventLogObject(d4, 0, "el", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, false, 100);

        // Add el2 and el3 as recipients, and el4 as a subscriber. el3 is set to not receive toNormal.
        new AddListElementRequest(nf.getId(), PropertyIdentifier.recipientList, null, new SequenceOf<>(
                new Destination(new Recipient(d2.getId()), new Unsigned32(1), Boolean.FALSE,
                        new EventTransitionBits(true, true, true)),
                new Destination(new Recipient(d3.getId()), new Unsigned32(2), Boolean.TRUE,
                        new EventTransitionBits(true, true, false)))).handle(d1, null);
        new AddListElementRequest(nf.getId(), PropertyIdentifier.subscribedRecipients, null, new SequenceOf<>(
                new EventNotificationSubscription(new Recipient(d4.getId()), new Unsigned32(3), Boolean.FALSE,
                        new UnsignedInteger(50)))).handle(d1, null);

        // Ensure that all logs are empty.
        assertEquals(0, el2.getBuffer().size());
        assertEquals(0, el3.getBuffer().size());
        assertEquals(0, el4.getBuffer().size());

        // Send an event and ensure that it was received by all.
        n1.handle(d1, TestNetworkUtils.toAddress(50));
        awaitEquals(1, el2.getBuffer()::size);
        awaitEquals(1, el3.getBuffer()::size);
        awaitEquals(1, el4.getBuffer()::size);

        // Make sure the content of the notifications is correct.
        assertEquals(new UnsignedInteger(1), el2.getBuffer().get(0).getNotification().getProcessIdentifier());
        assertEquals(n1.getInitiatingDeviceIdentifier(),
                el2.getBuffer().get(0).getNotification().getInitiatingDeviceIdentifier());
        assertEquals(n1.getEventObjectIdentifier(),
                el2.getBuffer().get(0).getNotification().getEventObjectIdentifier());
        assertEquals(n1.getTimeStamp(), el2.getBuffer().get(0).getNotification().getTimeStamp());
        assertEquals(n1.getNotificationClass(), el2.getBuffer().get(0).getNotification().getNotificationClass());
        assertEquals(n1.getPriority(), el2.getBuffer().get(0).getNotification().getPriority());
        assertEquals(n1.getEventType(), el2.getBuffer().get(0).getNotification().getEventType());
        assertEquals(n1.getMessageText(), el2.getBuffer().get(0).getNotification().getMessageText());
        assertEquals(n1.getNotifyType(), el2.getBuffer().get(0).getNotification().getNotifyType());
        assertEquals(n1.getAckRequired(), el2.getBuffer().get(0).getNotification().getAckRequired());
        assertEquals(n1.getFromState(), el2.getBuffer().get(0).getNotification().getFromState());
        assertEquals(n1.getToState(), el2.getBuffer().get(0).getNotification().getToState());
        assertEquals(n1.getEventValues(), el2.getBuffer().get(0).getNotification().getEventValues());

        assertEquals(new UnsignedInteger(2), el3.getBuffer().get(0).getNotification().getProcessIdentifier());
        assertEquals(new UnsignedInteger(3), el4.getBuffer().get(0).getNotification().getProcessIdentifier());

        //
        // Set the forwarder to out of service, send an event, and ensure that the logs did not change.
        nf.writeProperty(null, PropertyIdentifier.outOfService, Boolean.TRUE);
        n1.handle(d1, TestNetworkUtils.toAddress(50));
        awaitEquals(1, el2.getBuffer()::size);
        awaitEquals(1, el3.getBuffer()::size);
        awaitEquals(1, el4.getBuffer()::size);

        //
        // Change the process identifier filter. Not logs should get this event.
        nf.writeProperty(null, PropertyIdentifier.outOfService, Boolean.FALSE);
        nf.writeProperty(null, PropertyIdentifier.processIdentifierFilter, new ProcessIdSelection(new Unsigned32(123)));
        n1.handle(d1, TestNetworkUtils.toAddress(50));
        awaitEquals(1, el2.getBuffer()::size);
        awaitEquals(1, el3.getBuffer()::size);
        awaitEquals(1, el4.getBuffer()::size);

        // But all the logs should get this one...
        n2.handle(d1, TestNetworkUtils.toAddress(50));
        awaitEquals(2, el2.getBuffer()::size);
        awaitEquals(2, el3.getBuffer()::size);
        awaitEquals(2, el4.getBuffer()::size);

        //
        // Set the port filter, send an event, and ensure that the logs did not change.
        nf.writeProperty(null, PropertyIdentifier.processIdentifierFilter, new ProcessIdSelection(Null.instance));
        nf.writeProperty(null, PropertyIdentifier.portFilter,
                new BACnetArray<>(new PortPermission(new Unsigned8(0), Boolean.FALSE),
                        new PortPermission(new Unsigned8(1), Boolean.TRUE),
                        new PortPermission(new Unsigned8(2), Boolean.FALSE)));
        n1.handle(d1, TestNetworkUtils.toAddress(50));
        awaitEquals(2, el2.getBuffer()::size);
        awaitEquals(2, el3.getBuffer()::size);
        awaitEquals(2, el4.getBuffer()::size);

        //
        // Set local forwarding only, send an event, and ensure that the logs did not change.
        nf.writeProperty(null, PropertyIdentifier.portFilter,
                new BACnetArray<>(new PortPermission(new Unsigned8(0), Boolean.TRUE)));
        nf.writeProperty(null, PropertyIdentifier.localForwardingOnly, Boolean.TRUE);
        n1.handle(d1, TestNetworkUtils.toAddress(50));
        awaitEquals(2, el2.getBuffer()::size);
        awaitEquals(2, el3.getBuffer()::size);
        awaitEquals(2, el4.getBuffer()::size);

        // Now send an event from d1, and ensure that it was received by all.
        n2.handle(d1, TestNetworkUtils.toAddress(1));
        awaitEquals(3, el2.getBuffer()::size);
        awaitEquals(3, el3.getBuffer()::size);
        awaitEquals(3, el4.getBuffer()::size);

        //
        // Finally, send an event that is to normal. The transition bits for el3 are set to not receive.
        nf.writeProperty(null, PropertyIdentifier.localForwardingOnly, Boolean.FALSE);
        n3.handle(d1, TestNetworkUtils.toAddress(1));
        awaitEquals(4, el2.getBuffer()::size);
        awaitEquals(3, el3.getBuffer()::size);
        awaitEquals(4, el4.getBuffer()::size);
    }

    @Test
    public void persistence() throws Exception {
        final File file = new File("nfo-persistence.properties");
        file.delete();

        d1.setPersistence(new FilePersistence(file));

        NotificationForwarderObject nf =
                new NotificationForwarderObject(d1, 0, "nf", false, new ProcessIdSelection(Null.instance), portFilter,
                        false);

        // Ensure that there are no recipients or subscriptions.
        SequenceOf<Destination> recipients = nf.readProperty(PropertyIdentifier.subscribedRecipients);
        assertEquals(0, recipients.size());

        SequenceOf<EventNotificationSubscription> subscriptions =
                nf.readProperty(PropertyIdentifier.subscribedRecipients);
        assertEquals(0, subscriptions.size());

        //
        // Write some of each.
        new AddListElementRequest(nf.getId(), PropertyIdentifier.recipientList, null, new SequenceOf<>(
                new Destination(new Recipient(d2.getId()), new Unsigned32(1), Boolean.FALSE,
                        new EventTransitionBits(true, true, true)),
                new Destination(new Recipient(d3.getId()), new Unsigned32(2), Boolean.TRUE,
                        new EventTransitionBits(true, true, false)))).handle(d1, null);
        new AddListElementRequest(nf.getId(), PropertyIdentifier.subscribedRecipients, null, new SequenceOf<>(
                new EventNotificationSubscription(new Recipient(d4.getId()), new Unsigned32(3), Boolean.FALSE,
                        new UnsignedInteger(50)))).handle(d1, null);

        // Make sure they are there.
        recipients = nf.readProperty(PropertyIdentifier.recipientList);
        assertEquals(2, recipients.size());

        subscriptions = nf.readProperty(PropertyIdentifier.subscribedRecipients);
        assertEquals(1, subscriptions.size());

        //
        // Destroy the object.
        d1.removeObject(nf.getId());

        //
        // Create the object new again and ensure that the lists were loaded from the file.
        nf = new NotificationForwarderObject(d1, 0, "nf", false, new ProcessIdSelection(Null.instance), portFilter,
                false);
        recipients = nf.readProperty(PropertyIdentifier.recipientList);
        assertEquals(2, recipients.size());
        subscriptions = nf.readProperty(PropertyIdentifier.subscribedRecipients);
        assertEquals(1, subscriptions.size());

        // Clean up
        file.delete();
    }
}
