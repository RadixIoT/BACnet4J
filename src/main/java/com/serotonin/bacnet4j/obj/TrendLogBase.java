/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.PollingDelegate;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.BufferReadyAlgo;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.LoggingType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

abstract class TrendLogBase extends BACnetObject {
    protected boolean logDisabled;
    protected ScheduledFuture<?> startTimeFuture;
    protected ScheduledFuture<?> stopTimeFuture;

    protected PollingDelegate pollingDelegate;
    protected ScheduledFuture<?> pollingFuture;

    protected TrendLogBase(LocalDevice localDevice, ObjectType type, int instanceNumber, String name, boolean enable,
            DateTime startTime, DateTime stopTime, int logInterval, boolean stopWhenFull, int bufferSize) {
        super(localDevice, type, instanceNumber, name);

        Objects.requireNonNull(localDevice);
        Objects.requireNonNull(name);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(stopTime);

        set(PropertyIdentifier.enable, Boolean.valueOf(enable));
        set(PropertyIdentifier.startTime, startTime);
        set(PropertyIdentifier.stopTime, stopTime);
        set(PropertyIdentifier.logInterval, new UnsignedInteger(logInterval));
        set(PropertyIdentifier.stopWhenFull, Boolean.valueOf(stopWhenFull));
        set(PropertyIdentifier.bufferSize, new UnsignedInteger(bufferSize));
        set(PropertyIdentifier.recordCount, UnsignedInteger.ZERO);
        set(PropertyIdentifier.totalRecordCount, UnsignedInteger.ZERO);
        set(PropertyIdentifier.alignIntervals, Boolean.TRUE);
        set(PropertyIdentifier.intervalOffset, UnsignedInteger.ZERO);
        set(PropertyIdentifier.trigger, Boolean.FALSE);
        set(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        set(PropertyIdentifier.reliability, Reliability.noFaultDetected);
    }

    protected void postInitialize() {
        updateMonitoredProperty();
        updateStartTime(get(PropertyIdentifier.startTime));
        updateStopTime(get(PropertyIdentifier.stopTime));
        withTriggered();

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.logBuffer, PropertyIdentifier.reliability,
                PropertyIdentifier.totalRecordCount));
    }

    protected void baseWithPolled(int logInterval, TimeUnit logIntervalUnit, boolean alignIntervals,
            int intervalOffset, TimeUnit offsetUnit) {
        set(PropertyIdentifier.logInterval, new UnsignedInteger(logIntervalUnit.toMillis(logInterval) / 10));
        set(PropertyIdentifier.alignIntervals, Boolean.valueOf(alignIntervals));
        set(PropertyIdentifier.intervalOffset, new UnsignedInteger(offsetUnit.toMillis(intervalOffset) / 10));
        set(PropertyIdentifier.loggingType, LoggingType.polled);
        updateLoggingType();
    }

    protected void baseSupportIntrinsicReporting(int notificationThreshold, int notificationClass,
            EventTransitionBits eventEnable, NotifyType notifyType) {
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        // Prepare the object with all the properties that intrinsic reporting will need.
        // User-defined properties
        writePropertyInternal(PropertyIdentifier.notificationThreshold, new UnsignedInteger(notificationThreshold));
        writePropertyInternal(PropertyIdentifier.recordsSinceNotification, UnsignedInteger.ZERO);
        writePropertyInternal(PropertyIdentifier.lastNotifyRecord, UnsignedInteger.ZERO);
        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        BufferReadyAlgo algo = new BufferReadyAlgo(PropertyIdentifier.totalRecordCount,
                new DeviceObjectPropertyReference(getId(), PropertyIdentifier.logBuffer, null,
                        getLocalDevice().getId()),
                PropertyIdentifier.notificationThreshold, PropertyIdentifier.lastNotifyRecord);

        PropertyIdentifier[] triggerProps = new PropertyIdentifier[] { //
                PropertyIdentifier.totalRecordCount, //
                PropertyIdentifier.notificationThreshold};

        // Now add the mixin.
        addMixin(new IntrinsicReportingMixin(this, algo, null, PropertyIdentifier.totalRecordCount, triggerProps)
                .withPostNotificationAction(notifParams -> {
                    if (notifParams.getParameter() instanceof BufferReadyNotif brn) {
                        // After a notification has been sent, a couple values need to be updated.
                        writePropertyInternal(PropertyIdentifier.lastNotifyRecord, brn.getCurrentNotification());
                        writePropertyInternal(PropertyIdentifier.recordsSinceNotification, UnsignedInteger.ZERO);
                    }
                }));

        updateMonitoredProperty();
    }

    public boolean isLogDisabled() {
        return logDisabled;
    }

    public void setEnabled(boolean enabled) {
        writePropertyInternal(PropertyIdentifier.enable, Boolean.valueOf(enabled));
    }

    protected abstract void updateMonitoredProperty();

    protected void updateStartTime(DateTime startTime) {
        cancelFuture(startTimeFuture);
        if (!startTime.equals(DateTime.UNSPECIFIED)) {
            DateTime now = getNow();
            long diff = startTime.getGC().getTimeInMillis() - now.getGC().getTimeInMillis();
            if (diff > 0) {
                startTimeFuture = getLocalDevice().schedule(this::evaluateLogDisabled, diff, TimeUnit.MILLISECONDS);
            }
        }
        evaluateLogDisabled();
    }

    protected void updateStopTime(DateTime stopTime) {
        cancelFuture(stopTimeFuture);
        if (!stopTime.equals(DateTime.UNSPECIFIED)) {
            DateTime now = getNow();
            long diff = stopTime.getGC().getTimeInMillis() - now.getGC().getTimeInMillis();
            if (diff > 0) {
                stopTimeFuture = getLocalDevice().schedule(this::evaluateLogDisabled, diff, TimeUnit.MILLISECONDS);
            }
        }
        evaluateLogDisabled();
    }

    protected void withTriggered() {
        set(PropertyIdentifier.loggingType, LoggingType.triggered);
        updateLoggingType();
    }

    protected static void cancelFuture(ScheduledFuture<?> future) {
        if (future != null)
            future.cancel(false);
    }

    protected DateTime getNow() {
        return new DateTime(getLocalDevice().getClock().millis());
    }

    protected abstract void evaluateLogDisabled();

    protected abstract void updateLoggingType();

    protected abstract int bufferSize();


    /**
     * Locally trigger a poll.
     *
     * @return true if the trigger was done, false if the trigger value was already true, indicating that a trigger
     * was already in progress.
     */
    public synchronized boolean trigger() {
        Boolean trigger = get(PropertyIdentifier.trigger);
        if (trigger.booleanValue()) {
            return false;
        }
        set(PropertyIdentifier.trigger, Boolean.TRUE);
        doTrigger();
        return true;
    }

    @Override
    protected void beforeReadProperty(PropertyIdentifier pid) throws BACnetServiceException {
        if (PropertyIdentifier.logBuffer.equals(pid)) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.readAccessDenied);
        }
    }

    @Override
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.enable.equals(value.getPropertyIdentifier())) {
            Boolean enable = value.getValue();
            Boolean stopWhenFull = get(PropertyIdentifier.stopWhenFull);
            UnsignedInteger bufferSize = get(PropertyIdentifier.bufferSize);

            if (enable.booleanValue() && stopWhenFull.booleanValue() && bufferSize.intValue() == bufferSize()) {
                throw new BACnetServiceException(ErrorClass.object, ErrorCode.logBufferFull);
            }
        } else if (PropertyIdentifier.startTime.equals(value.getPropertyIdentifier()) //
                || PropertyIdentifier.stopTime.equals(value.getPropertyIdentifier())) {
            // Ensure that the date time is either entirely unspecified or entirely specified.
            DateTime dt = value.getValue();
            if (dt.equals(DateTime.UNSPECIFIED))
                return false;

            if (!dt.isFullySpecified())
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.parameterOutOfRange);
        } else if (PropertyIdentifier.bufferSize.equals(value.getPropertyIdentifier())) {
            Boolean enable = get(PropertyIdentifier.enable);
            if (enable.booleanValue()) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }
        } else if (PropertyIdentifier.recordCount.equals(value.getPropertyIdentifier())) {
            // Only allowed to write a zero to this record. What would any other value do?
            UnsignedInteger recordCount = value.getValue();
            if (recordCount.intValue() != 0)
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        if (PropertyIdentifier.enable.equals(pid)) {
            evaluateLogDisabled();
        } else if (PropertyIdentifier.startTime.equals(pid)) {
            updateStartTime((DateTime) newValue);
        } else if (PropertyIdentifier.stopTime.equals(pid)) {
            updateStopTime((DateTime) newValue);
        } else if (PropertyIdentifier.logDeviceObjectProperty.equals(pid)) {
            purge();
            updateMonitoredProperty();
        } else if (PropertyIdentifier.recordCount.equals(pid)) {
            UnsignedInteger recordCount = (UnsignedInteger) newValue;
            if (recordCount.intValue() == 0)
                purge();
        } else if (PropertyIdentifier.loggingType.equals(pid)) {
            updateLoggingType();
        } else if (pid.isOneOf(PropertyIdentifier.alignIntervals, PropertyIdentifier.intervalOffset)) {
            LoggingType loggingType = get(PropertyIdentifier.loggingType);
            if (loggingType.equals(LoggingType.polled)) {
                updateLoggingType();
            }
        } else if (PropertyIdentifier.trigger.equals(pid)) {
            // If the value has changed from false to true.
            if (((Boolean) newValue).booleanValue() && !((Boolean) oldValue).booleanValue()) {
                doTrigger();
            }
        }
    }

    protected void doTrigger() {
        // Perform the trigger asynchronously
        getLocalDevice().execute(() -> {
            try {
                // Do the poll.
                doPoll();
                LOG.debug("Trigger complete");
            } finally {
                // Set the trigger value back to false.
                writePropertyInternal(PropertyIdentifier.trigger, Boolean.FALSE);
            }
        });
    }

    protected abstract void purge();

    protected abstract void doPoll();

    @Override
    protected void terminateImpl() {
        super.terminate();
        cancelFuture(startTimeFuture);
        cancelFuture(stopTimeFuture);
        cancelFuture(pollingFuture);
    }

    protected void fullCheck() {
        Boolean stopWhenFull = get(PropertyIdentifier.stopWhenFull);
        UnsignedInteger bufferSize = get(PropertyIdentifier.bufferSize);
        if (stopWhenFull.booleanValue() && bufferSize() == bufferSize.intValue() - 1) {
            // There is only one spot left in the buffer, and StopWhenFull is true. Set Enable to false.
            writePropertyInternal(PropertyIdentifier.enable, Boolean.FALSE);
        }
    }


    /**
     * Determines whether logging should be performed based upon Enable, StartTime, and StopTime.
     */
    protected boolean allowLogging(DateTime now) {
        Boolean enabled = get(PropertyIdentifier.enable);
        if (!enabled.booleanValue())
            return false;

        DateTime start = get(PropertyIdentifier.startTime);
        DateTime stop = get(PropertyIdentifier.stopTime);

        if (!start.equals(DateTime.UNSPECIFIED)) {
            LOG.debug("Checking start time");
            if (now.compareTo(start) < 0)
                return false;
        }

        if (!stop.equals(DateTime.UNSPECIFIED)) {
            LOG.debug("Checking stop time, now={}, stop={}", now, stop);
            if (now.compareTo(stop) >= 0)
                return false;
        }

        return true;
    }

    protected void updateRecordCount() {
        writePropertyInternal(PropertyIdentifier.recordCount, new UnsignedInteger(bufferSize()));
    }
}
