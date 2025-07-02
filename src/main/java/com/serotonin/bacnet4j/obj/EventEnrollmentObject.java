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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.obj.mixin.event.AlgoReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.serotonin.bacnet4j.obj.mixin.event.faultAlgo.FaultAlgorithm;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.FaultParameter;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.AbstractFaultParameter;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.FaultType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.PropertyReferences;
import com.serotonin.bacnet4j.util.PropertyValues;
import com.serotonin.bacnet4j.util.RequestUtils;

public class EventEnrollmentObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(EventEnrollmentObject.class);

    private final AlgoReportingMixin algoReporting;
    private final ScheduledFuture<?> pollingFuture;
    private final PropertyIdentifier[] monitoredProperties;
    private final DeviceObjectPropertyReference eventParameterReference;
    private final List<DeviceObjectPropertyReference> faultParameterReferences;
    private final PropertyReferences monitoredPropertyReferences;
    private boolean configurationError;

    public EventEnrollmentObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final DeviceObjectPropertyReference objectPropertyReference, final NotifyType notifyType,
            final EventParameter eventParameter, final EventTransitionBits eventEnable, final int notificationClass,
            final int pollDelayMillis, final UnsignedInteger timeDelayNormal, final FaultParameter faultParameter)
            throws BACnetServiceException {
        super(localDevice, ObjectType.eventEnrollment, instanceNumber, name);

        // Validation
        if (objectPropertyReference.getPropertyIdentifier().isOneOf(PropertyIdentifier.all, PropertyIdentifier.required,
                PropertyIdentifier.optional)) {
            throw new IllegalArgumentException("PropertyIdentifier cannot be special identifier: "
                    + objectPropertyReference.getPropertyIdentifier());
        }
        final AbstractEventParameter aep = eventParameter.getChoice().getDatum();
        eventParameterReference = aep.getReference();
        if (eventParameterReference != null) {
            // Ensure that any reference made in the event parameters is to the same device
            // as the object property reference.
            // TODO allow different devices.
            if (!eventParameterReference.getDeviceIdentifier().equals(objectPropertyReference.getDeviceIdentifier())) {
                throw new IllegalArgumentException(
                        "Event parameter reference must use the same device as the object property reference: parameter="
                                + eventParameterReference.getDeviceIdentifier() + ", property="
                                + objectPropertyReference.getPropertyIdentifier());
            }
        }

        writePropertyInternal(PropertyIdentifier.eventType, eventParameter.getEventType());
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.eventParameters, eventParameter);
        writePropertyInternal(PropertyIdentifier.objectPropertyReference, objectPropertyReference);
        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);
        if (timeDelayNormal != null)
            writePropertyInternal(PropertyIdentifier.timeDelayNormal, timeDelayNormal);
        if (faultParameter != null) {
            writePropertyInternal(PropertyIdentifier.faultType, faultParameter.getFaultType());
            writePropertyInternal(PropertyIdentifier.faultParameters, faultParameter);
        } else {
            writePropertyInternal(PropertyIdentifier.faultType, FaultType.none);
            writePropertyInternal(PropertyIdentifier.faultParameters, new FaultParameter(Null.instance));
        }

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.eventType));

        // Event parameters and algo
        final EventAlgorithm eventAlgo = aep.createEventAlgorithm();
        Objects.requireNonNull(eventAlgo, "No algorithm defined for event parameter type " + eventParameter.getClass());

        // Fault parameters and algo
        AbstractFaultParameter afp = null;
        FaultAlgorithm faultAlgo = null;
        if (faultParameter != null) {
            afp = (AbstractFaultParameter) faultParameter.getEntry().getDatum();
            faultAlgo = afp.createFaultAlgorithm();
            Objects.requireNonNull(faultAlgo,
                    "No algorithm defined for fault parameter type " + faultParameter.getClass());

            if (afp.getReferences() != null) {
                faultParameterReferences = afp.getReferences();
                for (final DeviceObjectPropertyReference faultRef : faultParameterReferences) {
                    // Ensure that any reference made in the fault parameters is to the same device
                    // as the object property reference.
                    // TODO allow different devices.
                    if (!faultRef.getDeviceIdentifier().equals(objectPropertyReference.getDeviceIdentifier())) {
                        throw new IllegalArgumentException(
                                "Fault parameter reference must use the same device as the object property reference: parameter="
                                        + faultRef.getDeviceIdentifier() + ", property="
                                        + objectPropertyReference.getPropertyIdentifier());
                    }
                }
            } else {
                faultParameterReferences = new ArrayList<>();
            }

            // Table 13-5: add change of reliability notification parameters
            faultParameterReferences.add(new DeviceObjectPropertyReference( //
                    objectPropertyReference.getDeviceIdentifier().getInstanceNumber(), //
                    objectPropertyReference.getObjectIdentifier(), //
                    PropertyIdentifier.reliability));
            faultParameterReferences.add(new DeviceObjectPropertyReference( //
                    objectPropertyReference.getDeviceIdentifier().getInstanceNumber(), //
                    objectPropertyReference.getObjectIdentifier(), //
                    PropertyIdentifier.statusFlags));
        } else {
            faultParameterReferences = null;
        }

        // Algo reporting mixin
        algoReporting = new AlgoReportingMixin(this, eventAlgo, aep, faultAlgo, afp, objectPropertyReference);
        addMixin(algoReporting);

        //
        // Create the list of monitored values.
        monitoredPropertyReferences = new PropertyReferences();

        // Add the referenced value.
        monitoredPropertyReferences.addIndex(objectPropertyReference.getObjectIdentifier(),
                objectPropertyReference.getPropertyIdentifier(), objectPropertyReference.getPropertyArrayIndex());

        // Add the additional monitored properties (Table 12-15.1)
        monitoredProperties = eventAlgo.getAdditionalMonitoredProperties();
        for (final PropertyIdentifier pid : monitoredProperties)
            monitoredPropertyReferences.add(objectPropertyReference.getObjectIdentifier(), pid);

        // Add the event parameter reference, if any.
        if (eventParameterReference != null) {
            monitoredPropertyReferences.addIndex(eventParameterReference.getObjectIdentifier(),
                    eventParameterReference.getPropertyIdentifier(), eventParameterReference.getPropertyArrayIndex());
        }

        // Add the fault parameters references, if any.
        if (faultParameterReferences != null) {
            for (final DeviceObjectPropertyReference faultRef : faultParameterReferences) {
                monitoredPropertyReferences.addIndex(faultRef.getObjectIdentifier(), faultRef.getPropertyIdentifier(),
                        faultRef.getPropertyArrayIndex());
            }
        }

        //
        // Start polling
        pollingFuture = localDevice.scheduleWithFixedDelay(() -> doPoll(), pollDelayMillis, pollDelayMillis,
                TimeUnit.MILLISECONDS);

        localDevice.addObject(this);
    }

    @Override
    protected void terminateImpl() {
        pollingFuture.cancel(false);
    }

    private void doPoll() {
        try {
            doPollThrow();
        } catch (final PollException e) {
            if (!configurationError) {
                configurationError = true;
                writePropertyInternal(PropertyIdentifier.reliability, Reliability.configurationError);
                LOG.warn("Polling exception", e);
            }
        } catch (final Exception e) {
            LOG.warn("Exception while polling", e);
        }
    }

    private static class PollException extends Exception {
        private static final long serialVersionUID = 1L;

        public PollException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public PollException(final String message) {
            super(message);
        }
    }

    private void doPollThrow() throws PollException {
        // TODO consider using a polling delegate
        final DeviceObjectPropertyReference ref = get(PropertyIdentifier.objectPropertyReference);

        Encodable value;
        final Map<ObjectPropertyReference, Encodable> additionalValues = new HashMap<>();

        if (ref.getDeviceIdentifier().equals(getLocalDevice().getId())) {
            // A local object
            final BACnetObject bo = getLocalDevice().getObject(ref.getObjectIdentifier());
            if (bo == null) {
                throw new PollException("EventEnrollment could not find local object at " + ref);
            }

            try {
                value = bo.readProperty(ref.getPropertyIdentifier(), ref.getPropertyArrayIndex());
                for (final PropertyIdentifier pid : monitoredProperties) {
                    additionalValues.put(new ObjectPropertyReference(bo.getId(), pid), bo.readProperty(pid));
                }
            } catch (final BACnetServiceException e) {
                throw new PollException("Error getting property from local object at " + ref, e);
            }

            if (eventParameterReference != null) {
                addParameterReference(getLocalDevice(), eventParameterReference, additionalValues);
            }

            if (faultParameterReferences != null) {
                for (final DeviceObjectPropertyReference faultRef : faultParameterReferences) {
                    addParameterReference(getLocalDevice(), faultRef, additionalValues);
                }
            }

        } else {
            // A remote object
            final RemoteDevice rd;
            try {
                rd = getLocalDevice().getRemoteDeviceBlocking(ref.getDeviceIdentifier().getInstanceNumber(), 30000);
            } catch (final BACnetException e) {
                throw new PollException("Error finding remote device at " + ref, e);
            }

            try {
                final PropertyValues results = RequestUtils.readProperties(getLocalDevice(), rd,
                        monitoredPropertyReferences, false, null);

                value = results.getNoErrorCheck(ref.getObjectIdentifier(),
                        new PropertyReference(ref.getPropertyIdentifier(), ref.getPropertyArrayIndex()));

                // Gather the additional properties
                for (final PropertyIdentifier pid : monitoredProperties) {
                    transferPropertyValue(results, additionalValues, ref.getObjectIdentifier(), pid, null);
                }

                // Get the event parameter
                if (eventParameterReference != null) {
                    transferPropertyValue(results, additionalValues, eventParameterReference.getObjectIdentifier(),
                            eventParameterReference.getPropertyIdentifier(),
                            eventParameterReference.getPropertyArrayIndex());
                }

                if (faultParameterReferences != null) {
                    for (final DeviceObjectPropertyReference faultRef : faultParameterReferences) {
                        transferPropertyValue(results, additionalValues, faultRef.getObjectIdentifier(),
                                faultRef.getPropertyIdentifier(), faultRef.getPropertyArrayIndex());
                    }
                }
            } catch (final BACnetException e) {
                throw new PollException("Error getting property from remote device at " + ref, e);
            }
        }

        if (value == null) {
            throw new PollException("Null property found at " + ref);
        }

        if (value instanceof ErrorClassAndCode) {
            throw new PollException("Error returned from reading property at " + ref + ": " + value);
        }

        // Check if the reliability value needs to be reset.
        if (configurationError) {
            configurationError = false;
            writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        }

        algoReporting.updateValue(value, additionalValues);
    }

    private static void addParameterReference(final LocalDevice localDevice,
            final DeviceObjectPropertyReference paramRef,
            final Map<ObjectPropertyReference, Encodable> additionalValues) throws PollException {
        final BACnetObject bo = localDevice.getObject(paramRef.getObjectIdentifier());
        if (bo == null) {
            throw new PollException("EventEnrollment could not find local object at " + paramRef.getObjectIdentifier());
        }

        try {
            additionalValues.put(
                    new ObjectPropertyReference(bo.getId(), paramRef.getPropertyIdentifier(),
                            paramRef.getPropertyArrayIndex()),
                    bo.readProperty(paramRef.getPropertyIdentifier(), paramRef.getPropertyArrayIndex()));
        } catch (final BACnetServiceException e) {
            throw new PollException("Error getting property from local object at " + paramRef, e);
        }
    }

    private static void transferPropertyValue(final PropertyValues results,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final ObjectIdentifier oid,
            final PropertyIdentifier pid, final UnsignedInteger pin) throws PollException {
        final Encodable e = results.getNoErrorCheck(oid, pid);
        if (e instanceof ErrorClassAndCode) {
            throw new PollException(
                    "Error returned from reading oid=" + oid + ", pid=" + pid + ", pin=" + pin + ": " + e);
        }
        additionalValues.put(new ObjectPropertyReference(oid, pid, pin), e);
    }
}
