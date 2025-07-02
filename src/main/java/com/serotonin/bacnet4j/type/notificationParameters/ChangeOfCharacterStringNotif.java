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
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ChangeOfCharacterStringNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 17;

    private final CharacterString changedValue;
    private final StatusFlags statusFlags;
    private final CharacterString alarmValues;

    public ChangeOfCharacterStringNotif(final CharacterString changedValue, final StatusFlags statusFlags,
            final CharacterString alarmValues) {
        this.changedValue = changedValue;
        this.statusFlags = statusFlags;
        this.alarmValues = alarmValues;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, changedValue, 0);
        write(queue, statusFlags, 1);
        write(queue, alarmValues, 2);
    }

    public ChangeOfCharacterStringNotif(final ByteQueue queue) throws BACnetException {
        changedValue = read(queue, CharacterString.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        alarmValues = read(queue, CharacterString.class, 2);
    }

    public CharacterString getChangedValue() {
        return changedValue;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public CharacterString getAlarmValues() {
        return alarmValues;
    }

    @Override
    public String toString() {
        return "ChangeOfCharacterStringNotif[ changedValue=" + changedValue + ", statusFlags=" + statusFlags + ", alarmValues=" + alarmValues + ']';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (alarmValues == null ? 0 : alarmValues.hashCode());
        result = prime * result + (changedValue == null ? 0 : changedValue.hashCode());
        result = prime * result + (statusFlags == null ? 0 : statusFlags.hashCode());
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
        final ChangeOfCharacterStringNotif other = (ChangeOfCharacterStringNotif) obj;
        if (alarmValues == null) {
            if (other.alarmValues != null)
                return false;
        } else if (!alarmValues.equals(other.alarmValues))
            return false;
        if (changedValue == null) {
            if (other.changedValue != null)
                return false;
        } else if (!changedValue.equals(other.changedValue))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
