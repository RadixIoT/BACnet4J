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

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * @author Matthew Lohbihler
 */
public class ShedLevel extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, UnsignedInteger.class);
        choiceOptions.addContextual(1, UnsignedInteger.class);
        choiceOptions.addContextual(2, Real.class);
    }

    private final Choice choice;

    public ShedLevel(final UnsignedInteger datum, final boolean percent) {
        if (percent)
            choice = new Choice(0, datum, choiceOptions);
        else
            choice = new Choice(1, datum, choiceOptions);
    }

    public ShedLevel(final Real amount) {
        choice = new Choice(2, amount, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public UnsignedInteger getPercent() {
        return (UnsignedInteger) choice.getDatum();
    }

    public UnsignedInteger getLevel() {
        return (UnsignedInteger) choice.getDatum();
    }

    public Real getAmount() {
        return (Real) choice.getDatum();
    }

    public boolean isPercent() {
        return choice.getContextId() == 0;
    }

    public boolean isLevel() {
        return choice.getContextId() == 1;
    }

    public boolean isAmount() {
        return choice.getContextId() == 2;
    }

    public Choice getChoice() {
        return choice;
    }

    public <T extends Encodable> T getValue() {
        return choice.getDatum();
    }

    public ShedLevel(final ByteQueue queue) throws BACnetException {
        choice = new Choice(queue, choiceOptions);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (choice == null ? 0 : choice.hashCode());
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
        final ShedLevel other = (ShedLevel) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ShedLevel [choice=" + choice + ']';
    }
}
