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

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.SilencedState;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public interface LifeSafety {
    /**
     * Override as required. Default implementation implements minimal behaviour.
     *
     * @param from
     * @param requestingProcessIdentifier
     * @param requestingSource
     * @param request
     */
    default void handleLifeSafetyOperation(final Address from, final UnsignedInteger requestingProcessIdentifier,
            final CharacterString requestingSource, final LifeSafetyOperation request) {
        if (request.equals(LifeSafetyOperation.silence)) {
            writePropertyInternal(PropertyIdentifier.silenced, SilencedState.allSilenced);
        } else if (request.equals(LifeSafetyOperation.silenceAudible)) {
            writePropertyInternal(PropertyIdentifier.silenced, SilencedState.audibleSilenced);
        } else if (request.equals(LifeSafetyOperation.silenceVisual)) {
            writePropertyInternal(PropertyIdentifier.silenced, SilencedState.visibleSilenced);
        } else if (request.isOneOf(LifeSafetyOperation.unsilence, LifeSafetyOperation.unsilenceAudible,
                LifeSafetyOperation.unsilenceVisual)) {
            writePropertyInternal(PropertyIdentifier.silenced, SilencedState.unsilenced);
        }
    }

    BACnetObject writePropertyInternal(final PropertyIdentifier pid, final Encodable value);
}
