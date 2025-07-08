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

package com.serotonin.bacnet4j.obj.mixin.event.faultAlgo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.AbstractFaultParameter;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultState;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyStates;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

// 13.4.5
public class FaultStateAlgo extends FaultAlgorithm {
    static final Logger LOG = LoggerFactory.getLogger(FaultStateAlgo.class);

    private final PropertyIdentifier currentReliabilityProperty;
    private final PropertyIdentifier faultValuesProperty;

    public FaultStateAlgo() {
        this(null, null);
    }

    public FaultStateAlgo(final PropertyIdentifier currentReliabilityProperty,
            final PropertyIdentifier faultValuesProperty) {
        this.currentReliabilityProperty = currentReliabilityProperty;
        this.faultValuesProperty = faultValuesProperty;
    }

    @Override
    public Reliability evaluateIntrinsic(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            final BACnetObject bo) {
        return evaluate( //
                oldMonitoredValue, //
                newMonitoredValue, //
                bo.get(currentReliabilityProperty), //
                bo.get(faultValuesProperty));
    }

    @Override
    public Reliability evaluateAlgorithmic(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            final Reliability currentReliability, final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractFaultParameter parameters) {
        final FaultState p = (FaultState) parameters;
        return evaluate( //
                oldMonitoredValue, //
                newMonitoredValue, //
                currentReliability, //
                p.getListOfFaultValues());
    }

    private static Reliability evaluate(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            Reliability currentReliability, final SequenceOf<? extends Encodable> faultValues) {
        if (currentReliability == null)
            currentReliability = Reliability.noFaultDetected;

        Reliability newReliability = null;

        // If this is intrinsic reporting the fault values will be a sequence of encodables (unsigned for MV).
        // If this is algorithmic, the fault values will be a sequence of PropertyStates
        boolean isFaultValue = false;
        for (final Encodable e : faultValues) {
            if (e.equals(newMonitoredValue)) {
                isFaultValue = true;
                break;
            } else if (e instanceof PropertyStates) {
                if (((PropertyStates) e).getState().equals(e)) {
                    isFaultValue = true;
                    break;
                }
            }
        }

        if (currentReliability.equals(Reliability.noFaultDetected) && isFaultValue)
            newReliability = Reliability.multiStateFault;
        else if (currentReliability.equals(Reliability.multiStateFault) && !isFaultValue)
            newReliability = Reliability.noFaultDetected;
        else if (currentReliability.equals(Reliability.multiStateFault) && isFaultValue
                && !faultValues.equals(oldMonitoredValue))
            newReliability = Reliability.multiStateFault;

        if (newReliability != null)
            LOG.debug("FaultState evaluated new reliability: {}", newReliability);

        return newReliability;
    }
}
