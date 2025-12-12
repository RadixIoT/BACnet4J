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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.obj.mixin.WritablePropertyOutOfServiceMixin;
import com.serotonin.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.UnsignedRangeAlgo;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultOutOfRangeAlgo;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LimitEnable;
import com.serotonin.bacnet4j.type.constructed.Prescale;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Scale;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class AccumulatorObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(AccumulatorObject.class);


    public static enum ValueSetWrite {
        // Neither valueBeforeChange nor valueSet are writable.
        readOnly,
        // Only valueBeforeChange is writable
        valueBeforeChange,
        // Only valueSet is writable
        valueSet;
    }


    private final Object lock = new Object();
    private long accumulation;
    private long pulseCount;
    private ValueSetWrite valueSetWrite = ValueSetWrite.readOnly;
    private final ScheduledFuture<?> limitMonitoringFuture;

    public AccumulatorObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final long presentValue, final long accumulation, final EngineeringUnits units, final boolean outOfService,
            final Scale scale, final Prescale prescale, final long maxPresValue, final int limitMonitoringInterval)
            throws BACnetServiceException {
        super(localDevice, ObjectType.accumulator, instanceNumber, name);

        Objects.requireNonNull(units);
        Objects.requireNonNull(scale);

        writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(presentValue));
        writePropertyInternal(PropertyIdentifier.pulseRate, UnsignedInteger.ZERO);
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, outOfService));
        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.units, units);
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        writePropertyInternal(PropertyIdentifier.scale, scale);
        if (prescale != null)
            writePropertyInternal(PropertyIdentifier.prescale, prescale);
        writePropertyInternal(PropertyIdentifier.maxPresValue, new UnsignedInteger(maxPresValue));
        writePropertyInternal(PropertyIdentifier.limitMonitoringInterval, new UnsignedInteger(limitMonitoringInterval));
        set(PropertyIdentifier.valueChangeTime, DateTime.UNSPECIFIED);
        set(PropertyIdentifier.valueBeforeChange, UnsignedInteger.ZERO);
        set(PropertyIdentifier.valueSet, UnsignedInteger.ZERO);

        this.accumulation = accumulation;

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new WritablePropertyOutOfServiceMixin(this, PropertyIdentifier.presentValue,
                PropertyIdentifier.pulseRate, PropertyIdentifier.reliability));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.eventMessageTexts,
                PropertyIdentifier.valueChangeTime, PropertyIdentifier.loggingRecord,
                PropertyIdentifier.limitMonitoringInterval));

        // Schedule the job to monitor the pulse rate.
        limitMonitoringFuture = getLocalDevice().scheduleAtFixedRate(() -> {
            synchronized (lock) {
                LOG.info("Setting pulse rate to {}", pulseCount);
                writePropertyInternal(PropertyIdentifier.pulseRate, new UnsignedInteger(pulseCount));
                pulseCount = 0;
            }
        }, limitMonitoringInterval, limitMonitoringInterval, TimeUnit.SECONDS);

        // TODO
        // ?? Logging_Record
        // ?? Logging_Object -> not currently possible to know what is making a request to read a property.

        localDevice.addObject(this);
    }

    public AccumulatorObject supportIntrinsicReporting(final int highLimit, final int lowLimit,
            final int faultHighLimit, final int faultLowLimit, final int timeDelay,
            final UnsignedInteger timeDelayNormal, final int notificationClass, final LimitEnable limitEnable,
            final EventTransitionBits eventEnable, final NotifyType notifyType) {

        Objects.requireNonNull(limitEnable);
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        // Prepare the object with all of the properties that intrinsic reporting will need.
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.timeDelay, new UnsignedInteger(timeDelay));
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.highLimit, new UnsignedInteger(highLimit));
        writePropertyInternal(PropertyIdentifier.lowLimit, new UnsignedInteger(lowLimit));
        writePropertyInternal(PropertyIdentifier.limitEnable, limitEnable);
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        if (timeDelayNormal != null)
            writePropertyInternal(PropertyIdentifier.timeDelayNormal, timeDelayNormal);
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);
        writePropertyInternal(PropertyIdentifier.faultHighLimit, new UnsignedInteger(faultHighLimit));
        writePropertyInternal(PropertyIdentifier.faultLowLimit, new UnsignedInteger(faultLowLimit));

        // Now add the mixin.
        addMixin(new IntrinsicReportingMixin(this, new UnsignedRangeAlgo(PropertyIdentifier.pulseRate),
                new FaultOutOfRangeAlgo(PropertyIdentifier.faultLowLimit, PropertyIdentifier.faultHighLimit,
                        PropertyIdentifier.reliability), //
                PropertyIdentifier.pulseRate, //
                new PropertyIdentifier[] {PropertyIdentifier.pulseRate, PropertyIdentifier.highLimit,
                        PropertyIdentifier.lowLimit, PropertyIdentifier.limitEnable}));

        return this;
    }

    public AccumulatorObject supportValueWrite(final ValueSetWrite valueSetWrite) {
        this.valueSetWrite = valueSetWrite;
        return this;
    }

    public long getAccumulation() {
        return accumulation;
    }

    public void pulse() {
        pulses(1);
    }

    public void pulses(final long count) {
        synchronized (lock) {
            if (count < 1)
                throw new IllegalArgumentException("count cannot be < 1");

            pulseCount += count;

            final UnsignedInteger presentValue = get(PropertyIdentifier.presentValue);
            final Prescale prescale = get(PropertyIdentifier.prescale);
            final UnsignedInteger maxPresValue = get(PropertyIdentifier.maxPresValue);

            long newPresentValue = presentValue.longValue();
            if (prescale == null) {
                // Just add the pulse count to the present value.
                newPresentValue += count;
            } else {
                accumulation += count * prescale.getMultiplier().longValue();
                newPresentValue += accumulation / prescale.getModuloDivide().longValue();
                accumulation %= prescale.getModuloDivide().longValue();
            }

            newPresentValue %= maxPresValue.longValue() + 1;

            writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(newPresentValue));
        }
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.presentValue.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger presentValue = value.getValue();
            final UnsignedInteger maxPresValue = get(PropertyIdentifier.maxPresValue);
            if (presentValue.longValue() > maxPresValue.longValue()) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
            }
        } else if (PropertyIdentifier.valueBeforeChange.equals(value.getPropertyIdentifier())) {
            if (valueSetWrite != ValueSetWrite.valueBeforeChange) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }
        } else if (PropertyIdentifier.valueSet.equals(value.getPropertyIdentifier())) {
            if (valueSetWrite != ValueSetWrite.valueSet) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }
            final UnsignedInteger valueSet = value.getValue();
            final UnsignedInteger maxPresValue = get(PropertyIdentifier.maxPresValue);
            if (valueSet.longValue() > maxPresValue.longValue()) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
            }
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (PropertyIdentifier.valueBeforeChange.equals(pid)) {
            synchronized (lock) {
                // 12.61.16
                final UnsignedInteger presentValue = get(PropertyIdentifier.presentValue);
                // Cannot call writePropertyInternal here because then the code below will be run.
                set(PropertyIdentifier.valueSet, presentValue);
                writePropertyInternal(PropertyIdentifier.valueChangeTime, new DateTime(getLocalDevice()));
            }
        } else if (PropertyIdentifier.valueSet.equals(pid)) {
            synchronized (lock) {
                // 12.61.17
                final UnsignedInteger presentValue = get(PropertyIdentifier.presentValue);
                // Cannot call writePropertyInternal here because then the code above will be run.
                set(PropertyIdentifier.valueBeforeChange, presentValue);
                set(PropertyIdentifier.presentValue, newValue);
                writePropertyInternal(PropertyIdentifier.valueChangeTime, new DateTime(getLocalDevice()));
            }
        }
    }

    @Override
    protected void terminateImpl() {
        limitMonitoringFuture.cancel(false);
    }
}
