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
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthorizationScopeDescription extends BaseType {
    private final CharacterString name;
    private final CharacterString description;

    public AuthorizationScopeDescription(
            CharacterString name,
            CharacterString description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, name);
        write(queue, description);
    }

    @Override
    public String toString() {
        return "AuthorizationScopeDescription [" +
                "name=" + name +
                ", description=" + description +
                ']';
    }

    public CharacterString getName() {
        return name;
    }

    public CharacterString getDescription() {
        return description;
    }

    public AuthorizationScopeDescription(ByteQueue queue) throws BACnetException {
        name = read(queue, CharacterString.class);
        description = read(queue, CharacterString.class);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthorizationScopeDescription that = (AuthorizationScopeDescription) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
