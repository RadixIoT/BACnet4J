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

import java.io.Serial;
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
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultLifeSafety;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultState;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.FaultType;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfLifeSafety;
import com.serotonin.bacnet4j.type.eventParameter.ChangeOfState;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.eventParameter.OutOfRange;
import com.serotonin.bacnet4j.type.eventParameter.UnsignedRange;
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
    private boolean monitoredObjectFault;
    // Per 12.12.21 (addendum 135-2020co-2), true while the event and fault parameters are in
    // conflict, in which case the Reliability property is CONFIGURATION_ERROR and the algorithms
    // are not executed.
    private boolean parameterConflict;

    public EventEnrollmentObject(LocalDevice localDevice, int instanceNumber, String name,
            DeviceObjectPropertyReference objectPropertyReference, NotifyType notifyType, EventParameter eventParameter,
            EventTransitionBits eventEnable, int notificationClass, int pollDelayMillis,
            UnsignedInteger timeDelayNormal, FaultParameter faultParameter) {
        super(localDevice, ObjectType.eventEnrollment, instanceNumber, name);

        // Validation
        if (objectPropertyReference.getPropertyIdentifier().isOneOf(PropertyIdentifier.all, PropertyIdentifier.required,
                PropertyIdentifier.optional)) {
            throw new IllegalArgumentException("PropertyIdentifier cannot be special identifier: "
                    + objectPropertyReference.getPropertyIdentifier());
        }
        AbstractEventParameter aep = eventParameter.getChoice().getDatum();
        eventParameterReference = aep.getReference();
        if (eventParameterReference != null &&
                !eventParameterReference.getDeviceIdentifier().equals(objectPropertyReference.getDeviceIdentifier())) {
            // Ensure that any reference made in the event parameters is to the same device
            // as the object property reference.
            throw new IllegalArgumentException(
                    "Event parameter reference must use the same device as the object property reference: parameter="
                            + eventParameterReference.getDeviceIdentifier() + ", property="
                            + objectPropertyReference.getPropertyIdentifier());
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
        EventAlgorithm eventAlgo = aep.createEventAlgorithm();
        Objects.requireNonNull(eventAlgo, "No algorithm defined for event parameter type " + eventParameter.getClass());

        // Fault parameters and algo
        AbstractFaultParameter afp = null;
        FaultAlgorithm faultAlgo = null;
        faultParameterReferences = new ArrayList<>();
        if (faultParameter != null) {
            afp = faultParameter.getEntry().getDatum();
            faultAlgo = afp.createFaultAlgorithm();
            Objects.requireNonNull(faultAlgo,
                    "No algorithm defined for fault parameter type " + faultParameter.getClass());

            if (afp.getReferences() != null) {
                for (DeviceObjectPropertyReference faultRef : afp.getReferences()) {
                    // Ensure that any reference made in the fault parameters is to the same device
                    // as the object property reference.
                    if (!faultRef.getDeviceIdentifier().equals(objectPropertyReference.getDeviceIdentifier())) {
                        throw new IllegalArgumentException(
                                "Fault parameter reference must use the same device as the object property reference: parameter="
                                        + faultRef.getDeviceIdentifier() + ", property="
                                        + objectPropertyReference.getPropertyIdentifier());
                    }
                    faultParameterReferences.add(faultRef);
                }
            }
        }

        // Table 13-5 change-of-reliability notification parameters. Per addendum 135-2016bu-6
        // (Clause 12.12.21), the monitored object's Reliability is polled unconditionally so
        // that the Event Enrollment object's Reliability can be promoted to MONITORED_OBJECT_FAULT
        // whenever the monitored object itself is unreliable, regardless of whether a fault
        // algorithm is in use.
        faultParameterReferences.add(new DeviceObjectPropertyReference(
                objectPropertyReference.getDeviceIdentifier().getInstanceNumber(),
                objectPropertyReference.getObjectIdentifier(),
                PropertyIdentifier.reliability));
        faultParameterReferences.add(new DeviceObjectPropertyReference(
                objectPropertyReference.getDeviceIdentifier().getInstanceNumber(),
                objectPropertyReference.getObjectIdentifier(),
                PropertyIdentifier.statusFlags));

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
        for (PropertyIdentifier pid : monitoredProperties)
            monitoredPropertyReferences.add(objectPropertyReference.getObjectIdentifier(), pid);

        // Add the event parameter reference, if any.
        if (eventParameterReference != null) {
            monitoredPropertyReferences.addIndex(eventParameterReference.getObjectIdentifier(),
                    eventParameterReference.getPropertyIdentifier(), eventParameterReference.getPropertyArrayIndex());
        }

        // Add the fault parameters references.
        for (DeviceObjectPropertyReference faultRef : faultParameterReferences) {
            monitoredPropertyReferences.addIndex(faultRef.getObjectIdentifier(), faultRef.getPropertyIdentifier(),
                    faultRef.getPropertyArrayIndex());
        }

        // Check for conflicting event and fault parameters.
        updateParameterConflict();

        //
        // Start polling
        pollingFuture = localDevice.scheduleWithFixedDelay(this::doPoll, pollDelayMillis, pollDelayMillis,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Per 12.12.21 (addendum 135-2020co-2), the Reliability property takes on CONFIGURATION_ERROR
     * while the event and fault parameters are in conflict. Evaluated at construction and after
     * writes of the Event_Parameters and Fault_Parameters properties.
     */
    private synchronized void updateParameterConflict() {
        EventParameter eventParameter = get(PropertyIdentifier.eventParameters);
        FaultParameter faultParameter = get(PropertyIdentifier.faultParameters);
        boolean conflict = hasParameterConflict(eventParameter, faultParameter);
        if (conflict) {
            parameterConflict = true;
            writePropertyInternal(PropertyIdentifier.reliability, Reliability.configurationError);
        } else if (parameterConflict) {
            // The conflict has been resolved. Only restore the reliability that this check set, so
            // that reliabilities from other sources are not clobbered.
            parameterConflict = false;
            writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        }
    }

    private static boolean hasParameterConflict(EventParameter eventParameter, FaultParameter faultParameter) {
        AbstractEventParameter aep = eventParameter.getChoice().getDatum();
        AbstractFaultParameter afp = null;
        if (faultParameter != null && !faultParameter.isNull()) {
            afp = faultParameter.getEntry().getDatum();
        }

        if (aep instanceof ChangeOfState changeOfState && afp instanceof FaultState faultState) {
            // A value present in both the List_Of_Values and the List_Of_Fault_Values parameters.
            return SequenceOf.intersection(changeOfState.getListOfValues(), faultState.getListOfFaultValues());
        }
        if (aep instanceof ChangeOfLifeSafety changeOfLifeSafety) {
            SequenceOf<LifeSafetyState> alarmValues = changeOfLifeSafety.getListOfAlarmValues();
            SequenceOf<LifeSafetyState> lifeSafetyAlarmValues = changeOfLifeSafety.getListOfLifeSafetyAlarmValues();
            // A value present in more than one of the List_Of_Alarm_Values or the
            // List_Of_Life_Safety_Alarm_Values parameters and the List_Of_Fault_Values parameter.
            if (SequenceOf.intersection(alarmValues, lifeSafetyAlarmValues)) {
                return true;
            }
            if (afp instanceof FaultLifeSafety faultLifeSafety) {
                return SequenceOf.intersection(alarmValues, faultLifeSafety.getListOfFaultValues())
                        || SequenceOf.intersection(lifeSafetyAlarmValues, faultLifeSafety.getListOfFaultValues());
            }
            return false;
        }
        if (aep instanceof OutOfRange outOfRange) {
            // The Low_Limit parameter is greater than the High_Limit parameter.
            return outOfRange.getLowLimit().floatValue() > outOfRange.getHighLimit().floatValue();
        }
        if (aep instanceof UnsignedRange unsignedRange) {
            // The Low_Limit parameter is greater than the High_Limit parameter.
            return unsignedRange.getLowLimit().longValue() > unsignedRange.getHighLimit().longValue();
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        // The initialized check prevents the conflict check from running against the partially
        // written properties during construction, before the mixins that react to reliability
        // changes exist.
        if (isInitialized()
                && pid.isOneOf(PropertyIdentifier.eventParameters, PropertyIdentifier.faultParameters)) {
            updateParameterConflict();
        }
    }

    @Override
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
        if (PropertyIdentifier.eventParameters.equals(value.getPropertyIdentifier())
                && value.getValue() instanceof EventParameter eventParameter) {
            // Per 12.12.7 (addendum 135-2020ci-1), an attempt to write parameters for an unsupported
            // event algorithm returns OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED.
            AbstractEventParameter aep = eventParameter.getChoice().getDatum();
            if (aep.createEventAlgorithm() == null) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.optionalFunctionalityNotSupported);
            }
        } else if (PropertyIdentifier.faultParameters.equals(value.getPropertyIdentifier())
                && value.getValue() instanceof FaultParameter faultParameter) {
            // Per 12.12.23 (addendum 135-2020ci-1), an attempt to write parameters for an unsupported
            // fault algorithm returns OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED. The Null choice is valid
            // and means that no fault algorithm is in use.
            if (!faultParameter.isNull()) {
                AbstractFaultParameter afp = faultParameter.getEntry().getDatum();
                if (afp.createFaultAlgorithm() == null) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.optionalFunctionalityNotSupported);
                }
            }
        }
        return false;
    }

    @Override
    protected void terminateImpl() {
        pollingFuture.cancel(false);
    }

    private void doPoll() {
        try {
            doPollThrow();
        } catch (PollException e) {
            if (!configurationError) {
                configurationError = true;
                writePropertyInternal(PropertyIdentifier.reliability, Reliability.configurationError);
                LOG.warn("Polling exception", e);
            }
        } catch (Exception e) {
            LOG.warn("Exception while polling", e);
        }
    }

    private static class PollException extends Exception {
        @Serial
        private static final long serialVersionUID = 1L;

        public PollException(String message, Throwable cause) {
            super(message, cause);
        }

        public PollException(String message) {
            super(message);
        }
    }

    private void doPollThrow() throws PollException {
        if (parameterConflict) {
            // Per 12.12.21 (addendum 135-2020co-2), a conflicting configuration keeps the Reliability
            // property at CONFIGURATION_ERROR and inhibits the execution of the event and fault
            // algorithms.
            return;
        }

        DeviceObjectPropertyReference ref = get(PropertyIdentifier.objectPropertyReference);

        Encodable value;
        Map<ObjectPropertyReference, Encodable> additionalValues = new HashMap<>();

        if (ref.getDeviceIdentifier().equals(getLocalDevice().getId())) {
            // A local object
            BACnetObject bo = getLocalDevice().getObject(ref.getObjectIdentifier());
            if (bo == null) {
                throw new PollException("EventEnrollment could not find local object at " + ref);
            }

            try {
                value = bo.readProperty(ref.getPropertyIdentifier(), ref.getPropertyArrayIndex());
                for (PropertyIdentifier pid : monitoredProperties) {
                    additionalValues.put(new ObjectPropertyReference(bo.getId(), pid), bo.readProperty(pid));
                }
            } catch (BACnetServiceException e) {
                throw new PollException("Error getting property from local object at " + ref, e);
            }

            if (eventParameterReference != null) {
                addParameterReference(getLocalDevice(), eventParameterReference, additionalValues);
            }

            if (faultParameterReferences != null) {
                for (DeviceObjectPropertyReference faultRef : faultParameterReferences) {
                    addParameterReference(getLocalDevice(), faultRef, additionalValues);
                }
            }

        } else {
            // A remote object
            RemoteDevice rd;
            try {
                rd = getLocalDevice().getRemoteDeviceBlocking(ref.getDeviceIdentifier().getInstanceNumber(), 30000);
            } catch (BACnetException e) {
                throw new PollException("Error finding remote device at " + ref, e);
            }

            try {
                PropertyValues results = RequestUtils.readProperties(getLocalDevice(), rd,
                        monitoredPropertyReferences, false, null);

                value = results.getNoErrorCheck(ref.getObjectIdentifier(),
                        new PropertyReference(ref.getPropertyIdentifier(), ref.getPropertyArrayIndex()));

                // Gather the additional properties
                for (PropertyIdentifier pid : monitoredProperties) {
                    transferPropertyValue(results, additionalValues, ref.getObjectIdentifier(), pid, null);
                }

                // Get the event parameter
                if (eventParameterReference != null) {
                    transferPropertyValue(results, additionalValues, eventParameterReference.getObjectIdentifier(),
                            eventParameterReference.getPropertyIdentifier(),
                            eventParameterReference.getPropertyArrayIndex());
                }

                if (faultParameterReferences != null) {
                    for (DeviceObjectPropertyReference faultRef : faultParameterReferences) {
                        transferPropertyValue(results, additionalValues, faultRef.getObjectIdentifier(),
                                faultRef.getPropertyIdentifier(), faultRef.getPropertyArrayIndex());
                    }
                }
            } catch (BACnetException e) {
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

        // Per addendum 135-2016bu-6 (Clause 12.12.21): once the Event Enrollment object's own
        // reliability is NO_FAULT_DETECTED, if the monitored object's reliability is not
        // NO_FAULT_DETECTED, the Event Enrollment object's Reliability shall be
        // MONITORED_OBJECT_FAULT and the fault algorithm is not evaluated.
        Reliability monitoredReliability = (Reliability) additionalValues.get(
                new ObjectPropertyReference(ref.getObjectIdentifier(), PropertyIdentifier.reliability));
        if (monitoredReliability != null && !Reliability.noFaultDetected.equals(monitoredReliability)) {
            if (!monitoredObjectFault) {
                monitoredObjectFault = true;
                writePropertyInternal(PropertyIdentifier.reliability, Reliability.monitoredObjectFault);
            }
            return;
        }
        if (monitoredObjectFault) {
            monitoredObjectFault = false;
            writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        }

        algoReporting.updateValue(value, additionalValues);
    }

    private static void addParameterReference(LocalDevice localDevice, DeviceObjectPropertyReference paramRef,
            Map<ObjectPropertyReference, Encodable> additionalValues) throws PollException {
        BACnetObject bo = localDevice.getObject(paramRef.getObjectIdentifier());
        if (bo == null) {
            throw new PollException("EventEnrollment could not find local object at " + paramRef.getObjectIdentifier());
        }

        try {
            additionalValues.put(
                    new ObjectPropertyReference(bo.getId(), paramRef.getPropertyIdentifier(),
                            paramRef.getPropertyArrayIndex()),
                    bo.readProperty(paramRef.getPropertyIdentifier(), paramRef.getPropertyArrayIndex()));
        } catch (BACnetServiceException e) {
            throw new PollException("Error getting property from local object at " + paramRef, e);
        }
    }

    private static void transferPropertyValue(PropertyValues results,
            Map<ObjectPropertyReference, Encodable> additionalValues, ObjectIdentifier oid, PropertyIdentifier pid,
            UnsignedInteger pin) throws PollException {
        Encodable e = results.getNoErrorCheck(oid, pid);
        if (e instanceof ErrorClassAndCode) {
            throw new PollException(
                    "Error returned from reading oid=" + oid + ", pid=" + pid + ", pin=" + pin + ": " + e);
        }
        additionalValues.put(new ObjectPropertyReference(oid, pid, pin), e);
    }
}
