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
import com.serotonin.bacnet4j.type.enumerated.LiftCarDirection;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class LandingCallStatus extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(1, LiftCarDirection.class);
        choiceOptions.addContextual(2, Unsigned8.class);
    }

    private final Unsigned8 floorNumber;
    private final Choice command;
    private final CharacterString floorText;

    public LandingCallStatus(final Unsigned8 floorNumber, final LiftCarDirection direction,
            final CharacterString floorText) {
        this.floorNumber = floorNumber;
        command = new Choice(1, direction, choiceOptions);
        this.floorText = floorText;
    }

    public LandingCallStatus(final Unsigned8 floorNumber, final Unsigned8 destination,
            final CharacterString floorText) {
        this.floorNumber = floorNumber;
        command = new Choice(2, destination, choiceOptions);
        this.floorText = floorText;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, floorNumber, 0);
        write(queue, command);
        writeOptional(queue, floorText, 3);
    }

    public LandingCallStatus(final ByteQueue queue) throws BACnetException {
        floorNumber = read(queue, Unsigned8.class, 0);
        command = readChoice(queue, choiceOptions);
        floorText = read(queue, CharacterString.class, 3);
    }

    public Unsigned8 getFloorNumber() {
        return floorNumber;
    }

    public boolean isDirection() {
        return command.isa(LiftCarDirection.class);
    }

    public LiftCarDirection getDirection() {
        return command.getDatum();
    }

    public boolean isDestination() {
        return command.isa(Unsigned8.class);
    }

    public Unsigned8 getDestination() {
        return command.getDatum();
    }

    public CharacterString getFloorText() {
        return floorText;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (command == null ? 0 : command.hashCode());
        result = prime * result + (floorNumber == null ? 0 : floorNumber.hashCode());
        result = prime * result + (floorText == null ? 0 : floorText.hashCode());
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
        final LandingCallStatus other = (LandingCallStatus) obj;
        if (command == null) {
            if (other.command != null)
                return false;
        } else if (!command.equals(other.command))
            return false;
        if (floorNumber == null) {
            if (other.floorNumber != null)
                return false;
        } else if (!floorNumber.equals(other.floorNumber))
            return false;
        if (floorText == null) {
            if (other.floorText != null)
                return false;
        } else if (!floorText.equals(other.floorText))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "LandingCallStatus [floorNumber=" + floorNumber + ", command=" + command + ", floorText=" + floorText + ']';
    }
}
