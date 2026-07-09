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

package com.serotonin.bacnet4j.service.acknowledgement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

/**
 * Verifies that BACnet4J can parse a ReadPropertyMultiple acknowledgement from a non-conformant LG Smart 5
 * device (device 1124) that encodes "no value" as an opening context tag [4] immediately followed by its
 * closing context tag [4], with nothing between them. ASHRAE 135 requires propertyAccessError [5] in that
 * case, but we handle the empty value leniently rather than throwing character-set-not-supported '31' and
 * aborting the entire response.
 * The bytes used here were captured in LG1124-NotConnect.pcapng, reassembled from the four segments of the
 * complex-ack for the readPropertyMultiple request with invoke id 54 (the second half of the analog-input
 * object-name/description scan). The failing ReadAccessResult is at offset 2786, for analog-input 197457 /
 * property description, where the device replies with {[4] }[4] (empty).
 */
public class LG1124EmptyPropertyValueTest {
    private static final String FIXTURE = "/pcap/LG1124-NotConnect_invoke54_rpm_ack.hex";

    /**
     * End-to-end fixture: full RPM ack service data captured from the LG device. With the lenient fix, the
     * whole response parses to completion; the offending empty property comes back as Null.
     */
    @Test
    public void parsingCapturedLgResponseSucceeds() throws IOException, BACnetException {
        var queue = new ByteQueue(loadHexFixture());
        var ack = new ReadPropertyMultipleAck(queue);
        // Result 43 contains there error
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.valueNotInitialized),
                ack.getListOfReadAccessResults().get(43).getListOfResults().get(0).getReadResult().getDatum());

    }

    /**
     * Minimal repro: a single RPM ack containing analog-input 197458 whose object-name property is returned
     * as an empty constructed value {[4] }[4]. Parsing must succeed and surface the empty value as Null.
     */
    @Test
    public void emptyPropertyValueDecodesAsNull() throws BACnetException {
        // 0c 00 03 01 52   context[0] len 4, object id = analog-input 197458
        // 1e               context[1] opening  -- listOfResults begins
        // 29 4d            context[2] len 1, propertyIdentifier = 77 (object-name)
        // 4e               context[4] opening  -- readResult begins
        // 4f               context[4] closing  -- readResult ends (empty value)
        // 1f               context[1] closing  -- listOfResults ends
        var queue = new ByteQueue("0c000301521e294d4e4f1f");
        var ack = new ReadPropertyMultipleAck(queue);

        assertEquals(1, ack.getListOfReadAccessResults().getCount());
        var rar = ack.getListOfReadAccessResults().getBase1(1);
        assertEquals(1, rar.getListOfResults().getCount());
        var result = rar.getListOfResults().getBase1(1);
        assertEquals(PropertyIdentifier.objectName, result.getPropertyIdentifier());
        assertNotNull(result.getReadResult());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.valueNotInitialized),
                result.getReadResult().getDatum());
    }

    private static byte[] loadHexFixture() throws IOException {
        try (InputStream in = LG1124EmptyPropertyValueTest.class.getResourceAsStream(FIXTURE)) {
            if (in == null)
                throw new IOException("Fixture not found: " + FIXTURE);
            var raw = in.readAllBytes();
            return StreamUtils.fromHex(new String(raw, StandardCharsets.US_ASCII).trim());
        }
    }
}