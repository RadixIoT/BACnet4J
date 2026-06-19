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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.enums.DayOfWeek;
import com.serotonin.bacnet4j.enums.Month;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.SCConnectionState;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class SCHubConnectionTest {
    private static DateTime ts(int h) {
        return new DateTime(new Date(2026, Month.JUNE, 12, DayOfWeek.FRIDAY), new Time(h, 0, 0, 0));
    }

    @Test
    public void allFields() throws BACnetException {
        final SCHubConnection c = new SCHubConnection(
                SCConnectionState.disconnectedWithErrors,
                ts(8),
                ts(10),
                new ErrorClassAndCode(ErrorClass.communication, ErrorCode.communicationDisabled),
                new CharacterString("link reset"));

        final ByteQueue queue = new ByteQueue();
        c.write(queue);

        assertEquals(c, new SCHubConnection(queue));
    }

    @Test
    public void optionalsAbsent() throws BACnetException {
        final SCHubConnection c = new SCHubConnection(
                SCConnectionState.connected,
                ts(8),
                ts(10));

        final ByteQueue queue = new ByteQueue();
        c.write(queue);

        assertEquals(c, new SCHubConnection(queue));
    }
}
