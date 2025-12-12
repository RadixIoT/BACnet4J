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
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class CreateObjectError extends BaseError {
    private final ErrorClassAndCode errorType;
    private final UnsignedInteger firstFailedElementNumber;

    public CreateObjectError(final ErrorClassAndCode errorType, final UnsignedInteger firstFailedElementNumber) {
        this.errorType = errorType;
        this.firstFailedElementNumber = firstFailedElementNumber;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, errorType, 0);
        write(queue, firstFailedElementNumber, 1);
    }

    public CreateObjectError(final ByteQueue queue) throws BACnetException {
        errorType = read(queue, ErrorClassAndCode.class, 0);
        firstFailedElementNumber = read(queue, UnsignedInteger.class, 1);
    }

    public ErrorClassAndCode getErrorType() {
        return errorType;
    }

    public UnsignedInteger getFirstFailedElementNumber() {
        return firstFailedElementNumber;
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
        result = prime * result + (firstFailedElementNumber == null ? 0 : firstFailedElementNumber.hashCode());
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
        final CreateObjectError other = (CreateObjectError) obj;
        if (errorType == null) {
            if (other.errorType != null)
                return false;
        } else if (!errorType.equals(other.errorType))
            return false;
        if (firstFailedElementNumber == null) {
            if (other.firstFailedElementNumber != null)
                return false;
        } else if (!firstFailedElementNumber.equals(other.firstFailedElementNumber))
            return false;
        return true;
    }
}
