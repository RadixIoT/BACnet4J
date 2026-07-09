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

import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.SilencedState;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public interface LifeSafety {
    /**
     * Handles an incoming LifeSafetyOperation. The default implementation supports the silence
     * and unsilence variants; any other operation is rejected with
     * {@code OBJECT / VALUE_OUT_OF_RANGE} per addendum 135-2016bt-2 (Clause 13.13.1.3.1).
     *
     * <p>Overrides that add support for further operations should follow the same error mapping:
     * throw {@code OBJECT / VALUE_OUT_OF_RANGE} for operations the object does not implement, and
     * {@code OBJECT / INVALID_OPERATION_IN_THIS_STATE} for operations that are otherwise
     * supported but inapplicable in the object's current state.
     *
     * @param from                        request sender
     * @param requestingProcessIdentifier requesting process identifier
     * @param requestingSource            requesting source
     * @param request                     the request
     */
    default void handleLifeSafetyOperation(Address from, UnsignedInteger requestingProcessIdentifier,
            CharacterString requestingSource, LifeSafetyOperation request) throws BACnetServiceException {
        if (request.equals(LifeSafetyOperation.silence)) {
            writePropertyInternal(PropertyIdentifier.silenced, SilencedState.allSilenced);
        } else if (request.equals(LifeSafetyOperation.silenceAudible)) {
            writePropertyInternal(PropertyIdentifier.silenced, SilencedState.audibleSilenced);
        } else if (request.equals(LifeSafetyOperation.silenceVisual)) {
            writePropertyInternal(PropertyIdentifier.silenced, SilencedState.visibleSilenced);
        } else if (request.isOneOf(LifeSafetyOperation.unsilence, LifeSafetyOperation.unsilenceAudible,
                LifeSafetyOperation.unsilenceVisual)) {
            writePropertyInternal(PropertyIdentifier.silenced, SilencedState.unsilenced);
        } else {
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.valueOutOfRange);
        }
    }

    BACnetObject writePropertyInternal(PropertyIdentifier pid, Encodable value);
}
