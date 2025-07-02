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

public class PropertyIdentifier extends Enumerated {
    public static final PropertyIdentifier ackedTransitions = new PropertyIdentifier(0);
    public static final PropertyIdentifier ackRequired = new PropertyIdentifier(1);
    public static final PropertyIdentifier action = new PropertyIdentifier(2);
    public static final PropertyIdentifier actionText = new PropertyIdentifier(3);
    public static final PropertyIdentifier activeText = new PropertyIdentifier(4);
    public static final PropertyIdentifier activeVtSessions = new PropertyIdentifier(5);
    public static final PropertyIdentifier alarmValue = new PropertyIdentifier(6);
    public static final PropertyIdentifier alarmValues = new PropertyIdentifier(7);
    public static final PropertyIdentifier all = new PropertyIdentifier(8);
    public static final PropertyIdentifier allWritesSuccessful = new PropertyIdentifier(9);
    public static final PropertyIdentifier apduSegmentTimeout = new PropertyIdentifier(10);
    public static final PropertyIdentifier apduTimeout = new PropertyIdentifier(11);
    public static final PropertyIdentifier applicationSoftwareVersion = new PropertyIdentifier(12);
    public static final PropertyIdentifier archive = new PropertyIdentifier(13);
    public static final PropertyIdentifier bias = new PropertyIdentifier(14);
    public static final PropertyIdentifier changeOfStateCount = new PropertyIdentifier(15);
    public static final PropertyIdentifier changeOfStateTime = new PropertyIdentifier(16);
    public static final PropertyIdentifier notificationClass = new PropertyIdentifier(17);
    public static final PropertyIdentifier controlledVariableReference = new PropertyIdentifier(19);
    public static final PropertyIdentifier controlledVariableUnits = new PropertyIdentifier(20);
    public static final PropertyIdentifier controlledVariableValue = new PropertyIdentifier(21);
    public static final PropertyIdentifier covIncrement = new PropertyIdentifier(22);
    public static final PropertyIdentifier dateList = new PropertyIdentifier(23);
    public static final PropertyIdentifier daylightSavingsStatus = new PropertyIdentifier(24);
    public static final PropertyIdentifier deadband = new PropertyIdentifier(25);
    public static final PropertyIdentifier derivativeConstant = new PropertyIdentifier(26);
    public static final PropertyIdentifier derivativeConstantUnits = new PropertyIdentifier(27);
    public static final PropertyIdentifier description = new PropertyIdentifier(28);
    public static final PropertyIdentifier descriptionOfHalt = new PropertyIdentifier(29);
    public static final PropertyIdentifier deviceAddressBinding = new PropertyIdentifier(30);
    public static final PropertyIdentifier deviceType = new PropertyIdentifier(31);
    public static final PropertyIdentifier effectivePeriod = new PropertyIdentifier(32);
    public static final PropertyIdentifier elapsedActiveTime = new PropertyIdentifier(33);
    public static final PropertyIdentifier errorLimit = new PropertyIdentifier(34);
    public static final PropertyIdentifier eventEnable = new PropertyIdentifier(35);
    public static final PropertyIdentifier eventState = new PropertyIdentifier(36);
    public static final PropertyIdentifier eventType = new PropertyIdentifier(37);
    public static final PropertyIdentifier exceptionSchedule = new PropertyIdentifier(38);
    public static final PropertyIdentifier faultValues = new PropertyIdentifier(39);
    public static final PropertyIdentifier feedbackValue = new PropertyIdentifier(40);
    public static final PropertyIdentifier fileAccessMethod = new PropertyIdentifier(41);
    public static final PropertyIdentifier fileSize = new PropertyIdentifier(42);
    public static final PropertyIdentifier fileType = new PropertyIdentifier(43);
    public static final PropertyIdentifier firmwareRevision = new PropertyIdentifier(44);
    public static final PropertyIdentifier highLimit = new PropertyIdentifier(45);
    public static final PropertyIdentifier inactiveText = new PropertyIdentifier(46);
    public static final PropertyIdentifier inProcess = new PropertyIdentifier(47);
    public static final PropertyIdentifier instanceOf = new PropertyIdentifier(48);
    public static final PropertyIdentifier integralConstant = new PropertyIdentifier(49);
    public static final PropertyIdentifier integralConstantUnits = new PropertyIdentifier(50);
    public static final PropertyIdentifier limitEnable = new PropertyIdentifier(52);
    public static final PropertyIdentifier listOfGroupMembers = new PropertyIdentifier(53);
    public static final PropertyIdentifier listOfObjectPropertyReferences = new PropertyIdentifier(54);
    public static final PropertyIdentifier localDate = new PropertyIdentifier(56);
    public static final PropertyIdentifier localTime = new PropertyIdentifier(57);
    public static final PropertyIdentifier location = new PropertyIdentifier(58);
    public static final PropertyIdentifier lowLimit = new PropertyIdentifier(59);
    public static final PropertyIdentifier manipulatedVariableReference = new PropertyIdentifier(60);
    public static final PropertyIdentifier maximumOutput = new PropertyIdentifier(61);
    public static final PropertyIdentifier maxApduLengthAccepted = new PropertyIdentifier(62);
    public static final PropertyIdentifier maxInfoFrames = new PropertyIdentifier(63);
    public static final PropertyIdentifier maxMaster = new PropertyIdentifier(64);
    public static final PropertyIdentifier maxPresValue = new PropertyIdentifier(65);
    public static final PropertyIdentifier minimumOffTime = new PropertyIdentifier(66);
    public static final PropertyIdentifier minimumOnTime = new PropertyIdentifier(67);
    public static final PropertyIdentifier minimumOutput = new PropertyIdentifier(68);
    public static final PropertyIdentifier minPresValue = new PropertyIdentifier(69);
    public static final PropertyIdentifier modelName = new PropertyIdentifier(70);
    public static final PropertyIdentifier modificationDate = new PropertyIdentifier(71);
    public static final PropertyIdentifier notifyType = new PropertyIdentifier(72);
    public static final PropertyIdentifier numberOfApduRetries = new PropertyIdentifier(73);
    public static final PropertyIdentifier numberOfStates = new PropertyIdentifier(74);
    public static final PropertyIdentifier objectIdentifier = new PropertyIdentifier(75);
    public static final PropertyIdentifier objectList = new PropertyIdentifier(76);
    public static final PropertyIdentifier objectName = new PropertyIdentifier(77);
    public static final PropertyIdentifier objectPropertyReference = new PropertyIdentifier(78);
    public static final PropertyIdentifier objectType = new PropertyIdentifier(79);
    public static final PropertyIdentifier optional = new PropertyIdentifier(80);
    public static final PropertyIdentifier outOfService = new PropertyIdentifier(81);
    public static final PropertyIdentifier outputUnits = new PropertyIdentifier(82);
    public static final PropertyIdentifier eventParameters = new PropertyIdentifier(83);
    public static final PropertyIdentifier polarity = new PropertyIdentifier(84);
    public static final PropertyIdentifier presentValue = new PropertyIdentifier(85);
    public static final PropertyIdentifier priority = new PropertyIdentifier(86);
    public static final PropertyIdentifier priorityArray = new PropertyIdentifier(87);
    public static final PropertyIdentifier priorityForWriting = new PropertyIdentifier(88);
    public static final PropertyIdentifier processIdentifier = new PropertyIdentifier(89);
    public static final PropertyIdentifier programChange = new PropertyIdentifier(90);
    public static final PropertyIdentifier programLocation = new PropertyIdentifier(91);
    public static final PropertyIdentifier programState = new PropertyIdentifier(92);
    public static final PropertyIdentifier proportionalConstant = new PropertyIdentifier(93);
    public static final PropertyIdentifier proportionalConstantUnits = new PropertyIdentifier(94);
    public static final PropertyIdentifier protocolObjectTypesSupported = new PropertyIdentifier(96);
    public static final PropertyIdentifier protocolServicesSupported = new PropertyIdentifier(97);
    public static final PropertyIdentifier protocolVersion = new PropertyIdentifier(98);
    public static final PropertyIdentifier readOnly = new PropertyIdentifier(99);
    public static final PropertyIdentifier reasonForHalt = new PropertyIdentifier(100);
    public static final PropertyIdentifier recipientList = new PropertyIdentifier(102);
    public static final PropertyIdentifier reliability = new PropertyIdentifier(103);
    public static final PropertyIdentifier relinquishDefault = new PropertyIdentifier(104);
    public static final PropertyIdentifier required = new PropertyIdentifier(105);
    public static final PropertyIdentifier resolution = new PropertyIdentifier(106);
    public static final PropertyIdentifier segmentationSupported = new PropertyIdentifier(107);
    public static final PropertyIdentifier setpoint = new PropertyIdentifier(108);
    public static final PropertyIdentifier setpointReference = new PropertyIdentifier(109);
    public static final PropertyIdentifier stateText = new PropertyIdentifier(110);
    public static final PropertyIdentifier statusFlags = new PropertyIdentifier(111);
    public static final PropertyIdentifier systemStatus = new PropertyIdentifier(112);
    public static final PropertyIdentifier timeDelay = new PropertyIdentifier(113);
    public static final PropertyIdentifier timeOfActiveTimeReset = new PropertyIdentifier(114);
    public static final PropertyIdentifier timeOfStateCountReset = new PropertyIdentifier(115);
    public static final PropertyIdentifier timeSynchronizationRecipients = new PropertyIdentifier(116);
    public static final PropertyIdentifier units = new PropertyIdentifier(117);
    public static final PropertyIdentifier updateInterval = new PropertyIdentifier(118);
    public static final PropertyIdentifier utcOffset = new PropertyIdentifier(119);
    public static final PropertyIdentifier vendorIdentifier = new PropertyIdentifier(120);
    public static final PropertyIdentifier vendorName = new PropertyIdentifier(121);
    public static final PropertyIdentifier vtClassesSupported = new PropertyIdentifier(122);
    public static final PropertyIdentifier weeklySchedule = new PropertyIdentifier(123);
    public static final PropertyIdentifier attemptedSamples = new PropertyIdentifier(124);
    public static final PropertyIdentifier averageValue = new PropertyIdentifier(125);
    public static final PropertyIdentifier bufferSize = new PropertyIdentifier(126);
    public static final PropertyIdentifier clientCovIncrement = new PropertyIdentifier(127);
    public static final PropertyIdentifier covResubscriptionInterval = new PropertyIdentifier(128);
    public static final PropertyIdentifier eventTimeStamps = new PropertyIdentifier(130);
    public static final PropertyIdentifier logBuffer = new PropertyIdentifier(131);
    public static final PropertyIdentifier logDeviceObjectProperty = new PropertyIdentifier(132);
    public static final PropertyIdentifier enable = new PropertyIdentifier(133);
    public static final PropertyIdentifier logInterval = new PropertyIdentifier(134);
    public static final PropertyIdentifier maximumValue = new PropertyIdentifier(135);
    public static final PropertyIdentifier minimumValue = new PropertyIdentifier(136);
    public static final PropertyIdentifier notificationThreshold = new PropertyIdentifier(137);
    public static final PropertyIdentifier protocolRevision = new PropertyIdentifier(139);
    public static final PropertyIdentifier recordsSinceNotification = new PropertyIdentifier(140);
    public static final PropertyIdentifier recordCount = new PropertyIdentifier(141);
    public static final PropertyIdentifier startTime = new PropertyIdentifier(142);
    public static final PropertyIdentifier stopTime = new PropertyIdentifier(143);
    public static final PropertyIdentifier stopWhenFull = new PropertyIdentifier(144);
    public static final PropertyIdentifier totalRecordCount = new PropertyIdentifier(145);
    public static final PropertyIdentifier validSamples = new PropertyIdentifier(146);
    public static final PropertyIdentifier windowInterval = new PropertyIdentifier(147);
    public static final PropertyIdentifier windowSamples = new PropertyIdentifier(148);
    public static final PropertyIdentifier maximumValueTimestamp = new PropertyIdentifier(149);
    public static final PropertyIdentifier minimumValueTimestamp = new PropertyIdentifier(150);
    public static final PropertyIdentifier varianceValue = new PropertyIdentifier(151);
    public static final PropertyIdentifier activeCovSubscriptions = new PropertyIdentifier(152);
    public static final PropertyIdentifier backupFailureTimeout = new PropertyIdentifier(153);
    public static final PropertyIdentifier configurationFiles = new PropertyIdentifier(154);
    public static final PropertyIdentifier databaseRevision = new PropertyIdentifier(155);
    public static final PropertyIdentifier directReading = new PropertyIdentifier(156);
    public static final PropertyIdentifier lastRestoreTime = new PropertyIdentifier(157);
    public static final PropertyIdentifier maintenanceRequired = new PropertyIdentifier(158);
    public static final PropertyIdentifier memberOf = new PropertyIdentifier(159);
    public static final PropertyIdentifier mode = new PropertyIdentifier(160);
    public static final PropertyIdentifier operationExpected = new PropertyIdentifier(161);
    public static final PropertyIdentifier setting = new PropertyIdentifier(162);
    public static final PropertyIdentifier silenced = new PropertyIdentifier(163);
    public static final PropertyIdentifier trackingValue = new PropertyIdentifier(164);
    public static final PropertyIdentifier zoneMembers = new PropertyIdentifier(165);
    public static final PropertyIdentifier lifeSafetyAlarmValues = new PropertyIdentifier(166);
    public static final PropertyIdentifier maxSegmentsAccepted = new PropertyIdentifier(167);
    public static final PropertyIdentifier profileName = new PropertyIdentifier(168);
    public static final PropertyIdentifier autoSlaveDiscovery = new PropertyIdentifier(169);
    public static final PropertyIdentifier manualSlaveAddressBinding = new PropertyIdentifier(170);
    public static final PropertyIdentifier slaveAddressBinding = new PropertyIdentifier(171);
    public static final PropertyIdentifier slaveProxyEnable = new PropertyIdentifier(172);
    public static final PropertyIdentifier lastNotifyRecord = new PropertyIdentifier(173);
    public static final PropertyIdentifier scheduleDefault = new PropertyIdentifier(174);
    public static final PropertyIdentifier acceptedModes = new PropertyIdentifier(175);
    public static final PropertyIdentifier adjustValue = new PropertyIdentifier(176);
    public static final PropertyIdentifier count = new PropertyIdentifier(177);
    public static final PropertyIdentifier countBeforeChange = new PropertyIdentifier(178);
    public static final PropertyIdentifier countChangeTime = new PropertyIdentifier(179);
    public static final PropertyIdentifier covPeriod = new PropertyIdentifier(180);
    public static final PropertyIdentifier inputReference = new PropertyIdentifier(181);
    public static final PropertyIdentifier limitMonitoringInterval = new PropertyIdentifier(182);
    public static final PropertyIdentifier loggingObject = new PropertyIdentifier(183);
    public static final PropertyIdentifier loggingRecord = new PropertyIdentifier(184);
    public static final PropertyIdentifier prescale = new PropertyIdentifier(185);
    public static final PropertyIdentifier pulseRate = new PropertyIdentifier(186);
    public static final PropertyIdentifier scale = new PropertyIdentifier(187);
    public static final PropertyIdentifier scaleFactor = new PropertyIdentifier(188);
    public static final PropertyIdentifier updateTime = new PropertyIdentifier(189);
    public static final PropertyIdentifier valueBeforeChange = new PropertyIdentifier(190);
    public static final PropertyIdentifier valueSet = new PropertyIdentifier(191);
    public static final PropertyIdentifier valueChangeTime = new PropertyIdentifier(192);
    public static final PropertyIdentifier alignIntervals = new PropertyIdentifier(193);
    public static final PropertyIdentifier intervalOffset = new PropertyIdentifier(195);
    public static final PropertyIdentifier lastRestartReason = new PropertyIdentifier(196);
    public static final PropertyIdentifier loggingType = new PropertyIdentifier(197);
    public static final PropertyIdentifier restartNotificationRecipients = new PropertyIdentifier(202);
    public static final PropertyIdentifier timeOfDeviceRestart = new PropertyIdentifier(203);
    public static final PropertyIdentifier timeSynchronizationInterval = new PropertyIdentifier(204);
    public static final PropertyIdentifier trigger = new PropertyIdentifier(205);
    public static final PropertyIdentifier utcTimeSynchronizationRecipients = new PropertyIdentifier(206);
    public static final PropertyIdentifier nodeSubtype = new PropertyIdentifier(207);
    public static final PropertyIdentifier nodeType = new PropertyIdentifier(208);
    public static final PropertyIdentifier structuredObjectList = new PropertyIdentifier(209);
    public static final PropertyIdentifier subordinateAnnotations = new PropertyIdentifier(210);
    public static final PropertyIdentifier subordinateList = new PropertyIdentifier(211);
    public static final PropertyIdentifier actualShedLevel = new PropertyIdentifier(212);
    public static final PropertyIdentifier dutyWindow = new PropertyIdentifier(213);
    public static final PropertyIdentifier expectedShedLevel = new PropertyIdentifier(214);
    public static final PropertyIdentifier fullDutyBaseline = new PropertyIdentifier(215);
    public static final PropertyIdentifier requestedShedLevel = new PropertyIdentifier(218);
    public static final PropertyIdentifier shedDuration = new PropertyIdentifier(219);
    public static final PropertyIdentifier shedLevelDescriptions = new PropertyIdentifier(220);
    public static final PropertyIdentifier shedLevels = new PropertyIdentifier(221);
    public static final PropertyIdentifier stateDescription = new PropertyIdentifier(222);
    public static final PropertyIdentifier doorAlarmState = new PropertyIdentifier(226);
    public static final PropertyIdentifier doorExtendedPulseTime = new PropertyIdentifier(227);
    public static final PropertyIdentifier doorMembers = new PropertyIdentifier(228);
    public static final PropertyIdentifier doorOpenTooLongTime = new PropertyIdentifier(229);
    public static final PropertyIdentifier doorPulseTime = new PropertyIdentifier(230);
    public static final PropertyIdentifier doorStatus = new PropertyIdentifier(231);
    public static final PropertyIdentifier doorUnlockDelayTime = new PropertyIdentifier(232);
    public static final PropertyIdentifier lockStatus = new PropertyIdentifier(233);
    public static final PropertyIdentifier maskedAlarmValues = new PropertyIdentifier(234);
    public static final PropertyIdentifier securedStatus = new PropertyIdentifier(235);
    public static final PropertyIdentifier absenteeLimit = new PropertyIdentifier(244);
    public static final PropertyIdentifier accessAlarmEvents = new PropertyIdentifier(245);
    public static final PropertyIdentifier accessDoors = new PropertyIdentifier(246);
    public static final PropertyIdentifier accessEvent = new PropertyIdentifier(247);
    public static final PropertyIdentifier accessEventAuthenticationFactor = new PropertyIdentifier(248);
    public static final PropertyIdentifier accessEventCredential = new PropertyIdentifier(249);
    public static final PropertyIdentifier accessEventTime = new PropertyIdentifier(250);
    public static final PropertyIdentifier accessTransactionEvents = new PropertyIdentifier(251);
    public static final PropertyIdentifier accompaniment = new PropertyIdentifier(252);
    public static final PropertyIdentifier accompanimentTime = new PropertyIdentifier(253);
    public static final PropertyIdentifier activationTime = new PropertyIdentifier(254);
    public static final PropertyIdentifier activeAuthenticationPolicy = new PropertyIdentifier(255);
    public static final PropertyIdentifier assignedAccessRights = new PropertyIdentifier(256);
    public static final PropertyIdentifier authenticationFactors = new PropertyIdentifier(257);
    public static final PropertyIdentifier authenticationPolicyList = new PropertyIdentifier(258);
    public static final PropertyIdentifier authenticationPolicyNames = new PropertyIdentifier(259);
    public static final PropertyIdentifier authenticationStatus = new PropertyIdentifier(260);
    public static final PropertyIdentifier authorizationMode = new PropertyIdentifier(261);
    public static final PropertyIdentifier belongsTo = new PropertyIdentifier(262);
    public static final PropertyIdentifier credentialDisable = new PropertyIdentifier(263);
    public static final PropertyIdentifier credentialStatus = new PropertyIdentifier(264);
    public static final PropertyIdentifier credentials = new PropertyIdentifier(265);
    public static final PropertyIdentifier credentialsInZone = new PropertyIdentifier(266);
    public static final PropertyIdentifier daysRemaining = new PropertyIdentifier(267);
    public static final PropertyIdentifier entryPoints = new PropertyIdentifier(268);
    public static final PropertyIdentifier exitPoints = new PropertyIdentifier(269);
    public static final PropertyIdentifier expirationTime = new PropertyIdentifier(270);
    public static final PropertyIdentifier extendedTimeEnable = new PropertyIdentifier(271);
    public static final PropertyIdentifier failedAttemptEvents = new PropertyIdentifier(272);
    public static final PropertyIdentifier failedAttempts = new PropertyIdentifier(273);
    public static final PropertyIdentifier failedAttemptsTime = new PropertyIdentifier(274);
    public static final PropertyIdentifier lastAccessEvent = new PropertyIdentifier(275);
    public static final PropertyIdentifier lastAccessPoint = new PropertyIdentifier(276);
    public static final PropertyIdentifier lastCredentialAdded = new PropertyIdentifier(277);
    public static final PropertyIdentifier lastCredentialAddedTime = new PropertyIdentifier(278);
    public static final PropertyIdentifier lastCredentialRemoved = new PropertyIdentifier(279);
    public static final PropertyIdentifier lastCredentialRemovedTime = new PropertyIdentifier(280);
    public static final PropertyIdentifier lastUseTime = new PropertyIdentifier(281);
    public static final PropertyIdentifier lockout = new PropertyIdentifier(282);
    public static final PropertyIdentifier lockoutRelinquishTime = new PropertyIdentifier(283);
    public static final PropertyIdentifier maxFailedAttempts = new PropertyIdentifier(285);
    public static final PropertyIdentifier members = new PropertyIdentifier(286);
    public static final PropertyIdentifier musterPoint = new PropertyIdentifier(287);
    public static final PropertyIdentifier negativeAccessRules = new PropertyIdentifier(288);
    public static final PropertyIdentifier numberOfAuthenticationPolicies = new PropertyIdentifier(289);
    public static final PropertyIdentifier occupancyCount = new PropertyIdentifier(290);
    public static final PropertyIdentifier occupancyCountAdjust = new PropertyIdentifier(291);
    public static final PropertyIdentifier occupancyCountEnable = new PropertyIdentifier(292);
    public static final PropertyIdentifier occupancyLowerLimit = new PropertyIdentifier(294);
    public static final PropertyIdentifier occupancyLowerLimitEnforced = new PropertyIdentifier(295);
    public static final PropertyIdentifier occupancyState = new PropertyIdentifier(296);
    public static final PropertyIdentifier occupancyUpperLimit = new PropertyIdentifier(297);
    public static final PropertyIdentifier occupancyUpperLimitEnforced = new PropertyIdentifier(298);
    public static final PropertyIdentifier passbackMode = new PropertyIdentifier(300);
    public static final PropertyIdentifier passbackTimeout = new PropertyIdentifier(301);
    public static final PropertyIdentifier positiveAccessRules = new PropertyIdentifier(302);
    public static final PropertyIdentifier reasonForDisable = new PropertyIdentifier(303);
    public static final PropertyIdentifier supportedFormats = new PropertyIdentifier(304);
    public static final PropertyIdentifier supportedFormatClasses = new PropertyIdentifier(305);
    public static final PropertyIdentifier threatAuthority = new PropertyIdentifier(306);
    public static final PropertyIdentifier threatLevel = new PropertyIdentifier(307);
    public static final PropertyIdentifier traceFlag = new PropertyIdentifier(308);
    public static final PropertyIdentifier transactionNotificationClass = new PropertyIdentifier(309);
    public static final PropertyIdentifier userExternalIdentifier = new PropertyIdentifier(310);
    public static final PropertyIdentifier userInformationReference = new PropertyIdentifier(311);
    public static final PropertyIdentifier userName = new PropertyIdentifier(317);
    public static final PropertyIdentifier userType = new PropertyIdentifier(318);
    public static final PropertyIdentifier usesRemaining = new PropertyIdentifier(319);
    public static final PropertyIdentifier zoneFrom = new PropertyIdentifier(320);
    public static final PropertyIdentifier zoneTo = new PropertyIdentifier(321);
    public static final PropertyIdentifier accessEventTag = new PropertyIdentifier(322);
    public static final PropertyIdentifier globalIdentifier = new PropertyIdentifier(323);
    public static final PropertyIdentifier verificationTime = new PropertyIdentifier(326);
    public static final PropertyIdentifier baseDeviceSecurityPolicy = new PropertyIdentifier(327);
    public static final PropertyIdentifier distributionKeyRevision = new PropertyIdentifier(328);
    public static final PropertyIdentifier doNotHide = new PropertyIdentifier(329);
    public static final PropertyIdentifier keySets = new PropertyIdentifier(330);
    public static final PropertyIdentifier lastKeyServer = new PropertyIdentifier(331);
    public static final PropertyIdentifier networkAccessSecurityPolicies = new PropertyIdentifier(332);
    public static final PropertyIdentifier packetReorderTime = new PropertyIdentifier(333);
    public static final PropertyIdentifier securityPduTimeout = new PropertyIdentifier(334);
    public static final PropertyIdentifier securityTimeWindow = new PropertyIdentifier(335);
    public static final PropertyIdentifier supportedSecurityAlgorithms = new PropertyIdentifier(336);
    public static final PropertyIdentifier updateKeySetTimeout = new PropertyIdentifier(337);
    public static final PropertyIdentifier backupAndRestoreState = new PropertyIdentifier(338);
    public static final PropertyIdentifier backupPreparationTime = new PropertyIdentifier(339);
    public static final PropertyIdentifier restoreCompletionTime = new PropertyIdentifier(340);
    public static final PropertyIdentifier restorePreparationTime = new PropertyIdentifier(341);
    public static final PropertyIdentifier bitMask = new PropertyIdentifier(342);
    public static final PropertyIdentifier bitText = new PropertyIdentifier(343);
    public static final PropertyIdentifier isUtc = new PropertyIdentifier(344);
    public static final PropertyIdentifier groupMembers = new PropertyIdentifier(345);
    public static final PropertyIdentifier groupMemberNames = new PropertyIdentifier(346);
    public static final PropertyIdentifier memberStatusFlags = new PropertyIdentifier(347);
    public static final PropertyIdentifier requestedUpdateInterval = new PropertyIdentifier(348);
    public static final PropertyIdentifier covuPeriod = new PropertyIdentifier(349);
    public static final PropertyIdentifier covuRecipients = new PropertyIdentifier(350);
    public static final PropertyIdentifier eventMessageTexts = new PropertyIdentifier(351);
    public static final PropertyIdentifier eventMessageTextsConfig = new PropertyIdentifier(352);
    public static final PropertyIdentifier eventDetectionEnable = new PropertyIdentifier(353);
    public static final PropertyIdentifier eventAlgorithmInhibit = new PropertyIdentifier(354);
    public static final PropertyIdentifier eventAlgorithmInhibitRef = new PropertyIdentifier(355);
    public static final PropertyIdentifier timeDelayNormal = new PropertyIdentifier(356);
    public static final PropertyIdentifier reliabilityEvaluationInhibit = new PropertyIdentifier(357);
    public static final PropertyIdentifier faultParameters = new PropertyIdentifier(358);
    public static final PropertyIdentifier faultType = new PropertyIdentifier(359);
    public static final PropertyIdentifier localForwardingOnly = new PropertyIdentifier(360);
    public static final PropertyIdentifier processIdentifierFilter = new PropertyIdentifier(361);
    public static final PropertyIdentifier subscribedRecipients = new PropertyIdentifier(362);
    public static final PropertyIdentifier portFilter = new PropertyIdentifier(363);
    public static final PropertyIdentifier authorizationExemptions = new PropertyIdentifier(364);
    public static final PropertyIdentifier allowGroupDelayInhibit = new PropertyIdentifier(365);
    public static final PropertyIdentifier channelNumber = new PropertyIdentifier(366);
    public static final PropertyIdentifier controlGroups = new PropertyIdentifier(367);
    public static final PropertyIdentifier executionDelay = new PropertyIdentifier(368);
    public static final PropertyIdentifier lastPriority = new PropertyIdentifier(369);
    public static final PropertyIdentifier writeStatus = new PropertyIdentifier(370);
    public static final PropertyIdentifier propertyList = new PropertyIdentifier(371);
    public static final PropertyIdentifier serialNumber = new PropertyIdentifier(372);
    public static final PropertyIdentifier blinkWarnEnable = new PropertyIdentifier(373);
    public static final PropertyIdentifier defaultFadeTime = new PropertyIdentifier(374);
    public static final PropertyIdentifier defaultRampRate = new PropertyIdentifier(375);
    public static final PropertyIdentifier defaultStepIncrement = new PropertyIdentifier(376);
    public static final PropertyIdentifier egressTime = new PropertyIdentifier(377);
    public static final PropertyIdentifier inProgress = new PropertyIdentifier(378);
    public static final PropertyIdentifier instantaneousPower = new PropertyIdentifier(379);
    public static final PropertyIdentifier lightingCommand = new PropertyIdentifier(380);
    public static final PropertyIdentifier lightingCommandDefaultPriority = new PropertyIdentifier(381);
    public static final PropertyIdentifier maxActualValue = new PropertyIdentifier(382);
    public static final PropertyIdentifier minActualValue = new PropertyIdentifier(383);
    public static final PropertyIdentifier power = new PropertyIdentifier(384);
    public static final PropertyIdentifier transition = new PropertyIdentifier(385);
    public static final PropertyIdentifier egressActive = new PropertyIdentifier(386);
    public static final PropertyIdentifier interfaceValue = new PropertyIdentifier(387);
    public static final PropertyIdentifier faultHighLimit = new PropertyIdentifier(388);
    public static final PropertyIdentifier faultLowLimit = new PropertyIdentifier(389);
    public static final PropertyIdentifier lowDiffLimit = new PropertyIdentifier(390);
    public static final PropertyIdentifier strikeCount = new PropertyIdentifier(391);
    public static final PropertyIdentifier timeOfStrikeCountReset = new PropertyIdentifier(392);
    public static final PropertyIdentifier defaultTimeout = new PropertyIdentifier(393);
    public static final PropertyIdentifier initialTimeout = new PropertyIdentifier(394);
    public static final PropertyIdentifier lastStateChange = new PropertyIdentifier(395);
    public static final PropertyIdentifier stateChangeValues = new PropertyIdentifier(396);
    public static final PropertyIdentifier timerRunning = new PropertyIdentifier(397);
    public static final PropertyIdentifier timerState = new PropertyIdentifier(398);
    public static final PropertyIdentifier apduLength = new PropertyIdentifier(399);
    public static final PropertyIdentifier ipAddress = new PropertyIdentifier(400);
    public static final PropertyIdentifier ipDefaultGateway = new PropertyIdentifier(401);
    public static final PropertyIdentifier ipDhcpEnable = new PropertyIdentifier(402);
    public static final PropertyIdentifier ipDhcpLeaseTime = new PropertyIdentifier(403);
    public static final PropertyIdentifier ipDhcpLeaseTimeRemaining = new PropertyIdentifier(404);
    public static final PropertyIdentifier ipDhcpServer = new PropertyIdentifier(405);
    public static final PropertyIdentifier ipDnsServer = new PropertyIdentifier(406);
    public static final PropertyIdentifier bacnetIpGlobalAddress = new PropertyIdentifier(407);
    public static final PropertyIdentifier bacnetIpMode = new PropertyIdentifier(408);
    public static final PropertyIdentifier bacnetIpMulticastAddress = new PropertyIdentifier(409);
    public static final PropertyIdentifier bacnetIpNatTraversal = new PropertyIdentifier(410);
    public static final PropertyIdentifier ipSubnetMask = new PropertyIdentifier(411);
    public static final PropertyIdentifier bacnetIpUdpPort = new PropertyIdentifier(412);
    public static final PropertyIdentifier bbmdAcceptFdRegistrations = new PropertyIdentifier(413);
    public static final PropertyIdentifier bbmdBroadcastDistributionTable = new PropertyIdentifier(414);
    public static final PropertyIdentifier bbmdForeignDeviceTable = new PropertyIdentifier(415);
    public static final PropertyIdentifier changesPending = new PropertyIdentifier(416);
    public static final PropertyIdentifier command = new PropertyIdentifier(417);
    public static final PropertyIdentifier fdBbmdAddress = new PropertyIdentifier(418);
    public static final PropertyIdentifier fdSubscriptionLifetime = new PropertyIdentifier(419);
    public static final PropertyIdentifier linkSpeed = new PropertyIdentifier(420);
    public static final PropertyIdentifier linkSpeeds = new PropertyIdentifier(421);
    public static final PropertyIdentifier linkSpeedAutonegotiate = new PropertyIdentifier(422);
    public static final PropertyIdentifier macAddress = new PropertyIdentifier(423);
    public static final PropertyIdentifier networkInterfaceName = new PropertyIdentifier(424);
    public static final PropertyIdentifier networkNumber = new PropertyIdentifier(425);
    public static final PropertyIdentifier networkNumberQuality = new PropertyIdentifier(426);
    public static final PropertyIdentifier networkType = new PropertyIdentifier(427);
    public static final PropertyIdentifier routingTable = new PropertyIdentifier(428);
    public static final PropertyIdentifier virtualMacAddressTable = new PropertyIdentifier(429);
    public static final PropertyIdentifier commandTimeArray = new PropertyIdentifier(430);
    public static final PropertyIdentifier currentCommandPriority = new PropertyIdentifier(431);
    public static final PropertyIdentifier lastCommandTime = new PropertyIdentifier(432);
    public static final PropertyIdentifier valueSource = new PropertyIdentifier(433);
    public static final PropertyIdentifier valueSourceArray = new PropertyIdentifier(434);
    public static final PropertyIdentifier bacnetIpv6Mode = new PropertyIdentifier(435);
    public static final PropertyIdentifier ipv6Address = new PropertyIdentifier(436);
    public static final PropertyIdentifier ipv6PrefixLength = new PropertyIdentifier(437);
    public static final PropertyIdentifier bacnetIpv6UdpPort = new PropertyIdentifier(438);
    public static final PropertyIdentifier ipv6DefaultGateway = new PropertyIdentifier(439);
    public static final PropertyIdentifier bacnetIpv6MulticastAddress = new PropertyIdentifier(440);
    public static final PropertyIdentifier ipv6DnsServer = new PropertyIdentifier(441);
    public static final PropertyIdentifier ipv6AutoAddressingEnable = new PropertyIdentifier(442);
    public static final PropertyIdentifier ipv6DhcpLeaseTime = new PropertyIdentifier(443);
    public static final PropertyIdentifier ipv6DhcpLeaseTimeRemaining = new PropertyIdentifier(444);
    public static final PropertyIdentifier ipv6DhcpServer = new PropertyIdentifier(445);
    public static final PropertyIdentifier ipv6ZoneIndex = new PropertyIdentifier(446);
    public static final PropertyIdentifier assignedLandingCalls = new PropertyIdentifier(447);
    public static final PropertyIdentifier carAssignedDirection = new PropertyIdentifier(448);
    public static final PropertyIdentifier carDoorCommand = new PropertyIdentifier(449);
    public static final PropertyIdentifier carDoorStatus = new PropertyIdentifier(450);
    public static final PropertyIdentifier carDoorText = new PropertyIdentifier(451);
    public static final PropertyIdentifier carDoorZone = new PropertyIdentifier(452);
    public static final PropertyIdentifier carDriveStatus = new PropertyIdentifier(453);
    public static final PropertyIdentifier carLoad = new PropertyIdentifier(454);
    public static final PropertyIdentifier carLoadUnits = new PropertyIdentifier(455);
    public static final PropertyIdentifier carMode = new PropertyIdentifier(456);
    public static final PropertyIdentifier carMovingDirection = new PropertyIdentifier(457);
    public static final PropertyIdentifier carPosition = new PropertyIdentifier(458);
    public static final PropertyIdentifier elevatorGroup = new PropertyIdentifier(459);
    public static final PropertyIdentifier energyMeter = new PropertyIdentifier(460);
    public static final PropertyIdentifier energyMeterRef = new PropertyIdentifier(461);
    public static final PropertyIdentifier escalatorMode = new PropertyIdentifier(462);
    public static final PropertyIdentifier faultSignals = new PropertyIdentifier(463);
    public static final PropertyIdentifier floorText = new PropertyIdentifier(464);
    public static final PropertyIdentifier groupId = new PropertyIdentifier(465);
    public static final PropertyIdentifier groupMode = new PropertyIdentifier(467);
    public static final PropertyIdentifier higherDeck = new PropertyIdentifier(468);
    public static final PropertyIdentifier installationId = new PropertyIdentifier(469);
    public static final PropertyIdentifier landingCalls = new PropertyIdentifier(470);
    public static final PropertyIdentifier landingCallControl = new PropertyIdentifier(471);
    public static final PropertyIdentifier landingDoorStatus = new PropertyIdentifier(472);
    public static final PropertyIdentifier lowerDeck = new PropertyIdentifier(473);
    public static final PropertyIdentifier machineRoomId = new PropertyIdentifier(474);
    public static final PropertyIdentifier makingCarCall = new PropertyIdentifier(475);
    public static final PropertyIdentifier nextStoppingFloor = new PropertyIdentifier(476);
    public static final PropertyIdentifier operationDirection = new PropertyIdentifier(477);
    public static final PropertyIdentifier passengerAlarm = new PropertyIdentifier(478);
    public static final PropertyIdentifier powerMode = new PropertyIdentifier(479);
    public static final PropertyIdentifier registeredCarCall = new PropertyIdentifier(480);
    public static final PropertyIdentifier activeCovMultipleSubscriptions = new PropertyIdentifier(481);
    public static final PropertyIdentifier protocolLevel = new PropertyIdentifier(482);
    public static final PropertyIdentifier referencePort = new PropertyIdentifier(483);
    public static final PropertyIdentifier deployedProfileLocation = new PropertyIdentifier(484);
    public static final PropertyIdentifier profileLocation = new PropertyIdentifier(485);
    public static final PropertyIdentifier tags = new PropertyIdentifier(486);
    public static final PropertyIdentifier subordinateNodeTypes = new PropertyIdentifier(487);
    public static final PropertyIdentifier subordinateTags = new PropertyIdentifier(488);
    public static final PropertyIdentifier subordinateRelationships = new PropertyIdentifier(489);
    public static final PropertyIdentifier defaultSubordinateRelationship = new PropertyIdentifier(490);
    public static final PropertyIdentifier represents = new PropertyIdentifier(491);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static PropertyIdentifier forId(final int id) {
        PropertyIdentifier e = (PropertyIdentifier) idMap.get(id);
        if (e == null)
            e = new PropertyIdentifier(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static PropertyIdentifier forName(final String name) {
        return (PropertyIdentifier) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private PropertyIdentifier(final int value) {
        super(value);
    }

    public PropertyIdentifier(final ByteQueue queue) throws BACnetErrorException {
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
