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
import com.serotonin.bacnet4j.type.enumerated.LightingOperation;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class LightingCommand extends BaseType {
    private final LightingOperation operation;
    private final Real targetLevel;
    private final Real rampRate;
    private final Real stepIncrement;
    private final UnsignedInteger fadeTime;
    private final UnsignedInteger priority;

    public LightingCommand(final LightingOperation operation, final Real targetLevel, final Real rampRate,
            final Real stepIncrement, final UnsignedInteger fadeTime, final UnsignedInteger priority) {
        this.operation = operation;
        this.targetLevel = targetLevel;
        this.rampRate = rampRate;
        this.stepIncrement = stepIncrement;
        this.fadeTime = fadeTime;
        this.priority = priority;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, operation, 0);
        writeOptional(queue, targetLevel, 1);
        writeOptional(queue, rampRate, 2);
        writeOptional(queue, stepIncrement, 3);
        writeOptional(queue, fadeTime, 4);
        writeOptional(queue, priority, 5);
    }

    public LightingCommand(final ByteQueue queue) throws BACnetException {
        operation = read(queue, LightingOperation.class, 0);
        targetLevel = readOptional(queue, Real.class, 1);
        rampRate = readOptional(queue, Real.class, 2);
        stepIncrement = readOptional(queue, Real.class, 3);
        fadeTime = readOptional(queue, UnsignedInteger.class, 4);
        priority = readOptional(queue, UnsignedInteger.class, 5);
    }

    public LightingOperation getOperation() {
        return operation;
    }

    public Real getTargetLevel() {
        return targetLevel;
    }

    public Real getRampRate() {
        return rampRate;
    }

    public Real getStepIncrement() {
        return stepIncrement;
    }

    public UnsignedInteger getFadeTime() {
        return fadeTime;
    }

    public UnsignedInteger getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "LightingCommand [operation=" + operation + ", targetLevel=" + targetLevel + ", rampRate=" + rampRate
                + ", stepIncrement=" + stepIncrement + ", fadeTime=" + fadeTime + ", priority=" + priority + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (fadeTime == null ? 0 : fadeTime.hashCode());
        result = prime * result + (operation == null ? 0 : operation.hashCode());
        result = prime * result + (priority == null ? 0 : priority.hashCode());
        result = prime * result + (rampRate == null ? 0 : rampRate.hashCode());
        result = prime * result + (stepIncrement == null ? 0 : stepIncrement.hashCode());
        result = prime * result + (targetLevel == null ? 0 : targetLevel.hashCode());
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
        final LightingCommand other = (LightingCommand) obj;
        if (fadeTime == null) {
            if (other.fadeTime != null)
                return false;
        } else if (!fadeTime.equals(other.fadeTime))
            return false;
        if (operation == null) {
            if (other.operation != null)
                return false;
        } else if (!operation.equals(other.operation))
            return false;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
        if (rampRate == null) {
            if (other.rampRate != null)
                return false;
        } else if (!rampRate.equals(other.rampRate))
            return false;
        if (stepIncrement == null) {
            if (other.stepIncrement != null)
                return false;
        } else if (!stepIncrement.equals(other.stepIncrement))
            return false;
        if (targetLevel == null) {
            if (other.targetLevel != null)
                return false;
        } else if (!targetLevel.equals(other.targetLevel))
            return false;
        return true;
    }
}
