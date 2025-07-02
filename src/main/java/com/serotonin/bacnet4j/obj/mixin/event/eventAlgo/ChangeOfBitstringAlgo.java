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
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfBitString;
import com.serotonin.bacnet4j.type.notificationParameters.ChangeOfBitStringNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Implements change of bitstring event algorithm.
 *
 * @author Matthew
 */
public class ChangeOfBitstringAlgo extends EventAlgorithm {
    static final Logger LOG = LoggerFactory.getLogger(ChangeOfBitstringAlgo.class);

    private final PropertyIdentifier monitoredValueProperty;
    private final PropertyIdentifier alarmValuesProperty;

    private BitString lastValue;
    private BitString lastValueCausingTransition;

    public ChangeOfBitstringAlgo() {
        this(null, null);
    }

    public ChangeOfBitstringAlgo(final PropertyIdentifier monitoredValueProperty,
            final PropertyIdentifier alarmValuesProperty) {
        this.monitoredValueProperty = monitoredValueProperty;
        this.alarmValuesProperty = alarmValuesProperty;
    }

    @Override
    public EventType getEventType() {
        return EventType.changeOfBitstring;
    }

    @Override
    public PropertyIdentifier[] getAdditionalMonitoredProperties() {
        return new PropertyIdentifier[] {PropertyIdentifier.statusFlags};
    }

    @Override
    public StateTransition evaluateIntrinsicEventState(final BACnetObject bo) {
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                bo.get(monitoredValueProperty), //
                bo.get(alarmValuesProperty), //
                bo.get(PropertyIdentifier.bitMask), //
                bo.get(PropertyIdentifier.timeDelay), //
                bo.get(PropertyIdentifier.timeDelayNormal));
    }

    @Override
    public StateTransition evaluateAlgorithmicEventState(final BACnetObject bo, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        final ChangeOfBitString p = (ChangeOfBitString) parameters;
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                (BitString) monitoredValue, //
                p.getListOfBitstringValues(), //
                p.getBitMask(), //
                p.getTimeDelay(), //
                bo.get(PropertyIdentifier.timeDelayNormal));
    }

    private StateTransition evaluateEventState(final EventState currentState, final BitString monitoredValue,
            final SequenceOf<BitString> alarmValues, final BitString bitmask, final UnsignedInteger timeDelay,
            UnsignedInteger timeDelayNormal) {
        if (timeDelayNormal == null)
            timeDelayNormal = timeDelay;

        LOG.debug("Current state: {}, monitored value: {}, alarm values: {}", currentState, monitoredValue,
                alarmValues);

        final BitString anded = monitoredValue.and(bitmask);
        lastValue = monitoredValue;

        if (currentState.equals(EventState.normal) && isAlarmValue(anded, alarmValues))
            return new StateTransition(EventState.offnormal, timeDelay);

        if (currentState.equals(EventState.offnormal) && !isAlarmValue(monitoredValue, alarmValues))
            return new StateTransition(EventState.normal, timeDelayNormal);

        if (currentState.equals(EventState.offnormal) && isAlarmValue(monitoredValue, alarmValues)
                && !monitoredValue.equals(lastValueCausingTransition))
            return new StateTransition(EventState.normal, timeDelayNormal);

        return null;
    }

    @Override
    public void stateChangeNotify(final EventState toState) {
        lastValueCausingTransition = lastValue;
    }

    private static boolean isAlarmValue(final BitString monitoredValue, final SequenceOf<BitString> alarmValues) {
        return alarmValues.contains(monitoredValue);
    }

    @Override
    public NotificationParameters getIntrinsicNotificationParameters(final EventState fromState,
            final EventState toState, final BACnetObject bo) {
        return getNotificationParameters(bo.get(PropertyIdentifier.statusFlags), bo.get(monitoredValueProperty));
    }

    @Override
    public NotificationParameters getAlgorithmicNotificationParameters(final BACnetObject bo,
            final EventState fromState, final EventState toState, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        return getNotificationParameters(
                (StatusFlags) additionalValues
                        .get(new ObjectPropertyReference(monitoredObjectReference, PropertyIdentifier.statusFlags)),
                (BitString) monitoredValue);
    }

    private static NotificationParameters getNotificationParameters(StatusFlags statusFlags,
            final BitString monitoredValue) {
        if (statusFlags == null)
            statusFlags = new StatusFlags(false, false, false, false);

        return new NotificationParameters(new ChangeOfBitStringNotif(monitoredValue, statusFlags));
    }
}
