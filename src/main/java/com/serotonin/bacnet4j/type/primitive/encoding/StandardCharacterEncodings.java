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

package com.serotonin.bacnet4j.type.primitive.encoding;

public class StandardCharacterEncodings {
    private StandardCharacterEncodings() {
    }

    public static final byte ANSI_X3_4 = 0;
    public static final byte IBM_MS_DBCS = 1;
    public static final byte JIS_C_6226 = 2;
    public static final byte ISO_10646_UCS_4 = 3;
    public static final byte ISO_10646_UCS_2 = 4;
    public static final byte ISO_8859_1 = 5;

    public static final int CODE_PAGE_LATIN_1 = 850;
    public static final int NO_CODE_PAGE = -1;
}
