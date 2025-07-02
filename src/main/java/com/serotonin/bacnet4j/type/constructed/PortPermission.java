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
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class PortPermission extends BaseType {
    private final Unsigned8 portId;
    private final Boolean enabled;

    public PortPermission(final Unsigned8 portId, final Boolean enabled) {
        this.portId = portId;
        this.enabled = enabled;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, portId, 0);
        write(queue, enabled, 1);
    }

    public PortPermission(final ByteQueue queue) throws BACnetException {
        portId = read(queue, Unsigned8.class, 0);
        enabled = read(queue, Boolean.class, 1);
    }

    public Unsigned8 getPortId() {
        return portId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "PortPermission [portId=" + portId + ", enabled=" + enabled + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enabled == null ? 0 : enabled.hashCode());
        result = prime * result + (portId == null ? 0 : portId.hashCode());
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
        final PortPermission other = (PortPermission) obj;
        if (enabled == null) {
            if (other.enabled != null)
                return false;
        } else if (!enabled.equals(other.enabled))
            return false;
        if (portId == null) {
            if (other.portId != null)
                return false;
        } else if (!portId.equals(other.portId))
            return false;
        return true;
    }
}
