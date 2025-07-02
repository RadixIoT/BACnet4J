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
import com.serotonin.bacnet4j.type.enumerated.AuthenticationFactorType;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthenticationFactor extends BaseType {
    private final AuthenticationFactorType formatType;
    private final UnsignedInteger formatClass;
    private final OctetString value;

    public AuthenticationFactor(final AuthenticationFactorType formatType, final UnsignedInteger formatClass,
            final OctetString value) {
        this.formatType = formatType;
        this.formatClass = formatClass;
        this.value = value;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, formatType, 0);
        write(queue, formatClass, 1);
        write(queue, value, 2);
    }

    public AuthenticationFactor(final ByteQueue queue) throws BACnetException {
        formatType = read(queue, AuthenticationFactorType.class, 0);
        formatClass = read(queue, UnsignedInteger.class, 1);
        value = read(queue, OctetString.class, 2);
    }

    public AuthenticationFactorType getFormatType() {
        return formatType;
    }

    public UnsignedInteger getFormatClass() {
        return formatClass;
    }

    public OctetString getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (formatClass == null ? 0 : formatClass.hashCode());
        result = prime * result + (formatType == null ? 0 : formatType.hashCode());
        result = prime * result + (value == null ? 0 : value.hashCode());
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
        final AuthenticationFactor other = (AuthenticationFactor) obj;
        if (formatClass == null) {
            if (other.formatClass != null)
                return false;
        } else if (!formatClass.equals(other.formatClass))
            return false;
        if (formatType == null) {
            if (other.formatType != null)
                return false;
        } else if (!formatType.equals(other.formatType))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AuthenticationFactor [formatType=" + formatType + ", formatClass=" + formatClass + ", value=" + value + ']';
    }
}
