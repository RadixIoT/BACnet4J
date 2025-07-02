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

package com.serotonin.bacnet4j.service.confirmed;

import static com.serotonin.bacnet4j.TestUtils.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.AnalogInputObject;
import com.serotonin.bacnet4j.obj.TrendLogMultipleObject;
import com.serotonin.bacnet4j.obj.logBuffer.LinkedListLogBuffer;
import com.serotonin.bacnet4j.service.acknowledgement.ReadRangeAck;
import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest.ByPosition;
import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest.BySequenceNumber;
import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest.ByTime;
import com.serotonin.bacnet4j.service.confirmed.ReadRangeRequest.Sequenced;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.LogData;
import com.serotonin.bacnet4j.type.constructed.LogData.LogDataElement;
import com.serotonin.bacnet4j.type.constructed.LogMultipleRecord;
import com.serotonin.bacnet4j.type.constructed.LogRecord;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.ResultFlags;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

import lohbihler.warp.WarpClock;

public class ReadRangeRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    final PropertyIdentifier pid = PropertyIdentifier.forId(9999);
    private final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
    final DateTime now = new DateTime(d1);

    @Before
    public void before() throws Exception {
        d1.initialize();
    }

    @After
    public void after() throws Exception {
        d1.terminate();
    }

    /**
     * Ensure that the service can be accessed via the network.
     */
    @Test
    public void networkRead() throws Exception {
        final SequenceOf<Recipient> recipients =
                new SequenceOf<>(new Recipient(new ObjectIdentifier(ObjectType.device, 1)),
                        new Recipient(new ObjectIdentifier(ObjectType.device, 2)),
                        new Recipient(new ObjectIdentifier(ObjectType.device, 3)),
                        new Recipient(new ObjectIdentifier(ObjectType.device, 4)),
                        new Recipient(new ObjectIdentifier(ObjectType.device, 5)),
                        new Recipient(new ObjectIdentifier(ObjectType.device, 6)));
        d1.getObject(d1.getId()).writePropertyInternal(PropertyIdentifier.restartNotificationRecipients, recipients);

        final LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 0))).initialize();

        final RemoteDevice rd1 = d2.getRemoteDeviceBlocking(1);
        final ReadRangeAck ack =
                d2.send(rd1, new ReadRangeRequest(d1.getId(), PropertyIdentifier.restartNotificationRecipients, null))
                        .get();

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(PropertyIdentifier.restartNotificationRecipients, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(true, true, false), ack.getResultFlags());
        assertEquals(new UnsignedInteger(6), ack.getItemCount());
        assertEquals(recipients, ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());

        d2.terminate();
    }

    /**
     * Read data from a TrendLogMultiple object
     */
    @Test
    public void trendLogMultiple() throws Exception {
        final WarpClock clock = new WarpClock();

        final LocalDevice d11 =
                new LocalDevice(11, new DefaultTransport(new TestNetwork(map, 11, 0))).withClock(clock).initialize();
        final AnalogInputObject ai = new AnalogInputObject(d11, 0, "ai", 12, EngineeringUnits.noUnits, false);

        final LocalDevice d12 =
                new LocalDevice(12, new DefaultTransport(new TestNetwork(map, 12, 0))).withClock(clock).initialize();
        final TrendLogMultipleObject tl =
                new TrendLogMultipleObject(d12, 0, "tlm", new LinkedListLogBuffer<>(), true, DateTime.UNSPECIFIED,
                        DateTime.UNSPECIFIED, new BACnetArray<>(
                        new DeviceObjectPropertyReference(11, ai.getId(), PropertyIdentifier.presentValue)), 0, false,
                        100);

        final RemoteDevice rd12 = d11.getRemoteDeviceBlocking(12);
        final DateTime now = new DateTime(clock.millis());

        // Trigger the trend log a few times.
        doTriggers(tl, 11);

        // Read the buffer.
        final ReadRangeAck ack =
                d11.send(rd12, new ReadRangeRequest(tl.getId(), PropertyIdentifier.logBuffer, null)).get();

        assertEquals(tl.getId(), ack.getObjectIdentifier());
        assertEquals(PropertyIdentifier.logBuffer, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(true, true, false), ack.getResultFlags());
        assertEquals(new UnsignedInteger(11), ack.getItemCount());
        assertEquals(new SequenceOf<>( //
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12))))),
                        new LogMultipleRecord(now, new LogData(new SequenceOf<>(new LogDataElement(new Real(12)))))),
                ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    private static void doTriggers(final TrendLogMultipleObject tl, final int count) throws Exception {
        int remaining = count;
        while (remaining > 0) {
            await(tl::trigger);
            remaining--;
        }
        await(() -> !((Boolean) tl.get(PropertyIdentifier.trigger)).booleanValue());
    }

    /**
     * 15.8.1.1.4.1.3
     *
     * @throws BACnetException
     */
    @Test
    public void positionPositiveCount() throws BACnetException {
        SequenceOf<UnsignedInteger> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(new UnsignedInteger(i + 1));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new ByPosition(800, 300)).handle(d1, null);

        data = new SequenceOf<>(200);
        for (int i = 0; i < 200; i++)
            data.add(new UnsignedInteger(i + 800));

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, true), ack.getResultFlags());
        assertEquals(new UnsignedInteger(200), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    /**
     * 15.8.1.1.4.1.4
     *
     * @throws BACnetException
     */
    @Test
    public void positionNegativeCount() throws BACnetException {
        SequenceOf<UnsignedInteger> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(new UnsignedInteger(i + 1));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new ByPosition(1000, -1000)).handle(d1,
                        null);

        data = new SequenceOf<>(200);
        for (int i = 0; i < 200; i++)
            data.add(new UnsignedInteger(i + 801));

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, true, true), ack.getResultFlags());
        assertEquals(new UnsignedInteger(200), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    /**
     * 15.8.1.1.4.2.3
     *
     * @throws BACnetException
     */
    @Test
    public void sequencePositiveCount() throws BACnetException {
        SequenceOf<LogRecord> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(createLogRecord(now, 2001 + i));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new BySequenceNumber(2800, 300)).handle(d1,
                        null);

        data = new SequenceOf<>(200);
        for (int i = 0; i < 200; i++)
            data.add(createLogRecord(now, i + 2800));

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, true), ack.getResultFlags());
        assertEquals(new UnsignedInteger(200), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertEquals(new UnsignedInteger(2800), ack.getFirstSequenceNumber());
    }

    /**
     * 15.8.1.1.4.2.4
     *
     * @throws BACnetException
     */
    @Test
    public void sequenceNegativeCount() throws BACnetException {
        SequenceOf<LogRecord> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(createLogRecord(now, 2001 + i));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new BySequenceNumber(3000, -1000)).handle(d1,
                        null);

        data = new SequenceOf<>(200);
        for (int i = 0; i < 200; i++)
            data.add(createLogRecord(now, i + 2801));

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, true, true), ack.getResultFlags());
        assertEquals(new UnsignedInteger(200), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertEquals(new UnsignedInteger(2801), ack.getFirstSequenceNumber());
    }

    /**
     * 15.8.1.1.4.3.3
     *
     * @throws BACnetException
     */
    @Test
    public void timePositiveCount() throws BACnetException {
        SequenceOf<LogRecord> data = new SequenceOf<>(1000);
        final GregorianCalendar gc = new GregorianCalendar(2013, Calendar.MARCH, 18, 1, 1);
        for (int i = 0; i < 1000; i++) {
            data.add(createLogRecord(new DateTime(gc), i + 2001));
            gc.add(Calendar.MINUTE, 1);
        }
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack = (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null,
                new ByTime(new DateTime(new Date(2013, Month.MARCH, 18, null), new Time(13, 59, 0, 0)), 300)).handle(d1,
                null);

        data = new SequenceOf<>(200);
        gc.set(2013, Calendar.MARCH, 18, 14, 0, 0);
        for (int i = 0; i < 200; i++) {
            data.add(createLogRecord(new DateTime(gc), i + 2780));
            gc.add(Calendar.MINUTE, 1);
        }

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, true), ack.getResultFlags());
        assertEquals(new UnsignedInteger(200), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertEquals(new UnsignedInteger(2780), ack.getFirstSequenceNumber());
    }

    /**
     * 15.8.1.1.4.3.4
     *
     * @throws BACnetException
     */
    @Test
    public void timePositiveOutdatedCount() throws BACnetException {
        SequenceOf<LogRecord> data = new SequenceOf<>(1000);
        final GregorianCalendar gc = new GregorianCalendar(2013, Calendar.MARCH, 18, 1, 1);
        for (int i = 0; i < 1000; i++) {
            data.add(createLogRecord(new DateTime(gc), i + 2001));
            gc.add(Calendar.MINUTE, 1);
        }
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack = (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null,
                new ByTime(new DateTime(new Date(1991, Month.NOVEMBER, 17, null), new Time(19, 20, 0, 0)), 300)).handle(
                d1, null);

        data = new SequenceOf<>(200);
        gc.set(2013, Calendar.MARCH, 18, 1, 1, 0);
        for (int i = 0; i < 200; i++) {
            data.add(createLogRecord(new DateTime(gc), i + 2001));
            gc.add(Calendar.MINUTE, 1);
        }

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(true, false, true), ack.getResultFlags());
        assertEquals(new UnsignedInteger(200), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertEquals(new UnsignedInteger(2001), ack.getFirstSequenceNumber());
    }

    /**
     * 15.8.1.1.4.3.5
     *
     * @throws BACnetException
     */
    @Test
    public void timeNegativeCount() throws BACnetException {
        SequenceOf<LogRecord> data = new SequenceOf<>(1000);
        final GregorianCalendar gc = new GregorianCalendar(2013, Calendar.MARCH, 18, 1, 1);
        for (int i = 0; i < 1000; i++) {
            data.add(createLogRecord(new DateTime(gc), i + 2001));
            gc.add(Calendar.MINUTE, 1);
        }
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack = (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null,
                new ByTime(new DateTime(new Date(2013, Month.MARCH, 18, null), new Time(17, 40, 0, 0)), -1000)).handle(
                d1, null);

        data = new SequenceOf<>(200);
        gc.set(2013, Calendar.MARCH, 18, 14, 20, 0);
        for (int i = 0; i < 200; i++) {
            data.add(createLogRecord(new DateTime(gc), i + 2800));
            gc.add(Calendar.MINUTE, 1);
        }

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, true), ack.getResultFlags());
        assertEquals(new UnsignedInteger(200), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertEquals(new UnsignedInteger(2800), ack.getFirstSequenceNumber());
    }

    @Test
    public void validations() {
        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(new ObjectIdentifier(ObjectType.accessCredential, 0), pid, null).handle(d1,
                        null), ErrorClass.object, ErrorCode.unknownObject);

        TestUtils.assertRequestHandleException(() -> new ReadRangeRequest(d1.getId(), pid, null).handle(d1, null),
                ErrorClass.property, ErrorCode.unknownProperty);

        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), PropertyIdentifier.all, null).handle(d1, null),
                ErrorClass.services, ErrorCode.parameterOutOfRange);

        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), pid, UnsignedInteger.ZERO).handle(d1, null), ErrorClass.services,
                ErrorCode.parameterOutOfRange);

        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), pid, new UnsignedInteger(1), new ByPosition(1, 0)).handle(d1,
                        null), ErrorClass.services, ErrorCode.parameterOutOfRange);

        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), PropertyIdentifier.logBuffer, new UnsignedInteger(1)).handle(d1,
                        null), ErrorClass.property, ErrorCode.unknownProperty);

        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), PropertyIdentifier.logBuffer, null).handle(d1, null),
                ErrorClass.property, ErrorCode.unknownProperty);
    }

    @Test
    public void moreValidations() {
        d1.writePropertyInternal(PropertyIdentifier.logBuffer, UnsignedInteger.ZERO);
        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), PropertyIdentifier.logBuffer, null).handle(d1, null),
                ErrorClass.services, ErrorCode.propertyIsNotAList);

        d1.writePropertyInternal(PropertyIdentifier.logBuffer, new LinkedListLogBuffer<>());
        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), PropertyIdentifier.logBuffer, new UnsignedInteger(1)).handle(d1,
                        null), ErrorClass.property, ErrorCode.propertyIsNotAnArray);

        d1.writePropertyInternal(PropertyIdentifier.logBuffer, new SequenceOf<>(UnsignedInteger.ZERO));
        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), PropertyIdentifier.logBuffer, null,
                        new BySequenceNumber(1, 1)).handle(d1, null), ErrorClass.property,
                ErrorCode.datatypeNotSupported);

        d1.writePropertyInternal(PropertyIdentifier.logBuffer, new SequenceOf<>(UnsignedInteger.ZERO));
        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), PropertyIdentifier.logBuffer, null,
                        new ByTime(DateTime.UNSPECIFIED, 1)).handle(d1, null), ErrorClass.property,
                ErrorCode.datatypeNotSupported);

        d1.writePropertyInternal(PropertyIdentifier.logBuffer, new SequenceOf<>(UnsignedInteger.ZERO));
        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), PropertyIdentifier.logBuffer, null,
                        new ByTime(new DateTime(d1), 1)).handle(d1, null), ErrorClass.property,
                ErrorCode.datatypeNotSupported);

        d1.writePropertyInternal(PropertyIdentifier.logBuffer, new SequenceOf<>(new SequencedNotTimestamped()));
        TestUtils.assertRequestHandleException(
                () -> new ReadRangeRequest(d1.getId(), PropertyIdentifier.logBuffer, null,
                        new ByTime(new DateTime(d1), 1)).handle(d1, null), ErrorClass.property,
                ErrorCode.datatypeNotSupported);
    }

    /**
     * This is a unicorn object just for testing purposes.
     */
    private static class SequencedNotTimestamped extends Encodable implements Sequenced {
        @Override
        public long getSequenceNumber() {
            return 1;
        }

        @Override
        public void write(final ByteQueue queue) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void write(final ByteQueue queue, final int contextId) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void validate() {
            //Not necessary
        }
    }

    @Test
    public void noData() throws Exception {
        d1.getObject(d1.getId()).writePropertyInternal(pid, new SequenceOf<>());

        final ReadRangeAck ack = (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null).handle(d1, null);

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, false), ack.getResultFlags());
        assertEquals(UnsignedInteger.ZERO, ack.getItemCount());
        assertEquals(new SequenceOf<>(), ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    @Test
    public void positionNoMoreItems() throws BACnetException {
        SequenceOf<UnsignedInteger> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(new UnsignedInteger(i + 1));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new ByPosition(800, 150)).handle(d1, null);

        data = new SequenceOf<>(150);
        for (int i = 0; i < 150; i++)
            data.add(new UnsignedInteger(i + 800));

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, false), ack.getResultFlags());
        assertEquals(new UnsignedInteger(150), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    @Test
    public void positionTooLow() throws BACnetException {
        final SequenceOf<UnsignedInteger> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(new UnsignedInteger(i + 1));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new ByPosition(0, 150)).handle(d1, null);

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, false), ack.getResultFlags());
        assertEquals(UnsignedInteger.ZERO, ack.getItemCount());
        assertEquals(new SequenceOf<>(), ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    @Test
    public void positionTooHigh() throws BACnetException {
        final SequenceOf<UnsignedInteger> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(new UnsignedInteger(i + 1));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new ByPosition(1001, 150)).handle(d1, null);

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, false), ack.getResultFlags());
        assertEquals(UnsignedInteger.ZERO, ack.getItemCount());
        assertEquals(new SequenceOf<>(), ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    @Test
    public void allMoreItems() throws BACnetException {
        SequenceOf<UnsignedInteger> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(new UnsignedInteger(i + 1));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack = (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null).handle(d1, null);

        data = new SequenceOf<>(200);
        for (int i = 0; i < 200; i++)
            data.add(new UnsignedInteger(i + 1));

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(true, false, true), ack.getResultFlags());
        assertEquals(new UnsignedInteger(200), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    @Test
    public void sequenceOutOfRange() throws BACnetException {
        final SequenceOf<LogRecord> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(createLogRecord(now, 2001 + i));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        // Too low
        ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new BySequenceNumber(50, 300)).handle(d1,
                        null);

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, false), ack.getResultFlags());
        assertEquals(UnsignedInteger.ZERO, ack.getItemCount());
        assertEquals(new SequenceOf<>(), ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());

        // Too hight
        ack = (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new BySequenceNumber(5550, 300)).handle(d1,
                null);

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, false), ack.getResultFlags());
        assertEquals(UnsignedInteger.ZERO, ack.getItemCount());
        assertEquals(new SequenceOf<>(), ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    @Test
    public void timeNegativeCountSearchMiss() throws BACnetException {
        SequenceOf<LogRecord> data = new SequenceOf<>(1000);
        final GregorianCalendar gc = new GregorianCalendar(2013, Calendar.MARCH, 18, 1, 1);
        for (int i = 0; i < 20; i++) {
            data.add(createLogRecord(new DateTime(gc), i + 2001));
            gc.add(Calendar.MINUTE, 1);
        }
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack = (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null,
                new ByTime(new DateTime(new Date(2013, Month.MARCH, 18, null), new Time(1, 15, 30, 0)), -10)).handle(d1,
                null);

        data = new SequenceOf<>(10);
        gc.set(2013, Calendar.MARCH, 18, 1, 6, 0);
        for (int i = 0; i < 10; i++) {
            data.add(createLogRecord(new DateTime(gc), i + 2006));
            gc.add(Calendar.MINUTE, 1);
        }

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, false), ack.getResultFlags());
        assertEquals(new UnsignedInteger(10), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertEquals(new UnsignedInteger(2006), ack.getFirstSequenceNumber());
    }

    @Test
    public void timeOutOfRange() throws BACnetException {
        final SequenceOf<LogRecord> data = new SequenceOf<>(1000);
        final GregorianCalendar gc = new GregorianCalendar(2013, Calendar.MARCH, 18, 1, 1);
        for (int i = 0; i < 20; i++) {
            data.add(createLogRecord(new DateTime(gc), i + 2001));
            gc.add(Calendar.MINUTE, 1);
        }
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        // Too low
        ReadRangeAck ack = (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null,
                new ByTime(new DateTime(new Date(2013, Month.MARCH, 18, null), new Time(1, 1, 0, 0)), -10)).handle(d1,
                null);

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, false), ack.getResultFlags());
        assertEquals(UnsignedInteger.ZERO, ack.getItemCount());
        assertEquals(new SequenceOf<>(), ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());

        // Too high
        ack = (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null,
                new ByTime(new DateTime(new Date(2013, Month.MARCH, 18, null), new Time(1, 20, 0, 0)), 10)).handle(d1,
                null);

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, false, false), ack.getResultFlags());
        assertEquals(UnsignedInteger.ZERO, ack.getItemCount());
        assertEquals(new SequenceOf<>(), ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    @Test
    public void requestOverAvailable() throws BACnetException {
        SequenceOf<UnsignedInteger> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(new UnsignedInteger(i + 1));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new ByPosition(951, 100)).handle(d1, null);

        data = new SequenceOf<>(50);
        for (int i = 0; i < 50; i++)
            data.add(new UnsignedInteger(i + 951));

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(false, true, false), ack.getResultFlags());
        assertEquals(new UnsignedInteger(50), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    @Test
    public void requestBelowAvailable() throws BACnetException {
        SequenceOf<UnsignedInteger> data = new SequenceOf<>(1000);
        for (int i = 0; i < 1000; i++)
            data.add(new UnsignedInteger(i + 1));
        d1.getObject(d1.getId()).writePropertyInternal(pid, data);

        final ReadRangeAck ack =
                (ReadRangeAck) new ReadRangeRequest(d1.getId(), pid, null, new ByPosition(50, -100)).handle(d1, null);

        data = new SequenceOf<>(50);
        for (int i = 0; i < 50; i++)
            data.add(new UnsignedInteger(i + 1));

        assertEquals(d1.getId(), ack.getObjectIdentifier());
        assertEquals(pid, ack.getPropertyIdentifier());
        assertNull(ack.getPropertyArrayIndex());
        assertEquals(new ResultFlags(true, false, false), ack.getResultFlags());
        assertEquals(new UnsignedInteger(50), ack.getItemCount());
        assertEquals(data, ack.getItemData());
        assertNull(ack.getFirstSequenceNumber());
    }

    private static LogRecord createLogRecord(final DateTime timestamp, final long sequenceNumber) {
        final LogRecord record = new LogRecord(timestamp, Null.instance, null);
        record.setSequenceNumber(sequenceNumber);
        return record;
    }
}
