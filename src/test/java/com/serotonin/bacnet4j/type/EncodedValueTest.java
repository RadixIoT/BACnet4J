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

package com.serotonin.bacnet4j.type;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class EncodedValueTest {
    @Test
    public void constructed() throws Exception {
        final EncodedValue original = new EncodedValue(new CharacterString("test"), Boolean.TRUE,
                new DateTime(1491329790372L));

        ByteQueue queue = new ByteQueue();
        original.write(queue, 4);

        final EncodedValue parsed = new EncodedValue(queue, 4);
        assertEquals(original, parsed);

        queue = new ByteQueue(parsed.getData());
        assertEquals(new CharacterString("test"), new CharacterString(queue));
        assertEquals(Boolean.TRUE, new Boolean(queue));
        assertEquals(new DateTime(1491329790372L), new DateTime(queue));
    }

    @Test
    public void contextual() throws Exception {
        ByteQueue queue = new ByteQueue();
        new Real(3.14F).write(queue);
        new CharacterString("test").write(queue);
        new DateTime(1491329790372L).write(queue, 0);
        Boolean.TRUE.write(queue, 1);
        Boolean.FALSE.write(queue, 12);
        final EncodedValue original = new EncodedValue(queue.popAll());

        original.write(queue, 17);

        final EncodedValue parsed = new EncodedValue(queue, 17);
        queue = new ByteQueue(parsed.getData());

        assertEquals(new Real(3.14F), new Real(queue));
        assertEquals(new CharacterString("test"), new CharacterString(queue));
        assertEquals(new DateTime(1491329790372L), Encodable.read(queue, DateTime.class, 0));
        assertEquals(Boolean.TRUE, Encodable.read(queue, Boolean.class, 1));
        assertEquals(Boolean.FALSE, Encodable.read(queue, Boolean.class, 12));
    }
}
