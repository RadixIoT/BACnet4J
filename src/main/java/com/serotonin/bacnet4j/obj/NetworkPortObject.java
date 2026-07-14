/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.serotonin.bacnet4j.obj.mixin.WritablePropertyOutOfServiceMixin;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Health;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * <p>Changes written (non-internal) to this object will update the object's properties. The host's network
 * configuration is the source of truth of the current port state, such that if the device is restarted without
 * activation, port values can be read from the host configuration (or the Network subclass instance). If the device is
 * reinitialized using ACTIVATE_CHANGES or WARMSTART, the values in this object should be written to the host port
 * configuration.
 *
 * <p>Because network configuration is largely a local matter (dependent on the host), much of the functionality of
 * this class is left to product builders.
 */
public class NetworkPortObject extends BACnetObject {
    // Note: before the object is initialized, all written properties will be set in the object. After, all changes
    // pending properties will be written to the pending changes map.
    private final Map<PropertyIdentifier, Encodable> pendingChanges = new ConcurrentHashMap<>();

    public NetworkPortObject(LocalDevice localDevice, int instanceNumber, String name, boolean outOfService,
            NetworkType networkType, ProtocolLevel protocolLevel, Set<PropertyIdentifier> readOnlyProperties) {
        super(localDevice, ObjectType.networkPort, instanceNumber, name);

        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, outOfService));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.FALSE);
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        writePropertyInternal(PropertyIdentifier.networkType, networkType);
        writePropertyInternal(PropertyIdentifier.protocolLevel, protocolLevel);
        writePropertyInternal(PropertyIdentifier.referencePort, new UnsignedInteger(0x3FFFFF));
        writePropertyInternal(PropertyIdentifier.changesPending, Boolean.FALSE);
        writePropertyInternal(PropertyIdentifier.command, NetworkPortCommand.idle);
        writePropertyInternal(PropertyIdentifier.currentHealth, new Health(
                new DateTime(localDevice), new ErrorClassAndCode(ErrorClass.object, ErrorCode.success), null, null));
        writePropertyInternal(PropertyIdentifier.commandValidationResult, new Health(
                new DateTime(localDevice), new ErrorClassAndCode(ErrorClass.object, ErrorCode.success), null, null));
        writePropertyInternal(PropertyIdentifier.networkNumber, Unsigned16.ZERO);
        writePropertyInternal(PropertyIdentifier.networkNumberQuality, NetworkNumberQuality.unknown);

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new WritablePropertyOutOfServiceMixin(this, PropertyIdentifier.reliability));

        var readOnly = new HashSet<>(readOnlyProperties);
        readOnly.addAll(Set.of(
                PropertyIdentifier.statusFlags,
                PropertyIdentifier.networkNumberQuality,
                PropertyIdentifier.changesPending,
                PropertyIdentifier.currentHealth,
                PropertyIdentifier.commandValidationResult,
                PropertyIdentifier.linkSpeeds,
                PropertyIdentifier.ipDhcpLeaseTime,
                PropertyIdentifier.ipDhcpLeaseTimeRemaining,
                PropertyIdentifier.ipDhcpServer,
                PropertyIdentifier.bbmdForeignDeviceTable,
                PropertyIdentifier.ipv6DhcpLeaseTime,
                PropertyIdentifier.ipv6DhcpLeaseTimeRemaining,
                PropertyIdentifier.ipv6DhcpServer,
                PropertyIdentifier.routingTable,
                PropertyIdentifier.ackedTransitions,
                PropertyIdentifier.eventTimeStamps,
                PropertyIdentifier.eventMessageTexts,
                PropertyIdentifier.eventState,
                PropertyIdentifier.propertyList,
                PropertyIdentifier.scHubConnectorState,
                PropertyIdentifier.scPrimaryHubConnectionStatus,
                PropertyIdentifier.scFailoverHubConnectionStatus,
                PropertyIdentifier.scHubFunctionConnectionStatus,
                PropertyIdentifier.scDirectConnectConnectionStatus,
                PropertyIdentifier.scFailedConnectionRequests,
                PropertyIdentifier.operationalCertificateFile,
                PropertyIdentifier.issuerCertificateFiles,
                PropertyIdentifier.certificateSigningRequestFile
        ));
        addMixin(new ReadOnlyPropertyMixin(this, readOnly.toArray(new PropertyIdentifier[0])));
    }

    public Map<PropertyIdentifier, Encodable> getPendingChanges() {
        return Map.copyOf(pendingChanges);
    }

    /**
     * Returns the internal pending-changes map for subclass use. Subclasses that need to track
     * pending changes at a finer granularity than the base class's set/get overrides (e.g.,
     * SecureConnectNetworkPortObject tracking per-array-index cert file changes) mutate this
     * directly. External callers should use {@link #getPendingChanges()}.
     */
    protected Map<PropertyIdentifier, Encodable> pendingChanges() {
        return pendingChanges;
    }

    @Override
    protected boolean validateProperty(ValueSource valueSource, PropertyValue value) throws BACnetServiceException {
        if (value.getPropertyIdentifier().equals(PropertyIdentifier.command)) {
            validateCommand(value.getValue());
        }
        return false;
    }

    @Override
    protected void beforeReadProperty(PropertyIdentifier pid) throws BACnetServiceException {
        super.beforeReadProperty(pid);

        if (pid.equals(PropertyIdentifier.reliability) && isInService()) {
            Boolean rei = get(PropertyIdentifier.reliabilityEvaluationInhibit);
            var reliability = rei == Boolean.TRUE ? Reliability.noFaultDetected : evaluateReliability();
            writePropertyInternal(PropertyIdentifier.reliability, reliability);
        } else if (pid.equals(PropertyIdentifier.changesPending)) {
            writePropertyInternal(PropertyIdentifier.changesPending, isChanged() ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    protected Reliability evaluateReliability() {
        return Reliability.noFaultDetected;
    }

    public boolean isChanged() {
        return !pendingChanges.isEmpty();
    }

    protected boolean isInService() {
        return get(PropertyIdentifier.outOfService) != Boolean.TRUE;
    }

    public Health validateChanges() {
        // See the pending changes map. Override as required.
        return new Health(new DateTime(getLocalDevice()), new ErrorClassAndCode(ErrorClass.object, ErrorCode.success),
                null, null);
    }

    public void applyPendingChanges() {
        // See the pending changes map. To be implemented by subclasses. Can be called by a reinitialize device
        // listener.
    }

    private void validateCommand(NetworkPortCommand command) throws BACnetServiceException {
        if (get(PropertyIdentifier.command) != NetworkPortCommand.idle) {
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.busy);
        }
        if (command == NetworkPortCommand.idle) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        }
        if (isChanged() && !command.isOneOf(
                NetworkPortCommand.discardChanges, NetworkPortCommand.validateChanges)) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidValueInThisState);
        }
        if (!isInService() && !command.isOneOf(
                NetworkPortCommand.restartPort, NetworkPortCommand.disconnect, NetworkPortCommand.discardChanges)) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        }

        validateCommandInternal(command);
    }

    /**
     * Subclasses should implement as required.
     *
     * @param command the given command.
     */
    protected void validateCommandInternal(NetworkPortCommand command) throws BACnetServiceException {
        if (command == NetworkPortCommand.renewDhcp) {
            NetworkType networkType = get(PropertyIdentifier.networkType);
            if (networkType == NetworkType.ipv4 || networkType == NetworkType.ipv6) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.optionalFunctionalityNotSupported);
            }
        }
        if (command == NetworkPortCommand.restartSubordinateDiscovery) {
            // Per 12.56.14, writing this value to a non-MS/TP port returns VALUE_OUT_OF_RANGE, while an
            // MS/TP port without subordinate proxy support returns OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED.
            // Subclasses that support subordinate proxying override this method. The non-MS/TP case falls
            // through to the VALUE_OUT_OF_RANGE below.
            NetworkType networkType = get(PropertyIdentifier.networkType);
            if (networkType == NetworkType.mstp) {
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.optionalFunctionalityNotSupported);
            }
        }
        if (command.isOneOf(NetworkPortCommand.restartAutonegotiation, NetworkPortCommand.restartPort,
                NetworkPortCommand.restartDeviceDiscovery, NetworkPortCommand.generateCsrFile)) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.optionalFunctionalityNotSupported);
        }
        if (command.isOneOf(NetworkPortCommand.discardChanges, NetworkPortCommand.validateChanges)) {
            return;
        }
        throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
    }

    /**
     * All properties for which the changes pending flag should be set on.
     */
    private static final Set<PropertyIdentifier> CHANGES_PENDING_PROPERTIES = Set.of(
            PropertyIdentifier.referencePort,
            PropertyIdentifier.additionalReferencePorts,
            PropertyIdentifier.networkNumber,
            PropertyIdentifier.networkNumberQuality, // A readonly property, but the value is controlled by setting the networkNumber (12.56.13).
            PropertyIdentifier.macAddress,
            PropertyIdentifier.linkSpeed,
            PropertyIdentifier.linkSpeedAutonegotiate,
            PropertyIdentifier.networkInterfaceName,
            PropertyIdentifier.bacnetIpMode,
            PropertyIdentifier.bacnetIpUdpPort,
            PropertyIdentifier.bacnetIpMulticastAddress,
            PropertyIdentifier.bacnetIpNatTraversal,
            PropertyIdentifier.bacnetIpGlobalAddress,
            PropertyIdentifier.bbmdBroadcastDistributionTable,
            PropertyIdentifier.bbmdAcceptFdRegistrations,
            PropertyIdentifier.fdBbmdAddress,
            PropertyIdentifier.fdSubscriptionLifetime,
            PropertyIdentifier.ipAddress,
            PropertyIdentifier.ipSubnetMask,
            PropertyIdentifier.ipDefaultGateway,
            PropertyIdentifier.ipDnsServer,
            PropertyIdentifier.ipDhcpEnable,
            PropertyIdentifier.virtualMacAddressTable,
            PropertyIdentifier.bacnetIpv6Mode,
            PropertyIdentifier.bacnetIpv6UdpPort,
            PropertyIdentifier.bacnetIpv6MulticastAddress,
            PropertyIdentifier.ipv6Address,
            PropertyIdentifier.ipv6PrefixLength,
            PropertyIdentifier.ipv6DefaultGateway,
            PropertyIdentifier.ipv6DnsServer,
            PropertyIdentifier.ipv6AutoAddressingEnable,
            PropertyIdentifier.ipv6ZoneIndex,
            PropertyIdentifier.maxManager,
            PropertyIdentifier.maxInfoFrames,
            PropertyIdentifier.scPrimaryHubUri,
            PropertyIdentifier.scFailoverHubUri,
            PropertyIdentifier.scHubFunctionEnable,
            PropertyIdentifier.scHubFunctionBinding,
            PropertyIdentifier.scDirectConnectInitiateEnable,
            PropertyIdentifier.scDirectConnectAcceptEnable,
            PropertyIdentifier.scDirectConnectBinding
    );

    /**
     * Override the setting of a value if it is a "changes pending" property. These only become actual object
     * properties when the device is reinitialized with a "warm start" or "activate changes". But, these values need to
     * be readable, and so the {@link #get} method is overridden too.
     */
    @Override
    protected void set(PropertyIdentifier pid, Encodable value) {
        if (isInitialized() && CHANGES_PENDING_PROPERTIES.contains(pid)) {
            var existing = properties.get(pid);
            if (Objects.equals(existing, value)) {
                // Writing the currently active value. Remove the pending change if it exists.
                pendingChanges.remove(pid);
            } else {
                pendingChanges.put(pid, value);
            }
        } else {
            super.set(pid, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Encodable> T get(PropertyIdentifier pid) {
        return pendingChanges.containsKey(pid) ? (T) pendingChanges.get(pid) : super.get(pid);
    }

    /**
     * Return the current value of the object for this property, ignoring pending changes.
     */
    public <T extends Encodable> T getActive(PropertyIdentifier pid) {
        return super.get(pid);
    }

    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);

        if (pid.equals(PropertyIdentifier.command)) {
            if (newValue == NetworkPortCommand.discardChanges) {
                executeCommand(() -> {
                    discardChanges();
                    pendingChanges.clear();
                });
            } else if (newValue == NetworkPortCommand.validateChanges) {
                executeCommand(() -> {
                    var health = validateChanges();
                    writePropertyInternal(PropertyIdentifier.commandValidationResult, health);
                });
            }
        } else if (pid.equals(PropertyIdentifier.networkNumber)) {
            NetworkNumberQuality quality;
            if (pendingChanges.containsKey(PropertyIdentifier.networkNumber)) {
                // If there is a networkNumber value in the pending changes, then also set a quality
                quality = newValue.equals(Unsigned16.ZERO)
                        ? NetworkNumberQuality.unknown
                        : NetworkNumberQuality.configured;
            } else {
                // Otherwise, unset the pending quality value by setting the current config value if it exists.
                quality = (NetworkNumberQuality) properties.get(PropertyIdentifier.networkNumberQuality);
                if (quality == null) {
                    quality = NetworkNumberQuality.unknown;
                }
            }
            writePropertyInternal(PropertyIdentifier.networkNumberQuality, quality);
        }
    }

    protected void discardChanges() {
        // Override as needed.
    }

    /**
     * Asynchronously executes a command, and then resets the command property to back idle.
     *
     * @param runnable the task to execute
     */
    protected void executeCommand(Runnable runnable) {
        getLocalDevice().execute(() -> {
            try {
                runnable.run();
            } finally {
                writePropertyInternal(PropertyIdentifier.command, NetworkPortCommand.idle);
            }
        });
    }
}
