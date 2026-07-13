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

package com.serotonin.bacnet4j.type.enumerated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Verifies that the BACnetEngineeringUnits enumeration is compliant with Addendum 135-2020cn
 * "Clarify Engineering Units".
 *
 * cn assigns enumerated values across two ranges: the traditional 0..254 block and the new
 * ASHRAE-reserved block 47808..47967. Within the reserved block, cn does not assign 47813 or 47957.
 */
public class EngineeringUnitsCnComplianceTest {
    // Reserved-range slots that cn leaves unassigned.
    private static final int RESERVED_MIN = 47808;
    private static final int RESERVED_MAX = 47967;
    private static final int[] RESERVED_GAPS = {47813, 47957};

    @Test
    public void allTraditionalRangeUnitsDefined() {
        for (int id = 0; id <= 254; id++) {
            assertNotNull("Missing engineering unit for id " + id, EngineeringUnits.nameForId(id));
        }
    }

    @Test
    public void allReservedRangeUnitsDefined() {
        for (int id = RESERVED_MIN; id <= RESERVED_MAX; id++) {
            if (isReservedGap(id))
                continue;
            assertNotNull("Missing engineering unit for id " + id, EngineeringUnits.nameForId(id));
        }
    }

    @Test
    public void reactiveHoursUnitsUseCnTerminology() {
        // cn reorders the reactive-energy units from "...-hours-reactive" to "...-reactive-hours".
        assertUnit(203, "watt-reactive-hours", EngineeringUnits.wattReactiveHours);
        assertUnit(204, "kilowatt-reactive-hours", EngineeringUnits.kilowattReactiveHours);
        assertUnit(205, "megawatt-reactive-hours", EngineeringUnits.megawattReactiveHours);
        assertUnit(242, "volt-ampere-reactive-hours", EngineeringUnits.voltAmpereReactiveHours);
        assertUnit(243, "kilovolt-ampere-reactive-hours", EngineeringUnits.kilovoltAmpereReactiveHours);
        assertUnit(244, "megavolt-ampere-reactive-hours", EngineeringUnits.megavoltAmpereReactiveHours);
    }

    @Test
    public void kelvinUnitsDropDegreePrefix() {
        // cn removes the word "degree" from all Kelvin units.
        assertUnit(63, "kelvin", EngineeringUnits.kelvin);
        assertUnit(121, "delta-kelvin", EngineeringUnits.deltaKelvin);
        assertUnit(127, "joules-per-kelvin", EngineeringUnits.joulesPerKelvin);
        assertUnit(128, "joules-per-kilogram-kelvin", EngineeringUnits.joulesPerKilogramKelvin);
        assertUnit(141, "watts-per-square-meter-per-kelvin", EngineeringUnits.wattsPerSquareMeterPerKelvin);
        assertUnit(151, "kilojoules-per-kelvin", EngineeringUnits.kilojoulesPerKelvin);
        assertUnit(152, "megajoules-per-kelvin", EngineeringUnits.megajoulesPerKelvin);
        assertUnit(176, "volts-per-kelvin", EngineeringUnits.voltsPerKelvin);
        assertUnit(181, "kelvin-per-hour", EngineeringUnits.kelvinPerHour);
        assertUnit(182, "kelvin-per-minute", EngineeringUnits.kelvinPerMinute);
        assertUnit(189, "watts-per-meter-per-kelvin", EngineeringUnits.wattsPerMeterPerKelvin);
        assertUnit(236, "minutes-per-kelvin", EngineeringUnits.minutesPerKelvin);
    }

    @Test
    public void joulesPerHourErratumApplied() {
        // cn corrects the "joule-per-hour" erratum to "joules-per-hour".
        assertUnit(247, "joules-per-hour", EngineeringUnits.joulesPerHour);
    }

    private static void assertUnit(int id, String name, EngineeringUnits expected) {
        assertEquals("Wrong id for " + name, expected, EngineeringUnits.forId(id));
        assertEquals("Wrong name for id " + id, name, EngineeringUnits.nameForId(id));
        assertEquals("forName mismatch for " + name, expected, EngineeringUnits.forName(name));
    }

    private static boolean isReservedGap(int id) {
        for (int gap : RESERVED_GAPS) {
            if (gap == id)
                return true;
        }
        return false;
    }
}
