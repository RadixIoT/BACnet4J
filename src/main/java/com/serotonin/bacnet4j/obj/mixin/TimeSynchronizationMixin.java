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

import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.unconfirmed.TimeSynchronizationRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UTCTimeSynchronizationRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class TimeSynchronizationMixin extends AbstractMixin {
    static final Logger LOG = LoggerFactory.getLogger(TimeSynchronizationMixin.class);

    private ScheduledFuture<?> timeSyncTask;

    public TimeSynchronizationMixin(final BACnetObject owner, final SequenceOf<Recipient> timeSynchronizationRecipients,
            final SequenceOf<Recipient> utcTimeSynchronizationRecipients, final int timeSynchronizationInterval,
            final boolean alignIntervals, final int intervalOffset) {
        super(owner);

        set(PropertyIdentifier.timeSynchronizationRecipients, timeSynchronizationRecipients);
        set(PropertyIdentifier.utcTimeSynchronizationRecipients, utcTimeSynchronizationRecipients);
        set(PropertyIdentifier.timeSynchronizationInterval, new UnsignedInteger(timeSynchronizationInterval));
        set(PropertyIdentifier.alignIntervals, Boolean.valueOf(alignIntervals));
        set(PropertyIdentifier.intervalOffset, new UnsignedInteger(intervalOffset));
    }

    public void update() {
        final UnsignedInteger timeSynchronizationInterval = get(PropertyIdentifier.timeSynchronizationInterval);
        final Boolean alignIntervals = get(PropertyIdentifier.alignIntervals);
        final UnsignedInteger intervalOffset = get(PropertyIdentifier.intervalOffset);

        if (timeSyncTask != null) {
            timeSyncTask.cancel(false);
        }

        if (timeSynchronizationInterval.intValue() > 0) {
            // Convert from minutes to millis
            final long period = timeSynchronizationInterval.intValue() * 60 * 1000;
            long initialDelay = period;
            int offsetToUse = 0;
            if (alignIntervals.booleanValue()) {
                final long now = getLocalDevice().getClock().millis();

                // Find the largest time period to which the period aligns.
                if (period % TimeUnit.DAYS.toMinutes(1) == 0) {
                    initialDelay = TimeUnit.DAYS.toMillis(1) - now % TimeUnit.DAYS.toMillis(1);
                } else if (period % TimeUnit.HOURS.toMillis(1) == 0) {
                    initialDelay = TimeUnit.HOURS.toMillis(1) - now % TimeUnit.HOURS.toMillis(1);
                }

                offsetToUse = intervalOffset.intValue() * 60 * 1000;
                offsetToUse %= period;
            }

            initialDelay += offsetToUse;
            initialDelay %= period;

            timeSyncTask = getLocalDevice().scheduleAtFixedRate(() -> {
                // Send the time sync messages.
                final SequenceOf<Recipient> timeSynchronizationRecipients = get(
                        PropertyIdentifier.timeSynchronizationRecipients);
                final SequenceOf<Recipient> utcTimeSynchronizationRecipients = get(
                        PropertyIdentifier.utcTimeSynchronizationRecipients);

                final long nowMillis = getLocalDevice().getClock().millis();

                final DateTime now = new DateTime(nowMillis);
                for (final Recipient recipient : timeSynchronizationRecipients) {
                    Address address;
                    try {
                        address = recipient.toAddress(getLocalDevice());
                    } catch (final BACnetException e) {
                        LOG.warn("Unable to get address for recipient {}", recipient, e);
                        continue;
                    }
                    getLocalDevice().send(address, new TimeSynchronizationRequest(now));
                }

                final GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                gc.setTimeInMillis(nowMillis);
                final DateTime utcNow = new DateTime(gc);
                for (final Recipient recipient : utcTimeSynchronizationRecipients) {
                    Address address;
                    try {
                        address = recipient.toAddress(getLocalDevice());
                    } catch (final BACnetException e) {
                        LOG.warn("Unable to get address for recipient {}", recipient, e);
                        continue;
                    }
                    getLocalDevice().send(address, new UTCTimeSynchronizationRequest(utcNow));
                }
            }, initialDelay, period, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (pid.isOneOf(PropertyIdentifier.timeSynchronizationInterval, PropertyIdentifier.alignIntervals,
                PropertyIdentifier.intervalOffset)) {
            update();
        }
    }

    @Override
    protected void terminate() {
        if (timeSyncTask != null) {
            timeSyncTask.cancel(false);
        }
    }
}
