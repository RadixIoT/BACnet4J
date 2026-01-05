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

package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class MultistateValueObjectTest extends AbstractTest {
    @Test
    public void initialization() throws Exception {
        final MultistateValueObject mv = new MultistateValueObject(d1, 1, "mv1", 7, null, 1, false);
        // Allowed to create with a number of states but no text.
        assertEquals(new StatusFlags(false, false, false, false), mv.get(PropertyIdentifier.statusFlags));

        // Not allowed to create with 0 states.
        assertThrows(IllegalArgumentException.class, () -> new MultistateValueObject(d1, 2, "mv2", 0, null, 1, false));
    }

    @Test
    public void inconsistentStateText() {
        // Can't create when number of states doesn't match the length of state text.
        assertThrows(IllegalArgumentException.class,
                () -> new MultistateValueObject(d1, 0, "mv0", 7, new BACnetArray<>(new CharacterString("a")), 1, true));
    }

    @Test
    public void missingStateText() throws Exception {
        // Allowed to create with a number of states but no text.
        final MultistateValueObject mv = new MultistateValueObject(d1, 0, "mv0", 7, null, 1, true);

        // If the state text array is written, change the number of states to match.
        mv.writeProperty(null,
                new PropertyValue(PropertyIdentifier.stateText, new BACnetArray<>(new CharacterString("a"))));
        assertEquals(new UnsignedInteger(1), mv.get(PropertyIdentifier.numberOfStates));
    }

    @Test
    public void stateText() throws Exception {
        final MultistateValueObject mv = new MultistateValueObject(d1, 0, "mv0", 7, null, 1, true);

        mv.writeProperty(null,
                new PropertyValue(PropertyIdentifier.stateText,
                        new BACnetArray<>(new CharacterString("a"), new CharacterString("b"), new CharacterString("c"),
                                new CharacterString("d"), new CharacterString("e"), new CharacterString("f"),
                                new CharacterString("g"))));

        mv.writeProperty(null, new PropertyValue(PropertyIdentifier.numberOfStates, new UnsignedInteger(6)));
        assertEquals(
                new BACnetArray<>(new CharacterString("a"), new CharacterString("b"), new CharacterString("c"),
                        new CharacterString("d"), new CharacterString("e"), new CharacterString("f")),
                mv.get(PropertyIdentifier.stateText));

        mv.writeProperty(null, new PropertyValue(PropertyIdentifier.numberOfStates, new UnsignedInteger(8)));
        assertEquals(new BACnetArray<>(new CharacterString("a"), new CharacterString("b"), new CharacterString("c"),
                new CharacterString("d"), new CharacterString("e"), new CharacterString("f"), CharacterString.EMPTY,
                CharacterString.EMPTY), mv.get(PropertyIdentifier.stateText));
    }
}
