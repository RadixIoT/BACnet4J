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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.EventNotificationSubscription;
import com.serotonin.bacnet4j.type.constructed.PortPermission;
import com.serotonin.bacnet4j.type.constructed.ProcessIdSelection;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class NotificationForwarderObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(NotificationForwarderObject.class);

    private final AtomicInteger nextSubscriptionId = new AtomicInteger(0);
    private final DeviceEventAdapter eventListener;
    private final List<Subscription> subscriptions = new ArrayList<>();

    public NotificationForwarderObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final boolean outOfService, final ProcessIdSelection processIdentifierFilter,
            final BACnetArray<PortPermission> portFilter, final boolean localForwardingOnly)
            throws BACnetServiceException {
        super(localDevice, ObjectType.notificationForwarder, instanceNumber, name);

        Objects.requireNonNull(processIdentifierFilter);
        Objects.requireNonNull(portFilter);

        // Load values from persistence
        SequenceOf<Destination> recipientList = getLocalDevice().getPersistence()
                .loadSequenceOf(getPersistenceKey(PropertyIdentifier.recipientList), Destination.class);
        if (recipientList == null) {
            recipientList = new SequenceOf<>();
        }

        SequenceOf<EventNotificationSubscription> subscribedRecipients = getLocalDevice().getPersistence()
                .loadSequenceOf(getPersistenceKey(PropertyIdentifier.subscribedRecipients),
                        EventNotificationSubscription.class);
        if (subscribedRecipients == null) {
            subscribedRecipients = new SequenceOf<>();
        }

        // Set up object properties.
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        writePropertyInternal(PropertyIdentifier.processIdentifierFilter, processIdentifierFilter);
        writePropertyInternal(PropertyIdentifier.portFilter, portFilter);
        writePropertyInternal(PropertyIdentifier.localForwardingOnly, Boolean.valueOf(localForwardingOnly));
        writePropertyInternal(PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.FALSE);

        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new SubscriptionManagementMixin(this));

        writePropertyInternal(PropertyIdentifier.recipientList, recipientList);
        writeProperty(null, PropertyIdentifier.subscribedRecipients, subscribedRecipients);

        eventListener = new DeviceEventAdapter() {
            @Override
            public void eventNotificationReceived(final UnsignedInteger processIdentifier,
                    final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier eventObjectIdentifier,
                    final TimeStamp timeStamp, final UnsignedInteger notificationClass, final UnsignedInteger priority,
                    final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
                    final Boolean ackRequired, final EventState fromState, final EventState toState,
                    final NotificationParameters eventValues) {
                // Out of service.
                final Boolean outOfService = get(PropertyIdentifier.outOfService);
                if (outOfService.booleanValue()) {
                    // Do not forward events while out of service.
                    LOG.debug("Event not forwarded because {} is out of service", getId());
                    return;
                }

                // Process identifier filter.
                final ProcessIdSelection processIdentifierFilter = get(PropertyIdentifier.processIdentifierFilter);
                if (processIdentifierFilter.isProcessIdentifier()) {
                    final Unsigned32 filter = processIdentifierFilter.getProcessIdentifier();
                    if (filter.longValue() != processIdentifier.longValue()) {
                        LOG.debug(
                                "Event not forwarded by {} because the process identifier {} did not match the filter {}",
                                getId(), processIdentifier, processIdentifierFilter);
                        return;
                    }
                }

                // Port filter
                // No knowledge here of what port this notification was received on, so we assume a port id of zero.
                final int portId = 0;
                boolean portEnabled = false;
                final BACnetArray<PortPermission> portFilter = get(PropertyIdentifier.portFilter);
                for (final PortPermission filter : portFilter) {
                    if (filter.getPortId().intValue() == portId && filter.getEnabled().booleanValue()) {
                        portEnabled = true;
                        break;
                    }
                }
                if (!portEnabled) {
                    LOG.debug("Event not forwarded by {} because the port {} is not enabled for forwarding", getId(),
                            portId);
                    return;
                }

                final Boolean localForwardingOnly = get(PropertyIdentifier.localForwardingOnly);
                if (localForwardingOnly.booleanValue()
                        && initiatingDeviceIdentifier.getInstanceNumber() != getLocalDevice().getInstanceNumber()) {
                    LOG.debug(
                            "Event not forwarded because {} is configured for local forwarding only, "
                                    + "the local device id is {}, and the event is from device {}",
                            getId(), getLocalDevice().getInstanceNumber(),
                            initiatingDeviceIdentifier.getInstanceNumber());
                    return;
                }

                // Send to recipients
                final SequenceOf<Destination> recipientList = get(PropertyIdentifier.recipientList);
                for (final Destination destination : recipientList) {
                    if (destination.isSuitableForEvent(timeStamp, toState)) {
                        Address address;
                        try {
                            address = destination.getRecipient().toAddress(getLocalDevice());
                        } catch (final BACnetException e) {
                            LOG.warn("Unable to get address for recipient {}", destination.getRecipient(), e);
                            continue;
                        }

                        LOG.debug("Sending {} to {}", notifyType, destination.getRecipient());
                        sendNotification(destination.getProcessIdentifier(), initiatingDeviceIdentifier,
                                eventObjectIdentifier, timeStamp, notificationClass, priority, eventType, messageText,
                                notifyType, ackRequired, fromState, toState, eventValues, address,
                                destination.getIssueConfirmedNotifications());
                    }
                }

                // Send to subscribers
                forEachSubscriber((subscription) -> {
                    Address address;
                    try {
                        address = subscription.getRecipient().toAddress(getLocalDevice());
                    } catch (final BACnetException e) {
                        LOG.warn("Unable to get address for recipient {}", subscription.getRecipient(), e);
                        return;
                    }

                    LOG.debug("Sending {} to {}", notifyType, subscription.getRecipient());
                    sendNotification(subscription.getProcessIdentifier(), initiatingDeviceIdentifier,
                            eventObjectIdentifier, timeStamp, notificationClass, priority, eventType, messageText,
                            notifyType, ackRequired, fromState, toState, eventValues, address,
                            subscription.getIssueConfirmedNotifications());
                });
            }
        };

        localDevice.getEventHandler().addListener(eventListener);
        localDevice.addObject(this);
    }

    private void sendNotification(final UnsignedInteger processIdentifier,
            final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier eventObjectIdentifier,
            final TimeStamp timeStamp, final UnsignedInteger notificationClass, final UnsignedInteger priority,
            final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
            final Boolean ackRequired, final EventState fromState, final EventState toState,
            final NotificationParameters eventValues, final Address address,
            final Boolean issueConfirmedNotifications) {
        if (issueConfirmedNotifications.booleanValue()) {
            // Confirmed notification
            final ConfirmedEventNotificationRequest req = new ConfirmedEventNotificationRequest(processIdentifier,
                    initiatingDeviceIdentifier, eventObjectIdentifier, timeStamp, notificationClass, priority,
                    eventType, messageText, notifyType, ackRequired, fromState, toState, eventValues);
            getLocalDevice().send(address, req, null);
        } else {
            // Unconfirmed notification
            final UnconfirmedEventNotificationRequest req = new UnconfirmedEventNotificationRequest(processIdentifier,
                    initiatingDeviceIdentifier, eventObjectIdentifier, timeStamp, notificationClass, priority,
                    eventType, messageText, notifyType, ackRequired, fromState, toState, eventValues);
            getLocalDevice().send(address, req);
        }
    }

    @Override
    protected void terminateImpl() {
        getLocalDevice().getEventHandler().removeListener(eventListener);
    }

    private void forEachSubscriber(final Consumer<Subscription> consumer) {
        synchronized (subscriptions) {
            List<Subscription> expirations = null;

            for (final Subscription subscription : subscriptions) {
                if (subscription.hasExpired()) {
                    if (expirations == null)
                        expirations = new ArrayList<>();
                    expirations.add(subscription);
                } else {
                    consumer.accept(subscription);
                }
            }

            // Remove expired subscriptions
            if (expirations != null) {
                subscriptions.removeAll(expirations);
            }
        }
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) {
        if (PropertyIdentifier.subscribedRecipients.equals(pid)) {
            updateSubscribedRecipients();
        }
    }

    class SubscriptionManagementMixin extends AbstractMixin {
        public SubscriptionManagementMixin(final BACnetObject bo) {
            super(bo);
        }

        @Override
        protected void beforeReadProperty(final PropertyIdentifier pid) {
            if (PropertyIdentifier.subscribedRecipients.equals(pid)) {
                updateSubscribedRecipients();
            }
        }

        @Override
        protected boolean writeProperty(final ValueSource valueSource, final PropertyValue value) {
            if (PropertyIdentifier.subscribedRecipients.equals(value.getPropertyIdentifier())) {
                final SequenceOf<EventNotificationSubscription> subscribedRecipients = value.getValue();
                final long now = getLocalDevice().getClock().millis();

                // The AddListElement service will have simply added the given values to the list, meaning that there
                // can be new and duplicate entries present. The RemoveListElement service will have removed elements,
                // so there can be subscriptions that do not exist in the given list.
                synchronized (subscriptions) {
                    // Check for additions.
                    for (final EventNotificationSubscription ens : subscribedRecipients) {
                        // If an event already has a subscription id, then ignore it. If the value is -1, then it is
                        // new.
                        if (ens.getSubscriptionId() == -1) {
                            // Find a matching subscription.
                            Subscription subscription = null;
                            for (final Subscription s : subscriptions) {
                                if (s.matches(ens)) {
                                    subscription = s;
                                    break;
                                }
                            }

                            if (subscription == null) {
                                // A new subscription. Create the instance for it.
                                subscription = new Subscription(nextSubscriptionId.getAndIncrement(),
                                        ens.getRecipient(), ens.getProcessIdentifier());
                                subscriptions.add(subscription);
                            }
                            // Update the subscription with the new values.
                            subscription.setIssueConfirmedNotifications(ens.getIssueConfirmedNotifications());
                            subscription.setExpiryMillis(now + ens.getTimeRemaining().intValue() * 1000);
                        }
                    }

                    // Check for removals.
                    final Iterator<Subscription> iter = subscriptions.iterator();
                    while (iter.hasNext()) {
                        final Subscription subscription = iter.next();

                        // Find a matching notification subscription.
                        boolean matchFound = false;
                        for (final EventNotificationSubscription ens : subscribedRecipients) {
                            if (subscription.matches(ens)) {
                                matchFound = true;
                                break;
                            }
                        }

                        if (!matchFound) {
                            // No match was found, so delete the subscription.
                            iter.remove();
                        }
                    }
                }

                // Persist
                getLocalDevice().getPersistence().saveEncodable(
                        getPersistenceKey(PropertyIdentifier.subscribedRecipients), subscribedRecipients);

                // Consider the write handled.
                return true;
            }
            return false;
        }

        @Override
        protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
                final Encodable newValue) {
            if (pid.isOneOf(PropertyIdentifier.recipientList, PropertyIdentifier.subscribedRecipients)) {
                // Persist the latest written values.
                getLocalDevice().getPersistence().saveEncodable(getPersistenceKey(pid), newValue);
            }
        }
    }

    private void updateSubscribedRecipients() {
        // Dynamically construct the list of subscribers.
        final SequenceOf<EventNotificationSubscription> subscribedRecipients = new SequenceOf<>(subscriptions.size());
        final long now = getLocalDevice().getClock().millis();
        forEachSubscriber((subscription) -> {
            // Convert to seconds, using a ceiling.
            int timeRemaining = (int) ((subscription.getExpiryMillis() - now + 999) / 1000);
            // The forEachSubscriber method should ensure that no expired subscriptions appear here, but just make
            // sure that no zeros end up as the time remaining.
            if (timeRemaining < 1)
                timeRemaining = 1;

            final EventNotificationSubscription ens = new EventNotificationSubscription(subscription.getRecipient(),
                    subscription.getProcessIdentifier(), subscription.getIssueConfirmedNotifications(),
                    new UnsignedInteger(timeRemaining));
            ens.setSubscriptionId(subscription.getSubscriptionId());
            subscribedRecipients.add(ens);
        });
        set(PropertyIdentifier.subscribedRecipients, subscribedRecipients);
    }

    class Subscription {
        private final int subscriptionId;
        private final Recipient recipient;
        private final Unsigned32 processIdentifier;
        private Boolean issueConfirmedNotifications;
        private long expiryMillis;

        public Subscription(final int subscriptionId, final Recipient recipient, final Unsigned32 processIdentifier) {
            this.subscriptionId = subscriptionId;
            this.recipient = recipient;
            this.processIdentifier = processIdentifier;
        }

        public int getSubscriptionId() {
            return subscriptionId;
        }

        public Boolean getIssueConfirmedNotifications() {
            return issueConfirmedNotifications;
        }

        public void setIssueConfirmedNotifications(final Boolean issueConfirmedNotifications) {
            this.issueConfirmedNotifications = issueConfirmedNotifications;
        }

        public long getExpiryMillis() {
            return expiryMillis;
        }

        public void setExpiryMillis(final long expiryMillis) {
            this.expiryMillis = expiryMillis;
        }

        public Recipient getRecipient() {
            return recipient;
        }

        public Unsigned32 getProcessIdentifier() {
            return processIdentifier;
        }

        public boolean hasExpired() {
            return expiryMillis < getLocalDevice().getClock().millis();
        }

        public boolean matches(final EventNotificationSubscription ens) {
            return recipient.equals(ens.getRecipient()) && processIdentifier.equals(ens.getProcessIdentifier());
        }
    }
}
