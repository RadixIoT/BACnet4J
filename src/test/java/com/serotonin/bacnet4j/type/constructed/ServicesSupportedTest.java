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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServicesSupportedTest {
    @Test
    public void toStringTest() {
        var ss = new ServicesSupported();
        ss.setAcknowledgeAlarm(true);
        ss.setCreateObject(true);
        ss.setTimeSynchronization(true);
        ss.setUnconfirmedTextMessage(true);

        assertEquals(
                "ServicesSupported[IAm=false, IHave=false, acknowledgeAlarm=true, addListElement=false, atomicReadFile=false, atomicWriteFile=false, confirmedCovNotification=false, confirmedCovNotificationMultiple=false, confirmedEventNotification=false, confirmedPrivateTransfer=false, confirmedTextMessage=false, createObject=true, deleteObject=false, deviceCommunicationControl=false, getAlarmSummary=false, getEnrollmentSummary=false, getEventInformation=false, lifeSafetyOperation=false, readProperty=false, readPropertyMultiple=false, readRange=false, reinitializeDevice=false, removeListElement=false, subscribeCov=false, subscribeCovProperty=false, subscribeCovPropertyMultiple=false, timeSynchronization=true, unconfirmedCovNotification=false, unconfirmedCovNotificationMultiple=false, unconfirmedEventNotification=false, unconfirmedPrivateTransfer=false, unconfirmedTextMessage=true, utcTimeSynchronization=false, vtClose=false, vtData=false, vtOpen=false, whoHas=false, whoIs=false, writeGroup=false, writeProperty=false, writePropertyMultiple=false]",
                ss.toString());
    }
}
