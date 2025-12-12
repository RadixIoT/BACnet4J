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

package com.serotonin.bacnet4j.type.error;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class VTCloseError extends BaseError {
    private final ErrorClassAndCode errorType;
    private final SequenceOf<UnsignedInteger> listOfVTSessionIdentifiers;

    public VTCloseError(final ErrorClassAndCode errorType,
            final SequenceOf<UnsignedInteger> listOfVTSessionIdentifiers) {
        this.errorType = errorType;
        this.listOfVTSessionIdentifiers = listOfVTSessionIdentifiers;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, errorType, 0);
        writeOptional(queue, listOfVTSessionIdentifiers, 1);
    }

    public VTCloseError(final ByteQueue queue) throws BACnetException {
        errorType = read(queue, ErrorClassAndCode.class, 0);
        listOfVTSessionIdentifiers = readOptionalSequenceOf(queue, UnsignedInteger.class, 1);
    }

    public ErrorClassAndCode getErrorType() {
        return errorType;
    }

    public SequenceOf<UnsignedInteger> getListOfVTSessionIdentifiers() {
        return listOfVTSessionIdentifiers;
    }

    @Override
    public ErrorClassAndCode getErrorClassAndCode() {
        return errorType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (errorType == null ? 0 : errorType.hashCode());
        result = prime * result + (listOfVTSessionIdentifiers == null ? 0 : listOfVTSessionIdentifiers.hashCode());
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
        final VTCloseError other = (VTCloseError) obj;
        if (errorType == null) {
            if (other.errorType != null)
                return false;
        } else if (!errorType.equals(other.errorType))
            return false;
        if (listOfVTSessionIdentifiers == null) {
            if (other.listOfVTSessionIdentifiers != null)
                return false;
        } else if (!listOfVTSessionIdentifiers.equals(other.listOfVTSessionIdentifiers))
            return false;
        return true;
    }
}
