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

package com.serotonin.bacnet4j.cache;

import java.time.Duration;

import com.serotonin.bacnet4j.LocalDevice;

public interface RemoteEntityCachePolicy {
    Object prepareState(LocalDevice localDevice);

    boolean hasExpired(LocalDevice localDevice, Object state);

    public static final RemoteEntityCachePolicy NEVER_CACHE = new NeverCache();
    public static final RemoteEntityCachePolicy EXPIRE_5_SECONDS = new TimedExpiry(Duration.ofSeconds(5));
    public static final RemoteEntityCachePolicy EXPIRE_1_MINUTE = new TimedExpiry(Duration.ofMinutes(1));
    public static final RemoteEntityCachePolicy EXPIRE_15_MINUTES = new TimedExpiry(Duration.ofMinutes(15));
    public static final RemoteEntityCachePolicy EXPIRE_1_HOUR = new TimedExpiry(Duration.ofHours(1));
    public static final RemoteEntityCachePolicy EXPIRE_4_HOURS = new TimedExpiry(Duration.ofHours(4));
    public static final RemoteEntityCachePolicy EXPIRE_1_DAY = new TimedExpiry(Duration.ofDays(1));
    public static final RemoteEntityCachePolicy NEVER_EXPIRE = new NeverExpire();


    public static class TimedExpiry implements RemoteEntityCachePolicy {
        private final long duration;

        public TimedExpiry(final Duration duration) {
            this.duration = duration.toMillis();
        }

        @Override
        public Object prepareState(final LocalDevice localDevice) {
            return localDevice.getClock().millis() + duration;
        }

        @Override
        public boolean hasExpired(final LocalDevice localDevice, final Object state) {
            return localDevice.getClock().millis() > (Long) state;
        }

        @Override
        public String toString() {
            return "TimedExpiry [duration=" + duration + "]";
        }
    }


    public static class NeverExpire implements RemoteEntityCachePolicy {
        @Override
        public Object prepareState(final LocalDevice localDevice) {
            return null;
        }

        @Override
        public boolean hasExpired(final LocalDevice localDevice, final Object state) {
            return false;
        }
    }


    public static class NeverCache implements RemoteEntityCachePolicy {
        @Override
        public Object prepareState(final LocalDevice localDevice) {
            return null;
        }

        @Override
        public boolean hasExpired(final LocalDevice localDevice, final Object state) {
            return true;
        }
    }
}
