package com.serotonin.bacnet4j.obj;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class CovNotifListener extends DeviceEventAdapter {
    static final Logger LOG = LoggerFactory.getLogger(CovNotifListener.class);

    private final List<Notif> notifs = new CopyOnWriteArrayList<>();

    @Override
    public void covNotificationReceived(final UnsignedInteger subscriberProcessIdentifier,
            final ObjectIdentifier initiatingDevice, final ObjectIdentifier monitoredObjectIdentifier,
            final UnsignedInteger timeRemaining, final SequenceOf<PropertyValue> listOfValues) {
        LOG.info("COV notification received.");
        notifs.add(new Notif(subscriberProcessIdentifier, initiatingDevice, monitoredObjectIdentifier, timeRemaining,
                listOfValues));
    }

    public record Notif(UnsignedInteger subscriberProcessIdentifier, ObjectIdentifier initiatingDevice,
                        ObjectIdentifier monitoredObjectIdentifier, UnsignedInteger timeRemaining,
                        SequenceOf<PropertyValue> listOfValues) {
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
