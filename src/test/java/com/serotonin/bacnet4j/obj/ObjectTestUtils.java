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

package com.serotonin.bacnet4j.obj;

import static org.junit.Assert.fail;

import java.time.Clock;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;

public class ObjectTestUtils {
    /**
     * Test utility to wait for a specific value to be written to an object.
     */
    public static class ObjectWriteNotifier<T extends BACnetObject> extends AbstractMixin {
        static final Logger LOG = LoggerFactory.getLogger(ObjectWriteNotifier.class);

        private final T bo;
        private final BlockingQueue<WrittenProperty> writtenProperties = new LinkedBlockingQueue<>();

        /**
         * You can call this directly, but you probably want to use createObjectWriteNotifier below.
         *
         * @param bo the object to which to listen
         */
        public ObjectWriteNotifier(T bo) {
            super(bo);
            this.bo = bo;
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
            LOG.debug("DEBUG: written properties cleared");
            writtenProperties.clear();
        }

        @Override
        protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
            LOG.debug("PropertyWritten: {}, pid: {}, oldValue: {}, newValue: {}", bo.getId(), pid, oldValue, newValue);
            writtenProperties.add(new WrittenProperty(pid, newValue));
        }

        /**
         * Convenience method that defaults the wait time to 5 seconds.
         *
         * @param pid      the property identifier for which to watch
         * @param newValue the value for which to watch, or null for any value.
         */
        public void waitFor(PropertyIdentifier pid, Encodable newValue) {
            waitFor(pid, newValue, 5000);
        }

        /**
         * Wait until a given property has a given value written to it, or until the wait time has expired.
         *
         * @param pid      the property identifier for which to watch
         * @param newValue the value for which to watch, or null for any value.
         * @param waitTime how long to wait. If time expires an AssertionException is throws via the fail method.
         */
        public void waitFor(PropertyIdentifier pid, Encodable newValue, long waitTime) {
            LOG.debug("waitFor in {}, for pid: {}, newValue: {}", bo.getId(), pid, newValue);
            long startTime = Clock.systemUTC().millis();
            while (true) {
                try {
                    WrittenProperty wp = writtenProperties.poll(waitTime, TimeUnit.MILLISECONDS);
                    if (wp == null) {
                        fail("Timeout waiting for property write of " + pid + " to " + newValue);
                    }
                    if (wp.pid == pid && (newValue == null || wp.newValue.equals(newValue))) {
                        LOG.debug("{} waited for {}ms for pid: {}, newValue: {}", bo.getId(),
                                Clock.systemUTC().millis() - startTime, pid, newValue);
                        return;
                    }
                } catch (InterruptedException e) {
                    fail("Polling interrupted for property write of " + pid + " to " + newValue);
                }
            }
        }

        record WrittenProperty(PropertyIdentifier pid, Encodable newValue) {
        }
    }

    /**
     * Convenience method for setting up a property write notifier.
     *
     * @param bo  the object to which to listen.
     * @param <T> the type of BACnetObject to which is being listened
     * @return the newly created notifier
     */
    public static <T extends BACnetObject> ObjectWriteNotifier<T> createObjectWriteNotifier(T bo) {
        ObjectWriteNotifier<T> notifier = new ObjectWriteNotifier<>(bo);
        // Always add the mixin at the front so that it never missed updates.
        bo.addMixin(0, notifier);
        return notifier;
    }
}
