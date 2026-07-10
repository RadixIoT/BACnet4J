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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

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
    @Serial
    private static final long serialVersionUID = 6338537708566242078L;

    private final transient LocalDevice localDevice;
    private final transient ObjectIdentifier deviceOid;
    private transient Address address;
    private transient Object userData;
    private int maxReadMultipleReferences = -1;
    private final transient RemoteEntityCache<ObjectIdentifier, RemoteObject> remoteObjectCache;

    public RemoteDevice(LocalDevice localDevice, int instanceNumber) {
        this.localDevice = localDevice;

        // Create the remote cache.
        remoteObjectCache = new RemoteEntityCache<>(localDevice);

        // Create a device object to represent itself in the object cache
        this.deviceOid = new ObjectIdentifier(ObjectType.device, instanceNumber);
        remoteObjectCache.putEntity(deviceOid, new RemoteObject(localDevice, deviceOid),
                localDevice.getCachePolicies().getObjectPolicy(instanceNumber, deviceOid));
    }

    public RemoteDevice(LocalDevice localDevice, int instanceNumber, Address address) {
        this(localDevice, instanceNumber);
        this.address = address;
    }

    public int getInstanceNumber() {
        return deviceOid.getInstanceNumber();
    }

    public ObjectIdentifier getObjectIdentifier() {
        return deviceOid;
    }

    public void setObject(RemoteObject remoteObject) {
        remoteObjectCache.putEntity(remoteObject.getObjectIdentifier(), remoteObject, //
                localDevice.getCachePolicies().getObjectPolicy( //
                        deviceOid.getInstanceNumber(), //
                        remoteObject.getObjectIdentifier()));
    }

    public RemoteObject getObject(ObjectIdentifier oid) {
        return remoteObjectCache.getCachedEntity(oid);
    }

    //
    // Get properties
    //
    public <T extends Encodable> T getDeviceProperty(PropertyIdentifier pid) {
        return getObjectProperty(deviceOid, pid, null);
    }

    public <T extends Encodable> T getDeviceProperty(PropertyIdentifier pid, UnsignedInteger pin) {
        return getObjectProperty(deviceOid, pid, pin);
    }

    public <T extends Encodable> T getObjectProperty(ObjectIdentifier oid, PropertyIdentifier pid) {
        return getObjectProperty(oid, pid, null);
    }

    public <T extends Encodable> T getObjectProperty(ObjectIdentifier oid, PropertyIdentifier pid,
            UnsignedInteger pin) {
        RemoteObject ro = getObject(oid);
        if (ro == null)
            return null;
        return ro.getProperty(pid, pin);
    }

    //
    // Set properties
    //

    public void setDeviceProperty(PropertyIdentifier pid, Encodable value) {
        setObjectProperty(deviceOid, pid, null, value);
    }

    public void setDeviceProperty(PropertyIdentifier pid, UnsignedInteger pin, Encodable value) {
        setObjectProperty(deviceOid, pid, pin, value);
    }

    public void setObjectProperty(ObjectIdentifier oid, PropertyIdentifier pid, Encodable value) {
        setObjectProperty(oid, pid, null, value);
    }

    public void setObjectProperty(ObjectIdentifier oid, PropertyIdentifier pid, UnsignedInteger pin, Encodable value) {
        if (value instanceof ErrorClassAndCode e && ErrorClass.object.equals(e.getErrorClass())) {
            // Don't create objects if the error is about the object.
            // But don't remove the object because it is possible to get error responses on objects that
            // didn't cause the error. For example, a read multiple request can contain requests for properties
            // of multiple objects. But if only one of these objects doesn't exist, an error is returned for the
            // whole request, and this error can be assigned to the objects that do exist.
            return;
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

    public <T extends Encodable> T removeDeviceProperty(PropertyIdentifier pid) {
        return removeObjectProperty(deviceOid, pid, null);
    }

    public <T extends Encodable> T removeDeviceProperty(PropertyIdentifier pid, UnsignedInteger pin) {
        return removeObjectProperty(deviceOid, pid, pin);
    }

    public <T extends Encodable> T removeObjectProperty(ObjectIdentifier oid, PropertyIdentifier pid) {
        return removeObjectProperty(oid, pid, null);
    }

    public <T extends Encodable> T removeObjectProperty(ObjectIdentifier oid, PropertyIdentifier pid,
            UnsignedInteger pin) {
        RemoteObject ro = remoteObjectCache.getCachedEntity(oid);
        if (ro == null)
            return null;
        return ro.removeProperty(pid, pin);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public int getMaxAPDULengthAccepted() {
        return getUnsignedIntegerProperty(PropertyIdentifier.maxApduLengthAccepted);
    }

    public Segmentation getSegmentationSupported() {
        return getDeviceProperty(PropertyIdentifier.segmentationSupported);
    }

    public int getVendorIdentifier() {
        // This is actually an Unsigned16, but it will work anyway.
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
        return getDeviceProperty(PropertyIdentifier.protocolServicesSupported);
    }

    public int getUnsignedIntegerProperty(PropertyIdentifier pid) {
        UnsignedInteger p = getDeviceProperty(pid);
        if (p == null)
            return -1;
        return p.intValue();
    }

    public String getCharacterStringProperty(PropertyIdentifier pid) {
        CharacterString p = getDeviceProperty(pid);
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

    public void setUserData(Object userData) {
        this.userData = userData;
    }

    public void setMaxReadMultipleReferences(int maxReadMultipleReferences) {
        this.maxReadMultipleReferences = maxReadMultipleReferences;
    }

    public int getMaxReadMultipleReferences() {
        if (maxReadMultipleReferences == -1)
            maxReadMultipleReferences = getSegmentationSupported().hasTransmitSegmentation() ? 200 : 20;
        return maxReadMultipleReferences;
    }

    public void reduceMaxReadMultipleReferences(int from) {
        int current = getMaxReadMultipleReferences();
        if (current > from)
            current = from;
        if (current > 1) {
            maxReadMultipleReferences = (int) (current * 0.75);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        RemoteDevice that = (RemoteDevice) o;
        return maxReadMultipleReferences == that.maxReadMultipleReferences && Objects.equals(localDevice,
                that.localDevice) && Objects.equals(deviceOid, that.deviceOid) && Objects.equals(
                address, that.address) && Objects.equals(userData, that.userData) && Objects.equals(
                remoteObjectCache, that.remoteObjectCache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localDevice, deviceOid, address, userData, maxReadMultipleReferences, remoteObjectCache);
    }
}
