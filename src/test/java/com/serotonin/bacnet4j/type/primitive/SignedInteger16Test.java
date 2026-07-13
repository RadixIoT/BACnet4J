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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SignedInteger16Test {
    @Test
    public void constructorAcceptsInRangeValues() {
        assertEquals(0, new SignedInteger16(0).intValue());
        assertEquals(1, new SignedInteger16(1).intValue());
        assertEquals(-1, new SignedInteger16(-1).intValue());
        assertEquals(Short.MAX_VALUE, new SignedInteger16(Short.MAX_VALUE).intValue());
        assertEquals(Short.MIN_VALUE, new SignedInteger16(Short.MIN_VALUE).intValue());
    }

    @Test
    public void constructorRejectsAboveMax() {
        assertThrows(IllegalArgumentException.class,
                () -> new SignedInteger16(Short.MAX_VALUE + 1));
    }

    @Test
    public void constructorRejectsBelowMin() {
        assertThrows(IllegalArgumentException.class,
                () -> new SignedInteger16(Short.MIN_VALUE - 1));
    }

    @Test
    public void wireEncodingMatchesSignedInteger() {
        // The wire encoding for SignedInteger16 must be identical to SignedInteger — ck-1 defines
        // Integer16 as an ASN.1 subtype of Integer with no encoding difference.
        ByteQueue a = new ByteQueue();
        new SignedInteger16(12345).write(a);
        ByteQueue b = new ByteQueue();
        new SignedInteger(12345).write(b);
        assertEquals(b, a);
    }

    @Test
    public void validateAcceptsDecodedInRangeValue() throws Exception {
        // Encode via the base class then decode as SignedInteger16 and validate.
        SignedInteger source = new SignedInteger(100);
        ByteQueue queue = new ByteQueue();
        source.write(queue);
        SignedInteger16 decoded = new SignedInteger16(queue);
        decoded.validate();
        assertEquals(100, decoded.intValue());
    }

    @Test
    public void validateRejectsDecodedAboveMax() throws Exception {
        // A wire value that fits SignedInteger but exceeds Integer16 must fail validate().
        SignedInteger source = new SignedInteger(Short.MAX_VALUE + 1);
        ByteQueue queue = new ByteQueue();
        source.write(queue);
        SignedInteger16 decoded = new SignedInteger16(queue);
        BACnetServiceException ex = assertThrows(BACnetServiceException.class, decoded::validate);
        assertEquals(ErrorClass.property, ex.getErrorClass());
        assertEquals(ErrorCode.valueOutOfRange, ex.getErrorCode());
    }

    @Test
    public void validateRejectsDecodedBelowMin() throws Exception {
        SignedInteger source = new SignedInteger(Short.MIN_VALUE - 1);
        ByteQueue queue = new ByteQueue();
        source.write(queue);
        SignedInteger16 decoded = new SignedInteger16(queue);
        BACnetServiceException ex = assertThrows(BACnetServiceException.class, decoded::validate);
        assertEquals(ErrorClass.property, ex.getErrorClass());
        assertEquals(ErrorCode.valueOutOfRange, ex.getErrorCode());
    }
}
