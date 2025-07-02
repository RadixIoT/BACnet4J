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
import com.serotonin.bacnet4j.type.enumerated.DoorStatus;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class LandingDoorStatus extends BaseType {
    private final SequenceOf<LandingDoor> landingDoors;

    public LandingDoorStatus(final SequenceOf<LandingDoor> landingDoors) {
        this.landingDoors = landingDoors;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, landingDoors, 0);
    }

    public LandingDoorStatus(final ByteQueue queue) throws BACnetException {
        landingDoors = readSequenceOf(queue, LandingDoor.class, 0);
    }

    public SequenceOf<LandingDoor> getLandingDoors() {
        return landingDoors;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (landingDoors == null ? 0 : landingDoors.hashCode());
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
        final LandingDoorStatus other = (LandingDoorStatus) obj;
        if (landingDoors == null) {
            if (other.landingDoors != null)
                return false;
        } else if (!landingDoors.equals(other.landingDoors))
            return false;
        return true;
    }

    public static class LandingDoor extends BaseType {
        private final Unsigned8 floorNumber;
        private final DoorStatus doorStatus;

        public LandingDoor(final Unsigned8 floorNumber, final DoorStatus doorStatus) {
            this.floorNumber = floorNumber;
            this.doorStatus = doorStatus;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, floorNumber, 0);
            write(queue, doorStatus, 1);
        }

        public LandingDoor(final ByteQueue queue) throws BACnetException {
            floorNumber = read(queue, Unsigned8.class, 0);
            doorStatus = read(queue, DoorStatus.class, 1);
        }

        public Unsigned8 getFloorNumber() {
            return floorNumber;
        }

        public DoorStatus getDoorStatus() {
            return doorStatus;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (doorStatus == null ? 0 : doorStatus.hashCode());
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
            final LandingDoor other = (LandingDoor) obj;
            if (doorStatus == null) {
                if (other.doorStatus != null)
                    return false;
            } else if (!doorStatus.equals(other.doorStatus))
                return false;
            if (floorNumber == null) {
                if (other.floorNumber != null)
                    return false;
            } else if (!floorNumber.equals(other.floorNumber))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "LandingDoor [floorNumber=" + floorNumber + ", doorStatus=" + doorStatus + ']';
        }
    }

    @Override
    public String toString() {
        return "LandingDoorStatus [landingDoors=" + landingDoors + ']';
    }
}
