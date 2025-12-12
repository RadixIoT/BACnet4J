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
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthenticationFactorFormat extends BaseType {
    private final AuthenticationFactorType formatType;
    private final Unsigned16 vendorId;
    private final Unsigned16 vendorFormat;

    public AuthenticationFactorFormat(final AuthenticationFactorType formatType, final Unsigned16 vendorId,
            final Unsigned16 vendorFormat) {
        super();
        this.formatType = formatType;
        this.vendorId = vendorId;
        this.vendorFormat = vendorFormat;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, formatType, 0);
        writeOptional(queue, vendorId, 1);
        writeOptional(queue, vendorFormat, 2);
    }

    public AuthenticationFactorFormat(final ByteQueue queue) throws BACnetException {
        formatType = read(queue, AuthenticationFactorType.class, 0);
        vendorId = read(queue, Unsigned16.class, 1);
        vendorFormat = read(queue, Unsigned16.class, 2);
    }

    public AuthenticationFactorType getFormatType() {
        return formatType;
    }

    public Unsigned16 getVendorId() {
        return vendorId;
    }

    public Unsigned16 getVendorFormat() {
        return vendorFormat;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (formatType == null ? 0 : formatType.hashCode());
        result = prime * result + (vendorFormat == null ? 0 : vendorFormat.hashCode());
        result = prime * result + (vendorId == null ? 0 : vendorId.hashCode());
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
        final AuthenticationFactorFormat other = (AuthenticationFactorFormat) obj;
        if (formatType == null) {
            if (other.formatType != null)
                return false;
        } else if (!formatType.equals(other.formatType))
            return false;
        if (vendorFormat == null) {
            if (other.vendorFormat != null)
                return false;
        } else if (!vendorFormat.equals(other.vendorFormat))
            return false;
        if (vendorId == null) {
            if (other.vendorId != null)
                return false;
        } else if (!vendorId.equals(other.vendorId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AuthenticationFactorFormat [formatType=" + formatType + ", vendorId=" + vendorId + ", vendorFormat=" + vendorFormat + ']';
    }
}
