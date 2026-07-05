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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.logBuffer.ILogRecord;
import com.serotonin.bacnet4j.obj.logBuffer.LogBuffer;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
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
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Common base for log objects (Trend Log, Trend Log Multiple, Event Log). Handles the properties
 * and behaviors shared by all buffered log types: Enable / Start_Time / Stop_Time scheduling,
 * Buffer_Size / Record_Count validation, the mixin setup, and the intrinsic-reporting scaffolding.
 */
public abstract class LogBase extends BACnetObject {
    /**
     * Reserved Buffer_Size value indicating that the buffer size is unknown and constrained solely by currently
     * available resources. See addendum 135-2016bi-3.
     */
    public static final long BUFFER_SIZE_UNKNOWN = 0xFFFFFFFFL;

    protected boolean logDisabled;
    protected ScheduledFuture<?> startTimeFuture;
    protected ScheduledFuture<?> stopTimeFuture;

    protected LogBase(LocalDevice localDevice, ObjectType type, int instanceNumber, String name, boolean enable,
            DateTime startTime, DateTime stopTime, boolean stopWhenFull, int bufferSize) {
        super(localDevice, type, instanceNumber, name);

        Objects.requireNonNull(localDevice);
        Objects.requireNonNull(name);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(stopTime);

        set(PropertyIdentifier.enable, Boolean.valueOf(enable));
        set(PropertyIdentifier.startTime, startTime);
        set(PropertyIdentifier.stopTime, stopTime);
        set(PropertyIdentifier.stopWhenFull, Boolean.valueOf(stopWhenFull));
        set(PropertyIdentifier.bufferSize, new Unsigned32(bufferSize));
        set(PropertyIdentifier.recordCount, Unsigned32.ZERO);
        set(PropertyIdentifier.totalRecordCount, Unsigned32.ZERO);
        set(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        set(PropertyIdentifier.reliability, Reliability.noFaultDetected);
    }

    /**
     * Reports whether the underlying log buffer has room for another record. Concrete subclasses delegate to their
     * LogBuffer. Used when Buffer_Size holds the reserved sentinel {@link #BUFFER_SIZE_UNKNOWN} — see
     * addendum 135-2016bi-3.
     */
    protected boolean hasSpaceForAnotherRecord() {
        AtomicBoolean hasSpaceForAnotherRecord = new AtomicBoolean(false);
        doWithBuffer(buffer -> {
            hasSpaceForAnotherRecord.set(buffer.hasSpaceForAnotherRecord());
        });
        return hasSpaceForAnotherRecord.get();
    }

    /**
     * Adds the {@link HasStatusFlagsMixin} and a {@link ReadOnlyPropertyMixin} for
     * {@code Log_Buffer}, {@code Reliability}, and {@code Total_Record_Count}. Subclasses call
     * this from their own construction sequence.
     */
    protected void addLogMixins() {
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.logBuffer, PropertyIdentifier.reliability,
                PropertyIdentifier.totalRecordCount));
    }

    protected void baseSupportIntrinsicReporting(int notificationThreshold, int notificationClass,
            EventTransitionBits eventEnable, NotifyType notifyType) {
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        writePropertyInternal(PropertyIdentifier.notificationThreshold, new Unsigned32(notificationThreshold));
        writePropertyInternal(PropertyIdentifier.recordsSinceNotification, Unsigned32.ZERO);
        writePropertyInternal(PropertyIdentifier.lastNotifyRecord, Unsigned32.ZERO);
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

        addMixin(new IntrinsicReportingMixin(this, algo, null, PropertyIdentifier.totalRecordCount, triggerProps)
                .withPostNotificationAction(notifParams -> {
                    if (notifParams.getParameter() instanceof BufferReadyNotif brn) {
                        writePropertyInternal(PropertyIdentifier.lastNotifyRecord,
                                new Unsigned32(brn.getCurrentNotification()));
                        writePropertyInternal(PropertyIdentifier.recordsSinceNotification, Unsigned32.ZERO);
                    }
                }));
    }

    public boolean isLogDisabled() {
        return logDisabled;
    }

    public void setEnabled(boolean enabled) {
        writePropertyInternal(PropertyIdentifier.enable, Boolean.valueOf(enabled));
    }

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

    protected static void cancelFuture(ScheduledFuture<?> future) {
        if (future != null)
            future.cancel(false);
    }

    protected DateTime getNow() {
        return new DateTime(getLocalDevice().getClock().millis());
    }

    /**
     * Returns the current number of records in the buffer. Used by common validation and by
     * {@link #fullCheck()} / {@link #updateRecordCount()}.
     */
    protected abstract int bufferSize();

    /**
     * Reconsiders the {@code logDisabled} flag based on Enable, Start_Time, and Stop_Time, and
     * flushes any subclass-specific log-status record if the flag transitions to true.
     */
    protected abstract void evaluateLogDisabled();

    protected abstract void purge();

    @Override
    protected void beforeReadProperty(PropertyIdentifier pid) throws BACnetServiceException {
        if (PropertyIdentifier.logBuffer.equals(pid)) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.readAccessDenied);
        }
    }

    @Override
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
        if (PropertyIdentifier.enable.equals(value.getPropertyIdentifier())) {
            Boolean enable = value.getValue();
            Boolean stopWhenFull = get(PropertyIdentifier.stopWhenFull);
            Unsigned32 bufferSize = get(PropertyIdentifier.bufferSize);

            if (enable.booleanValue() && stopWhenFull.booleanValue() && isBufferFull(bufferSize)) {
                throw new BACnetServiceException(ErrorClass.object, ErrorCode.logBufferFull);
            }
        } else if (PropertyIdentifier.startTime.equals(value.getPropertyIdentifier()) //
                || PropertyIdentifier.stopTime.equals(value.getPropertyIdentifier())) {
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
            Unsigned32 recordCount = value.getValue();
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
        } else if (PropertyIdentifier.recordCount.equals(pid)) {
            Unsigned32 recordCount = (Unsigned32) newValue;
            if (recordCount.intValue() == 0)
                purge();
        }
    }

    @Override
    protected void terminateImpl() {
        cancelFuture(startTimeFuture);
        cancelFuture(stopTimeFuture);
    }

    protected void fullCheck() {
        Boolean stopWhenFull = get(PropertyIdentifier.stopWhenFull);
        Unsigned32 bufferSize = get(PropertyIdentifier.bufferSize);
        if (stopWhenFull.booleanValue() && isBufferFullAfterNext(bufferSize)) {
            writePropertyInternal(PropertyIdentifier.enable, Boolean.FALSE);
        }
    }

    /**
     * True if Record_Count equals Buffer_Size, or Buffer_Size is the reserved sentinel value and the buffer cannot
     * accept another record. Per addendum 135-2016bi-3.
     */
    protected boolean isBufferFull(Unsigned32 bufferSize) {
        if (bufferSize.longValue() == BUFFER_SIZE_UNKNOWN) {
            return !hasSpaceForAnotherRecord();
        }
        return bufferSize() >= bufferSize.longValue();
    }

    /**
     * True if adding one more record would fill the buffer — either the current fill is one below Buffer_Size, or
     * Buffer_Size is the reserved sentinel value and the buffer cannot accept another record. Per bi-3.
     */
    protected boolean isBufferFullAfterNext(Unsigned32 bufferSize) {
        if (bufferSize.longValue() == BUFFER_SIZE_UNKNOWN) {
            return !hasSpaceForAnotherRecord();
        }
        return bufferSize() >= bufferSize.longValue() - 1;
    }

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
            return now.compareTo(stop) < 0;
        }

        return true;
    }

    protected void updateRecordCount() {
        writePropertyInternal(PropertyIdentifier.recordCount, new Unsigned32(bufferSize()));
    }

    public abstract <E extends ILogRecord> void doWithBuffer(Consumer<LogBuffer<E>> consumer);

    protected <E extends ILogRecord> void addLogRecordImpl(E rec) {
        Unsigned32 bufferSize = get(PropertyIdentifier.bufferSize);

        doWithBuffer(buf -> {
            // Don't add more to the buffer than capacity. Also covers the bi-3 unknown-size case where the buffer
            // itself signals exhaustion.
            if (isBufferFull(bufferSize)) {
                // Buffer is already full. Drop the oldest record.
                buf.remove();
            }

            buf.add(rec);
        });

        updateRecordCount();

        Unsigned32 recordsSinceNotification = get(PropertyIdentifier.recordsSinceNotification);
        if (recordsSinceNotification != null) {
            writePropertyInternal(PropertyIdentifier.recordsSinceNotification, recordsSinceNotification.increment());
        }

        // The total record count must be written last because it is the monitored property for intrinsic reporting.
        Unsigned32 totalRecordCount = get(PropertyIdentifier.totalRecordCount);
        totalRecordCount = totalRecordCount.increment();
        if (totalRecordCount.longValue() == 0)
            // Value overflowed. As per 12.27.15, 12.30.21, 12.25.16 set to 1.
            totalRecordCount = new Unsigned32(1);
        rec.setSequenceNumber(totalRecordCount.longValue());
        writePropertyInternal(PropertyIdentifier.totalRecordCount, totalRecordCount);
    }
}
