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

import static com.serotonin.bacnet4j.util.BACnetUtils.toInt;
import static com.serotonin.bacnet4j.util.BACnetUtils.toLong;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.exception.ReflectionException;
import com.serotonin.bacnet4j.obj.ObjectProperties;
import com.serotonin.bacnet4j.obj.ObjectPropertyTypeDefinition;
import com.serotonin.bacnet4j.obj.PropertyTypeDefinition;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.constructed.OptionalBase;
import com.serotonin.bacnet4j.type.constructed.PriorityArray;
import com.serotonin.bacnet4j.type.constructed.PriorityValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public abstract class Encodable {
    static final Logger LOG = LoggerFactory.getLogger(Encodable.class);

    public abstract void write(ByteQueue queue);

    public abstract void write(ByteQueue queue, int contextId);

    /**
     * Optionally validate the value before it is written into our device
     */
    public abstract void validate() throws BACnetServiceException;

    @Override
    public String toString() {
        return "Encodable(" + getClass().getName() + ")";
    }

    protected static void popTagData(ByteQueue queue, TagData tagData) {
        peekTagData(queue, tagData);
        queue.pop(tagData.tagLength);
    }

    protected static void peekTagData(ByteQueue queue, TagData tagData) {
        int peekIndex = 0;
        byte b = queue.peek(peekIndex++);
        tagData.tagNumber = (b & 0xff) >> 4;
        tagData.contextSpecific = (b & 8) == 8;
        tagData.length = b & 7;

        if (tagData.tagNumber == 0xf)
            // Extended tag.
            tagData.tagNumber = toInt(queue.peek(peekIndex++));

        if (tagData.length == 5) {
            tagData.length = toInt(queue.peek(peekIndex++));
            if (tagData.length == 254)
                tagData.length = toLong(queue.peek(peekIndex++)) << 8 | toLong(queue.peek(peekIndex++));
            else if (tagData.length == 255)
                tagData.length = toLong(queue.peek(peekIndex++)) << 24 | toLong(queue.peek(peekIndex++)) << 16
                        | toLong(queue.peek(peekIndex++)) << 8 | toLong(queue.peek(peekIndex++));
        }

        tagData.tagLength = peekIndex;
    }

    protected static boolean isContextTag(ByteQueue queue) {
        if (queue.size() == 0)
            return false;
        return (queue.peek(0) & 8) == 8;
    }

    protected static int peekTagNumber(ByteQueue queue) {
        if (queue.size() == 0)
            return -1;

        // Take a peek at the tag number.
        int tagNumber = toInt(queue.peek(0)) >> 4;
        if (tagNumber == 15)
            tagNumber = toInt(queue.peek(1));
        return tagNumber;
    }

    protected static int getTagLength(ByteQueue queue) {
        if (queue.size() == 0)
            return -1;

        // Take a peek at the tag number.
        int tagNumber = toInt(queue.peek(0)) >> 4;
        if (tagNumber == 15)
            return 2;
        return 1;
    }

    //
    // Write context tags for base types.
    protected void writeContextTag(ByteQueue queue, int contextId, boolean start) {
        if (contextId < 0 || contextId > 254)
            throw new RuntimeException("Invalid context id: " + contextId);

        if (contextId <= 14)
            queue.push(contextId << 4 | (start ? 0xe : 0xf));
        else {
            queue.push(start ? 0xfe : 0xff);
            queue.push(contextId);
        }
    }

    //
    // Read start tags.
    protected static int readStart(ByteQueue queue) {
        if (queue.size() == 0)
            return -1;

        int b = toInt(queue.peek(0));
        if ((b & 0xf) != 0xe)
            return -1;
        if ((b & 0xf0) == 0xf0)
            return toInt(queue.peek(1));
        return b >> 4;
    }

    protected static int popStart(ByteQueue queue) {
        int contextId = readStart(queue);
        if (contextId != -1) {
            queue.pop();
            if (contextId > 14)
                queue.pop();
        }
        return contextId;
    }

    protected static void popStart(ByteQueue queue, int contextId) throws BACnetErrorException {
        if (popStart(queue) != contextId)
            throw new BACnetErrorException(ErrorClass.property, ErrorCode.missingRequiredParameter);
    }

    //
    // Read end tags.
    protected static int readEnd(ByteQueue queue) {
        if (queue.size() == 0)
            return -1;
        int b = toInt(queue.peek(0));
        if ((b & 0xf) != 0xf)
            return -1;
        if ((b & 0xf0) == 0xf0)
            return toInt(queue.peek(1));
        return b >> 4;
    }

    protected static void popEnd(ByteQueue queue, int contextId) throws BACnetErrorException {
        if (readEnd(queue) != contextId)
            throw new BACnetErrorException(ErrorClass.property, ErrorCode.missingRequiredParameter);
        queue.pop();
        if (contextId > 14)
            queue.pop();
    }

    /**
     * Check if the tag number at the beginning of the queue matches the given context id.
     */
    private static boolean matchContextId(ByteQueue queue, int contextId) {
        return peekTagNumber(queue) == contextId;
    }

    /**
     * Check if the tag at the beginning of the queue is a start tag that matches given context id.
     */
    protected static boolean matchStartTag(ByteQueue queue, int contextId) {
        return matchContextId(queue, contextId) && (queue.peek(0) & 0xf) == 0xe;
    }

    /**
     * Check if the tag at the beginning of the queue is an end tag that matches given context id.
     */
    protected static boolean matchEndTag(ByteQueue queue, int contextId) {
        return matchContextId(queue, contextId) && (queue.peek(0) & 0xf) == 0xf;
    }

    /**
     * Check if the tag at the beginning of the queue is NOT an end tag, but that it matches given context id.
     */
    protected static boolean matchNonEndTag(ByteQueue queue, int contextId) {
        return matchContextId(queue, contextId) && (queue.peek(0) & 0xf) != 0xf;
    }

    //
    // Reading
    //

    @SuppressWarnings("unchecked")
    public static <T extends Encodable> T read(ByteQueue queue, Class<T> clazz) throws BACnetException {
        if (clazz == Primitive.class)
            return (T) Primitive.createPrimitive(queue);

        try {
            return clazz.getConstructor(ByteQueue.class).newInstance(queue);
        } catch (InvocationTargetException e) {
            // Check if there is a wrapped BACnet exception
            if (e.getCause() instanceof BACnetException be)
                throw be;
            throw new ReflectionException(e);
        } catch (Exception e) {
            throw new BACnetException(e);
        }
    }

    //
    // Read and write with context id.
    public static <T extends Encodable> T read(ByteQueue queue, Class<T> clazz, int contextId) throws BACnetException {
        if (!matchNonEndTag(queue, contextId))
            throw new BACnetErrorException(ErrorClass.property, ErrorCode.missingRequiredParameter);

        if (Primitive.class.isAssignableFrom(clazz)) {
            return read(queue, clazz);
        }
        return readWrapped(queue, clazz, contextId);
    }

    protected static <T extends Encodable> T readOptional(ByteQueue queue, Class<T> clazz, int contextId)
            throws BACnetException {
        if (!matchNonEndTag(queue, contextId))
            return null;
        return read(queue, clazz, contextId);
    }

    /**
     * Read an application-tagged optional primitive. Peeks the next tag and compares it against
     * the class's {@code TYPE_ID} static field; if it matches (and is not a context tag), decodes
     * the value, otherwise returns null. Symmetric with {@link #writeOptional(ByteQueue, Encodable)}
     * for primitives, and complementary to the context-tagged {@link #readOptional(ByteQueue, Class, int)}.
     */
    protected static <T extends Primitive> T readOptional(ByteQueue queue, Class<T> clazz) throws BACnetException {
        byte typeId;
        try {
            typeId = clazz.getField("TYPE_ID").getByte(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ReflectionException(e);
        }
        if (isContextTag(queue) || peekTagNumber(queue) != (typeId & 0xff))
            return null;
        return read(queue, clazz);
    }

    //
    // Read choices
    protected static Choice readChoice(ByteQueue queue, ChoiceOptions choiceOptions) throws BACnetException {
        return new Choice(queue, choiceOptions);
    }

    protected static Choice readChoice(ByteQueue queue, ChoiceOptions choiceOptions, int contextId)
            throws BACnetException {
        popStart(queue, contextId);
        try {
            return readChoice(queue, choiceOptions);
        } finally {
            popEnd(queue, contextId);
        }
    }

    protected static Choice readOptionalChoice(ByteQueue queue, ChoiceOptions choiceOptions) throws BACnetException {
        if (peekTagNumber(queue) == -1)
            return null;
        return readChoice(queue, choiceOptions);
    }

    protected static Choice readOptionalChoice(ByteQueue queue, ChoiceOptions choiceOptions, int contextId)
            throws BACnetException {
        if (!matchNonEndTag(queue, contextId))
            return null;
        return readChoice(queue, choiceOptions, contextId);
    }

    //
    // Read lists
    public static <T extends Encodable> SequenceOf<T> readSequenceOf(ByteQueue queue, Class<T> clazz)
            throws BACnetException {
        return new SequenceOf<>(queue, clazz);
    }

    protected static <T extends Encodable> SequenceOf<T> readSequenceOf(ByteQueue queue, int count, Class<T> clazz)
            throws BACnetException {
        return new SequenceOf<>(queue, count, clazz);
    }

    protected static <T extends Encodable> SequenceOf<T> readSequenceOf(ByteQueue queue, Class<T> clazz, int contextId)
            throws BACnetException {
        popStart(queue, contextId);
        SequenceOf<T> result = new SequenceOf<>(queue, clazz, contextId);
        popEnd(queue, contextId);
        return result;
    }

    protected static <T extends Encodable> T readSequenceType(ByteQueue queue, Class<T> clazz, int contextId)
            throws BACnetException {
        popStart(queue, contextId);
        T result;
        try {
            result = clazz.getConstructor(ByteQueue.class, Integer.TYPE).newInstance(queue, contextId);
        } catch (Exception e) {
            throw new BACnetException(e);
        }
        popEnd(queue, contextId);
        return result;
    }

    protected static SequenceOf<Choice> readSequenceOfChoice(ByteQueue queue, ChoiceOptions choiceOptions,
            int contextId) throws BACnetException {
        popStart(queue, contextId);
        SequenceOf<Choice> result = new SequenceOf<>();
        while (readEnd(queue) != contextId)
            result.add(new Choice(queue, choiceOptions));
        popEnd(queue, contextId);
        return result;
    }

    protected static <T extends Encodable> SequenceOf<T> readOptionalSequenceOf(ByteQueue queue, Class<T> clazz,
            int contextId) throws BACnetException {
        if (readStart(queue) != contextId)
            return null;
        return readSequenceOf(queue, clazz, contextId);
    }

    protected static <T extends Encodable> BACnetArray<T> readArray(ByteQueue queue, Class<T> clazz, int contextId)
            throws BACnetException {
        popStart(queue, contextId);
        BACnetArray<T> result = new BACnetArray<>(queue, clazz, contextId);
        popEnd(queue, contextId);
        return result;
    }

    private static Encodable readUnknown(ByteQueue queue, int contextId) throws BACnetException {
        TagData tagData = new TagData();
        peekTagData(queue, tagData);

        // Check if the tag number matches the context id. If they match, then create the context-specific parameter,
        // otherwise an AmbiguousValue.
        if (!tagData.isStartTag(contextId)) {
            return new AmbiguousValue(queue, contextId);
        }
        // Keep the original queue
        ByteQueue originalQueue = (ByteQueue) queue.clone();

        // Get the first tagData
        popStart(queue, contextId);
        peekTagData(queue, tagData);
        if (tagData.contextSpecific || !Primitive.isPrimitive(tagData.tagNumber)) {
            // Constructed type or unknown primitive type. Give up and create an ambiguous.
            queue.clear();
            queue.push(originalQueue);
            return new AmbiguousValue(queue, contextId);
        } else {
            // Primitive type
            Primitive primitive = Primitive.createPrimitive(queue);

            // Peek again to see what the next tagData is.
            peekTagData(queue, tagData);
            if (tagData.isEndTag()) {
                // Just one primitive. Check contextId in the End-Tag.
                popEnd(queue, contextId);
                return primitive;
            } else {
                // Try to create a sequence of primitives.
                SequenceOf<Encodable> seq = new SequenceOf<>();
                seq.add(primitive);
                while (queue.size() > 0 && !tagData.isEndTag()) {
                    //If the data is something special, give up and create an ambiguous.
                    if (tagData.contextSpecific || !Primitive.isPrimitive(tagData.tagNumber)) {
                        queue.clear();
                        queue.push(originalQueue);
                        return new AmbiguousValue(queue, contextId);
                    }
                    seq.add(Primitive.createPrimitive(queue));
                    peekTagData(queue, tagData);
                }
                // Check contextId in the End-Tag.
                popEnd(queue, contextId);
                return seq;
            }
        }
    }

    protected static Encodable readANY(ByteQueue queue, ObjectType objectType, PropertyIdentifier propertyIdentifier,
            UnsignedInteger propertyArrayIndex, int contextId) throws BACnetException {
        // A property array index of 0 indicates a request for the length of an array.
        if (propertyArrayIndex != null && propertyArrayIndex.intValue() == 0)
            return readWrapped(queue, UnsignedInteger.class, contextId);

        if (!matchNonEndTag(queue, contextId))
            throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType);

        // Get the definition for the property, if there is one.
        PropertyTypeDefinition def = getPropertyTypeDefinition(objectType, propertyIdentifier);

        if (def == null) {
            // We don't know what this is.
            return readUnknown(queue, contextId);
        }

        if (ObjectProperties.isCommandable(objectType, propertyIdentifier)) {
            // If the object is commandable, it could be set to Null, so we need to treat it as ambiguous.
            AmbiguousValue amb = new AmbiguousValue(queue, contextId);

            if (amb.isNull())
                return Null.instance;

            // Try converting to the definition value.
            return amb.convertTo(def.getClazz());
        }

        if (propertyArrayIndex == null && def.isCollection()) {
            if (def.isArray()) {
                return readArray(queue, def.getClazz(), contextId);
            }
            return readSequenceOf(queue, def.getClazz(), contextId);
        }

        if (propertyArrayIndex != null && def.getClazz() == PriorityArray.class) {
            // An element of a priority array.
            return readWrapped(queue, PriorityValue.class, contextId);
        }

        if (isNullPrimitiveBody(queue, contextId) && !OptionalBase.class.isAssignableFrom(def.getClazz())) {
            // Per addendum 135-2016br-2: preserve a wire NULL value as a bare Null primitive so the write
            // layer can silently succeed a relinquish attempt against a non-commandable property whose
            // declared datatype does not include NULL. Only applied when the declared class is a leaf
            // primitive (e.g. CharacterString, Real, Boolean); CHOICE-based wrappers such as
            // OptionalCharacterString or ChannelValue accept NULL as a legitimate alternative and are
            // decoded normally via readWrapped.
            //
            // Note that we do not include subsclasses of OptionalBase here because we need those to be instantiated
            // normally. It's just that when they are null they look like primitive nulls.
            popStart(queue, contextId);
            Null n = new Null(queue);
            popEnd(queue, contextId);
            return n;
        }

        // Some non-conformant devices (observed on LG Smart 5) encode "no value" for a
        // scalar property as an opening context tag immediately followed by its matching
        // closing tag, with nothing between them. ASHRAE 135 requires propertyAccessError
        // [5] in this situation, but rather than abort the surrounding RPM response we
        // consume the empty value and substitute Error so the caller can keep parsing the
        // remaining results. This check runs last, after the branches above, so it never
        // overrides their own correct handling of an empty `[n][/n]` -- an empty collection
        // already decodes to an empty SequenceOf via readSequenceOf, and a commandable
        // property's Null already flows through the AmbiguousValue branch above.
        if (isEmptyConstructedValue(queue, contextId)) {
            LOG.warn("Non-conformant empty property value for object={} property={}; decoding as not initialized",
                    objectType, propertyIdentifier);
            popStart(queue, contextId);
            popEnd(queue, contextId);
            return new ErrorClassAndCode(ErrorClass.property, ErrorCode.valueNotInitialized);
        }

        return readWrapped(queue, def.getClazz(), contextId);
    }

    /**
     * Peeks at the byte following the opening context tag and reports whether it is a NULL primitive
     * (application tag 0, length 0). Used by {@link #readANY} to detect a relinquish-style Null value
     * that would otherwise be rejected by strict-type decode. Does not consume any bytes.
     */
    private static boolean isNullPrimitiveBody(ByteQueue queue, int contextId) {
        if (readStart(queue) != contextId) {
            return false;
        }
        int startTagLength = contextId > 14 ? 2 : 1;
        if (queue.size() < startTagLength + 2) {
            return false;
        }
        // The byte after the opening context tag encodes the value tag. NULL is application tag 0, primitive,
        // length 0 — a single 0x00 byte. The following byte should be the closing context tag.
        return (queue.peek(startTagLength) & 0xff) == 0x00;
    }

    /**
     * Return true if the queue begins with an opening context tag for contextId that is
     * immediately followed by its matching closing tag -- an empty constructed value,
     * such as `[4][/4]`. This is a non-conformant encoding emitted by some devices to
     * indicate "no value", and should be handled gracefully rather than failing the
     * enclosing response.
     */
    private static boolean isEmptyConstructedValue(ByteQueue queue, int contextId) {
        if (readStart(queue) != contextId)
            return false;

        int startTagLength = contextId > 14 ? 2 : 1;
        if (queue.size() <= startTagLength) {
            return false;
        }

        int b = toInt(queue.peek(startTagLength));
        if ((b & 0xf) != 0xf)
            return false;

        int endTagNumber;
        if ((b & 0xf0) == 0xf0) {
            if (queue.size() <= startTagLength + 1)
                return false;
            endTagNumber = toInt(queue.peek(startTagLength + 1));
        } else {
            endTagNumber = (b & 0xff) >> 4;
        }
        return endTagNumber == contextId;
    }

    protected static Encodable readOptionalANY(ByteQueue queue, ObjectType objectType,
            PropertyIdentifier propertyIdentifier, int contextId) throws BACnetException {
        if (readStart(queue) != contextId)
            return null;
        return readANY(queue, objectType, propertyIdentifier, null, contextId);
    }

    protected static Encodable readOptionalANY(ByteQueue queue, ObjectType objectType,
            PropertyIdentifier propertyIdentifier, UnsignedInteger propertyArrayIndex, int contextId)
            throws BACnetException {
        if (readStart(queue) != contextId)
            return null;
        return readANY(queue, objectType, propertyIdentifier, propertyArrayIndex, contextId);
    }

    protected static SequenceOf<? extends Encodable> readSequenceOfANY(ByteQueue queue, ObjectType objectType,
            PropertyIdentifier propertyIdentifier, int contextId) throws BACnetException {
        PropertyTypeDefinition def = getPropertyTypeDefinition(objectType, propertyIdentifier);
        if (def == null)
            return readSequenceOf(queue, AmbiguousValue.class, contextId);
        return readSequenceOf(queue, def.getClazz(), contextId);
    }

    private static PropertyTypeDefinition getPropertyTypeDefinition(ObjectType objectType,
            PropertyIdentifier propertyIdentifier) {
        if (objectType != null && propertyIdentifier != null) {
            ObjectPropertyTypeDefinition def = ObjectProperties.getObjectPropertyTypeDefinition(objectType,
                    propertyIdentifier);
            if (def != null) {
                return def.getPropertyTypeDefinition();
            }
        }

        // Check if the pid has only one type
        if (propertyIdentifier != null) {
            return ObjectProperties.getPropertyTypeDefinition(propertyIdentifier);
        }

        return null;
    }

    // Read vendor-specific
    protected static EncodedValue readEncodedValue(ByteQueue queue, int contextId) throws BACnetException {
        if (readStart(queue) != contextId)
            return null;
        return new EncodedValue(queue, contextId);
    }

    private static <T extends Encodable> T readWrapped(ByteQueue queue, Class<T> clazz, int contextId)
            throws BACnetException {
        popStart(queue, contextId);
        T result = read(queue, clazz);
        popEnd(queue, contextId);
        return result;
    }

    //
    // Writing
    //

    public static void write(ByteQueue queue, Encodable type) {
        type.write(queue);
    }

    public static void write(ByteQueue queue, Encodable type, int contextId) {
        type.write(queue, contextId);
    }

    //
    // Optional read and write.
    protected static void writeOptional(ByteQueue queue, Encodable type) {
        if (type == null)
            return;
        write(queue, type);
    }

    protected static void writeOptional(ByteQueue queue, Encodable type, int contextId) {
        if (type == null)
            return;
        write(queue, type, contextId);
    }

    // Read and write encodable
    protected static void writeANY(ByteQueue queue, Encodable type, int contextId) {
        if (Primitive.class.isAssignableFrom(type.getClass()))
            ((Primitive) type).writeWithContextTag(queue, contextId);
        else
            type.write(queue, contextId);
    }

}
