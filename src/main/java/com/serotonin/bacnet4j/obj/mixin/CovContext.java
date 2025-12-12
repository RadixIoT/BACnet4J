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

package com.serotonin.bacnet4j.obj.mixin;

import java.time.Clock;

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class CovContext {
    private final Clock clock;

    // Identifying properties.
    private final Address address;
    private final UnsignedInteger subscriberProcessIdentifier;
    // This is the property that was requested to be monitored in the subscription request. Can be null.
    private final PropertyIdentifier monitoredProperty;
    // This is the property that will be reported as being monitored in the active COV subscriptions property in the
    // device. Cannot be null.
    private final PropertyIdentifier exposedMonitoredProperty;

    // Mutable properties.
    private boolean issueConfirmedNotifications;
    private long expiryTime;
    private Real covIncrement;

    // Runtime values.
    private Encodable lastCovIncrementValue;

    public CovContext(final Clock clock, final Address address, final UnsignedInteger subscriberProcessIdentifier,
            final PropertyIdentifier monitoredProperty, final PropertyIdentifier exposedMonitoredProperty) {
        this.clock = clock;
        this.address = address;
        this.subscriberProcessIdentifier = subscriberProcessIdentifier;
        this.monitoredProperty = monitoredProperty;
        this.exposedMonitoredProperty = exposedMonitoredProperty;
    }

    public Address getAddress() {
        return address;
    }

    public UnsignedInteger getSubscriberProcessIdentifier() {
        return subscriberProcessIdentifier;
    }

    public PropertyIdentifier getMonitoredProperty() {
        return monitoredProperty;
    }

    public boolean isObjectSubscription() {
        return monitoredProperty == null;
    }

    public PropertyIdentifier getExposedMonitoredProperty() {
        return exposedMonitoredProperty;
    }

    public boolean isIssueConfirmedNotifications() {
        return issueConfirmedNotifications;
    }

    public void setIssueConfirmedNotifications(final boolean issueConfirmedNotifications) {
        this.issueConfirmedNotifications = issueConfirmedNotifications;
    }

    public Real getCovIncrement() {
        return covIncrement;
    }

    public void setCovIncrement(final Real covIncrement) {
        this.covIncrement = covIncrement;
    }

    public void setExpiryTime(final int seconds) {
        if (seconds == 0)
            expiryTime = -1;
        else
            expiryTime = clock.millis() + seconds * 1000;
    }

    public boolean hasExpired(final long now) {
        if (expiryTime == -1)
            return false;
        return expiryTime < now;
    }

    public Encodable getLastCovIncrementValue() {
        return lastCovIncrementValue;
    }

    public void setLastCovIncrementValue(final Encodable lastCovIncrementValue) {
        this.lastCovIncrementValue = lastCovIncrementValue;
    }

    public int getSecondsRemaining(final long now) {
        if (expiryTime == -1)
            return 0;
        final int left = (int) ((expiryTime - now + 500) / 1000);
        if (left < 1)
            return 1;
        return left;
    }
}
