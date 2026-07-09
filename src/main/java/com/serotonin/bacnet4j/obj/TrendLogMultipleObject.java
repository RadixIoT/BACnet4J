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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.logBuffer.ILogRecord;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.obj.logBuffer.LogBuffer;
import com.serotonin.bacnet4j.obj.mixin.PollingDelegate;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LogData;
import com.serotonin.bacnet4j.type.constructed.LogData.LogDataElement;
import com.serotonin.bacnet4j.type.constructed.LogMultipleRecord;
import com.serotonin.bacnet4j.type.constructed.LogStatus;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.LoggingType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyReferences;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyValues;
import com.serotonin.bacnet4j.util.PropertyValues;

public class TrendLogMultipleObject extends TrendLogBase {
    // CreateObject constructor
    public static TrendLogMultipleObject create(LocalDevice localDevice, int instanceNumber) {
        return new TrendLogMultipleObject(localDevice, instanceNumber,
                ObjectType.trendLogMultiple + " " + instanceNumber, new LinkedListLogBuffer<>(), false,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED, new BACnetArray<>(), 60, false, 100) //
                .supportIntrinsicReporting(20, 0, new EventTransitionBits(false, false, false),
                        NotifyType.event);
    }

    private final LogBuffer<LogMultipleRecord> buffer;

    public TrendLogMultipleObject(LocalDevice localDevice, int instanceNumber, String name,
            LogBuffer<LogMultipleRecord> buffer, boolean enable, DateTime startTime,
            DateTime stopTime, BACnetArray<DeviceObjectPropertyReference> logDeviceObjectProperty,
            int logInterval, boolean stopWhenFull, int bufferSize) {
        super(localDevice, ObjectType.trendLogMultiple, instanceNumber, name, enable, startTime, stopTime, logInterval,
                stopWhenFull, bufferSize);

        Objects.requireNonNull(logDeviceObjectProperty);

        set(PropertyIdentifier.logDeviceObjectProperty, logDeviceObjectProperty);
        set(PropertyIdentifier.logBuffer, buffer);

        postInitialize();

        this.buffer = buffer;
        logDisabled = !allowLogging(getNow());
    }

    public TrendLogMultipleObject withPolled(int logInterval, TimeUnit logIntervalUnit, boolean alignIntervals,
            int intervalOffset, TimeUnit offsetUnit) {
        baseWithPolled(logInterval, logIntervalUnit, alignIntervals, intervalOffset, offsetUnit);
        return this;
    }

    public TrendLogMultipleObject supportIntrinsicReporting(int notificationThreshold, int notificationClass,
            EventTransitionBits eventEnable, NotifyType notifyType) {
        baseSupportIntrinsicReporting(notificationThreshold, notificationClass, eventEnable, notifyType);

        return this;
    }

    /**
     * Allows the consumer to work with the buffer in a thread-safe manner.
     *
     * @param consumer the work to do while synchronized.
     */
    @SuppressWarnings("unchecked")
    public <E extends ILogRecord> void doWithBuffer(Consumer<LogBuffer<E>> consumer) {
        synchronized (buffer) {
            consumer.accept((LogBuffer<E>) buffer);
        }
    }

    public int getRecordCount() {
        // Synchronize the buffer before requesting the size because we don't know the implementation of the buffer,
        // and whether the operation is atomic or not.
        synchronized (buffer) {
            return buffer.size();
        }
    }

    public LogMultipleRecord getRecord(int index) {
        synchronized (buffer) {
            return buffer.get(index);
        }
    }

    @Override
    protected int bufferSize() {
        synchronized (buffer) {
            return buffer.size();
        }
    }

    @Override
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value)
            throws BACnetServiceException {
        super.validateProperty(valueSource, value);
        if (PropertyIdentifier.loggingType.equals(value.getPropertyIdentifier())) {
            LoggingType loggingType = value.getValue();
            if (loggingType.equals(LoggingType.cov))
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        } else if (PropertyIdentifier.logDeviceObjectProperty.equals(value.getPropertyIdentifier())) {
            BACnetArray<DeviceObjectPropertyReference> refs = value.getValue();
            for (DeviceObjectPropertyReference ref : refs) {
                if (ref.getPropertyIdentifier().isOneOf(PropertyIdentifier.all, PropertyIdentifier.required,
                        PropertyIdentifier.optional)) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.parameterOutOfRange);
                }
            }
        } else if (PropertyIdentifier.logInterval.equals(value.getPropertyIdentifier())) {
            LoggingType loggingType = get(PropertyIdentifier.loggingType);
            if (!loggingType.equals(LoggingType.polled)) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);

        if (PropertyIdentifier.logInterval.equals(pid)) {
            updateLoggingType();
        } else if (PropertyIdentifier.stopWhenFull.equals(pid)) {
            Boolean oldStopWhenFull = (Boolean) oldValue;
            Boolean stopWhenFull = (Boolean) newValue;
            if (!oldStopWhenFull.booleanValue() && stopWhenFull.booleanValue()) {
                // Turning StopWhenFull on.
                Unsigned32 bufferSize = get(PropertyIdentifier.bufferSize);
                if (buffer.size() >= bufferSize.intValue()) {
                    synchronized (buffer) {
                        while (buffer.size() >= bufferSize.intValue())
                            buffer.remove();
                    }
                    updateRecordCount();
                    writePropertyInternal(PropertyIdentifier.enable, Boolean.FALSE);
                }
            }
        } else if (PropertyIdentifier.bufferSize.equals(pid)) {
            Unsigned32 bufferSize = (Unsigned32) newValue;
            // In case the buffer size was reduced, remove extra entries in the buffer.
            synchronized (buffer) {
                while (buffer.size() >= bufferSize.intValue())
                    buffer.remove();
            }
            updateRecordCount();
        }
    }

    @Override
    protected void purge() {
        synchronized (buffer) {
            buffer.clear();
        }
        writePropertyInternal(PropertyIdentifier.recordsSinceNotification, Unsigned32.ZERO);
        addLogRecordImpl(new LogMultipleRecord(getNow(), new LogData(new LogStatus(logDisabled, true, false))));
    }

    @Override
    protected void updateMonitoredProperty() {
        BACnetArray<DeviceObjectPropertyReference> props = get(PropertyIdentifier.logDeviceObjectProperty);

        // Add the monitored property.
        DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences();
        for (DeviceObjectPropertyReference prop : props) {
            if (prop.getDeviceIdentifier().isUninitialized() || prop.getObjectIdentifier().isUninitialized())
                // Don't ask for properties that are not initialized.
                continue;

            refs.addIndex(prop.getDeviceIdentifier().getInstanceNumber(), prop.getObjectIdentifier(),
                    prop.getPropertyIdentifier(), prop.getPropertyArrayIndex());
        }

        pollingDelegate = new PollingDelegate(getLocalDevice(), refs);
    }

    /**
     * This method reinitializes all data retrieval.
     */
    @Override
    protected void updateLoggingType() {
        LoggingType loggingType = get(PropertyIdentifier.loggingType);

        cancelFuture(pollingFuture);

        if (loggingType.equals(LoggingType.polled)) {
            updatePolledLoggingType();
        } else if (loggingType.equals(LoggingType.triggered)) {
            set(PropertyIdentifier.logInterval, UnsignedInteger.ZERO);
        }
    }

    @Override
    protected synchronized void doPoll() {
        // The spec says that no *logging* should occur if the log is disabled, but there doesn't seem to be much
        // point in polling at all if this is the case, so we check here and abort accordingly.
        if (logDisabled)
            return;

        // Get the time before the poll, so that alignment looks right.
        DateTime now = getNow();

        // Call the delegate to perform the poll.
        DeviceObjectPropertyValues result = pollingDelegate.doPoll();

        // Process the results.
        List<LogDataElement> elements = new ArrayList<>();
        BACnetArray<DeviceObjectPropertyReference> props = get(PropertyIdentifier.logDeviceObjectProperty);
        for (DeviceObjectPropertyReference prop : props) {
            LogDataElement element;
            if (prop.getDeviceIdentifier().isUninitialized() || prop.getObjectIdentifier().isUninitialized()) {
                element = new LogDataElement(new ErrorClassAndCode(ErrorClass.property, ErrorCode.noPropertySpecified));
            } else {
                PropertyValues values = result.getPropertyValues(prop.getDeviceIdentifier().getInstanceNumber());
                if (values == null) {
                    throw new NullPointerException("Didn't find device "
                            + prop.getDeviceIdentifier().getInstanceNumber() + " in results " + result
                            + ", polling delegate remote references: " + pollingDelegate.getRemoteReferences());
                }
                Encodable value = values.getNoErrorCheck(prop.getObjectIdentifier(),
                        new PropertyReference(prop.getPropertyIdentifier(), prop.getPropertyArrayIndex()));
                element = LogDataElement.createFromMonitoredValue(value);
            }
            elements.add(element);
        }

        LogMultipleRecord rec = new LogMultipleRecord(now, new LogData(new SequenceOf<>(elements)));
        addLogRecord(rec);
    }

    private synchronized void addLogRecord(LogMultipleRecord rec) {
        // Check if logging is allowed.
        if (logDisabled)
            return;

        // Add the new record.
        addLogRecordImpl(rec);

        fullCheck();
    }

    @Override
    protected void evaluateLogDisabled() {
        // Don't evaluate until instantiation is complete.
        if (buffer != null) {
            DateTime now = getNow();
            boolean newValue = !allowLogging(now);
            if (logDisabled != newValue) {
                logDisabled = newValue;
                if (logDisabled)
                    // Only write a log status if the log is disabled.
                    addLogRecordImpl(new LogMultipleRecord(now, new LogData(new LogStatus(logDisabled, false, false))));
            }
        }
    }
}
