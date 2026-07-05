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

package com.serotonin.bacnet4j.obj;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.obj.mixin.PollingDelegate;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.EventTransitionBits;
import com.serotonin.bacnet4j.type.enumerated.LoggingType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public abstract class TrendLogBase extends LogBase {
    protected boolean logDisabled;
    protected ScheduledFuture<?> startTimeFuture;
    protected ScheduledFuture<?> stopTimeFuture;

    protected PollingDelegate pollingDelegate;
    protected ScheduledFuture<?> pollingFuture;

    protected TrendLogBase(LocalDevice localDevice, ObjectType type, int instanceNumber, String name, boolean enable,
            DateTime startTime, DateTime stopTime, int logInterval, boolean stopWhenFull, int bufferSize) {
        super(localDevice, type, instanceNumber, name, enable, startTime, stopTime, stopWhenFull, bufferSize);
        set(PropertyIdentifier.logInterval, new UnsignedInteger(logInterval));
        set(PropertyIdentifier.alignIntervals, Boolean.TRUE);
        set(PropertyIdentifier.intervalOffset, UnsignedInteger.ZERO);
        set(PropertyIdentifier.trigger, Boolean.FALSE);
    }

    protected void postInitialize() {
        updateMonitoredProperty();
        updateStartTime(get(PropertyIdentifier.startTime));
        updateStopTime(get(PropertyIdentifier.stopTime));
        withTriggered();

        addLogMixins();
    }

    protected void baseWithPolled(int logInterval, TimeUnit logIntervalUnit, boolean alignIntervals,
            int intervalOffset, TimeUnit offsetUnit) {
        set(PropertyIdentifier.logInterval, new UnsignedInteger(logIntervalUnit.toMillis(logInterval) / 10));
        set(PropertyIdentifier.alignIntervals, Boolean.valueOf(alignIntervals));
        set(PropertyIdentifier.intervalOffset, new UnsignedInteger(offsetUnit.toMillis(intervalOffset) / 10));
        set(PropertyIdentifier.loggingType, LoggingType.polled);
        updateLoggingType();
    }

    @Override
    protected void baseSupportIntrinsicReporting(int notificationThreshold, int notificationClass,
            EventTransitionBits eventEnable, NotifyType notifyType) {
        super.baseSupportIntrinsicReporting(notificationThreshold, notificationClass, eventEnable, notifyType);
        updateMonitoredProperty();
    }

    protected abstract void updateMonitoredProperty();

    protected void withTriggered() {
        set(PropertyIdentifier.loggingType, LoggingType.triggered);
        updateLoggingType();
    }

    protected abstract void updateLoggingType();

    protected void updatePolledLoggingType() {
        UnsignedInteger logInterval = get(PropertyIdentifier.logInterval);
        Boolean alignIntervals = get(PropertyIdentifier.alignIntervals);
        UnsignedInteger intervalOffset = get(PropertyIdentifier.intervalOffset);

        long period = logInterval.longValue() * 10;
        if (period == 0)
            // 0 is a poor value. Default to 5 minutes in this case, since it "is a local matter".
            period = TimeUnit.MINUTES.toMillis(5);

        long initialDelay = 0;
        int offsetToUse = 0;
        if (alignIntervals.booleanValue()) {
            long now = getLocalDevice().getClock().millis();

            // Find the largest time period to which the period aligns.
            if (period % TimeUnit.DAYS.toMillis(1) == 0) {
                initialDelay = TimeUnit.DAYS.toMillis(1) - now % TimeUnit.DAYS.toMillis(1);
            } else if (period % TimeUnit.HOURS.toMillis(1) == 0) {
                initialDelay = TimeUnit.HOURS.toMillis(1) - now % TimeUnit.HOURS.toMillis(1);
            } else if (period % TimeUnit.MINUTES.toMillis(1) == 0) {
                initialDelay = TimeUnit.MINUTES.toMillis(1) - now % TimeUnit.MINUTES.toMillis(1);
            } else if (period % TimeUnit.SECONDS.toMillis(1) == 0) {
                initialDelay = TimeUnit.SECONDS.toMillis(1) - now % TimeUnit.SECONDS.toMillis(1);
            }

            offsetToUse = intervalOffset.intValue() * 10;
            offsetToUse %= (int) period;
        }

        initialDelay += offsetToUse;
        initialDelay %= period;

        pollingFuture = getLocalDevice().scheduleAtFixedRate(this::doPoll, initialDelay, period,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Locally trigger a poll.
     *
     * @return true if the trigger was done, false if the trigger value was already true, indicating that a trigger
     * was already in progress.
     */
    public synchronized boolean trigger() {
        Boolean trigger = get(PropertyIdentifier.trigger);
        if (trigger.booleanValue()) {
            return false;
        }
        set(PropertyIdentifier.trigger, Boolean.TRUE);
        doTrigger();
        return true;
    }

    @Override
    protected void afterWriteProperty(PropertyIdentifier pid, Encodable oldValue, Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);
        if (PropertyIdentifier.logDeviceObjectProperty.equals(pid)) {
            purge();
            updateMonitoredProperty();
        } else if (PropertyIdentifier.loggingType.equals(pid)) {
            updateLoggingType();
        } else if (pid.isOneOf(PropertyIdentifier.alignIntervals, PropertyIdentifier.intervalOffset)) {
            LoggingType loggingType = get(PropertyIdentifier.loggingType);
            if (loggingType.equals(LoggingType.polled)) {
                updateLoggingType();
            }
        } else if (PropertyIdentifier.trigger.equals(pid)) {
            // If the value has changed from false to true.
            if (((Boolean) newValue).booleanValue() && !((Boolean) oldValue).booleanValue()) {
                doTrigger();
            }
        }
    }

    protected void doTrigger() {
        // Perform the trigger asynchronously
        getLocalDevice().execute(() -> {
            try {
                doPoll();
                LOG.debug("Trigger complete");
            } finally {
                writePropertyInternal(PropertyIdentifier.trigger, Boolean.FALSE);
            }
        });
    }

    protected abstract void doPoll();

    @Override
    protected void terminateImpl() {
        super.terminateImpl();
        cancelFuture(pollingFuture);
    }
}
