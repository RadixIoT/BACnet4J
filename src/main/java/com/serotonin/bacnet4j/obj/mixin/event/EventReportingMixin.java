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

package com.serotonin.bacnet4j.obj.mixin.event;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.NotificationClassObject;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultAlgorithm;
import com.serotonin.bacnet4j.service.acknowledgement.GetAlarmSummaryAck.AlarmSummary;
import com.serotonin.bacnet4j.service.acknowledgement.GetEnrollmentSummaryAck.EnrollmentSummary;
import com.serotonin.bacnet4j.service.acknowledgement.GetEventInformationAck.EventSummary;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.service.confirmed.GetEnrollmentSummaryRequest.AcknowledgmentFilter;
import com.serotonin.bacnet4j.service.confirmed.GetEnrollmentSummaryRequest.EventStateFilter;
import com.serotonin.bacnet4j.service.confirmed.GetEnrollmentSummaryRequest.PriorityFilter;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.RecipientProcess;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Common code for intrinsic and algorithmic reporting classes.
 *
 * @author Matthew
 */
abstract public class EventReportingMixin extends AbstractMixin {
    static final Logger LOG = LoggerFactory.getLogger(EventReportingMixin.class);

    // Configuration
    private final BACnetObject bo;
    private final EventAlgorithm eventAlgo;
    private final FaultAlgorithm faultAlgo;
    private final CORPropertyValueProducer[] changeOfReliabilityProperties;
    private Consumer<NotificationParameters> postNotificationAction;

    // Runtime
    private Delayer delayer;

    public EventReportingMixin(final BACnetObject bo, final EventAlgorithm eventAlgo, final FaultAlgorithm faultAlgo) {
        super(bo);

        this.bo = bo;
        this.eventAlgo = eventAlgo;
        this.faultAlgo = faultAlgo;
        changeOfReliabilityProperties = getCORProperties(bo.getId().getObjectType());

        // Check that the notification object with the given instance number exists.
        //        final UnsignedInteger ncId = bo.get(PropertyIdentifier.notificationClass);
        //        final ObjectIdentifier ncOid = new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue());
        //        if (getLocalDevice().getObject(ncOid) == null)
        //            throw new BACnetRuntimeException("Notification class with id " + ncId + " does not exist");

        // Defaulted properties
        bo.writePropertyInternal(PropertyIdentifier.ackedTransitions, new EventTransitionBits(true, true, true));
        bo.writePropertyInternal(PropertyIdentifier.eventTimeStamps, new BACnetArray<>(TimeStamp.UNSPECIFIED_DATETIME,
                TimeStamp.UNSPECIFIED_DATETIME, TimeStamp.UNSPECIFIED_DATETIME));
        bo.writePropertyInternal(PropertyIdentifier.eventMessageTexts,
                new BACnetArray<>(CharacterString.EMPTY, CharacterString.EMPTY, CharacterString.EMPTY));
        bo.writePropertyInternal(PropertyIdentifier.eventMessageTextsConfig,
                new BACnetArray<>(CharacterString.EMPTY, CharacterString.EMPTY, CharacterString.EMPTY));
        //ee.writePropertyImpl(PropertyIdentifier.eventAlgorithmInhibitRef, new ObjectPropertyReference()); Not supported
        bo.writePropertyInternal(PropertyIdentifier.eventAlgorithmInhibit, Boolean.FALSE);
    }

    public void setPostNotificationAction(final Consumer<NotificationParameters> postNotificationAction) {
        this.postNotificationAction = postNotificationAction;
    }

    abstract protected StateTransition evaluateEventState(BACnetObject bo, EventAlgorithm eventAlgo);

    abstract protected EventType getEventType(EventAlgorithm eventAlgo);

    abstract protected boolean updateAckedTransitions();

    abstract protected NotificationParameters getNotificationParameters(EventState fromState, EventState toState,
            BACnetObject bo, EventAlgorithm eventAlgo);

    abstract protected Reliability evaluateFaultState(Encodable oldMonitoredValue, Encodable newMonitoredValue,
            BACnetObject bo, FaultAlgorithm faultAlgo);

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (value.getPropertyIdentifier().isOneOf( //
                PropertyIdentifier.eventDetectionEnable, //
                PropertyIdentifier.eventTimeStamps, //
                PropertyIdentifier.eventMessageTexts, //
                PropertyIdentifier.eventAlgorithmInhibitRef)) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        }
        return super.validateProperty(valueSource, value);
    }

    @Override
    protected synchronized void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (PropertyIdentifier.reliability.equals(pid)) {
            // Is reliability evaluation inhibited?
            final Boolean rei = get(PropertyIdentifier.reliabilityEvaluationInhibit);
            if (Boolean.falsey(rei)) {
                // Not inhibited. Check the reliability
                final Reliability reli = (Reliability) newValue;
                if (!reli.equals(Reliability.noFaultDetected))
                    // Fault detected. Do an immediate state change to fault.
                    doStateTransitionInternal(EventState.fault);
                else if (!newValue.equals(oldValue)) {
                    // No fault detected. Do an immediate state change to normal.
                    doStateTransitionInternal(EventState.normal);
                    // Now call the event algorithm in case we need a change to offnormal.
                    executeEventAlgo();
                }
            }
        } else if (PropertyIdentifier.eventAlgorithmInhibit.equals(pid) && !newValue.equals(oldValue)) {
            // Ensure there is no fault.
            final Reliability reli = get(PropertyIdentifier.reliability);
            if (reli == null || reli.equals(Reliability.noFaultDetected)) {
                // No fault detected.
                final Boolean eai = (Boolean) newValue;
                if (eai.booleanValue())
                    // Inhibited. Update the event state immediately to normal.
                    doStateTransitionInternal(EventState.normal);
                else
                    // Uninhibited.
                    executeEventAlgo();
            }
        }
    }

    protected void updateEventState(final StateTransition transition) {
        if (transition.getDelay() == null)
            // Do an immediate state transition.
            doStateTransitionInternal(transition.getToState());
        else {
            synchronized (this) {
                if (delayer != null && delayer.transition.equals(transition))
                    // There already is a timer for the same state transition. Ignore this one.
                    return;
                if (delayer != null)
                    // Cancel the existing timer
                    delayer.cancel();

                // Create a timer for the state.
                delayer = new Delayer(transition);
            }
        }
    }

    protected void executeEventAlgo() {
        // Check if the event algorithm is inhibited.
        final Boolean eai = get(PropertyIdentifier.eventAlgorithmInhibit);
        if (eai == null || !eai.booleanValue()) {
            // Uninhibited. Continue with event detection. First determine the provisional event state.
            final StateTransition transition = evaluateEventState(bo, eventAlgo);
            if (transition != null) {
                LOG.debug("Event algo indicated a change to event state {}", transition);
                updateEventState(transition);
            } else
                cancelTimer();
        }
    }

    protected boolean executeFaultAlgo(final Encodable oldValue, final Encodable newValue) {
        if (faultAlgo != null) {
            final Reliability newReli = evaluateFaultState(oldValue, newValue, bo, faultAlgo);
            if (newReli != null) {
                // After setting this value there is nothing else that need be done since this method will be
                // called again due to the property change, and the reliability code above will handle it.
                writePropertyInternal(PropertyIdentifier.reliability, newReli);
                return true;
            }
        }
        return false;
    }

    private void doStateTransitionInternal(final EventState toState) {
        // Notify the algo of the state change.
        eventAlgo.stateChangeNotify(toState);

        doStateTransition(toState);
    }

    protected void doStateTransition(final EventState toState) {
        final EventState fromState = get(PropertyIdentifier.eventState);
        LOG.debug("Event state changing from {} to {}", fromState, toState);

        // If there is a timer in effect, cancel it.
        cancelTimer();

        //
        // Perform the state change. 13.2.2.1.4
        //
        writePropertyInternal(PropertyIdentifier.eventState, toState);

        final TimeStamp now = new TimeStamp(new DateTime(getLocalDevice()));

        BACnetArray<TimeStamp> ets = get(PropertyIdentifier.eventTimeStamps);
        // Make a copy in which to make the change so that the write property method works properly.
        ets = new BACnetArray<>(ets);
        ets.setBase1(toState.getTransitionIndex(), now);
        writePropertyInternal(PropertyIdentifier.eventTimeStamps, ets);

        // Not implemented
        //BACnetArray<CharacterString> emt = get(PropertyIdentifier.eventMessageTexts);
        //emt.set(arrayIndex, new CharacterString(""));

        // Get the notification class object.
        final UnsignedInteger ncId = get(PropertyIdentifier.notificationClass);
        final BACnetObject nc = getLocalDevice()
                .getObject(new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue()));
        if (nc == null) {
            LOG.info("Notification class with id {} does not exist. Aborting state transition processing.", ncId);
            return;
        }

        final boolean isAckRequired;
        if (updateAckedTransitions()) {
            //
            // Update acknowledged transitions. 13.2.3
            //
            EventTransitionBits ackedTransitions = get(PropertyIdentifier.ackedTransitions);
            final EventTransitionBits ackRequired = nc.get(PropertyIdentifier.ackRequired);

            // Make a copy in which to make the change so that the write property method works properly.
            ackedTransitions = new EventTransitionBits(ackedTransitions);

            // If the corresponding bit in Ack_Required is set then the bit in Acked_Transitions is
            // cleared, otherwise it is set.
            isAckRequired = ackRequired.contains(toState);
            ackedTransitions.setValue(toState.getTransitionIndex(), !isAckRequired);
            writePropertyInternal(PropertyIdentifier.ackedTransitions, ackedTransitions);
        } else {
            isAckRequired = false;
        }

        //
        // Event notification distribution. 13.2.5
        //
        final EventTransitionBits eventEnable = get(PropertyIdentifier.eventEnable);

        // Do we need to send any notifications?
        if (eventEnable.contains(toState)) {
            // Send notifications for this transition.
            LOG.debug("Notification enabled for state change to {}. Checking recipient list", toState);

            final SequenceOf<Destination> recipientList = nc.get(PropertyIdentifier.recipientList);
            final NotifyType notifyType = get(PropertyIdentifier.notifyType);
            final BACnetArray<UnsignedInteger> priority = nc.get(PropertyIdentifier.priority);

            EventType eventType;
            NotificationParameters notifParams;
            if (fromState.equals(EventState.fault) || toState.equals(EventState.fault)) {
                eventType = EventType.changeOfReliability;

                // Gather the property values required for the change of reliability notification.
                final SequenceOf<PropertyValue> propertyValues = new SequenceOf<>();
                for (final CORPropertyValueProducer producer : changeOfReliabilityProperties) {
                    final PropertyValue propertyValue = producer.get(this);
                    if (propertyValue != null)
                        propertyValues.add(propertyValue);
                }

                notifParams = new NotificationParameters(new ChangeOfReliabilityNotif( //
                        (Reliability) get(PropertyIdentifier.reliability), //
                        (StatusFlags) get(PropertyIdentifier.statusFlags), //
                        propertyValues));
            } else {
                eventType = getEventType(eventAlgo);
                notifParams = getNotificationParameters(fromState, toState, bo, eventAlgo);
            }

            sendNotifications(recipientList, now, nc, priority, toState, eventType, null, notifyType, //
                    Boolean.valueOf(isAckRequired), fromState, notifParams);
        }
    }

    protected synchronized void cancelTimer() {
        if (delayer != null) {
            LOG.debug("Cancelling delay timer");
            delayer.cancel();
        }
    }

    private class Delayer implements Runnable {
        final StateTransition transition;
        private final ScheduledFuture<?> future;

        public Delayer(final StateTransition transition) {
            LOG.debug("Creating timer for state transition {}", transition);
            this.transition = transition;
            future = getLocalDevice().schedule(this, transition.getDelay().intValue(), TimeUnit.SECONDS);
        }

        @Override
        public void run() {
            LOG.debug("Timer completed for transition {}", transition);
            synchronized (EventReportingMixin.this) {
                delayer = null;
                doStateTransitionInternal(transition.getToState());
            }
        }

        void cancel() {
            LOG.debug("Timer cancelled for transition {}", transition);
            delayer = null;
            future.cancel(false);
        }
    }

    private void sendNotifications(final SequenceOf<Destination> recipientList, final TimeStamp timeStamp,
            final BACnetObject nc, final BACnetArray<UnsignedInteger> priority, final EventState toState,
            final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
            final Boolean ackRequired, final EventState fromState, final NotificationParameters notifParams) {
        final ObjectIdentifier initiatingDeviceIdentifier = getLocalDevice().getId();
        final ObjectIdentifier eventObjectIdentifier = (ObjectIdentifier) get(PropertyIdentifier.objectIdentifier);
        final UnsignedInteger notificationClass = (UnsignedInteger) nc.get(PropertyIdentifier.notificationClass);
        final UnsignedInteger priorityNum = priority.getBase1(toState.getTransitionIndex());

        for (final Destination destination : recipientList) {
            if (destination.isSuitableForEvent(timeStamp, toState)) {
                Address address;
                try {
                    address = destination.getRecipient().toAddress(getLocalDevice());
                } catch (final BACnetException e) {
                    LOG.warn("Failed to get address for recipient {}", destination.getRecipient(), e);
                    continue;
                }

                LOG.debug("Sending {} to {}", notifyType, destination.getRecipient());

                final UnsignedInteger processIdentifier = destination.getProcessIdentifier();

                if (destination.getIssueConfirmedNotifications().booleanValue()) {
                    // Confirmed notification
                    final ConfirmedEventNotificationRequest req = new ConfirmedEventNotificationRequest(
                            processIdentifier, initiatingDeviceIdentifier, eventObjectIdentifier, timeStamp,
                            notificationClass, priorityNum, eventType, messageText, notifyType, ackRequired, fromState,
                            toState, notifParams);
                    getLocalDevice().send(address, req, null);
                } else {
                    // Unconfirmed notification
                    final UnconfirmedEventNotificationRequest req = new UnconfirmedEventNotificationRequest(
                            processIdentifier, initiatingDeviceIdentifier, eventObjectIdentifier, timeStamp,
                            notificationClass, priorityNum, eventType, messageText, notifyType, ackRequired, fromState,
                            toState, notifParams);
                    getLocalDevice().send(address, req);
                }
            }
        }

        // Internal (proprietary) handling of notifications for NotificationClass objects.
        // If the nc is a NotificationClassObject, provide notification to it directly as well.
        if (nc instanceof NotificationClassObject) {
            final NotificationClassObject nco = (NotificationClassObject) nc;
            nco.fireEventNotification(eventObjectIdentifier, timeStamp, notificationClass, priorityNum, eventType,
                    messageText, notifyType, ackRequired, fromState, toState, notifParams);
        }

        if (postNotificationAction != null) {
            postNotificationAction.accept(notifParams);
        }
    }

    //
    //
    // Acknowledgements
    //
    public synchronized void acknowledgeAlarm(final UnsignedInteger acknowledgingProcessIdentifier,
            final EventState eventStateAcknowledged, final TimeStamp timeStamp,
            final CharacterString acknowledgmentSource, final TimeStamp timeOfAcknowledgment)
            throws BACnetServiceException {
        LOG.debug("Alarm acknowledgement received for {}, ts={}, tsAck={}", eventStateAcknowledged, timeStamp,
                timeOfAcknowledgment);
        // Verify that the timestamp for the given acknowledgement matches.
        final BACnetArray<TimeStamp> ets = get(PropertyIdentifier.eventTimeStamps);
        final TimeStamp ts = ets.getBase1(eventStateAcknowledged.getTransitionIndex());
        if (!timeStamp.equals(ts))
            throw new BACnetServiceException(ErrorClass.services, ErrorCode.invalidTimeStamp);

        //
        // Update acknowledged transitions.
        //
        EventTransitionBits ackedTransitions = get(PropertyIdentifier.ackedTransitions);
        if (ackedTransitions.getValue(eventStateAcknowledged.getTransitionIndex())) {
            LOG.info("Aborting alarm acknowledgement for state that did not require acknowledgement");
            return;
        }

        // Make a copy in which to make the change so that the write property method works properly.
        ackedTransitions = new EventTransitionBits(ackedTransitions);
        ackedTransitions.setValue(eventStateAcknowledged.getTransitionIndex(), true);
        writePropertyInternal(PropertyIdentifier.ackedTransitions, ackedTransitions);

        //
        // Event notification distribution.
        //
        final EventTransitionBits eventEnable = get(PropertyIdentifier.eventEnable);

        // Get the notification class object.
        final UnsignedInteger ncId = get(PropertyIdentifier.notificationClass);
        final BACnetObject nc = getLocalDevice()
                .getObject(new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue()));

        // Do we need to send any notifications?
        if (eventEnable.contains(eventStateAcknowledged)) {
            // Send notifications for this transition.
            LOG.debug("Notification enabled for ack of {}. Checking recipient list", eventStateAcknowledged);

            final StringBuilder sb = new StringBuilder();
            sb.append(acknowledgingProcessIdentifier);
            if (acknowledgmentSource != null)
                sb.append(": ").append(acknowledgmentSource.getValue());
            final CharacterString messageText = new CharacterString(sb.toString());

            final SequenceOf<Destination> recipientList = nc.get(PropertyIdentifier.recipientList);
            final BACnetArray<UnsignedInteger> priority = nc.get(PropertyIdentifier.priority);

            sendNotifications(recipientList, timeOfAcknowledgment, nc, priority, eventStateAcknowledged,
                    getEventType(eventAlgo), messageText, NotifyType.ackNotification, null, null, null);
        }
    }

    public AlarmSummary getAlarmSummary() {
        final Boolean eventDetectionEnable = get(PropertyIdentifier.eventDetectionEnable);
        if (eventDetectionEnable != null && eventDetectionEnable.booleanValue()) {
            final EventState eventState = get(PropertyIdentifier.eventState);
            final NotifyType notifyType = get(PropertyIdentifier.notifyType);

            if (!EventState.normal.equals(eventState) && NotifyType.alarm.equals(notifyType))
                return new AlarmSummary( //
                        (ObjectIdentifier) get(PropertyIdentifier.objectIdentifier), //
                        eventState, //
                        (EventTransitionBits) get(PropertyIdentifier.ackedTransitions));
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public EventSummary getEventSummary() {
        final Boolean eventDetectionEnable = get(PropertyIdentifier.eventDetectionEnable);
        if (eventDetectionEnable != null && eventDetectionEnable.booleanValue()) {
            final EventState eventState = get(PropertyIdentifier.eventState);
            final EventTransitionBits ackedTransitions = get(PropertyIdentifier.ackedTransitions);

            if (!EventState.normal.equals(eventState) || !ackedTransitions.allTrue()) {
                final UnsignedInteger ncId = get(PropertyIdentifier.notificationClass);
                final BACnetObject nc = getLocalDevice()
                        .getObject(new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue()));

                return new EventSummary( //
                        (ObjectIdentifier) get(PropertyIdentifier.objectIdentifier), //
                        eventState, //
                        (EventTransitionBits) get(PropertyIdentifier.ackedTransitions), //
                        (BACnetArray<TimeStamp>) get(PropertyIdentifier.eventTimeStamps), //
                        (NotifyType) get(PropertyIdentifier.notifyType), //
                        (EventTransitionBits) get(PropertyIdentifier.eventEnable), //
                        (BACnetArray<UnsignedInteger>) nc.get(PropertyIdentifier.priority));
            }
        }

        return null;
    }

    public EnrollmentSummary getEnrollmentSummary(final AcknowledgmentFilter acknowledgmentFilter,
            final RecipientProcess enrollmentFilter, final EventStateFilter eventStateFilter,
            final EventType eventTypeFilter, final PriorityFilter priorityFilter,
            final UnsignedInteger notificationClassFilter) {
        final Boolean eventDetectionEnable = get(PropertyIdentifier.eventDetectionEnable);
        if (eventDetectionEnable != null && eventDetectionEnable.booleanValue()) {
            final EventState eventState = get(PropertyIdentifier.eventState);
            final UnsignedInteger ncId = get(PropertyIdentifier.notificationClass);
            final BACnetObject nc = getLocalDevice()
                    .getObject(new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue()));
            final BACnetArray<UnsignedInteger> priorities = nc.get(PropertyIdentifier.priority);
            final UnsignedInteger priority = priorities.getBase1(eventState.getTransitionIndex());

            boolean include = true;

            // Acknowledgment filter
            final EventTransitionBits ackedTransitions = get(PropertyIdentifier.ackedTransitions);
            if (AcknowledgmentFilter.acked.equals(acknowledgmentFilter) && !ackedTransitions.allTrue())
                include = false;
            if (AcknowledgmentFilter.notAcked.equals(acknowledgmentFilter) && ackedTransitions.allTrue())
                include = false;

            // Enrollment filter
            if (enrollmentFilter != null) {
                final SequenceOf<Destination> recipientList = nc.get(PropertyIdentifier.recipientList);
                boolean found = false;
                for (final Destination destination : recipientList) {
                    if (destination.getRecipient().equals(enrollmentFilter.getRecipient()) && //
                            destination.getProcessIdentifier().equals(enrollmentFilter.getProcessIdentifier())) {
                        found = true;
                        break;
                    }
                }

                if (!found)
                    include = false;
            }

            // Event state filter
            if (eventStateFilter != null) {
                if (EventStateFilter.offnormal.equals(eventStateFilter) && !eventState.isOffNormal())
                    include = false;
                if (EventStateFilter.fault.equals(eventStateFilter) && !eventState.equals(EventState.fault))
                    include = false;
                if (EventStateFilter.normal.equals(eventStateFilter) && !eventState.equals(EventState.normal))
                    include = false;
                if (EventStateFilter.active.equals(eventStateFilter) && eventState.equals(EventState.normal))
                    include = false;
            }

            // Event type filter
            if (eventTypeFilter != null && !eventTypeFilter.equals(getEventType(eventAlgo)))
                include = false;

            // Priority filter
            if (priorityFilter != null) {
                if (priority.intValue() < priorityFilter.getMinPriority().intValue())
                    include = false;
                if (priority.intValue() > priorityFilter.getMaxPriority().intValue())
                    include = false;
            }

            // Notification class filter
            if (notificationClassFilter != null && !notificationClassFilter.equals(ncId))
                include = false;

            if (include)
                return new EnrollmentSummary((ObjectIdentifier) get(PropertyIdentifier.objectIdentifier),
                        getEventType(eventAlgo), eventState, priority, ncId);
        }

        return null;
    }

    /**
     * @param pid use null to get the monitored property.
     */
    abstract protected PropertyValue getEventEnrollmentMonitoredProperty(PropertyIdentifier pid);

    private static abstract class CORPropertyValueProducer {
        protected final PropertyIdentifier pid;

        public CORPropertyValueProducer(final PropertyIdentifier pid) {
            this.pid = pid;
        }

        abstract PropertyValue get(EventReportingMixin mixin);
    }


    private static class ObjectCORProperyProducer extends CORPropertyValueProducer {
        public ObjectCORProperyProducer(final PropertyIdentifier pid) {
            super(pid);
        }

        @Override
        PropertyValue get(final EventReportingMixin mixin) {
            return new PropertyValue(pid, mixin.get(pid));
        }
    }


    private static class EventEnrollmentMonitoredPropertyProducer extends CORPropertyValueProducer {
        public EventEnrollmentMonitoredPropertyProducer(final PropertyIdentifier pid) {
            super(pid);
        }

        @Override
        PropertyValue get(final EventReportingMixin mixin) {
            return mixin.getEventEnrollmentMonitoredProperty(pid);
        }
    }


    private static class EventEnrollmentMonitoredReferencedPropertyProducer extends CORPropertyValueProducer {
        public EventEnrollmentMonitoredReferencedPropertyProducer(final PropertyIdentifier pid) {
            super(pid);
        }

        @Override
        PropertyValue get(final EventReportingMixin mixin) {
            final DeviceObjectPropertyReference reference = mixin.get(pid);
            return mixin.getEventEnrollmentMonitoredProperty(reference.getPropertyIdentifier());
        }
    }


    private static class NotImplementedProducer extends CORPropertyValueProducer {
        public NotImplementedProducer(final PropertyIdentifier pid) {
            super(pid);
        }

        @Override
        PropertyValue get(final EventReportingMixin mixin) {
            throw new RuntimeException("Not implemented: " + pid);
        }
    }

    //
    //
    // Table 13-5
    private static CORPropertyValueProducer[] getCORProperties(final ObjectType objectType) {
        if (ObjectType.accessDoor.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.doorAlarmState), //
                    new ObjectCORProperyProducer(PropertyIdentifier.presentValue), //
            };

        if (ObjectType.accessPoint.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.accessEvent), //
                    new ObjectCORProperyProducer(PropertyIdentifier.accessEventTag), //
                    new ObjectCORProperyProducer(PropertyIdentifier.accessEventTime), //
                    new ObjectCORProperyProducer(PropertyIdentifier.accessEventCredential), //
            };

        if (ObjectType.accessZone.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.occupancyState), //
            };

        if (ObjectType.accumulator.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.pulseRate), //
                    new ObjectCORProperyProducer(PropertyIdentifier.presentValue), //
            };

        if (objectType.isOneOf( //
                ObjectType.analogInput, //
                ObjectType.analogOutput, //
                ObjectType.analogValue, //
                ObjectType.binaryInput, //
                ObjectType.binaryValue, //
                ObjectType.bitstringValue, //
                ObjectType.channel, //
                ObjectType.characterstringValue, //
                ObjectType.globalGroup, //
                ObjectType.integerValue, //
                ObjectType.largeAnalogValue, //
                ObjectType.lightingOutput, //
                ObjectType.multiStateInput, //
                ObjectType.multiStateValue, //
                ObjectType.positiveIntegerValue, //
                ObjectType.pulseConverter))
            return new CORPropertyValueProducer[] {new ObjectCORProperyProducer(PropertyIdentifier.presentValue)};

        if (objectType.isOneOf( //
                ObjectType.binaryOutput, //
                ObjectType.binaryLightingOutput, //
                ObjectType.multiStateOutput))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.presentValue), //
                    new ObjectCORProperyProducer(PropertyIdentifier.feedbackValue), //
            };

        if (ObjectType.credentialDataInput.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.updateTime), //
                    new ObjectCORProperyProducer(PropertyIdentifier.presentValue), //
            };

        if (objectType.isOneOf( //
                ObjectType.escalator, //
                ObjectType.lift))
            return new CORPropertyValueProducer[] {new NotImplementedProducer(PropertyIdentifier.faultSignals)};

        if (ObjectType.eventEnrollment.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.objectPropertyReference), //
                    new EventEnrollmentMonitoredReferencedPropertyProducer(PropertyIdentifier.objectPropertyReference),
                    //
                    new EventEnrollmentMonitoredPropertyProducer(PropertyIdentifier.reliability), //
                    new EventEnrollmentMonitoredPropertyProducer(PropertyIdentifier.statusFlags), //
            };

        if (objectType.isOneOf( //
                ObjectType.lifeSafetyPoint, //
                ObjectType.lifeSafetyZone))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.presentValue), //
                    new ObjectCORProperyProducer(PropertyIdentifier.mode), //
                    new ObjectCORProperyProducer(PropertyIdentifier.operationExpected), //
            };

        if (ObjectType.loadControl.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.presentValue), //
                    new ObjectCORProperyProducer(PropertyIdentifier.requestedShedLevel), //
                    new ObjectCORProperyProducer(PropertyIdentifier.actualShedLevel), //
            };

        if (ObjectType.loop.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.presentValue), //
                    new ObjectCORProperyProducer(PropertyIdentifier.controlledVariableValue), //
                    new ObjectCORProperyProducer(PropertyIdentifier.setpoint), //
            };

        if (ObjectType.program.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.programState), //
                    new ObjectCORProperyProducer(PropertyIdentifier.controlledVariableValue), //
                    new ObjectCORProperyProducer(PropertyIdentifier.descriptionOfHalt), //
            };

        if (ObjectType.timer.equals(objectType))
            return new CORPropertyValueProducer[] { //
                    new ObjectCORProperyProducer(PropertyIdentifier.presentValue), //
                    new ObjectCORProperyProducer(PropertyIdentifier.timerState), //
                    new ObjectCORProperyProducer(PropertyIdentifier.updateTime), //
                    new ObjectCORProperyProducer(PropertyIdentifier.lastStateChange), //
                    new ObjectCORProperyProducer(PropertyIdentifier.initialTimeout), //
                    new ObjectCORProperyProducer(PropertyIdentifier.expirationTime), //
            };

        return new CORPropertyValueProducer[] {};
    }
}
