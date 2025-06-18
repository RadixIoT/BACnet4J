package com.serotonin.bacnet4j.obj;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.PollingDelegate;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyReferences;
import com.serotonin.bacnet4j.util.DeviceObjectPropertyValues;
import com.serotonin.bacnet4j.util.PropertyValues;

public class AveragingObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(AveragingObject.class);

    private final List<Sample> buffer = new LinkedList<>();

    private PollingDelegate pollingDelegate;
    private ScheduledFuture<?> pollingFuture;

    public AveragingObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final DeviceObjectPropertyReference objectPropertyReference, final int windowInterval,
            final int windowSamples) throws BACnetServiceException {
        super(localDevice, ObjectType.averaging, instanceNumber, name);

        Objects.requireNonNull(objectPropertyReference);

        if (windowInterval < 1)
            throw new IllegalArgumentException("windowInterval must be greater than zero");
        if (windowSamples < 1)
            throw new IllegalArgumentException("windowSamples must be greater than zero");

        reset();

        set(PropertyIdentifier.objectPropertyReference, objectPropertyReference);
        set(PropertyIdentifier.windowInterval, new UnsignedInteger(windowInterval));
        set(PropertyIdentifier.windowSamples, new UnsignedInteger(windowSamples));

        // Mixins
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.minimumValue,
                PropertyIdentifier.minimumValueTimestamp, PropertyIdentifier.averageValue,
                PropertyIdentifier.varianceValue, PropertyIdentifier.maximumValue,
                PropertyIdentifier.maximumValueTimestamp, PropertyIdentifier.validSamples));

        updateMonitoredProperty();
        reinitializePolling();

        localDevice.addObject(this);
    }

    private void reset() {
        buffer.clear();
        writePropertyInternal(PropertyIdentifier.minimumValue, new Real(Float.POSITIVE_INFINITY));
        writePropertyInternal(PropertyIdentifier.minimumValueTimestamp, DateTime.UNSPECIFIED);
        writePropertyInternal(PropertyIdentifier.averageValue, new Real(Float.NaN));
        writePropertyInternal(PropertyIdentifier.varianceValue, new Real(Float.NaN));
        writePropertyInternal(PropertyIdentifier.maximumValue, new Real(Float.NEGATIVE_INFINITY));
        writePropertyInternal(PropertyIdentifier.maximumValueTimestamp, DateTime.UNSPECIFIED);
        set(PropertyIdentifier.attemptedSamples, UnsignedInteger.ZERO);
        writePropertyInternal(PropertyIdentifier.validSamples, UnsignedInteger.ZERO);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.attemptedSamples.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger attemptedSamples = value.getValue();
            if (attemptedSamples.intValue() != 0) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.parameterOutOfRange);
            }
        } else if (PropertyIdentifier.windowInterval.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger windowInterval = value.getValue();
            if (windowInterval.intValue() < 1) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.parameterOutOfRange);
            }
        } else if (PropertyIdentifier.windowSamples.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger windowSamples = value.getValue();
            if (windowSamples.intValue() < 1) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.parameterOutOfRange);
            }
        }
        return false;
    }

    @Override
    protected synchronized void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (pid.equals(PropertyIdentifier.attemptedSamples)) {
            reset();
        } else if (pid.equals(PropertyIdentifier.objectPropertyReference)) {
            updateMonitoredProperty();
            reset();
        } else if (pid.isOneOf(PropertyIdentifier.windowInterval, PropertyIdentifier.windowSamples)) {
            reset();
            reinitializePolling();
        }
    }

    @Override
    protected void terminateImpl() {
        super.terminate();
        cancelFuture(pollingFuture);
    }

    private static void cancelFuture(final ScheduledFuture<?> future) {
        if (future != null)
            future.cancel(false);
    }

    private void updateMonitoredProperty() {
        final DeviceObjectPropertyReference monitored = get(PropertyIdentifier.objectPropertyReference);

        // Add the monitored property.
        final DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences();
        refs.addIndex(monitored.getDeviceIdentifier().getInstanceNumber(), monitored.getObjectIdentifier(),
                monitored.getPropertyIdentifier(), monitored.getPropertyArrayIndex());

        pollingDelegate = new PollingDelegate(getLocalDevice(), refs);
    }

    private void reinitializePolling() {
        cancelFuture(pollingFuture);

        final UnsignedInteger windowInterval = get(PropertyIdentifier.windowInterval);
        final UnsignedInteger windowSamples = get(PropertyIdentifier.windowSamples);

        final long periodMillis = windowInterval.intValue() * 1000L / windowSamples.intValue();

        pollingFuture = getLocalDevice().scheduleAtFixedRate(this::doPoll, 1, periodMillis, TimeUnit.MILLISECONDS);
    }

    private synchronized void doPoll() {
        LOG.debug("Starting poll");

        // Call the delegate to perform the poll.
        final DeviceObjectPropertyValues result = pollingDelegate.doPoll();

        // Check the result.
        final DeviceObjectPropertyReference monitored = get(PropertyIdentifier.objectPropertyReference);
        final PropertyValues values = result.getPropertyValues(monitored.getDeviceIdentifier().getInstanceNumber());
        final Encodable value = values.getNoErrorCheck(monitored.getObjectIdentifier(),
                new PropertyReference(monitored.getPropertyIdentifier(), monitored.getPropertyArrayIndex()));

        Sample newSample = null;
        if (value instanceof ErrorClassAndCode) {
            LOG.warn("Error returned for value from poll: {}", value);
        } else if (!(value instanceof Boolean || value instanceof SignedInteger || value instanceof UnsignedInteger || value instanceof Enumerated || value instanceof Real)) {
            LOG.warn("Unsupported data type returned from poll: {}", value);
        } else {
            newSample = new Sample((Primitive) value, new DateTime(getLocalDevice().getClock().millis()));
        }

        final UnsignedInteger windowSamples = get(PropertyIdentifier.windowSamples);

        //
        // Update the buffer.
        // Drop the tail value if we're at the max size.
        while (buffer.size() >= windowSamples.intValue()) {
            buffer.remove(0);
        }

        buffer.add(newSample);

        //
        // Calculate new values. Because the min or max values may have dropped off of the buffer, and the new min or
        // max could be anywhere in the list, we need to scan the buffer. So, might as well recalculate everything.
        float minimumValue = Float.POSITIVE_INFINITY;
        DateTime minimumValueTimestamp = DateTime.UNSPECIFIED;
        float sum = 0;
        float maximumValue = Float.NEGATIVE_INFINITY;
        DateTime maximumValueTimestamp = DateTime.UNSPECIFIED;
        int validSamples = 0;

        for (final Sample sample : buffer) {
            if (sample != null) {
                final float sampleValue = sample.getValue();

                if (sampleValue < minimumValue) {
                    minimumValue = sampleValue;
                    minimumValueTimestamp = sample.timestamp;
                }

                sum += sampleValue;

                if (sampleValue > maximumValue) {
                    maximumValue = sampleValue;
                    maximumValueTimestamp = sample.timestamp;
                }

                validSamples++;
            }
        }

        //
        // Update object values.
        writePropertyInternal(PropertyIdentifier.minimumValue, new Real(minimumValue));
        writePropertyInternal(PropertyIdentifier.minimumValueTimestamp, minimumValueTimestamp);
        if (validSamples == 0) {
            writePropertyInternal(PropertyIdentifier.averageValue, new Real(Float.NaN));
            writePropertyInternal(PropertyIdentifier.varianceValue, new Real(Float.NaN));
        } else {
            writePropertyInternal(PropertyIdentifier.averageValue, new Real(sum / validSamples));
            writePropertyInternal(PropertyIdentifier.varianceValue, new Real(maximumValue - minimumValue));
        }
        writePropertyInternal(PropertyIdentifier.maximumValue, new Real(maximumValue));
        writePropertyInternal(PropertyIdentifier.maximumValueTimestamp, maximumValueTimestamp);
        set(PropertyIdentifier.attemptedSamples, new UnsignedInteger(buffer.size()));
        writePropertyInternal(PropertyIdentifier.validSamples, new UnsignedInteger(validSamples));

        LOG.debug("Finished poll");
    }

    record Sample(Primitive value, DateTime timestamp) {
        float getValue() {
            if (value instanceof Boolean v)
                return v.booleanValue() ? 1 : 0;
            if (value instanceof SignedInteger v)
                return v.intValue();
            if (value instanceof UnsignedInteger v)
                return v.intValue();
            if (value instanceof Enumerated v)
                return v.intValue();
            return ((Real) value).floatValue();
        }
    }
}
