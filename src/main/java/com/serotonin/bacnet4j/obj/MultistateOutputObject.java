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

import java.util.Objects;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.CommandableMixin;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.MultistateMixin;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.obj.mixin.WritablePropertyOutOfServiceMixin;
import com.serotonin.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.CommandFailureAlgo;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.OptionalUnsigned;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class MultistateOutputObject extends BACnetObject {
    public MultistateOutputObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final int numberOfStates, final BACnetArray<CharacterString> stateText, final int presentValueBase1,
            final int relinquishDefaultBase1, final boolean outOfService) throws BACnetServiceException {
        super(localDevice, ObjectType.multiStateOutput, instanceNumber, name);

        if (numberOfStates < 1) {
            throw new IllegalArgumentException("numberOfStates cannot be less than 1");
        }

        final ValueSource valueSource = new ValueSource(new DeviceObjectReference(localDevice.getId(), getId()));

        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writeProperty(valueSource, PropertyIdentifier.presentValue, new UnsignedInteger(presentValueBase1));
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.TRUE);
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.interfaceValue, new OptionalUnsigned());

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new CommandableMixin(this, PropertyIdentifier.presentValue));
        addMixin(new MultistateMixin(this));

        writePropertyInternal(PropertyIdentifier.numberOfStates, new UnsignedInteger(numberOfStates));
        if (stateText != null) {
            if (numberOfStates != stateText.getCount()) {
                throw new IllegalArgumentException("numberOfStates does not match state text count");
            }
            writeProperty(null, PropertyIdentifier.stateText, stateText);
        }
        writeProperty(valueSource, PropertyIdentifier.presentValue, new UnsignedInteger(presentValueBase1));
        if (!outOfService) {
            writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        }

        addMixin(new WritablePropertyOutOfServiceMixin(this, PropertyIdentifier.reliability));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.ackedTransitions,
                PropertyIdentifier.eventTimeStamps, PropertyIdentifier.eventMessageTexts));

        _supportCommandable(new UnsignedInteger(relinquishDefaultBase1));
        _supportValueSource();

        localDevice.addObject(this);
    }

    public MultistateOutputObject supportCovReporting() {
        _supportCovReporting(null, null);
        return this;
    }

    public MultistateOutputObject supportIntrinsicReporting(final int timeDelay, final int notificationClass,
            final int feedbackValue, final EventTransitionBits eventEnable, final NotifyType notifyType,
            final int timeDelayNormal) {
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        writePropertyInternal(PropertyIdentifier.timeDelay, new UnsignedInteger(timeDelay));
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.feedbackValue, new UnsignedInteger(feedbackValue));
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.timeDelayNormal, new UnsignedInteger(timeDelayNormal));
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        addMixin(new IntrinsicReportingMixin(this, new CommandFailureAlgo(), null, PropertyIdentifier.presentValue,
                new PropertyIdentifier[] {PropertyIdentifier.presentValue, PropertyIdentifier.feedbackValue}));

        return this;
    }
}
