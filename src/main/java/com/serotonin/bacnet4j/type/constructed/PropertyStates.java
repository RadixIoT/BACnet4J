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

package com.serotonin.bacnet4j.type.constructed;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.AccessCredentialDisable;
import com.serotonin.bacnet4j.type.enumerated.AccessCredentialDisableReason;
import com.serotonin.bacnet4j.type.enumerated.AccessEvent;
import com.serotonin.bacnet4j.type.enumerated.AccessZoneOccupancyState;
import com.serotonin.bacnet4j.type.enumerated.Action;
import com.serotonin.bacnet4j.type.enumerated.AuthenticationStatus;
import com.serotonin.bacnet4j.type.enumerated.BackupState;
import com.serotonin.bacnet4j.type.enumerated.BinaryLightingPV;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.DeviceStatus;
import com.serotonin.bacnet4j.type.enumerated.DoorAlarmState;
import com.serotonin.bacnet4j.type.enumerated.DoorSecuredStatus;
import com.serotonin.bacnet4j.type.enumerated.DoorStatus;
import com.serotonin.bacnet4j.type.enumerated.DoorValue;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.EscalatorFault;
import com.serotonin.bacnet4j.type.enumerated.EscalatorMode;
import com.serotonin.bacnet4j.type.enumerated.EscalatorOperationDirection;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.FileAccessMethod;
import com.serotonin.bacnet4j.type.enumerated.IPMode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyMode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
import com.serotonin.bacnet4j.type.enumerated.LiftCarDirection;
import com.serotonin.bacnet4j.type.enumerated.LiftCarDoorCommand;
import com.serotonin.bacnet4j.type.enumerated.LiftCarDriveStatus;
import com.serotonin.bacnet4j.type.enumerated.LiftCarMode;
import com.serotonin.bacnet4j.type.enumerated.LiftFault;
import com.serotonin.bacnet4j.type.enumerated.LiftGroupMode;
import com.serotonin.bacnet4j.type.enumerated.LightingInProgress;
import com.serotonin.bacnet4j.type.enumerated.LightingOperation;
import com.serotonin.bacnet4j.type.enumerated.LightingTransition;
import com.serotonin.bacnet4j.type.enumerated.LockStatus;
import com.serotonin.bacnet4j.type.enumerated.Maintenance;
import com.serotonin.bacnet4j.type.enumerated.NetworkNumberQuality;
import com.serotonin.bacnet4j.type.enumerated.NetworkPortCommand;
import com.serotonin.bacnet4j.type.enumerated.NetworkType;
import com.serotonin.bacnet4j.type.enumerated.NodeType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.enumerated.Polarity;
import com.serotonin.bacnet4j.type.enumerated.ProgramError;
import com.serotonin.bacnet4j.type.enumerated.ProgramRequest;
import com.serotonin.bacnet4j.type.enumerated.ProgramState;
import com.serotonin.bacnet4j.type.enumerated.ProtocolLevel;
import com.serotonin.bacnet4j.type.enumerated.Reliability;
import com.serotonin.bacnet4j.type.enumerated.RestartReason;
import com.serotonin.bacnet4j.type.enumerated.SecurityLevel;
import com.serotonin.bacnet4j.type.enumerated.ShedState;
import com.serotonin.bacnet4j.type.enumerated.SilencedState;
import com.serotonin.bacnet4j.type.enumerated.TimerState;
import com.serotonin.bacnet4j.type.enumerated.TimerTransition;
import com.serotonin.bacnet4j.type.enumerated.WriteStatus;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class PropertyStates extends BaseType {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, Boolean.class); // 0
        choiceOptions.addContextual(1, BinaryPV.class); // 1
        choiceOptions.addContextual(2, EventType.class); // 2
        choiceOptions.addContextual(3, Polarity.class); // 3
        choiceOptions.addContextual(4, ProgramRequest.class); // 4;
        choiceOptions.addContextual(5, ProgramState.class); // 5
        choiceOptions.addContextual(6, ProgramError.class); // 6
        choiceOptions.addContextual(7, Reliability.class); // 7
        choiceOptions.addContextual(8, EventState.class); // 8
        choiceOptions.addContextual(9, DeviceStatus.class); // 9
        choiceOptions.addContextual(10, EngineeringUnits.class); // 10
        choiceOptions.addContextual(11, UnsignedInteger.class); // 11
        choiceOptions.addContextual(12, LifeSafetyMode.class); // 12
        choiceOptions.addContextual(13, LifeSafetyState.class); // 13
        choiceOptions.addContextual(14, RestartReason.class); // 14
        choiceOptions.addContextual(15, DoorAlarmState.class); // 15
        choiceOptions.addContextual(16, Action.class); // 16
        choiceOptions.addContextual(17, DoorSecuredStatus.class); // 17
        choiceOptions.addContextual(18, DoorStatus.class); // 18
        choiceOptions.addContextual(19, DoorValue.class); // 19
        choiceOptions.addContextual(20, FileAccessMethod.class); // 20
        choiceOptions.addContextual(21, LockStatus.class); // 21
        choiceOptions.addContextual(22, LifeSafetyOperation.class); // 22
        choiceOptions.addContextual(23, Maintenance.class); // 23
        choiceOptions.addContextual(24, NodeType.class); // 24
        choiceOptions.addContextual(25, NotifyType.class); // 25
        choiceOptions.addContextual(26, SecurityLevel.class); // 26
        choiceOptions.addContextual(27, ShedState.class); // 27
        choiceOptions.addContextual(28, SilencedState.class); // 28
        choiceOptions.addContextual(30, AccessEvent.class); // 30
        choiceOptions.addContextual(31, AccessZoneOccupancyState.class); // 31
        choiceOptions.addContextual(32, AccessCredentialDisableReason.class); // 32
        choiceOptions.addContextual(33, AccessCredentialDisable.class); // 33
        choiceOptions.addContextual(34, AuthenticationStatus.class); // 34
        choiceOptions.addContextual(36, BackupState.class); // 36
        choiceOptions.addContextual(37, WriteStatus.class); // 37
        choiceOptions.addContextual(38, LightingInProgress.class); // 38
        choiceOptions.addContextual(39, LightingOperation.class); // 39
        choiceOptions.addContextual(40, LightingTransition.class); // 40
        choiceOptions.addContextual(41, SignedInteger.class); // 41
        choiceOptions.addContextual(42, BinaryLightingPV.class); // 42
        choiceOptions.addContextual(43, TimerState.class); // 43
        choiceOptions.addContextual(44, TimerTransition.class); // 44
        choiceOptions.addContextual(45, IPMode.class); // 45
        choiceOptions.addContextual(46, NetworkPortCommand.class); // 46
        choiceOptions.addContextual(47, NetworkType.class); // 47
        choiceOptions.addContextual(48, NetworkNumberQuality.class); // 48
        choiceOptions.addContextual(49, EscalatorOperationDirection.class); // 49
        choiceOptions.addContextual(50, EscalatorFault.class); // 50
        choiceOptions.addContextual(51, EscalatorMode.class); // 51
        choiceOptions.addContextual(52, LiftCarDirection.class); // 52
        choiceOptions.addContextual(53, LiftCarDoorCommand.class); // 53
        choiceOptions.addContextual(54, LiftCarDriveStatus.class); // 54
        choiceOptions.addContextual(55, LiftCarMode.class); // 55
        choiceOptions.addContextual(56, LiftGroupMode.class); // 56
        choiceOptions.addContextual(57, LiftFault.class);
        choiceOptions.addContextual(58, ProtocolLevel.class); // 58
        choiceOptions.addContextual(63, Unsigned32.class);
    }

    private final Choice state;

    public PropertyStates(final Encodable state) {
        this.state = new Choice(choiceOptions.getContextId(state.getClass(), false), state, choiceOptions);
    }

    @SuppressWarnings("unchecked")
    public <T extends Encodable> T getState() {
        return (T) state.getDatum();
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, state);
    }

    public PropertyStates(final ByteQueue queue) throws BACnetException {
        state = new Choice(queue, choiceOptions);
    }

    @Override
    public String toString() {
        return "PropertyStates [state=" + state + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (state == null ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final PropertyStates other = (PropertyStates) obj;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        return true;
    }
}
