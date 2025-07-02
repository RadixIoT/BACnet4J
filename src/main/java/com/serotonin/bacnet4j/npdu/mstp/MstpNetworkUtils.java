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

package com.serotonin.bacnet4j.npdu.mstp;

import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;

public class MstpNetworkUtils {
    public static OctetString toOctetString(byte station) {
        return new OctetString(new byte[] {station});
    }

    public static byte getMstpAddress(OctetString mac) {
        return mac.getBytes()[0];
    }

    public static String toString(OctetString mac) {
        return Integer.toString(getMstpAddress(mac) & 0xff);
    }

    public static Address toAddress(byte station) {
        return new Address(toOctetString(station));
    }

    public static Address toAddress(int networkNumber, byte station) {
        return new Address(networkNumber, toOctetString(station));
    }

    public static Address toAddress(int station) {
        return new Address(toOctetString((byte) station));
    }

    public static Address toAddress(int networkNumber, int station) {
        return new Address(networkNumber, toOctetString((byte) station));
    }
}
