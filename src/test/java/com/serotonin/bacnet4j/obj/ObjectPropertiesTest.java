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
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;

public class ObjectPropertiesTest {
    @Test
    public void propertyDefinition() {
        // Reliability only ever has a type of Reliability
        final PropertyTypeDefinition def = ObjectProperties.getPropertyTypeDefinition(PropertyIdentifier.reliability);
        assertEquals(Reliability.class, def.getClazz());
        assertEquals(null, def.getInnerType());
        assertEquals(PropertyIdentifier.reliability, def.getPropertyIdentifier());

        // Present value takes on different values in different objects.
        assertNull(ObjectProperties.getPropertyTypeDefinition(PropertyIdentifier.presentValue));
    }
}
