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
import java.util.TimeZone;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.mstp.MasterNode;
import com.serotonin.bacnet4j.npdu.mstp.MstpNetwork;
import com.serotonin.bacnet4j.npdu.mstp.MstpNode;
import com.serotonin.bacnet4j.obj.mixin.ActiveCovSubscriptionMixin;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.ObjectListMixin;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.obj.mixin.TimeSynchronizationMixin;
import com.serotonin.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.event.eventAlgo.NoneAlgo;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.AddressBinding;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.ObjectTypesSupported;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.BackupState;
import com.serotonin.bacnet4j.type.enumerated.DeviceStatus;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class DeviceObject extends BACnetObject {
    private static final int VENDOR_ID = 865; //Infinite Automation Systems, Inc.

    public DeviceObject(final LocalDevice localDevice, final int instanceNumber) throws BACnetServiceException {
        super(localDevice, ObjectType.device, instanceNumber, "BACnet4J device " + instanceNumber);

        writePropertyInternal(PropertyIdentifier.maxApduLengthAccepted,
                new UnsignedInteger(MaxApduLength.UP_TO_1476.getMaxLengthInt()));
        writePropertyInternal(PropertyIdentifier.vendorIdentifier, new UnsignedInteger(VENDOR_ID));
        writePropertyInternal(PropertyIdentifier.vendorName,
                new CharacterString("Infinite Automation Systems, Inc."));
        writePropertyInternal(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        writePropertyInternal(PropertyIdentifier.maxSegmentsAccepted, new UnsignedInteger(Integer.MAX_VALUE));
        writePropertyInternal(PropertyIdentifier.apduSegmentTimeout,
                new UnsignedInteger(Transport.DEFAULT_SEG_TIMEOUT));
        writePropertyInternal(PropertyIdentifier.apduTimeout, new UnsignedInteger(Transport.DEFAULT_TIMEOUT));
        writePropertyInternal(PropertyIdentifier.numberOfApduRetries, new UnsignedInteger(Transport.DEFAULT_RETRIES));
        writePropertyInternal(PropertyIdentifier.objectList, new BACnetArray<ObjectIdentifier>());

        // Set up the supported services indicators. Remove lines as services get implemented.
        final ServicesSupported servicesSupported = new ServicesSupported();
        servicesSupported.setAcknowledgeAlarm(true);
        servicesSupported.setConfirmedCovNotification(true);
        servicesSupported.setConfirmedEventNotification(true);
        servicesSupported.setGetAlarmSummary(true);
        servicesSupported.setGetEnrollmentSummary(true);
        servicesSupported.setSubscribeCov(true);
        servicesSupported.setAtomicReadFile(true);
        servicesSupported.setAtomicWriteFile(true);
        servicesSupported.setAddListElement(true);
        servicesSupported.setRemoveListElement(true);
        servicesSupported.setCreateObject(true);
        servicesSupported.setDeleteObject(true);
        servicesSupported.setReadProperty(true);
        servicesSupported.setReadPropertyMultiple(true);
        servicesSupported.setWriteProperty(true);
        servicesSupported.setWritePropertyMultiple(true);
        servicesSupported.setDeviceCommunicationControl(true);
        servicesSupported.setConfirmedPrivateTransfer(true);
        servicesSupported.setConfirmedTextMessage(true);
        servicesSupported.setReinitializeDevice(true);
        //        servicesSupported.setVtOpen(true);
        //        servicesSupported.setVtClose(true);
        //        servicesSupported.setVtData(true);
        servicesSupported.setIAm(true);
        servicesSupported.setIHave(true);
        servicesSupported.setUnconfirmedCovNotification(true);
        servicesSupported.setUnconfirmedEventNotification(true);
        servicesSupported.setUnconfirmedPrivateTransfer(true);
        servicesSupported.setUnconfirmedTextMessage(true);
        servicesSupported.setTimeSynchronization(true);
        servicesSupported.setWhoHas(true);
        servicesSupported.setWhoIs(true);
        servicesSupported.setReadRange(true);
        servicesSupported.setUtcTimeSynchronization(true);
        servicesSupported.setLifeSafetyOperation(true);
        servicesSupported.setSubscribeCovProperty(true);
        servicesSupported.setGetEventInformation(true);
        //        servicesSupported.setWriteGroup(true);
        servicesSupported.setSubscribeCovPropertyMultiple(true);
        servicesSupported.setConfirmedCovNotificationMultiple(true);
        servicesSupported.setUnconfirmedCovNotificationMultiple(true);

        writePropertyInternal(PropertyIdentifier.protocolServicesSupported, servicesSupported);

        // Set up the object types supported.
        final ObjectTypesSupported objectTypesSupported = new ObjectTypesSupported();
        objectTypesSupported.set(ObjectType.analogInput, true);
        objectTypesSupported.set(ObjectType.analogOutput, true);
        objectTypesSupported.set(ObjectType.analogValue, true);
        objectTypesSupported.set(ObjectType.binaryInput, true);
        objectTypesSupported.set(ObjectType.binaryOutput, true);
        objectTypesSupported.set(ObjectType.binaryValue, true);
        objectTypesSupported.set(ObjectType.calendar, true);
        //        objectTypesSupported.set(ObjectType.command, true);
        objectTypesSupported.set(ObjectType.device, true);
        objectTypesSupported.set(ObjectType.eventEnrollment, true);
        objectTypesSupported.set(ObjectType.file, true);
        objectTypesSupported.set(ObjectType.group, true);
        //        objectTypesSupported.set(ObjectType.loop, true);
        objectTypesSupported.set(ObjectType.multiStateInput, true);
        objectTypesSupported.set(ObjectType.multiStateOutput, true);
        objectTypesSupported.set(ObjectType.notificationClass, true);
        //        objectTypesSupported.set(ObjectType.program, true);
        objectTypesSupported.set(ObjectType.schedule, true);
        objectTypesSupported.set(ObjectType.averaging, true);
        objectTypesSupported.set(ObjectType.multiStateValue, true);
        objectTypesSupported.set(ObjectType.trendLog, true);
        objectTypesSupported.set(ObjectType.lifeSafetyPoint, true);
        objectTypesSupported.set(ObjectType.lifeSafetyZone, true);
        objectTypesSupported.set(ObjectType.accumulator, true);
        objectTypesSupported.set(ObjectType.pulseConverter, true);
        objectTypesSupported.set(ObjectType.eventLog, true);
        //        objectTypesSupported.set(ObjectType.globalGroup, true);
        objectTypesSupported.set(ObjectType.trendLogMultiple, true);
        //        objectTypesSupported.set(ObjectType.loadControl, true);
        //        objectTypesSupported.set(ObjectType.structuredView, true);
        //        objectTypesSupported.set(ObjectType.accessDoor, true);
        //        objectTypesSupported.set(ObjectType.timer, true);
        //        objectTypesSupported.set(ObjectType.accessCredential, true);
        //        objectTypesSupported.set(ObjectType.accessPoint, true);
        //        objectTypesSupported.set(ObjectType.accessRights, true);
        //        objectTypesSupported.set(ObjectType.accessUser, true);
        //        objectTypesSupported.set(ObjectType.accessZone, true);
        //        objectTypesSupported.set(ObjectType.credentialDataInput, true);
        //        objectTypesSupported.set(ObjectType.networkSecurity, true);
        //        objectTypesSupported.set(ObjectType.bitstringValue, true);
        //        objectTypesSupported.set(ObjectType.characterstringValue, true);
        //        objectTypesSupported.set(ObjectType.datePatternValue, true);
        //        objectTypesSupported.set(ObjectType.dateValue, true);
        //        objectTypesSupported.set(ObjectType.datetimePatternValue, true);
        //        objectTypesSupported.set(ObjectType.datetimeValue, true);
        //        objectTypesSupported.set(ObjectType.integerValue, true);
        //        objectTypesSupported.set(ObjectType.largeAnalogValue, true);
        //        objectTypesSupported.set(ObjectType.octetstringValue, true);
        //        objectTypesSupported.set(ObjectType.positiveIntegerValue, true);
        //        objectTypesSupported.set(ObjectType.timePatternValue, true);
        //        objectTypesSupported.set(ObjectType.timeValue, true);
        objectTypesSupported.set(ObjectType.notificationForwarder, true);
        objectTypesSupported.set(ObjectType.alertEnrollment, true);
        //        objectTypesSupported.set(ObjectType.channel, true);
        //        objectTypesSupported.set(ObjectType.lightingOutput, true);
        //        objectTypesSupported.set(ObjectType.binaryLightingOutput, true);
        //        objectTypesSupported.set(ObjectType.networkPort, true);
        //        objectTypesSupported.set(ObjectType.elevatorGroup, true);
        //        objectTypesSupported.set(ObjectType.escalator, true);
        //        objectTypesSupported.set(ObjectType.lift, true);

        writePropertyInternal(PropertyIdentifier.protocolObjectTypesSupported, objectTypesSupported);

        // Set some other required values to defaults
        writePropertyInternal(PropertyIdentifier.systemStatus, DeviceStatus.operational);
        writePropertyInternal(PropertyIdentifier.modelName, new CharacterString("BACnet4J"));
        writePropertyInternal(PropertyIdentifier.firmwareRevision, new CharacterString("not set"));
        writePropertyInternal(PropertyIdentifier.applicationSoftwareVersion, new CharacterString(LocalDevice.VERSION));
        writePropertyInternal(PropertyIdentifier.protocolVersion, new UnsignedInteger(1));
        writePropertyInternal(PropertyIdentifier.protocolRevision, new UnsignedInteger(19));

        UnsignedInteger databaseRevision = getLocalDevice().getPersistence()
                .loadEncodable(getPersistenceKey(PropertyIdentifier.databaseRevision), UnsignedInteger.class);
        if (databaseRevision == null)
            databaseRevision = UnsignedInteger.ZERO;
        writePropertyInternal(PropertyIdentifier.databaseRevision, databaseRevision);

        writePropertyInternal(PropertyIdentifier.timeOfDeviceRestart, new TimeStamp(new DateTime(getLocalDevice())));
        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.configurationFiles, new BACnetArray<>(0, null));
        writePropertyInternal(PropertyIdentifier.lastRestoreTime, new TimeStamp(DateTime.UNSPECIFIED));
        writePropertyInternal(PropertyIdentifier.backupFailureTimeout, new Unsigned16(60));
        writePropertyInternal(PropertyIdentifier.backupPreparationTime, new Unsigned16(0));
        writePropertyInternal(PropertyIdentifier.restorePreparationTime, new Unsigned16(0));
        writePropertyInternal(PropertyIdentifier.restoreCompletionTime, new Unsigned16(0));
        writePropertyInternal(PropertyIdentifier.backupAndRestoreState, BackupState.idle);

        //These properties are automatically overwritten when reading. They are defined here to be present when reading the PropertyList.     
        set(PropertyIdentifier.utcOffset, new SignedInteger(0));
        set(PropertyIdentifier.localTime, new Time(getLocalDevice()));
        set(PropertyIdentifier.localDate, new Date(getLocalDevice()));
        set(PropertyIdentifier.daylightSavingsStatus, Boolean.FALSE);

        // Mixins
        addMixin(new ActiveCovSubscriptionMixin(this));
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.activeCovSubscriptions,
                PropertyIdentifier.localTime, PropertyIdentifier.localDate, PropertyIdentifier.deviceAddressBinding));
        addMixin(new ObjectListMixin(this));

        localDevice.addObject(this);
    }

    public DeviceObject supportTimeSynchronization(final SequenceOf<Recipient> timeSynchronizationRecipients,
            final SequenceOf<Recipient> utcTimeSynchronizationRecipients, final int timeSynchronizationInterval,
            final boolean alignIntervals, final int intervalOffset) {
        final TimeSynchronizationMixin m = new TimeSynchronizationMixin(this, timeSynchronizationRecipients,
                utcTimeSynchronizationRecipients, timeSynchronizationInterval, alignIntervals, intervalOffset);
        addMixin(m);
        m.update();
        return this;
    }

    public DeviceObject supportIntrinsicReporting(final int notificationClass, final EventTransitionBits eventEnable,
            final NotifyType notifyType) {
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        addMixin(new IntrinsicReportingMixin(this, new NoneAlgo(), null, null, new PropertyIdentifier[] {}));

        return this;
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) {
        if (pid.equals(PropertyIdentifier.localTime)) {
            set(PropertyIdentifier.localTime, new Time(getLocalDevice()));
        } else if (pid.equals(PropertyIdentifier.localDate)) {
            set(PropertyIdentifier.localDate, new Date(getLocalDevice()));
        } else if (pid.equals(PropertyIdentifier.utcOffset)) {
            final int offsetMillis = TimeZone.getDefault().getOffset(getLocalDevice().getClock().millis());
            writePropertyInternal(PropertyIdentifier.utcOffset, new SignedInteger(offsetMillis / 1000 / 60));
        } else if (pid.equals(PropertyIdentifier.daylightSavingsStatus)) {
            final boolean dst = TimeZone.getDefault()
                    .inDaylightTime(new java.util.Date(getLocalDevice().getClock().millis()));
            writePropertyInternal(PropertyIdentifier.daylightSavingsStatus, Boolean.valueOf(dst));
        } else if (pid.equals(PropertyIdentifier.deviceAddressBinding)) {
            final SequenceOf<AddressBinding> bindings = new SequenceOf<>();
            for (final RemoteDevice d : getLocalDevice().getRemoteDevices()) {
                if (d != null) {
                    bindings.add(new AddressBinding(d.getObjectIdentifier(), d.getAddress()));
                }
            }
            writePropertyInternal(PropertyIdentifier.deviceAddressBinding, bindings);
        }
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (value.getPropertyIdentifier().equals(PropertyIdentifier.maxMaster)) {
            final MasterNode masterNode = getMasterNode();
            if (masterNode != null) {
                final UnsignedInteger maxMaster = value.getValue();
                if (masterNode.getThisStation() > maxMaster.intValue()) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
                }
            }
        }
        value.validate();
        return false;
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (pid.equals(PropertyIdentifier.restartNotificationRecipients)) {
            // Persist the new list.
            getLocalDevice().getPersistence()
                    .saveEncodable(getPersistenceKey(PropertyIdentifier.restartNotificationRecipients), newValue);
        } else if (pid.equals(PropertyIdentifier.maxMaster)) {
            final MasterNode masterNode = getMasterNode();
            if (masterNode != null) {
                final UnsignedInteger maxMaster = (UnsignedInteger) newValue;
                masterNode.setMaxMaster(maxMaster.intValue());
            }
        } else if (pid.equals(PropertyIdentifier.maxInfoFrames)) {
            final MasterNode masterNode = getMasterNode();
            if (masterNode != null) {
                final UnsignedInteger maxInfoFrames = (UnsignedInteger) newValue;
                masterNode.setMaxInfoFrames(maxInfoFrames.intValue());
            }
        }
    }

    private MasterNode getMasterNode() {
        final Network network = getLocalDevice().getNetwork();
        if (network instanceof MstpNetwork) {
            final MstpNode node = ((MstpNetwork) network).getNode();
            if (node instanceof MasterNode) {
                return (MasterNode) node;
            }
        }
        return null;
    }
}
