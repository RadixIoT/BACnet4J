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

package com.serotonin.bacnet4j.type.notificationParameters;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfValueNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 2;

    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, BitString.class);
        choiceOptions.addContextual(1, Real.class);
    }

    private final Choice newValue;
    private final StatusFlags statusFlags;

    public ChangeOfValueNotif(final BitString newValue, final StatusFlags statusFlags) {
        this.newValue = new Choice(0, newValue, choiceOptions);
        this.statusFlags = statusFlags;
    }

    public ChangeOfValueNotif(final Real newValue, final StatusFlags statusFlags) {
        this.newValue = new Choice(1, newValue, choiceOptions);
        this.statusFlags = statusFlags;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, newValue, 0);
        write(queue, statusFlags, 1);
    }

    public ChangeOfValueNotif(final ByteQueue queue) throws BACnetException {
        newValue = new Choice(queue, choiceOptions, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
    }

    public Choice getNewValue() {
        return newValue;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    @Override
    public String toString() {
        return "ChangeOfValueNotif[ newValue=" + newValue + ", statusFlags=" + statusFlags + ']';
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (newValue == null ? 0 : newValue.hashCode());
        result = PRIME * result + (statusFlags == null ? 0 : statusFlags.hashCode());
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
        final ChangeOfValueNotif other = (ChangeOfValueNotif) obj;
        if (newValue == null) {
            if (other.newValue != null)
                return false;
        } else if (!newValue.equals(other.newValue))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
