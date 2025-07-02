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

package com.serotonin.bacnet4j.service.acknowledgement;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ResultFlags;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ReadRangeAck extends AcknowledgementService {
    public static final byte TYPE_ID = 26;

    private final ObjectIdentifier objectIdentifier;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger propertyArrayIndex;
    private final ResultFlags resultFlags;
    private final UnsignedInteger itemCount;
    private final SequenceOf<? extends Encodable> itemData;
    private final UnsignedInteger firstSequenceNumber;

    public ReadRangeAck(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex, final ResultFlags resultFlags, final UnsignedInteger itemCount,
            final SequenceOf<? extends Encodable> itemData, final UnsignedInteger firstSequenceNumber) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
        this.resultFlags = resultFlags;
        this.itemCount = itemCount;
        this.itemData = itemData;
        this.firstSequenceNumber = firstSequenceNumber;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, propertyIdentifier, 1);
        writeOptional(queue, propertyArrayIndex, 2);
        write(queue, resultFlags, 3);
        write(queue, itemCount, 4);
        write(queue, itemData, 5);
        writeOptional(queue, firstSequenceNumber, 6);
    }

    ReadRangeAck(final ByteQueue queue) throws BACnetException {
        objectIdentifier = read(queue, ObjectIdentifier.class, 0);
        propertyIdentifier = read(queue, PropertyIdentifier.class, 1);
        propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 2);
        resultFlags = read(queue, ResultFlags.class, 3);
        itemCount = read(queue, UnsignedInteger.class, 4);
        itemData = readSequenceOfANY(queue, objectIdentifier.getObjectType(), propertyIdentifier, 5);
        firstSequenceNumber = readOptional(queue, UnsignedInteger.class, 6);
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public PropertyIdentifier getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public UnsignedInteger getPropertyArrayIndex() {
        return propertyArrayIndex;
    }

    public ResultFlags getResultFlags() {
        return resultFlags;
    }

    public UnsignedInteger getItemCount() {
        return itemCount;
    }

    public SequenceOf<? extends Encodable> getItemData() {
        return itemData;
    }

    public UnsignedInteger getFirstSequenceNumber() {
        return firstSequenceNumber;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (firstSequenceNumber == null ? 0 : firstSequenceNumber.hashCode());
        result = PRIME * result + (itemCount == null ? 0 : itemCount.hashCode());
        result = PRIME * result + (itemData == null ? 0 : itemData.hashCode());
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
        result = PRIME * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
        result = PRIME * result + (resultFlags == null ? 0 : resultFlags.hashCode());
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
        final ReadRangeAck other = (ReadRangeAck) obj;
        if (firstSequenceNumber == null) {
            if (other.firstSequenceNumber != null)
                return false;
        } else if (!firstSequenceNumber.equals(other.firstSequenceNumber))
            return false;
        if (itemCount == null) {
            if (other.itemCount != null)
                return false;
        } else if (!itemCount.equals(other.itemCount))
            return false;
        if (itemData == null) {
            if (other.itemData != null)
                return false;
        } else if (!itemData.equals(other.itemData))
            return false;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        if (propertyArrayIndex == null) {
            if (other.propertyArrayIndex != null)
                return false;
        } else if (!propertyArrayIndex.equals(other.propertyArrayIndex))
            return false;
        if (propertyIdentifier == null) {
            if (other.propertyIdentifier != null)
                return false;
        } else if (!propertyIdentifier.equals(other.propertyIdentifier))
            return false;
        if (resultFlags == null) {
            if (other.resultFlags != null)
                return false;
        } else if (!resultFlags.equals(other.resultFlags))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ReadRangeAck [objectIdentifier=" + objectIdentifier + ", propertyIdentifier=" + propertyIdentifier
                + ", propertyArrayIndex=" + propertyArrayIndex + ", resultFlags=" + resultFlags + ", itemCount="
                + itemCount + ", itemData=" + itemData + ", firstSequenceNumber=" + firstSequenceNumber + "]";
    }

}
