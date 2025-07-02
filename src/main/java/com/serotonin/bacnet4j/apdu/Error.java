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
import com.serotonin.bacnet4j.type.error.BACnetError;
import com.serotonin.bacnet4j.type.error.BaseError;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * The BACnet-Error-PDU is used to convey the information contained in a service response primitive ('Result(-)') that
 * indicates the reason why a previous confirmed service request failed in its entirety.
 *
 * @author mlohbihler
 */
public class Error extends AckAPDU {
    public static final byte TYPE_ID = 5;

    private final int errorChoice;
    /**
     * This parameter, of type BACnet-Error, indicates the reason the indicated service request could not be carried
     * out. This parameter shall be encoded according to the rules of 20.2.
     */
    private final BaseError error;

    public Error(final byte originalInvokeId, final int errorChoice, final BaseError error) {
        this.originalInvokeId = originalInvokeId;
        this.errorChoice = errorChoice;
        this.error = error;
    }

    public Error(final byte originalInvokeId, final BACnetError bacnetError) {
        this(originalInvokeId, bacnetError.getChoice(), bacnetError.getError());
    }

    @Override
    public byte getPduType() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        queue.push(getShiftedTypeId(TYPE_ID));
        queue.push(originalInvokeId);
        queue.push(errorChoice);
        error.write(queue);
    }

    Error(final ByteQueue queue) throws BACnetException {
        queue.pop(); // Ignore the first byte. No news there.
        originalInvokeId = queue.pop();
        errorChoice = queue.popU1B();
        error = BaseError.createBaseError(errorChoice, queue);
    }

    @Override
    public String toString() {
        return "ErrorAPDU(" + error + "," + error.getErrorClassAndCode() + ")";
    }

    public BaseError getError() {
        return error;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (error == null ? 0 : error.hashCode());
        result = PRIME * result + originalInvokeId;
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
        final Error other = (Error) obj;
        if (error == null) {
            if (other.error != null)
                return false;
        } else if (!error.equals(other.error))
            return false;
        if (originalInvokeId != other.originalInvokeId)
            return false;
        return true;
    }

    @Override
    public boolean expectsReply() {
        return false;
    }
}
