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

import static com.serotonin.bacnet4j.type.primitive.encoding.StandardCharacterEncodings.ANSI_X3_4;
import static com.serotonin.bacnet4j.type.primitive.encoding.StandardCharacterEncodings.IBM_MS_DBCS;
import static com.serotonin.bacnet4j.type.primitive.encoding.StandardCharacterEncodings.NO_CODE_PAGE;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.primitive.encoding.CharacterEncoder;
import com.serotonin.bacnet4j.type.primitive.encoding.CharacterEncoding;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class CharacterString extends Primitive {
    public static final byte TYPE_ID = 7;

    // load encoders before creating EMPTY
    private static final List<CharacterEncoder> characterEncoders = loadEncoders();

    public static final CharacterString EMPTY = new CharacterString("");

    private final CharacterEncoding encoding;
    private final CharacterEncoder encoder;
    private final String value;

    public CharacterString(String value) {
        this(new CharacterEncoding(ANSI_X3_4), value);
    }

    /**
     * According to Oracle java documentation about Charset, the behavior of optional charsets may vary between java
     * platform implementations. This concerns ISO_10646_UCS_4 (UTF-32), IBM_MS_DBCS and JIS_C_6226.
     */
    public CharacterString(CharacterEncoding encoding, String value) {
        this.encoding = encoding;
        try {
            encoder = findEncoder(encoding);
        } catch (BACnetErrorException e) {
            // This is an API constructor, so it doesn't need to throw checked exceptions. Convert to runtime.
            throw new BACnetRuntimeException(e);
        }
        this.value = value == null ? "" : value;
    }

    //
    // Reading and writing
    //
    public CharacterString(ByteQueue queue) throws BACnetErrorException {
        int length = (int) readTag(queue, TYPE_ID);

        var parsedEncoding = createCharacterEncoding(queue);
        var headerLength = calcHeaderLength(parsedEncoding);
        var bytes = new byte[length - headerLength];
        queue.pop(bytes);

        CharacterEncoder foundEncoder;
        try {
            foundEncoder = findEncoder(parsedEncoding);
        } catch (BACnetErrorException e) {
            foundEncoder = null;
        }

        if (foundEncoder != null) {
            encoding = parsedEncoding;
            encoder = foundEncoder;
            value = foundEncoder.decode(bytes);
        } else {
            // Per addendum 135-2016bu-2 (Clauses 12.1.4 and 12.1.X): the receiver shall recover
            // from an unsupported character encoding rather than fail to decode a properly-tagged
            // message. The malformed bytes have been consumed; substitute a zero-length string
            // in a supported encoding.
            encoding = new CharacterEncoding(ANSI_X3_4);
            encoder = findEncoder(encoding);
            value = "";
        }
    }

    public CharacterEncoding getEncoding() {
        return encoding;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void writeImpl(ByteQueue queue) {
        queue.push(encoding.getEncoding());
        queue.push(encoder.encode(value));
    }

    @Override
    protected long getLength() {
        return encoder.encode(value).length + 1L;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    private static int calcHeaderLength(CharacterEncoding encoding) {
        int headerLength = 1;
        if (encoding.getCodePage() != NO_CODE_PAGE) {
            headerLength += 2;
        }
        return headerLength;
    }

    private CharacterEncoding createCharacterEncoding(ByteQueue queue) {
        byte encodingValue = queue.pop();
        if (encodingValue != IBM_MS_DBCS) {
            return new CharacterEncoding(encodingValue);
        }
        //Decode the codePage
        int codePage = queue.popU2B();
        return new CharacterEncoding(encodingValue, codePage);
    }

    private static CharacterEncoder findEncoder(CharacterEncoding encoding) throws BACnetErrorException {
        return characterEncoders.stream()
                .filter(encoder -> encoder.isEncodingSupported(encoding))
                .findFirst()
                .orElseThrow(() -> new BACnetErrorException(
                        ErrorClass.property,
                        ErrorCode.characterSetNotSupported,
                        encoding.toString())
                );
    }

    private static List<CharacterEncoder> loadEncoders() {
        ServiceLoader<CharacterEncoder> loader = ServiceLoader.load(CharacterEncoder.class);
        return StreamSupport.stream(loader.spliterator(), false).toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CharacterString that = (CharacterString) o;
        return Objects.equals(encoding, that.encoding) &&
                Objects.equals(encoder, that.encoder) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encoding, encoder, value);
    }

    @Override
    public String toString() {
        return value;
    }
}
