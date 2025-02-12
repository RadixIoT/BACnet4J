package com.serotonin.bacnet4j.obj;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.obj.logBuffer.LogBuffer;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.PollingDelegate;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.BufferReadyAlgo;
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
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyReferences;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyValues;
import com.serotonin.bacnet4j.util.PropertyValues;

public class TrendLogMultipleObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(TrendLogMultipleObject.class);

    // CreateObject constructor
    public static TrendLogMultipleObject create(final LocalDevice localDevice, final int instanceNumber)
            throws BACnetServiceException {
        return new TrendLogMultipleObject(localDevice, instanceNumber,
                ObjectType.trendLogMultiple.toString() + " " + instanceNumber, new LinkedListLogBuffer<>(), false,
                DateTime.UNSPECIFIED, DateTime.UNSPECIFIED, new BACnetArray<>(), 60, false, 100) //
                        .supportIntrinsicReporting(20, 0, new EventTransitionBits(false, false, false),
                                NotifyType.event);
    }

    private final LogBuffer<LogMultipleRecord> buffer;

    private boolean logDisabled;
    private ScheduledFuture<?> startTimeFuture;
    private ScheduledFuture<?> stopTimeFuture;

    private PollingDelegate pollingDelegate;
    private ScheduledFuture<?> pollingFuture;

    public TrendLogMultipleObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final LogBuffer<LogMultipleRecord> buffer, final boolean enable, final DateTime startTime,
            final DateTime stopTime, final BACnetArray<DeviceObjectPropertyReference> logDeviceObjectProperty,
            final int logInterval, final boolean stopWhenFull, final int bufferSize) throws BACnetServiceException {
        super(localDevice, ObjectType.trendLogMultiple, instanceNumber, name);

        Objects.requireNonNull(localDevice);
        Objects.requireNonNull(name);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(stopTime);
        Objects.requireNonNull(logDeviceObjectProperty);

        set(PropertyIdentifier.enable, Boolean.valueOf(enable));
        set(PropertyIdentifier.startTime, startTime);
        set(PropertyIdentifier.stopTime, stopTime);
        set(PropertyIdentifier.logDeviceObjectProperty, logDeviceObjectProperty);
        set(PropertyIdentifier.logInterval, new UnsignedInteger(logInterval));
        set(PropertyIdentifier.stopWhenFull, Boolean.valueOf(stopWhenFull));
        set(PropertyIdentifier.bufferSize, new UnsignedInteger(bufferSize));
        set(PropertyIdentifier.logBuffer, buffer);
        set(PropertyIdentifier.recordCount, UnsignedInteger.ZERO);
        set(PropertyIdentifier.totalRecordCount, UnsignedInteger.ZERO);
        set(PropertyIdentifier.alignIntervals, Boolean.TRUE);
        set(PropertyIdentifier.intervalOffset, UnsignedInteger.ZERO);
        set(PropertyIdentifier.trigger, Boolean.FALSE);
        set(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        set(PropertyIdentifier.reliability, Reliability.noFaultDetected);

        updateMonitoredProperty();
        updateStartTime(startTime);
        updateStopTime(stopTime);
        withTriggered();

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.logBuffer, PropertyIdentifier.reliability,
                PropertyIdentifier.totalRecordCount));

        this.buffer = buffer;
        logDisabled = !allowLogging(getNow());

        localDevice.addObject(this);
    }

    public TrendLogMultipleObject withPolled(final int logInterval, final TimeUnit logIntervalUnit,
            final boolean alignIntervals, final int intervalOffset, final TimeUnit offsetUnit) {
        set(PropertyIdentifier.logInterval, new UnsignedInteger(logIntervalUnit.toMillis(logInterval) / 10));
        set(PropertyIdentifier.alignIntervals, Boolean.valueOf(alignIntervals));
        set(PropertyIdentifier.intervalOffset, new UnsignedInteger(offsetUnit.toMillis(intervalOffset) / 10));
        set(PropertyIdentifier.loggingType, LoggingType.polled);
        updateLoggingType();

        return this;
    }

    public TrendLogMultipleObject withTriggered() {
        set(PropertyIdentifier.loggingType, LoggingType.triggered);
        updateLoggingType();

        return this;
    }

    public TrendLogMultipleObject supportIntrinsicReporting(final int notificationThreshold,
            final int notificationClass, final EventTransitionBits eventEnable, final NotifyType notifyType) {
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
                PropertyIdentifier.notificationThreshold };

        // Now add the mixin.
        addMixin(new IntrinsicReportingMixin(this, algo, null, PropertyIdentifier.totalRecordCount, triggerProps)
                .withPostNotificationAction((notifParams) -> {
                    // After a notification has been sent, a couple values need to be updated.
                    final BufferReadyNotif brn = (BufferReadyNotif) notifParams.getParameter();
                    writePropertyInternal(PropertyIdentifier.lastNotifyRecord, brn.getCurrentNotification());
                    writePropertyInternal(PropertyIdentifier.recordsSinceNotification, UnsignedInteger.ZERO);
                }));

        updateMonitoredProperty();

        return this;
    }

    public boolean isLogDisabled() {
        return logDisabled;
    }

    public void withBuffer(Consumer<LogBuffer<LogMultipleRecord>> bufferConsumer) {
        synchronized (buffer) {
            bufferConsumer.accept(buffer);
        }
    }

    public void setEnabled(final boolean enabled) {
        writePropertyInternal(PropertyIdentifier.enable, Boolean.valueOf(enabled));
    }

    /**
     * Locally trigger a poll.
     *
     * @return true if the trigger was done, false if the trigger value was already true, indicating that a trigger
     *         was already in progress.
     */
    public synchronized boolean trigger() {
        final Boolean trigger = get(PropertyIdentifier.trigger);
        if (trigger.booleanValue()) {
            return false;
        }
        set(PropertyIdentifier.trigger, Boolean.TRUE);
        doTrigger();
        return true;
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
        } else if (PropertyIdentifier.loggingType.equals(value.getPropertyIdentifier())) {
            final LoggingType loggingType = value.getValue();
            if (loggingType.equals(LoggingType.cov))
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);

        } else if (PropertyIdentifier.startTime.equals(value.getPropertyIdentifier()) //
                || PropertyIdentifier.stopTime.equals(value.getPropertyIdentifier())) {
            // Ensure that the date time is either entirely unspecified or entirely specified.
            final DateTime dt = value.getValue();
            if (dt.equals(DateTime.UNSPECIFIED))
                return false;

            if (!dt.isFullySpecified())
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.parameterOutOfRange);

        } else if (PropertyIdentifier.logDeviceObjectProperty.equals(value.getPropertyIdentifier())) {
            final BACnetArray<DeviceObjectPropertyReference> refs = value.getValue();
            for (final DeviceObjectPropertyReference ref : refs) {
                if (ref.getPropertyIdentifier().isOneOf(PropertyIdentifier.all, PropertyIdentifier.required,
                        PropertyIdentifier.optional)) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.parameterOutOfRange);
                }
            }

        } else if (PropertyIdentifier.logInterval.equals(value.getPropertyIdentifier())) {
            final LoggingType loggingType = get(PropertyIdentifier.loggingType);
            if (!loggingType.equals(LoggingType.polled)) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            }
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
        } else if (PropertyIdentifier.logDeviceObjectProperty.equals(pid)) {
            purge();
            updateMonitoredProperty();

        } else if (PropertyIdentifier.logInterval.equals(pid)) {
            updateLoggingType();

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

        } else if (PropertyIdentifier.loggingType.equals(pid)) {
            updateLoggingType();

        } else if (pid.isOneOf(PropertyIdentifier.alignIntervals, PropertyIdentifier.intervalOffset)) {
            final LoggingType loggingType = get(PropertyIdentifier.loggingType);
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

    private void purge() {
        synchronized (buffer) {
            buffer.clear();
        }
        writePropertyInternal(PropertyIdentifier.recordsSinceNotification, UnsignedInteger.ZERO);
        addLogRecordImpl(new LogMultipleRecord(getNow(), new LogData(new LogStatus(logDisabled, true, false))));
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
        super.terminate();
        cancelFuture(startTimeFuture);
        cancelFuture(stopTimeFuture);
        cancelFuture(pollingFuture);
    }

    private static void cancelFuture(final ScheduledFuture<?> future) {
        if (future != null)
            future.cancel(false);
    }

    private void updateMonitoredProperty() {
        final BACnetArray<DeviceObjectPropertyReference> props = get(PropertyIdentifier.logDeviceObjectProperty);

        // Add the monitored property.
        final DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences();
        for (final DeviceObjectPropertyReference prop : props) {
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
    private void updateLoggingType() {
        final LoggingType loggingType = get(PropertyIdentifier.loggingType);

        cancelFuture(pollingFuture);

        if (loggingType.equals(LoggingType.polled)) {
            final UnsignedInteger logInterval = get(PropertyIdentifier.logInterval);
            final Boolean alignIntervals = get(PropertyIdentifier.alignIntervals);
            final UnsignedInteger intervalOffset = get(PropertyIdentifier.intervalOffset);

            long period = logInterval.longValue() * 10;
            if (period == 0)
                // 0 is a poor value. Default to 5 minutes in this case, since it "is a local matter".
                period = TimeUnit.MINUTES.toMillis(5);

            long initialDelay = 0;
            int offsetToUse = 0;
            if (alignIntervals.booleanValue()) {
                final long now = getLocalDevice().getClock().millis();

                // Find the largest time period to which the period aligns.
                if (period % TimeUnit.DAYS.toMillis(1) == 0) {
                    initialDelay = TimeUnit.DAYS.toMillis(1) - now % TimeUnit.DAYS.toMillis(1);
                } else if (period % TimeUnit.HOURS.toMillis(1) == 0) {
                    initialDelay = TimeUnit.HOURS.toMillis(1) - now % TimeUnit.HOURS.toMillis(1);
                } else if (period % TimeUnit.MINUTES.toMillis(1) == 0) {
                    initialDelay = TimeUnit.MINUTES.toMillis(1) - now % TimeUnit.MINUTES.toMillis(1);
                } else if (period % TimeUnit.SECONDS.toMillis(1) == 0) {
                    initialDelay = TimeUnit.SECONDS.toMillis(1) - now % TimeUnit.SECONDS.toMillis(1);
                }

                offsetToUse = intervalOffset.intValue() * 10;
                offsetToUse %= period;
            }

            initialDelay += offsetToUse;
            initialDelay %= period;

            pollingFuture = getLocalDevice().scheduleAtFixedRate(() -> doPoll(), initialDelay, period,
                    TimeUnit.MILLISECONDS);

        } else if (loggingType.equals(LoggingType.triggered)) {
            set(PropertyIdentifier.logInterval, UnsignedInteger.ZERO);
        }
    }

    private void doTrigger() {
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

    private synchronized void doPoll() {
        // The spec says that no *logging* should occur if the log is disabled, but there doesn't seem to be much
        // point in polling at all if this is the case, so we check here and abort accordingly.
        if (logDisabled)
            return;

        // Get the time before the poll, so that alignment looks right.
        final DateTime now = getNow();

        // Call the delegate to perform the poll.
        final DeviceObjectPropertyValues result = pollingDelegate.doPoll();

        // Process the results.
        final List<LogDataElement> elements = new ArrayList<>();
        final BACnetArray<DeviceObjectPropertyReference> props = get(PropertyIdentifier.logDeviceObjectProperty);
        for (final DeviceObjectPropertyReference prop : props) {
            LogDataElement element;
            if (prop.getDeviceIdentifier().isUninitialized() || prop.getObjectIdentifier().isUninitialized()) {
                element = new LogDataElement(new ErrorClassAndCode(ErrorClass.property, ErrorCode.noPropertySpecified));
            } else {
                final PropertyValues values = result.getPropertyValues(prop.getDeviceIdentifier().getInstanceNumber());
                if (values == null) {
                    throw new NullPointerException("Didn't find device "
                            + prop.getDeviceIdentifier().getInstanceNumber() + " in results " + result
                            + ", polling delegate remote references: " + pollingDelegate.getRemoteReferences());
                }
                final Encodable value = values.getNoErrorCheck(prop.getObjectIdentifier(),
                        new PropertyReference(prop.getPropertyIdentifier(), prop.getPropertyArrayIndex()));
                element = LogDataElement.createFromMonitoredValue(value);
            }
            elements.add(element);
        }

        final LogMultipleRecord record = new LogMultipleRecord(now, new LogData(new SequenceOf<>(elements)));
        addLogRecord(record);
    }

    private synchronized void addLogRecord(final LogMultipleRecord record) {
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

    private void addLogRecordImpl(final LogMultipleRecord record) {
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
            // Value overflowed. As per 12.30.21 set to 1.
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
                    addLogRecordImpl(new LogMultipleRecord(now, new LogData(new LogStatus(logDisabled, false, false))));
            }
        }
    }

    private DateTime getNow() {
        return new DateTime(getLocalDevice().getClock().millis());
    }
}
