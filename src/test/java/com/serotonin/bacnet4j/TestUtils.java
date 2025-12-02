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

package com.serotonin.bacnet4j;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.exception.ErrorAPDUException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.error.BaseError;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

import lohbihler.warp.WarpClock;

public class TestUtils {
    static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

    public static <T, U> void assertListEqualsIgnoreOrder(final List<T> expectedList, final List<U> actualList,
            final BiPredicate<T, U> predicate) {
        Assert.assertEquals(expectedList.size(), actualList.size());
        final List<U> actualListCopy = new ArrayList<>(actualList);
        for (final T expected : expectedList) {
            // Find an element in the actual copy list where the predicate returns true.
            final int index = indexOf(actualListCopy, expected, predicate);
            if (index == -1)
                Assert.fail("Did not find " + expected + " in actual list");
            actualListCopy.remove(index);
        }
    }

    public static <T, U> int indexOf(final List<U> list, final T key, final BiPredicate<T, U> predicate) {
        for (int i = 0; i < list.size(); i++) {
            final U value = list.get(i);
            if (predicate.test(key, value))
                return i;
        }
        return -1;
    }

    public static <T> void assertListEqualsIgnoreOrder(final List<T> expectedList, final List<T> actualList) {
        Assert.assertEquals(expectedList.size(), actualList.size());
        final List<T> actualListCopy = new ArrayList<>(actualList);
        for (final T expected : expectedList) {
            // Find an element in the actual copy list which equals the expected.
            final int index = indexOf(actualListCopy, expected);
            if (index == -1)
                Assert.fail("Did not find " + expected + " in actual list");
            actualListCopy.remove(index);
        }
    }

    public static <T> int indexOf(final List<T> list, final T key) {
        for (int i = 0; i < list.size(); i++) {
            final T value = list.get(i);
            if (Objects.equals(value, key))
                return i;
        }
        return -1;
    }

    public static void assertEquals(final TimeStamp expected, final TimeStamp actual, final int deadbandHundredths) {
        if (expected.isDateTime() && actual.isDateTime())
            assertEquals(expected.getDateTime(), actual.getDateTime(), deadbandHundredths);
        else if (expected.isTime() && actual.isTime())
            assertEquals(expected.getTime(), actual.getTime(), deadbandHundredths);
        else
            Assert.assertEquals(expected, actual);
    }

    public static void assertEquals(final DateTime expected, final DateTime actual, final int deadbandHundredths) {
        final long expectedMillis = expected.getGC().getTimeInMillis();
        final long actualMillis = actual.getGC().getTimeInMillis();

        long diff = expectedMillis - actualMillis;
        if (diff < 0)
            diff = -diff;
        diff /= 10;
        if (diff > deadbandHundredths) {
            // This will fail
            Assert.assertEquals(expected, actual);
        }
    }

    public static void assertEquals(final Time expected, final Time actual, final int deadbandHundredths) {
        // Can only compare this way if the times are fully specified.
        if (expected.isFullySpecified() && actual.isFullySpecified()) {
            final int diff = expected.getSmallestDiff(actual);
            if (diff > deadbandHundredths) {
                // This will fail
                Assert.assertEquals(expected, actual);
            }
        } else {
            Assert.assertEquals(expected, actual);
        }
    }

    @SafeVarargs
    public static <T> List<T> toList(final T... elements) {
        final List<T> result = new ArrayList<>(elements.length);
        result.addAll(Arrays.asList(elements));
        return result;
    }

    public static void assertBACnetServiceException(final BACnetServiceException e, final ErrorClass errorClass,
            final ErrorCode errorCode) {
        Assert.assertEquals(errorClass, e.getErrorClass());
        Assert.assertEquals(errorCode, e.getErrorCode());
    }

    public static void assertBACnetServiceException(final ServiceExceptionCommand command, final ErrorClass errorClass,
            final ErrorCode errorCode) {
        try {
            command.call();
            fail("BACnetServiceException was expected");
        } catch (final BACnetServiceException e) {
            assertBACnetServiceException(e, errorClass, errorCode);
        }
    }

    @FunctionalInterface
    public interface ServiceExceptionCommand {
        void call() throws BACnetServiceException;
    }

    public static void assertRequestHandleException(final RequestHandleExceptionCommand command,
            final ErrorClass errorClass, final ErrorCode errorCode) {
        try {
            command.call();
            fail("BACnetException was expected");
        } catch (final BACnetErrorException e) {
            assertErrorClassAndCode(e.getBacnetError().getError().getErrorClassAndCode(), errorClass, errorCode);
        } catch (final BACnetException e) {
            fail("Not a BACnetErrorException: " + e);
        }
    }

    @FunctionalInterface
    public interface RequestHandleExceptionCommand {
        void call() throws BACnetException;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BaseError> T assertErrorAPDUException(final BACnetExceptionCommand command,
            final ErrorClass errorClass, final ErrorCode errorCode) {
        try {
            command.call();
            fail("BACnetException was expected");
        } catch (final BACnetException e) {
            if (e instanceof ErrorAPDUException eae) {
                assertErrorClassAndCode(eae.getError().getErrorClassAndCode(), errorClass, errorCode);
                return (T) eae.getApdu().getError();
            }
            fail("Embedded ErrorAPDUException was expected: " + e.getClass());
        }
        return null;
    }

    @FunctionalInterface
    public interface BACnetExceptionCommand {
        void call() throws BACnetException;
    }

    public static void assertErrorClassAndCode(final ErrorClassAndCode ecac, final ErrorClass errorClass,
            final ErrorCode errorCode) {
        Assert.assertEquals(errorClass, ecac.getErrorClass());
        Assert.assertEquals(errorCode, ecac.getErrorCode());
    }

    public static void assertEncoding(final Encodable encodable, final String expectedHex) {
        final ByteQueue expectedResult = new ByteQueue(expectedHex);

        // Serialize the Encodable and compare with the hex.
        final ByteQueue queue = new ByteQueue();
        encodable.write(queue);
        Assert.assertEquals(expectedResult, queue);

        // Parse the hex and confirm the objects are equal.
        Encodable parsed;
        try {
            parsed = Encodable.read(queue, encodable.getClass());
        } catch (final BACnetException e) {
            LOG.error("", e);
            fail(e.getMessage());
            return;
        }

        Assert.assertEquals(0, queue.size());
        Assert.assertEquals(encodable, parsed);
    }

    public static <T extends Encodable> void assertSequenceEncoding(final SequenceOf<T> encodable,
            final Class<T> innerType, final String expectedHex) {
        final ByteQueue expectedResult = new ByteQueue(expectedHex);

        // Serialize the Encodable and compare with the hex.
        final ByteQueue queue = new ByteQueue();
        encodable.write(queue);
        Assert.assertEquals(expectedResult, queue);

        // Parse the hex and confirm the objects are equal.
        Encodable parsed;
        try {
            parsed = Encodable.readSequenceOf(queue, innerType);
        } catch (final BACnetException e) {
            LOG.error("", e);
            fail(e.getMessage());
            return;
        }

        Assert.assertEquals(0, queue.size());
        Assert.assertEquals(encodable, parsed);
    }

    public static void assertFileContentEquals(final File expected, final File actual) throws IOException {
        Assert.assertEquals(expected.exists(), actual.exists());
        Assert.assertEquals(expected.length(), actual.length());

        // Slow, but easy
        final long length = expected.length();
        long position = 0;
        try (FileInputStream expectedFis = new FileInputStream(expected);
                FileInputStream actualFis = new FileInputStream(actual)) {
            while (position < length) {
                Assert.assertEquals("At file position " + position, expectedFis.read(), actualFis.read());
                position++;
            }
        }
    }

    /**
     * Supplier that returns a boolean and can throw an exception doing so.
     */
    @FunctionalInterface
    public interface BooleanSupplierWithException {
        boolean getAsBoolean() throws Exception;
    }

    /**
     * Convenience method to default the timeout to 5 seconds.
     *
     * @param condition the condition to which to wait
     * @throws Exception the exception if any that the condition threw
     */
    public static void awaitTrue(BooleanSupplierWithException condition) throws Exception {
        awaitTrue(condition, 5000);
    }

    /**
     * A utility to busy-wait up to a given timeout for the given condition to become true.
     *
     * @param condition the condition to which to wait
     * @param timeoutMs the maximum amount of time to wait
     * @throws Exception the exception if any that the condition threw
     */
    public static void awaitTrue(BooleanSupplierWithException condition, long timeoutMs) throws Exception {
        if (await(true, condition, timeoutMs)) {
            return;
        }
        fail("awaitTrue timed out");
    }

    /**
     * Convenience method defaulting the timeout to 5 seconds.
     *
     * @param condition the condition to which to wait
     * @throws Exception the exception if any thrown by the condition
     */
    public static void awaitFalse(BooleanSupplierWithException condition) throws Exception {
        if (await(false, condition, 5000)) {
            return;
        }
        fail("awaitFalse timed out");
    }

    /**
     * Utility to busy-wait up to a given timeout for the given condition to become false.
     *
     * @param condition the condition to which to wait
     * @param timeoutMs the maximum amount of time to wait
     * @throws Exception the exception if any thrown by the condition
     */
    public static void awaitFalse(BooleanSupplierWithException condition, long timeoutMs) throws Exception {
        if (await(false, condition, timeoutMs)) {
            return;
        }
        fail("awaitFalse timed out");
    }

    /**
     * Lifted from Assert to test whether a given object pair are either both null or equal.
     *
     * @param expected the expected value
     * @param actual   the value to check
     * @return true if they are both null or equal, false otherwise
     */
    public static boolean equalsRegardingNull(Object expected, Object actual) {
        if (expected == null) {
            return actual == null;
        }
        return expected.equals(actual);
    }

    /**
     * Supplier that returns an int and can throw an exception doing so.
     */
    @FunctionalInterface
    public interface IntSupplierWithException {
        int get() throws Exception;
    }

    /**
     * Convenience formulation of the awaitEquals implementation that looks more like an assert.
     *
     * @param expected       the value to match
     * @param actualSupplier the supplier the value of which to check
     */
    public static void awaitEquals(int expected, final IntSupplierWithException actualSupplier) throws Exception {
        awaitEquals(expected, actualSupplier, 10000);
    }

    /**
     * Utility to busy-wait up to a given timeout for the given supplier to supply a value that matches that given.
     *
     * @param expected  the value to match
     * @param supplier  the supplier the value of which to check
     * @param timeoutMs the maximum amount of time to wait
     * @throws Exception the exception if any thrown by the supplier
     */
    public static void awaitEquals(int expected, final IntSupplierWithException supplier, long timeoutMs)
            throws Exception {
        if (await(true, () -> supplier.get() == expected, timeoutMs)) {
            return;
        }
        fail("awaitEquals timed out. Wanted " + expected + " but last value was " + supplier.get());
    }

    /**
     * Supplier that returns an Encodable and can throw an exception doing so.
     */
    @FunctionalInterface
    public interface EncodableSupplierWithException {
        Encodable get() throws Exception;
    }

    /**
     * Convenience formulation of the awaitEquals implementation that looks more like an assert.
     *
     * @param expected       the value to match
     * @param actualSupplier the supplier the value of which to check
     */
    public static void awaitEquals(Encodable expected, final EncodableSupplierWithException actualSupplier)
            throws Exception {
        awaitEquals(expected, actualSupplier, 5000);
    }

    /**
     * Utility to busy-wait up to a given timeout for the given supplier to supply a value that matches that given.
     *
     * @param expected  the value to match
     * @param supplier  the supplier the value of which to check
     * @param timeoutMs the maximum amount of time to wait
     * @throws Exception the exception if any thrown by the supplier
     */
    public static void awaitEquals(Encodable expected, final EncodableSupplierWithException supplier, long timeoutMs)
            throws Exception {
        if (await(true, () -> equalsRegardingNull(expected, supplier.get()), timeoutMs)) {
            return;
        }
        fail("awaitEquals timed out. Wanted " + expected + " but last value was " + supplier.get());
    }

    /**
     * Convenience method defaulting the value to true and the timeout to 5 seconds.
     *
     * @param condition the condition to which to wait
     * @return true if the condition was matched, false otherwise
     * @throws Exception the exception if any thrown by the condition
     */
    public static boolean await(BooleanSupplierWithException condition) throws Exception {
        return await(true, condition, 5000);
    }

    /**
     * Convenience method defaulting the value to true.
     *
     * @param condition the condition to which to wait
     * @param timeoutMs the maximum amount of time to wait
     * @return true if the condition was matched, false otherwise
     * @throws Exception the exception if any thrown by the condition
     */
    public static boolean await(BooleanSupplierWithException condition, long timeoutMs) throws Exception {
        return await(true, condition, timeoutMs);
    }

    /**
     * Utility that will "busy-wait" up to a given timeout for a condition to match the given value.
     *
     * @param expected  the value the condition should match
     * @param condition the condition to which to wait
     * @param timeoutMs the maximum amount of time to wait
     * @return true if the condition was matched, false otherwise
     * @throws Exception the exception if any thrown by the condition
     */
    public static boolean await(boolean expected, BooleanSupplierWithException condition, long timeoutMs)
            throws Exception {
        final long deadline = Clock.systemUTC().millis() + timeoutMs;
        while (true) {
            if (condition.getAsBoolean() == expected) {
                return true;
            }
            if (deadline < Clock.systemUTC().millis()) {
                return false;
            }
            ThreadUtils.sleep(2);
        }
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }

    /**
     * Lifted from the WarpClock.plus method, but instead of sleep intervals this uses runnables.
     *
     * @param clock       the clock to advance
     * @param amount      the amount of time to advance
     * @param unit        the unit of total time amount
     * @param preAdvance  a function to run before each increment
     * @param postAdvance a function to run after each increment, including after the total time has advanced
     * @return the final datetime
     */
    public static LocalDateTime advanceClock(WarpClock clock, int amount, TimeUnit unit,
            RunnableWithException preAdvance, RunnableWithException postAdvance) throws Exception {
        return advanceClock(clock, amount, unit, 0, null, preAdvance, postAdvance);
    }

    /**
     * Lifted from the WarpClock.plus method, but instead of sleep intervals this uses runnables.
     *
     * @param clock           the clock to advance
     * @param totalAmount     the total amount of time to advance
     * @param unit            the unit of total time amount
     * @param incrementAmount the amount of time between each increment
     * @param incrementUnit   the unit of time increment
     * @param preAdvance      a function to run before each increment
     * @param postAdvance     a function to run after each increment, including after the total time has advanced
     * @return the final datetime
     */
    public static LocalDateTime advanceClock(WarpClock clock, int totalAmount, TimeUnit unit, int incrementAmount,
            TimeUnit incrementUnit, RunnableWithException preAdvance, RunnableWithException postAdvance)
            throws Exception {
        long remainder = unit.toNanos(totalAmount);
        long each = (incrementUnit == null ? unit : incrementUnit).toNanos(
                incrementAmount == 0 ? (long) totalAmount : (long) incrementAmount);
        LocalDateTime result = null;

        try {
            if (remainder <= 0L) {
                if (preAdvance != null)
                    preAdvance.run();
                result = clock.plusNanos(0L);
                if (postAdvance != null)
                    postAdvance.run();
            } else {
                while (remainder > 0L) {
                    long nanos = each;
                    if (each > remainder) {
                        nanos = remainder;
                    }

                    if (preAdvance != null)
                        preAdvance.run();
                    result = clock.plusNanos(nanos);
                    remainder -= nanos;
                    if (postAdvance != null)
                        postAdvance.run();
                }
            }

            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sleep for a given period before continuing to ensure that nothing happens during that time. This is frequently
     * used to ensure that notifications are not sent following some activity.
     */
    public static void quiesce() {
        ThreadUtils.sleep(500);
    }

    /**
     * Looks for the existence of a `.dockerenv` file at the root directory as evidence that this is running in a
     * docker container.
     *
     * @return true if the docker env flag file is found.
     */
    public static boolean isDockerEnv() {
        return new File("/.dockerenv").exists();
    }
}
