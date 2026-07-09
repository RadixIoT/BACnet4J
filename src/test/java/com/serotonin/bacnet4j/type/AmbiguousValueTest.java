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

package com.serotonin.bacnet4j.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AmbiguousValueTest {
    @Test
    public void recognizesContextualNullAsNull() throws BACnetException {
        var content = new ByteQueue("4e004f");
        var amb = new AmbiguousValue(content, 4);
        assertTrue(amb.isNull());
        assertEquals(Null.instance, amb.convertTo(Null.class));
    }

    @Test
    public void recognizesContextualBlankAsNull() throws BACnetException {
        var content = new ByteQueue("4e4f");
        var amb = new AmbiguousValue(content, 4);
        assertTrue(amb.isNull());
    }

    @Test
    public void recognizesContextualIntAsNotNull() throws BACnetException {
        var content = new ByteQueue("ae210daf");
        var amb = new AmbiguousValue(content, 0xa);
        assertFalse(amb.isNull());
        assertEquals(new UnsignedInteger(13), amb.convertTo(UnsignedInteger.class));
    }
}
