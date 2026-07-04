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

package com.serotonin.bacnet4j.service.confirmed;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.event.ReinitializeDeviceHandler;
import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class ReinitializeDeviceRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 20;

    private final ReinitializedStateOfDevice reinitializedStateOfDevice;
    private final CharacterString password;

    public ReinitializeDeviceRequest(ReinitializedStateOfDevice reinitializedStateOfDevice,
            CharacterString password) {
        this.reinitializedStateOfDevice = reinitializedStateOfDevice;
        this.password = password;
    }

    public ReinitializeDeviceRequest(ReinitializedStateOfDevice reinitializedStateOfDevice) {
        this(reinitializedStateOfDevice, null);
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, reinitializedStateOfDevice, 0);
        writeOptional(queue, password, 1);
    }

    ReinitializeDeviceRequest(ByteQueue queue) throws BACnetException {
        reinitializedStateOfDevice = read(queue, ReinitializedStateOfDevice.class, 0);
        password = readOptional(queue, CharacterString.class, 1);
    }

    @Override
    public AcknowledgementService handle(LocalDevice localDevice, Address from) throws BACnetException {
        ReinitializeDeviceHandler handler = localDevice.getReinitializeDeviceHandler();
        if (handler == null) {
            throw new BACnetErrorException(ErrorClass.device, ErrorCode.notConfigured);
        }

        // Performance of warmstart and coldstart are listed before the password check in 16.4.2, but we'll check
        // the password here anyway.
        String givenPassword = null;
        if (password != null) {
            givenPassword = password.getValue();
        }
        if (!Objects.equals(givenPassword, localDevice.getPassword())) {
            throw new BACnetErrorException(getChoiceId(), ErrorClass.security, ErrorCode.passwordFailure);
        }

        handler.handle(localDevice, from, reinitializedStateOfDevice);

        return null;
    }

    @Override
    public boolean isCommunicationControlOverride() {
        return true;
    }

    public static class ReinitializedStateOfDevice extends Enumerated {
        public static final ReinitializedStateOfDevice coldstart = new ReinitializedStateOfDevice(0);
        public static final ReinitializedStateOfDevice warmstart = new ReinitializedStateOfDevice(1);
        public static final ReinitializedStateOfDevice startBackup = new ReinitializedStateOfDevice(2);
        public static final ReinitializedStateOfDevice endBackup = new ReinitializedStateOfDevice(3);
        public static final ReinitializedStateOfDevice startRestore = new ReinitializedStateOfDevice(4);
        public static final ReinitializedStateOfDevice endRestore = new ReinitializedStateOfDevice(5);
        public static final ReinitializedStateOfDevice abortRestore = new ReinitializedStateOfDevice(6);
        public static final ReinitializedStateOfDevice activateChanges = new ReinitializedStateOfDevice(7);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        static {
            Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        }

        public static ReinitializedStateOfDevice forId(int id) {
            ReinitializedStateOfDevice e = (ReinitializedStateOfDevice) idMap.get(id);
            if (e == null)
                e = new ReinitializedStateOfDevice(id);
            return e;
        }

        public static String nameForId(int id) {
            return prettyMap.get(id);
        }

        public static ReinitializedStateOfDevice forName(String name) {
            return (ReinitializedStateOfDevice) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private ReinitializedStateOfDevice(int value) {
            super(value);
        }

        public ReinitializedStateOfDevice(ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (password == null ? 0 : password.hashCode());
        result = prime * result + (reinitializedStateOfDevice == null ? 0 : reinitializedStateOfDevice.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReinitializeDeviceRequest other = (ReinitializeDeviceRequest) obj;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (reinitializedStateOfDevice == null) {
            if (other.reinitializedStateOfDevice != null)
                return false;
        } else if (!reinitializedStateOfDevice.equals(other.reinitializedStateOfDevice))
            return false;
        return true;
    }
}
