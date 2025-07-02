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

import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.mixin.event.StateTransition;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

/**
 * Base class for implementations of event algorithms from 13.3
 *
 * @author Matthew
 */
abstract public class EventAlgorithm {
    abstract public EventType getEventType();

    /**
     * Evaluation of state transition in intrinsic reporting
     *
     * @param bo the object in which the intrinsic monitoring is occurring.
     * @return the state transition
     */
    abstract public StateTransition evaluateIntrinsicEventState(BACnetObject bo);

    /**
     * Return the notification parameters for intrinsic reporting.
     *
     * @param fromState
     * @param toState
     * @param bo        the object in which the intrinsic monitoring is occurring.
     * @return
     */
    abstract public NotificationParameters getIntrinsicNotificationParameters(EventState fromState, EventState toState,
            BACnetObject bo);

    /**
     * Additionally monitored properties for algorithmic reporting. See Table 12-15.1
     *
     * @return
     */
    abstract public PropertyIdentifier[] getAdditionalMonitoredProperties();

    /**
     * Evaluation of state transition in algorithmic reporting
     *
     * @param ee             the event enrollment object
     * @param monitoredValue
     * @param parameters
     * @return
     */
    abstract public StateTransition evaluateAlgorithmicEventState(BACnetObject ee, Encodable monitoredValue,
            ObjectIdentifier monitoredObjectReference, Map<ObjectPropertyReference, Encodable> additionalValues,
            AbstractEventParameter parameters);

    /**
     * Return the notification parameters for algorithmic reporting.
     *
     * @param ee               the event enrollment object
     * @param fromState
     * @param toState
     * @param monitoredValue
     * @param additionalValues the additional parameters values as per getAdditionalMonitoredProperties.
     * @param parameters
     * @return
     */
    abstract public NotificationParameters getAlgorithmicNotificationParameters(BACnetObject ee, EventState fromState,
            EventState toState, Encodable monitoredValue, ObjectIdentifier monitoredObjectReference,
            Map<ObjectPropertyReference, Encodable> additionalValues, AbstractEventParameter parameters);

    /**
     * Override as required to handle actual state changes.
     *
     * @param toState
     */
    public void stateChangeNotify(final EventState toState) {
        // no op
    }
}
