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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.AmbiguousValue;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.ObjectTypePropertyReference;
import com.serotonin.bacnet4j.type.ThreadLocalObjectTypePropertyReferenceStack;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions.ContextualType;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Choice extends BaseType {
    static final Logger LOG = LoggerFactory.getLogger(Choice.class);

    private int contextId;
    private Encodable datum;
    private final ChoiceOptions choiceOptions;

    public Choice(final Encodable datum, final ChoiceOptions choiceOptions) {
        this(-1, datum, choiceOptions);
    }

    public Choice(final int contextId, final Encodable datum, final ChoiceOptions choiceOptions) {
        this.contextId = contextId;
        this.datum = datum;
        this.choiceOptions = choiceOptions;
    }

    @Override
    public void write(final ByteQueue queue) {
        if (contextId == -1)
            write(queue, datum);
        else if (choiceOptions.getContextualClass(contextId).getClazz() == AmbiguousValue.class)
            writeANY(queue, datum, contextId);
        else
            write(queue, datum, contextId);
    }

    public Choice(final ByteQueue queue, final ChoiceOptions choiceOptions) throws BACnetException {
        this.choiceOptions = choiceOptions;
        read(queue);
    }

    public Choice(final ByteQueue queue, final ChoiceOptions choiceOptions, final int contextId)
            throws BACnetException {
        this.choiceOptions = choiceOptions;
        popStart(queue, contextId);
        read(queue);
        popEnd(queue, contextId);
    }

    private void read(final ByteQueue queue) throws BACnetException {
        if (isContextTag(queue)) {
            contextId = peekTagNumber(queue);
            final ContextualType type = choiceOptions.getContextualClass(contextId);
            if (type == null) {
                LOG.warn("Could not associated choice context tag with class: {}", contextId);
                throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType);
            }

            if (type.isSequence() && type.getClazz() == AmbiguousValue.class) {
                final ObjectTypePropertyReference ref = ThreadLocalObjectTypePropertyReferenceStack.get();
                datum = readSequenceOfANY(queue, ref.getObjectType(), ref.getPropertyIdentifier(), contextId);
            } else if (type.isSequence()) {
                datum = readSequenceOf(queue, type.getClazz(), contextId);
            } else if (type.getClazz() == AmbiguousValue.class) {
                final ObjectTypePropertyReference ref = ThreadLocalObjectTypePropertyReferenceStack.get();
                datum = readANY(queue, ref.getObjectType(), ref.getPropertyIdentifier(), ref.getPropertyArrayIndex(),
                        contextId);
            } else {
                datum = read(queue, type.getClazz(), contextId);
            }
        } else {
            contextId = -1;
            // Decode a primitive
            Primitive primitive = read(queue, Primitive.class);
            // Validate that this primitive is allowed.
            if (!choiceOptions.containsPrimitive(primitive.getClass())) {
                if (primitive.getClass() == UnsignedInteger.class) {
                    // Since there is only one application tag for all unsigned types, 
                    // try to convert in the allowed unsigned.  
                    UnsignedInteger unsigned = (UnsignedInteger) primitive;
                    try {
                        if (choiceOptions.containsPrimitive(Unsigned32.class)) {
                            primitive = new Unsigned32(unsigned.bigIntegerValue());
                        } else if (choiceOptions.containsPrimitive(Unsigned16.class)) {
                            primitive = new Unsigned16(unsigned.bigIntegerValue().intValueExact());
                        } else if (choiceOptions.containsPrimitive(Unsigned8.class)) {
                            primitive = new Unsigned8(unsigned.bigIntegerValue().intValueExact());
                        } else {
                            LOG.warn("Decoded a primitive that is not allowed in this context: {}",
                                    primitive.getClass());
                            throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType);
                        }
                    } catch (IllegalArgumentException | ArithmeticException ex) {
                        LOG.warn("Decoded a unsigned that is not allowed in this context: {}", ex.getMessage());
                        throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType);
                    }
                } else {
                    LOG.warn("Decoded a primitive that is not allowed in this context: {}", primitive.getClass());
                    throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType);
                }
            }
            datum = primitive;
        }
    }

    public int getContextId() {
        return contextId;
    }

    public ChoiceOptions getChoiceOptions() {
        return choiceOptions;
    }

    @SuppressWarnings("unchecked")
    public <T extends Encodable> T getDatum() {
        return (T) datum;
    }

    public boolean isa(final Class<?> clazz) {
        return clazz.isAssignableFrom(datum.getClass());
    }

    @Override
    public String toString() {
        return datum.toString();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + contextId;
        result = PRIME * result + (datum == null ? 0 : datum.hashCode());
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
        final Choice other = (Choice) obj;
        if (contextId != other.contextId)
            return false;
        if (datum == null) {
            if (other.datum != null)
                return false;
        } else if (!datum.equals(other.datum))
            return false;
        return true;
    }
}
