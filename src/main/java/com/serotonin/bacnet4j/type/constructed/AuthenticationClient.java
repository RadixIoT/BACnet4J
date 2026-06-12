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
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthenticationClient extends BaseType {
    private final Boolean authenticated;
    private final Unsigned32 device;

    public AuthenticationClient(Boolean authenticated, Unsigned32 device) {
        this.authenticated = authenticated;
        this.device = device;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, authenticated);
        write(queue, device);
    }

    @Override
    public String toString() {
        return "AuthenticationClient [" +
                "authenticated=" + authenticated +
                ", device=" + device +
                ']';
    }

    public Boolean getAuthenticated() {
        return authenticated;
    }

    public Unsigned32 getDevice() {
        return device;
    }

    public AuthenticationClient(final ByteQueue queue) throws BACnetException {
        authenticated = read(queue, Boolean.class);
        device = read(queue, Unsigned32.class);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthenticationClient that = (AuthenticationClient) o;
        return Objects.equals(authenticated, that.authenticated) && Objects.equals(device, that.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authenticated, device);
    }
}
