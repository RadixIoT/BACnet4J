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
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.OutOfRangeAlgo;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LimitEnable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
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
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class PulseConverterObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(PulseConverterObject.class);

    private final Object lock = new Object();
    private ScheduledFuture<?> inputMonitoringFuture;
    /**
     * This field maintains the last polling error that was received, or null if the poll was successful. It is kept
     * so that identical error messages are not spammed to the log, and also so that the reliability value is only
     * written when necessary.
     */
    private String lastPollingError;
    private long lastPollingValue = Long.MAX_VALUE;

    public PulseConverterObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final long count, final float scaleFactor, final EngineeringUnits units, final boolean outOfService)
            throws BACnetServiceException {
        super(localDevice, ObjectType.pulseConverter, instanceNumber, name);

        Objects.requireNonNull(units);

        set(PropertyIdentifier.count, new UnsignedInteger(count));
        writePropertyInternal(PropertyIdentifier.scaleFactor, new Real(scaleFactor));
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, outOfService));
        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.units, units);
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.adjustValue, new Real(0));
        writePropertyInternal(PropertyIdentifier.countChangeTime, DateTime.UNSPECIFIED);
        writePropertyInternal(PropertyIdentifier.countBeforeChange, UnsignedInteger.ZERO);

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new WritablePropertyOutOfServiceMixin(this, PropertyIdentifier.presentValue,
                PropertyIdentifier.reliability));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.count, PropertyIdentifier.updateTime,
                PropertyIdentifier.ackedTransitions, PropertyIdentifier.eventMessageTexts));

        localDevice.addObject(this);
    }

    public PulseConverterObject supportIntrinsicReporting(final float highLimit, final float lowLimit,
            final float deadband, final int timeDelay, final UnsignedInteger timeDelayNormal,
            final int notificationClass, final LimitEnable limitEnable, final EventTransitionBits eventEnable,
            final NotifyType notifyType) {

        Objects.requireNonNull(limitEnable);
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        // Prepare the object with all of the properties that intrinsic reporting will need.
        writePropertyInternal(PropertyIdentifier.timeDelay, new UnsignedInteger(timeDelay));
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.highLimit, new Real(highLimit));
        writePropertyInternal(PropertyIdentifier.lowLimit, new Real(lowLimit));
        writePropertyInternal(PropertyIdentifier.deadband, new Real(deadband));
        writePropertyInternal(PropertyIdentifier.limitEnable, limitEnable);
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        if (timeDelayNormal != null)
            writePropertyInternal(PropertyIdentifier.timeDelayNormal, timeDelayNormal);
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        // Now add the mixin.
        addMixin(new IntrinsicReportingMixin(this, new OutOfRangeAlgo(), null, PropertyIdentifier.presentValue, //
                new PropertyIdentifier[] {PropertyIdentifier.presentValue, PropertyIdentifier.highLimit,
                        PropertyIdentifier.lowLimit, PropertyIdentifier.deadband, PropertyIdentifier.limitEnable}));

        return this;
    }

    public PulseConverterObject supportCovReporting(final float covIncrement, final int covPeriod) {
        _supportCovReporting(new Real(covIncrement), new UnsignedInteger(covPeriod));
        return this;
    }

    public PulseConverterObject supportInputReference(final ObjectPropertyReference inputReference,
            final long pollingMillis) {
        Objects.requireNonNull(inputReference);
        writePropertyInternal(PropertyIdentifier.inputReference, inputReference);

        inputMonitoringFuture = getLocalDevice().scheduleWithFixedDelay(() -> {
            final Boolean outOfService = get(PropertyIdentifier.outOfService);
            if (outOfService.booleanValue()) {
                // Do no track changes to the input when the value of out-of-service is true.
                return;
            }

            final ObjectPropertyReference ref = get(PropertyIdentifier.inputReference);

            final BACnetObject that = getLocalDevice().getObject(ref.getObjectIdentifier());
            String pollingError = null;
            if (that == null) {
                pollingError = "Unknown object " + ref.getObjectIdentifier();
            } else {
                try {
                    final Encodable value = that.readProperty(ref.getPropertyIdentifier(), ref.getPropertyArrayIndex());
                    long newValue = 0;
                    if (value == null) {
                        pollingError = "Unknown property " + ref;
                    } else if (value instanceof UnsignedInteger) {
                        newValue = ((UnsignedInteger) value).longValue();
                    } else if (value instanceof SignedInteger) {
                        newValue = ((SignedInteger) value).longValue();
                    } else {
                        pollingError = "Invalid input reference data type: " + value.getClass();
                    }

                    if (pollingError == null) {
                        if (newValue > lastPollingValue) {
                            // Only use the new value if it is an increase from the last value. Add the difference
                            // from the last value to the current count.
                            addPulses(newValue - lastPollingValue);
                        }
                        lastPollingValue = newValue;
                    }
                } catch (final BACnetServiceException e) {
                    pollingError = "Error reading property " + ref + ": " + e.getMessage();
                }
            }

            if (!Objects.equals(pollingError, lastPollingError)) {
                // Polling error changed.
                if (pollingError == null) {
                    // Poll succeeded. Reset the reliability.
                    LOG.info("Poll succeeded");
                    writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
                } else {
                    LOG.warn("Polling error: " + pollingError);
                    writePropertyInternal(PropertyIdentifier.reliability, Reliability.configurationError);
                }
                lastPollingError = pollingError;
            }
        }, pollingMillis, pollingMillis, TimeUnit.MILLISECONDS);

        return this;
    }

    public void pulse() {
        pulses(1);
    }

    public void pulses(final long pulsesToAdd) {
        if (get(PropertyIdentifier.inputReference) != null) {
            throw new IllegalStateException("Cannot set pulses directly while using an input reference");
        }
        addPulses(pulsesToAdd);
    }

    private void addPulses(final long pulsesToAdd) {
        synchronized (lock) {
            final UnsignedInteger count = get(PropertyIdentifier.count);
            writePropertyInternal(PropertyIdentifier.count, new UnsignedInteger(count.longValue() + pulsesToAdd));
        }
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (value.getPropertyIdentifier().equals(PropertyIdentifier.inputReference)) {
            // The object needs to support input references.
            if (inputMonitoringFuture == null) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (pid.isOneOf(PropertyIdentifier.count, PropertyIdentifier.scaleFactor)) {
            synchronized (lock) {
                if (pid.equals(PropertyIdentifier.count)) {
                    writePropertyInternal(PropertyIdentifier.updateTime, new DateTime(getLocalDevice()));
                }
                final UnsignedInteger count = get(PropertyIdentifier.count);
                final Real scaleFactor = get(PropertyIdentifier.scaleFactor);
                writePropertyInternal(PropertyIdentifier.presentValue,
                        new Real(scaleFactor.floatValue() * count.longValue()));
            }
        } else if (pid.equals(PropertyIdentifier.adjustValue)) {
            synchronized (lock) {
                final Real adjustValue = (Real) newValue;
                final UnsignedInteger count = get(PropertyIdentifier.count);
                final Real scaleFactor = get(PropertyIdentifier.scaleFactor);

                writePropertyInternal(PropertyIdentifier.countBeforeChange, count);
                final long diff = (long) (adjustValue.floatValue() / scaleFactor.floatValue());
                writePropertyInternal(PropertyIdentifier.count, new UnsignedInteger(count.longValue() - diff));
                writePropertyInternal(PropertyIdentifier.countChangeTime, new DateTime(getLocalDevice()));
            }

        }
    }

    @Override
    protected void terminateImpl() {
        if (inputMonitoringFuture != null)
            inputMonitoringFuture.cancel(false);
    }
}
