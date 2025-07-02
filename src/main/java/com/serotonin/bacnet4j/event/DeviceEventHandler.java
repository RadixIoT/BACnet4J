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

import java.util.concurrent.ConcurrentLinkedQueue;

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
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Class to handle various events that occur on the local device. This class accepts 0 to many listeners, and dispatches
 * notifications synchronously.
 *
 * @author Matthew Lohbihler
 */
public class DeviceEventHandler {
    final ConcurrentLinkedQueue<DeviceEventListener> listeners = new ConcurrentLinkedQueue<>();

    //
    //
    // Listener management
    //
    public void addListener(final DeviceEventListener l) {
        listeners.add(l);
    }

    public void removeListener(final DeviceEventListener l) {
        listeners.remove(l);
    }

    public int getListenerCount() {
        return listeners.size();
    }

    //
    //
    // Checks and notifications
    //
    public boolean checkAllowPropertyWrite(final Address from, final BACnetObject obj, final PropertyValue pv) {
        for (final DeviceEventListener l : listeners) {
            try {
                if (!l.allowPropertyWrite(from, obj, pv))
                    return false;
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
        return true;
    }

    public void fireIAmReceived(final RemoteDevice d) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.iAmReceived(d);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void propertyWritten(final Address from, final BACnetObject obj, final PropertyValue pv) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.propertyWritten(from, obj, pv);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireIHaveReceived(final RemoteDevice d, final RemoteObject o) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.iHaveReceived(d, o);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireCovNotification(final UnsignedInteger subscriberProcessIdentifier,
            final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier monitoredObjectIdentifier,
            final UnsignedInteger timeRemaining, final SequenceOf<PropertyValue> listOfValues) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.covNotificationReceived(subscriberProcessIdentifier, initiatingDeviceIdentifier,
                        monitoredObjectIdentifier, timeRemaining, listOfValues);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireEventNotification(final UnsignedInteger processIdentifier,
            final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier eventObjectIdentifier,
            final TimeStamp timeStamp, final UnsignedInteger notificationClass, final UnsignedInteger priority,
            final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
            final Boolean ackRequired, final EventState fromState, final EventState toState,
            final NotificationParameters eventValues) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.eventNotificationReceived(processIdentifier, initiatingDeviceIdentifier, eventObjectIdentifier,
                        timeStamp, notificationClass, priority, eventType, messageText, notifyType, ackRequired,
                        fromState, toState, eventValues);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireTextMessage(final ObjectIdentifier textMessageSourceDevice, final Choice messageClass,
            final MessagePriority messagePriority, final CharacterString message) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.textMessageReceived(textMessageSourceDevice, messageClass, messagePriority, message);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void synchronizeTime(final Address from, final DateTime dateTime, final boolean utc) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.synchronizeTime(from, dateTime, utc);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void requestReceived(final Address from, final Service service) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.requestReceived(from, service);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void handleException(final Exception e) {
        for (final DeviceEventListener l : listeners)
            handleException(l, e);
    }

    private static void handleException(final DeviceEventListener l, final Exception e) {
        try {
            l.listenerException(e);
        } catch (@SuppressWarnings("unused") final Exception e1) {
            // no op
        }
    }
}
