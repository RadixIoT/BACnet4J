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
import com.serotonin.bacnet4j.type.enumerated.ColorOperation;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ColorCommand extends BaseType {
    private final ColorOperation operation;
    private final XyColor targetColor;
    private final UnsignedInteger targetColorTemperature;
    private final UnsignedInteger fadeTime;
    private final UnsignedInteger rampRate;
    private final UnsignedInteger stepIncrement;

    public ColorCommand(
            ColorOperation operation,
            XyColor targetColor,
            UnsignedInteger targetColorTemperature,
            UnsignedInteger fadeTime,
            UnsignedInteger rampRate,
            UnsignedInteger stepIncrement) {
        this.operation = operation;
        this.targetColor = targetColor;
        this.targetColorTemperature = targetColorTemperature;
        this.fadeTime = fadeTime;
        this.rampRate = rampRate;
        this.stepIncrement = stepIncrement;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, operation, 0);
        writeOptional(queue, targetColor, 1);
        writeOptional(queue, targetColorTemperature, 2);
        writeOptional(queue, fadeTime, 3);
        writeOptional(queue, rampRate, 4);
        writeOptional(queue, stepIncrement, 5);
    }

    @Override
    public String toString() {
        return "ColorCommand [" +
                "operation=" + operation +
                ", targetColor=" + targetColor +
                ", targetColorTemperature=" + targetColorTemperature +
                ", fadeTime=" + fadeTime +
                ", rampRate=" + rampRate +
                ", stepIncrement=" + stepIncrement +
                ']';
    }

    public ColorOperation getOperation() {
        return operation;
    }

    public XyColor getTargetColor() {
        return targetColor;
    }

    public UnsignedInteger getTargetColorTemperature() {
        return targetColorTemperature;
    }

    public UnsignedInteger getFadeTime() {
        return fadeTime;
    }

    public UnsignedInteger getRampRate() {
        return rampRate;
    }

    public UnsignedInteger getStepIncrement() {
        return stepIncrement;
    }

    public ColorCommand(final ByteQueue queue) throws BACnetException {
        operation = read(queue, ColorOperation.class, 0);
        targetColor = readOptional(queue, XyColor.class, 1);
        targetColorTemperature = readOptional(queue, UnsignedInteger.class, 2);
        fadeTime = readOptional(queue, UnsignedInteger.class, 3);
        rampRate = readOptional(queue, UnsignedInteger.class, 4);
        stepIncrement = readOptional(queue, UnsignedInteger.class, 5);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        ColorCommand that = (ColorCommand) o;
        return Objects.equals(operation, that.operation) && Objects.equals(targetColor,
                that.targetColor) && Objects.equals(targetColorTemperature,
                that.targetColorTemperature) && Objects.equals(fadeTime,
                that.fadeTime) && Objects.equals(rampRate, that.rampRate) && Objects.equals(
                stepIncrement, that.stepIncrement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, targetColor, targetColorTemperature, fadeTime, rampRate, stepIncrement);
    }
}
