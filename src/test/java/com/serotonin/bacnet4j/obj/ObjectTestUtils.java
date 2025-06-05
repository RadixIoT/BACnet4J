package com.serotonin.bacnet4j.obj;

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;

import java.time.Clock;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.fail;

public class ObjectTestUtils {
    /**
     * Test utility to wait for a specific value to be written to an object.
     */
    public static class ObjectWriteNotifier<T extends BACnetObject> extends AbstractMixin {
        private final T bo;
        private final boolean debug;
        private final List<WrittenProperty> writtenProperties = new LinkedList<>();

        public ObjectWriteNotifier(T bo, boolean debug) {
            super(bo);
            this.bo = bo;
            this.debug = debug;
        }

        public T obj() {
            return bo;
        }

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

    public static <T extends BACnetObject> ObjectWriteNotifier<T> createObjectWriteNotifier(T bo) {
        return createObjectWriteNotifier(bo, false);
    }

    public static <T extends BACnetObject> ObjectWriteNotifier<T> createObjectWriteNotifier(T bo, boolean debug) {
        ObjectWriteNotifier<T> notifier = new ObjectWriteNotifier<>(bo, debug);
        bo.addMixin(notifier);
        return notifier;
    }
}
