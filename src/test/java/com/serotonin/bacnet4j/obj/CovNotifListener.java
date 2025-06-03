package com.serotonin.bacnet4j.obj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class CovNotifListener extends DeviceEventAdapter {
    static final Logger LOG = LoggerFactory.getLogger(CovNotifListener.class);

    private final List<Map<String, Object>> notifs = new ArrayList<>();

    @Override
    public void covNotificationReceived(final UnsignedInteger subscriberProcessIdentifier,
            final ObjectIdentifier initiatingDevice, final ObjectIdentifier monitoredObjectIdentifier,
            final UnsignedInteger timeRemaining, final SequenceOf<PropertyValue> listOfValues) {
        LOG.info("COV notification received.");

        final Map<String, Object> notif = new HashMap<>();
        notif.put("subscriberProcessIdentifier", subscriberProcessIdentifier);
        notif.put("initiatingDevice", initiatingDevice);
        notif.put("monitoredObjectIdentifier", monitoredObjectIdentifier);
        notif.put("timeRemaining", timeRemaining);
        notif.put("listOfValues", listOfValues);
        notifs.add(notif);
    }

    public int getNotifCount() {
        return notifs.size();
    }

    public Map<String, Object> getNotif(final int index) {
        return notifs.get(index);
    }

    public Map<String, Object> removeNotif(final int index) {
        return notifs.remove(index);
    }

    public void clearNotifs() {
        notifs.clear();
    }
}
