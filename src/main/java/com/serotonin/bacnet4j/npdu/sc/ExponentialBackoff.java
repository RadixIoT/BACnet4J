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

package com.serotonin.bacnet4j.npdu.sc;

public class ExponentialBackoff implements BackoffPolicy {
    private final double multiplier;
    private int minimumReconnectTime;
    private int maximumReconnectTime;

    private int nextReconnectTime;

    public ExponentialBackoff(double multiplier) {
        this.multiplier = multiplier;
    }

    public void configure(int minimumReconnectTime, int maximumReconnectTime) {
        this.minimumReconnectTime = minimumReconnectTime;
        this.maximumReconnectTime = maximumReconnectTime;
        reset();
    }

    public int getInitialWaitTimeout() {
        // 12.56.82
        return Math.min(minimumReconnectTime, maximumReconnectTime);
    }

    public int getReconnectWaitTimeout() {
        int result = nextReconnectTime;

        // Might consider adding some randomness here too.
        int next = (int) Math.round(nextReconnectTime * multiplier);
        if (next == nextReconnectTime) {
            next += 1; // Ensure it increases by at least one.
        }
        if (next > maximumReconnectTime) {
            next = maximumReconnectTime;
        }

        nextReconnectTime = next;

        return result;
    }

    public void reset() {
        nextReconnectTime = minimumReconnectTime;
    }
}
