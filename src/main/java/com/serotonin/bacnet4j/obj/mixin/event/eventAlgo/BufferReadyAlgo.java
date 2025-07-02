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
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.serotonin.bacnet4j.type.eventParameter.BufferReady;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class BufferReadyAlgo extends EventAlgorithm {
    static final Logger LOG = LoggerFactory.getLogger(BufferReadyAlgo.class);

    private final PropertyIdentifier monitoredValueProperty;
    private final DeviceObjectPropertyReference logBufferReference;
    private final PropertyIdentifier thresholdProperty;
    private final PropertyIdentifier previousCountProperty;

    public BufferReadyAlgo() {
        this(null, null, null, null);
    }

    public BufferReadyAlgo(final PropertyIdentifier monitoredValueProperty,
            final DeviceObjectPropertyReference logBufferReference, final PropertyIdentifier thresholdProperty,
            final PropertyIdentifier previousCountProperty) {
        this.monitoredValueProperty = monitoredValueProperty;
        this.logBufferReference = logBufferReference;
        this.thresholdProperty = thresholdProperty;
        this.previousCountProperty = previousCountProperty;
    }

    @Override
    public EventType getEventType() {
        return EventType.bufferReady;
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
                bo.get(thresholdProperty), //
                bo.get(previousCountProperty));
    }

    @Override
    public StateTransition evaluateAlgorithmicEventState(final BACnetObject bo, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        final BufferReady p = (BufferReady) parameters;
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                (UnsignedInteger) monitoredValue, //
                p.getNotificationThreshold(), //
                p.getMutablePreviousNotificationCount());
    }

    private static StateTransition evaluateEventState(final EventState currentState,
            final UnsignedInteger monitoredValue, final UnsignedInteger threshold,
            final UnsignedInteger previousCount) {
        final long mon = monitoredValue.longValue();
        final long thr = threshold.longValue();
        final long pre = previousCount.longValue();

        if (currentState.equals(EventState.normal) //
                && mon >= pre //
                && mon - pre >= thr //
                && thr > 0) {
            return new StateTransition(EventState.normal, null);
        }

        if (currentState.equals(EventState.normal) //
                && mon < pre //
                && mon - pre + 0xFFFFFFFFL >= thr //
                && thr > 0) {
            return new StateTransition(EventState.normal, null);
        }

        return null;
    }

    @Override
    public NotificationParameters getIntrinsicNotificationParameters(final EventState fromState,
            final EventState toState, final BACnetObject bo) {
        return getNotificationParameters(logBufferReference, bo.get(previousCountProperty),
                bo.get(monitoredValueProperty));
    }

    @Override
    public NotificationParameters getAlgorithmicNotificationParameters(final BACnetObject bo,
            final EventState fromState, final EventState toState, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        // The log buffer reference can be made from the object property reference, substituting the PID with logBuffer.
        final DeviceObjectPropertyReference ref = bo.get(PropertyIdentifier.objectPropertyReference);
        final DeviceObjectPropertyReference logBufferReference = new DeviceObjectPropertyReference(
                ref.getObjectIdentifier(), PropertyIdentifier.logBuffer, ref.getPropertyArrayIndex(),
                ref.getDeviceIdentifier());

        // The previous notification count can be pulled from the mutable value in the event parameters.
        final BufferReady p = (BufferReady) parameters;

        return getNotificationParameters(logBufferReference, p.getMutablePreviousNotificationCount(),
                (UnsignedInteger) monitoredValue);
    }

    private static NotificationParameters getNotificationParameters(
            final DeviceObjectPropertyReference logBufferReference, final UnsignedInteger previousNotification,
            final UnsignedInteger currentNotification) {
        return new NotificationParameters(
                new BufferReadyNotif(logBufferReference, previousNotification, currentNotification));
    }
}
