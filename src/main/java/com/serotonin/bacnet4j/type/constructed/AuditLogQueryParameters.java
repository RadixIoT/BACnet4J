/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.type.constructed;

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.SuccessFilter;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditLogQueryParameters extends BaseType {
    private static final ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, ByTarget.class);
        choiceOptions.addContextual(1, BySource.class);
    }

    private final Choice choice;

    public AuditLogQueryParameters(ByTarget byTarget) {
        choice = new Choice(0, byTarget, choiceOptions);
    }

    public AuditLogQueryParameters(BySource bySource) {
        choice = new Choice(1, bySource, choiceOptions);
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, choice);
    }

    public AuditLogQueryParameters(ByteQueue queue) throws BACnetException {
        choice = new Choice(queue, choiceOptions);
    }

    public boolean isByTargetDate() {
        return choice.getDatum() instanceof ByTarget;
    }

    public boolean isBySource() {
        return choice.getDatum() instanceof BySource;
    }

    public ByTarget getByTarget() {
        return (ByTarget) choice.getDatum();
    }

    public BySource getBySource() {
        return (BySource) choice.getDatum();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuditLogQueryParameters that = (AuditLogQueryParameters) o;
        return Objects.equals(choice, that.choice);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(choice);
    }

    @Override
    public String toString() {
        return "AuditLogQueryParameters [choice=" + choice + "]";
    }

    public static class ByTarget extends BaseType {
        private final ObjectIdentifier targetDeviceIdentifier;
        private final Address targetDeviceAddress;
        private final ObjectIdentifier targetObjectIdentifier;
        private final PropertyIdentifier targetPropertyIdentifier;
        private final UnsignedInteger targetArrayIndex;
        private final UnsignedInteger targetPriority;
        private final AuditOperationFlags operations;
        private final SuccessFilter successfulActionsOnly;

        public ByTarget(
                ObjectIdentifier targetDeviceIdentifier,
                Address targetDeviceAddress,
                ObjectIdentifier targetObjectIdentifier,
                PropertyIdentifier targetPropertyIdentifier,
                UnsignedInteger targetArrayIndex,
                UnsignedInteger targetPriority,
                AuditOperationFlags operations,
                SuccessFilter successfulActionsOnly) {
            this.targetDeviceIdentifier = targetDeviceIdentifier;
            this.targetDeviceAddress = targetDeviceAddress;
            this.targetObjectIdentifier = targetObjectIdentifier;
            this.targetPropertyIdentifier = targetPropertyIdentifier;
            this.targetArrayIndex = targetArrayIndex;
            this.targetPriority = targetPriority;
            this.operations = operations;
            this.successfulActionsOnly = successfulActionsOnly;
        }

        @Override
        public void write(ByteQueue queue) {
            write(queue, targetDeviceIdentifier, 0);
            writeOptional(queue, targetDeviceAddress, 1);
            writeOptional(queue, targetObjectIdentifier, 2);
            writeOptional(queue, targetPropertyIdentifier, 3);
            writeOptional(queue, targetArrayIndex, 4);
            writeOptional(queue, targetPriority, 5);
            writeOptional(queue, operations, 6);
            write(queue, successfulActionsOnly, 7);
        }

        public ByTarget(ByteQueue queue) throws BACnetException {
            targetDeviceIdentifier = read(queue, ObjectIdentifier.class, 0);
            targetDeviceAddress = readOptional(queue, Address.class, 1);
            targetObjectIdentifier = readOptional(queue, ObjectIdentifier.class, 2);
            targetPropertyIdentifier = readOptional(queue, PropertyIdentifier.class, 3);
            targetArrayIndex = readOptional(queue, UnsignedInteger.class, 4);
            targetPriority = readOptional(queue, UnsignedInteger.class, 5);
            operations = readOptional(queue, AuditOperationFlags.class, 6);
            successfulActionsOnly = read(queue, SuccessFilter.class, 7);
        }

        public ObjectIdentifier getTargetDeviceIdentifier() {
            return targetDeviceIdentifier;
        }

        public Address getTargetDeviceAddress() {
            return targetDeviceAddress;
        }

        public ObjectIdentifier getTargetObjectIdentifier() {
            return targetObjectIdentifier;
        }

        public PropertyIdentifier getTargetPropertyIdentifier() {
            return targetPropertyIdentifier;
        }

        public UnsignedInteger getTargetArrayIndex() {
            return targetArrayIndex;
        }

        public UnsignedInteger getTargetPriority() {
            return targetPriority;
        }

        public AuditOperationFlags getOperations() {
            return operations;
        }

        public SuccessFilter getSuccessfulActionsOnly() {
            return successfulActionsOnly;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass())
                return false;
            ByTarget byTarget = (ByTarget) o;
            return Objects.equals(targetDeviceIdentifier,
                    byTarget.targetDeviceIdentifier) && Objects.equals(targetDeviceAddress,
                    byTarget.targetDeviceAddress) && Objects.equals(targetObjectIdentifier,
                    byTarget.targetObjectIdentifier) && Objects.equals(targetPropertyIdentifier,
                    byTarget.targetPropertyIdentifier) && Objects.equals(targetArrayIndex,
                    byTarget.targetArrayIndex) && Objects.equals(targetPriority,
                    byTarget.targetPriority) && Objects.equals(operations,
                    byTarget.operations) && Objects.equals(successfulActionsOnly, byTarget.successfulActionsOnly);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetDeviceIdentifier, targetDeviceAddress, targetObjectIdentifier,
                    targetPropertyIdentifier,
                    targetArrayIndex, targetPriority, operations, successfulActionsOnly);
        }

        @Override
        public String toString() {
            return "ByTarget [+"
                    + "targetDeviceIdentifier=" + targetDeviceIdentifier
                    + ", targetDeviceAddress=" + targetDeviceAddress
                    + ", targetObjectIdentifier=" + targetObjectIdentifier
                    + ", targetPropertyIdentifier=" + targetPropertyIdentifier
                    + ", targetArrayIndex=" + targetArrayIndex
                    + ", targetPriority=" + targetPriority
                    + ", operations=" + operations
                    + ", successfulActionsOnly=" + successfulActionsOnly
                    + ']';
        }
    }


    public static class BySource extends BaseType {
        private final ObjectIdentifier sourceDeviceIdentifier;
        private final Address sourceDeviceAddress;
        private final ObjectIdentifier sourceObjectIdentifier;
        private final AuditOperationFlags operations;
        private final SuccessFilter successfulActionsOnly;

        public BySource(
                ObjectIdentifier sourceDeviceIdentifier,
                Address sourceDeviceAddress,
                ObjectIdentifier sourceObjectIdentifier,
                AuditOperationFlags operations,
                SuccessFilter successfulActionsOnly) {
            this.sourceDeviceIdentifier = sourceDeviceIdentifier;
            this.sourceDeviceAddress = sourceDeviceAddress;
            this.sourceObjectIdentifier = sourceObjectIdentifier;
            this.operations = operations;
            this.successfulActionsOnly = successfulActionsOnly;
        }

        @Override
        public void write(ByteQueue queue) {
            write(queue, sourceDeviceIdentifier, 0);
            writeOptional(queue, sourceDeviceAddress, 1);
            writeOptional(queue, sourceObjectIdentifier, 2);
            writeOptional(queue, operations, 3);
            write(queue, successfulActionsOnly, 4);
        }

        public BySource(ByteQueue queue) throws BACnetException {
            sourceDeviceIdentifier = read(queue, ObjectIdentifier.class, 0);
            sourceDeviceAddress = readOptional(queue, Address.class, 1);
            sourceObjectIdentifier = readOptional(queue, ObjectIdentifier.class, 2);
            operations = readOptional(queue, AuditOperationFlags.class, 3);
            successfulActionsOnly = read(queue, SuccessFilter.class, 4);
        }

        public ObjectIdentifier getSourceDeviceIdentifier() {
            return sourceDeviceIdentifier;
        }

        public Address getSourceDeviceAddress() {
            return sourceDeviceAddress;
        }

        public ObjectIdentifier getSourceObjectIdentifier() {
            return sourceObjectIdentifier;
        }

        public AuditOperationFlags getOperations() {
            return operations;
        }

        public SuccessFilter getSuccessfulActionsOnly() {
            return successfulActionsOnly;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass())
                return false;
            BySource bySource = (BySource) o;
            return Objects.equals(sourceDeviceIdentifier,
                    bySource.sourceDeviceIdentifier) && Objects.equals(sourceDeviceAddress,
                    bySource.sourceDeviceAddress) && Objects.equals(sourceObjectIdentifier,
                    bySource.sourceObjectIdentifier) && Objects.equals(operations,
                    bySource.operations) && Objects.equals(successfulActionsOnly, bySource.successfulActionsOnly);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceDeviceIdentifier, sourceDeviceAddress, sourceObjectIdentifier, operations,
                    successfulActionsOnly);
        }

        @Override
        public String toString() {
            return "BySource [+"
                    + "sourceDeviceIdentifier=" + sourceDeviceIdentifier
                    + ", sourceDeviceAddress=" + sourceDeviceAddress
                    + ", sourceObjectIdentifier=" + sourceObjectIdentifier
                    + ", operations=" + operations
                    + ", successfulActionsOnly=" + successfulActionsOnly
                    + ']';
        }
    }
}
