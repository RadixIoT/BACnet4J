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
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.obj.logBuffer.LogBuffer;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.BufferReadyAlgo;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventLogRecord;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LogStatus;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * TODO
 * - Align intervals considering daylight savings time.
 */
public class EventLogObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(EventLogObject.class);

    // CreateObject constructor
    public static EventLogObject create(final LocalDevice localDevice, final int instanceNumber)
            throws BACnetServiceException {
        return new EventLogObject(localDevice, instanceNumber, ObjectType.eventLog.toString() + " " + instanceNumber,
                new LinkedListLogBuffer<>(), false, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED, false, 100) //
                .supportIntrinsicReporting(20, 0, new EventTransitionBits(false, false, false),
                        NotifyType.event);
    }

    private final LogBuffer<EventLogRecord> buffer;

    private boolean logDisabled;
    private ScheduledFuture<?> startTimeFuture;
    private ScheduledFuture<?> stopTimeFuture;
    private final DeviceEventAdapter eventListener;

    /**
     * Log buffers are expected to have been initialized to their buffer size.
     */
    public EventLogObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final LogBuffer<EventLogRecord> buffer, final boolean enable, final DateTime startTime,
            final DateTime stopTime, final boolean stopWhenFull, final int bufferSize) throws BACnetServiceException {
        super(localDevice, ObjectType.eventLog, instanceNumber, name);

        Objects.requireNonNull(localDevice);
        Objects.requireNonNull(name);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(stopTime);

        set(PropertyIdentifier.enable, Boolean.valueOf(enable));
        set(PropertyIdentifier.startTime, startTime);
        set(PropertyIdentifier.stopTime, stopTime);
        set(PropertyIdentifier.stopWhenFull, Boolean.valueOf(stopWhenFull));
        set(PropertyIdentifier.bufferSize, new UnsignedInteger(bufferSize));
        set(PropertyIdentifier.logBuffer, buffer);
        set(PropertyIdentifier.recordCount, UnsignedInteger.ZERO);
        set(PropertyIdentifier.totalRecordCount, UnsignedInteger.ZERO);
        set(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        set(PropertyIdentifier.reliability, Reliability.noFaultDetected);

        updateStartTime(startTime);
        updateStopTime(stopTime);

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.logBuffer, PropertyIdentifier.reliability,
                PropertyIdentifier.totalRecordCount));

        this.buffer = buffer;
        logDisabled = !allowLogging(getNow());

        eventListener = new DeviceEventAdapter() {
            @Override
            public void eventNotificationReceived(final UnsignedInteger processIdentifier,
                    final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier eventObjectIdentifier,
                    final TimeStamp timeStamp, final UnsignedInteger notificationClass, final UnsignedInteger priority,
                    final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
                    final Boolean ackRequired, final EventState fromState, final EventState toState,
                    final NotificationParameters eventValues) {
                addLogRecord(new EventLogRecord(getNow(),
                        new ConfirmedEventNotificationRequest(processIdentifier, initiatingDeviceIdentifier,
                                eventObjectIdentifier, timeStamp, notificationClass, priority, eventType, messageText,
                                notifyType, ackRequired, fromState, toState, eventValues)));
            }
        };
        localDevice.getEventHandler().addListener(eventListener);

        localDevice.addObject(this);
    }

    public EventLogObject supportIntrinsicReporting(final int notificationThreshold, final int notificationClass,
            final EventTransitionBits eventEnable, final NotifyType notifyType) {
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        // Prepare the object with all of the properties that intrinsic reporting will need.
        // User-defined properties
        writePropertyInternal(PropertyIdentifier.notificationThreshold, new UnsignedInteger(notificationThreshold));
        writePropertyInternal(PropertyIdentifier.recordsSinceNotification, UnsignedInteger.ZERO);
        writePropertyInternal(PropertyIdentifier.lastNotifyRecord, UnsignedInteger.ZERO);
        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        final BufferReadyAlgo algo = new BufferReadyAlgo(PropertyIdentifier.totalRecordCount,
                new DeviceObjectPropertyReference(getId(), PropertyIdentifier.logBuffer, null,
                        getLocalDevice().getId()),
                PropertyIdentifier.notificationThreshold, PropertyIdentifier.lastNotifyRecord);

        final PropertyIdentifier[] triggerProps = new PropertyIdentifier[] { //
                PropertyIdentifier.totalRecordCount, //
                PropertyIdentifier.notificationThreshold};

        // Now add the mixin.
        addMixin(new IntrinsicReportingMixin(this, algo, null, PropertyIdentifier.totalRecordCount, triggerProps)
                .withPostNotificationAction((notifParams) -> {
                    // After a notification has been sent, a couple values need to be updated.
                    final BufferReadyNotif brn = (BufferReadyNotif) notifParams.getParameter();
                    writePropertyInternal(PropertyIdentifier.lastNotifyRecord, brn.getCurrentNotification());
                    writePropertyInternal(PropertyIdentifier.recordsSinceNotification, UnsignedInteger.ZERO);
                }));

        return this;
    }

    public boolean isLogDisabled() {
        return logDisabled;
    }

    public LogBuffer<EventLogRecord> getBuffer() {
        return buffer;
    }

    public void setEnabled(final boolean enabled) {
        writePropertyInternal(PropertyIdentifier.enable, Boolean.valueOf(enabled));
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) throws BACnetServiceException {
        if (PropertyIdentifier.logBuffer.equals(pid)) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.readAccessDenied);
        }
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.enable.equals(value.getPropertyIdentifier())) {
            final Boolean enable = value.getValue();
            final Boolean stopWhenFull = get(PropertyIdentifier.stopWhenFull);
            final UnsignedInteger bufferSize = get(PropertyIdentifier.bufferSize);

            if (enable.booleanValue() && stopWhenFull.booleanValue() && bufferSize.intValue() == buffer.size()) {
                throw new BACnetServiceException(ErrorClass.object, ErrorCode.logBufferFull);
            }

        } else if (PropertyIdentifier.startTime.equals(value.getPropertyIdentifier()) //
                || PropertyIdentifier.stopTime.equals(value.getPropertyIdentifier())) {
            // Ensure that the date time is either entirely unspecified or entirely specified.
            final DateTime dt = value.getValue();
            if (dt.equals(DateTime.UNSPECIFIED))
                return false;

            if (!dt.isFullySpecified())
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.parameterOutOfRange);

        } else if (PropertyIdentifier.bufferSize.equals(value.getPropertyIdentifier())) {
            final Boolean enable = get(PropertyIdentifier.enable);
            if (enable.booleanValue()) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }
        } else if (PropertyIdentifier.recordCount.equals(value.getPropertyIdentifier())) {
            // Only allowed to write a zero to this record. What would any other value do?
            final UnsignedInteger recordCount = value.getValue();
            if (recordCount.intValue() != 0)
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (PropertyIdentifier.enable.equals(pid)) {
            evaluateLogDisabled();
        } else if (PropertyIdentifier.startTime.equals(pid)) {
            updateStartTime((DateTime) newValue);
        } else if (PropertyIdentifier.stopTime.equals(pid)) {
            updateStopTime((DateTime) newValue);

        } else if (PropertyIdentifier.stopWhenFull.equals(pid)) {
            final Boolean oldStopWhenFull = (Boolean) oldValue;
            final Boolean stopWhenFull = (Boolean) newValue;
            if (!oldStopWhenFull.booleanValue() && stopWhenFull.booleanValue()) {
                // Turning StopWhenFull on.
                final UnsignedInteger bufferSize = get(PropertyIdentifier.bufferSize);
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
            final UnsignedInteger bufferSize = (UnsignedInteger) newValue;
            // In case the buffer size was reduced, remove extra entries in the buffer.
            synchronized (buffer) {
                while (buffer.size() >= bufferSize.intValue())
                    buffer.remove();
            }
            updateRecordCount();

        } else if (PropertyIdentifier.recordCount.equals(pid)) {
            final UnsignedInteger recordCount = (UnsignedInteger) newValue;
            if (recordCount.intValue() == 0)
                purge();

        }
    }

    private void purge() {
        synchronized (buffer) {
            buffer.clear();
        }
        writePropertyInternal(PropertyIdentifier.recordsSinceNotification, UnsignedInteger.ZERO);
        addLogRecordImpl(new EventLogRecord(getNow(), new LogStatus(logDisabled, true, false)));
    }

    private void updateStartTime(final DateTime startTime) {
        cancelFuture(startTimeFuture);
        if (!startTime.equals(DateTime.UNSPECIFIED)) {
            final DateTime now = getNow();
            final long diff = startTime.getGC().getTimeInMillis() - now.getGC().getTimeInMillis();
            if (diff > 0) {
                startTimeFuture = getLocalDevice().schedule(() -> evaluateLogDisabled(), diff, TimeUnit.MILLISECONDS);
            }
        }
        evaluateLogDisabled();
    }

    private void updateStopTime(final DateTime stopTime) {
        cancelFuture(stopTimeFuture);
        if (!stopTime.equals(DateTime.UNSPECIFIED)) {
            final DateTime now = getNow();
            final long diff = stopTime.getGC().getTimeInMillis() - now.getGC().getTimeInMillis();
            if (diff > 0) {
                stopTimeFuture = getLocalDevice().schedule(() -> evaluateLogDisabled(), diff, TimeUnit.MILLISECONDS);
            }
        }
        evaluateLogDisabled();
    }

    @Override
    protected void terminateImpl() {
        cancelFuture(startTimeFuture);
        cancelFuture(stopTimeFuture);
        getLocalDevice().getEventHandler().removeListener(eventListener);
    }

    private static void cancelFuture(final ScheduledFuture<?> future) {
        if (future != null)
            future.cancel(false);
    }

    private synchronized void addLogRecord(final EventLogRecord record) {
        // Check if logging is allowed.
        if (logDisabled)
            return;

        // Add the new record.
        addLogRecordImpl(record);

        fullCheck();
    }

    private void fullCheck() {
        final Boolean stopWhenFull = get(PropertyIdentifier.stopWhenFull);
        final UnsignedInteger bufferSize = get(PropertyIdentifier.bufferSize);
        if (stopWhenFull.booleanValue() && buffer.size() == bufferSize.intValue() - 1) {
            // There is only one spot left in the buffer, and StopWhenFull is true. Set Enable to false.
            writePropertyInternal(PropertyIdentifier.enable, Boolean.FALSE);
        }
    }

    private void addLogRecordImpl(final EventLogRecord record) {
        final UnsignedInteger bufferSize = get(PropertyIdentifier.bufferSize);

        synchronized (buffer) {
            // Don't add more to the buffer than capacity.
            if (buffer.size() == bufferSize.intValue()) {
                // Buffer is already full. Drop the oldest record.
                buffer.remove();
            }

            buffer.add(record);
        }

        updateRecordCount();

        final UnsignedInteger recordsSinceNotification = get(PropertyIdentifier.recordsSinceNotification);
        if (recordsSinceNotification != null) {
            writePropertyInternal(PropertyIdentifier.recordsSinceNotification, recordsSinceNotification.increment32());
        }

        // The total record count must be written last because it is the monitored property for intrinsic reporting.
        UnsignedInteger totalRecordCount = get(PropertyIdentifier.totalRecordCount);
        totalRecordCount = totalRecordCount.increment32();
        if (totalRecordCount.longValue() == 0)
            // Value overflowed. As per 12.27.15 set to 1.
            totalRecordCount = new UnsignedInteger(1);
        record.setSequenceNumber(totalRecordCount.longValue());
        writePropertyInternal(PropertyIdentifier.totalRecordCount, totalRecordCount);
    }

    /**
     * Determines whether logging should be performed based upon Enable, StartTime, and StopTime.
     */
    private boolean allowLogging(final DateTime now) {
        final Boolean enabled = get(PropertyIdentifier.enable);
        if (!enabled.booleanValue())
            return false;

        final DateTime start = get(PropertyIdentifier.startTime);
        final DateTime stop = get(PropertyIdentifier.stopTime);

        if (!start.equals(DateTime.UNSPECIFIED)) {
            LOG.info("Checking start time");
            if (now.compareTo(start) < 0)
                return false;
        }

        if (!stop.equals(DateTime.UNSPECIFIED)) {
            LOG.info("Checking stop time");
            if (now.compareTo(stop) >= 0)
                return false;
        }

        return true;
    }

    private void updateRecordCount() {
        writePropertyInternal(PropertyIdentifier.recordCount, new UnsignedInteger(buffer.size()));
    }

    private void evaluateLogDisabled() {
        // Don't evaluate until instantiation is complete.
        if (buffer != null) {
            final DateTime now = getNow();
            final boolean newValue = !allowLogging(now);
            if (logDisabled != newValue) {
                logDisabled = newValue;
                if (logDisabled)
                    // Only write a log status if the log is disabled.
                    addLogRecordImpl(new EventLogRecord(now, new LogStatus(logDisabled, false, false)));
            }
        }
    }

    private DateTime getNow() {
        return new DateTime(getLocalDevice().getClock().millis());
    }
}
