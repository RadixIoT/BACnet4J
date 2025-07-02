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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.CalendarEntry;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Date;

public class CalendarObject extends BACnetObject {
    static final Logger LOG = LoggerFactory.getLogger(CalendarObject.class);

    // CreateObject constructor
    public static CalendarObject create(final LocalDevice localDevice, final int instanceNumber)
            throws BACnetServiceException {
        return new CalendarObject(localDevice, instanceNumber, ObjectType.calendar.toString() + " " + instanceNumber,
                new SequenceOf<>());
    }

    private int timeTolerance = 0;

    // This timer task keeps the present value up to date in case other objects have registered
    // for COV on it.
    private ScheduledFuture<?> presentValueRefresher;

    public CalendarObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final SequenceOf<CalendarEntry> dateList) throws BACnetServiceException {
        super(localDevice, ObjectType.calendar, instanceNumber, name);

        writePropertyInternal(PropertyIdentifier.dateList, dateList);
        updatePresentValue();

        addMixin(new CalendarMixin(this));

        // Schedule a timer task to run every hour. This way we don't need to worry
        // about daylight savings time changeovers.
        // Calculate the amount of time until the next hour.
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(localDevice.getClock().millis());
        final long elapsed = gc.get(Calendar.MILLISECOND) //
                + gc.get(Calendar.SECOND) * 1000 //
                + gc.get(Calendar.MINUTE) * 60 * 1000;
        final long hour = 1000 * 60 * 60;
        final long delay = hour - elapsed + 10; // Add a few milliseconds for fun.

        // Delay until the top of the next hour, and then run every hour.
        presentValueRefresher = getLocalDevice().scheduleAtFixedRate(() -> updatePresentValue(), delay, hour,
                TimeUnit.MILLISECONDS);

        localDevice.addObject(this);
    }

    public int getTimeTolerance() {
        return timeTolerance;
    }

    /**
     * To compensate for clock variances if the time is close to midnight, pretend that it is already the next day.
     * This protects against schedules on devices with clocks that are a bit ahead of ours, so that they get
     * the correct calendar value even if they ask for it a bit too early.
     */
    public void setTimeTolerance(final int timeTolerance) {
        this.timeTolerance = timeTolerance;
    }

    @Override
    protected void terminateImpl() {
        if (presentValueRefresher != null) {
            presentValueRefresher.cancel(false);
            presentValueRefresher = null;
        }
    }

    class Refresher extends TimerTask {
        @Override
        public void run() {
            updatePresentValue();
        }
    }


    class CalendarMixin extends AbstractMixin {
        public CalendarMixin(final BACnetObject bo) {
            super(bo);
        }

        @Override
        protected void beforeReadProperty(final PropertyIdentifier pid) {
            if (pid.equals(PropertyIdentifier.presentValue))
                // Ensure that the present value gets updated before the read is performed.
                // TODO it could make a bit of sense to only run this again after some timeout, in
                // case a date range check takes a long time.
                updatePresentValue();
        }

        @Override
        protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
                throws BACnetServiceException {
            if (PropertyIdentifier.presentValue.equals(value.getPropertyIdentifier()))
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
            return false;
        }

        @Override
        protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
                final Encodable newValue) {
            if (PropertyIdentifier.dateList.equals(pid))
                updatePresentValue();
        }
    }

    synchronized void updatePresentValue() {
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(getLocalDevice().getClock().millis());
        LOG.info("Updating present value with date {}", new java.util.Date(gc.getTimeInMillis()));

        if (timeTolerance > 0) {
            // And on the compensatory time.
            gc.add(Calendar.MILLISECOND, timeTolerance);
            LOG.info("Updating present value with date and tolerance {}", new java.util.Date(gc.getTimeInMillis()));
        }

        updatePresentValue(new Date(gc));
    }

    private void updatePresentValue(final Date date) {
        final SequenceOf<CalendarEntry> dateList = get(PropertyIdentifier.dateList);

        boolean match = false;
        for (final CalendarEntry e : dateList) {
            if (e.matches(date)) {
                match = true;
                break;
            }
        }

        writePropertyInternal(PropertyIdentifier.presentValue, Boolean.valueOf(match));
    }
}
