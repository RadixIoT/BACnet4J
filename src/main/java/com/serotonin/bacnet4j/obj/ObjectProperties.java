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

import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.AccessRule;
import com.serotonin.bacnet4j.type.constructed.AccessThreatLevel;
import com.serotonin.bacnet4j.type.constructed.AccumulatorRecord;
import com.serotonin.bacnet4j.type.constructed.ActionList;
import com.serotonin.bacnet4j.type.constructed.AddressBinding;
import com.serotonin.bacnet4j.type.constructed.AssignedAccessRights;
import com.serotonin.bacnet4j.type.constructed.AssignedLandingCalls;
import com.serotonin.bacnet4j.type.constructed.AuthenticationFactor;
import com.serotonin.bacnet4j.type.constructed.AuthenticationFactorFormat;
import com.serotonin.bacnet4j.type.constructed.AuthenticationPolicy;
import com.serotonin.bacnet4j.type.constructed.BDTEntry;
import com.serotonin.bacnet4j.type.constructed.CalendarEntry;
import com.serotonin.bacnet4j.type.constructed.ChannelValue;
import com.serotonin.bacnet4j.type.constructed.ClientCov;
import com.serotonin.bacnet4j.type.constructed.CovMultipleSubscription;
import com.serotonin.bacnet4j.type.constructed.CovSubscription;
import com.serotonin.bacnet4j.type.constructed.CredentialAuthenticationFactor;
import com.serotonin.bacnet4j.type.constructed.DailySchedule;
import com.serotonin.bacnet4j.type.constructed.DateRange;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Destination;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectReference;
import com.serotonin.bacnet4j.type.constructed.EventLogRecord;
import com.serotonin.bacnet4j.type.constructed.EventNotificationSubscription;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.FDTEntry;
import com.serotonin.bacnet4j.type.constructed.FaultParameter;
import com.serotonin.bacnet4j.type.constructed.HostNPort;
import com.serotonin.bacnet4j.type.constructed.LandingCallStatus;
import com.serotonin.bacnet4j.type.constructed.LandingDoorStatus;
import com.serotonin.bacnet4j.type.constructed.LiftCarCallList;
import com.serotonin.bacnet4j.type.constructed.LightingCommand;
import com.serotonin.bacnet4j.type.constructed.LimitEnable;
import com.serotonin.bacnet4j.type.constructed.LogMultipleRecord;
import com.serotonin.bacnet4j.type.constructed.LogRecord;
import com.serotonin.bacnet4j.type.constructed.NameValue;
import com.serotonin.bacnet4j.type.constructed.NameValueCollection;
import com.serotonin.bacnet4j.type.constructed.NetworkSecurityPolicy;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.ObjectTypesSupported;
import com.serotonin.bacnet4j.type.constructed.OptionalBinaryPV;
import com.serotonin.bacnet4j.type.constructed.OptionalCharacterString;
import com.serotonin.bacnet4j.type.constructed.OptionalReal;
import com.serotonin.bacnet4j.type.constructed.OptionalUnsigned;
import com.serotonin.bacnet4j.type.constructed.PortPermission;
import com.serotonin.bacnet4j.type.constructed.Prescale;
import com.serotonin.bacnet4j.type.constructed.PriorityArray;
import com.serotonin.bacnet4j.type.constructed.ProcessIdSelection;
import com.serotonin.bacnet4j.type.constructed.PropertyAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.RouterEntry;
import com.serotonin.bacnet4j.type.constructed.Scale;
import com.serotonin.bacnet4j.type.constructed.SecurityKeySet;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.constructed.SetpointReference;
import com.serotonin.bacnet4j.type.constructed.ShedLevel;
import com.serotonin.bacnet4j.type.constructed.SpecialEvent;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.TimerStateChangeValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.constructed.VmacEntry;
import com.serotonin.bacnet4j.type.constructed.VtSession;
import com.serotonin.bacnet4j.type.enumerated.AccessCredentialDisable;
import com.serotonin.bacnet4j.type.enumerated.AccessCredentialDisableReason;
import com.serotonin.bacnet4j.type.enumerated.AccessEvent;
import com.serotonin.bacnet4j.type.enumerated.AccessPassbackMode;
import com.serotonin.bacnet4j.type.enumerated.AccessUserType;
import com.serotonin.bacnet4j.type.enumerated.AccessZoneOccupancyState;
import com.serotonin.bacnet4j.type.enumerated.Action;
import com.serotonin.bacnet4j.type.enumerated.AuthenticationStatus;
import com.serotonin.bacnet4j.type.enumerated.AuthorizationExemption;
import com.serotonin.bacnet4j.type.enumerated.AuthorizationMode;
import com.serotonin.bacnet4j.type.enumerated.BackupState;
import com.serotonin.bacnet4j.type.enumerated.BinaryLightingPV;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.DeviceStatus;
import com.serotonin.bacnet4j.type.enumerated.DoorAlarmState;
import com.serotonin.bacnet4j.type.enumerated.DoorSecuredStatus;
import com.serotonin.bacnet4j.type.enumerated.DoorStatus;
import com.serotonin.bacnet4j.type.enumerated.DoorValue;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EscalatorFault;
import com.serotonin.bacnet4j.type.enumerated.EscalatorMode;
import com.serotonin.bacnet4j.type.enumerated.EscalatorOperationDirection;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.FaultType;
import com.serotonin.bacnet4j.type.enumerated.FileAccessMethod;
import com.serotonin.bacnet4j.type.enumerated.IPMode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyMode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
import com.serotonin.bacnet4j.type.enumerated.LiftCarDirection;
import com.serotonin.bacnet4j.type.enumerated.LiftCarDoorCommand;
import com.serotonin.bacnet4j.type.enumerated.LiftCarDriveStatus;
import com.serotonin.bacnet4j.type.enumerated.LiftCarMode;
import com.serotonin.bacnet4j.type.enumerated.LiftFault;
import com.serotonin.bacnet4j.type.enumerated.LiftGroupMode;
import com.serotonin.bacnet4j.type.enumerated.LightingInProgress;
import com.serotonin.bacnet4j.type.enumerated.LightingTransition;
import com.serotonin.bacnet4j.type.enumerated.LockStatus;
import com.serotonin.bacnet4j.type.enumerated.LoggingType;
import com.serotonin.bacnet4j.type.enumerated.Maintenance;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.NodeType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.Polarity;
import com.serotonin.bacnet4j.type.enumerated.ProgramError;
import com.serotonin.bacnet4j.type.enumerated.ProgramRequest;
import com.serotonin.bacnet4j.type.enumerated.ProgramState;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.Relationship;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.enumerated.RestartReason;
import com.serotonin.bacnet4j.type.enumerated.SecurityLevel;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.enumerated.ShedState;
import com.serotonin.bacnet4j.type.enumerated.SilencedState;
import com.serotonin.bacnet4j.type.enumerated.TimerState;
import com.serotonin.bacnet4j.type.enumerated.TimerTransition;
import com.serotonin.bacnet4j.type.enumerated.VtClass;
import com.serotonin.bacnet4j.type.enumerated.WriteStatus;
import com.serotonin.bacnet4j.type.eventParameter.EventParameter;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Double;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class ObjectProperties {
    private static final Map<PropertyIdentifier, PropertyTypeDefinition> propertyTypes = new HashMap<>();
    private static final Map<ObjectType, Map<PropertyIdentifier, ObjectPropertyTypeDefinition>> objectPropertyTypes =
            new HashMap<>();

    public static ObjectPropertyTypeDefinition getObjectPropertyTypeDefinition(final ObjectType objectType,
            final PropertyIdentifier propertyIdentifier) {
        final Map<PropertyIdentifier, ObjectPropertyTypeDefinition> props = objectPropertyTypes.get(objectType);
        if (props == null)
            return null;
        return props.get(propertyIdentifier);
    }

    public static ObjectPropertyTypeDefinition getObjectPropertyTypeDefinitionRequired(final ObjectType objectType,
            final PropertyIdentifier propertyIdentifier) throws BACnetServiceException {
        final ObjectPropertyTypeDefinition def = getObjectPropertyTypeDefinition(objectType, propertyIdentifier);
        if (def == null)
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty,
                    objectType + "/" + propertyIdentifier);
        return def;
    }

    public static List<ObjectPropertyTypeDefinition> getObjectPropertyTypeDefinitions(final ObjectType objectType) {
        return getObjectPropertyTypeDefinitions(objectType, 0);
    }

    public static List<ObjectPropertyTypeDefinition> getRequiredObjectPropertyTypeDefinitions(
            final ObjectType objectType) {
        return getObjectPropertyTypeDefinitions(objectType, 1);
    }

    public static List<ObjectPropertyTypeDefinition> getOptionalObjectPropertyTypeDefinitions(
            final ObjectType objectType) {
        return getObjectPropertyTypeDefinitions(objectType, 2);
    }

    public static PropertyTypeDefinition getPropertyTypeDefinition(final PropertyIdentifier pid) {
        return propertyTypes.get(pid);
    }

    public static boolean isCommandable(final ObjectType type, final PropertyIdentifier pid) {
        if (!pid.equals(PropertyIdentifier.presentValue))
            return false;
        return type.isOneOf( //
                ObjectType.analogOutput, //
                ObjectType.analogValue, //
                ObjectType.binaryOutput, //
                ObjectType.binaryValue, //
                ObjectType.multiStateOutput, //
                ObjectType.multiStateValue, //
                ObjectType.accessDoor, //
                ObjectType.characterstringValue, //
                ObjectType.datetimeValue, //
                ObjectType.largeAnalogValue, //
                ObjectType.bitstringValue, //
                ObjectType.octetstringValue, //
                ObjectType.timeValue, //
                ObjectType.integerValue, //
                ObjectType.positiveIntegerValue, //
                ObjectType.dateValue, //
                ObjectType.datetimePatternValue, //
                ObjectType.timePatternValue, //
                ObjectType.datePatternValue, //
                ObjectType.lightingOutput, //
                ObjectType.binaryLightingOutput);
    }

    /**
     * @param objectType
     * @param include    0 = all, 1 = required, 2 = optional
     * @return
     */
    private static List<ObjectPropertyTypeDefinition> getObjectPropertyTypeDefinitions(final ObjectType objectType,
            final int include) {
        final List<ObjectPropertyTypeDefinition> result = new ArrayList<>();
        final Map<PropertyIdentifier, ObjectPropertyTypeDefinition> props = objectPropertyTypes.get(objectType);
        if (props != null) {
            for (final ObjectPropertyTypeDefinition def : props.values()) {
                if (include == 0 || include == 1 && def.isRequired() || include == 2 && def.isOptional())
                    result.add(def);
            }
        }
        return result;
    }

    /**
     * Add a scalar property
     */
    private static void add(final ObjectType type, final PropertyIdentifier pid, final Class<? extends Encodable> clazz,
            final boolean required) {
        add(type, required, new PropertyTypeDefinition(pid, clazz));
    }

    /**
     * Add a list property
     */
    private static void add(final ObjectType type, final PropertyIdentifier pid, final Class<? extends Encodable> clazz,
            final boolean required, final boolean isList) {
        add(type, required, new PropertyTypeDefinition(pid, clazz, isList));
    }

    /**
     * Add an array property. If the array length is n, use 0.
     */
    private static void add(final ObjectType type, final PropertyIdentifier pid, final Class<? extends Encodable> clazz,
            final boolean required, final int arrayLength) {
        add(type, required, new PropertyTypeDefinition(pid, clazz, arrayLength));
    }

    private static void add(final ObjectType type, final boolean required, final PropertyTypeDefinition ptd) {
        // Add to the object property types
        Map<PropertyIdentifier, ObjectPropertyTypeDefinition> props = objectPropertyTypes.get(type);
        if (props == null) {
            props = new HashMap<>();
            objectPropertyTypes.put(type, props);
        }

        // Check for existing entries.
        final PropertyIdentifier pid = ptd.getPropertyIdentifier();
        if (props.containsKey(pid))
            throw new RuntimeException("Found an existing entry for " + type + "/" + pid);

        props.put(pid, new ObjectPropertyTypeDefinition(type, required, ptd));

        // Add to the property types. If an entry already exists, then replace it with null. In the end, only
        // properties that have a single type will remain.
        if (propertyTypes.containsKey(pid)) {
            final PropertyTypeDefinition existing = propertyTypes.get(pid);
            if (existing != null) {
                if (!existing.equals(ptd)) {
                    propertyTypes.put(pid, null);
                }
            }
        } else {
            propertyTypes.put(pid, ptd);
        }
    }

    static {
        // Access credential - 12.35
        add(ObjectType.accessCredential, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.accessCredential, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.accessCredential, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.accessCredential, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.globalIdentifier, Unsigned32.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.accessCredential, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.accessCredential, PropertyIdentifier.credentialStatus, BinaryPV.class, true);
        add(ObjectType.accessCredential, PropertyIdentifier.reasonForDisable, AccessCredentialDisableReason.class, true,
                true);
        add(ObjectType.accessCredential, PropertyIdentifier.authenticationFactors, CredentialAuthenticationFactor.class,
                true, 0);
        add(ObjectType.accessCredential, PropertyIdentifier.activationTime, DateTime.class, true);
        add(ObjectType.accessCredential, PropertyIdentifier.expirationTime, DateTime.class, true);
        add(ObjectType.accessCredential, PropertyIdentifier.credentialDisable, AccessCredentialDisable.class, true);
        add(ObjectType.accessCredential, PropertyIdentifier.daysRemaining, SignedInteger.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.usesRemaining, SignedInteger.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.absenteeLimit, UnsignedInteger.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.belongsTo, DeviceObjectReference.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.assignedAccessRights, AssignedAccessRights.class, true, 0);
        add(ObjectType.accessCredential, PropertyIdentifier.lastAccessPoint, DeviceObjectReference.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.lastAccessEvent, AccessEvent.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.lastUseTime, DateTime.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.traceFlag, Boolean.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.threatAuthority, AccessThreatLevel.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.extendedTimeEnable, Boolean.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.authorizationExemptions, AuthorizationExemption.class,
                false, true);
        add(ObjectType.accessCredential, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.accessCredential, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.accessCredential, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.accessCredential, PropertyIdentifier.profileName, CharacterString.class, false);

        // Access door - 12.26
        add(ObjectType.accessDoor, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.presentValue, DoorValue.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.eventState, EventState.class, true); // EventState.normal
        add(ObjectType.accessDoor, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.priorityArray, PriorityArray.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.relinquishDefault, DoorValue.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.doorStatus, DoorStatus.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.lockStatus, LockStatus.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.securedStatus, DoorSecuredStatus.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.doorMembers, DeviceObjectReference.class, false, 0);
        add(ObjectType.accessDoor, PropertyIdentifier.doorPulseTime, UnsignedInteger.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.doorExtendedPulseTime, UnsignedInteger.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.doorUnlockDelayTime, UnsignedInteger.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.doorOpenTooLongTime, UnsignedInteger.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.doorAlarmState, DoorAlarmState.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.maskedAlarmValues, DoorAlarmState.class, false, true);
        add(ObjectType.accessDoor, PropertyIdentifier.maintenanceRequired, Maintenance.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.alarmValues, DoorAlarmState.class, false, true);
        add(ObjectType.accessDoor, PropertyIdentifier.faultValues, DoorAlarmState.class, false, true);
        add(ObjectType.accessDoor, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.accessDoor, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.accessDoor, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.accessDoor, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.accessDoor, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.accessDoor, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.accessDoor, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.accessDoor, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.accessDoor, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.accessDoor, PropertyIdentifier.profileName, CharacterString.class, false);

        // Access point - 12.31
        add(ObjectType.accessPoint, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.authenticationStatus, AuthenticationStatus.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.activeAuthenticationPolicy, UnsignedInteger.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.numberOfAuthenticationPolicies, UnsignedInteger.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.authenticationPolicyList, AuthenticationPolicy.class, false, 0);
        add(ObjectType.accessPoint, PropertyIdentifier.authenticationPolicyNames, CharacterString.class, false, 0);
        add(ObjectType.accessPoint, PropertyIdentifier.authorizationMode, AuthorizationMode.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.verificationTime, UnsignedInteger.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.lockout, Boolean.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.lockoutRelinquishTime, UnsignedInteger.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.failedAttempts, UnsignedInteger.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.failedAttemptEvents, AccessEvent.class, false, true);
        add(ObjectType.accessPoint, PropertyIdentifier.maxFailedAttempts, UnsignedInteger.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.failedAttemptsTime, UnsignedInteger.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.threatLevel, AccessThreatLevel.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.occupancyUpperLimitEnforced, Boolean.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.occupancyLowerLimitEnforced, Boolean.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.occupancyCountAdjust, Boolean.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.accompanimentTime, UnsignedInteger.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.accessEvent, AccessEvent.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.accessEventTag, UnsignedInteger.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.accessEventTime, TimeStamp.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.accessEventCredential, DeviceObjectReference.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.accessEventAuthenticationFactor, AuthenticationFactor.class,
                false);
        add(ObjectType.accessPoint, PropertyIdentifier.accessDoors, DeviceObjectReference.class, true, 0);
        add(ObjectType.accessPoint, PropertyIdentifier.priorityForWriting, UnsignedInteger.class, true);
        add(ObjectType.accessPoint, PropertyIdentifier.musterPoint, Boolean.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.zoneTo, DeviceObjectReference.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.zoneFrom, DeviceObjectReference.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.transactionNotificationClass, UnsignedInteger.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.accessAlarmEvents, AccessEvent.class, false, true);
        add(ObjectType.accessPoint, PropertyIdentifier.accessTransactionEvents, AccessEvent.class, false, true);
        add(ObjectType.accessPoint, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.accessPoint, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.accessPoint, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.accessPoint, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.accessPoint, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.accessPoint, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.accessPoint, PropertyIdentifier.profileName, CharacterString.class, false);

        // Access rights - 12.34
        add(ObjectType.accessRights, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.accessRights, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.accessRights, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.accessRights, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.accessRights, PropertyIdentifier.globalIdentifier, Unsigned32.class, false);
        add(ObjectType.accessRights, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.accessRights, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.accessRights, PropertyIdentifier.enable, Boolean.class, true);
        add(ObjectType.accessRights, PropertyIdentifier.negativeAccessRules, AccessRule.class, true, 0);
        add(ObjectType.accessRights, PropertyIdentifier.positiveAccessRules, AccessRule.class, true, 0);
        add(ObjectType.accessRights, PropertyIdentifier.accompaniment, DeviceObjectReference.class, false);
        add(ObjectType.accessRights, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.accessRights, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.accessRights, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.accessRights, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.accessRights, PropertyIdentifier.profileName, CharacterString.class, false);

        // Access user - 12.33
        add(ObjectType.accessUser, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.accessUser, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.accessUser, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.accessUser, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.accessUser, PropertyIdentifier.globalIdentifier, Unsigned32.class, false);
        add(ObjectType.accessUser, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.accessUser, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.accessUser, PropertyIdentifier.userType, AccessUserType.class, true);
        add(ObjectType.accessUser, PropertyIdentifier.userName, CharacterString.class, false);
        add(ObjectType.accessUser, PropertyIdentifier.userExternalIdentifier, CharacterString.class, false);
        add(ObjectType.accessUser, PropertyIdentifier.userInformationReference, CharacterString.class, false);
        add(ObjectType.accessUser, PropertyIdentifier.members, DeviceObjectReference.class, false, true);
        add(ObjectType.accessUser, PropertyIdentifier.memberOf, DeviceObjectReference.class, false, true);
        add(ObjectType.accessUser, PropertyIdentifier.credentials, DeviceObjectReference.class, true, true);
        add(ObjectType.accessUser, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.accessUser, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.accessUser, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.accessUser, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.accessUser, PropertyIdentifier.profileName, CharacterString.class, false);

        // Access zone - 12.32
        add(ObjectType.accessZone, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.accessZone, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.accessZone, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.accessZone, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.globalIdentifier, Unsigned32.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.occupancyState, AccessZoneOccupancyState.class, true);
        add(ObjectType.accessZone, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.accessZone, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.accessZone, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.accessZone, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.accessZone, PropertyIdentifier.occupancyCount, UnsignedInteger.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.occupancyCountEnable, Boolean.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.adjustValue, SignedInteger.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.occupancyUpperLimit, UnsignedInteger.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.occupancyLowerLimit, UnsignedInteger.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.credentialsInZone, DeviceObjectReference.class, false, true);
        add(ObjectType.accessZone, PropertyIdentifier.lastCredentialAdded, DeviceObjectReference.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.lastCredentialAddedTime, DateTime.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.lastCredentialRemoved, DeviceObjectReference.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.lastCredentialRemovedTime, DateTime.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.passbackMode, AccessPassbackMode.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.passbackTimeout, UnsignedInteger.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.entryPoints, DeviceObjectReference.class, true, true);
        add(ObjectType.accessZone, PropertyIdentifier.exitPoints, DeviceObjectReference.class, true, true);
        add(ObjectType.accessZone, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.alarmValues, AccessZoneOccupancyState.class, false, true);
        add(ObjectType.accessZone, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.accessZone, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.accessZone, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.accessZone, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.accessZone, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.accessZone, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.accessZone, PropertyIdentifier.profileName, CharacterString.class, false);

        // Accumulator - 12.61
        add(ObjectType.accumulator, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.presentValue, UnsignedInteger.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.deviceType, CharacterString.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.scale, Scale.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.units, EngineeringUnits.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.prescale, Prescale.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.maxPresValue, UnsignedInteger.class, true);
        add(ObjectType.accumulator, PropertyIdentifier.valueChangeTime, DateTime.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.valueBeforeChange, UnsignedInteger.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.valueSet, UnsignedInteger.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.loggingRecord, AccumulatorRecord.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.loggingObject, ObjectIdentifier.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.pulseRate, UnsignedInteger.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.highLimit, UnsignedInteger.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.lowLimit, UnsignedInteger.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.limitMonitoringInterval, UnsignedInteger.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.limitEnable, LimitEnable.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.accumulator, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.accumulator, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.accumulator, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.accumulator, PropertyIdentifier.faultHighLimit, Real.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.faultLowLimit, Real.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.accumulator, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.accumulator, PropertyIdentifier.profileName, CharacterString.class, false);

        // Alert enrollment - 12.52
        add(ObjectType.alertEnrollment, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.alertEnrollment, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.alertEnrollment, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.alertEnrollment, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.alertEnrollment, PropertyIdentifier.presentValue, ObjectIdentifier.class, true);
        add(ObjectType.alertEnrollment, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.alertEnrollment, PropertyIdentifier.eventDetectionEnable, Boolean.class, true);
        add(ObjectType.alertEnrollment, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.alertEnrollment, PropertyIdentifier.eventEnable, EventTransitionBits.class, true);
        add(ObjectType.alertEnrollment, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, true);
        add(ObjectType.alertEnrollment, PropertyIdentifier.notifyType, NotifyType.class, true);
        add(ObjectType.alertEnrollment, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.alertEnrollment, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.alertEnrollment, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.alertEnrollment, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.alertEnrollment, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.alertEnrollment, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.alertEnrollment, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.alertEnrollment, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.alertEnrollment, PropertyIdentifier.profileName, CharacterString.class, false);

        // Analog input - 12.2
        add(ObjectType.analogInput, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.analogInput, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.analogInput, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.analogInput, PropertyIdentifier.presentValue, Real.class, true);
        add(ObjectType.analogInput, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.deviceType, CharacterString.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.analogInput, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.analogInput, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.analogInput, PropertyIdentifier.updateInterval, UnsignedInteger.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.units, EngineeringUnits.class, true);
        add(ObjectType.analogInput, PropertyIdentifier.minPresValue, Real.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.maxPresValue, Real.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.resolution, Real.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.covIncrement, Real.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.highLimit, Real.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.lowLimit, Real.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.deadband, Real.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.limitEnable, LimitEnable.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.analogInput, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.analogInput, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.analogInput, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.analogInput, PropertyIdentifier.interfaceValue, OptionalReal.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.faultHighLimit, Real.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.faultLowLimit, Real.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.analogInput, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.analogInput, PropertyIdentifier.profileName, CharacterString.class, false);

        // Analog output - 12.3
        add(ObjectType.analogOutput, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.presentValue, Real.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.deviceType, CharacterString.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.units, EngineeringUnits.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.minPresValue, Real.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.maxPresValue, Real.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.resolution, Real.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.priorityArray, PriorityArray.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.relinquishDefault, Real.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.covIncrement, Real.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.highLimit, Real.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.lowLimit, Real.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.deadband, Real.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.limitEnable, LimitEnable.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.analogOutput, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.analogOutput, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.analogOutput, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.analogOutput, PropertyIdentifier.interfaceValue, OptionalReal.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.analogOutput, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.analogOutput, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.analogOutput, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.analogOutput, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.analogOutput, PropertyIdentifier.profileName, CharacterString.class, false);

        // Analog value - 12.4
        add(ObjectType.analogValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.analogValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.analogValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.analogValue, PropertyIdentifier.presentValue, Real.class, true);
        add(ObjectType.analogValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.analogValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.analogValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.analogValue, PropertyIdentifier.units, EngineeringUnits.class, true);
        add(ObjectType.analogValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.relinquishDefault, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.covIncrement, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.highLimit, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.lowLimit, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.deadband, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.limitEnable, LimitEnable.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.analogValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.analogValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.analogValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.minPresValue, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.maxPresValue, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.resolution, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.analogValue, PropertyIdentifier.faultHighLimit, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.faultLowLimit, Real.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.analogValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.analogValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.analogValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.analogValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.analogValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Averaging - 12.5
        add(ObjectType.averaging, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.averaging, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.averaging, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.averaging, PropertyIdentifier.minimumValue, Real.class, true);
        add(ObjectType.averaging, PropertyIdentifier.minimumValueTimestamp, DateTime.class, false);
        add(ObjectType.averaging, PropertyIdentifier.averageValue, Real.class, true);
        add(ObjectType.averaging, PropertyIdentifier.varianceValue, Real.class, false);
        add(ObjectType.averaging, PropertyIdentifier.maximumValue, Real.class, true);
        add(ObjectType.averaging, PropertyIdentifier.maximumValueTimestamp, DateTime.class, false);
        add(ObjectType.averaging, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.averaging, PropertyIdentifier.attemptedSamples, UnsignedInteger.class, true);
        add(ObjectType.averaging, PropertyIdentifier.validSamples, UnsignedInteger.class, true);
        add(ObjectType.averaging, PropertyIdentifier.objectPropertyReference, DeviceObjectPropertyReference.class,
                true);
        add(ObjectType.averaging, PropertyIdentifier.windowInterval, UnsignedInteger.class, true);
        add(ObjectType.averaging, PropertyIdentifier.windowSamples, UnsignedInteger.class, true);
        add(ObjectType.averaging, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.averaging, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.averaging, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.averaging, PropertyIdentifier.profileName, CharacterString.class, false);

        // Binary input - 12.6
        add(ObjectType.binaryInput, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.binaryInput, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.binaryInput, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.binaryInput, PropertyIdentifier.presentValue, BinaryPV.class, true);
        add(ObjectType.binaryInput, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.deviceType, CharacterString.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.binaryInput, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.binaryInput, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.binaryInput, PropertyIdentifier.polarity, Polarity.class, true);
        add(ObjectType.binaryInput, PropertyIdentifier.inactiveText, CharacterString.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.activeText, CharacterString.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.changeOfStateTime, DateTime.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.changeOfStateCount, UnsignedInteger.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.timeOfStateCountReset, DateTime.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.elapsedActiveTime, UnsignedInteger.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.timeOfActiveTimeReset, DateTime.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.alarmValue, BinaryPV.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.binaryInput, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.binaryInput, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.binaryInput, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.binaryInput, PropertyIdentifier.interfaceValue, OptionalBinaryPV.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.binaryInput, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.binaryInput, PropertyIdentifier.profileName, CharacterString.class, false);

        // Binary lighting output - 12.55
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.presentValue, BinaryLightingPV.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.blinkWarnEnable, Boolean.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.egressTime, UnsignedInteger.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.egressActive, Boolean.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.feedbackValue, BinaryLightingPV.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.priorityArray, PriorityArray.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.relinquishDefault, Real.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.power, Real.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.polarity, Polarity.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.elapsedActiveTime, UnsignedInteger.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.timeOfActiveTimeReset, DateTime.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.strikeCount, UnsignedInteger.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.timeOfStrikeCountReset, DateTime.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false,
                3);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.binaryLightingOutput, PropertyIdentifier.profileName, CharacterString.class, false);

        // Binary output - 12.7
        add(ObjectType.binaryOutput, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.presentValue, BinaryPV.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.deviceType, CharacterString.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.polarity, Polarity.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.inactiveText, CharacterString.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.activeText, CharacterString.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.changeOfStateTime, DateTime.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.changeOfStateCount, UnsignedInteger.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.timeOfStateCountReset, DateTime.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.elapsedActiveTime, UnsignedInteger.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.timeOfActiveTimeReset, DateTime.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.minimumOffTime, UnsignedInteger.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.minimumOnTime, UnsignedInteger.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.priorityArray, PriorityArray.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.relinquishDefault, BinaryPV.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.feedbackValue, BinaryPV.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.binaryOutput, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.binaryOutput, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.binaryOutput, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.binaryOutput, PropertyIdentifier.interfaceValue, OptionalBinaryPV.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.binaryOutput, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.binaryOutput, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.binaryOutput, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.binaryOutput, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.binaryOutput, PropertyIdentifier.profileName, CharacterString.class, false);

        // Binary value - 12.8
        add(ObjectType.binaryValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.binaryValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.binaryValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.binaryValue, PropertyIdentifier.presentValue, BinaryPV.class, true);
        add(ObjectType.binaryValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.binaryValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.binaryValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.inactiveText, CharacterString.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.activeText, CharacterString.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.changeOfStateTime, DateTime.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.changeOfStateCount, UnsignedInteger.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.timeOfStateCountReset, DateTime.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.elapsedActiveTime, UnsignedInteger.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.timeOfActiveTimeReset, DateTime.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.minimumOffTime, UnsignedInteger.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.minimumOnTime, UnsignedInteger.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.relinquishDefault, BinaryPV.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.alarmValue, BinaryPV.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.binaryValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.binaryValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.binaryValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.binaryValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.binaryValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.binaryValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.binaryValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.binaryValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.binaryValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // BitString value - 12.40
        add(ObjectType.bitstringValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.bitstringValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.bitstringValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.bitstringValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.presentValue, BitString.class, true);
        add(ObjectType.bitstringValue, PropertyIdentifier.bitText, CharacterString.class, false, 0);
        add(ObjectType.bitstringValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.bitstringValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.bitstringValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.bitstringValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.relinquishDefault, BitString.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.alarmValues, BitString.class, false, 0);
        add(ObjectType.bitstringValue, PropertyIdentifier.bitMask, BitString.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.bitstringValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.bitstringValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.bitstringValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.bitstringValue, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.bitstringValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.bitstringValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.bitstringValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.bitstringValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.bitstringValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.bitstringValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Calendar - 12.9
        add(ObjectType.calendar, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.calendar, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.calendar, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.calendar, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.calendar, PropertyIdentifier.presentValue, Boolean.class, true);
        add(ObjectType.calendar, PropertyIdentifier.dateList, CalendarEntry.class, true, true);
        add(ObjectType.calendar, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.calendar, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.calendar, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.calendar, PropertyIdentifier.profileName, CharacterString.class, false);

        // Channel - 12.53
        add(ObjectType.channel, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.channel, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.channel, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.channel, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.channel, PropertyIdentifier.presentValue, ChannelValue.class, true);
        add(ObjectType.channel, PropertyIdentifier.lastPriority, UnsignedInteger.class, true);
        add(ObjectType.channel, PropertyIdentifier.writeStatus, WriteStatus.class, true);
        add(ObjectType.channel, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.channel, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.channel, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.channel, PropertyIdentifier.listOfObjectPropertyReferences, DeviceObjectPropertyReference.class,
                true, 0);
        add(ObjectType.channel, PropertyIdentifier.executionDelay, UnsignedInteger.class, false, 0);
        add(ObjectType.channel, PropertyIdentifier.allowGroupDelayInhibit, Boolean.class, false);
        add(ObjectType.channel, PropertyIdentifier.channelNumber, Unsigned16.class, true);
        add(ObjectType.channel, PropertyIdentifier.controlGroups, Unsigned32.class, true, 0);
        add(ObjectType.channel, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.channel, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.channel, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.channel, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.channel, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.channel, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.channel, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.channel, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.channel, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.channel, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.channel, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.channel, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.channel, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.channel, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.channel, PropertyIdentifier.profileName, CharacterString.class, false);

        // CharacterString value - 12.37
        add(ObjectType.characterstringValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.characterstringValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.characterstringValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.characterstringValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.presentValue, CharacterString.class, true);
        add(ObjectType.characterstringValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.characterstringValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.characterstringValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.relinquishDefault, CharacterString.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.alarmValues, OptionalCharacterString.class, false, 0);
        add(ObjectType.characterstringValue, PropertyIdentifier.faultValues, OptionalCharacterString.class, false, 0);
        add(ObjectType.characterstringValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.characterstringValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.characterstringValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false,
                3);
        add(ObjectType.characterstringValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.characterstringValue, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.characterstringValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.characterstringValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.characterstringValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.characterstringValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.characterstringValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.characterstringValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Command - 12.10
        add(ObjectType.command, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.command, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.command, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.command, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.command, PropertyIdentifier.presentValue, UnsignedInteger.class, true);
        add(ObjectType.command, PropertyIdentifier.inProcess, Boolean.class, true);
        add(ObjectType.command, PropertyIdentifier.allWritesSuccessful, Boolean.class, true);
        add(ObjectType.command, PropertyIdentifier.action, ActionList.class, true, 0);
        add(ObjectType.command, PropertyIdentifier.actionText, CharacterString.class, false, 0);
        add(ObjectType.command, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.command, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.command, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.command, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.command, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.command, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.command, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.command, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.command, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.command, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.command, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.command, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.command, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.command, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.command, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.command, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.command, PropertyIdentifier.profileName, CharacterString.class, false);

        // Credential Data Input - 12.36
        add(ObjectType.credentialDataInput, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.credentialDataInput, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.credentialDataInput, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.credentialDataInput, PropertyIdentifier.presentValue, AuthenticationFactor.class, true);
        add(ObjectType.credentialDataInput, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.credentialDataInput, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.credentialDataInput, PropertyIdentifier.supportedFormats, AuthenticationFactorFormat.class, true,
                0);
        add(ObjectType.credentialDataInput, PropertyIdentifier.supportedFormatClasses, UnsignedInteger.class, false, 0);
        add(ObjectType.credentialDataInput, PropertyIdentifier.updateTime, TimeStamp.class, true);
        add(ObjectType.credentialDataInput, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.eventState, EventState.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.credentialDataInput, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.credentialDataInput, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false,
                3);
        add(ObjectType.credentialDataInput, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.credentialDataInput, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.credentialDataInput, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.credentialDataInput, PropertyIdentifier.profileName, CharacterString.class, false);

        // Date value - 12.45
        add(ObjectType.dateValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.dateValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.dateValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.dateValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.presentValue, Date.class, true);
        add(ObjectType.dateValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.dateValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.dateValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.relinquishDefault, Date.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.dateValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.dateValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.dateValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.dateValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.dateValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.dateValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.dateValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.dateValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.dateValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Date pattern value - 12.48
        add(ObjectType.datePatternValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.datePatternValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.datePatternValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.datePatternValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.presentValue, Date.class, true);
        add(ObjectType.datePatternValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.datePatternValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.datePatternValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.relinquishDefault, Date.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.datePatternValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.datePatternValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.datePatternValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.datePatternValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.datePatternValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.datePatternValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.datePatternValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.datePatternValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.datePatternValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // DateTime value - 12.38
        add(ObjectType.datetimeValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.datetimeValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.datetimeValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.datetimeValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.presentValue, DateTime.class, true);
        add(ObjectType.datetimeValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.datetimeValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.datetimeValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.relinquishDefault, DateTime.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.isUtc, Boolean.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.datetimeValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.datetimeValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.datetimeValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.datetimeValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.datetimeValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.datetimeValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.datetimeValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.datetimeValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.datetimeValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // DateTime pattern value - 12.46
        add(ObjectType.datetimePatternValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.presentValue, DateTime.class, true);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.isUtc, Boolean.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.relinquishDefault, DateTime.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false,
                3);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.datetimePatternValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Device - 12.11
        add(ObjectType.device, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.device, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.device, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.device, PropertyIdentifier.systemStatus, DeviceStatus.class, true);
        add(ObjectType.device, PropertyIdentifier.vendorName, CharacterString.class, true);
        add(ObjectType.device, PropertyIdentifier.vendorIdentifier, Unsigned16.class, true);
        add(ObjectType.device, PropertyIdentifier.modelName, CharacterString.class, true);
        add(ObjectType.device, PropertyIdentifier.firmwareRevision, CharacterString.class, true);
        add(ObjectType.device, PropertyIdentifier.applicationSoftwareVersion, CharacterString.class, true);
        add(ObjectType.device, PropertyIdentifier.location, CharacterString.class, false);
        add(ObjectType.device, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.device, PropertyIdentifier.protocolVersion, UnsignedInteger.class, true);
        add(ObjectType.device, PropertyIdentifier.protocolRevision, UnsignedInteger.class, true);
        add(ObjectType.device, PropertyIdentifier.protocolServicesSupported, ServicesSupported.class, true);
        add(ObjectType.device, PropertyIdentifier.protocolObjectTypesSupported, ObjectTypesSupported.class, true);
        add(ObjectType.device, PropertyIdentifier.objectList, ObjectIdentifier.class, true, 0);
        add(ObjectType.device, PropertyIdentifier.structuredObjectList, ObjectIdentifier.class, false, 0);
        add(ObjectType.device, PropertyIdentifier.maxApduLengthAccepted, UnsignedInteger.class, true);
        add(ObjectType.device, PropertyIdentifier.segmentationSupported, Segmentation.class, true);
        add(ObjectType.device, PropertyIdentifier.maxSegmentsAccepted, UnsignedInteger.class, false);
        add(ObjectType.device, PropertyIdentifier.vtClassesSupported, VtClass.class, false, true);
        add(ObjectType.device, PropertyIdentifier.activeVtSessions, VtSession.class, false, true);
        add(ObjectType.device, PropertyIdentifier.localTime, Time.class, false);
        add(ObjectType.device, PropertyIdentifier.localDate, Date.class, false);
        add(ObjectType.device, PropertyIdentifier.utcOffset, SignedInteger.class, false);
        add(ObjectType.device, PropertyIdentifier.daylightSavingsStatus, Boolean.class, false);
        add(ObjectType.device, PropertyIdentifier.apduSegmentTimeout, UnsignedInteger.class, false);
        add(ObjectType.device, PropertyIdentifier.apduTimeout, UnsignedInteger.class, true);
        add(ObjectType.device, PropertyIdentifier.numberOfApduRetries, UnsignedInteger.class, true);
        add(ObjectType.device, PropertyIdentifier.timeSynchronizationRecipients, Recipient.class, false, true);
        add(ObjectType.device, PropertyIdentifier.maxMaster, UnsignedInteger.class, false);
        add(ObjectType.device, PropertyIdentifier.maxInfoFrames, UnsignedInteger.class, false);
        add(ObjectType.device, PropertyIdentifier.deviceAddressBinding, AddressBinding.class, true, true);
        add(ObjectType.device, PropertyIdentifier.databaseRevision, UnsignedInteger.class, true);
        add(ObjectType.device, PropertyIdentifier.configurationFiles, ObjectIdentifier.class, false, 0);
        add(ObjectType.device, PropertyIdentifier.lastRestoreTime, TimeStamp.class, false);
        add(ObjectType.device, PropertyIdentifier.backupFailureTimeout, Unsigned16.class, false);
        add(ObjectType.device, PropertyIdentifier.backupPreparationTime, Unsigned16.class, false);
        add(ObjectType.device, PropertyIdentifier.restorePreparationTime, Unsigned16.class, false);
        add(ObjectType.device, PropertyIdentifier.restoreCompletionTime, Unsigned16.class, false);
        add(ObjectType.device, PropertyIdentifier.backupAndRestoreState, BackupState.class, false);
        add(ObjectType.device, PropertyIdentifier.activeCovSubscriptions, CovSubscription.class, false, true);
        add(ObjectType.device, PropertyIdentifier.lastRestartReason, RestartReason.class, false);
        add(ObjectType.device, PropertyIdentifier.timeOfDeviceRestart, TimeStamp.class, false);
        add(ObjectType.device, PropertyIdentifier.restartNotificationRecipients, Recipient.class, false, true);
        add(ObjectType.device, PropertyIdentifier.utcTimeSynchronizationRecipients, Recipient.class, false, true);
        add(ObjectType.device, PropertyIdentifier.timeSynchronizationInterval, UnsignedInteger.class, false);
        add(ObjectType.device, PropertyIdentifier.alignIntervals, Boolean.class, false);
        add(ObjectType.device, PropertyIdentifier.intervalOffset, UnsignedInteger.class, false);
        add(ObjectType.device, PropertyIdentifier.serialNumber, CharacterString.class, false);
        add(ObjectType.device, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.device, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.device, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.device, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.device, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.device, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.device, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.device, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.device, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.device, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.device, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.device, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.device, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.device, PropertyIdentifier.activeCovMultipleSubscriptions, CovMultipleSubscription.class, false,
                true);
        add(ObjectType.device, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.device, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.device, PropertyIdentifier.deployedProfileLocation, CharacterString.class, false);
        add(ObjectType.device, PropertyIdentifier.profileName, CharacterString.class, false);
        add(ObjectType.device, PropertyIdentifier.autoSlaveDiscovery, Boolean.class, false, 0);
        add(ObjectType.device, PropertyIdentifier.slaveProxyEnable, Boolean.class, false, 0);

        // Elevator group - 12.58
        add(ObjectType.elevatorGroup, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.elevatorGroup, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.elevatorGroup, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.elevatorGroup, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.elevatorGroup, PropertyIdentifier.machineRoomId, ObjectIdentifier.class, true);
        add(ObjectType.elevatorGroup, PropertyIdentifier.groupId, Unsigned8.class, true);
        add(ObjectType.elevatorGroup, PropertyIdentifier.groupMembers, ObjectIdentifier.class, true, 0);
        add(ObjectType.elevatorGroup, PropertyIdentifier.groupMode, LiftGroupMode.class, false);
        add(ObjectType.elevatorGroup, PropertyIdentifier.landingCalls, LandingCallStatus.class, false, true);
        add(ObjectType.elevatorGroup, PropertyIdentifier.landingCallControl, LandingCallStatus.class, false);
        add(ObjectType.elevatorGroup, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.elevatorGroup, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.elevatorGroup, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.elevatorGroup, PropertyIdentifier.profileName, CharacterString.class, false);

        // Escalator - 12.60
        add(ObjectType.escalator, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.escalator, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.escalator, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.escalator, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.escalator, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.escalator, PropertyIdentifier.elevatorGroup, ObjectIdentifier.class, true);
        add(ObjectType.escalator, PropertyIdentifier.groupId, Unsigned8.class, true);
        add(ObjectType.escalator, PropertyIdentifier.installationId, Unsigned8.class, true);
        add(ObjectType.escalator, PropertyIdentifier.powerMode, Boolean.class, false);
        add(ObjectType.escalator, PropertyIdentifier.operationDirection, EscalatorOperationDirection.class, true);
        add(ObjectType.escalator, PropertyIdentifier.escalatorMode, EscalatorMode.class, false);
        add(ObjectType.escalator, PropertyIdentifier.energyMeter, Real.class, false);
        add(ObjectType.escalator, PropertyIdentifier.energyMeterRef, DeviceObjectReference.class, false);
        add(ObjectType.escalator, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.escalator, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.escalator, PropertyIdentifier.faultSignals, EscalatorFault.class, false, true);
        add(ObjectType.escalator, PropertyIdentifier.passengerAlarm, Boolean.class, true);
        add(ObjectType.escalator, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.escalator, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.escalator, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.escalator, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.escalator, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.escalator, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.escalator, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.escalator, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.escalator, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.escalator, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.escalator, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.escalator, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.escalator, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.escalator, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.escalator, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.escalator, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.escalator, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.escalator, PropertyIdentifier.profileName, CharacterString.class, false);

        // Event enrollment - 12.12
        add(ObjectType.eventEnrollment, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventType, EventType.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.notifyType, NotifyType.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventParameters, EventParameter.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.objectPropertyReference, DeviceObjectPropertyReference.class,
                true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventEnable, EventTransitionBits.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.eventEnrollment, PropertyIdentifier.faultType, FaultType.class, false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.faultParameters, FaultParameter.class, false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.eventEnrollment, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.eventEnrollment, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.eventEnrollment, PropertyIdentifier.profileName, CharacterString.class, false);

        // Event log - 12.27
        add(ObjectType.eventLog, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.eventLog, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.enable, Boolean.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.startTime, DateTime.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.stopTime, DateTime.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.stopWhenFull, Boolean.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.bufferSize, UnsignedInteger.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.logBuffer, EventLogRecord.class, true, true);
        add(ObjectType.eventLog, PropertyIdentifier.recordCount, UnsignedInteger.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.totalRecordCount, UnsignedInteger.class, true);
        add(ObjectType.eventLog, PropertyIdentifier.notificationThreshold, UnsignedInteger.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.recordsSinceNotification, UnsignedInteger.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.lastNotifyRecord, UnsignedInteger.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.eventLog, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.eventLog, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.eventLog, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.eventLog, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.eventLog, PropertyIdentifier.profileName, CharacterString.class, false);

        // File - 12.13
        add(ObjectType.file, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.file, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.file, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.file, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.file, PropertyIdentifier.fileType, CharacterString.class, true);
        add(ObjectType.file, PropertyIdentifier.fileSize, UnsignedInteger.class, true);
        add(ObjectType.file, PropertyIdentifier.modificationDate, DateTime.class, true);
        add(ObjectType.file, PropertyIdentifier.archive, Boolean.class, true);
        add(ObjectType.file, PropertyIdentifier.readOnly, Boolean.class, true);
        add(ObjectType.file, PropertyIdentifier.fileAccessMethod, FileAccessMethod.class, true);
        add(ObjectType.file, PropertyIdentifier.recordCount, UnsignedInteger.class, false);
        add(ObjectType.file, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.file, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.file, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.file, PropertyIdentifier.profileName, CharacterString.class, false);

        // Group - 12.14
        add(ObjectType.group, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.group, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.group, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.group, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.group, PropertyIdentifier.listOfGroupMembers, ReadAccessSpecification.class, true, true);
        add(ObjectType.group, PropertyIdentifier.presentValue, ReadAccessResult.class, true, true);
        add(ObjectType.group, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.group, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.group, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.group, PropertyIdentifier.profileName, CharacterString.class, false);

        // Global group - 12.50
        add(ObjectType.globalGroup, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.globalGroup, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.globalGroup, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.globalGroup, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.groupMembers, DeviceObjectPropertyReference.class, true, 0);
        add(ObjectType.globalGroup, PropertyIdentifier.groupMemberNames, CharacterString.class, false, 0);
        add(ObjectType.globalGroup, PropertyIdentifier.presentValue, PropertyAccessResult.class, true, 0);
        add(ObjectType.globalGroup, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.globalGroup, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.globalGroup, PropertyIdentifier.memberStatusFlags, StatusFlags.class, true);
        add(ObjectType.globalGroup, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.globalGroup, PropertyIdentifier.updateInterval, UnsignedInteger.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.requestedUpdateInterval, UnsignedInteger.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.covResubscriptionInterval, UnsignedInteger.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.clientCovIncrement, ClientCov.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.globalGroup, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.globalGroup, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.globalGroup, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.covuPeriod, UnsignedInteger.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.covuRecipients, Recipient.class, false, true);
        add(ObjectType.globalGroup, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.globalGroup, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.globalGroup, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.globalGroup, PropertyIdentifier.profileName, CharacterString.class, false);

        // Integer value - 12.43
        add(ObjectType.integerValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.integerValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.integerValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.integerValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.presentValue, SignedInteger.class, true);
        add(ObjectType.integerValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.integerValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.integerValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.units, EngineeringUnits.class, true);
        add(ObjectType.integerValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.relinquishDefault, SignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.covIncrement, UnsignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.highLimit, SignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.lowLimit, SignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.deadband, UnsignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.limitEnable, LimitEnable.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.integerValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.integerValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.integerValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.minPresValue, SignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.maxPresValue, SignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.resolution, SignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.integerValue, PropertyIdentifier.faultHighLimit, SignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.faultLowLimit, SignedInteger.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.integerValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.integerValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.integerValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.integerValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Large analog value - 12.39
        add(ObjectType.largeAnalogValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.presentValue, Double.class, true);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.units, EngineeringUnits.class, true);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.relinquishDefault, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.covIncrement, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.highLimit, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.lowLimit, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.deadband, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.limitEnable, LimitEnable.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.minPresValue, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.maxPresValue, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.resolution, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);

        add(ObjectType.largeAnalogValue, PropertyIdentifier.faultHighLimit, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.faultLowLimit, Double.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.largeAnalogValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Life safety point - 12.15
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.presentValue, LifeSafetyState.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.trackingValue, LifeSafetyState.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.deviceType, CharacterString.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.mode, LifeSafetyMode.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.acceptedModes, LifeSafetyMode.class, true, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.lifeSafetyAlarmValues, LifeSafetyState.class, false, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.alarmValues, LifeSafetyState.class, false, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.faultValues, LifeSafetyState.class, false, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.silenced, SilencedState.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.operationExpected, LifeSafetyOperation.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.maintenanceRequired, Maintenance.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.setting, UnsignedInteger.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.directReading, Real.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.units, EngineeringUnits.class, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.memberOf, DeviceObjectReference.class, false, true);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.lifeSafetyPoint, PropertyIdentifier.profileName, CharacterString.class, false);

        // Life safety zone - 12.16
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.presentValue, LifeSafetyState.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.trackingValue, LifeSafetyState.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.deviceType, CharacterString.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.mode, LifeSafetyMode.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.acceptedModes, LifeSafetyMode.class, true, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.lifeSafetyAlarmValues, LifeSafetyState.class, false, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.alarmValues, LifeSafetyState.class, false, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.faultValues, LifeSafetyState.class, false, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.silenced, SilencedState.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.operationExpected, LifeSafetyOperation.class, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.maintenanceRequired, Maintenance.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.zoneMembers, DeviceObjectReference.class, true, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.memberOf, DeviceObjectReference.class, false, true);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.lifeSafetyZone, PropertyIdentifier.profileName, CharacterString.class, false);

        // Lift - 12.59
        add(ObjectType.lift, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.lift, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.lift, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.lift, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.lift, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.lift, PropertyIdentifier.elevatorGroup, ObjectIdentifier.class, true);
        add(ObjectType.lift, PropertyIdentifier.groupId, Unsigned8.class, true);
        add(ObjectType.lift, PropertyIdentifier.installationId, Unsigned8.class, true);
        add(ObjectType.lift, PropertyIdentifier.floorText, CharacterString.class, false, 0);
        add(ObjectType.lift, PropertyIdentifier.carDoorText, CharacterString.class, false, 0);
        add(ObjectType.lift, PropertyIdentifier.assignedLandingCalls, AssignedLandingCalls.class, false, 0);
        add(ObjectType.lift, PropertyIdentifier.makingCarCall, Unsigned8.class, false, 0);
        add(ObjectType.lift, PropertyIdentifier.registeredCarCall, LiftCarCallList.class, false, 0);
        add(ObjectType.lift, PropertyIdentifier.carPosition, Unsigned8.class, true);
        add(ObjectType.lift, PropertyIdentifier.carMovingDirection, LiftCarDirection.class, true);
        add(ObjectType.lift, PropertyIdentifier.carAssignedDirection, LiftCarDirection.class, false);
        add(ObjectType.lift, PropertyIdentifier.carDoorStatus, DoorStatus.class, true, 0);
        add(ObjectType.lift, PropertyIdentifier.carDoorCommand, LiftCarDoorCommand.class, false, 0);
        add(ObjectType.lift, PropertyIdentifier.carDoorZone, Boolean.class, false);
        add(ObjectType.lift, PropertyIdentifier.carMode, LiftCarMode.class, false);
        add(ObjectType.lift, PropertyIdentifier.carLoad, Real.class, false);
        add(ObjectType.lift, PropertyIdentifier.carLoadUnits, EngineeringUnits.class, false);
        add(ObjectType.lift, PropertyIdentifier.nextStoppingFloor, Unsigned8.class, false);
        add(ObjectType.lift, PropertyIdentifier.passengerAlarm, Boolean.class, true);
        add(ObjectType.lift, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.lift, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.lift, PropertyIdentifier.energyMeter, Real.class, false);
        add(ObjectType.lift, PropertyIdentifier.energyMeterRef, DeviceObjectReference.class, false);
        add(ObjectType.lift, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.lift, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.lift, PropertyIdentifier.carDriveStatus, LiftCarDriveStatus.class, false);
        add(ObjectType.lift, PropertyIdentifier.faultSignals, LiftFault.class, true, true);
        add(ObjectType.lift, PropertyIdentifier.landingDoorStatus, LandingDoorStatus.class, false, 0);
        add(ObjectType.lift, PropertyIdentifier.higherDeck, ObjectIdentifier.class, false);
        add(ObjectType.lift, PropertyIdentifier.lowerDeck, ObjectIdentifier.class, false);
        add(ObjectType.lift, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.lift, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.lift, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.lift, PropertyIdentifier.eventState, EventState.class, false);
        add(ObjectType.lift, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.lift, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.lift, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.lift, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.lift, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.lift, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.lift, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.lift, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.lift, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.lift, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.lift, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.lift, PropertyIdentifier.profileName, CharacterString.class, false);

        // Lighting output - 12.54
        add(ObjectType.lightingOutput, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.presentValue, Real.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.trackingValue, Real.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.lightingCommand, LightingCommand.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.inProgress, LightingInProgress.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.blinkWarnEnable, Boolean.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.egressTime, UnsignedInteger.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.egressActive, Boolean.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.defaultFadeTime, UnsignedInteger.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.defaultRampRate, Real.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.defaultStepIncrement, Real.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.transition, LightingTransition.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.feedbackValue, Real.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.priorityArray, PriorityArray.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.relinquishDefault, Real.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.power, Real.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.instantaneousPower, Real.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.minActualValue, Real.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.maxActualValue, Real.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.lightingCommandDefaultPriority, UnsignedInteger.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.covIncrement, Real.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.lightingOutput, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.lightingOutput, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.lightingOutput, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.lightingOutput, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.lightingOutput, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.lightingOutput, PropertyIdentifier.profileName, CharacterString.class, false);

        // Load control - 12.28
        add(ObjectType.loadControl, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.presentValue, ShedState.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.stateDescription, CharacterString.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.requestedShedLevel, ShedLevel.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.startTime, DateTime.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.shedDuration, UnsignedInteger.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.dutyWindow, UnsignedInteger.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.enable, Boolean.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.fullDutyBaseline, Real.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.expectedShedLevel, ShedLevel.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.actualShedLevel, ShedLevel.class, true);
        add(ObjectType.loadControl, PropertyIdentifier.shedLevels, UnsignedInteger.class, true, 0);
        add(ObjectType.loadControl, PropertyIdentifier.shedLevelDescriptions, CharacterString.class, true, 0);
        add(ObjectType.loadControl, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.loadControl, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.loadControl, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.loadControl, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.loadControl, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.loadControl, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.loadControl, PropertyIdentifier.profileName, CharacterString.class, false);

        // Loop - 12.17
        add(ObjectType.loop, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.loop, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.loop, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.loop, PropertyIdentifier.presentValue, Real.class, true);
        add(ObjectType.loop, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.loop, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.loop, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.loop, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.loop, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.loop, PropertyIdentifier.updateInterval, UnsignedInteger.class, false);
        add(ObjectType.loop, PropertyIdentifier.outputUnits, EngineeringUnits.class, true);
        add(ObjectType.loop, PropertyIdentifier.manipulatedVariableReference, ObjectPropertyReference.class, true);
        add(ObjectType.loop, PropertyIdentifier.controlledVariableReference, ObjectPropertyReference.class, true);
        add(ObjectType.loop, PropertyIdentifier.controlledVariableValue, Real.class, true);
        add(ObjectType.loop, PropertyIdentifier.controlledVariableUnits, EngineeringUnits.class, true);
        add(ObjectType.loop, PropertyIdentifier.setpointReference, SetpointReference.class, true);
        add(ObjectType.loop, PropertyIdentifier.setpoint, Real.class, true);
        add(ObjectType.loop, PropertyIdentifier.action, Action.class, true);
        add(ObjectType.loop, PropertyIdentifier.proportionalConstant, Real.class, false);
        add(ObjectType.loop, PropertyIdentifier.proportionalConstantUnits, EngineeringUnits.class, false);
        add(ObjectType.loop, PropertyIdentifier.integralConstant, Real.class, false);
        add(ObjectType.loop, PropertyIdentifier.integralConstantUnits, EngineeringUnits.class, false);
        add(ObjectType.loop, PropertyIdentifier.derivativeConstant, Real.class, false);
        add(ObjectType.loop, PropertyIdentifier.derivativeConstantUnits, EngineeringUnits.class, false);
        add(ObjectType.loop, PropertyIdentifier.bias, Real.class, false);
        add(ObjectType.loop, PropertyIdentifier.maximumOutput, Real.class, false);
        add(ObjectType.loop, PropertyIdentifier.minimumOutput, Real.class, false);
        add(ObjectType.loop, PropertyIdentifier.priorityForWriting, UnsignedInteger.class, true);
        add(ObjectType.loop, PropertyIdentifier.covIncrement, Real.class, false);
        add(ObjectType.loop, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.loop, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.loop, PropertyIdentifier.errorLimit, Real.class, false);
        add(ObjectType.loop, PropertyIdentifier.deadband, Real.class, false);
        add(ObjectType.loop, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.loop, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.loop, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.loop, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.loop, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.loop, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.loop, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.loop, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.loop, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.loop, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.loop, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.loop, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.loop, PropertyIdentifier.lowDiffLimit, OptionalReal.class, false);
        add(ObjectType.loop, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.loop, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.loop, PropertyIdentifier.profileName, CharacterString.class, false);

        // Multi state input - 12.18
        add(ObjectType.multiStateInput, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.presentValue, UnsignedInteger.class, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.deviceType, CharacterString.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.numberOfStates, UnsignedInteger.class, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.stateText, CharacterString.class, false, 0);
        add(ObjectType.multiStateInput, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.alarmValues, UnsignedInteger.class, false, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.faultValues, UnsignedInteger.class, false, true);
        add(ObjectType.multiStateInput, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.multiStateInput, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.multiStateInput, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.multiStateInput, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.multiStateInput, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.multiStateInput, PropertyIdentifier.interfaceValue, OptionalUnsigned.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.multiStateInput, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.multiStateInput, PropertyIdentifier.profileName, CharacterString.class, false);

        // Multi state output - 12.19
        add(ObjectType.multiStateOutput, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.presentValue, UnsignedInteger.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.deviceType, CharacterString.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.numberOfStates, UnsignedInteger.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.stateText, CharacterString.class, false, 0);
        add(ObjectType.multiStateOutput, PropertyIdentifier.priorityArray, PriorityArray.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.relinquishDefault, UnsignedInteger.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.feedbackValue, UnsignedInteger.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.multiStateOutput, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.multiStateOutput, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.multiStateOutput, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.multiStateOutput, PropertyIdentifier.interfaceValue, OptionalUnsigned.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.multiStateOutput, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.multiStateOutput, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.multiStateOutput, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.multiStateOutput, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.multiStateOutput, PropertyIdentifier.profileName, CharacterString.class, false);

        // Multi state value - 12.20
        add(ObjectType.multiStateValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.presentValue, UnsignedInteger.class, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.numberOfStates, UnsignedInteger.class, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.stateText, CharacterString.class, false, 0);
        add(ObjectType.multiStateValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.relinquishDefault, UnsignedInteger.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.alarmValues, UnsignedInteger.class, false, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.faultValues, UnsignedInteger.class, false, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.multiStateValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.multiStateValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.multiStateValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.multiStateValue, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.multiStateValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.multiStateValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.multiStateValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.multiStateValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.multiStateValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.multiStateValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Network port - 12.56
        add(ObjectType.networkPort, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.networkType, NetworkType.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.protocolLevel, ProtocolLevel.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.referencePort, UnsignedInteger.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.networkNumber, Unsigned16.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.networkNumberQuality, NetworkNumberQuality.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.changesPending, Boolean.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.command, NetworkPortCommand.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.macAddress, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.apduLength, UnsignedInteger.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.linkSpeed, Real.class, true);
        add(ObjectType.networkPort, PropertyIdentifier.linkSpeeds, Real.class, false, 0);
        add(ObjectType.networkPort, PropertyIdentifier.linkSpeedAutonegotiate, Boolean.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.networkInterfaceName, CharacterString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bacnetIpMode, IPMode.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipAddress, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bacnetIpUdpPort, Unsigned16.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipSubnetMask, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipDefaultGateway, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bacnetIpMulticastAddress, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipDnsServer, OctetString.class, false, 0);
        add(ObjectType.networkPort, PropertyIdentifier.ipDhcpEnable, Boolean.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipDhcpLeaseTime, UnsignedInteger.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipDhcpLeaseTimeRemaining, UnsignedInteger.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipDhcpServer, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bacnetIpNatTraversal, Boolean.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bacnetIpGlobalAddress, HostNPort.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bbmdBroadcastDistributionTable, BDTEntry.class, false, true);
        add(ObjectType.networkPort, PropertyIdentifier.bbmdAcceptFdRegistrations, Boolean.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bbmdForeignDeviceTable, FDTEntry.class, false, true);
        add(ObjectType.networkPort, PropertyIdentifier.fdBbmdAddress, HostNPort.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.fdSubscriptionLifetime, Unsigned16.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bacnetIpv6Mode, IPMode.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipv6Address, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipv6PrefixLength, Unsigned8.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bacnetIpv6UdpPort, Unsigned16.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipv6DefaultGateway, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.bacnetIpv6MulticastAddress, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipv6DnsServer, OctetString.class, false, 0);
        add(ObjectType.networkPort, PropertyIdentifier.ipv6AutoAddressingEnable, Boolean.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipv6DhcpLeaseTime, UnsignedInteger.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipv6DhcpLeaseTimeRemaining, UnsignedInteger.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipv6DhcpServer, OctetString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ipv6ZoneIndex, CharacterString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.maxMaster, Unsigned8.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.maxInfoFrames, Unsigned8.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.slaveProxyEnable, Boolean.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.manualSlaveAddressBinding, AddressBinding.class, false, true);
        add(ObjectType.networkPort, PropertyIdentifier.autoSlaveDiscovery, Boolean.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.slaveAddressBinding, AddressBinding.class, false, true);
        add(ObjectType.networkPort, PropertyIdentifier.virtualMacAddressTable, VmacEntry.class, false, true);
        add(ObjectType.networkPort, PropertyIdentifier.routingTable, RouterEntry.class, false, true);
        add(ObjectType.networkPort, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.networkPort, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.networkPort, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.networkPort, PropertyIdentifier.eventState, EventState.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.networkPort, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.networkPort, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.networkPort, PropertyIdentifier.profileName, CharacterString.class, false);

        // Network security - 12.49
        add(ObjectType.networkSecurity, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.networkSecurity, PropertyIdentifier.baseDeviceSecurityPolicy, SecurityLevel.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.networkAccessSecurityPolicies, NetworkSecurityPolicy.class,
                true, 0);
        add(ObjectType.networkSecurity, PropertyIdentifier.securityTimeWindow, UnsignedInteger.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.packetReorderTime, UnsignedInteger.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.distributionKeyRevision, Unsigned8.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.keySets, SecurityKeySet.class, true, 2);
        add(ObjectType.networkSecurity, PropertyIdentifier.lastKeyServer, AddressBinding.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.securityPduTimeout, Unsigned16.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.updateKeySetTimeout, Unsigned16.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.supportedSecurityAlgorithms, Unsigned8.class, true, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.doNotHide, Boolean.class, true);
        add(ObjectType.networkSecurity, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.networkSecurity, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.networkSecurity, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.networkSecurity, PropertyIdentifier.profileName, CharacterString.class, false);

        // Notification class - 12.21
        add(ObjectType.notificationClass, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.notificationClass, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.notificationClass, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.notificationClass, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.notificationClass, PropertyIdentifier.notificationClass, UnsignedInteger.class, true);
        add(ObjectType.notificationClass, PropertyIdentifier.priority, UnsignedInteger.class, true, 3);
        add(ObjectType.notificationClass, PropertyIdentifier.ackRequired, EventTransitionBits.class, true);
        add(ObjectType.notificationClass, PropertyIdentifier.recipientList, Destination.class, true, true);
        add(ObjectType.notificationClass, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.notificationClass, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.notificationClass, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.notificationClass, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.notificationClass, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.notificationClass, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.notificationClass, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.notificationClass, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.notificationClass, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.notificationClass, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.notificationClass, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.notificationClass, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.notificationClass, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.notificationClass, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.notificationClass, PropertyIdentifier.profileName, CharacterString.class, false);

        // Notification forwarder - 12.51
        add(ObjectType.notificationForwarder, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.notificationForwarder, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.recipientList, Destination.class, true, true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.subscribedRecipients,
                EventNotificationSubscription.class, true, true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.processIdentifierFilter, ProcessIdSelection.class,
                true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.portFilter, PortPermission.class, false, 0);
        add(ObjectType.notificationForwarder, PropertyIdentifier.localForwardingOnly, Boolean.class, true);
        add(ObjectType.notificationForwarder, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.notificationForwarder, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.notificationForwarder, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.notificationForwarder, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.notificationForwarder, PropertyIdentifier.profileName, CharacterString.class, false);

        // OctetString value - 12.41
        add(ObjectType.octetstringValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.octetstringValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.octetstringValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.octetstringValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.octetstringValue, PropertyIdentifier.presentValue, OctetString.class, true);
        add(ObjectType.octetstringValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.octetstringValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.octetstringValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.octetstringValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.octetstringValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.octetstringValue, PropertyIdentifier.relinquishDefault, OctetString.class, false);
        add(ObjectType.octetstringValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.octetstringValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.octetstringValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.octetstringValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.octetstringValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.octetstringValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.octetstringValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.octetstringValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.octetstringValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.octetstringValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Positive integer value - 12.44
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.presentValue, UnsignedInteger.class, true);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.units, EngineeringUnits.class, true);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.relinquishDefault, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.covIncrement, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.highLimit, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.lowLimit, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.deadband, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.limitEnable, LimitEnable.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false,
                3);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.minPresValue, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.maxPresValue, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.resolution, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.faultHighLimit, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.faultLowLimit, UnsignedInteger.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.positiveIntegerValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Program - 12.22
        add(ObjectType.program, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.program, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.program, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.program, PropertyIdentifier.programState, ProgramState.class, true);
        add(ObjectType.program, PropertyIdentifier.programChange, ProgramRequest.class, true);
        add(ObjectType.program, PropertyIdentifier.reasonForHalt, ProgramError.class, false);
        add(ObjectType.program, PropertyIdentifier.descriptionOfHalt, CharacterString.class, true);
        add(ObjectType.program, PropertyIdentifier.programLocation, CharacterString.class, false);
        add(ObjectType.program, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.program, PropertyIdentifier.instanceOf, CharacterString.class, false);
        add(ObjectType.program, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.program, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.program, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.program, PropertyIdentifier.eventDetectionEnable, Boolean.class, true);
        add(ObjectType.program, PropertyIdentifier.notificationClass, UnsignedInteger.class, true);
        add(ObjectType.program, PropertyIdentifier.eventEnable, EventTransitionBits.class, true);
        add(ObjectType.program, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.program, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, true);
        add(ObjectType.program, PropertyIdentifier.notifyType, NotifyType.class, true);
        add(ObjectType.program, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.program, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.program, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.program, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.program, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.program, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.program, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.program, PropertyIdentifier.profileName, CharacterString.class, false);

        // Pulse converter - 12.23
        add(ObjectType.pulseConverter, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.presentValue, Real.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.inputReference, ObjectPropertyReference.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.units, EngineeringUnits.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.scaleFactor, Real.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.adjustValue, Real.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.count, UnsignedInteger.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.updateTime, DateTime.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.countChangeTime, DateTime.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.countBeforeChange, UnsignedInteger.class, true);
        add(ObjectType.pulseConverter, PropertyIdentifier.covIncrement, Real.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.covPeriod, UnsignedInteger.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.highLimit, Real.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.lowLimit, Real.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.deadband, Real.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.limitEnable, LimitEnable.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.pulseConverter, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.pulseConverter, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.pulseConverter, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.pulseConverter, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.pulseConverter, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.pulseConverter, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.pulseConverter, PropertyIdentifier.profileName, CharacterString.class, false);

        // Schedule - 12.24
        add(ObjectType.schedule, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.schedule, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.schedule, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.schedule, PropertyIdentifier.presentValue, Primitive.class, true);
        add(ObjectType.schedule, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.schedule, PropertyIdentifier.effectivePeriod, DateRange.class, true);
        add(ObjectType.schedule, PropertyIdentifier.weeklySchedule, DailySchedule.class, false, 7);
        add(ObjectType.schedule, PropertyIdentifier.exceptionSchedule, SpecialEvent.class, false, 0);
        add(ObjectType.schedule, PropertyIdentifier.scheduleDefault, Primitive.class, true);
        add(ObjectType.schedule, PropertyIdentifier.listOfObjectPropertyReferences, DeviceObjectPropertyReference.class,
                true, true);
        add(ObjectType.schedule, PropertyIdentifier.priorityForWriting, UnsignedInteger.class, true);
        add(ObjectType.schedule, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.schedule, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.schedule, PropertyIdentifier.outOfService, Boolean.class, true);
        add(ObjectType.schedule, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.schedule, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.schedule, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.schedule, PropertyIdentifier.eventState, EventState.class, false);
        add(ObjectType.schedule, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.schedule, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.schedule, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.schedule, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.schedule, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.schedule, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.schedule, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.schedule, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.schedule, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.schedule, PropertyIdentifier.profileName, CharacterString.class, false);

        // Structured View - 12.29
        add(ObjectType.structuredView, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.structuredView, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.structuredView, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.structuredView, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.structuredView, PropertyIdentifier.nodeType, NodeType.class, false);
        add(ObjectType.structuredView, PropertyIdentifier.nodeSubtype, CharacterString.class, false);
        add(ObjectType.structuredView, PropertyIdentifier.subordinateList, DeviceObjectReference.class, true, 0);
        add(ObjectType.structuredView, PropertyIdentifier.subordinateAnnotations, CharacterString.class, false, 0);
        add(ObjectType.structuredView, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.structuredView, PropertyIdentifier.subordinateTags, NameValueCollection.class, false, 0);
        add(ObjectType.structuredView, PropertyIdentifier.subordinateNodeTypes, NodeType.class, false, 0);
        add(ObjectType.structuredView, PropertyIdentifier.subordinateRelationships, Relationship.class, false, 0);
        add(ObjectType.structuredView, PropertyIdentifier.defaultSubordinateRelationship, Relationship.class, false);
        add(ObjectType.structuredView, PropertyIdentifier.represents, DeviceObjectReference.class, false);
        add(ObjectType.structuredView, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.structuredView, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.structuredView, PropertyIdentifier.profileName, CharacterString.class, false);

        // Time value - 12.42
        add(ObjectType.timeValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.timeValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.timeValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.timeValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.presentValue, Time.class, true);
        add(ObjectType.timeValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.timeValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.timeValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.relinquishDefault, Time.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.timeValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.timeValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.timeValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.timeValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.timeValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.timeValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.timeValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.timeValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.timeValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Time pattern value - 12.47
        add(ObjectType.timePatternValue, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.timePatternValue, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.timePatternValue, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.timePatternValue, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.presentValue, Time.class, true);
        add(ObjectType.timePatternValue, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.timePatternValue, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.timePatternValue, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.priorityArray, PriorityArray.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.relinquishDefault, Time.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.timePatternValue, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.timePatternValue, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.timePatternValue, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.timePatternValue, PropertyIdentifier.currentCommandPriority, OptionalUnsigned.class, true);
        add(ObjectType.timePatternValue, PropertyIdentifier.valueSource, ValueSource.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.valueSourceArray, ValueSource.class, false, 16);
        add(ObjectType.timePatternValue, PropertyIdentifier.lastCommandTime, TimeStamp.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.commandTimeArray, TimeStamp.class, false, 16);
        add(ObjectType.timePatternValue, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.timePatternValue, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.timePatternValue, PropertyIdentifier.profileName, CharacterString.class, false);

        // Timer - 12.57
        add(ObjectType.timer, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.timer, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.timer, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.timer, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.timer, PropertyIdentifier.presentValue, Time.class, true);
        add(ObjectType.timer, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.timer, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.timer, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.timer, PropertyIdentifier.outOfService, Boolean.class, false);
        add(ObjectType.timer, PropertyIdentifier.timerState, TimerState.class, true);
        add(ObjectType.timer, PropertyIdentifier.timerRunning, Boolean.class, true);
        add(ObjectType.timer, PropertyIdentifier.updateTime, DateTime.class, false);
        add(ObjectType.timer, PropertyIdentifier.lastStateChange, TimerTransition.class, false);
        add(ObjectType.timer, PropertyIdentifier.expirationTime, DateTime.class, false);
        add(ObjectType.timer, PropertyIdentifier.initialTimeout, UnsignedInteger.class, false);
        add(ObjectType.timer, PropertyIdentifier.defaultTimeout, UnsignedInteger.class, false);
        add(ObjectType.timer, PropertyIdentifier.minPresValue, UnsignedInteger.class, false);
        add(ObjectType.timer, PropertyIdentifier.maxPresValue, UnsignedInteger.class, false);
        add(ObjectType.timer, PropertyIdentifier.resolution, UnsignedInteger.class, false);
        add(ObjectType.timer, PropertyIdentifier.stateChangeValues, TimerStateChangeValue.class, false, 7);
        add(ObjectType.timer, PropertyIdentifier.listOfObjectPropertyReferences, DeviceObjectPropertyReference.class,
                false, true);
        add(ObjectType.timer, PropertyIdentifier.priorityForWriting, UnsignedInteger.class, true);
        add(ObjectType.timer, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.timer, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.timer, PropertyIdentifier.timeDelay, UnsignedInteger.class, false);
        add(ObjectType.timer, PropertyIdentifier.timeDelayNormal, UnsignedInteger.class, false);
        add(ObjectType.timer, PropertyIdentifier.alarmValues, TimerState.class, false, true);
        add(ObjectType.timer, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.timer, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.timer, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.timer, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.timer, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.timer, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.timer, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.timer, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.timer, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.timer, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.timer, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.timer, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.timer, PropertyIdentifier.profileName, CharacterString.class, false);

        // Trend log - 12.25
        add(ObjectType.trendLog, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.enable, Boolean.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.startTime, DateTime.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.stopTime, DateTime.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.logDeviceObjectProperty, DeviceObjectPropertyReference.class,
                false);
        add(ObjectType.trendLog, PropertyIdentifier.logInterval, UnsignedInteger.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.covResubscriptionInterval, UnsignedInteger.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.clientCovIncrement, ClientCov.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.stopWhenFull, Boolean.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.bufferSize, UnsignedInteger.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.logBuffer, LogRecord.class, true, true);
        add(ObjectType.trendLog, PropertyIdentifier.recordCount, UnsignedInteger.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.totalRecordCount, UnsignedInteger.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.loggingType, LoggingType.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.alignIntervals, Boolean.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.intervalOffset, UnsignedInteger.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.trigger, Boolean.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.reliability, Reliability.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.notificationThreshold, UnsignedInteger.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.recordsSinceNotification, UnsignedInteger.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.lastNotifyRecord, UnsignedInteger.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.trendLog, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.trendLog, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.trendLog, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.trendLog, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.trendLog, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.trendLog, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.trendLog, PropertyIdentifier.profileName, CharacterString.class, false);

        // Trend log multiple - 12.30
        add(ObjectType.trendLogMultiple, PropertyIdentifier.objectIdentifier, ObjectIdentifier.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.objectName, CharacterString.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.objectType, ObjectType.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.description, CharacterString.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.statusFlags, StatusFlags.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.eventState, EventState.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.reliability, Reliability.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.enable, Boolean.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.startTime, DateTime.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.stopTime, DateTime.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.logDeviceObjectProperty,
                DeviceObjectPropertyReference.class, true, 0);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.loggingType, LoggingType.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.logInterval, UnsignedInteger.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.alignIntervals, Boolean.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.intervalOffset, UnsignedInteger.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.trigger, Boolean.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.stopWhenFull, Boolean.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.bufferSize, UnsignedInteger.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.logBuffer, LogMultipleRecord.class, true, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.recordCount, UnsignedInteger.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.totalRecordCount, UnsignedInteger.class, true);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.notificationThreshold, UnsignedInteger.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.recordsSinceNotification, UnsignedInteger.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.lastNotifyRecord, UnsignedInteger.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.notificationClass, UnsignedInteger.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.eventEnable, EventTransitionBits.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.ackedTransitions, EventTransitionBits.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.notifyType, NotifyType.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.eventTimeStamps, TimeStamp.class, false, 3);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.eventMessageTexts, CharacterString.class, false, 3);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.eventMessageTextsConfig, CharacterString.class, false, 3);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.eventDetectionEnable, Boolean.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.eventAlgorithmInhibitRef, ObjectPropertyReference.class,
                false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.eventAlgorithmInhibit, Boolean.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.propertyList, PropertyIdentifier.class, true, 0);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.tags, NameValue.class, false, 0);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.profileLocation, CharacterString.class, false);
        add(ObjectType.trendLogMultiple, PropertyIdentifier.profileName, CharacterString.class, false);
    }
}
