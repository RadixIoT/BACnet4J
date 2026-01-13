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

package com.serotonin.bacnet4j;

import java.io.Serializable;

import com.serotonin.bacnet4j.cache.RemoteEntityCache;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class RemoteDevice implements Serializable {
    private static final long serialVersionUID = 6338537708566242078L;

    private final LocalDevice localDevice;
    private final ObjectIdentifier deviceOid;
    private Address address;
    private Object userData;
    private int maxReadMultipleReferences = -1;
    private final RemoteEntityCache<ObjectIdentifier, RemoteObject> remoteObjectCache;

    public RemoteDevice(final LocalDevice localDevice, final int instanceNumber) {
        this.localDevice = localDevice;

        // Create the remote cache.
        remoteObjectCache = new RemoteEntityCache<>(localDevice);

        // Create a device object to represent itself in the object cache
        this.deviceOid = new ObjectIdentifier(ObjectType.device, instanceNumber);
        remoteObjectCache.putEntity(deviceOid, new RemoteObject(localDevice, deviceOid),
                localDevice.getCachePolicies().getObjectPolicy(instanceNumber, deviceOid));
    }

    public RemoteDevice(final LocalDevice localDevice, final int instanceNumber, final Address address) {
        this(localDevice, instanceNumber);
        this.address = address;
    }

    /**
     * Add properties that are in 'that' if they are not already in 'this'.
     *
     * @param that
     */
    //    public void merge(final RemoteDevice that) {
    //        synchronized (objects) {
    //            for (final Map.Entry<ObjectIdentifier, RemoteObject> e : that.objects.entrySet()) {
    //                if (e.getValue() != null) {
    //                    final RemoteObject o = objects.get(e.getKey());
    //                    if (o == null) {
    //                        objects.put(e.getKey(), e.getValue());
    //                    } else {
    //                        o.merge(e.getValue());
    //                    }
    //                }
    //            }
    //        }
    //
    //        if (userData != null)
    //            userData = that.userData;
    //        if (maxReadMultipleReferences != -1)
    //            maxReadMultipleReferences = that.maxReadMultipleReferences;
    //    }
    public int getInstanceNumber() {
        return deviceOid.getInstanceNumber();
    }

    public ObjectIdentifier getObjectIdentifier() {
        return deviceOid;
    }

    public void setObject(final RemoteObject remoteObject) {
        remoteObjectCache.putEntity(remoteObject.getObjectIdentifier(), remoteObject, //
                localDevice.getCachePolicies().getObjectPolicy( //
                        deviceOid.getInstanceNumber(), //
                        remoteObject.getObjectIdentifier()));
    }

    public RemoteObject getObject(final ObjectIdentifier oid) {
        return remoteObjectCache.getCachedEntity(oid);
    }

    //
    // Get properties
    //
    public <T extends Encodable> T getDeviceProperty(final PropertyIdentifier pid) {
        return getObjectProperty(deviceOid, pid, null);
    }

    public <T extends Encodable> T getDeviceProperty(final PropertyIdentifier pid, final UnsignedInteger pin) {
        return getObjectProperty(deviceOid, pid, pin);
    }

    public <T extends Encodable> T getObjectProperty(final ObjectIdentifier oid, final PropertyIdentifier pid) {
        return getObjectProperty(oid, pid, null);
    }

    public <T extends Encodable> T getObjectProperty(final ObjectIdentifier oid, final PropertyIdentifier pid,
            final UnsignedInteger pin) {
        final RemoteObject ro = getObject(oid);
        if (ro == null)
            return null;
        return ro.getProperty(pid, pin);
    }

    //
    // Set properties
    //

    public void setDeviceProperty(final PropertyIdentifier pid, final Encodable value) {
        setObjectProperty(deviceOid, pid, null, value);
    }

    public void setDeviceProperty(final PropertyIdentifier pid, final UnsignedInteger pin, final Encodable value) {
        setObjectProperty(deviceOid, pid, pin, value);
    }

    public void setObjectProperty(final ObjectIdentifier oid, final PropertyIdentifier pid, final Encodable value) {
        setObjectProperty(oid, pid, null, value);
    }

    public void setObjectProperty(final ObjectIdentifier oid, final PropertyIdentifier pid, final UnsignedInteger pin,
            final Encodable value) {
        if (value instanceof ErrorClassAndCode) {
            final ErrorClassAndCode e = (ErrorClassAndCode) value;
            if (ErrorClass.object.equals(e.getErrorClass())) {
                // Don't create objects if the error is about the object.
                // But don't remove the object because it is possible to get error responses on objects that
                // didn't cause the error. For example, a read multiple request can contain requests for properties
                // of multiple objects. But if only one of these objects doesn't exist, an error is returned for the
                // whole request, and this error can be assigned to the objects that do exist.
                return;
            }
        }

        synchronized (remoteObjectCache) {
            RemoteObject ro = remoteObjectCache.getCachedEntity(oid);
            if (ro == null) {
                ro = new RemoteObject(localDevice, oid);
                remoteObjectCache.putEntity(oid, ro,
                        localDevice.getCachePolicies().getObjectPolicy(deviceOid.getInstanceNumber(), oid));
            }
            ro.setProperty(pid, pin, value,
                    localDevice.getCachePolicies().getPropertyPolicy(deviceOid.getInstanceNumber(), oid, pid));
        }
    }

    //
    // Remove properties
    //

    public <T extends Encodable> T removeDeviceProperty(final PropertyIdentifier pid) {
        return removeObjectProperty(deviceOid, pid, null);
    }

    public <T extends Encodable> T removeDeviceProperty(final PropertyIdentifier pid, final UnsignedInteger pin) {
        return removeObjectProperty(deviceOid, pid, pin);
    }

    public <T extends Encodable> T removeObjectProperty(final ObjectIdentifier oid, final PropertyIdentifier pid) {
        return removeObjectProperty(oid, pid, null);
    }

    public <T extends Encodable> T removeObjectProperty(final ObjectIdentifier oid, final PropertyIdentifier pid,
            final UnsignedInteger pin) {
        final RemoteObject ro = remoteObjectCache.getCachedEntity(oid);
        if (ro == null)
            return null;
        return ro.removeProperty(pid, pin);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public int getMaxAPDULengthAccepted() {
        return getUnsignedIntegerProperty(PropertyIdentifier.maxApduLengthAccepted);
    }

    public Segmentation getSegmentationSupported() {
        return (Segmentation) getDeviceProperty(PropertyIdentifier.segmentationSupported);
    }

    public int getVendorIdentifier() {
        return getUnsignedIntegerProperty(PropertyIdentifier.vendorIdentifier);
    }

    public String getVendorName() {
        return getCharacterStringProperty(PropertyIdentifier.vendorName);
    }

    public String getName() {
        return getCharacterStringProperty(PropertyIdentifier.objectName);
    }

    public String getModelName() {
        return getCharacterStringProperty(PropertyIdentifier.modelName);
    }

    public ServicesSupported getServicesSupported() {
        return (ServicesSupported) getDeviceProperty(PropertyIdentifier.protocolServicesSupported);
    }

    public int getUnsignedIntegerProperty(final PropertyIdentifier pid) {
        final UnsignedInteger p = (UnsignedInteger) getDeviceProperty(pid);
        if (p == null)
            return -1;
        return p.intValue();
    }

    public String getCharacterStringProperty(final PropertyIdentifier pid) {
        final CharacterString p = (CharacterString) getDeviceProperty(pid);
        if (p == null)
            return null;
        return p.getValue();
    }

    @Override
    public String toString() {
        return "RemoteDevice(instanceNumber=" + getInstanceNumber() + ", address=" + address + ")";
    }

    public String toExtendedString() {
        return "RemoteDevice(instanceNumber=" + getInstanceNumber() + ", address=" + address
                + ", maxAPDULengthAccepted=" + getMaxAPDULengthAccepted() + ", segmentationSupported="
                + getSegmentationSupported() + ", vendorId=" + getVendorIdentifier() + ", vendorName=" + getVendorName()
                + ", name=" + getName() + ", servicesSupported=" + getServicesSupported() + ")";
    }

    public Object getUserData() {
        return userData;
    }

    public void setUserData(final Object userData) {
        this.userData = userData;
    }

    public void setMaxReadMultipleReferences(final int maxReadMultipleReferences) {
        this.maxReadMultipleReferences = maxReadMultipleReferences;
    }

    public int getMaxReadMultipleReferences() {
        if (maxReadMultipleReferences == -1)
            maxReadMultipleReferences = getSegmentationSupported().hasTransmitSegmentation() ? 200 : 20;
        return maxReadMultipleReferences;
    }

    public void reduceMaxReadMultipleReferences(final int from) {
        int current = getMaxReadMultipleReferences();
        if (current > from)
            current = from;
        if (current > 1) {
            maxReadMultipleReferences = (int) (current * 0.75);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (deviceOid == null ? 0 : deviceOid.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final RemoteDevice other = (RemoteDevice) obj;
        if (deviceOid == null) {
            if (other.deviceOid != null)
                return false;
        } else if (!deviceOid.equals(other.deviceOid))
            return false;
        return true;
    }
}
