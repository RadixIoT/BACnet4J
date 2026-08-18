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

package com.serotonin.bacnet4j.transport;

import java.time.Clock;

import com.serotonin.bacnet4j.ResponseConsumer;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.Segmentable;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * The state of a single transaction, i.e. an instance of one of the Transaction State Machines of clause 5.4. The
 * variable names of clause 5.4.1 are used where applicable.
 */
public class UnackedMessageContext {
    private long deadline;
    private int attemptsLeft;

    private final Clock clock;

    // Temporarily add to the context for troubleshooting.
    private final ConfirmedRequestService service;

    // The response consumer, for confirmed requests
    private final ResponseConsumer consumer;

    // The original APDU for resending in case of timeout.
    private APDU originalApdu;

    // The state of this transaction's state machine.
    private TsmState state;

    //
    // Clause 5.4.1 segmentation variables. Sequence numbers are unsigned eight bit values; all arithmetic on them is
    // modulo 256. See SegmentSequence.
    //
    private int lastSequenceNumber;
    private int initialSequenceNumber;
    private int actualWindowSize;
    private int proposedWindowSize;
    private int duplicateCount;
    private int segmentRetryCount;
    private boolean sentAllSegments;

    //
    // Receiving a segmented message.
    //
    private Segmentable segmentedMessage;
    private int segmentsReceived;

    //
    // Sending a segmented message. The serialized service data is retained in full rather than consumed, because
    // FillWindow must be able to retransmit the segments of the current window.
    //
    private Segmentable segmentTemplate;
    private byte[] segmentData;
    private int segmentSize;
    private int segmentCount;
    private int windowStartIndex;

    public UnackedMessageContext(Clock clock, int timeout, int retries, ResponseConsumer consumer,
            ConfirmedRequestService service) {
        this.clock = clock;
        reset(timeout, retries);
        this.consumer = consumer;
        this.service = service;
    }

    public void retry(int timeout) {
        this.deadline = clock.millis() + timeout;
        attemptsLeft--;
    }

    public void reset(int timeout, int retries) {
        this.deadline = clock.millis() + timeout;
        this.attemptsLeft = retries;
    }

    public void resetTimer(int timeout) {
        this.deadline = clock.millis() + timeout;
    }

    public long getDeadline() {
        return deadline;
    }

    public boolean hasMoreAttempts() {
        return attemptsLeft > 0;
    }

    public ResponseConsumer getConsumer() {
        return consumer;
    }

    public ConfirmedRequestService getService() {
        return service;
    }

    public APDU getOriginalApdu() {
        return originalApdu;
    }

    public void setOriginalApdu(APDU originalApdu) {
        this.originalApdu = originalApdu;
    }

    public TsmState getState() {
        return state;
    }

    public void setState(TsmState state) {
        this.state = state;
    }

    public boolean isExpired(long now) {
        return deadline < now;
    }

    //
    //
    // Clause 5.4.1 variables
    //
    public int getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    public void setLastSequenceNumber(int lastSequenceNumber) {
        this.lastSequenceNumber = lastSequenceNumber;
    }

    public int getInitialSequenceNumber() {
        return initialSequenceNumber;
    }

    public void setInitialSequenceNumber(int initialSequenceNumber) {
        this.initialSequenceNumber = initialSequenceNumber;
    }

    public int getActualWindowSize() {
        return actualWindowSize;
    }

    public void setActualWindowSize(int actualWindowSize) {
        this.actualWindowSize = actualWindowSize;
    }

    /**
     * The window size this device proposes on each segment it sends. Clause 5.4.3 FillWindow puts this value, rather
     * than the negotiated ActualWindowSize, into every transmitted segment.
     */
    public int getProposedWindowSize() {
        return proposedWindowSize;
    }

    public void setProposedWindowSize(int proposedWindowSize) {
        this.proposedWindowSize = proposedWindowSize;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public void incrementDuplicateCount() {
        duplicateCount++;
    }

    /**
     * Ndup, the number of duplicates that will be silently dropped per window before a negative segment
     * acknowledgement is returned. Clause 5.4.1 defines this as being equal to ActualWindowSize.
     */
    public int getNdup() {
        return getActualWindowSize();
    }

    public int getSegmentRetryCount() {
        return segmentRetryCount;
    }

    public void setSegmentRetryCount(int segmentRetryCount) {
        this.segmentRetryCount = segmentRetryCount;
    }

    public void incrementSegmentRetryCount() {
        segmentRetryCount++;
    }

    public boolean isSentAllSegments() {
        return sentAllSegments;
    }

    public void setSentAllSegments(boolean sentAllSegments) {
        this.sentAllSegments = sentAllSegments;
    }

    //
    //
    // Receiving a segmented message
    //
    public Segmentable getSegmentedMessage() {
        return segmentedMessage;
    }

    public void setSegmentedMessage(Segmentable segmentedResponse) {
        this.segmentedMessage = segmentedResponse;
        this.segmentsReceived = 1;
    }

    /**
     * Appends the given segment's service data onto the message being assembled.
     */
    public void appendSegment(Segmentable segment) {
        segmentedMessage.appendServiceData(segment.getServiceData());
        segmentsReceived++;
    }

    /**
     * The number of segments of the incoming message that have been saved, including the first.
     */
    public int getSegmentsReceived() {
        return segmentsReceived;
    }

    //
    //
    // Sending a segmented message
    //
    public Segmentable getSegmentTemplate() {
        return segmentTemplate;
    }

    public void setSegmentTemplate(Segmentable segmentTemplate) {
        this.segmentTemplate = segmentTemplate;
    }

    /**
     * Sets the data to be segmented, and the maximum size of each segment.
     */
    public void setSegmentData(ByteQueue serviceData, int segmentSize) {
        this.segmentData = serviceData.popAll();
        this.segmentSize = segmentSize;
        this.segmentCount = segmentCount(this.segmentData.length, segmentSize);
        this.windowStartIndex = 0;
    }

    /**
     * The number of segments required to send the given number of bytes.
     */
    public static int segmentCount(int dataLength, int segmentSize) {
        if (dataLength == 0)
            return 1;
        return (dataLength + segmentSize - 1) / segmentSize;
    }

    /**
     * The total number of segments in the message being sent.
     */
    public int getSegmentCount() {
        return segmentCount;
    }

    /**
     * The absolute index of the first segment of the current window.
     */
    public int getWindowStartIndex() {
        return windowStartIndex;
    }

    public void setWindowStartIndex(int windowStartIndex) {
        this.windowStartIndex = windowStartIndex;
    }

    /**
     * The data of the segment at the given absolute index.
     */
    public ByteQueue getSegment(int index) {
        int offset = index * segmentSize;
        int length = Math.min(segmentSize, segmentData.length - offset);
        return new ByteQueue(segmentData, offset, length);
    }

    /**
     * Whether the segment at the given absolute index is the last one of the message.
     */
    public boolean isFinalSegment(int index) {
        return index == segmentCount - 1;
    }

    public void useConsumer(ConsumerClient client) {
        if (consumer != null) {
            client.use(consumer);
        }
    }

    @Override
    public String toString() {
        return "UnackedMessageContext [deadline=" + deadline + ", attemptsLeft=" + attemptsLeft + ", clock=" + clock
                + ", service=" + service + ", consumer=" + consumer + ", originalApdu=" + originalApdu + ", state="
                + state + ", lastSequenceNumber=" + lastSequenceNumber + ", initialSequenceNumber="
                + initialSequenceNumber + ", actualWindowSize=" + actualWindowSize + ", proposedWindowSize="
                + proposedWindowSize + ", duplicateCount=" + duplicateCount + ", segmentRetryCount=" + segmentRetryCount
                + ", sentAllSegments=" + sentAllSegments + ", segmentedMessage=" + segmentedMessage
                + ", segmentsReceived=" + segmentsReceived + ", segmentTemplate=" + segmentTemplate + ", segmentCount="
                + segmentCount + ", windowStartIndex=" + windowStartIndex + "]";
    }

    @FunctionalInterface
    public interface ConsumerClient {
        void use(ResponseConsumer consumer);
    }
}
