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

import java.util.Collections;
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
 */
public class AlgoReportingMixin extends EventReportingMixin {
    static final Logger LOG = LoggerFactory.getLogger(AlgoReportingMixin.class);

    private final AbstractEventParameter eventParameter;
    private final AbstractFaultParameter faultParameter;
    private final DeviceObjectPropertyReference objectPropertyReference;

    private Encodable monitoredPropertyValue;
    private Map<ObjectPropertyReference, Encodable> additionalValues = Collections.emptyMap();

    public AlgoReportingMixin(EventEnrollmentObject ee, EventAlgorithm eventAlgo, AbstractEventParameter eventParameter,
            FaultAlgorithm faultAlgo, AbstractFaultParameter faultParameter,
            DeviceObjectPropertyReference objectPropertyReference) {
        super(ee, eventAlgo, faultAlgo);

        ee.writePropertyInternal(PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.FALSE);

        this.eventParameter = eventParameter;
        this.faultParameter = faultParameter;
        this.objectPropertyReference = objectPropertyReference;

        setPostNotificationAction(eventParameter::postNotification);
    }

    public synchronized void updateValue(Encodable newValue, Map<ObjectPropertyReference, Encodable> additionalValues) {
        Encodable oldValue = monitoredPropertyValue;
        monitoredPropertyValue = newValue;
        this.additionalValues = additionalValues;

        // Check if the value has changed to a fault value.
        boolean fault = executeFaultAlgo(oldValue, monitoredPropertyValue);

        if (!fault) {
            // Ensure there is no current fault.
            Reliability reliability = get(PropertyIdentifier.reliability);
            if (reliability == null || reliability.equals(Reliability.noFaultDetected))
                // No fault detected. Run the event algorithm
                executeEventAlgo();
        }
    }

    @Override
    protected StateTransition evaluateEventState(BACnetObject bo, EventAlgorithm eventAlgo) {
        if (monitoredPropertyValue == null) {
            // The monitored value has not yet been polled, so there is nothing to evaluate.
            return null;
        }
        return eventAlgo.evaluateAlgorithmicEventState(bo, monitoredPropertyValue,
                objectPropertyReference.getObjectIdentifier(), additionalValues, eventParameter);
    }

    @Override
    protected EventType getEventType(EventAlgorithm eventAlgo) {
        return eventAlgo.getEventType();
    }

    @Override
    protected boolean updateAckedTransitions() {
        return true;
    }

    @Override
    protected NotificationParameters getNotificationParameters(EventState fromState, EventState toState,
            BACnetObject bo, EventAlgorithm eventAlgo) {
        return eventAlgo.getAlgorithmicNotificationParameters(bo, fromState, toState, monitoredPropertyValue,
                objectPropertyReference.getObjectIdentifier(), additionalValues, eventParameter);
    }

    @Override
    protected Reliability evaluateFaultState(Encodable oldMonitoredValue, Encodable newMonitoredValue, BACnetObject bo,
            FaultAlgorithm faultAlgo) {
        return faultAlgo.evaluateAlgorithmic(oldMonitoredValue, newMonitoredValue,
                bo.get(PropertyIdentifier.reliability), objectPropertyReference.getObjectIdentifier(), additionalValues,
                faultParameter);
    }

    @Override
    protected PropertyValue getEventEnrollmentMonitoredProperty(PropertyIdentifier pid) {
        // Have to do this while the monitored property is not in the additional values.
        if (pid.equals(objectPropertyReference.getPropertyIdentifier())) {
            if (monitoredPropertyValue == null) {
                // The monitored value has not yet been polled.
                return null;
            }
            return new PropertyValue(objectPropertyReference.getPropertyIdentifier(),
                    objectPropertyReference.getPropertyArrayIndex(), monitoredPropertyValue, null);
        }

        Encodable value = additionalValues
                .get(new ObjectPropertyReference(objectPropertyReference.getObjectIdentifier(), pid));
        if (value == null) {
            LOG.debug("Could not find property {} in additional polled properties", pid);
            return null;
        }

        return new PropertyValue(pid, value);
    }
}
