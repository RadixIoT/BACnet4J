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

package com.serotonin.bacnet4j.type.primitive;

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ObjectIdentifier extends Primitive {
    public static final int UNINITIALIZED = 4194303;
    public static final byte TYPE_ID = 12;

    private final ObjectType objectType;
    private int instanceNumber;

    public ObjectIdentifier(int objectType, int instanceNumber) {
        this(ObjectType.forId(objectType), instanceNumber);
    }

    public ObjectIdentifier(ObjectType objectType, int instanceNumber) {
        Objects.requireNonNull(objectType);

        if (instanceNumber < 0 || instanceNumber > UNINITIALIZED)
            throw new IllegalArgumentException("Illegal instance number: " + instanceNumber);

        this.objectType = objectType;
        this.instanceNumber = instanceNumber;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public int getInstanceNumber() {
        return instanceNumber;
    }

    public boolean isUninitialized() {
        return instanceNumber == UNINITIALIZED;
    }

    @Override
    public String toString() {
        return objectType.toString() + " " + instanceNumber;
    }

    //
    // Reading and writing
    //
    public ObjectIdentifier(ByteQueue queue) throws BACnetErrorException {
        readTag(queue, TYPE_ID);

        int type = queue.popU1B() << 2;
        int i = queue.popU1B();
        type |= i >> 6;

        this.objectType = ObjectType.forId(type);

        instanceNumber = (i & 0x3f) << 16;
        instanceNumber |= queue.popU1B() << 8;
        instanceNumber |= queue.popU1B();
    }

    @Override
    public void writeImpl(ByteQueue queue) {
        int objectType = this.objectType.intValue();
        queue.push(objectType >> 2);
        queue.push((objectType & 3) << 6 | instanceNumber >> 16);
        queue.push(instanceNumber >> 8);
        queue.push(instanceNumber);
    }

    @Override
    protected long getLength() {
        return 4;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        ObjectIdentifier that = (ObjectIdentifier) o;
        return instanceNumber == that.instanceNumber && Objects.equals(objectType, that.objectType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectType, instanceNumber);
    }
}
