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

import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.mixin.event.StateTransition;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.LimitEnable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.serotonin.bacnet4j.type.eventParameter.UnsignedRange;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.notificationParameters.UnsignedRangeNotif;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class UnsignedRangeAlgo extends EventAlgorithm {
    static final Logger LOG = LoggerFactory.getLogger(UnsignedRangeAlgo.class);

    private final LimitEnable algorithmicLimitEnable = new LimitEnable(true, true);
    private final PropertyIdentifier monitoredProperty;

    public UnsignedRangeAlgo() {
        this(null);
    }

    public UnsignedRangeAlgo(final PropertyIdentifier monitoredProperty) {
        this.monitoredProperty = monitoredProperty;
    }

    @Override
    public EventType getEventType() {
        return EventType.unsignedRange;
    }

    @Override
    public PropertyIdentifier[] getAdditionalMonitoredProperties() {
        return new PropertyIdentifier[] {PropertyIdentifier.statusFlags};
    }

    @Override
    public StateTransition evaluateIntrinsicEventState(final BACnetObject bo) {
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                bo.get(monitoredProperty), //
                bo.get(PropertyIdentifier.highLimit), //
                bo.get(PropertyIdentifier.lowLimit), //
                bo.get(PropertyIdentifier.limitEnable), //
                bo.get(PropertyIdentifier.timeDelay), //
                bo.get(PropertyIdentifier.timeDelayNormal));
    }

    @Override
    public StateTransition evaluateAlgorithmicEventState(final BACnetObject bo, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        final UnsignedRange p = (UnsignedRange) parameters;
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                (UnsignedInteger) monitoredValue, //
                p.getHighLimit(), //
                p.getLowLimit(), //
                algorithmicLimitEnable, // 13.3.6 pLimitEnable - default to both on.
                p.getTimeDelay(), //
                bo.get(PropertyIdentifier.timeDelayNormal));
    }

    private static StateTransition evaluateEventState(final EventState currentState,
            final UnsignedInteger monitoredValue, final UnsignedInteger highLimit, final UnsignedInteger lowLimit,
            final LimitEnable limitEnable, final UnsignedInteger timeDelay, UnsignedInteger timeDelayNormal) {
        final float monitoredValueInt = monitoredValue.intValue();
        final float highLimitInt = highLimit.intValue();
        final float lowLimitInt = lowLimit.intValue();

        if (timeDelayNormal == null)
            timeDelayNormal = timeDelay;

        LOG.debug("state={}, pv={}, highLimit={}, lowLimit={}", currentState, monitoredValueInt, highLimitInt,
                lowLimitInt);

        // (a)
        if (currentState.equals(EventState.normal) && limitEnable.isHighLimitEnable()
                && monitoredValueInt > highLimitInt)
            return new StateTransition(EventState.highLimit, timeDelay);

        // (b)
        if (currentState.equals(EventState.normal) && limitEnable.isLowLimitEnable() && monitoredValueInt < lowLimitInt)
            return new StateTransition(EventState.lowLimit, timeDelay);

        // (c)
        if (currentState.equals(EventState.highLimit) && !limitEnable.isHighLimitEnable())
            return new StateTransition(EventState.normal, null);

        // (d)
        if (currentState.equals(EventState.highLimit) && limitEnable.isLowLimitEnable()
                && monitoredValueInt < lowLimitInt)
            return new StateTransition(EventState.lowLimit, timeDelay);

        // (e)
        if (currentState.equals(EventState.highLimit) && monitoredValueInt < highLimitInt)
            return new StateTransition(EventState.normal, timeDelayNormal);

        // (f)
        if (currentState.equals(EventState.lowLimit) && !limitEnable.isLowLimitEnable())
            return new StateTransition(EventState.normal, null);

        // (g)
        if (currentState.equals(EventState.lowLimit) && limitEnable.isHighLimitEnable()
                && monitoredValueInt > highLimitInt)
            return new StateTransition(EventState.highLimit, timeDelay);

        // (h)
        if (currentState.equals(EventState.lowLimit) && monitoredValueInt > lowLimitInt)
            return new StateTransition(EventState.normal, timeDelayNormal);

        return null;
    }

    @Override
    public NotificationParameters getIntrinsicNotificationParameters(final EventState fromState,
            final EventState toState, final BACnetObject bo) {
        return getNotificationParameters(fromState, toState, //
                bo.get(PropertyIdentifier.lowLimit), //
                bo.get(PropertyIdentifier.highLimit), //
                bo.get(monitoredProperty), //
                bo.get(PropertyIdentifier.statusFlags));
    }

    @Override
    public NotificationParameters getAlgorithmicNotificationParameters(final BACnetObject bo,
            final EventState fromState, final EventState toState, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        final UnsignedRange p = (UnsignedRange) parameters;
        return getNotificationParameters(fromState, toState, //
                p.getLowLimit(), //
                p.getHighLimit(), //
                (UnsignedInteger) monitoredValue, //
                (StatusFlags) additionalValues
                        .get(new ObjectPropertyReference(monitoredObjectReference, PropertyIdentifier.statusFlags)));
    }

    private static NotificationParameters getNotificationParameters(final EventState fromState,
            final EventState toState, final UnsignedInteger lowLimit, final UnsignedInteger highLimit,
            final UnsignedInteger monitoredValue, final StatusFlags statusFlags) {
        UnsignedInteger exceededLimit;
        if (EventState.lowLimit.equals(toState) //
                || EventState.lowLimit.equals(fromState) && EventState.normal.equals(toState))
            exceededLimit = lowLimit;
        else if (EventState.highLimit.equals(toState) //
                || EventState.highLimit.equals(fromState) && EventState.normal.equals(toState))
            exceededLimit = highLimit;
        else
            throw new BACnetRuntimeException("Invalid state transition: " + toState + " to " + fromState);

        LOG.debug("Notification parameters: from={}, to={}, exceededLimit={}", fromState, toState, exceededLimit);

        return new NotificationParameters(new UnsignedRangeNotif(monitoredValue, statusFlags, exceededLimit));
    }
}
