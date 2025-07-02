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

package com.serotonin.bacnet4j.apdu;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ComplexACK extends AckAPDU implements Segmentable {
    public static final byte TYPE_ID = 3;

    public static int getHeaderSize(final boolean segmented) {
        if (segmented)
            return 5;
        return 3;
    }

    /**
     * This parameter indicates whether or not the confirmed service response is entirely, or only partially, contained
     * in the present PDU. If the response is present in its entirety, the 'segmented-message' parameter shall be FALSE.
     * If the present PDU contains only a segment of the response, this parameter shall be TRUE.
     */
    private boolean segmentedMessage;

    /**
     * This parameter is only meaningful if the 'segmented-message' parameter is TRUE. If 'segmented-message' is TRUE,
     * then the 'more-follows' parameter shall be TRUE for all segments comprising the confirmed service response except
     * for the last and shall be FALSE for the final segment. If 'segmented-message' is FALSE, then 'more-follows' shall
     * be set FALSE by the encoder and shall be ignored by the decoder.
     */
    private boolean moreFollows;

    /**
     * This optional parameter is only present if the 'segmented-message' parameter is TRUE. In this case, the
     * 'sequence-number' shall be a sequentially incremented unsigned integer, modulo 256, which identifies each segment
     * of a segmented response. The value of the received 'sequence-number' is used by the original requester to
     * acknowledge the receipt of one or more segments of a segmented response. The sequence-number of the first segment
     * of a segmented response shall be zero.
     */
    private int sequenceNumber;

    /**
     * This optional parameter is only present if the 'segmented-message' parameter is TRUE. In this case, the
     * 'proposed-window-size' parameter shall specify as an unsigned binary integer the maximum number of message
     * segments containing 'original-invokeID' the sender is able or willing to send before waiting for a segment
     * acknowledgment PDU (see 5.2 and 5.3). The value of the 'proposed-window-size' shall be in the range 1 - 127.
     */
    private int proposedWindowSize;

    /**
     * This parameter shall contain the value of the BACnetConfirmedServiceChoice corresponding to the service contained
     * in the previous BACnet-Confirmed-Service-Request that has resulted in this acknowledgment. See Clause 21.
     *
     * This parameter shall contain the parameters of the specific service acknowledgment that is being encoded
     * according to the rules of 20.2. These parameters are defined in the individual service descriptions in this
     * standard and are represented in Clause 21 in accordance with the rules of ASN.1.
     */
    private AcknowledgementService service;

    /**
     * This field is used to allow parsing of only the APDU so that those fields are available in case there is a
     * problem parsing the service request.
     */
    private ByteQueue serviceData;

    private byte serviceChoice;

    public ComplexACK(final boolean segmentedMessage, final boolean moreFollows, final byte originalInvokeId,
            final int sequenceNumber, final int proposedWindowSize, final AcknowledgementService service) {

        setFields(segmentedMessage, moreFollows, originalInvokeId, sequenceNumber, proposedWindowSize,
                service.getChoiceId());

        this.service = service;
    }

    public ComplexACK(final boolean segmentedMessage, final boolean moreFollows, final byte originalInvokeId,
            final int sequenceNumber, final int proposedWindowSize, final byte serviceChoice,
            final ByteQueue serviceData) {

        setFields(segmentedMessage, moreFollows, originalInvokeId, sequenceNumber, proposedWindowSize, serviceChoice);

        this.serviceData = serviceData;
    }

    private void setFields(final boolean segmentedMessage, final boolean moreFollows, final byte originalInvokeId,
            final int sequenceNumber, final int proposedWindowSize, final byte serviceChoice) {
        this.segmentedMessage = segmentedMessage;
        this.moreFollows = moreFollows;
        this.originalInvokeId = originalInvokeId;
        this.sequenceNumber = sequenceNumber;
        this.proposedWindowSize = proposedWindowSize;
        this.serviceChoice = serviceChoice;
    }

    @Override
    public byte getPduType() {
        return TYPE_ID;
    }

    @Override
    public boolean isMoreFollows() {
        return moreFollows;
    }

    @Override
    public int getProposedWindowSize() {
        return proposedWindowSize;
    }

    @Override
    public boolean isSegmentedMessage() {
        return segmentedMessage;
    }

    @Override
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @SuppressWarnings("unchecked")
    public <T extends AcknowledgementService> T getService() {
        return (T) service;
    }

    @Override
    public void appendServiceData(final ByteQueue data) {
        this.serviceData.push(data);
    }

    @Override
    public ByteQueue getServiceData() {
        return serviceData;
    }

    @Override
    public byte getInvokeId() {
        return originalInvokeId;
    }

    public byte getServiceChoice() {
        return serviceChoice;
    }

    @Override
    public String toString() {
        return "ComplexACK [segmentedMessage=" + segmentedMessage + ", moreFollows=" + moreFollows + ", sequenceNumber="
                + sequenceNumber + ", proposedWindowSize=" + proposedWindowSize + ", service=" + service
                + ", serviceData=" + serviceData + ", serviceChoice=" + serviceChoice + ", originalInvokeId="
                + originalInvokeId + "]";
    }

    @Override
    public void write(final ByteQueue queue) {
        queue.push(getShiftedTypeId(TYPE_ID) | (segmentedMessage ? 8 : 0) | (moreFollows ? 4 : 0));
        queue.push(originalInvokeId);
        if (segmentedMessage) {
            queue.push(sequenceNumber);
            queue.push(proposedWindowSize);
        }
        queue.push(serviceChoice);
        if (service != null)
            service.write(queue);
        else
            queue.push(serviceData);
    }

    ComplexACK(final ByteQueue queue) {
        final byte b = queue.pop();
        segmentedMessage = (b & 8) != 0;
        moreFollows = (b & 4) != 0;

        originalInvokeId = queue.pop();
        if (segmentedMessage) {
            sequenceNumber = queue.popU1B();
            proposedWindowSize = queue.popU1B();
        }
        serviceChoice = queue.pop();
        serviceData = new ByteQueue(queue.popAll());
    }

    @Override
    public void parseServiceData() throws BACnetException {
        if (serviceData != null) {
            service = AcknowledgementService.createAcknowledgementService(serviceChoice, serviceData);
            serviceData = null;
        }
    }

    @Override
    public APDU clone(final boolean moreFollows, final int sequenceNumber, final int actualSegWindow,
            final ByteQueue serviceData) {
        return new ComplexACK(this.segmentedMessage, moreFollows, this.originalInvokeId, sequenceNumber,
                actualSegWindow, this.serviceChoice, serviceData);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (moreFollows ? 1231 : 1237);
        result = PRIME * result + originalInvokeId;
        result = PRIME * result + proposedWindowSize;
        result = PRIME * result + (segmentedMessage ? 1231 : 1237);
        result = PRIME * result + sequenceNumber;
        result = PRIME * result + serviceChoice;
        result = PRIME * result + (service == null ? 0 : service.hashCode());
        result = PRIME * result + (serviceData == null ? 0 : serviceData.hashCode());
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
        final ComplexACK other = (ComplexACK) obj;
        if (moreFollows != other.moreFollows)
            return false;
        if (originalInvokeId != other.originalInvokeId)
            return false;
        if (proposedWindowSize != other.proposedWindowSize)
            return false;
        if (segmentedMessage != other.segmentedMessage)
            return false;
        if (sequenceNumber != other.sequenceNumber)
            return false;
        if (serviceChoice != other.serviceChoice)
            return false;
        if (service == null) {
            if (other.service != null)
                return false;
        } else if (!service.equals(other.service))
            return false;
        if (serviceData == null) {
            if (other.serviceData != null)
                return false;
        } else if (!serviceData.equals(other.serviceData))
            return false;
        return true;
    }

    @Override
    public boolean expectsReply() {
        return segmentedMessage;
    }
}
