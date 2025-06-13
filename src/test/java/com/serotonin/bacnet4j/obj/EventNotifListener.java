package com.serotonin.bacnet4j.obj;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private final List<Notif> notifs = new CopyOnWriteArrayList<>();

    @Override
    public void eventNotificationReceived(final UnsignedInteger processIdentifier,
            final ObjectIdentifier initiatingDevice, final ObjectIdentifier eventObjectIdentifier,
            final TimeStamp timeStamp, final UnsignedInteger notificationClass, final UnsignedInteger priority,
            final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
            final Boolean ackRequired, final EventState fromState, final EventState toState,
            final NotificationParameters eventValues) {
        LOG.debug("Event notification received.");
        notifs.add(new Notif(processIdentifier, initiatingDevice, eventObjectIdentifier, timeStamp, notificationClass,
                priority, eventType, messageText, notifyType, ackRequired, fromState, toState, eventValues));
    }

    public record Notif(UnsignedInteger processIdentifier, ObjectIdentifier initiatingDevice,
                        ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp, UnsignedInteger notificationClass,
                        UnsignedInteger priority, EventType eventType, CharacterString messageText,
                        NotifyType notifyType, Boolean ackRequired, EventState fromState, EventState toState,
                        NotificationParameters eventValues) {
    }


    public int getNotifCount() {
        return notifs.size();
    }

    public Notif getNotif() {
        return getNotif(0);
    }

    public Notif getNotif(int index) {
        return notifs.get(index);
    }

    public Notif removeNotif() {
        return notifs.remove(0);
    }

    public Notif removeNotif(int index) {
        return notifs.remove(index);
    }

    public void clearNotifs() {
        notifs.clear();
    }
}
