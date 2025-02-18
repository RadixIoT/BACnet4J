package com.serotonin.bacnet4j.obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.NotifyType;
import com.serotonin.bacnet4j.type.notificationParameters.NotificationParameters;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class EventNotifListener extends DeviceEventAdapter {
    static final Logger LOG = LoggerFactory.getLogger(EventNotifListener.class);

    private final List<Map<String, Object>> notifs = new ArrayList<>();

    @Override
    public void eventNotificationReceived(final UnsignedInteger processIdentifier,
            final ObjectIdentifier initiatingDevice, final ObjectIdentifier eventObjectIdentifier,
            final TimeStamp timeStamp, final UnsignedInteger notificationClass, final UnsignedInteger priority,
            final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
            final Boolean ackRequired, final EventState fromState, final EventState toState,
            final NotificationParameters eventValues) {
        LOG.debug("Event notification received.");

        synchronized (notifs) {
            final Map<String, Object> notif = new HashMap<>();
            notif.put("processIdentifier", processIdentifier);
            notif.put("initiatingDevice", initiatingDevice);
            notif.put("eventObjectIdentifier", eventObjectIdentifier);
            notif.put("timeStamp", timeStamp);
            notif.put("notificationClass", notificationClass);
            notif.put("priority", priority);
            notif.put("eventType", eventType);
            notif.put("messageText", messageText);
            notif.put("notifyType", notifyType);
            notif.put("ackRequired", ackRequired);
            notif.put("fromState", fromState);
            notif.put("toState", toState);
            notif.put("eventValues", eventValues);
            notifs.add(notif);
        }
    }

    public Map<String, Object> poll() {
        synchronized (notifs) {
            return notifs.remove(0);
        }
    }

    public int size() {
        synchronized (notifs) {
            return notifs.size();
        }
    }

    public void clear() {
        synchronized (notifs) {
            notifs.clear();
        }
    }
}
