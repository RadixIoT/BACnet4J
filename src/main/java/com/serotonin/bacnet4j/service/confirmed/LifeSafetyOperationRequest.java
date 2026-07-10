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

package com.serotonin.bacnet4j.service.confirmed;

import java.util.Objects;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.LifeSafety;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class LifeSafetyOperationRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 27;

    private final UnsignedInteger requestingProcessIdentifier;
    private final CharacterString requestingSource;
    private final LifeSafetyOperation request;
    private final ObjectIdentifier objectIdentifier;

    public LifeSafetyOperationRequest(UnsignedInteger requestingProcessIdentifier, CharacterString requestingSource,
            LifeSafetyOperation request, ObjectIdentifier objectIdentifier) {
        this.requestingProcessIdentifier = requestingProcessIdentifier;
        this.requestingSource = requestingSource;
        this.request = request;
        this.objectIdentifier = objectIdentifier;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(LocalDevice localDevice, Address from) throws BACnetException {
        try {
            if (objectIdentifier != null) {
                BACnetObject bo = localDevice.getObjectRequired(objectIdentifier);
                handleForObject(from, bo, true);
            } else {
                for (BACnetObject bo : localDevice.getLocalObjects()) {
                    handleForObject(from, bo, false);
                }
            }
        } catch (BACnetServiceException e) {
            throw new BACnetErrorException(getChoiceId(), e.getErrorClass(), e.getErrorCode());
        }
        return null;
    }

    private void handleForObject(Address from, BACnetObject bo, boolean throwOnBadType) throws BACnetServiceException {
        if (bo instanceof LifeSafety ls) {
            ls.handleLifeSafetyOperation(from, requestingProcessIdentifier, requestingSource, request);
        } else if (throwOnBadType) {
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.optionalFunctionalityNotSupported);
        }
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, requestingProcessIdentifier, 0);
        write(queue, requestingSource, 1);
        write(queue, request, 2);
        writeOptional(queue, objectIdentifier, 3);
    }

    LifeSafetyOperationRequest(ByteQueue queue) throws BACnetException {
        requestingProcessIdentifier = read(queue, UnsignedInteger.class, 0);
        requestingSource = read(queue, CharacterString.class, 1);
        request = read(queue, LifeSafetyOperation.class, 2);
        objectIdentifier = readOptional(queue, ObjectIdentifier.class, 3);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        LifeSafetyOperationRequest that = (LifeSafetyOperationRequest) o;
        return Objects.equals(requestingProcessIdentifier,
                that.requestingProcessIdentifier) && Objects.equals(requestingSource,
                that.requestingSource) && Objects.equals(request, that.request) && Objects.equals(
                objectIdentifier, that.objectIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestingProcessIdentifier, requestingSource, request, objectIdentifier);
    }
}
