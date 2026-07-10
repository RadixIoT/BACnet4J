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
 */
public abstract class EventReportingMixin extends AbstractMixin {
    static final Logger LOG = LoggerFactory.getLogger(EventReportingMixin.class);

    // Configuration
    private final BACnetObject bo;
    private final EventAlgorithm eventAlgo;
    private final FaultAlgorithm faultAlgo;
    private final CORPropertyValueProducer[] changeOfReliabilityProperties;
    private Consumer<NotificationParameters> postNotificationAction;

    // Runtime
    private Delayer delayer;

    protected EventReportingMixin(BACnetObject bo, EventAlgorithm eventAlgo, FaultAlgorithm faultAlgo) {
        super(bo);

        this.bo = bo;
        this.eventAlgo = eventAlgo;
        this.faultAlgo = faultAlgo;
        changeOfReliabilityProperties = getCORProperties(bo.getId().getObjectType());

        // Check that the notification object with the given instance number exists.
        //         UnsignedInteger ncId = bo.get(PropertyIdentifier.notificationClass);
        //         ObjectIdentifier ncOid = new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue());
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

    public void setPostNotificationAction(Consumer<NotificationParameters> postNotificationAction) {
        this.postNotificationAction = postNotificationAction;
    }

    protected abstract StateTransition evaluateEventState(BACnetObject bo, EventAlgorithm eventAlgo);

    protected abstract EventType getEventType(EventAlgorithm eventAlgo);

    protected abstract boolean updateAckedTransitions();

    protected abstract NotificationParameters getNotificationParameters(EventState fromState, EventState toState,
            BACnetObject bo, EventAlgorithm eventAlgo);

    protected abstract Reliability evaluateFaultState(Encodable oldMonitoredValue, Encodable newMonitoredValue,
            BACnetObject bo, FaultAlgorithm faultAlgo);

    @Override
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
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
    protected synchronized void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        if (PropertyIdentifier.reliability.equals(pid)) {
            // Is reliability evaluation inhibited?
            Boolean rei = get(PropertyIdentifier.reliabilityEvaluationInhibit);
            if (Boolean.falsey(rei)) {
                // Not inhibited. Check the reliability
                Reliability reli = (Reliability) newValue;
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
            Reliability reli = get(PropertyIdentifier.reliability);
            if (reli == null || reli.equals(Reliability.noFaultDetected)) {
                // No fault detected.
                Boolean eai = (Boolean) newValue;
                if (eai.booleanValue())
                    // Inhibited. Update the event state immediately to normal.
                    doStateTransitionInternal(EventState.normal);
                else
                    // Uninhibited.
                    executeEventAlgo();
            }
        }
    }

    protected void updateEventState(StateTransition transition) {
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
        Boolean eai = get(PropertyIdentifier.eventAlgorithmInhibit);
        if (eai == null || !eai.booleanValue()) {
            // Uninhibited. Continue with event detection. First determine the provisional event state.
            StateTransition transition = evaluateEventState(bo, eventAlgo);
            if (transition != null) {
                LOG.debug("Event algo indicated a change to event state {}", transition);
                updateEventState(transition);
            } else
                cancelTimer();
        }
    }

    protected boolean executeFaultAlgo(Encodable oldValue, Encodable newValue) {
        if (faultAlgo != null) {
            Reliability newReli = evaluateFaultState(oldValue, newValue, bo, faultAlgo);
            if (newReli != null) {
                // After setting this value there is nothing else that need be done since this method will be
                // called again due to the property change, and the reliability code above will handle it.
                writePropertyInternal(PropertyIdentifier.reliability, newReli);
                return true;
            }
        }
        return false;
    }

    private void doStateTransitionInternal(EventState toState) {
        // Notify the algo of the state change.
        eventAlgo.stateChangeNotify(toState);

        doStateTransition(toState);
    }

    protected void doStateTransition(EventState toState) {
        EventState fromState = get(PropertyIdentifier.eventState);
        LOG.debug("Event state changing from {} to {}", fromState, toState);

        // If there is a timer in effect, cancel it.
        cancelTimer();

        //
        // Perform the state change. 13.2.2.1.4
        //
        writePropertyInternal(PropertyIdentifier.eventState, toState);

        TimeStamp now = new TimeStamp(new DateTime(getLocalDevice()));

        BACnetArray<TimeStamp> ets = get(PropertyIdentifier.eventTimeStamps);
        // Make a copy in which to make the change so that the write property method works properly.
        ets = new BACnetArray<>(ets);
        ets.setBase1(toState.getTransitionIndex(), now);
        writePropertyInternal(PropertyIdentifier.eventTimeStamps, ets);

        var messageText = getEventMessageText(toState);
        if (messageText != null) {
            BACnetArray<CharacterString> emt = get(PropertyIdentifier.eventMessageTexts);
            emt = new BACnetArray<>(emt);
            emt.setBase1(toState.getTransitionIndex(), messageText);
            writePropertyInternal(PropertyIdentifier.eventMessageTexts, emt);
        }

        // Get the notification class object.
        UnsignedInteger ncId = get(PropertyIdentifier.notificationClass);
        BACnetObject nc = getLocalDevice()
                .getObject(new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue()));
        if (nc == null) {
            LOG.info("Notification class with id {} does not exist. Aborting state transition processing.", ncId);
            return;
        }

        boolean isAckRequired;
        if (updateAckedTransitions()) {
            //
            // Update acknowledged transitions. 13.2.3
            //
            EventTransitionBits ackedTransitions = get(PropertyIdentifier.ackedTransitions);
            EventTransitionBits ackRequired = nc.get(PropertyIdentifier.ackRequired);

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
        EventTransitionBits eventEnable = get(PropertyIdentifier.eventEnable);

        // Do we need to send any notifications?
        if (eventEnable.contains(toState)) {
            // Send notifications for this transition.
            LOG.debug("Notification enabled for state change to {}. Checking recipient list", toState);

            SequenceOf<Destination> recipientList = nc.get(PropertyIdentifier.recipientList);
            NotifyType notifyType = get(PropertyIdentifier.notifyType);
            BACnetArray<UnsignedInteger> priority = nc.get(PropertyIdentifier.priority);

            EventType eventType;
            NotificationParameters notifParams;
            if (fromState.equals(EventState.fault) || toState.equals(EventState.fault)) {
                eventType = EventType.changeOfReliability;

                // Gather the property values required for the change of reliability notification.
                SequenceOf<PropertyValue> propertyValues = new SequenceOf<>();
                for (CORPropertyValueProducer producer : changeOfReliabilityProperties) {
                    PropertyValue propertyValue = producer.get(this);
                    if (propertyValue != null)
                        propertyValues.add(propertyValue);
                }

                notifParams = new NotificationParameters(new ChangeOfReliabilityNotif(
                        get(PropertyIdentifier.reliability), get(PropertyIdentifier.statusFlags), propertyValues));
            } else {
                eventType = getEventType(eventAlgo);
                notifParams = getNotificationParameters(fromState, toState, bo, eventAlgo);
            }

            sendNotifications(recipientList, now, nc, priority, toState, eventType, messageText, notifyType,
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
        StateTransition transition;
        private final ScheduledFuture<?> future;

        public Delayer(StateTransition transition) {
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

    private void sendNotifications(SequenceOf<Destination> recipientList, TimeStamp timeStamp, BACnetObject nc,
            BACnetArray<UnsignedInteger> priority, EventState toState, EventType eventType, CharacterString messageText,
            NotifyType notifyType, Boolean ackRequired, EventState fromState, NotificationParameters notifParams) {
        ObjectIdentifier initiatingDeviceIdentifier = getLocalDevice().getId();
        ObjectIdentifier eventObjectIdentifier = get(PropertyIdentifier.objectIdentifier);
        UnsignedInteger notificationClass = nc.get(PropertyIdentifier.notificationClass);
        UnsignedInteger priorityNum = priority.getBase1(toState.getTransitionIndex());

        for (Destination destination : recipientList) {
            if (destination.isSuitableForEvent(timeStamp, toState)) {
                Address address;
                try {
                    address = destination.getRecipient().toAddress(getLocalDevice());
                } catch (BACnetException e) {
                    LOG.warn("Failed to get address for recipient {}", destination.getRecipient(), e);
                    continue;
                }

                LOG.debug("Sending {} to {}", notifyType, destination.getRecipient());

                UnsignedInteger processIdentifier = destination.getProcessIdentifier();

                if (destination.getIssueConfirmedNotifications().booleanValue()) {
                    // Confirmed notification
                    ConfirmedEventNotificationRequest req = new ConfirmedEventNotificationRequest(
                            processIdentifier, initiatingDeviceIdentifier, eventObjectIdentifier, timeStamp,
                            notificationClass, priorityNum, eventType, messageText, notifyType, ackRequired, fromState,
                            toState, notifParams);
                    getLocalDevice().send(address, req, null);
                } else {
                    // Unconfirmed notification
                    UnconfirmedEventNotificationRequest req = new UnconfirmedEventNotificationRequest(
                            processIdentifier, initiatingDeviceIdentifier, eventObjectIdentifier, timeStamp,
                            notificationClass, priorityNum, eventType, messageText, notifyType, ackRequired, fromState,
                            toState, notifParams);
                    getLocalDevice().send(address, req);
                }
            }
        }

        // Internal (proprietary) handling of notifications for NotificationClass objects.
        // If the nc is a NotificationClassObject, provide notification to it directly as well.
        if (nc instanceof NotificationClassObject nco) {
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
    public synchronized void acknowledgeAlarm(UnsignedInteger acknowledgingProcessIdentifier,
            EventState eventStateAcknowledged, TimeStamp timeStamp, CharacterString acknowledgmentSource,
            TimeStamp timeOfAcknowledgment) throws BACnetServiceException {
        LOG.debug("Alarm acknowledgement received for {}, ts={}, tsAck={}", eventStateAcknowledged, timeStamp,
                timeOfAcknowledgment);
        // Verify that the timestamp for the given acknowledgement matches.
        BACnetArray<TimeStamp> ets = get(PropertyIdentifier.eventTimeStamps);
        TimeStamp ts = ets.getBase1(eventStateAcknowledged.getTransitionIndex());
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
        EventTransitionBits eventEnable = get(PropertyIdentifier.eventEnable);

        // Get the notification class object.
        UnsignedInteger ncId = get(PropertyIdentifier.notificationClass);
        BACnetObject nc = getLocalDevice()
                .getObject(new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue()));

        // Do we need to send any notifications?
        if (eventEnable.contains(eventStateAcknowledged)) {
            // Send notifications for this transition.
            LOG.debug("Notification enabled for ack of {}. Checking recipient list", eventStateAcknowledged);

            StringBuilder sb = new StringBuilder();
            sb.append(acknowledgingProcessIdentifier);
            if (acknowledgmentSource != null)
                sb.append(": ").append(acknowledgmentSource.getValue());
            CharacterString messageText = new CharacterString(sb.toString());

            SequenceOf<Destination> recipientList = nc.get(PropertyIdentifier.recipientList);
            BACnetArray<UnsignedInteger> priority = nc.get(PropertyIdentifier.priority);

            var ackEventType = EventState.fault.equals(eventStateAcknowledged)
                    ? EventType.changeOfReliability
                    : getEventType(eventAlgo);
            sendNotifications(recipientList, timeOfAcknowledgment, nc, priority, eventStateAcknowledged,
                    ackEventType, messageText, NotifyType.ackNotification, null, null, null);
        }
    }

    public AlarmSummary getAlarmSummary() {
        Boolean eventDetectionEnable = get(PropertyIdentifier.eventDetectionEnable);
        if (eventDetectionEnable != null && eventDetectionEnable.booleanValue()) {
            EventState eventState = get(PropertyIdentifier.eventState);
            NotifyType notifyType = get(PropertyIdentifier.notifyType);

            if (!EventState.normal.equals(eventState) && NotifyType.alarm.equals(notifyType))
                return new AlarmSummary(get(PropertyIdentifier.objectIdentifier), eventState,
                        get(PropertyIdentifier.ackedTransitions));
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public EventSummary getEventSummary() {
        Boolean eventDetectionEnable = get(PropertyIdentifier.eventDetectionEnable);
        if (eventDetectionEnable != null && eventDetectionEnable.booleanValue()) {
            EventState eventState = get(PropertyIdentifier.eventState);
            EventTransitionBits ackedTransitions = get(PropertyIdentifier.ackedTransitions);

            if (!EventState.normal.equals(eventState) || !ackedTransitions.allTrue()) {
                UnsignedInteger ncId = get(PropertyIdentifier.notificationClass);
                BACnetObject nc = getLocalDevice()
                        .getObject(new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue()));

                return new EventSummary(get(PropertyIdentifier.objectIdentifier),
                        eventState,
                        get(PropertyIdentifier.ackedTransitions),
                        get(PropertyIdentifier.eventTimeStamps),
                        get(PropertyIdentifier.notifyType),
                        get(PropertyIdentifier.eventEnable),
                        nc.get(PropertyIdentifier.priority));
            }
        }

        return null;
    }

    public EnrollmentSummary getEnrollmentSummary(AcknowledgmentFilter acknowledgmentFilter,
            RecipientProcess enrollmentFilter, EventStateFilter eventStateFilter, EventType eventTypeFilter,
            PriorityFilter priorityFilter, UnsignedInteger notificationClassFilter) {
        Boolean eventDetectionEnable = get(PropertyIdentifier.eventDetectionEnable);
        if (eventDetectionEnable != null && eventDetectionEnable.booleanValue()) {
            EventState eventState = get(PropertyIdentifier.eventState);
            UnsignedInteger ncId = get(PropertyIdentifier.notificationClass);
            BACnetObject nc = getLocalDevice()
                    .getObject(new ObjectIdentifier(ObjectType.notificationClass, ncId.intValue()));
            BACnetArray<UnsignedInteger> priorities = nc.get(PropertyIdentifier.priority);
            UnsignedInteger priority = priorities.getBase1(eventState.getTransitionIndex());

            boolean include = true;

            // Acknowledgment filter
            EventTransitionBits ackedTransitions = get(PropertyIdentifier.ackedTransitions);
            if (AcknowledgmentFilter.acked.equals(acknowledgmentFilter) && !ackedTransitions.allTrue())
                include = false;
            if (AcknowledgmentFilter.notAcked.equals(acknowledgmentFilter) && ackedTransitions.allTrue())
                include = false;

            // Enrollment filter
            if (enrollmentFilter != null) {
                SequenceOf<Destination> recipientList = nc.get(PropertyIdentifier.recipientList);
                boolean found = false;
                for (Destination destination : recipientList) {
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
                return new EnrollmentSummary(get(PropertyIdentifier.objectIdentifier),
                        getEventType(eventAlgo), eventState, priority, ncId);
        }

        return null;
    }

    private CharacterString getEventMessageText(EventState eventState) {
        BACnetArray<CharacterString> emtc = get(PropertyIdentifier.eventMessageTextsConfig);
        CharacterString config = null;
        if (emtc != null) {
            config = emtc.getBase1(eventState.getTransitionIndex());
            if (CharacterString.EMPTY.equals(config)) {
                config = null;
            }
        }
        return formatEventMessageTextConfig(eventState, config);
    }

    /**
     * Subclasses can perform "proprietary text substitution codes to incorporate dynamic information such as date
     * and time or other information"
     *
     * @param eventState the state transitioning to
     * @param config     the config at the given state. Can be null.
     * @return default return value is the unchanged text config
     */
    protected CharacterString formatEventMessageTextConfig(EventState eventState, CharacterString config) {
        return config;
    }

    /**
     * @param pid use null to get the monitored property.
     */
    protected abstract PropertyValue getEventEnrollmentMonitoredProperty(PropertyIdentifier pid);

    private abstract static class CORPropertyValueProducer {
        protected final PropertyIdentifier pid;

        public CORPropertyValueProducer(PropertyIdentifier pid) {
            this.pid = pid;
        }

        abstract PropertyValue get(EventReportingMixin mixin);
    }


    private static class ObjectCORPropertyProducer extends CORPropertyValueProducer {
        public ObjectCORPropertyProducer(PropertyIdentifier pid) {
            super(pid);
        }

        @Override
        PropertyValue get(EventReportingMixin mixin) {
            return new PropertyValue(pid, mixin.get(pid));
        }
    }


    private static class EventEnrollmentMonitoredPropertyProducer extends CORPropertyValueProducer {
        public EventEnrollmentMonitoredPropertyProducer(PropertyIdentifier pid) {
            super(pid);
        }

        @Override
        PropertyValue get(EventReportingMixin mixin) {
            return mixin.getEventEnrollmentMonitoredProperty(pid);
        }
    }


    private static class EventEnrollmentMonitoredReferencedPropertyProducer extends CORPropertyValueProducer {
        public EventEnrollmentMonitoredReferencedPropertyProducer(PropertyIdentifier pid) {
            super(pid);
        }

        @Override
        PropertyValue get(EventReportingMixin mixin) {
            DeviceObjectPropertyReference reference = mixin.get(pid);
            return mixin.getEventEnrollmentMonitoredProperty(reference.getPropertyIdentifier());
        }
    }


    private static class NotImplementedProducer extends CORPropertyValueProducer {
        public NotImplementedProducer(PropertyIdentifier pid) {
            super(pid);
        }

        @Override
        PropertyValue get(EventReportingMixin mixin) {
            throw new RuntimeException("Not implemented: " + pid);
        }
    }

    //
    //
    // Table 13-5
    private static CORPropertyValueProducer[] getCORProperties(ObjectType objectType) {
        if (ObjectType.accessDoor.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.doorAlarmState),
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentValue),
            };

        if (ObjectType.accessPoint.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.accessEvent),
                    new ObjectCORPropertyProducer(PropertyIdentifier.accessEventTag),
                    new ObjectCORPropertyProducer(PropertyIdentifier.accessEventTime),
                    new ObjectCORPropertyProducer(PropertyIdentifier.accessEventCredential),
            };

        if (ObjectType.accessZone.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.occupancyState),
            };

        if (ObjectType.accumulator.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.pulseRate),
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentValue),
            };

        if (objectType.isOneOf(
                ObjectType.analogInput,
                ObjectType.analogOutput,
                ObjectType.analogValue,
                ObjectType.binaryInput,
                ObjectType.binaryValue,
                ObjectType.bitstringValue,
                ObjectType.channel,
                ObjectType.characterstringValue,
                ObjectType.globalGroup,
                ObjectType.integerValue,
                ObjectType.largeAnalogValue,
                ObjectType.lightingOutput,
                ObjectType.multiStateInput,
                ObjectType.multiStateValue,
                ObjectType.positiveIntegerValue,
                ObjectType.pulseConverter))
            return new CORPropertyValueProducer[] {new ObjectCORPropertyProducer(PropertyIdentifier.presentValue)};

        if (objectType.isOneOf(
                ObjectType.binaryOutput,
                ObjectType.binaryLightingOutput,
                ObjectType.multiStateOutput))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentValue),
                    new ObjectCORPropertyProducer(PropertyIdentifier.feedbackValue),
            };

        if (ObjectType.credentialDataInput.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.updateTime),
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentValue),
            };

        if (objectType.isOneOf(
                ObjectType.escalator,
                ObjectType.lift))
            return new CORPropertyValueProducer[] {new NotImplementedProducer(PropertyIdentifier.faultSignals)};

        if (ObjectType.eventEnrollment.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.objectPropertyReference),
                    new EventEnrollmentMonitoredReferencedPropertyProducer(PropertyIdentifier.objectPropertyReference),
                    new EventEnrollmentMonitoredPropertyProducer(PropertyIdentifier.reliability),
                    new EventEnrollmentMonitoredPropertyProducer(PropertyIdentifier.statusFlags),
            };

        if (objectType.isOneOf(
                ObjectType.lifeSafetyPoint,
                ObjectType.lifeSafetyZone))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentValue),
                    new ObjectCORPropertyProducer(PropertyIdentifier.mode),
                    new ObjectCORPropertyProducer(PropertyIdentifier.operationExpected),
            };

        if (ObjectType.loadControl.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentValue),
                    new ObjectCORPropertyProducer(PropertyIdentifier.requestedShedLevel),
                    new ObjectCORPropertyProducer(PropertyIdentifier.actualShedLevel),
            };

        if (ObjectType.loop.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentValue),
                    new ObjectCORPropertyProducer(PropertyIdentifier.controlledVariableValue),
                    new ObjectCORPropertyProducer(PropertyIdentifier.setpoint),
            };

        if (ObjectType.program.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.programState),
                    new ObjectCORPropertyProducer(PropertyIdentifier.controlledVariableValue),
                    new ObjectCORPropertyProducer(PropertyIdentifier.descriptionOfHalt),
            };

        if (ObjectType.staging.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentValue),
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentStage),
            };

        if (ObjectType.timer.equals(objectType))
            return new CORPropertyValueProducer[] {
                    new ObjectCORPropertyProducer(PropertyIdentifier.presentValue),
                    new ObjectCORPropertyProducer(PropertyIdentifier.timerState),
                    new ObjectCORPropertyProducer(PropertyIdentifier.updateTime),
                    new ObjectCORPropertyProducer(PropertyIdentifier.lastStateChange),
                    new ObjectCORPropertyProducer(PropertyIdentifier.initialTimeout),
                    new ObjectCORPropertyProducer(PropertyIdentifier.expirationTime),
            };

        return new CORPropertyValueProducer[] {};
    }
}
