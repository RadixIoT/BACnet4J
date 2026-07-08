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
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.obj.logBuffer.LogBuffer;
import com.serotonin.bacnet4j.obj.mixin.PollingDelegate;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ClientCov;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.LogRecord;
import com.serotonin.bacnet4j.type.constructed.LogStatus;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.LoggingType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyReferences;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyValues;
import com.serotonin.bacnet4j.util.PropertyValues;

/**
 * TODO
 * - Time change events. See "time-change" in 12.25.14.
 * - Log interrupted
 * - Align intervals considering daylight savings time.
 * - What if a device doesn't support SubscribeCOVPropertyRequest?
 */
public class TrendLogObject extends TrendLogBase {
    static final Logger LOG = LoggerFactory.getLogger(TrendLogObject.class);

    // CreateObject constructor
    public static TrendLogObject create(LocalDevice localDevice, int instanceNumber) {
        return new TrendLogObject(localDevice, instanceNumber, ObjectType.trendLog + " " + instanceNumber,
                new LinkedListLogBuffer<>(), false, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED,
                new DeviceObjectPropertyReference(localDevice.getInstanceNumber(), localDevice.getId(),
                        PropertyIdentifier.databaseRevision),
                60, false, 100) //
                .supportIntrinsicReporting(20, 0, new EventTransitionBits(false, false, false),
                        NotifyType.event);
    }

    private final LogBuffer<LogRecord> buffer;

    private SubscribeCOVPropertyRequest covSubscription;
    private DeviceEventAdapter covListener;
    private ScheduledFuture<?> resubscriptionFuture;

    private boolean configurationError;

    /**
     * Log buffers are expected to have been initialized to their buffer size.
     */
    public TrendLogObject(LocalDevice localDevice, int instanceNumber, String name, LogBuffer<LogRecord> buffer,
            boolean enable, DateTime startTime, DateTime stopTime,
            DeviceObjectPropertyReference logDeviceObjectProperty, int logInterval, boolean stopWhenFull,
            int bufferSize) {
        super(localDevice, ObjectType.trendLog, instanceNumber, name, enable, startTime, stopTime, logInterval,
                stopWhenFull, bufferSize);

        Objects.requireNonNull(logDeviceObjectProperty);

        set(PropertyIdentifier.logDeviceObjectProperty, logDeviceObjectProperty);
        set(PropertyIdentifier.logBuffer, buffer);

        postInitialize();

        this.buffer = buffer;
        logDisabled = !allowLogging(getNow());
    }

    public TrendLogObject withPolled(int logInterval, TimeUnit logIntervalUnit, boolean alignIntervals,
            int intervalOffset, TimeUnit offsetUnit) {
        baseWithPolled(logInterval, logIntervalUnit, alignIntervals, intervalOffset, offsetUnit);
        return this;
    }

    public TrendLogObject supportIntrinsicReporting(int notificationThreshold, int notificationClass,
            EventTransitionBits eventEnable, NotifyType notifyType) {
        baseSupportIntrinsicReporting(notificationThreshold, notificationClass, eventEnable, notifyType);
        return this;
    }

    public TrendLogObject withCov(int covResubscriptionIntervalSeconds, ClientCov clientCovIncrement) {
        Objects.requireNonNull(clientCovIncrement);

        set(PropertyIdentifier.covResubscriptionInterval, new UnsignedInteger(covResubscriptionIntervalSeconds));
        set(PropertyIdentifier.clientCovIncrement, clientCovIncrement);
        set(PropertyIdentifier.loggingType, LoggingType.cov);
        updateLoggingType();

        return this;
    }

    public void doWithBuffer(Consumer<LogBuffer<LogRecord>> consumer) {
        synchronized (buffer) {
            consumer.accept(buffer);
        }
    }

    public int getRecordCount() {
        // Synchronize the buffer before requesting the size because we don't know the implementation of the buffer,
        // and whether the operation is atomic or not.
        synchronized (buffer) {
            return buffer.size();
        }
    }

    public LogRecord getRecord(int index) {
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
        if (PropertyIdentifier.logDeviceObjectProperty.equals(value.getPropertyIdentifier())) {
            DeviceObjectPropertyReference logDeviceObjectProperty = value.getValue();
            if (logDeviceObjectProperty.getPropertyIdentifier().isOneOf(PropertyIdentifier.all,
                    PropertyIdentifier.required, PropertyIdentifier.optional)) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.parameterOutOfRange);
            }
        } else if (PropertyIdentifier.logInterval.equals(value.getPropertyIdentifier())) {
            LoggingType loggingType = get(PropertyIdentifier.loggingType);
            if (!loggingType.isOneOf(LoggingType.polled, LoggingType.cov)) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);
        if (PropertyIdentifier.logInterval.equals(pid)) {
            int oldLogInterval = ((UnsignedInteger) oldValue).intValue();
            int logInterval = ((UnsignedInteger) newValue).intValue();
            LoggingType loggingType = get(PropertyIdentifier.loggingType);

            if (loggingType.equals(LoggingType.polled) && oldLogInterval != 0 && logInterval == 0) {
                set(PropertyIdentifier.loggingType, LoggingType.cov);
            } else if (loggingType.equals(LoggingType.cov) && logInterval != 0) {
                set(PropertyIdentifier.loggingType, LoggingType.polled);
            }

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
        } else if (pid.isOneOf(PropertyIdentifier.covResubscriptionInterval, PropertyIdentifier.clientCovIncrement)) {
            LoggingType loggingType = get(PropertyIdentifier.loggingType);
            if (loggingType.equals(LoggingType.cov)) {
                updateLoggingType();
            }
        }
    }

    @Override
    protected void purge() {
        synchronized (buffer) {
            buffer.clear();
        }
        writePropertyInternal(PropertyIdentifier.recordsSinceNotification, Unsigned32.ZERO);
        addLogRecordImpl(new LogRecord(getNow(), new LogStatus(logDisabled, true, false), null));
    }

    @Override
    protected void terminateImpl() {
        super.terminate();
        cancelCov();
    }

    private void cancelCov() {
        if (covSubscription != null) {
            DeviceObjectPropertyReference monitored = get(PropertyIdentifier.logDeviceObjectProperty);

            // Cancel the subscription
            SubscribeCOVPropertyRequest cancellation = new SubscribeCOVPropertyRequest(covSubscription);
            if (monitored.getDeviceIdentifier().getInstanceNumber() == getLocalDevice().getInstanceNumber()) {
                try {
                    cancellation.handle(getLocalDevice(), getLocalDevice().getLoopbackAddress());
                } catch (BACnetException e) {
                    // Shouldn't really happen, but note it anyway.
                    LOG.error("Failed to unsubscribe locally", e);
                }
            } else {
                RemoteDevice rd;
                try {
                    rd = getLocalDevice().getRemoteDeviceBlocking(monitored.getDeviceIdentifier().getInstanceNumber());
                } catch (BACnetException e) {
                    LOG.warn("Failed to find remote device to which to send unsubscribe", e);
                    updateConfigurationError(true);
                    return;
                }
                getLocalDevice().send(rd, cancellation, null);
            }
            covSubscription = null;
        }

        if (covListener != null) {
            getLocalDevice().getEventHandler().removeListener(covListener);
            covListener = null;
        }

        cancelFuture(resubscriptionFuture);
    }

    @Override
    protected void updateMonitoredProperty() {
        DeviceObjectPropertyReference monitored = get(PropertyIdentifier.logDeviceObjectProperty);

        // Add the monitored property.
        DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences();
        refs.addIndex(monitored.getDeviceIdentifier().getInstanceNumber(), monitored.getObjectIdentifier(),
                monitored.getPropertyIdentifier(), monitored.getPropertyArrayIndex());

        // Check if status flags exist for the object.
        ObjectPropertyTypeDefinition def = ObjectProperties.getObjectPropertyTypeDefinition(
                monitored.getObjectIdentifier().getObjectType(), PropertyIdentifier.statusFlags);
        if (def != null) {
            refs.add(monitored.getDeviceIdentifier().getInstanceNumber(), monitored.getObjectIdentifier(),
                    PropertyIdentifier.statusFlags);
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
        cancelCov();

        if (loggingType.equals(LoggingType.polled)) {
            updatePolledLoggingType();
        } else if (loggingType.equals(LoggingType.cov)) {
            DeviceObjectPropertyReference monitored = get(PropertyIdentifier.logDeviceObjectProperty);
            set(PropertyIdentifier.logInterval, UnsignedInteger.ZERO);

            UnsignedInteger covResubscriptionInterval = get(PropertyIdentifier.covResubscriptionInterval);
            int resubscribeSeconds = covResubscriptionInterval.intValue();
            ClientCov clientCovIncrement = get(PropertyIdentifier.clientCovIncrement);

            // Create the subscription
            ObjectIdentifier deviceIdentifier = monitored.getDeviceIdentifier();
            SubscribeCOVPropertyRequest localCovSubscription = new SubscribeCOVPropertyRequest(
                    new UnsignedInteger(getLocalDevice().getNextProcessId()), monitored.getObjectIdentifier(),
                    Boolean.TRUE, new UnsignedInteger(resubscribeSeconds * 2),
                    new PropertyReference(monitored.getPropertyIdentifier(), monitored.getPropertyArrayIndex()),
                    clientCovIncrement.isRealIncrement() ? clientCovIncrement.getRealIncrement() : null);
            covSubscription = localCovSubscription;

            // Create the listener that will catch the COV notifications.
            covListener = new DeviceEventAdapter() {
                @Override
                public void covNotificationReceived(UnsignedInteger subscriberProcessIdentifier,
                        ObjectIdentifier initiatingDeviceIdentifier,
                        ObjectIdentifier monitoredObjectIdentifier, UnsignedInteger timeRemaining,
                        SequenceOf<PropertyValue> listOfValues) {
                    LOG.debug("Received COV notification");

                    // Handle the COV subscription. Check if it matches the subscription.
                    if (localCovSubscription.getSubscriberProcessIdentifier().equals(subscriberProcessIdentifier) //
                            && deviceIdentifier.equals(initiatingDeviceIdentifier) //
                            && localCovSubscription.getMonitoredObjectIdentifier().equals(monitoredObjectIdentifier)) {
                        // Looks like this is for us.
                        Encodable value = null;
                        StatusFlags statusFlags = null;
                        for (PropertyValue pv : listOfValues) {
                            if (pv.getPropertyIdentifier().equals(monitored.getPropertyIdentifier())) {
                                value = pv.getValue();
                            } else if (pv.getPropertyIdentifier().equals(PropertyIdentifier.statusFlags)) {
                                statusFlags = pv.getValue();
                            }
                        }

                        if (value == null) {
                            LOG.warn("Requested property not found in COV notification: {}", listOfValues);
                            updateConfigurationError(true);
                        } else {
                            LOG.debug("COV update: {}", value);
                            addLogRecord(LogRecord.createFromMonitoredValue(getNow(), value, statusFlags));
                        }
                    }
                }
            };
            getLocalDevice().getEventHandler().addListener(covListener);

            // Check if we're monitoring locally.
            if (monitored.getDeviceIdentifier().getInstanceNumber() == getLocalDevice().getInstanceNumber()) {
                // Subscribe, and resubscribe.
                resubscriptionFuture = getLocalDevice().scheduleAtFixedRate(() -> {
                    try {
                        covSubscription.handle(getLocalDevice(), getLocalDevice().getLoopbackAddress());
                        LOG.debug("COV subscription successful");
                    } catch (BACnetException e) {
                        LOG.warn("COV subscription failed", e);
                        updateConfigurationError(true);
                    }
                }, 0, resubscribeSeconds, TimeUnit.SECONDS);

            } else {
                // Get the remote device.
                RemoteDevice rd;
                try {
                    rd = getLocalDevice().getRemoteDeviceBlocking(monitored.getDeviceIdentifier().getInstanceNumber());
                } catch (BACnetException e) {
                    LOG.warn("Failed to find remote device to which to send unsubscribe", e);
                    updateConfigurationError(true);
                    return;
                }

                // Subscribe, and resubscribe.
                resubscriptionFuture = getLocalDevice().scheduleAtFixedRate(() -> {
                    try {
                        getLocalDevice().send(rd, covSubscription).get();
                        LOG.debug("COV subscription successful");
                    } catch (BACnetException e) {
                        LOG.warn("COV subscription failed", e);
                        updateConfigurationError(true);
                    }
                }, 0, resubscribeSeconds, TimeUnit.SECONDS);
            }

        } else if (loggingType.equals(LoggingType.triggered)) {
            set(PropertyIdentifier.logInterval, UnsignedInteger.ZERO);
        }

        updateConfigurationError(false);
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

        // Check the result.
        DeviceObjectPropertyReference monitored = get(PropertyIdentifier.logDeviceObjectProperty);
        PropertyValues values = result.getPropertyValues(monitored.getDeviceIdentifier().getInstanceNumber());
        Encodable value = values.getNoErrorCheck(monitored.getObjectIdentifier(),
                new PropertyReference(monitored.getPropertyIdentifier(), monitored.getPropertyArrayIndex()));

        LogRecord rec;
        boolean error = false;
        if (value instanceof ErrorClassAndCode) {
            rec = LogRecord.createFromMonitoredValue(now, value, null);
            error = true;
            LOG.warn("Error returned for value from poll: {}", value);
        } else {
            // Get the status flags
            Encodable statusFlags = values.getNoErrorCheck(monitored.getObjectIdentifier(),
                    PropertyIdentifier.statusFlags);
            if (statusFlags instanceof ErrorClassAndCode) {
                error = true;
                LOG.warn("Error returned for statusFlags from poll: {}", value);
                rec = LogRecord.createFromMonitoredValue(now, value, null);
            } else {
                rec = LogRecord.createFromMonitoredValue(now, value, (StatusFlags) statusFlags);
            }
        }

        updateConfigurationError(error);

        addLogRecord(rec);
    }

    private void updateConfigurationError(boolean error) {
        if (configurationError != error) {
            configurationError = error;
            if (error) {
                writePropertyInternal(PropertyIdentifier.reliability, Reliability.configurationError);
            } else {
                writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
            }
        }
    }

    private synchronized void addLogRecord(LogRecord rec) {
        // Check if logging is allowed.
        if (logDisabled)
            return;

        // Add the new record.
        addLogRecordImpl(rec);

        fullCheck();
    }

    private void addLogRecordImpl(LogRecord rec) {
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
            // Value overflowed. As per 12.25.16 set to 1.
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
                    addLogRecordImpl(new LogRecord(now, new LogStatus(logDisabled, false, false), null));
            }
        }
    }
}
