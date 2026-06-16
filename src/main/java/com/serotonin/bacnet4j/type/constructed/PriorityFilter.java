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

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class PriorityFilter extends BitString {
    public PriorityFilter() {
        this(new boolean[16]);
    }

    // For creation from a choice
    PriorityFilter(final boolean[] value) {
        super(value);
        if (value.length != 16) {
            throw new IllegalArgumentException();
        }
    }

    public PriorityFilter(
            boolean manualLifeSafety,
            boolean automaticLifeSafety,
            boolean priority3,
            boolean priority4,
            boolean criticalEquipmentControls,
            boolean minimumOnOff,
            boolean priority7,
            boolean manualOperator,
            boolean priority9,
            boolean priority10,
            boolean priority11,
            boolean priority12,
            boolean priority13,
            boolean priority14,
            boolean priority15,
            boolean priority16
    ) {
        this(new boolean[] {
                manualLifeSafety,
                automaticLifeSafety,
                priority3,
                priority4,
                criticalEquipmentControls,
                minimumOnOff,
                priority7,
                manualOperator,
                priority9,
                priority10,
                priority11,
                priority12,
                priority13,
                priority14,
                priority15,
                priority16,
        });
    }

    public PriorityFilter(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean isManualLifeSafety() {
        return getValue()[0];
    }

    public void setManualLifeSafety(boolean b) {
        getValue()[0] = b;
    }

    public boolean isAutomaticLifeSafety() {
        return getValue()[1];
    }

    public void setAutomaticLifeSafety(boolean b) {
        getValue()[1] = b;
    }

    public boolean isPriority3() {
        return getValue()[2];
    }

    public void setPriority3(boolean b) {
        getValue()[2] = b;
    }

    public boolean isPriority4() {
        return getValue()[3];
    }

    public void setPriority4(boolean b) {
        getValue()[3] = b;
    }

    public boolean isCriticalEquipmentControls() {
        return getValue()[4];
    }

    public void setCriticalEquipmentControls(boolean b) {
        getValue()[4] = b;
    }

    public boolean isMinimumOnOff() {
        return getValue()[5];
    }

    public void setMinimumOnOff(boolean b) {
        getValue()[5] = b;
    }

    public boolean isPriority7() {
        return getValue()[6];
    }

    public void setPriority7(boolean b) {
        getValue()[6] = b;
    }

    public boolean isManualOperator() {
        return getValue()[7];
    }

    public void setManualOperator(boolean b) {
        getValue()[7] = b;
    }

    public boolean isPriority9() {
        return getValue()[8];
    }

    public void setPriority9(boolean b) {
        getValue()[8] = b;
    }

    public boolean isPriority10() {
        return getValue()[9];
    }

    public void setPriority10(boolean b) {
        getValue()[9] = b;
    }

    public boolean isPriority11() {
        return getValue()[10];
    }

    public void setPriority11(boolean b) {
        getValue()[10] = b;
    }

    public boolean isPriority12() {
        return getValue()[11];
    }

    public void setPriority12(boolean b) {
        getValue()[11] = b;
    }

    public boolean isPriority13() {
        return getValue()[12];
    }

    public void setPriority13(boolean b) {
        getValue()[12] = b;
    }

    public boolean isPriority14() {
        return getValue()[13];
    }

    public void setPriority14(boolean b) {
        getValue()[13] = b;
    }

    public boolean isPriority15() {
        return getValue()[14];
    }

    public void setPriority15(boolean b) {
        getValue()[14] = b;
    }

    public boolean isPriority16() {
        return getValue()[15];
    }

    public void setPriority16(boolean b) {
        getValue()[15] = b;
    }

    @Override
    public String toString() {
        return "PriorityFilter [" +
                "manual-life-safety=" + isManualLifeSafety() +
                ", automatic-life-safety=" + isAutomaticLifeSafety() +
                ", priority-3=" + isPriority3() +
                ", priority-4=" + isPriority4() +
                ", critical-equipment-controls=" + isCriticalEquipmentControls() +
                ", minimum-on-off=" + isMinimumOnOff() +
                ", priority-7=" + isPriority7() +
                ", manual-operator=" + isManualOperator() +
                ", priority-9=" + isPriority9() +
                ", priority-10=" + isPriority10() +
                ", priority-11=" + isPriority11() +
                ", priority-12=" + isPriority12() +
                ", priority-13=" + isPriority13() +
                ", priority-14=" + isPriority14() +
                ", priority-15=" + isPriority15() +
                ", priority-16=" + isPriority16() +
                "]";
    }
}
