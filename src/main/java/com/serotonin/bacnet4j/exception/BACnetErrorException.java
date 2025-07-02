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

package com.serotonin.bacnet4j.exception;

import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.error.BACnetError;
import com.serotonin.bacnet4j.type.error.BaseError;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;

public class BACnetErrorException extends BACnetException {
    private static final long serialVersionUID = -1;

    private final BACnetError bacnetError;

    public BACnetErrorException(final byte choice, final BaseError baseError) {
        super(getBaseMessage(baseError.getErrorClassAndCode().getErrorClass(),
                baseError.getErrorClassAndCode().getErrorCode(), null));
        bacnetError = new BACnetError(choice, baseError);
    }

    public BACnetErrorException(final byte choice, final BACnetErrorException cause) {
        super(cause.getMessage(), cause);
        bacnetError = new BACnetError(choice, cause.getBacnetError().getError());
    }

    public BACnetErrorException(final byte choice, final ErrorClass errorClass, final ErrorCode errorCode) {
        super(getBaseMessage(errorClass, errorCode, null));
        bacnetError = new BACnetError(choice, new ErrorClassAndCode(errorClass, errorCode));
    }

    public BACnetErrorException(final byte choice, final BACnetServiceException e) {
        super(e);
        bacnetError = new BACnetError(choice, new ErrorClassAndCode(e.getErrorClass(), e.getErrorCode()));
    }

    public BACnetErrorException(final ErrorClass errorClass, final ErrorCode errorCode) {
        super(getBaseMessage(errorClass, errorCode, null));
        bacnetError = new BACnetError(127, new ErrorClassAndCode(errorClass, errorCode));
    }

    public BACnetErrorException(final BACnetServiceException e) {
        super(e.getMessage());
        bacnetError = new BACnetError(127, new ErrorClassAndCode(e.getErrorClass(), e.getErrorCode()));
    }

    public BACnetErrorException(final ErrorClass errorClass, final ErrorCode errorCode, final String message) {
        super(getBaseMessage(errorClass, errorCode, message));
        bacnetError = new BACnetError(127, new ErrorClassAndCode(errorClass, errorCode));
    }

    public BACnetErrorException(final BACnetError bacnetError) {
        this.bacnetError = bacnetError;
    }

    public BACnetError getBacnetError() {
        return bacnetError;
    }

    private static String getBaseMessage(final ErrorClass errorClass, final ErrorCode errorCode, final String message) {
        final StringBuilder sb = new StringBuilder();
        sb.append(errorClass.toString());
        sb.append(": ");
        sb.append(errorCode.toString());
        if (message != null)
            sb.append(" '").append(message).append("'");
        return sb.toString();
    }
}
