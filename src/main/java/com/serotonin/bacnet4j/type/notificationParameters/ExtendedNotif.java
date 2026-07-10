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

package com.serotonin.bacnet4j.type.notificationParameters;

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.Primitive;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ExtendedNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 9;

    private final Unsigned16 vendorId;
    private final UnsignedInteger extendedEventType;
    private final SequenceOf<Parameter> parameters;

    public ExtendedNotif(Unsigned16 vendorId, UnsignedInteger extendedEventType, SequenceOf<Parameter> parameters) {
        this.vendorId = vendorId;
        this.extendedEventType = extendedEventType;
        this.parameters = parameters;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, vendorId, 0);
        write(queue, extendedEventType, 1);
        write(queue, parameters, 2);
    }

    public ExtendedNotif(ByteQueue queue) throws BACnetException {
        vendorId = read(queue, Unsigned16.class, 0);
        extendedEventType = read(queue, UnsignedInteger.class, 1);
        parameters = readSequenceOf(queue, Parameter.class, 2);
    }

    public Unsigned16 getVendorId() {
        return vendorId;
    }

    public UnsignedInteger getExtendedEventType() {
        return extendedEventType;
    }

    public SequenceOf<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "ExtendedNotif [vendorId=" + vendorId + ", extendedEventType=" + extendedEventType + ", parameters="
                + parameters + "]";
    }

    public static class Parameter extends BaseType {
        private Primitive primitive;
        private DeviceObjectPropertyReference reference;

        public Parameter(Primitive primitive) {
            this.primitive = primitive;
        }

        public Parameter(DeviceObjectPropertyReference reference) {
            this.reference = reference;
        }

        @Override
        public void write(ByteQueue queue) {
            if (primitive != null)
                primitive.write(queue);
            else
                reference.write(queue, 0);
        }

        public Parameter(ByteQueue queue) throws BACnetException {
            if (queue.peek(0) == 0) {
                primitive = new Null(queue);
            } else {
                reference = readOptional(queue, DeviceObjectPropertyReference.class, 0);
                if (reference == null) {
                    primitive = Primitive.createPrimitive(queue);
                }
            }
        }

        @Override
        public String toString() {
            return "Parameter [primitive=" + primitive + ", reference=" + reference + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass())
                return false;
            Parameter parameter = (Parameter) o;
            return Objects.equals(primitive, parameter.primitive) && Objects.equals(reference,
                    parameter.reference);
        }

        @Override
        public int hashCode() {
            return Objects.hash(primitive, reference);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        ExtendedNotif that = (ExtendedNotif) o;
        return Objects.equals(vendorId, that.vendorId) && Objects.equals(extendedEventType,
                that.extendedEventType) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, extendedEventType, parameters);
    }
}
