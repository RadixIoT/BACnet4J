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
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class XyColor extends BaseType {
    private final Real xCoordinate;
    private final Real yCoordinate;

    public XyColor(Real xCoordinate, Real yCoordinate) {
        // BACnetxyColor production: x-coordinate and y-coordinate are Real values constrained to the range 0.0..1.0
        // (CIE xy chromaticity).
        if (outOfRange(xCoordinate) || outOfRange(yCoordinate)) {
            throw new IllegalArgumentException("invalid coordinate value");
        }
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, xCoordinate);
        write(queue, yCoordinate);
    }

    private static boolean outOfRange(Real r) {
        float v = r.floatValue();
        return Float.isNaN(v) || v < 0.0f || v > 1.0f;
    }

    @Override
    public String toString() {
        return "XyColor [" +
                "x-coordinate=" + xCoordinate +
                ", y-coordinate=" + yCoordinate +
                ']';
    }

    public Real getXCoordinate() {
        return xCoordinate;
    }

    public Real getYCoordinate() {
        return yCoordinate;
    }

    public XyColor(final ByteQueue queue) throws BACnetException {
        xCoordinate = read(queue, Real.class);
        yCoordinate = read(queue, Real.class);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        XyColor xyColor = (XyColor) o;
        return Objects.equals(xCoordinate, xyColor.xCoordinate) && Objects.equals(yCoordinate,
                xyColor.yCoordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xCoordinate, yCoordinate);
    }
}
