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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.serotonin.bacnet4j.apdu.ConfirmedRequest;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.enums.MaxSegments;
import com.serotonin.bacnet4j.npdu.NPCI;
import com.serotonin.bacnet4j.npdu.sc.SCVmac;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.StreamUtils;

public class SCBVLCTest {
    @Test
    public void testEncapsulatedNPDU_AB_2_17_Example1() {
        var rpr = new ReadPropertyRequest(new ObjectIdentifier(ObjectType.analogInput, 5),
                PropertyIdentifier.presentValue);
        var req =
                new ConfirmedRequest(false, false, false, MaxSegments.UNSPECIFIED, MaxApduLength.UP_TO_50, (byte) 1,
                        0, 33, rpr);
        var npci = new NPCI(null, null, req.expectsReply());

        var queue = new ByteQueue();
        npci.write(queue);
        req.write(queue);

        var msg = new SCBVLC(null, new SCVmac(StreamUtils.fromHex("927bf71a96a2")), SCBVLC.ENCAPSULATED_NPDU,
                queue.popAll(), 0xB5EC,
                List.of(
                        new SCOption(SCOption.TYPE_PROPRIETARY, false, StreamUtils.fromHex("022bbac5ecc099")),
                        new SCOption(SCOption.TYPE_PROPRIETARY, false, StreamUtils.fromHex("030939"))
                ),
                List.of(new SCOption(SCOption.TYPE_SECURE_PATH, false)));

        assertEquals("0107b5ec927bf71a96a2bf0007022bbac5ecc0993f00030309390101040000010c0c000000051955",
                StreamUtils.toHex(msg.write()));
    }

    @Test
    public void testEncapsulatedNPDU_AB_2_17_Example2() {
        var payload = new SCPayloadBVLCResult(SCBVLC.ENCAPSULATED_NPDU, 0xbf,
                ErrorClass.communication, ErrorCode.forId(273), "Unmöglicher Code!");
        var msg = new SCBVLC(new SCVmac(StreamUtils.fromHex("927bf71a96a2")), null, SCBVLC.BVLC_RESULT,
                payload.write(), 0xB5EC);

        assertEquals("0008b5ec927bf71a96a20101bf00070111556e6dc3b6676c696368657220436f646521",
                StreamUtils.toHex(msg.write()));
    }

    @Test
    public void testEncapsulatedNPDU_AB_2_17_Example3() {
        var payload = new SCPayloadBVLCResult(SCBVLC.ENCAPSULATED_NPDU, 0x3f,
                ErrorClass.communication, ErrorCode.forId(279), null);
        var msg = new SCBVLC(new SCVmac(StreamUtils.fromHex("927bf71a96a2")), null, SCBVLC.BVLC_RESULT,
                payload.write(), 0xB5EC);

        assertEquals("0008b5ec927bf71a96a201013f00070117",
                StreamUtils.toHex(msg.write()));
    }
}
