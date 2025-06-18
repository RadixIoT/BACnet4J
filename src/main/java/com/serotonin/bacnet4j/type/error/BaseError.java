/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2015 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.type.error;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

abstract public class BaseError extends BaseType {
    public static BaseError createBaseError(final int choice, final ByteQueue queue) throws BACnetException {
        try {
            queue.mark();
            switch (choice) {
                case 8:
                case 9:
                    return new ChangeListError(queue);
                case 10:
                    return new CreateObjectError(queue);
                case 16:
                    return new WritePropertyMultipleError(queue);
                case 18:
                    return new ConfirmedPrivateTransferError(queue);
                case 22:
                    return new VTCloseError(queue);
                case 30:
                    return new SubscribeCovPropertyMultipleError(queue);
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 11:
                case 12:
                case 14:
                case 15:
                case 17:
                case 19:
                case 20:
                case 21:
                case 23:
                case 26:
                case 27:
                case 28:
                case 29:
                case 31:
                case 127:
                    return new ErrorClassAndCode(queue);
                default:
                    throw new BACnetException("Could not map error choice to class: " + choice);
            }
        } catch (final BACnetErrorException e) {
            // Some devices do not send a properly formatted error. In case of error, try just parsing as a BaseError.
            if (e.getBacnetError().getError().getErrorClassAndCode().getErrorClass().isOneOf(ErrorClass.property) && e
                    .getBacnetError().getError().getErrorClassAndCode().getErrorCode().isOneOf(
                            ErrorCode.missingRequiredParameter)) {
                queue.reset();
                return new ErrorClassAndCode(queue);
            }
            throw e;
        }
    }

    abstract public ErrorClassAndCode getErrorClassAndCode();
}
