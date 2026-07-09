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
    // Efficiency
    public static final EngineeringUnits btuPerHourPerWatt = new EngineeringUnits(47898);
    public static final EngineeringUnits btuPerWattHourSeasonal = new EngineeringUnits(47899);
    public static final EngineeringUnits coefficientOfPerformance = new EngineeringUnits(47900);
    public static final EngineeringUnits coefficientOfPerformanceSeasonal = new EngineeringUnits(47901);
    public static final EngineeringUnits kilowattPerTonRefrigeration = new EngineeringUnits(47902);
    public static final EngineeringUnits lumensPerWatt = new EngineeringUnits(47903);
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
    public static final EngineeringUnits microsiemensPerCentimeter = new EngineeringUnits(47909);
    public static final EngineeringUnits millisiemensPerCentimeter = new EngineeringUnits(47910);
    public static final EngineeringUnits millisiemensPerMeter = new EngineeringUnits(47911);
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
    public static final EngineeringUnits voltsPerKelvin = new EngineeringUnits(176);
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
    public static final EngineeringUnits activeEnergyPulseValue = new EngineeringUnits(47918);
    public static final EngineeringUnits reactiveEnergyPulseValue = new EngineeringUnits(47919);
    public static final EngineeringUnits apparentEnergyPulseValue = new EngineeringUnits(47920);
    public static final EngineeringUnits voltSquaredHourPulseValue = new EngineeringUnits(47921);
    public static final EngineeringUnits ampereSquaredHourPulseValue = new EngineeringUnits(47922);
    // Enthalpy
    public static final EngineeringUnits joulesPerKilogramDryAir = new EngineeringUnits(23);
    public static final EngineeringUnits kilojoulesPerKilogramDryAir = new EngineeringUnits(149);
    public static final EngineeringUnits megajoulesPerKilogramDryAir = new EngineeringUnits(150);
    public static final EngineeringUnits btusPerPoundDryAir = new EngineeringUnits(24);
    public static final EngineeringUnits btusPerPound = new EngineeringUnits(117);
    // Entropy
    public static final EngineeringUnits joulesPerKelvin = new EngineeringUnits(127);
    public static final EngineeringUnits kilojoulesPerKelvin = new EngineeringUnits(151);
    public static final EngineeringUnits megajoulesPerKelvin = new EngineeringUnits(152);
    public static final EngineeringUnits joulesPerKilogramKelvin = new EngineeringUnits(128);
    // Force
    public static final EngineeringUnits newton = new EngineeringUnits(153);
    // Frequency
    public static final EngineeringUnits cyclesPerHour = new EngineeringUnits(25);
    public static final EngineeringUnits cyclesPerMinute = new EngineeringUnits(26);
    public static final EngineeringUnits hertz = new EngineeringUnits(27);
    public static final EngineeringUnits kilohertz = new EngineeringUnits(129);
    public static final EngineeringUnits megahertz = new EngineeringUnits(130);
    public static final EngineeringUnits perDay = new EngineeringUnits(47823);
    public static final EngineeringUnits perHour = new EngineeringUnits(131);
    public static final EngineeringUnits perMillisecond = new EngineeringUnits(47824);
    // Humidity
    public static final EngineeringUnits gramsOfWaterPerKilogramDryAir = new EngineeringUnits(28);
    public static final EngineeringUnits grainsOfWaterPerPoundDryAir = new EngineeringUnits(47972);
    public static final EngineeringUnits percentRelativeHumidity = new EngineeringUnits(29);
    // Length
    public static final EngineeringUnits micrometers = new EngineeringUnits(194);
    public static final EngineeringUnits millimeters = new EngineeringUnits(30);
    public static final EngineeringUnits centimeters = new EngineeringUnits(118);
    public static final EngineeringUnits kilometers = new EngineeringUnits(193);
    public static final EngineeringUnits meters = new EngineeringUnits(31);
    public static final EngineeringUnits inches = new EngineeringUnits(32);
    public static final EngineeringUnits feet = new EngineeringUnits(33);
    public static final EngineeringUnits yards = new EngineeringUnits(47825);
    public static final EngineeringUnits miles = new EngineeringUnits(47826);
    public static final EngineeringUnits nauticalMiles = new EngineeringUnits(47827);
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
    public static final EngineeringUnits metricTonnes = new EngineeringUnits(47830);
    public static final EngineeringUnits shortTons = new EngineeringUnits(47831);
    public static final EngineeringUnits longTons = new EngineeringUnits(47832);
    // Mass Flow
    public static final EngineeringUnits gramsPerSecond = new EngineeringUnits(154);
    public static final EngineeringUnits gramsPerMinute = new EngineeringUnits(155);
    public static final EngineeringUnits gramsPerHour = new EngineeringUnits(47833);
    public static final EngineeringUnits gramsPerDay = new EngineeringUnits(47834);
    public static final EngineeringUnits kilogramsPerSecond = new EngineeringUnits(42);
    public static final EngineeringUnits kilogramsPerMinute = new EngineeringUnits(43);
    public static final EngineeringUnits kilogramsPerHour = new EngineeringUnits(44);
    public static final EngineeringUnits kilogramsPerDay = new EngineeringUnits(47835);
    public static final EngineeringUnits poundsMassPerSecond = new EngineeringUnits(119);
    public static final EngineeringUnits poundsMassPerMinute = new EngineeringUnits(45);
    public static final EngineeringUnits poundsMassPerHour = new EngineeringUnits(46);
    public static final EngineeringUnits tonsPerHour = new EngineeringUnits(156);
    public static final EngineeringUnits shortTonsPerSecond = new EngineeringUnits(47836);
    public static final EngineeringUnits shortTonsPerMinute = new EngineeringUnits(47837);
    public static final EngineeringUnits shortTonsPerHour = new EngineeringUnits(47838);
    public static final EngineeringUnits shortTonsPerDay = new EngineeringUnits(47839);
    public static final EngineeringUnits metricTonnesPerSecond = new EngineeringUnits(47840);
    public static final EngineeringUnits metricTonnesPerMinute = new EngineeringUnits(47841);
    public static final EngineeringUnits metricTonnesPerHour = new EngineeringUnits(47842);
    public static final EngineeringUnits metricTonnesPerDay = new EngineeringUnits(47843);
    public static final EngineeringUnits longTonsPerSecond = new EngineeringUnits(47844);
    public static final EngineeringUnits longTonsPerMinute = new EngineeringUnits(47845);
    public static final EngineeringUnits longTonsPerHour = new EngineeringUnits(47846);
    public static final EngineeringUnits longTonsPerDay = new EngineeringUnits(47847);
    // Power
    public static final EngineeringUnits milliwatts = new EngineeringUnits(132);
    public static final EngineeringUnits watts = new EngineeringUnits(47);
    public static final EngineeringUnits kilowatts = new EngineeringUnits(48);
    public static final EngineeringUnits megawatts = new EngineeringUnits(49);
    public static final EngineeringUnits gigawatts = new EngineeringUnits(47924);
    public static final EngineeringUnits btusPerSecond = new EngineeringUnits(47848);
    public static final EngineeringUnits btusPerMinute = new EngineeringUnits(47849);
    public static final EngineeringUnits btusPerHour = new EngineeringUnits(50);
    public static final EngineeringUnits btusPerDay = new EngineeringUnits(47850);
    public static final EngineeringUnits kiloBtusPerSecond = new EngineeringUnits(47851);
    public static final EngineeringUnits kiloBtusPerMinute = new EngineeringUnits(47852);
    public static final EngineeringUnits kiloBtusPerHour = new EngineeringUnits(157);
    public static final EngineeringUnits kiloBtusPerDay = new EngineeringUnits(47853);
    public static final EngineeringUnits megaBtusPerSecond = new EngineeringUnits(47854);
    public static final EngineeringUnits megaBtusPerMinute = new EngineeringUnits(47855);
    public static final EngineeringUnits megaBtusPerHour = new EngineeringUnits(47856);
    public static final EngineeringUnits megaBtusPerDay = new EngineeringUnits(47857);
    public static final EngineeringUnits joulesPerSecond = new EngineeringUnits(47858);
    public static final EngineeringUnits joulesPerMinute = new EngineeringUnits(47859);
    public static final EngineeringUnits joulesPerHour = new EngineeringUnits(247);
    public static final EngineeringUnits joulesPerDay = new EngineeringUnits(47860);
    public static final EngineeringUnits kilojoulesPerSecond = new EngineeringUnits(47861);
    public static final EngineeringUnits kilojoulesPerMinute = new EngineeringUnits(47862);
    public static final EngineeringUnits kilojoulesPerHour = new EngineeringUnits(47863);
    public static final EngineeringUnits kilojoulesPerDay = new EngineeringUnits(47864);
    public static final EngineeringUnits megajoulesPerSecond = new EngineeringUnits(47865);
    public static final EngineeringUnits megajoulesPerMinute = new EngineeringUnits(47866);
    public static final EngineeringUnits megajoulesPerHour = new EngineeringUnits(47867);
    public static final EngineeringUnits megajoulesPerDay = new EngineeringUnits(47868);
    public static final EngineeringUnits horsepower = new EngineeringUnits(51);
    public static final EngineeringUnits tonsRefrigeration = new EngineeringUnits(52);
    // Pressure
    public static final EngineeringUnits pascals = new EngineeringUnits(53);
    public static final EngineeringUnits hectopascals = new EngineeringUnits(133);
    public static final EngineeringUnits kilopascals = new EngineeringUnits(54);
    public static final EngineeringUnits millibars = new EngineeringUnits(134);
    public static final EngineeringUnits bars = new EngineeringUnits(55);
    public static final EngineeringUnits poundsForcePerSquareInch = new EngineeringUnits(56);
    public static final EngineeringUnits poundsForcePerSquareInchAbsolute = new EngineeringUnits(47907);
    public static final EngineeringUnits poundsForcePerSquareInchGauge = new EngineeringUnits(47908);
    public static final EngineeringUnits millimetersOfWater = new EngineeringUnits(206);
    public static final EngineeringUnits centimetersOfWater = new EngineeringUnits(57);
    public static final EngineeringUnits inchesOfWater = new EngineeringUnits(58);
    public static final EngineeringUnits millimetersOfMercury = new EngineeringUnits(59);
    public static final EngineeringUnits centimetersOfMercury = new EngineeringUnits(60);
    public static final EngineeringUnits inchesOfMercury = new EngineeringUnits(61);
    // Temperature
    public static final EngineeringUnits degreesCelsius = new EngineeringUnits(62);
    public static final EngineeringUnits degreesCelsiusPerDay = new EngineeringUnits(47869);
    public static final EngineeringUnits degreesCelsiusPerHour = new EngineeringUnits(91);
    public static final EngineeringUnits degreesCelsiusPerMinute = new EngineeringUnits(92);
    public static final EngineeringUnits kelvin = new EngineeringUnits(63);
    public static final EngineeringUnits kelvinPerDay = new EngineeringUnits(47870);
    public static final EngineeringUnits kelvinPerHour = new EngineeringUnits(181);
    public static final EngineeringUnits kelvinPerMinute = new EngineeringUnits(182);
    public static final EngineeringUnits degreesFahrenheit = new EngineeringUnits(64);
    public static final EngineeringUnits degreesFahrenheitPerDay = new EngineeringUnits(47871);
    public static final EngineeringUnits degreesFahrenheitPerHour = new EngineeringUnits(93);
    public static final EngineeringUnits degreesFahrenheitPerMinute = new EngineeringUnits(94);
    public static final EngineeringUnits degreeDaysCelsius = new EngineeringUnits(65);
    public static final EngineeringUnits degreeDaysFahrenheit = new EngineeringUnits(66);
    public static final EngineeringUnits deltaDegreesCelsius = new EngineeringUnits(47872);
    public static final EngineeringUnits deltaDegreesFahrenheit = new EngineeringUnits(120);
    public static final EngineeringUnits deltaKelvin = new EngineeringUnits(121);
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
    public static final EngineeringUnits microseconds = new EngineeringUnits(47979);
    public static final EngineeringUnits nanoseconds = new EngineeringUnits(47980);
    public static final EngineeringUnits picoseconds = new EngineeringUnits(47981);
    // Torque
    public static final EngineeringUnits newtonMeters = new EngineeringUnits(160);
    public static final EngineeringUnits poundForceFeet = new EngineeringUnits(47904);
    public static final EngineeringUnits poundForceInches = new EngineeringUnits(47905);
    public static final EngineeringUnits ounceForceInches = new EngineeringUnits(47906);
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
    public static final EngineeringUnits millionsOfUsGallons = new EngineeringUnits(47912);
    public static final EngineeringUnits millionsOfImperialGallons = new EngineeringUnits(47913);
    public static final EngineeringUnits volume1 = new EngineeringUnits(47937);
    public static final EngineeringUnits volume2 = new EngineeringUnits(47938);
    public static final EngineeringUnits volume3 = new EngineeringUnits(47939);
    public static final EngineeringUnits volume4 = new EngineeringUnits(47940);
    public static final EngineeringUnits volume5 = new EngineeringUnits(47941);
    public static final EngineeringUnits volume6 = new EngineeringUnits(47942);
    public static final EngineeringUnits volume7 = new EngineeringUnits(47943);
    public static final EngineeringUnits volume8 = new EngineeringUnits(47944);
    public static final EngineeringUnits volume9 = new EngineeringUnits(47945);
    public static final EngineeringUnits volume10 = new EngineeringUnits(47946);
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
    public static final EngineeringUnits millionCubicFeetPerMinute = new EngineeringUnits(47873);
    public static final EngineeringUnits millionCubicFeetPerDay = new EngineeringUnits(47874);
    public static final EngineeringUnits poundsMassPerDay = new EngineeringUnits(47812);
    public static final EngineeringUnits cubicMetersPerSecond = new EngineeringUnits(85);
    public static final EngineeringUnits cubicMetersPerMinute = new EngineeringUnits(165);
    public static final EngineeringUnits cubicMetersPerHour = new EngineeringUnits(135);
    public static final EngineeringUnits cubicMetersPerDay = new EngineeringUnits(249);
    public static final EngineeringUnits imperialGallonsPerSecond = new EngineeringUnits(47875);
    public static final EngineeringUnits imperialGallonsPerMinute = new EngineeringUnits(86);
    public static final EngineeringUnits millilitersPerSecond = new EngineeringUnits(198);
    public static final EngineeringUnits millilitersPerMinute = new EngineeringUnits(47914);
    public static final EngineeringUnits litersPerSecond = new EngineeringUnits(87);
    public static final EngineeringUnits litersPerMinute = new EngineeringUnits(88);
    public static final EngineeringUnits litersPerHour = new EngineeringUnits(136);
    public static final EngineeringUnits litersPerDay = new EngineeringUnits(47878);
    public static final EngineeringUnits usGallonsPerSecond = new EngineeringUnits(47879);
    public static final EngineeringUnits usGallonsPerMinute = new EngineeringUnits(89);
    public static final EngineeringUnits usGallonsPerHour = new EngineeringUnits(192);
    public static final EngineeringUnits usGallonsPerDay = new EngineeringUnits(47880);
    public static final EngineeringUnits cubicMeterPulseValue = new EngineeringUnits(47923);
    public static final EngineeringUnits volumetricFlow1 = new EngineeringUnits(47947);
    public static final EngineeringUnits volumetricFlow2 = new EngineeringUnits(47948);
    public static final EngineeringUnits volumetricFlow3 = new EngineeringUnits(47949);
    public static final EngineeringUnits volumetricFlow4 = new EngineeringUnits(47950);
    public static final EngineeringUnits volumetricFlow5 = new EngineeringUnits(47951);
    public static final EngineeringUnits volumetricFlow6 = new EngineeringUnits(47952);
    public static final EngineeringUnits volumetricFlow7 = new EngineeringUnits(47953);
    public static final EngineeringUnits volumetricFlow8 = new EngineeringUnits(47954);
    public static final EngineeringUnits volumetricFlow9 = new EngineeringUnits(47955);
    public static final EngineeringUnits volumetricFlow10 = new EngineeringUnits(47956);
    // Other
    public static final EngineeringUnits degreesAngular = new EngineeringUnits(90);
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
    public static final EngineeringUnits percentPerMinute = new EngineeringUnits(47881);
    public static final EngineeringUnits percentPerHour = new EngineeringUnits(47882);
    public static final EngineeringUnits percentPerDay = new EngineeringUnits(47883);
    public static final EngineeringUnits perMinute = new EngineeringUnits(100);
    public static final EngineeringUnits perSecond = new EngineeringUnits(101);
    public static final EngineeringUnits psiPerDegreeFahrenheit = new EngineeringUnits(102);
    public static final EngineeringUnits radians = new EngineeringUnits(103);
    public static final EngineeringUnits radiansPerSecond = new EngineeringUnits(184);
    public static final EngineeringUnits revolutionsPerMinute = new EngineeringUnits(104);
    public static final EngineeringUnits squareMetersPerNewton = new EngineeringUnits(185);
    public static final EngineeringUnits wattsPerMeterPerKelvin = new EngineeringUnits(189);
    public static final EngineeringUnits wattsPerSquareMeterPerKelvin = new EngineeringUnits(141);
    public static final EngineeringUnits perMille = new EngineeringUnits(207);
    public static final EngineeringUnits perMillion = new EngineeringUnits(47884);
    public static final EngineeringUnits perBillion = new EngineeringUnits(47885);
    public static final EngineeringUnits gramsPerGram = new EngineeringUnits(208);
    public static final EngineeringUnits milligramsPerGram = new EngineeringUnits(211);
    public static final EngineeringUnits kilogramsPerKilogram = new EngineeringUnits(209);
    public static final EngineeringUnits gramsPerKilogram = new EngineeringUnits(210);
    public static final EngineeringUnits milligramsPerKilogram = new EngineeringUnits(212);
    public static final EngineeringUnits microgramsPerKilogram = new EngineeringUnits(47888);
    public static final EngineeringUnits nanogramsPerKilogram = new EngineeringUnits(47889);
    public static final EngineeringUnits gramsPerMilliliter = new EngineeringUnits(213);
    public static final EngineeringUnits milligramsPerMilliliter = new EngineeringUnits(47890);
    public static final EngineeringUnits microgramsPerMilliliter = new EngineeringUnits(47891);
    public static final EngineeringUnits nanogramsPerMilliliter = new EngineeringUnits(47892);
    public static final EngineeringUnits kilogramsPerLiter = new EngineeringUnits(47893);
    public static final EngineeringUnits gramsPerLiter = new EngineeringUnits(214);
    public static final EngineeringUnits milligramsPerLiter = new EngineeringUnits(215);
    public static final EngineeringUnits microgramsPerLiter = new EngineeringUnits(216);
    public static final EngineeringUnits nanogramsPerLiter = new EngineeringUnits(47894);
    public static final EngineeringUnits gramsPerCubicMeter = new EngineeringUnits(217);
    public static final EngineeringUnits milligramsPerCubicMeter = new EngineeringUnits(218);
    public static final EngineeringUnits microgramsPerCubicMeter = new EngineeringUnits(219);
    public static final EngineeringUnits nanogramsPerCubicMeter = new EngineeringUnits(220);
    public static final EngineeringUnits gramsPerCubicCentimeter = new EngineeringUnits(221);
    public static final EngineeringUnits milligramsPerCubicCentimeter = new EngineeringUnits(47895);
    public static final EngineeringUnits microgramsPerCubicCentimeter = new EngineeringUnits(47896);
    public static final EngineeringUnits nanogramsPerCubicCentimeter = new EngineeringUnits(47897);
    public static final EngineeringUnits particlesPerCubicFoot = new EngineeringUnits(47968);
    public static final EngineeringUnits particlesPerCubicMeter = new EngineeringUnits(47969);
    public static final EngineeringUnits picocuriesPerLiter = new EngineeringUnits(47970);
    public static final EngineeringUnits becquerelsPerCubicMeter = new EngineeringUnits(47971);
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
    public static final EngineeringUnits minutesPerKelvin = new EngineeringUnits(236);
    public static final EngineeringUnits degreesLovibond = new EngineeringUnits(47816);
    public static final EngineeringUnits alcoholByVolume = new EngineeringUnits(47817);
    public static final EngineeringUnits internationalBitteringUnits = new EngineeringUnits(47818);
    public static final EngineeringUnits europeanBitternessUnits = new EngineeringUnits(47819);
    public static final EngineeringUnits degreesPlato = new EngineeringUnits(47820);
    public static final EngineeringUnits specificGravity = new EngineeringUnits(47821);
    public static final EngineeringUnits europeanBrewingConvention = new EngineeringUnits(47822);
    public static final EngineeringUnits milsPerYear = new EngineeringUnits(47915);
    public static final EngineeringUnits millimetersPerYear = new EngineeringUnits(47916);
    public static final EngineeringUnits pulsesPerMinute = new EngineeringUnits(47917);
    public static final EngineeringUnits bitsPerSecond = new EngineeringUnits(47929);
    public static final EngineeringUnits kilobitsPerSecond = new EngineeringUnits(47930);
    public static final EngineeringUnits megabitsPerSecond = new EngineeringUnits(47931);
    public static final EngineeringUnits gigabitsPerSecond = new EngineeringUnits(47932);
    public static final EngineeringUnits bytesPerSecond = new EngineeringUnits(47933);
    public static final EngineeringUnits kilobytesPerSecond = new EngineeringUnits(47934);
    public static final EngineeringUnits megabytesPerSecond = new EngineeringUnits(47935);
    public static final EngineeringUnits gigabytesPerSecond = new EngineeringUnits(47936);
    public static final EngineeringUnits siteUnit1 = new EngineeringUnits(47958);
    public static final EngineeringUnits siteUnit2 = new EngineeringUnits(47959);
    public static final EngineeringUnits siteUnit3 = new EngineeringUnits(47960);
    public static final EngineeringUnits siteUnit4 = new EngineeringUnits(47961);
    public static final EngineeringUnits siteUnit5 = new EngineeringUnits(47962);
    public static final EngineeringUnits siteUnit6 = new EngineeringUnits(47963);
    public static final EngineeringUnits siteUnit7 = new EngineeringUnits(47964);
    public static final EngineeringUnits siteUnit8 = new EngineeringUnits(47965);
    public static final EngineeringUnits siteUnit9 = new EngineeringUnits(47966);
    public static final EngineeringUnits siteUnit10 = new EngineeringUnits(47967);
    public static final EngineeringUnits degreeHoursCelsius = new EngineeringUnits(47973);
    public static final EngineeringUnits degreeHoursFahrenheit = new EngineeringUnits(47974);
    public static final EngineeringUnits degreeMinutesCelsius = new EngineeringUnits(47975);
    public static final EngineeringUnits degreeMinutesFahrenheit = new EngineeringUnits(47976);
    public static final EngineeringUnits degreeSecondsCelsius = new EngineeringUnits(47977);
    public static final EngineeringUnits degreeSecondsFahrenheit = new EngineeringUnits(47978);
    // Uncategorized
    public static final EngineeringUnits nanograms = new EngineeringUnits(47828);
    public static final EngineeringUnits micrograms = new EngineeringUnits(47829);
    public static final EngineeringUnits imperialGallonsPerHour = new EngineeringUnits(47876);
    public static final EngineeringUnits imperialGallonsPerDay = new EngineeringUnits(47877);
    public static final EngineeringUnits microgramsPerGram = new EngineeringUnits(47886);
    public static final EngineeringUnits nanogramsPerGram = new EngineeringUnits(47887);
    public static final EngineeringUnits gigajoules = new EngineeringUnits(47925);
    public static final EngineeringUnits terajoules = new EngineeringUnits(47926);
    public static final EngineeringUnits gigawattHours = new EngineeringUnits(47927);
    public static final EngineeringUnits gigawattReactiveHours = new EngineeringUnits(47928);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static EngineeringUnits forId(int id) {
        EngineeringUnits e = (EngineeringUnits) idMap.get(id);
        if (e == null)
            e = new EngineeringUnits(id);
        return e;
    }

    public static String nameForId(int id) {
        return prettyMap.get(id);
    }

    public static EngineeringUnits forName(String name) {
        return (EngineeringUnits) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private EngineeringUnits(int value) {
        super(value);
    }

    public EngineeringUnits(ByteQueue queue) throws BACnetErrorException {
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
