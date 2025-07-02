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

package com.serotonin.bacnet4j.type.primitive;

import java.io.UnsupportedEncodingException;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class CharacterString extends Primitive {
    public static final byte TYPE_ID = 7;
    public static final int IBM_MS_DBCS_CODEPAGE = 850;


    public interface Encodings {
        byte ANSI_X3_4 = 0;
        byte IBM_MS_DBCS = 1;
        byte JIS_C_6226 = 2;
        byte ISO_10646_UCS_4 = 3;
        byte ISO_10646_UCS_2 = 4;
        byte ISO_8859_1 = 5;
    }


    public static final CharacterString EMPTY = new CharacterString("");

    private final byte encoding;
    private final String value;

    public CharacterString(final String value) {
        encoding = Encodings.ANSI_X3_4;
        this.value = value == null ? "" : value;
    }

    /**
     * According to Oracle java documentation about Charset, the behavior of optional charsets may vary between java
     * platform implementations.
     * This concerns ISO_10646_UCS_4 (UTF-32), IBM_MS_DBCS and JIS_C_6226.
     *
     * @param encoding
     * @param value
     */
    public CharacterString(final byte encoding, final String value) {
        try {
            validateEncoding();
        } catch (final BACnetErrorException e) {
            // This is an API constructor, so it doesn't need to throw checked exceptions. Convert to runtime.
            throw new BACnetRuntimeException(e);
        }
        this.encoding = encoding;
        this.value = value == null ? "" : value;
    }

    public byte getEncoding() {
        return encoding;
    }

    public String getValue() {
        return value;
    }

    //
    // Reading and writing
    //
    public CharacterString(final ByteQueue queue) throws BACnetErrorException {
        final int length = (int) readTag(queue, TYPE_ID);

        encoding = queue.pop();
        validateEncoding();
        int headerLength = 1;
        if (encoding == Encodings.IBM_MS_DBCS) {
            headerLength += 2;
            //Decode the codePage
            int codePage = queue.popU2B();
            //Currently only the codepage 850 is supported for IBM_MS_DBCS.
            if (codePage != IBM_MS_DBCS_CODEPAGE) {
                throw new BACnetErrorException(ErrorClass.property, ErrorCode.characterSetNotSupported,
                        Byte.toString(encoding));
            }
        }

        final byte[] bytes = new byte[length - headerLength];
        queue.pop(bytes);

        value = decode(encoding, bytes);
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        queue.push(encoding);
        queue.push(encode(encoding, value));
    }

    @Override
    protected long getLength() {
        return encode(encoding, value).length + 1;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    private static byte[] encode(final byte encoding, final String value) {
        try {
            switch (encoding) {
                case Encodings.ANSI_X3_4:
                    return value.getBytes("UTF-8");
                case Encodings.ISO_10646_UCS_2:
                    return value.getBytes("UTF-16");
                case Encodings.ISO_8859_1:
                    return value.getBytes("ISO-8859-1");
                case Encodings.ISO_10646_UCS_4:
                    return value.getBytes("UTF-32");
                case Encodings.IBM_MS_DBCS:
                    byte[] bytes = value.getBytes("IBM" + IBM_MS_DBCS_CODEPAGE);
                    //Add the codePage
                    byte[] result = new byte[2 + bytes.length];
                    result[0] = (byte) (IBM_MS_DBCS_CODEPAGE >> 8);
                    result[1] = (byte) IBM_MS_DBCS_CODEPAGE;
                    System.arraycopy(bytes, 0, result, 2, bytes.length);
                    return result;
                default:
                    return null;
            }
        } catch (final UnsupportedEncodingException e) {
            // Should never happen, so convert to a runtime exception.
            throw new RuntimeException(e);
        }
    }

    private static String decode(final byte encoding, final byte[] bytes) {
        try {
            switch (encoding) {
                case Encodings.ANSI_X3_4:
                    return new String(bytes, "UTF-8");
                case Encodings.ISO_10646_UCS_2:
                    return new String(bytes, "UTF-16");
                case Encodings.ISO_8859_1:
                    return new String(bytes, "ISO-8859-1");
                case Encodings.ISO_10646_UCS_4:
                    return new String(bytes, "UTF-32");
                case Encodings.IBM_MS_DBCS:
                    return new String(bytes, "IBM" + IBM_MS_DBCS_CODEPAGE);
                default:
                    return "";
            }
        } catch (final UnsupportedEncodingException e) {
            // Should never happen, so convert to a runtime exception.
            throw new RuntimeException(e);
        }
    }

    private void validateEncoding() throws BACnetErrorException {
        if (encoding != Encodings.ANSI_X3_4 && encoding != Encodings.ISO_10646_UCS_2
                && encoding != Encodings.ISO_8859_1 && encoding != Encodings.ISO_10646_UCS_4 && encoding != Encodings.IBM_MS_DBCS)
            throw new BACnetErrorException(ErrorClass.property, ErrorCode.characterSetNotSupported,
                    Byte.toString(encoding));
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (value == null ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CharacterString other = (CharacterString) obj;
        if (encoding != other.encoding)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return value;
    }
}
