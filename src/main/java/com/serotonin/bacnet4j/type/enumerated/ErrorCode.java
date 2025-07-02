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
    public static final ErrorCode badTimestamp = new ErrorCode(90);
    public static final ErrorCode cannotUseKey = new ErrorCode(91);
    public static final ErrorCode cannotVerifyMessageId = new ErrorCode(92);
    public static final ErrorCode correctKeyRevision = new ErrorCode(93);
    public static final ErrorCode destinationDeviceIdRequired = new ErrorCode(94);
    public static final ErrorCode duplicateMessage = new ErrorCode(95);
    public static final ErrorCode encryptionNotConfigured = new ErrorCode(96);
    public static final ErrorCode encryptionRequired = new ErrorCode(97);
    public static final ErrorCode incorrectKey = new ErrorCode(98);
    public static final ErrorCode invalidKeyData = new ErrorCode(99);
    public static final ErrorCode keyUpdateInProgress = new ErrorCode(100);
    public static final ErrorCode malformedMessage = new ErrorCode(101);
    public static final ErrorCode notKeyServer = new ErrorCode(102);
    public static final ErrorCode securityNotConfigured = new ErrorCode(103);
    public static final ErrorCode sourceSecurityRequired = new ErrorCode(104);
    public static final ErrorCode tooManyKeys = new ErrorCode(105);
    public static final ErrorCode unknownAuthenticationType = new ErrorCode(106);
    public static final ErrorCode unknownKey = new ErrorCode(107);
    public static final ErrorCode unknownKeyRevision = new ErrorCode(108);
    public static final ErrorCode unknownSourceMessage = new ErrorCode(109);
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
