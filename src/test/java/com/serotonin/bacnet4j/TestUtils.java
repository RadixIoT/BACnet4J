package com.serotonin.bacnet4j;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.junit.Assert;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.exception.ErrorAPDUException;
import com.serotonin.bacnet4j.exception.RejectAPDUException;
import com.serotonin.bacnet4j.obj.logBuffer.LogBuffer;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.RejectReason;
import com.serotonin.bacnet4j.type.error.BaseError;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

public class TestUtils {
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
        for (final T e : elements)
            result.add(e);
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
    public static interface ServiceExceptionCommand {
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
    public static interface RequestHandleExceptionCommand {
        void call() throws BACnetException;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BaseError> T assertErrorAPDUException(final BACnetExceptionCommand command,
            final ErrorClass errorClass, final ErrorCode errorCode) {
        try {
            command.call();
            fail("BACnetException was expected");
        } catch (final BACnetException e) {
            if (e instanceof ErrorAPDUException) {
                final ErrorAPDUException eae = (ErrorAPDUException) e;
                assertErrorClassAndCode(eae.getError().getErrorClassAndCode(), errorClass, errorCode);
                return (T) eae.getApdu().getError();
            }
            fail("Embedded ErrorAPDUException was expected: " + e.getClass());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void assertRejectAPDUException(final BACnetExceptionCommand command,
            final RejectReason rejectReason) {
        try {
            command.call();
            fail("BACnetException was expected");
        } catch (final BACnetException e) {
            if (e instanceof RejectAPDUException) {
                final RejectAPDUException eae = (RejectAPDUException) e;
                Assert.assertEquals(rejectReason, eae.getApdu().getRejectReason());
            } else {
                fail("RejectAPDUException was expected: " + e.getClass());
            }            
        }
    }
    
    @FunctionalInterface
    public static interface BACnetExceptionCommand {
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
            e.printStackTrace();
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
            e.printStackTrace();
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

    //
    // Size assurance. Uses busy wait with timeout to ensure that a collection reaches a certain size.
    public static void assertSize(final LogBuffer<?> buffer, final int size, final int wait) {
        assertSize(() -> buffer.size(), size, wait);
    }

    public static void assertSize(final Collection<?> collection, final int size, final int wait) {
        assertSize(() -> collection.size(), size, wait);
    }

    private static void assertSize(final SizeRetriever thingWithSize, final int size, final int wait) {
        final long deadline = Clock.systemUTC().millis() + wait;
        while (true) {
            if (thingWithSize.size() == size) {
                return;
            }
            if (deadline < Clock.systemUTC().millis()) {
                fail("Expected collection size of " + size + ", but was " + thingWithSize.size());
            }
            ThreadUtils.sleep(2);
        }
    }

    @FunctionalInterface
    interface SizeRetriever {
        int size();
    }

    @FunctionalInterface
    public interface BooleanSupplierWithException {
        boolean getAsBoolean() throws Exception;
    }

    public static void awaitCondition(BooleanSupplierWithException condition, long timeoutMs) throws Exception {
        final long deadline = Clock.systemUTC().millis() + timeoutMs;
        while (true) {
            if (condition.getAsBoolean()) {
                return;
            }
            if (deadline < Clock.systemUTC().millis()) {
                fail("awaitCondition timed out");
            }
            ThreadUtils.sleep(2);
        }
    }

}
