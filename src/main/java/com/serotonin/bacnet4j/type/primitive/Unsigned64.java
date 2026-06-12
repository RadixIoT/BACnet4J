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

package com.serotonin.bacnet4j.type.primitive;

import java.math.BigInteger;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Unsigned64 extends UnsignedInteger {
    private static final BigInteger MAX = new BigInteger("18446744073709551615");

    public Unsigned64(final int value) {
        super(value);
    }

    public Unsigned64(final BigInteger value) {
        super(value);
        if (value.compareTo(MAX) > 0)
            throw new IllegalArgumentException("Value cannot be greater than " + MAX);
    }

    public Unsigned64(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    @Override
    public void validate() throws BACnetServiceException {
        super.validate();
        if (super.bigIntegerValue().compareTo(MAX) > 0) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        }
    }
}
