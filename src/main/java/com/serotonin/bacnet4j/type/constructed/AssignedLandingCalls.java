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
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AssignedLandingCalls extends BaseType {
    private final SequenceOf<LandingCall> landingCalls;

    public AssignedLandingCalls(final SequenceOf<LandingCall> landingCalls) {
        this.landingCalls = landingCalls;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, landingCalls, 0);
    }

    public AssignedLandingCalls(final ByteQueue queue) throws BACnetException {
        landingCalls = readSequenceOf(queue, LandingCall.class, 0);
    }

    public SequenceOf<LandingCall> getLandingCalls() {
        return landingCalls;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (landingCalls == null ? 0 : landingCalls.hashCode());
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
        final AssignedLandingCalls other = (AssignedLandingCalls) obj;
        if (landingCalls == null) {
            if (other.landingCalls != null)
                return false;
        } else if (!landingCalls.equals(other.landingCalls))
            return false;
        return true;
    }

    public static class LandingCall extends BaseType {
        private final Unsigned8 floorNumber;
        private final LiftCarDirection direction;

        public LandingCall(final Unsigned8 floorNumber, final LiftCarDirection direction) {
            this.floorNumber = floorNumber;
            this.direction = direction;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, floorNumber, 0);
            write(queue, direction, 1);
        }

        public LandingCall(final ByteQueue queue) throws BACnetException {
            floorNumber = read(queue, Unsigned8.class, 0);
            direction = read(queue, LiftCarDirection.class, 1);
        }

        public Unsigned8 getFloorNumber() {
            return floorNumber;
        }

        public LiftCarDirection getDirection() {
            return direction;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (direction == null ? 0 : direction.hashCode());
            result = prime * result + (floorNumber == null ? 0 : floorNumber.hashCode());
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
            final LandingCall other = (LandingCall) obj;
            if (direction == null) {
                if (other.direction != null)
                    return false;
            } else if (!direction.equals(other.direction))
                return false;
            if (floorNumber == null) {
                if (other.floorNumber != null)
                    return false;
            } else if (!floorNumber.equals(other.floorNumber))
                return false;
            return true;
        }
    }

    @Override
    public String toString() {
        return "AssignedLandingCalls [landingCalls=" + landingCalls + ']';
    }
}
