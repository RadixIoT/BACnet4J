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

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.obj.logBuffer.LogBuffer;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedEventNotificationRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.EventLogRecord;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LogStatus;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class EventLogObject extends LogBase {

    // CreateObject constructor
    public static EventLogObject create(LocalDevice localDevice, int instanceNumber) {
        return new EventLogObject(localDevice, instanceNumber, ObjectType.eventLog.toString() + " " + instanceNumber,
                new LinkedListLogBuffer<>(), false, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED, false, 100) //
                .supportIntrinsicReporting(20, 0, new EventTransitionBits(false, false, false),
                        NotifyType.event);
    }

    private final LogBuffer<EventLogRecord> buffer;
    private final DeviceEventAdapter eventListener;

    /**
     * Log buffers are expected to have been initialized to their buffer size.
     */
    public EventLogObject(LocalDevice localDevice, int instanceNumber, String name,
            LogBuffer<EventLogRecord> buffer, boolean enable, DateTime startTime,
            DateTime stopTime, boolean stopWhenFull, int bufferSize) {
        super(localDevice, ObjectType.eventLog, instanceNumber, name, enable, startTime, stopTime, stopWhenFull,
                bufferSize);

        set(PropertyIdentifier.logBuffer, buffer);

        updateStartTime(startTime);
        updateStopTime(stopTime);

        addLogMixins();

        this.buffer = buffer;
        logDisabled = !allowLogging(getNow());

        eventListener = new DeviceEventAdapter() {
            @Override
            public void eventNotificationReceived(UnsignedInteger processIdentifier,
                    ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier eventObjectIdentifier,
                    TimeStamp timeStamp, UnsignedInteger notificationClass, UnsignedInteger priority,
                    EventType eventType, CharacterString messageText, NotifyType notifyType,
                    Boolean ackRequired, EventState fromState, EventState toState,
                    NotificationParameters eventValues) {
                addLogRecord(new EventLogRecord(getNow(),
                        new ConfirmedEventNotificationRequest(processIdentifier, initiatingDeviceIdentifier,
                                eventObjectIdentifier, timeStamp, notificationClass, priority, eventType, messageText,
                                notifyType, ackRequired, fromState, toState, eventValues)));
            }
        };
        localDevice.getEventHandler().addListener(eventListener);
    }

    public EventLogObject supportIntrinsicReporting(int notificationThreshold, int notificationClass,
            EventTransitionBits eventEnable, NotifyType notifyType) {
        baseSupportIntrinsicReporting(notificationThreshold, notificationClass, eventEnable, notifyType);
        return this;
    }

    public LogBuffer<EventLogRecord> getBuffer() {
        return buffer;
    }

    @Override
    protected int bufferSize() {
        return buffer.size();
    }

    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);
        if (PropertyIdentifier.stopWhenFull.equals(pid)) {
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
        addLogRecordImpl(new EventLogRecord(getNow(), new LogStatus(logDisabled, true, false)));
    }

    @Override
    protected void terminateImpl() {
        super.terminateImpl();
        getLocalDevice().getEventHandler().removeListener(eventListener);
    }

    private synchronized void addLogRecord(EventLogRecord rec) {
        // Check if logging is allowed.
        if (logDisabled)
            return;

        // Add the new record.
        addLogRecordImpl(rec);

        fullCheck();
    }

    private void addLogRecordImpl(EventLogRecord rec) {
        Unsigned32 bufferSize = get(PropertyIdentifier.bufferSize);

        synchronized (buffer) {
            // Don't add more to the buffer than capacity.
            if (buffer.size() == bufferSize.intValue()) {
                // Buffer is already full. Drop the oldest record.
                buffer.remove();
            }

            buffer.add(rec);
        }

        updateRecordCount();

        Unsigned32 recordsSinceNotification = get(PropertyIdentifier.recordsSinceNotification);
        if (recordsSinceNotification != null) {
            writePropertyInternal(PropertyIdentifier.recordsSinceNotification, recordsSinceNotification.increment());
        }

        // The total record count must be written last because it is the monitored property for intrinsic reporting.
        Unsigned32 totalRecordCount = get(PropertyIdentifier.totalRecordCount);
        totalRecordCount = totalRecordCount.increment();
        if (totalRecordCount.longValue() == 0)
            // Value overflowed. As per 12.27.15 set to 1.
            totalRecordCount = new Unsigned32(1);
        rec.setSequenceNumber(totalRecordCount.longValue());
        writePropertyInternal(PropertyIdentifier.totalRecordCount, totalRecordCount);
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
                    addLogRecordImpl(new EventLogRecord(now, new LogStatus(logDisabled, false, false)));
            }
        }
    }
}
