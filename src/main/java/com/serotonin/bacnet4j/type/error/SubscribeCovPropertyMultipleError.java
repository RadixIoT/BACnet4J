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
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SubscribeCovPropertyMultipleError extends BaseError {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, ErrorClassAndCode.class);
        choiceOptions.addContextual(1, FirstFailedSubscription.class);
    }

    private final Choice choice;

    public SubscribeCovPropertyMultipleError(final ErrorClassAndCode errorType) {
        choice = new Choice(0, errorType, choiceOptions);
    }

    public SubscribeCovPropertyMultipleError(final FirstFailedSubscription firstFailedSubscription) {
        choice = new Choice(1, firstFailedSubscription, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public SubscribeCovPropertyMultipleError(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    public boolean isErrorType() {
        return choice.isa(ErrorClassAndCode.class);
    }

    public ErrorClassAndCode getErrorType() {
        return choice.getDatum();
    }

    public boolean isFirstFailedSubscription() {
        return choice.isa(FirstFailedSubscription.class);
    }

    public FirstFailedSubscription getFirstFailedSubscription() {
        return choice.getDatum();
    }

    @Override
    public ErrorClassAndCode getErrorClassAndCode() {
        if (isErrorType())
            return getErrorType();
        return getFirstFailedSubscription().getErrorType();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (choice == null ? 0 : choice.hashCode());
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
        final SubscribeCovPropertyMultipleError other = (SubscribeCovPropertyMultipleError) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    public static class FirstFailedSubscription extends BaseType {
        private final ObjectIdentifier monitoredObjectIdentifier;
        private final PropertyReference monitoredPropertyReference;
        private final ErrorClassAndCode errorType;

        public FirstFailedSubscription(final ObjectIdentifier monitoredObjectIdentifier,
                final PropertyReference monitoredPropertyReference, final ErrorClassAndCode errorType) {
            this.monitoredObjectIdentifier = monitoredObjectIdentifier;
            this.monitoredPropertyReference = monitoredPropertyReference;
            this.errorType = errorType;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, monitoredObjectIdentifier, 0);
            write(queue, monitoredPropertyReference, 1);
            write(queue, errorType, 2);
        }

        public FirstFailedSubscription(final ByteQueue queue) throws BACnetException {
            monitoredObjectIdentifier = read(queue, ObjectIdentifier.class, 0);
            monitoredPropertyReference = read(queue, PropertyReference.class, 1);
            errorType = read(queue, ErrorClassAndCode.class, 2);
        }

        public ObjectIdentifier getMonitoredObjectIdentifier() {
            return monitoredObjectIdentifier;
        }

        public PropertyReference getMonitoredPropertyReference() {
            return monitoredPropertyReference;
        }

        public ErrorClassAndCode getErrorType() {
            return errorType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (errorType == null ? 0 : errorType.hashCode());
            result = prime * result + (monitoredObjectIdentifier == null ? 0 : monitoredObjectIdentifier.hashCode());
            result = prime * result + (monitoredPropertyReference == null ? 0 : monitoredPropertyReference.hashCode());
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
            final FirstFailedSubscription other = (FirstFailedSubscription) obj;
            if (errorType == null) {
                if (other.errorType != null)
                    return false;
            } else if (!errorType.equals(other.errorType))
                return false;
            if (monitoredObjectIdentifier == null) {
                if (other.monitoredObjectIdentifier != null)
                    return false;
            } else if (!monitoredObjectIdentifier.equals(other.monitoredObjectIdentifier))
                return false;
            if (monitoredPropertyReference == null) {
                if (other.monitoredPropertyReference != null)
                    return false;
            } else if (!monitoredPropertyReference.equals(other.monitoredPropertyReference))
                return false;
            return true;
        }
    }
}
