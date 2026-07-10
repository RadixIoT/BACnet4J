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
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChannelValue extends BaseType {
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
        choiceOptions.addContextual(0, LightingCommand.class);
        choiceOptions.addContextual(1, XyColor.class);
        choiceOptions.addContextual(2, ColorCommand.class);
    }

    private final Choice choice;

    public ChannelValue(Null nullValue) {
        this.choice = new Choice(nullValue, choiceOptions);
    }

    public ChannelValue(Real realValue) {
        this.choice = new Choice(realValue, choiceOptions);
    }

    public ChannelValue(Enumerated enumeratedValue) {
        this.choice = new Choice(enumeratedValue, choiceOptions);
    }

    public ChannelValue(UnsignedInteger unsignedValue) {
        this.choice = new Choice(unsignedValue, choiceOptions);
    }

    public ChannelValue(Boolean booleanValue) {
        this.choice = new Choice(booleanValue, choiceOptions);
    }

    public ChannelValue(SignedInteger integerValue) {
        this.choice = new Choice(integerValue, choiceOptions);
    }

    public ChannelValue(Double doubleValue) {
        this.choice = new Choice(doubleValue, choiceOptions);
    }

    public ChannelValue(Time timeValue) {
        this.choice = new Choice(timeValue, choiceOptions);
    }

    public ChannelValue(CharacterString characterStringValue) {
        this.choice = new Choice(characterStringValue, choiceOptions);
    }

    public ChannelValue(OctetString octetStringValue) {
        this.choice = new Choice(octetStringValue, choiceOptions);
    }

    public ChannelValue(BitString bitStringValue) {
        this.choice = new Choice(bitStringValue, choiceOptions);
    }

    public ChannelValue(Date dateValue) {
        this.choice = new Choice(dateValue, choiceOptions);
    }

    public ChannelValue(ObjectIdentifier objectIdentifierValue) {
        this.choice = new Choice(objectIdentifierValue, choiceOptions);
    }

    public ChannelValue(LightingCommand lightingCommandValue) {
        this.choice = new Choice(0, lightingCommandValue, choiceOptions);
    }

    public ChannelValue(XyColor xyColor) {
        this.choice = new Choice(1, xyColor, choiceOptions);
    }

    public ChannelValue(ColorCommand colorCommand) {
        this.choice = new Choice(2, colorCommand, choiceOptions);
    }

    public Choice getChoice() {
        return choice;
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, choice);
    }

    public ChannelValue(ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        ChannelValue that = (ChannelValue) o;
        return Objects.equals(choice, that.choice);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(choice);
    }
}
