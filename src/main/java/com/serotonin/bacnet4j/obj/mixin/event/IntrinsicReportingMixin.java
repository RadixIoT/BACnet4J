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

import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultAlgorithm;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;

/**
 * Mixin class for intrinsic reporting.
 *
 * @author Matthew
 */
public class IntrinsicReportingMixin extends EventReportingMixin {
    // Configuration
    private final PropertyIdentifier monitoredProperty;
    private final PropertyIdentifier[] triggerProperties;

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

    @Override
    protected synchronized void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);

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
