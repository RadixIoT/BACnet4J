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
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class Health extends BaseType {
    private final DateTime timestamp; // 0
    private final ErrorClassAndCode result; // 1
    private final PropertyIdentifier property; // 2 optional
    private final CharacterString details; // 3 optional

    public Health(DateTime timestamp, ErrorClassAndCode result, PropertyIdentifier property, CharacterString details) {
        this.timestamp = timestamp;
        this.result = result;
        this.property = property;
        this.details = details;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, result, 1);
        writeOptional(queue, property, 2);
        writeOptional(queue, details, 3);
    }

    @Override
    public String toString() {
        return "Health [" +
                "timestamp=" + timestamp +
                ", result=" + result +
                ", property=" + property +
                ", details=" + details +
                ']';
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public ErrorClassAndCode getResult() {
        return result;
    }

    public PropertyIdentifier getProperty() {
        return property;
    }

    public CharacterString getDetails() {
        return details;
    }

    public Health(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        result = read(queue, ErrorClassAndCode.class, 1);
        property = readOptional(queue, PropertyIdentifier.class, 2);
        details = readOptional(queue, CharacterString.class, 3);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Health health = (Health) o;
        return Objects.equals(timestamp, health.timestamp) &&
                Objects.equals(result, health.result) &&
                Objects.equals(property, health.property) &&
                Objects.equals(details, health.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, result, property, details);
    }
}
