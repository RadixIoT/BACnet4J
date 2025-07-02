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

package com.serotonin.bacnet4j.type.constructed;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SecurityKeySet extends BaseType {
    private final Unsigned8 keyRevision;
    private final DateTime activationTime;
    private final DateTime expirationTime;
    private final SequenceOf<KeyIdentifier> keyIds;

    public SecurityKeySet(final Unsigned8 keyRevision, final DateTime activationTime, final DateTime expirationTime,
            final SequenceOf<KeyIdentifier> keyIds) {
        this.keyRevision = keyRevision;
        this.activationTime = activationTime;
        this.expirationTime = expirationTime;
        this.keyIds = keyIds;
    }

    public Unsigned8 getKeyRevision() {
        return keyRevision;
    }

    public DateTime getActivationTime() {
        return activationTime;
    }

    public DateTime getExpirationTime() {
        return expirationTime;
    }

    public SequenceOf<KeyIdentifier> getKeyIds() {
        return keyIds;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, keyRevision, 0);
        write(queue, activationTime, 1);
        write(queue, expirationTime, 2);
        write(queue, keyIds, 3);
    }

    @Override
    public String toString() {
        return "SecurityKeySet [keyRevision=" + keyRevision + ", activationTime=" + activationTime + ", expirationTime="
                + expirationTime + ", keyIds=" + keyIds + "]";
    }

    public SecurityKeySet(final ByteQueue queue) throws BACnetException {
        keyRevision = read(queue, Unsigned8.class, 0);
        activationTime = read(queue, DateTime.class, 1);
        expirationTime = read(queue, DateTime.class, 2);
        keyIds = readSequenceOf(queue, KeyIdentifier.class, 3);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (activationTime == null ? 0 : activationTime.hashCode());
        result = prime * result + (expirationTime == null ? 0 : expirationTime.hashCode());
        result = prime * result + (keyIds == null ? 0 : keyIds.hashCode());
        result = prime * result + (keyRevision == null ? 0 : keyRevision.hashCode());
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
        final SecurityKeySet other = (SecurityKeySet) obj;
        if (activationTime == null) {
            if (other.activationTime != null)
                return false;
        } else if (!activationTime.equals(other.activationTime))
            return false;
        if (expirationTime == null) {
            if (other.expirationTime != null)
                return false;
        } else if (!expirationTime.equals(other.expirationTime))
            return false;
        if (keyIds == null) {
            if (other.keyIds != null)
                return false;
        } else if (!keyIds.equals(other.keyIds))
            return false;
        if (keyRevision == null) {
            if (other.keyRevision != null)
                return false;
        } else if (!keyRevision.equals(other.keyRevision))
            return false;
        return true;
    }
}
