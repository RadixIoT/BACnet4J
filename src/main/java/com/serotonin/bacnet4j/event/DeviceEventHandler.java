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
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Class to handle various events that occur on the local device. This class accepts 0 to many listeners, and dispatches
 * notifications synchronously.
 *
 * @author Matthew Lohbihler
 */
public class DeviceEventHandler {
    protected final ConcurrentLinkedQueue<DeviceEventListener> listeners = new ConcurrentLinkedQueue<>();

    //
    //
    // Listener management
    //
    public void addListener(DeviceEventListener l) {
        listeners.add(l);
    }

    public void removeListener(DeviceEventListener l) {
        listeners.remove(l);
    }

    public int getListenerCount() {
        return listeners.size();
    }

    //
    //
    // Checks and notifications
    //
    public boolean checkAllowPropertyWrite(Address from, BACnetObject obj, PropertyValue pv) {
        for (DeviceEventListener l : listeners) {
            try {
                if (!l.allowPropertyWrite(from, obj, pv))
                    return false;
            } catch (Exception e) {
                handleException(l, e);
            }
        }
        return true;
    }

    public void fireIAmReceived(RemoteDevice d) {
        for (DeviceEventListener l : listeners) {
            try {
                l.iAmReceived(d);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }

    public void propertyWritten(Address from, BACnetObject obj, PropertyValue pv) {
        for (DeviceEventListener l : listeners) {
            try {
                l.propertyWritten(from, obj, pv);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireIHaveReceived(RemoteDevice d, RemoteObject o) {
        for (DeviceEventListener l : listeners) {
            try {
                l.iHaveReceived(d, o);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireCovNotification(UnsignedInteger subscriberProcessIdentifier,
            ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier monitoredObjectIdentifier,
            UnsignedInteger timeRemaining, SequenceOf<PropertyValue> listOfValues) {
        for (DeviceEventListener l : listeners) {
            try {
                l.covNotificationReceived(subscriberProcessIdentifier, initiatingDeviceIdentifier,
                        monitoredObjectIdentifier, timeRemaining, listOfValues);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireEventNotification(UnsignedInteger processIdentifier, ObjectIdentifier initiatingDeviceIdentifier,
            ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp, UnsignedInteger notificationClass,
            UnsignedInteger priority, EventType eventType, CharacterString messageText, NotifyType notifyType,
            Boolean ackRequired, EventState fromState, EventState toState, NotificationParameters eventValues) {
        for (DeviceEventListener l : listeners) {
            try {
                l.eventNotificationReceived(processIdentifier, initiatingDeviceIdentifier, eventObjectIdentifier,
                        timeStamp, notificationClass, priority, eventType, messageText, notifyType, ackRequired,
                        fromState, toState, eventValues);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireTextMessage(ObjectIdentifier textMessageSourceDevice, Choice messageClass,
            MessagePriority messagePriority, CharacterString message) {
        for (DeviceEventListener l : listeners) {
            try {
                l.textMessageReceived(textMessageSourceDevice, messageClass, messagePriority, message);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }

    public void synchronizeTime(Address from, DateTime dateTime, boolean utc) {
        for (DeviceEventListener l : listeners) {
            try {
                l.synchronizeTime(from, dateTime, utc);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }

    public void requestReceived(Address from, Service service) {
        for (DeviceEventListener l : listeners) {
            try {
                l.requestReceived(from, service);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireWhoAmIReceived(Address from, Unsigned16 vendorId, CharacterString modelName,
            CharacterString serialNumber) {
        for (DeviceEventListener l : listeners) {
            try {
                l.whoAmIReceived(from, vendorId, modelName, serialNumber);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireYouAreReceived(Address from, ObjectIdentifier deviceIdentifier, OctetString deviceMacAddress) {
        for (DeviceEventListener l : listeners) {
            try {
                l.youAreReceived(from, deviceIdentifier, deviceMacAddress);
            } catch (Exception e) {
                handleException(l, e);
            }
        }
    }


    public void handleException(Exception e) {
        for (DeviceEventListener l : listeners) {
            handleException(l, e);
        }
    }

    private static void handleException(DeviceEventListener l, Exception e) {
        try {
            l.listenerException(e);
        } catch (@SuppressWarnings("unused") Exception e1) {
            // no op
        }
    }
}
