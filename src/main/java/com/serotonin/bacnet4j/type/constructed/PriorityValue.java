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

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.AmbiguousValue;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Double;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class PriorityValue extends BaseType {
    private static final ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addPrimitive(Null.class);
        choiceOptions.addPrimitive(Real.class);
        choiceOptions.addPrimitive(Enumerated.class);
        choiceOptions.addPrimitive(UnsignedInteger.class);
        choiceOptions.addPrimitive(Boolean.class);
        choiceOptions.addPrimitive(SignedInteger.class);
        choiceOptions.addPrimitive(Double.class);
        choiceOptions.addPrimitive(Time.class);
        choiceOptions.addPrimitive(CharacterString.class);
        choiceOptions.addPrimitive(OctetString.class);
        choiceOptions.addPrimitive(BitString.class);
        choiceOptions.addPrimitive(Date.class);
        choiceOptions.addPrimitive(ObjectIdentifier.class);

        choiceOptions.addContextual(0, AmbiguousValue.class);
        choiceOptions.addContextual(1, DateTime.class);
        choiceOptions.addContextual(2, XyColor.class);
    }

    private final Choice choice;

    public PriorityValue(Encodable value) {
        if (value instanceof Primitive)
            choice = new Choice(value, choiceOptions);
        else if (value instanceof DateTime)
            choice = new Choice(1, value, choiceOptions);
        else if (value instanceof XyColor)
            choice = new Choice(2, value, choiceOptions);
        else
            choice = new Choice(0, value, choiceOptions);
    }

    public Null getNullValue() {
        return choice.getDatum();
    }

    public Real getRealValue() {
        return choice.getDatum();
    }

    public Enumerated getEnumeratedValue() {
        return choice.getDatum();
    }

    public UnsignedInteger getUnsignedValue() {
        return choice.getDatum();
    }

    public Boolean getBooleanValue() {
        return choice.getDatum();
    }

    public SignedInteger getSignedValue() {
        return choice.getDatum();
    }

    public Double getDoubleValue() {
        return choice.getDatum();
    }

    public Time getTimeValue() {
        return choice.getDatum();
    }

    public CharacterString getCharacterStringValue() {
        return choice.getDatum();
    }

    public OctetString getOctetStringValue() {
        return choice.getDatum();
    }

    public BitString getBitStringValue() {
        return choice.getDatum();
    }

    public Date getDateValue() {
        return choice.getDatum();
    }

    public ObjectIdentifier getOidValue() {
        return choice.getDatum();
    }

    public DateTime getDateTimeValue() {
        return choice.getDatum();
    }

    public Encodable getConstructedValue() {
        return choice.getDatum();
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }

    public boolean isa(Class<?> clazz) {
        return choice.isa(clazz);
    }

    @Override
    public String toString() {
        return "PriorityValue(" + choice + ")";
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, choice);
    }

    public PriorityValue(ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        PriorityValue that = (PriorityValue) o;
        return Objects.equals(choice, that.choice);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(choice);
    }
}
