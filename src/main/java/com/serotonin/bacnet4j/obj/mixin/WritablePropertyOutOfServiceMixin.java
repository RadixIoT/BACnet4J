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

import java.util.HashSet;
import java.util.Set;

import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;

/**
 * Allows writing of the given properties only when the object is out of service.
 *
 * @author Matthew
 */
public class WritablePropertyOutOfServiceMixin extends AbstractMixin {
    private final Set<PropertyIdentifier> pids = new HashSet<>();

    public WritablePropertyOutOfServiceMixin(final BACnetObject bo, final PropertyIdentifier... pids) {
        super(bo);
        for (final PropertyIdentifier pid : pids)
            this.pids.add(pid);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        final Boolean outOfService = get(PropertyIdentifier.outOfService);
        if (!outOfService.booleanValue()) {
            if (pids.contains(value.getPropertyIdentifier()))
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        }
        return false;
    }
}
