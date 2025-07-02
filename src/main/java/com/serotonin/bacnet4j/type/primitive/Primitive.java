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

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.BACnetUtils;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

abstract public class Primitive extends Encodable {
    /**
     * Creates a primitive value where it is encoded immediately in the queue.
     */
    public static Primitive createPrimitive(final ByteQueue queue) throws BACnetErrorException {
        // Get the first byte. The 4 high-order bits will tell us what the data type is.
        byte type = queue.peek(0);
        type = (byte) ((type & 0xff) >> 4);
        return createPrimitive(type, queue);
    }

    /**
     * Creates a primitive value where it is encoded between context tags in the queue. If the value in the tags
     * is not a primitive, null is returned.
     */
    public static Primitive createPrimitive(final ByteQueue queue, final int contextId) throws BACnetErrorException {
        final int tagNumber = peekTagNumber(queue);

        // Check if the tag number matches the context id. If they match, then create the context-specific parameter,
        // otherwise return null.
        if (tagNumber != contextId)
            return null;

        final int typeId = getPrimitiveTypeId(queue.peek(getTagLength(queue)));
        if (typeId == -1)
            return null;

        popStart(queue, contextId);
        final Primitive result = createPrimitive(typeId, queue);
        popEnd(queue, contextId);
        return result;
    }

    private static Primitive createPrimitive(final int typeId, final ByteQueue queue) throws BACnetErrorException {
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

    public static int getPrimitiveTypeId(final byte firstByte) {
        // Get the first byte. The 4 high-order bits will tell us what the data type is.
        final int typeId = (int) ((firstByte & 0xff) >> 4);
        if (isPrimitive(typeId))
            return typeId;
        return -1;
    }

    public static boolean isPrimitive(final byte firstByte) {
        return getPrimitiveTypeId(firstByte) != -1;
    }

    public static boolean isPrimitive(int typeId) {
        return typeId >= Null.TYPE_ID && typeId <= ObjectIdentifier.TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        writeTag(queue, getTypeId(), false, getLength());
        writeImpl(queue);
    }

    @Override
    public void write(final ByteQueue queue, final int contextId) {
        writeTag(queue, contextId, true, getLength());
        writeImpl(queue);
    }

    final public void writeWithContextTag(final ByteQueue queue, final int contextId) {
        writeContextTag(queue, contextId, true);
        write(queue);
        writeContextTag(queue, contextId, false);
    }

    abstract protected void writeImpl(ByteQueue queue);

    abstract protected long getLength();

    abstract public byte getTypeId();

    protected static void writeTag(final ByteQueue queue, final int tagNumber, final boolean classTag,
            final long length) {
        final int classValue = classTag ? 8 : 0;

        if (length < 0 || length > 0x100000000l)
            throw new IllegalArgumentException("Invalid length: " + length);

        final boolean extendedTag = tagNumber > 14;

        if (length < 5) {
            if (extendedTag) {
                queue.push(0xf0 | classValue | length);
                queue.push(tagNumber);
            } else
                queue.push(tagNumber << 4 | classValue | length);
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

    protected long readTag(final ByteQueue queue, byte type_Id) throws BACnetErrorException {
        final byte b = queue.pop();
        typeId = type_Id;
        tagNumber = (b & 0xff) >> 4;
        contextSpecific = (b & 8) != 0;
        long length = b & 7;

        if (tagNumber == 0xf)
            // Extended tag.
            tagNumber = queue.popU1B();


        if (length == 5) {
            length = queue.popU1B();
            if (length == 254)
                length = queue.popU2B();
            else if (length == 255)
                length = queue.popU4B();
        }

        return length;
    }

    private int tagNumber;
    private int typeId;
    private boolean contextSpecific;

    @Override
    public void validate() throws BACnetServiceException {
        //if the tagNumber its not contextSpecific, validate the type
        if (!contextSpecific && tagNumber != typeId) {
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
        }
    }
}
