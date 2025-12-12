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

import org.junit.Test;

import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Time;

public class NameValueTest {
    @Test
    public void characterString() {
        final NameValue nv = new NameValue("tagName", new CharacterString("tagValue"));
        TestUtils.assertEncoding(nv, "0d08007461674e616d6575090074616756616c7565");
    }

    @Test
    public void octetString() {
        final NameValue nv = new NameValue("tagName", new OctetString(new byte[] {0, 1, 2, 3, 4, 5}));
        TestUtils.assertEncoding(nv, "0d08007461674e616d656506000102030405");
    }

    @Test
    public void dateTime() {
        final NameValue nv = new NameValue("tagName", DateTime.UNSPECIFIED);
        TestUtils.assertEncoding(nv, "0d08007461674e616d65a4ffffffffb4ffffffff");
    }

    @Test
    public void optional() {
        final NameValue nv = new NameValue("tagName");
        TestUtils.assertEncoding(nv, "0d08007461674e616d65");
    }

    @Test
    public void sequence() {
        final SequenceOf<NameValue> seq = new SequenceOf<>( //
                new NameValue("t1", CharacterString.EMPTY), //
                new NameValue("t2"), //
                new NameValue("t3", new CharacterString("v1")), //
                new NameValue("t4", DateTime.UNSPECIFIED), //
                new NameValue("t6", Date.UNSPECIFIED), //
                new NameValue("t7", Time.UNSPECIFIED), //
                new NameValue("t5", Null.instance));
        TestUtils.assertSequenceEncoding(seq, NameValue.class,
                "0b00743171000b0074320b007433730076310b007434a4ffffffffb4ffffffff0b007436a4ffffffff0b007437b4ffffffff0b00743500");
    }
}
