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

/**
 * Represents the Integer16 type — an Integer constrained to the range -32768..32767 per
 * 135-2020ck-1 Clause 21.5.
 */
public class SignedInteger16 extends SignedInteger {
    private static final int MIN = Short.MIN_VALUE;
    private static final int MAX = Short.MAX_VALUE;
    private static final BigInteger BIGMIN = BigInteger.valueOf(MIN);
    private static final BigInteger BIGMAX = BigInteger.valueOf(MAX);

    public SignedInteger16(int value) {
        super(value);
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException("Value out of range for Integer16: " + value);
        }
    }

    public SignedInteger16(ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    @Override
    public void validate() throws BACnetServiceException {
        super.validate();
        BigInteger value = bigIntegerValue();
        if (value.compareTo(BIGMIN) < 0 || value.compareTo(BIGMAX) > 0) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        }
    }
}
