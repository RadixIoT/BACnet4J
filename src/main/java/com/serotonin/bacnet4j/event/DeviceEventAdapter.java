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

package com.serotonin.bacnet4j.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.service.Service;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.DateTime;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * A default class for easy implementation of the DeviceEventListener interface. Instead of having to implement all
 * the defined methods, listener classes can override this and only implement the desired methods.
 */
public class DeviceEventAdapter implements DeviceEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceEventAdapter.class);

    @Override
    public void listenerException(Exception e) {
        // Override as required
        LOG.error(e.getMessage(), e);
    }

    @Override
    public boolean allowPropertyWrite(Address from, BACnetObject obj, PropertyValue pv) {
        return true;
    }

    @Override
    public void iAmReceived(RemoteDevice d) {
        // Override as required
    }

    @Override
    public void propertyWritten(Address from, BACnetObject obj, PropertyValue pv) {
        // Override as required
    }

    @Override
    public void iHaveReceived(RemoteDevice d, RemoteObject o) {
        // Override as required
    }

    @Override
    public void covNotificationReceived(UnsignedInteger subscriberProcessIdentifier,
            ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier monitoredObjectIdentifier,
            UnsignedInteger timeRemaining, SequenceOf<PropertyValue> listOfValues) {
        // Override as required
    }

    @Override
    public void eventNotificationReceived(UnsignedInteger processIdentifier,
            ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp,
            UnsignedInteger notificationClass, UnsignedInteger priority, EventType eventType,
            CharacterString messageText, NotifyType notifyType, Boolean ackRequired, EventState fromState,
            EventState toState, NotificationParameters eventValues) {
        // Override as required
    }

    @Override
    public void textMessageReceived(ObjectIdentifier textMessageSourceDevice, Choice messageClass,
            MessagePriority messagePriority, CharacterString message) {
        // Override as required
    }

    @Override
    public void synchronizeTime(Address from, DateTime dateTime, boolean utc) {
        // Override as required
    }

    @Override
    public void requestReceived(Address from, Service service) {
        // Override as required
    }

    @Override
    public void whoAmIReceived(Address from, Unsigned16 vendorId, CharacterString modelName,
            CharacterString serialNumber) {
        // Override as required
    }

    @Override
    public void youAreReceived(Address from, ObjectIdentifier deviceIdentifier, OctetString deviceMacAddress) {
        // Override as required. See 16.11.4
    }
}
