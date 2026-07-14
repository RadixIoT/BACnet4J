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

import java.util.function.Consumer;
import java.util.function.Predicate;

import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultAlgorithm;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.LimitEnable;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Mixin class for intrinsic reporting.
 *
 * @author Matthew
 */
public class IntrinsicReportingMixin extends EventReportingMixin {
    // Configuration
    private final PropertyIdentifier monitoredProperty;
    private final PropertyIdentifier[] triggerProperties;
    private Predicate<BACnetObject> configurationConflictCheck;
    private PropertyIdentifier[] configurationProperties = new PropertyIdentifier[0];

    // Runtime
    private boolean configurationConflict;

    public IntrinsicReportingMixin(final BACnetObject bo, final EventAlgorithm eventAlgo,
            final FaultAlgorithm faultAlgo, final PropertyIdentifier monitoredProperty,
            final PropertyIdentifier[] triggerProperties) {
        super(bo, eventAlgo, faultAlgo);

        bo.writePropertyInternal(PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.FALSE);

        this.monitoredProperty = monitoredProperty;
        this.triggerProperties = triggerProperties;

        // Update the state with the current values in the object.
        for (final PropertyIdentifier pid : triggerProperties)
            afterWriteProperty(pid, null, get(pid));
    }

    public IntrinsicReportingMixin withPostNotificationAction(
            final Consumer<NotificationParameters> postNotificationAction) {
        setPostNotificationAction(postNotificationAction);
        return this;
    }

    /**
     * Registers a check for conflicting event and fault parameters per addendum 135-2020co-2. While
     * the given predicate holds, the Reliability property takes on the value CONFIGURATION_ERROR,
     * which inhibits the execution of the event algorithm. The predicate is evaluated at registration
     * and after every write of one of the given properties.
     */
    public IntrinsicReportingMixin withConfigurationConflictCheck(final Predicate<BACnetObject> check,
            final PropertyIdentifier... configurationProperties) {
        this.configurationConflictCheck = check;
        this.configurationProperties = configurationProperties;
        evaluateConfigurationConflict();
        return this;
    }

    public IntrinsicReportingMixin withAlarmFaultCommonPropertyConflictCheck() {
        // Per 12.37.8, 12.18.9, 12.20.8, 12.26.8 the Reliability property takes on CONFIGURATION_ERROR
        // if a value is present in both the Alarm_Values property and the Fault_Values property.
        withConfigurationConflictCheck(
                o -> SequenceOf.intersection(
                        o.get(PropertyIdentifier.alarmValues),
                        o.get(PropertyIdentifier.faultValues)),
                PropertyIdentifier.alarmValues,
                PropertyIdentifier.faultValues);
        return this;
    }

    public IntrinsicReportingMixin withHighLimitBelowLowLimitConflictCheck() {
        //    // Per 12.3.9, 12.23.9 the Reliability property takes on CONFIGURATION_ERROR
        //    // if both limits are enabled and High_Limit is less than Low_Limit.
        withConfigurationConflictCheck(o -> {
            LimitEnable le = o.get(PropertyIdentifier.limitEnable);
            Real high = o.get(PropertyIdentifier.highLimit);
            Real low = o.get(PropertyIdentifier.lowLimit);
            return le.isHighLimitEnable() && le.isLowLimitEnable() && high.floatValue() < low.floatValue();
        }, PropertyIdentifier.limitEnable, PropertyIdentifier.highLimit, PropertyIdentifier.lowLimit);
        return this;
    }

    public IntrinsicReportingMixin withHighLimitBelowLowLimitFaultUnsignedConflictCheck() {
        // Per 12.2.9, 12.4.8, 12.39.8, 12.43.8, 12.44.8, 12.61.9 the Reliability property takes on
        // CONFIGURATION_ERROR if both limits are enabled and High_Limit is less than Low_Limit, or if
        // Fault_High_Limit is less than Fault_Low_Limit.
        withConfigurationConflictCheck(o -> {
                    LimitEnable le = o.get(PropertyIdentifier.limitEnable);
                    UnsignedInteger high = o.get(PropertyIdentifier.highLimit);
                    UnsignedInteger low = o.get(PropertyIdentifier.lowLimit);
                    UnsignedInteger faultHigh = o.get(PropertyIdentifier.faultHighLimit);
                    UnsignedInteger faultLow = o.get(PropertyIdentifier.faultLowLimit);
                    return (le.isHighLimitEnable() && le.isLowLimitEnable() && high.longValue() < low.longValue())
                            || faultHigh.longValue() < faultLow.longValue();
                },
                PropertyIdentifier.limitEnable, PropertyIdentifier.highLimit, PropertyIdentifier.lowLimit,
                PropertyIdentifier.faultHighLimit, PropertyIdentifier.faultLowLimit);
        return this;
    }

    public IntrinsicReportingMixin withHighLimitBelowLowLimitFaultRealConflictCheck() {
        // Per 12.2.9, 12.4.8, 12.39.8, 12.43.8, 12.44.8, 12.61.9 the Reliability property takes on
        // CONFIGURATION_ERROR if both limits are enabled and High_Limit is less than Low_Limit, or if
        // Fault_High_Limit is less than Fault_Low_Limit.
        withConfigurationConflictCheck(o -> {
                    LimitEnable le = o.get(PropertyIdentifier.limitEnable);
                    Real high = o.get(PropertyIdentifier.highLimit);
                    Real low = o.get(PropertyIdentifier.lowLimit);
                    Real faultHigh = o.get(PropertyIdentifier.faultHighLimit);
                    Real faultLow = o.get(PropertyIdentifier.faultLowLimit);
                    return (le.isHighLimitEnable() && le.isLowLimitEnable() && high.floatValue() < low.floatValue())
                            || faultHigh.floatValue() < faultLow.floatValue();
                }, PropertyIdentifier.limitEnable, PropertyIdentifier.highLimit, PropertyIdentifier.lowLimit,
                PropertyIdentifier.faultHighLimit, PropertyIdentifier.faultLowLimit);
        return this;
    }

    public IntrinsicReportingMixin withLifeSafetyCommonPropertyConflictCheck() {
        // Per 12.15.10, 12.16.10 the Reliability property takes on CONFIGURATION_ERROR
        // if a value is found in more than one of the Fault_Values, Alarm_Values, or
        // Life_Safety_Alarm_Values properties.
        withConfigurationConflictCheck(o -> {
            BACnetArray<LifeSafetyState> lifeSafetyAlarm = o.get(PropertyIdentifier.lifeSafetyAlarmValues);
            BACnetArray<LifeSafetyState> alarm = o.get(PropertyIdentifier.alarmValues);
            BACnetArray<LifeSafetyState> fault = o.get(PropertyIdentifier.faultValues);
            return SequenceOf.intersection(alarm, fault)
                    || SequenceOf.intersection(lifeSafetyAlarm, fault)
                    || SequenceOf.intersection(alarm, lifeSafetyAlarm);
        }, PropertyIdentifier.lifeSafetyAlarmValues, PropertyIdentifier.alarmValues, PropertyIdentifier.faultValues);
        return this;
    }



    private synchronized void evaluateConfigurationConflict() {
        if (configurationConflictCheck == null)
            return;
        final boolean conflict = configurationConflictCheck.test(getBo());
        if (conflict) {
            configurationConflict = true;
            if (!Reliability.configurationError.equals(get(PropertyIdentifier.reliability))) {
                writePropertyInternal(PropertyIdentifier.reliability, Reliability.configurationError);
            }
        } else if (configurationConflict) {
            // The conflict has been resolved. Only restore the reliability that this check set, so
            // that reliabilities from other sources are not clobbered.
            configurationConflict = false;
            if (Reliability.configurationError.equals(get(PropertyIdentifier.reliability))) {
                writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
            }
        }
    }

    @Override
    protected synchronized void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);

        if (pid.isOneOf(configurationProperties)) {
            evaluateConfigurationConflict();
        }

        if (configurationConflict) {
            // Per addendum 135-2020co-2, a conflicting configuration inhibits the execution of the
            // event and fault algorithms.
            return;
        }

        if (pid.isOneOf(triggerProperties)) {
            // Get the monitored value, in case this isn't it.
            final Encodable prev, curr;
            if (pid.equals(monitoredProperty)) {
                prev = oldValue;
                curr = newValue;
            } else {
                prev = null;
                curr = get(monitoredProperty);
            }

            // Check if there was a fault state transition.
            final boolean fault = executeFaultAlgo(prev, curr);
            if (!fault) {
                // Ensure there is no current fault.
                final Reliability reli = get(PropertyIdentifier.reliability);
                if (reli == null || reli.equals(Reliability.noFaultDetected))
                    // No fault detected. Run the event algorithm
                    executeEventAlgo();
            }
        }
    }

    @Override
    protected StateTransition evaluateEventState(final BACnetObject bo, final EventAlgorithm eventAlgo) {
        return eventAlgo.evaluateIntrinsicEventState(bo);
    }

    @Override
    protected EventType getEventType(final EventAlgorithm eventAlgo) {
        return eventAlgo.getEventType();
    }

    @Override
    protected boolean updateAckedTransitions() {
        return true;
    }

    @Override
    protected NotificationParameters getNotificationParameters(final EventState fromState, final EventState toState,
            final BACnetObject bo, final EventAlgorithm eventAlgo) {
        return eventAlgo.getIntrinsicNotificationParameters(fromState, toState, bo);
    }

    @Override
    protected Reliability evaluateFaultState(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            final BACnetObject bo, final FaultAlgorithm faultAlgo) {
        return faultAlgo.evaluateIntrinsic(oldMonitoredValue, newMonitoredValue, bo);
    }

    @Override
    protected PropertyValue getEventEnrollmentMonitoredProperty(final PropertyIdentifier pid) {
        throw new RuntimeException("Should not be called because EventEnrollment does not support intrinsic reporting");
    }
}
