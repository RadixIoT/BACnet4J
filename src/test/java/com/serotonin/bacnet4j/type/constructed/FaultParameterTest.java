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

package com.serotonin.bacnet4j.type.constructed;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultCharacterString;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultExtended;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultExtended.FaultExtendedParameter;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultLifeSafety;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultState;
import com.serotonin.bacnet4j.type.constructed.FaultParameter.FaultStatusFlags;
import com.serotonin.bacnet4j.type.enumerated.Action;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.ProgramError;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.WriteStatus;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Double;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class FaultParameterTest {
    @Test
    public void characterString() throws BACnetException {
        final FaultParameter faultParameter = new FaultParameter(new FaultCharacterString(new SequenceOf<>( //
                new CharacterString("a"), //
                new CharacterString("bb"), //
                new CharacterString("ccc"))));

        final ByteQueue queue = new ByteQueue();
        faultParameter.write(queue);

        assertEquals(faultParameter, new FaultParameter(queue));
    }

    @Test
    public void extended() throws BACnetException {
        final FaultParameter faultParameter = new FaultParameter(new FaultExtended(new Unsigned16(236),
                new UnsignedInteger(314),
                new SequenceOf<>( //
                        new FaultExtendedParameter(Null.instance), //
                        new FaultExtendedParameter(new Real(3.1415F)), //
                        new FaultExtendedParameter(new UnsignedInteger(314)), //
                        new FaultExtendedParameter(Boolean.TRUE), //
                        new FaultExtendedParameter(new SignedInteger(-314)), //
                        new FaultExtendedParameter(new Double(Math.PI)), //
                        new FaultExtendedParameter(new OctetString(new byte[] {0, 1, 2, 3, 4, 5})), //
                        new FaultExtendedParameter(new CharacterString("This is an extended fault parameter")), //
                        new FaultExtendedParameter(new BitString(new boolean[] {true, false, false, true})), //
                        new FaultExtendedParameter(PropertyIdentifier.absenteeLimit), //
                        new FaultExtendedParameter(new Date(2017, Month.FEBRUARY, 1, DayOfWeek.WEDNESDAY)), //
                        new FaultExtendedParameter(new Time(17, 04, 31, 57)), //
                        new FaultExtendedParameter(new ObjectIdentifier(ObjectType.credentialDataInput, 12)), //
                        new FaultExtendedParameter(new DeviceObjectPropertyReference(
                                new ObjectIdentifier(ObjectType.largeAnalogValue, 13), PropertyIdentifier.description,
                                null, new ObjectIdentifier(ObjectType.device, 1234))))));

        final ByteQueue queue = new ByteQueue();
        faultParameter.write(queue);

        final FaultExtended rebuilt = new FaultParameter(queue).getFaultExtended();

        assertEquals(faultParameter.getFaultExtended().getVendorId(), rebuilt.getVendorId());
        assertEquals(faultParameter.getFaultExtended().getExtendedFaultType(), rebuilt.getExtendedFaultType());
        assertEquals(faultParameter.getFaultExtended().getParameters().size(), rebuilt.getParameters().size());
        assertEquals(faultParameter.getFaultExtended().getParameters().get(0), rebuilt.getParameters().get(0));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(1), rebuilt.getParameters().get(1));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(2), rebuilt.getParameters().get(2));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(3), rebuilt.getParameters().get(3));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(4), rebuilt.getParameters().get(4));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(5), rebuilt.getParameters().get(5));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(6), rebuilt.getParameters().get(6));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(7), rebuilt.getParameters().get(7));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(8), rebuilt.getParameters().get(8));
        assertEquals(((Enumerated) faultParameter.getFaultExtended().getParameters().get(9).getValue()).intValue(),
                ((Enumerated) rebuilt.getParameters().get(9).getValue()).intValue());
        assertEquals(faultParameter.getFaultExtended().getParameters().get(10), rebuilt.getParameters().get(10));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(11), rebuilt.getParameters().get(11));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(12), rebuilt.getParameters().get(12));
        assertEquals(faultParameter.getFaultExtended().getParameters().get(13), rebuilt.getParameters().get(13));
    }

    @Test
    public void lifeSafety() throws BACnetException {
        final FaultParameter faultParameter = new FaultParameter(new FaultLifeSafety(
                new SequenceOf<>( //
                        LifeSafetyState.abnormal, //
                        LifeSafetyState.active, //
                        LifeSafetyState.blocked, //
                        LifeSafetyState.duress), //
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.largeAnalogValue, 13),
                        PropertyIdentifier.description, null, new ObjectIdentifier(ObjectType.device, 1234))));

        final ByteQueue queue = new ByteQueue();
        faultParameter.write(queue);

        assertEquals(faultParameter, new FaultParameter(queue));
    }

    @Test
    public void state() throws BACnetException {
        final FaultParameter faultParameter = new FaultParameter(new FaultState(new SequenceOf<>( //
                new PropertyStates(Boolean.TRUE), //
                new PropertyStates(Boolean.FALSE), //
                new PropertyStates(ProgramError.loadFailed), //
                new PropertyStates(Action.reverse), //
                new PropertyStates(NotifyType.ackNotification), //
                new PropertyStates(NotifyType.event), //
                new PropertyStates(WriteStatus.failed), //
                new PropertyStates(WriteStatus.failed), //
                new PropertyStates(WriteStatus.inProgress), //
                new PropertyStates(WriteStatus.successful) //
        )));

        final ByteQueue queue = new ByteQueue();
        faultParameter.write(queue);

        assertEquals(faultParameter, new FaultParameter(queue));
    }

    @Test
    public void statusFlags() throws BACnetException {
        final FaultParameter faultParameter = new FaultParameter(new FaultStatusFlags(
                new DeviceObjectPropertyReference(new ObjectIdentifier(ObjectType.largeAnalogValue, 13),
                        PropertyIdentifier.description, null, new ObjectIdentifier(ObjectType.device, 1234))));

        final ByteQueue queue = new ByteQueue();
        faultParameter.write(queue);

        assertEquals(faultParameter, new FaultParameter(queue));
    }
}
