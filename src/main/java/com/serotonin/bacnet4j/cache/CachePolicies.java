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

package com.serotonin.bacnet4j.cache;

import java.util.HashMap;
import java.util.Map;

import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class CachePolicies {
    /**
     * Policies that define the caching of remote devices. By default, all devices are cached indefinitely.
     */
    private final Map<RemoteDeviceIdentifier, RemoteEntityCachePolicy> devicePolicies = new HashMap<>();

    /**
     * Policies that define the caching of remote objects.
     */
    private final Map<RemoteObjectIdentifier, RemoteEntityCachePolicy> objectPolicies = new HashMap<>();

    /**
     * Policies that define the caching of remote properties.
     */
    private final Map<RemotePropertyIdentifier, RemoteEntityCachePolicy> propertyPolicies = new HashMap<>();

    public CachePolicies() {
        // Set up the default policies.

        // All devices expire after 15 minutes. Override as required.
        devicePolicies.put(new RemoteDeviceIdentifier(null), RemoteEntityCachePolicy.EXPIRE_15_MINUTES);

        // Objects within all devices never expire, assuming that object lists in devices never change. Override
        // as required.
        objectPolicies.put(new RemoteObjectIdentifier(null, null), RemoteEntityCachePolicy.NEVER_EXPIRE);

        // By default properties in all objects in all devices are never cached, under the assumption that although
        // caching is convenient, caching by default can lead to unexpected results.
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, null), RemoteEntityCachePolicy.NEVER_CACHE);

        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.objectIdentifier),
                RemoteEntityCachePolicy.NEVER_EXPIRE);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.objectName),
                RemoteEntityCachePolicy.NEVER_EXPIRE);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.objectType),
                RemoteEntityCachePolicy.NEVER_EXPIRE);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.maxApduLengthAccepted),
                RemoteEntityCachePolicy.NEVER_EXPIRE);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.segmentationSupported),
                RemoteEntityCachePolicy.NEVER_EXPIRE);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.vendorIdentifier),
                RemoteEntityCachePolicy.NEVER_EXPIRE);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.vendorName),
                RemoteEntityCachePolicy.NEVER_EXPIRE);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.modelName),
                RemoteEntityCachePolicy.NEVER_EXPIRE);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.protocolServicesSupported),
                RemoteEntityCachePolicy.NEVER_EXPIRE);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.maxSegmentsAccepted),
                RemoteEntityCachePolicy.NEVER_EXPIRE);

        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.description),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.units),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.inactiveText),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.activeText),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.protocolVersion),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.protocolRevision),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.protocolObjectTypesSupported),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.objectList),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.outputUnits),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.numberOfStates),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.stateText),
                RemoteEntityCachePolicy.EXPIRE_1_DAY);

        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.notificationClass),
                RemoteEntityCachePolicy.EXPIRE_4_HOURS);
        propertyPolicies.put(new RemotePropertyIdentifier(null, null, PropertyIdentifier.relinquishDefault),
                RemoteEntityCachePolicy.EXPIRE_4_HOURS);
    }

    public void putDevicePolicy(final Integer did, final RemoteEntityCachePolicy policy) {
        devicePolicies.put(new RemoteDeviceIdentifier(did), policy);
    }

    public RemoteEntityCachePolicy getDevicePolicy(final int did) {
        final RemoteDeviceIdentifier id = new RemoteDeviceIdentifier(did);
        RemoteEntityCachePolicy policy = devicePolicies.get(id);
        if (policy == null) {
            policy = devicePolicies.get(id.set(null));
        }
        return policy;
    }

    public void putObjectPolicy(final Integer did, final ObjectIdentifier oid, final RemoteEntityCachePolicy policy) {
        objectPolicies.put(new RemoteObjectIdentifier(did, oid), policy);
    }

    public RemoteEntityCachePolicy getObjectPolicy(final int did, final ObjectIdentifier oid) {
        final RemoteObjectIdentifier id = new RemoteObjectIdentifier(did, oid);
        RemoteEntityCachePolicy policy = objectPolicies.get(id);
        if (policy == null) {
            policy = objectPolicies.get(id.set(did, null));
            if (policy == null) {
                policy = objectPolicies.get(id.set(null, oid));
                if (policy == null) {
                    policy = objectPolicies.get(id.set(null, null));
                }
            }
        }
        return policy;
    }

    public void putPropertyPolicy(final Integer did, final ObjectIdentifier oid, final PropertyIdentifier pid,
            final RemoteEntityCachePolicy policy) {
        propertyPolicies.put(new RemotePropertyIdentifier(did, oid, pid), policy);
    }

    public RemoteEntityCachePolicy getPropertyPolicy(final int did, final ObjectIdentifier oid,
            final PropertyIdentifier pid) {
        final RemotePropertyIdentifier id = new RemotePropertyIdentifier(did, oid, pid);
        RemoteEntityCachePolicy policy = propertyPolicies.get(id);
        if (policy == null) {
            policy = propertyPolicies.get(id.set(did, oid, null));
            if (policy == null) {
                policy = propertyPolicies.get(id.set(did, null, pid));
                if (policy == null) {
                    policy = propertyPolicies.get(id.set(did, null, null));
                    if (policy == null) {
                        policy = propertyPolicies.get(id.set(null, oid, pid));
                        if (policy == null) {
                            policy = propertyPolicies.get(id.set(null, oid, null));
                            if (policy == null) {
                                policy = propertyPolicies.get(id.set(null, null, pid));
                                if (policy == null) {
                                    policy = propertyPolicies.get(id.set(null, null, null));
                                }
                            }
                        }
                    }
                }
            }
        }
        return policy;
    }

    static class RemoteDeviceIdentifier {
        private Integer did;

        public RemoteDeviceIdentifier(final Integer did) {
            this.did = did;
        }

        public RemoteDeviceIdentifier set(final Integer did) {
            this.did = did;
            return this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (did == null ? 0 : did.hashCode());
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
            final RemoteDeviceIdentifier other = (RemoteDeviceIdentifier) obj;
            if (did == null) {
                if (other.did != null)
                    return false;
            } else if (!did.equals(other.did))
                return false;
            return true;
        }
    }


    static class RemoteObjectIdentifier {
        private Integer did;
        private ObjectIdentifier oid;

        public RemoteObjectIdentifier(final Integer did, final ObjectIdentifier oid) {
            this.did = did;
            this.oid = oid;
        }

        public RemoteObjectIdentifier set(final Integer did, final ObjectIdentifier oid) {
            this.did = did;
            this.oid = oid;
            return this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (did == null ? 0 : did.hashCode());
            result = prime * result + (oid == null ? 0 : oid.hashCode());
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
            final RemoteObjectIdentifier other = (RemoteObjectIdentifier) obj;
            if (did == null) {
                if (other.did != null)
                    return false;
            } else if (!did.equals(other.did))
                return false;
            if (oid == null) {
                if (other.oid != null)
                    return false;
            } else if (!oid.equals(other.oid))
                return false;
            return true;
        }
    }


    static class RemotePropertyIdentifier {
        private Integer did;
        private ObjectIdentifier oid;
        private PropertyIdentifier pid;

        public RemotePropertyIdentifier(final Integer did, final ObjectIdentifier oid, final PropertyIdentifier pid) {
            this.did = did;
            this.oid = oid;
            this.pid = pid;
        }

        public RemotePropertyIdentifier set(final Integer did, final ObjectIdentifier oid,
                final PropertyIdentifier pid) {
            this.did = did;
            this.oid = oid;
            this.pid = pid;
            return this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (did == null ? 0 : did.hashCode());
            result = prime * result + (oid == null ? 0 : oid.hashCode());
            result = prime * result + (pid == null ? 0 : pid.hashCode());
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
            final RemotePropertyIdentifier other = (RemotePropertyIdentifier) obj;
            if (did == null) {
                if (other.did != null)
                    return false;
            } else if (!did.equals(other.did))
                return false;
            if (oid == null) {
                if (other.oid != null)
                    return false;
            } else if (!oid.equals(other.oid))
                return false;
            if (pid == null) {
                if (other.pid != null)
                    return false;
            } else if (!pid.equals(other.pid))
                return false;
            return true;
        }
    }
}
