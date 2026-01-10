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

public abstract class AbstractCharacterEncoder implements CharacterEncoder {
    private final CharacterEncoding characterEncoding;
    private final String javaCharsetName;

    protected AbstractCharacterEncoder(CharacterEncoding characterEncoding, String javaCharsetName) {
        this.characterEncoding = characterEncoding;
        this.javaCharsetName = javaCharsetName;
    }

    @Override
    public boolean isEncodingSupported(CharacterEncoding encoding) {
        return characterEncoding.equals(encoding);
    }

    @Override
    public byte[] encode(String value) {
        try {
            return value.getBytes(javaCharsetName);
        } catch (final UnsupportedEncodingException e) {
            // Should never happen
            throw new BACnetRuntimeException(e);
        }
    }

    @Override
    public String decode(byte[] bytes) {
        try {
            return new String(bytes, javaCharsetName);
        } catch (final UnsupportedEncodingException e) {
            // Should never happen
            throw new BACnetRuntimeException(e);
        }
    }
}
