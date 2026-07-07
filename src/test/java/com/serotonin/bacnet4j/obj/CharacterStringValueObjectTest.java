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

package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.ErrorAPDUException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.OptionalCharacterString;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class CharacterStringValueObjectTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private LocalDevice client;
    private LocalDevice server;
    private CharacterStringValueObject csv;
    private RemoteDevice remoteServer;

    @Before
    public void before() throws Exception {
        client = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        server = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0))).initialize();

        csv = server.addObject(new CharacterStringValueObject(server, 0, "csv0",
                new CharacterString("initial"), false));

        // Seed Fault_Values with three OptionalCharacterString elements. Element type is
        // CHOICE { NULL, CharacterString }, so a mix of set and null entries is legal.
        csv.writePropertyInternal(PropertyIdentifier.faultValues,
                new BACnetArray<>(
                        new OptionalCharacterString(new CharacterString("fault-a")),
                        new OptionalCharacterString(new CharacterString("fault-b")),
                        new OptionalCharacterString(new CharacterString("fault-c"))));

        remoteServer = client.getRemoteDeviceBlocking(2, 1000);
    }

    @After
    public void after() {
        client.terminate();
        server.terminate();
    }

    /**
     * A client writes a bare NULL primitive to a specific index of the Fault_Values array on a
     * CharacterString_Value object. Fault_Values' element datatype is
     * OptionalCharacterString = CHOICE { NULL, CharacterString }, so this is a legitimate value
     * — not the "Null write silently ignored" case from addendum 135-2016br-2. The wire path
     * decodes the bare Null against the property's declared type and delivers an
     * OptionalCharacterString wrapping Null to the write handler, so the array element ends up
     * as Null and the write completes successfully.
     */
    @Test
    public void writeNullToFaultValuesIndex() throws Exception {
        client.send(remoteServer, new WritePropertyRequest(
                        csv.getId(),
                        PropertyIdentifier.faultValues,
                        new UnsignedInteger(2),   // 1-based array index
                        Null.instance,            // bare Null primitive on the wire
                        null))                    // no priority
                .get();

        BACnetArray<OptionalCharacterString> arr = csv.get(PropertyIdentifier.faultValues);
        assertEquals(3, arr.getCount());
        assertTrue(arr.getBase1(1).isCharacterStringValue());
        assertEquals("fault-a", arr.getBase1(1).getCharacterStringValue().getValue());
        assertFalse("index 2 should have been overwritten with Null",
                arr.getBase1(2).isCharacterStringValue());
        assertTrue(arr.getBase1(2).isNullValue());
        assertTrue(arr.getBase1(3).isCharacterStringValue());
        assertEquals("fault-c", arr.getBase1(3).getCharacterStringValue().getValue());
    }

    /**
     * Complementary case: writing an actual CharacterString to the same index round-trips
     * intact, confirming the write path is unrelated to the Null-specific handling above.
     */
    @Test
    public void writeCharacterStringToFaultValuesIndex() throws Exception {
        client.send(remoteServer, new WritePropertyRequest(
                        csv.getId(),
                        PropertyIdentifier.faultValues,
                        new UnsignedInteger(2),
                        new OptionalCharacterString(new CharacterString("fault-b-replaced")),
                        null))
                .get();

        BACnetArray<OptionalCharacterString> arr = csv.get(PropertyIdentifier.faultValues);
        assertEquals(3, arr.getCount());
        assertTrue(arr.getBase1(2).isCharacterStringValue());
        assertEquals("fault-b-replaced", arr.getBase1(2).getCharacterStringValue().getValue());
    }

    /**
     * Positive test for addendum 135-2016br-2 (strict reading): a client relinquishes a
     * non-commandable property whose declared datatype does not include NULL. Object_Name is
     * CharacterString-typed and not commandable. Under strict Clause 19.2.2 semantics a
     * relinquish requires both a NULL value and a priority. The property shall not be changed
     * and the write shall be reported successful.
     */
    @Test
    public void br2_relinquishOfNonCommandableNonNullTypedPropertySilentlySucceeds() throws Exception {
        CharacterString before = csv.get(PropertyIdentifier.objectName);

        client.send(remoteServer, new WritePropertyRequest(
                        csv.getId(),
                        PropertyIdentifier.objectName,
                        null,
                        Null.instance,
                        new UnsignedInteger(8)))         // priority present -> strict relinquish
                .get();

        assertEquals(before, csv.get(PropertyIdentifier.objectName));
    }

    /**
     * br-2 strict-relinquish gate: a NULL write to a non-commandable non-Null-typed property
     * without an explicit priority is not a "relinquish" per Clause 19.2.2. The br-2 bypass
     * does not apply, and the write must fail with invalidDataType.
     */
    @Test
    public void br2_bareNullWithoutPriorityStillFailsInvalidDataType() throws Exception {
        try {
            client.send(remoteServer, new WritePropertyRequest(
                            csv.getId(),
                            PropertyIdentifier.description,
                            null,
                            Null.instance,
                            null))            // no priority — not a relinquish
                    .get();
            fail("Expected ErrorAPDUException with invalid-data-type");
        } catch (ErrorAPDUException expected) {
            assertEquals(ErrorClass.property, expected.getError().getErrorClass());
            assertEquals(ErrorCode.invalidDataType, expected.getError().getErrorCode());
        }
    }

    /**
     * br-2 non-Null path: a non-NULL value written with a priority to a non-commandable
     * property succeeds; the priority is ignored per Clause 19.2.1. Confirms the guard does
     * not misfire on non-NULL values.
     */
    @Test
    public void br2_nonNullWriteWithPriorityToNonCommandableSucceeds() throws Exception {
        CharacterString newDescription = new CharacterString("changed via write");
        client.send(remoteServer, new WritePropertyRequest(
                        csv.getId(),
                        PropertyIdentifier.description,
                        null,
                        newDescription,
                        new UnsignedInteger(8)))
                .get();

        assertEquals(newDescription, csv.get(PropertyIdentifier.description));
    }

    /**
     * br-2 direct-API path: calling {@code writeProperty} on the object with a bare NULL
     * primitive PropertyValue behaves identically to the network path. Confirms the write-layer
     * guard fires regardless of how the write is issued.
     */
    @Test
    public void br2_directApiRelinquishSilentlySucceeds() throws Exception {
        CharacterString before = csv.get(PropertyIdentifier.description);

        csv.writeProperty(null,
                new PropertyValue(PropertyIdentifier.description, null, Null.instance, new UnsignedInteger(6)));

        assertEquals(before, csv.get(PropertyIdentifier.description));
    }

    /**
     * br-2 commandable-instance exclusion: a NULL-with-priority write to Present_Value on a
     * commandable CharacterString_Value (supportCommandable enabled) must be routed to
     * CommandableMixin as a real relinquish — not silently succeeded. The priority slot is
     * cleared and the effective Present_Value falls back through the priority array.
     */
    @Test
    public void br2_relinquishOfCommandablePresentValueRoutesToCommandableMixin() throws Exception {
        CharacterString relinquishDefault = new CharacterString("relinquished");
        CharacterStringValueObject commandable = server.addObject(new CharacterStringValueObject(
                server, 1, "csv1", new CharacterString("initial"), false).supportCommandable(relinquishDefault));

        // Command a value at priority 8 so we have a slot to relinquish.
        client.send(remoteServer, new WritePropertyRequest(
                        commandable.getId(),
                        PropertyIdentifier.presentValue,
                        null,
                        new CharacterString("commanded"),
                        new UnsignedInteger(8)))
                .get();
        assertEquals(new CharacterString("commanded"), commandable.get(PropertyIdentifier.presentValue));

        // Now relinquish priority 8 with a NULL. This is a REAL relinquish handled by
        // CommandableMixin, not a br-2 silent success.
        client.send(remoteServer, new WritePropertyRequest(
                        commandable.getId(),
                        PropertyIdentifier.presentValue,
                        null,
                        Null.instance,
                        new UnsignedInteger(8)))
                .get();

        // With no other priority active, effective present value falls back to Relinquish_Default.
        assertEquals(relinquishDefault, commandable.get(PropertyIdentifier.presentValue));
    }
}
