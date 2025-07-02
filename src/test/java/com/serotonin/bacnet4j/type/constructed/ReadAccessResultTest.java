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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ReadAccessResultTest {
    @Test
    public void decoding() throws BACnetException {
        final String hex = "0c020000021e294c4ec402000002c4008000004f1f";

        final ReadAccessResult rar = new ReadAccessResult(new ByteQueue(hex));

        assertEquals(ObjectType.device, rar.getObjectIdentifier().getObjectType());
        assertEquals(2, rar.getObjectIdentifier().getInstanceNumber());
        assertEquals(1, rar.getListOfResults().getCount());
        assertEquals(PropertyIdentifier.objectList, rar.getListOfResults().getBase1(1).getPropertyIdentifier());
        assertEquals(null, rar.getListOfResults().getBase1(1).getPropertyArrayIndex());
        assertEquals(true, rar.getListOfResults().getBase1(1).getReadResult().isa(SequenceOf.class));

        final SequenceOf<ObjectIdentifier> oids = rar.getListOfResults().getBase1(1).getReadResult().getDatum();
        assertEquals(2, oids.getCount());
        assertEquals(new ObjectIdentifier(ObjectType.device, 2), oids.getBase1(1));
        assertEquals(new ObjectIdentifier(ObjectType.analogValue, 0), oids.getBase1(2));
    }
}
