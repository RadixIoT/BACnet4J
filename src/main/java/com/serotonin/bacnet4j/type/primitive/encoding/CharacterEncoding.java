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

import static com.serotonin.bacnet4j.type.primitive.encoding.StandardCharacterEncodings.NO_CODE_PAGE;

import java.util.Objects;

public class CharacterEncoding {
    private final byte encoding;
    private final int codePage;

    public CharacterEncoding(byte encoding) {
        this(encoding, NO_CODE_PAGE);
    }

    public CharacterEncoding(byte encoding, int codePage) {
        this.encoding = encoding;
        this.codePage = codePage;
    }

    public byte getEncoding() {
        return encoding;
    }

    public int getCodePage() {
        return codePage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CharacterEncoding that = (CharacterEncoding) o;
        return encoding == that.encoding &&
                codePage == that.codePage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(encoding, codePage);
    }

    @Override
    public String toString() {
        return "CharacterEncoding{" +
                "encoding=" + encoding +
                ", codePage=" + codePage +
                '}';
    }
}
