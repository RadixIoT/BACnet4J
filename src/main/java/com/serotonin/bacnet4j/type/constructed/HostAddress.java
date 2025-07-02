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
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class HostAddress extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, Null.class); // 0
        choiceOptions.addContextual(1, OctetString.class); // 1
        choiceOptions.addContextual(2, CharacterString.class); // 2
    }

    private final Choice state;

    public HostAddress(final Null none) {
        state = new Choice(0, none, choiceOptions);
    }

    public HostAddress(final OctetString ipAddress) {
        state = new Choice(1, ipAddress, choiceOptions);
    }

    public HostAddress(final CharacterString name) {
        state = new Choice(2, name, choiceOptions);
    }

    public boolean isIpAddress() {
        return this.state.getDatum() instanceof OctetString;
    }

    public OctetString getIpAddress() {
        return this.state.getDatum();
    }

    public boolean isName() {
        return this.state.getDatum() instanceof CharacterString;
    }

    public CharacterString getName() {
        return this.state.getDatum();
    }

    @SuppressWarnings("unchecked")
    public <T extends Encodable> T getAddress() {
        return (T) state.getDatum();
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, state);
    }

    public HostAddress(final ByteQueue queue) throws BACnetException {
        state = new Choice(queue, choiceOptions);
    }

    @Override
    public String toString() {
        return "HostAddress [state=" + state + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (state == null ? 0 : state.hashCode());
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
        final HostAddress other = (HostAddress) obj;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        return true;
    }
}
