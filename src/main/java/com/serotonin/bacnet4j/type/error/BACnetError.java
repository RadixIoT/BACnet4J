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

package com.serotonin.bacnet4j.type.error;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class BACnetError extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(127, ErrorClassAndCode.class);
        choiceOptions.addContextual(0, ErrorClassAndCode.class);
        choiceOptions.addContextual(1, ErrorClassAndCode.class);
        choiceOptions.addContextual(31, ErrorClassAndCode.class);
        choiceOptions.addContextual(2, ErrorClassAndCode.class);
        choiceOptions.addContextual(3, ErrorClassAndCode.class);
        choiceOptions.addContextual(4, ErrorClassAndCode.class);
        choiceOptions.addContextual(29, ErrorClassAndCode.class);
        choiceOptions.addContextual(27, ErrorClassAndCode.class);
        choiceOptions.addContextual(5, ErrorClassAndCode.class);
        choiceOptions.addContextual(28, ErrorClassAndCode.class);
        choiceOptions.addContextual(30, SubscribeCovPropertyMultipleError.class);
        choiceOptions.addContextual(6, ErrorClassAndCode.class);
        choiceOptions.addContextual(7, ErrorClassAndCode.class);
        choiceOptions.addContextual(8, ChangeListError.class);
        choiceOptions.addContextual(9, ChangeListError.class);
        choiceOptions.addContextual(10, CreateObjectError.class);
        choiceOptions.addContextual(11, ErrorClassAndCode.class);
        choiceOptions.addContextual(12, ErrorClassAndCode.class);
        choiceOptions.addContextual(14, ErrorClassAndCode.class);
        choiceOptions.addContextual(26, ErrorClassAndCode.class);
        choiceOptions.addContextual(15, ErrorClassAndCode.class);
        choiceOptions.addContextual(16, WritePropertyMultipleError.class);
        choiceOptions.addContextual(17, ErrorClassAndCode.class);
        choiceOptions.addContextual(18, ConfirmedPrivateTransferError.class);
        choiceOptions.addContextual(19, ErrorClassAndCode.class);
        choiceOptions.addContextual(20, ErrorClassAndCode.class);
        choiceOptions.addContextual(21, ErrorClassAndCode.class);
        choiceOptions.addContextual(22, VTCloseError.class);
        choiceOptions.addContextual(23, ErrorClassAndCode.class);
    }

    private final Choice choice;

    public BACnetError(final int contextId, final BaseError error) {
        choice = new Choice(contextId, error, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public BACnetError(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    public int getChoice() {
        return choice.getContextId();
    }

    public BaseError getError() {
        return choice.getDatum();
    }
}
