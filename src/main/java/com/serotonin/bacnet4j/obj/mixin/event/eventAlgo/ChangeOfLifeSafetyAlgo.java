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

package com.serotonin.bacnet4j.obj.mixin.event.eventAlgo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.mixin.event.StateTransition;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyMode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfLifeSafety;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfLifeSafetyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class ChangeOfLifeSafetyAlgo extends EventAlgorithm {
    static final Logger LOG = LoggerFactory.getLogger(ChangeOfLifeSafetyAlgo.class);

    private LifeSafetyMode lastMode;
    private LifeSafetyState lastState;
    private LifeSafetyState lastStateCausingTransition;

    @Override
    public EventType getEventType() {
        return EventType.changeOfLifeSafety;
    }

    @Override
    public PropertyIdentifier[] getAdditionalMonitoredProperties() {
        return new PropertyIdentifier[] {PropertyIdentifier.statusFlags, PropertyIdentifier.operationExpected};
    }

    @Override
    public StateTransition evaluateIntrinsicEventState(final BACnetObject bo) {
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                bo.get(PropertyIdentifier.presentValue), //
                bo.get(PropertyIdentifier.mode), //
                bo.get(PropertyIdentifier.alarmValues), //
                bo.get(PropertyIdentifier.lifeSafetyAlarmValues), //
                bo.get(PropertyIdentifier.timeDelay), //
                bo.get(PropertyIdentifier.timeDelayNormal));
    }

    @Override
    public StateTransition evaluateAlgorithmicEventState(final BACnetObject ee, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        final ChangeOfLifeSafety p = (ChangeOfLifeSafety) parameters;
        return evaluateEventState( //
                ee.get(PropertyIdentifier.eventState), //
                (LifeSafetyState) monitoredValue, //
                (LifeSafetyMode) additionalValues.get(new ObjectPropertyReference( //
                        p.getModePropertyReference().getObjectIdentifier(), //
                        p.getModePropertyReference().getPropertyIdentifier(), //
                        p.getModePropertyReference().getPropertyArrayIndex())), //
                p.getListOfAlarmValues(), //
                p.getListOfLifeSafetyAlarmValues(), //
                p.getTimeDelay(), //
                ee.get(PropertyIdentifier.timeDelayNormal));
    }

    private StateTransition evaluateEventState(final EventState currentState, final LifeSafetyState monitoredValue,
            final LifeSafetyMode mode, final SequenceOf<LifeSafetyState> alarmValues,
            final SequenceOf<LifeSafetyState> lifeSafetyAlarmValues, final UnsignedInteger timeDelay,
            UnsignedInteger timeDelayNormal) {
        if (timeDelayNormal == null)
            timeDelayNormal = timeDelay;

        final boolean isAlarmValue = alarmValues.contains(monitoredValue);
        final boolean isLifeSafetyAlarmValue = lifeSafetyAlarmValues.contains(monitoredValue);
        final boolean modeChanged = lastMode != null && !mode.equals(lastMode);
        lastMode = mode;
        lastState = monitoredValue;

        // (a)
        if (currentState.equals(EventState.normal) && isAlarmValue) {
            if (modeChanged)
                return new StateTransition(EventState.offnormal, null);
            return new StateTransition(EventState.offnormal, timeDelay);
        }

        // (b)
        if (currentState.equals(EventState.normal) && isLifeSafetyAlarmValue) {
            if (modeChanged)
                return new StateTransition(EventState.lifeSafetyAlarm, null);
            return new StateTransition(EventState.lifeSafetyAlarm, timeDelay);
        }

        // (c)
        if (currentState.equals(EventState.normal) && modeChanged) {
            return new StateTransition(EventState.normal, null);
        }

        // (d)
        if (currentState.equals(EventState.offnormal) && !isAlarmValue && !isLifeSafetyAlarmValue) {
            if (modeChanged)
                return new StateTransition(EventState.normal, null);
            return new StateTransition(EventState.normal, timeDelayNormal);
        }

        // (e)
        if (currentState.equals(EventState.offnormal) && isLifeSafetyAlarmValue) {
            if (modeChanged)
                return new StateTransition(EventState.lifeSafetyAlarm, null);
            return new StateTransition(EventState.lifeSafetyAlarm, timeDelay);
        }

        // (f)
        if (currentState.equals(EventState.offnormal) && isAlarmValue
                && !monitoredValue.equals(lastStateCausingTransition)) {
            return new StateTransition(EventState.offnormal, timeDelay);
        }

        // (g)
        if (currentState.equals(EventState.offnormal) && modeChanged) {
            return new StateTransition(EventState.offnormal, null);
        }

        // (h)
        if (currentState.equals(EventState.lifeSafetyAlarm) && !isAlarmValue && !isLifeSafetyAlarmValue) {
            if (modeChanged)
                return new StateTransition(EventState.normal, null);
            return new StateTransition(EventState.normal, timeDelayNormal);
        }

        // (i)
        if (currentState.equals(EventState.lifeSafetyAlarm) && isAlarmValue) {
            if (modeChanged)
                return new StateTransition(EventState.offnormal, null);
            return new StateTransition(EventState.offnormal, timeDelay);
        }

        // (j)
        if (currentState.equals(EventState.lifeSafetyAlarm) && isLifeSafetyAlarmValue
                && !monitoredValue.equals(lastStateCausingTransition)) {
            return new StateTransition(EventState.lifeSafetyAlarm, timeDelay);
        }

        // (k)
        if (currentState.equals(EventState.lifeSafetyAlarm) && modeChanged) {
            return new StateTransition(EventState.lifeSafetyAlarm, null);
        }

        return null;
    }

    @Override
    public void stateChangeNotify(final EventState toState) {
        lastStateCausingTransition = lastState;
    }

    @Override
    public NotificationParameters getIntrinsicNotificationParameters(final EventState fromState,
            final EventState toState, final BACnetObject bo) {
        return getNotificationParameters( //
                bo.get(PropertyIdentifier.presentValue), //
                bo.get(PropertyIdentifier.mode), //
                bo.get(PropertyIdentifier.statusFlags), //
                bo.get(PropertyIdentifier.operationExpected));
    }

    @Override
    public NotificationParameters getAlgorithmicNotificationParameters(final BACnetObject bo,
            final EventState fromState, final EventState toState, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        final ChangeOfLifeSafety p = (ChangeOfLifeSafety) parameters;
        return getNotificationParameters( //
                (LifeSafetyState) monitoredValue, //
                (LifeSafetyMode) additionalValues.get(new ObjectPropertyReference( //
                        p.getModePropertyReference().getObjectIdentifier(), //
                        p.getModePropertyReference().getPropertyIdentifier(), //
                        p.getModePropertyReference().getPropertyArrayIndex())), //
                (StatusFlags) additionalValues
                        .get(new ObjectPropertyReference(monitoredObjectReference, PropertyIdentifier.statusFlags)),
                (LifeSafetyOperation) additionalValues.get(
                        new ObjectPropertyReference(monitoredObjectReference, PropertyIdentifier.operationExpected)));
    }

    private static NotificationParameters getNotificationParameters(final LifeSafetyState newState,
            final LifeSafetyMode newMode, StatusFlags statusFlags, final LifeSafetyOperation operationExpected) {
        if (statusFlags == null)
            statusFlags = new StatusFlags(false, false, false, false);

        return new NotificationParameters(
                new ChangeOfLifeSafetyNotif(newState, newMode, statusFlags, operationExpected));
    }
}
