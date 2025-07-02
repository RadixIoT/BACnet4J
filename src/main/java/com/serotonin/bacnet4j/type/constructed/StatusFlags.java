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

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class StatusFlags extends BitString {
    public StatusFlags(final boolean inAlarm, final boolean fault, final boolean overridden,
            final boolean outOfService) {
        super(new boolean[] {inAlarm, fault, overridden, outOfService});
    }

    public StatusFlags(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean isInAlarm() {
        return getValue()[0];
    }

    public void setInAlarm(final boolean b) {
        getValue()[0] = b;
    }

    public boolean isFault() {
        return getValue()[1];
    }

    public void setFault(final boolean b) {
        getValue()[1] = b;
    }

    public boolean isOverridden() {
        return getValue()[2];
    }

    public void setOverridden(final boolean b) {
        getValue()[2] = b;
    }

    public boolean isOutOfService() {
        return getValue()[3];
    }

    public void setOutOfService(final boolean b) {
        getValue()[3] = b;
    }

    @Override
    public String toString() {
        return "StatusFlags [in-alarm=" + isInAlarm() + ", fault=" + isFault() + ", overridden=" + isOverridden() + ", out-of-service=" + isOutOfService() + "]";
    }
}
