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

package com.serotonin.bacnet4j.service.acknowledgement;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult.Result;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.type.primitive.encoding.CharacterEncoding;
import com.serotonin.bacnet4j.type.primitive.encoding.StandardCharacterEncodings;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Test to show that reading a protocol version on a device will throw an exception and fail if it is not the correct
 * data type.
 *
 * It was found that in the previous version of BACnet4J that we could read invalid values without failing.  This test
 * is just to show that it no longer works.
 *
 * @author Terry Packer
 */
public class InvalidPropertyValueTest {

    @Test
    public void testParse() {

        List<Result> deviceResultList = new ArrayList<>();

        //Error reading a property
        ErrorClassAndCode errorResponse = new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty);
        UnsignedInteger propertyArrayIndex = new UnsignedInteger(0);
        deviceResultList.add(new Result(PropertyIdentifier.maxSegmentsAccepted, propertyArrayIndex, errorResponse));

        //String protocol version which is Invalid as per the spec
        deviceResultList.add(new Result(PropertyIdentifier.protocolVersion, new UnsignedInteger(0),
                new CharacterString(new CharacterEncoding(StandardCharacterEncodings.ANSI_X3_4), "hxzy-1.01")));

        SequenceOf<Result> deviceResults = new SequenceOf<>(deviceResultList);

        List<ReadAccessResult> resultList = new ArrayList<>();
        resultList.add(new ReadAccessResult(new ObjectIdentifier(ObjectType.device, 1003), deviceResults));
        SequenceOf<ReadAccessResult> results = new SequenceOf<>(resultList);
        ReadPropertyMultipleAck mockAck = new ReadPropertyMultipleAck(results);

        ByteQueue queue = new ByteQueue();
        mockAck.write(queue);

        try {
            new ReadPropertyMultipleAck(queue);
        } catch (BACnetException e) {
            fail(e.getMessage());
        }

    }

}
