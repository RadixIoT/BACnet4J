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

package com.serotonin.bacnet4j.persistence;

import java.io.File;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public interface IPersistence {
    void save(String key, String value);

    default void saveEncodable(final String key, final Encodable value) {
        final ByteQueue queue = new ByteQueue();
        value.write(queue);
        save(key, queue.toHexString());
    }

    String load(String key);

    default <T extends Encodable> T loadEncodable(final String key, final Class<T> clazz) {
        final String value = load(key);
        if (value == null)
            return null;
        final ByteQueue queue = new ByteQueue(value);
        try {
            return Encodable.read(queue, clazz);
        } catch (final BACnetException e) {
            throw new BACnetRuntimeException(e);
        }
    }

    default <T extends Encodable> SequenceOf<T> loadSequenceOf(final String key, final Class<T> clazz) {
        final String value = load(key);
        if (value == null)
            return null;
        final ByteQueue queue = new ByteQueue(value);
        try {
            return Encodable.readSequenceOf(queue, clazz);
        } catch (final BACnetException e) {
            throw new BACnetRuntimeException(e);
        }
    }

    void remove(String key);

    /**
     * Provide the list of persistence files, if any, for backups.
     *
     * @return list of file, or null.
     */
    default File[] getFiles() {
        return null;
    }
}
