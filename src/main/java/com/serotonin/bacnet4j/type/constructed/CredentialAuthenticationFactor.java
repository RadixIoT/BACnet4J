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
import com.serotonin.bacnet4j.type.enumerated.AccessAuthenticationFactorDisable;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class CredentialAuthenticationFactor extends BaseType {
    private final AccessAuthenticationFactorDisable disable;
    private final AuthenticationFactor authenticationFactor;

    public CredentialAuthenticationFactor(final AccessAuthenticationFactorDisable disable,
            final AuthenticationFactor authenticationFactor) {
        this.disable = disable;
        this.authenticationFactor = authenticationFactor;
    }

    public AccessAuthenticationFactorDisable getDisable() {
        return disable;
    }

    public AuthenticationFactor getAuthenticationFactor() {
        return authenticationFactor;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, disable);
        write(queue, authenticationFactor);
    }

    public CredentialAuthenticationFactor(final ByteQueue queue) throws BACnetException {
        disable = read(queue, AccessAuthenticationFactorDisable.class);
        authenticationFactor = read(queue, AuthenticationFactor.class);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (authenticationFactor == null ? 0 : authenticationFactor.hashCode());
        result = prime * result + (disable == null ? 0 : disable.hashCode());
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
        final CredentialAuthenticationFactor other = (CredentialAuthenticationFactor) obj;
        if (authenticationFactor == null) {
            if (other.authenticationFactor != null)
                return false;
        } else if (!authenticationFactor.equals(other.authenticationFactor))
            return false;
        if (disable == null) {
            if (other.disable != null)
                return false;
        } else if (!disable.equals(other.disable))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CredentialAuthenticationFactor [disable=" + disable + ", authenticationFactor=" + authenticationFactor + ']';
    }
}
