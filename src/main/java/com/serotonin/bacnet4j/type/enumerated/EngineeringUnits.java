/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Infinite Automation Software,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.infiniteautomation.com for commercial license options.
 *
 * @author Matthew Lohbihler
 *//*
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

package com.serotonin.bacnet4j.type.enumerated;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class EngineeringUnits extends Enumerated {
    // Acceleration
    public static final EngineeringUnits metersPerSecondPerSecond = new EngineeringUnits(166);
    // Area
    public static final EngineeringUnits squareMeters = new EngineeringUnits(0);
    public static final EngineeringUnits squareCentimeters = new EngineeringUnits(116);
    public static final EngineeringUnits squareFeet = new EngineeringUnits(1);
    public static final EngineeringUnits squareInches = new EngineeringUnits(115);
    // Currency
    public static final EngineeringUnits currency1 = new EngineeringUnits(105);
    public static final EngineeringUnits currency2 = new EngineeringUnits(106);
    public static final EngineeringUnits currency3 = new EngineeringUnits(107);
    public static final EngineeringUnits currency4 = new EngineeringUnits(108);
    public static final EngineeringUnits currency5 = new EngineeringUnits(109);
    public static final EngineeringUnits currency6 = new EngineeringUnits(110);
    public static final EngineeringUnits currency7 = new EngineeringUnits(111);
    public static final EngineeringUnits currency8 = new EngineeringUnits(112);
    public static final EngineeringUnits currency9 = new EngineeringUnits(113);
    public static final EngineeringUnits currency10 = new EngineeringUnits(114);
    // Electrical
    public static final EngineeringUnits milliamperes = new EngineeringUnits(2);
    public static final EngineeringUnits amperes = new EngineeringUnits(3);
    public static final EngineeringUnits amperesPerMeter = new EngineeringUnits(167);
    public static final EngineeringUnits amperesPerSquareMeter = new EngineeringUnits(168);
    public static final EngineeringUnits ampereSquareMeters = new EngineeringUnits(169);
    public static final EngineeringUnits decibels = new EngineeringUnits(199);
    public static final EngineeringUnits decibelsMillivolt = new EngineeringUnits(200);
    public static final EngineeringUnits decibelsVolt = new EngineeringUnits(201);
    public static final EngineeringUnits farads = new EngineeringUnits(170);
    public static final EngineeringUnits henrys = new EngineeringUnits(171);
    public static final EngineeringUnits ohms = new EngineeringUnits(4);
    public static final EngineeringUnits ohmMeterSquaredPerMeter = new EngineeringUnits(237);
    public static final EngineeringUnits ohmMeters = new EngineeringUnits(172);
    public static final EngineeringUnits milliohms = new EngineeringUnits(145);
    public static final EngineeringUnits kilohms = new EngineeringUnits(122);
    public static final EngineeringUnits megohms = new EngineeringUnits(123);
    public static final EngineeringUnits microsiemens = new EngineeringUnits(190);
    public static final EngineeringUnits millisiemens = new EngineeringUnits(202);
    public static final EngineeringUnits siemens = new EngineeringUnits(173); // 1 mho equals 1 siemens
    public static final EngineeringUnits siemensPerMeter = new EngineeringUnits(174);
    public static final EngineeringUnits teslas = new EngineeringUnits(175);
    public static final EngineeringUnits volts = new EngineeringUnits(5);
    public static final EngineeringUnits millivolts = new EngineeringUnits(124);
    public static final EngineeringUnits kilovolts = new EngineeringUnits(6);
    public static final EngineeringUnits megavolts = new EngineeringUnits(7);
    public static final EngineeringUnits voltAmperes = new EngineeringUnits(8);
    public static final EngineeringUnits kilovoltAmperes = new EngineeringUnits(9);
    public static final EngineeringUnits megavoltAmperes = new EngineeringUnits(10);
    public static final EngineeringUnits voltAmperesReactive = new EngineeringUnits(11);
    public static final EngineeringUnits kilovoltAmperesReactive = new EngineeringUnits(12);
    public static final EngineeringUnits megavoltAmperesReactive = new EngineeringUnits(13);
    public static final EngineeringUnits voltsPerDegreeKelvin = new EngineeringUnits(176);
    public static final EngineeringUnits voltsPerMeter = new EngineeringUnits(177);
    public static final EngineeringUnits degreesPhase = new EngineeringUnits(14);
    public static final EngineeringUnits powerFactor = new EngineeringUnits(15);
    public static final EngineeringUnits webers = new EngineeringUnits(178);
    // Energy
    public static final EngineeringUnits ampereSeconds = new EngineeringUnits(238);
    public static final EngineeringUnits voltAmpereHours = new EngineeringUnits(239);
    public static final EngineeringUnits kilovoltAmpereHours = new EngineeringUnits(240);
    public static final EngineeringUnits megavoltAmpereHours = new EngineeringUnits(241);
    public static final EngineeringUnits voltAmpereHoursReactive = new EngineeringUnits(242);
    public static final EngineeringUnits kilovoltAmpereHoursReactive = new EngineeringUnits(243);
    public static final EngineeringUnits megavoltAmpereHoursReactive = new EngineeringUnits(244);
    public static final EngineeringUnits voltSquareHours = new EngineeringUnits(245);
    public static final EngineeringUnits ampereSquareHours = new EngineeringUnits(246);
    public static final EngineeringUnits joules = new EngineeringUnits(16);
    public static final EngineeringUnits kilojoules = new EngineeringUnits(17);
    public static final EngineeringUnits kilojoulesPerKilogram = new EngineeringUnits(125);
    public static final EngineeringUnits megajoules = new EngineeringUnits(126);
    public static final EngineeringUnits wattHours = new EngineeringUnits(18);
    public static final EngineeringUnits kilowattHours = new EngineeringUnits(19);
    public static final EngineeringUnits megawattHours = new EngineeringUnits(146);
    public static final EngineeringUnits wattHoursReactive = new EngineeringUnits(203);
    public static final EngineeringUnits kilowattHoursReactive = new EngineeringUnits(204);
    public static final EngineeringUnits megawattHoursReactive = new EngineeringUnits(205);
    public static final EngineeringUnits btus = new EngineeringUnits(20);
    public static final EngineeringUnits kiloBtus = new EngineeringUnits(147);
    public static final EngineeringUnits megaBtus = new EngineeringUnits(148);
    public static final EngineeringUnits therms = new EngineeringUnits(21);
    public static final EngineeringUnits tonHours = new EngineeringUnits(22);
    // Enthalpy
    public static final EngineeringUnits joulesPerKilogramDryAir = new EngineeringUnits(23);
    public static final EngineeringUnits kilojoulesPerKilogramDryAir = new EngineeringUnits(149);
    public static final EngineeringUnits megajoulesPerKilogramDryAir = new EngineeringUnits(150);
    public static final EngineeringUnits btusPerPoundDryAir = new EngineeringUnits(24);
    public static final EngineeringUnits btusPerPound = new EngineeringUnits(117);
    // Entropy
    public static final EngineeringUnits joulesPerDegreeKelvin = new EngineeringUnits(127);
    public static final EngineeringUnits kilojoulesPerDegreeKelvin = new EngineeringUnits(151);
    public static final EngineeringUnits megajoulesPerDegreeKelvin = new EngineeringUnits(152);
    public static final EngineeringUnits joulesPerKilogramDegreeKelvin = new EngineeringUnits(128);
    // Force
    public static final EngineeringUnits newton = new EngineeringUnits(153);
    // Frequency
    public static final EngineeringUnits cyclesPerHour = new EngineeringUnits(25);
    public static final EngineeringUnits cyclesPerMinute = new EngineeringUnits(26);
    public static final EngineeringUnits hertz = new EngineeringUnits(27);
    public static final EngineeringUnits kilohertz = new EngineeringUnits(129);
    public static final EngineeringUnits megahertz = new EngineeringUnits(130);
    public static final EngineeringUnits perHour = new EngineeringUnits(131);
    // Humidity
    public static final EngineeringUnits gramsOfWaterPerKilogramDryAir = new EngineeringUnits(28);
    public static final EngineeringUnits percentRelativeHumidity = new EngineeringUnits(29);
    // Length
    public static final EngineeringUnits micrometers = new EngineeringUnits(194);
    public static final EngineeringUnits millimeters = new EngineeringUnits(30);
    public static final EngineeringUnits centimeters = new EngineeringUnits(118);
    public static final EngineeringUnits kilometers = new EngineeringUnits(193);
    public static final EngineeringUnits meters = new EngineeringUnits(31);
    public static final EngineeringUnits inches = new EngineeringUnits(32);
    public static final EngineeringUnits feet = new EngineeringUnits(33);
    // Light
    public static final EngineeringUnits candelas = new EngineeringUnits(179);
    public static final EngineeringUnits candelasPerSquareMeter = new EngineeringUnits(180);
    public static final EngineeringUnits wattsPerSquareFoot = new EngineeringUnits(34);
    public static final EngineeringUnits wattsPerSquareMeter = new EngineeringUnits(35);
    public static final EngineeringUnits lumens = new EngineeringUnits(36);
    public static final EngineeringUnits luxes = new EngineeringUnits(37);
    public static final EngineeringUnits footCandles = new EngineeringUnits(38);
    // Mass
    public static final EngineeringUnits milligrams = new EngineeringUnits(196);
    public static final EngineeringUnits grams = new EngineeringUnits(195);
    public static final EngineeringUnits kilograms = new EngineeringUnits(39);
    public static final EngineeringUnits poundsMass = new EngineeringUnits(40);
    public static final EngineeringUnits tons = new EngineeringUnits(41);
    // Mass Flow
    public static final EngineeringUnits gramsPerSecond = new EngineeringUnits(154);
    public static final EngineeringUnits gramsPerMinute = new EngineeringUnits(155);
    public static final EngineeringUnits kilogramsPerSecond = new EngineeringUnits(42);
    public static final EngineeringUnits kilogramsPerMinute = new EngineeringUnits(43);
    public static final EngineeringUnits kilogramsPerHour = new EngineeringUnits(44);
    public static final EngineeringUnits poundsMassPerSecond = new EngineeringUnits(119);
    public static final EngineeringUnits poundsMassPerMinute = new EngineeringUnits(45);
    public static final EngineeringUnits poundsMassPerHour = new EngineeringUnits(46);
    public static final EngineeringUnits tonsPerHour = new EngineeringUnits(156);
    // Power
    public static final EngineeringUnits milliwatts = new EngineeringUnits(132);
    public static final EngineeringUnits watts = new EngineeringUnits(47);
    public static final EngineeringUnits kilowatts = new EngineeringUnits(48);
    public static final EngineeringUnits megawatts = new EngineeringUnits(49);
    public static final EngineeringUnits btusPerHour = new EngineeringUnits(50);
    public static final EngineeringUnits kiloBtusPerHour = new EngineeringUnits(157);
    public static final EngineeringUnits joulesPerHour = new EngineeringUnits(247);
    public static final EngineeringUnits horsepower = new EngineeringUnits(51);
    public static final EngineeringUnits tonsRefrigeration = new EngineeringUnits(52);
    // Pressure
    public static final EngineeringUnits pascals = new EngineeringUnits(53);
    public static final EngineeringUnits hectopascals = new EngineeringUnits(133);
    public static final EngineeringUnits kilopascals = new EngineeringUnits(54);
    public static final EngineeringUnits millibars = new EngineeringUnits(134);
    public static final EngineeringUnits bars = new EngineeringUnits(55);
    public static final EngineeringUnits poundsForcePerSquareInch = new EngineeringUnits(56);
    public static final EngineeringUnits millimetersOfWater = new EngineeringUnits(206);
    public static final EngineeringUnits centimetersOfWater = new EngineeringUnits(57);
    public static final EngineeringUnits inchesOfWater = new EngineeringUnits(58);
    public static final EngineeringUnits millimetersOfMercury = new EngineeringUnits(59);
    public static final EngineeringUnits centimetersOfMercury = new EngineeringUnits(60);
    public static final EngineeringUnits inchesOfMercury = new EngineeringUnits(61);
    // Temperature
    public static final EngineeringUnits degreesCelsius = new EngineeringUnits(62);
    public static final EngineeringUnits degreesKelvin = new EngineeringUnits(63);
    public static final EngineeringUnits degreesKelvinPerHour = new EngineeringUnits(181);
    public static final EngineeringUnits degreesKelvinPerMinute = new EngineeringUnits(182);
    public static final EngineeringUnits degreesFahrenheit = new EngineeringUnits(64);
    public static final EngineeringUnits degreeDaysCelsius = new EngineeringUnits(65);
    public static final EngineeringUnits degreeDaysFahrenheit = new EngineeringUnits(66);
    public static final EngineeringUnits deltaDegreesFahrenheit = new EngineeringUnits(120);
    public static final EngineeringUnits deltaDegreesKelvin = new EngineeringUnits(121);
    // Time
    public static final EngineeringUnits years = new EngineeringUnits(67);
    public static final EngineeringUnits months = new EngineeringUnits(68);
    public static final EngineeringUnits weeks = new EngineeringUnits(69);
    public static final EngineeringUnits days = new EngineeringUnits(70);
    public static final EngineeringUnits hours = new EngineeringUnits(71);
    public static final EngineeringUnits minutes = new EngineeringUnits(72);
    public static final EngineeringUnits seconds = new EngineeringUnits(73);
    public static final EngineeringUnits hundredthsSeconds = new EngineeringUnits(158);
    public static final EngineeringUnits milliseconds = new EngineeringUnits(159);
    // Torque
    public static final EngineeringUnits newtonMeters = new EngineeringUnits(160);
    // Velocity
    public static final EngineeringUnits millimetersPerSecond = new EngineeringUnits(161);
    public static final EngineeringUnits millimetersPerMinute = new EngineeringUnits(162);
    public static final EngineeringUnits metersPerSecond = new EngineeringUnits(74);
    public static final EngineeringUnits metersPerMinute = new EngineeringUnits(163);
    public static final EngineeringUnits metersPerHour = new EngineeringUnits(164);
    public static final EngineeringUnits kilometersPerHour = new EngineeringUnits(75);
    public static final EngineeringUnits feetPerSecond = new EngineeringUnits(76);
    public static final EngineeringUnits feetPerMinute = new EngineeringUnits(77);
    public static final EngineeringUnits milesPerHour = new EngineeringUnits(78);
    // Volume
    public static final EngineeringUnits cubicFeet = new EngineeringUnits(79);
    public static final EngineeringUnits cubicMeters = new EngineeringUnits(80);
    public static final EngineeringUnits imperialGallons = new EngineeringUnits(81);
    public static final EngineeringUnits milliliters = new EngineeringUnits(197);
    public static final EngineeringUnits liters = new EngineeringUnits(82);
    public static final EngineeringUnits usGallons = new EngineeringUnits(83);
    // Volumetric Flow
    public static final EngineeringUnits cubicFeetPerSecond = new EngineeringUnits(142);
    public static final EngineeringUnits cubicFeetPerMinute = new EngineeringUnits(84);
    public static final EngineeringUnits millionStandardCubicFeetPerMinute = new EngineeringUnits(254);
    public static final EngineeringUnits cubicFeetPerHour = new EngineeringUnits(191);
    public static final EngineeringUnits cubicFeetPerDay = new EngineeringUnits(248);
    public static final EngineeringUnits standardCubicFeetPerDay = new EngineeringUnits(47808);
    public static final EngineeringUnits millionStandardCubicFeetPerDay = new EngineeringUnits(47809);
    public static final EngineeringUnits thousandCubicFeetPerDay = new EngineeringUnits(47810);
    public static final EngineeringUnits thousandStandardCubicFeetPerDay = new EngineeringUnits(47811);
    public static final EngineeringUnits poundsMassPerDay = new EngineeringUnits(47812);
    public static final EngineeringUnits cubicMetersPerSecond = new EngineeringUnits(85);
    public static final EngineeringUnits cubicMetersPerMinute = new EngineeringUnits(165);
    public static final EngineeringUnits cubicMetersPerHour = new EngineeringUnits(135);
    public static final EngineeringUnits cubicMetersPerDay = new EngineeringUnits(249);
    public static final EngineeringUnits imperialGallonsPerMinute = new EngineeringUnits(86);
    public static final EngineeringUnits millilitersPerSecond = new EngineeringUnits(198);
    public static final EngineeringUnits litersPerSecond = new EngineeringUnits(87);
    public static final EngineeringUnits litersPerMinute = new EngineeringUnits(88);
    public static final EngineeringUnits litersPerHour = new EngineeringUnits(136);
    public static final EngineeringUnits usGallonsPerMinute = new EngineeringUnits(89);
    public static final EngineeringUnits usGallonsPerHour = new EngineeringUnits(192);
    // Other
    public static final EngineeringUnits degreesAngular = new EngineeringUnits(90);
    public static final EngineeringUnits degreesCelsiusPerHour = new EngineeringUnits(91);
    public static final EngineeringUnits degreesCelsiusPerMinute = new EngineeringUnits(92);
    public static final EngineeringUnits degreesFahrenheitPerHour = new EngineeringUnits(93);
    public static final EngineeringUnits degreesFahrenheitPerMinute = new EngineeringUnits(94);
    public static final EngineeringUnits jouleSeconds = new EngineeringUnits(183);
    public static final EngineeringUnits kilogramsPerCubicMeter = new EngineeringUnits(186);
    public static final EngineeringUnits kilowattHoursPerSquareMeter = new EngineeringUnits(137);
    public static final EngineeringUnits kilowattHoursPerSquareFoot = new EngineeringUnits(138);
    public static final EngineeringUnits wattHoursPerCubicMeter = new EngineeringUnits(250);
    public static final EngineeringUnits joulesPerCubicMeter = new EngineeringUnits(251);
    public static final EngineeringUnits megajoulesPerSquareMeter = new EngineeringUnits(139);
    public static final EngineeringUnits megajoulesPerSquareFoot = new EngineeringUnits(140);
    public static final EngineeringUnits molePercent = new EngineeringUnits(252);
    public static final EngineeringUnits noUnits = new EngineeringUnits(95);
    public static final EngineeringUnits newtonSeconds = new EngineeringUnits(187);
    public static final EngineeringUnits newtonsPerMeter = new EngineeringUnits(188);
    public static final EngineeringUnits partsPerMillion = new EngineeringUnits(96);
    public static final EngineeringUnits partsPerBillion = new EngineeringUnits(97);
    public static final EngineeringUnits pascalSeconds = new EngineeringUnits(253);
    public static final EngineeringUnits percent = new EngineeringUnits(98);
    public static final EngineeringUnits percentObscurationPerFoot = new EngineeringUnits(143);
    public static final EngineeringUnits percentObscurationPerMeter = new EngineeringUnits(144);
    public static final EngineeringUnits percentPerSecond = new EngineeringUnits(99);
    public static final EngineeringUnits perMinute = new EngineeringUnits(100);
    public static final EngineeringUnits perSecond = new EngineeringUnits(101);
    public static final EngineeringUnits psiPerDegreeFahrenheit = new EngineeringUnits(102);
    public static final EngineeringUnits radians = new EngineeringUnits(103);
    public static final EngineeringUnits radiansPerSecond = new EngineeringUnits(184);
    public static final EngineeringUnits revolutionsPerMinute = new EngineeringUnits(104);
    public static final EngineeringUnits squareMetersPerNewton = new EngineeringUnits(185);
    public static final EngineeringUnits wattsPerMeterPerDegreeKelvin = new EngineeringUnits(189);
    public static final EngineeringUnits wattsPerSquareMeterDegreeKelvin = new EngineeringUnits(141);
    public static final EngineeringUnits perMille = new EngineeringUnits(207);
    public static final EngineeringUnits gramsPerGram = new EngineeringUnits(208);
    public static final EngineeringUnits kilogramsPerKilogram = new EngineeringUnits(209);
    public static final EngineeringUnits gramsPerKilogram = new EngineeringUnits(210);
    public static final EngineeringUnits milligramsPerGram = new EngineeringUnits(211);
    public static final EngineeringUnits milligramsPerKilogram = new EngineeringUnits(212);
    public static final EngineeringUnits gramsPerMilliliter = new EngineeringUnits(213);
    public static final EngineeringUnits gramsPerLiter = new EngineeringUnits(214);
    public static final EngineeringUnits milligramsPerLiter = new EngineeringUnits(215);
    public static final EngineeringUnits microgramsPerLiter = new EngineeringUnits(216);
    public static final EngineeringUnits gramsPerCubicMeter = new EngineeringUnits(217);
    public static final EngineeringUnits milligramsPerCubicMeter = new EngineeringUnits(218);
    public static final EngineeringUnits microgramsPerCubicMeter = new EngineeringUnits(219);
    public static final EngineeringUnits nanogramsPerCubicMeter = new EngineeringUnits(220);
    public static final EngineeringUnits gramsPerCubicCentimeter = new EngineeringUnits(221);
    public static final EngineeringUnits becquerels = new EngineeringUnits(222);
    public static final EngineeringUnits kilobecquerels = new EngineeringUnits(223);
    public static final EngineeringUnits megabecquerels = new EngineeringUnits(224);
    public static final EngineeringUnits gray = new EngineeringUnits(225);
    public static final EngineeringUnits milligray = new EngineeringUnits(226);
    public static final EngineeringUnits microgray = new EngineeringUnits(227);
    public static final EngineeringUnits sieverts = new EngineeringUnits(228);
    public static final EngineeringUnits millisieverts = new EngineeringUnits(229);
    public static final EngineeringUnits microsieverts = new EngineeringUnits(230);
    public static final EngineeringUnits microsievertsPerHour = new EngineeringUnits(231);
    public static final EngineeringUnits millirems = new EngineeringUnits(47814);
    public static final EngineeringUnits milliremsPerHour = new EngineeringUnits(47815);
    public static final EngineeringUnits decibelsA = new EngineeringUnits(232);
    public static final EngineeringUnits nephelometricTurbidityUnit = new EngineeringUnits(233);
    public static final EngineeringUnits pH = new EngineeringUnits(234);
    public static final EngineeringUnits gramsPerSquareMeter = new EngineeringUnits(235);
    public static final EngineeringUnits minutesPerDegreeKelvin = new EngineeringUnits(236);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static EngineeringUnits forId(final int id) {
        EngineeringUnits e = (EngineeringUnits) idMap.get(id);
        if (e == null)
            e = new EngineeringUnits(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static EngineeringUnits forName(final String name) {
        return (EngineeringUnits) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private EngineeringUnits(final int value) {
        super(value);
    }

    public EngineeringUnits(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    /**
     * Returns a unmodifiable map.
     *
     * @return unmodifiable map
     */
    public static Map<Integer, String> getPrettyMap() {
        return Collections.unmodifiableMap(prettyMap);
    }

    /**
     * Returns a unmodifiable nameMap.
     *
     * @return unmodifiable map
     */
    public static Map<String, Enumerated> getNameMap() {
        return Collections.unmodifiableMap(nameMap);
    }

    @Override
    public String toString() {
        return super.toString(prettyMap);
    }
}
