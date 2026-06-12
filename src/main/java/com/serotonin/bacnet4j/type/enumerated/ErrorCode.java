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

public class ErrorCode extends Enumerated {
    public static final ErrorCode other = new ErrorCode(0);
    public static final ErrorCode configurationInProgress = new ErrorCode(2);
    public static final ErrorCode deviceBusy = new ErrorCode(3);
    public static final ErrorCode dynamicCreationNotSupported = new ErrorCode(4);
    public static final ErrorCode fileAccessDenied = new ErrorCode(5);
    public static final ErrorCode inconsistentParameters = new ErrorCode(7);
    public static final ErrorCode inconsistentSelectionCriterion = new ErrorCode(8);
    public static final ErrorCode invalidDataType = new ErrorCode(9);
    public static final ErrorCode invalidFileAccessMethod = new ErrorCode(10);
    public static final ErrorCode invalidFileStartPosition = new ErrorCode(11);
    public static final ErrorCode invalidParameterDataType = new ErrorCode(13);
    public static final ErrorCode invalidTimeStamp = new ErrorCode(14);
    public static final ErrorCode missingRequiredParameter = new ErrorCode(16);
    public static final ErrorCode noObjectsOfSpecifiedType = new ErrorCode(17);
    public static final ErrorCode noSpaceForObject = new ErrorCode(18);
    public static final ErrorCode noSpaceToAddListElement = new ErrorCode(19);
    public static final ErrorCode noSpaceToWriteProperty = new ErrorCode(20);
    public static final ErrorCode noVtSessionsAvailable = new ErrorCode(21);
    public static final ErrorCode propertyIsNotAList = new ErrorCode(22);
    public static final ErrorCode objectDeletionNotPermitted = new ErrorCode(23);
    public static final ErrorCode objectIdentifierAlreadyExists = new ErrorCode(24);
    public static final ErrorCode operationalProblem = new ErrorCode(25);
    public static final ErrorCode passwordFailure = new ErrorCode(26);
    public static final ErrorCode readAccessDenied = new ErrorCode(27);
    public static final ErrorCode serviceRequestDenied = new ErrorCode(29);
    public static final ErrorCode timeout = new ErrorCode(30);
    public static final ErrorCode unknownObject = new ErrorCode(31);
    public static final ErrorCode unknownProperty = new ErrorCode(32);
    public static final ErrorCode unknownVtClass = new ErrorCode(34);
    public static final ErrorCode unknownVtSession = new ErrorCode(35);
    public static final ErrorCode unsupportedObjectType = new ErrorCode(36);
    public static final ErrorCode valueOutOfRange = new ErrorCode(37);
    public static final ErrorCode vtSessionAlreadyClosed = new ErrorCode(38);
    public static final ErrorCode vtSessionTerminationFailure = new ErrorCode(39);
    public static final ErrorCode writeAccessDenied = new ErrorCode(40);
    public static final ErrorCode characterSetNotSupported = new ErrorCode(41);
    public static final ErrorCode invalidArrayIndex = new ErrorCode(42);
    public static final ErrorCode covSubscriptionFailed = new ErrorCode(43);
    public static final ErrorCode notCovProperty = new ErrorCode(44);
    public static final ErrorCode optionalFunctionalityNotSupported = new ErrorCode(45);
    public static final ErrorCode invalidConfigurationData = new ErrorCode(46);
    public static final ErrorCode datatypeNotSupported = new ErrorCode(47);
    public static final ErrorCode duplicateName = new ErrorCode(48);
    public static final ErrorCode duplicateObjectId = new ErrorCode(49);
    public static final ErrorCode propertyIsNotAnArray = new ErrorCode(50);
    public static final ErrorCode abortBufferOverflow = new ErrorCode(51);
    public static final ErrorCode abortInvalidApduInThisState = new ErrorCode(52);
    public static final ErrorCode abortPreemptedByHigherPriorityTask = new ErrorCode(53);
    public static final ErrorCode abortSegmentationNotSupported = new ErrorCode(54);
    public static final ErrorCode abortProprietary = new ErrorCode(55);
    public static final ErrorCode abortOther = new ErrorCode(56);
    public static final ErrorCode invalidTag = new ErrorCode(57);
    public static final ErrorCode networkDown = new ErrorCode(58);
    public static final ErrorCode rejectBufferOverflow = new ErrorCode(59);
    public static final ErrorCode rejectInconsistentParameters = new ErrorCode(60);
    public static final ErrorCode rejectInvalidParameterDataType = new ErrorCode(61);
    public static final ErrorCode rejectInvalidTag = new ErrorCode(62);
    public static final ErrorCode rejectMissingRequiredParameter = new ErrorCode(63);
    public static final ErrorCode rejectParameterOutOfRange = new ErrorCode(64);
    public static final ErrorCode rejectTooManyArguments = new ErrorCode(65);
    public static final ErrorCode rejectUndefinedEnumeration = new ErrorCode(66);
    public static final ErrorCode rejectUnrecognizedService = new ErrorCode(67);
    public static final ErrorCode rejectProprietary = new ErrorCode(68);
    public static final ErrorCode rejectOther = new ErrorCode(69);
    public static final ErrorCode unknownDevice = new ErrorCode(70);
    public static final ErrorCode unknownRoute = new ErrorCode(71);
    public static final ErrorCode valueNotInitialized = new ErrorCode(72);
    public static final ErrorCode invalidEventState = new ErrorCode(73);
    public static final ErrorCode noAlarmConfigured = new ErrorCode(74);
    public static final ErrorCode logBufferFull = new ErrorCode(75);
    public static final ErrorCode loggedValuePurged = new ErrorCode(76);
    public static final ErrorCode noPropertySpecified = new ErrorCode(77);
    public static final ErrorCode notConfiguredForTriggeredLogging = new ErrorCode(78);
    public static final ErrorCode unknownSubscription = new ErrorCode(79);
    public static final ErrorCode parameterOutOfRange = new ErrorCode(80);
    public static final ErrorCode listElementNotFound = new ErrorCode(81);
    public static final ErrorCode busy = new ErrorCode(82);
    public static final ErrorCode communicationDisabled = new ErrorCode(83);
    public static final ErrorCode success = new ErrorCode(84);
    public static final ErrorCode accessDenied = new ErrorCode(85);
    public static final ErrorCode badDestinationAddress = new ErrorCode(86);
    public static final ErrorCode badDestinationDeviceId = new ErrorCode(87);
    public static final ErrorCode badSignature = new ErrorCode(88);
    public static final ErrorCode badSourceAddress = new ErrorCode(89);
    public static final ErrorCode duplicateMessage = new ErrorCode(95);
    public static final ErrorCode encryptionNotConfigured = new ErrorCode(96);
    public static final ErrorCode encryptionRequired = new ErrorCode(97);
    public static final ErrorCode malformedMessage = new ErrorCode(101);
    public static final ErrorCode securityNotConfigured = new ErrorCode(103);
    public static final ErrorCode sourceSecurityRequired = new ErrorCode(104);
    public static final ErrorCode unknownAuthenticationType = new ErrorCode(106);
    public static final ErrorCode notRouterToDnet = new ErrorCode(110);
    public static final ErrorCode routerBusy = new ErrorCode(111);
    public static final ErrorCode unknownNetworkMessage = new ErrorCode(112);
    public static final ErrorCode messageTooLong = new ErrorCode(113);
    public static final ErrorCode securityError = new ErrorCode(114);
    public static final ErrorCode addressingError = new ErrorCode(115);
    public static final ErrorCode writeBdtFailed = new ErrorCode(116);
    public static final ErrorCode readBdtFailed = new ErrorCode(117);
    public static final ErrorCode registerForeignDeviceFailed = new ErrorCode(118);
    public static final ErrorCode readFdtFailed = new ErrorCode(119);
    public static final ErrorCode deleteFdtEntryFailed = new ErrorCode(120);
    public static final ErrorCode distributeBroadcastFailed = new ErrorCode(121);
    public static final ErrorCode unknownFileSize = new ErrorCode(122);
    public static final ErrorCode abortApduTooLong = new ErrorCode(123);
    public static final ErrorCode abortApplicationExceededReplyTime = new ErrorCode(124);
    public static final ErrorCode abortOutOfResources = new ErrorCode(125);
    public static final ErrorCode abortTsmTimeout = new ErrorCode(126);
    public static final ErrorCode abortWindowSizeOutOfRange = new ErrorCode(127);
    public static final ErrorCode fileFull = new ErrorCode(128);
    public static final ErrorCode inconsistentConfiguration = new ErrorCode(129);
    public static final ErrorCode inconsistentObjectType = new ErrorCode(130);
    public static final ErrorCode internalError = new ErrorCode(131);
    public static final ErrorCode notConfigured = new ErrorCode(132);
    public static final ErrorCode outOfMemory = new ErrorCode(133);
    public static final ErrorCode valueTooLong = new ErrorCode(134);
    public static final ErrorCode abortInsufficientSecurity = new ErrorCode(135);
    public static final ErrorCode abortSecurityError = new ErrorCode(136);
    public static final ErrorCode duplicateEntry = new ErrorCode(137);
    public static final ErrorCode invalidValueInThisState = new ErrorCode(138);
    public static final ErrorCode invalidOperationInThisState = new ErrorCode(139);
    public static final ErrorCode listItemNotNumbered = new ErrorCode(140);
    public static final ErrorCode listItemNotTimestamped = new ErrorCode(141);
    public static final ErrorCode invalidDataEncoding = new ErrorCode(142);
    public static final ErrorCode bvlcFunctionUnknown = new ErrorCode(143);
    public static final ErrorCode bvlcProprietaryFunctionUnknown = new ErrorCode(144);
    public static final ErrorCode headerEncodingError = new ErrorCode(145);
    public static final ErrorCode headerNotUnderstood = new ErrorCode(146);
    public static final ErrorCode messageIncomplete = new ErrorCode(147);
    public static final ErrorCode notABacnetScHub = new ErrorCode(148);
    public static final ErrorCode payloadExpected = new ErrorCode(149);
    public static final ErrorCode unexpectedData = new ErrorCode(150);
    public static final ErrorCode nodeDuplicateVmac = new ErrorCode(151);
    public static final ErrorCode httpUnexpectedResponseCode = new ErrorCode(152);
    public static final ErrorCode httpNoUpgrade = new ErrorCode(153);
    public static final ErrorCode httpResourceNotLocal = new ErrorCode(154);
    public static final ErrorCode httpProxyAuthenticationFailed = new ErrorCode(155);
    public static final ErrorCode httpResponseTimeout = new ErrorCode(156);
    public static final ErrorCode httpResponseSyntaxError = new ErrorCode(157);
    public static final ErrorCode httpResponseValueError = new ErrorCode(158);
    public static final ErrorCode httpResponseMissingHeader = new ErrorCode(159);
    public static final ErrorCode httpWebsocketHeaderError = new ErrorCode(160);
    public static final ErrorCode httpUpgradeRequired = new ErrorCode(161);
    public static final ErrorCode httpUpgradeError = new ErrorCode(162);
    public static final ErrorCode httpTemporaryUnavailable = new ErrorCode(163);
    public static final ErrorCode httpNotAServer = new ErrorCode(164);
    public static final ErrorCode httpError = new ErrorCode(165);
    public static final ErrorCode websocketSchemeNotSupported = new ErrorCode(166);
    public static final ErrorCode websocketUnknownControlMessage = new ErrorCode(167);
    public static final ErrorCode websocketCloseError = new ErrorCode(168);
    public static final ErrorCode websocketClosedByPeer = new ErrorCode(169);
    public static final ErrorCode websocketEndpointLeaves = new ErrorCode(170);
    public static final ErrorCode websocketProtocolError = new ErrorCode(171);
    public static final ErrorCode websocketDataNotAccepted = new ErrorCode(172);
    public static final ErrorCode websocketClosedAbnormally = new ErrorCode(173);
    public static final ErrorCode websocketDataInconsistent = new ErrorCode(174);
    public static final ErrorCode websocketDataAgainstPolicy = new ErrorCode(175);
    public static final ErrorCode websocketFrameTooLong = new ErrorCode(176);
    public static final ErrorCode websocketExtensionMissing = new ErrorCode(177);
    public static final ErrorCode websocketRequestUnavailable = new ErrorCode(178);
    public static final ErrorCode websocketError = new ErrorCode(179);
    public static final ErrorCode tlsClientCertificateError = new ErrorCode(180);
    public static final ErrorCode tlsServerCertificateError = new ErrorCode(181);
    public static final ErrorCode tlsClientAuthenticationFailed = new ErrorCode(182);
    public static final ErrorCode tlsServerAuthenticationFailed = new ErrorCode(183);
    public static final ErrorCode tlsClientCertificateExpired = new ErrorCode(184);
    public static final ErrorCode tlsServerCertificateExpired = new ErrorCode(185);
    public static final ErrorCode tlsClientCertificateRevoked = new ErrorCode(186);
    public static final ErrorCode tlsServerCertificateRevoked = new ErrorCode(187);
    public static final ErrorCode tlsError = new ErrorCode(188);
    public static final ErrorCode dnsUnavailable = new ErrorCode(189);
    public static final ErrorCode dnsNameResolutionFailed = new ErrorCode(190);
    public static final ErrorCode dnsResolverFailure = new ErrorCode(191);
    public static final ErrorCode dnsError = new ErrorCode(192);
    public static final ErrorCode tcpConnectTimeout = new ErrorCode(193);
    public static final ErrorCode tcpConnectionRefused = new ErrorCode(194);
    public static final ErrorCode tcpClosedByLocal = new ErrorCode(195);
    public static final ErrorCode tcpClosedOther = new ErrorCode(196);
    public static final ErrorCode tcpError = new ErrorCode(197);
    public static final ErrorCode ipAddressNotReachable = new ErrorCode(198);
    public static final ErrorCode ipError = new ErrorCode(199);
    public static final ErrorCode certificateExpired = new ErrorCode(200);
    public static final ErrorCode certificateInvalid = new ErrorCode(201);
    public static final ErrorCode certificateMalformed = new ErrorCode(202);
    public static final ErrorCode certificateRevoked = new ErrorCode(203);
    public static final ErrorCode unknownCertificateKey = new ErrorCode(204);
    public static final ErrorCode referencedPortInError = new ErrorCode(205);
    public static final ErrorCode notEnabled = new ErrorCode(206);
    public static final ErrorCode adjustScopeRequired = new ErrorCode(207);
    public static final ErrorCode authScopeRequired = new ErrorCode(208);
    public static final ErrorCode bindScopeRequired = new ErrorCode(209);
    public static final ErrorCode configScopeRequired = new ErrorCode(210);
    public static final ErrorCode controlScopeRequired = new ErrorCode(211);
    public static final ErrorCode extendedScopeRequired = new ErrorCode(212);
    public static final ErrorCode incorrectClient = new ErrorCode(213);
    public static final ErrorCode installScopeRequired = new ErrorCode(214);
    public static final ErrorCode insufficientScope = new ErrorCode(215);
    public static final ErrorCode noDefaultScope = new ErrorCode(216);
    public static final ErrorCode noPolicy = new ErrorCode(217);
    public static final ErrorCode revokedToken = new ErrorCode(218);
    public static final ErrorCode overrideScopeRequired = new ErrorCode(219);
    public static final ErrorCode inactiveToken = new ErrorCode(220);
    public static final ErrorCode unknownAudience = new ErrorCode(221);
    public static final ErrorCode unknownClient = new ErrorCode(222);
    public static final ErrorCode unknownScope = new ErrorCode(223);
    public static final ErrorCode viewScopeRequired = new ErrorCode(224);
    public static final ErrorCode incorrectAudience = new ErrorCode(225);
    public static final ErrorCode incorrectClientOrigin = new ErrorCode(226);
    public static final ErrorCode invalidArraySize = new ErrorCode(227);
    public static final ErrorCode incorrectIssuer = new ErrorCode(228);
    public static final ErrorCode invalidToken = new ErrorCode(229);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }

    public static ErrorCode forId(final int id) {
        ErrorCode e = (ErrorCode) idMap.get(id);
        if (e == null)
            e = new ErrorCode(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static ErrorCode forName(final String name) {
        return (ErrorCode) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private ErrorCode(final int value) {
        super(value);
    }

    public ErrorCode(final ByteQueue queue) throws BACnetErrorException {
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
