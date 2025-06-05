package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.fail;

import java.time.Clock;
import java.util.LinkedList;
import java.util.List;

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;

public class ObjectTestUtils {
    /**
     * Test utility to wait for a specific value to be written to an object.
     */
    public static class ObjectWriteNotifier<T extends BACnetObject> extends AbstractMixin {
        private final T bo;
        private final boolean debug;
        private final List<WrittenProperty> writtenProperties = new LinkedList<>();

        /**
         * You can call this directly, but you probably want to use createObjectWriteNotifier below.
         *
         * @param bo the object to which to listen
         * @param debug whether debugging messages should be written to stdout. This is done instead of logging because
         *              logging requires extra work to enable messages to be seen, which also includes the logging of a
         *              lot of other messages.
         */
        public ObjectWriteNotifier(T bo, boolean debug) {
            super(bo);
            this.bo = bo;
            this.debug = debug;
        }

        /**
         * This method allows the notifier to be the only object reference required. I.e. the client code doesn't need
         * to maintain a reference to the object and the notifier. It can just ask the notifier for the object.
         *
         * @return the object which is being monitored
         */
        public T obj() {
            return bo;
        }

        /**
         * Useful for preventing the matching of initialization writes, or other writes that may be inconsequential to
         * the test.
         */
        public void clear() {
            synchronized (this) {
                writtenProperties.clear();
            }
        }

        @Override
        protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
            if (debug) {
                System.out.println("DEBUG: PropertyWritten: "+ bo.getId() +", pid: "+ pid +", oldValue: "+ oldValue +", newValue: "+ newValue);
            }
            synchronized (this) {
                // Write all written properties to the list of seen writes.
                writtenProperties.add(new WrittenProperty(pid, newValue));
                notifyAll();
            }
        }

        /**
         * Wait until a given property has a given value written to it, or until the wait time has expired.
         *
         * @param pid the property identifier for which to watch
         * @param newValue the value for which to watch
         * @param waitTime how long to wait. If time expires an AssertionException is throws via the fail method.
         */
        public void waitFor(PropertyIdentifier pid, Encodable newValue, long waitTime) {
            if (debug) {
                System.out.println("DEBUG: waitFor in "+ bo.getId() +", for pid: "+ pid +", newValue: "+ newValue);
            }
            synchronized (this) {
                long startTime = Clock.systemUTC().millis();
                long deadline = startTime + waitTime;
                while (true) {
                    if (writtenProperties.isEmpty()) {
                        long thisWait = deadline - Clock.systemUTC().millis();
                        if (thisWait <= 0) {
                            fail("Timeout waiting for property write of " + pid + " to " + newValue);
                        } else {
                            try {
                                if (debug) {
                                    System.out.println("DEBUG: Waiting at least "+ thisWait +"ms in "+ bo.getId() +", for pid: "+ pid +", newValue: "+ newValue);
                                }
                                wait(thisWait);
                            } catch (final InterruptedException e) {
                                if (debug) {
                                    System.out.println("DEBUG: Wait interrupted");
                                }
                                // Ignore
                            }
                        }
                    } else {
                        WrittenProperty wp = writtenProperties.remove(0);
                        if (wp.pid == pid && wp.newValue.equals(newValue)) {
                            if (debug) {
                                System.out.println("DEBUG "+ bo.getId() +" waited for "+ (Clock.systemUTC().millis() - startTime) +" ms for "+ pid);
                            }
                            return;
                        }
                    }
                }
            }
        }

        static class WrittenProperty {
            PropertyIdentifier pid;
            Encodable newValue;

            WrittenProperty(PropertyIdentifier pid, Encodable newValue) {
                this.pid = pid;
                this.newValue = newValue;
            }
        }
    }

    /**
     * Convenience method for setting up a property write notifier without debugging.
     *
     * @param bo the object to which to listen.
     * @return the newly created notifier
     * @param <T> the type of BACnetObject to which is being listened
     */
    public static <T extends BACnetObject> ObjectWriteNotifier<T> createObjectWriteNotifier(T bo) {
        return createObjectWriteNotifier(bo, false);
    }

    /**
     * Convenience method for setting up a property write notifier
     *
     * @param bo the object to which to listen.
     * @param debug true if debugging messages should be written.
     * @return the newly created notifier
     * @param <T> the type of BACnetObject to which is being listened
     */
    public static <T extends BACnetObject> ObjectWriteNotifier<T> createObjectWriteNotifier(T bo, boolean debug) {
        ObjectWriteNotifier<T> notifier = new ObjectWriteNotifier<>(bo, debug);
        bo.addMixin(notifier);
        return notifier;
    }
}
