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

import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.eventState;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.outOfService;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.reliability;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.statusFlags;

import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;

public class HasStatusFlagsMixin extends AbstractMixin {
    private boolean overridden;

    public HasStatusFlagsMixin(final BACnetObject bo) {
        super(bo);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (statusFlags.equals(value.getPropertyIdentifier()))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        return false;
    }

    @Override
    public void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue, final Encodable newValue) {
        if (pid.isOneOf(eventState, reliability, outOfService))
            update();
    }

    private void update() {
        // Get the status flags object and associated values.
        final EventState eventState = get(PropertyIdentifier.eventState);
        final Reliability reliability = get(PropertyIdentifier.reliability);
        final Boolean outOfService = get(PropertyIdentifier.outOfService, Boolean.FALSE);

        // Update the status flags
        final StatusFlags statusFlags = new StatusFlags(//
                !EventState.normal.equals(eventState), //
                reliability == null ? false : !Reliability.noFaultDetected.equals(reliability), //
                overridden, //
                outOfService == null ? false : outOfService.booleanValue());
        writePropertyInternal(PropertyIdentifier.statusFlags, statusFlags);
    }

    public boolean isOverridden() {
        return overridden;
    }

    public void setOverridden(final boolean overridden) {
        this.overridden = overridden;
        update();
    }
}
