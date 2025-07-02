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

package com.serotonin.bacnet4j.obj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.obj.mixin.event.AlertReportingMixin;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.notificationParameters.ExtendedNotif.Parameter;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class AlertEnrollmentObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(AlertEnrollmentObject.class);

    private final AlertReportingMixin alertReporting;
    private final UnsignedInteger defaultVendorId;

    public AlertEnrollmentObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final int notificationClass, final NotifyType notifyType) throws BACnetServiceException {
        super(localDevice, ObjectType.alertEnrollment, instanceNumber, name);

        defaultVendorId = localDevice.get(PropertyIdentifier.vendorIdentifier);

        writePropertyInternal(PropertyIdentifier.presentValue, localDevice.getId());
        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.FALSE);
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.eventEnable, new EventTransitionBits(false, false, true));
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);

        // Mixins
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.presentValue));

        alertReporting = new AlertReportingMixin(this);
        addMixin(alertReporting);

        localDevice.addObject(this);
    }

    public void issueAlert(final ObjectIdentifier alertSource, final int extendedEventType,
            final Parameter... parameters) {
        issueAlert(alertSource, defaultVendorId, extendedEventType, parameters);
    }

    public void issueAlert(final ObjectIdentifier alertSource, final UnsignedInteger vendorId,
            final int extendedEventType, final Parameter... parameters) {
        // Delegate to the mixin
        alertReporting.issueAlert(alertSource, vendorId, new UnsignedInteger(extendedEventType), parameters);
    }
}
