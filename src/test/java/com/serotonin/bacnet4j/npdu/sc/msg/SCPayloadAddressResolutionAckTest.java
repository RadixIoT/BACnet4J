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

package com.serotonin.bacnet4j.npdu.sc.msg;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class SCPayloadAddressResolutionAckTest {

    /**
     * Zero-byte payload means "supports direct connections but no URIs currently known." (AB.2.7). Verifies the
     * empty-payload guard in the constructor (previously a NPE risk on write()).
     */
    @Test
    public void empty_writeReturnsEmpty() {
        var ack = new SCPayloadAddressResolutionAck(new byte[0]);
        assertArrayEquals(new byte[0], ack.write());
    }

    /**
     * Payload with a single URL should round-trip identically.
     */
    @Test
    public void singleUrl_roundTrip() {
        var input = "wss://node.example.com:4443".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var ack = new SCPayloadAddressResolutionAck(input);
        assertArrayEquals(input, ack.write());
    }

    /**
     * Payload with multiple space-separated URLs (per AB.2.7) should round-trip identically.
     */
    @Test
    public void multipleUrls_roundTrip() {
        var input = "wss://a.example.com:4443 wss://b.example.com:4443 wss://c.example.com:4443"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var ack = new SCPayloadAddressResolutionAck(input);
        assertArrayEquals(input, ack.write());
    }
}
