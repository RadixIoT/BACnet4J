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

import org.junit.Test;

import com.serotonin.bacnet4j.type.primitive.encoding.CharacterEncoding;
import com.serotonin.bacnet4j.type.primitive.encoding.DbcsCp850CharacterEncoder;
import com.serotonin.bacnet4j.type.primitive.encoding.StandardCharacterEncodings;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class CharacterStringTest {
    @Test
    public void spec20_2_9_a() {
        var str = new CharacterString(new CharacterEncoding(StandardCharacterEncodings.ANSI_X3_4),
                "This is a BACnet string!");
        ByteQueue queue = new ByteQueue();
        str.write(queue);
        assertEquals(new ByteQueue("751900546869732069732061204241436E657420737472696E6721"), queue);
    }

    @Test
    public void spec20_2_9_b() {
        var str = new CharacterString(new CharacterEncoding(StandardCharacterEncodings.IBM_MS_DBCS,
                DbcsCp850CharacterEncoder.DOS_LATIN_1_CODEPAGE), "This is a BACnet string!");
        ByteQueue queue = new ByteQueue();
        str.write(queue);
        assertEquals(new ByteQueue("751B010352546869732069732061204241436E657420737472696E6721"), queue);
    }

    @Test
    public void spec20_2_9_c1() {
        var str = new CharacterString(new CharacterEncoding(StandardCharacterEncodings.ISO_10646_UCS_2),
                "This is a BACnet string!");
        ByteQueue queue = new ByteQueue();
        str.write(queue);
        assertEquals(new ByteQueue(
                        "7531040054006800690073002000690073002000610020004200410043006E0065007400200073007400720069006E00670021"),
                queue);
    }

    // This test is added to ensure that UTF-32 doesn't add a byte order mark.
    @Test
    public void spec20_2_9_c2() {
        var str = new CharacterString(new CharacterEncoding(StandardCharacterEncodings.ISO_10646_UCS_4),
                "This is a BACnet string!");
        ByteQueue queue = new ByteQueue();
        str.write(queue);
        assertEquals(new ByteQueue(
                        "756103000000540000006800000069000000730000002000000069000000730000002000000061000000200000004200000041000000430000006E000000650000007400000020000000730000007400000072000000690000006E0000006700000021"),
                queue);
    }
}
