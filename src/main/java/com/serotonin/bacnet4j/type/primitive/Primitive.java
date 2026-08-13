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

import java.math.BigInteger;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.TagData;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.BACnetUtils;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public abstract class Primitive extends Encodable {
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

    /**
     * Passed as the maximum length for a datatype whose encoded length has no upper bound of its own, leaving it
     * constrained only by the data available.
     */
    protected static final int NO_MAX_LENGTH = Integer.MAX_VALUE;

    /**
     * The most content octets the integer datatypes are read from. Unsigned64 is the widest named unsigned type and
     * Integer32 the widest signed type (135-2024 clause 21), so eight octets covers every integer value the standard
     * defines a datatype for. Note that the base Unsigned production is {@code INTEGER (0..MAX)} and Integer is
     * unconstrained, so this is a pragmatic limit rather than one the standard states.
     */
    protected static final int MAX_INTEGER_LENGTH = 8;

    /**
     * Creates a primitive value where it is encoded immediately in the queue.
     */
    public static Primitive createPrimitive(ByteQueue queue) throws BACnetErrorException {
        // Get the first byte. The 4 high-order bits will tell us what the data type is.
        byte type = queue.peek(0);
        type = (byte) ((type & 0xff) >> 4);
        return createPrimitive(type, queue);
    }

    /**
     * Creates a primitive value where it is encoded between context tags in the queue. If the value in the tags
     * is not a primitive, null is returned.
     */
    public static Primitive createPrimitive(ByteQueue queue, int contextId) throws BACnetErrorException {
        int tagNumber = peekTagNumber(queue);

        // Check if the tag number matches the context id. If they match, then create the context-specific parameter,
        // otherwise return null.
        if (tagNumber != contextId)
            return null;

        int typeId = getPrimitiveTypeId(queue.peek(getTagLength(queue)));
        if (typeId == -1)
            return null;

        popStart(queue, contextId);
        Primitive result = createPrimitive(typeId, queue);
        popEnd(queue, contextId);
        return result;
    }

    private static Primitive createPrimitive(int typeId, ByteQueue queue) throws BACnetErrorException {
        if (typeId == Null.TYPE_ID)
            return new Null(queue);
        if (typeId == Boolean.TYPE_ID)
            return new Boolean(queue);
        if (typeId == UnsignedInteger.TYPE_ID)
            return new UnsignedInteger(queue);
        if (typeId == SignedInteger.TYPE_ID)
            return new SignedInteger(queue);
        if (typeId == Real.TYPE_ID)
            return new Real(queue);
        if (typeId == Double.TYPE_ID)
            return new Double(queue);
        if (typeId == OctetString.TYPE_ID)
            return new OctetString(queue);
        if (typeId == CharacterString.TYPE_ID)
            return new CharacterString(queue);
        if (typeId == BitString.TYPE_ID)
            return new BitString(queue);
        if (typeId == Enumerated.TYPE_ID)
            return new Enumerated(queue);
        if (typeId == Date.TYPE_ID)
            return new Date(queue);
        if (typeId == Time.TYPE_ID)
            return new Time(queue);
        if (typeId == ObjectIdentifier.TYPE_ID)
            return new ObjectIdentifier(queue);

        throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType);
    }

    public static int getPrimitiveTypeId(byte firstByte) {
        // Get the first byte. The 4 high-order bits will tell us what the data type is.
        int typeId = (firstByte & 0xff) >> 4;
        if (isPrimitive(typeId))
            return typeId;
        return -1;
    }

    public static boolean isPrimitive(byte firstByte) {
        return getPrimitiveTypeId(firstByte) != -1;
    }

    public static boolean isPrimitive(int typeId) {
        return typeId >= Null.TYPE_ID && typeId <= ObjectIdentifier.TYPE_ID;
    }

    @Override
    public void write(ByteQueue queue) {
        writeTag(queue, getTypeId(), false, getLength());
        writeImpl(queue);
    }

    @Override
    public void write(ByteQueue queue, int contextId) {
        writeTag(queue, contextId, true, getLength());
        writeImpl(queue);
    }

    public final void writeWithContextTag(ByteQueue queue, int contextId) {
        writeContextTag(queue, contextId, true);
        write(queue);
        writeContextTag(queue, contextId, false);
    }

    protected abstract void writeImpl(ByteQueue queue);

    protected abstract long getLength();

    public abstract byte getTypeId();

    protected static void writeTag(ByteQueue queue, int tagNumber, boolean classTag, long length) {
        int classValue = classTag ? 8 : 0;

        if (length < 0 || length > 0x100000000L)
            throw new IllegalArgumentException("Invalid length: " + length);

        boolean extendedTag = tagNumber > 14;

        if (length < 5) {
            if (extendedTag) {
                queue.push(0xf0 | classValue | length);
                queue.push(tagNumber);
            } else
                queue.push((long) tagNumber << 4 | classValue | length);
        } else {
            if (extendedTag) {
                queue.push(0xf5 | classValue);
                queue.push(tagNumber);
            } else
                queue.push(tagNumber << 4 | classValue | 0x5);

            if (length < 254)
                queue.push(length);
            else if (length < 65536) {
                queue.push(254);
                queue.pushU2B((int) length);
            } else {
                queue.push(255);
                BACnetUtils.pushInt(queue, length);
            }
        }
    }

    /**
     * Reads a tag whose content length is constrained only by the amount of data remaining in the queue. For the
     * datatypes whose encoded length is genuinely variable, and for which the standard therefore states no maximum.
     */
    protected int readTag(ByteQueue queue, byte typeId) throws BACnetErrorException {
        return readTag(queue, typeId, 0, NO_MAX_LENGTH);
    }

    /**
     * Reads a tag and validates the declared content length, both against the range of lengths that the datatype
     * can be encoded in and against the amount of data actually remaining in the queue.
     *
     * <p>Both checks are needed. A length outside the datatype's range has to be rejected even when the peer
     * supplies enough data to back it, and a length within range has to be rejected when the data is not present.
     * Unvalidated, the declared length is used directly to size an array, and a corrupted or hostile value fails
     * with an unchecked exception - NegativeArraySizeException where the length feeds a {@code length + 1}
     * expression that overflows int, or ArithmeticException where BigInteger rejects an oversized magnitude -
     * rather than with a decoding error the caller can turn into a response.</p>
     *
     * <p>The return type is the point of the validation: the raw length is a long because an extended length is
     * read as an unsigned 32 bit value, but a validated length always fits in an int, so callers get one and do
     * not narrow it themselves. Narrowing before validating is what turned a corrupt length into a negative
     * array size.</p>
     *
     * @param minLength the fewest content octets this datatype can be encoded in
     * @param maxLength the most content octets this datatype can be encoded in, or {@link #NO_MAX_LENGTH}
     */
    protected int readTag(ByteQueue queue, byte typeId, int minLength, int maxLength)
            throws BACnetErrorException {
        long length = readTagHeader(queue, typeId);
        if (length < minLength || length > maxLength)
            throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType,
                    "Length " + length + " is outside the range " + minLength + ".." + maxLength
                            + " for primitive type " + typeId);
        if (length > queue.size())
            throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType,
                    "Length " + length + " exceeds the " + queue.size() + " octets remaining");
        // Both bounds are ints, so the validated length cannot be truncated by this narrowing.
        return (int) length;
    }

    /**
     * Reads a tag without validating its length/value field, and returns that field verbatim. Only for Boolean,
     * where it carries the value rather than a count of content octets (135-2024 clause 20.2.3) and so cannot be
     * range checked here. Every other primitive should use one of the readTag methods so that the length is
     * validated.
     */
    protected long readTagHeader(ByteQueue queue, byte typeId) throws BACnetErrorException {
        tagData.pop(queue);
        this.typeId = typeId;
        return tagData.getLength();
    }

    /**
     * Narrows to an int by clamping to the int range rather than truncating to its low order bits.
     *
     * <p>A BACnet integer can hold values that do not fit in a Java int - Unsigned32 above 0x7FFFFFFF and
     * Unsigned64 above 0x7FFFFFFFFFFFFFFF are both legal - and truncating those produces a negative number for a
     * value that is not negative. Callers treat these as sizes, counts, bounds and limits, where a sign flip
     * inverts the meaning of a comparison rather than merely losing precision. Clamping keeps the narrowing
     * monotonic, so a value too large to represent arrives as an implausibly large one instead.</p>
     *
     * <p>Use {@code bigIntegerValue()} where the exact value matters.</p>
     */
    protected static int saturatedIntValue(BigInteger value) {
        if (value.compareTo(INT_MAX) > 0)
            return Integer.MAX_VALUE;
        if (value.compareTo(INT_MIN) < 0)
            return Integer.MIN_VALUE;
        return value.intValue();
    }

    /**
     * Narrows to a long by clamping to the long range rather than truncating. See {@link #saturatedIntValue}.
     */
    protected static long saturatedLongValue(BigInteger value) {
        if (value.compareTo(LONG_MAX) > 0)
            return Long.MAX_VALUE;
        if (value.compareTo(LONG_MIN) < 0)
            return Long.MIN_VALUE;
        return value.longValue();
    }

    protected final int getTagNumber() {
        return tagData.getTagNumber();
    }

    protected final boolean isContextSpecific() {
        return tagData.isContextSpecific();
    }

    /**
     * The decoded tag this value was read from, empty until one of the readTag methods has run. The expected type
     * is held separately because it is what the caller asked for, not what the encoding declared.
     */
    private final TagData tagData = new TagData();
    private int typeId;

    @Override
    public void validate() throws BACnetServiceException {
        // If the tagNumber is not contextSpecific, validate the type
        if (!tagData.isContextSpecific() && tagData.getTagNumber() != typeId) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
        }
    }
}
