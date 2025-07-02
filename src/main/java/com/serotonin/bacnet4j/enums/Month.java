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

package com.serotonin.bacnet4j.enums;

public enum Month {
    JANUARY(1), FEBRUARY(2), MARCH(3), APRIL(4), MAY(5), JUNE(6), JULY(7), AUGUST(8), SEPTEMBER(9), OCTOBER(
            10), NOVEMBER(
            11), DECEMBER(12), ODD_MONTHS(13), EVEN_MONTHS(14), UNSPECIFIED(255);

    private byte id;

    Month(int id) {
        this.id = (byte) id;
    }

    public byte getId() {
        return id;
    }

    public static Month valueOf(int id) {
        return valueOf((byte) id);
    }

    public boolean isSpecific() {
        switch (this) {
            case ODD_MONTHS:
            case EVEN_MONTHS:
            case UNSPECIFIED:
                return false;
            default:
                return true;
        }
    }

    public boolean isOdd() {
        switch (this) {
            case JANUARY:
            case MARCH:
            case MAY:
            case JULY:
            case SEPTEMBER:
            case NOVEMBER:
                return true;
            default:
                return false;
        }
    }

    public boolean isEven() {
        switch (this) {
            case FEBRUARY:
            case APRIL:
            case JUNE:
            case AUGUST:
            case OCTOBER:
            case DECEMBER:
                return true;
            default:
                return false;
        }
    }

    public boolean matches(Month that) {
        if (this == Month.UNSPECIFIED)
            return true;
        if (this == Month.ODD_MONTHS)
            return that.isOdd();
        if (this == Month.EVEN_MONTHS)
            return that.isEven();
        return this == that;
    }

    public static Month valueOf(byte id) {
        if (id == JANUARY.id)
            return JANUARY;
        if (id == FEBRUARY.id)
            return FEBRUARY;
        if (id == MARCH.id)
            return MARCH;
        if (id == APRIL.id)
            return APRIL;
        if (id == MAY.id)
            return MAY;
        if (id == JUNE.id)
            return JUNE;
        if (id == JULY.id)
            return JULY;
        if (id == AUGUST.id)
            return AUGUST;
        if (id == SEPTEMBER.id)
            return SEPTEMBER;
        if (id == OCTOBER.id)
            return OCTOBER;
        if (id == NOVEMBER.id)
            return NOVEMBER;
        if (id == DECEMBER.id)
            return DECEMBER;
        if (id == ODD_MONTHS.id)
            return ODD_MONTHS;
        if (id == EVEN_MONTHS.id)
            return EVEN_MONTHS;
        return UNSPECIFIED;
    }
}
