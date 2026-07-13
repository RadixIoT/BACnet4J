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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
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
import com.serotonin.bacnet4j.type.primitive.SignedInteger16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ReadRangeRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 26;

    // This could be configurable, say in LocalDevice.
    private static final int MAX_ITEMS_RETURNED = 200;

    private static final ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(3, ByPosition.class);
        choiceOptions.addContextual(6, BySequenceNumber.class);
        choiceOptions.addContextual(7, ByTime.class);
    }

    private final ObjectIdentifier objectIdentifier;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger propertyArrayIndex;
    private final Choice range;

    public ReadRangeRequest(ObjectIdentifier objectIdentifier, PropertyIdentifier propertyIdentifier,
            UnsignedInteger propertyArrayIndex) {
        this(objectIdentifier, propertyIdentifier, propertyArrayIndex, (Choice) null);
    }

    public ReadRangeRequest(ObjectIdentifier objectIdentifier, PropertyIdentifier propertyIdentifier,
            UnsignedInteger propertyArrayIndex, ByPosition range) {
        this(objectIdentifier, propertyIdentifier, propertyArrayIndex, new Choice(3, range, choiceOptions));
    }

    public ReadRangeRequest(ObjectIdentifier objectIdentifier, PropertyIdentifier propertyIdentifier,
            UnsignedInteger propertyArrayIndex, BySequenceNumber range) {
        this(objectIdentifier, propertyIdentifier, propertyArrayIndex, new Choice(6, range, choiceOptions));
    }

    public ReadRangeRequest(ObjectIdentifier objectIdentifier, PropertyIdentifier propertyIdentifier,
            UnsignedInteger propertyArrayIndex, ByTime range) {
        this(objectIdentifier, propertyIdentifier, propertyArrayIndex, new Choice(7, range, choiceOptions));
    }

    private ReadRangeRequest(ObjectIdentifier objectIdentifier, PropertyIdentifier propertyIdentifier,
            UnsignedInteger propertyArrayIndex, Choice range) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
        this.range = range;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, propertyIdentifier, 1);
        writeOptional(queue, propertyArrayIndex, 2);
        writeOptional(queue, range);
    }

    public ReadRangeRequest(ByteQueue queue) throws BACnetException {
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
    public AcknowledgementService handle(LocalDevice localDevice, Address from) throws BACnetException {
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
            BACnetObject obj = localDevice.getObjectRequired(objectIdentifier);

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
        } catch (BACnetServiceException e) {
            throw new BACnetErrorException(getChoiceId(), e);
        }
    }

    private ReadRangeAck readRange(RangeReadable<?> list) throws BACnetServiceException {
        if (list.size() == 0) {
            return null;
        }

        AtomicReference<ReadRangeAck> result = new AtomicReference<>();
        list.inSynchronizedBlock(() -> {
            // Handle the choice. The branches are mutually exclusive; a null result from a branch
            // is legitimate (e.g. an out-of-range reference index) and is passed through to the
            // caller as an empty ack.
            if (range == null) {
                result.set(readRangeDefault(list));
            } else if (isByPosition()) {
                result.set(readRangeByPosition(list, getByPosition()));
            } else if (isBySequenceNumber()) {
                result.set(readRangeBySequenceNumber(list, getBySequenceNumber()));
            } else if (isByTime()) {
                result.set(readRangeByTime(list, getByTime()));
            } else {
                // Should never happen — the Choice is validated at decode time.
                throw new BACnetRuntimeException("No handling for choice: " + range);
            }
        });

        return result.get();
    }

    private ReadRangeAck readRangeDefault(RangeReadable<?> list) {
        SequenceOf<Encodable> data;
        ResultFlags resultFlags;

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

        // Return the result.
        return new ReadRangeAck(objectIdentifier, propertyIdentifier, propertyArrayIndex, resultFlags,
                new UnsignedInteger(data.size()), data, null);
    }

    private ReadRangeAck readRangeByPosition(RangeReadable<?> list, ByPosition position) {
        // Check if the reference index is in the range of the list.
        int pos = position.getReferenceIndex().intValue();
        int size = list.size();
        if (pos < 1 || pos > size)
            return null;

        return readAroundIndex(list, pos - 1, position);
    }

    private ReadRangeAck readRangeBySequenceNumber(RangeReadable<?> list, BySequenceNumber sequenceNumber)
            throws BACnetServiceException {
        // Check again that the list isn't empty. Shouldn't really happen. Just here because the list was only
        // synchronized just now, and there is a very small possibility that it may have since changed.
        if (list.size() == 0)
            return null;

        // Ensure that this list contains Sequenced elements. Per addendum 135-2016bu-1, a
        // 'By Sequence Number' request against a list whose items are not sequence-numbered
        // shall be reported with LIST_ITEM_NOT_NUMBERED.
        if (!(list.get(0) instanceof Sequenced))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.listItemNotNumbered);

        // Use a binary search to find the index of the record we need.
        @SuppressWarnings("unchecked") int pos = binarySearch((RangeReadable<Sequenced>) list,
                (Sequenced) () -> sequenceNumber.getReferenceIndex().longValue());

        // Check if the reference index is in the range of the list.
        if (pos < 0)
            return null;

        return readAroundIndex(list, pos, sequenceNumber);
    }

    private ReadRangeAck readRangeByTime(RangeReadable<?> list, ByTime time) throws BACnetServiceException {
        // Make sure the given timestamp is fully specified.
        if (!time.getReferenceTime().isFullySpecified())
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.datatypeNotSupported);

        int size = list.size();

        // Check again that the list isn't empty. Shouldn't really happen. Just here because the list was only
        // synchronized just now, and there is a very small possibility that it may have since changed.
        if (size == 0)
            return null;

        // Ensure that this list contains Sequenced and Timestamped elements. Per addendum
        // 135-2016bu-1, a 'By Time' request against a list whose items are not timestamped
        // shall be reported with LIST_ITEM_NOT_TIMESTAMPED. The Sequenced check is retained
        // because the response's First Sequence Number field is populated from the item's
        // sequence number; if the items aren't sequence-numbered we also can't honour the
        // 'By Time' semantics.
        if (!(list.get(0) instanceof Sequenced))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.listItemNotTimestamped);
        if (!(list.get(0) instanceof Timestamped))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.listItemNotTimestamped);

        // Use a binary search to find the index of the record we need.
        @SuppressWarnings("unchecked")
        int pos = binarySearch((RangeReadable<Timestamped>) list, (Timestamped) time::getReferenceTime);

        // Check if the reference index is in the range of the list.
        int count = time.getCount().intValue();
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

    /**
     * The list should already be synchronized by now.
     *
     * @param list  the list
     * @param index the 0-based index
     * @param range the range
     * @return the ack
     */
    private ReadRangeAck readAroundIndex(RangeReadable<?> list, int index, Range range) {
        int size = list.size();

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

        ResultFlags resultFlags = new ResultFlags(startIndex == 0, endIndex == size - 1, moreItems);

        SequenceOf<Encodable> data = new SequenceOf<>(endIndex - startIndex + 1);
        for (int i = startIndex; i <= endIndex; i++) {
            data.add((Encodable) list.get(i));
        }

        UnsignedInteger firstSequenceNumber = null;
        if (data.getBase1(1) instanceof Sequenced sequenced) {
            firstSequenceNumber = new UnsignedInteger(sequenced.getSequenceNumber());
        }

        // Return the result.
        return new ReadRangeAck(objectIdentifier, propertyIdentifier, propertyArrayIndex, resultFlags,
                new UnsignedInteger(data.size()), data, firstSequenceNumber);
    }

    private static <T> int binarySearch(RangeReadable<? extends RangeComparable> list, T key) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            int mid = low + high >>> 1;
            RangeComparable midVal = list.get(mid);
            int cmp;
            if (key instanceof Timestamped timestamped) {
                cmp = ((Timestamped) midVal).compareTimestamp(timestamped);
            } else if (key instanceof Sequenced sequenced) {
                cmp = ((Sequenced) midVal).compareSequenceNumber(sequenced);
            } else {
                // Should not happen.
                throw new BACnetRuntimeException("Key is not a valid class: " + key.getClass());
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
    public interface RangeReadable<E> {
        int size();

        E get(int index);

        void inSynchronizedBlock(BACnetServiceRunnable task) throws BACnetServiceException;
    }


    @FunctionalInterface
    public interface BACnetServiceRunnable {
        void run() throws BACnetServiceException;
    }


    /**
     * Allows Timestamped and Sequenced to be compared generically.
     */
    interface RangeComparable {
        // no op
    }


    /**
     * Implemented by elements of RangeReadable that have timestamps. Currently implemented by ILogRecord.
     */
    public interface Timestamped extends RangeComparable {
        DateTime getTimestamp();

        default int compareTimestamp(Timestamped that) {
            return getTimestamp().compareTo(that.getTimestamp());
        }
    }


    /**
     * Implemented by elements of RangeReadable that have sequence numbers. Currently implemented by ILogRecord.
     */
    public interface Sequenced extends RangeComparable {
        long getSequenceNumber();

        default int compareSequenceNumber(Sequenced that) {
            return Long.compare(getSequenceNumber(), that.getSequenceNumber());
        }

        default void setSequenceNumber(long num) {
            // no op
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ReadRangeRequest that))
            return false;
        return Objects.equals(objectIdentifier, that.objectIdentifier)
                && Objects.equals(propertyIdentifier, that.propertyIdentifier)
                && Objects.equals(propertyArrayIndex, that.propertyArrayIndex)
                && Objects.equals(range, that.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectIdentifier, propertyIdentifier, propertyArrayIndex, range);
    }

    public abstract static class Range extends BaseType {
        protected SignedInteger16 count;

        protected Range(SignedInteger16 count) {
            this.count = count;
        }

        Range() {
            // no op
        }

        public SignedInteger16 getCount() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass())
                return false;
            Range range = (Range) o;
            return Objects.equals(count, range.count);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(count);
        }
    }


    public static class ByPosition extends Range {
        private final UnsignedInteger referenceIndex;

        public ByPosition(int referenceIndex, int count) {
            this(new UnsignedInteger(referenceIndex), new SignedInteger16(count));
        }

        public ByPosition(UnsignedInteger referenceIndex, SignedInteger16 count) {
            super(count);
            this.referenceIndex = referenceIndex;
        }

        @Override
        public void write(ByteQueue queue) {
            write(queue, referenceIndex);
            write(queue, count);
        }

        public ByPosition(ByteQueue queue) throws BACnetException {
            referenceIndex = read(queue, UnsignedInteger.class);
            count = read(queue, SignedInteger16.class);
        }

        public UnsignedInteger getReferenceIndex() {
            return referenceIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ByPosition that))
                return false;
            if (!super.equals(o))
                return false;
            return Objects.equals(referenceIndex, that.referenceIndex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), referenceIndex);
        }
    }


    public static class BySequenceNumber extends Range {
        private final UnsignedInteger referenceIndex;

        public BySequenceNumber(long referenceIndex, int count) {
            this(new UnsignedInteger(referenceIndex), new SignedInteger16(count));
        }

        public BySequenceNumber(UnsignedInteger referenceIndex, SignedInteger16 count) {
            super(count);
            this.referenceIndex = referenceIndex;
        }

        @Override
        public void write(ByteQueue queue) {
            write(queue, referenceIndex);
            write(queue, count);
        }

        public BySequenceNumber(ByteQueue queue) throws BACnetException {
            referenceIndex = read(queue, UnsignedInteger.class);
            count = read(queue, SignedInteger16.class);
        }

        public UnsignedInteger getReferenceIndex() {
            return referenceIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BySequenceNumber that))
                return false;
            if (!super.equals(o))
                return false;
            return Objects.equals(referenceIndex, that.referenceIndex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), referenceIndex);
        }
    }


    public static class ByTime extends Range {
        private final DateTime referenceTime;

        public ByTime(DateTime referenceTime, int count) {
            this(referenceTime, new SignedInteger16(count));
        }

        public ByTime(DateTime referenceTime, SignedInteger16 count) {
            super(count);
            this.referenceTime = referenceTime;
        }

        @Override
        public void write(ByteQueue queue) {
            write(queue, referenceTime);
            write(queue, count);
        }

        public ByTime(ByteQueue queue) throws BACnetException {
            referenceTime = read(queue, DateTime.class);
            count = read(queue, SignedInteger16.class);
        }

        public DateTime getReferenceTime() {
            return referenceTime;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ByTime byTime))
                return false;
            if (!super.equals(o))
                return false;
            return Objects.equals(referenceTime, byTime.referenceTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), referenceTime);
        }
    }
}
