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

package com.serotonin.bacnet4j.service.confirmed;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.logBuffer.LogBuffer;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.acknowledgement.ReadRangeAck;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.ResultFlags;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * @author Matthew
 */
public class ReadRangeRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 26;

    // TODO make this configurable, say in LocalDevice.
    private static final int MAX_ITEMS_RETURNED = 200;

    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(3, ByPosition.class);
        choiceOptions.addContextual(6, BySequenceNumber.class);
        choiceOptions.addContextual(7, ByTime.class);
    }

    private final ObjectIdentifier objectIdentifier;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger propertyArrayIndex;
    private final Choice range;

    public ReadRangeRequest(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex) {
        this(objectIdentifier, propertyIdentifier, propertyArrayIndex, (Choice) null);
    }

    public ReadRangeRequest(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex, final ByPosition range) {
        this(objectIdentifier, propertyIdentifier, propertyArrayIndex, new Choice(3, range, choiceOptions));
    }

    public ReadRangeRequest(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex, final BySequenceNumber range) {
        this(objectIdentifier, propertyIdentifier, propertyArrayIndex, new Choice(6, range, choiceOptions));
    }

    public ReadRangeRequest(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex, final ByTime range) {
        this(objectIdentifier, propertyIdentifier, propertyArrayIndex, new Choice(7, range, choiceOptions));
    }

    private ReadRangeRequest(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex, final Choice range) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
        this.range = range;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, propertyIdentifier, 1);
        writeOptional(queue, propertyArrayIndex, 2);
        writeOptional(queue, range);
    }

    public ReadRangeRequest(final ByteQueue queue) throws BACnetException {
        objectIdentifier = read(queue, ObjectIdentifier.class, 0);
        propertyIdentifier = read(queue, PropertyIdentifier.class, 1);
        propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 2);
        range = readOptionalChoice(queue, choiceOptions);
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
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

    public boolean isByPosition() {
        return range.getContextId() == 3;
    }

    public ByPosition getByPosition() {
        return range.getDatum();
    }

    public boolean isBySequenceNumber() {
        return range.getContextId() == 6;
    }

    public BySequenceNumber getBySequenceNumber() {
        return range.getDatum();
    }

    public boolean isByTime() {
        return range.getContextId() == 7;
    }

    public ByTime getByTime() {
        return range.getDatum();
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        try {
            // Property identifier cannot be all, required, or optional.
            if (propertyIdentifier.isOneOf(PropertyIdentifier.all, PropertyIdentifier.required,
                    PropertyIdentifier.optional))
                throw new BACnetServiceException(ErrorClass.services, ErrorCode.parameterOutOfRange);
            // Property array index, if provided, cannot be 0.
            if (propertyArrayIndex != null && propertyArrayIndex.intValue() == 0)
                throw new BACnetServiceException(ErrorClass.services, ErrorCode.parameterOutOfRange);
            // Count, if provided, cannot be zero.
            if (range != null && ((Range) range.getDatum()).getCount().intValue() == 0)
                throw new BACnetServiceException(ErrorClass.services, ErrorCode.parameterOutOfRange);

            // Find the object.
            final BACnetObject obj = localDevice.getObjectRequired(objectIdentifier);

            Encodable prop = obj.get(propertyIdentifier);
            if (prop == null)
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty);

            // Special handling for LogBuffer objects.
            if (prop instanceof LogBuffer) {
                if (propertyArrayIndex != null)
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.propertyIsNotAnArray);
            } else {
                // Generic handling.
                prop = obj.readPropertyRequired(propertyIdentifier, propertyArrayIndex);

                // Ensure this is a list.
                if (!(prop instanceof SequenceOf))
                    throw new BACnetServiceException(ErrorClass.services, ErrorCode.propertyIsNotAList);
            }

            ReadRangeAck ack = readRange((RangeReadable<?>) prop);
            if (ack == null) {
                // Assume no results to return.
                ack = new ReadRangeAck(objectIdentifier, propertyIdentifier, propertyArrayIndex,
                        new ResultFlags(false, false, false), UnsignedInteger.ZERO, new SequenceOf<>(), null);
            }
            return ack;
        } catch (final BACnetServiceException e) {
            throw new BACnetErrorException(getChoiceId(), e);
        }
    }

    private ReadRangeAck readRange(final RangeReadable<?> list) throws BACnetServiceException {
        if (list.size() == 0) {
            return null;
        }

        // Handle the choice
        if (range == null)
            return readRangeDefault(list);
        if (isByPosition())
            return readRangeByPosition(list, getByPosition());
        if (isBySequenceNumber())
            return readRangeBySequenceNumber(list, getBySequenceNumber());
        if (isByTime())
            return readRangeByTime(list, getByTime());

        // Should never happen.
        throw new RuntimeException("No handing for choice: " + range);
    }

    private ReadRangeAck readRangeDefault(final RangeReadable<?> list) {
        final SequenceOf<Encodable> data;
        final ResultFlags resultFlags;

        synchronized (list) {
            int readCount = list.size();
            boolean readAll = true;
            if (readCount > MAX_ITEMS_RETURNED) {
                readCount = MAX_ITEMS_RETURNED;
                readAll = false;
            }

            resultFlags = new ResultFlags(true, readAll, !readAll);

            data = new SequenceOf<>(readCount);
            for (int i = 0; i < readCount; i++) {
                data.add((Encodable) list.get(i));
            }
        }

        // Return the result.
        return new ReadRangeAck(objectIdentifier, propertyIdentifier, propertyArrayIndex, resultFlags,
                new UnsignedInteger(data.size()), data, null);
    }

    private ReadRangeAck readRangeByPosition(final RangeReadable<?> list, final ByPosition position) {
        synchronized (list) {
            // Check if the reference index is in the range of the list.
            final int pos = position.getReferenceIndex().intValue();
            final int size = list.size();
            if (pos < 1 || pos > size)
                return null;

            return readAroundIndex(list, pos - 1, position);
        }
    }

    private ReadRangeAck readRangeBySequenceNumber(final RangeReadable<?> list, final BySequenceNumber sequenceNumber)
            throws BACnetServiceException {
        synchronized (list) {
            // Check again that the list isn't empty. Shouldn't really happen. Just here because the list was only
            // synchronized just now, and there is a very small possibility that it may have since changed.
            if (list.size() == 0)
                return null;

            // Ensure that this list contains Sequenced elements.
            if (!(list.get(0) instanceof Sequenced))
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.datatypeNotSupported);

            // Use a binary search to find the index of the record we need.
            @SuppressWarnings("unchecked") final int pos =
                    binarySearch((RangeReadable<Sequenced>) list, new Sequenced() {
                        @Override
                        public long getSequenceNumber() {
                            return sequenceNumber.getReferenceIndex().longValue();
                        }
                    });

            // Check if the reference index is in the range of the list.
            if (pos < 0)
                return null;

            return readAroundIndex(list, pos, sequenceNumber);
        }
    }

    private ReadRangeAck readRangeByTime(final RangeReadable<?> list, final ByTime time) throws BACnetServiceException {
        // Make sure the given timestamp is fully specified.
        if (!time.getReferenceTime().isFullySpecified())
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.datatypeNotSupported);

        synchronized (list) {
            final int size = list.size();

            // Check again that the list isn't empty. Shouldn't really happen. Just here because the list was only
            // synchronized just now, and there is a very small possibility that it may have since changed.
            if (size == 0)
                return null;

            // Ensure that this list contains Sequenced and Timestamped elements.
            if (!(list.get(0) instanceof Sequenced))
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.datatypeNotSupported);
            if (!(list.get(0) instanceof Timestamped))
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.datatypeNotSupported);

            // Use a binary search to find the index of the record we need.
            @SuppressWarnings("unchecked")
            int pos = binarySearch((RangeReadable<Timestamped>) list, new Timestamped() {
                @Override
                public DateTime getTimestamp() {
                    return time.getReferenceTime();
                }
            });

            // Check if the reference index is in the range of the list.
            final int count = time.getCount().intValue();
            if (pos >= 0) {
                // Found the exact timestamp.
                if (count > 0) {
                    // We use the record with a timestamp newer than the time specified.
                    pos++;
                } else {
                    // We use the newest record with timestamp older than the time specified.
                    pos--;
                }
            } else {
                // Didn't find the exact timestamp. The -position-1 is the insertion point.
                pos = -pos - 1;
                if (count < 0)
                    pos--;
            }

            // Make sure we're still in range.
            if (pos < 0 || pos >= size)
                return null;

            return readAroundIndex(list, pos, time);
        }
    }

    /**
     * The list should already be synchronized by now.
     *
     * @param list
     * @param index the 0-based index
     * @param range
     * @return
     */
    private ReadRangeAck readAroundIndex(final RangeReadable<?> list, final int index, final Range range) {
        final int size = list.size();

        // Start and end indices are both inclusive
        int startIndex;
        int endIndex;
        boolean moreItems = false;

        int maxRead = range.getCount().intValue();
        if (maxRead < 0) {
            endIndex = index;

            maxRead = -maxRead;
            if (maxRead > MAX_ITEMS_RETURNED) {
                maxRead = MAX_ITEMS_RETURNED;
                moreItems = true;
            }

            startIndex = endIndex - maxRead + 1;
            if (startIndex <= 0) {
                startIndex = 0;
                moreItems = false;
            }
        } else {
            startIndex = index;

            if (maxRead > MAX_ITEMS_RETURNED) {
                maxRead = MAX_ITEMS_RETURNED;
                moreItems = true;
            }

            endIndex = startIndex + maxRead - 1;
            if (endIndex >= size - 1) {
                endIndex = size - 1;
                moreItems = false;
            }
        }

        final ResultFlags resultFlags = new ResultFlags(startIndex == 0, endIndex == size - 1, moreItems);

        final SequenceOf<Encodable> data = new SequenceOf<>(endIndex - startIndex + 1);
        for (int i = startIndex; i <= endIndex; i++) {
            data.add((Encodable) list.get(i));
        }

        UnsignedInteger firstSequenceNumber = null;
        if (data.getBase1(1) instanceof Sequenced) {
            firstSequenceNumber = new UnsignedInteger(((Sequenced) data.getBase1(1)).getSequenceNumber());
        }

        // Return the result.
        return new ReadRangeAck(objectIdentifier, propertyIdentifier, propertyArrayIndex, resultFlags,
                new UnsignedInteger(data.size()), data, firstSequenceNumber);
    }

    private static <T> int binarySearch(final RangeReadable<? extends RangeComparable> list, final T key) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            final int mid = low + high >>> 1;
            final RangeComparable midVal = list.get(mid);
            final int cmp;
            if (key instanceof Timestamped) {
                cmp = ((Timestamped) midVal).compareTimestamp((Timestamped) key);
            } else if (key instanceof Sequenced) {
                cmp = ((Sequenced) midVal).compareSequenceNumbero((Sequenced) key);
            } else {
                // Should not happen.
                throw new RuntimeException("Key is not a valid class: " + key.getClass());
            }

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }

        return -(low + 1); // key not found
    }

    /**
     * Interface that is applied to list types that are readable via this service. Currently implemented by LogBuffer
     * and SequenceOf
     */
    public static interface RangeReadable<E> {
        int size();

        E get(int index);
    }


    /**
     * Allows Timestamped and Sequenced to be compared generically.
     */
    static interface RangeComparable {
        // no op
    }


    /**
     * Implemented by elements of RangeReadable that have timestamps. Currently implemented by ILogRecord.
     */
    public static interface Timestamped extends RangeComparable {
        DateTime getTimestamp();

        default int compareTimestamp(final Timestamped that) {
            return getTimestamp().compareTo(that.getTimestamp());
        }
    }


    /**
     * Implemented by elements of RangeReadable that have sequence numbers. Currently implemented by ILogRecord.
     */
    public static interface Sequenced extends RangeComparable {
        long getSequenceNumber();

        default int compareSequenceNumbero(final Sequenced that) {
            return Long.compare(getSequenceNumber(), that.getSequenceNumber());
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
        result = PRIME * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
        result = PRIME * result + (range == null ? 0 : range.hashCode());
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
        final ReadRangeRequest other = (ReadRangeRequest) obj;
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
        if (range == null) {
            if (other.range != null)
                return false;
        } else if (!range.equals(other.range))
            return false;
        return true;
    }

    abstract public static class Range extends BaseType {
        protected SignedInteger count;

        public Range(final SignedInteger count) {
            this.count = count;
        }

        Range() {
            // no op
        }

        public SignedInteger getCount() {
            return count;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (count == null ? 0 : count.hashCode());
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
            final Range other = (Range) obj;
            if (count == null) {
                if (other.count != null)
                    return false;
            } else if (!count.equals(other.count))
                return false;
            return true;
        }
    }


    public static class ByPosition extends Range {
        private final UnsignedInteger referenceIndex;

        public ByPosition(final int referenceIndex, final int count) {
            this(new UnsignedInteger(referenceIndex), new SignedInteger(count));
        }

        public ByPosition(final UnsignedInteger referenceIndex, final SignedInteger count) {
            super(count);
            this.referenceIndex = referenceIndex;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, referenceIndex);
            write(queue, count);
        }

        public ByPosition(final ByteQueue queue) throws BACnetException {
            referenceIndex = read(queue, UnsignedInteger.class);
            count = read(queue, SignedInteger.class);
        }

        public UnsignedInteger getReferenceIndex() {
            return referenceIndex;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + (referenceIndex == null ? 0 : referenceIndex.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ByPosition other = (ByPosition) obj;
            if (referenceIndex == null) {
                if (other.referenceIndex != null)
                    return false;
            } else if (!referenceIndex.equals(other.referenceIndex))
                return false;
            return true;
        }
    }


    public static class BySequenceNumber extends Range {
        private final UnsignedInteger referenceIndex;

        public BySequenceNumber(final long referenceIndex, final int count) {
            this(new UnsignedInteger(referenceIndex), new SignedInteger(count));
        }

        public BySequenceNumber(final UnsignedInteger referenceIndex, final SignedInteger count) {
            super(count);
            this.referenceIndex = referenceIndex;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, referenceIndex);
            write(queue, count);
        }

        public BySequenceNumber(final ByteQueue queue) throws BACnetException {
            referenceIndex = read(queue, UnsignedInteger.class);
            count = read(queue, SignedInteger.class);
        }

        public UnsignedInteger getReferenceIndex() {
            return referenceIndex;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + (referenceIndex == null ? 0 : referenceIndex.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            final BySequenceNumber other = (BySequenceNumber) obj;
            if (referenceIndex == null) {
                if (other.referenceIndex != null)
                    return false;
            } else if (!referenceIndex.equals(other.referenceIndex))
                return false;
            return true;
        }
    }


    public static class ByTime extends Range {
        private final DateTime referenceTime;

        public ByTime(final DateTime referenceTime, final int count) {
            this(referenceTime, new SignedInteger(count));
        }

        public ByTime(final DateTime referenceTime, final SignedInteger count) {
            super(count);
            this.referenceTime = referenceTime;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, referenceTime);
            write(queue, count);
        }

        public ByTime(final ByteQueue queue) throws BACnetException {
            referenceTime = read(queue, DateTime.class);
            count = read(queue, SignedInteger.class);
        }

        public DateTime getReferenceTime() {
            return referenceTime;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + (referenceTime == null ? 0 : referenceTime.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ByTime other = (ByTime) obj;
            if (referenceTime == null) {
                if (other.referenceTime != null)
                    return false;
            } else if (!referenceTime.equals(other.referenceTime))
                return false;
            return true;
        }
    }
}
