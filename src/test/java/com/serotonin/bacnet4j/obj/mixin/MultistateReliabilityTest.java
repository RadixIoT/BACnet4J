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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.MultistateInputObject;
import com.serotonin.bacnet4j.obj.MultistateOutputObject;
import com.serotonin.bacnet4j.obj.MultistateValueObject;
import com.serotonin.bacnet4j.obj.NotificationClassObject;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyMultipleRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.StatusFlags;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.constructed.WriteAccessSpecification;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * The first stage of reliability-evaluation for the Multi-state Output and Value object types, per 12.19.11 and
 * 12.20.10 (addendum 135-2016br-5) and the precedence rules of 13.2.2.2.
 */
public class MultistateReliabilityTest extends AbstractTest {
    private static final int NUMBER_OF_STATES = 8;

    private MultistateOutputObject mo() throws Exception {
        return d1.addObject(new MultistateOutputObject(d1, 0, "mo0", NUMBER_OF_STATES, null, 1, 1, false));
    }

    private MultistateValueObject mv() throws Exception {
        return d1.addObject(new MultistateValueObject(d1, 0, "mv0", NUMBER_OF_STATES, null, 1, false)
                .supportCommandable(new UnsignedInteger(1)));
    }

    /** A commanding entity that is not the local device, so that the value source mechanism has a source. */
    private static final ValueSource COMMANDER = new ValueSource(new Address(new byte[] {(byte) 12}));

    private static void command(BACnetObject bo, Integer value, int priority) throws Exception {
        bo.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.presentValue, null,
                value == null ? Null.instance : new UnsignedInteger(value), new UnsignedInteger(priority)));
    }

    private static void writeNumberOfStates(BACnetObject bo, int numberOfStates) throws Exception {
        bo.writeProperty(COMMANDER,
                new PropertyValue(PropertyIdentifier.numberOfStates, new UnsignedInteger(numberOfStates)));
    }

    // ----------------------------------------------------------------------------------------------------
    // What a reported fault looks like to a client.
    // ----------------------------------------------------------------------------------------------------

    /**
     * 13.2.2.1.1: the object is in the Fault state whenever reliability-evaluation indicates a value other than
     * NO_FAULT_DETECTED, and 12.19.7 derives the FAULT and IN_ALARM status flags from that.
     */
    @Test
    public void reportedFaultDrivesEventStateAndStatusFlags() throws Exception {
        notificationClass();
        MultistateOutputObject mo = mo();
        mo.supportIntrinsicReporting(5, 17, 1, new EventTransitionBits(true, true, true), NotifyType.alarm, 12);

        command(mo, 6, 8);
        assertReliability(mo, Reliability.noFaultDetected);
        assertEquals(EventState.normal, mo.get(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), mo.get(PropertyIdentifier.statusFlags));

        writeNumberOfStates(mo, 4);
        assertReliability(mo, Reliability.multiStateOutOfRange);
        assertEquals(EventState.fault, mo.get(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(true, true, false, false), mo.get(PropertyIdentifier.statusFlags));

        writeNumberOfStates(mo, NUMBER_OF_STATES);
        assertReliability(mo, Reliability.noFaultDetected);
        assertEquals(EventState.normal, mo.get(PropertyIdentifier.eventState));
        assertEquals(new StatusFlags(false, false, false, false), mo.get(PropertyIdentifier.statusFlags));
    }

    /**
     * An object that already reports a fault when event reporting is added to it starts in the Fault state. The
     * write of Reliability that reported the fault happened before the reporting mixin existed, so the event
     * state has to be initialized from the property rather than observed.
     */
    @Test
    public void eventStateIsInitializedFromAnExistingFault() throws Exception {
        notificationClass();
        MultistateValueObject mv = mv();

        command(mv, 6, 8);
        writeNumberOfStates(mv, 4);
        assertReliability(mv, Reliability.multiStateOutOfRange);
        assertEquals(EventState.normal, mv.get(PropertyIdentifier.eventState));

        mv.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(2)), null,
                new EventTransitionBits(true, true, true), NotifyType.alarm, 12);
        assertEquals(EventState.fault, mv.get(PropertyIdentifier.eventState));
    }

    // ----------------------------------------------------------------------------------------------------
    // Simulated internal faults, per 13.2.2.3.
    // ----------------------------------------------------------------------------------------------------

    /**
     * 13.2.2.3: a write of Reliability while Out_Of_Service is TRUE simulates an internal fault and blocks the
     * object from updating Reliability, until Out_Of_Service is written FALSE. The Alarm_Values/Fault_Values
     * overlap is used here because it is the one condition that is not already suppressed by Out_Of_Service.
     */
    @Test
    public void simulatedFaultBlocksReporting() throws Exception {
        notificationClass();
        MultistateValueObject mv = mv();
        mv.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(2)),
                new BACnetArray<>(new UnsignedInteger(3)), new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);
        assertReliability(mv, Reliability.noFaultDetected);

        mv.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.outOfService, Boolean.TRUE));
        // MULTI_STATE_FAULT is simulated rather than some other value because it is the one fault the first
        // stage is otherwise free to displace, per 13.2.2.2. The block is therefore the only thing that can
        // prevent the update below, so this test fails if the block is removed.
        mv.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.reliability, Reliability.multiStateFault));
        assertReliability(mv, Reliability.multiStateFault);

        // The overlap now holds, but the object must not update Reliability over the simulated fault.
        mv.writePropertyInternal(PropertyIdentifier.faultValues, new BACnetArray<>(new UnsignedInteger(2)));
        assertReliability(mv, Reliability.multiStateFault);

        // Returning to service removes the block, and the situation is reported.
        mv.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.outOfService, Boolean.FALSE));
        assertReliability(mv, Reliability.configurationError);
    }

    // ----------------------------------------------------------------------------------------------------
    // The Alarm_Values / Fault_Values overlap, per 12.20.8.
    // ----------------------------------------------------------------------------------------------------

    /**
     * 12.20.8: a value present in both Alarm_Values and Fault_Values is a configuration error. Unlike the range
     * conditions of 12.20.10, this one carries no out of service exemption.
     */
    @Test
    public void alarmAndFaultValueOverlap() throws Exception {
        notificationClass();
        MultistateValueObject mv = mv();
        mv.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(2), new UnsignedInteger(3)),
                new BACnetArray<>(new UnsignedInteger(5)), new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);
        assertReliability(mv, Reliability.noFaultDetected);

        mv.writePropertyInternal(PropertyIdentifier.faultValues, new BACnetArray<>(new UnsignedInteger(3)));
        assertReliability(mv, Reliability.configurationError);

        // Out of service does not suppress it.
        mv.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.outOfService, Boolean.TRUE));
        assertReliability(mv, Reliability.configurationError);
        mv.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.outOfService, Boolean.FALSE));

        // Removing the overlap resolves it.
        mv.writePropertyInternal(PropertyIdentifier.faultValues, new BACnetArray<>(new UnsignedInteger(5)));
        assertReliability(mv, Reliability.noFaultDetected);
    }

    /**
     * The overlap of 12.20.8 and the range conditions of 12.20.10 can hold at once, and Reliability carries one
     * value. The overlap is reported, because it is the condition that has no out of service exemption.
     */
    @Test
    public void overlapIsReportedAheadOfTheRangeConditions() throws Exception {
        notificationClass();
        MultistateValueObject mv = mv();
        mv.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(2)),
                new BACnetArray<>(new UnsignedInteger(3)), new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);

        command(mv, 6, 8);
        writeNumberOfStates(mv, 4);
        assertReliability(mv, Reliability.multiStateOutOfRange);

        mv.writePropertyInternal(PropertyIdentifier.faultValues, new BACnetArray<>(new UnsignedInteger(2)));
        assertReliability(mv, Reliability.configurationError);

        // With the overlap gone, the range condition is reported again, since it never went away.
        mv.writePropertyInternal(PropertyIdentifier.faultValues, new BACnetArray<>(new UnsignedInteger(3)));
        assertReliability(mv, Reliability.multiStateOutOfRange);
    }

    // ----------------------------------------------------------------------------------------------------
    // The Multi-state Input is deliberately exempt from the range conditions.
    // ----------------------------------------------------------------------------------------------------

    /**
     * 12.18.11 has no equivalent of the CONFIGURATION_ERROR requirement in 12.19.11 and 12.20.10, so the
     * Multi-state Input neither reports nor refuses out of range Alarm_Values and Fault_Values.
     */
    @Test
    public void multistateInputIsExemptFromTheRangeConditions() throws Exception {
        notificationClass();
        MultistateInputObject mi = d1.addObject(
                new MultistateInputObject(d1, 0, "mi0", NUMBER_OF_STATES, null, 1, false));
        mi.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(6)),
                new BACnetArray<>(new UnsignedInteger(7)), new EventTransitionBits(true, true, true),
                NotifyType.alarm, new UnsignedInteger(12));

        // Present_Value stays in range, so only the other properties fall out of it.
        writeNumberOfStates(mi, 4);
        assertReliability(mi, Reliability.noFaultDetected);

        // And a write of an out of range value is accepted rather than refused.
        mi.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.alarmValues,
                new BACnetArray<>(new UnsignedInteger(9))));
        assertReliability(mi, Reliability.noFaultDetected);

        // Present_Value out of range is still reported, which 12.18.11 does require.
        mi.writePropertyInternal(PropertyIdentifier.presentValue, new UnsignedInteger(6));
        assertReliability(mi, Reliability.multiStateOutOfRange);
    }

    // ----------------------------------------------------------------------------------------------------
    // Handing the Reliability property back to the fault algorithm.
    // ----------------------------------------------------------------------------------------------------

    /**
     * 13.4: the fault algorithm's result has to reflect the current monitored value. When the first stage stops
     * reporting a fault it owned, the algorithm's own condition may still hold, so it is re-derived rather than
     * left until the next write of the monitored value.
     */
    @Test
    public void faultAlgorithmReDerivesWhenTheFirstStageStopsReporting() throws Exception {
        notificationClass();
        MultistateValueObject mv = mv();
        mv.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(2)),
                new BACnetArray<>(new UnsignedInteger(6)), new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);

        command(mv, 6, 8);
        assertReliability(mv, Reliability.multiStateFault);

        writeNumberOfStates(mv, 4);
        assertReliability(mv, Reliability.multiStateOutOfRange);

        // The first stage stops reporting. Present_Value is still 6, which is still in Fault_Values, so the
        // fault algorithm takes the property back rather than leaving it at no-fault-detected.
        writeNumberOfStates(mv, NUMBER_OF_STATES);
        assertReliability(mv, Reliability.multiStateFault);
        assertEquals(EventState.fault, mv.get(PropertyIdentifier.eventState));

        // And once the monitored value leaves Fault_Values, the algorithm clears it.
        command(mv, 2, 8);
        assertReliability(mv, Reliability.noFaultDetected);
    }

    // ----------------------------------------------------------------------------------------------------
    // Interaction with the sequential semantics of WritePropertyMultiple, per 15.10.2.
    // ----------------------------------------------------------------------------------------------------

    /**
     * 15.10: properties are modified in the order given, so each is validated against the state the earlier
     * writes produced. Ordering Number_Of_States ahead of the values it bounds is the client's responsibility.
     */
    @Test
    public void writePropertyMultipleIsValidatedInOrder() throws Exception {
        MultistateOutputObject widening = d1.addObject(
                new MultistateOutputObject(d1, 1, "mo1", 4, null, 1, 1, false));

        // Number_Of_States first: both writes are applied.
        d2.send(rd1, new WritePropertyMultipleRequest(new SequenceOf<>(
                new WriteAccessSpecification(widening.getId(), new SequenceOf<>( //
                        new PropertyValue(PropertyIdentifier.numberOfStates, new UnsignedInteger(NUMBER_OF_STATES)),
                        new PropertyValue(PropertyIdentifier.relinquishDefault, new UnsignedInteger(7))))))).get();
        assertEquals(new UnsignedInteger(NUMBER_OF_STATES), widening.get(PropertyIdentifier.numberOfStates));
        assertEquals(new UnsignedInteger(7), widening.get(PropertyIdentifier.relinquishDefault));
        assertReliability(widening, Reliability.noFaultDetected);

        MultistateOutputObject misordered = d1.addObject(
                new MultistateOutputObject(d1, 2, "mo2", 4, null, 1, 1, false));

        // The other order fails at the first element, and nothing is applied.
        TestUtils.assertErrorAPDUException(() -> d2.send(rd1, new WritePropertyMultipleRequest(new SequenceOf<>(
                        new WriteAccessSpecification(misordered.getId(), new SequenceOf<>( //
                                new PropertyValue(PropertyIdentifier.relinquishDefault, new UnsignedInteger(7)),
                                new PropertyValue(PropertyIdentifier.numberOfStates,
                                        new UnsignedInteger(NUMBER_OF_STATES))))))).get(), //
                ErrorClass.property, ErrorCode.valueOutOfRange);
        assertEquals(new UnsignedInteger(4), misordered.get(PropertyIdentifier.numberOfStates));
        assertEquals(new UnsignedInteger(1), misordered.get(PropertyIdentifier.relinquishDefault));
        assertNotEquals(Reliability.configurationError, misordered.get(PropertyIdentifier.reliability));
    }

    private void notificationClass() throws Exception {
        d1.addObject(new NotificationClassObject(d1, 17, "nc17", 100, 5, 200,
                new EventTransitionBits(false, false, false)));
    }

    private static void assertRefused(BACnetObject bo, PropertyIdentifier pid, Encodable value) {
        Encodable before = bo.get(pid);
        try {
            bo.writeProperty(COMMANDER, new PropertyValue(pid, value));
            fail(bo.getId() + ": expected an error writing " + pid + " of " + value);
        } catch (BACnetServiceException e) {
            assertEquals(bo.getId() + ": wrong error class writing " + pid, ErrorClass.property, e.getErrorClass());
            assertEquals(bo.getId() + ": wrong error code writing " + pid, ErrorCode.valueOutOfRange,
                    e.getErrorCode());
        }
        assertEquals(bo.getId() + ": " + pid + " was modified by a refused write", before, bo.get(pid));
    }

    private static void assertReliability(BACnetObject bo, Reliability expected) {
        assertEquals(bo.getId().toString(), expected, bo.get(PropertyIdentifier.reliability));
    }

    /**
     * A state value outside 1..Number_Of_States is refused with VALUE_OUT_OF_RANGE, per 15.9.1.3.1, for every
     * property that 12.19.11 and 12.20.10 describe as capable of falling out of range.
     */
    @Test
    public void outOfRangeWritesAreRefused() throws Exception {
        d1.addObject(new NotificationClassObject(d1, 17, "nc17", 100, 5, 200,
                new EventTransitionBits(false, false, false)));

        MultistateOutputObject mo = mo();
        mo.supportIntrinsicReporting(5, 17, 1, new EventTransitionBits(true, true, true), NotifyType.alarm, 12);
        assertRefused(mo, PropertyIdentifier.relinquishDefault, new UnsignedInteger(NUMBER_OF_STATES + 1));
        assertRefused(mo, PropertyIdentifier.relinquishDefault, UnsignedInteger.ZERO);
        assertRefused(mo, PropertyIdentifier.feedbackValue, new UnsignedInteger(NUMBER_OF_STATES + 1));
        assertRefused(mo, PropertyIdentifier.feedbackValue, UnsignedInteger.ZERO);

        MultistateValueObject mv = mv();
        mv.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(2)),
                new BACnetArray<>(new UnsignedInteger(3)), new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);
        assertRefused(mv, PropertyIdentifier.relinquishDefault, new UnsignedInteger(NUMBER_OF_STATES + 1));
        assertRefused(mv, PropertyIdentifier.alarmValues,
                new BACnetArray<>(new UnsignedInteger(2), new UnsignedInteger(NUMBER_OF_STATES + 1)));
        assertRefused(mv, PropertyIdentifier.faultValues, new BACnetArray<>(UnsignedInteger.ZERO));

        // Present_Value and Number_Of_States carry the same range rules and so the same error code. Priority_Array
        // is read-only, so an out of range command reaches it only as a write of Present_Value.
        assertRefused(mv, PropertyIdentifier.presentValue, new UnsignedInteger(NUMBER_OF_STATES + 1));
        assertRefused(mv, PropertyIdentifier.presentValue, UnsignedInteger.ZERO);
        assertRefused(mv, PropertyIdentifier.numberOfStates, UnsignedInteger.ZERO);
    }

    /**
     * A state value given at creation is not refused. The object is created and reports the situation, the same
     * as if Number_Of_States had later been reduced beneath the value.
     */
    @Test
    public void outOfRangeCreationValuesAreReported() throws Exception {
        d1.addObject(new NotificationClassObject(d1, 17, "nc17", 100, 5, 200,
                new EventTransitionBits(false, false, false)));

        MultistateOutputObject mo = d1.addObject(new MultistateOutputObject(d1, 1, "mo1", NUMBER_OF_STATES, null, 1,
                NUMBER_OF_STATES + 1, false));
        assertReliability(mo, Reliability.configurationError);

        MultistateValueObject mv = d1.addObject(new MultistateValueObject(d1, 1, "mv1", NUMBER_OF_STATES, null, 1,
                false).supportCommandable(UnsignedInteger.ZERO));
        assertReliability(mv, Reliability.configurationError);

        // An out of range Feedback_Value, given when intrinsic reporting is added.
        MultistateOutputObject mo2 = mo();
        mo2.supportIntrinsicReporting(5, 17, NUMBER_OF_STATES + 1, new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);
        assertReliability(mo2, Reliability.configurationError);

        // An out of range Alarm_Values member, likewise.
        MultistateValueObject mv2 = mv();
        mv2.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(NUMBER_OF_STATES + 1)), null,
                new EventTransitionBits(true, true, true), NotifyType.alarm, 12);
        assertReliability(mv2, Reliability.configurationError);
    }

    /**
     * An object created out of service reports none of the range conditions, per 12.19.11 and 12.20.10.
     */
    @Test
    public void outOfRangeCreationValuesAreSuppressedWhileOutOfService() throws Exception {
        MultistateOutputObject mo = d1.addObject(new MultistateOutputObject(d1, 1, "mo1", NUMBER_OF_STATES, null, 1,
                NUMBER_OF_STATES + 1, true));
        assertReliability(mo, Reliability.noFaultDetected);

        // Returning it to service reports the situation, which has not changed.
        mo.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.outOfService, Boolean.FALSE));
        assertReliability(mo, Reliability.configurationError);
    }

    /**
     * 12.19.11, 12.20.10: an out of range Present_Value is reported as MULTI_STATE_OUT_OF_RANGE for as long as
     * the situation remains.
     */
    @Test
    public void presentValueOutOfRange() throws Exception {
        for (BACnetObject bo : new BACnetObject[] {mo(), mv()}) {
            command(bo, 6, 8);
            assertReliability(bo, Reliability.noFaultDetected);

            writeNumberOfStates(bo, 4);
            assertReliability(bo, Reliability.multiStateOutOfRange);

            // Raising Number_Of_States again resolves the situation.
            writeNumberOfStates(bo, NUMBER_OF_STATES);
            assertReliability(bo, Reliability.noFaultDetected);
        }
    }

    /**
     * 12.19.11, 12.20.10: a Relinquish_Default left out of range is reported as CONFIGURATION_ERROR. Present_Value
     * is in range throughout, so the more specific MULTI_STATE_OUT_OF_RANGE does not apply.
     */
    @Test
    public void relinquishDefaultOutOfRange() throws Exception {
        for (BACnetObject bo : new BACnetObject[] {mo(), mv()}) {
            bo.writeProperty(null,
                    new PropertyValue(PropertyIdentifier.relinquishDefault, new UnsignedInteger(7)));
            command(bo, 2, 8);
            assertReliability(bo, Reliability.noFaultDetected);

            writeNumberOfStates(bo, 4);
            assertEquals(new UnsignedInteger(2), bo.get(PropertyIdentifier.presentValue));
            assertReliability(bo, Reliability.configurationError);

            // Bringing Relinquish_Default back into range resolves the situation.
            bo.writeProperty(null,
                    new PropertyValue(PropertyIdentifier.relinquishDefault, new UnsignedInteger(3)));
            assertReliability(bo, Reliability.noFaultDetected);
        }
    }

    /**
     * 12.19.11, 12.20.10: a Priority_Array entry that is not currently in effect is reported as
     * CONFIGURATION_ERROR, and becomes MULTI_STATE_OUT_OF_RANGE once the commands above it are relinquished.
     */
    @Test
    public void latentPriorityArrayEntryOutOfRange() throws Exception {
        for (BACnetObject bo : new BACnetObject[] {mo(), mv()}) {
            command(bo, 7, 12);
            command(bo, 2, 8);
            assertReliability(bo, Reliability.noFaultDetected);

            // Present_Value comes from priority 8 and stays in range. The latent command at priority 12 does not.
            writeNumberOfStates(bo, 4);
            assertEquals(new UnsignedInteger(2), bo.get(PropertyIdentifier.presentValue));
            assertReliability(bo, Reliability.configurationError);

            // Relinquishing priority 8 makes the out of range command the Present_Value.
            command(bo, null, 8);
            assertEquals(new UnsignedInteger(7), bo.get(PropertyIdentifier.presentValue));
            assertReliability(bo, Reliability.multiStateOutOfRange);

            // Relinquishing priority 12 too leaves nothing out of range.
            command(bo, null, 12);
            assertReliability(bo, Reliability.noFaultDetected);
        }
    }

    /**
     * 12.19.11: a Feedback_Value left out of range is reported as CONFIGURATION_ERROR.
     */
    @Test
    public void feedbackValueOutOfRange() throws Exception {
        d1.addObject(new NotificationClassObject(d1, 17, "nc17", 100, 5, 200,
                new EventTransitionBits(false, false, false)));
        MultistateOutputObject mo = mo();
        mo.supportIntrinsicReporting(5, 17, 6, new EventTransitionBits(true, true, true), NotifyType.alarm, 12);

        command(mo, 2, 8);
        assertReliability(mo, Reliability.noFaultDetected);

        writeNumberOfStates(mo, 4);
        assertEquals(new UnsignedInteger(2), mo.get(PropertyIdentifier.presentValue));
        assertReliability(mo, Reliability.configurationError);

        mo.writePropertyInternal(PropertyIdentifier.feedbackValue, new UnsignedInteger(2));
        assertReliability(mo, Reliability.noFaultDetected);
    }

    /**
     * 12.20.10: Alarm_Values and Fault_Values left out of range are reported as CONFIGURATION_ERROR.
     */
    @Test
    public void alarmAndFaultValuesOutOfRange() throws Exception {
        d1.addObject(new NotificationClassObject(d1, 17, "nc17", 100, 5, 200,
                new EventTransitionBits(false, false, false)));
        MultistateValueObject mv = mv();
        mv.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(6)),
                new BACnetArray<>(new UnsignedInteger(3)), new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);

        command(mv, 2, 8);
        assertReliability(mv, Reliability.noFaultDetected);

        writeNumberOfStates(mv, 4);
        assertReliability(mv, Reliability.configurationError);

        mv.writePropertyInternal(PropertyIdentifier.alarmValues, new BACnetArray<>(new UnsignedInteger(4)));
        assertReliability(mv, Reliability.noFaultDetected);
    }

    /**
     * 12.19.11, 12.20.10: none of the range conditions are reported while the object is out of service.
     */
    @Test
    public void outOfServiceSuppressesReporting() throws Exception {
        for (BACnetObject bo : new BACnetObject[] {mo(), mv()}) {
            command(bo, 6, 8);
            bo.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.outOfService, Boolean.TRUE));

            writeNumberOfStates(bo, 4);
            assertReliability(bo, Reliability.noFaultDetected);

            // Returning to service reports the situation, which has not changed.
            bo.writeProperty(COMMANDER, new PropertyValue(PropertyIdentifier.outOfService, Boolean.FALSE));
            assertReliability(bo, Reliability.multiStateOutOfRange);
        }
    }

    /**
     * 13.2.2.2: a fault detected by the first stage of reliability-evaluation takes precedence over one detected
     * by a fault algorithm, which is the second stage.
     */
    @Test
    public void firstStageDisplacesTheFaultAlgorithm() throws Exception {
        d1.addObject(new NotificationClassObject(d1, 17, "nc17", 100, 5, 200,
                new EventTransitionBits(false, false, false)));
        MultistateValueObject mv = mv();
        mv.supportIntrinsicReporting(5, 17, new BACnetArray<>(new UnsignedInteger(2)),
                new BACnetArray<>(new UnsignedInteger(6)), new EventTransitionBits(true, true, true),
                NotifyType.alarm, 12);

        // The FAULT_STATE algorithm reports its own reliability for a Present_Value in Fault_Values.
        command(mv, 6, 8);
        assertReliability(mv, Reliability.multiStateFault);

        // Present_Value is now also out of range, which the first stage reports in its place.
        writeNumberOfStates(mv, 4);
        assertReliability(mv, Reliability.multiStateOutOfRange);
    }

    /**
     * A CONFIGURATION_ERROR from another source is itself required by a first stage condition, and the standard
     * does not rank one first stage condition against another, so it is left in place.
     */
    @Test
    public void configurationErrorFromAnotherSourceIsNotDisplaced() throws Exception {
        for (BACnetObject bo : new BACnetObject[] {mo(), mv()}) {
            command(bo, 6, 8);
            bo.writePropertyInternal(PropertyIdentifier.reliability, Reliability.configurationError);

            writeNumberOfStates(bo, 4);
            assertReliability(bo, Reliability.configurationError);

            // Once the other source clears its fault, the situation here is reported.
            bo.writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
            assertReliability(bo, Reliability.multiStateOutOfRange);
        }
    }

    /**
     * 12.19.11, 12.20.10: the conditions evaluated here are reported "as long as this situation remains", so a
     * reliability that no clause requires does not stop them being reported.
     */
    @Test
    public void unrequiredFaultDoesNotSuppressReporting() throws Exception {
        for (BACnetObject bo : new BACnetObject[] {mo(), mv()}) {
            command(bo, 6, 8);
            bo.writePropertyInternal(PropertyIdentifier.reliability, Reliability.unreliableOther);

            writeNumberOfStates(bo, 4);
            assertReliability(bo, Reliability.multiStateOutOfRange);
        }
    }
}
