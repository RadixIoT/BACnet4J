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

package com.serotonin.bacnet4j.type.notificationParameters;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.AmbiguousValue;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfStatusFlagsNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 18;

    private final Encodable presentValue;
    private final StatusFlags referencedFlags;

    public ChangeOfStatusFlagsNotif(final Encodable presentValue, final StatusFlags referencedFlags) {
        this.presentValue = presentValue;
        this.referencedFlags = referencedFlags;
    }

    @Override
    public void write(final ByteQueue queue) {
        writeANY(queue, presentValue, 0);
        write(queue, referencedFlags, 1);
    }

    public ChangeOfStatusFlagsNotif(final ByteQueue queue) throws BACnetException {
        presentValue = new AmbiguousValue(queue, 0);
        referencedFlags = read(queue, StatusFlags.class, 1);
    }

    public Encodable getPresentValue() {
        return presentValue;
    }

    public StatusFlags getReferencedFlags() {
        return referencedFlags;
    }

    @Override
    public String toString() {
        return "ChangeOfStatusFlagsNotif[ presentValue=" + presentValue + ", referencedFlags=" + referencedFlags + ']';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (presentValue == null ? 0 : presentValue.hashCode());
        result = prime * result + (referencedFlags == null ? 0 : referencedFlags.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ChangeOfStatusFlagsNotif other = (ChangeOfStatusFlagsNotif) obj;
        if (presentValue == null) {
            if (other.presentValue != null)
                return false;
        } else if (!presentValue.equals(other.presentValue))
            return false;
        if (referencedFlags == null) {
            if (other.referencedFlags != null)
                return false;
        } else if (!referencedFlags.equals(other.referencedFlags))
            return false;
        return true;
    }
}
