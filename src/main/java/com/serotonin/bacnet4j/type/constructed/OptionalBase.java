/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Base class for optional-type classes.
 */
public abstract class OptionalBase extends BaseType {
    protected final Choice choice;

    protected OptionalBase(ChoiceOptions choiceOptions) {
        this.choice = new Choice(Null.instance, choiceOptions);
    }

    protected OptionalBase(Encodable e, ChoiceOptions choiceOptions) {
        this.choice = new Choice(e, choiceOptions);
    }

    public boolean isNull() {
        return choice.getContextId() == 0;
    }

    public Null getNullValue() {
        return choice.getDatum();
    }

    public boolean isNullValue() {
        return choice.getDatum() instanceof Null;
    }

    public Choice getChoice() {
        return choice;
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }

    @Override
    public final void write(ByteQueue queue) {
        write(queue, choice);
    }

    protected OptionalBase(ByteQueue queue, ChoiceOptions choiceOptions) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    @Override
    public String toString() {
        return getClass().getName() + " [choice=" + choice + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        OptionalBase that = (OptionalBase) o;
        return Objects.equals(choice, that.choice);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(choice);
    }
}
