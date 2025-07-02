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
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultOutOfRange;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Double;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

//13.4.7
public class FaultOutOfRangeAlgo extends FaultAlgorithm {
    static final Logger LOG = LoggerFactory.getLogger(FaultOutOfRangeAlgo.class);

    private final PropertyIdentifier minimumNormalValueProperty;
    private final PropertyIdentifier maximumNormalValueProperty;
    private final PropertyIdentifier currentReliabilityProperty;

    public FaultOutOfRangeAlgo() {
        this(null, null, null);
    }

    public FaultOutOfRangeAlgo(final PropertyIdentifier minimumNormalValueProperty,
            final PropertyIdentifier maximumNormalValueProperty, final PropertyIdentifier currentReliabilityProperty) {
        this.minimumNormalValueProperty = minimumNormalValueProperty;
        this.maximumNormalValueProperty = maximumNormalValueProperty;
        this.currentReliabilityProperty = currentReliabilityProperty;
    }

    @Override
    public Reliability evaluateIntrinsic(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            final BACnetObject bo) {
        return evaluate( //
                bo.get(minimumNormalValueProperty), //
                bo.get(maximumNormalValueProperty), //
                newMonitoredValue, //
                bo.get(currentReliabilityProperty));
    }

    @Override
    public Reliability evaluateAlgorithmic(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            final Reliability currentReliability, final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractFaultParameter parameters) {
        final FaultOutOfRange p = (FaultOutOfRange) parameters;
        return evaluate( //
                p.getMinNormalValue().getValue(), //
                p.getMaxNormalValue().getValue(), //
                newMonitoredValue, //
                currentReliability);
    }

    private static Reliability evaluate(final Encodable minimumNormalValue, final Encodable maximumNormalValue,
            final Encodable monitoredValue, final Reliability currentReliability) {
        // All of the Encodable parameters must have the same type, either Real, Unsigned, Double, or Signed.
        if (monitoredValue instanceof Real) {
            return evaluateFloating(((Real) minimumNormalValue).floatValue(), ((Real) maximumNormalValue).floatValue(),
                    ((Real) monitoredValue).floatValue(), currentReliability);
        }
        if (monitoredValue instanceof Double) {
            return evaluateFloating(((Double) minimumNormalValue).doubleValue(),
                    ((Double) maximumNormalValue).doubleValue(), ((Double) monitoredValue).doubleValue(),
                    currentReliability);
        }
        if (monitoredValue instanceof UnsignedInteger) {
            return evaluateInteger(((UnsignedInteger) minimumNormalValue).longValue(),
                    ((UnsignedInteger) maximumNormalValue).longValue(), ((UnsignedInteger) monitoredValue).longValue(),
                    currentReliability);
        }
        if (monitoredValue instanceof SignedInteger) {
            return evaluateInteger(((SignedInteger) minimumNormalValue).longValue(),
                    ((SignedInteger) maximumNormalValue).longValue(), ((SignedInteger) monitoredValue).longValue(),
                    currentReliability);
        }
        throw new RuntimeException("Unsupported type: " + monitoredValue.getClass());
    }

    private static Reliability evaluateInteger(final long min, final long max, final long value,
            Reliability currentReliability) {
        if (currentReliability == null)
            currentReliability = Reliability.noFaultDetected;

        Reliability newReliability = null;

        if (Reliability.noFaultDetected.equals(currentReliability) && value < min) // (a)
            newReliability = Reliability.underRange;
        else if (Reliability.noFaultDetected.equals(currentReliability) && value > max) // (b)
            newReliability = Reliability.overRange;
        else if (Reliability.underRange.equals(currentReliability) && value > max) // (c)
            newReliability = Reliability.overRange;
        else if (Reliability.overRange.equals(currentReliability) && value < min) // (d)
            newReliability = Reliability.underRange;
        else if (Reliability.underRange.equals(currentReliability) && value >= min) // (e)
            newReliability = Reliability.noFaultDetected;
        else if (Reliability.overRange.equals(currentReliability) && value <= max) // (f)
            newReliability = Reliability.noFaultDetected;

        if (newReliability != null)
            LOG.debug("FaultState evaluated new reliability: {}", newReliability);

        return newReliability;
    }

    private static Reliability evaluateFloating(final double min, final double max, final double value,
            Reliability currentReliability) {
        if (currentReliability == null)
            currentReliability = Reliability.noFaultDetected;

        Reliability newReliability = null;

        if (Reliability.noFaultDetected.equals(currentReliability) && value < min) // (a)
            newReliability = Reliability.underRange;
        else if (Reliability.noFaultDetected.equals(currentReliability) && value > max) // (b)
            newReliability = Reliability.overRange;
        else if (Reliability.underRange.equals(currentReliability) && value > max) // (c)
            newReliability = Reliability.overRange;
        else if (Reliability.overRange.equals(currentReliability) && value < min) // (d)
            newReliability = Reliability.underRange;
        else if (Reliability.underRange.equals(currentReliability) && value >= min) // (e)
            newReliability = Reliability.noFaultDetected;
        else if (Reliability.overRange.equals(currentReliability) && value <= max) // (f)
            newReliability = Reliability.noFaultDetected;

        if (newReliability != null)
            LOG.debug("FaultState evaluated new reliability: {}", newReliability);

        return newReliability;
    }
}
