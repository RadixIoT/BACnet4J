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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.EventEnrollmentObject;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultAlgorithm;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.AbstractFaultParameter;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;

/**
 * Provides support for algorithmic reporting, particularly for the EventEnrollment object.
 *
 * @author Matthew
 */
public class AlgoReportingMixin extends EventReportingMixin {
    static final Logger LOG = LoggerFactory.getLogger(AlgoReportingMixin.class);

    private final AbstractEventParameter eventParameter;
    private final AbstractFaultParameter faultParameter;
    private final DeviceObjectPropertyReference objectPropertyReference;

    private Encodable monitoredPropertyValue;
    private Map<ObjectPropertyReference, Encodable> additionalValues;

    public AlgoReportingMixin(final EventEnrollmentObject ee, final EventAlgorithm eventAlgo,
            final AbstractEventParameter eventParameter, final FaultAlgorithm faultAlgo,
            final AbstractFaultParameter faultParameter, final DeviceObjectPropertyReference objectPropertyReference) {
        super(ee, eventAlgo, faultAlgo);

        ee.writePropertyInternal(PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.FALSE);

        this.eventParameter = eventParameter;
        this.faultParameter = faultParameter;
        this.objectPropertyReference = objectPropertyReference;

        setPostNotificationAction((notifParams) -> {
            eventParameter.postNotification(notifParams);
        });
    }

    public synchronized void updateValue(final Encodable newValue,
            final Map<ObjectPropertyReference, Encodable> additionalValues) {
        final Encodable oldValue = monitoredPropertyValue;
        monitoredPropertyValue = newValue;
        this.additionalValues = additionalValues;

        // Check if the value has changed to a fault value.
        final boolean fault = executeFaultAlgo(oldValue, monitoredPropertyValue);

        if (!fault) {
            // Ensure there is no current fault.
            final Reliability reli = get(PropertyIdentifier.reliability);
            if (reli == null || reli.equals(Reliability.noFaultDetected))
                // No fault detected. Run the event algorithm
                executeEventAlgo();
        }
    }

    @Override
    protected StateTransition evaluateEventState(final BACnetObject bo, final EventAlgorithm eventAlgo) {
        return eventAlgo.evaluateAlgorithmicEventState(bo, monitoredPropertyValue,
                objectPropertyReference.getObjectIdentifier(), additionalValues, eventParameter);
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
        return eventAlgo.getAlgorithmicNotificationParameters(bo, fromState, toState, monitoredPropertyValue,
                objectPropertyReference.getObjectIdentifier(), additionalValues, eventParameter);
    }

    @Override
    protected Reliability evaluateFaultState(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            final BACnetObject bo, final FaultAlgorithm faultAlgo) {
        return faultAlgo.evaluateAlgorithmic(oldMonitoredValue, newMonitoredValue,
                bo.get(PropertyIdentifier.reliability), objectPropertyReference.getObjectIdentifier(), additionalValues,
                faultParameter);
    }

    @Override
    protected PropertyValue getEventEnrollmentMonitoredProperty(final PropertyIdentifier pid) {
        // Have to do this while the monitored property is not in the additional values.
        if (pid.equals(objectPropertyReference.getPropertyIdentifier())) {
            return new PropertyValue(objectPropertyReference.getPropertyIdentifier(),
                    objectPropertyReference.getPropertyArrayIndex(), monitoredPropertyValue, null);
        }

        final Encodable value = additionalValues
                .get(new ObjectPropertyReference(objectPropertyReference.getObjectIdentifier(), pid));
        if (value == null) {
            LOG.debug("Could not find property {} in additional polled properties", pid);
            return null;
        }

        return new PropertyValue(pid, value);
    }
}
