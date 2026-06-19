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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SCHubConnection;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Network Port object for a BACnet/SC port (Clauses 12.56 and Annex AB).
 *
 * <p>The properties in Table 12-71.8 not initialized here are either optional (and left to product builders) or
 * dynamic state owned by the BACnet/SC stack (e.g., {@code SC_Hub_Connector_State}, hub connection status lists,
 * failed connection requests).
 */
public class SecureConnectNetworkPortObject extends NetworkPortObject {
    public SecureConnectNetworkPortObject(
            LocalDevice localDevice,
            int instanceNumber,
            String name,
            OctetString vmac,
            CharacterString primaryHubUri,
            CharacterString failoverHubUri,
            ObjectIdentifier operationalCertificateFile,
            BACnetArray<ObjectIdentifier> issuerCertificateFiles,
            ObjectIdentifier certificateSigningRequestFile) {
        super(localDevice, instanceNumber, name, false, NetworkType.secureConnect,
                ProtocolLevel.bacnetApplication, Set.of(
                        PropertyIdentifier.scHubFunctionEnable,
                        PropertyIdentifier.scHubFunctionAcceptUris,
                        PropertyIdentifier.scHubFunctionBinding,
                        PropertyIdentifier.scDirectConnectInitiateEnable,
                        PropertyIdentifier.scDirectConnectAcceptEnable,
                        PropertyIdentifier.scDirectConnectAcceptUris,
                        PropertyIdentifier.scDirectConnectBinding
                ));

        writePropertyInternal(PropertyIdentifier.apduLength, MaxApduLength.UP_TO_1476.getMaxLength());
        writePropertyInternal(PropertyIdentifier.macAddress, vmac);
        writePropertyInternal(PropertyIdentifier.maxBvlcLengthAccepted, new UnsignedInteger(1497));
        writePropertyInternal(PropertyIdentifier.maxNpduLengthAccepted, new UnsignedInteger(61327));

        writePropertyInternal(PropertyIdentifier.scPrimaryHubUri, primaryHubUri);
        writePropertyInternal(PropertyIdentifier.scFailoverHubUri, failoverHubUri);
        // Defaults per spec; SC_Minimum/Maximum_Reconnect_Time have ranges of 2..300 and 2..600 respectively, but
        // the spec specifies no defaults. The values below are reasonable starting points.
        writePropertyInternal(PropertyIdentifier.scMinimumReconnectTime, new UnsignedInteger(2));
        writePropertyInternal(PropertyIdentifier.scMaximumReconnectTime, new UnsignedInteger(30));
        // Recommended defaults per 12.56.84, 12.56.85, 12.56.86.
        writePropertyInternal(PropertyIdentifier.scConnectWaitTimeout, new UnsignedInteger(10));
        writePropertyInternal(PropertyIdentifier.scDisconnectWaitTimeout, new UnsignedInteger(10));
        writePropertyInternal(PropertyIdentifier.scHeartbeatTimeout, new UnsignedInteger(300));

        writePropertyInternal(PropertyIdentifier.scHubConnectorState, evaluateSCHubConnectorState());
        writePropertyInternal(PropertyIdentifier.scPrimaryHubConnectionStatus, evaluateSCPrimaryHubConnectionStatus());
        writePropertyInternal(PropertyIdentifier.scFailoverHubConnectionStatus,
                evaluateSCFailoverHubConnectionStatus());
        writePropertyInternal(PropertyIdentifier.scHubFunctionEnable, Boolean.FALSE);
        writePropertyInternal(PropertyIdentifier.scDirectConnectInitiateEnable, Boolean.FALSE);
        writePropertyInternal(PropertyIdentifier.scDirectConnectAcceptEnable, Boolean.FALSE);

        writePropertyInternal(PropertyIdentifier.operationalCertificateFile, operationalCertificateFile);
        writePropertyInternal(PropertyIdentifier.issuerCertificateFiles, issuerCertificateFiles);
        writePropertyInternal(PropertyIdentifier.certificateSigningRequestFile, certificateSigningRequestFile);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        PropertyIdentifier pid = value.getPropertyIdentifier();
        if (pid.equals(PropertyIdentifier.scPrimaryHubUri) || pid.equals(PropertyIdentifier.scFailoverHubUri)) {
            CharacterString cs = value.getValue();
            String s = cs.getValue();
            if (!s.isEmpty()) {
                try {
                    new URI(s);
                } catch (URISyntaxException e) {
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
                }
            }
        } else if (pid.equals(PropertyIdentifier.scMinimumReconnectTime)) {
            validateUnsignedRange(value.getValue(), 2, 300);
        } else if (pid.equals(PropertyIdentifier.scMaximumReconnectTime)) {
            validateUnsignedRange(value.getValue(), 2, 600);
        } else if (pid.equals(PropertyIdentifier.scConnectWaitTimeout)) {
            validateUnsignedRange(value.getValue(), 5, 300);
        } else if (pid.equals(PropertyIdentifier.scDisconnectWaitTimeout)) {
            validateUnsignedRange(value.getValue(), 5, 300);
        } else if (pid.equals(PropertyIdentifier.scHeartbeatTimeout)) {
            validateUnsignedRange(value.getValue(), 3, 300);
        }
        return false;
    }

    protected void validateUnsignedRange(UnsignedInteger value, int min, int max) throws BACnetServiceException {
        var i = value.intValue();
        if (i < min || i > max) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        }
    }

    protected SCHubConnectorState evaluateSCHubConnectorState() {
        return SCHubConnectorState.noHubConnection;
    }

    protected SCHubConnection evaluateSCPrimaryHubConnectionStatus() {
        return new SCHubConnection(SCConnectionState.notConnected, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED);

    }

    protected SCHubConnection evaluateSCFailoverHubConnectionStatus() {
        return new SCHubConnection(SCConnectionState.notConnected, DateTime.UNSPECIFIED, DateTime.UNSPECIFIED);
    }
}
