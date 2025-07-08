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

import java.util.Objects;

import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Mixin that manages the following properties:
 * - change-of-state-time
 * - change-of-state-count
 * - time-of-state-count-reset
 *
 * @author Matthew
 */
public class StateChangeMixin extends AbstractMixin {
    public StateChangeMixin(final BACnetObject bo) {
        super(bo);

        // Default the values.
        writePropertyInternal(PropertyIdentifier.changeOfStateTime, DateTime.UNSPECIFIED);
        writePropertyInternal(PropertyIdentifier.changeOfStateCount, UnsignedInteger.ZERO);
        writePropertyInternal(PropertyIdentifier.timeOfStateCountReset, new DateTime(getLocalDevice()));
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (pid.equals(PropertyIdentifier.presentValue)) {
            if (!Objects.equals(oldValue, newValue)) {
                writePropertyInternal(PropertyIdentifier.changeOfStateTime, new DateTime(getLocalDevice()));
                final UnsignedInteger changeOfStateCount = get(PropertyIdentifier.changeOfStateCount);
                writePropertyInternal(PropertyIdentifier.changeOfStateCount, changeOfStateCount.increment16());
            }
        } else if (pid.equals(PropertyIdentifier.changeOfStateCount)) {
            final UnsignedInteger changeOfStateCount = (UnsignedInteger) newValue;
            if (changeOfStateCount.intValue() == 0) {
                writePropertyInternal(PropertyIdentifier.timeOfStateCountReset, new DateTime(getLocalDevice()));
            }
        }
    }
}
