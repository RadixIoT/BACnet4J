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

package com.serotonin.bacnet4j.type.constructed;

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuditLogRecord extends BaseType {
    private static final ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, LogStatus.class);
        choiceOptions.addContextual(1, AuditNotification.class);
        choiceOptions.addContextual(2, Real.class);
    }

    private final DateTime timestamp;
    private final Choice logDatum;

    private AuditLogRecord(DateTime timestamp, Choice logDatum) {
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(logDatum);

        this.timestamp = timestamp;
        this.logDatum = logDatum;
    }

    public AuditLogRecord(DateTime timestamp, LogStatus logStatus) {
        this(timestamp, new Choice(0, logStatus, choiceOptions));
    }

    public AuditLogRecord(DateTime timestamp, AuditNotification auditNotification) {
        this(timestamp, new Choice(1, auditNotification, choiceOptions));
    }

    public AuditLogRecord(DateTime timestamp, Real timeChange) {
        this(timestamp, new Choice(2, timeChange, choiceOptions));
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timestamp, 0);
        write(queue, logDatum, 1);
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public boolean isLogStatus() {
        return logDatum.getContextId() == 0;
    }

    public boolean isAuditNotification() {
        return logDatum.getContextId() == 1;
    }

    public boolean isReal() {
        return logDatum.getContextId() == 2;
    }

    public <T extends Encodable> T getChoice() {
        return logDatum.getDatum();
    }

    public AuditLogRecord(final ByteQueue queue) throws BACnetException {
        timestamp = read(queue, DateTime.class, 0);
        logDatum = new Choice(queue, choiceOptions, 1);
    }

    @Override
    public String toString() {
        return "AuditLogRecord [timestamp=" + timestamp + ", choice=" + logDatum + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuditLogRecord that = (AuditLogRecord) o;
        return Objects.equals(timestamp, that.timestamp) && Objects.equals(logDatum, that.logDatum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, logDatum);
    }
}
