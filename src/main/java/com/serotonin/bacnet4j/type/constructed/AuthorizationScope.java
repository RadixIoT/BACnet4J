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

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.BitString;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthorizationScope extends BaseType {
    private final Standard standard;
    private final SequenceOf<CharacterString> extended;

    public AuthorizationScope(Standard standard, SequenceOf<CharacterString> extended) {
        this.standard = standard;
        this.extended = extended;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, standard);
        writeOptional(queue, extended, 0);
    }

    @Override
    public String toString() {
        return "AuthorizationScope [" +
                "standard=" + standard +
                ", extended=" + extended +
                ']';
    }

    public Standard getStandard() {
        return standard;
    }

    public SequenceOf<CharacterString> getExtended() {
        return extended;
    }

    public AuthorizationScope(final ByteQueue queue) throws BACnetException {
        standard = read(queue, Standard.class);
        extended = readOptionalSequenceOf(queue, CharacterString.class, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthorizationScope that = (AuthorizationScope) o;
        return Objects.equals(standard, that.standard) && Objects.equals(extended, that.extended);
    }

    @Override
    public int hashCode() {
        return Objects.hash(standard, extended);
    }

    public static class Standard extends BitString {
        public Standard() {
            super(new boolean[24]);
        }

        public Standard(
                boolean view,
                boolean adjust,
                boolean control,
                boolean override,
                boolean config,
                boolean bind,
                boolean install,
                boolean auth,
                boolean infrastructure,
                boolean reserved9,
                boolean reserved10,
                boolean reserved11,
                boolean reserved12,
                boolean reserved13,
                boolean reserved14,
                boolean reserved15,
                boolean reserved16,
                boolean reserved17,
                boolean reserved18,
                boolean reserved19,
                boolean reserved20,
                boolean reserved21,
                boolean reserved22,
                boolean reserved23
        ) {
            super(new boolean[] {
                    view,
                    adjust,
                    control,
                    override,
                    config,
                    bind,
                    install,
                    auth,
                    infrastructure,
                    reserved9,
                    reserved10,
                    reserved11,
                    reserved12,
                    reserved13,
                    reserved14,
                    reserved15,
                    reserved16,
                    reserved17,
                    reserved18,
                    reserved19,
                    reserved20,
                    reserved21,
                    reserved22,
                    reserved23
            });
        }

        public Standard(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        public boolean isView() {
            return getValue()[0];
        }

        public void setView(boolean b) {
            getValue()[0] = b;
        }

        public boolean isAdjust() {
            return getValue()[1];
        }

        public void setAdjust(boolean b) {
            getValue()[1] = b;
        }

        public boolean isControl() {
            return getValue()[2];
        }

        public void setControl(boolean b) {
            getValue()[2] = b;
        }

        public boolean isOverride() {
            return getValue()[3];
        }

        public void setOverride(boolean b) {
            getValue()[3] = b;
        }

        public boolean isConfig() {
            return getValue()[4];
        }

        public void setConfig(boolean b) {
            getValue()[4] = b;
        }

        public boolean isBind() {
            return getValue()[5];
        }

        public void setBind(boolean b) {
            getValue()[5] = b;
        }

        public boolean isInstall() {
            return getValue()[6];
        }

        public void setInstall(boolean b) {
            getValue()[6] = b;
        }

        public boolean isAuth() {
            return getValue()[7];
        }

        public void setAuth(boolean b) {
            getValue()[7] = b;
        }

        public boolean isInfrastructure() {
            return getValue()[8];
        }

        public void setInfrastructure(boolean b) {
            getValue()[8] = b;
        }

        public boolean isReserved9() {
            return getValue()[9];
        }

        public void setReserved9(boolean b) {
            getValue()[9] = b;
        }

        public boolean isReserved10() {
            return getValue()[10];
        }

        public void setReserved10(boolean b) {
            getValue()[10] = b;
        }

        public boolean isReserved11() {
            return getValue()[11];
        }

        public void setReserved11(boolean b) {
            getValue()[11] = b;
        }

        public boolean isReserved12() {
            return getValue()[12];
        }

        public void setReserved12(boolean b) {
            getValue()[12] = b;
        }

        public boolean isReserved13() {
            return getValue()[13];
        }

        public void setReserved13(boolean b) {
            getValue()[13] = b;
        }

        public boolean isReserved14() {
            return getValue()[14];
        }

        public void setReserved14(boolean b) {
            getValue()[14] = b;
        }

        public boolean isReserved15() {
            return getValue()[15];
        }

        public void setReserved15(boolean b) {
            getValue()[15] = b;
        }

        public boolean isReserved16() {
            return getValue()[16];
        }

        public void setReserved16(boolean b) {
            getValue()[16] = b;
        }

        public boolean isReserved17() {
            return getValue()[17];
        }

        public void setReserved17(boolean b) {
            getValue()[17] = b;
        }

        public boolean isReserved18() {
            return getValue()[18];
        }

        public void setReserved18(boolean b) {
            getValue()[18] = b;
        }

        public boolean isReserved19() {
            return getValue()[19];
        }

        public void setReserved19(boolean b) {
            getValue()[19] = b;
        }

        public void setReserved20(boolean b) {
            getValue()[20] = b;
        }

        public boolean isReserved20() {
            return getValue()[20];
        }

        public boolean isReserved21() {
            return getValue()[21];
        }

        public void setReserved21(boolean b) {
            getValue()[21] = b;
        }

        public boolean isReserved22() {
            return getValue()[22];
        }

        public void setReserved22(boolean b) {
            getValue()[22] = b;
        }

        public boolean isReserved23() {
            return getValue()[23];
        }

        public void setReserved23(boolean b) {
            getValue()[23] = b;
        }

        @Override
        public String toString() {
            return "Standard [" +
                    "view=" + isView() +
                    ", adjust=" + isAdjust() +
                    ", control=" + isControl() +
                    ", override=" + isOverride() +
                    ", config=" + isConfig() +
                    ", bind=" + isBind() +
                    ", install=" + isInstall() +
                    ", auth=" + isAuth() +
                    ", infrastructure=" + isInfrastructure() +
                    ", reserved-9=" + isReserved9() +
                    ", reserved-10=" + isReserved10() +
                    ", reserved-11=" + isReserved11() +
                    ", reserved-12=" + isReserved12() +
                    ", reserved-13=" + isReserved13() +
                    ", reserved-14=" + isReserved14() +
                    ", reserved-15=" + isReserved15() +
                    ", reserved-16=" + isReserved16() +
                    ", reserved-17=" + isReserved17() +
                    ", reserved-18=" + isReserved18() +
                    ", reserved-19=" + isReserved19() +
                    ", reserved-20=" + isReserved20() +
                    ", reserved-21=" + isReserved21() +
                    ", reserved-22=" + isReserved22() +
                    ", reserved-23=" + isReserved23() +
                    "]";
        }
    }
}
