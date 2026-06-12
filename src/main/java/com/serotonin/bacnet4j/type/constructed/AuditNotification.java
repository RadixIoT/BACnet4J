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
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.AuditOperation;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.Unsigned8;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditNotification extends BaseType {
    private final TimeStamp sourceTimestamp;
    private final TimeStamp targetTimestamp;
    private final Recipient sourceDevice;
    private final ObjectIdentifier sourceObject;
    private final AuditOperation operation;
    private final CharacterString sourceComment;
    private final CharacterString targetComment;
    private final Unsigned8 invokeId;
    private final Unsigned16 sourceUserId;
    private final Unsigned8 sourceUserRole;
    private final Recipient targetDevice;
    private final ObjectIdentifier targetObject;
    private final PropertyReference targetProperty;
    private final UnsignedInteger targetPriority;
    private final Encodable targetValue;
    private final Encodable currentValue;
    private final ErrorClassAndCode result;

    public AuditNotification(
            TimeStamp sourceTimestamp,
            TimeStamp targetTimestamp,
            Recipient sourceDevice,
            ObjectIdentifier sourceObject,
            AuditOperation operation,
            CharacterString sourceComment,
            CharacterString targetComment,
            Unsigned8 invokeId,
            Unsigned16 sourceUserId,
            Unsigned8 sourceUserRole,
            Recipient targetDevice,
            ObjectIdentifier targetObject,
            PropertyReference targetProperty,
            UnsignedInteger targetPriority,
            Encodable targetValue,
            Encodable currentValue,
            ErrorClassAndCode result) {
        this.sourceTimestamp = sourceTimestamp;
        this.targetTimestamp = targetTimestamp;
        this.sourceDevice = sourceDevice;
        this.sourceObject = sourceObject;
        this.operation = operation;
        this.sourceComment = sourceComment;
        this.targetComment = targetComment;
        this.invokeId = invokeId;
        this.sourceUserId = sourceUserId;
        this.sourceUserRole = sourceUserRole;
        this.targetDevice = targetDevice;
        this.targetObject = targetObject;
        this.targetProperty = targetProperty;
        this.targetPriority = targetPriority;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.result = result;
    }

    @Override
    public void write(final ByteQueue queue) {
        writeOptional(queue, sourceTimestamp, 0);
        writeOptional(queue, targetTimestamp, 1);
        write(queue, sourceDevice, 2);
        writeOptional(queue, sourceObject, 3);
        write(queue, operation, 4);
        writeOptional(queue, sourceComment, 5);
        writeOptional(queue, targetComment, 6);
        writeOptional(queue, invokeId, 7);
        writeOptional(queue, sourceUserId, 8);
        writeOptional(queue, sourceUserRole, 9);
        write(queue, targetDevice, 10);
        writeOptional(queue, targetObject, 11);
        writeOptional(queue, targetProperty, 12);
        writeOptional(queue, targetPriority, 13);
        writeOptional(queue, targetValue, 14);
        writeOptional(queue, currentValue, 15);
        writeOptional(queue, result, 16);
    }

    @Override
    public String toString() {
        return "AuditNotification [" +
                "sourceTimestamp=" + sourceTimestamp +
                ", targetTimestamp=" + targetTimestamp +
                ", sourceDevice=" + sourceDevice +
                ", sourceObject=" + sourceObject +
                ", operation=" + operation +
                ", sourceComment=" + sourceComment +
                ", targetComment=" + targetComment +
                ", invokeId=" + invokeId +
                ", sourceUserId=" + sourceUserId +
                ", sourceUserRole=" + sourceUserRole +
                ", targetDevice=" + targetDevice +
                ", targetObject=" + targetObject +
                ", targetProperty=" + targetProperty +
                ", targetPriority=" + targetPriority +
                ", targetValue=" + targetValue +
                ", currentValue=" + currentValue +
                ", result=" + result +
                ']';
    }

    public TimeStamp getSourceTimestamp() {
        return sourceTimestamp;
    }

    public TimeStamp getTargetTimestamp() {
        return targetTimestamp;
    }

    public Recipient getSourceDevice() {
        return sourceDevice;
    }

    public ObjectIdentifier getSourceObject() {
        return sourceObject;
    }

    public AuditOperation getOperation() {
        return operation;
    }

    public CharacterString getSourceComment() {
        return sourceComment;
    }

    public CharacterString getTargetComment() {
        return targetComment;
    }

    public Unsigned8 getInvokeId() {
        return invokeId;
    }

    public Unsigned16 getSourceUserId() {
        return sourceUserId;
    }

    public Unsigned8 getSourceUserRole() {
        return sourceUserRole;
    }

    public Recipient getTargetDevice() {
        return targetDevice;
    }

    public ObjectIdentifier getTargetObject() {
        return targetObject;
    }

    public PropertyReference getTargetProperty() {
        return targetProperty;
    }

    public UnsignedInteger getTargetPriority() {
        return targetPriority;
    }

    public Encodable getTargetValue() {
        return targetValue;
    }

    public Encodable getCurrentValue() {
        return currentValue;
    }

    public ErrorClassAndCode getResult() {
        return result;
    }

    public AuditNotification(final ByteQueue queue) throws BACnetException {
        sourceTimestamp = readOptional(queue, TimeStamp.class, 0);
        targetTimestamp = readOptional(queue, TimeStamp.class, 1);
        sourceDevice = read(queue, Recipient.class, 2);
        sourceObject = readOptional(queue, ObjectIdentifier.class, 3);
        operation = read(queue, AuditOperation.class, 4);
        sourceComment = readOptional(queue, CharacterString.class, 5);
        targetComment = readOptional(queue, CharacterString.class, 6);
        invokeId = readOptional(queue, Unsigned8.class, 7);
        sourceUserId = readOptional(queue, Unsigned16.class, 8);
        sourceUserRole = readOptional(queue, Unsigned8.class, 9);
        targetDevice = read(queue, Recipient.class, 10);
        targetObject = readOptional(queue, ObjectIdentifier.class, 11);
        targetProperty = readOptional(queue, PropertyReference.class, 12);
        targetPriority = readOptional(queue, UnsignedInteger.class, 13);
        if (targetObject != null && targetProperty != null) {
            targetValue = readOptionalANY(queue, targetObject.getObjectType(), targetProperty.getPropertyIdentifier(),
                    targetProperty.getPropertyArrayIndex(), 14);
            currentValue = readOptionalANY(queue, targetObject.getObjectType(), targetProperty.getPropertyIdentifier(),
                    targetProperty.getPropertyArrayIndex(), 15);
        } else {
            targetValue = null;
            currentValue = null;
        }
        result = readOptional(queue, ErrorClassAndCode.class, 16);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuditNotification that = (AuditNotification) o;
        return Objects.equals(sourceTimestamp, that.sourceTimestamp) && Objects.equals(targetTimestamp,
                that.targetTimestamp) && Objects.equals(sourceDevice,
                that.sourceDevice) && Objects.equals(sourceObject, that.sourceObject) && Objects.equals(
                operation, that.operation) && Objects.equals(sourceComment,
                that.sourceComment) && Objects.equals(targetComment,
                that.targetComment) && Objects.equals(invokeId, that.invokeId) && Objects.equals(
                sourceUserId, that.sourceUserId) && Objects.equals(sourceUserRole,
                that.sourceUserRole) && Objects.equals(targetDevice,
                that.targetDevice) && Objects.equals(targetObject, that.targetObject) && Objects.equals(
                targetProperty, that.targetProperty) && Objects.equals(targetPriority,
                that.targetPriority) && Objects.equals(targetValue, that.targetValue) && Objects.equals(
                currentValue, that.currentValue) && Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceTimestamp, targetTimestamp, sourceDevice, sourceObject, operation, sourceComment,
                targetComment, invokeId, sourceUserId, sourceUserRole, targetDevice, targetObject, targetProperty,
                targetPriority, targetValue, currentValue, result);
    }
}
