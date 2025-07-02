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

package com.serotonin.bacnet4j.obj.mixin.event;

import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class StateTransition {
    private final EventState toState;
    private final UnsignedInteger delay;

    public StateTransition(final EventState toState, final UnsignedInteger delay) {
        this.toState = toState;
        this.delay = delay;
    }

    public EventState getToState() {
        return toState;
    }

    public UnsignedInteger getDelay() {
        return delay;
    }

    @Override
    public String toString() {
        return "StateTransition [toState=" + toState + ", delay=" + delay + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (delay == null ? 0 : delay.hashCode());
        result = prime * result + (toState == null ? 0 : toState.hashCode());
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
        final StateTransition other = (StateTransition) obj;
        if (delay == null) {
            if (other.delay != null)
                return false;
        } else if (!delay.equals(other.delay))
            return false;
        if (toState == null) {
            if (other.toState != null)
                return false;
        } else if (!toState.equals(other.toState))
            return false;
        return true;
    }
}
