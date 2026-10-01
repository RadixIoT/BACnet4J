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

package com.serotonin.bacnet4j.obj.mixin;

import static com.serotonin.bacnet4j.TestUtils.assertErrorAPDUException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.MultistateInputObject;
import com.serotonin.bacnet4j.obj.MultistateOutputObject;
import com.serotonin.bacnet4j.obj.MultistateValueObject;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.OptionalUnsigned;
import com.serotonin.bacnet4j.type.constructed.PriorityArray;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Relinquish_Default of a multi-state object has no range statement of its own, but Clause 19.2(c) requires it to be
 * of the same datatype as the commandable property, and 19.2.1.3 requires Present_Value to assume its value once the
 * Priority_Array empties. Present_Value is constrained to 1..Number_Of_States by 12.19.4 and 12.20.4, so a
 * Relinquish_Default outside that range is a value the property cannot take on, and a write of it must be refused
 * with PROPERTY / VALUE_OUT_OF_RANGE per the WriteProperty error table in 15.9.1.3.1.
 * <p>
 * Out_Of_Service does not relax this. 12.19.10(c) and 12.20.9(b) extend mandatory writability only to Present_Value
 * and Reliability, and 12.19.10(e) and 12.20.9(d) keep command prioritization running while out of service, so
 * Relinquish_Default still feeds Present_Value.
 * <p>
 * The state that the standard does permit - Relinquish_Default left out of range because Number_Of_States was
 * reduced beneath it - is reached by writing Number_Of_States, not by writing Relinquish_Default, and is covered by
 * the reliability handling in MultistateMixin.
 */
public class MultistateMixinTest extends AbstractTest {
    private static final int NUMBER_OF_STATES = 5;

    private List<BACnetObject> commandableMultistateObjects(boolean outOfService)
            throws BACnetServiceException {
        return List.of( //
                d1.addObject(new MultistateOutputObject(d1, 0, "mo", NUMBER_OF_STATES,
                        null, 1, 1, outOfService)), //
                d1.addObject(new MultistateValueObject(d1, 0, "mv", NUMBER_OF_STATES,
                        null, 1, outOfService).supportCommandable(new UnsignedInteger(1))));
    }

    @Test
    public void relinquishDefault_aboveNumberOfStatesIsRejected() throws Exception {
        for (BACnetObject bo : commandableMultistateObjects(false)) {
            assertRejected(bo, NUMBER_OF_STATES + 1);
            assertRejected(bo, Integer.MAX_VALUE);
        }
    }

    @Test
    public void relinquishDefault_zeroIsRejected() throws Exception {
        // 12.19.4 and 12.20.4: "The Present_Value property shall always have a value greater than zero."
        for (BACnetObject bo : commandableMultistateObjects(false)) {
            assertRejected(bo, 0);
        }
    }

    @Test
    public void relinquishDefault_outOfServiceDoesNotPermitOutOfRange() throws Exception {
        for (BACnetObject bo : commandableMultistateObjects(true)) {
            assertRejected(bo, NUMBER_OF_STATES + 1);
            assertRejected(bo, 0);
        }
    }

    @Test
    public void relinquishDefault_rejectionIsRelativeToCurrentNumberOfStates() throws Exception {
        for (BACnetObject bo : commandableMultistateObjects(false)) {
            // Raising Number_Of_States widens the range that Relinquish_Default may take.
            bo.writeProperty(null, new PropertyValue(PropertyIdentifier.numberOfStates,
                    new UnsignedInteger(NUMBER_OF_STATES + 3)));
            assertAccepted(bo, NUMBER_OF_STATES + 3);
            assertRejected(bo, NUMBER_OF_STATES + 4);
        }
    }

    @Test
    public void relinquishDefault_inRangeValuesAreAccepted() throws Exception {
        for (BACnetObject bo : commandableMultistateObjects(false)) {
            for (int value = 1; value <= NUMBER_OF_STATES; value++) {
                assertAccepted(bo, value);
            }
        }
    }

    @Test
    public void relinquishDefault_rejectedWriteLeavesPresentValueInRange() throws Exception {
        // CommandableMixin.afterWriteProperty pushes a new Relinquish_Default straight into Present_Value when the
        // Priority_Array is empty, so a rejected write must not have been applied at all.
        for (BACnetObject bo : commandableMultistateObjects(false)) {
            assertRejected(bo, NUMBER_OF_STATES + 1);

            UnsignedInteger relinquishDefault = bo.get(PropertyIdentifier.relinquishDefault);
            assertEquals(bo.getId() + ": Relinquish_Default was modified by a rejected write",
                    new UnsignedInteger(1), relinquishDefault);

            UnsignedInteger presentValue = bo.get(PropertyIdentifier.presentValue);
            assertEquals(bo.getId() + ": Present_Value was driven out of range by a rejected write",
                    new UnsignedInteger(1), presentValue);
        }
    }

    /**
     * Updating Relinquish_Default must leave the Priority_Array alone. It is only the value the commandable
     * property falls back to once every slot is Null, per 19.2(c), so it neither issues nor cancels a command.
     */
    @Test
    public void relinquishDefault_updatePreservesPriorityArray() throws Exception {
        ValueSource commander = new ValueSource(new Address(new byte[] {(byte) 12}));

        for (BACnetObject bo : commandableMultistateObjects(false)) {
            bo.writeProperty(commander, new PropertyValue(PropertyIdentifier.presentValue, null,
                    new UnsignedInteger(2), new UnsignedInteger(8)));
            bo.writeProperty(commander, new PropertyValue(PropertyIdentifier.presentValue, null,
                    new UnsignedInteger(4), new UnsignedInteger(12)));
            PriorityArray before = bo.get(PropertyIdentifier.priorityArray);

            bo.writeProperty(commander,
                    new PropertyValue(PropertyIdentifier.relinquishDefault, new UnsignedInteger(3)));

            assertEquals(bo.getId() + ": Priority_Array was modified", before,
                    bo.get(PropertyIdentifier.priorityArray));
            assertEquals(bo.getId() + ": Present_Value left the active command", new UnsignedInteger(2),
                    bo.get(PropertyIdentifier.presentValue));
            assertEquals(bo.getId() + ": Current_Command_Priority changed", new OptionalUnsigned(8),
                    bo.get(PropertyIdentifier.currentCommandPriority));

            // Only once every slot is relinquished does the new default take effect.
            bo.writeProperty(commander, new PropertyValue(PropertyIdentifier.presentValue, null, Null.instance,
                    new UnsignedInteger(8)));
            assertEquals(bo.getId() + ": Present_Value did not fall back to priority 12", new UnsignedInteger(4),
                    bo.get(PropertyIdentifier.presentValue));
            bo.writeProperty(commander, new PropertyValue(PropertyIdentifier.presentValue, null, Null.instance,
                    new UnsignedInteger(12)));
            assertEquals(bo.getId() + ": Present_Value did not fall back to Relinquish_Default",
                    new UnsignedInteger(3), bo.get(PropertyIdentifier.presentValue));
            assertEquals(bo.getId() + ": Current_Command_Priority is not Null", new OptionalUnsigned(),
                    bo.get(PropertyIdentifier.currentCommandPriority));
        }
    }

    @Test
    public void relinquishDefault_remoteWriteGetsAnErrorResponse() throws Exception {
        // The same rule seen from the wire: the write must produce an Error PDU, not a simple ack.
        for (BACnetObject bo : commandableMultistateObjects(false)) {
            assertErrorAPDUException(() -> d2.send(rd1, new WritePropertyRequest(bo.getId(),
                            PropertyIdentifier.relinquishDefault, null, new UnsignedInteger(NUMBER_OF_STATES + 1),
                            null)).get(), //
                    ErrorClass.property, ErrorCode.valueOutOfRange);
        }
    }

    @Test
    public void multistateInputHasNoRelinquishDefault() throws Exception {
        // MultistateInputObject uses the mixin but is not commandable, which is why it is absent from
        // commandableMultistateObjects. If that ever changes, this assertion fails and the object must be added.
        MultistateInputObject mi = d1.addObject(
                new MultistateInputObject(d1, 0, "mi0", NUMBER_OF_STATES, null, 1, false));
        assertNull(mi.get(PropertyIdentifier.relinquishDefault));
    }

    private static void assertRejected(BACnetObject bo, int relinquishDefault) {
        try {
            bo.writeProperty(null, new PropertyValue(PropertyIdentifier.relinquishDefault,
                    new UnsignedInteger(relinquishDefault)));
            fail(bo.getId() + ": expected an error writing a Relinquish_Default of " + relinquishDefault
                    + " when Number_Of_States is " + bo.get(PropertyIdentifier.numberOfStates));
        } catch (BACnetServiceException e) {
            assertEquals(bo.getId() + ": wrong error class writing a Relinquish_Default of " + relinquishDefault,
                    ErrorClass.property, e.getErrorClass());
            assertEquals(bo.getId() + ": wrong error code writing a Relinquish_Default of " + relinquishDefault,
                    ErrorCode.valueOutOfRange, e.getErrorCode());
        }
    }

    private static void assertAccepted(BACnetObject bo, int relinquishDefault) throws BACnetServiceException {
        bo.writeProperty(null, new PropertyValue(PropertyIdentifier.relinquishDefault,
                new UnsignedInteger(relinquishDefault)));
        UnsignedInteger written = bo.get(PropertyIdentifier.relinquishDefault);
        assertNotNull(bo.getId() + ": Relinquish_Default is missing", written);
        assertEquals(bo.getId() + ": Relinquish_Default of " + relinquishDefault + " was not accepted",
                new UnsignedInteger(relinquishDefault), written);
    }
}
