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

import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.commandTimeArray;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.currentCommandPriority;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.lastCommandTime;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.minimumOffTime;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.minimumOnTime;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.outOfService;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.priorityArray;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.relinquishDefault;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.valueSource;
import static com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier.valueSourceArray;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectReference;
import com.serotonin.bacnet4j.type.constructed.OptionalUnsigned;
import com.serotonin.bacnet4j.type.constructed.PriorityArray;
import com.serotonin.bacnet4j.type.constructed.PriorityValue;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class CommandableMixin extends AbstractMixin {
    private static final int MIN_OFF_ON_PRIORITY = 6;
    static final Logger LOG = LoggerFactory.getLogger(CommandableMixin.class);

    private final PropertyIdentifier pvProperty;

    private boolean overridden;

    /**
     * Determines if the pv property is writable when the object is not commandable, and it is in service. If this
     * is true, writes will be allowed. If false, writes will fail with property/write-access-denied.
     */
    private boolean supportsWritable;

    private boolean supportsCommandable;
    private boolean supportsValueSource;

    // Runtime
    private ScheduledFuture<?> minOnOffTimerTask;

    public CommandableMixin(final BACnetObject bo, final PropertyIdentifier pvProperty) {
        super(bo);
        this.pvProperty = pvProperty;
    }

    public boolean isOverridden() {
        return overridden;
    }

    public void setOverridden(final boolean overridden) {
        this.overridden = overridden;
    }

    public void supportWritable() {
        supportsWritable = true;
    }

    public boolean supportsWritable() {
        return supportsWritable;
    }

    public void supportCommandable(final Encodable relqDefault) {
        supportsCommandable = true;

        // If the object does not have a priority array and relinquish default, add them
        addProperty(priorityArray, new PriorityArray(), false);
        addProperty(relinquishDefault, relqDefault, false);
        addProperty(currentCommandPriority, new OptionalUnsigned(), false);

        if (supportsValueSource) {
            addProperty(valueSourceArray, new BACnetArray<>(16, new ValueSource()), false);
            addProperty(lastCommandTime, new TimeStamp(new DateTime(getLocalDevice())), false);
            addProperty(commandTimeArray, new BACnetArray<>(16, TimeStamp.UNSPECIFIED_TIME), false);
        }
    }

    public boolean supportsCommandable() {
        return supportsCommandable;
    }

    public void supportValueSource() {
        supportsValueSource = true;

        addProperty(valueSource, getLocalValueSource(), false);
        if (supportsCommandable) {
            addProperty(valueSourceArray, new BACnetArray<>(16, new ValueSource()), false);
            addProperty(lastCommandTime, new TimeStamp(new DateTime(getLocalDevice())), false);
            addProperty(commandTimeArray, new BACnetArray<>(16, TimeStamp.UNSPECIFIED_TIME), false);
        }
    }

    public boolean supportsValueSource() {
        return supportsValueSource;
    }

    @Override
    synchronized protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (pvProperty.equals(value.getPropertyIdentifier())) {
            if (overridden)
                return false;

            final Boolean oos = get(outOfService);
            if (oos.booleanValue())
                return false;

            if (supportsCommandable && value.getValue() instanceof Null)
                return true;
        } else if (value.getPropertyIdentifier().isOneOf(PropertyIdentifier.priorityArray,
                PropertyIdentifier.currentCommandPriority, PropertyIdentifier.valueSourceArray,
                PropertyIdentifier.lastCommandTime, PropertyIdentifier.commandTimeArray)) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        } else if (PropertyIdentifier.valueSource.equals(value.getPropertyIdentifier())) {
            if (!supportsValueSource)
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);

            final ValueSource vs = get(PropertyIdentifier.valueSource);
            if (vs == null || !vs.isAddress())
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            if (!vs.getAddress().equals(valueSource.getAddress()))
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);

            final ValueSource newVs = (ValueSource) value.getValue();
            if (!newVs.isObject())
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);

            writePropertyInternal(PropertyIdentifier.valueSource, newVs);

            // ?? Does the value source array need to be updated too?
        }

        return false;
    }

    @Override
    synchronized protected boolean writeProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (pvProperty.equals(value.getPropertyIdentifier())) {
            if (overridden) {
                // Never allow a write if the object is overridden.
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }

            final Boolean oos = get(outOfService);
            if (oos.booleanValue()) {
                // Writable while the object is out of service.
                directPVWrite(valueSource, value);
                return true;
            }

            if (supportsCommandable) {
                command(valueSource, value.getValue(), value.getPriority());
                return true;
            }

            // Not commandable. Return an error if a priority was specified.
            if (value.getPriority() != null) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }

            // If the object does not support writable, return an error.
            if (!supportsWritable) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }

            // Allow the direct write to the pv property.
            directPVWrite(valueSource, value);
            return true;
        }

        return false;
    }

    private void directPVWrite(final ValueSource valueSource, final PropertyValue value) {
        writePropertyInternal(pvProperty, value.getValue());
        if (supportsValueSource)
            writePropertyInternal(PropertyIdentifier.valueSource, valueSource);
    }

    @Override
    synchronized protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (relinquishDefault.equals(pid)) {
            // The relinquish default was changed. Ensure that the present value gets updated if necessary.
            updatePresentValue(null, new TimeStamp(new DateTime(getLocalDevice())));
        } else if (minimumOffTime.equals(pid) || minimumOnTime.equals(pid)) {
            if (supportsValueSource)
                updateValueSourceArray(MIN_OFF_ON_PRIORITY, getLocalValueSource());
        }
    }

    private void command(final ValueSource valueSource, final Encodable value, final UnsignedInteger priority)
            throws BACnetServiceException {
        int pri = 16;
        if (priority != null)
            pri = priority.intValue();

        if (pri < 1 || pri > 16)
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidArrayIndex);

        // Cannot set to priority level 6 - reserved for minimum_on_time and minimum_off_time functioning.
        if (pri == MIN_OFF_ON_PRIORITY)
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);

        // Set the value in the priority array.
        final PriorityArray priArr = get(priorityArray);
        priArr.setBase1(pri, new PriorityValue(value));

        final TimeStamp now = new TimeStamp(new DateTime(getLocalDevice()));
        if (supportsValueSource) {
            updateValueSourceArray(pri, valueSource);
            updateCommandTimeArray(pri, now);
        }

        updatePresentValue(priArr, now);
    }

    synchronized void minOnOffCompleted() {
        minOnOffTimerTask = null;
        final PriorityArray priArr = get(priorityArray);
        priArr.setBase1(MIN_OFF_ON_PRIORITY, new PriorityValue(Null.instance));
        updatePresentValue(priArr, new TimeStamp(new DateTime(getLocalDevice())));
    }

    private void updatePresentValue(final PriorityArray priorityArray, final TimeStamp now) {
        final PriorityArray pa = priorityArray == null ? get(PropertyIdentifier.priorityArray) : priorityArray;

        // Calculate from the priority array what are the current value and index.
        PriorityValue topValue = null;
        int topIndex = 17;
        for (int i = 1; i <= pa.getCount(); i++) {
            final PriorityValue priv = pa.getBase1(i);
            if (!priv.isa(Null.class)) {
                topValue = priv;
                topIndex = i;
                break;
            }
        }

        Encodable newValue;
        if (topValue == null) {
            newValue = get(relinquishDefault);
        } else {
            newValue = topValue.getValue();
        }

        // Minimum on/off time. See 19.2.3.
        final Encodable oldValue = get(pvProperty);
        final UnsignedInteger minOff = get(minimumOffTime);
        final UnsignedInteger minOn = get(minimumOnTime);
        if (minOff != null && minOn != null) {
            if (!newValue.equals(oldValue)) {
                // The value has changed. Check for updates to the timer.

                // If the write was to a priority higher than 6, cancel any existing timer.
                if (topIndex < MIN_OFF_ON_PRIORITY && minOnOffTimerTask != null) {
                    minOnOffTimerTask.cancel(false);
                    minOnOffTimerTask = null;
                }

                // If a timer task already exists, there is no action to take.
                if (minOnOffTimerTask == null) {
                    // Initialize the timer.
                    pa.setBase1(MIN_OFF_ON_PRIORITY, new PriorityValue(newValue));
                    updateCommandTimeArray(MIN_OFF_ON_PRIORITY, now);

                    int time;
                    if (BinaryPV.inactive.equals(newValue)) {
                        time = minOff.intValue();
                        LOG.debug("Starting min off timer: {}s", time);
                    } else {
                        time = minOn.intValue();
                        LOG.debug("Starting min on timer: {}s", time);
                    }

                    minOnOffTimerTask = getLocalDevice().schedule(() -> {
                        // Min time has elapsed.
                        LOG.debug("Min off/on timer has expired");
                        minOnOffCompleted();
                    }, time, TimeUnit.SECONDS);

                    // Update the new index if it is less than 6.
                    if (topIndex > MIN_OFF_ON_PRIORITY)
                        topIndex = MIN_OFF_ON_PRIORITY;
                }
            }
        }

        final OptionalUnsigned newIndex;
        if (topValue == null) {
            newIndex = new OptionalUnsigned();
        } else {
            newIndex = new OptionalUnsigned(topIndex);
        }

        final OptionalUnsigned oldIndex = get(currentCommandPriority);
        writePropertyInternal(currentCommandPriority, newIndex);
        if (supportsValueSource) {
            ValueSource vs;
            if (topValue == null) {
                vs = getLocalValueSource();
            } else {
                final BACnetArray<ValueSource> vsa = get(valueSourceArray);
                vs = vsa.getBase1(topIndex);
            }
            writePropertyInternal(PropertyIdentifier.valueSource, vs);

            if (!newIndex.equals(oldIndex) || !newValue.equals(oldValue)) {
                writePropertyInternal(PropertyIdentifier.lastCommandTime, now);
            }
        }

        writePropertyInternal(pvProperty, newValue);
    }

    private ValueSource getLocalValueSource() {
        return new ValueSource(new DeviceObjectReference(getLocalDevice().getId(), getId()));
    }

    private void updateValueSourceArray(final int indexBase1, final ValueSource va) {
        final BACnetArray<ValueSource> vsa = get(valueSourceArray);
        vsa.setBase1(indexBase1, va);
    }

    private void updateCommandTimeArray(final int indexBase1, final TimeStamp ts) {
        final BACnetArray<TimeStamp> cta = get(commandTimeArray);
        cta.setBase1(indexBase1, ts);
    }

    @Override
    protected void terminate() {
        if (minOnOffTimerTask != null)
            minOnOffTimerTask.cancel(false);
    }
}
