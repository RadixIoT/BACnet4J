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

package com.serotonin.bacnet4j.obj.mixin;

import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Mixin that manages the following properties:
 * - elapsed-active-time
 * - time-of-active-time-reset
 *
 * @author Matthew
 */
public class ActiveTimeMixin extends AbstractMixin {
    private final PropertyIdentifier monitoredValue;
    private long accumulatedActiveTime;
    private long lastActiveTime;

    /**
     * @param bo          the owning object
     * @param useFeedback whether to use present-value (false) or feedback-value (true) as the indicator for the
     *                    calculations.
     */
    public ActiveTimeMixin(final BACnetObject bo, final boolean useFeedback) {
        super(bo);

        // Default the values.
        writePropertyInternal(PropertyIdentifier.elapsedActiveTime, UnsignedInteger.ZERO);
        writePropertyInternal(PropertyIdentifier.timeOfActiveTimeReset, new DateTime(getLocalDevice()));

        if (useFeedback) {
            monitoredValue = PropertyIdentifier.feedbackValue;
        } else {
            monitoredValue = PropertyIdentifier.presentValue;
        }

        resetLastActiveTime();
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) {
        if (pid.equals(PropertyIdentifier.elapsedActiveTime)) {
            synchronized (monitoredValue) {
                long elapsed = accumulatedActiveTime;
                if (lastActiveTime != -1) {
                    elapsed += getLocalDevice().getClock().millis() - lastActiveTime;
                }
                set(PropertyIdentifier.elapsedActiveTime, new UnsignedInteger(elapsed / 1000));
            }
        }
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (pid.equals(monitoredValue)) {
            synchronized (monitoredValue) {
                final BinaryPV presentValue = (BinaryPV) newValue;
                if (presentValue.equals(BinaryPV.active)) {
                    if (lastActiveTime == -1) {
                        lastActiveTime = getLocalDevice().getClock().millis();
                    }
                } else {
                    if (lastActiveTime != -1) {
                        accumulatedActiveTime += getLocalDevice().getClock().millis() - lastActiveTime;
                        lastActiveTime = -1;
                    }
                }
            }
        } else if (pid.equals(PropertyIdentifier.elapsedActiveTime)) {
            synchronized (monitoredValue) {
                final UnsignedInteger elapsedActiveTime = (UnsignedInteger) newValue;
                accumulatedActiveTime = elapsedActiveTime.longValue() * 1000;
                resetLastActiveTime();
                if (elapsedActiveTime.longValue() == 0) {
                    writePropertyInternal(PropertyIdentifier.timeOfActiveTimeReset, new DateTime(getLocalDevice()));
                }
            }
        }
    }

    private void resetLastActiveTime() {
        final BinaryPV presentValue = get(monitoredValue);
        if (presentValue.equals(BinaryPV.active)) {
            lastActiveTime = getLocalDevice().getClock().millis();
        } else {
            lastActiveTime = -1;
        }
    }
}
