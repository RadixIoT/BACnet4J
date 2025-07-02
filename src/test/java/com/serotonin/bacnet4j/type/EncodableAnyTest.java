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

package com.serotonin.bacnet4j.type;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;

import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Double;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class EncodableAnyTest {

    private final ByteQueue queue = new ByteQueue();
    private final PropertyIdentifier proprietaryProperty = PropertyIdentifier.forId(888); //proprietary property

    @After
    public void after() {
        //Clear queue after each test
        queue.clear();
    }

    @Test
    public void decodeProprietaryNull() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        com.serotonin.bacnet4j.type.primitive.Null value = com.serotonin.bacnet4j.type.primitive.Null.instance;
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(com.serotonin.bacnet4j.type.primitive.Null.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryBoolean() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        com.serotonin.bacnet4j.type.primitive.Boolean value = com.serotonin.bacnet4j.type.primitive.Boolean.TRUE;
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(com.serotonin.bacnet4j.type.primitive.Boolean.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryUnsignedInteger() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        UnsignedInteger value = new UnsignedInteger(777);
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(UnsignedInteger.class, encodable.getClass());
    }

    @Test
    public void decodeProprietarySignedInteger() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        SignedInteger value = new SignedInteger(777);
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(SignedInteger.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryReal() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        Real value = new Real(55.123F);
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(Real.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryDouble() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        com.serotonin.bacnet4j.type.primitive.Double value = new Double(55.123);
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(com.serotonin.bacnet4j.type.primitive.Double.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryOctetString() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        OctetString value = new OctetString(new ByteQueue("7c5d3a"));
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(OctetString.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryCharacterString() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        CharacterString value = new CharacterString("This is text");
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(CharacterString.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryBitString() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        BitString value = new BitString(new boolean[] {true, false, true});
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(BitString.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryEnumarated() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        Enumerated value = new Enumerated(5);
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(Enumerated.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryDate() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        com.serotonin.bacnet4j.type.primitive.Date value = new Date(2018, Month.JUNE, 1, DayOfWeek.MONDAY);
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(com.serotonin.bacnet4j.type.primitive.Date.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryTime() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        com.serotonin.bacnet4j.type.primitive.Time value = new Time(14, 56, 30, 0);
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(com.serotonin.bacnet4j.type.primitive.Time.class, encodable.getClass());
    }

    @Test
    public void decodeProprietaryObjectidentifier() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        ObjectIdentifier value = new ObjectIdentifier(ObjectType.device, 1);
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(ObjectIdentifier.class, encodable.getClass());
    }

    @Test
    public void decodeProprietarySequenceOfCharacterString() throws BACnetException {
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        SequenceOf<CharacterString> value = new SequenceOf<>();
        value.add(new CharacterString("Text 1"));
        value.add(new CharacterString("Text 2"));
        value.add(new CharacterString("Text 3"));
        value.write(queue);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(SequenceOf.class, encodable.getClass());
        SequenceOf<Encodable> seq = (SequenceOf<Encodable>) encodable;
        assertEquals(CharacterString.class, seq.get(0).getClass());
        assertEquals("Text 1", seq.get(0).toString());
        assertEquals("Text 2", seq.get(1).toString());
        assertEquals("Text 3", seq.get(2).toString());
    }

    @Test
    public void decodeProprietaryAmbigousPrimitive() throws BACnetException {
        String data =
                "e50d00546869732069732074657874"; // String "This is Text" with unknown primtive datatype Nr.14 (Hex 'e5')
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        queue.push(data);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(AmbiguousValue.class, encodable.getClass());
        ByteQueue resultQueue = new ByteQueue();
        encodable.write(resultQueue);
        assertEquals(data, resultQueue.toHexString());
    }

    @Test
    public void decodeProprietaryAmbigousConstructed() throws BACnetException {
        String data = "8201feb400000000b4173b3b630c0200295d2100118205e0"; //BACnetLIST of BACnetDestination
        queue.push("4e"); //Opening-Tag (Context Specific Tag, TagNumber 4)
        queue.push(data);
        queue.push("4f"); //Closing-Tag

        Encodable encodable = Encodable.readANY(queue, ObjectType.analogValue, proprietaryProperty, null, 4);
        assertEquals(AmbiguousValue.class, encodable.getClass());
        ByteQueue resultQueue = new ByteQueue();
        encodable.write(resultQueue);
        assertEquals(data, resultQueue.toHexString());
    }

}
