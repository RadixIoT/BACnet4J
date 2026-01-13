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

package com.serotonin.bacnet4j.type.primitive.encoding;

import java.io.UnsupportedEncodingException;

import com.serotonin.bacnet4j.exception.BACnetRuntimeException;

public abstract class AbstractDbcsCharacterEncoder extends AbstractCharacterEncoder {
    private final CharacterEncoding characterEncoding;
    private final String javaCharsetName;

    protected AbstractDbcsCharacterEncoder(CharacterEncoding characterEncoding, String javaCharsetName) {
        super(characterEncoding, javaCharsetName);
        this.characterEncoding = characterEncoding;
        this.javaCharsetName = javaCharsetName;
    }

    @Override
    public byte[] encode(String value) {
        try {
            byte[] bytes = value.getBytes(javaCharsetName);
            //Add the codePage
            byte[] result = new byte[2 + bytes.length];
            int codePage = characterEncoding.getCodePage();
            result[0] = (byte) (codePage >> 8);
            result[1] = (byte) codePage;
            System.arraycopy(bytes, 0, result, 2, bytes.length);
            return result;
        } catch (final UnsupportedEncodingException e) {
            // Should never happen
            throw new BACnetRuntimeException(e);
        }
    }
}
