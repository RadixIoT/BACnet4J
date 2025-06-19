package com.serotonin.bacnet4j.obj.mixin;

import static com.serotonin.bacnet4j.TestUtils.assertBACnetServiceException;
import static com.serotonin.bacnet4j.TestUtils.awaitEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.AnalogValueObject;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.BinaryOutputObject;
import com.serotonin.bacnet4j.obj.BinaryValueObject;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectReference;
import com.serotonin.bacnet4j.type.constructed.OptionalUnsigned;
import com.serotonin.bacnet4j.type.constructed.PriorityArray;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.Polarity;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class CommandableMixinTest extends AbstractTest {
    @Test
    public void avNotCommandableNotValueSource() throws BACnetServiceException {
        final AnalogValueObject av = new AnalogValueObject(d1, 0, "av0", 0, EngineeringUnits.noUnits, false);
        final ValueSource valueSource = createValueSource(12);

        assertEquals(new Real(0), av.get(PropertyIdentifier.presentValue));
        assertFalse(av.isOverridden());
        assertNull(av.get(PropertyIdentifier.priorityArray));
        assertNull(av.get(PropertyIdentifier.relinquishDefault));
        assertNull(av.get(PropertyIdentifier.currentCommandPriority));
        assertNull(av.get(PropertyIdentifier.valueSource));
        assertNull(av.get(PropertyIdentifier.valueSourceArray));
        assertNull(av.get(PropertyIdentifier.lastCommandTime));
        assertNull(av.get(PropertyIdentifier.commandTimeArray));

        // The write will fail because the object is not commandable.
        assertBACnetServiceException(
                () -> av.writeProperty(valueSource, new PropertyValue(PropertyIdentifier.presentValue, new Real(1))), //
                ErrorClass.property, ErrorCode.writeAccessDenied);

        // Change the object to out of service. Now, writes should work.
        av.writeProperty(valueSource, new PropertyValue(PropertyIdentifier.outOfService, Boolean.TRUE));
        av.writeProperty(valueSource, new PropertyValue(PropertyIdentifier.presentValue, new Real(1)));

        assertEquals(new Real(1), av.get(PropertyIdentifier.presentValue));
        assertFalse(av.isOverridden());
        assertNull(av.get(PropertyIdentifier.priorityArray));
        assertNull(av.get(PropertyIdentifier.relinquishDefault));
        assertNull(av.get(PropertyIdentifier.currentCommandPriority));
        assertNull(av.get(PropertyIdentifier.valueSource));
        assertNull(av.get(PropertyIdentifier.valueSourceArray));
        assertNull(av.get(PropertyIdentifier.lastCommandTime));
        assertNull(av.get(PropertyIdentifier.commandTimeArray));

        // Change the object to overridden. Now writes will fail because of that, but with the same error as above.
        av.setOverridden(true);
        assertBACnetServiceException(
                () -> av.writeProperty(valueSource, new PropertyValue(PropertyIdentifier.presentValue, new Real(1))), //
                ErrorClass.property, ErrorCode.writeAccessDenied);

        assertTrue(av.isOverridden());
        assertFalse(av.supportsCommandable());
        assertFalse(av.supportsValueSource());
    }

    @Test
    public void bvCommandableNotValueSource() throws Exception {
        final BinaryValueObject bv =
                new BinaryValueObject(d1, 0, "bv0", BinaryPV.inactive, false).supportCommandable(BinaryPV.inactive);

        // Assert default values.
        assertEquals(BinaryPV.inactive, bv.get(PropertyIdentifier.presentValue));
        assertNotNull(bv.get(PropertyIdentifier.priorityArray));
        assertEquals(BinaryPV.inactive, bv.get(PropertyIdentifier.relinquishDefault));
        assertEquals(new OptionalUnsigned(), bv.get(PropertyIdentifier.currentCommandPriority));
        assertNull(bv.get(PropertyIdentifier.valueSource));
        assertNull(bv.get(PropertyIdentifier.valueSourceArray));
        assertNull(bv.get(PropertyIdentifier.lastCommandTime));
        assertNull(bv.get(PropertyIdentifier.commandTimeArray));

        // Write a new PV at priority 12. Ensure it is the new value.
        bv.writeProperty(createValueSource(12),
                new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.active, new UnsignedInteger(12)));
        assertEquals(BinaryPV.active, bv.get(PropertyIdentifier.presentValue));
        assertEquals(new PriorityArray().put(12, BinaryPV.active), bv.get(PropertyIdentifier.priorityArray));
        assertEquals(new OptionalUnsigned(12), bv.get(PropertyIdentifier.currentCommandPriority));

        // Write a new PV at priority 13. Ensure that 12 is still active.
        bv.writeProperty(createValueSource(13),
                new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.inactive, new UnsignedInteger(13)));
        assertEquals(BinaryPV.active, bv.get(PropertyIdentifier.presentValue));
        assertEquals(new PriorityArray().put(12, BinaryPV.active).put(13, BinaryPV.inactive),
                bv.get(PropertyIdentifier.priorityArray));
        assertEquals(new OptionalUnsigned(12), bv.get(PropertyIdentifier.currentCommandPriority));

        // Write a new PV at priority 10. Ensure that it is active.
        bv.writeProperty(createValueSource(10),
                new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.inactive, new UnsignedInteger(10)));
        assertEquals(BinaryPV.inactive, bv.get(PropertyIdentifier.presentValue));
        assertEquals(new PriorityArray().put(10, BinaryPV.inactive).put(12, BinaryPV.active).put(13, BinaryPV.inactive),
                bv.get(PropertyIdentifier.priorityArray));
        assertEquals(new OptionalUnsigned(10), bv.get(PropertyIdentifier.currentCommandPriority));

        // Remove the PV at 12. Ensure that 10 is still active.
        bv.writeProperty(createValueSource(12),
                new PropertyValue(PropertyIdentifier.presentValue, null, Null.instance, new UnsignedInteger(12)));
        assertEquals(BinaryPV.inactive, bv.get(PropertyIdentifier.presentValue));
        assertEquals(new PriorityArray().put(10, BinaryPV.inactive).put(13, BinaryPV.inactive),
                bv.get(PropertyIdentifier.priorityArray));
        assertEquals(new OptionalUnsigned(10), bv.get(PropertyIdentifier.currentCommandPriority));

        // Remove the PV at 10. Ensure that 13 becomes active.
        bv.writeProperty(createValueSource(10),
                new PropertyValue(PropertyIdentifier.presentValue, null, Null.instance, new UnsignedInteger(10)));
        assertEquals(BinaryPV.inactive, bv.get(PropertyIdentifier.presentValue));
        assertEquals(new PriorityArray().put(13, BinaryPV.inactive), bv.get(PropertyIdentifier.priorityArray));
        assertEquals(new OptionalUnsigned(13), bv.get(PropertyIdentifier.currentCommandPriority));

        // Remove the PV at 13. Ensure that the relinquish default becomes active.
        bv.writeProperty(createValueSource(13),
                new PropertyValue(PropertyIdentifier.presentValue, null, Null.instance, new UnsignedInteger(13)));
        assertEquals(BinaryPV.inactive, bv.get(PropertyIdentifier.presentValue));
        assertEquals(new PriorityArray(), bv.get(PropertyIdentifier.priorityArray));
        assertEquals(new OptionalUnsigned(), bv.get(PropertyIdentifier.currentCommandPriority));

        // Ensure that the value source fields are still null.
        assertNull(bv.get(PropertyIdentifier.valueSource));
        assertNull(bv.get(PropertyIdentifier.valueSourceArray));
        assertNull(bv.get(PropertyIdentifier.lastCommandTime));
        assertNull(bv.get(PropertyIdentifier.commandTimeArray));

        assertTrue(bv.supportsCommandable());
        assertFalse(bv.supportsValueSource());
    }

    @Test
    public void bvNotCommandableValueSource() throws Exception {
        final BinaryValueObject bv = new BinaryValueObject(d1, 0, "bv0", BinaryPV.inactive, false).supportValueSource();
        bv.writeProperty(null, new PropertyValue(PropertyIdentifier.outOfService, Boolean.TRUE));

        // Assert default values.
        assertEquals(BinaryPV.inactive, bv.get(PropertyIdentifier.presentValue));
        assertNull(bv.get(PropertyIdentifier.priorityArray));
        assertNull(bv.get(PropertyIdentifier.relinquishDefault));
        assertNull(bv.get(PropertyIdentifier.currentCommandPriority));
        assertEquals(createLocalValueSource(bv), bv.get(PropertyIdentifier.valueSource));
        assertNull(bv.get(PropertyIdentifier.valueSourceArray));
        assertNull(bv.get(PropertyIdentifier.lastCommandTime));
        assertNull(bv.get(PropertyIdentifier.commandTimeArray));

        // Set the PV with a value source.
        bv.writeProperty(createValueSource(1), PropertyIdentifier.presentValue, BinaryPV.active);
        assertEquals(BinaryPV.active, bv.get(PropertyIdentifier.presentValue));
        assertEquals(createValueSource(1), bv.get(PropertyIdentifier.valueSource));

        // Set the PV with a different value source.
        bv.writeProperty(createValueSource(2), PropertyIdentifier.presentValue, BinaryPV.active);
        assertEquals(BinaryPV.active, bv.get(PropertyIdentifier.presentValue));
        assertEquals(createValueSource(2), bv.get(PropertyIdentifier.valueSource));

        final ValueSource vs = new ValueSource(new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 123),
                new ObjectIdentifier(ObjectType.group, 124)));
        // Try to set the value directly but with an invalid source.
        assertBACnetServiceException(() -> bv.writeProperty(createValueSource(3),
                        new PropertyValue(PropertyIdentifier.valueSource, null, vs, null)), ErrorClass.property,
                ErrorCode.writeAccessDenied);

        // Set the value source directly
        bv.writeProperty(createValueSource(2), PropertyIdentifier.valueSource, vs);
        assertEquals(BinaryPV.active, bv.get(PropertyIdentifier.presentValue));
        assertEquals(vs, bv.get(PropertyIdentifier.valueSource));

        assertFalse(bv.supportsCommandable());
        assertTrue(bv.supportsValueSource());
    }

    @Test
    public void bvCommandableValueSource() throws Exception {
        final BinaryValueObject bv =
                new BinaryValueObject(d1, 0, "bv0", BinaryPV.inactive, false).supportCommandable(BinaryPV.inactive)
                        .supportValueSource();

        // Assert default values.
        assertEquals(BinaryPV.inactive, bv.get(PropertyIdentifier.presentValue));
        assertNotNull(bv.get(PropertyIdentifier.priorityArray));
        assertEquals(BinaryPV.inactive, bv.get(PropertyIdentifier.relinquishDefault));
        assertEquals(new OptionalUnsigned(), bv.get(PropertyIdentifier.currentCommandPriority));
        assertEquals(createLocalValueSource(bv), bv.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources(), bv.get(PropertyIdentifier.valueSourceArray));
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), bv.get(PropertyIdentifier.lastCommandTime), 1);
        assertEquals(emptyCommandTimes(), bv.get(PropertyIdentifier.commandTimeArray));

        // Write a new PV at priority 12.
        bv.writeProperty(createValueSource(12),
                new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.active, new UnsignedInteger(12)));
        assertEquals(createValueSource(12), bv.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(12, createValueSource(12)),
                bv.get(PropertyIdentifier.valueSourceArray));
        BACnetArray<TimeStamp> cta = bv.get(PropertyIdentifier.commandTimeArray);
        TimeStamp time12 = cta.getBase1(12);
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), time12, 1);
        assertEquals(time12, bv.get(PropertyIdentifier.lastCommandTime));
        assertEquals(emptyCommandTimes().putBase1(12, time12), bv.get(PropertyIdentifier.commandTimeArray));

        // Write a new PV at priority 13.
        bv.writeProperty(createValueSource(13),
                new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.inactive, new UnsignedInteger(13)));
        assertEquals(createValueSource(12), bv.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(12, createValueSource(12)).putBase1(13, createValueSource(13)),
                bv.get(PropertyIdentifier.valueSourceArray));
        cta = bv.get(PropertyIdentifier.commandTimeArray);
        TimeStamp time13 = cta.getBase1(13);
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), time13, 1);
        assertEquals(time12, bv.get(PropertyIdentifier.lastCommandTime));
        assertEquals(emptyCommandTimes().putBase1(12, time12).putBase1(13, time13),
                bv.get(PropertyIdentifier.commandTimeArray));

        // Write a new PV at priority 10. Ensure that it is active.
        bv.writeProperty(createValueSource(10),
                new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.inactive, new UnsignedInteger(10)));
        assertEquals(createValueSource(10), bv.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(10, createValueSource(10)).putBase1(12, createValueSource(12))
                .putBase1(13, createValueSource(13)), bv.get(PropertyIdentifier.valueSourceArray));
        cta = bv.get(PropertyIdentifier.commandTimeArray);
        TimeStamp time10 = cta.getBase1(10);
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), time10, 1);
        assertEquals(time10, bv.get(PropertyIdentifier.lastCommandTime));
        assertEquals(emptyCommandTimes().putBase1(10, time10).putBase1(12, time12).putBase1(13, time13),
                bv.get(PropertyIdentifier.commandTimeArray));

        // Update the value source at priority 10
        final ValueSource vs10 = new ValueSource(new DeviceObjectReference(new ObjectIdentifier(ObjectType.device, 123),
                new ObjectIdentifier(ObjectType.group, 124)));
        bv.writeProperty(createValueSource(10), PropertyIdentifier.valueSource, vs10);

        // Remove the PV at 12. Ensure that 10 is still active.
        bv.writeProperty(createValueSource(12),
                new PropertyValue(PropertyIdentifier.presentValue, null, Null.instance, new UnsignedInteger(12)));
        assertEquals(createValueSource(10), bv.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(10, createValueSource(10)).putBase1(12, createValueSource(12))
                .putBase1(13, createValueSource(13)), bv.get(PropertyIdentifier.valueSourceArray));
        cta = bv.get(PropertyIdentifier.commandTimeArray);
        time12 = cta.getBase1(12);
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), time12, 1);
        assertEquals(time10, bv.get(PropertyIdentifier.lastCommandTime));
        assertEquals(emptyCommandTimes().putBase1(10, time10).putBase1(12, time12).putBase1(13, time13),
                bv.get(PropertyIdentifier.commandTimeArray));

        // Remove the PV at 10. Ensure that 13 becomes active.
        bv.writeProperty(createValueSource(10),
                new PropertyValue(PropertyIdentifier.presentValue, null, Null.instance, new UnsignedInteger(10)));
        assertEquals(createValueSource(13), bv.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(10, createValueSource(10)).putBase1(12, createValueSource(12))
                .putBase1(13, createValueSource(13)), bv.get(PropertyIdentifier.valueSourceArray));
        cta = bv.get(PropertyIdentifier.commandTimeArray);
        time10 = cta.getBase1(10);
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), time10, 1);
        assertEquals(time10, bv.get(PropertyIdentifier.lastCommandTime)); // See last paragraph of 19.5.1.4.
        assertEquals(emptyCommandTimes().putBase1(10, time10).putBase1(12, time12).putBase1(13, time13),
                bv.get(PropertyIdentifier.commandTimeArray));

        // Remove the PV at 13. Ensure that the relinquish default becomes active.
        bv.writeProperty(createValueSource(13),
                new PropertyValue(PropertyIdentifier.presentValue, null, Null.instance, new UnsignedInteger(13)));
        assertEquals(createLocalValueSource(bv), bv.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(10, createValueSource(10)).putBase1(12, createValueSource(12))
                .putBase1(13, createValueSource(13)), bv.get(PropertyIdentifier.valueSourceArray));
        time13 = bv.get(PropertyIdentifier.lastCommandTime);
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), time13, 1);
        assertEquals(emptyCommandTimes().putBase1(10, time10).putBase1(12, time12).putBase1(13, time13),
                bv.get(PropertyIdentifier.commandTimeArray));

        assertTrue(bv.supportsCommandable());
        assertTrue(bv.supportsValueSource());
    }

    @Test
    public void boMinOnOffTime() throws Exception {
        final BinaryOutputObject bo =
                new BinaryOutputObject(d1, 0, "bo0", BinaryPV.inactive, false, Polarity.normal, BinaryPV.inactive);

        // Assert default values.
        assertEquals(BinaryPV.inactive, bo.get(PropertyIdentifier.presentValue));
        assertNotNull(bo.get(PropertyIdentifier.priorityArray));
        assertEquals(BinaryPV.inactive, bo.get(PropertyIdentifier.relinquishDefault));
        assertEquals(new OptionalUnsigned(), bo.get(PropertyIdentifier.currentCommandPriority));
        assertEquals(createLocalValueSource(bo), bo.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources(), bo.get(PropertyIdentifier.valueSourceArray));
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), bo.get(PropertyIdentifier.lastCommandTime), 1);
        assertEquals(emptyCommandTimes(), bo.get(PropertyIdentifier.commandTimeArray));

        // Try to write to priority 0, which will fail.
        assertBACnetServiceException(() -> bo.writeProperty(createValueSource(0),
                        new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.active, UnsignedInteger.ZERO)),
                ErrorClass.property, ErrorCode.invalidArrayIndex);

        // Try to write to priority 17, which will fail.
        assertBACnetServiceException(() -> bo.writeProperty(createValueSource(17),
                        new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.active, new UnsignedInteger(17))),
                ErrorClass.property, ErrorCode.invalidArrayIndex);

        // Try to write to priority 6, which will fail.
        assertBACnetServiceException(() -> bo.writeProperty(createValueSource(6),
                        new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.active, new UnsignedInteger(6))),
                ErrorClass.property, ErrorCode.writeAccessDenied);

        // Enable min off/on times.
        bo.writePropertyInternal(PropertyIdentifier.minimumOffTime, new Unsigned32(4));
        bo.writePropertyInternal(PropertyIdentifier.minimumOnTime, new Unsigned32(2));

        // Validate properties
        assertEquals(createLocalValueSource(bo), bo.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(6, createLocalValueSource(bo)),
                bo.get(PropertyIdentifier.valueSourceArray));
        assertEquals(emptyCommandTimes(), bo.get(PropertyIdentifier.commandTimeArray));

        // Write a new PV at priority 8 so that the PV changes.
        bo.writeProperty(createValueSource(8),
                new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.active, new UnsignedInteger(8)));
        assertEquals(BinaryPV.active, bo.get(PropertyIdentifier.presentValue));
        assertEquals(createLocalValueSource(bo), bo.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(6, createLocalValueSource(bo)).putBase1(8, createValueSource(8)),
                bo.get(PropertyIdentifier.valueSourceArray));
        final TimeStamp time6 = bo.get(PropertyIdentifier.lastCommandTime);
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), time6, 1);
        assertEquals(emptyCommandTimes().putBase1(6, time6).putBase1(8, time6),
                bo.get(PropertyIdentifier.commandTimeArray));

        // Remove the value at 8, and ensure that the PV is still active.
        bo.writeProperty(createValueSource(8),
                new PropertyValue(PropertyIdentifier.presentValue, null, Null.instance, new UnsignedInteger(8)));
        assertEquals(BinaryPV.active, bo.get(PropertyIdentifier.presentValue));
        assertEquals(createLocalValueSource(bo), bo.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(6, createLocalValueSource(bo)).putBase1(8, createValueSource(8)),
                bo.get(PropertyIdentifier.valueSourceArray));
        final BACnetArray<TimeStamp> cta = bo.get(PropertyIdentifier.commandTimeArray);
        final TimeStamp time8 = cta.getBase1(8);
        TestUtils.assertEquals(new TimeStamp(new DateTime(d1)), time8, 1);
        assertEquals(time6, bo.get(PropertyIdentifier.lastCommandTime));
        assertEquals(emptyCommandTimes().putBase1(6, time6).putBase1(8, time8),
                bo.get(PropertyIdentifier.commandTimeArray));

        // Wait for the timer to complete, and ensure that the PV is not inactive.
        clock.plusSeconds(2);
        awaitEquals(BinaryPV.inactive, () -> bo.get(PropertyIdentifier.presentValue));
        assertEquals(createLocalValueSource(bo), bo.get(PropertyIdentifier.valueSource));
        assertEquals(emptyValueSources().putBase1(6, createLocalValueSource(bo)).putBase1(8, createValueSource(8)),
                bo.get(PropertyIdentifier.valueSourceArray));
        final TimeStamp timeComplete = bo.get(PropertyIdentifier.lastCommandTime);
        final GregorianCalendar gc = time6.getDateTime().getGC();
        gc.add(Calendar.SECOND, 2);
        final TimeStamp time6Plus2s = new TimeStamp(new DateTime(gc));
        TestUtils.assertEquals(time6Plus2s, timeComplete, 2);
        assertEquals(emptyCommandTimes().putBase1(6, timeComplete).putBase1(8, time8),
                bo.get(PropertyIdentifier.commandTimeArray));
    }

    private static ValueSource createValueSource(final int address) {
        return new ValueSource(new Address(new byte[] {(byte) address}));
    }

    private ValueSource createLocalValueSource(final BACnetObject bo) {
        return new ValueSource(new DeviceObjectReference(d1.getId(), bo.getId()));
    }

    private static BACnetArray<ValueSource> emptyValueSources() {
        return new BACnetArray<>(16, new ValueSource());
    }

    private static BACnetArray<TimeStamp> emptyCommandTimes() {
        return new BACnetArray<>(16, TimeStamp.UNSPECIFIED_TIME);
    }

    @Test
    public void writable() throws BACnetServiceException {
        final BinaryValueObject bv = new BinaryValueObject(d1, 0, "bv", BinaryPV.inactive, false).supportWritable();

        // Write with priority not allowed.
        assertBACnetServiceException(() -> bv.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.active, new UnsignedInteger(12))),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        assertEquals(BinaryPV.inactive, bv.readProperty(PropertyIdentifier.presentValue));

        // Write without priority not allowed.
        bv.writeProperty(null, PropertyIdentifier.presentValue, BinaryPV.active);
        assertEquals(BinaryPV.active, bv.readProperty(PropertyIdentifier.presentValue));
    }

    @Test
    public void notWritable() throws BACnetServiceException {
        final BinaryValueObject bv = new BinaryValueObject(d1, 0, "bv", BinaryPV.inactive, false);

        // Write with priority not allowed.
        assertBACnetServiceException(() -> bv.writeProperty(null,
                        new PropertyValue(PropertyIdentifier.presentValue, null, BinaryPV.active, new UnsignedInteger(12))),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        assertEquals(BinaryPV.inactive, bv.readProperty(PropertyIdentifier.presentValue));

        // Write without priority also not allowed.
        assertBACnetServiceException(() -> bv.writeProperty(null, PropertyIdentifier.presentValue, BinaryPV.active),
                ErrorClass.property, ErrorCode.writeAccessDenied);
        assertEquals(BinaryPV.inactive, bv.readProperty(PropertyIdentifier.presentValue));
    }
}
