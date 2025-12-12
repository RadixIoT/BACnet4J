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

import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.enums.MaxSegments;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.NPCI.NetworkPriority;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ConfirmedRequest extends APDU implements Segmentable {
    public static final byte TYPE_ID = 0;

    public static int getHeaderSize(final boolean segmented) {
        if (segmented)
            return 6;
        return 4;
    }

    /**
     * This parameter indicates whether or not the confirmed service request is entirely, or only partially, contained
     * in the present PDU. If the request is present in its entirety, the value of the 'segmented-message' parameter
     * shall be FALSE. If the present PDU contains only a segment of the request, this parameter shall be TRUE.
     */
    private boolean segmentedMessage;

    /**
     * This parameter is only meaningful if the 'segmented-message' parameter is TRUE. If 'segmented-message' is TRUE,
     * then the 'more-follows' parameter shall be TRUE for all segments comprising the confirmed service request except
     * for the last and shall be FALSE for the final segment. If 'segmented-message' is FALSE, then 'more-follows' shall
     * be set FALSE by the encoder and shall be ignored by the decoder.
     */
    private boolean moreFollows;

    /**
     * This parameter shall be TRUE if the device issuing the confirmed request will accept a segmented complex
     * acknowledgment as a response. It shall be FALSE otherwise. This parameter is included in the confirmed request so
     * that the responding device may determine how to convey its response.
     */
    private boolean segmentedResponseAccepted;

    /**
     * This optional parameter specifies the maximum number of segments that the device will accept. This parameter is
     * included in the confirmed request so that the responding device may determine how to convey its response. The
     * parameter shall be encoded as follows: B'000' Unspecified number of segments accepted. B'001' 2 segments
     * accepted. B'010' 4 segments accepted. B'011' 8 segments accepted. B'100' 16 segments accepted. B'101' 32 segments
     * accepted. B'110' 64 segments accepted. B'111' Greater than 64 segments accepted.
     */
    private MaxSegments maxSegmentsAccepted;

    /**
     * This parameter specifies the maximum size of a single APDU that the issuing device will accept. This parameter is
     * included in the confirmed request so that the responding device may determine how to convey its response. The
     * parameter shall be encoded as follows: B'0000' Up to MinimumMessageSize (50 octets) B'0001' Up to 128 octets
     * B'0010' Up to 206 octets (fits in a LonTalk frame) B'0011' Up to 480 octets (fits in an ARCNET frame) B'0100' Up
     * to 1024 octets B'0101' Up to 1476 octets (fits in an ISO 8802-3 frame) B'0110' reserved by ASHRAE B'0111'
     * reserved by ASHRAE B'1000' reserved by ASHRAE B'1001' reserved by ASHRAE B'1010' reserved by ASHRAE B'1011'
     * reserved by ASHRAE B'1100' reserved by ASHRAE B'1101' reserved by ASHRAE B'1110' reserved by ASHRAE B'1111'
     * reserved by ASHRAE
     */
    private MaxApduLength maxApduLengthAccepted;

    /**
     * This parameter shall be an integer in the range 0 - 255 assigned by the service requester. It shall be used to
     * associate the response to a confirmed service request with the original request. In the absence of any error, the
     * 'invokeID' shall be returned by the service provider in a BACnet-SimpleACK-PDU or a BACnet-ComplexACK-PDU. In the
     * event of an error condition, the 'invokeID' shall be returned by the service provider in a BACnet-Error-PDU,
     * BACnet-Reject-PDU, or BACnet-Abort-PDU as appropriate.
     *
     * The 'invokeID' shall be generated by the device issuing the service request. It shall be unique for all
     * outstanding confirmed request APDUs generated by the device. The same 'invokeID' shall be used for all segments
     * of a segmented service request. Once an 'invokeID' has been assigned to an APDU, it shall be maintained within
     * the device until either a response APDU is received with the same 'invokeID' or a no response timer expires (see
     * 5.3). In either case, the 'invokeID' value shall then be released for reassignment. The algorithm used to pick a
     * value out of the set of unused values is a local matter. The storage mechanism for maintaining the used
     * 'invokeID' values within the requesting and responding devices is also a local matter. The requesting device may
     * use a single 'invokeID' space for all its confirmed APDUs or multiple 'invokeID' spaces (one per destination
     * device address) as desired. Since the 'invokeID' values are only source-device-unique, the responding device
     * shall maintain the 'invokeID' as well as the requesting device address until a response has been sent. The
     * responding device may discard the 'invokeID' information after a response has been sent.
     */
    private byte invokeId;

    /**
     * This optional parameter is only present if the 'segmented-message' parameter is TRUE. In this case, the
     * 'sequence-number' shall be a sequentially incremented unsigned integer, modulo 256, which identifies each segment
     * of a segmented request. The value of the received 'sequence-number' is used by the responder to acknowledge the
     * receipt of one or more segments of a segmented request. The 'sequence-number' of the first segment of a segmented
     * request shall be zero.
     */
    private int sequenceNumber;

    /**
     * This optional parameter is only present if the 'segmented-message' parameter is TRUE. In this case, the
     * 'proposed-windowsize' parameter shall specify as an unsigned binary integer the maximum number of message
     * segments containing 'invokeID' the sender is able or willing to send before waiting for a segment acknowledgment
     * PDU (see 5.2 and 5.3). The value of the 'proposed-window-size' shall be in the range 1 - 127.
     */
    private int proposedWindowSize;

    /**
     * This parameter shall contain the parameters of the specific service that is being requested, encoded according to
     * the rules of 20.2. These parameters are defined in the individual service descriptions in this standard and are
     * represented in Clause 21 in accordance with the rules of ASN.1.
     */
    private byte serviceChoice;
    private ConfirmedRequestService serviceRequest;

    /**
     * This field is used to allow parsing of only the APDU so that those fields are available in case there is a
     * problem parsing the service request.
     */
    private ByteQueue serviceData;

    private final NetworkPriority networkPriority;

    public ConfirmedRequest(final boolean segmentedMessage, final boolean moreFollows,
            final boolean segmentedResponseAccepted, final MaxSegments maxSegmentsAccepted,
            final MaxApduLength maxApduLengthAccepted, final byte invokeId, final int sequenceNumber,
            final int proposedWindowSize, final ConfirmedRequestService serviceRequest) {

        setFields(segmentedMessage, moreFollows, segmentedResponseAccepted, maxSegmentsAccepted, maxApduLengthAccepted,
                invokeId, sequenceNumber, proposedWindowSize, serviceRequest.getChoiceId());

        this.serviceRequest = serviceRequest;
        networkPriority = serviceRequest.getNetworkPriority();
    }

    public ConfirmedRequest(final boolean segmentedMessage, final boolean moreFollows,
            final boolean segmentedResponseAccepted, final MaxSegments maxSegmentsAccepted,
            final MaxApduLength maxApduLengthAccepted, final byte invokeId, final int sequenceNumber,
            final int proposedWindowSize, final byte serviceChoice, final ByteQueue serviceData,
            final NetworkPriority networkPriority) {

        setFields(segmentedMessage, moreFollows, segmentedResponseAccepted, maxSegmentsAccepted, maxApduLengthAccepted,
                invokeId, sequenceNumber, proposedWindowSize, serviceChoice);

        this.serviceData = serviceData;
        this.networkPriority = networkPriority;
    }

    private void setFields(final boolean segmentedMessage, final boolean moreFollows,
            final boolean segmentedResponseAccepted, final MaxSegments maxSegmentsAccepted,
            final MaxApduLength maxApduLengthAccepted, final byte invokeId, final int sequenceNumber,
            final int proposedWindowSize, final byte serviceChoice) {
        this.segmentedMessage = segmentedMessage;
        this.moreFollows = moreFollows;
        this.segmentedResponseAccepted = segmentedResponseAccepted;
        this.maxSegmentsAccepted = maxSegmentsAccepted;
        this.maxApduLengthAccepted = maxApduLengthAccepted;
        this.invokeId = invokeId;
        this.sequenceNumber = sequenceNumber;
        this.proposedWindowSize = proposedWindowSize;
        this.serviceChoice = serviceChoice;
    }

    @Override
    public byte getPduType() {
        return TYPE_ID;
    }

    @Override
    public byte getInvokeId() {
        return invokeId;
    }

    @Override
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public MaxApduLength getMaxApduLengthAccepted() {
        return maxApduLengthAccepted;
    }

    public MaxSegments getMaxSegmentsAccepted() {
        return maxSegmentsAccepted;
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

    public boolean isSegmentedResponseAccepted() {
        return segmentedResponseAccepted;
    }

    public ConfirmedRequestService getServiceRequest() {
        return serviceRequest;
    }

    @Override
    public void appendServiceData(final ByteQueue data) {
        this.serviceData.push(data);
    }

    @Override
    public ByteQueue getServiceData() {
        return serviceData;
    }

    public byte getServiceChoice() {
        return serviceChoice;
    }

    @Override
    public NetworkPriority getNetworkPriority() {
        return networkPriority;
    }

    @Override
    public void write(final ByteQueue queue) {
        queue.push(getShiftedTypeId(TYPE_ID) | (segmentedMessage ? 8 : 0) | (moreFollows ? 4 : 0)
                | (segmentedResponseAccepted ? 2 : 0));
        queue.push((maxSegmentsAccepted.getId() & 7) << 4 | maxApduLengthAccepted.getId() & 0xf);
        queue.push(invokeId);
        if (segmentedMessage) {
            queue.push(sequenceNumber);
            queue.push(proposedWindowSize);
        }
        queue.push(serviceChoice);
        if (serviceRequest != null)
            serviceRequest.write(queue);
        else
            queue.push(serviceData);
    }

    ConfirmedRequest(final ByteQueue queue) {
        byte b = queue.pop();
        segmentedMessage = (b & 8) != 0;
        moreFollows = (b & 4) != 0;
        segmentedResponseAccepted = (b & 2) != 0;

        b = queue.pop();
        maxSegmentsAccepted = MaxSegments.valueOf((byte) ((b & 0x70) >> 4));
        maxApduLengthAccepted = MaxApduLength.valueOf((byte) (b & 0xf));
        invokeId = queue.pop();
        if (segmentedMessage) {
            sequenceNumber = queue.popU1B();
            proposedWindowSize = queue.popU1B();
        }
        serviceChoice = queue.pop();
        serviceData = new ByteQueue(queue.popAll());
        // This is called due to an incoming request, so setting to null here should be ok.
        networkPriority = null;
    }

    @Override
    public void parseServiceData() throws BACnetException {
        if (serviceData != null) {
            serviceRequest = ConfirmedRequestService.createConfirmedRequestService(serviceChoice, serviceData);
            serviceData = null;
        }
    }

    @Override
    public APDU clone(final boolean moreFollows, final int sequenceNumber, final int actualSegWindow,
            final ByteQueue serviceData) {
        return new ConfirmedRequest(this.segmentedMessage, moreFollows, this.segmentedResponseAccepted,
                this.maxSegmentsAccepted, this.maxApduLengthAccepted, this.invokeId, sequenceNumber, actualSegWindow,
                this.serviceChoice, serviceData, networkPriority);
    }

    @Override
    public String toString() {
        return "ConfirmedRequest(segmentedMessage=" + segmentedMessage + ", moreFollows=" + moreFollows
                + ", segmentedResponseAccepted=" + segmentedResponseAccepted + ", maxSegmentsAccepted="
                + maxSegmentsAccepted + ", maxApduLengthAccepted=" + maxApduLengthAccepted + ", invokeId=" + invokeId
                + ", sequenceNumber=" + sequenceNumber + ", proposedWindowSize=" + proposedWindowSize
                + ", serviceChoice=" + serviceChoice + ", serviceRequest=" + serviceRequest + ")";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + invokeId;
        result = PRIME * result + (maxApduLengthAccepted == null ? 0 : maxApduLengthAccepted.hashCode());
        result = PRIME * result + (maxSegmentsAccepted == null ? 0 : maxSegmentsAccepted.hashCode());
        result = PRIME * result + (moreFollows ? 1231 : 1237);
        result = PRIME * result + proposedWindowSize;
        result = PRIME * result + (segmentedMessage ? 1231 : 1237);
        result = PRIME * result + (segmentedResponseAccepted ? 1231 : 1237);
        result = PRIME * result + sequenceNumber;
        result = PRIME * result + serviceChoice;
        result = PRIME * result + (serviceData == null ? 0 : serviceData.hashCode());
        result = PRIME * result + (serviceRequest == null ? 0 : serviceRequest.hashCode());
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
        final ConfirmedRequest other = (ConfirmedRequest) obj;
        if (invokeId != other.invokeId)
            return false;
        if (maxApduLengthAccepted == null) {
            if (other.maxApduLengthAccepted != null)
                return false;
        } else if (!maxApduLengthAccepted.equals(other.maxApduLengthAccepted))
            return false;
        if (maxSegmentsAccepted != other.maxSegmentsAccepted)
            return false;
        if (moreFollows != other.moreFollows)
            return false;
        if (proposedWindowSize != other.proposedWindowSize)
            return false;
        if (segmentedMessage != other.segmentedMessage)
            return false;
        if (segmentedResponseAccepted != other.segmentedResponseAccepted)
            return false;
        if (sequenceNumber != other.sequenceNumber)
            return false;
        if (serviceChoice != other.serviceChoice)
            return false;
        if (serviceData == null) {
            if (other.serviceData != null)
                return false;
        } else if (!serviceData.equals(other.serviceData))
            return false;
        if (serviceRequest == null) {
            if (other.serviceRequest != null)
                return false;
        } else if (!serviceRequest.equals(other.serviceRequest))
            return false;
        return true;
    }

    @Override
    public boolean expectsReply() {
        return true;
    }
}
